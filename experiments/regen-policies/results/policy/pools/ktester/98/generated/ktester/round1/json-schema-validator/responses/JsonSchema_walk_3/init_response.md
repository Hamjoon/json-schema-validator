```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

class JsonSchema_walk_3_Test {

    private static JsonSchemaFactory factory;
    private static JsonSchema schema;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // simple schema – an object with a required string property "name"
        String schemaData = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\","
                + "\"type\": \"object\","
                + "\"properties\": {"
                + "    \"name\": {\"type\": \"string\"}"
                + "},"
                + "\"required\": [\"name\"]"
                + "}";

        // use the factory to obtain a JsonSchema instance
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(schemaData);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // nothing needed per‑test at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing needed per‑test at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing needed globally at the moment
    }

    @Test
    void walkValidNodeReturnsNoMessages() throws Exception {
        // given: a valid JSON instance {"name":"John"}
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");

        // when: walk is invoked with a fresh ExecutionContext and validation enabled
        ExecutionContext ctx = new ExecutionContext();
        ValidationResult result = schema.walk(ctx, validNode, true);

        // then: the result should contain no validation messages
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
    }

    @Test
    void walkInvalidNodeReturnsMessages() throws Exception {
        // given: an invalid JSON instance {"name":1}
        JsonNode invalidNode = mapper.readTree("{\"name\":1}");

        // when: walk is invoked with a fresh ExecutionContext and validation enabled
        ExecutionContext ctx = new ExecutionContext();
        ValidationResult result = schema.walk(ctx, invalidNode, true);

        // then: the result should contain at least one validation message
        Set<ValidationMessage> messages = result.getMessages();
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
        // optional: verify the message mentions the type mismatch
        boolean containsTypeError = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑error message");
    }
}
```