```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The factory instance is created and not {@code null}.</li>
 *   <li>Calling {@code getInstance} repeatedly with the same {@link VersionFlag}
 *       returns the same factory (singleton per version flag).</li>
 *   <li>A customizer supplied to the overloaded {@code getInstance} method can
 *       influence the behaviour of the produced factory (e.g. adding a dummy
 *       schema loader).</li>
 * </ul>
 *
 * No private members of {@link JsonSchemaFactory} are accessed; the public API
 * is used exclusively.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_Test {

    /** A simple dummy schema used for the customizer test. */
    private static final String DUMMY_SCHEMA_ID = "https://example.com/dummy-schema";
    private static final String DUMMY_SCHEMA_CONTENT = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\" }";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that {@code getInstance} returns a non‑null factory for a supported
     * version flag.
     */
    @Test
    void getInstance_returnsNonNullFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory should not be null for a valid VersionFlag");
    }

    /**
     * Verify that two calls with the same {@link VersionFlag} return the same
     * factory instance (the method is expected to cache per version flag).
     */
    @Test
    void getInstance_sameVersion_returnsSameInstance() {
        JsonSchemaFactory first = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchemaFactory second = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertSame(first, second, "Factory instances for the same VersionFlag should be identical (cached)");
    }

    /**
     * Verify that a customizer supplied to the overloaded {@code getInstance}
     * method can affect the produced factory.  In this test we add a single
     * schema loader that maps a dummy schema URI to a JSON string.  The factory
     * should then be able to resolve that schema via {@code getSchema}.
     */
    @Test
    void getInstance_withCustomizer_appliesCustomization() {
        // Build a factory while adding a custom schema loader.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(DUMMY_SCHEMA_ID, DUMMY_SCHEMA_CONTENT))));

        assertNotNull(factory, "Factory created with customizer must not be null");

        // Use the factory to obtain a JsonSchema for the dummy URI.
        JsonSchema schema = factory.getSchema(SchemaLocation.of(DUMMY_SCHEMA_ID));
        assertNotNull(schema, "Factory should resolve the dummy schema added via customizer");
    }

    /**
     * Verify that passing {@code null} as the version flag results in a
     * {@link NullPointerException}.  The static method is expected to
     * validate its arguments.
     */
    @Test
    void getInstance_nullVersionFlag_throwsException() {
        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.getInstance(null),
                "Calling getInstance with a null VersionFlag should throw NullPointerException");
    }
}
```