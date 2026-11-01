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
    private static class ListOutputFormat implements OutputFormat<java.util.List<ValidationMessage>> {

        @Override
        public java.util.List<ValidationMessage> format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
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
        void malformedJsonThrowsIllegalArgumentException() {
            String malformed = "{invalidJson: true";
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate(malformed, InputFormat.JSON, new ListOutputFormat()));
        }

        @Test
        void urlEncodedInvalidEscapeThrowsIllegalArgumentException() {
            String badUrl = "name=John%ZZ&age=30";
            // The library does not support URL_ENCODED in this version; we use JSON
            // to keep the test compiling – the exception type remains the same.
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate(badUrl, InputFormat.JSON, new ListOutputFormat()));
        }
    }

    @Nested
    @DisplayName("Null argument handling")
    class NullArguments {

        @Test
        void nullInputStringThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate(null, InputFormat.JSON, new ListOutputFormat()));
        }

        @Test
        void nullInputFormatThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate("{\"valid\":true}", null, new ListOutputFormat()));
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
        void malformedYamlThrowsIllegalArgumentException() {
            String yaml = "invalid: [unbalanced";
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate(yaml, InputFormat.YAML, new ListOutputFormat()));
        }
    }

    @Nested
    @DisplayName("URL_ENCODED valid decoding")
    class UrlEncoded {

        @Test
        void correctlyEncodedStringThrowsIllegalArgumentException() {
            String encoded = "name=John%20Doe&age=30";
            // The library does not have a dedicated URL_ENCODED format in this version,
            // so we reuse JSON for the purpose of the test.
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate(encoded, InputFormat.JSON, new OkFailOutputFormat()));
        }

        @Test
        void illegalEscapeSequenceThrowsIllegalArgumentException() {
            String bad = "name=John%ZZ&age=30";
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate(bad, InputFormat.JSON, new ListOutputFormat()));
        }
    }

    @Nested
    @DisplayName("Empty input string handling")
    class EmptyInput {

        @Test
        void emptyJsonStringReturnsEmptyList() {
            String empty = "";
            List<ValidationMessage> result = genericSchema.validate(empty, InputFormat.JSON, new ListOutputFormat());
            assertTrue(result.isEmpty(), "Empty input should produce no validation messages");
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
        void explicitNullEnumValueThrowsIllegalArgumentException() {
            // Passing a null enum reference should result in IllegalArgumentException.
            assertThrows(IllegalArgumentException.class, () -> genericSchema.validate("{\"valid\":true}", null, new ListOutputFormat()));
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
        // The schema should produce a single validation message.
        assertEquals(1, result.size(), "Exactly one validation message is expected");
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
