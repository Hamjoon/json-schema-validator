```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchema_hasTypeValidator_Test {

    private static ObjectMapper mapper;
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper for building schema nodes from JSON strings
        mapper = new ObjectMapper();
        // Use Draft‑07 (or any supported version) for the factory
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    @Test
    void hasTypeValidator_returnsTrueWhenSchemaDefinesType() throws Exception {
        // GIVEN a schema node that contains a "type" keyword
        String schemaJson = "{ \"type\": \"string\" }";
        JsonNode schemaNode = mapper.readTree(schemaJson);

        // WHEN a JsonSchema instance is created from that node
        JsonSchema schema = factory.getSchema(schemaNode);

        // THEN hasTypeValidator() should report true
        assertTrue(schema.hasTypeValidator(),
                "hasTypeValidator() should return true when a type validator is present");
    }

    @Test
    void hasTypeValidator_returnsFalseWhenSchemaDoesNotDefineType() throws Exception {
        // GIVEN a schema node without a "type" keyword
        String schemaJson = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonNode schemaNode = mapper.readTree(schemaJson);

        // WHEN a JsonSchema instance is created from that node
        JsonSchema schema = factory.getSchema(schemaNode);

        // THEN hasTypeValidator() should report false
        assertFalse(schema.hasTypeValidator(),
                "hasTypeValidator() should return false when no type validator is defined");
    }
}
```