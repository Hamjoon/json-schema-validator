```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator#reportChildErrors(ExecutionContext)}.
 *
 * The method simply returns the negation of {@code executionContext.getExecutionConfig().isFailFast()}.
 * The test suite covers:
 * <ul>
 *   <li>normal behaviour for both true/false fail‑fast flag</li>
 *   <li>null arguments (ExecutionContext or ExecutionConfig)</li>
 *   <li>exception propagation from {@code getExecutionConfig()} and {@code isFailFast()}</li>
 *   <li>verification that {@code isFailFast()} is invoked exactly once</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_reportChildErrors_Test {

    @Mock
    private ValidationContext validationContext;   // used only during construction

    private OneOfValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- Prepare constructor arguments -------------------------------------------------
        // SchemaLocation and JsonNodePath are only used for bookkeeping inside the constructor.
        // They can be simple Mockito mocks because the constructor does not dereference them
        // beyond passing them to ValidationContext#newSchema (which we also mock).
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);

        // The parent schema is not used in the logic we are testing, so a mock is sufficient.
        JsonSchema parentSchema = mock(JsonSchema.class);

        // The validator expects an *array* schema node.  An empty array is enough for construction.
        JsonNode schemaNode = JsonNodeFactory.instance.arrayNode();

        // Stub ValidationContext#newSchema to avoid NPEs when the constructor iterates over the
        // (empty) array.  It will never be called for an empty array, but we stub it defensively.
        when(validationContext.newSchema(any(SchemaLocation.class),
                any(JsonNodePath.class),
                any(JsonNode.class),
                any(JsonSchema.class)))
                .thenReturn(mock(JsonSchema.class));

        // ----- Create the validator -----------------------------------------------------------
        validator = new OneOfValidator(schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    // -------------------------------------------------------------------------
    // Normal behaviour
    // -------------------------------------------------------------------------

    @Test
    void reportChildErrors_returnsFalseWhenFailFastIsTrue() {
        // Mock ExecutionConfig to report fail‑fast = true
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isFailFast()).thenReturn(true);

        // Mock ExecutionContext to return the above config
        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Invoke the protected method (accessible because the test class is in the same package)
        boolean result = validator.reportChildErrors(execContext);

        assertFalse(result, "When failFast is true, reportChildErrors should return false");
    }

    @Test
    void reportChildErrors_returnsTrueWhenFailFastIsFalse() {
        // Mock ExecutionConfig to report fail‑fast = false
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isFailFast()).thenReturn(false);

        // Mock ExecutionContext to return the above config
        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Invoke the method
        boolean result = validator.reportChildErrors(execContext);

        assertTrue(result, "When failFast is false, reportChildErrors should return true");
    }

    // -------------------------------------------------------------------------
    // Null / exceptional arguments
    // -------------------------------------------------------------------------

    @Test
    void reportChildErrors_throwsNPE_whenExecutionContextIsNull() {
        assertThrows(NullPointerException.class,
                () -> validator.reportChildErrors(null),
                "Passing a null ExecutionContext should result in NullPointerException");
    }

    @Test
    void reportChildErrors_throwsNPE_whenExecutionConfigIsNull() {
        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenReturn(null); // simulate missing config

        assertThrows(NullPointerException.class,
                () -> validator.reportChildErrors(execContext),
                "If ExecutionConfig is null, a NullPointerException is expected");
    }

    @Test
    void reportChildErrors_propagatesException_fromGetExecutionConfig() {
        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig())
                .thenThrow(new IllegalArgumentException("invalid config"));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> validator.reportChildErrors(execContext));
        assertEquals("invalid config", thrown.getMessage(),
                "Exception message should be propagated unchanged");
    }

    @Test
    void reportChildErrors_propagatesRuntimeException_fromIsFailFast() {
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isFailFast()).thenThrow(new RuntimeException("boom"));

        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> validator.reportChildErrors(execContext));
        assertEquals("boom", thrown.getMessage(),
                "RuntimeException from isFailFast() must propagate with original message");
    }

    @Test
    void reportChildErrors_propagatesAssertionError_fromIsFailFast() {
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isFailFast()).thenThrow(new AssertionError("assertion failed"));

        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        AssertionError thrown = assertThrows(AssertionError.class,
                () -> validator.reportChildErrors(execContext));
        assertEquals("assertion failed", thrown.getMessage(),
                "AssertionError from isFailFast() must propagate with original message");
    }

    // -------------------------------------------------------------------------
    // Interaction verification
    // -------------------------------------------------------------------------

    @Test
    void reportChildErrors_callsIsFailFastExactlyOnce_andUsesFirstResult() {
        // Configure isFailFast() to return true on first call, false on second.
        // The validator should call it only once, therefore the result must be !true == false.
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isFailFast()).thenReturn(true, false);

        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        boolean result = validator.reportChildErrors(execContext);

        assertFalse(result, "Result should be the negation of the first isFailFast() call (true → false)");
        // Verify that isFailFast() was invoked exactly once
        verify(execConfig, times(1)).isFailFast();
    }
}
```