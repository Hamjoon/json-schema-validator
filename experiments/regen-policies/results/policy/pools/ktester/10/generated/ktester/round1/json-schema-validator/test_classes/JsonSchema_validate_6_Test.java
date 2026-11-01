package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_6_Test {

    /**
     * Instance under test
     */
    private JsonSchema jsonSchema;

    /**
     * Dependencies required to build a JsonSchema instance
     */
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    private SchemaLocation schemaLocation;

    private JsonNodePath evaluationPath;

    // typical enum value
    private InputFormat inputFormat = InputFormat.JSON;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        /* --------------------------------------------------------------------
         * Build the minimal objects required by JsonSchema.from(...).  The
         * schema we use is an empty JSON object (i.e. "{}") which imposes no
         * constraints – therefore any well‑formed JSON should validate
         * without producing ValidationMessage instances.
         * -------------------------------------------------------------------- */
        // empty schema node: {}
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        // dummy location – the actual IRI is irrelevant for this test
        // null absolute IRI → allowed for test
        schemaLocation = new SchemaLocation(null);
        // evaluation path – root path
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // Stub the ValidationContext behaviour required by the constructor
        // (the real implementation uses these maps to store resources)
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // The meta‑schema is also queried for anchors – we return null for simplicity
        when(metaSchema.readAnchor(emptySchemaNode)).thenReturn(null);
        when(metaSchema.readDynamicAnchor(emptySchemaNode)).thenReturn(null);
        // Create the JsonSchema instance via the public static factory method
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, emptySchemaNode, // no parent schema
        null, // suppressSubSchemaRetrieval
        false);
    }

    /* --------------------------------------------------------------------
     * Helper to create a JsonSchema from a JSON schema string.
     * -------------------------------------------------------------------- */
    private JsonSchema createSchemaFromJson(String schemaJson) throws JsonProcessingException {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        return JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
    }

    /* --------------------------------------------------------------------
     * Helper to create a JsonSchema from a YAML schema string.
     * -------------------------------------------------------------------- */
    private JsonSchema createSchemaFromYaml(String yaml) {
        // YamlMapperFactory provides a Jackson mapper for YAML.
        JsonNode schemaNode = YamlMapperFactory.getMapper().readTree(yaml);
        return JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
    }

    // ---------- Valid inputs – JSON ----------
    private static Stream<Arguments> validJsonInputs() {
        return Stream.of(Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}", "A well‑formed JSON schema string is supplied with InputFormat.JSON. The method should deserialize the JSON successfully and return an empty validation result set."), Arguments.of("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}},\"required\":[\"age\"]}", "A schema with a required field should still return an empty set when only the schema is parsed."));
    }

    // ---------- Valid inputs – YAML ----------
    private static Stream<Arguments> validYamlInputs() {
        return Stream.of(Arguments.of("type: object\nproperties:\n  name:\n    type: string", "A well‑formed YAML schema string is supplied with InputFormat.YAML."));
    }

    @Test
    void validate_EmptySchema_WithValidJson_ReturnsEmptyMessageSet() {
        // GIVEN a well‑formed JSON document
        String json = "{}";
        // WHEN the string‑based validate method is invoked
        Set<ValidationMessage> messages = jsonSchema.validate(json, inputFormat);
        // THEN no validation messages are produced
        assertTrue(messages.isEmpty(), () -> "Expected no validation messages, but got: " + messages);
    }

    @Test
    void validate_EmptySchema_WithInvalidJson_ThrowsException() {
        // GIVEN an invalid JSON document (malformed)
        String malformedJson = "{";
        // WHEN the validation is attempted, a JsonProcessingException (or a
        // subclass) should be thrown by the internal deserializer.
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformedJson, inputFormat));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("validJsonInputs")
    void validate_ValidJsonInputs_ReturnsEmptySet(String schemaString, String description) {
        // The schema is parsed *once* when the JsonSchema instance is built.
        // Here we build a schema that imposes no constraints on the *instance*.
        assertDoesNotThrow(() -> {
            JsonSchema schema = createSchemaFromJson(schemaString);
            Set<ValidationMessage> msgs = schema.validate("{}", InputFormat.JSON);
            assertTrue(msgs.isEmpty(), "Expected empty validation messages for schema: " + description);
        });
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("validYamlInputs")
    void validate_ValidYamlInputs_ReturnsEmptySet(String yaml, String description) {
        assertDoesNotThrow(() -> {
            JsonSchema schema = createSchemaFromYaml(yaml);
            Set<ValidationMessage> msgs = schema.validate("{}", InputFormat.YAML);
            assertTrue(msgs.isEmpty(), "Expected empty validation messages for YAML schema: " + description);
        });
    }

    // ---------- Invalid inputs – malformed JSON ----------
    @Test
    void validate_MalformedJson_ThrowsRuntimeException() {
        String malformed = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}";
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformed, InputFormat.JSON));
    }

    // ---------- Invalid inputs – malformed YAML ----------
    @Test
    void validate_MalformedYaml_ThrowsRuntimeException() {
        String malformedYaml = "type: object\nproperties:\n  name:\n    type: string\n  - invalid";
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformedYaml, InputFormat.YAML));
    }

    // ---------- Edge case – null input string ----------
    @Test
    void validate_NullInput_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.validate(null, InputFormat.JSON));
    }

    // ---------- Edge case – empty input string ----------
    @Test
    void validate_EmptyInput_ThrowsRuntimeException() {
        assertThrows(RuntimeException.class, () -> jsonSchema.validate("", InputFormat.JSON));
    }

    // ---------- Edge case – null InputFormat ----------
    @Test
    void validate_NullInputFormat_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.validate("{}", null));
    }

    // ---------- Edge case – unsupported InputFormat ----------
    @Test
    void validate_UnsupportedInputFormat_ThrowsUnsupportedOperationException() {
        // Assuming the enum contains a value UNKNOWN for the purpose of this test.
        InputFormat unknown = InputFormat.valueOf("UNKNOWN");
        assertThrows(UnsupportedOperationException.class, () -> jsonSchema.validate("{}", unknown));
    }

    // ---------- Special handling – URL encoded input ----------
    @Test
    void validate_UrlEncodedJson_ReturnsEmptySet() {
        // {"type":"object"}
        String urlEncoded = "%7B%22type%22%3A%22object%22%7D";
        Set<ValidationMessage> msgs = jsonSchema.validate(urlEncoded, InputFormat.JSON);
        assertTrue(msgs.isEmpty(), "URL‑decoded JSON should validate without errors");
    }

    // ---------- Valid inputs – whitespace handling ----------
    @Test
    void validate_WhitespaceTrimmedJson_ReturnsEmptySet() {
        String padded = "   {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}   ";
        Set<ValidationMessage> msgs = jsonSchema.validate(padded, InputFormat.JSON);
        assertTrue(msgs.isEmpty(), "Whitespace‑padded JSON should be accepted");
    }

    // ---------- Valid inputs – Unicode characters ----------
    @Test
    void validate_UnicodeCharactersJson_ReturnsEmptySet() {
        String json = "{\"type\":\"object\",\"properties\":{\"emoji\":{\"type\":\"string\"}},\"description\":\"😀\"}";
        Set<ValidationMessage> msgs = jsonSchema.validate(json, InputFormat.JSON);
        assertTrue(msgs.isEmpty(), "JSON containing Unicode characters should validate");
    }

    // ---------- Invalid inputs – whitespace‑only string ----------
    @Test
    void validate_WhitespaceOnly_ThrowsRuntimeException() {
        String wsOnly = "   \n\t  ";
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(wsOnly, InputFormat.JSON));
    }

    // ---------- Invalid inputs – instance violates required property ----------
    @Test
    void validate_InstanceViolatesRequiredProperty_ReturnsMessage() throws Exception {
        String schema = "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}},\"required\":[\"age\"]}";
        JsonSchema schemaObj = createSchemaFromJson(schema);
        Set<ValidationMessage> msgs = schemaObj.validate("{}", InputFormat.JSON);
        assertFalse(msgs.isEmpty(), "Missing required property should produce a validation message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("age")), "Validation message should mention the missing 'age' property");
    }

    // ---------- Invalid inputs – type mismatch ----------
    @Test
    void validate_TypeMismatch_ReturnsMessage() throws Exception {
        String schema = "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}";
        JsonSchema schemaObj = createSchemaFromJson(schema);
        Set<ValidationMessage> msgs = schemaObj.validate("{\"age\":\"twenty\"}", InputFormat.JSON);
        assertFalse(msgs.isEmpty(), "Type mismatch should produce a validation message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("integer")), "Message should indicate that 'age' is not an integer");
    }

    // ---------- Invalid inputs – JSON array when object expected ----------
    @Test
    void validate_ArrayInsteadOfObject_ReturnsMessage() throws Exception {
        // schema expects an object (empty schema defaults to object)
        Set<ValidationMessage> msgs = jsonSchema.validate("[{\"age\":30}]", InputFormat.JSON);
        assertFalse(msgs.isEmpty(), "Array root should trigger a type‑mismatch validation message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("object")), "Message should mention that an object was expected");
    }

    // ---------- Edge case – UTF‑8 BOM prefix ----------
    @Test
    void validate_Utf8BomPrefix_ReturnsEmptySet() {
        String bom = "\ufeff{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        Set<ValidationMessage> msgs = jsonSchema.validate(bom, InputFormat.JSON);
        assertTrue(msgs.isEmpty(), "JSON prefixed with UTF‑8 BOM should be parsed correctly");
    }

    // ---------- Special handling – JSON with comments (should fail) ----------
    @Test
    void validate_JsonWithComments_ThrowsRuntimeException() {
        String jsonWithComment = "{/* comment */ \"type\":\"object\"}";
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(jsonWithComment, InputFormat.JSON));
    }

    // ---------- Valid inputs – YAML with Windows line endings ----------
    @Test
    void validate_YamlWithWindowsLineEndings_ReturnsEmptySet() {
        String yaml = "type: object\r\nproperties:\r\n  name:\r\n    type: string\r\n";
        Set<ValidationMessage> msgs = jsonSchema.validate(yaml, InputFormat.YAML);
        assertTrue(msgs.isEmpty(), "YAML with CRLF line endings should be parsed without errors");
    }

    // ---------- Invalid inputs – malformed URL‑encoded JSON ----------
    @Test
    void validate_MalformedUrlEncodedJson_ThrowsRuntimeException() {
        // missing closing brace after decoding
        String malformed = "%7B%22type%22%3A%22object%22%7";
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformed, InputFormat.JSON));
    }

    // ---------- Invalid inputs – both input and InputFormat null ----------
    @Test
    void validate_BothNull_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.validate(null, null));
    }

    // ---------- Invalid inputs – mismatched format (YAML supplied as JSON) ----------
    @Test
    void validate_YamlSuppliedAsJson_ThrowsRuntimeException() {
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(yaml, InputFormat.JSON));
    }
}
