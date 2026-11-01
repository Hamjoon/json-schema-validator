```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String, SchemaValidatorsConfig)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>It should return a non‑null {@link JsonSchema} for a valid JSON schema string.</li>
 *   <li>The same {@link JsonSchemaFactory} instance should return the same {@link JsonSchema}
 *       object when the same schema string and configuration are supplied (caching behaviour).</li>
 *   <li>Concurrent calls with the same inputs must not break the caching invariant.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_Test {

    private static final String SIMPLE_SCHEMA = "{ \"type\": \"string\" }";

    private JsonSchemaFactory factory;
    private SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Create a factory that knows about the Draft‑2020‑12 meta‑schema.
         * The builder is used only to ensure the factory can be instantiated
         * without pulling external resources.
         */
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                builder -> builder.schemaLoaders(loaders -> loaders
                        .schemas(Collections.emptyMap())));

        // Use the default configuration provided by the builder.
        config = SchemaValidatorsConfig.builder().build();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getSchema_returnsNonNullJsonSchema() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA, config);
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance");
        // Basic sanity check – the schema should be able to validate a matching instance.
        assertTrue(schema.validate("hello").isSuccess(),
                "A string value should be valid against a simple 'type:string' schema");
    }

    @Test
    void getSchema_returnsSameInstanceForIdenticalInput() {
        JsonSchema first = factory.getSchema(SIMPLE_SCHEMA, config);
        JsonSchema second = factory.getSchema(SIMPLE_SCHEMA, config);
        assertSame(first, second, "Repeated calls with identical schema and config must return the same cached instance");
    }

    @Test
    void getSchema_concurrentAccessKeepsCacheInvariant() throws InterruptedException {
        final int threadCount = 30;
        final JsonSchema[] holder = new JsonSchema[1];
        final boolean[] mismatch = new boolean[1];
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    JsonSchema s = factory.getSchema(SIMPLE_SCHEMA, config);
                    synchronized (holder) {
                        if (holder[0] == null) {
                            holder[0] = s;
                        } else if (holder[0] != s) {
                            mismatch[0] = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }, "t-" + i).start();
        }

        // Release all threads simultaneously.
        startLatch.countDown();
        // Wait for all threads to finish.
        doneLatch.await();

        assertFalse(mismatch[0], "All threads should receive the same cached JsonSchema instance");
        assertNotNull(holder[0], "A JsonSchema instance should have been created");
    }
}
```