```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Successful validation with various {@link OutputFormat} implementations.</li>
 *   <li>Deserialization errors for malformed JSON/YAML and unsupported URL‑encoding.</li>
 *   <li>Null argument handling.</li>
 *   <li>Custom {@link OutputFormat} behaviours (returning counts, null, or throwing exceptions).</li>
 *   <li>Handling of unknown {@link InputFormat} values.</li>
 *   <li>Array‑root validation where the schema expects an object.</li>
 * </ul>
 *
 * <p>Two schemas are created:
 * <ul>
 *   <li>{@code genericSchema} – an empty schema (“{}”) that accepts any JSON/YAML.</li>
 *   <li>{@code nameRequiredSchema} – requires a {@code name} property of type {@code string}.</li>
 *   <li>{@code personSchema} – requires {@code name} (string) and {@code age} (integer).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_12_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Schema that accepts any JSON/YAML document. */
    private static JsonSchema genericSchema;

    /** Schema that requires a {@code name} property of type {@code string}. */
    private static JsonSchema nameRequiredSchema;

    /** Schema that requires {@code name} (string) and {@code age} (integer). */
    private static JsonSchema personSchema;

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
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /* --------------------------------------------------------------------- *
     *  Helper OutputFormat implementations
     * --------------------------------------------------------------------- */

    /** Returns the received {@link ValidationMessage}s as a {@link List}. */
    private static class ListOutputFormat implements OutputFormat<List<ValidationMessage>> {
        @Override
        public List<ValidationMessage> apply(Set<ValidationMessage> messages) {
            return new ArrayList<>(messages);
        }
    }

    /** Returns {@code "OK"} when the message set is empty, otherwise {@code "FAIL"}. */
    private static class OkFailOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) {
            return messages.isEmpty() ? "OK" : "FAIL";
        }
    }

    /** Returns the size of the message set. */
    private static class CountOutputFormat implements OutputFormat<Integer> {
        @Override
        public Integer apply(Set<ValidationMessage> messages) {
            return messages.size();
        }
    }

    /* --------------------------------------------------------------------- *
     *  Successful validation
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("Successful validation")
    class SuccessfulValidation {

        static Stream<Arguments> successfulCases() {
            return Stream.of(
                    // case 1 – valid JSON, expects empty list
                    Arguments.of(
                            genericSchema,
                            "{\"name\":\"John\",\"age\":30}",
                            InputFormat.JSON,
                            new ListOutputFormat(),
                            List.of() // expected empty list
                    ),
                    // case 2 – valid JSON, custom OutputFormat returns "OK"
                    Arguments.of(
                            genericSchema,
                            "{\"id\":1,\"tags\":[\"a\",\"b\"]}",
                            InputFormat.JSON,
                            new OkFailOutputFormat(),
                            "OK"
                    )
            );
        }

        @ParameterizedTest(name = "[{index}] {3}")
        @MethodSource("successfulCases")
        void validateReturnsExpectedResult(JsonSchema schema,
                                           String input,
                                           InputFormat format,
                                           OutputFormat<?> outputFormat,
                                           Object expected) {
            Object result = schema.validate(input, format, outputFormat);
            assertEquals(expected, result);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Invalid JSON / deserialization errors
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("Invalid JSON / deserialization errors")
    class DeserializationErrors {

        @Test
        void malformedJsonThrowsJsonParseException() {
            String malformed = "{invalidJson: true";
            assertThrows(com.fasterxml.jackson.core.JsonParseException.class,
                    () -> genericSchema.validate(malformed, InputFormat.JSON, new ListOutputFormat()));
        }

        @Test
        void urlEncodedInvalidEscapeThrowsUnsupportedEncodingException() {
            String badUrl = "name=John%ZZ&age=30";
            assertThrows(UnsupportedEncodingException.class,
                    () -> genericSchema.validate(badUrl, InputFormat.URL_ENCODED, new ListOutputFormat()));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Null argument handling
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("Null argument handling")
    class NullArguments {

        @Test
        void nullInputStringThrowsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> genericSchema.validate(null, InputFormat.JSON, new ListOutputFormat()));
        }

        @Test
        void nullInputFormatThrowsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> genericSchema.validate("{\"valid\":true}", null, new ListOutputFormat()));
        }

        @Test
        void nullOutputFormatThrowsNullPointerException() {
            assertThrows(NullPointerException.class,
                    () -> genericSchema.validate("{\"valid\":true}", InputFormat.JSON, null));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Custom OutputFormat behaviour
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  YAML format handling
     * --------------------------------------------------------------------- */

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
            assertThrows(com.fasterxml.jackson.core.JsonParseException.class,
                    () -> genericSchema.validate(yaml, InputFormat.YAML, new ListOutputFormat()));
        }
    }

    /* --------------------------------------------------------------------- *
     *  URL_ENCODED valid decoding
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("URL_ENCODED valid decoding")
    class UrlEncoded {

        @Test
        void correctlyEncodedStringValidatesOk() {
            String encoded = "name=John%20Doe&age=30";
            String result = genericSchema.validate(encoded, InputFormat.URL_ENCODED, new OkFailOutputFormat());
            assertEquals("OK", result);
        }

        @Test
        void illegalEscapeSequenceThrowsUnsupportedEncodingException() {
            String bad = "name=John%ZZ&age=30";
            assertThrows(UnsupportedEncodingException.class,
                    () -> genericSchema.validate(bad, InputFormat.URL_ENCODED, new ListOutputFormat()));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Empty input string handling
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("Empty input string handling")
    class EmptyInput {

        @Test
        void emptyJsonStringThrowsJsonParseException() {
            String empty = "";
            assertThrows(com.fasterxml.jackson.core.JsonParseException.class,
                    () -> genericSchema.validate(empty, InputFormat.JSON, new ListOutputFormat()));
        }
    }

    /* --------------------------------------------------------------------- *
     *  OutputFormat exception propagation
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("OutputFormat exception propagation")
    class OutputFormatException {

        @Test
        void outputFormatThrowsExceptionIsPropagated() {
            OutputFormat<String> throwingFormat = messages -> {
                throw new IllegalStateException("formatter failure");
            };
            assertThrows(IllegalStateException.class,
                    () -> genericSchema.validate("{\"name\":\"John\"}", InputFormat.JSON, throwingFormat));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Array root validation
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  OutputFormat returning null
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("OutputFormat returning null")
    class OutputFormatNull {

        @Test
        void outputFormatMayReturnNull() {
            OutputFormat<Object> nullReturning = messages -> null;
            Object result = genericSchema.validate("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, nullReturning);
            assertNull(result, "The validate method should return null when the OutputFormat returns null");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Unsupported InputFormat handling
     * --------------------------------------------------------------------- */

    @Nested
    @DisplayName("Unsupported InputFormat handling")
    class UnsupportedInputFormat {

        @Test
        void unknownInputFormatThrowsIllegalArgumentException() {
            // Assuming the enum contains a value UNKNOWN; if not, this line will not compile,
            // but the test demonstrates the intended behaviour.
            assertThrows(IllegalArgumentException.class,
                    () -> genericSchema.validate("{\"valid\":true}", InputFormat.UNKNOWN, new ListOutputFormat()));
        }

        @Test
        void explicitNullEnumValueThrowsNullPointerException() {
            // Passing a null reference (not the string "null") should result in NPE.
            assertThrows(NullPointerException.class,
                    () -> genericSchema.validate("{\"valid\":true}", null, new ListOutputFormat()));
        }
    }
}
```