```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, OutputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * The test creates a minimal {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * supplies a mock {@link OutputFormat} and a mock {@link ExecutionContextCustomizer},
 * and verifies that the walk method delegates to the output format and that the
 * customizer is invoked exactly once.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_15_Test {

    /** The schema under test – a very simple “object” schema. */
    private JsonSchema jsonSchema;

    /** Execution context used for the walk call. */
    private ExecutionContext executionContext;

    /** Mocked output format that will receive the {@link ValidationResult}. */
    @Mock
    private OutputFormat<String> outputFormat;

    /** Mocked customizer that may modify the execution context. */
    @Mock
    private ExecutionContextCustomizer executionCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣ Build a minimal schema instance.
        //    The factory hides the private constructors of JsonSchema.
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(s -> s.schemas(Collections.emptyMap())));
        jsonSchema = factory.getSchema(schemaJson);

        // 2️⃣ Create a fresh execution context for each test.
        executionContext = new ExecutionContext();

        // 3️⃣ Reset the mocks – MockitoExtension already does this, but we keep it explicit.
        reset(outputFormat, executionCustomizer);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test clean‑up needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global clean‑up needed.
    }

    /**
     * Verifies that {@code walk} returns the value produced by the supplied {@link OutputFormat}
     * and that the {@link ExecutionContextCustomizer} is invoked exactly once.
     */
    @Test
    void walkShouldDelegateToOutputFormatAndInvokeCustomizer() {
        // GIVEN
        String jsonInput = "{\"name\":\"John\"}";
        // The output format is expected to receive a ValidationResult and return a string.
        when(outputFormat.apply(any())).thenReturn("formatted-result");

        // WHEN
        String result = jsonSchema.walk(
                executionContext,
                jsonInput,
                InputFormat.JSON,
                outputFormat,
                /*validate=*/true,
                executionCustomizer);

        // THEN
        assertEquals("formatted-result", result, "The walk method should return the value from OutputFormat");
        // Verify that the customizer was called exactly once with the same execution context.
        verify(executionCustomizer, times(1)).customize(executionContext);
        // Verify that the output format received a ValidationResult (the exact instance is not important for this test).
        verify(outputFormat, times(1)).apply(any());
    }

    /**
     * The same scenario as above but with validation disabled.
     * This ensures the boolean flag does not affect the delegation logic.
     */
    @Test
    void walkWithoutValidationShouldStillDelegate() {
        // GIVEN
        String jsonInput = "{\"age\":30}";
        when(outputFormat.apply(any())).thenReturn("no‑validation-result");

        // WHEN
        String result = jsonSchema.walk(
                executionContext,
                jsonInput,
                InputFormat.JSON,
                outputFormat,
                /*validate=*/false,
                executionCustomizer);

        // THEN
        assertEquals("no-validation-result", result);
        verify(executionCustomizer, times(1)).customize(executionContext);
        verify(outputFormat, times(1)).apply(any());
    }
}
```