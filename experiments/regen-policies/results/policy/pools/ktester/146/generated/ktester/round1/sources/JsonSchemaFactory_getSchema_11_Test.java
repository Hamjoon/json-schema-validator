package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_11_Test {

    private JsonSchemaFactory factory;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Create a factory instance that uses the 2020‑12 draft.  The default
         * configuration is sufficient for the simple schemas used in the
         * tests, therefore we rely on the static factory method.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    static Stream<Arguments> validJsonProvider() {
        return Stream.of(Arguments.of("Simple object schema", "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), Arguments.of("Schema with $ref", "{\n" + "  \"definitions\": { \"posInt\": { \"type\": \"integer\", \"minimum\": 0 } },\n" + "  \"type\": \"object\",\n" + "  \"properties\": { \"age\": { \"$ref\": \"#/definitions/posInt\" } }\n" + "}"), Arguments.of("Schema with explicit $schema meta‑schema", "{ \"$schema\":\"http://json-schema.org/draft-07/schema#\", \"type\":\"object\", \"properties\":{\"id\":{\"type\":\"string\"}} }"));
    }

    static Stream<Arguments> invalidSchemaProvider() {
        return Stream.of(Arguments.of("Mismatched format – JSON string with YAML format", "{\"type\":\"boolean\"}", InputFormat.YAML), Arguments.of("Mismatched format – YAML string with JSON format", "type: object\nproperties:\n  age:\n    type: integer", InputFormat.JSON), Arguments.of("Malformed JSON", "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } ", InputFormat.JSON), Arguments.of("Malformed YAML", "type: object\nproperties\n  name: { type: string }", InputFormat.YAML));
    }

    @Test
    void getSchema_withJsonFormat_returnsValidSchema() throws Exception {
        // GIVEN a simple JSON schema and the JSON input format
        String jsonSchema = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;
        // WHEN the factory creates a JsonSchema from the string
        JsonSchema schema = factory.getSchema(jsonSchema, InputFormat.JSON);
        // THEN the schema object is not null
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance");
        // AND a valid instance validates without errors
        JsonNode validInstance = MAPPER.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> errors = schema.validate(validInstance);
        assertTrue(errors.isEmpty(), "A valid JSON instance must produce no validation messages");
        // AND an invalid instance produces at least one validation message
        JsonNode invalidInstance = MAPPER.readTree("{\"name\":123}");
        Set<ValidationMessage> invalidErrors = schema.validate(invalidInstance);
        assertFalse(invalidErrors.isEmpty(), "An invalid JSON instance must produce validation messages");
    }

    @Test
    void getSchema_withYamlFormat_returnsValidSchema() throws Exception {
        // GIVEN a simple YAML schema and the YAML input format
        String yamlSchema = """
            type: object
            properties:
              age:
                type: integer
            required:
              - age
            """;
        // WHEN the factory creates a JsonSchema from the YAML string
        JsonSchema schema = factory.getSchema(yamlSchema, InputFormat.YAML);
        // THEN the schema object is not null
        assertNotNull(schema, "Factory should return a non‑null JsonSchema instance for YAML input");
        // AND a valid instance validates without errors
        JsonNode validInstance = MAPPER.readTree("{\"age\":30}");
        Set<ValidationMessage> errors = schema.validate(validInstance);
        assertTrue(errors.isEmpty(), "A valid YAML‑derived schema must accept a correct JSON instance");
        // AND an invalid instance produces validation errors
        JsonNode invalidInstance = MAPPER.readTree("{\"age\":\"thirty\"}");
        Set<ValidationMessage> invalidErrors = schema.validate(invalidInstance);
        assertFalse(invalidErrors.isEmpty(), "A wrong type should be reported by the validator");
    }

    @Test
    void getSchema_withEmptyString_throwsIllegalArgumentException() {
        // GIVEN an empty schema string
        String emptySchema = "";
        // WHEN the factory is asked to parse it, THEN an IllegalArgumentException is expected
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(emptySchema, InputFormat.JSON), "Empty schema strings should result in an IllegalArgumentException");
    }

    @Test
    void getSchema_withNullString_throwsNullPointerException() {
        // GIVEN a null schema reference
        String nullSchema = null;
        // WHEN the factory is asked to parse it, THEN a NullPointerException is expected
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullSchema, InputFormat.JSON), "Null schema strings should result in a NullPointerException");
    }

    @Test
    void getSchema_validJson_returnsSchema() throws Exception {
        // GIVEN a well‑formed JSON schema
        String schema = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;
        // WHEN the factory parses it as JSON
        JsonSchema jsonSchema = factory.getSchema(schema, InputFormat.JSON);
        // THEN a non‑null schema is returned and validates correctly
        assertNotNull(jsonSchema, "Schema must not be null");
        JsonNode valid = MAPPER.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> errors = jsonSchema.validate(valid);
        assertTrue(errors.isEmpty(), "Valid instance should produce no errors");
    }

    @Test
    void getSchema_validYaml_returnsSchema() throws Exception {
        // GIVEN a well‑formed YAML schema
        String yaml = """
            type: object
            properties:
              age:
                type: integer
            required:
              - age
            """;
        // WHEN the factory parses it as YAML
        JsonSchema yamlSchema = factory.getSchema(yaml, InputFormat.YAML);
        // THEN the schema works as expected
        assertNotNull(yamlSchema);
        JsonNode valid = MAPPER.readTree("{\"age\":30}");
        assertTrue(yamlSchema.validate(valid).isEmpty());
    }

    @Test
    void getSchema_whitespaceIsIgnored() throws Exception {
        // GIVEN a JSON schema surrounded by whitespace
        String schema = "   {\"type\":\"string\"}   ";
        JsonSchema s = factory.getSchema(schema, InputFormat.JSON);
        assertNotNull(s);
        JsonNode valid = MAPPER.readTree("\"hello\"");
        assertTrue(s.validate(valid).isEmpty());
    }

    @Test
    void getSchema_yamlWithBomAndWhitespace() throws Exception {
        // GIVEN a YAML schema that starts with a UTF‑8 BOM and contains surrounding whitespace
        String yaml = "\ufeff   \n" + "type: object\n" + "properties:\n" + "  name:\n" + "    type: string\n";
        JsonSchema s = factory.getSchema(yaml, InputFormat.YAML);
        assertNotNull(s);
        JsonNode valid = MAPPER.readTree("{\"name\":\"Bob\"}");
        assertTrue(s.validate(valid).isEmpty());
    }

    @Test
    void getSchema_internalRef_resolvesCorrectly() throws Exception {
        // GIVEN a JSON schema with an internal $ref
        String schema = """
            {
              "definitions": {
                "positiveInteger": { "type": "integer", "minimum": 0 }
              },
              "type": "object",
              "properties": {
                "age": { "$ref": "#/definitions/positiveInteger" }
              }
            }
            """;
        JsonSchema s = factory.getSchema(schema, InputFormat.JSON);
        assertNotNull(s);
        // VALID instance (age >= 0)
        JsonNode ok = MAPPER.readTree("{\"age\":5}");
        assertTrue(s.validate(ok).isEmpty());
        // INVALID instance (negative age)
        JsonNode bad = MAPPER.readTree("{\"age\":-1}");
        assertFalse(s.validate(bad).isEmpty());
    }

    @Test
    void getSchema_explicitMetaSchemaReference() throws Exception {
        // GIVEN a schema that explicitly references Draft‑07 meta‑schema
        String schema = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "id": { "type": "string" }
              }
            }
            """;
        JsonSchema s = factory.getSchema(schema, InputFormat.JSON);
        assertNotNull(s);
        JsonNode ok = MAPPER.readTree("{\"id\":\"123\"}");
        assertTrue(s.validate(ok).isEmpty());
    }

    @Test
    void getSchema_unicodePatternEmoji() throws Exception {
        // GIVEN a schema with an emoji Unicode range pattern
        String schema = """
            {
              "type": "object",
              "properties": {
                "emoji": {
                  "type": "string",
                  "pattern": "^[\\uD83D\\uDE00-\\uD83D\\uDE4F]$"
                }
              }
            }
            """;
        JsonSchema s = factory.getSchema(schema, InputFormat.JSON);
        assertNotNull(s);
        // 😀 is U+1F600 (first in the range)
        JsonNode ok = MAPPER.readTree("{\"emoji\":\"😀\"}");
        assertTrue(s.validate(ok).isEmpty());
        JsonNode bad = MAPPER.readTree("{\"emoji\":\"A\"}");
        assertFalse(s.validate(bad).isEmpty());
    }

    @Test
    void getSchema_unicodePatternChinese() throws Exception {
        // GIVEN a schema with a Chinese character range pattern
        String schema = """
            {
              "type": "object",
              "properties": {
                "city": {
                  "type": "string",
                  "pattern": "^[\\u4e00-\\u9fa5]+$"
                }
              }
            }
            """;
        JsonSchema s = factory.getSchema(schema, InputFormat.JSON);
        assertNotNull(s);
        JsonNode ok = MAPPER.readTree("{\"city\":\"北京\"}");
        assertTrue(s.validate(ok).isEmpty());
        JsonNode bad = MAPPER.readTree("{\"city\":\"Beijing\"}");
        assertFalse(s.validate(bad).isEmpty());
    }

    @Test
    void getSchema_nullSchema_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, InputFormat.JSON), "Null schema strings should result in a NullPointerException");
    }

    @Test
    void getSchema_emptySchema_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema("", InputFormat.JSON), "Empty schema strings should result in an IllegalArgumentException");
    }

    @Test
    void getSchema_whitespaceOnly_throwsIllegalArgumentException() {
        String wsOnly = "   \t  \n  ";
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(wsOnly, InputFormat.JSON), "Whitespace‑only schema should be treated as empty");
    }

    @Test
    void getSchema_nullInputFormat_throwsIllegalArgumentException() {
        String schema = "{\"type\":\"number\"}";
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schema, null), "Null InputFormat must cause an IllegalArgumentException");
    }

    @Test
    void getSchema_unsupportedInputFormat_throwsIllegalArgumentException(@Mock InputFormat unknownFormat) {
        // Mockito creates a mock that is not one of the known enum constants.
        String schema = "{\"type\":\"string\"}";
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schema, unknownFormat), "An unknown InputFormat should trigger an IllegalArgumentException");
    }

    @Test
    void getSchema_mismatchedJsonWithYamlFormat_throwsParsingException() {
        String json = "{\"type\":\"boolean\"}";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> factory.getSchema(json, InputFormat.YAML), "Parsing JSON as YAML must fail");
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException (parsing failure)");
    }

    @Test
    void getSchema_mismatchedYamlWithJsonFormat_throwsParsingException() {
        String yaml = """
            type: object
            properties:
              age:
                type: integer
            """;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> factory.getSchema(yaml, InputFormat.JSON), "Parsing YAML as JSON must fail");
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException (parsing failure)");
    }

    @Test
    void getSchema_malformedJson_throwsParsingException() {
        String malformed = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } ";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> factory.getSchema(malformed, InputFormat.JSON), "Malformed JSON should cause a parsing exception");
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException");
    }

    @Test
    void getSchema_malformedYaml_throwsParsingException() {
        String malformed = """
            type: object
            properties
              name: { type: string }
            """;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> factory.getSchema(malformed, InputFormat.YAML), "Malformed YAML should cause a parsing exception");
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException");
    }

    @Test
    void getSchema_veryLargeSchema_parsesSuccessfully() {
        // Build a schema with 10 000 dummy properties (still syntactically valid)
        StringBuilder sb = new StringBuilder("{\"type\":\"object\",\"properties\":{");
        for (int i = 0; i < 10_000; i++) {
            sb.append("\"p").append(i).append("\":{\"type\":\"string\"}");
            if (i < 9_999)
                sb.append(',');
        }
        sb.append("}}");
        String largeSchema = sb.toString();
        JsonSchema s = factory.getSchema(largeSchema, InputFormat.JSON);
        assertNotNull(s, "Factory should handle a very large schema without throwing");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validJsonProvider")
    void getSchema_parameterisedValidJson(String description, String schema) {
        JsonSchema s = factory.getSchema(schema, InputFormat.JSON);
        assertNotNull(s);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidSchemaProvider")
    void getSchema_parameterisedInvalid(String description, String schema, InputFormat fmt) {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> factory.getSchema(schema, fmt), description);
        assertTrue(ex.getCause() instanceof IOException, "Root cause must be an IOException for parsing failures");
    }
}
