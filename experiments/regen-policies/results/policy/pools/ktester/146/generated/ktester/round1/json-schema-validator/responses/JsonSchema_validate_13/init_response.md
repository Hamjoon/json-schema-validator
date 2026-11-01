```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * The tests focus on the orchestration performed by the method:
 * <ul>
 *   <li>the {@code OutputFormat} is asked to customise the {@link ExecutionContext}</li>
 *   <li>the optional {@link ExecutionContextCustomizer} is invoked when supplied</li>
 *   <li>the {@code OutputFormat} formats the result using the validation messages returned by the internal validation</li>
 * </ul>
 *
 * All heavy‑weight collaborators (schema loading, actual validation logic, etc.) are mocked
 * so the test runs fast and deterministically.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /** The schema instance under test – a spy so we can stub the internal validation. */
    private JsonSchema schemaSpy;

    /** Mocks used by the test methods. */
    @Mock private ExecutionContext executionContext;
    @Mock private JsonNode jsonNode;
    @Mock private OutputFormat<String> outputFormat;
    @Mock private ExecutionContextCustomizer executionCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal real JsonSchema using the factory (empty schema is sufficient).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema realSchema = factory.getSchema("{}");   // an empty object schema

        // Spy the real instance so we can stub the internal validate(ExecutionContext, JsonNode) method.
        schemaSpy = Mockito.spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        Mockito.reset(executionContext, jsonNode, outputFormat, executionCustomizer, schemaSpy);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verifies the normal flow when a non‑null {@link ExecutionContextCustomizer} is supplied.
     */
    @Test
    void validate_withCustomizer_callsAllCollaborators() {
        // ----- GIVEN ---------------------------------------------------------
        // Prepare a deterministic set of validation messages that the internal validation will return.
        Set<ValidationMessage> validationMessages = Collections.emptySet();
        doReturn(validationMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        // The format.customize and executionCustomizer.customize are void methods – we just let them run.
        // The format.format must return a concrete value that we can assert on.
        when(outputFormat.format(any(JsonSchema.class), eq(validationMessages), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn("formatted-result");

        // ----- WHEN ----------------------------------------------------------
        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, executionCustomizer);

        // ----- THEN ----------------------------------------------------------
        // Verify that the format was asked to customise the execution context.
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));

        // Verify that the customizer was invoked.
        verify(executionCustomizer, times(1)).customize(eq(executionContext), any(ValidationContext.class));

        // Verify that the format.format method received the expected arguments.
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(validationMessages), eq(executionContext),
                any(ValidationContext.class));

        // The method should return exactly what the format.format stub returned.
        assertEquals("formatted-result", result);
    }

    /**
     * Verifies that the method works correctly when the {@link ExecutionContextCustomizer}
     * argument is {@code null} – the customizer must not be invoked.
     */
    @Test
    void validate_withoutCustomizer_skipsCustomizer() {
        // ----- GIVEN ---------------------------------------------------------
        Set<ValidationMessage> validationMessages = Collections.emptySet();
        doReturn(validationMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        when(outputFormat.format(any(JsonSchema.class), eq(validationMessages), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn("no-customizer-result");

        // ----- WHEN ----------------------------------------------------------
        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, null);

        // ----- THEN ----------------------------------------------------------
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));

        // The customizer must never be called.
        verifyNoInteractions(executionCustomizer);

        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(validationMessages), eq(executionContext),
                any(ValidationContext.class));

        assertEquals("no-customizer-result", result);
    }
}
```