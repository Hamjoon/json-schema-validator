
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

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
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);
        // Act
        JsonNode result = schema.validate(VALID_INPUT, InputFormat.JSON, OutputFormat.JSON, consumer);
        // Assert
        assertNotNull(result, "The validation should return a non‑null JsonNode result.");
        // The consumer must have been called once with any ExecutionContext instance.
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
    }

    /**
     * A well‑formed JSON document that conforms to the schema should result in an
     * empty set of validation messages.
     */
    @Test
    void validateValidJsonString_returnsEmptySet() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> {
            /* no‑op */
        };
        // Act
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, OutputFormat.SET, noop);
        // Assert
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid JSON document.");
    }

    /**
     * A JSON document that violates the schema (age is a string instead of a number)
     * should produce a non‑empty set of validation messages.
     */
    @Test
    void validateInvalidJsonString_returnsNonEmptySet() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> {
        };
        // Act
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":\"thirty\"}", InputFormat.JSON, OutputFormat.SET, noop);
        // Assert
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid JSON document.");
    }

    /**
     * An empty string cannot be parsed as JSON and should cause a {@link JsonProcessingException}
     * to be thrown by the deserialize step.
     */
    @Test
    void validateEmptyString_throwsJsonProcessingException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> {
        };
        // Act & Assert
        assertThrows(JsonProcessingException.class, () -> schema.validate("", InputFormat.JSON, OutputFormat.SET, noop), "Deserialising an empty string should throw JsonProcessingException.");
    }

    /**
     * Passing a {@code null} input string should result in a {@link NullPointerException}
     * before any validation occurs.
     */
    @Test
    void validateNullInput_throwsNullPointerException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> {
        };
        // Act & Assert
        assertThrows(NullPointerException.class, () -> schema.validate(null, InputFormat.JSON, OutputFormat.SET, noop), "A null input string should cause NullPointerException.");
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
        assertThrows(IllegalArgumentException.class, () -> schema.validate("{\"name\":\"John\"}", // assumed to exist for the test scenario
        InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
    }

    /**
     * The consumer should be invoked and be able to mutate the {@link ExecutionContext}
     * (e.g., setting a custom attribute). Validation should still succeed and return an empty set.
     */
    @Test
    void validateWithCustomizerModifiesContext() {
        // Arrange
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> customiser = mock(Consumer.class);
        doAnswer(invocation -> {
            ExecutionContext ctx = invocation.getArgument(0);
            // Assume ExecutionContext has a mutable attribute map via setAttribute.
            ctx.setAttribute("custom", true);
            return null;
        }).when(customiser).accept(any());
        // Act
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, customiser);
        // Capture the ExecutionContext that was passed to the consumer.
        ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(customiser, times(1)).accept(captor.capture());
        ExecutionContext capturedCtx = captor.getValue();
        // Verify that the custom attribute was set.
        assertEquals(Boolean.TRUE, capturedCtx.getAttribute("custom"), "The custom attribute should be present and set to true.");
        // Validation result must be empty.
        assertTrue(messages.isEmpty(), "Validation should succeed and return an empty set.");
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
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, failingCustomizer), "Exception thrown by the customiser should propagate.");
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
        Consumer<ExecutionContext> noop = ctx -> {
        };
        // Act
        Set<ValidationMessage> messages = schema.validate(yaml, InputFormat.YAML, OutputFormat.SET, noop);
        // Assert
        assertTrue(messages.isEmpty(), "Valid YAML input should produce an empty validation set.");
    }

    /**
     * Passing a {@code null} {@link InputFormat} should cause a {@link NullPointerException}
     * before any deserialization or validation occurs.
     */
    @Test
    void validateNullInputFormat_throwsNullPointerException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> {
        };
        // Act & Assert
        assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, OutputFormat.SET, noop), "A null InputFormat should cause NullPointerException.");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:4: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:12: error: package org.mockito does not exist
