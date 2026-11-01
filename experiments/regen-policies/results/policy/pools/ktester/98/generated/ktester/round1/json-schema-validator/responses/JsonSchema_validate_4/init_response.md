```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, ExecutionContextCustomizer)}.
 *
 * The tests use a real {@link JsonSchema} instance created via {@link JsonSchemaFactory}
 * with a very small schema.  A {@link ExecutionContextCustomizer} mock is supplied
 * to verify that the customizer is invoked during validation.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_4_Test {

    private static JsonSchema schema;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // a minimal schema: an object with a single string property "name"
        String schemaJson = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{ \"type\":\"string\" }"
                + "},"
                + "\"required\":[\"name\"]"
                + "}";

        // use the default factory (draft‑2020‑12) – no special loaders needed
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // nothing required for each test at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up after all tests
    }

    @Test
    void validate_validNode_callsCustomizerAndReturnsEmptySet(@Mock ExecutionContextCustomizer customizer) throws Exception {
        // given – a valid JSON node that satisfies the schema
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");

        // when – validate is invoked with a mocked customizer
        Set<ValidationMessage> messages = schema.validate(validNode, customizer);

        // then – no validation errors and the customizer was invoked exactly once
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
        verify(customizer, times(1)).customize(any(ExecutionContext.class));
    }

    @Test
    void validate_invalidNode_callsCustomizerAndReturnsMessages(@Mock ExecutionContextCustomizer customizer) throws Exception {
        // given – an invalid JSON node (wrong type for 'name')
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");

        // when
        Set<ValidationMessage> messages = schema.validate(invalidNode, customizer);

        // then – at least one validation message is produced and the customizer was still called
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
        verify(customizer, times(1)).customize(any(ExecutionContext.class));
    }
}
```