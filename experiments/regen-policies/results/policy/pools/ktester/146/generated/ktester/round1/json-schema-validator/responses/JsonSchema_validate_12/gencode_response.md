```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * Additional unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat)}.
 *
 * <p>These tests cover the edge‑case scenarios described in the supplied JSON test
 * matrix, such as null arguments, malformed input, custom {@link OutputFormat}
 * implementations, and various input formats (JSON/YAML).</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_12_AdditionalTest {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Schema under test – created per‑test so that each test gets a fresh instance. */
    private JsonSchema schema;

    /** Simple schema that accepts any JSON object (no constraints). */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /** Schema that requires a {@code name} property of type {@code string}. */
    private static final String REQUIRED_STRING_SCHEMA = "{\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": { \"name\": { \"type\": \"string\" } },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    /** Schema that allows a top‑level JSON array. */
    private static final String ARRAY_SCHEMA = "{\"type\":\"array\"}";

    @BeforeAll
    static void setupBeforeAll() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default schema – can be overridden inside individual tests.
        schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    /* --------------------------------------------------------------------- */
    /*  Null / Invalid arguments                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_nullInputString_throwsIllegalArgumentException() {
        // GIVEN
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(null, InputFormat.JSON, format),
                "A null input string should cause IllegalArgumentException");
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        // GIVEN
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"John\"}", null, format),
                "A null InputFormat must result in NullPointerException");
    }

    @Test
    void validate_nullOutputFormat_throwsNullPointerException() {
        // GIVEN
        // No format instance – we pass null directly.

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"Bob\"}", InputFormat.JSON, null),
                "A null OutputFormat must cause NullPointerException before validation starts");
    }

    /* --------------------------------------------------------------------- */
    /*  Malformed inputs (JSON / YAML)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_malformedJson_throwsJsonParseException() {
        String malformed = "{invalidJson: true,,}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        assertThrows(JsonParseException.class,
                () -> schema.validate(malformed, InputFormat.JSON, format),
                "Malformed JSON should propagate JsonParseException");
    }

    @Test
    void validate_emptyString_throwsJsonParseException() {
        String empty = "";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        assertThrows(JsonParseException.class,
                () -> schema.validate(empty, InputFormat.JSON, format),
                "Empty string is not valid JSON and must throw JsonParseException");
    }

    @Test
    void validate_whitespaceOnly_throwsJsonParseException() {
        String ws = "   \n\t  ";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        assertThrows(JsonParseException.class,
                () -> schema.validate(ws, InputFormat.JSON, format),
                "Whitespace‑only input should result in JsonParseException");
    }

    @Test
    void validate_invalidUrlEncoding_throwsIllegalArgumentException() {
        String badEncoding = "%GG";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(badEncoding, InputFormat.JSON, format),
                "Malformed percent‑encoding should cause IllegalArgumentException");
    }

    @Test
    void validate_malformedYaml_throwsJsonParseException() {
        String malformedYaml = "name: John\nage: : thirty";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        assertThrows(JsonParseException.class,
                () -> schema.validate(malformedYaml, InputFormat.YAML, format),
                "Invalid YAML must propagate JsonParseException");
    }

    /* --------------------------------------------------------------------- */
    /*  Valid inputs – expecting empty ValidationMessage list                */
    /* --------------------------------------------------------------------- */

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
        String largeJson = "{\"items\":["
                + "{\"id\":1,\"value\":\"a\"},"
                + "{\"id\":2,\"value\":\"b\"},"
                + "{\"id\":3,\"value\":\"c\"},"
                + "{\"id\":4,\"value\":\"d\"},"
                + "{\"id\":5,\"value\":\"e\"}"
                + "],\"count\":5}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        List<ValidationMessage> messages = schema.validate(largeJson, InputFormat.JSON, format);
        assertTrue(messages.isEmpty(), "A large but valid JSON payload should still result in an empty ValidationMessage list");
    }

    /* --------------------------------------------------------------------- */
    /*  Validation failures – required property missing                      */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_missingRequiredProperty_returnsExpectedMessage() {
        schema = factory.getSchema(REQUIRED_STRING_SCHEMA);
        String jsonMissingName = "{\"age\":25}";
        OutputFormat<List<ValidationMessage>> format = result -> new ArrayList<>(result.getMessages());

        List<ValidationMessage> messages = schema.validate(jsonMissingName, InputFormat.JSON, format);
        assertEquals(1, messages.size(), "Exactly one validation message is expected for the missing required property");
        assertTrue(messages.get(0).getMessage().contains("required property 'name' is missing"),
                "The validation message should indicate the missing 'name' property");
    }

    /* --------------------------------------------------------------------- */
    /*  Custom OutputFormat behaviours                                      */
    /* --------------------------------------------------------------------- */

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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, InputFormat.JSON, explodingFormat),
                "Exceptions from the OutputFormat must bubble up unchanged");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Helper – simple list OutputFormat implementation (used in many tests) */
    /* --------------------------------------------------------------------- */

    /**
     * Convenience method to create an {@link OutputFormat} that converts a
     * {@link ValidationResult} into a mutable {@link List} of {@link ValidationMessage}s.
     */
    private static OutputFormat<List<ValidationMessage>> simpleListOutputFormat() {
        return result -> new ArrayList<>(result.getMessages());
    }
}
```