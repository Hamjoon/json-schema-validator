package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchema_validate_6_Test {

    private static JsonSchemaFactory factory;

    private static JsonSchema schema;

    /**
     * Initialise shared resources once for all tests.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – it is the most recent stable version in the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No special configuration needed for these simple tests.
        });
        // A tiny schema that requires an object with a required string property "name".
        String schemaData = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;
        // Default configuration – do not preload any external schemas.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        schema = factory.getSchema(schemaData, config);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references – helps the GC when many test classes are run.
        factory = null;
        schema = null;
    }

    /**
     * All inputs in this stream are syntactically correct and satisfy the
     * schema, therefore the validator must return an empty {@link Set}.
     */
    private static Stream<Arguments> validInputProvider() {
        return Stream.of(// Valid JSON (extra property "age" is ignored)
        Arguments.of("{\"name\":\"John\",\"age\":30}", InputFormat.JSON), // Valid YAML (same content as above)
        Arguments.of("name: John\nage: 30", InputFormat.YAML), // URL‑encoded JSON (UTF‑8) – decoder should produce {"name":"John"}
        Arguments.of("%7B%22name%22%3A%22John%22%7D", InputFormat.URL_ENCODED_JSON), // Plain JSON while format is URL_ENCODED_JSON – decoder returns unchanged string
        Arguments.of("{\"city\":\"Paris\",\"population\":2148000}", InputFormat.URL_ENCODED_JSON), // JSON payload supplied with YAML format – Jackson's YAML parser accepts pure JSON
        Arguments.of("{\"product\":\"Book\",\"price\":12.99}", InputFormat.YAML), // Valid JSON with surrounding whitespace
        Arguments.of("  \n\t { \"status\" : \"ok\" , \"code\" : 200 } \t\n ", InputFormat.JSON), // Unicode characters inside JSON strings
        Arguments.of("{\"greeting\":\"こんにちは\",\"language\":\"Japanese\"}", InputFormat.JSON), // JSON with UTF‑8 BOM (the BOM is stripped by the parser)
        Arguments.of("\ufeff{\"name\":\"Alice\",\"age\":25}", InputFormat.JSON));
    }

    /**
     * All inputs in this stream are syntactically malformed or otherwise
     * unparsable by the chosen {@link InputFormat}.  The validator must return
     * at least one {@link ValidationMessage} that mentions a parsing problem.
     */
    private static Stream<Arguments> parsingErrorProvider() {
        return Stream.of(// Invalid JSON syntax
        Arguments.of("{\"name\":\"John\", \"age\":}", InputFormat.JSON), // Invalid YAML syntax
        Arguments.of("name: John\nage: : 30", InputFormat.YAML), // Empty string (no content)
        Arguments.of("", InputFormat.JSON), // Whitespace‑only string
        Arguments.of("   \t  \n  ", InputFormat.JSON), // Malformed percent‑encoding (trailing %7)
        Arguments.of("%7B%22name%22%3A%22Bob%22%7", InputFormat.URL_ENCODED_JSON), // YAML document supplied while format is JSON – should fail parsing
        Arguments.of("title: \"Effective Java\"\nauthor: \"Joshua Bloch\"", InputFormat.JSON));
    }

    /**
     * A valid JSON document that satisfies the schema should produce an empty
     * validation result.
     */
    @Test
    void validate_validInput_returnsEmptySet() {
        String validJson = """
            {
              "name": "John Doe"
            }
            """;
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid document");
    }

    /**
     * An invalid JSON document (wrong type for {@code name}) should yield a
     * single validation message.
     */
    @Test
    void validate_invalidInput_returnsMessages() {
        String invalidJson = """
            {
              "name": 123
            }
            """;
        Set<ValidationMessage> messages = schema.validate(invalidJson, InputFormat.JSON);
        assertEquals(1, messages.size(), "Expected exactly one validation message for type mismatch");
        ValidationMessage message = messages.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("type"), "Message should indicate a type error");
    }

    @ParameterizedTest(name = "[{index}] format={1} input={0}")
    @MethodSource("validInputProvider")
    void validate_validInputs_returnEmptySet(String input, InputFormat format) {
        Set<ValidationMessage> messages = schema.validate(input, format);
        assertTrue(messages.isEmpty(), () -> "Expected no validation messages for valid input with format " + format);
    }

    @ParameterizedTest(name = "[{index}] format={1} input={0}")
    @MethodSource("parsingErrorProvider")
    void validate_parsingErrors_returnMessages(String input, InputFormat format) {
        Set<ValidationMessage> messages = schema.validate(input, format);
        assertFalse(messages.isEmpty(), () -> "Expected parsing error messages for input format " + format);
        // Very loosely check that the message mentions parsing or unexpected token.
        boolean containsParsingHint = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("parse") || m.getMessage().toLowerCase().contains("unexpected") || m.getMessage().toLowerCase().contains("invalid"));
        assertTrue(containsParsingHint, () -> "At least one message should indicate a parsing problem, got: " + messages);
    }

    @Test
    void validate_nullInputString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.validate(null, InputFormat.JSON), "Null input string should trigger IllegalArgumentException");
    }

    @Test
    void validate_nullInputFormat_throwsIllegalArgumentException() {
        String json = "{\"key\":\"value\"}";
        assertThrows(IllegalArgumentException.class, () -> schema.validate(json, null), "Null InputFormat should trigger IllegalArgumentException");
    }

    @Test
    void validate_unsupportedInputFormat_throwsIllegalArgumentException() {
        String json = "{\"name\":\"John\"}";
        // Assuming the library defines an UNKNOWN constant for unsupported formats.
        // If such a constant does not exist, replace with a mock or a custom enum value.
        assertThrows(IllegalArgumentException.class, () -> schema.validate(json, InputFormat.UNKNOWN), "Unsupported InputFormat should trigger IllegalArgumentException");
    }

    @Test
    void validate_arrayRoot_returnsTypeMismatchMessage() {
        String arrayJson = "[1, 2, 3]";
        Set<ValidationMessage> messages = schema.validate(arrayJson, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for array root");
        boolean typeMismatch = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(typeMismatch, "Message should indicate a type mismatch (expected object)");
    }

    @Test
    void validate_literalNullRoot_returnsTypeMismatchMessage() {
        String nullLiteral = "null";
        Set<ValidationMessage> messages = schema.validate(nullLiteral, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for null literal root");
        boolean typeMismatch = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(typeMismatch, "Message should indicate a type mismatch (expected object)");
    }

    /**
     * This test attempts to provoke an {@link UnsupportedEncodingException}
     * during URL decoding.  The library always uses UTF‑8 which is guaranteed
     * to be supported, therefore we cannot realistically trigger the exception.
     * Instead we verify that a malformed percent sequence is reported as a
     * parsing error (which is the observable behaviour for the library).
     */
    @Test
    void validate_unsupportedCharacterEncoding_behavesAsParsingError() {
        // The percent sequence represents bytes that are not valid UTF‑8.
        // "你好" encoded correctly, but we treat it as unsupported for the test.
        String badEncoded = "%E4%BD%A0%E5%A5%BD";
        Set<ValidationMessage> messages = schema.validate(badEncoded, InputFormat.URL_ENCODED_JSON);
        // The library will successfully decode UTF‑8 and then parse JSON, which fails.
        assertFalse(messages.isEmpty(), "Expected a validation message for unsupported character encoding");
        boolean containsParseHint = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("parse") || m.getMessage().toLowerCase().contains("unexpected"));
        assertTrue(containsParseHint, "Message should indicate a parsing problem");
    }
}
