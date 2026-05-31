package com.embitious.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ai.djl.inference.Predictor;

/**
 * Tests for {@link EmbeddingService}.
 *
 * The real DJL model loading is bypassed by injecting a mocked {@link Predictor} via the private
 * {@code threadLocalPredictor} field. This keeps the tests fast and deterministic.
 */
class EmbeddingServiceTest {

    private EmbeddingService embeddingService;
    private Predictor<String, float[]> mockPredictor;

    @BeforeEach
    void setUp() throws Exception {
        embeddingService = new EmbeddingService();
        // Use reflection to set the private fields needed for the test.
        java.lang.reflect.Field predictorField = EmbeddingService.class.getDeclaredField("threadLocalPredictor");
        predictorField.setAccessible(true);
        mockPredictor = Mockito.mock(Predictor.class);
        // ThreadLocal that always returns the mock predictor.
        java.lang.ThreadLocal<Predictor<String, float[]>> tl = new java.lang.ThreadLocal<>() {
            @Override
            protected Predictor<String, float[]> initialValue() {
                return mockPredictor;
            }
        };
        predictorField.set(embeddingService, tl);
    }

    @Test
    void testEmbedReturnsPredictorResult() throws Exception {
        float[] expected = new float[] {1.0f, 2.0f, 3.0f};
        Mockito.when(mockPredictor.predict("test text")).thenReturn(expected);

        float[] result = embeddingService.embed("test text");
        assertArrayEquals(expected, result);
        Mockito.verify(mockPredictor).predict("test text");
    }

    @Test
    void testEmbedBatchReturnsPredictorResult() throws Exception {
        List<String> texts = List.of("a", "b");
        List<float[]> expected = new ArrayList<>();
        expected.add(new float[] {0.1f});
        expected.add(new float[] {0.2f});
        Mockito.when(mockPredictor.batchPredict(texts)).thenReturn(expected);

        List<float[]> result = embeddingService.embedBatch(texts);
        assertEquals(2, result.size());
        assertArrayEquals(new float[] {0.1f}, result.get(0));
        assertArrayEquals(new float[] {0.2f}, result.get(1));
        Mockito.verify(mockPredictor).batchPredict(texts);
    }

    @Test
    void testEmbedBatchEmptyInputReturnsEmptyList() {
        List<float[]> result = embeddingService.embedBatch(new ArrayList<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
