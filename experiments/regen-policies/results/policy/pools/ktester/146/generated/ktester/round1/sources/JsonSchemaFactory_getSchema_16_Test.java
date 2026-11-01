package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_16_Test {

    /**
     * Factory instance used by all test methods.
     */
    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default 2020‑12 draft instance – it provides a complete set of meta‑schemas.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper();
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
        assertEquals(schemaNode, jsonSchema.getSchemaNode(), "The schema node inside the created JsonSchema should match the supplied node");
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
        assertEquals(schemaNode, jsonSchema.getSchemaNode(), "The schema node stored in JsonSchema must be the one supplied");
    }

    @Test
    void getSchema_withNullUri_throwsNPE() throws Exception {
        // given
        URI schemaUri = null;
        JsonNode schemaNode = factory.readTree("{\"type\":\"string\"}", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when / then
        assertThrows(NullPointerException.class, () -> factory.getSchema(schemaUri, schemaNode, config), "Calling getSchema with a null URI should throw NullPointerException");
    }

    @Test
    void getSchema_withNullJsonNode_throwsNPE() throws Exception {
        // given
        URI schemaUri = URI.create("http://example.com/schema.json");
        JsonNode schemaNode = null;
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when / then
        assertThrows(NullPointerException.class, () -> factory.getSchema(schemaUri, schemaNode, config), "Calling getSchema with a null JsonNode should throw NullPointerException");
    }

    @Test
    void getSchema_withEmptyUri_returnsSchemaWithEmptyLocation() throws Exception {
        // given
        URI schemaUri = new URI("");
        JsonNode schemaNode = factory.readTree("{\"type\":\"array\"}", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, config);
        // then
        assertNotNull(jsonSchema);
        assertEquals("", jsonSchema.getSchemaLocation().toString(), "Empty URI should result in an empty schema location string");
        assertEquals(schemaNode, jsonSchema.getSchemaNode());
    }

    @Test
    void getSchema_withFileUri_ignoresResourceExistence() throws Exception {
        // given
        URI schemaUri = new URI("file:/nonexistent/path/schema.json");
        JsonNode schemaNode = factory.readTree("{\"type\":\"boolean\"}", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, config);
        // then
        assertNotNull(jsonSchema);
        assertEquals(schemaUri.toString(), jsonSchema.getSchemaLocation().toString(), "Location should reflect the supplied file URI even if the file does not exist");
        assertEquals(schemaNode, jsonSchema.getSchemaNode());
    }

    @Test
    void getSchema_withEncodedUri_preservesEncoding() throws Exception {
        // given
        URI schemaUri = new URI("http://example.com/schema%20with%20spaces.json");
        JsonNode schemaNode = factory.readTree("{\"type\":\"boolean\"}", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, config);
        // then
        assertNotNull(jsonSchema);
        assertEquals(schemaUri.toString(), jsonSchema.getSchemaLocation().toString(), "Encoded characters must be preserved in the stored location");
        assertEquals(schemaNode, jsonSchema.getSchemaNode());
    }

    @Test
    void getSchema_withRelativeUri_throwsIllegalArgumentException() throws Exception {
        // given
        // relative, no scheme
        URI schemaUri = new URI("relative/path/schema.json");
        JsonNode schemaNode = factory.readTree("{\"type\":\"object\"}", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaUri, schemaNode, config), "Relative URIs should be rejected with IllegalArgumentException");
    }

    @Test
    void getSchema_withNonObjectNode_throwsIllegalArgumentException() throws Exception {
        // given
        URI schemaUri = URI.create("http://example.com/schema.json");
        // JSON string node (not an object)
        JsonNode schemaNode = factory.readTree("\"just a string\"", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaUri, schemaNode, config), "A non‑object JsonNode should cause IllegalArgumentException");
    }
}
