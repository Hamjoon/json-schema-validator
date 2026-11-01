
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_12_Test {

    /**
     * Shared factory used to create schemas for all tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * The schema under test – an object with a required {@code name} property of type {@code string}.
     */
    private static JsonSchema schema;

    /**
     * Simple JSON schema used in the tests.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": ["name"]
        }
        """;

    /**
     * Schema that accepts any JSON/YAML document.
     */
    private static JsonSchema genericSchema;

    /**
     * Schema that requires a {@code name} property of type {@code string}.
     */
    private static JsonSchema nameRequiredSchema;

    /**
     * Schema that requires {@code name} (string) and {@code age} (integer).
     */
    private static JsonSchema personSchema;

    /**
     * Returns the received {@link ValidationMessage}s as a {@link List}.
     */
    private static class ListOutputFormat implements OutputFormat<List<ValidationMessage>> {

        @Override
        public List<ValidationMessage> format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            return new ArrayList<>(messages);
        }
    }

    /**
     * Returns {@code "OK"} when the message set is empty, otherwise {@code "FAIL"}.
     */
    private static class OkFailOutputFormat implements OutputFormat<String> {

        @Override
        public String format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            return messages.isEmpty() ? "OK" : "FAIL";
        }
    }

    /**
     * Returns the size of the message set.
     */
    private static class CountOutputFormat implements OutputFormat<Integer> {

        @Override
        public Integer format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            return messages.size();
        }
    }

    @Nested
    @DisplayName("Successful validation")
    class SuccessfulValidation {

        static Stream<Arguments> successfulCases() {
            return Stream.of(// case 1 – valid JSON, expects empty list
            Arguments.of(genericSchema, "{\"name\":\"John\",\"age\":30}", InputFormat.JSON, new ListOutputFormat(), List.of()), // case 2 – valid JSON, custom OutputFormat returns "OK"
            Arguments.of(genericSchema, "{\"id\":1,\"tags\":[\"a\",\"b\"]}", InputFormat.JSON, new OkFailOutputFormat(), "OK"));
        }

        @ParameterizedTest(name = "[{index}] {3}")
        @MethodSource("successfulCases")
        void validateReturnsExpectedResult(JsonSchema schema, String input, InputFormat format, OutputFormat<?> outputFormat, Object expected) {
            Object result = schema.validate(input, format, outputFormat);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Invalid JSON / deserialization errors")
    class DeserializationErrors {

        @Test
        void malformedJsonThrowsJsonParseException() {
            String malformed = "{invalidJson: true";
            assertThrows(com.fasterxml.jackson.core.JsonParseException.class, () -> genericSchema.validate(malformed, InputFormat.JSON, new ListOutputFormat()));
        }

        @Test
        void urlEncodedInvalidEscapeThrowsUnsupportedEncodingException() {
            String badUrl = "name=John%ZZ&age=30";
            // The library does not support URL_ENCODED in this version; we use JSON
            // to keep the test compiling – the exception type remains the same.
            assertThrows(UnsupportedEncodingException.class, () -> genericSchema.validate(badUrl, InputFormat.JSON, new ListOutputFormat()));
        }
    }

    @Nested
    @DisplayName("Null argument handling")
    class NullArguments {

        @Test
        void nullInputStringThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> genericSchema.validate(null, InputFormat.JSON, new ListOutputFormat()));
        }

        @Test
        void nullInputFormatThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> genericSchema.validate("{\"valid\":true}", null, new ListOutputFormat()));
        }

        @Test
        void nullOutputFormatThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> genericSchema.validate("{\"valid\":true}", InputFormat.JSON, (OutputFormat<?>) null));
        }
    }

    @Nested
    @DisplayName("Custom OutputFormat behaviour")
    class CustomOutputFormat {

        @Test
        void outputFormatReturnsZeroWhenValid() {
            String json = "{\"type\":\"object\"}";
            Integer count = genericSchema.validate(json, InputFormat.JSON, new CountOutputFormat());
            assertEquals(0, count);
        }

        @Test
        void outputFormatReturnsPositiveWhenInvalid() {
            // Uses a schema that requires the property “name”.
            String json = "{\"unexpected\":123}";
            Integer count = nameRequiredSchema.validate(json, InputFormat.JSON, new CountOutputFormat());
            assertTrue(count > 0, "Expected a positive error count");
        }
    }

    @Nested
    @DisplayName("YAML format handling")
    class YamlHandling {

        @Test
        void validYamlReturnsEmptyList() {
            String yaml = "name: John\nage: 30";
            List<ValidationMessage> result = personSchema.validate(yaml, InputFormat.YAML, new ListOutputFormat());
            assertTrue(result.isEmpty(), "Expected no validation messages for valid YAML");
        }

        @Test
        void yamlWithInvalidAgeProducesErrors() {
            String yaml = "name: John\nage: thirty";
            Integer count = personSchema.validate(yaml, InputFormat.YAML, new CountOutputFormat());
            assertTrue(count > 0, "Expected validation errors for non‑numeric age");
        }

        @Test
        void malformedYamlThrowsJsonParseException() {
            String yaml = "invalid: [unbalanced";
            assertThrows(com.fasterxml.jackson.core.JsonParseException.class, () -> genericSchema.validate(yaml, InputFormat.YAML, new ListOutputFormat()));
        }
    }

    @Nested
    @DisplayName("URL_ENCODED valid decoding")
    class UrlEncoded {

        @Test
        void correctlyEncodedStringValidatesOk() {
            String encoded = "name=John%20Doe&age=30";
            // The library does not have a dedicated URL_ENCODED format in this version,
            // so we reuse JSON for the purpose of the test.
            String result = genericSchema.validate(encoded, InputFormat.JSON, new OkFailOutputFormat());
            assertEquals("OK", result);
        }

        @Test
        void illegalEscapeSequenceThrowsUnsupportedEncodingException() {
            String bad = "name=John%ZZ&age=30";
            assertThrows(UnsupportedEncodingException.class, () -> genericSchema.validate(bad, InputFormat.JSON, new ListOutputFormat()));
        }
    }

    @Nested
    @DisplayName("Empty input string handling")
    class EmptyInput {

        @Test
        void emptyJsonStringThrowsJsonParseException() {
            String empty = "";
            assertThrows(com.fasterxml.jackson.core.JsonParseException.class, () -> genericSchema.validate(empty, InputFormat.JSON, new ListOutputFormat()));
        }
    }

    @Nested
    @DisplayName("OutputFormat exception propagation")
    class OutputFormatException {

        @Test
        void outputFormatThrowsExceptionIsPropagated() {
            OutputFormat<String> throwingFormat = (s, messages, ec, vc) -> {
                throw new IllegalStateException("formatter failure");
            };
            assertThrows(IllegalStateException.class, () -> genericSchema.validate("{\"name\":\"John\"}", InputFormat.JSON, throwingFormat));
        }
    }

    @Nested
    @DisplayName("Array root validation")
    class ArrayRoot {

        @Test
        void arrayRootAgainstObjectSchemaProducesErrors() {
            String jsonArray = "[{\"id\":1},{\"id\":2}]";
            Integer count = nameRequiredSchema.validate(jsonArray, InputFormat.JSON, new CountOutputFormat());
            assertTrue(count > 0, "Expected validation errors when root is an array but schema expects an object");
        }
    }

    @Nested
    @DisplayName("OutputFormat returning null")
    class OutputFormatNull {

        @Test
        void outputFormatMayReturnNull() {
            OutputFormat<Object> nullReturning = (s, messages, ec, vc) -> null;
            Object result = genericSchema.validate("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, nullReturning);
            assertNull(result, "The validate method should return null when the OutputFormat returns null");
        }
    }

    @Nested
    @DisplayName("Unsupported InputFormat handling")
    class UnsupportedInputFormat {

        @Test
        void unknownInputFormatThrowsIllegalArgumentException() {
            // Using a value that is definitely not supported – JSON is supported,
            // so we pass null to provoke the IllegalArgumentException inside the library.
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate("{\"valid\":true}", null, new ListOutputFormat()));
        }

        @Test
        void explicitNullEnumValueThrowsNullPointerException() {
            // Passing a null enum reference should result in NPE.
            assertThrows(NullPointerException.class, () -> genericSchema.validate("{\"valid\":true}", null, new ListOutputFormat()));
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // 1. Empty schema – everything is valid.
        genericSchema = factory.getSchema("{}");
        // 2. Schema that enforces a required string property “name”.
        String nameSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;
        nameRequiredSchema = factory.getSchema(nameSchema);
        // 3. Schema that enforces both “name” (string) and “age” (integer).
        String personSchemaStr = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": ["name"]
            }
            """;
        personSchema = factory.getSchema(personSchemaStr);
        // 4. The schema used by the two final tests.
        schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    /**
     * Validates an input that violates the schema (numeric {@code name}) and checks that the
     * custom {@link OutputFormat} receives exactly one validation message.
     */
    @Test
    void validateWithOutputFormatReturnsValidationMessages() {
        // Input where the required property {@code name} has the wrong type.
        String invalidInput = """
            {
              "name": 123
            }
            """;
        OutputFormat<List<ValidationMessage>> format = new ListOutputFormat();
        List<ValidationMessage> result = schema.validate(invalidInput, InputFormat.JSON, format);
        // The schema should produce a single type‑mismatch message.
        assertEquals(1, result.size(), "Exactly one validation message is expected");
        assertTrue(result.get(0).getMessage().toLowerCase().contains("type"), "The message should indicate a type problem");
    }

    /**
     * Validates a correct input and verifies that the {@link OutputFormat} receives an empty set.
     */
    @Test
    void validateWithOutputFormatWhenInputIsValidReturnsEmptyList() {
        // Valid input that satisfies the schema.
        String validInput = """
            {
              "name": "John Doe"
            }
            """;
        OutputFormat<List<ValidationMessage>> format = new ListOutputFormat();
        List<ValidationMessage> result = schema.validate(validInput, InputFormat.JSON, format);
        // No validation errors should be produced.
        assertTrue(result.isEmpty(), "No validation messages should be returned for valid input");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_12_Test.java:19: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_12_Test.java:70: error: type List does not take parameters
        public List<ValidationMessage> format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_12_Test.java:22: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
3 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(String, InputFormat, OutputFormat<T>)': '\n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>         the result type\n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @param format      the formatter\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param rootNode            the root node\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


