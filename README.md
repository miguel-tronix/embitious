# Embitious

A high-performance vector embedding service built with **Java 21** and **Quarkus**, designed as a drop-in replacement for Python-based embedding services. It leverages the [Deep Java Library (DJL)](https://djl.ai/) to run Hugging Face `sentence-transformers` models natively on the JVM, with no Python runtime required.

## Overview

Embitious exposes two interfaces:

| Interface | Protocol | Use case |
|-----------|----------|----------|
| `POST /embed` | REST / JSON | Single text embedding (synchronous) |
| JMS Queue | ActiveMQ Artemis | Batch embedding (async, request-reply) |

The service is stateless: it computes embeddings and returns them to the caller. There is no database, no persistent storage, and no outbound network calls at inference time.

## Architecture

```
src/
├── main/java/com/embitious/
│   ├── resource/
│   │   └── EmbeddingResource.java      # REST endpoint  (POST /embed)
│   ├── service/
│   │   └── EmbeddingService.java       # DJL model loading & inference
│   └── messaging/
│       └── BatchEmbeddingListener.java # JMS consumer for batch requests
└── test/java/com/embitious/
    ├── resource/
    │   └── EmbeddingResourceTest.java  # REST integration tests
    ├── service/
    │   └── EmbeddingServiceTest.java   # EmbeddingService unit tests
    └── messaging/
        └── BatchEmbeddingListenerTest.java # JMS listener unit tests
```

## REST API

### `POST /embed`

**Request:**
```json
{ "text": "The quick brown fox jumps over the lazy dog" }
```

**Response `200 OK`:**
```json
{ "embedding": [0.12, -0.34, ...] }
```

**Response `400 Bad Request`** — when `text` is null or blank:
```json
{ "error": "Text must not be null or empty" }
```

## JMS Batch API

Send a `TextMessage` to the configured request queue with a JSON body:

```json
{
  "id": "req-abc123",
  "texts": ["sentence one", "sentence two", "sentence three"]
}
```

Set `JMSReplyTo` to your reply queue and `JMSCorrelationID` for tracking. The service responds with:

```json
{
  "requestId": "req-abc123",
  "embeddings": [[0.1, ...], [0.2, ...], [0.3, ...]]
}
```

On error:
```json
{ "error": "Texts list cannot be empty" }
```

## Configuration

Set the following properties in `src/main/resources/application.properties`:

```properties
# Hugging Face model name (via DJL model zoo)
embitious.embedding.model-name=sentence-transformers/all-MiniLM-L6-v2

# Run in offline mode (no model download at startup)
embitious.embedding.offline=false

# JMS request queue name
embitious.jms.request-queue=embedding-requests

# ActiveMQ Artemis broker URL
quarkus.artemis.url=tcp://localhost:61616
```

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 21 |
| Framework | Quarkus 3.36 |
| ML inference | DJL 0.31 + PyTorch engine |
| Models | Hugging Face sentence-transformers |
| Messaging | Apache ActiveMQ Artemis (JMS) |
| Testing | JUnit 5, Mockito, REST-assured |
| Build | Maven (via `./mvnw`) |

## Running Locally

**Prerequisites:** Java 21, Maven wrapper (`./mvnw`)

```bash
# Development mode with live reload
./mvnw quarkus:dev

# Run tests
./mvnw test

# Package (JVM mode)
./mvnw package
```

## Docker / Kubernetes

A `.dockerignore` is included. Build the container image via Quarkus container image extensions, then deploy to Kubernetes. The service is intentionally stateless and scales horizontally — each pod loads its own model into memory.

## Testing

Unit tests use Mockito to mock DJL `Predictor` and JMS infrastructure — no broker or model download required:

```bash
./mvnw test
```

Integration tests (`@QuarkusTest`) spin up the full Quarkus context:

```bash
./mvnw verify
```

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.
