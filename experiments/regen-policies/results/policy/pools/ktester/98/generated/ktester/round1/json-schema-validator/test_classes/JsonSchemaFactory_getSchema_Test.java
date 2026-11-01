package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

class JsonSchemaFactory_getSchema_Test {

    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        // Use the 2020‑12 draft – it is the most recent and works with the simple
        // schemas used in the tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @Test
    void getSchema_withValidJsonString_returnsJsonSchema() throws Exception {
        // GIVEN – a minimal JSON Schema that accepts only strings
        String schemaJson = "{\"type\":\"string\"}";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // WHEN – the factory creates a JsonSchema from the string
        JsonSchema jsonSchema = factory.getSchema(schemaJson, config);
        // THEN – the result is not null and can validate a correct instance
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        // a valid JSON instance according to the schema
        JsonNode validNode = factory.getObjectMapper(InputFormat.JSON).readTree("\"hello\"");
        Set<ValidationMessage> validationResult = jsonSchema.validate(validNode);
        assertTrue(validationResult.isEmpty(), "Validation should succeed for a valid string");
    }

    @Test
    void getSchema_withInvalidInstance_returnsValidationMessages() throws Exception {
        // GIVEN – the same simple schema
        String schemaJson = "{\"type\":\"string\"}";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        JsonSchema jsonSchema = factory.getSchema(schemaJson, config);
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        // WHEN – an invalid JSON instance (number) is validated
        JsonNode invalidNode = factory.getObjectMapper(InputFormat.JSON).readTree("123");
        Set<ValidationMessage> validationResult = jsonSchema.validate(invalidNode);
        // THEN – validation messages are produced
        assertFalse(validationResult.isEmpty(), "Validation should fail for a non‑string value");
        // optional: check that the message mentions the type mismatch
        boolean containsTypeError = validationResult.stream().anyMatch(msg -> msg.getMessage().contains("type"));
        assertTrue(containsTypeError, "At least one message should refer to a type error");
    }

    @Test
    void getSchema_withDefaultConfig_isEquivalentToBuilderMethod() throws Exception {
        // GIVEN – a schema string and a default‑config instance
        String schemaJson = "{\"type\":\"integer\"}";
        SchemaValidatorsConfig defaultConfig = SchemaValidatorsConfig.builder().build();
        // WHEN – using the overload that accepts a config
        JsonSchema schemaWithConfig = factory.getSchema(schemaJson, defaultConfig);
        // AND – using the overload that creates its own config internally
        JsonSchema schemaWithDefault = factory.getSchema(schemaJson);
        // THEN – both schemas should behave identically
        assertNotNull(schemaWithConfig);
        assertNotNull(schemaWithDefault);
        JsonNode validNode = factory.getObjectMapper(InputFormat.JSON).readTree("42");
        assertTrue(schemaWithConfig.validate(validNode).isEmpty(), "Schema with explicit config should accept a valid integer");
        assertTrue(schemaWithDefault.validate(validNode).isEmpty(), "Schema with internal default config should accept a valid integer");
    }

