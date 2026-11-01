```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchema#walk(JsonNode, boolean)}.
 *
 * <p>The test suite covers a wide range of scenarios described in the
 * specification JSON supplied with the kata – null handling, required
 * properties, array constraints, pattern validation, $ref handling,
 * recursive anchors, etc.  Where the library throws an exception (e.g.
 * malformed $ref) the test asserts the exception type and message.
 *
 * <p>All tests use a fresh {@link JsonSchema} instance created via
 * {@link JsonSchemaFactory#getInstance(VersionFlag)}.  The factory is
 * thread‑safe, so a single instance per test method is sufficient.
 *
 * <p>Mockito is only required for the “external $ref I/O error” case; the
 * rest of the tests rely on real schema processing.
 */
class JsonSchema_walk_2_Test {

    private static ObjectMapper mapper;
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // ObjectMapper is thread‑safe after configuration, so a single instance is fine.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default schema used by the majority of tests – an object with a required
        // string property "name".  Individual tests replace this schema with a
        // more specific one when needed.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """;

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test clean‑up required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple tests (kept unchanged for context)                    */
    /* --------------------------------------------------------------------- */

    /**
     * Walk a valid JSON node with validation enabled.
     * Expect an empty set of validation messages.
     */
    @Test
    void walkValidNodeShouldReturnNoMessages() throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
        ValidationResult result = schema.walk(validNode, true);
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid instance");
    }

    /**
     * Walk an invalid JSON node with validation enabled.
     * The property "name" is a number, which violates the schema.
     * Expect at least one validation message.
     */
    @Test
    void walkInvalidNodeShouldReturnMessages() throws Exception {
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");
        ValidationResult result = schema.walk(invalidNode, true);
        Set<ValidationMessage> messages = result.getMessages();
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid instance");
        boolean containsTypeError = messages.stream()
                .anyMatch(msg -> msg.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑related validation message");
    }

    /**
     * Walk a valid JSON node with validation disabled.
     * The method should still return a {@link ValidationResult} but without performing
     * any validation, i.e., the messages set must be empty.
     */
    @Test
    void walkValidNodeWithoutValidationShouldReturnNoMessages() throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
        ValidationResult result = schema.walk(validNode, false);
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(),
                "When validation is disabled, walk should return an empty message set");
    }

    /* --------------------------------------------------------------------- */
    /* New parameterised tests covering the extensive JSON test matrix       */
    /* --------------------------------------------------------------------- */

    /*** 1. Null node handling ***/
    @ParameterizedTest(name = "[{index}] validate={1}")
    @MethodSource("nullNodeProvider")
    void walkNullNodeShouldBeInvalid(JsonNode node, boolean validate) {
        ValidationResult result = schema.walk(node, validate);
        assertFalse(result.isValid(), "Result must be invalid when the instance node is null");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("null")),
                "Expected a message that mentions a null instance");
    }

    private static Stream<Arguments> nullNodeProvider() {
        return Stream.of(
                Arguments.of(null, true),
                Arguments.of(null, false)
        );
    }

    /*** 2. JSON literal null handling – schema allows null ***/
    @Test
    void walkJsonLiteralNullAllowedBySchema() throws Exception {
        // Schema that permits "null" as a valid type
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": ["null","object"],
                  "properties": { "dummy": { "type": "string" } }
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode nullNode = mapper.readTree("null");
        ValidationResult result = s.walk(nullNode, true);
        assertTrue(result.isValid(), "A JSON literal null should be valid when the schema includes the null type");
        assertTrue(result.getMessages().isEmpty());
    }

    /*** 3. JSON literal null handling – schema disallows null ***/
    @Test
    void walkJsonLiteralNullDisallowedBySchema() throws Exception {
        // Re‑use the default schema (type object) – it does not allow null
        JsonNode nullNode = mapper.readTree("null");
        ValidationResult result = schema.walk(nullNode, true);
        assertFalse(result.isValid(), "A JSON literal null should be invalid when the schema does not allow null");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type")),
                "Expected a type‑mismatch message");
    }

    /*** 4. Missing required property ***/
    @Test
    void walkMissingRequiredProperty() throws Exception {
        JsonNode instance = mapper.readTree("{\"age\":30}");
        ValidationResult result = schema.walk(instance, true);
        assertFalse(result.isValid(), "Missing required property must cause validation failure");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("required")),
                "Expected a required‑property validation message");
    }

    /*** 5. Array minItems violation ***/
    @Test
    void walkArrayMinItemsViolation() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "array",
                  "minItems": 2,
                  "items": { "type": "integer" }
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("[1]");
        ValidationResult result = s.walk(instance, true);
        assertFalse(result.isValid(), "Array with fewer items than minItems must be invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("minitems")),
                "Expected a minItems validation message");
    }

    /*** 6. String pattern validation ***/
    @Test
    void walkStringPatternViolation() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "string",
                  "pattern": "^[A-Z]+$"
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("\"abc123\"");
        ValidationResult result = s.walk(instance, true);
        assertFalse(result.isValid(), "String not matching the pattern must be invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("pattern")),
                "Expected a pattern‑mismatch validation message");
    }

    /*** 7. AdditionalProperties = false ***/
    @Test
    void walkAdditionalPropertiesDisallowed() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"],
                  "additionalProperties": false
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("{\"name\":\"John\",\"age\":30,\"extra\":\"value\"}");
        ValidationResult result = s.walk(instance, true);
        assertFalse(result.isValid(), "Instance with disallowed additional property must be invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("additional")),
                "Expected an additional‑property validation message");
    }

    /*** 8. Numeric range violation (minimum) ***/
    @Test
    void walkNumericMinimumViolation() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "number",
                  "minimum": 0
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("-5");
        ValidationResult result = s.walk(instance, true);
        assertFalse(result.isValid(), "Number below the defined minimum must be invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("minimum")),
                "Expected a minimum‑value validation message");
    }

    /*** 9. Conditional validation – then branch passes ***/
    @Test
    void walkConditionalThenBranchPasses() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "type": { "type": "string" },
                    "value": {}
                  },
                  "if": {
                    "properties": { "type": { "const": "A" } },
                    "required": ["type"]
                  },
                  "then": {
                    "properties": {
                      "value": { "type": "integer", "minimum": 0 }
                    },
                    "required": ["value"]
                  },
                  "else": {
                    "properties": {
                      "value": { "type": "string" }
                    },
                    "required": ["value"]
                  }
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("{\"type\":\"A\",\"value\":10}");
        ValidationResult result = s.walk(instance, true);
        assertTrue(result.isValid(), "Conditional then‑branch instance should be valid");
        assertTrue(result.getMessages().isEmpty());
    }

    /*** 10. Conditional validation – else branch violation ***/
    @Test
    void walkConditionalElseBranchViolation() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "type": { "type": "string" },
                    "value": {}
                  },
                  "if": {
                    "properties": { "type": { "const": "A" } },
                    "required": ["type"]
                  },
                  "then": {
                    "properties": {
                      "value": { "type": "integer", "minimum": 0 }
                    },
                    "required": ["value"]
                  },
                  "else": {
                    "properties": {
                      "value": { "type": "number" }
                    },
                    "required": ["value"]
                  }
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("{\"type\":\"B\",\"value\":\"oops\"}");
        ValidationResult result = s.walk(instance, true);
        assertFalse(result.isValid(), "Conditional else‑branch instance should be invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type")),
                "Expected a type‑mismatch message for the else branch");
    }

    /*** 11. $ref resolution success ***/
    @Test
    void walkRefResolutionSuccess() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "#/definitions/person",
                  "definitions": {
                    "person": {
                      "type": "object",
                      "properties": {
                        "name": { "type": "string" }
                      },
                      "required": ["name"]
                    }
                  }
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("{\"name\":\"John\"}");
        ValidationResult result = s.walk(instance, true);
        assertTrue(result.isValid(), "Instance should validate successfully via $ref");
        assertTrue(result.getMessages().isEmpty());
    }

    /*** 12. Unsupported encoding in $ref (URL‑decode failure) ***/
    @Test
    void walkUnsupportedEncodingInRefShouldYieldInvalidResult() throws Exception {
        // The fragment "%ZZ" cannot be URL‑decoded, triggering UnsupportedEncodingException internally.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "#/definitions/%ZZ",
                  "definitions": {
                    "valid": { "type": "string" }
                  }
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        JsonNode instance = mapper.readTree("\"any\"");
        ValidationResult result = s.walk(instance, true);
        assertFalse(result.isValid(), "Unsupported encoding in $ref must produce an invalid result");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("unsupported")),
                "Expected a message mentioning unsupported encoding");
    }

    /*** 13. Internal $ref to missing definition – expect exception ***/
    @Test
    void walkInternalRefMissingDefinitionShouldThrowException() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "#/definitions/unknown"
                }
                """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // The factory validates the schema during creation; a missing definition
        // results in a JsonSchemaException (runtime) – we assert that it is thrown.
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaJson),
                "Missing $ref target should cause a JsonSchemaException");
    }

    /*** 14. Malformed $ref URI – illegal characters ***/
    @Test
    void walkMalformedRefUriShouldThrowIllegalArgumentException() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "http://example.com/invalid[ref].json"
                }
                """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaJson),
                "Malformed $ref URI should result in IllegalArgumentException");
    }

    /*** 15. Schema with unsupported keyword – validator initialization failure ***/
    @Test
    void walkSchemaWithUnsupportedKeywordShouldThrowIllegalArgumentException() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "unexpectedKeyword": 123
                }
                """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaJson),
                "Unsupported keyword should cause IllegalArgumentException during schema creation");
    }

    /*** 16. Recursive anchor handling – should not overflow ***/
    @Test
    void walkRecursiveAnchorShouldNotOverflow() throws Exception {
        // A minimal recursive‑anchor schema (draft‑2020‑12) where the schema
        // references itself via $recursiveAnchor and $ref.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$recursiveAnchor": true,
                  "$ref": "#"
                }
                """;
        JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        // The instance can be any JSON value; we use an empty object.
        JsonNode instance = mapper.readTree("{}");
        ValidationResult result = s.walk(instance, true);
        assertTrue(result.isValid(), "Recursive anchor should be handled gracefully without StackOverflowError");
        assertTrue(result.getMessages().isEmpty());
    }

    /*** 17. External $ref I/O error – simulate via Mockito ***/
    @Test
    void walkExternalRefIoErrorShouldReturnInvalidResult() throws Exception {
        // Build a schema that contains an external $ref.  We mock the
        // JsonSchemaFactory's URL loader to throw an IOException when the
        // external reference is accessed.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "http://nonexistent.example.com/missing-schema.json"
                }
                """;

        // Create a spy of the factory so we can stub the loader behaviour.
        JsonSchemaFactory realFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchemaFactory factorySpy = org.mockito.Mockito.spy(realFactory);

        // The internal method that fetches remote schemas is
        // JsonSchemaFactory#getSchema(JsonNode) (indirectly).  Stubbing the
        // exact call chain is brittle, but we can force the factory to
        // throw when asked for the external schema.
        org.mockito.Mockito.doThrow(new IOException("Simulated I/O error"))
                .when(factorySpy).getSchema(org.mockito.ArgumentMatchers.eq("http://nonexistent.example.com/missing-schema.json"));

        // Attempt to create the schema – it will succeed because the $ref is
        // not resolved until walk() is invoked.
        JsonSchema s = factorySpy.getSchema(schemaJson);

        JsonNode dummyInstance = mapper.readTree("{}");
        ValidationResult result = s.walk(dummyInstance, true);
        assertFalse(result.isValid(), "I/O error during external $ref resolution must produce an invalid result");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("io") ||
                               m.getMessage().toLowerCase().contains("cannot read")),
                "Expected a message indicating an I/O problem");
    }

    // ----------------------------------------------------------------------
    // Helper methods (if any) could be placed here.
    // ----------------------------------------------------------------------
}
```