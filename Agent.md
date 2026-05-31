# Agent.md — Embitious Development Guidelines

## Project Overview

**Embitious** is a high-performance, stateless vector embedding service built on **Java 21 + Quarkus**. It replaces Python-based embedding services (e.g. FastAPI + sentence-transformers) with a lower-latency JVM implementation using the **Deep Java Library (DJL)** and Hugging Face `sentence-transformers` models.

## Tech Stack

- **Java 21** — target language and runtime
- **Quarkus 3.36** — cloud-native framework; prefer Quarkus extensions over plain Jakarta EE where available
- **DJL 0.31** with **PyTorch engine** — ML inference; uses `ThreadLocal<Predictor>` for thread safety
- **Apache ActiveMQ Artemis** (via `quarkus-artemis-jms`) — JMS broker for batch requests
- **Jackson** (`quarkus-rest-jackson`) — JSON serialisation
- **JUnit 5 + Mockito + REST-assured** — testing

## Service Design Principles

- **Stateless, pure compute.** This service only does maths. It makes no outbound calls, writes to no database, and holds no persistent state beyond the in-memory model.
- **Thread safety.** `EmbeddingService` uses a `ThreadLocal<Predictor>` pool. Never share a single `Predictor` across threads.
- **Efficiency first.** Batch requests go via JMS (`BatchEmbeddingListener`) using DJL's `batchPredict()`. REST handles single requests. Warm up the predictor at startup.
- **Graceful degradation.** JMS consumer failures at startup do not prevent REST endpoints from serving. Log and continue.

## Package Structure

```
com.embitious.resource   → REST endpoints (JAX-RS)
com.embitious.service    → Core business logic (EmbeddingService)
com.embitious.messaging  → JMS listener (BatchEmbeddingListener) + request/response POJOs
```

## Configuration Keys

| Key | Description |
|-----|-------------|
| `embitious.embedding.model-name` | Hugging Face model name via DJL model zoo |
| `embitious.embedding.offline` | Set `true` to disable model download (use local cache) |
| `embitious.jms.request-queue` | JMS queue name for batch embedding requests |

## Testing Conventions

- **Unit tests** live under `src/test/java/com/embitious/` mirroring the main package structure.
- Use **Mockito** to mock DJL `Predictor` and JMS infrastructure — tests must not require a running broker or a downloaded model.
- Use **`@QuarkusTest`** only for REST integration tests.
- Inject mocks via reflection where CDI injection is not available in plain unit tests.
- All tests should be fast (< 1s each) and fully deterministic.

## Build & Run

```bash
# Dev mode
./mvnw quarkus:dev

# Unit tests only
./mvnw test

# Full verify (includes integration tests)
./mvnw verify
```

## Motivation

This service was created to outperform the Python FastAPI embedding service at `~/apps/deepdivee/src/deepdive/agent/embedders.py`. Benchmark comparisons should focus on p99 latency and throughput under concurrent load.

## License

MIT — see [LICENSE](LICENSE).
