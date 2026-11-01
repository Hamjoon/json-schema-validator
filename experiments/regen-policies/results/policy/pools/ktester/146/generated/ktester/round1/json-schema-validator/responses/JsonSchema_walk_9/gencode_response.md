```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#walk(String, InputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Argument validation (null checks)</li>
 *   <li>Deserialization failures</li>
 *   <li>Behaviour of the {@code validate} flag</li>
 *   <li>ExecutionContextCustomizer invocation, side‑effects and exception propagation</li>
 *   <li>Different input formats (JSON / YAML)</li>
 *   <li>Edge cases such as empty / whitespace‑only input</li>
 *   <li>Schema‑specific scenarios (unknown keywords, recursive $ref)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_9_Test {

    /** Shared factory for creating schemas */
    private static JsonSchemaFactory factory;

    /** Simple schema used by many tests – expects a string */
    private static final String SIMPLE_STRING_SCHEMA = "{ \"type\": \"string\" }";

    /** Schema that expects an object – used when the test needs successful validation of an object */
    private static final String SIMPLE_OBJECT_SCHEMA = "{ \"type\": \"object\" }";

    @BeforeAll
    static void setupBeforeAll() {
        // Use the most recent draft supported by the library
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /** Helper that builds a {@link JsonSchema} from a JSON schema string */
    private static JsonSchema schemaFrom(String schemaJson) {
        return factory.getSchema(schemaJson);
    }

    /* --------------------------------------------------------------------- */
    /*                         Branch‑coverage test cases                    */
    /* --------------------------------------------------------------------- */

    @Test
    void walk_nullInput_throwsIllegalArgumentException() {
        JsonSchema schema = schemaFrom(SIMPLE_STRING_SCHEMA);
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(null, InputFormat.JSON, true, null),
                "null input must cause IllegalArgumentException");
    }

    @Test
    void walk_nullInputFormat_throwsIllegalArgumentException() {
        JsonSchema schema = schemaFrom(SIMPLE_STRING_SCHEMA);
        assertThrows(IllegalArgumentException.class,
                () -> schema.walk("{\"type\":\"object\"}", null, true, null),
                "null InputFormat must cause IllegalArgumentException");
    }

    @Test
    void walk_malformedJson_returnsInvalidResult() {
        JsonSchema schema = schemaFrom(SIMPLE_STRING_SCHEMA);
        ValidationResult result = schema.walk("invalid json", InputFormat.JSON, true, null);
        assertFalse(result.isValid(), "Result must be invalid for malformed JSON");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("json")),
                "A parsing‑error message should be present");
    }

    @Test
    void walk_validateFlagFalse_skipsValidation() {
        JsonSchema schema = schemaFrom(SIMPLE_OBJECT_SCHEMA);
        // Input is a valid object, but we ask the method *not* to validate it
        ValidationResult result = schema.walk("{\"any\":\"value\"}", InputFormat.JSON, false, null);
        assertTrue(result.isValid(), "Result must be valid when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages should be produced");
    }

    @Test
    void walk_withCustomizer_isInvoked() {
        JsonSchema schema = schemaFrom(SIMPLE_OBJECT_SCHEMA);
        // Use a Mockito mock to verify that the customizer is called exactly once
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        // The mock simply returns the received context unchanged
        when(customizer.customize(any())).thenAnswer(inv -> inv.getArgument(0));

        ValidationResult result = schema.walk("{\"foo\":\"bar\"}", InputFormat.JSON, true, customizer);

        assertTrue(result.isValid(), "Object input should validate successfully");
        verify(customizer, times(1)).customize(any());
    }

    @Test
    void walk_customizerThrowsRuntimeException_propagates() {
        JsonSchema schema = schemaFrom(SIMPLE_OBJECT_SCHEMA);
        ExecutionContextCustomizer throwingCustomizer = ctx -> {
            throw new RuntimeException("boom");
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk("{\"x\":1}", InputFormat.JSON, true, throwingCustomizer));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void walk_yamlInputFormat_validatesSuccessfully() {
        JsonSchema schema = schemaFrom(SIMPLE_OBJECT_SCHEMA);
        // Simple YAML that represents an empty object
        String yaml = "type: object";
        ValidationResult result = schema.walk(yaml, InputFormat.YAML, true, null);
        assertTrue(result.isValid(), "YAML input that matches the schema must be valid");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /*                     Additional edge & interaction cases               */
    /* --------------------------------------------------------------------- */

    @Test
    void walk_emptyString_returnsParsingError() {
        JsonSchema schema = schemaFrom(SIMPLE_STRING_SCHEMA);
        ValidationResult result = schema.walk("", InputFormat.JSON, true, null);
        assertFalse(result.isValid(), "Empty input should be treated as invalid JSON");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("json")),
                "Parsing‑error message expected for empty input");
    }

    @Test
    void walk_whitespaceOnlyString_returnsParsingError() {
        JsonSchema schema = schemaFrom(SIMPLE_STRING_SCHEMA);
        ValidationResult result = schema.walk("   \t\n", InputFormat.JSON, true, null);
        assertFalse(result.isValid(), "Whitespace‑only input should be invalid JSON");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("json")),
                "Parsing‑error message expected for whitespace‑only input");
    }

    @Test
    void walk_schemaWithUnknownKeyword_returnsSchemaError() {
        // The schema itself contains an unknown keyword; the library reports it as a validation error
        String schemaWithUnknown = "{ \"type\": \"object\", \"unknownKeyword\": 123 }";
        JsonSchema schema = schemaFrom(schemaWithUnknown);
        ValidationResult result = schema.walk("{\"any\":\"value\"}", InputFormat.JSON, true, null);
        assertFalse(result.isValid(), "Schema containing an unknown keyword must be reported as invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("unknown")),
                "At least one message should mention the unknown keyword");
    }

    @Test
    void walk_customizerInvokedEvenWhenValidateFalse() {
        JsonSchema schema = schemaFrom(SIMPLE_OBJECT_SCHEMA);
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        when(customizer.customize(any())).thenAnswer(inv -> inv.getArgument(0));

        ValidationResult result = schema.walk("{\"a\":1}", InputFormat.JSON, false, customizer);

        assertTrue(result.isValid(), "Result must be valid when validation is disabled");
        verify(customizer, times(1)).customize(any());
    }

    @Test
    void walk_recursiveRef_resolvesWithoutError() {
        // A schema that references itself via $ref (recursive definition)
        String recursiveSchema = "{\n" +
                "  \"$id\": \"#root\",\n" +
                "  \"$defs\": {\n" +
                "    \"node\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"next\": { \"$ref\": \"#/$defs/node\" }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"root\": { \"$ref\": \"#/$defs/node\" }\n" +
                "  }\n" +
                "}";
        JsonSchema schema = schemaFrom(recursiveSchema);
        // Validate the schema itself – the library must handle the recursion gracefully
        ValidationResult result = schema.walk(recursiveSchema, InputFormat.JSON, true, null);
        assertTrue(result.isValid(), "Recursive $ref should be resolved without validation errors");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for a valid recursive schema");
    }

    @Test
    void walk_customizerThrowsIllegalStateException_propagates() {
        JsonSchema schema = schemaFrom(SIMPLE_OBJECT_SCHEMA);
        ExecutionContextCustomizer illegalStateCustomizer = ctx -> {
            throw new IllegalStateException("illegal state");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.walk("{\"x\":2}", InputFormat.JSON, true, illegalStateCustomizer));
        assertEquals("illegal state", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*                         Parameterised helper methods                  */
    /* --------------------------------------------------------------------- */

    /**
     * Provides data for a parameterised test that exercises the same
     * {@code walk(...)} overload with different combinations of arguments.
     * The stream is not used directly in the current test suite but can be
     * extended later for further combinatorial coverage.
     */
    static Stream<Arguments> walkParameterProvider() {
        return Stream.of(
                Arguments.of("{\"a\":\"b\"}", InputFormat.JSON, true, null),
                Arguments.of("{\"a\":1}", InputFormat.JSON, false, null)
        );
    }

    @ParameterizedTest
    @MethodSource("walkParameterProvider")
    void walk_parameterisedTests(String input, InputFormat format, boolean validate, ExecutionContextCustomizer customizer) {
        JsonSchema schema = schemaFrom(SIMPLE_STRING_SCHEMA);
        // When the schema expects a string, any object input will be invalid if validation is enabled
        ValidationResult result = schema.walk(input, format, validate, customizer);
        if (validate) {
            assertFalse(result.isValid(), "With validation enabled the non‑string input must be invalid");
        } else {
            assertTrue(result.isValid(), "With validation disabled the result must be valid");
        }
    }
}
```