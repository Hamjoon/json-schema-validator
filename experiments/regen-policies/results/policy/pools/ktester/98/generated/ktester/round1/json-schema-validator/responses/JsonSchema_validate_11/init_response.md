```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, ExecutionContextCustomizer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_11_Test {

    /** Shared factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Configuration for the schema (no pre‑loading). */
    private static SchemaValidatorsConfig config;

    /** The schema under test. */
    private JsonSchema schema;

    /** Simple customizer that does nothing – used to exercise the overload. */
    private ExecutionContextCustomizer customizer;

    @BeforeAll
    static void setupBeforeAll() {
        // Build a factory that can resolve the external reference used in the schema.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders
                        .schemas(Collections.singletonMap(
                                "http://example.org/ref.json",
                                // reference schema – a simple object with a required string property
                                "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"$id\": \"http://example.org/ref.json\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"name\": { \"type\": \"string\" }\n" +
                                "  },\n" +
                                "  \"required\": [\"name\"]\n" +
                                "}"))));
        // Do not preload the root schema – we want the factory to parse it lazily.
        config = SchemaValidatorsConfig.builder()
                .preloadJsonSchema(false)
                .build();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Root schema that references the above schema via $ref.
        String schemaData = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                "  \"$id\": \"http://example.org/schema.json\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"$ref\": \"http://example.org/ref.json\"\n" +
                "}";

        // Create the JsonSchema instance that will be exercised.
        schema = factory.getSchema(schemaData, config);

        // A no‑op customizer – the method under test accepts any implementation.
        customizer = executionContext -> {
            // intentionally left blank
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy.
        schema = null;
        customizer = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
        config = null;
    }

    /**
     * Validates that the overload accepting an {@link ExecutionContextCustomizer}
     * correctly delegates to the core validation logic and returns the expected
     * {@link ValidationMessage}s.
     */
    @Test
    void validate_WithExecutionContextCustomizer_ReturnsExpectedMessages() {
        // Input that violates the referenced schema (name must be a string).
        String inputData = "{ \"name\": 1 }";

        // Invoke the method under test.
        Set<ValidationMessage> messages = schema.validate(inputData, InputFormat.JSON, customizer);

        // The schema should report exactly one validation error (type mismatch).
        assertEquals(1, messages.size(), "Expected a single validation error");

        // Optional: verify that the message relates to the 'type' keyword.
        ValidationMessage message = messages.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("type"),
                "Expected the validation message to mention a type error");
    }

    /**
     * A concurrency sanity check that also uses the customizer overload.
     * This mirrors the existing concurrency test but exercises the three‑argument
     * method.
     */
    @Test
    void concurrency_WithCustomizer() throws Exception {
        String inputData = "{ \"name\": 1 }";
        CountDownLatch latch = new CountDownLatch(1);
        Exception[] captured = new Exception[1];

        Runnable task = () -> {
            try {
                latch.await();
                Set<ValidationMessage> msgs = schema.validate(inputData, InputFormat.JSON, customizer);
                assertEquals(1, msgs.size());
            } catch (Exception e) {
                synchronized (captured) {
                    captured[0] = e;
                }
            }
        };

        Thread[] threads = new Thread[30];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task, "worker-" + i);
            threads[i].start();
        }

        latch.countDown(); // release all workers simultaneously

        for (Thread t : threads) {
            t.join();
        }

        if (captured[0] != null) {
            throw captured[0];
        }
    }
}
```