package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.input.InputFormat;
import com.networknt.schema.walk.WalkListenerRunner;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_3_Test {

    /**
     * The schema under test – a simple object schema.
     */
    private static JsonSchema jsonSchema;

    /**
     * A JSON document that conforms to the schema.
     */
    private static final String VALID_JSON = "{\"name\":\"John\"}";

    /**
     * A JSON document that violates the schema (not an object).
     */
    private static final String INVALID_JSON = "[1,2,3]";

    @BeforeAll
    static void setupBeforeAll() {
        // Build a JsonSchemaFactory for Draft‑07 (the default in the library)
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        // The schema we will use for the walk tests – a very small object schema.
        String schemaStr = "{\"type\":\"object\"}";
        jsonSchema = factory.getSchema(schemaStr);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Explicitly release the reference – not strictly required but keeps the
        // test class tidy.
        jsonSchema = null;
    }

    /**
     * Verify that {@code walk} with {@code validate = false} returns a
     * {@link ValidationResult} whose {@code isValid()} method reports {@code true}
     * even when the JSON document does not conform to the schema.  When validation
     * is disabled the walk operation should simply traverse the document.
     */
    @Test
    void walkWithoutValidationShouldReturnValidResult() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        // WHEN
        ValidationResult result = jsonSchema.walk(INVALID_JSON, format, false);
        // THEN
        assertNotNull(result, "walk should never return null");
        assertTrue(result.isValid(), "When validation is disabled the result should be reported as valid");
        // No validation messages are expected because validation was turned off.
        assertTrue(result.getMessages().isEmpty(), "No validation messages should be produced when validation is disabled");
    }

    /**
     * Verify that {@code walk} with {@code validate = true} returns a
     * {@link ValidationResult} that reflects schema violations.
     */
    @Test
    void walkWithValidationShouldDetectSchemaViolations() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        // WHEN
        ValidationResult result = jsonSchema.walk(INVALID_JSON, format, true);
        // THEN
        assertNotNull(result, "walk should never return null");
        // The supplied JSON is an array, which does not satisfy the object schema.
        assertFalse(result.isValid(), "When validation is enabled the result should be invalid for a non‑object JSON");
        assertFalse(result.getMessages().isEmpty(), "Validation messages should be present for schema violations");
    }

    /**
     * Verify that a valid JSON document yields a successful validation result
     * when {@code validate = true}.
     */
    @Test
    void walkWithValidationShouldPassForValidJson() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        // WHEN
        ValidationResult result = jsonSchema.walk(VALID_JSON, format, true);
        // THEN
        assertNotNull(result, "walk should never return null");
        assertTrue(result.isValid(), "A JSON document that conforms to the schema should be reported as valid");
        assertTrue(result.getMessages().isEmpty(), "No validation messages should be produced for a valid JSON document");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_validJson_validateTrue() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        // WHEN
        ValidationResult result = jsonSchema.walk("{\"name\":\"John\",\"age\":30}", format, true);
        // THEN
        assertNotNull(result);
        assertTrue(result.isValid(), "Valid JSON with validation=true must be reported as valid");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for a conforming JSON");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_validJson_validateFalse() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        // WHEN
        ValidationResult result = jsonSchema.walk("{\"name\":\"John\",\"age\":30}", format, false);
        // THEN
        assertNotNull(result);
        assertTrue(result.isValid(), "When validation is disabled the result must be valid");
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is off");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_invalidJson_malformed() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        String malformed = "{\"name\":\"John\", \"age\":}";
        // WHEN
        ValidationResult result = jsonSchema.walk(malformed, format, true);
        // THEN
        assertNotNull(result);
        assertFalse(result.isValid(), "Malformed JSON must result in an invalid ValidationResult");
        assertFalse(result.getMessages().isEmpty(), "Parsing error messages should be present");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_emptyString() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        String empty = "";
        // WHEN
        ValidationResult result = jsonSchema.walk(empty, format, true);
        // THEN
        assertNotNull(result);
        assertFalse(result.isValid(), "Empty input should be reported as invalid");
        assertFalse(result.getMessages().isEmpty(), "An error message about empty payload is expected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullInput_throwsNPE() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        // WHEN / THEN
        Executable executable = () -> jsonSchema.walk(null, format, true);
        assertThrows(NullPointerException.class, executable, "Passing a null input string must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullInputFormat_throwsNPE() {
        // GIVEN
        String someJson = "{\"name\":\"John\"}";
        // WHEN / THEN
        Executable executable = () -> jsonSchema.walk(someJson, null, true);
        assertThrows(NullPointerException.class, executable, "Passing a null InputFormat must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_validYaml_validateTrue() {
        // GIVEN
        InputFormat format = InputFormat.YAML;
        String yaml = "name: John\nage: 30";
        // WHEN
        ValidationResult result = jsonSchema.walk(yaml, format, true);
        // THEN
        assertNotNull(result);
        assertTrue(result.isValid(), "Well‑formed YAML with validation=true must be valid");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for conforming YAML");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_invalidYaml_malformed() {
        // GIVEN
        InputFormat format = InputFormat.YAML;
        String malformedYaml = "name: John\nage: [unclosed";
        // WHEN
        ValidationResult result = jsonSchema.walk(malformedYaml, format, true);
        // THEN
        assertNotNull(result);
        assertFalse(result.isValid(), "Malformed YAML must produce an invalid ValidationResult");
        assertFalse(result.getMessages().isEmpty(), "Parsing error messages should be present for bad YAML");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_largeJson_validateTrue() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        String largeJson = """
            {"items":[{"id":1},{"id":2},{"id":3},{"id":4},{"id":5},{"id":6},{"id":7},{"id":8},{"id":9},{"id":10}],"metadata":{"count":10}}
            """;
        // WHEN
        ValidationResult result = jsonSchema.walk(largeJson, format, true);
        // THEN
        assertNotNull(result);
        assertTrue(result.isValid(), "Large well‑formed JSON should be valid");
        assertTrue(result.getMessages().isEmpty(), "No messages expected for a valid large payload");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_inputWithSpecialCharacters() {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        String specialJson = "{\"text\":\"Line1\\nLine2\\tTabbed\\u00A9\"}";
        // WHEN
        ValidationResult result = jsonSchema.walk(specialJson, format, true);
        // THEN
        assertNotNull(result);
        assertTrue(result.isValid(), "JSON containing escaped characters should be parsed correctly");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for valid special‑char JSON");
    }
}
