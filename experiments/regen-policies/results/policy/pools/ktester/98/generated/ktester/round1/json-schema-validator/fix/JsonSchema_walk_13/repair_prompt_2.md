
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, OutputFormat, boolean)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_13_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private JsonSchemaFactory factory;

    /**
     * Simple schema that accepts any JSON object.
     */
    private JsonSchema schema;

    /**
     * Execution context used for the walk operation.
     */
    private ExecutionContext executionContext;

    /**
     * Input format for JSON strings.
     */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /**
     * Sample JSON input that conforms to the simple schema.
     */
    private static final String VALID_JSON = "{\"name\":\"John\"}";

    /**
     * Simple schema that accepts any JSON object.
     */
    private JsonSchema anyObjectSchema;

    /**
     * Schema that requires a {@code name} property (used for validation‑failure test).
     */
    private JsonSchema requiredNameSchema;

    /**
     * Simple {@link OutputFormat} that just returns the {@link ValidationResult}
     * it receives.
     */
    private static class IdentityOutputFormat implements OutputFormat<ValidationResult> {

        @Override
        public ValidationResult format(JsonSchema schema, Set<ValidationMessage> messages,
                                       ExecutionContext executionContext, ValidationContext validationContext) {
            // The library provides a constructor that accepts the message set.
            return new ValidationResult(messages);
        }
    }

    /**
     * {@link OutputFormat} that always throws a {@link RuntimeException}
     * when {@code format} is invoked.
     */
    private static class ThrowingOutputFormat<T> implements OutputFormat<T> {

        private final RuntimeException toThrow;

        ThrowingOutputFormat(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public T format(JsonSchema schema, Set<ValidationMessage> messages,
                        ExecutionContext executionContext, ValidationContext validationContext) {
            throw toThrow;
        }
    }

    /**
     * {@link OutputFormat} that throws a {@link RuntimeException} wrapping an
     * {@link IOException}. Used to verify exception‑wrapping behaviour.
     */
    private static class IOExceptionThrowingOutputFormat<T> implements OutputFormat<T> {

        @Override
        public T format(JsonSchema schema, Set<ValidationMessage> messages,
                        ExecutionContext executionContext, ValidationContext validationContext) {
            throw new RuntimeException(new IOException("simulated IO failure"));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft (the most recent at the time of writing).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Schema that accepts any object.
        anyObjectSchema = factory.getSchema("{\"type\":\"object\"}");
        // Schema that requires a string property "name".
        requiredNameSchema = factory.getSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        // Fresh execution context for each test.
        executionContext = new ExecutionContext();
    }

    private static Stream<Arguments> validJsonEnabledProvider() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema anyObj = f.getSchema("{\"type\":\"object\"}");
        JsonSchema reqName = f.getSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        return Stream.of(
                Arguments.of("Valid JSON input – validation enabled (any object schema)", "{\"name\":\"John\",\"age\":30}", anyObj),
                Arguments.of("Valid JSON input – validation enabled (required name schema)", "{\"name\":\"John\",\"age\":30}", reqName)
        );
    }

    /**
     * Verifies that {@code walk} forwards the {@link ValidationResult} to the
     * supplied {@link OutputFormat} and returns the value produced by the format.
     */
    @Test
    void walkShouldDelegateToOutputFormat_andReturnItsResult() {
        OutputFormat<ValidationResult> outputFormat = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(executionContext, VALID_JSON, INPUT_FORMAT, outputFormat, true);
        assertNotNull(result, "walk should return a non‑null ValidationResult");
        assertTrue(result.getMessages().isEmpty(), "The simple schema should produce no validation messages for valid JSON");
    }

    /**
     * Checks that when validation is disabled, the schema still walks the tree
     * and the OutputFormat receives a {@link ValidationResult} with an empty
     * message set.
     */
    @Test
    void walkWithoutValidationShouldStillReturnEmptyResult() {
        OutputFormat<ValidationResult> outputFormat = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(executionContext, "{\"name\":\"John\"}", INPUT_FORMAT, outputFormat, false);
        assertNotNull(result, "Result must never be null");
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled the result should contain no messages");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validJsonEnabledProvider")
    void validJsonInput_validationEnabled(String description, String json, JsonSchema schemaUnderTest) {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = schemaUnderTest.walk(new ExecutionContext(), json, InputFormat.JSON, format, true);
        assertTrue(result.isSuccess(), "Expected a successful ValidationResult for valid JSON with validation enabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages should be present");
    }

    @Test
    void validJsonInput_validationDisabled() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "{\"id\":123}", InputFormat.JSON, format, false);
        assertTrue(result.isSuccess(), "Result should be successful when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No messages expected when validation is disabled");
    }

    @Test
    void nullExecutionContext_isCreatedInternally() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(null, "{\"type\":\"string\"}", InputFormat.JSON, format, true);
        assertNotNull(result, "Result must not be null even when executionContext is null");
        assertTrue(result.isSuccess(), "Result should be successful for valid JSON");
    }

    @Test
    void emptyInputStringProducesEmptyNode() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "", InputFormat.JSON, format, true);
        if (result != null) {
            assertTrue(result.isSuccess(), "Empty input should lead to a successful (empty) result");
        }
    }

    @Test
    void invalidJsonDeserializationThrowsRuntimeException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"unclosed\":\"value\"", InputFormat.JSON, format, true));
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException caused by malformed JSON");
    }

    @Test
    void nullInputFormat_throwsIllegalArgumentException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"key\":\"value\"}", null, format, true));
        assertTrue(ex.getMessage().contains("InputFormat cannot be null"));
    }

    @Test
    void outputFormatThrowsDuringProcessing_propagatesException() {
        OutputFormat<ValidationResult> throwingFormat = new ThrowingOutputFormat<>(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"a\":1}", InputFormat.JSON, throwingFormat, true));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void yamlInputFormat_successfulWalk() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "name: Jane\nage: 25", InputFormat.YAML, format, true);
        assertTrue(result.isSuccess(), "YAML input should be processed successfully");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for valid YAML");
    }

    @Test
    void nullOutputFormatWithValidateTrue_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"x\":10}", InputFormat.JSON, null, true));
        assertTrue(ex.getMessage().contains("outputFormat"));
    }

    @Test
    void nullInputString_throwsNullPointerException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), null, InputFormat.JSON, format, true));
        assertTrue(ex.getMessage().contains("input"));
    }

    @Test
    void whitespaceOnlyInput_treatedAsEmptyJsonNode() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "   \n\t  ", InputFormat.JSON, format, false);
        assertTrue(result.isSuccess(), "Whitespace‑only input should result in a successful walk when validation is disabled");
    }

    @Test
    void invalidYamlDeserializationThrowsRuntimeException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "name: John\nage: [30,]", InputFormat.YAML, format, true));
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException caused by malformed YAML");
    }

    @Test
    void nullOutputFormatWithValidateFalse_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"a\":1}", InputFormat.JSON, null, false));
        assertTrue(ex.getMessage().contains("outputFormat"));
    }

    @Test
    void validationFailure_schemaViolationReported() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = requiredNameSchema.walk(new ExecutionContext(), "{\"age\":25}", InputFormat.JSON, format, true);
        assertFalse(result.isSuccess(), "Result should indicate failure when required property is missing");
        boolean containsNameMessage = result.getMessages().stream()
                .anyMatch(msg -> msg.getMessage().contains("name"));
        assertTrue(containsNameMessage, "Validation messages should mention missing required property 'name'");
    }

    @Test
    void customOutputFormatReturningNonValidationResult() {
        // Simple OutputFormat<Integer> that returns the size of the JSON array (3)
        OutputFormat<Integer> nodeCountFormat = new OutputFormat<Integer>() {
            @Override
            public Integer format(JsonSchema schema, Set<ValidationMessage> messages,
                                  ExecutionContext executionContext, ValidationContext validationContext) {
                // For the purpose of the test we just return a constant.
                return 3;
            }
        };
        Integer count = anyObjectSchema.walk(new ExecutionContext(), "[1,2,3]", InputFormat.JSON, nodeCountFormat, false);
        assertEquals(3, count, "NodeCountOutputFormat should return the number of elements in the array");
    }

    @Test
    void unsupportedInputFormat_throwsIllegalArgumentException() {
        // The library only supports JSON and YAML in this version.
        // Supplying an unsupported format (e.g., XML) should result in an exception.
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "some plain text", InputFormat.XML, format, true));
        assertTrue(ex.getMessage().contains("Unsupported InputFormat"));
    }

    @Test
    void outputFormatThrowsWhileValidationFlagFalse_exceptionPropagates() {
        OutputFormat<ValidationResult> throwingFormat = new ThrowingOutputFormat<>(new RuntimeException("format error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"x\":10}", InputFormat.JSON, throwingFormat, false));
        assertEquals("format error", ex.getMessage());
    }

    @Test
    void nullInputAndOutputFormat_inputFormatValidationPrecedesOutputFormat() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"key\":\"value\"}", null, null, true));
        assertTrue(ex.getMessage().contains("InputFormat cannot be null"));
    }

    @Test
    void unsupportedUrlEncodedInputFormat_throwsRuntimeExceptionWrappingUnsupportedEncodingException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "%ZZ%", InputFormat.URL_ENCODED, format, true));
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException,
                "Root cause should be UnsupportedEncodingException for malformed URL‑encoded data");
    }

    @Test
    void outputFormatThrowsCheckedIOException_isWrappedInRuntimeException() {
        OutputFormat<ValidationResult> format = new IOExceptionThrowingOutputFormat<>();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> anyObjectSchema.walk(new ExecutionContext(), "{\"a\":1}", InputFormat.JSON, format, true));
        assertTrue(ex.getCause() instanceof IOException,
                "The RuntimeException should wrap the original IOException");
    }

    @Test
    void executionContextNullAndInputFormatNull_inputFormatValidationStillFires() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> anyObjectSchema.walk(null, "{\"x\":10}", null, new IdentityOutputFormat(), false));
        assertTrue(ex.getMessage().contains("InputFormat cannot be null"));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:68: error: constructor ValidationResult in class ValidationResult cannot be applied to given types;
            return new ValidationResult(messages);
                   ^
  required: Set<ValidationMessage>,ExecutionContext
  found:    Set<ValidationMessage>
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:135: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "The simple schema should produce no validation messages for valid JSON");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:148: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled the result should contain no messages");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:156: error: cannot find symbol
        assertTrue(result.isSuccess(), "Expected a successful ValidationResult for valid JSON with validation enabled");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:157: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages should be present");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:164: error: cannot find symbol
        assertTrue(result.isSuccess(), "Result should be successful when validation is disabled");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:165: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No messages expected when validation is disabled");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:173: error: cannot find symbol
        assertTrue(result.isSuccess(), "Result should be successful for valid JSON");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:181: error: cannot find symbol
            assertTrue(result.isSuccess(), "Empty input should lead to a successful (empty) result");
                             ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:213: error: cannot find symbol
        assertTrue(result.isSuccess(), "YAML input should be processed successfully");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:214: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for valid YAML");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:236: error: cannot find symbol
        assertTrue(result.isSuccess(), "Whitespace‑only input should result in a successful walk when validation is disabled");
                         ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:258: error: cannot find symbol
        assertFalse(result.isSuccess(), "Result should indicate failure when required property is missing");
                          ^
  symbol:   method isSuccess()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:259: error: cannot find symbol
        boolean containsNameMessage = result.getMessages().stream()
                                            ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:285: error: cannot find symbol
                () -> anyObjectSchema.walk(new ExecutionContext(), "some plain text", InputFormat.XML, format, true));
                                                                                                 ^
  symbol:   variable XML
  location: class InputFormat
src/test/java/com/networknt/schema/JsonSchema_walk_13_Test.java:308: error: cannot find symbol
                () -> anyObjectSchema.walk(new ExecutionContext(), "%ZZ%", InputFormat.URL_ENCODED, format, true));
                                                                                      ^
  symbol:   variable URL_ENCODED
  location: class InputFormat
19 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)': '\n     * Walk the input.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)`, api document: \n     * Walk the input.\n     * \n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)`, api document: \n     * Walk at the node.\n     * \n     * @param executionContext the execution content\n     * @param node             the current node\n     * @param rootNode         the root node\n     * @param instanceLocation the instance location\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


