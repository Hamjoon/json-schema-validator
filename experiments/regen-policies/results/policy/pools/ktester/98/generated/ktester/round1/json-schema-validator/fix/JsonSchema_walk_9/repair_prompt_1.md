
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_9_Test {

    /**
     * The factory used to create {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * A simple schema used in the tests – a JSON string type.
     */
    private static final String SCHEMA_DATA = "{ \"type\": \"string\" }";

    /**
     * Input that does NOT satisfy the schema (a number instead of a string).
     */
    private static final String INVALID_INPUT = "123";

    /**
     * Input that satisfies the schema (a JSON string).
     */
    private static final String VALID_INPUT = "\"hello\"";

    /**
     * Input format used for the tests.
     */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /**
     * The {@link JsonSchema} instance under test.
     */
    private static JsonSchema schema;

    /**
     * Mocked customizer – we only verify that it is invoked.
     */
    @Mock
    private ExecutionContextCustomizer executionContextCustomizer;

    @Mock
    private ExecutionContextCustomizer mockCustomizer;

    /**
     * Simple customizer that counts how many times it has been invoked.
     */
    private static class CountingCustomizer implements ExecutionContextCustomizer {

        int count = 0;

        @Override
        public void customize(ExecutionContext ctx) {
            count++;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft (the same version used in the original concurrency test)
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Build the schema once – it is thread‑safe and can be reused across tests
        schema = factory.getSchema(SCHEMA_DATA);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test so that previous interactions do not interfere
        reset(executionContextCustomizer);
    }

    /**
     * Helper to create a {@link JsonSchema} from a JSON schema string.
     */
    private static JsonSchema schemaOf(String schemaJson) {
        return factory.getSchema(schemaJson);
    }

    /**
     * Checks whether any {@link ValidationMessage} in the result contains the given text (case‑insensitive).
     */
    private static boolean containsMessageContaining(ValidationResult result, String text) {
        for (ValidationMessage vm : result.getMessages()) {
            if (vm.getMessage().toLowerCase().contains(text.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * When {@code validate} is {@code true} the walk operation must perform validation
     * and return the corresponding {@link ValidationResult}. The customizer should be
     * invoked exactly once.
     */
    @Test
    void walkShouldValidateAndReturnMessagesWhenValidateTrue() {
        // GIVEN
        CountingCustomizer customizer = new CountingCustomizer();
        // WHEN
        ValidationResult result = schema.walk(INVALID_INPUT, INPUT_FORMAT, true, customizer);
        // THEN
        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertEquals(1, messages.size(), "Invalid input should produce exactly one validation message");
        assertEquals(1, customizer.count, "Customizer should be invoked exactly once");
    }

    /**
     * When {@code validate} is {@code false} the walk operation must skip validation
     * and therefore return an empty set of messages. The customizer is still expected
     * to be invoked.
     */
    @Test
    void walkShouldSkipValidationWhenValidateFalse() {
        // GIVEN
        CountingCustomizer customizer = new CountingCustomizer();
        // WHEN
        ValidationResult result = schema.walk(VALID_INPUT, INPUT_FORMAT, false, customizer);
        // THEN
        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(), "Validation should be skipped when the validate flag is false");
        assertEquals(1, customizer.count, "Customizer should be invoked exactly once");
    }

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
        String input = """
            { "name": "John" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        assertTrue(result.isSuccess(), "Result should be successful");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

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
        String input = """
            { "age": 30 }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, false, (ExecutionContextCustomizer) null);
        assertTrue(result.isSuccess(), "Result should be successful even when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void malformedJson_returnsFailureResult() {
        String schema = """
            { "type": "object" }""";
        // missing closing brace
        String malformed = "{invalidJson: true,";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(malformed, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        assertFalse(result.isSuccess(), "Result should indicate failure");
        assertTrue(containsMessageContaining(result, "parsing"), "Message should mention parsing error");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullInput_throwsNullPointerException() {
        String schema = """
            { "type": "object" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(null, InputFormat.JSON, true, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------------- */
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

    /* --------------------------------------------------------------------- */
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

    /* --------------------------------------------------------------------- */
    @Test
    void customizerThrowsRuntimeException_propagates() {
        String schema = """
            { "type": "object" }""";
        String input = """
            { "any": "value" }""";
        ExecutionContextCustomizer failingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(input, InputFormat.JSON, true, failingCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

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
        String input = """
            { "age": 25 }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        assertFalse(result.isSuccess(), "Result should be failure because required property is missing");
        assertTrue(containsMessageContaining(result, "required"), "Message should contain 'required'");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void arrayInputAgainstObjectSchema_returnsTypeMismatch() {
        String schema = """
            { "type": "object" }""";
        String input = """
            [{"type":"object"}]""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        assertFalse(result.isSuccess(), "Array input should not match object schema");
        assertTrue(containsMessageContaining(result, "type"), "Message should mention type mismatch");
    }

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
        ValidationResult result = jsonSchema.walk(yamlInput, InputFormat.YAML, true, (ExecutionContextCustomizer) null);
        assertTrue(result.isSuccess(), "YAML input should be parsed and validated successfully");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

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
        String input = """
            { "self": {} }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        assertTrue(result.isSuccess(), "Recursive anchor should be handled without infinite recursion");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

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
        String input = """
            { "address": { "city": "Paris" } }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        assertTrue(result.isSuccess(), "Reference should be resolved and validation succeed");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

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
        String input = """
            { "email": "not-an-email" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ExecutionContextCustomizer disableFormat = ctx -> ctx.getValidationContext().getConfig().setFormatEnabled(false);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, disableFormat);
        assertTrue(result.isSuccess(), "Validation should succeed because format check is disabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validationDisabled_customizerThrows_propagatesException() {
        String schema = """
            { "type": "object" }""";
        String input = """
            { "any": "value" }""";
        ExecutionContextCustomizer failing = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(input, InputFormat.JSON, false, failing));
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyStringInput_returnsParsingFailure() {
        String schema = """
            { "type": "object" }""";
        // empty
        String input = "";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        assertFalse(result.isSuccess(), "Empty input should cause parsing failure");
        assertTrue(containsMessageContaining(result, "parsing"), "Message should mention parsing error");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullInputFormat_throwsNullPointerException() {
        String schema = """
            { "type": "object" }""";
        String input = """
            { "any": "value" }""";
        JsonSchema jsonSchema = schemaOf(schema);
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(input, null, true, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void customizerThrowsAssertionError_propagates() {
        String schema = """
            { "type": "object" }""";
        String input = """
            { "any": "value" }""";
        ExecutionContextCustomizer failing = ctx -> {
            throw new AssertionError("customizer assertion failure");
        };
        JsonSchema jsonSchema = schemaOf(schema);
        AssertionError err = assertThrows(AssertionError.class, () -> jsonSchema.walk(input, InputFormat.JSON, true, failing));
        assertEquals("customizer assertion failure", err.getMessage());
    }

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
        String input = """
            { "a": {} }""";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.JSON, true, (ExecutionContextCustomizer) null);
        // The library should not crash; it will report a recursion depth limit.
        assertFalse(result.isSuccess(), "Recursive schema without anchor should fail gracefully");
        assertTrue(containsMessageContaining(result, "recursion") || containsMessageContaining(result, "depth"), "Message should mention recursion or depth limit");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void urlEncodedInvalidPercentEncoding_returnsFailure() {
        String schema = """
            { "type": "object" }""";
        String input = "%ZZ%GG";
        JsonSchema jsonSchema = schemaOf(schema);
        ValidationResult result = jsonSchema.walk(input, InputFormat.URL_ENCODED, true, null);
        assertFalse(result.isSuccess(), "Malformed percent‑encoding should cause failure");
        assertTrue(containsMessageContaining(result, "decoding") || containsMessageContaining(result, "UnsupportedEncodingException"), "Message should mention decoding error");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:6: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:7: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:10: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:46: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_9_Test
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:49: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_9_Test
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:55: error: CountingCustomizer is not abstract and does not override abstract method customize(ExecutionContext,ValidationContext) in ExecutionContextCustomizer
    private static class CountingCustomizer implements ExecutionContextCustomizer {
                   ^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:59: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:76: error: cannot find symbol
        reset(executionContextCustomizer);
        ^
  symbol:   method reset(ExecutionContextCustomizer)
  location: class JsonSchema_walk_9_Test
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:90: error: cannot find symbol
        for (ValidationMessage vm : result.getMessages()) {
                                          ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:111: error: cannot find symbol
        Set<ValidationMessage> messages = result.getMessages();
                                                ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:129: error: cannot find symbol
        Set<ValidationMessage> messages = result.getMessages();
                                                ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:150: error: cannot find symbol
        assertTrue(result.isSuccess(), "Result should be successful");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:151: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:168: error: cannot find symbol
        assertTrue(result.isSuccess(), "Result should be successful even when validation is disabled");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:169: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:181: error: cannot find symbol
        assertFalse(result.isSuccess(), "Result should indicate failure");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:201: error: cannot find symbol
        ValidationResult result = jsonSchema.walk(input, InputFormat.XML, true, null);
                                                                    ^
  symbol:   variable XML
  location: class InputFormat
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:202: error: cannot find symbol
        assertFalse(result.isSuccess(), "XML format is not supported – should fail");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:226: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer failingCustomizer = ctx -> {
                                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:249: error: cannot find symbol
        assertFalse(result.isSuccess(), "Result should be failure because required property is missing");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:262: error: cannot find symbol
        assertFalse(result.isSuccess(), "Array input should not match object schema");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:284: error: cannot find symbol
        assertTrue(result.isSuccess(), "YAML input should be parsed and validated successfully");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:285: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:304: error: cannot find symbol
        assertTrue(result.isSuccess(), "Recursive anchor should be handled without infinite recursion");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:305: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:331: error: cannot find symbol
        assertTrue(result.isSuccess(), "Reference should be resolved and validation succeed");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:332: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:348: error: cannot find symbol
        ExecutionContextCustomizer disableFormat = ctx -> ctx.getValidationContext().getConfig().setFormatEnabled(false);
                                                             ^
  symbol:   method getValidationContext()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:348: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer disableFormat = ctx -> ctx.getValidationContext().getConfig().setFormatEnabled(false);
                                                   ^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:350: error: cannot find symbol
        assertTrue(result.isSuccess(), "Validation should succeed because format check is disabled");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:351: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:361: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer failing = ctx -> {
                                             ^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:378: error: cannot find symbol
        assertFalse(result.isSuccess(), "Empty input should cause parsing failure");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:400: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer failing = ctx -> {
                                             ^
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:424: error: cannot find symbol
        assertFalse(result.isSuccess(), "Recursive schema without anchor should fail gracefully");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:435: error: cannot find symbol
        ValidationResult result = jsonSchema.walk(input, InputFormat.URL_ENCODED, true, null);
                                                                    ^
  symbol:   variable URL_ENCODED
  location: class InputFormat
src/test/java/com/networknt/schema/JsonSchema_walk_9_Test.java:436: error: cannot find symbol
        assertFalse(result.isSuccess(), "Malformed percent‑encoding should cause failure");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
37 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(String, InputFormat, boolean, ExecutionContextCustomizer)': '\n     * Walk the input.\n     * \n     * @param input       the input\n     * @param inputFormat the input format\n     * @param validate    true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     * @return the validation result\n     ', 'return type': 'com.networknt.schema.ValidationResult', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public ValidationResult walk(String, InputFormat, boolean, ExecutionContextCustomizer)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the input.\n     * \n     * @param input       the input\n     * @param inputFormat the input format\n     * @param validate    true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     * @return the validation result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


