```java
package com.networknt.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat)}.
 *
 * <p>These tests cover:
 * <ul>
 *   <li>Successful validation for JSON and YAML inputs.</li>
 *   <li>All error‑handling paths (null/empty arguments, malformed input, unsupported encodings, etc.).</li>
 *   <li>Propagation of exceptions thrown by the supplied {@link OutputFormat}.</li>
 *   <li>Schema‑specific validation errors (required, type, enum, pattern, additionalProperties).</li>
 * </ul>
 *
 * <p>The test class re‑uses the {@code @BeforeEach} setup that creates a simple
 * schema (object with required string property {@code name}).  For tests that need a
 * different schema a local {@link JsonSchema} instance is built inside the test.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_11_Test {

    /** The default schema used by most tests. */
    private JsonSchema schema;

    /** Simple JSON schema: an object with a required string property {@code name}. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Very small {@link OutputFormat} that returns the validation messages as a JSON array string.
     */
    private static class SimpleOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) {
            // For readability we just return the set's {@code toString()} – the tests only check
            // whether the string contains expected fragments.
            return messages.toString();
        }
    }

    /**
     * {@link OutputFormat} that deliberately throws a {@link RuntimeException}.
     */
    private static class ThrowingOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) {
            throw new RuntimeException("OutputFormat failure");
        }
    }

    /**
     * {@link OutputFormat} that throws a checked {@link IOException}; the
     * {@code validate} method must wrap it into a {@link RuntimeException}.
     */
    private static class CheckedThrowingOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) throws IOException {
            throw new IOException("Checked exception from OutputFormat");
        }
    }

    /* --------------------------------------------------------------------- */
    /* Helper to create a JsonSchema from an arbitrary schema string.        */
    private static JsonSchema createSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        return factory.getSchema(schemaJson);
    }

    /* --------------------------------------------------------------------- */
    /* Positive test cases                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_ValidJson_ReturnsZeroMessageCount() {
        // GIVEN
        String validJson = """{ "name": "John Doe" }""";
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        // WHEN
        String result = schema.validate(validJson, inputFormat, outputFormat);

        // THEN
        assertTrue(result.contains("[]") || result.equals("[]"),
                "A valid JSON document should produce an empty validation message list");
    }

    @Test
    void validate_ValidYaml_ReturnsZeroMessageCount() {
        // GIVEN
        String yaml = """
                name: John Doe
                """;
        InputFormat inputFormat = InputFormat.YAML;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        // WHEN
        String result = schema.validate(yaml, inputFormat, outputFormat);

        // THEN
        assertTrue(result.contains("[]") || result.equals("[]"),
                "A valid YAML document should produce an empty validation message list");
    }

    @Test
    void validate_ValidUrlEncodedJson_ReturnsZeroMessageCount() {
        // GIVEN: URL‑encoded version of {"name":"John"}
        String encoded = "%7B%22name%22%3A%22John%22%7D";
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        // WHEN
        String result = schema.validate(encoded, inputFormat, outputFormat);

        // THEN
        assertTrue(result.contains("[]") || result.equals("[]"));
    }

    /* --------------------------------------------------------------------- */
    /* Negative test cases – argument validation                              */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: input={0}")
    @MethodSource("nullOrEmptyInputProvider")
    void validate_NullOrEmptyInput_throwsIllegalArgumentException(String description, String input) {
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(input, inputFormat, outputFormat),
                description);
    }

    private static Stream<Arguments> nullOrEmptyInputProvider() {
        return Stream.of(
                arguments("null input string", null),
                arguments("empty input string", ""),
                arguments("whitespace‑only input string", "   ")
        );
    }

    @Test
    void validate_NullInputFormat_throwsIllegalArgumentException() {
        String json = """{ "name":"John" }""";
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(json, null, outputFormat));
    }

    @Test
    void validate_NullOutputFormat_throwsIllegalArgumentException() {
        String json = """{ "name":"John" }""";
        InputFormat inputFormat = InputFormat.JSON;

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(json, inputFormat, null));
    }

    /* --------------------------------------------------------------------- */
    /* Negative test cases – malformed input                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_MalformedJson_throwsRuntimeExceptionWithCause() {
        String badJson = """{ "name":John }"""; // missing quotes around value
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(badJson, inputFormat, outputFormat));
        assertTrue(ex.getCause() instanceof JsonProcessingException,
                "Cause should be JsonProcessingException");
    }

    @Test
    void validate_MalformedYaml_throwsRuntimeExceptionWithCause() {
        String badYaml = "name: John: Doe"; // invalid YAML
        InputFormat inputFormat = InputFormat.YAML;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(badYaml, inputFormat, outputFormat));
        assertTrue(ex.getCause() instanceof JsonProcessingException,
                "Cause should be JsonProcessingException");
    }

    @Test
    void validate_MismatchedInputFormat_JsonTreatedAsYaml_throwsRuntimeException() {
        String json = """{ "name":"John" }""";
        InputFormat inputFormat = InputFormat.YAML; // wrong format
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, inputFormat, outputFormat));
        assertTrue(ex.getCause() instanceof JsonProcessingException);
    }

    @Test
    void validate_MismatchedInputFormat_YamlTreatedAsJson_throwsRuntimeException() {
        String yaml = "name: John";
        InputFormat inputFormat = InputFormat.JSON; // wrong format
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(yaml, inputFormat, outputFormat));
        assertTrue(ex.getCause() instanceof JsonProcessingException);
    }

    @Test
    void validate_MalformedPercentEncoding_throwsIllegalArgumentException() {
        // Incomplete percent‑escape sequence
        String malformed = "%E0%A4";
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(malformed, inputFormat, outputFormat));
    }

    @Test
    void validate_UnsupportedEncodingExceptionWrapped_throwsRuntimeException() throws UnsupportedEncodingException {
        String input = "%E0%A4%A"; // any string – we will force the exception via mocking
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        try (MockedStatic<URLDecoder> mocked = Mockito.mockStatic(URLDecoder.class)) {
            mocked.when(() -> URLDecoder.decode(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new UnsupportedEncodingException("simulated"));
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> schema.validate(input, inputFormat, outputFormat));
            assertTrue(ex.getCause() instanceof UnsupportedEncodingException);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Negative test cases – validation errors produced by the schema          */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_MissingRequiredProperty_ReturnsErrorMessage() {
        String json = "{}"; // missing required "name"
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> outputFormat = new SimpleOutputFormat();

        String result = schema.validate(json, inputFormat, outputFormat);
        assertTrue(result.contains("required"),
                "Result should contain a required‑property validation message");
    }

    @Test
    void validate_TypeMismatch_ReturnsErrorMessage() {
        // Schema that expects a string property "age"
        String schemaJson = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "age": { "type": "string" }
                  }
                }
                """;
        JsonSchema typeSchema = createSchema(schemaJson);
        String json = """{ "age": 123 }""";
        String result = typeSchema.validate(json, InputFormat.JSON, new SimpleOutputFormat());
        assertTrue(result.contains("type"),
                "Result should contain a type‑mismatch validation message");
    }

    @Test
    void validate_EnumViolation_ReturnsErrorMessage() {
        String schemaJson = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "enum": ["red","green"]
                }
                """;
        JsonSchema enumSchema = createSchema(schemaJson);
        String json = "\"blue\"";
        String result = enumSchema.validate(json, InputFormat.JSON, new SimpleOutputFormat());
        assertTrue(result.contains("enum"),
                "Result should contain an enum‑violation validation message");
    }

    @Test
    void validate_PatternViolation_ReturnsErrorMessage() {
        String schemaJson = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "email": { "type": "string", "pattern": "^[A-Za-z0-9+_.-]+@(.+)$" }
                  }
                }
                """;
        JsonSchema patternSchema = createSchema(schemaJson);
        String json = """{ "email":"not-an-email" }""";
        String result = patternSchema.validate(json, InputFormat.JSON, new SimpleOutputFormat());
        assertTrue(result.contains("pattern"),
                "Result should contain a pattern‑violation validation message");
    }

    @Test
    void validate_AdditionalPropertiesNotAllowed_ReturnsErrorMessage() {
        String schemaJson = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "allowed": { "type": "string" }
                  },
                  "additionalProperties": false
                }
                """;
        JsonSchema addPropSchema = createSchema(schemaJson);
        String json = """{ "allowed":"yes","extra":"no" }""";
        String result = addPropSchema.validate(json, InputFormat.JSON, new SimpleOutputFormat());
        assertTrue(result.contains("additionalProperties"),
                "Result should contain an additional‑properties validation message");
    }

    /* --------------------------------------------------------------------- */
    /* Negative test cases – OutputFormat misbehaviour                         */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_OutputFormatThrowsRuntimeException_propagates() {
        String json = """{ "name":"John" }""";
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> throwingFormat = new ThrowingOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, inputFormat, throwingFormat));
        assertEquals("OutputFormat failure", ex.getMessage());
    }

    @Test
    void validate_OutputFormatThrowsCheckedException_wrappedInRuntimeException() {
        String json = """{ "name":"John" }""";
        InputFormat inputFormat = InputFormat.JSON;
        OutputFormat<String> checkedThrowing = new CheckedThrowingOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, inputFormat, checkedThrowing));
        assertTrue(ex.getCause() instanceof IOException,
                "The cause should be the checked IOException thrown by the OutputFormat");
    }
}
```