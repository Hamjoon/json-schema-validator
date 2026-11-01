package com.networknt.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;

import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Test class for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /**
     * Mocks used across test methods
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    // may be null – we use a mock for safety
    @Mock
    JsonSchema parentSchema;

    @Mock
    ExecutionContext executionContext;

    @Mock
    OutputFormat<Object> outputFormat;

    @Mock
    ExecutionContextCustomizer executionCustomizer;

    /**
     * The JsonSchema instance under test (a spy so we can stub the internal validate call).
     */
    JsonSchema schemaSpy;

    /**
     * A deterministic set of validation messages returned by the stubbed internal validation.
     */
    Set<ValidationMessage> dummyMessages = Collections.emptySet();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* ------------------------------------------------------------------
         * Create a real JsonSchema instance via its private constructor.
         * ------------------------------------------------------------------ */
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonSchema realInstance = ctor.newInstance(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                false);
        /* ------------------------------------------------------------------
         * Wrap it in a Mockito spy so we can stub the internal validate(...) call.
         * ------------------------------------------------------------------ */
        schemaSpy = Mockito.spy(realInstance);
        /* ------------------------------------------------------------------
         * By default, the internal validation returns an empty set.
         * Individual tests override this behaviour as needed.
         * ------------------------------------------------------------------ */
        doReturn(dummyMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
    }

    @Test
    void validate_withCustomizer_callsAllCollaborators_andReturnsFormattedResult() {
        // arrange – stub the OutputFormat behaviour
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("formatted-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert – the result comes from the OutputFormat
        assertEquals("formatted-result", result);
        // verify orchestration
        verify(outputFormat, times(1)).customize(executionContext, validationContext);
        verify(executionCustomizer, times(1)).customize(executionContext, validationContext);
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
    }

    @Test
    void validate_withoutCustomizer_skipsCustomizer_andStillFormatsResult() {
        // arrange – null customizer, stub OutputFormat
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn(42);
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals(42, result);
        // verify that the customizer was NOT invoked
        verifyNoInteractions(executionCustomizer);
        // other interactions remain the same
        verify(outputFormat, times(1)).customize(executionContext, validationContext);
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
    }

    /* ----------------------------------------------------------------------
     * 1. Normal flow – no customizer
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – normal flow (no customizer, no exception)")
    void validate_normalFlow_noCustomizer() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("formatted-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("formatted-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        verify(outputFormat).format(schemaSpy, dummyMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 2. Normal flow – with ExecutionContextCustomizer
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – with ExecutionContextCustomizer")
    void validate_normalFlow_withCustomizer() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        doNothing().when(executionCustomizer).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn(123);
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert
        assertEquals(123, result);
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
        inOrder.verify(outputFormat).customize(executionContext, validationContext);
        inOrder.verify(executionCustomizer).customize(executionContext, validationContext);
        inOrder.verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        inOrder.verify(outputFormat).format(schemaSpy, dummyMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 3. FailFastAssertionException handling – normal customizer absent
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – FailFastAssertionException handling (no customizer)")
    void validate_failFast_noCustomizer() {
        // arrange
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> failFastMessages = Collections.singleton(vm);
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
        when(ffa.getValidationMessages()).thenReturn(failFastMessages);
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("ff-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("ff-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verify(outputFormat).format(schemaSpy, failFastMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 4. FailFastAssertionException handling – with customizer
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – FailFastAssertionException handling (with customizer)")
    void validate_failFast_withCustomizer() {
        // arrange
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> failFastMessages = Collections.singleton(vm);
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
        when(ffa.getValidationMessages()).thenReturn(failFastMessages);
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        doNothing().when(executionCustomizer).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("ff-with-customizer");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert
        assertEquals("ff-with-customizer", result);
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
        inOrder.verify(outputFormat).customize(executionContext, validationContext);
        inOrder.verify(executionCustomizer).customize(executionContext, validationContext);
        inOrder.verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        inOrder.verify(outputFormat).format(schemaSpy, failFastMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 5. FailFastAssertionException with null messages
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – FailFastAssertionException with null messages")
    void validate_failFast_nullMessages() {
        // arrange
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
        when(ffa.getValidationMessages()).thenReturn(null);
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), isNull(), eq(executionContext), eq(validationContext)))
                .thenReturn("null-msg-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("null-msg-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(outputFormat).format(schemaSpy, null, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 6. Null ExecutionContext – should propagate NPE from format.customize
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – null ExecutionContext")
    void validate_nullExecutionContext() {
        // arrange: make format.customize throw NPE when executionContext is null
        doThrow(new NullPointerException("executionContext is null"))
                .when(outputFormat).customize(null, validationContext);
        // act & assert
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(null, mock(JsonNode.class), outputFormat, null));
        assertEquals("executionContext is null", ex.getMessage());
        // ensure no further interactions
        verify(outputFormat).customize(null, validationContext);
        verifyNoMoreInteractions(outputFormat, executionCustomizer, schemaSpy);
    }

    /* ----------------------------------------------------------------------
     * 7. Null OutputFormat – NPE should be thrown before any interaction
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – null OutputFormat")
    void validate_nullOutputFormat() {
        // act & assert
        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), null, null));
    }

    /* ----------------------------------------------------------------------
     * 8. OutputFormat.customize throws RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – OutputFormat.customize throws RuntimeException")
    void validate_outputFormatCustomizeThrows() {
        // arrange
        RuntimeException rte = new RuntimeException("customize failed");
        doThrow(rte).when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verifyNoMoreInteractions(outputFormat);
    }

    /* ----------------------------------------------------------------------
     * 9. OutputFormat.format throws RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – OutputFormat.format throws RuntimeException")
    void validate_outputFormatFormatThrows() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        RuntimeException rte = new RuntimeException("format failed");
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenThrow(rte);
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(outputFormat).format(schemaSpy, dummyMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 10. ExecutionContextCustomizer throws RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – ExecutionContextCustomizer throws RuntimeException")
    void validate_customizerThrows() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        RuntimeException rte = new RuntimeException("customizer failed");
        doThrow(rte).when(executionCustomizer).customize(executionContext, validationContext);
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(executionCustomizer).customize(executionContext, validationContext);
        // internal validation and format must NOT be called
        verify(schemaSpy, never()).validate(any(), any());
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 11. Both customizer and format.customize throw – customizer should not run
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – both customizer and format.customize throw")
    void validate_bothThrow_formatFirst() {
        // arrange
        RuntimeException rte = new RuntimeException("format customize failed");
        doThrow(rte).when(outputFormat).customize(executionContext, validationContext);
        // customizer is a mock; we verify it is never invoked
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verify(schemaSpy, never()).validate(any(), any());
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 12. Internal validate returns null
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – internal validate returns null")
    void validate_internalValidateReturnsNull() {
        // arrange
        doReturn(null).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), isNull(), eq(executionContext), eq(validationContext)))
                .thenReturn("null-set-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("null-set-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(outputFormat).format(schemaSpy, null, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 13. Null JsonNode – internal validation should receive null and we simulate NPE
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – null JsonNode")
    void validate_nullJsonNode() {
        // arrange: internal validate throws NPE when node is null
        doThrow(new NullPointerException("node is null")).when(schemaSpy).validate(executionContext, isNull());
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(executionContext, null, outputFormat, null));
        assertEquals("node is null", ex.getMessage());
        verify(outputFormat).customize(executionContext, validationContext);
        // format.format must never be called
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 14. Internal validate throws generic RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – internal validate throws RuntimeException")
    void validate_internalValidateThrowsRuntime() {
        // arrange
        IllegalStateException ise = new IllegalStateException("boom");
        doThrow(ise).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(ise, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 15. Internal validate throws Error (e.g., AssertionError)
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – internal validate throws Error")
    void validate_internalValidateThrowsError() {
        // arrange
        AssertionError error = new AssertionError("assertion failed");
        doThrow(error).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        AssertionError thrown = assertThrows(AssertionError.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(error, thrown);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    @Test
    void placeholder() {
        // This test does nothing but guarantees that the test class compiles.
    }
}