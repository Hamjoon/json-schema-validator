


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
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

// @ExtendWith(MockitoExtension.class)
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

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_validate_12_Test ✔
│     ├─ validateWithOutputFormatWhenInputIsValidReturnsEmptyList() ✔
│     ├─ validateWithOutputFormatReturnsValidationMessages() ✘ The message should indicate a type problem ==> expected: <true> but was: <false>
│     ├─ Unsupported InputFormat handling ✔
│     │  ├─ unknownInputFormatThrowsIllegalArgumentException() ✔
│     │  └─ explicitNullEnumValueThrowsNullPointerException() ✘ Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
│     ├─ OutputFormat returning null ✔
│     │  └─ outputFormatMayReturnNull() ✔
│     ├─ Array root validation ✔
│     │  └─ arrayRootAgainstObjectSchemaProducesErrors() ✔
│     ├─ OutputFormat exception propagation ✔
│     │  └─ outputFormatThrowsExceptionIsPropagated() ✔
│     ├─ Empty input string handling ✔
│     │  └─ emptyJsonStringThrowsJsonParseException() ✘ Expected com.fasterxml.jackson.core.JsonParseException to be thrown, but nothing was thrown.
│     ├─ URL_ENCODED valid decoding ✔
│     │  ├─ illegalEscapeSequenceThrowsUnsupportedEncodingException() ✘ Unexpected exception type thrown, expected: <java.io.UnsupportedEncodingException> but was: <java.lang.IllegalArgumentException>
│     │  └─ correctlyEncodedStringValidatesOk() ✘ Invalid input
│     ├─ YAML format handling ✔
│     │  ├─ malformedYamlThrowsJsonParseException() ✘ Unexpected exception type thrown, expected: <com.fasterxml.jackson.core.JsonParseException> but was: <java.lang.IllegalArgumentException>
│     │  ├─ validYamlReturnsEmptyList() ✔
│     │  └─ yamlWithInvalidAgeProducesErrors() ✔
│     ├─ Custom OutputFormat behaviour ✔
│     │  ├─ outputFormatReturnsPositiveWhenInvalid() ✔
│     │  └─ outputFormatReturnsZeroWhenValid() ✔
│     ├─ Null argument handling ✔
│     │  ├─ nullInputStringThrowsNullPointerException() ✘ Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
│     │  ├─ nullInputFormatThrowsNullPointerException() ✘ Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
│     │  └─ nullOutputFormatThrowsNullPointerException() ✔
│     ├─ Invalid JSON / deserialization errors ✔
│     │  ├─ malformedJsonThrowsJsonParseException() ✘ Unexpected exception type thrown, expected: <com.fasterxml.jackson.core.JsonParseException> but was: <java.lang.IllegalArgumentException>
│     │  └─ urlEncodedInvalidEscapeThrowsUnsupportedEncodingException() ✘ Unexpected exception type thrown, expected: <java.io.UnsupportedEncodingException> but was: <java.lang.IllegalArgumentException>
│     └─ Successful validation ✔
│        └─ validateReturnsExpectedResult(JsonSchema, String, InputFormat, OutputFormat, Obj... ✔
│           ├─ [1] com.networknt.schema.JsonSchema_validate_12_Test$ListOutputFormat@7b5a12ae ✔
│           └─ [2] com.networknt.schema.JsonSchema_validate_12_Test$OkFailOutputFormat@476b0ae6 ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (10):
  JUnit Jupiter:JsonSchema_validate_12_Test:validateWithOutputFormatReturnsValidationMessages()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test', methodName = 'validateWithOutputFormatReturnsValidationMessages', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: The message should indicate a type problem ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_validate_12_Test.validateWithOutputFormatReturnsValidationMessages(JsonSchema_validate_12_Test.java:333)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:Unsupported InputFormat handling:explicitNullEnumValueThrowsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$UnsupportedInputFormat', methodName = 'explicitNullEnumValueThrowsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$UnsupportedInputFormat.explicitNullEnumValueThrowsNullPointerException(JsonSchema_validate_12_Test.java:279)
       [...]
     Caused by: java.lang.IllegalArgumentException: Unsupported input format null
       com.networknt.schema.JsonSchemaFactory.getObjectMapper(JsonSchemaFactory.java:495)
       com.networknt.schema.JsonSchemaFactory.readTree(JsonSchemaFactory.java:475)
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:947)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       com.networknt.schema.JsonSchema_validate_12_Test$UnsupportedInputFormat.lambda$explicitNullEnumValueThrowsNullPointerException$1(JsonSchema_validate_12_Test.java:279)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:Empty input string handling:emptyJsonStringThrowsJsonParseException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$EmptyInput', methodName = 'emptyJsonStringThrowsJsonParseException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected com.fasterxml.jackson.core.JsonParseException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$EmptyInput.emptyJsonStringThrowsJsonParseException(JsonSchema_validate_12_Test.java:224)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:URL_ENCODED valid decoding:illegalEscapeSequenceThrowsUnsupportedEncodingException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$UrlEncoded', methodName = 'illegalEscapeSequenceThrowsUnsupportedEncodingException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.io.UnsupportedEncodingException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$UrlEncoded.illegalEscapeSequenceThrowsUnsupportedEncodingException(JsonSchema_validate_12_Test.java:213)
       [...]
     Caused by: java.lang.IllegalArgumentException: Invalid input
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:949)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       com.networknt.schema.JsonSchema_validate_12_Test$UrlEncoded.lambda$illegalEscapeSequenceThrowsUnsupportedEncodingException$0(JsonSchema_validate_12_Test.java:213)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
     Caused by: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
       com.fasterxml.jackson.core.JsonParser._constructError(JsonParser.java:2567)
       com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2593)
       com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2601)
       com.fasterxml.jackson.core.base.ParserMinimalBase._reportError(ParserMinimalBase.java:765)
       com.fasterxml.jackson.core.json.ReaderBasedJsonParser._reportInvalidToken(ReaderBasedJsonParser.java:3018)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:URL_ENCODED valid decoding:correctlyEncodedStringValidatesOk()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$UrlEncoded', methodName = 'correctlyEncodedStringValidatesOk', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Invalid input
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:949)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       com.networknt.schema.JsonSchema_validate_12_Test$UrlEncoded.correctlyEncodedStringValidatesOk(JsonSchema_validate_12_Test.java:206)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
     Caused by: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
       com.fasterxml.jackson.core.JsonParser._constructError(JsonParser.java:2567)
       com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2593)
       com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2601)
       com.fasterxml.jackson.core.base.ParserMinimalBase._reportError(ParserMinimalBase.java:765)
       com.fasterxml.jackson.core.json.ReaderBasedJsonParser._reportInvalidToken(ReaderBasedJsonParser.java:3018)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:YAML format handling:malformedYamlThrowsJsonParseException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$YamlHandling', methodName = 'malformedYamlThrowsJsonParseException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <com.fasterxml.jackson.core.JsonParseException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$YamlHandling.malformedYamlThrowsJsonParseException(JsonSchema_validate_12_Test.java:193)
       [...]
     Caused by: java.lang.IllegalArgumentException: Invalid input
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:949)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       com.networknt.schema.JsonSchema_validate_12_Test$YamlHandling.lambda$malformedYamlThrowsJsonParseException$0(JsonSchema_validate_12_Test.java:193)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
     Caused by: com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException: while parsing a flow sequence
 in 'reader', line 1, column 10:
    invalid: [unbalanced
             ^
expected ',' or ']', but got <stream end>
 in 'reader', line 1, column 21:
    invalid: [unbalanced
                        ^

 at [Source: (StringReader); line: 1, column: 21]
       com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException.from(MarkedYAMLException.java:28)
       com.fasterxml.jackson.dataformat.yaml.YAMLParser.nextToken(YAMLParser.java:441)
       com.fasterxml.jackson.databind.deser.std.BaseNodeDeserializer._deserializeContainerNoRecursion(JsonNodeDeserializer.java:609)
       com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer.deserialize(JsonNodeDeserializer.java:100)
       com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer.deserialize(JsonNodeDeserializer.java:25)
       [...]
     Caused by: while parsing a flow sequence
 in 'reader', line 1, column 10:
    invalid: [unbalanced
             ^
expected ',' or ']', but got <stream end>
 in 'reader', line 1, column 21:
    invalid: [unbalanced
                        ^

       org.yaml.snakeyaml.parser.ParserImpl$ParseFlowSequenceEntry.produce(ParserImpl.java:772)
       org.yaml.snakeyaml.parser.ParserImpl.peekEvent(ParserImpl.java:161)
       org.yaml.snakeyaml.parser.ParserImpl.getEvent(ParserImpl.java:170)
       com.fasterxml.jackson.dataformat.yaml.YAMLParser.nextToken(YAMLParser.java:437)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:Null argument handling:nullInputStringThrowsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$NullArguments', methodName = 'nullInputStringThrowsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$NullArguments.nullInputStringThrowsNullPointerException(JsonSchema_validate_12_Test.java:138)
       [...]
     Caused by: java.lang.IllegalArgumentException: argument "content" is null
       com.fasterxml.jackson.databind.ObjectMapper._assertNotNull(ObjectMapper.java:5060)
       com.fasterxml.jackson.databind.ObjectMapper.readTree(ObjectMapper.java:3278)
       com.networknt.schema.JsonSchemaFactory.readTree(JsonSchemaFactory.java:475)
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:947)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:Null argument handling:nullInputFormatThrowsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$NullArguments', methodName = 'nullInputFormatThrowsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$NullArguments.nullInputFormatThrowsNullPointerException(JsonSchema_validate_12_Test.java:143)
       [...]
     Caused by: java.lang.IllegalArgumentException: Unsupported input format null
       com.networknt.schema.JsonSchemaFactory.getObjectMapper(JsonSchemaFactory.java:495)
       com.networknt.schema.JsonSchemaFactory.readTree(JsonSchemaFactory.java:475)
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:947)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       com.networknt.schema.JsonSchema_validate_12_Test$NullArguments.lambda$nullInputFormatThrowsNullPointerException$1(JsonSchema_validate_12_Test.java:143)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:Invalid JSON / deserialization errors:malformedJsonThrowsJsonParseException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$DeserializationErrors', methodName = 'malformedJsonThrowsJsonParseException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <com.fasterxml.jackson.core.JsonParseException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$DeserializationErrors.malformedJsonThrowsJsonParseException(JsonSchema_validate_12_Test.java:120)
       [...]
     Caused by: java.lang.IllegalArgumentException: Invalid input
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:949)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       com.networknt.schema.JsonSchema_validate_12_Test$DeserializationErrors.lambda$malformedJsonThrowsJsonParseException$0(JsonSchema_validate_12_Test.java:120)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
     Caused by: com.fasterxml.jackson.core.JsonParseException: Unexpected character ('i' (code 105)): was expecting double-quote to start field name
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 2]
       com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2643)
       com.fasterxml.jackson.core.base.ParserMinimalBase._reportUnexpectedChar(ParserMinimalBase.java:685)
       com.fasterxml.jackson.core.json.ReaderBasedJsonParser._handleOddName(ReaderBasedJsonParser.java:1910)
       com.fasterxml.jackson.core.json.ReaderBasedJsonParser.nextFieldName(ReaderBasedJsonParser.java:940)
       com.fasterxml.jackson.databind.deser.std.BaseNodeDeserializer._deserializeContainerNoRecursion(JsonNodeDeserializer.java:536)
       [...]
  JUnit Jupiter:JsonSchema_validate_12_Test:Invalid JSON / deserialization errors:urlEncodedInvalidEscapeThrowsUnsupportedEncodingException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_12_Test$DeserializationErrors', methodName = 'urlEncodedInvalidEscapeThrowsUnsupportedEncodingException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.io.UnsupportedEncodingException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_12_Test$DeserializationErrors.urlEncodedInvalidEscapeThrowsUnsupportedEncodingException(JsonSchema_validate_12_Test.java:128)
       [...]
     Caused by: java.lang.IllegalArgumentException: Invalid input
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:949)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:853)
       com.networknt.schema.JsonSchema_validate_12_Test$DeserializationErrors.lambda$urlEncodedInvalidEscapeThrowsUnsupportedEncodingException$1(JsonSchema_validate_12_Test.java:128)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
     Caused by: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 5]
       com.fasterxml.jackson.core.JsonParser._constructError(JsonParser.java:2567)
       com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2593)
       com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2601)
       com.fasterxml.jackson.core.base.ParserMinimalBase._reportError(ParserMinimalBase.java:765)
       com.fasterxml.jackson.core.json.ReaderBasedJsonParser._reportInvalidToken(ReaderBasedJsonParser.java:3018)
       [...]

