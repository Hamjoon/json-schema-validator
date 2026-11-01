```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer)}.
 *
 * The tests focus on the public {@code walk} method that delegates to the private
 * {@code walkAtNodeInternal} implementation.  A minimal {@link JsonSchema} instance
 * is created via {@link JsonSchemaFactory}.  An {@link OutputFormat} implementation
 * that simply returns a constant value is used so that the result of {@code walk}
 * can be asserted.  A {@link Consumer} is supplied and verified that it is invoked.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_11_Test {

    private static JsonSchemaFactory factory;
    private static JsonSchema schema;

    private ExecutionContext executionContext;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // a very small schema – an empty object schema is sufficient for the walk tests
        String simpleSchema = "{ \"type\": \"object\" }";
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(simpleSchema);
    }

    @BeforeEach
    void setupBeforeEach() {
        executionContext = new ExecutionContext(); // default execution context
        mapper = new ObjectMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up for now
    }

    /**
     * Verify that {@code walk} returns the value produced by the supplied {@link OutputFormat}
     * and that the {@link Consumer} customiser is invoked exactly once.
     */
    @Test
    void walk_withConsumer_returnsOutputAndInvokesConsumer() throws Exception {
        // ----- given ---------------------------------------------------------
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");

        // simple OutputFormat that returns a constant string
        OutputFormat<String> outputFormat = messages -> "formatted-output";

        // consumer that flips a flag so we can assert it was called
        final boolean[] consumerCalled = {false};
        Consumer<ExecutionContext> customizer = ctx -> consumerCalled[0] = true;

        // ----- when ----------------------------------------------------------
        String result = schema.walk(executionContext, node, outputFormat, false, customizer);

        // ----- then ----------------------------------------------------------
        assertEquals("formatted-output", result, "walk should return the value from OutputFormat");
        assertTrue(consumerCalled[0], "Consumer customizer must have been invoked");
    }

    /**
     * The same flow but with validation enabled.  The schema is an empty object schema,
     * so the node is valid and the walk should still return the OutputFormat result.
     */
    @Test
    void walk_withValidationEnabled_returnsOutput() throws Exception {
        // ----- given ---------------------------------------------------------
        JsonNode node = mapper.readTree("{\"a\":1}");
        OutputFormat<Integer> outputFormat = messages -> messages.size(); // return number of messages
        Consumer<ExecutionContext> customizer = ctx -> { /* no‑op */ };

        // ----- when ----------------------------------------------------------
        Integer result = schema.walk(executionContext, node, outputFormat, true, customizer);

        // ----- then ----------------------------------------------------------
        // The node conforms to the empty object schema, therefore no validation messages
        assertEquals(0, result.intValue(), "There should be no validation messages for a valid node");
    }
}
```