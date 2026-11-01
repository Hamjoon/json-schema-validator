```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    /** Shared objects for all tests */
    private static JsonSchema schema;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // A tiny schema that requires a string property "name"
        String schemaData = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }""";

        // Use the factory to obtain a JsonSchema instance – this is the public way
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        schema = factory.getSchema(schemaData);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    /**
     * Verify that a valid JSON node produces an empty list of validation messages
     * when the {@code validate(ExecutionContext, JsonNode, OutputFormat)} method
     * is used.
     */
    @Test
    void testValidateValidNode() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");

        // OutputFormat that simply collects the messages into a List
        OutputFormat<List<ValidationMessage>> format = messages -> new ArrayList<>(messages);

        List<ValidationMessage> result = schema.validate(ctx, node, format);
        assertTrue(result.isEmpty(), "Expected no validation messages for a valid node");
    }

    /**
     * Verify that an invalid JSON node (wrong type for {@code name}) yields a
     * non‑empty list of validation messages.
     */
    @Test
    void testValidateInvalidNode() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":1}");

        OutputFormat<List<ValidationMessage>> format = messages -> new ArrayList<>(messages);

        List<ValidationMessage> result = schema.validate(ctx, node, format);
        assertFalse(result.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, result.size(), "Exactly one validation error should be reported");
    }
}
```