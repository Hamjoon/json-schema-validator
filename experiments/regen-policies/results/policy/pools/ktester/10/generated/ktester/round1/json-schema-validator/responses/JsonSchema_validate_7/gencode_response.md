```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, Consumer)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal flow – the {@link ExecutionContext} is created, passed to the
 *       supplied {@link Consumer}, and the result of the delegated {@code validate}
 *       call is returned;</li>
 *   <li>handling of {@code null} arguments;</li>
 *   <li>propagation of exceptions thrown by the customizer or the {@link OutputFormat};</li>
 *   <li>different kinds of formatted results (simple string, complex object,
 *       {@code null}) to prove that the generic return type works correctly.</li>
 * </ul>
 *
 * <p>All collaborators are mocked; the {@link JsonSchema} instance is created as a
 * partial mock (real methods are used unless explicitly stubbed).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    // ----- collaborators required for the JsonSchema constructor -----
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;          // the schema definition (dummy)
    @Mock JsonSchema parentSchema;      // parent schema – not used in these tests

    // ----- objects created per test -----
    JsonSchema jsonSchema;              // class under test (partial mock)
    ExecutionContext executionContext; // the execution context created by the SUT

    @BeforeEach
    void setupBeforeEach() {
        // Create a real (partial) mock of JsonSchema using its package‑private constructor.
        jsonSchema = Mockito.mock(
                JsonSchema.class,
                withSettings()
                        .useConstructor(validationContext, schemaLocation, evaluationPath,
                                schemaNode, parentSchema, false) // suppressSubSchemaRetrieval = false
                        .defaultAnswer(CALLS_REAL_METHODS));

        // Mock the ExecutionContext that the JsonSchema will create.
        executionContext = mock(ExecutionContext.class);
        when(jsonSchema.createExecutionContext()).thenReturn(executionContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all interactions to keep tests independent.
        Mockito.reset(validationContext, schemaLocation, evaluationPath,
                schemaNode, parentSchema, jsonSchema, executionContext);
    }

    /* --------------------------------------------------------------
     *  1. Normal flow – consumer is invoked and the delegated result is returned
     * -------------------------------------------------------------- */
    @Test
    void validate_WithConsumer_ShouldInvokeConsumerAndReturnResult() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any())).thenReturn("formatted-result");

        // consumer that we want to be called exactly once
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> executionCustomizer = mock(Consumer.class);

        // stub the overloaded validate method – it must invoke the
        // ExecutionContextCustomizer (the fourth argument) before returning.
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any()))
                .thenAnswer(invocation -> {
                    Object customizer = invocation.getArgument(3);
                    // The customizer is a functional interface with a method
                    //   void accept(ExecutionContext, ValidationContext)
                    // Use reflection to call it.
                    Method accept = customizer.getClass().getMethod("accept",
                            ExecutionContext.class, ValidationContext.class);
                    accept.invoke(customizer, executionContext, null);
                    return "formatted-result";
                });

        // act
        String result = jsonSchema.validate(rootNode, outputFormat, executionCustomizer);

        // assert
        assertEquals("formatted-result", result,
                "The method should return the value produced by the delegated validate call.");
        verify(executionCustomizer, times(1)).accept(executionContext);
    }

    /* --------------------------------------------------------------
     *  2. Null rootNode – expect NPE
     * -------------------------------------------------------------- */
    @Test
    void validate_NullRootNode_ShouldThrowNullPointerException() {
        // arrange
        @SuppressWarnings("unchecked")
        OutputFormat<String> outputFormat = mock(OutputFormat.class);
        Consumer<ExecutionContext> customizer = mock(Consumer.class);

        // act & assert
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(null, outputFormat, customizer),
                "Passing a null rootNode should result in NullPointerException.");
    }

    /* --------------------------------------------------------------
     *  3. Null format – expect NPE
     * -------------------------------------------------------------- */
    @Test
    void validate_NullFormat_ShouldThrowNullPointerException() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        Consumer<ExecutionContext> customizer = mock(Consumer.class);

        // act & assert
        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(rootNode, null, customizer),
                "Passing a null OutputFormat should result in NullPointerException.");
    }

    /* --------------------------------------------------------------
     *  4. Null consumer – method should behave like a no‑op customizer
     * -------------------------------------------------------------- */
    @Test
    void validate_NullConsumer_ShouldTreatAsNoOpAndReturnResult() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any())).thenReturn("ok");

        // stub the overloaded method – we do **not** invoke the customizer
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any()))
                .thenReturn("ok");

        // act
        String result = jsonSchema.validate(rootNode, outputFormat, null);

        // assert
        assertEquals("ok", result,
                "When the consumer is null the method should still delegate and return the result.");
    }

    /* --------------------------------------------------------------
     *  5. Customizer throws exception – the exception must propagate
     * -------------------------------------------------------------- */
    @Test
    void validate_CustomizerThrowsException_ShouldPropagate() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> outputFormat = mock(OutputFormat.class);
        Consumer<ExecutionContext> failingCustomizer = mock(Consumer.class);
        RuntimeException ex = new RuntimeException("Customizer failure");
        doThrow(ex).when(failingCustomizer).accept(any());

        // stub the overloaded method – it will invoke the customizer
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any()))
                .thenAnswer(invocation -> {
                    Object customizer = invocation.getArgument(3);
                    Method accept = customizer.getClass().getMethod("accept",
                            ExecutionContext.class, ValidationContext.class);
                    // this will trigger the RuntimeException from the mock consumer
                    accept.invoke(customizer, executionContext, null);
                    return null; // never reached
                });

        // act & assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> jsonSchema.validate(rootNode, outputFormat, failingCustomizer),
                "Exception thrown by the customizer must be propagated.");
        assertSame(ex, thrown.getCause() != null ? thrown.getCause() : thrown,
                "The propagated exception should be the original one.");
    }

    /* --------------------------------------------------------------
     *  6. OutputFormat throws exception – the exception must propagate
     * -------------------------------------------------------------- */
    @Test
    void validate_OutputFormatThrowsException_ShouldPropagate() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> throwingFormat = mock(OutputFormat.class);
        when(throwingFormat.apply(any())).thenThrow(new IllegalStateException("Format failure"));
        Consumer<ExecutionContext> customizer = mock(Consumer.class);

        // stub the overloaded method – it simply returns the result of format.apply,
        // which in this case throws.
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(throwingFormat), any()))
                .thenAnswer(invocation -> {
                    // invoke the customizer first (no‑op)
                    Object customizerObj = invocation.getArgument(3);
                    Method accept = customizerObj.getClass().getMethod("accept",
                            ExecutionContext.class, ValidationContext.class);
                    accept.invoke(customizerObj, executionContext, null);
                    // now call the format – this will throw
                    return throwingFormat.apply(Collections.emptySet());
                });

        // act & assert
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> jsonSchema.validate(rootNode, throwingFormat, customizer),
                "Exception thrown by the OutputFormat must be propagated.");
        assertEquals("Format failure", thrown.getMessage());
    }

    /* --------------------------------------------------------------
     *  7. Validation failure – required property missing
     * -------------------------------------------------------------- */
    @Test
    void validate_ValidationFailure_MissingRequiredProperty() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> outputFormat = mock(OutputFormat.class);
        String expectedMessage = "required property 'name' is missing";
        when(outputFormat.apply(any())).thenReturn(expectedMessage);
        Consumer<ExecutionContext> customizer = mock(Consumer.class);

        // stub the overloaded method – it returns the formatted message
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any()))
                .thenAnswer(invocation -> {
                    // invoke the customizer (no‑op for this test)
                    Object cust = invocation.getArgument(3);
                    Method accept = cust.getClass().getMethod("accept",
                            ExecutionContext.class, ValidationContext.class);
                    accept.invoke(cust, executionContext, null);
                    return outputFormat.apply(Collections.emptySet());
                });

        // act
        String result = jsonSchema.validate(rootNode, outputFormat, customizer);

        // assert
        assertEquals(expectedMessage, result,
                "The formatted result should contain the required‑property error message.");
    }

    /* --------------------------------------------------------------
     *  8. Validation failure – type mismatch
     * -------------------------------------------------------------- */
    @Test
    void validate_ValidationFailure_TypeMismatch() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> outputFormat = mock(OutputFormat.class);
        String expectedMessage = "type mismatch: expected string but found integer";
        when(outputFormat.apply(any())).thenReturn(expectedMessage);
        Consumer<ExecutionContext> customizer = mock(Consumer.class);

        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any()))
                .thenAnswer(invocation -> {
                    Object cust = invocation.getArgument(3);
                    Method accept = cust.getClass().getMethod("accept",
                            ExecutionContext.class, ValidationContext.class);
                    accept.invoke(cust, executionContext, null);
                    return outputFormat.apply(Collections.emptySet());
                });

        // act
        String result = jsonSchema.validate(rootNode, outputFormat, customizer);

        // assert
        assertEquals(expectedMessage, result,
                "The formatted result should contain the type‑mismatch error message.");
    }

    /* --------------------------------------------------------------
     *  9. Custom OutputFormat returning a complex object (List)
     * -------------------------------------------------------------- */
    @Test
    void validate_CustomOutputFormatReturningComplexObject() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<List<ValidationMessage>> listFormat = mock(OutputFormat.class);
        List<ValidationMessage> emptyList = Collections.emptyList();
        when(listFormat.apply(any())).thenReturn(emptyList);
        Consumer<ExecutionContext> customizer = mock(Consumer.class);

        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(listFormat), any()))
                .thenAnswer(invocation -> {
                    Object cust = invocation.getArgument(3);
                    Method accept = cust.getClass().getMethod("accept",
                            ExecutionContext.class, ValidationContext.class);
                    accept.invoke(cust, executionContext, null);
                    return listFormat.apply(Collections.emptySet());
                });

        // act
        List<ValidationMessage> result = jsonSchema.validate(rootNode, listFormat, customizer);

        // assert
        assertNotNull(result, "Result should not be null.");
        assertTrue(result.isEmpty(), "For a valid instance the list should be empty.");
    }

    /* --------------------------------------------------------------
     * 10. Custom OutputFormat that deliberately returns null
     * -------------------------------------------------------------- */
    @Test
    void validate_CustomOutputFormatReturningNull() {
        // arrange
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> nullFormat = mock(OutputFormat.class);
        when(nullFormat.apply(any())).thenReturn(null);
        Consumer<ExecutionContext> customizer = mock(Consumer.class);

        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(nullFormat), any()))
                .thenAnswer(invocation -> {
                    Object cust = invocation.getArgument(3);
                    Method accept = cust.getClass().getMethod("accept",
                            ExecutionContext.class, ValidationContext.class);
                    accept.invoke(cust, executionContext, null);
                    return nullFormat.apply(Collections.emptySet());
                });

        // act
        String result = jsonSchema.validate(rootNode, nullFormat, customizer);

        // assert
        assertNull(result, "The method should propagate a null result from the OutputFormat.");
    }
}
```