Test run finished after 470 ms
[        16 containers found      ]
[         0 containers skipped    ]
[        16 containers started    ]
[         0 containers aborted    ]
[        16 containers successful ]
[         0 containers failed     ]
[        22 tests found           ]
[         0 tests skipped         ]
[        22 tests started         ]
[         0 tests aborted         ]
[        12 tests successful      ]
[        10 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

api document of method validate(String, InputFormat, OutputFormat<T>): 

     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>         the result type
     * @param input       the input
     * @param inputFormat the inputFormat
     * @param format      the formatter
     * @return the result
     

return type: 
T

dependent classes: 
class `com.networknt.schema.InputFormat`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: 
     * Validate the given input string using the input format, starting at the root
     * of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     *
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param executionCustomizer the execution customizer
     * @return the assertions
     ;
		method `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: 
     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public Set<ValidationMessage> validate(String, InputFormat)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: 
     * Validate the given input string using the input format, starting at the root
     * of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param input       the input
     * @param inputFormat the inputFormat
     * @return A list of ValidationMessage if there is any validation error, or an
     *         empty list if there is no error.
     ;
		method `public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, OutputFormat<T>)`, api document: 
     * Validate the given root JsonNode, starting at the root of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     *
     * @param rootNode            the root node
     * @param executionCustomizer the execution customizer
     * @return the assertions
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
