```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchema#validate(JsonNode, Consumer)}.
 *
 * The test builds a simple {@code JsonSchema} instance using the public
 * {@link JsonSchemaFactory#getSchema(JsonNode)} factory method, then calls the
 * {@code validate} overload that receives a {@link Consumer} for customizing the
 * {@link ExecutionContext}. The consumer is a no‑op in these tests – the goal
 * is simply to verify that the method delegates correctly to the underlying
 * validation logic.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_3_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema jsonSchema;   // schema under test
    private JsonNode   schemaNode;   // {"type":"object"}

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a very small schema: {"type":"object"}
        schemaNode = MAPPER.readTree("{\"type\":\"object\"}");

        // JsonSchemaFactory is the public way to obtain a JsonSchema instance.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        jsonSchema = factory.getSchema(schemaNode);
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now – the objects are GC‑ed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    @Test
    void validate_validObject_returnsEmptyMessages() throws Exception {
        // GIVEN a JSON object that satisfies the schema
        JsonNode validInstance = MAPPER.readTree("{\"name\":\"John\",\"age\":30}");

        // WHEN we validate with a no‑op consumer
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* nothing */ };
        Set<ValidationMessage> messages = jsonSchema.validate(validInstance, noOpCustomizer);

        // THEN the result set should be empty (no validation errors)
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid object");
    }

    @Test
    void validate_invalidArray_returnsMessages() throws Exception {
        // GIVEN a JSON array (does NOT satisfy the \"object\" type schema)
        JsonNode invalidInstance = MAPPER.readTree("[1,2,3]");

        // WHEN we validate with a no‑op consumer
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* nothing */ };
        Set<ValidationMessage> messages = jsonSchema.validate(invalidInstance, noOpCustomizer);

        // THEN the result set should contain at least one validation error
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid instance");
        // Optional: check that the message mentions the type mismatch
        boolean containsTypeError = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑related validation message");
    }
}
```