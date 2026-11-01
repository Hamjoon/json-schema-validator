```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#loadSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * <p>The tests verify the caching behaviour of {@code loadSchema} by exercising the
 * public {@code getSchema(String)} API, which internally delegates to {@code loadSchema}.
 * Two factories are created – one with schema‑caching enabled and one with it disabled –
 * and the identity of the returned {@link JsonSchema} objects is asserted.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadSchema_Test {

    /** Factory with schema‑caching enabled. */
    private JsonSchemaFactory cachingFactory;

    /** Factory with schema‑caching disabled. */
    private JsonSchemaFactory nonCachingFactory;

    /** A minimal JSON schema used for the tests. */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * The builder API of JsonSchemaFactory allows us to turn the cache on/off.
         * The exact method name is not part of the public contract we are testing,
         * but the library provides an {@code enableSchemaCache(boolean)} method.
         * If the method name changes, the test will need a small adaptation.
         */
        cachingFactory = JsonSchemaFactory.builder()
                .enableSchemaCache(true)               // enable the internal cache
                .build();

        nonCachingFactory = JsonSchemaFactory.builder()
                .enableSchemaCache(false)              // disable the internal cache
                .build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – factories do not hold external resources.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void loadSchema_withCache_returnsSameInstance() {
        // First load – creates a new JsonSchema instance.
        JsonSchema first = cachingFactory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(first, "First schema load should not be null");

        // Second load – should retrieve the cached instance.
        JsonSchema second = cachingFactory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(second, "Second schema load should not be null");

        // Because caching is enabled, both references must point to the same object.
        assertSame(first, second, "Schemas should be identical when caching is enabled");
    }

    @Test
    void loadSchema_withoutCache_returnsDifferentInstances() {
        // First load – creates a new JsonSchema instance.
        JsonSchema first = nonCachingFactory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(first, "First schema load should not be null");

        // Second load – with caching disabled a new instance must be created.
        JsonSchema second = nonCachingFactory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(second, "Second schema load should not be null");

        // The two instances must be different objects.
        assertNotSame(first, second, "Schemas should be different when caching is disabled");
    }
}
```