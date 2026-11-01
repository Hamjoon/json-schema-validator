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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    // ----- collaborators required for the JsonSchema constructor -----
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    // the schema definition (can be a dummy node)
    @Mock
    JsonNode schemaNode;

    // parent schema – not used in this test
    @Mock
    JsonSchema parentSchema;

    // the class under test (partial mock)
    JsonSchema jsonSchema;

    // the execution context that will be returned
    ExecutionContext executionContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real (partial) mock of JsonSchema using its package‑private constructor.
        jsonSchema = Mockito.mock(JsonSchema.class, withSettings().useConstructor(validationContext, schemaLocation, evaluationPath, schemaNode, parentSchema, // `suppressSubSchemaRetrieval = false`
        false).defaultAnswer(CALLS_REAL_METHODS));
        // Mock the ExecutionContext that the JsonSchema will create.
        executionContext = mock(ExecutionContext.class);
        when(jsonSchema.createExecutionContext()).thenReturn(executionContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to avoid cross‑test contamination.
        Mockito.reset(validationContext, schemaLocation, evaluationPath, schemaNode, parentSchema, jsonSchema, executionContext);
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any())).thenReturn("formatted-result");
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
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(null, outputFormat, customizer), "Passing a null rootNode should result in NullPointerException.");
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
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(rootNode, null, customizer), "Passing a null OutputFormat should result in NullPointerException.");
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any())).thenReturn("ok");
        // act
        String result = jsonSchema.validate(rootNode, outputFormat, null);
        // assert
        assertEquals("ok", result, "When the consumer is null the method should still delegate and return the result.");
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any())).thenAnswer(invocation -> {
            Object customizer = invocation.getArgument(3);
            Method accept = customizer.getClass().getMethod("accept", ExecutionContext.class, ValidationContext.class);
            // this will trigger the RuntimeException from the mock consumer
            accept.invoke(customizer, executionContext, null);
            // never reached
            return null;
        });
        // act & assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> jsonSchema.validate(rootNode, outputFormat, failingCustomizer), "Exception thrown by the customizer must be propagated.");
        assertSame(ex, thrown.getCause() != null ? thrown.getCause() : thrown, "The propagated exception should be the original one.");
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(throwingFormat), any())).thenAnswer(invocation -> {
            // invoke the customizer first (no‑op)
            Object customizerObj = invocation.getArgument(3);
            Method accept = customizerObj.getClass().getMethod("accept", ExecutionContext.class, ValidationContext.class);
            accept.invoke(customizerObj, executionContext, null);
            // now call the format – this will throw
            return throwingFormat.apply(Collections.emptySet());
        });
        // act & assert
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> jsonSchema.validate(rootNode, throwingFormat, customizer), "Exception thrown by the OutputFormat must be propagated.");
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any())).thenAnswer(invocation -> {
            // invoke the customizer (no‑op for this test)
            Object cust = invocation.getArgument(3);
            Method accept = cust.getClass().getMethod("accept", ExecutionContext.class, ValidationContext.class);
            accept.invoke(cust, executionContext, null);
            return outputFormat.apply(Collections.emptySet());
        });
        // act
        String result = jsonSchema.validate(rootNode, outputFormat, customizer);
        // assert
        assertEquals(expectedMessage, result, "The formatted result should contain the required‑property error message.");
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(outputFormat), any())).thenAnswer(invocation -> {
            Object cust = invocation.getArgument(3);
            Method accept = cust.getClass().getMethod("accept", ExecutionContext.class, ValidationContext.class);
            accept.invoke(cust, executionContext, null);
            return outputFormat.apply(Collections.emptySet());
        });
        // act
        String result = jsonSchema.validate(rootNode, outputFormat, customizer);
        // assert
        assertEquals(expectedMessage, result, "The formatted result should contain the type‑mismatch error message.");
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(listFormat), any())).thenAnswer(invocation -> {
            Object cust = invocation.getArgument(3);
            Method accept = cust.getClass().getMethod("accept", ExecutionContext.class, ValidationContext.class);
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
        when(jsonSchema.validate(eq(executionContext), eq(rootNode), eq(nullFormat), any())).thenAnswer(invocation -> {
            Object cust = invocation.getArgument(3);
            Method accept = cust.getClass().getMethod("accept", ExecutionContext.class, ValidationContext.class);
            accept.invoke(cust, executionContext, null);
            return nullFormat.apply(Collections.emptySet());
        });
        // act
        String result = jsonSchema.validate(rootNode, nullFormat, customizer);
        // assert
        assertNull(result, "The method should propagate a null result from the OutputFormat.");
    }
}
