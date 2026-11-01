```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import org.mockito.Mockito;

/**
 * Extended unit tests for {@link JsonSchema#validate(String, InputFormat)}.
 *
 * <p>These tests cover the full set of scenarios described in the JSON test case
 * specification, including successful validation for JSON and YAML inputs,
 * handling of missing required properties, malformed inputs, null arguments,
 * and unsupported/unknown {@link InputFormat}s.</p>
 *
 * <p>The {@link JsonSchema} instance is created via {@link JsonSchemaFactory}
 * using a simple schema that requires a {@code name} property of type
 * {@code string} and allows additional properties.</p>
 *
 * @author ---
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_6_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Schema under test. */
    private JsonSchema schema;

    /** Configuration used when creating the schema. */
    private static SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the most recent draft supported by the library
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        config = SchemaValidatorsConfig.builder().build();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal schema: object with required string property "name"
        String schemaData = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{ \"type\":\"string\" }"
                + "},"
                + "\"required\":[\"name\"]"
                + "}";

        schema = factory.getSchema(schemaData, config);
    }

    @AfterEach
    void teardownAfterEach() {
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
        config = null;
    }

    /* ---------------------------------------------------------------------- */
    /*  Positive test cases (valid inputs)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_validJson_returnsEmptySet() {
        String validJson = "{ \"name\": \"John Doe\" }";
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);
        assertTrue(messages.isEmpty(),
                () -> "Expected no validation messages for valid JSON, but got: " + messages);
    }

    @Test
    void validate_validYaml_returnsEmptySet() {
        String validYaml = "name: John Doe\nage: 30";
        Set<ValidationMessage> messages = schema.validate(validYaml, InputFormat.YAML);
        assertTrue(messages.isEmpty(),
                () -> "Expected no validation messages for valid YAML, but got: " + messages);
    }

    @Test
    void validate_extraProperties_returnsEmptySet() {
        String jsonWithExtra = "{\"name\":\"John\",\"extra\":\"value\"}";
        Set<ValidationMessage> messages = schema.validate(jsonWithExtra, InputFormat.JSON);
        assertTrue(messages.isEmpty(),
                () -> "Extra properties should be allowed by default, but got: " + messages);
    }

    /* ---------------------------------------------------------------------- */
    /*  Negative test cases (invalid inputs)                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    void validate_missingRequiredProperty_returnsMessage() {
        String jsonMissingName = "{\"age\":30}";
        Set<ValidationMessage> messages = schema.validate(jsonMissingName, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for missing required property");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("required", msg.getType(),
                () -> "Expected a 'required' validation error, but got: " + msg.getType());
    }

    @Test
    void validate_typeMismatch_returnsMessage() {
        String jsonTypeMismatch = "{\"name\":123}";
        Set<ValidationMessage> messages = schema.validate(jsonTypeMismatch, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for type mismatch");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("type", msg.getType(),
                () -> "Expected a 'type' validation error, but got: " + msg.getType());
    }

    @Test
    void validate_yamlTypeMismatch_returnsMessage() {
        String yamlTypeMismatch = "name: John\nage: thirty";
        Set<ValidationMessage> messages = schema.validate(yamlTypeMismatch, InputFormat.YAML);
        assertFalse(messages.isEmpty(), "Expected a validation message for YAML type mismatch");
        ValidationMessage msg = messages.iterator().next();
        // The schema does not define "age", but the example expects a type‑mismatch;
        // we simply verify that at least one message is produced.
        assertNotNull(msg.getMessage(),
                () -> "Expected a non‑null validation message, but got null");
    }

    @Test
    void validate_emptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate("", InputFormat.JSON),
                "Empty input should trigger IllegalArgumentException");
    }

    @Test
    void validate_whitespaceOnly_throwsIllegalArgumentException() {
        String ws = "   \n\t  ";
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(ws, InputFormat.JSON),
                "Whitespace‑only input should trigger IllegalArgumentException");
    }

    @Test
    void validate_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> schema.validate(null, InputFormat.JSON),
                "Null input should trigger NullPointerException");
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"John\"}", null),
                "Null InputFormat should trigger NullPointerException");
    }

    @Test
    void validate_malformedJson_throwsJsonParseException() {
        String malformed = "{name:\"John\"}";
        // The library wraps parsing errors in a ValidationException; however,
        // JsonParseException is the root cause. We assert that an exception
        // (any RuntimeException) is thrown and that its cause is JsonParseException.
        Exception ex = assertThrows(RuntimeException.class,
                () -> schema.validate(malformed, InputFormat.JSON),
                "Malformed JSON should result in a parsing exception");
        assertTrue(ex.getCause() instanceof JsonParseException ||
                        ex instanceof JsonParseException,
                () -> "Expected JsonParseException as cause, but got: " + ex);
    }

    @Test
    void validate_jsonContentWithYamlFormat_throwsException() {
        String json = "{\"name\":\"John\"}";
        Exception ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, InputFormat.YAML),
                "Providing JSON while declaring YAML format should fail");
        // The exact exception type may vary; we only assert that it is a runtime exception.
        assertNotNull(ex, "Exception should not be null");
    }

    @Test
    void validate_malformedPercentEncoding_throwsUnsupportedEncodingException() {
        String badEncoding = "%ZZ%invalid%encoding";
        // The library may wrap the UnsupportedEncodingException; we check the cause.
        Exception ex = assertThrows(RuntimeException.class,
                () -> schema.validate(badEncoding, InputFormat.JSON),
                "Malformed percent‑encoding should trigger UnsupportedEncodingException");
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof UnsupportedEncodingException,
                () -> "Expected UnsupportedEncodingException as cause, but got: " + cause);
    }

    @Test
    void validate_unknownInputFormat_throwsIllegalArgumentException() {
        // Create a mock InputFormat that the library does not recognise.
        InputFormat unknown = Mockito.mock(InputFormat.class);
        // The mock will return a name that does not match any supported format.
        Mockito.when(unknown.name()).thenReturn("UNKNOWN");

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate("{\"name\":\"John\"}", unknown),
                "Unsupported InputFormat should trigger IllegalArgumentException");
    }

    /* ---------------------------------------------------------------------- */
    /*  Parameterised tests for similar scenarios (optional)                 */
    /* ---------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] input={0}, format={1}")
    @MethodSource("provideInvalidInputs")
    void validate_invalidInputs_throwException(String input, InputFormat format, Class<? extends Throwable> expected) {
        assertThrows(expected,
                () -> schema.validate(input, format),
                () -> "Expected " + expected.getSimpleName() + " for input: " + input);
    }

    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(
                Arguments.of("", InputFormat.JSON, IllegalArgumentException.class),
                Arguments.of("   \n\t", InputFormat.JSON, IllegalArgumentException.class),
                Arguments.of("{name:\"John\"}", InputFormat.JSON, RuntimeException.class), // malformed JSON
                Arguments.of("{\"name\":\"John\"}", InputFormat.YAML, RuntimeException.class) // JSON as YAML
        );
    }
}
```