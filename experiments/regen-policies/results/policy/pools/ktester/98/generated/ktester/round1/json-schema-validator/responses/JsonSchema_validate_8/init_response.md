```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, Consumer)}.
 *
 * The tests focus on the interaction between the {@code Consumer<ExecutionContext>}
 * customizer and the {@code OutputFormat}.  A real {@link JsonSchema} instance is
 * created via {@link JsonSchemaFactory} so that the method under test exercises
 * the actual validation logic without needing to mock internal collaborators.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_8_Test {

    /** Factory used for creating {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** A simple schema that expects an object with a required property {@code name}. */
    private static JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – the default for the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Schema: object with a required property "name"
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "required": [ "name" ]
                }
                """;
        schema = factory.getSchema(schemaJson);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up per test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
    }

    /**
     * Verify that a valid JSON node results in an empty {@link Set} of
     * {@link ValidationMessage}s and that the supplied {@code Consumer}
     * is invoked exactly once with the {@link ExecutionContext} created by the
     * {@link JsonSchema}.
     */
    @Test
    void validate_validNode_callsCustomizer_andReturnsEmptySet(@Mock Consumer<ExecutionContext> customizer) throws Exception {
        // arrange – a JSON object that satisfies the schema (has the required "name")
        JsonNode validNode = JsonMapper.getInstance()
                .readTree("{\"name\":\"John Doe\"}");

        // act
        Set<ValidationMessage> messages = schema.validate(validNode, OutputFormat.DEFAULT, customizer);

        // assert – no validation errors
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");

        // assert – the customizer was called once with a non‑null ExecutionContext
        verify(customizer, times(1)).accept(any(ExecutionContext.class));
    }

    /**
     * Verify that an invalid JSON node yields validation messages while the
     * customizer is still invoked. The schema requires the property {@code name},
     * so omitting it should produce exactly one message.
     */
    @Test
    void validate_invalidNode_callsCustomizer_andReturnsMessages(@Mock Consumer<ExecutionContext> customizer) throws Exception {
        // arrange – a JSON object missing the required "name" property
        JsonNode invalidNode = JsonMapper.getInstance()
                .readTree("{\"age\":30}");

        // act
        Set<ValidationMessage> messages = schema.validate(invalidNode, OutputFormat.DEFAULT, customizer);

        // assert – we expect a single validation error about the missing required property
        assertEquals(1, messages.size(),
                "Expected exactly one validation message for missing required property");

        // The message should contain the word "required" (implementation detail, but useful)
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("required"),
                "Validation message should mention the required property");

        // assert – the customizer was still called exactly once
        verify(customizer, times(1)).accept(any(ExecutionContext.class));
    }
}
```