```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, ExecutionContextCustomizer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_4_Test {

    private static JsonSchemaFactory factory;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft – this is the public way to obtain a JsonSchema
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        mapper = new ObjectMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test class
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally
    }

    /**
     * Validates that the {@code validate(JsonNode, ExecutionContextCustomizer)} method
     * correctly returns a validation error when the instance does not conform to the
     * schema and that a customizer can be supplied without interfering with the result.
     */
    @Test
    void validate_WithExecutionContextCustomizer_ReturnsExpectedMessages(@Mock ExecutionContextCustomizer customizer) {
        // GIVEN – a simple schema that requires a string property "name"
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // AND – an input JSON where "name" is an integer (invalid according to the schema)
        String inputJson = """
                {
                  "name": 123
                }
                """;

        JsonNode inputNode = mapper.readTree(inputJson);

        // WHEN – the schema is validated with a (mocked) customizer
        Set<ValidationMessage> messages = schema.validate(inputNode, customizer);

        // THEN – exactly one validation message is produced and it mentions a type error
        assertEquals(1, messages.size(), "Expected a single validation error");
        ValidationMessage message = messages.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("type"),
                "Validation message should refer to a type mismatch");
        // Verify that the customizer was not invoked (the method under test does not call it directly)
        verifyNoInteractions(customizer);
    }
}
```