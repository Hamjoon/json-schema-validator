package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_reportChildErrors_Test {

    /**
     * Minimal valid schema node – an empty JSON array (required by the constructor).
     */
    private JsonNode schemaNode;

    /**
     * Mocked collaborators – they are not used by {@code reportChildErrors} but are required for construction.
     */
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonSchema parentSchema;

    /**
     * Instance under test.
     */
    private OneOfValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // Create an empty array node using Jackson's JsonNodeFactory.
        schemaNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        // The constructor also needs a SchemaLocation and a JsonNodePath.
        // For the purpose of the test simple (non‑null) instances are sufficient.
        SchemaLocation schemaLocation = new SchemaLocation(// any absolute IRI
        new AbsoluteIri("http://example.com/schema"), // root path
        new JsonNodePath(PathType.JSON_POINTER));
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // Build the validator instance. No exception is expected because the schemaNode is an array.
        validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
    }

    @Test
    void reportChildErrors_whenFailFastEnabled_returnsFalse() {
        // Arrange: ExecutionContext with fail‑fast set to true.
        ExecutionContext ctx = new ExecutionContext();
        // the setter exists (used in the validator code)
        ctx.setFailFast(true);
        // Act
        boolean result = validator.reportChildErrors(ctx);
        // Assert
        assertFalse(result, "When fail‑fast is enabled the method should return false");
    }

    @Test
    void reportChildErrors_whenFailFastDisabled_returnsTrue() {
        // Arrange: ExecutionContext with fail‑fast set to false.
        ExecutionContext ctx = new ExecutionContext();
        ctx.setFailFast(false);
        // Act
        boolean result = validator.reportChildErrors(ctx);
        // Assert
        assertTrue(result, "When fail‑fast is disabled the method should return true");
    }

    @Test
    void reportChildErrors_withDefaultExecutionContext_returnsTrue() {
        // Arrange: default ExecutionContext (its ExecutionConfig defaults to fail‑fast = false).
        ExecutionContext ctx = new ExecutionContext();
        // Act
        boolean result = validator.reportChildErrors(ctx);
        // Assert
        assertTrue(result, "Default ExecutionContext should have fail‑fast disabled, thus method returns true");
    }

    @Test
    void reportChildErrors_whenExecutionContextIsNull_throwsNullPointerException() {
        ExecutionContext ctx = null;
        assertThrows(NullPointerException.class, () -> validator.reportChildErrors(ctx), "Passing a null ExecutionContext should throw NullPointerException");
    }

    @Test
    void reportChildErrors_whenExecutionConfigIsNull_throwsNullPointerException() {
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        // config is null
        when(mockCtx.getExecutionConfig()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> validator.reportChildErrors(mockCtx), "When ExecutionConfig is null a NullPointerException is expected");
    }

    @Test
    void reportChildErrors_whenIsFailFastThrowsRuntimeException_propagates() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenThrow(new RuntimeException("boom"));
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.reportChildErrors(mockCtx));
        assertEquals("boom", ex.getMessage(), "RuntimeException from isFailFast() must propagate unchanged");
    }

    @Test
    void reportChildErrors_whenIsFailFastThrowsAssertionError_propagates() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenThrow(new AssertionError("assert fail"));
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);
        AssertionError err = assertThrows(AssertionError.class, () -> validator.reportChildErrors(mockCtx));
        assertEquals("assert fail", err.getMessage(), "AssertionError from isFailFast() must propagate unchanged");
    }

    @Test
    void reportChildErrors_whenGetExecutionConfigThrowsIllegalStateException_propagates() {
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenThrow(new IllegalStateException("illegal state"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.reportChildErrors(mockCtx));
        assertEquals("illegal state", ex.getMessage(), "IllegalStateException from getExecutionConfig() must propagate");
    }

    @Test
    void reportChildErrors_whenIsFailFastTrue_hasSideEffectOnce() {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenAnswer(invocation -> {
            // side‑effect
            counter.incrementAndGet();
            return true;
        });
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);
        boolean result = validator.reportChildErrors(mockCtx);
        assertFalse(result, "Expected false because isFailFast() returns true");
        assertEquals(1, counter.get(), "Side‑effect (counter increment) must happen exactly once");
    }

    @Test
    void reportChildErrors_whenIsFailFastFalse_hasSideEffectOnce() {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenAnswer(invocation -> {
            // side‑effect
            counter.incrementAndGet();
            return false;
        });
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);
        boolean result = validator.reportChildErrors(mockCtx);
        assertTrue(result, "Expected true because isFailFast() returns false");
        assertEquals(1, counter.get(), "Side‑effect (counter increment) must happen exactly once");
    }

    @Test
    void reportChildErrors_spyWhenFailFastTrue_verifiesCalls() {
        // Real objects (assuming default constructors are available)
        ExecutionConfig realConfig = new ExecutionConfig();
        realConfig.setFailFast(true);
        ExecutionConfig spyConfig = spy(realConfig);
        ExecutionContext realCtx = new ExecutionContext();
        realCtx.setExecutionConfig(spyConfig);
        ExecutionContext spyCtx = spy(realCtx);
        boolean result = validator.reportChildErrors(spyCtx);
        assertFalse(result, "When isFailFast() returns true the method should return false");
        // Verify each method is called exactly once
        verify(spyCtx, times(1)).getExecutionConfig();
        verify(spyConfig, times(1)).isFailFast();
    }

    @Test
    void reportChildErrors_spyWhenFailFastFalse_verifiesCalls() {
        ExecutionConfig realConfig = new ExecutionConfig();
        realConfig.setFailFast(false);
        ExecutionConfig spyConfig = spy(realConfig);
        ExecutionContext realCtx = new ExecutionContext();
        realCtx.setExecutionConfig(spyConfig);
        ExecutionContext spyCtx = spy(realCtx);
        boolean result = validator.reportChildErrors(spyCtx);
        assertTrue(result, "When isFailFast() returns false the method should return true");
        verify(spyCtx, times(1)).getExecutionConfig();
        verify(spyConfig, times(1)).isFailFast();
    }

    @Test
    void reportChildErrors_multipleCallsWithSameFailFastTrue_consistentResult() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenReturn(true);
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);
        // first call
        assertFalse(validator.reportChildErrors(mockCtx), "First invocation should return false");
        // second call
        assertFalse(validator.reportChildErrors(mockCtx), "Second invocation should also return false");
    }

    @Test
    void reportChildErrors_multipleCallsWithSameFailFastFalse_consistentResult() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenReturn(false);
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);
        // first call
        assertTrue(validator.reportChildErrors(mockCtx), "First invocation should return true");
        // second call
        assertTrue(validator.reportChildErrors(mockCtx), "Second invocation should also return true");
    }
}
