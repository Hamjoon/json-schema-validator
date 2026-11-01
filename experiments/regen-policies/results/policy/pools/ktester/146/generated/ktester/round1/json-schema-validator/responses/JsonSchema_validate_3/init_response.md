```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, Consumer)}.
 *
 * The tests focus on the public {@code validate(JsonNode, Consumer<ExecutionContext>)}
 * overload. They verify that:
 * <ul>
 *   <li>The supplied {@code Consumer} is executed.</li>
 *   <li>Validation is performed correctly for a simple schema.</li>
 * </ul>
 *
 * No private members of {@link JsonSchema} or its collaborators are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_3_Test {

    /** Shared Jackson mapper used to create {@link JsonNode} instances. */
    private static ObjectMapper mapper;

    /** The factory used to create {@link JsonSchema} instances. */
    private JsonSchemaFactory factory;

    /** The schema under test – a very small schema that requires a {@code name}
     *  property of type {@code string}. */
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the Jackson ObjectMapper once for the whole test class.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a factory for the 2020‑12 draft (the most recent stable draft in the
        // library). No special configuration is required for these tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // Simple schema: an object with a required string property "name".
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "required": ["name"],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """;

        // Create the JsonSchema instance that will be exercised.
        schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly required but keeps the test tidy.
        schema = null;
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
        mapper = null;
    }

    /**
     * Verify that the {@code Consumer<ExecutionContext>} passed to {@code validate}
     * is invoked exactly once and that validation works as expected.
     */
    @Test
    void validate_withConsumer_executesConsumerAndDetectsErrors() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN – a JSON instance that violates the schema (name is a number)
        // --------------------------------------------------------------------
        String invalidJson = """
                {
                  "name": 123
                }
                """;
        JsonNode invalidNode = mapper.readTree(invalidJson);

        // A flag that the consumer will toggle when it is called.
        AtomicBoolean consumerCalled = new AtomicBoolean(false);

        // Consumer that simply records that it has been executed.
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerCalled.set(true);

        // --------------------------------------------------------------------
        // WHEN – the validation method is invoked
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = schema.validate(invalidNode, executionCustomizer);

        // --------------------------------------------------------------------
        // THEN – the consumer must have been called and a validation error must be present
        // --------------------------------------------------------------------
        assertTrue(consumerCalled.get(),
                "The execution customizer supplied to validate(...) should have been invoked.");

        // The schema requires a string for 'name', so we expect exactly one error.
        assertNotNull(messages, "The returned message set must not be null.");
        assertEquals(1, messages.size(),
                "For the provided invalid instance a single validation message is expected.");

        // Optional sanity check: the message should contain the property name.
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().contains("name"),
                "The validation message should reference the offending property 'name'.");
    }

    /**
     * Verify that a valid JSON instance produces an empty message set while the
     * consumer is still executed.
     */
    @Test
    void validate_withConsumer_andValidInstance_returnsEmptySet() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN – a JSON instance that satisfies the schema
        // --------------------------------------------------------------------
        String validJson = """
                {
                  "name": "John Doe"
                }
                """;
        JsonNode validNode = mapper.readTree(validJson);

        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerCalled.set(true);

        // --------------------------------------------------------------------
        // WHEN
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = schema.validate(validNode, executionCustomizer);

        // --------------------------------------------------------------------
        // THEN
        // --------------------------------------------------------------------
        assertTrue(consumerCalled.get(),
                "The execution customizer must be invoked even when validation succeeds.");
        assertNotNull(messages, "The returned message set must not be null.");
        assertTrue(messages.isEmpty(),
                "A valid instance should result in an empty set of validation messages.");
    }
}
```