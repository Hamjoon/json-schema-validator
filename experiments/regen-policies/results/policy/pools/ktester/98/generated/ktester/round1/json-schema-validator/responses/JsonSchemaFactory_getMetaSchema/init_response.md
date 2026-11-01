```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * <p>The tests focus on the caching behaviour of {@code getMetaSchema} and on its
 * thread‑safety.  A small meta‑schema JSON document is registered via the
 * {@link JsonSchemaFactory.Builder} so that the factory can resolve the IRI
 * without touching the network or the file system.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMetaSchema_Test {

    /** IRI used for the custom meta‑schema in the tests. */
    private static final String TEST_META_SCHEMA_IRI = "https://www.example.com/test-meta-schema";

    /** Minimal meta‑schema JSON – it only needs to be syntactically valid. */
    private static final String TEST_META_SCHEMA_JSON = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"$id\": \"" + TEST_META_SCHEMA_IRI + "\",\n"
            + "  \"type\": \"object\"\n"
            + "}";

    /** Factory under test – created once per test method. */
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a JsonSchemaFactory that knows about our test meta‑schema.
         * The builder registers a custom {@link SchemaLoaders} instance that
         * returns the JSON string defined above when the IRI is requested.
         */
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(TEST_META_SCHEMA_IRI, TEST_META_SCHEMA_JSON))));
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that the same {@link JsonMetaSchema} instance is returned for
     * repeated calls with the same IRI, regardless of the {@link SchemaValidatorsConfig}
     * supplied. This proves that the internal cache works as intended.
     */
    @Test
    void getMetaSchema_isCachedAcrossCalls() {
        SchemaValidatorsConfig config1 = SchemaValidatorsConfig.builder().build();
        SchemaValidatorsConfig config2 = SchemaValidatorsConfig.builder().build();

        JsonMetaSchema meta1 = factory.getMetaSchema(TEST_META_SCHEMA_IRI, config1);
        assertNotNull(meta1, "First call should return a non‑null meta‑schema");

        JsonMetaSchema meta2 = factory.getMetaSchema(TEST_META_SCHEMA_IRI, config2);
        assertNotNull(meta2, "Second call should also return a non‑null meta‑schema");

        assertSame(meta1, meta2, "Meta‑schema instances must be identical (cached)");
    }

    /**
     * Stress test the thread‑safety of {@code getMetaSchema}.  Fifty threads
     * invoke the method concurrently; all must obtain the same cached instance.
     */
    @Test
    void getMetaSchema_isThreadSafe() throws InterruptedException {
        final int THREAD_COUNT = 50;
        final JsonMetaSchema[] holder = new JsonMetaSchema[1];
        final AtomicBoolean failed = new AtomicBoolean(false);
        final CountDownLatch startLatch = new CountDownLatch(1);

        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
                try {
                    startLatch.await(); // wait until all threads are ready
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                JsonMetaSchema meta = factory.getMetaSchema(TEST_META_SCHEMA_IRI, cfg);
                synchronized (holder) {
                    if (holder[0] == null) {
                        holder[0] = meta;
                    } else if (holder[0] != meta) {
                        failed.set(true);
                    }
                }
            }, "MetaSchema-Thread-" + i);
            threads[i].start();
        }

        // Release all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        for (Thread t : threads) {
            t.join();
        }

        assertFalse(failed.get(), "All threads must receive the same JsonMetaSchema instance");
        assertNotNull(holder[0], "Meta‑schema should have been loaded by at least one thread");
    }

    /**
     * Ensure that an unknown IRI results in an {@link IllegalArgumentException}
     * because the factory cannot locate a meta‑schema for it.
     */
    @Test
    void getMetaSchema_unknownIri_throwsException() {
        String unknownIri = "https://www.example.com/unknown-meta-schema";
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getMetaSchema(unknownIri, cfg),
                "Requesting an unknown meta‑schema IRI should throw IllegalArgumentException");

        assertTrue(thrown.getMessage().contains("Meta Schema for default Meta Schema URI must be provided")
                || thrown.getMessage().contains("cannot be null"),
                "Exception message should indicate a missing meta‑schema");
    }
}
```