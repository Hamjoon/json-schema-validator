```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer", "minimum": 0 }
              },
              "required": ["name"]
            }
            """;

    private JsonSchema jsonSchema;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a JsonSchema instance from the simple schema string.
        // JsonSchemaFactory is thread‑safe and can be reused.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        jsonSchema = factory.getSchema(SIMPLE_SCHEMA);
        mapper = JsonMapperFactory.getMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the objects are garbage‑collected.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void validate_validJson_returnsEmptyMessages() throws Exception {
        // GIVEN a JSON document that satisfies the schema
        String validJson = """
                {
                  "name": "John Doe",
                  "age": 30
                }
                """;

        // WHEN validate is invoked with a no‑op consumer
        Consumer<ExecutionContext> noop = ctx -> { };
        Set<ValidationMessage> messages = jsonSchema.validate(validJson, InputFormat.JSON, noop);

        // THEN no validation messages are produced
        assertNotNull(messages, "The returned set should never be null");
        assertTrue(messages.isEmpty(), "Expected no validation errors for a valid document");
    }

    @Test
    void validate_invalidJson_returnsMessages() throws Exception {
        // GIVEN a JSON document that violates the schema (missing required 'name' and negative age)
        String invalidJson = """
                {
                  "age": -5
                }
                """;

        // WHEN validate is invoked with a no‑op consumer
        Consumer<ExecutionContext> noop = ctx -> { };
        Set<ValidationMessage> messages = jsonSchema.validate(invalidJson, InputFormat.JSON, noop);

        // THEN validation messages are reported
        assertNotNull(messages, "The returned set should never be null");
        assertFalse(messages.isEmpty(), "Expected validation errors for an invalid document");

        // OPTIONAL: check that the messages contain the expected keywords
        boolean hasRequired = messages.stream()
                .anyMatch(m -> "required".equalsIgnoreCase(m.getType()));
        boolean hasMinimum = messages.stream()
                .anyMatch(m -> "minimum".equalsIgnoreCase(m.getType()));
        assertTrue(hasRequired, "Expected a 'required' validation error");
        assertTrue(hasMinimum, "Expected a 'minimum' validation error");
    }
}
```