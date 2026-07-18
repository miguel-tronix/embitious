package com.embitious.service;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EmbeddingService {

    private static final Logger LOG = Logger.getLogger(EmbeddingService.class);

    @ConfigProperty(name = "embitious.embedding.model-name")
    String modelName;

    @ConfigProperty(name = "embitious.embedding.offline", defaultValue = "false")
    boolean offline;

    private ZooModel<String, float[]> model;
    private ThreadLocal<Predictor<String, float[]>> threadLocalPredictor;
    private final List<Predictor<String, float[]>> predictorsToClose = new ArrayList<>();

    @PostConstruct
    void init() {
        try {
            if (offline) {
                System.setProperty("ai.djl.offline", "true");
            }

            Criteria<String, float[]> criteria = Criteria.builder()
                    .optApplication(Application.NLP.TEXT_EMBEDDING)
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/" + modelName)
                    .optEngine("PyTorch")
                    .build();

            model = criteria.loadModel();
            threadLocalPredictor = ThreadLocal.withInitial(() -> {
                Predictor<String, float[]> p = model.newPredictor();
                predictorsToClose.add(p);
                return p;
            });

            // Warm up the predictor pool by initializing a predictor for the startup thread
            threadLocalPredictor.get();
            
            LOG.infof("Model '%s' loaded and warmed up successfully", modelName);
        } catch (IOException | ModelException e) {
            LOG.errorf(e, "Failed to load model: %s", modelName);
            throw new RuntimeException("Failed to initialize embedding model", e);
        }
    }

    @Timeout(30000)
    public float[] embed(String text) {
        try {
            return threadLocalPredictor.get().predict(text);
        } catch (TranslateException e) {
            throw new RuntimeException("Failed to compute embedding for text", e);
        }
    }

    @Timeout(60000)
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return threadLocalPredictor.get().batchPredict(texts);
        } catch (TranslateException e) {
            throw new RuntimeException("Failed to compute batch embeddings", e);
        }
    }

    @PreDestroy
    void cleanup() {
        if (predictorsToClose != null) {
            synchronized (predictorsToClose) {
                for (Predictor<String, float[]> p : predictorsToClose) {
                    if (p != null) {
                        try {
                            p.close();
                        } catch (Exception e) {
                            LOG.error("Failed to close predictor", e);
                        }
                    }
                }
                predictorsToClose.clear();
            }
        }
        if (model != null) {
            model.close();
        }
    }
}
