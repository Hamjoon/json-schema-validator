```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI, JsonNode, SchemaValidatorsConfig)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_16_Test {

    /** Factory instance used by all test methods. */
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default 2020‑12 draft instance – it provides a complete set of meta‑schemas.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that {@code getSchema(URI, JsonNode, SchemaValidatorsConfig)} returns a non‑null
     * {@link JsonSchema} when supplied with a valid URI, a well‑formed schema node and a
     * default {@link SchemaValidatorsConfig}.
     */
    @Test
    void getSchema_withUriNodeAndConfig_returnsJsonSchema() throws Exception {
        // ----- given ---------------------------------------------------------
        URI schemaUri = URI.create("http://example.com/simple-schema");
        String schemaContent = """
                {
                  "$id": "http://example.com/simple-schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }
                """;

        // Parse the JSON string into a JsonNode using the factory's public helper.
        JsonNode schemaNode = factory.readTree(schemaContent, InputFormat.JSON);

        // Use the builder provided by SchemaValidatorsConfig to obtain a default config.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ----- when ----------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, config);

        // ----- then ----------------------------------------------------------
        assertNotNull(jsonSchema, "The returned JsonSchema should not be null");
        // The factory creates a JsonSchema that internally holds the supplied node;
        // we can at least verify that the node we passed is the same as the one
        // stored inside the created schema.
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The schema node inside the created JsonSchema should match the supplied node");
    }

    /**
     * Verify that the method works correctly when the {@link SchemaValidatorsConfig}
     * argument is {@code null}. The factory should fall back to its default configuration.
     */
    @Test
    void getSchema_withNullConfig_usesDefaultConfig() throws Exception {
        // ----- given ---------------------------------------------------------
        URI schemaUri = URI.create("http://example.com/empty-schema");
        String schemaContent = "{ \"type\": \"object\" }";
        JsonNode schemaNode = factory.readTree(schemaContent, InputFormat.JSON);

        // ----- when ----------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, null);

        // ----- then ----------------------------------------------------------
        assertNotNull(jsonSchema, "JsonSchema should be created even when config is null");
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The schema node stored in JsonSchema must be the one supplied");
    }
}
```