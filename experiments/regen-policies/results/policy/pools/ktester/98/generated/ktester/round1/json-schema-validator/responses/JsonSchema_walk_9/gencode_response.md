```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(String, InputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * The tests cover a wide range of scenarios described in the specification JSON.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaWalkTest {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    @Mock
    private ExecutionContextCustomizer mockCustomizer;

    @BeforeAll
    static void initFactory() {
        // Use the 2020‑12 draft – the same version used by the library in production.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /** Helper to create a {@link JsonSchema} from a JSON schema string. */
    private static JsonSchema schemaOf(String schemaJson) {
        return factory.getSchema(schemaJson);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Valid input, validation enabled, default execution context        */
    /* --------------------------------------------------------------------- */
    @Test
    void validInput_validationEnabled_defaultContext() {
        // schema expects an object with a string property "name"
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }""";
        String input = """{ "name": "John" }""";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);

        assertTrue(result.isSuccess(), "Result should be successful");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Valid input, validation disabled, default execution context        */
    /* --------------------------------------------------------------------- */
    @Test
    void validInput_validationDisabled_defaultContext() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }""";
        String input = """{ "age": 30 }""";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, false, null);

        assertTrue(result.isSuccess(), "Result should be successful even when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Invalid JSON input – deserialization failure                      */
    /* --------------------------------------------------------------------- */
    @Test
    void malformedJson_returnsFailureResult() {
        String schema = """{ "type": "object" }""";
        String malformed = "{invalidJson: true,";   // missing closing brace

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(malformed, InputFormat.JSON, true, null);

        assertFalse(result.isSuccess(), "Result should indicate failure");
        assertTrue(containsMessageContaining(result, "parsing"), "Message should mention parsing error");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Null input string – argument validation                            */
    /* --------------------------------------------------------------------- */
    @Test
    void nullInput_throwsNullPointerException() {
        String schema = """{ "type": "object" }""";
        JsonSchema jsonSchema = schemaOf(schema);

        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(null, InputFormat.JSON, true, null));
    }

    /* --------------------------------------------------------------------- */
    /* 5. Unsupported input format – branch for format handling               */
    /* --------------------------------------------------------------------- */
    @Test
    void unsupportedInputFormat_returnsFailureResult() {
        String schema = """{ "type": "object" }""";
        String input = "<root><a>1</a></root>";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.XML, true, null);

        assertFalse(result.isSuccess(), "XML format is not supported – should fail");
        assertTrue(containsMessageContaining(result, "unsupported"), "Message should mention unsupported format");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Non‑null ExecutionContextCustomizer – customizer invoked           */
    /* --------------------------------------------------------------------- */
    @Test
    void customizerIsInvoked() {
        String schema = """{ "type": "object" }""";
        String input = """{ "any": "value" }""";

        JsonSchema jsonSchema = schemaOf(schema);
        jsonSchema.walk(input, InputFormat.JSON, true, mockCustomizer);

        verify(mockCustomizer, times(1)).customize(any(ExecutionContext.class));
    }

    /* --------------------------------------------------------------------- */
    /* 7. ExecutionContextCustomizer throws exception – error propagation   */
    /* --------------------------------------------------------------------- */
    @Test
    void customizerThrowsRuntimeException_propagates() {
        String schema = """{ "type": "object" }""";
        String input = """{ "any": "value" }""";

        ExecutionContextCustomizer failingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(input, InputFormat.JSON, true, failingCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 8. Valid input with required property missing – validation enabled    */
    /* --------------------------------------------------------------------- */
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
        String input = """{ "age": 25 }""";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);

        assertFalse(result.isSuccess(), "Result should be failure because required property is missing");
        assertTrue(containsMessageContaining(result, "required"), "Message should contain 'required'");
    }

    /* --------------------------------------------------------------------- */
    /* 9. Valid JSON array input against object schema – validation error    */
    /* --------------------------------------------------------------------- */
    @Test
    void arrayInputAgainstObjectSchema_returnsTypeMismatch() {
        String schema = """{ "type": "object" }""";
        String input = """[{"type":"object"}]""";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);

        assertFalse(result.isSuccess(), "Array input should not match object schema");
        assertTrue(containsMessageContaining(result, "type"), "Message should mention type mismatch");
    }

    /* --------------------------------------------------------------------- */
    /* 10. Valid YAML input – validation enabled                              */
    /* --------------------------------------------------------------------- */
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

    /* --------------------------------------------------------------------- */
    /* 11. Recursive schema anchor – validation succeeds without overflow    */
    /* --------------------------------------------------------------------- */
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
        String input = """{ "self": {} }""";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);

        assertTrue(result.isSuccess(), "Recursive anchor should be handled without infinite recursion");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 12. Schema with $ref to internal definition – reference resolved        */
    /* --------------------------------------------------------------------- */
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
        String input = """{ "address": { "city": "Paris" } }""";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);

        assertTrue(result.isSuccess(), "Reference should be resolved and validation succeed");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 13. ExecutionContextCustomizer disables format validation              */
    /* --------------------------------------------------------------------- */
    @Test
    void customizerDisablesFormatValidation_success() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "email": { "type": "string", "format": "email" }
                  }
                }""";
        String input = """{ "email": "not-an-email" }""";

        JsonSchema jsonSchema = schemaOf(schema);
        ExecutionContextCustomizer disableFormat = ctx ->
                ctx.getValidationContext()
                   .getConfig()
                   .setFormatEnabled(false);

        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, disableFormat);

        assertTrue(result.isSuccess(), "Validation should succeed because format check is disabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 14. Validate flag false but customizer throws exception – propagates   */
    /* --------------------------------------------------------------------- */
    @Test
    void validationDisabled_customizerThrows_propagatesException() {
        String schema = """{ "type": "object" }""";
        String input = """{ "any": "value" }""";

        ExecutionContextCustomizer failing = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(input, InputFormat.JSON, false, failing));
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 15. Empty string input – deserialization failure                       */
    /* --------------------------------------------------------------------- */
    @Test
    void emptyStringInput_returnsParsingFailure() {
        String schema = """{ "type": "object" }""";
        String input = ""; // empty

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);

        assertFalse(result.isSuccess(), "Empty input should cause parsing failure");
        assertTrue(containsMessageContaining(result, "parsing"), "Message should mention parsing error");
    }

    /* --------------------------------------------------------------------- */
    /* 16. Null InputFormat – argument validation                             */
    /* --------------------------------------------------------------------- */
    @Test
    void nullInputFormat_throwsNullPointerException() {
        String schema = """{ "type": "object" }""";
        String input = """{ "any": "value" }""";

        JsonSchema jsonSchema = schemaOf(schema);
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(input, null, true, null));
    }

    /* --------------------------------------------------------------------- */
    /* 17. ExecutionContextCustomizer throws AssertionError – propagates      */
    /* --------------------------------------------------------------------- */
    @Test
    void customizerThrowsAssertionError_propagates() {
        String schema = """{ "type": "object" }""";
        String input = """{ "any": "value" }""";

        ExecutionContextCustomizer failing = ctx -> {
            throw new AssertionError("customizer assertion failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);

        AssertionError err = assertThrows(AssertionError.class,
                () -> jsonSchema.walk(input, InputFormat.JSON, true, failing));
        assertEquals("customizer assertion failure", err.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 18. Deep recursive schema without $recursiveAnchor – guard against   */
    /*     infinite recursion                                                */
    /* --------------------------------------------------------------------- */
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
        String input = """{ "a": {} }""";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, null);

        // The library should not crash; it will report a recursion depth limit.
        assertFalse(result.isSuccess(), "Recursive schema without anchor should fail gracefully");
        assertTrue(containsMessageContaining(result, "recursion") ||
                   containsMessageContaining(result, "depth"),
                "Message should mention recursion or depth limit");
    }

    /* --------------------------------------------------------------------- */
    /* 19. Invalid percent‑encoding in input – UnsupportedEncodingException   */
    /* --------------------------------------------------------------------- */
    @Test
    void urlEncodedInvalidPercentEncoding_returnsFailure() {
        String schema = """{ "type": "object" }""";
        String input = "%ZZ%GG";

        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.URL_ENCODED, true, null);

        assertFalse(result.isSuccess(), "Malformed percent‑encoding should cause failure");
        assertTrue(containsMessageContaining(result, "decoding") ||
                   containsMessageContaining(result, "UnsupportedEncodingException"),
                "Message should mention decoding error");
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Checks whether any {@link ValidationMessage} in the result contains the given text (case‑insensitive). */
    private static boolean containsMessageContaining(ValidationResult result, String text) {
        for (ValidationMessage vm : result.getMessages()) {
            if (vm.getMessage().toLowerCase().contains(text.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
```