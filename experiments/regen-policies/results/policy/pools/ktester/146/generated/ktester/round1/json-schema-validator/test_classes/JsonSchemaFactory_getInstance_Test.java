package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_Test {

    /**
     * A simple meta‑schema JSON used in the customizer test.
     */
    private static final String META_SCHEMA_DATA = "{\r\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\r\n" + "  \"$id\": \"https://www.example.com/no-validation-no-format/schema\",\r\n" + "  \"$vocabulary\": {\r\n" + "    \"https://www.example.com/vocab/validation\": false,\r\n" + "    \"https://json-schema.org/draft/2020-12/vocab/applicator\": true,\r\n" + "    \"https://json-schema.org/draft/2020-12/vocab/core\": true\r\n" + "  },\r\n" + "  \"allOf\": [\r\n" + "    { \"$ref\": \"https://json-schema.org/draft/2020-12/meta/applicator\" },\r\n" + "    { \"$ref\": \"https://json-schema.org/draft/2020-12/meta/core\" }\r\n" + "  ]\r\n" + "}";

    private static Stream<Arguments> provideValidVersionFlags() {
        return Stream.of(Arguments.of(VersionFlag.V4, "http://json-schema.org/draft-04/schema#"), Arguments.of(VersionFlag.V6, "http://json-schema.org/draft-06/schema#"), Arguments.of(VersionFlag.V7, "http://json-schema.org/draft-07/schema#"), Arguments.of(VersionFlag.DRAFT_2019_09, "https://json-schema.org/draft/2019-09/schema"), Arguments.of(VersionFlag.DRAFT_2020_12, "https://json-schema.org/draft/2020-12/schema"));
    }

    private static Stream<VersionFlag> provideCachingFlags() {
        return Stream.of(VersionFlag.V4, VersionFlag.V7, VersionFlag.DRAFT_2020_12);
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
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap("https://www.example.com/no-validation-no-format/schema", META_SCHEMA_DATA))));
        assertNotNull(factory, "Factory created with customizer must not be null");
        // Retrieve the meta‑schema that we just registered.
        JsonMetaSchema metaSchema = factory.getMetaSchema("https://www.example.com/no-validation-no-format/schema", SchemaValidatorsConfig.builder().build());
        assertNotNull(metaSchema, "Meta schema should be resolved from the custom loader");
    }

    /**
     * Concurrency test similar to the original repository test but scoped to the
     * {@code getInstance} method. It ensures that multiple threads obtaining the
     * same factory instance see the same underlying {@link JsonMetaSchema}.
     */
    @Test
    void concurrency_getInstanceSameFactoryAcrossThreads() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap("https://www.example.com/no-validation-no-format/schema", META_SCHEMA_DATA))));
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
                JsonMetaSchema meta = factory.getMetaSchema("https://www.example.com/no-validation-no-format/schema", SchemaValidatorsConfig.builder().build());
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

    /**
     * Parameterised test that verifies a factory can be created for each supported
     * {@link SpecVersion.VersionFlag} and that the corresponding meta‑schema is
     * resolvable.
     */
    @ParameterizedTest(name = "[{index}] versionFlag={0}")
    @MethodSource("provideValidVersionFlags")
    void getInstance_validVersionFlags_resolvesMetaSchema(VersionFlag flag, String metaSchemaUri) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        assertNotNull(factory, "Factory should not be null for flag " + flag);
        JsonMetaSchema meta = factory.getMetaSchema(metaSchemaUri, SchemaValidatorsConfig.builder().build());
        assertNotNull(meta, "Meta‑schema for URI '" + metaSchemaUri + "' should be resolvable with flag " + flag);
    }

    /**
     * Verify that passing {@code null} as the version flag results in a
     * {@link NullPointerException}. The implementation may also throw an
     * {@link IllegalArgumentException}; both are acceptable as they indicate a
     * fast‑fail on invalid input.
     */
    @Test
    void getInstance_nullVersionFlag_throwsException() {
        // catch both NPE and IAE
        assertThrows(// catch both NPE and IAE
        RuntimeException.class, () -> JsonSchemaFactory.getInstance(null), "Calling getInstance(null) should throw an exception");
    }

    /**
     * Two successive calls with the same flag must return the *identical* factory
     * instance (reference equality). This test is parameterised to run for a few
     * different flags.
     */
    @ParameterizedTest(name = "[{index}] flag={0}")
    @MethodSource("provideCachingFlags")
    void getInstance_sameFlag_returnsSameInstance(VersionFlag flag) {
        JsonSchemaFactory f1 = JsonSchemaFactory.getInstance(flag);
        JsonSchemaFactory f2 = JsonSchemaFactory.getInstance(flag);
        assertSame(f1, f2, "Factory instances for the same flag (" + flag + ") must be cached and identical");
    }

    /**
     * Factories created for *different* version flags must be distinct objects.
     */
    @Test
    void getInstance_differentFlags_returnDifferentInstances() {
        JsonSchemaFactory v4Factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchemaFactory v6Factory = JsonSchemaFactory.getInstance(VersionFlag.V6);
        assertNotSame(v4Factory, v6Factory, "Factories for different version flags should not be the same instance");
    }

    /**
     * Concurrency test that ensures the caching mechanism returns the same
     * {@link JsonSchemaFactory} instance when many threads request it simultaneously.
     */
    @Test
    void concurrency_getInstanceReturnsSameFactoryAcrossThreads() throws Exception {
        final VersionFlag flag = VersionFlag.DRAFT_2020_12;
        final int threadCount = 20;
        final JsonSchemaFactory[] holder = new JsonSchemaFactory[1];
        final AtomicBoolean mismatch = new AtomicBoolean(false);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    // wait for the go‑signal
                    startLatch.await();
                    JsonSchemaFactory f = JsonSchemaFactory.getInstance(flag);
                    synchronized (holder) {
                        if (holder[0] == null) {
                            holder[0] = f;
                        } else if (holder[0] != f) {
                            mismatch.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }, "t-" + i).start();
        }
        // Release all threads at once
        startLatch.countDown();
        // Wait for all threads to finish (with a small timeout to avoid hanging tests)
        assertTrue(doneLatch.await(2, java.util.concurrent.TimeUnit.SECONDS), "Threads did not finish in time");
        assertFalse(mismatch.get(), "All threads should receive the exact same JsonSchemaFactory instance");
        assertNotNull(holder[0], "Factory instance should have been captured");
    }

    /**
     * Verify that an exception thrown inside the customizer {@link java.util.function.Consumer}
     * propagates unchanged from {@code getInstance}.
     */
    @Test
    void getInstance_customizerThrowsIllegalStateException_propagates() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> JsonSchemaFactory.getInstance(VersionFlag.V7, builder -> {
            throw new IllegalStateException("customizer failure");
        }));
        assertEquals("customizer failure", thrown.getMessage(), "Exception message should be preserved");
    }

    /**
     * Verify that a {@link NullPointerException} thrown from the customizer also propagates.
     */
    @Test
    void getInstance_customizerThrowsNullPointerException_propagates() {
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(VersionFlag.V7, builder -> {
            throw new NullPointerException("null inside customizer");
        }));
        assertEquals("null inside customizer", thrown.getMessage(), "Exception message should be preserved");
    }
}
