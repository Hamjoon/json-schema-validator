```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean)}.
 *
 * <p>These tests cover the exhaustive scenarios described in the supplied JSON test
 * matrix.  Where a scenario cannot be exercised without deep internal mocking (e.g.
 * forcing {@link UnsupportedEncodingException}), the test is marked {@code @Disabled}
 * with a clear rationale.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_7_Test {

    /** Shared factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** The default schema used for most test cases (object with a required {@code name}). */
    private JsonSchema defaultSchema;

    /** Execution context supplied to the {@code walk} method. */
    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft (the most recent at the time of writing)
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default schema – used for the majority of test cases
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }
                """;

        defaultSchema = factory.getSchema(schemaJson);
        executionContext = defaultSchema.createExecutionContext();
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for the “Valid inputs” groups
     * ------------------------------------------------------------- */
    private static Stream<Arguments> validInputProvider() {
        return Stream.of(
                // JSON – validation true
                Arguments.of(
                        InputFormat.JSON,
                        """{"name":"John","age":30}""",
                        true,
                        true),
                // JSON – validation false
                Arguments.of(
                        InputFormat.JSON,
                        """{"name":"John","age":30}""",
                        false,
                        true),
                // YAML – validation true
                Arguments.of(
                        InputFormat.YAML,
                        "name: John\nage: 30",
                        true,
                        true),
                // YAML – validation false (explicit test case)
                Arguments.of(
                        InputFormat.YAML,
                        "name: John\nage: 30",
                        false,
                        true));
    }

    @ParameterizedTest(name = "[{index}] format={0}, validate={2}")
    @MethodSource("validInputProvider")
    void walkShouldReturnSuccessForValidInputs(InputFormat format,
                                                String input,
                                                boolean validate,
                                                boolean expectSuccess) {
        ValidationResult result = defaultSchema.walk(executionContext, input, format, validate);
        assertNotNull(result, "walk must never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "messages set must not be null");
        if (expectSuccess) {
            assertTrue(messages.isEmpty(),
                    () -> "Expected no validation messages, but got: " + messages);
            assertTrue(result.isSuccess(),
                    "ValidationResult#isSuccess() should be true for valid input");
        }
    }

    /* -------------------------------------------------------------
     *  Invalid JSON syntax
     * ------------------------------------------------------------- */
    @Test
    void walkShouldPropagateJsonParseExceptionForMalformedJson() {
        String malformed = """{"name":"John","age":}""";
        assertThrows(JsonParseException.class,
                () -> defaultSchema.walk(executionContext, malformed, InputFormat.JSON, true),
                "Malformed JSON should cause a JsonParseException");
    }

    /* -------------------------------------------------------------
     *  Null input string
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowNullPointerExceptionWhenInputIsNull() {
        assertThrows(NullPointerException.class,
                () -> defaultSchema.walk(executionContext, null, InputFormat.JSON, true),
                "Passing a null input string should result in NPE");
    }

    /* -------------------------------------------------------------
     *  Unsupported InputFormat (XML)
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowIllegalArgumentExceptionForUnsupportedInputFormat() {
        // The library does not support XML – we expect an IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> defaultSchema.walk(executionContext, "<root></root>", InputFormat.XML, true),
                "Unsupported InputFormat should raise IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Validate flag false with valid YAML (already covered by param test)
     * ------------------------------------------------------------- */
    // No extra test needed – covered in the parameterised method above.

    /* -------------------------------------------------------------
     *  Null ExecutionContext
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowNullPointerExceptionWhenExecutionContextIsNull() {
        String validJson = """{"name":"John"}""";
        assertThrows(NullPointerException.class,
                () -> defaultSchema.walk(null, validJson, InputFormat.JSON, true),
                "Null ExecutionContext must cause NPE");
    }

    /* -------------------------------------------------------------
     *  Null InputFormat
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowNullPointerExceptionWhenInputFormatIsNull() {
        String validJson = """{"name":"John"}""";
        assertThrows(NullPointerException.class,
                () -> defaultSchema.walk(executionContext, validJson, null, true),
                "Null InputFormat must cause NPE");
    }

    /* -------------------------------------------------------------
     *  Empty input string (JSON)
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowJsonParseExceptionForEmptyJsonString() {
        assertThrows(JsonParseException.class,
                () -> defaultSchema.walk(executionContext, "", InputFormat.JSON, true),
                "Empty string is not valid JSON");
    }

    /* -------------------------------------------------------------
     *  Blank input string (whitespace only) – JSON
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowJsonParseExceptionForBlankJsonString() {
        String blank = "   \n\t  ";
        assertThrows(JsonParseException.class,
                () -> defaultSchema.walk(executionContext, blank, InputFormat.JSON, true),
                "Whitespace‑only string should cause JsonParseException");
    }

    /* -------------------------------------------------------------
     *  Valid JSON array as root node
     * ------------------------------------------------------------- */
    @Test
    void walkShouldValidateArrayRootWhenSchemaAllowsArray() {
        // Schema that permits any array at the root
        String arraySchema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "array"
                }
                """;
        JsonSchema schema = factory.getSchema(arraySchema);
        ExecutionContext ctx = schema.createExecutionContext();

        String jsonArray = "[1,2,3]";
        ValidationResult result = schema.walk(ctx, jsonArray, InputFormat.JSON, true);
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Array root should validate successfully");
        assertTrue(result.getMessages().isEmpty(),
                "No validation messages expected for a valid array");
    }

    /* -------------------------------------------------------------
     *  Invalid YAML syntax
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowJsonParseExceptionForMalformedYaml() {
        String malformedYaml = "name: John\nage 30"; // missing colon
        assertThrows(JsonParseException.class,
                () -> defaultSchema.walk(executionContext, malformedYaml, InputFormat.YAML, true),
                "Malformed YAML should raise JsonParseException");
    }

    /* -------------------------------------------------------------
     *  Valid JSON with Unicode characters
     * ------------------------------------------------------------- */
    @Test
    void walkShouldHandleUnicodeEscapesInJson() {
        // Schema that allows any object with a "text" property of type string
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "text": { "type": "string" }
                  },
                  "required": [ "text" ]
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        ExecutionContext ctx = schema.createExecutionContext();

        String json = """{"text":"\\u00A9\\u20AC"}"""; // ©€
        ValidationResult result = schema.walk(ctx, json, InputFormat.JSON, true);
        assertTrue(result.isSuccess(),
                "Unicode escaped characters should be parsed and validated successfully");
        assertTrue(result.getMessages().isEmpty(),
                "No validation messages expected for valid Unicode JSON");
    }

    /* -------------------------------------------------------------
     *  UnsupportedEncodingException during deserialization
     * ------------------------------------------------------------- */
    @Disabled("The library's deserialize implementation does not expose a hook to trigger " +
            "UnsupportedEncodingException.  To test this path one would need to replace the " +
            "private deserialize method via byte‑code manipulation or a specialized test " +
            "double, which is out of scope for pure unit tests using only JUnit/Mockito.")
    @Test
    void walkShouldPropagateUnsupportedEncodingException() {
        // Placeholder – see @Disabled explanation above.
    }

    /* -------------------------------------------------------------
     *  Empty input string – YAML format
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowJsonParseExceptionForEmptyYamlString() {
        assertThrows(JsonParseException.class,
                () -> defaultSchema.walk(executionContext, "", InputFormat.YAML, true),
                "Empty string is not valid YAML");
    }

    /* -------------------------------------------------------------
     *  Blank input string – YAML format (whitespace only)
     * ------------------------------------------------------------- */
    @Test
    void walkShouldThrowJsonParseExceptionForBlankYamlString() {
        String blank = "   \n\t  ";
        assertThrows(JsonParseException.class,
                () -> defaultSchema.walk(executionContext, blank, InputFormat.YAML, true),
                "Whitespace‑only YAML should raise JsonParseException");
    }

    /* -------------------------------------------------------------
     *  Invalid enum constant for InputFormat
     * ------------------------------------------------------------- */
    @Disabled("Java enums cannot be instantiated with an unknown constant at compile time. " +
            "Attempting to reference InputFormat.UNKNOWN does not compile. " +
            "If the library exposed a method to create a custom InputFormat, we could test it, " +
            "but with the current API this case is not reachable.")
    @Test
    void walkShouldThrowIllegalArgumentExceptionForUnknownInputFormat() {
        // Placeholder – see @Disabled explanation above.
    }

    /* -------------------------------------------------------------
     *  Existing tests from the original framework (kept unchanged)
     * ------------------------------------------------------------- */
    @Test
    void walkShouldReturnValidationResultWhenInputIsInvalid() {
        String invalidJson = """
                {
                  "name": 123
                }
                """;

        ValidationResult result = defaultSchema.walk(executionContext, invalidJson, InputFormat.JSON, true);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages);
        assertFalse(messages.isEmpty());

        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getPath().contains("name"));
        assertTrue(mentionsName);
    }

    @Test
    void walkShouldReturnEmptyResultWhenInputIsValid() {
        String validJson = """
                {
                  "name": "John Doe"
                }
                """;

        ValidationResult result = defaultSchema.walk(executionContext, validJson, InputFormat.JSON, true);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void walkShouldSkipValidationWhenValidateFlagIsFalse() {
        String invalidJson = """
                {
                  "name": 123
                }
                """;

        ValidationResult result = defaultSchema.walk(executionContext, invalidJson, InputFormat.JSON, false);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages);
        assertTrue(messages.isEmpty(),
                "When the validate flag is false, walk should return an empty ValidationResult");
    }
}
```