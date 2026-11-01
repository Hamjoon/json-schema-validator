```java
package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.input.InputFormat;
import com.networknt.schema.walk.WalkListenerRunner;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests for {@link JsonSchema#walk(String, InputFormat, boolean)}.
 *
 * The tests create a real {@link JsonSchema} instance using the public
 * {@link JsonSchemaFactory} API.  The schema used in the tests is a minimal
 * object schema ( {@code {"type":"object"}} ) so that the validation logic
 * can be exercised without needing any external resources.
 *
 * The focus of the tests is to verify that the {@code walk} method returns a
 * non‑null {@link ValidationResult} and that the {@code validate} flag influences
 * the presence of validation messages.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_3_Test {

    /** The schema under test – a simple object schema. */
    private static JsonSchema jsonSchema;

    /** A JSON document that conforms to the schema. */
    private static final String VALID_JSON = "{\"name\":\"John\"}";

    /** A JSON document that violates the schema (not an object). */
    private static final String INVALID_JSON = "[1,2,3]";

    @BeforeAll
    static void setupBeforeAll() {
        // Build a JsonSchemaFactory for Draft‑07 (the default in the library)
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);

        // The schema we will use for the walk tests – a very small object schema.
        String schemaStr = "{\"type\":\"object\"}";
        jsonSchema = factory.getSchema(schemaStr);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
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
        assertTrue(result.isValid(),
                "When validation is disabled the result should be reported as valid");
        // No validation messages are expected because validation was turned off.
        assertTrue(result.getMessages().isEmpty(),
                "No validation messages should be produced when validation is disabled");
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
        assertFalse(result.isValid(),
                "When validation is enabled the result should be invalid for a non‑object JSON");
        assertFalse(result.getMessages().isEmpty(),
                "Validation messages should be present for schema violations");
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
        assertTrue(result.isValid(),
                "A JSON document that conforms to the schema should be reported as valid");
        assertTrue(result.getMessages().isEmpty(),
                "No validation messages should be produced for a valid JSON document");
    }
}
```