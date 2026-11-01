```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * The tests focus on the public overload that receives a {@link JsonNode},
 * an {@link OutputFormat} and an {@link ExecutionContextCustomizer}.
 *
 * The schema used in the tests is a minimal object schema with a required
 * {@code name} property of type {@code string}.  Supplying a numeric value for
 * {@code name} triggers a single validation message, which we assert on.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private JsonSchemaFactory factory;

    /** The schema under test. */
    private JsonSchema schema;

    /** Jackson mapper for creating {@link JsonNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A simple object schema that requires a string property {@code name}. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;

    /** Input JSON that violates the schema (numeric {@code name}). */
    private static final String INVALID_INPUT = """
            { "name": 123 }
            """;

    /** A mock customizer – the default behaviour (do‑nothing) is sufficient for the test. */
    @Mock
    ExecutionContextCustomizer executionCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft (the schema declares this draft).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Build the JsonSchema instance from the literal schema string.
        schema = factory.getSchema(SIMPLE_SCHEMA, SchemaValidatorsConfig.builder().build());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Validates a {@link JsonNode} using the {@code validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}
     * overload and verifies that a single validation error is produced.
     */
    @Test
    void validateJsonNodeWithCustomizer_returnsExpectedMessages() throws Exception {
        // Arrange: parse the invalid JSON into a JsonNode.
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);

        // Act: invoke the focal method.  OutputFormat.DEFAULT returns a {@code Set<ValidationMessage>}.
        Set<ValidationMessage> messages = schema.validate(rootNode, OutputFormat.DEFAULT, executionCustomizer);

        // Assert: exactly one validation message (type mismatch for {@code name}) should be present.
        assertNotNull(messages, "The returned message set must not be null");
        assertEquals(1, messages.size(),
                () -> "Expected exactly one validation error, but got: " + messages);
    }

    /**
     * The same validation performed without providing a customizer (null is allowed).
     * This mirrors the overload {@code validate(JsonNode, OutputFormat)}.
     */
    @Test
    void validateJsonNodeWithoutCustomizer_behavesIdentically() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);

        Set<ValidationMessage> messages = schema.validate(rootNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);

        assertNotNull(messages);
        assertEquals(1, messages.size());
    }

    /**
     * Demonstrates usage of a custom {@link OutputFormat}.  Here we use the built‑in
     * {@link OutputFormat#COLLECTOR} which aggregates messages into a {@link java.util.List}.
     */
    @Test
    void validateJsonNodeWithCollectorOutputFormat() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);

        // The collector format returns a List<ValidationMessage>.
        java.util.List<ValidationMessage> list = schema.validate(rootNode, OutputFormat.COLLECTOR, executionCustomizer);

        assertNotNull(list);
        assertEquals(1, list.size());
        // Verify that the message refers to the property "name".
        assertTrue(list.get(0).getPath().contains("name"),
                "The validation message should be about the 'name' property");
    }
}
```