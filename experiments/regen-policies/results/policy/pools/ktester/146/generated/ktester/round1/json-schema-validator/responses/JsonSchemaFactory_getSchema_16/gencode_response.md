```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI, JsonNode, SchemaValidatorsConfig)}.
 *
 * The test cases cover normal operation, null handling, various URI edge cases,
 * encoded URIs, invalid URIs and non‑object schema nodes as described in the
 * supplied JSON test matrix.
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

    /* --------------------------------------------------------------------- *
     *  Normal behavior
     * --------------------------------------------------------------------- */

    @Test
    void getSchema_withUriNodeAndConfig_returnsJsonSchema() throws Exception {
        // given
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
        JsonNode schemaNode = factory.readTree(schemaContent, InputFormat.JSON);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, config);

        // then
        assertNotNull(jsonSchema, "The returned JsonSchema should not be null");
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The schema node inside the created JsonSchema should match the supplied node");
        assertEquals(schemaUri.toString(), jsonSchema.getSchemaLocation().toString(),
                "The schema location should reflect the supplied URI");
    }

    /* --------------------------------------------------------------------- *
     *  Null handling
     * --------------------------------------------------------------------- */

    @Test
    void getSchema_withNullConfig_usesDefaultConfig() throws Exception {
        // given
        URI schemaUri = URI.create("http://example.com/empty-schema");
        JsonNode schemaNode = factory.readTree("{\"type\":\"object\"}", InputFormat.JSON);

        // when
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, null);

        // then
        assertNotNull(jsonSchema, "JsonSchema should be created even when config is null");
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The schema node stored in JsonSchema must be the one supplied");
        assertEquals(schemaUri.toString(), jsonSchema.getSchemaLocation().toString(),
                "Location must still reflect the supplied URI");
    }

    @Test
    void getSchema_withNullUri_throwsNPE() throws Exception {
        // given
        URI schemaUri = null;
        JsonNode schemaNode = factory.readTree("{\"type\":\"string\"}", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when / then
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(schemaUri, schemaNode, config),
                "Calling getSchema with a null URI should throw NullPointerException");
    }

    @Test
    void getSchema_withNullJsonNode_throwsNPE() throws Exception {
        // given
        URI schemaUri = URI.create("http://example.com/schema.json");
        JsonNode schemaNode = null;
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when / then
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(schemaUri, schemaNode, config),
                "Calling getSchema with a null JsonNode should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  Edge cases for URI
     * --------------------------------------------------------------------- */

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
        assertEquals("", jsonSchema.getSchemaLocation().toString(),
                "Empty URI should result in an empty schema location string");
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
        assertEquals(schemaUri.toString(), jsonSchema.getSchemaLocation().toString(),
                "Location should reflect the supplied file URI even if the file does not exist");
        assertEquals(schemaNode, jsonSchema.getSchemaNode());
    }

    /* --------------------------------------------------------------------- *
     *  Encoded URI handling
     * --------------------------------------------------------------------- */

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
        assertEquals(schemaUri.toString(), jsonSchema.getSchemaLocation().toString(),
                "Encoded characters must be preserved in the stored location");
        assertEquals(schemaNode, jsonSchema.getSchemaNode());
    }

    /* --------------------------------------------------------------------- *
     *  Invalid URI handling
     * --------------------------------------------------------------------- */

    @Test
    void getSchema_withRelativeUri_throwsIllegalArgumentException() throws Exception {
        // given
        URI schemaUri = new URI("relative/path/schema.json"); // relative, no scheme
        JsonNode schemaNode = factory.readTree("{\"type\":\"object\"}", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(schemaUri, schemaNode, config),
                "Relative URIs should be rejected with IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     *  Non‑object schema node
     * --------------------------------------------------------------------- */

    @Test
    void getSchema_withNonObjectNode_throwsIllegalArgumentException() throws Exception {
        // given
        URI schemaUri = URI.create("http://example.com/schema.json");
        // JSON string node (not an object)
        JsonNode schemaNode = factory.readTree("\"just a string\"", InputFormat.JSON);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(schemaUri, schemaNode, config),
                "A non‑object JsonNode should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     *  Helper for creating ObjectMapper (if needed elsewhere)
     * --------------------------------------------------------------------- */

    private static ObjectMapper mapper() {
        return new ObjectMapper();
    }
}
```