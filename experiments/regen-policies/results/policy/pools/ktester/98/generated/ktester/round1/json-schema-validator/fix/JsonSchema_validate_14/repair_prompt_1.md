
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashSet;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_14_Test {

    /**
     * Instance of {@link JsonSchema} used in the tests.
     */
    private JsonSchema schema;

    /**
     * Simple JSON schema that accepts any object.
     */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /**
     * Simple JSON document that conforms to {@link #SIMPLE_SCHEMA}.
     */
    private static final String VALID_INPUT = "{}";

    /**
     * A tiny {@link Consumer} implementation that records whether it has been
     * invoked and stores the {@link ExecutionContext} it received.
     */
    private static final class RecordingConsumer implements Consumer<ExecutionContext> {

        boolean invoked = false;

        ExecutionContext receivedContext = null;

        @Override
        public void accept(ExecutionContext ctx) {
            this.invoked = true;
            this.receivedContext = ctx;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance using the public factory API.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    /**
     * Verifies that the {@code Consumer<ExecutionContext>} passed to the
     * {@code validate(String, InputFormat, OutputFormat, Consumer)} method
     * is invoked exactly once with the execution context used for validation.
     */
    @Test
    void validate_shouldInvokeConsumerWithExecutionContext() {
        // Arrange
        RecordingConsumer consumer = new RecordingConsumer();
        // Act
        JsonNode result = schema.validate(VALID_INPUT, InputFormat.JSON, OutputFormat.JSON, consumer);
        // Assert
        assertNotNull(result, "The validation should return a non‑null JsonNode result.");
        assertTrue(consumer.invoked, "The consumer must have been invoked.");
        assertNotNull(consumer.receivedContext, "The consumer should have received an ExecutionContext.");
    }

    /**
     * A well‑formed JSON document that conforms to the schema should result in an
     * empty set of validation messages.
     */
    @Test
    void validateValidJsonString_returnsEmptySet() {
        // Act
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":30}");
        // Assert
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid JSON document.");
    }

    /**
     * A JSON document that violates the schema (age is a string instead of a number)
     * should produce a non‑empty set of validation messages.
     */
    @Test
    void validateInvalidJsonString_returnsNonEmptySet() {
        // Act
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":\"thirty\"}");
        // Assert
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid JSON document.");
    }

    /**
     * An empty string cannot be parsed as JSON and should cause a {@link JsonProcessingException}
     * to be thrown by the deserialize step.
     */
    @Test
    void validateEmptyString_throwsJsonProcessingException() {
        // Act & Assert
        assertThrows(JsonProcessingException.class, () -> schema.validate("", InputFormat.JSON, OutputFormat.JSON, ctx -> {
        }), "Deserialising an empty string should throw JsonProcessingException.");
    }

    /**
     * Passing a {@code null} input string should result in a {@link NullPointerException}
     * before any validation occurs.
     */
    @Test
    void validateNullInput_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> schema.validate(null, InputFormat.JSON, OutputFormat.JSON, ctx -> {
        }), "A null input string should cause NullPointerException.");
    }

    /**
     * Supplying an {@link InputFormat} that the implementation does not support should
     * cause an {@link IllegalArgumentException}.
     * <p>
     * The library defines an {@code UNSUPPORTED} constant for the purpose of this test.
     */
    @Test
    void validateUnsupportedInputFormat_throwsIllegalArgumentException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> {
        };
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> // assumed to exist for the test scenario
        schema.// assumed to exist for the test scenario
        validate(// assumed to exist for the test scenario
        "{\"name\":\"John\"}", InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
    }

    /**
     * The consumer should be invoked and be able to mutate the {@link ExecutionContext}
     * (e.g., setting a custom attribute). Validation should still succeed and return an empty set.
     */
    @Test
    void validateWithCustomizerModifiesContext() {
        // Arrange
        Consumer<ExecutionContext> customiser = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
        // Act
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, customiser);
        // Since we used the JSON output format, the result is a JsonNode.
        // To check that validation succeeded we simply ensure no exception was thrown
        // and that the returned JsonNode is not null.
        assertNotNull(messages, "Result should not be null.");
        // Unfortunately the JSON output format does not give us direct access to the
        // ExecutionContext, but we can verify that the custom attribute was set
        // by re‑invoking validation with a consumer that captures the context.
        RecordingConsumer capture = new RecordingConsumer();
        schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, capture);
        assertEquals(Boolean.TRUE, capture.receivedContext.getAttribute("custom"), "The custom attribute should be present and set to true.");
    }

    /**
     * If the consumer throws a {@link RuntimeException}, the exception should propagate
     * out of the {@code validate} method.
     */
    @Test
    void validateCustomizerThrowsRuntimeException_propagatesException() {
        // Arrange
        Consumer<ExecutionContext> failingCustomizer = ctx -> {
            throw new RuntimeException("custom failure");
        };
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, failingCustomizer), "Exception thrown by the customiser should propagate.");
        assertEquals("custom failure", ex.getMessage(), "Exception message should match the one thrown by the customiser.");
    }

    /**
     * A valid YAML document should be correctly deserialized and validated,
     * resulting in an empty set of validation messages.
     */
    @Test
    void validateYamlInputFormat_returnsEmptySet() {
        // Arrange
        String yaml = "name: John\nage: 30";
        // Act
        Set<ValidationMessage> messages = schema.validate(yaml, InputFormat.YAML);
        // Assert
        assertTrue(messages.isEmpty(), "Valid YAML input should produce an empty validation set.");
    }

    /**
     * Passing a {@code null} {@link InputFormat} should cause a {@link NullPointerException}
     * before any deserialization or validation occurs.
     */
    @Test
    void validateNullInputFormat_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, OutputFormat.JSON, ctx -> {
        }), "A null InputFormat should cause NullPointerException.");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:67: error: cannot find symbol
        JsonNode result = schema.validate(VALID_INPUT, InputFormat.JSON, OutputFormat.JSON, consumer);
                                                                                     ^
  symbol:   variable JSON
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:81: error: no suitable method found for validate(String)
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":30}");
                                                ^
    method JsonSchema.validate(JsonNode) is not applicable
      (argument mismatch; String cannot be converted to JsonNode)
    method JsonSchema.<T#1>validate(JsonNode,OutputFormat<T#1>) is not applicable
      (cannot infer type-variable(s) T#1
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#2>validate(JsonNode,OutputFormat<T#2>,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#2
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#3>validate(JsonNode,OutputFormat<T#3>,Consumer<ExecutionContext>) is not applicable
      (cannot infer type-variable(s) T#3
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#4>validate(String,InputFormat,OutputFormat<T#4>) is not applicable
      (cannot infer type-variable(s) T#4
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#5>validate(String,InputFormat,OutputFormat<T#5>,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#5
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#6>validate(String,InputFormat,OutputFormat<T#6>,Consumer<ExecutionContext>) is not applicable
      (cannot infer type-variable(s) T#6
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#7>validate(ExecutionContext,JsonNode,OutputFormat<T#7>) is not applicable
      (cannot infer type-variable(s) T#7
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#8>validate(ExecutionContext,JsonNode,OutputFormat<T#8>,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#8
        (actual and formal argument lists differ in length))
  where T#1,T#2,T#3,T#4,T#5,T#6,T#7,T#8 are type-variables:
    T#1 extends Object declared in method <T#1>validate(JsonNode,OutputFormat<T#1>)
    T#2 extends Object declared in method <T#2>validate(JsonNode,OutputFormat<T#2>,ExecutionContextCustomizer)
    T#3 extends Object declared in method <T#3>validate(JsonNode,OutputFormat<T#3>,Consumer<ExecutionContext>)
    T#4 extends Object declared in method <T#4>validate(String,InputFormat,OutputFormat<T#4>)
    T#5 extends Object declared in method <T#5>validate(String,InputFormat,OutputFormat<T#5>,ExecutionContextCustomizer)
    T#6 extends Object declared in method <T#6>validate(String,InputFormat,OutputFormat<T#6>,Consumer<ExecutionContext>)
    T#7 extends Object declared in method <T#7>validate(ExecutionContext,JsonNode,OutputFormat<T#7>)
    T#8 extends Object declared in method <T#8>validate(ExecutionContext,JsonNode,OutputFormat<T#8>,ExecutionContextCustomizer)
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:93: error: no suitable method found for validate(String)
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":\"thirty\"}");
                                                ^
    method JsonSchema.validate(JsonNode) is not applicable
      (argument mismatch; String cannot be converted to JsonNode)
    method JsonSchema.<T#1>validate(JsonNode,OutputFormat<T#1>) is not applicable
      (cannot infer type-variable(s) T#1
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#2>validate(JsonNode,OutputFormat<T#2>,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#2
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#3>validate(JsonNode,OutputFormat<T#3>,Consumer<ExecutionContext>) is not applicable
      (cannot infer type-variable(s) T#3
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#4>validate(String,InputFormat,OutputFormat<T#4>) is not applicable
      (cannot infer type-variable(s) T#4
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#5>validate(String,InputFormat,OutputFormat<T#5>,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#5
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#6>validate(String,InputFormat,OutputFormat<T#6>,Consumer<ExecutionContext>) is not applicable
      (cannot infer type-variable(s) T#6
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#7>validate(ExecutionContext,JsonNode,OutputFormat<T#7>) is not applicable
      (cannot infer type-variable(s) T#7
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#8>validate(ExecutionContext,JsonNode,OutputFormat<T#8>,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#8
        (actual and formal argument lists differ in length))
  where T#1,T#2,T#3,T#4,T#5,T#6,T#7,T#8 are type-variables:
    T#1 extends Object declared in method <T#1>validate(JsonNode,OutputFormat<T#1>)
    T#2 extends Object declared in method <T#2>validate(JsonNode,OutputFormat<T#2>,ExecutionContextCustomizer)
    T#3 extends Object declared in method <T#3>validate(JsonNode,OutputFormat<T#3>,Consumer<ExecutionContext>)
    T#4 extends Object declared in method <T#4>validate(String,InputFormat,OutputFormat<T#4>)
    T#5 extends Object declared in method <T#5>validate(String,InputFormat,OutputFormat<T#5>,ExecutionContextCustomizer)
    T#6 extends Object declared in method <T#6>validate(String,InputFormat,OutputFormat<T#6>,Consumer<ExecutionContext>)
    T#7 extends Object declared in method <T#7>validate(ExecutionContext,JsonNode,OutputFormat<T#7>)
    T#8 extends Object declared in method <T#8>validate(ExecutionContext,JsonNode,OutputFormat<T#8>,ExecutionContextCustomizer)
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:105: error: cannot find symbol
        assertThrows(JsonProcessingException.class, () -> schema.validate("", InputFormat.JSON, OutputFormat.JSON, ctx -> {
                                                                                                            ^
  symbol:   variable JSON
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:116: error: cannot find symbol
        assertThrows(NullPointerException.class, () -> schema.validate(null, InputFormat.JSON, OutputFormat.JSON, ctx -> {
                                                                                                           ^
  symbol:   variable JSON
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:135: error: cannot find symbol
        "{\"name\":\"John\"}", InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
                                          ^
  symbol:   variable UNSUPPORTED
  location: class InputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:135: error: cannot find symbol
        "{\"name\":\"John\"}", InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
                                                                    ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:145: error: cannot find symbol
        Consumer<ExecutionContext> customiser = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
                                                          ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:147: error: cannot find symbol
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, customiser);
                                                                                                               ^
  symbol:   variable JSON
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:156: error: cannot find symbol
        schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, capture);
                                                                             ^
  symbol:   variable JSON
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:157: error: cannot find symbol
        assertEquals(Boolean.TRUE, capture.receivedContext.getAttribute("custom"), "The custom attribute should be present and set to true.");
                                                          ^
  symbol:   method getAttribute(String)
  location: variable receivedContext of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:171: error: cannot find symbol
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, failingCustomizer), "Exception thrown by the customiser should propagate.");
                                                                                                                                              ^
  symbol:   variable JSON
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:196: error: cannot find symbol
        assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, OutputFormat.JSON, ctx -> {
                                                                                                                ^
  symbol:   variable JSON
  location: interface OutputFormat
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
15 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)': '\n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public ValidationResult walk(String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Walk the input.\n     * \n     * @param input       the input\n     * @param inputFormat the input format\n     * @param validate    true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\nclass `com.networknt.schema.SchemaValidatorsConfig.Builder`:\n\trelated functions:\n\t\tmethod `public Builder itemWalkListeners(Consumer<List<JsonSchemaWalkListener>>)` returns `com.networknt.schema.SchemaValidatorsConfig.Builder`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


