```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, SchemaValidatorsConfig)}.
 * <p>
 * The factory is created via {@link JsonSchemaFactory#getInstance(VersionFlag, java.util.function.Consumer)} with a
 * custom {@link SchemaLoaders.Builder} that supplies a minimal in‑memory schema. The test verifies that the
 * {@code getSchema} method returns a non‑null {@link JsonSchema} instance and that the schema node corresponds to the
 * supplied JSON.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    /** Factory under test. */
    private JsonSchemaFactory factory;

    /** Simple JSON schema used for the test. */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /** IRI used to identify the test schema. */
    private static final String TEST_SCHEMA_IRI = "https://example.com/simple-schema";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a factory that knows how to load the simple schema from the given IRI.
        Map<String, String> schemas = Collections.singletonMap(TEST_SCHEMA_IRI, SIMPLE_SCHEMA);
        this.factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                builder -> builder.schemaLoaders(sl -> sl.schemas(schemas)));
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
    void getSchema_shouldReturnParsedSchema() {
        // GIVEN – a SchemaLocation pointing to the test IRI and a default validators config
        SchemaLocation location = SchemaLocation.of(TEST_SCHEMA_IRI);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN – the factory creates a JsonSchema for the given location
        JsonSchema schema = factory.getSchema(location, config);

        // THEN – a non‑null JsonSchema is returned and its underlying JsonNode matches the source JSON
        assertNotNull(schema, "JsonSchema should not be null");
        // The JsonSchema implementation exposes the original JsonNode via getSchemaNode()
        // (this is part of the public API, not a private field).
        JsonNode node = schema.getSchemaNode();
        assertNotNull(node, "Underlying JsonNode should not be null");
        assertEquals("object", node.get("type").asText(), "Schema type should be 'object'");
    }
}
```