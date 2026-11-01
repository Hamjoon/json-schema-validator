```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JsonSchema#walk(String, InputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * All test methods are enabled and the necessary helper utilities are provided.
 */
class JsonSchema_walk_9_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Simple schema used in the first two tests – a JSON string type. */
    private static final String SCHEMA_DATA = "{ \"type\": \"string\" }";

    /** Input that does NOT satisfy the schema (a number instead of a string). */
    private static final String INVALID_INPUT = "123";

    /** Input that satisfies the schema (a JSON string). */
    private static final String VALID_INPUT = "\"hello\"";

    /** Input format used for the tests. */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /** The {@link JsonSchema} instance under test. */
    private static JsonSchema schema;

    /** Simple customizer that counts how many times it has been invoked. */
    private static class CountingCustomizer implements ExecutionContextCustomizer {
        int count = 0;

        @Override
        public void customize(ExecutionContext ctx, ValidationContext vc) {
            count++;
        }
    }

    /** Initialise the factory and the reusable schema once for all tests. */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft (the same version used in the original concurrency test)
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(SCHEMA_DATA);
    }

    /** Helper to create a {@link JsonSchema} from a JSON schema string. */
    private static JsonSchema schemaOf(String schemaJson) {
        return factory.getSchema(schemaJson);
    }

    /** Checks whether any {@link ValidationMessage} in the result contains the given text (case‑insensitive). */
    private static boolean containsMessageContaining(ValidationResult result, String text) {
        for (ValidationMessage vm : result.getMessages()) {
            if (vm.getMessage().toLowerCase().contains(text.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /* --------------------------------------------------------------------- */

    @Test
    void walkShouldValidateAndReturnMessagesWhenValidateTrue() {
        CountingCustomizer customizer = new CountingCustomizer();
        ValidationResult result = schema.walk(INVALID_INPUT, INPUT_FORMAT, true, customizer);
        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertEquals(1, messages.size(), "Invalid input should produce exactly one validation message");
        assertEquals(1, customizer.count, "Customizer should be invoked exactly once");
    }

    @Test
    void walkShouldSkipValidationWhenValidateFalse() {
        CountingCustomizer customizer = new CountingCustomizer();
        ValidationResult result = schema.walk(VALID_INPUT, INPUT_FORMAT, false, customizer);
        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(), "Validation should be skipped when the validate flag is false");
        assertEquals(1, customizer.count, "Customizer should be invoked exactly once");
    }

    @Test
    void validInput_validationEnabled_defaultContext() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }""";
        String input = """
                { "name": "John" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);
        assertTrue(result.isSuccess(), "Result should be successful");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    @Test
    void validInput_validationDisabled_defaultContext() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }""";
        String input = """
                { "age": 30 }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, false, null);
        assertTrue(result.isSuccess(), "Result should be successful even when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    @Test
    void malformedJson_returnsFailureResult() {
        String schema = """
                { "type": "object" }""";
        // missing closing brace
        String malformed = "{invalidJson: true,";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(malformed, InputFormat.JSON, true, null);
        assertFalse(result.isSuccess(), "Result should indicate failure");
        assertTrue(containsMessageContaining(result, "parsing"), "Message should mention parsing error");
    }

    @Test
    void nullInput_throwsNullPointerException() {
        String schema = """
                { "type": "object" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(null, InputFormat.JSON, true, null));
    }

    @Test
    void unsupportedInputFormat_returnsFailureResult() {
        String schema = """
                { "type": "object" }""";
        String input = "<root><a>1</a></root>";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.XML, true, null);
        assertFalse(result.isSuccess(), "XML format is not supported – should fail");
        assertTrue(containsMessageContaining(result, "unsupported"), "Message should mention unsupported format");
    }

    @Test
    void customizerIsInvoked() {
        String schema = """
                { "type": "object" }""";
        String input = """
                { "any": "value" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        CountingCustomizer customizer = new CountingCustomizer();
        jsonSchema.walk(input, InputFormat.JSON, true, customizer);
        assertEquals(1, customizer.count, "Customizer should be invoked exactly once");
    }

    @Test
    void customizerThrowsRuntimeException_propagates() {
        String schema = """
                { "type": "object" }""";
        String input = """
                { "any": "value" }""";
        ExecutionContextCustomizer failingCustomizer = (ctx, vc) -> {
            throw new RuntimeException("customizer failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(input, InputFormat.JSON, true, failingCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void missingRequiredProperty_returnsValidationError() {
        String schema = """
                {
                  "type": "object",
                  "required": ["name"],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }""";
        String input = """
                { "age": 25 }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);
        assertFalse(result.isSuccess(), "Result should be failure because required property is missing");
        assertTrue(containsMessageContaining(result, "required"), "Message should contain 'required'");
    }

    @Test
    void arrayInputAgainstObjectSchema_returnsTypeMismatch() {
        String schema = """
                { "type": "object" }""";
        String input = """
                [{"type":"object"}]""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);
        assertFalse(result.isSuccess(), "Array input should not match object schema");
        assertTrue(containsMessageContaining(result, "type"), "Message should mention type mismatch");
    }

    @Test
    void validYamlInput_validationEnabled_success() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "id": { "type": "integer" }
                  }
                }""";
        String yamlInput = """
                type: object
                properties:
                  id:
                    type: integer
                """;
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(yamlInput, InputFormat.YAML, true, null);
        assertTrue(result.isSuccess(), "YAML input should be parsed and validated successfully");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    @Test
    void recursiveAnchor_schemaValid_success() {
        String schema = """
                {
                  "$id": "#",
                  "$recursiveAnchor": true,
                  "type": "object",
                  "properties": {
                    "self": { "$ref": "#" }
                  }
                }""";
        String input = """
                { "self": {} }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);
        assertTrue(result.isSuccess(), "Recursive anchor should be handled without infinite recursion");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    @Test
    void internalRef_resolvesAndValidatesSuccessfully() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "address": { "$ref": "#/definitions/address" }
                  },
                  "definitions": {
                    "address": {
                      "type": "object",
                      "properties": {
                        "city": { "type": "string" }
                      },
                      "required": ["city"]
                    }
                  }
                }""";
        String input = """
                { "address": { "city": "Paris" } }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);
        assertTrue(result.isSuccess(), "Reference should be resolved and validation succeed");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    @Test
    void customizerDisablesFormatValidation_success() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "email": { "type": "string", "format": "email" }
                  }
                }""";
        String input = """
                { "email": "not-an-email" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ExecutionContextCustomizer disableFormat = (ctx, vc) ->
                ctx.getValidationContext().getConfig().setFormatEnabled(false);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, disableFormat);
        assertTrue(result.isSuccess(), "Validation should succeed because format check is disabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    @Test
    void validationDisabled_customizerThrows_propagatesException() {
        String schema = """
                { "type": "object" }""";
        String input = """
                { "any": "value" }""";
        ExecutionContextCustomizer failing = (ctx, vc) -> {
            throw new RuntimeException("customizer failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(input, InputFormat.JSON, false, failing));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void emptyStringInput_returnsParsingFailure() {
        String schema = """
                { "type": "object" }""";
        String input = "";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);
        assertFalse(result.isSuccess(), "Empty input should cause parsing failure");
        assertTrue(containsMessageContaining(result, "parsing"), "Message should mention parsing error");
    }

    @Test
    void nullInputFormat_throwsNullPointerException() {
        String schema = """
                { "type": "object" }""";
        String input = """
                { "any": "value" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(input, null, true, null));
    }

    @Test
    void customizerThrowsAssertionError_propagates() {
        String schema = """
                { "type": "object" }""";
        String input = """
                { "any": "value" }""";
        ExecutionContextCustomizer failing = (ctx, vc) -> {
            throw new AssertionError("customizer assertion failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);
        AssertionError err = assertThrows(AssertionError.class,
                () -> jsonSchema.walk(input, InputFormat.JSON, true, failing));
        assertEquals("customizer assertion failure", err.getMessage());
    }

    @Test
    void deepRecursiveSchema_withoutAnchor_isHandledGracefully() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "a": { "$ref": "#/properties/b" },
                    "b": { "$ref": "#/properties/a" }
                  }
                }""";
        String input = """
                { "a": {} }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);
        assertFalse(result.isSuccess(), "Recursive schema without anchor should fail gracefully");
        assertTrue(containsMessageContaining(result, "recursion") || containsMessageContaining(result, "depth"),
                "Message should mention recursion or depth limit");
    }

    @Test
    void urlEncodedInvalidPercentEncoding_returnsFailure() {
        String schema = """
                { "type": "object" }""";
        String input = "%ZZ%GG";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.URL_ENCODED, true, null);
        assertFalse(result.isSuccess(), "Malformed percent‑encoding should cause failure");
        assertTrue(containsMessageContaining(result, "decoding") || containsMessageContaining(result, "UnsupportedEncodingException"),
                "Message should mention decoding error");
    }
}
```