    @Test
    void getSchema_basicValidJsonString_returnsNonNullSchema() {
        // GIVEN
        String schema = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        // config == null → factory must create a default config internally
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schema, null);
        // THEN
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema");
    }

    @Test
    void getSchema_withFailFastConfig_isPropagatedToValidationContext() {
        // GIVEN
        String schema = "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setFailFast(true);
        JsonSchema jsonSchema = factory.getSchema(schema, cfg);
        assertNotNull(jsonSchema);
        // WHEN – validate an instance that violates two constraints.
        // The first violation is a missing required property, the second is a type mismatch.
        // With failFast=true the validator should stop after the first error.
        JsonNode invalid = factory.getObjectMapper(InputFormat.JSON).readTree("{\"age\":\"not-an-int\"}");
        Set<ValidationMessage> messages = jsonSchema.validate(invalid);
        // THEN – only one message should be present because validation stopped early.
        assertEquals(1, messages.size(), "With failFast=true validation should stop after the first error");
    }

    @Test
    void getSchema_nullSchemaString_throwsIllegalArgumentException() {
        // GIVEN
        String schema = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schema, null), "Passing a null schema string must throw IllegalArgumentException");
    }

    @Test
    void getSchema_emptySchemaString_throwsIllegalArgumentException() {
        // GIVEN
        String schema = "";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schema, null), "Passing an empty schema string must throw IllegalArgumentException");
    }

    @Test
    void getSchema_unknownType_throwsException() {
        // GIVEN – a schema that contains an unknown type keyword
        String schema = "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"unknown\" } } }";
        // WHEN / THEN – the factory should fail while parsing/creating the schema
        // JsonSchemaException extends RuntimeException
        assertThrows(// JsonSchemaException extends RuntimeException
        RuntimeException.class, () -> factory.getSchema(schema, null), "A schema with an unknown type must cause a parsing exception");
    }

    @Test
    void getSchema_metaSchemaIdentifier_returnsMetaSchema() {
        // GIVEN – the well‑known Draft‑07 meta‑schema identifier
        String schema = "http://json-schema.org/draft-07/schema#";
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schema, null);
        // THEN
        assertNotNull(jsonSchema, "Meta‑schema identifier should resolve to a JsonSchema instance");
        // Verify that the meta‑schema can validate a simple instance (e.g. a boolean)
        JsonNode valid = factory.getObjectMapper(InputFormat.JSON).readTree("true");
        assertTrue(jsonSchema.validate(valid).isEmpty(), "Meta‑schema should accept a valid boolean instance");
    }

    @Test
    void getSchema_yamlContentWithDefaultJsonFormat_throwsException() {
        // GIVEN – YAML representation of a schema
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        // WHEN / THEN
        // JsonProcessingException wrapped in RuntimeException
        assertThrows(// JsonProcessingException wrapped in RuntimeException
        RuntimeException.class, () -> factory.getSchema(yaml, null), "Supplying YAML while the factory expects JSON must cause a parsing exception");
    }

    @Test
    void getSchema_typeLooseEnabled_allowsStringForInteger() {
        // GIVEN – schema expects integer, config enables type‑loose
        String schema = "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setTypeLoose(true);
        JsonSchema jsonSchema = factory.getSchema(schema, cfg);
        assertNotNull(jsonSchema);
        // WHEN – a string that can be coerced to integer is validated
        JsonNode instance = factory.getObjectMapper(InputFormat.JSON).readTree("{\"age\":\"42\"}");
        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        // THEN – no validation errors because type‑loose is active
        assertTrue(messages.isEmpty(), "With typeLoose=true the string \"42\" should be accepted for an integer property");
    }

    @Test
    void getSchema_validateFormatsDisabled_ignoresFormatErrors() {
        // GIVEN – schema with a date‑time format
        String schema = "{ \"type\": \"object\", \"properties\": { \"date\": { \"type\": \"string\", \"format\": \"date-time\" } } }";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setValidateFormats(false);
        JsonSchema jsonSchema = factory.getSchema(schema, cfg);
        assertNotNull(jsonSchema);
        // WHEN – an instance with an invalid date‑time string is validated
        JsonNode instance = factory.getObjectMapper(InputFormat.JSON).readTree("{\"date\":\"not-a-date\"}");
        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        // THEN – format validation is skipped, therefore no errors
        assertTrue(messages.isEmpty(), "When validateFormats=false the invalid date‑time string must not produce a validation error");
    }

    @Test
    void getSchema_freshDefaultConfig_behavesLikeNullConfig() {
        // GIVEN – a simple integer schema and a freshly created config with defaults
        String schema = "{ \"type\": \"integer\" }";
        // defaults
        SchemaValidatorsConfig freshConfig = new SchemaValidatorsConfig();
        JsonSchema withConfig = factory.getSchema(schema, freshConfig);
        JsonSchema withNull = factory.getSchema(schema, null);
        assertNotNull(withConfig);
        assertNotNull(withNull);
        // WHEN – both schemas validate a correct integer instance
        JsonNode valid = factory.getObjectMapper(InputFormat.JSON).readTree("123");
        Set<ValidationMessage> msgsConfig = withConfig.validate(valid);
        Set<ValidationMessage> msgsNull = withNull.validate(valid);
        // THEN – both validation results must be identical (empty)
        assertTrue(msgsConfig.isEmpty() && msgsNull.isEmpty(), "A freshly instantiated default config must behave the same as passing null");
    }

    @Test
    void getSchema_schemaStringWithWhitespace_isParsedSuccessfully() {
        // GIVEN – a valid schema surrounded by whitespace and newlines
        String schema = "   { \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }   \n";
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schema, null);
        // THEN
        assertNotNull(jsonSchema, "Whitespace‑trimmed schema string should be parsed without error");
    }

    @Test
    void getSchema_unresolvableRef_throwsException() {
        // GIVEN – schema containing a $ref that does not exist
        String schema = "{ \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"#/nonexistent\" } } }";
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> factory.getSchema(schema, null), "Unresolvable $ref must cause a schema loading exception");
    }

    @Test
    void getSchema_fileUriLoadsSchemaSuccessfully() throws IOException {
        // GIVEN – write a simple schema to a temporary file
        File tempFile = File.createTempFile("temp-schema", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }");
        }
        String uri = tempFile.toURI().toString();
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(uri, null);
        // THEN
        assertNotNull(jsonSchema, "Schema loaded from a file URI should not be null");
        JsonNode valid = factory.getObjectMapper(InputFormat.JSON).readTree("{\"id\":10}");
        assertTrue(jsonSchema.validate(valid).isEmpty(), "Loaded schema should validate a correct instance");
    }

    @Test
    void getSchema_sameUriReturnedFromCache_isIdenticalInstance() throws IOException {
        // GIVEN – a temporary file containing a schema
        File tempFile = File.createTempFile("cached-schema", ".json");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{ \"type\": \"object\", \"properties\": { \"code\": { \"type\": \"string\" } } }");
        }
        String uri = tempFile.toURI().toString();
        // WHEN – request the same URI twice
        JsonSchema first = factory.getSchema(uri, null);
        JsonSchema second = factory.getSchema(uri, null);
        // THEN – the factory should return the same instance (cached)
        assertSame(first, second, "Repeated calls with the same absolute URI must return the cached JsonSchema instance");
    }

    @Test
    void getSchema_malformedJson_throwsException() {
        // GIVEN – JSON missing a closing brace
        String schema = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } ";
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> factory.getSchema(schema, null), "Invalid JSON syntax must result in a parsing exception");
    }
}
