```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link OneOfValidator#reportChildErrors(ExecutionContext)}.
 *
 * The method simply returns the negated value of
 * {@code executionContext.getExecutionConfig().isFailFast()}.
 *
 * The test suite covers:
 * <ul>
 *   <li>normal true/false scenarios</li>
 *   <li>null {@link ExecutionContext}</li>
 *   <li>null {@link ExecutionConfig}</li>
 *   <li>exceptions thrown by {@code ExecutionConfig}</li>
 *   <li>side‑effects inside {@code isFailFast()}</li>
 *   <li>spies verifying exact invocation counts</li>
 *   <li>repeated invocations with the same mock</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_reportChildErrors_Test {

    /** Minimal valid schema node – an empty JSON array (required by the constructor). */
    private JsonNode schemaNode;

    /** Mocked collaborators – they are not used by {@code reportChildErrors} but are required for construction. */
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonSchema parentSchema;

    /** Instance under test. */
    private OneOfValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create an empty array node using Jackson's JsonNodeFactory.
        schemaNode = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();

        // The constructor also needs a SchemaLocation and a JsonNodePath.
        // For the purpose of the test simple (non‑null) instances are sufficient.
        SchemaLocation schemaLocation = new SchemaLocation(
                new AbsoluteIri("http://example.com/schema"),
                new JsonNodePath(PathType.JSON_POINTER)
        );
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // Build the validator instance. No exception is expected because the schemaNode is an array.
        validator = new OneOfValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validationContext
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------
     *  Simple true / false scenarios (already covered in the original
     *  test class but kept here for completeness)
     * -------------------------------------------------------------- */

    @Test
    void reportChildErrors_whenFailFastEnabled_returnsFalse() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setFailFast(true); // fail‑fast enabled
        assertFalse(validator.reportChildErrors(ctx),
                "When fail‑fast is enabled the method should return false");
    }

    @Test
    void reportChildErrors_whenFailFastDisabled_returnsTrue() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setFailFast(false); // fail‑fast disabled
        assertTrue(validator.reportChildErrors(ctx),
                "When fail‑fast is disabled the method should return true");
    }

    @Test
    void reportChildErrors_withDefaultExecutionContext_returnsTrue() {
        ExecutionContext ctx = new ExecutionContext(); // defaults to fail‑fast = false
        assertTrue(validator.reportChildErrors(ctx),
                "Default ExecutionContext should have fail‑fast disabled, thus method returns true");
    }

    /* --------------------------------------------------------------
     *  Null handling
     * -------------------------------------------------------------- */

    @Test
    void reportChildErrors_whenExecutionContextIsNull_throwsNullPointerException() {
        ExecutionContext ctx = null;
        assertThrows(NullPointerException.class,
                () -> validator.reportChildErrors(ctx),
                "Passing a null ExecutionContext should throw NullPointerException");
    }

    @Test
    void reportChildErrors_whenExecutionConfigIsNull_throwsNullPointerException() {
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(null); // config is null

        assertThrows(NullPointerException.class,
                () -> validator.reportChildErrors(mockCtx),
                "When ExecutionConfig is null a NullPointerException is expected");
    }

    /* --------------------------------------------------------------
     *  Exceptions bubbling up from ExecutionConfig
     * -------------------------------------------------------------- */

    @Test
    void reportChildErrors_whenIsFailFastThrowsRuntimeException_propagates() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenThrow(new RuntimeException("boom"));
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.reportChildErrors(mockCtx));
        assertEquals("boom", ex.getMessage(),
                "RuntimeException from isFailFast() must propagate unchanged");
    }

    @Test
    void reportChildErrors_whenIsFailFastThrowsAssertionError_propagates() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenThrow(new AssertionError("assert fail"));
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);

        AssertionError err = assertThrows(AssertionError.class,
                () -> validator.reportChildErrors(mockCtx));
        assertEquals("assert fail", err.getMessage(),
                "AssertionError from isFailFast() must propagate unchanged");
    }

    @Test
    void reportChildErrors_whenGetExecutionConfigThrowsIllegalStateException_propagates() {
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenThrow(new IllegalStateException("illegal state"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.reportChildErrors(mockCtx));
        assertEquals("illegal state", ex.getMessage(),
                "IllegalStateException from getExecutionConfig() must propagate");
    }

    /* --------------------------------------------------------------
     *  Side‑effect verification
     * -------------------------------------------------------------- */

    @Test
    void reportChildErrors_whenIsFailFastTrue_hasSideEffectOnce() {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenAnswer(invocation -> {
            counter.incrementAndGet(); // side‑effect
            return true;
        });
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);

        boolean result = validator.reportChildErrors(mockCtx);
        assertFalse(result, "Expected false because isFailFast() returns true");
        assertEquals(1, counter.get(),
                "Side‑effect (counter increment) must happen exactly once");
    }

    @Test
    void reportChildErrors_whenIsFailFastFalse_hasSideEffectOnce() {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenAnswer(invocation -> {
            counter.incrementAndGet(); // side‑effect
            return false;
        });
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);

        boolean result = validator.reportChildErrors(mockCtx);
        assertTrue(result, "Expected true because isFailFast() returns false");
        assertEquals(1, counter.get(),
                "Side‑effect (counter increment) must happen exactly once");
    }

    /* --------------------------------------------------------------
     *  Spy based verification (exact invocation counts)
     * -------------------------------------------------------------- */

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

    /* --------------------------------------------------------------
     *  Repeated calls with the same mock – consistency checks
     * -------------------------------------------------------------- */

    @Test
    void reportChildErrors_multipleCallsWithSameFailFastTrue_consistentResult() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenReturn(true);
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);

        // first call
        assertFalse(validator.reportChildErrors(mockCtx),
                "First invocation should return false");
        // second call
        assertFalse(validator.reportChildErrors(mockCtx),
                "Second invocation should also return false");
    }

    @Test
    void reportChildErrors_multipleCallsWithSameFailFastFalse_consistentResult() {
        ExecutionConfig mockConfig = mock(ExecutionConfig.class);
        when(mockConfig.isFailFast()).thenReturn(false);
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getExecutionConfig()).thenReturn(mockConfig);

        // first call
        assertTrue(validator.reportChildErrors(mockCtx),
                "First invocation should return true");
        // second call
        assertTrue(validator.reportChildErrors(mockCtx),
                "Second invocation should also return true");
    }
}
```