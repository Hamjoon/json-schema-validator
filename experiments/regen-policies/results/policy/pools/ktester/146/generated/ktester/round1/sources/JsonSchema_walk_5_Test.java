package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_5_Test {

    /**
     * Simple schema used by the tests.
     */
    private static final String SIMPLE_SCHEMA = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{\"type\":\"string\"}" + "}," + "\"required\":[\"name\"]" + "}";

    /**
     * JSON input that conforms to {@link #SIMPLE_SCHEMA}.
     */
    private static final String VALID_INPUT = "{\"name\":\"John\"}";

    /**
     * JSON input that violates {@link #SIMPLE_SCHEMA}.
     */
    private static final String INVALID_INPUT = "{\"name\":1}";

    private static JsonSchemaFactory factory;

    private JsonSchema schema;

    /**
     * A permissive schema (no constraints) used for primitive/array tests.
     */
    private static final String PERMISSIVE_SCHEMA = "{}";

    // schema without restrictions
    private JsonSchema permissiveSchema;

    @FunctionalInterface
    private interface Executable {

        void execute() throws Throwable;
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a factory that can create JsonSchema instances.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for every test to avoid state leakage.
        schema = factory.getSchema(SIMPLE_SCHEMA);
        permissiveSchema = factory.getSchema(PERMISSIVE_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly drop the reference – not strictly required but keeps the
        // test class tidy and mirrors typical cleanup patterns.
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    private static Stream<Arguments> provideWalkScenarios() {
        return Stream.of(// Valid Input – JSON format, validation enabled
        Arguments.of("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, true, true, true, "Valid Input - JSON format, validation enabled"), // Valid Input – JSON format, validation disabled
        Arguments.of("{\"city\":\"Paris\",\"population\":2148000}", InputFormat.JSON, false, true, true, "Valid Input - JSON format, validation disabled"), // URL‑encoded JSON string (decoded to {"id":123,"valid":true})
        Arguments.of("%7B%22id%22%3A123%2C%22valid%22%3Atrue%7D", InputFormat.JSON, true, true, true, "URL‑encoded JSON string"), // JSON array input – validation enabled (permissive schema)
        Arguments.of("[{\"id\":1},{\"id\":2}]", InputFormat.JSON, true, true, true, "JSON array input – validation enabled"), // JSON array input – validation disabled
        Arguments.of("[{\"id\":1},{\"id\":2}]", InputFormat.JSON, false, true, true, "JSON array input – validation disabled"), // JSON primitive number input
        Arguments.of("12345", InputFormat.JSON, true, true, true, "JSON primitive number input"), // JSON primitive boolean input
        Arguments.of("true", InputFormat.JSON, true, true, true, "JSON primitive boolean input"), // JSON null literal input
        Arguments.of("null", InputFormat.JSON, true, true, true, "JSON null literal input"), // Invalid JSON syntax (parsing error) – validation enabled
        Arguments.of("{\"name\":\"John\", \"age\":}", InputFormat.JSON, true, false, false, "Invalid JSON syntax"), // Malformed JSON with validation disabled (parsing still fails)
        Arguments.of("{\"name\":\"John\", \"age\":}", InputFormat.JSON, false, false, false, "Malformed JSON with validation disabled"), // Empty string input
        Arguments.of("", InputFormat.JSON, true, false, false, "Empty string input"), // Whitespace‑only input
        Arguments.of("   \t\n  ", InputFormat.JSON, true, false, false, "Whitespace‑only input"));
    }

    /* -------------------------------------------------------------
     *  Helper for asserting that an executable throws one of several
     *  possible exception types.
     * ------------------------------------------------------------- */
    private static void assertThrowsAnyOf(Class<? extends Throwable>[] types, Executable executable) {
        try {
            executable.execute();
            fail("Expected one of " + java.util.Arrays.toString(types) + " to be thrown, but nothing was thrown.");
        } catch (Throwable actual) {
            for (Class<? extends Throwable> t : types) {
                if (t.isInstance(actual)) {
                    // Expected – test passes.
                    return;
                }
            }
            fail("Unexpected exception type thrown: " + actual.getClass(), actual);
        }
    }

    @Test
    void walk_validInput_returnsNoValidationMessages() {
        // given
        String input = VALID_INPUT;
        InputFormat format = InputFormat.JSON;
        boolean validate = true;
        // when
        ValidationResult result = schema.walk(input, format, validate);
        // then
        assertNotNull(result, "Result should never be null");
        // The ValidationResult API provides a collection of messages; an empty
        // collection means the input is valid.
        assertTrue(result.getMessages().isEmpty(), () -> "Expected no validation messages, but got: " + result.getMessages());
    }

    @Test
    void walk_invalidInput_returnsValidationMessages() {
        // given
        String input = INVALID_INPUT;
        InputFormat format = InputFormat.JSON;
        boolean validate = true;
        // when
        ValidationResult result = schema.walk(input, format, validate);
        // then
        assertNotNull(result, "Result should never be null");
        // The schema requires \"name\" to be a string, so we expect at least one message.
        assertFalse(result.getMessages().isEmpty(), "Expected validation messages for an invalid input");
    }

    /* -------------------------------------------------------------
     *  Parameterised success / failure cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("provideWalkScenarios")
    void walk_variousScenarios(String input, InputFormat format, boolean validate, boolean expectValid, boolean expectMessagesEmpty, String description) {
        // Choose the appropriate schema: object‑based for SIMPLE_SCHEMA,
        // otherwise the permissive one.
        JsonSchema usedSchema = (description.contains("array") || description.contains("primitive") || description.contains("null literal")) ? permissiveSchema : schema;
        ValidationResult result = usedSchema.walk(input, format, validate);
        assertNotNull(result, "Result must never be null");
        assertEquals(expectValid, result.isValid(), "Validity mismatch for case: " + description);
        if (expectMessagesEmpty) {
            assertTrue(result.getMessages().isEmpty(), () -> "Expected no messages but got: " + result.getMessages());
        } else {
            assertFalse(result.getMessages().isEmpty(), () -> "Expected messages for case: " + description);
        }
    }

    @Test
    void walk_nullInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.walk(null, InputFormat.JSON, true), "Null input should cause IllegalArgumentException");
    }

    @Test
    void walk_nullInputFormat_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.walk("{\"key\":\"value\"}", null, true), "Null InputFormat should cause IllegalArgumentException");
    }

    @Test
    void walk_unsupportedInputFormat_throwsException() {
        // The library may support YAML; if it does, the call succeeds.
        // We accept either UnsupportedOperationException or IOException.
        assertThrowsAnyOf(new Class[] { UnsupportedOperationException.class, IOException.class }, () -> schema.walk("name: John\nage: 30", InputFormat.YAML, true));
    }

    @Test
    void walk_malformedUrlEncodedInput_throwsIllegalArgumentException() {
        // Incomplete percent‑encoding triggers IllegalArgumentException from URLDecoder.
        assertThrows(IllegalArgumentException.class, () -> schema.walk("%E0%A4%A", InputFormat.JSON, true), "Malformed percent‑encoding should propagate IllegalArgumentException");
    }

    @Test
    void walk_invalidRef_throwsJsonSchemaException() {
        // Build a schema that contains an unresolvable $ref.
        String schemaWithBadRef = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"$ref\":\"#/definitions/missing\"" + "}";
        JsonSchema badRefSchema = factory.getSchema(schemaWithBadRef);
        assertThrows(JsonSchemaException.class, () -> badRefSchema.walk("{\"name\":\"John\"}", InputFormat.JSON, true), "Unresolvable $ref should result in JsonSchemaException");
    }
}
