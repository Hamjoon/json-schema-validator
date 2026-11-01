```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional tests for {@link AnyOfValidator#canShortCircuit(ExecutionContext)}.
 *
 * <p>The existing tests cover the two normal outcomes (annotation collection enabled/disabled).
 * This class adds edge‑case verification:
 * <ul>
 *   <li>Passing {@code null} as the {@link ExecutionContext}.</li>
 *   <li>{@link ExecutionContext#getExecutionConfig()} returning {@code null}.</li>
 *   <li>{@link ExecutionConfig#isAnnotationCollectionEnabled()} throwing an exception.</li>
 *   <li>Ensuring the result is stable across multiple invocations.</li>
 *   <li>Propagation of exceptions thrown by {@code getExecutionConfig()}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_canShortCircuit_Test {

    /** Instance under test. */
    private AnyOfValidator anyOfValidator;

    /** Mocks required for the constructor of {@link AnyOfValidator}. */
    @Mock
    private SchemaLocation mockSchemaLocation;
    @Mock
    private JsonNodePath mockEvaluationPath;
    @Mock
    private JsonSchema mockParentSchema;
    @Mock
    private ValidationContext mockValidationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The validator expects the schema node to be an array.
        // An empty array is sufficient – it avoids any calls to
        // ValidationContext#newSchema().
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyArrayNode = mapper.readTree("[]");

        anyOfValidator = new AnyOfValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                emptyArrayNode,
                mockParentSchema,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* --------------------------------------------------------------
     * Existing normal‑case tests (kept unchanged for completeness)
     * -------------------------------------------------------------- */

    @Test
    void canShortCircuit_returnsTrueWhenAnnotationCollectionDisabled() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Act
        boolean result = anyOfValidator.canShortCircuit(execContext);

        // Assert
        assertTrue(result, "Short‑circuit should be allowed when annotation collection is disabled");
    }

    @Test
    void canShortCircuit_returnsFalseWhenAnnotationCollectionEnabled() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Act
        boolean result = anyOfValidator.canShortCircuit(execContext);

        // Assert
        assertFalse(result, "Short‑circuit should NOT be allowed when annotation collection is enabled");
    }

    /* --------------------------------------------------------------
     * Edge‑case tests derived from the supplied JSON test matrix
     * -------------------------------------------------------------- */

    @Test
    void canShortCircuit_throwsNullPointerExceptionWhenExecutionContextIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> anyOfValidator.canShortCircuit(null),
                "Passing a null ExecutionContext should throw NullPointerException");
    }

    @Test
    void canShortCircuit_throwsNullPointerExceptionWhenExecutionConfigIsNull() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> anyOfValidator.canShortCircuit(execContext),
                "A null ExecutionConfig should cause NullPointerException");
    }

    @Test
    void canShortCircuit_propagatesRuntimeExceptionFromIsAnnotationCollectionEnabled() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isAnnotationCollectionEnabled()).thenThrow(new RuntimeException("boom"));
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> anyOfValidator.canShortCircuit(execContext),
                "Exception from isAnnotationCollectionEnabled should be propagated");
        assertEquals("boom", thrown.getMessage());
    }

    @Test
    void canShortCircuit_multipleCallsWithAnnotationEnabledTrue_stableResult() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        ExecutionConfig execConfig = spy(ExecutionConfig.class);
        // Use a spy to verify the method is called multiple times.
        when(execConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Act – call the method twice
        boolean first = anyOfValidator.canShortCircuit(execContext);
        boolean second = anyOfValidator.canShortCircuit(execContext);

        // Assert
        assertFalse(first, "First call should return false when annotation collection is enabled");
        assertFalse(second, "Second call should return false as well");
        verify(execConfig, times(2)).isAnnotationCollectionEnabled();
    }

    @Test
    void canShortCircuit_multipleCallsWithAnnotationEnabledFalse_stableResult() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        ExecutionConfig execConfig = spy(ExecutionConfig.class);
        when(execConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Act – call the method twice
        boolean first = anyOfValidator.canShortCircuit(execContext);
        boolean second = anyOfValidator.canShortCircuit(execContext);

        // Assert
        assertTrue(first, "First call should return true when annotation collection is disabled");
        assertTrue(second, "Second call should return true as well");
        verify(execConfig, times(2)).isAnnotationCollectionEnabled();
    }

    @Test
    void canShortCircuit_propagatesRuntimeExceptionFromGetExecutionConfig() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenThrow(new RuntimeException("config failure"));

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> anyOfValidator.canShortCircuit(execContext),
                "RuntimeException from getExecutionConfig should be propagated");
        assertEquals("config failure", thrown.getMessage());
    }

    @Test
    void canShortCircuit_propagatesNullPointerExceptionFromGetExecutionConfig() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        when(execContext.getExecutionConfig()).thenThrow(new NullPointerException("null config"));

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> anyOfValidator.canShortCircuit(execContext),
                "NullPointerException from getExecutionConfig should be propagated");
        assertEquals("null config", thrown.getMessage());
    }
}
```