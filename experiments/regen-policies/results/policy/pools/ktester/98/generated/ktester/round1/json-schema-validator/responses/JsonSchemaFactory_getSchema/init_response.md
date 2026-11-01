```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String, SchemaValidatorsConfig)}.
 */
class JsonSchemaFactory_getSchema_Test {

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the 2020‑12 draft – it is the most recent and works with the simple
        // schemas used in the tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
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
        boolean containsTypeError = validationResult.stream()
                .anyMatch(msg -> msg.getMessage().contains("type"));
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
        assertTrue(schemaWithConfig.validate(validNode).isEmpty(),
                "Schema with explicit config should accept a valid integer");
        assertTrue(schemaWithDefault.validate(validNode).isEmpty(),
                "Schema with internal default config should accept a valid integer");
    }
}
```