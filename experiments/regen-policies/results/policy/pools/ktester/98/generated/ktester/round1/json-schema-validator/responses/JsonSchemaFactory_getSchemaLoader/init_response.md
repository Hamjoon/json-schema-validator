```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchemaLoader()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The {@code SchemaLoader} returned by the factory is never {@code null}.</li>
 *   <li>Repeated calls return the same instance (factory‑level singleton).</li>
 *   <li>A custom {@code SchemaLoader} created via the builder can actually load a schema
 *       that was supplied through {@code SchemaLoaders.Builder}.</li>
 * </ul>
 *
 * No private members of {@code JsonSchemaFactory} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLoader_Test {

    /** Factory used in the tests. */
    private JsonSchemaFactory factory;

    /** Simple JSON schema used for the custom loader test. */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /** URI under which the simple schema is registered. */
    private static final String SIMPLE_SCHEMA_URI = "https://example.com/simple-schema";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a factory that registers a single schema (SIMPLE_SCHEMA) under the
         * URI SIMPLE_SCHEMA_URI.  The builder API mirrors the usage shown in the
         * existing JsonSchemaFactoryTest.
         */
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(SIMPLE_SCHEMA_URI, SIMPLE_SCHEMA)
                        )
                )
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void getSchemaLoader_ShouldReturnNonNullInstance() {
        SchemaLoader loader = factory.getSchemaLoader();
        assertNotNull(loader, "SchemaLoader must not be null");
    }

    @Test
    void getSchemaLoader_ShouldReturnSameInstanceOnRepeatedCalls() {
        SchemaLoader first = factory.getSchemaLoader();
        SchemaLoader second = factory.getSchemaLoader();
        assertSame(first, second, "Repeated calls must return the same SchemaLoader instance");
    }

    @Test
    void getSchemaLoader_ShouldLoadSchemaRegisteredViaBuilder() {
        // Arrange
        SchemaLoader loader = factory.getSchemaLoader();
        SchemaLocation location = new SchemaLocation(SIMPLE_SCHEMA_URI);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // Act
        JsonSchema loadedSchema = loader.loadSchema(location, config);

        // Assert
        assertNotNull(loadedSchema, "The loaded JsonSchema must not be null");
        // The schema node should represent an object type – we verify this via the public API.
        assertEquals("object", loadedSchema.getSchemaNode().get("type").asText(),
                "Loaded schema should have type 'object'");
    }
}
```