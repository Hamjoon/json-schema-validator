```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.walk.WalkListenerRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit‑tests for {@link JsonSchema#validateAndCollect(JsonNode)}.
 * <p>
 * The tests cover the different validation branches described in the
 * specification (type, required, enum, const, pattern, numeric ranges, array
 * length, additionalProperties, $ref handling, recursive anchors, walk listeners,
 * execution‑context customisation, and error‑handling edge cases).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_2_Test {

    /** Object mapper used to build JSON nodes for the tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The JsonSchema instance under test – created per test method. */
    private JsonSchema jsonSchema;

    /** Mocked listener used in the “Keyword Walk Listener” test. */
    @Mock
    private WalkListenerRunner mockListener;

    /** Helper to create a {@link JsonSchema} from a JSON schema string. */
    private static JsonSchema schemaFrom(String schemaJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        return JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaNode);
    }

    /* --------------------------------------------------------------------- *
     *  Simple positive / negative cases (already present in the template) *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_WithValidNode_ReturnsSuccessfulResult() throws Exception {
        String schema = """
                {
                  "type": "object",
                  "required": ["id"],
                  "properties": {
                    "id": { "type": "integer" },
                    "name": { "type": "string" }
                  }
                }
                """;
        jsonSchema = schemaFrom(schema);

        JsonNode valid = MAPPER.readTree("""
                { "id": 10, "name": "Alice" }
                """);

        ValidationResult result = jsonSchema.validateAndCollect(valid);
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(0, result.getMessages().size());
    }

    @Test
    void validateAndCollect_WithInvalidNode_ReturnsFailedResult() throws Exception {
        String schema = """
                {
                  "type": "object",
                  "required": ["id"],
                  "properties": {
                    "id": { "type": "integer" },
                    "name": { "type": "string" }
                  }
                }
                """;
        jsonSchema = schemaFrom(schema);

        JsonNode invalid = MAPPER.readTree("""
                { "name": "Bob" }
                """);

        ValidationResult result = jsonSchema.validateAndCollect(invalid);
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("required")));
    }

    /* --------------------------------------------------------------------- *
     *  Null or missing instance node                                         *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_NullInstanceNode_ReturnsErrorResult() {
        // The method should handle a null JsonNode gracefully.
        ValidationResult result = jsonSchema.validateAndCollect(null);
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("null")));
    }

    /* --------------------------------------------------------------------- *
     *  Type validation (matching & mismatching)                             *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("typeValidationProvider")
    void validateAndCollect_TypeValidation(String schemaJson, String instanceJson,
                                            boolean expectValid, String expectedFragment) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid(),
                "Validity does not match expectation for instance: " + instanceJson);
        if (!expectValid) {
            assertTrue(result.getMessages().stream()
                    .anyMatch(m -> m.getMessage().contains(expectedFragment)),
                    "Expected validation message fragment not found");
        }
    }

    private static Stream<Arguments> typeValidationProvider() {
        String schema = """
                { "type": "object" }
                """;
        return Stream.of(
                Arguments.of(schema, """{ "age": 30 }""", true, ""),
                Arguments.of(schema, "\"just a string\"", false, "type")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Required property validation                                          *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("requiredValidationProvider")
    void validateAndCollect_RequiredProperty(String schemaJson, String instanceJson,
                                            boolean expectValid, String missingProp) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid(),
                "Validity does not match for instance: " + instanceJson);
        if (!expectValid) {
            assertTrue(result.getMessages().stream()
                    .anyMatch(m -> m.getMessage().contains(missingProp)),
                    "Missing property message not found");
        }
    }

    private static Stream<Arguments> requiredValidationProvider() {
        String schema = """
                {
                  "type": "object",
                  "required": ["name"]
                }
                """;
        return Stream.of(
                Arguments.of(schema, "{}", false, "name"),
                Arguments.of(schema, """{ "name": "John", "age": 25 }""", true, "")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Recursive anchor handling                                            *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_RecursiveAnchor_DoesNotRecurseInfinitely() throws Exception {
        String schema = """
                {
                  "$recursiveAnchor": true,
                  "type": "object",
                  "properties": {
                    "self": { "$ref": "#" }
                  }
                }
                """;
        jsonSchema = schemaFrom(schema);
        JsonNode instance = MAPPER.readTree("""{ "self": { "$ref": "#" } }""");
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertNotNull(result);
        // No stack overflow – we simply assert the validation completed.
        // The schema does not define any required properties, so it should be valid.
        assertTrue(result.isValid());
    }

    /* --------------------------------------------------------------------- *
     *  Keyword walk listener invocation                                      *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_WalkListenerRunnerIsInvoked() throws Exception {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "title": { "type": "string" }
                  }
                }
                """;
        jsonSchema = schemaFrom(schema);

        // Inject the mock listener via reflection
        Field field = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        field.setAccessible(true);
        field.set(jsonSchema, mockListener);

        JsonNode instance = MAPPER.readTree("""{ "title": "Example" }""");
        jsonSchema.validateAndCollect(instance);

        // Verify that the listener's run method was called exactly once.
        verify(mockListener, times(1)).run(any());
    }

    /* --------------------------------------------------------------------- *
     *  Schema resource root shortcut                                         *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_SchemaResourceRootShortcut() throws Exception {
        String schema = """
                {
                  "$id": "http://example.com/schemas/base/",
                  "type": "object",
                  "properties": {
                    "id": { "type": "string" }
                  }
                }
                """;
        jsonSchema = schemaFrom(schema);

        // Spy to force isSchemaResourceRoot() to return true
        JsonSchema spy = spy(jsonSchema);
        doReturn(true).when(spy).isSchemaResourceRoot();

        JsonNode instance = MAPPER.readTree("""{ "id": "http://example.com/schemas/base/" }""");
        ValidationResult result = spy.validateAndCollect(instance);
        assertTrue(result.isValid());
    }

    /* --------------------------------------------------------------------- *
     *  Enum validation                                                      *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("enumValidationProvider")
    void validateAndCollect_Enum(String schemaJson, String instanceJson,
                                 boolean expectValid) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid());
    }

    private static Stream<Arguments> enumValidationProvider() {
        String schema = """
                {
                  "enum": ["RED", "GREEN", "BLUE"]
                }
                """;
        return Stream.of(
                Arguments.of(schema, "\"RED\"", true),
                Arguments.of(schema, "\"YELLOW\"", false)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Const validation                                                      *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("constValidationProvider")
    void validateAndCollect_Const(String schemaJson, String instanceJson,
                                  boolean expectValid) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid());
    }

    private static Stream<Arguments> constValidationProvider() {
        String schema = """
                { "const": 42 }
                """;
        return Stream.of(
                Arguments.of(schema, "42", true),
                Arguments.of(schema, "43", false)
        );
    }

    /* --------------------------------------------------------------------- *
     *  String pattern validation                                            *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("patternValidationProvider")
    void validateAndCollect_Pattern(String schemaJson, String instanceJson,
                                   boolean expectValid) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid());
    }

    private static Stream<Arguments> patternValidationProvider() {
        String schema = """
                {
                  "type": "string",
                  "pattern": "^[A-Z]{2}\\d{4}$"
                }
                """;
        return Stream.of(
                Arguments.of(schema, "\"AB1234\"", true),
                Arguments.of(schema, "\"ab1234\"", false)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Numeric range validation (minimum / maximum)                         *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("numericRangeProvider")
    void validateAndCollect_NumericRange(String schemaJson, String instanceJson,
                                         boolean expectValid) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid());
    }

    private static Stream<Arguments> numericRangeProvider() {
        String schema = """
                {
                  "type": "number",
                  "minimum": 10,
                  "maximum": 20
                }
                """;
        return Stream.of(
                Arguments.of(schema, "15", true),
                Arguments.of(schema, "9", false),
                Arguments.of(schema, "21", false)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Array length validation (minItems / maxItems)                        *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("arrayLengthProvider")
    void validateAndCollect_ArrayLength(String schemaJson, String instanceJson,
                                         boolean expectValid) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid());
    }

    private static Stream<Arguments> arrayLengthProvider() {
        String schema = """
                {
                  "type": "array",
                  "minItems": 2,
                  "maxItems": 5
                }
                """;
        return Stream.of(
                Arguments.of(schema, "[1,2,3]", true),
                Arguments.of(schema, "[1]", false),
                Arguments.of(schema, "[1,2,3,4,5,6]", false)
        );
    }

    /* --------------------------------------------------------------------- *
     *  AdditionalProperties = false                                         *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("additionalPropertiesProvider")
    void validateAndCollect_AdditionalProperties(String schemaJson, String instanceJson,
                                                 boolean expectValid) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid());
    }

    private static Stream<Arguments> additionalPropertiesProvider() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "allowed": { "type": "string" }
                  },
                  "additionalProperties": false
                }
                """;
        return Stream.of(
                Arguments.of(schema, """{ "allowed": "yes" }""", true),
                Arguments.of(schema, """{ "allowed": "yes", "extra": 123 }""", false)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Format validation (date-time)                                         *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("dateTimeFormatProvider")
    void validateAndCollect_DateTimeFormat(String schemaJson, String instanceJson,
                                           boolean expectValid) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectValid, result.isValid());
    }

    private static Stream<Arguments> dateTimeFormatProvider() {
        String schema = """
                {
                  "type": "string",
                  "format": "date-time"
                }
                """;
        return Stream.of(
                Arguments.of(schema, "\"2023-08-15T13:45:30Z\"", true),
                Arguments.of(schema, "\"15-08-2023 13:45\"", false)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Multiple validators combined (type + minLength + pattern)            *
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("multipleValidatorsProvider")
    void validateAndCollect_MultipleValidators(String schemaJson, String instanceJson,
                                                int expectedMessageCount) throws Exception {
        jsonSchema = schemaFrom(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertEquals(expectedMessageCount, result.getMessages().size(),
                "Unexpected number of validation messages");
    }

    private static Stream<Arguments> multipleValidatorsProvider() {
        String schema = """
                {
                  "type": "string",
                  "minLength": 4,
                  "pattern": "^[A-Z]{2}\\d{2}$"
                }
                """;
        return Stream.of(
                Arguments.of(schema, "\"AB12\"", 0),
                Arguments.of(schema, "\"A12\"", 2)   // length <4 and pattern mismatch
        );
    }

    /* --------------------------------------------------------------------- *
     *  Keyword Walk Listener Runner null (no invocation)                  *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_WalkListenerRunnerNull_NoInvocation() throws Exception {
        String schema = """
                { "type": "object" }
                """;
        jsonSchema = schemaFrom(schema);
        // Ensure the field is null
        Field f = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        f.setAccessible(true);
        f.set(jsonSchema, null);

        JsonNode instance = MAPPER.readTree("""{ "simple": true }""");
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertTrue(result.isValid());
        // No NPE should have been thrown – the test passes if we reach here.
    }

    /* --------------------------------------------------------------------- *
     *  $id base‑URI handling with relative $ref                              *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_IdBaseUriResolution() throws Exception {
        String schema = """
                {
                  "$id": "http://example.com/schemas/",
                  "type": "object",
                  "properties": {
                    "ref": { "$ref": "other-schema.json#/defs/value" }
                  },
                  "definitions": {
                    "value": { "type": "string" }
                  }
                }
                """;
        jsonSchema = schemaFrom(schema);
        JsonNode instance = MAPPER.readTree("""
                { "ref": "hello" }
                """);
        ValidationResult result = jsonSchema.validateAndCollect(instance);
        assertTrue(result.isValid());
    }

    /* --------------------------------------------------------------------- *
     *  ExecutionContext customiser – add a custom format validator          *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_ExecutionContextCustomizerAddsCustomFormat() throws Exception {
        String schema = """
                {
                  "type": "string",
                  "format": "custom-format"
                }
                """;
        jsonSchema = schemaFrom(schema);

        // Build an ExecutionContext and add a custom format validator that always fails.
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        ctx.getExecutionConfig().addCustomFormat("custom-format",
                (value, node) -> Collections.singletonList(new ValidationMessage.Builder()
                        .code("custom-format")
                        .path("")
                        .message("custom format not satisfied")
                        .type(ValidationMessage.Type.ERROR)
                        .build()));

        JsonNode instance = MAPPER.readTree("\"CUSTOM-123\"");
        ValidationResult result = jsonSchema.validateAndCollect(ctx, instance);
        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("custom format")));
    }

    /* --------------------------------------------------------------------- *
     *  Invalid $ref URI – IllegalArgumentException                           *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_InvalidRefUri_ThrowsIllegalArgumentException() throws Exception {
        String schema = """
                {
                  "$ref": "ht!tp://[invalid]"
                }
                """;
        jsonSchema = schemaFrom(schema);
        JsonNode instance = MAPPER.readTree("{}");
        assertThrows(IllegalArgumentException.class,
                () -> jsonSchema.validateAndCollect(instance));
    }

    /* --------------------------------------------------------------------- *
     *  Private overload invoked with null ExecutionContext – NPE             *
     * --------------------------------------------------------------------- */

    @Test
    void privateValidateAndCollect_NullExecutionContext_ThrowsNPE() throws Exception {
        String schema = """
                { "type": "object" }
                """;
        jsonSchema = schemaFrom(schema);
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");

        // Access the private overload via reflection
        Method privateMethod = JsonSchema.class.getDeclaredMethod(
                "validateAndCollect", ExecutionContext.class, JsonNode.class, JsonNode.class, JsonNodePath.class);
        privateMethod.setAccessible(true);

        assertThrows(NullPointerException.class,
                () -> privateMethod.invoke(jsonSchema, null, instance, instance, JsonNodePath.ROOT));
    }

    /* --------------------------------------------------------------------- *
     *  External schema reference not found – JsonProcessingException         *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_ExternalRefNotFound_ThrowsRuntimeException() throws Exception {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "person": { "$ref": "nonexistent-schema.json#/definitions/name" }
                  }
                }
                """;
        jsonSchema = schemaFrom(schema);
        JsonNode instance = MAPPER.readTree("""
                { "person": { "$ref": "nonexistent-schema.json#/definitions/name" } }
                """);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.validateAndCollect(instance));
        assertTrue(ex.getCause() instanceof JsonProcessingException,
                "Cause should be JsonProcessingException");
    }

    /* --------------------------------------------------------------------- *
     *  Custom validator throws unchecked exception – propagation            *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_CustomValidatorThrowsUncheckedException_Propagates() throws Exception {
        // Build a dummy schema and then spy it to inject a custom validator.
        String schema = """
                { "type": "object" }
                """;
        jsonSchema = schemaFrom(schema);
        JsonValidator explodingValidator = mock(JsonValidator.class);
        doThrow(new IllegalStateException("boom")).when(explodingValidator).validate(any(), any());

        JsonSchema spy = spy(jsonSchema);
        doReturn(List.of(explodingValidator)).when(spy).getValidators();

        JsonNode instance = MAPPER.readTree("{\"any\":\"value\"}");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> spy.validateAndCollect(instance));
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  UnsupportedEncodingException during $id decoding                      *
     * --------------------------------------------------------------------- */

    @Test
    void validateAndCollect_MalformedId_ThrowsUnsupportedEncodingException() throws Exception {
        String schema = """
                {
                  "$id": "http://example.com/schema%ZZ",
                  "type": "object"
                }
                """;
        jsonSchema = schemaFrom(schema);
        JsonNode instance = MAPPER.readTree("{}");
        // The library wraps the UnsupportedEncodingException in a RuntimeException.
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.validateAndCollect(instance));
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException,
                "Root cause should be UnsupportedEncodingException");
    }
}
```