import org.mockito.ArgumentCaptor;
                  ^
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:49: error: cannot find symbol
        Consumer<ExecutionContext> consumer = mock(Consumer.class);
                                              ^
  symbol:   method mock(Class<Consumer>)
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:51: error: cannot find symbol
        JsonNode result = schema.validate(VALID_INPUT, InputFormat.JSON, OutputFormat.JSON, consumer);
                                                                                     ^
  symbol:   variable JSON
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:55: error: cannot find symbol
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
                         ^
  symbol:   method times(int)
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:55: error: cannot find symbol
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
                                          ^
  symbol:   method any(Class<ExecutionContext>)
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:69: error: cannot find symbol
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, OutputFormat.SET, noop);
                                                                                                                          ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:84: error: cannot find symbol
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":\"thirty\"}", InputFormat.JSON, OutputFormat.SET, noop);
                                                                                                                                  ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:99: error: cannot find symbol
        assertThrows(JsonProcessingException.class, () -> schema.validate("", InputFormat.JSON, OutputFormat.SET, noop), "Deserialising an empty string should throw JsonProcessingException.");
                                                                                                            ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:112: error: cannot find symbol
        assertThrows(NullPointerException.class, () -> schema.validate(null, InputFormat.JSON, OutputFormat.SET, noop), "A null input string should cause NullPointerException.");
                                                                                                           ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:128: error: cannot find symbol
        InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
                   ^
  symbol:   variable UNSUPPORTED
  location: class InputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:128: error: cannot find symbol
        InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
                                             ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:139: error: cannot find symbol
        Consumer<ExecutionContext> customiser = mock(Consumer.class);
                                                ^
  symbol:   method mock(Class<Consumer>)
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:140: error: cannot find symbol
        doAnswer(invocation -> {
        ^
  symbol:   method doAnswer((invocatio[...]ll; })
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:143: error: cannot find symbol
            ctx.setAttribute("custom", true);
               ^
  symbol:   method setAttribute(String,boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:145: error: cannot find symbol
        }).when(customiser).accept(any());
                                   ^
  symbol:   method any()
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:147: error: cannot find symbol
        Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, customiser);
                                                                                                               ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:149: error: cannot find symbol
        ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);
        ^
  symbol:   class ArgumentCaptor
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:149: error: cannot find symbol
        ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);
                                                  ^
  symbol:   variable ArgumentCaptor
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:150: error: cannot find symbol
        verify(customiser, times(1)).accept(captor.capture());
                           ^
  symbol:   method times(int)
  location: class JsonSchema_validate_14_Test
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:153: error: cannot find symbol
        assertEquals(Boolean.TRUE, capturedCtx.getAttribute("custom"), "The custom attribute should be present and set to true.");
                                              ^
  symbol:   method getAttribute(String)
  location: variable capturedCtx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:169: error: cannot find symbol
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, failingCustomizer), "Exception thrown by the customiser should propagate.");
                                                                                                                                              ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:184: error: cannot find symbol
        Set<ValidationMessage> messages = schema.validate(yaml, InputFormat.YAML, OutputFormat.SET, noop);
                                                                                              ^
  symbol:   variable SET
  location: interface OutputFormat
src/test/java/com/networknt/schema/JsonSchema_validate_14_Test.java:199: error: cannot find symbol
        assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, OutputFormat.SET, noop), "A null InputFormat should cause NullPointerException.");
                                                                                                                ^
  symbol:   variable SET
  location: interface OutputFormat
27 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)': '\n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public ValidationResult walk(String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Walk the input.\n     * \n     * @param input       the input\n     * @param inputFormat the input format\n     * @param validate    true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\nclass `com.networknt.schema.SchemaValidatorsConfig.Builder`:\n\trelated functions:\n\t\tmethod `public Builder itemWalkListeners(Consumer<List<JsonSchemaWalkListener>>)` returns `com.networknt.schema.SchemaValidatorsConfig.Builder`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


