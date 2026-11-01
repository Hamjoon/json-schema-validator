package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.core.JsonParseException;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_12_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * Schema under test – created per‑test so that each test gets a fresh instance.
     */
    private JsonSchema schema;

    /**
     * Simple JSON schema used for the happy‑path test (valid object).
     */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /**
     * Schema that requires a {@code name} property of type {@code string}.
     */
    private static final String REQUIRED_STRING_SCHEMA = "{\n" + "  \"type\": \"object\",\n" + "  \"properties\": { \"name\": { \"type\": \"string\" } },\n" + "  \"required\": [\"name\"]\n" + "}";

    /**
     * Schema that allows a top‑level JSON array.
     */
    private static final String ARRAY_SCHEMA = "{\"type\":\"array\"}";

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factory once for the whole test class.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default schema – can be overridden inside individual tests.
        schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    /**
     * Convenience method to create an {@link OutputFormat} that converts a
     * {@link ValidationResult} into a mutable {@link List} of {@link ValidationMessage}s.
     */
    private static OutputFormat<List<ValidationMessage>> simpleListOutputFormat() {
        return result -> new ArrayList<>(result.getMessages());
    }

    /**
     * Verify that the {@code validate(String, InputFormat, OutputFormat)} method
     * forwards the {@link ValidationResult} to the supplied {@link OutputFormat}
     * and returns the value produced by that format.
     */
    @Test
    void validate_returnsOutputFromOutputFormat() {
        // GIVEN – a valid JSON input and a mocked OutputFormat that simply returns the
        // ValidationResult it receives.
        String validJson = "{}";
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // WHEN – the schema is validated using the mocked OutputFormat.
        ValidationResult result = schema.validate(validJson, InputFormat.JSON, outputFormat);
        // THEN – the OutputFormat should have been invoked exactly once with a non‑null
        // ValidationResult, and the method should return the same object that the
        // OutputFormat returned.
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
        assertNotNull(result, "The returned ValidationResult must not be null");
        assertTrue(result.getMessages().isEmpty(), "There should be no validation messages for a valid input");
    }

    /**
     * Verify that validation failures are correctly propagated to the {@link OutputFormat}
     * and that the {@link ValidationResult} contains the expected messages.
     */
    @Test
    void validate_failureIsPropagatedToOutputFormat() {
        // GIVEN – a schema that requires a string property {@code name} and an input that
        // violates this rule (provides a number instead).
        schema = factory.getSchema(REQUIRED_STRING_SCHEMA);
        String invalidJson = "{\"name\": 123}";
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // WHEN
        ValidationResult result = schema.validate(invalidJson, InputFormat.JSON, outputFormat);
        // THEN
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
        assertNotNull(result, "Result must not be null even when validation fails");
        assertFalse(result.getMessages().isEmpty(), "There should be at least one validation message");
        // The library reports a single message for the type mismatch.
        assertEquals(1, result.getMessages().size(), "Exactly one validation message is expected for the type error");
    }

    /**
     * Demonstrates that the method works with a custom {@link OutputFormat}
     * that transforms the {@link ValidationResult} into a simple {@code boolean}
     * indicating success.
     */
    @Test
    void validate_withCustomBooleanOutputFormat() {
        // GIVEN – a custom OutputFormat that returns {@code true} when there are no
        // validation messages and {@code false} otherwise.
        OutputFormat<Boolean> booleanFormat = ValidationResult::isSuccess;
        String json = "{}";
        // WHEN
        Boolean success = schema.validate(json, InputFormat.JSON, booleanFormat);
        // THEN
        assertTrue(success, "The validation should succeed and the custom OutputFormat should return true");
    }

    @Test
    void validate_nullInputString_throwsIllegalArgumentException() {
        // GIVEN
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> schema.validate(null, InputFormat.JSON, format), "A null input string should cause IllegalArgumentException");
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        // GIVEN
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, format), "A null InputFormat must result in NullPointerException");
    }

    @Test
    void validate_nullOutputFormat_throwsNullPointerException() {
        // GIVEN
        // No format instance – we pass null directly.
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"Bob\"}", InputFormat.JSON, null), "A null OutputFormat must cause NullPointerException before validation starts");
    }

    @Test
    void validate_malformedJson_throwsJsonParseException() {
        String malformed = "{invalidJson: true,,}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        assertThrows(JsonParseException.class, () -> schema.validate(malformed, InputFormat.JSON, format), "Malformed JSON should propagate JsonParseException");
    }

    @Test
    void validate_emptyString_throwsJsonParseException() {
        String empty = "";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        assertThrows(JsonParseException.class, () -> schema.validate(empty, InputFormat.JSON, format), "Empty string is not valid JSON and must throw JsonParseException");
    }

    @Test
    void validate_whitespaceOnly_throwsJsonParseException() {
        String ws = "   \n\t  ";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        assertThrows(JsonParseException.class, () -> schema.validate(ws, InputFormat.JSON, format), "Whitespace‑only input should result in JsonParseException");
    }

    @Test
    void validate_invalidUrlEncoding_throwsIllegalArgumentException() {
        String badEncoding = "%GG";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        assertThrows(IllegalArgumentException.class, () -> schema.validate(badEncoding, InputFormat.JSON, format), "Malformed percent‑encoding should cause IllegalArgumentException");
    }

    @Test
    void validate_malformedYaml_throwsJsonParseException() {
        String malformedYaml = "name: John\nage: : thirty";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        assertThrows(JsonParseException.class, () -> schema.validate(malformedYaml, InputFormat.YAML, format), "Invalid YAML must propagate JsonParseException");
    }

    @Test
    void validate_validJsonWithDefaultOutputFormat_returnsEmptyList() {
        String json = "{\"age\":30,\"name\":\"Alice\"}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        List<ValidationMessage> messages = schema.validate(json, InputFormat.JSON, format);
        assertTrue(messages.isEmpty(), "A valid JSON should produce an empty ValidationMessage list");
    }

    @Test
    void validate_emptyJsonObject_returnsEmptyList() {
        String json = "{}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        List<ValidationMessage> messages = schema.validate(json, InputFormat.JSON, format);
        assertTrue(messages.isEmpty(), "Empty JSON object should be valid against a schema without required fields");
    }

    @Test
    void validate_jsonArrayInput_allowedBySchema_returnsEmptyList() {
        // Switch to a schema that permits an array at the root.
        schema = factory.getSchema(ARRAY_SCHEMA);
        String jsonArray = "[1,2,3]";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        List<ValidationMessage> messages = schema.validate(jsonArray, InputFormat.JSON, format);
        assertTrue(messages.isEmpty(), "A valid JSON array should produce no validation messages when the schema allows arrays");
    }

    @Test
    void validate_validYamlInput_returnsEmptyList() {
        // Use the required‑property schema – the YAML provides the required field.
        schema = factory.getSchema(REQUIRED_STRING_SCHEMA);
        String yaml = "name: Charlie\nage: 40";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        List<ValidationMessage> messages = schema.validate(yaml, InputFormat.YAML, format);
        assertTrue(messages.isEmpty(), "Valid YAML input that satisfies the schema should yield an empty list");
    }

    @Test
    void validate_unicodeJsonInput_returnsEmptyList() {
        String json = "{\"name\":\"José\",\"city\":\"München\"}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        List<ValidationMessage> messages = schema.validate(json, InputFormat.JSON, format);
        assertTrue(messages.isEmpty(), "Unicode characters must be handled correctly and produce no validation errors");
    }

    @Test
    void validate_largeJsonInputValid_returnsEmptyList() {
        String largeJson = "{\"items\":[" + "{\"id\":1,\"value\":\"a\"}," + "{\"id\":2,\"value\":\"b\"}," + "{\"id\":3,\"value\":\"c\"}," + "{\"id\":4,\"value\":\"d\"}," + "{\"id\":5,\"value\":\"e\"}" + "],\"count\":5}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        List<ValidationMessage> messages = schema.validate(largeJson, InputFormat.JSON, format);
        assertTrue(messages.isEmpty(), "A large but valid JSON payload should still result in an empty ValidationMessage list");
    }

    @Test
    void validate_missingRequiredProperty_returnsExpectedMessage() {
        schema = factory.getSchema(REQUIRED_STRING_SCHEMA);
        String jsonMissingName = "{\"age\":25}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());
        List<ValidationMessage> messages = schema.validate(jsonMissingName, InputFormat.JSON, format);
        assertEquals(1, messages.size(), "Exactly one validation message is expected for the missing required property");
        assertTrue(messages.get(0).getMessage().contains("required property 'name' is missing"), "The validation message should indicate the missing 'name' property");
    }

    @Test
    void validate_customBooleanOutputFormat_returnsTrueOnSuccess() {
        // Boolean format: true if validation succeeded, false otherwise.
        OutputFormat<Boolean> booleanFormat = ValidationResult::isSuccess;
        String json = "{}";
        Boolean success = schema.validate(json, InputFormat.JSON, booleanFormat);
        assertTrue(success, "Custom boolean OutputFormat should return true for a successful validation");
    }

    @Test
    void validate_customOutputFormatThrowsRuntimeException_propagates() {
        // OutputFormat that deliberately throws.
        @SuppressWarnings("unchecked")
        OutputFormat<Object> explodingFormat = mock(OutputFormat.class);
        when(explodingFormat.apply(any())).thenThrow(new RuntimeException("boom"));
        String json = "{\"valid\":true}";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(json, InputFormat.JSON, explodingFormat), "Exceptions from the OutputFormat must bubble up unchanged");
        assertEquals("boom", ex.getMessage());
    }
}
