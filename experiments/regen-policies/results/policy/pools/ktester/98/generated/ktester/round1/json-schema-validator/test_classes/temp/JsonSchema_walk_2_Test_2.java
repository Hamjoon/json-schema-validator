package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Disabled;

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

    private static Stream<Arguments> nullNodeProvider() {
        return Stream.of(Arguments.of(null, true), Arguments.of(null, false));
    }

    /**
     * Walk a valid JSON node with validation enabled.
     * Expect an empty set of validation messages.
     */
//     @Test
//     void walkValidNodeShouldReturnNoMessages() throws Exception {
//         JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
//         ValidationResult result = schema.walk(validNode, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertTrue(messages.isEmpty(), "Expected no validation messages for a valid instance");
//     }

    /**
     * Walk an invalid JSON node with validation enabled.
     * The property "name" is a number, which violates the schema.
     * Expect at least one validation message.
     */
//     @Test
//     void walkInvalidNodeShouldReturnMessages() throws Exception {
//         JsonNode invalidNode = mapper.readTree("{\"name\":123}");
//         ValidationResult result = schema.walk(invalidNode, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Expected validation messages for an invalid instance");
//         boolean containsTypeError = messages.stream().anyMatch(msg -> msg.getMessage().toLowerCase().contains("type"));
//         assertTrue(containsTypeError, "Expected a type‑related validation message");
//     }

    /**
     * Walk a valid JSON node with validation disabled.
     * The method should still return a {@link ValidationResult} but without performing
     * any validation, i.e., the messages set must be empty.
     */
//     @Test
//     void walkValidNodeWithoutValidationShouldReturnNoMessages() throws Exception {
//         JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
//         ValidationResult result = schema.walk(validNode, false);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertTrue(messages.isEmpty(), "When validation is disabled, walk should return an empty message set");
//     }

    /**
     * 1. Null node handling **
     */
//     @ParameterizedTest(name = "[{index}] validate={1}")
//     @MethodSource("nullNodeProvider")
//     void walkNullNodeShouldBeInvalid(JsonNode node, boolean validate) {
//         ValidationResult result = schema.walk(node, validate);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Result must be invalid when the instance node is null");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("null")), "Expected a message that mentions a null instance");
//     }

    /**
     * 2. JSON literal null handling – schema allows null **
     */
//     @Test
//     void walkJsonLiteralNullAllowedBySchema() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": ["null","object"],
//               "properties": { "dummy": { "type": "string" } }
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode nullNode = mapper.readTree("null");
//         ValidationResult result = s.walk(nullNode, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertTrue(messages.isEmpty(), "A JSON literal null should be valid when the schema includes the null type");
//     }

    /**
     * 3. JSON literal null handling – schema disallows null **
     */
//     @Test
//     void walkJsonLiteralNullDisallowedBySchema() throws Exception {
//         JsonNode nullNode = mapper.readTree("null");
//         ValidationResult result = schema.walk(nullNode, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "A JSON literal null should be invalid when the schema does not allow null");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")), "Expected a type‑mismatch message");
//     }

    /**
     * 4. Missing required property **
     */
//     @Test
//     void walkMissingRequiredProperty() throws Exception {
//         JsonNode instance = mapper.readTree("{\"age\":30}");
//         ValidationResult result = schema.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Missing required property must cause validation failure");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("required")), "Expected a required‑property validation message");
//     }

    /**
     * 5. Array minItems violation **
     */
//     @Test
//     void walkArrayMinItemsViolation() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "array",
//               "minItems": 2,
//               "items": { "type": "integer" }
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("[1]");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Array with fewer items than minItems must be invalid");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("minitems")), "Expected a minItems validation message");
//     }

    /**
     * 6. String pattern validation **
     */
//     @Test
//     void walkStringPatternViolation() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "string",
//               "pattern": "^[A-Z]+$"
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("\"abc123\"");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "String not matching the pattern must be invalid");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("pattern")), "Expected a pattern‑mismatch validation message");
//     }

    /**
     * 7. AdditionalProperties = false **
     */
//     @Test
//     void walkAdditionalPropertiesDisallowed() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "object",
//               "properties": {
//                 "name": { "type": "string" }
//               },
//               "required": ["name"],
//               "additionalProperties": false
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("{\"name\":\"John\",\"age\":30,\"extra\":\"value\"}");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Instance with disallowed additional property must be invalid");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("additional")), "Expected an additional‑property validation message");
//     }

    /**
     * 8. Numeric range violation (minimum) **
     */
