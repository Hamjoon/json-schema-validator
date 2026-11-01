```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#walk(JsonNode, boolean)}.
 *
 * <p>The {@code @BeforeEach} method creates a simple object schema that requires a
 * {@code name} property of type {@code string}.  Several test cases need a different
 * schema – they create their own {@link JsonSchema} instance locally, leaving the
 * shared {@code jsonSchema} untouched.</p>
 *
 * <p>All tests use the Jackson {@link ObjectMapper} to build {@link JsonNode}
 * instances from JSON literals.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_2_Test {

    /** shared factory for all tests */
    private static JsonSchemaFactory factory;

    /** fresh schema instance for each test (object with required "name") */
    private JsonSchema jsonSchema;

    /** Jackson mapper to create JsonNode instances */
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Default factory for Draft‑2020‑12.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal schema used by the generic tests.
        String schemaData = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }""";

        jsonSchema = factory.getSchema(schemaData);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
    }

    /* --------------------------------------------------------------------- */
    /*  Basic flag handling – already covered in the template but kept for   */
    /*  completeness and as a parameterized example.                        */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "walk(validate={0}) on valid object should succeed")
    @CsvSource({ "true", "false" })
    void walk_validObject_respectsValidateFlag(boolean validate) throws Exception {
        JsonNode node = mapper.readTree("{\"name\": \"John\", \"age\": 30}");
        ValidationResult result = jsonSchema.walk(node, validate);
        assertTrue(result.isValid(), "Result must be valid for a conforming JSON");
        assertEquals(0, result.getMessages().size(),
                "No validation messages should be produced for a valid JSON");
    }

    /* --------------------------------------------------------------------- */
    /*  Validation true with missing required property (schema requires age) */
    /* --------------------------------------------------------------------- */

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
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("required")
                        && m.getPath().equals("$")), "Message should mention a required property");
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
        assertEquals(0, result.getMessages().size(),
                "No messages should be produced when validation is disabled");
    }

    /* --------------------------------------------------------------------- */
    /*  Validation true with type mismatch (age should be integer)            */
    /* --------------------------------------------------------------------- */

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
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type")
                        && m.getPath().equals("$.age")), "Message should mention a type error for $.age");
    }

    /* --------------------------------------------------------------------- */
    /*  Array schema – valid and invalid cases                               */
    /* --------------------------------------------------------------------- */

    private JsonSchema arrayOfIntegersSchema() {
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "array",
                  "items": { "type": "integer" }
                }""";
        return factory.getSchema(schema);
    }

    @Test
    void walk_arrayValid_validateTrue_succeeds() throws Exception {
        JsonSchema arraySchema = arrayOfIntegersSchema();
        JsonNode node = mapper.readTree("[1, 2, 3]");
        ValidationResult result = arraySchema.walk(node, true);
        assertTrue(result.isValid(), "Array of integers should validate successfully");
        assertEquals(0, result.getMessages().size(),
                "No validation messages expected for a valid array");
    }

    @Test
    void walk_arrayInvalidItem_validateTrue_fails() throws Exception {
        JsonSchema arraySchema = arrayOfIntegersSchema();
        JsonNode node = mapper.readTree("[1, \"two\", 3]");
        ValidationResult result = arraySchema.walk(node, true);
        assertFalse(result.isValid(), "Array containing a non‑integer should fail");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type")
                        && m.getPath().equals("$[1]")), "Message should point to the offending index $[1]");
    }

    /* --------------------------------------------------------------------- */
    /*  Null node handling                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void walk_nullNode_validateTrue_fails() throws Exception {
        // The default schema expects an object; a JSON null should be rejected.
        JsonNode nullNode = mapper.readTree("null");
        ValidationResult result = jsonSchema.walk(nullNode, true);
        assertFalse(result.isValid(), "Null node should be invalid when validation is enabled");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("null")
                        || m.getMessage().toLowerCase().contains("type")),
                "Message should mention null or type error");
    }

    @Test
    void walk_nullNode_validateFalse_succeeds() throws Exception {
        JsonNode nullNode = mapper.readTree("null");
        ValidationResult result = jsonSchema.walk(nullNode, false);
        assertTrue(result.isValid(), "Null node should be considered valid when validation is disabled");
        assertEquals(0, result.getMessages().size(),
                "No messages should be produced when validation is disabled");
    }

    /* --------------------------------------------------------------------- */
    /*  Schema with optional properties only (no required fields)           */
    /* --------------------------------------------------------------------- */

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
        assertEquals(0, result.getMessages().size(),
                "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /*  Null JsonNode argument – expect NullPointerException                 */
    /* --------------------------------------------------------------------- */

    @Test
    void walk_nullJsonNodeArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(null, true),
                "Passing a null JsonNode should raise NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /*  Additional explicit tests for the original two scenarios (kept)      */
    /* --------------------------------------------------------------------- */

    /**
     * When {@code validate} is {@code true} the walk operation must perform validation.
     * The supplied JSON node violates the schema (numeric instead of string) and therefore
     * the returned {@link ValidationResult} should contain a validation message.
     */
    @Test
    void walk_shouldValidateWhenFlagIsTrue_andReportError() throws Exception {
        JsonNode node = mapper.readTree("{\"name\": 1}");
        ValidationResult result = jsonSchema.walk(node, true);
        assertFalse(result.isValid(), "Result should be marked as invalid");
        assertEquals(1, result.getMessages().size(),
                "Exactly one validation message is expected for the type mismatch");
    }

    /**
     * When {@code validate} is {@code false} the walk operation should skip validation.
     * The same invalid JSON node is used, but the result must be considered valid and
     * contain no messages.
     */
    @Test
    void walk_shouldSkipValidationWhenFlagIsFalse() throws Exception {
        JsonNode node = mapper.readTree("{\"name\": 1}");
        ValidationResult result = jsonSchema.walk(node, false);
        assertTrue(result.isValid(), "Result should be marked as valid when validation is disabled");
        assertEquals(0, result.getMessages().size(),
                "No validation messages should be produced when validation is disabled");
    }
}
```