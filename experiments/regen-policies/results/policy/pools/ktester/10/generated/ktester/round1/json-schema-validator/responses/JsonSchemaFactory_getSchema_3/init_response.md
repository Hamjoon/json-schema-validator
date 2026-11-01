```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.SpecVersion.VersionFlag;

import java.util.Set;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(JsonNode)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_3_Test {

    private static ObjectMapper mapper;
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialize a shared ObjectMapper for parsing JSON strings.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default factory for Draft‑07 (V7). This provides all required
        // default meta‑schemas and configuration.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to clean up.
    }

    @Test
    void testGetSchemaReturnsValidSchemaAndValidates() throws Exception {
        // given: a simple object schema that requires a "name" property of type string.
        String schemaJson = "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "    \"name\":{ \"type\":\"string\" }"
                + "},"
                + "\"required\":[\"name\"]"
                + "}";
        JsonNode schemaNode = mapper.readTree(schemaJson);

        // when: the schema is created via the factory.
        JsonSchema jsonSchema = factory.getSchema(schemaNode);

        // then: the returned JsonSchema must not be null.
        assertNotNull(jsonSchema, "JsonSchema should not be null");

        // Validate a correct instance – should produce no errors.
        String validInstanceJson = "{\"name\":\"John\"}";
        JsonNode validNode = mapper.readTree(validInstanceJson);
        Set<ValidationMessage> errorsValid = jsonSchema.validate(validNode);
        assertTrue(errorsValid.isEmpty(), "Valid instance should have no validation errors");

        // Validate an incorrect instance – missing required property "name".
        String invalidInstanceJson = "{}";
        JsonNode invalidNode = mapper.readTree(invalidInstanceJson);
        Set<ValidationMessage> errorsInvalid = jsonSchema.validate(invalidNode);
        assertFalse(errorsInvalid.isEmpty(), "Invalid instance should have validation errors");

        // The error set should contain a message about the missing required property "name".
        boolean containsMissingName = errorsInvalid.stream()
                .anyMatch(msg -> msg.getMessage().contains("required") && msg.getMessage().contains("name"));
        assertTrue(containsMissingName, "Error messages should indicate missing required property 'name'");
    }

    @Test
    void testGetSchemaWithNullJsonNodeThrowsException() {
        // given: a null JsonNode.
        JsonNode nullNode = null;

        // when / then: invoking getSchema(null) must throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(nullNode));
    }
}
```