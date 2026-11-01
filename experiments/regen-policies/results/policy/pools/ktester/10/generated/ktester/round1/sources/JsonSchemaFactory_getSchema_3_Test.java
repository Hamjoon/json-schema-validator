package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_3_Test {

    private static ObjectMapper mapper;

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Shared ObjectMapper to parse JSON literals used in the tests.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default factory for Draft‑07 (V7). This provides all required
        // default meta‑schemas and configuration.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    private static Stream<JsonNode> invalidNodeProvider() throws Exception {
        return Stream.of(// Array node
        mapper.readTree("[]"), // String node
        mapper.readTree("\"just a string\""), // Number node
        mapper.readTree("123"), // Null node (explicit JSON null)
        mapper.readTree("null"));
    }

    @Test
    void testGetSchemaReturnsValidSchemaAndValidates() throws Exception {
        // given: a simple object schema that requires a "name" property of type string.
        String schemaJson = "{" + "\"type\":\"object\"," + "\"properties\":{" + "    \"name\":{ \"type\":\"string\" }" + "}," + "\"required\":[\"name\"]" + "}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // when: the schema is created via the factory.
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        // then: the returned JsonSchema must not be null.
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        // Validate a correct instance – should produce no errors.
        String validInstanceJson = "{\"name\":\"John\"}";
        JsonNode validNode = mapper.readTree(validInstanceJson);
        Set<ValidationMessage> errorsValid = jsonSchema.validate(validNode);
        assertTrue(errorsValid.isEmpty(), "Valid instance should have no validation errors");
        // Validate an incorrect instance – missing required property "name".
        String invalidInstanceJson = "{}";
        JsonNode invalidNode = mapper.readTree(invalidInstanceJson);
        Set<ValidationMessage> errorsInvalid = jsonSchema.validate(invalidNode);
        assertFalse(errorsInvalid.isEmpty(), "Invalid instance should have validation errors");
        // The error set should contain a message about the missing required property "name".
        boolean containsMissingName = errorsInvalid.stream().anyMatch(msg -> msg.getMessage().contains("required") && msg.getMessage().contains("name"));
        assertTrue(containsMissingName, "Error messages should indicate missing required property 'name'");
    }

    @Test
    void testGetSchemaWithNullJsonNodeThrowsException() {
        // given: a null JsonNode.
        JsonNode nullNode = null;
        // when / then: invoking getSchema(null) must throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(nullNode));
    }

    /* --------------------------------------------------------------
     *  Basic functionality – creation and validation of a normal schema
     * -------------------------------------------------------------- */
    @Test
    void testGetSchemaCreatesValidObjectSchema() throws Exception {
        // A simple object schema requiring a "name" property of type string.
        String schemaJson = "{" + "\"type\":\"object\"," + "\"properties\":{" + "    \"name\":{ \"type\":\"string\" }" + "}," + "\"required\":[\"name\"]" + "}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "Factory must return a non‑null JsonSchema instance");
        // --- a valid instance -------------------------------------------------
        JsonNode valid = mapper.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> validErrors = jsonSchema.validate(valid);
        assertTrue(validErrors.isEmpty(), "A correct instance should produce no validation errors");
        // --- an invalid instance (missing required property) --------------------
        JsonNode invalid = mapper.readTree("{}");
        Set<ValidationMessage> invalidErrors = jsonSchema.validate(invalid);
        assertFalse(invalidErrors.isEmpty(), "Missing required property must yield validation errors");
        assertTrue(invalidErrors.stream().anyMatch(msg -> msg.getMessage().contains("required") && msg.getMessage().contains("name")), "Error message should mention the missing required property 'name'");
    }

    /* --------------------------------------------------------------
     *  Boolean schema – true (always valid)
     * -------------------------------------------------------------- */
    @Test
    void testBooleanTrueSchemaAcceptsAnything() throws Exception {
        // a JSON boolean true
        JsonNode trueNode = mapper.readTree("true");
        JsonSchema schema = factory.getSchema(trueNode);
        assertNotNull(schema, "Schema for boolean true must not be null");
        // Any JSON instance should be accepted – we test three different shapes.
        JsonNode obj = mapper.readTree("{\"any\":\"thing\"}");
        JsonNode arr = mapper.readTree("[1,2,3]");
        JsonNode primitive = mapper.readTree("\"string\"");
        assertAll(() -> assertTrue(schema.validate(obj).isEmpty(), "Object instance must be valid"), () -> assertTrue(schema.validate(arr).isEmpty(), "Array instance must be valid"), () -> assertTrue(schema.validate(primitive).isEmpty(), "Primitive instance must be valid"));
    }

    /* --------------------------------------------------------------
     *  Boolean schema – false (always invalid)
     * -------------------------------------------------------------- */
    @Test
    void testBooleanFalseSchemaRejectsEverything() throws Exception {
        // a JSON boolean false
        JsonNode falseNode = mapper.readTree("false");
        JsonSchema schema = factory.getSchema(falseNode);
        assertNotNull(schema, "Schema for boolean false must not be null");
        // Any JSON instance should produce at least one validation error.
        JsonNode any = mapper.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> errors = schema.validate(any);
        assertFalse(errors.isEmpty(), "Boolean false schema must reject any instance");
    }

    /* --------------------------------------------------------------
     *  $ref handling – internal reference resolution
     * -------------------------------------------------------------- */
    @Test
    void testRefResolutionInternalDefinition() throws Exception {
        // Schema contains an internal definition "foo" (type string) and a $ref to it.
        String schemaJson = "{" + "\"$ref\":\"#/definitions/foo\"," + "\"definitions\":{" + "    \"foo\":{ \"type\":\"string\" }" + "}" + "}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertNotNull(schema, "Schema with $ref must be created");
        // Valid string instance – should pass.
        JsonNode valid = mapper.readTree("\"hello\"");
        assertTrue(schema.validate(valid).isEmpty(), "String instance must satisfy the resolved schema");
        // Invalid number instance – should fail.
        JsonNode invalid = mapper.readTree("123");
        Set<ValidationMessage> errors = schema.validate(invalid);
        assertFalse(errors.isEmpty(), "Number instance must violate the resolved string schema");
    }

    /* --------------------------------------------------------------
     *  Explicit $schema declaration – draft‑07
     * -------------------------------------------------------------- */
    @Test
    void testExplicitDraft07MetaSchema() throws Exception {
        // The schema explicitly declares draft‑07 via $schema.
        String schemaJson = "{" + "\"$schema\":\"http://json-schema.org/draft-07/schema#\"," + "\"type\":\"null\"" + "}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertNotNull(schema, "Schema with explicit $schema must be created");
        // Verify that the meta‑schema associated with the JsonSchema is draft‑07.
        // The JsonSchema class exposes getMetaSchema().getUri() in the library.
        String metaUri = schema.getMetaSchema().getUri();
        assertTrue(metaUri.contains("draft-07"), "Meta‑schema URI should contain 'draft-07' for the explicit declaration");
    }

    /* --------------------------------------------------------------
     *  Unknown $schema URI – should fallback to the default meta‑schema
     * -------------------------------------------------------------- */
    @Test
    void testUnknownSchemaUriFallsBackToDefault() throws Exception {
        String schemaJson = "{" + "\"$schema\":\"http://example.com/unknown/schema#\"," + "\"type\":\"object\"" + "}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        assertNotNull(schema, "Schema with unknown $schema must still be created");
        // The meta‑schema should be the factory's default (draft‑07 for this factory).
        String metaUri = schema.getMetaSchema().getUri();
        assertTrue(metaUri.contains("draft-07"), "When $schema cannot be resolved, the factory should fall back to the default meta‑schema (draft‑07)");
    }

    /* --------------------------------------------------------------
     *  Invalid top‑level node types – array, string, number, null
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => node={0}")
    @MethodSource("invalidNodeProvider")
    void testInvalidTopLevelNodeTypes(JsonNode node) {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(node), "Factory must reject unsupported top‑level schema node types");
    }
}
