```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, Consumer)}.
 *
 * The tests focus on the public {@code validate} method that accepts a JSON
 * string, an {@link InputFormat}, and a {@link Consumer} that receives the
 * {@link ExecutionContext} used during validation.
 *
 * No private members of {@link JsonSchema} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_10_Test {

    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;

    private JsonSchema schema;
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft and obtain a JsonSchema instance.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> {}); // default builder – no special configuration
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should be created successfully");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void validateValidJsonCallsConsumerAndReturnsNoMessages() {
        // GIVEN
        String validJson = """{ "name": "John Doe" }""";
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);

        // WHEN
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON, consumer);

        // THEN
        assertTrue(messages.isEmpty(), "A valid JSON document should produce no validation messages");
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
    }

    @Test
    void validateInvalidJsonCallsConsumerAndReturnsMessages() {
        // GIVEN
        String invalidJson = """{ "name": 123 }"""; // name should be a string
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);

        // WHEN
        Set<ValidationMessage> messages = schema.validate(invalidJson, InputFormat.JSON, consumer);

        // THEN
        assertFalse(messages.isEmpty(), "An invalid JSON document should produce validation messages");
        // The schema has only one rule that can be violated in this payload.
        assertEquals(1, messages.size(), "Exactly one validation message is expected");
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
    }
}
```