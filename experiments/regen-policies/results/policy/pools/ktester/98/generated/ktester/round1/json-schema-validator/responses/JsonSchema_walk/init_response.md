```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)}.
 *
 * The tests create a minimal {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * then invoke {@code walk} with a real {@link ExecutionContext}, a {@link JsonNode}
 * representing the instance to validate and a no‑op {@link ExecutionContextCustomizer}.
 *
 * The expectations are:
 * <ul>
 *   <li>When the instance conforms to the schema, {@link ValidationResult#getMessages()}
 *       is empty.</li>
 *   <li>When the instance violates the schema, the result contains the expected
 *       {@link ValidationMessage}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Shared schema used by all tests. */
    private static JsonSchema jsonSchema;

    /** ExecutionContext created for each test method. */
    private ExecutionContext executionContext;

    /** No‑op customizer – required by the API but does not modify the context. */
    private ExecutionContextCustomizer noOpCustomizer = ctx -> {
        // intentionally left blank
    };

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // A very small schema: an object with a required string property "name".
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

        // Use the factory to obtain a JsonSchema instance. No special configuration is needed.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema(schemaJson);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh ExecutionContext for every test ensures isolation.
        executionContext = new ExecutionContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to release for this test class.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up at the class level.
    }

    /**
     * Walk the schema with a valid JSON instance.
     * Expect an empty set of validation messages.
     */
    @Test
    void walk_validInstance_returnsSuccess() throws Exception {
        // {"name":"John"}
        JsonNode instance = MAPPER.readTree("{\"name\":\"John\"}");

        ValidationResult result = jsonSchema.walk(executionContext, instance, true, noOpCustomizer);

        // The walk should succeed – no validation messages.
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid instance");
    }

    /**
     * Walk the schema with an invalid JSON instance (wrong type for {@code name}).
     * Expect at least one validation message describing the type mismatch.
     */
    @Test
    void walk_invalidInstance_returnsFailure() throws Exception {
        // {"name":123}
        JsonNode instance = MAPPER.readTree("{\"name\":123}");

        ValidationResult result = jsonSchema.walk(executionContext, instance, true, noOpCustomizer);

        Set<ValidationMessage> messages = result.getMessages();
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid instance");

        // The library reports the property path in the message; we verify that the
        // message mentions the property "name" and the expected type "string".
        boolean containsName = messages.stream()
                .anyMatch(m -> m.getPath().equals("name") && m.getMessage().contains("string"));
        assertTrue(containsName, "Expected a type‑mismatch message for property 'name'");
    }
}
```