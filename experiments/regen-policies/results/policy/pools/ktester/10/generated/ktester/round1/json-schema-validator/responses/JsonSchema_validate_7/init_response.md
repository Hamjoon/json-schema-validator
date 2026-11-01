```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#validate(JsonNode, OutputFormat, Consumer)}.
 * The test verifies that:
 * <ul>
 *   <li>The {@link ExecutionContext} created by {@code createExecutionContext()}
 *       is passed to the supplied {@link Consumer}.</li>
 *   <li>The method forwards the call to the overloaded {@code validate(...)} method
 *       and returns its result.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    // ----- collaborators required for the JsonSchema constructor -----
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;          // the schema definition (can be a dummy node)
    @Mock JsonSchema parentSchema;      // parent schema – not used in this test

    // ----- objects created per test -----
    JsonSchema jsonSchema;              // the class under test (partial mock)
    ExecutionContext executionContext; // the execution context that will be returned

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real (partial) mock of JsonSchema using its package‑private constructor.
        jsonSchema = Mockito.mock(
                JsonSchema.class,
                withSettings()
                        .useConstructor(validationContext, schemaLocation, evaluationPath,
                                schemaNode, parentSchema, false)   // `suppressSubSchemaRetrieval = false`
                        .defaultAnswer(CALLS_REAL_METHODS));

        // Mock the ExecutionContext that the JsonSchema will create.
        executionContext = mock(ExecutionContext.class);
        when(jsonSchema.createExecutionContext()).thenReturn(executionContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to avoid cross‑test contamination.
        Mockito.reset(validationContext, schemaLocation, evaluationPath,
                schemaNode, parentSchema, jsonSchema, executionContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void validate_WithConsumer_ShouldInvokeConsumerAndReturnResult() {
        // ----- arrange ---------------------------------------------------------
        // A dummy JSON instance to validate.
        JsonNode rootNode = mock(JsonNode.class);

        // A dummy OutputFormat that simply returns a constant value.
        @SuppressWarnings("unchecked")
        OutputFormat<String> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any())).thenReturn("formatted-result");

        // Stub the overloaded validate method that the public method delegates to.
        // The signature is: <T> T validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)
        // We only need to match the first three arguments; the fourth can be any().
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any()))
                .thenReturn("formatted-result");

        // Consumer that will receive the ExecutionContext.
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> executionCustomizer = mock(Consumer.class);

        // ----- act -------------------------------------------------------------
        String result = jsonSchema.validate(rootNode, outputFormat, executionCustomizer);

        // ----- assert -----------------------------------------------------------
        // The result must be the value returned by the delegated validate call.
        assertEquals("formatted-result", result, "The validate method should return the delegated result.");

        // The consumer must have been invoked exactly once with the ExecutionContext.
        verify(executionCustomizer, times(1)).accept(executionContext);
    }
}
```