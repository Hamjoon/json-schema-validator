package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.stream.Stream;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.Mockito;
/**
 * Tests for {@link JsonSchema#validate(String, InputFormat)} covering a variety of
 * valid and invalid inputs.
 */
class JsonSchema_validate_6_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * Schema under test.
     */
    private JsonSchema schema;

    /**
     * Configuration used when creating the schema.
     */
    private static SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft (the most recent draft supported by the library)
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Simple config – we do not need any special behaviour for these tests
        config = SchemaValidatorsConfig.builder().build();
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal schema that requires a string property "name"
        String schemaData = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{ \"type\":\"string\" }" + "}," + "\"required\":[\"name\"]" + "}";
        // Create the JsonSchema instance that will be exercised in each test
        schema = factory.getSchema(schemaData, config);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
        config = null;
    }

    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(Arguments.of("{name:\"John\"}", InputFormat.JSON, RuntimeException.class));
    }

    @Test
    void validate_validInput_returnsEmptySet() {
        String validJson = "{ \"name\": \"John Doe\" }";
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);
        assertTrue(messages.isEmpty(), () -> "Expected no validation messages for valid input, but got: " + messages);
    }

    @Test
    void validate_invalidInput_returnsMessages() {
        String invalidJson = "{ \"name\": 123 }";
        Set<ValidationMessage> messages = schema.validate(invalidJson, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected validation messages for invalid input, but the set was empty");
        assertEquals(1, messages.size(), () -> "Expected exactly one validation message, but got: " + messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("type", msg.getType(), () -> "Expected a type validation error, but got: " + msg.getType());
    }

    @Test
    void validate_validJson_returnsEmptySet() {
        String validJson = "{ \"name\": \"John Doe\" }";
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);
        assertTrue(messages.isEmpty(), () -> "Expected no validation messages for valid JSON, but got: " + messages);
    }

    @Test
    void validate_validYaml_returnsEmptySet() {
        String validYaml = "name: John Doe\nage: 30";
        Set<ValidationMessage> messages = schema.validate(validYaml, InputFormat.YAML);
        assertTrue(messages.isEmpty(), () -> "Expected no validation messages for valid YAML, but got: " + messages);
    }

    @Test
    void validate_extraProperties_returnsEmptySet() {
        String jsonWithExtra = "{\"name\":\"John\",\"extra\":\"value\"}";
        Set<ValidationMessage> messages = schema.validate(jsonWithExtra, InputFormat.JSON);
        assertTrue(messages.isEmpty(), () -> "Extra properties should be allowed by default, but got: " + messages);
    }

    @Test
    void validate_missingRequiredProperty_returnsMessage() {
        String jsonMissingName = "{\"age\":30}";
        Set<ValidationMessage> messages = schema.validate(jsonMissingName, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for missing required property");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("required", msg.getType(), () -> "Expected a 'required' validation error, but got: " + msg.getType());
    }

    @Test
    void validate_typeMismatch_returnsMessage() {
        String jsonTypeMismatch = "{\"name\":123}";
        Set<ValidationMessage> messages = schema.validate(jsonTypeMismatch, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for type mismatch");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("type", msg.getType(), () -> "Expected a 'type' validation error, but got: " + msg.getType());
    }

    @Test
    void validate_yamlTypeMismatch_returnsMessage() {
        String yamlTypeMismatch = "name: John\nage: thirty";
        Set<ValidationMessage> messages = schema.validate(yamlTypeMismatch, InputFormat.YAML);
        assertFalse(messages.isEmpty(), "Expected a validation message for YAML type mismatch");
        ValidationMessage msg = messages.iterator().next();
        // The schema does not define "age", but we simply verify that at least one message is produced.
        assertNotNull(msg.getMessage(), () -> "Expected a non‑null validation message, but got null");
    }

    @Test
    void validate_emptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.validate("", InputFormat.JSON), "Empty input should trigger IllegalArgumentException");
    }

    @Test
    void validate_whitespaceOnly_throwsIllegalArgumentException() {
        String ws = "   \n\t  ";
        assertThrows(IllegalArgumentException.class, () -> schema.validate(ws, InputFormat.JSON), "Whitespace‑only input should trigger IllegalArgumentException");
    }

    @Test
    void validate_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> schema.validate(null, InputFormat.JSON), "Null input should trigger NullPointerException");
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null), "Null InputFormat should trigger NullPointerException");
    }

    @Test
    void validate_malformedJson_throwsJsonParseException() {
        String malformed = "{name:\"John\"}";
        Exception ex = assertThrows(RuntimeException.class, () -> schema.validate(malformed, InputFormat.JSON), "Malformed JSON should result in a parsing exception");
        assertTrue(ex.getCause() instanceof JsonParseException || ex instanceof JsonParseException, () -> "Expected JsonParseException as cause, but got: " + ex);
    }

    @Test
    void validate_jsonContentWithYamlFormat_throwsException() {
        String json = "{\"name\":\"John\"}";
        Exception ex = assertThrows(RuntimeException.class, () -> schema.validate(json, InputFormat.YAML), "Providing JSON while declaring YAML format should fail");
        // The exact exception type may vary; we only assert that it is a runtime exception.
        assertNotNull(ex, "Exception should not be null");
    }

    @Test
    void validate_malformedPercentEncoding_throwsUnsupportedEncodingException() {
        String badEncoding = "%ZZ%invalid%encoding";
        // The library may wrap the UnsupportedEncodingException; we check the cause.
        Exception ex = assertThrows(RuntimeException.class, () -> schema.validate(badEncoding, InputFormat.JSON), "Malformed percent‑encoding should trigger UnsupportedEncodingException");
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof UnsupportedEncodingException, () -> "Expected UnsupportedEncodingException as cause, but got: " + cause);
    }

    @ParameterizedTest(name = "[{index}] input={0}, format={1}")
    @MethodSource("provideInvalidInputs")
    void validate_invalidInputs_throwException(String input, InputFormat format, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> schema.validate(input, format), () -> "Expected " + expected.getSimpleName() + " for input: " + input);
    }

    @Test
    void validate_yamlTypeMismatch_returnsEmptySet() {
        // The schema does not define "age", so extra properties are ignored.
        String yamlTypeMismatch = "name: John\nage: thirty";
        Set<ValidationMessage> messages = schema.validate(yamlTypeMismatch, InputFormat.YAML);
        assertTrue(messages.isEmpty(), () -> "No validation messages are expected when extra properties are present");
    }

    @Test
    void validate_emptyString_returnsEmptySet() {
        Set<ValidationMessage> messages = schema.validate("", InputFormat.JSON);
        assertTrue(messages.isEmpty(), () -> "Empty input should produce no validation messages");
    }

    @Test
    void validate_whitespaceOnly_returnsEmptySet() {
        String ws = "   \n\t  ";
        Set<ValidationMessage> messages = schema.validate(ws, InputFormat.JSON);
        assertTrue(messages.isEmpty(), () -> "Whitespace‑only input should produce no validation messages");
    }

    @Test
    void validate_nullInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.validate(null, InputFormat.JSON), "Null input should trigger IllegalArgumentException");
    }

    @Test
    void validate_nullInputFormat_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.validate("{\"name\":\"John\"}", null), "Null InputFormat should trigger IllegalArgumentException");
    }

    @Test
    void validate_jsonContentWithYamlFormat_isAccepted() {
        String json = "{\"name\":\"John\"}";
        // YAML parser accepts JSON as valid YAML, so no exception is expected.
        Set<ValidationMessage> messages = schema.validate(json, InputFormat.YAML);
        assertTrue(messages.isEmpty(), () -> "JSON content supplied with YAML format should be accepted and produce no messages");
    }

    @Test
    void validate_malformedPercentEncoding_throwsJsonParseException() {
        String badEncoding = "%ZZ%invalid%encoding";
        Exception ex = assertThrows(RuntimeException.class, () -> schema.validate(badEncoding, InputFormat.JSON), "Malformed percent‑encoding should trigger a parsing exception");
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof JsonParseException, () -> "Expected JsonParseException as cause, but got: " + cause);
    }
}
