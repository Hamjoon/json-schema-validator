```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)} and
 * {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag, java.util.function.Consumer)}.
 *
 * The tests focus on:
 * <ul>
 *   <li>Obtaining a factory without a customizer.</li>
 *   <li>Obtaining a factory with a customizer that registers a custom schema loader and verifying that the
 *       registered schema can be retrieved via {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_Test {

    /** A simple meta‑schema JSON used in the customizer test. */
    private static final String META_SCHEMA_DATA = "{\r\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\r\n"
            + "  \"$id\": \"https://www.example.com/no-validation-no-format/schema\",\r\n"
            + "  \"$vocabulary\": {\r\n"
            + "    \"https://www.example.com/vocab/validation\": false,\r\n"
            + "    \"https://json-schema.org/draft/2020-12/vocab/applicator\": true,\r\n"
            + "    \"https://json-schema.org/draft/2020-12/vocab/core\": true\r\n"
            + "  },\r\n"
            + "  \"allOf\": [\r\n"
            + "    { \"$ref\": \"https://json-schema.org/draft/2020-12/meta/applicator\" },\r\n"
            + "    { \"$ref\": \"https://json-schema.org/draft/2020-12/meta/core\" }\r\n"
            + "  ]\r\n"
            + "}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
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
     * Verify that {@code getInstance(VersionFlag)} returns a non‑null factory and that
     * repeated calls with the same flag return the *same* instance (the factory is cached).
     */
    @Test
    void getInstance_withoutCustomizer_returnsCachedFactory() {
        JsonSchemaFactory factory1 = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchemaFactory factory2 = JsonSchemaFactory.getInstance(VersionFlag.V202012);

        assertNotNull(factory1, "Factory should not be null");
        assertNotNull(factory2, "Factory should not be null");
        assertSame(factory1, factory2, "Factory instances for the same VersionFlag should be cached and identical");
    }

    /**
     * Verify that a customizer can be used to register a schema loader and that the
     * registered schema can be retrieved via {@code getMetaSchema}.
     */
    @Test
    void getInstance_withCustomizer_registersSchemaLoader() {
        // Build a factory that knows about a custom meta‑schema.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(
                                        "https://www.example.com/no-validation-no-format/schema",
                                        META_SCHEMA_DATA))));

        assertNotNull(factory, "Factory created with customizer must not be null");

        // Retrieve the meta‑schema that we just registered.
        JsonMetaSchema metaSchema = factory.getMetaSchema(
                "https://www.example.com/no-validation-no-format/schema",
                SchemaValidatorsConfig.builder().build());

        assertNotNull(metaSchema, "Meta schema should be resolved from the custom loader");
    }

    /**
     * Concurrency test similar to the original repository test but scoped to the
     * {@code getInstance} method. It ensures that multiple threads obtaining the
     * same factory instance see the same underlying {@link JsonMetaSchema}.
     */
    @Test
    void concurrency_getInstanceSameFactoryAcrossThreads() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(
                                        "https://www.example.com/no-validation-no-format/schema",
                                        META_SCHEMA_DATA))));

        AtomicBoolean failed = new AtomicBoolean(false);
        JsonMetaSchema[] sharedInstance = new JsonMetaSchema[1];
        CountDownLatch latch = new CountDownLatch(1);
        int threadCount = 30;

        for (int i = 0; i < threadCount; ++i) {
            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                JsonMetaSchema meta = factory.getMetaSchema(
                        "https://www.example.com/no-validation-no-format/schema",
                        SchemaValidatorsConfig.builder().build());

                synchronized (sharedInstance) {
                    if (sharedInstance[0] == null) {
                        sharedInstance[0] = meta;
                    } else if (sharedInstance[0] != meta) {
                        failed.set(true);
                    }
                }
            }, "t-" + i).start();
        }

        // Release all threads simultaneously.
        latch.countDown();

        // Give a little time for threads to finish.
        Thread.sleep(500);

        assertFalse(failed.get(), "All threads should obtain the same JsonMetaSchema instance");
    }
}
```