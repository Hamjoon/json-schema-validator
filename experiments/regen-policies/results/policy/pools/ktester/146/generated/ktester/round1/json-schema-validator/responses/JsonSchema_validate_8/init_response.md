```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, Consumer)}.
 *
 * The test creates a {@link JsonSchema} instance via the package‑private
 * {@code JsonSchema.from(...)} factory method using mocked collaborators.
 * It then spies on the instance to stub the deeper {@code validate}
 * overload that receives an {@link ExecutionContext}. This allows us to
 * verify that the {@code Consumer<ExecutionContext>} supplied by the caller
 * is invoked exactly once and that the result from the delegated method
 * is returned unchanged.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_8_Test {

    /** The {@link JsonSchema} instance under test – created once for all tests. */
    private static JsonSchema jsonSchema;

    /** Commonly used mocks. */
    @Mock private static ValidationContext mockValidationContext;
    @Mock private static SchemaLocation mockSchemaLocation;
    @Mock private static JsonNodePath mockEvaluationPath;
    @Mock private static JsonNode mockSchemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a JsonSchema instance using the package‑private factory.
        // All constructor arguments are mocked – the concrete behaviour of
        // those collaborators is irrelevant for the overload we are testing.
        jsonSchema = JsonSchema.from(
                mockValidationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,               // no parent schema
                false);             // do not suppress sub‑schema retrieval
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verifies that {@code JsonSchema.validate(JsonNode, OutputFormat, Consumer)}
     * delegates to the {@code validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}
     * overload, invokes the supplied {@link Consumer} with the created
     * {@link ExecutionContext}, and returns the value produced by the delegate.
     */
    @Test
    void validate_WithConsumer_DelegatesAndInvokesConsumer() {
        // Arrange ------------------------------------------------------------
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);
        ExecutionContext fakeExecutionContext = mock(ExecutionContext.class);

        // Spy the original JsonSchema so we can stub the deeper overload.
        JsonSchema spySchema = spy(jsonSchema);

        // Stub the deeper overload. It receives an ExecutionContextCustomizer
        // that we must invoke with our fake ExecutionContext to emulate the
        // real behaviour of the library.
        when(spySchema.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(invocation -> {
                    // Extract the ExecutionContextCustomizer supplied by the method
                    // under test and invoke it with the fake ExecutionContext.
                    ExecutionContextCustomizer customizer = invocation.getArgument(3);
                    customizer.customize(fakeExecutionContext);
                    // Return a dummy result that the outer overload should propagate.
                    return "delegated-result";
                });

        // Act ----------------------------------------------------------------
        String result = spySchema.validate(rootNode, format, consumer);

        // Assert -------------------------------------------------------------
        assertEquals("delegated-result", result,
                "The result returned by the delegate overload should be propagated.");

        // Verify that the consumer was called exactly once with the fake
        // ExecutionContext that we supplied in the stub.
        verify(consumer, times(1)).accept(fakeExecutionContext);
    }
}
```