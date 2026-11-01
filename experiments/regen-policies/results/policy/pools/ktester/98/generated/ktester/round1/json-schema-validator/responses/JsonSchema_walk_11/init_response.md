```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_11_Test {

    private static ObjectMapper MAPPER;
    private static JsonSchemaFactory FACTORY;
    private static JsonSchema SCHEMA;

    /** Simple schema: an object with a required string property {@code name}. */
    private static final String SCHEMA_JSON = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        MAPPER = new ObjectMapper();
        // Use the default factory for the 2020‑12 draft
        FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Build a JsonSchema instance from the JSON string above
        SCHEMA = FACTORY.getSchema(SCHEMA_JSON);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    /**
     * When {@code validate} is {@code true} the walk method must perform validation and
     * return the set of {@link ValidationMessage}s produced by the schema.
     */
    @Test
    void walk_validatesWhenFlagIsTrue() throws Exception {
        // ----------------------------------------------------------------------
        // given
        // ----------------------------------------------------------------------
        ExecutionContext execCtx = SCHEMA.createExecutionContext();

        // JSON that violates the schema (name is a number, not a string)
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");

        // Simple OutputFormat that just returns the raw set of messages
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;

        // No‑op customizer – we only need to satisfy the method signature
        Consumer<ExecutionContext> customizer = ctx -> {};

        // ----------------------------------------------------------------------
        // when
        // ----------------------------------------------------------------------
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, true, customizer);

        // ----------------------------------------------------------------------
        // then
        // ----------------------------------------------------------------------
        assertNotNull(result, "Resulting message set must not be null");
        assertFalse(result.isEmpty(), "Invalid input should produce at least one validation message");
        // The message should refer to the 'type' keyword for the property 'name'
        boolean typeErrorPresent = result.stream()
                .anyMatch(m -> "type".equals(m.getType()) && m.getPath().equals("$.name"));
        assertTrue(typeErrorPresent, "Expected a type‑error message for $.name");
    }

    /**
     * When {@code validate} is {@code false} the walk method must skip validation and
     * therefore return an empty set of messages.
     */
    @Test
    void walk_skipsValidationWhenFlagIsFalse() throws Exception {
        // ----------------------------------------------------------------------
        // given
        // ----------------------------------------------------------------------
        ExecutionContext execCtx = SCHEMA.createExecutionContext();

        // Same invalid JSON as before – it would normally cause a validation error
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");

        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {};

        // ----------------------------------------------------------------------
        // when
        // ----------------------------------------------------------------------
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, false, customizer);

        // ----------------------------------------------------------------------
        // then
        // ----------------------------------------------------------------------
        assertNotNull(result, "Resulting message set must not be null");
        assertTrue(result.isEmpty(), "When validation is disabled the result set should be empty");
    }
}
```