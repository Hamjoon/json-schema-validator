package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    /**
     * Factory under test – created once per test method.
     */
    private JsonSchemaFactory factory;

    /**
     * A minimal JSON schema used in the tests.
     */
    private JsonNode simpleSchemaNode;

    /**
     * A mock {@link SchemaLocation} – the concrete implementation is not needed for the behaviour under test.
     */
    @Mock
    private SchemaLocation mockLocation;

    /**
     * A reusable {@link ObjectMapper} for building {@link JsonNode}s.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Obtain a factory instance that is fully functional but does not load any external schemas.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No customisation required for these simple tests.
        });
        // Prepare a tiny, valid JSON schema document.
        ObjectMapper mapper = new ObjectMapper();
        simpleSchemaNode = mapper.readTree("{\"type\":\"object\"}");
    }

    /**
     * Convenience factory for {@link SchemaLocation}.
     */
    private static SchemaLocation location(String uri) {
        return new SchemaLocation(uri);
    }

    /**
     * Parses a JSON string into a {@link JsonNode}.
     */
    private static JsonNode node(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON used in test", e);
        }
    }

    private static Stream<Arguments> booleanSchemaProvider() {
        return Stream.of(Arguments.of("true", node("true")), Arguments.of("false", node("false")));
    }

    @Test
    @DisplayName("getSchema(SchemaLocation, JsonNode) returns a JsonSchema when inputs are valid")
    void getSchema_returnsJsonSchemaForValidInputs() {
        // GIVEN – a mock SchemaLocation and a valid JsonNode (prepared in @BeforeEach)
        // WHEN
        JsonSchema result = factory.getSchema(mockLocation, simpleSchemaNode);
        // THEN
        assertNotNull(result, "The factory should return a non‑null JsonSchema instance");
        // The JsonSchema API provides a method to retrieve the original schema node.
        // If that method is not present in a particular version, the test still validates
        // that the returned object is of the expected type.
        assertTrue(result instanceof JsonSchema, "Result must be an instance of JsonSchema");
    }

    @Test
    @DisplayName("getSchema throws NullPointerException when JsonNode argument is null")
    void getSchema_throwsWhenJsonNodeIsNull() {
        // GIVEN – a mock SchemaLocation and a null JsonNode
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> factory.getSchema(mockLocation, null), "Calling getSchema with a null JsonNode should raise NullPointerException");
    }

    @Test
    @DisplayName("Valid inputs – returns a non‑null JsonSchema")
    void validInputs_returnsJsonSchema() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"type\":\"object\"}");
        JsonSchema result = factory.getSchema(loc, schemaNode);
        assertNotNull(result, "Factory should return a non‑null JsonSchema");
        assertTrue(result instanceof JsonSchema, "Result must be an instance of JsonSchema");
    }

    @Test
    @DisplayName("Null schemaUri – throws NullPointerException")
    void nullSchemaUri_throwsNPE() {
        JsonNode schemaNode = node("{\"type\":\"string\"}");
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, schemaNode), "Calling getSchema with a null SchemaLocation should raise NullPointerException");
    }

    @Test
    @DisplayName("Null jsonNode – throws NullPointerException")
    void nullJsonNode_throwsNPE() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        assertThrows(NullPointerException.class, () -> factory.getSchema(loc, null), "Calling getSchema with a null JsonNode should raise NullPointerException");
    }

    @Test
    @DisplayName("Both parameters null – throws NullPointerException")
    void bothNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, null), "Calling getSchema with both arguments null should raise NullPointerException");
    }

    @Test
    @DisplayName("Relative schema location – resolves base URI and returns JsonSchema")
    void relativeSchemaLocation_returnsJsonSchema() {
        SchemaLocation loc = location("relative/schema.json");
        JsonNode schemaNode = node("{\"$id\":\"http://example.com/relative/schema.json\",\"type\":\"array\"}");
        JsonSchema result = factory.getSchema(loc, schemaNode);
        assertNotNull(result, "Result must not be null for a relative location");
        // The returned schema should have the supplied $id as its base URI.
        assertEquals("http://example.com/relative/schema.json", result.getSchemaNode().get("$id").asText(), "Base URI should be resolved from the supplied $id");
    }

    @Test
    @DisplayName("Non‑existent schema location – expects RuntimeException")
    void nonExistentLocation_throwsRuntimeException() {
        SchemaLocation loc = location("classpath:/does-not-exist.json");
        JsonNode schemaNode = node("{\"type\":\"null\"}");
        // The factory does not read the resource when a JsonNode is supplied,
        // but many implementations still validate the base location.
        assertThrows(RuntimeException.class, () -> factory.getSchema(loc, schemaNode), "A non‑existent schema location should cause a RuntimeException (or subclass)");
    }

    @ParameterizedTest(name = "Boolean schema node {0} – returns JsonSchema")
    @MethodSource("booleanSchemaProvider")
    void booleanSchemaNode_returnsJsonSchema(String description, JsonNode booleanNode) {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonSchema result = factory.getSchema(loc, booleanNode);
        assertNotNull(result, "Result must not be null for boolean schema node");
        assertTrue(result instanceof JsonSchema);
    }

    @Test
    @DisplayName("Schema node with unknown keyword – expects JsonSchemaException")
    void unknownKeyword_throwsJsonSchemaException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"type\":\"object\",\"unknownKeyword\":123}");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, schemaNode), "Schema containing an unknown keyword should raise JsonSchemaException");
    }

    @Test
    @DisplayName("SchemaUri with unsupported scheme – expects SchemaLoadingException")
    void unsupportedScheme_throwsSchemaLoadingException() {
        SchemaLocation loc = location("ftp://example.com/schema.json");
        JsonNode schemaNode = node("{\"type\":\"string\"}");
        assertThrows(SchemaLoadingException.class, () -> factory.getSchema(loc, schemaNode), "Unsupported URI scheme should raise SchemaLoadingException");
    }

    @Test
    @DisplayName("SchemaUri with empty string – expects IllegalArgumentException")
    void emptySchemaUri_throwsIllegalArgumentException() {
        SchemaLocation loc = location("");
        JsonNode schemaNode = node("{\"type\":\"number\"}");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, schemaNode), "Empty schema URI should raise IllegalArgumentException");
    }

    @Test
    @DisplayName("Valid location but mismatched jsonNode – returns JsonSchema reflecting jsonNode")
    void mismatchedJsonNode_ignoresLocationContent() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        JsonSchema result = factory.getSchema(loc, schemaNode);
        assertNotNull(result);
        // Verify that the schema node inside the result matches the supplied node.
        assertEquals("array", result.getSchemaNode().get("type").asText(), "Resulting schema should reflect the supplied jsonNode, not the resource at the location");
    }

    @Test
    @DisplayName("$ref to missing external schema – expects SchemaLoadingException")
    void refToMissingExternalSchema_throwsSchemaLoadingException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"$ref\":\"nonexistent.json\"}");
        assertThrows(SchemaLoadingException.class, () -> factory.getSchema(loc, schemaNode), "Missing external $ref should raise SchemaLoadingException");
    }

    @Test
    @DisplayName("JsonNode with numeric root – expects JsonSchemaException")
    void numericRoot_throwsJsonSchemaException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("123");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, schemaNode), "Numeric root node is not a valid JSON Schema and should raise JsonSchemaException");
    }

    @Test
    @DisplayName("Malformed schemaUri – expects IllegalArgumentException")
    void malformedSchemaUri_throwsIllegalArgumentException() {
        SchemaLocation loc = location("http://[invalid");
        JsonNode schemaNode = node("{\"type\":\"object\"}");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, schemaNode), "Malformed URI should raise IllegalArgumentException");
    }

    @Test
    @DisplayName("Invalid $id in jsonNode – expects IllegalArgumentException")
    void invalidId_throwsIllegalArgumentException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"$id\":\"http://[invalid\",\"type\":\"object\"}");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, schemaNode), "Invalid $id URI should raise IllegalArgumentException");
    }

    @Test
    @DisplayName("$ref with unsupported scheme – expects SchemaLoadingException")
    void refWithUnsupportedScheme_throwsSchemaLoadingException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"$ref\":\"ftp://example.com/other.json\"}");
        assertThrows(SchemaLoadingException.class, () -> factory.getSchema(loc, schemaNode), "Unsupported scheme in $ref should raise SchemaLoadingException");
    }

    @Test
    @DisplayName("$ref with malformed URI – expects IllegalArgumentException")
    void refWithMalformedUri_throwsIllegalArgumentException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"$ref\":\"http://[invalid\"}");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, schemaNode), "Malformed URI in $ref should raise IllegalArgumentException");
    }

    @Test
    @DisplayName("Circular $ref reference – expects JsonSchemaException")
    void circularRef_throwsJsonSchemaException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("{\"$ref\":\"#\"}");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, schemaNode), "Circular $ref should raise JsonSchemaException");
    }

    @Test
    @DisplayName("Null root JsonNode – expects JsonSchemaException")
    void nullRootNode_throwsJsonSchemaException() {
        SchemaLocation loc = location("classpath:/valid-schema.json");
        JsonNode schemaNode = node("null");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, schemaNode), "Null root node is not a valid schema and should raise JsonSchemaException");
    }
}
