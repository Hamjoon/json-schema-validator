package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import java.lang.reflect.Field;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /**
     * The schema instance under test – a spy so we can stub the internal validation.
     */
    private JsonSchema schemaSpy;

    /**
     * Mocks used by the test methods.
     */
    @Mock
    private ExecutionContext executionContext;

    @Mock
    private JsonNode jsonNode;

    @Mock
    private OutputFormat<String> outputFormat;

    @Mock
    private ExecutionContextCustomizer executionCustomizer;

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal real JsonSchema using the factory (empty schema is sufficient).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // an empty object schema
        JsonSchema realSchema = factory.getSchema("{}");
        // Spy the real instance so we can stub the internal validate(ExecutionContext, JsonNode) method.
        schemaSpy = Mockito.spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        Mockito.reset(executionContext, jsonNode, outputFormat, executionCustomizer, schemaSpy);
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
        when(outputFormat.format(any(JsonSchema.class), eq(validationMessages), any(ExecutionContext.class), any(ValidationContext.class))).thenReturn("formatted-result");
        // ----- WHEN ----------------------------------------------------------
        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, executionCustomizer);
        // ----- THEN ----------------------------------------------------------
        // Verify that the format was asked to customise the execution context.
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        // Verify that the customizer was invoked.
        verify(executionCustomizer, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        // Verify that the format.format method received the expected arguments.
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(validationMessages), eq(executionContext), any(ValidationContext.class));
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
        when(outputFormat.format(any(JsonSchema.class), eq(validationMessages), any(ExecutionContext.class), any(ValidationContext.class))).thenReturn("no-customizer-result");
        // ----- WHEN ----------------------------------------------------------
        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, null);
        // ----- THEN ----------------------------------------------------------
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        // The customizer must never be called.
        verifyNoInteractions(executionCustomizer);
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(validationMessages), eq(executionContext), any(ValidationContext.class));
        assertEquals("no-customizer-result", result);
    }

    @Test
    void validate_whenFailFastException_isHandledAndFormatted() {
        // Simulate a validation that aborts via FailFastAssertionException.
        Set<ValidationMessage> messages = Collections.singleton(new ValidationMessage.Builder().code("error").path("$").message("fail fast").build());
        FailFastAssertionException ffExc = new FailFastAssertionException(messages);
        doThrow(ffExc).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
        when(outputFormat.format(any(JsonSchema.class), eq(messages), any(ExecutionContext.class), any(ValidationContext.class))).thenReturn("ff-formatted");
        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, null);
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(messages), eq(executionContext), any(ValidationContext.class));
        assertEquals("ff-formatted", result);
    }

    @Test
    void validate_withCustomizer_andFailFastException() {
        Set<ValidationMessage> messages = Collections.singleton(new ValidationMessage.Builder().code("ff").path("$").message("fast").build());
        FailFastAssertionException ffExc = new FailFastAssertionException(messages);
        doThrow(ffExc).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
        when(outputFormat.format(any(JsonSchema.class), eq(messages), any(ExecutionContext.class), any(ValidationContext.class))).thenReturn("ff-with-customizer");
        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, executionCustomizer);
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(executionCustomizer, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(messages), eq(executionContext), any(ValidationContext.class));
        assertEquals("ff-with-customizer", result);
    }

    @Test
    void validate_nullExecutionContext_throwsNPEFromCustomize() {
        // outputFormat.customize should throw NPE when executionContext is null.
        doThrow(new NullPointerException("executionContext is null")).when(outputFormat).customize(isNull(), any());
        assertThrows(NullPointerException.class, () -> schemaSpy.validate(null, jsonNode, outputFormat, null));
        // No further interactions expected.
        verify(outputFormat, times(1)).customize(isNull(), any());
        verifyNoMoreInteractions(outputFormat);
    }

    @Test
    void validate_nullOutputFormat_throwsNPEImmediately() {
        assertThrows(NullPointerException.class, () -> schemaSpy.validate(executionContext, jsonNode, null, null));
        // No interaction with executionContext or customizer should have occurred.
        verifyNoInteractions(outputFormat, executionCustomizer);
    }

    @Test
    void validate_nullJsonNode_throwsNPEFromInternalValidate() {
        // Stub internal validate to throw NPE when node is null.
        doThrow(new NullPointerException("node is null")).when(schemaSpy).validate(any(ExecutionContext.class), isNull());
        assertThrows(NullPointerException.class, () -> schemaSpy.validate(executionContext, null, outputFormat, null));
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verifyNoInteractions(executionCustomizer);
    }

    @Test
    void validate_emptyResult_returnsEmptyCollectionFromFormat() {
        Set<ValidationMessage> emptySet = Collections.emptySet();
        doReturn(emptySet).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
        // OutputFormat returns an empty List.
        when(outputFormat.format(any(JsonSchema.class), eq(emptySet), any(ExecutionContext.class), any(ValidationContext.class))).thenReturn(Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) schemaSpy.validate(executionContext, jsonNode, outputFormat, null);
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(emptySet), eq(executionContext), any(ValidationContext.class));
        assertTrue(result.isEmpty(), "Result should be an empty collection");
    }

    @Test
    void validate_customizerThrows_propagatesException() {
        doThrow(new RuntimeException("customizer failure")).when(executionCustomizer).customize(any(ExecutionContext.class), any(ValidationContext.class));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, executionCustomizer));
        assertEquals("customizer failure", ex.getMessage());
        // format.format must never be called.
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    @Test
    void validate_outputFormatCustomizeThrows_propagatesException() {
        doThrow(new RuntimeException("customize failure")).when(outputFormat).customize(any(ExecutionContext.class), any(ValidationContext.class));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));
        assertEquals("customize failure", ex.getMessage());
        // No validation or format.format should be invoked.
        verify(schemaSpy, never()).validate(any(), any());
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    @Test
    void validate_outputFormatFormatThrows_propagatesException() {
        Set<ValidationMessage> messages = Collections.emptySet();
        doReturn(messages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
        doThrow(new RuntimeException("format failure")).when(outputFormat).format(any(JsonSchema.class), eq(messages), any(ExecutionContext.class), any(ValidationContext.class));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));
        assertEquals("format failure", ex.getMessage());
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(messages), eq(executionContext), any(ValidationContext.class));
    }

    @Test
    void validate_internalValidateReturnsNull_setIsPassedAsNull() {
        // Simulate internal validation returning null.
        doReturn(null).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
        when(outputFormat.format(any(JsonSchema.class), isNull(), any(ExecutionContext.class), any(ValidationContext.class))).thenReturn("null-set-result");
        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, null);
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), isNull(), eq(executionContext), any(ValidationContext.class));
        assertEquals("null-set-result", result);
    }

    @Test
    void validate_nullValidationContext_throwsNPEFromCustomize() throws Exception {
        // Force the protected field validationContext in BaseJsonValidator to null.
        Field vcField = BaseJsonValidator.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(schemaSpy, null);
        // The OutputFormat should throw NPE when the ValidationContext argument is null.
        doThrow(new NullPointerException("validationContext is null")).when(outputFormat).customize(any(), isNull());
        assertThrows(NullPointerException.class, () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));
        verify(outputFormat, times(1)).customize(eq(executionContext), isNull());
        // No further calls should happen.
        verifyNoMoreInteractions(outputFormat);
    }

    @Test
    void validate_nullExecutionContextAfterCustomize_throwsNPEFromValidate() {
        // outputFormat.customize tolerates null executionContext (do nothing).
        // Internal validate should throw NPE when executionContext is null.
        doThrow(new NullPointerException("executionContext is null")).when(schemaSpy).validate(isNull(), any(JsonNode.class));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schemaSpy.validate(null, jsonNode, outputFormat, null));
        assertEquals("executionContext is null", ex.getMessage());
        verify(outputFormat, times(1)).customize(isNull(), any(ValidationContext.class));
        verify(schemaSpy, times(1)).validate(isNull(), any(JsonNode.class));
    }

    @Test
    void validate_internalValidateThrowsRuntimeException_propagates() {
        doThrow(new IllegalStateException("internal failure")).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));
        assertEquals("internal failure", ex.getMessage());
        // format.format must not be invoked.
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }
}
