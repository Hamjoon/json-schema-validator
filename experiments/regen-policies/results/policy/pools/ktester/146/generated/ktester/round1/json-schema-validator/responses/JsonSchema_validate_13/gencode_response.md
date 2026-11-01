```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * <p>The tests verify the orchestration performed by the method:
 * <ul>
 *   <li>customisation of the {@link ExecutionContext} by {@link OutputFormat}</li>
 *   <li>optional invocation of an {@link ExecutionContextCustomizer}</li>
 *   <li>handling of normal validation results, {@link FailFastAssertionException},
 *       and exceptional situations.</li>
 * </ul>
 *
 * <p>All heavy‑weight collaborators (schema loading, actual validation logic, etc.) are mocked,
 * allowing the tests to run quickly and deterministically.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /** Spy of a real {@link JsonSchema} – we stub the internal {@code validate(ExecutionContext, JsonNode)} method. */
    private JsonSchema schemaSpy;

    /** Mocks used by the test methods. */
    @Mock private ExecutionContext executionContext;
    @Mock private JsonNode jsonNode;
    @Mock private OutputFormat<String> outputFormat;
    @Mock private ExecutionContextCustomizer executionCustomizer;

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal real JsonSchema (empty schema is sufficient) and spy it.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema realSchema = factory.getSchema("{}");
        schemaSpy = Mockito.spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        Mockito.reset(executionContext, jsonNode, outputFormat, executionCustomizer, schemaSpy);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Customisation and normal validation flow (already covered)      */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_withCustomizer_callsAllCollaborators() {
        Set<ValidationMessage> validationMessages = Collections.emptySet();
        doReturn(validationMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        when(outputFormat.format(any(JsonSchema.class), eq(validationMessages), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn("formatted-result");

        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, executionCustomizer);

        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(executionCustomizer, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(validationMessages), eq(executionContext),
                any(ValidationContext.class));
        assertEquals("formatted-result", result);
    }

    @Test
    void validate_withoutCustomizer_skipsCustomizer() {
        Set<ValidationMessage> validationMessages = Collections.emptySet();
        doReturn(validationMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        when(outputFormat.format(any(JsonSchema.class), eq(validationMessages), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn("no-customizer-result");

        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, null);

        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verifyNoInteractions(executionCustomizer);
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(validationMessages), eq(executionContext),
                any(ValidationContext.class));
        assertEquals("no-customizer-result", result);
    }

    /* --------------------------------------------------------------------- */
    /* 2. FailFastAssertionException handling                               */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_whenFailFastException_isHandledAndFormatted() {
        // Simulate a validation that aborts via FailFastAssertionException.
        Set<ValidationMessage> messages = Collections.singleton(new ValidationMessage.Builder()
                .code("error")
                .path("$")
                .message("fail fast")
                .build());

        FailFastAssertionException ffExc = new FailFastAssertionException(messages);
        doThrow(ffExc).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        when(outputFormat.format(any(JsonSchema.class), eq(messages), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn("ff-formatted");

        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, null);

        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(messages), eq(executionContext),
                any(ValidationContext.class));
        assertEquals("ff-formatted", result);
    }

    @Test
    void validate_withCustomizer_andFailFastException() {
        Set<ValidationMessage> messages = Collections.singleton(new ValidationMessage.Builder()
                .code("ff")
                .path("$")
                .message("fast")
                .build());

        FailFastAssertionException ffExc = new FailFastAssertionException(messages);
        doThrow(ffExc).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        when(outputFormat.format(any(JsonSchema.class), eq(messages), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn("ff-with-customizer");

        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, executionCustomizer);

        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(executionCustomizer, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(messages), eq(executionContext),
                any(ValidationContext.class));
        assertEquals("ff-with-customizer", result);
    }

    /* --------------------------------------------------------------------- */
    /* 3. Null parameter handling                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_nullExecutionContext_throwsNPEFromCustomize() {
        // outputFormat.customize should throw NPE when executionContext is null.
        doThrow(new NullPointerException("executionContext is null"))
                .when(outputFormat).customize(isNull(), any());

        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(null, jsonNode, outputFormat, null));
        // No further interactions expected.
        verify(outputFormat, times(1)).customize(isNull(), any());
        verifyNoMoreInteractions(outputFormat);
    }

    @Test
    void validate_nullOutputFormat_throwsNPEImmediately() {
        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(executionContext, jsonNode, null, null));
        // No interaction with executionContext or customizer should have occurred.
        verifyNoInteractions(outputFormat, executionCustomizer);
    }

    @Test
    void validate_nullJsonNode_throwsNPEFromInternalValidate() {
        // Stub internal validate to throw NPE when node is null.
        doThrow(new NullPointerException("node is null"))
                .when(schemaSpy).validate(any(ExecutionContext.class), isNull());

        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(executionContext, null, outputFormat, null));

        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verifyNoInteractions(executionCustomizer);
    }

    /* --------------------------------------------------------------------- */
    /* 4. Empty validation result                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_emptyResult_returnsEmptyCollectionFromFormat() {
        Set<ValidationMessage> emptySet = Collections.emptySet();
        doReturn(emptySet).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        // OutputFormat returns an empty List.
        when(outputFormat.format(any(JsonSchema.class), eq(emptySet), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn(Collections.emptyList());

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) schemaSpy.validate(executionContext, jsonNode, outputFormat, null);

        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(emptySet), eq(executionContext),
                any(ValidationContext.class));
        assertTrue(result.isEmpty(), "Result should be an empty collection");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Customizer exception propagation                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_customizerThrows_propagatesException() {
        doThrow(new RuntimeException("customizer failure"))
                .when(executionCustomizer).customize(any(ExecutionContext.class), any(ValidationContext.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, executionCustomizer));

        assertEquals("customizer failure", ex.getMessage());
        // format.format must never be called.
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* --------------------------------------------------------------------- */
    /* 6. OutputFormat exception propagation                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_outputFormatCustomizeThrows_propagatesException() {
        doThrow(new RuntimeException("customize failure"))
                .when(outputFormat).customize(any(ExecutionContext.class), any(ValidationContext.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));

        assertEquals("customize failure", ex.getMessage());
        // No validation or format.format should be invoked.
        verify(schemaSpy, never()).validate(any(), any());
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    @Test
    void validate_outputFormatFormatThrows_propagatesException() {
        Set<ValidationMessage> messages = Collections.emptySet();
        doReturn(messages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        doThrow(new RuntimeException("format failure"))
                .when(outputFormat).format(any(JsonSchema.class), eq(messages), any(ExecutionContext.class),
                        any(ValidationContext.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));

        assertEquals("format failure", ex.getMessage());
        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(messages), eq(executionContext),
                any(ValidationContext.class));
    }

    /* --------------------------------------------------------------------- */
    /* 7. Null ValidationMessage set handling                               */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_internalValidateReturnsNull_setIsPassedAsNull() {
        // Simulate internal validation returning null.
        doReturn(null).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        when(outputFormat.format(any(JsonSchema.class), isNull(), any(ExecutionContext.class),
                any(ValidationContext.class))).thenReturn("null-set-result");

        String result = schemaSpy.validate(executionContext, jsonNode, outputFormat, null);

        verify(outputFormat, times(1)).customize(eq(executionContext), any(ValidationContext.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), isNull(), eq(executionContext),
                any(ValidationContext.class));
        assertEquals("null-set-result", result);
    }

    /* --------------------------------------------------------------------- */
    /* 8. Null ValidationContext handling (field is null via reflection)  */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_nullValidationContext_throwsNPEFromCustomize() throws Exception {
        // Force the protected field validationContext in BaseJsonValidator to null.
        Field vcField = BaseJsonValidator.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(schemaSpy, null);

        // The OutputFormat should throw NPE when the ValidationContext argument is null.
        doThrow(new NullPointerException("validationContext is null"))
                .when(outputFormat).customize(any(), isNull());

        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));

        verify(outputFormat, times(1)).customize(eq(executionContext), isNull());
        // No further calls should happen.
        verifyNoMoreInteractions(outputFormat);
    }

    /* --------------------------------------------------------------------- */
    /* 9. Null ExecutionContext after successful customise                  */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_nullExecutionContextAfterCustomize_throwsNPEFromValidate() {
        // outputFormat.customize tolerates null executionContext (do nothing).
        // Internal validate should throw NPE when executionContext is null.
        doThrow(new NullPointerException("executionContext is null"))
                .when(schemaSpy).validate(isNull(), any(JsonNode.class));

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(null, jsonNode, outputFormat, null));

        assertEquals("executionContext is null", ex.getMessage());
        verify(outputFormat, times(1)).customize(isNull(), any(ValidationContext.class));
        verify(schemaSpy, times(1)).validate(isNull(), any(JsonNode.class));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Internal validation runtime exception propagation                  */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_internalValidateThrowsRuntimeException_propagates() {
        doThrow(new IllegalStateException("internal failure"))
                .when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schemaSpy.validate(executionContext, jsonNode, outputFormat, null));

        assertEquals("internal failure", ex.getMessage());
        // format.format must not be invoked.
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }
}
```