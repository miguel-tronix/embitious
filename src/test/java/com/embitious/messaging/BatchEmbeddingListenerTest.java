package com.embitious.messaging;

import com.embitious.service.EmbeddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BatchEmbeddingListener}. The private {@code onMessage} method is exercised via
 * reflection. All JMS interactions are mocked so the test runs fast and does not require a running
 * broker.
 */
class BatchEmbeddingListenerTest {

    private BatchEmbeddingListener listener;
    private EmbeddingService mockEmbeddingService;
    private ObjectMapper objectMapper;
    private jakarta.jms.ConnectionFactory mockConnectionFactory;
    private JMSContext mockJmsContext;
    private JMSProducer mockProducer;
    private TextMessage mockReplyMessage;

    @BeforeEach
    void setUp() throws Exception {
        listener = new BatchEmbeddingListener();
        mockEmbeddingService = mock(EmbeddingService.class);
        objectMapper = new ObjectMapper();
        mockConnectionFactory = mock(jakarta.jms.ConnectionFactory.class);
        mockJmsContext = mock(JMSContext.class);
        mockProducer = mock(JMSProducer.class);
        mockReplyMessage = mock(TextMessage.class);

        // Inject mocks via reflection
        java.lang.reflect.Field cfField = BatchEmbeddingListener.class.getDeclaredField("connectionFactory");
        cfField.setAccessible(true);
        cfField.set(listener, mockConnectionFactory);
        java.lang.reflect.Field esField = BatchEmbeddingListener.class.getDeclaredField("embeddingService");
        esField.setAccessible(true);
        esField.set(listener, mockEmbeddingService);
        java.lang.reflect.Field omField = BatchEmbeddingListener.class.getDeclaredField("objectMapper");
        omField.setAccessible(true);
        omField.set(listener, objectMapper);

        // Mock reply handling behavior
        when(mockConnectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)).thenReturn(mockJmsContext);
        when(mockJmsContext.createProducer()).thenReturn(mockProducer);
        when(mockJmsContext.createTextMessage(anyString())).thenReturn(mockReplyMessage);
    }

    @Test
    void onMessageProcessesValidBatchRequestAndSendsResponse() throws Exception {
        // Build request JSON
        BatchEmbeddingListener.BatchRequest request = new BatchEmbeddingListener.BatchRequest();
        request.setId("req-123");
        request.setTexts(List.of("text1", "text2"));
        String requestJson = objectMapper.writeValueAsString(request);

        // Mock embedding service response
        List<float[]> fakeEmbeddings = List.of(new float[]{0.1f}, new float[]{0.2f});
        when(mockEmbeddingService.embedBatch(request.getTexts())).thenReturn(fakeEmbeddings);

        // Mock incoming JMS message
        TextMessage mockMessage = mock(TextMessage.class);
        Destination mockReplyTo = mock(Destination.class);
        when(mockMessage.getText()).thenReturn(requestJson);
        when(mockMessage.getJMSReplyTo()).thenReturn(mockReplyTo);
        when(mockMessage.getJMSCorrelationID()).thenReturn("corr-456");

        // Call private onMessage via reflection
        java.lang.reflect.Method onMessageMethod = BatchEmbeddingListener.class.getDeclaredMethod("onMessage", Message.class);
        onMessageMethod.setAccessible(true);
        onMessageMethod.invoke(listener, mockMessage);

        // Verify service called
        verify(mockEmbeddingService).embedBatch(request.getTexts());

        // Verify a reply was created and sent with correlation ID
        verify(mockJmsContext).createTextMessage(anyString());
        verify(mockProducer).send(eq(mockReplyTo), eq(mockReplyMessage));
        verify(mockReplyMessage).setJMSCorrelationID("corr-456");
    }

    @Test
    void onMessageHandlesEmptyTextsBySendingErrorResponse() throws Exception {
        BatchEmbeddingListener.BatchRequest request = new BatchEmbeddingListener.BatchRequest();
        request.setId("req-empty");
        request.setTexts(List.of()); // empty list
        String requestJson = objectMapper.writeValueAsString(request);

        TextMessage mockMessage = mock(TextMessage.class);
        Destination mockReplyTo = mock(Destination.class);
        when(mockMessage.getText()).thenReturn(requestJson);
        when(mockMessage.getJMSReplyTo()).thenReturn(mockReplyTo);
        when(mockMessage.getJMSCorrelationID()).thenReturn("corr-789");

        // Spy listener to ensure error handling path
        BatchEmbeddingListener spyListener = Mockito.spy(listener);
        java.lang.reflect.Method onMessageMethod = BatchEmbeddingListener.class.getDeclaredMethod("onMessage", Message.class);
        onMessageMethod.setAccessible(true);
        onMessageMethod.invoke(spyListener, mockMessage);

        // Verify an error response was attempted
        verify(mockJmsContext).createTextMessage(anyString());
        verify(mockProducer).send(eq(mockReplyTo), eq(mockReplyMessage));
        // Service should not be called
        verify(mockEmbeddingService, never()).embedBatch(anyList());
    }
}
