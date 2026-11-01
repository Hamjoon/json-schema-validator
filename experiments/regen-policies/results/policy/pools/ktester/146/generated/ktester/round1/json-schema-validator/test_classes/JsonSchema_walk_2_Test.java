package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_2_Test {

    /**
     * shared factory for all tests
     */
    private static JsonSchemaFactory factory;

    /**
     * fresh schema instance for each test
     */
    private JsonSchema jsonSchema;

    /**
     * Jackson mapper to create JsonNode instances
     */
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory that can resolve schemas from the class‑path / URLs.
        // The simple schema used in the tests does not need external references,
        // therefore the default factory is sufficient.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal schema that requires an object with a required string property "name".
        String schemaData = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }""";
        // No special configuration – the default works for this simple schema.
        jsonSchema = factory.getSchema(schemaData);
    }

    private JsonSchema arrayOfIntegersSchema() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "array",
              "items": { "type": "integer" }
            }""";
        return factory.getSchema(schema);
    }

    /**
     * When {@code validate} is {@code true} the walk operation must perform validation.
     * The supplied JSON node violates the schema (numeric instead of string) and therefore
     * the returned {@link ValidationResult} should contain a validation message.
     */
    @Test
    void walk_shouldValidateWhenFlagIsTrue_andReportError() throws Exception {
        // given – a JSON node that does NOT satisfy the schema
        JsonNode node = mapper.readTree("{\"name\": 1}");
        // when – walk with validation enabled
        ValidationResult result = jsonSchema.walk(node, true);
        // then – the result must be invalid and contain exactly one message
        assertFalse(result.isValid(), "Result should be marked as invalid");
        assertEquals(1, result.getMessages().size(), "Exactly one validation message is expected for the type mismatch");
    }

    /**
     * When {@code validate} is {@code false} the walk operation should skip validation.
     * The same invalid JSON node is used, but the result must be considered valid and
     * contain no messages.
     */
    @Test
    void walk_shouldSkipValidationWhenFlagIsFalse() throws Exception {
        // given – a JSON node that would fail validation
        JsonNode node = mapper.readTree("{\"name\": 1}");
        // when – walk with validation disabled
        ValidationResult result = jsonSchema.walk(node, false);
        // then – the walk result is considered valid and carries no messages
        assertTrue(result.isValid(), "Result should be marked as valid when validation is disabled");
        assertEquals(0, result.getMessages().size(), "No validation messages should be produced when validation is disabled");
    }

    @ParameterizedTest(name = "walk(validate={0}) on valid object should succeed")
    @CsvSource({ "true", "false" })
    void walk_validObject_respectsValidateFlag(boolean validate) throws Exception {
        JsonNode node = mapper.readTree("{\"name\": \"John\", \"age\": 30}");
        ValidationResult result = jsonSchema.walk(node, validate);
        assertTrue(result.isValid(), "Result must be valid for a conforming JSON");
        assertEquals(0, result.getMessages().size(), "No validation messages should be produced for a valid JSON");
    }

    @Test
    void walk_missingRequiredProperty_validateTrue_fails() throws Exception {
        // Schema that requires an integer property "age".
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": [ "age" ]
            }""";
        JsonSchema schemaWithAge = factory.getSchema(schema);
        // Node missing the required "age".
        JsonNode node = mapper.readTree("{ \"name\": \"John\" }");
        ValidationResult result = schemaWithAge.walk(node, true);
        assertFalse(result.isValid(), "Result must be invalid when a required property is missing");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("required") && m.getPath().equals("$")), "Message should mention a required property");
    }

    @Test
    void walk_missingRequiredProperty_validateFalse_succeeds() throws Exception {
        // Same schema as above.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": [ "age" ]
            }""";
        JsonSchema schemaWithAge = factory.getSchema(schema);
        JsonNode node = mapper.readTree("{ \"name\": \"John\" }");
        ValidationResult result = schemaWithAge.walk(node, false);
        assertTrue(result.isValid(), "Result must be valid when validation is disabled");
        assertEquals(0, result.getMessages().size(), "No messages should be produced when validation is disabled");
    }

    @Test
    void walk_typeMismatch_validateTrue_fails() throws Exception {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": [ "age" ]
            }""";
        JsonSchema schemaWithAge = factory.getSchema(schema);
        // Provide a string where an integer is expected.
        JsonNode node = mapper.readTree("{ \"age\": \"thirty\" }");
        ValidationResult result = schemaWithAge.walk(node, true);
        assertFalse(result.isValid(), "Result must be invalid due to type mismatch");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type") && m.getPath().equals("$.age")), "Message should mention a type error for $.age");
    }

    @Test
    void walk_arrayValid_validateTrue_succeeds() throws Exception {
        JsonSchema arraySchema = arrayOfIntegersSchema();
        JsonNode node = mapper.readTree("[1, 2, 3]");
        ValidationResult result = arraySchema.walk(node, true);
        assertTrue(result.isValid(), "Array of integers should validate successfully");
        assertEquals(0, result.getMessages().size(), "No validation messages expected for a valid array");
    }

    @Test
    void walk_arrayInvalidItem_validateTrue_fails() throws Exception {
        JsonSchema arraySchema = arrayOfIntegersSchema();
        JsonNode node = mapper.readTree("[1, \"two\", 3]");
        ValidationResult result = arraySchema.walk(node, true);
        assertFalse(result.isValid(), "Array containing a non‑integer should fail");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type") && m.getPath().equals("$[1]")), "Message should point to the offending index $[1]");
    }

    @Test
    void walk_nullNode_validateTrue_fails() throws Exception {
        // The default schema expects an object; a JSON null should be rejected.
        JsonNode nullNode = mapper.readTree("null");
        ValidationResult result = jsonSchema.walk(nullNode, true);
        assertFalse(result.isValid(), "Null node should be invalid when validation is enabled");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("null") || m.getMessage().toLowerCase().contains("type")), "Message should mention null or type error");
    }

    @Test
    void walk_nullNode_validateFalse_succeeds() throws Exception {
        JsonNode nullNode = mapper.readTree("null");
        ValidationResult result = jsonSchema.walk(nullNode, false);
        assertTrue(result.isValid(), "Null node should be considered valid when validation is disabled");
        assertEquals(0, result.getMessages().size(), "No messages should be produced when validation is disabled");
    }

    @Test
    void walk_emptyObject_noRequiredProperties_validateTrue_succeeds() throws Exception {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema optionalSchema = factory.getSchema(schema);
        JsonNode emptyObject = mapper.readTree("{}");
        ValidationResult result = optionalSchema.walk(emptyObject, true);
        assertTrue(result.isValid(), "Empty object should be valid when there are no required fields");
        assertEquals(0, result.getMessages().size(), "No validation messages expected");
    }

    @Test
    void walk_nullJsonNodeArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(null, true), "Passing a null JsonNode should raise NullPointerException");
    }
}
