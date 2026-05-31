package com.embitious.messaging;

import com.embitious.service.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class BatchEmbeddingListener {

    private static final Logger LOG = Logger.getLogger(BatchEmbeddingListener.class);

    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "embitious.jms.request-queue")
    String requestQueueName;

    private JMSContext context;
    private JMSConsumer consumer;

    void onStart(@Observes StartupEvent ev) {
        LOG.infof("Starting JMS Batch Embedding Consumer on queue: %s", requestQueueName);
        try {
            context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
            Queue queue = context.createQueue(requestQueueName);
            consumer = context.createConsumer(queue);
            
            // Register an asynchronous MessageListener
            consumer.setMessageListener(this::onMessage);
            LOG.info("JMS MessageListener successfully registered.");
        } catch (Exception e) {
            LOG.error("Failed to initialize JMS consumer. The broker may be offline.", e);
            // We do not fail startup, so that REST endpoints can still function.
            // In Kubernetes, this can be monitored or retried.
        }
    }

    private void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                String body = ((TextMessage) message).getText();
                LOG.debugf("Received JMS TextMessage: %s", body);
                
                BatchRequest request;
                try {
                    request = objectMapper.readValue(body, BatchRequest.class);
                } catch (Exception e) {
                    LOG.error("Failed to parse JSON batch request from JMS body", e);
                    sendErrorResponse(message, "Invalid JSON payload: " + e.getMessage());
                    return;
                }

                if (request.getTexts() == null || request.getTexts().isEmpty()) {
                    LOG.warn("Received empty texts in batch embedding request");
                    sendErrorResponse(message, "Texts list cannot be empty");
                    return;
                }

                long startTime = System.currentTimeMillis();
                List<float[]> embeddings = embeddingService.embedBatch(request.getTexts());
                long duration = System.currentTimeMillis() - startTime;
                LOG.infof("Processed batch of %d items in %d ms", request.getTexts().size(), duration);

                // Prepare reply
                BatchResponse response = new BatchResponse(request.getId(), embeddings);
                String responseBody = objectMapper.writeValueAsString(response);

                Destination replyTo = message.getJMSReplyTo();
                if (replyTo != null) {
                    // Send to reply destination
                    try (JMSContext replyContext = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
                        TextMessage replyMessage = replyContext.createTextMessage(responseBody);
                        if (message.getJMSCorrelationID() != null) {
                            replyMessage.setJMSCorrelationID(message.getJMSCorrelationID());
                        }
                        replyContext.createProducer().send(replyTo, replyMessage);
                        LOG.debug("Sent response back to JMSReplyTo queue");
                    }
                } else {
                    LOG.warn("JMS message did not specify JMSReplyTo. Embedding computed but response was not sent.");
                }
            } else {
                LOG.warnf("Unsupported message type received: %s", message.getClass().getName());
            }
        } catch (Exception e) {
            LOG.error("Error processing JMS message", e);
        }
    }

    private void sendErrorResponse(Message requestMsg, String errorMsg) {
        try {
            Destination replyTo = requestMsg.getJMSReplyTo();
            if (replyTo != null) {
                try (JMSContext replyContext = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
                    BatchErrorResponse errResponse = new BatchErrorResponse(errorMsg);
                    String body = objectMapper.writeValueAsString(errResponse);
                    TextMessage replyMessage = replyContext.createTextMessage(body);
                    if (requestMsg.getJMSCorrelationID() != null) {
                        replyMessage.setJMSCorrelationID(requestMsg.getJMSCorrelationID());
                    }
                    replyContext.createProducer().send(replyTo, replyMessage);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to send JMS error response", e);
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Stopping JMS Batch Embedding Consumer...");
        if (consumer != null) {
            try {
                consumer.close();
            } catch (Exception e) {
                LOG.error("Error closing consumer", e);
            }
        }
        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                LOG.error("Error closing context", e);
            }
        }
    }

    // --- Request/Response POJOs ---

    public static class BatchRequest {
        private String id;
        private List<String> texts;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getTexts() {
            return texts;
        }

        public void setTexts(List<String> texts) {
            this.texts = texts;
        }
    }

    public static class BatchResponse {
        private String requestId;
        private List<float[]> embeddings;

        public BatchResponse() {}

        public BatchResponse(String requestId, List<float[]> embeddings) {
            this.requestId = requestId;
            this.embeddings = embeddings;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public List<float[]> getEmbeddings() {
            return embeddings;
        }

        public void setEmbeddings(List<float[]> embeddings) {
            this.embeddings = embeddings;
        }
    }

    public static class BatchErrorResponse {
        private String error;

        public BatchErrorResponse() {}

        public BatchErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
