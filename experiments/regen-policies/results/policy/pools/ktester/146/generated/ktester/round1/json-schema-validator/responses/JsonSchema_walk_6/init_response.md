```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean, java.util.function.Consumer)}.
 *
 * The tests create a minimal {@link JsonSchema} instance using {@link JsonSchemaFactory}
 * and verify that:
 * <ul>
 *   <li>The method returns a non‑null {@link ValidationResult} and the result is valid
 *       when the supplied JSON node conforms to the schema.</li>
 *   <li>The {@link java.util.function.Consumer} supplied as the execution customizer is
 *       invoked with the same {@link ExecutionContext} that is passed to the method.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_6_Test {

    /** ObjectMapper used to build {@link JsonNode} instances from JSON strings. */
    private ObjectMapper mapper;

    /** The JsonSchema instance under test – a simple “object” schema. */
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        mapper = new ObjectMapper();

        // A very small schema that accepts any JSON object.
        String schemaJson = "{\"type\":\"object\"}";

        // Use the default factory (draft‑2020‑12) – no special configuration needed.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should be created successfully");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verify that {@code walk(...)} returns a successful {@link ValidationResult}
     * when the input node satisfies the schema and that the supplied customizer
     * is executed.
     */
    @Test
    void walk_WithValidNode_InvokesCustomizerAndReturnsValidResult() throws Exception {
        // GIVEN – a valid JSON object node and a fresh execution context
        JsonNode validNode = mapper.readTree("{\"anyProperty\":\"anyValue\"}");
        ExecutionContext executionContext = new ExecutionContext();

        // A Mockito mock to verify that the consumer is called exactly once
        @SuppressWarnings("unchecked")
        java.util.function.Consumer<ExecutionContext> customizer = mock(java.util.function.Consumer.class);

        // WHEN – the walk method is invoked
        ValidationResult result = schema.walk(executionContext, validNode, true, customizer);

        // THEN – the result must be non‑null and indicate success
        assertNotNull(result, "walk should return a ValidationResult");
        // ValidationResult in networknt library provides isValid()
        assertTrue(result.isValid(), "Result should be valid for a conforming node");

        // AND – the customizer must have been invoked with the same ExecutionContext
        verify(customizer, times(1)).accept(executionContext);
    }

    /**
     * Verify that {@code walk(...)} returns an invalid {@link ValidationResult}
     * when the supplied node does not satisfy the schema.
     */
    @Test
    void walk_WithInvalidNode_ReturnsInvalidResult() throws Exception {
        // GIVEN – a JSON node that is NOT an object (schema expects an object)
        JsonNode invalidNode = mapper.readTree("\"just a string\"");
        ExecutionContext executionContext = new ExecutionContext();

        // No customizer needed for this scenario
        java.util.function.Consumer<ExecutionContext> noop = ctx -> {};

        // WHEN
        ValidationResult result = schema.walk(executionContext, invalidNode, true, noop);

        // THEN – the result should be invalid and contain at least one message
        assertNotNull(result, "walk should return a ValidationResult");
        assertFalse(result.isValid(), "Result should be invalid for a non‑object node");
        Set<ValidationMessage> messages = result.getMessages();
        assertNotNull(messages, "Validation messages should be available");
        assertFalse(messages.isEmpty(), "There should be at least one validation message");
    }
}
```