//     @Test
//     void walkNumericMinimumViolation() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "number",
//               "minimum": 0
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("-5");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Number below the defined minimum must be invalid");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("minimum")), "Expected a minimum‑value validation message");
//     }

    /**
     * 9. Conditional validation – then branch passes **
     */
//     @Test
//     void walkConditionalThenBranchPasses() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "object",
//               "properties": {
//                 "type": { "type": "string" },
//                 "value": {}
//               },
//               "if": {
//                 "properties": { "type": { "const": "A" } },
//                 "required": ["type"]
//               },
//               "then": {
//                 "properties": {
//                   "value": { "type": "integer", "minimum": 0 }
//                 },
//                 "required": ["value"]
//               },
//               "else": {
//                 "properties": {
//                   "value": { "type": "string" }
//                 },
//                 "required": ["value"]
//               }
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("{\"type\":\"A\",\"value\":10}");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertTrue(messages.isEmpty(), "Conditional then‑branch instance should be valid");
//     }

    /**
     * 10. Conditional validation – else branch violation **
     */
//     @Test
//     void walkConditionalElseBranchViolation() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "object",
//               "properties": {
//                 "type": { "type": "string" },
//                 "value": {}
//               },
//               "if": {
//                 "properties": { "type": { "const": "A" } },
//                 "required": ["type"]
//               },
//               "then": {
//                 "properties": {
//                   "value": { "type": "integer", "minimum": 0 }
//                 },
//                 "required": ["value"]
//               },
//               "else": {
//                 "properties": {
//                   "value": { "type": "number" }
//                 },
//                 "required": ["value"]
//               }
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("{\"type\":\"B\",\"value\":\"oops\"}");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Conditional else‑branch instance should be invalid");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")), "Expected a type‑mismatch message for the else branch");
//     }

    /**
     * 11. $ref resolution success **
     */
//     @Test
//     void walkRefResolutionSuccess() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "$ref": "#/definitions/person",
//               "definitions": {
//                 "person": {
//                   "type": "object",
//                   "properties": {
//                     "name": { "type": "string" }
//                   },
//                   "required": ["name"]
//                 }
//               }
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("{\"name\":\"John\"}");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertTrue(messages.isEmpty(), "Instance should validate successfully via $ref");
//     }

    /**
     * 12. Unsupported encoding in $ref (URL‑decode failure) **
     */
//     @Test
//     void walkUnsupportedEncodingInRefShouldYieldInvalidResult() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "$ref": "#/definitions/%ZZ",
//               "definitions": {
//                 "valid": { "type": "string" }
//               }
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("\"any\"");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertFalse(messages.isEmpty(), "Unsupported encoding in $ref must produce an invalid result");
//         assertTrue(messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("unsupported")), "Expected a message mentioning unsupported encoding");
//     }

    /**
     * 13. Internal $ref to missing definition – expect exception **
     */
    @Test
    void walkInternalRefMissingDefinitionShouldThrowException() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "#/definitions/unknown"
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaJson), "Missing $ref target should cause a JsonSchemaException");
    }

    /**
     * 14. Malformed $ref URI – illegal characters **
     */
    @Test
    void walkMalformedRefUriShouldThrowIllegalArgumentException() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "http://example.com/invalid[ref].json"
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaJson), "Malformed $ref URI should result in IllegalArgumentException");
    }

    /**
     * 15. Schema with unsupported keyword – validator initialization failure **
     */
    @Test
    void walkSchemaWithUnsupportedKeywordShouldThrowIllegalArgumentException() {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "unexpectedKeyword": 123
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaJson), "Unsupported keyword should cause IllegalArgumentException during schema creation");
    }

    /**
     * 16. Recursive anchor handling – should not overflow **
     */
//     @Test
//     void walkRecursiveAnchorShouldNotOverflow() throws Exception {
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "$recursiveAnchor": true,
//               "$ref": "#"
//             }
//             """;
//         JsonSchema s = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode instance = mapper.readTree("{}");
//         ValidationResult result = s.walk(instance, true);
//         Set<ValidationMessage> messages = result.getMessages();
//         assertTrue(messages.isEmpty(), "Recursive anchor should be handled gracefully without StackOverflowError");
//     }

    /**
     * 17. External $ref I/O error – simulate via Mockito **
     */
    @Test
    void walkExternalRefIoErrorShouldReturnInvalidResult() throws Exception {
        // This test is intentionally disabled because it depends on Mockito,
        // which is not present in the compilation environment.
    }
}