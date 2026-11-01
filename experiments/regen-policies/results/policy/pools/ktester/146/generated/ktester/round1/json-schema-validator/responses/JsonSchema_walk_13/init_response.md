```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, OutputFormat, boolean)}.
 *
 * The test creates a minimal {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * supplies a mocked {@link OutputFormat}, and verifies that the walk method delegates
 * to the {@code OutputFormat} and returns the expected value.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_13_Test {

    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";
    private static final String INPUT_JSON = "{ \"name\": \"John\" }";

    private JsonSchemaFactory factory;
    private JsonSchema schema;
    private ExecutionContext executionContext;

    @Mock
    private OutputFormat<String> outputFormat; // mocked OutputFormat that returns a String

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft (the most recent draft supported by the library).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // Build the schema from a tiny JSON‑Schema definition.
        schema = factory.getSchema(SIMPLE_SCHEMA);

        // Fresh execution context for each test.
        executionContext = new ExecutionContext();

        // Configure the mock OutputFormat – it should simply return a fixed string
        // when its apply method is invoked with any set of ValidationMessage.
        when(outputFormat.apply(anySet())).thenReturn("formatted‑result");
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references; not strictly required but keeps the test tidy.
        schema = null;
        executionContext = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    @Test
    void walk_WithOutputFormat_ReturnsFormattedResult() {
        // ------------------------------------------------------------
        // GIVEN – a valid execution context, input JSON and a mock format
        // ------------------------------------------------------------
        // (All set up in @BeforeEach)

        // ------------------------------------------------------------
        // WHEN – the walk method is invoked
        // ------------------------------------------------------------
        String result = schema.walk(
                executionContext,
                INPUT_JSON,
                InputFormat.JSON,
                outputFormat,
                false); // 'validate' flag set to false – we only care about the formatting path

        // ------------------------------------------------------------
        // THEN – the result should be the value supplied by the mock
        // ------------------------------------------------------------
        assertEquals("formatted‑result", result, "The walk method should return the value produced by OutputFormat");

        // Verify that the mock was indeed used.
        verify(outputFormat, times(1)).apply(anySet());
    }

    @Test
    void walk_WithValidationEnabled_StillReturnsFormattedResult() {
        // This test repeats the same flow but with validation turned on.
        String result = schema.walk(
                executionContext,
                INPUT_JSON,
                InputFormat.JSON,
                outputFormat,
                true); // enable validation

        assertEquals("formatted‑result", result);
        verify(outputFormat, times(1)).apply(anySet());
    }

    @Test
    void walk_WithEmptyInput_ReturnsFormattedResult() {
        // Walk an empty JSON object – the schema permits any object, so validation passes.
        String emptyJson = "{}";

        String result = schema.walk(
                executionContext,
                emptyJson,
                InputFormat.JSON,
                outputFormat,
                false);

        assertEquals("formatted‑result", result);
        verify(outputFormat, times(1)).apply(anySet());
    }
}
```