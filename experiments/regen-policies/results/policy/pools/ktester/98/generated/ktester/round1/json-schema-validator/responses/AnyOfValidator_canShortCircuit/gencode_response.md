```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AnyOfValidator#canShortCircuit(ExecutionContext)}.
 *
 * <p>The method simply returns the negation of
 * {@code executionContext.getExecutionConfig().isAnnotationCollectionEnabled()}.
 * The test suite therefore covers:
 *
 * <ul>
 *   <li>normal true/false outcomes</li>
 *   <li>null arguments and null sub‑objects causing {@link NullPointerException}</li>
 *   <li>unchecked exceptions bubbling up unchanged</li>
 *   <li>verification that the method invokes its collaborators exactly once</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_canShortCircuit_Test {

    /** A concrete subclass that makes the protected method visible for testing. */
    private static class TestableAnyOfValidator extends AnyOfValidator {
        TestableAnyOfValidator(SchemaLocation schemaLocation,
                               JsonNodePath evaluationPath,
                               JsonNode schemaNode,
                               JsonSchema parentSchema,
                               ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        @Override
        public boolean canShortCircuit(ExecutionContext executionContext) {
            return super.canShortCircuit(executionContext);
        }
    }

    @Mock
    private ValidationContext validationContext;   // not used directly in the tests

    @Mock
    private JsonSchema parentSchema;               // not used directly in the tests

    private AnyOfValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The AnyOfValidator constructor expects a schema node that is an array.
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ArrayNode schemaArray = factory.arrayNode(); // empty array is sufficient

        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        validator = new TestableAnyOfValidator(
                schemaLocation,
                evaluationPath,
                schemaArray,
                parentSchema,
                validationContext);
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
     *  Normal behaviour tests (already covered in the original scaffold)
     * -------------------------------------------------------------- */

    @Test
    void canShortCircuitReturnsTrueWhenAnnotationCollectionDisabled() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ExecutionConfig executionConfig = mock(ExecutionConfig.class);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(false);

        // Act
        boolean result = ((TestableAnyOfValidator) validator).canShortCircuit(executionContext);

        // Assert
        assertTrue(result, "Expected true when annotation collection is disabled");
    }

    @Test
    void canShortCircuitReturnsFalseWhenAnnotationCollectionEnabled() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ExecutionConfig executionConfig = mock(ExecutionConfig.class);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);

        // Act
        boolean result = ((TestableAnyOfValidator) validator).canShortCircuit(executionContext);

        // Assert
        assertFalse(result, "Expected false when annotation collection is enabled");
    }

    /* --------------------------------------------------------------
     *  Edge‑case & exceptional behaviour tests
     * -------------------------------------------------------------- */

    @Test
    void throwsNullPointerExceptionWhenExecutionContextIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> ((TestableAnyOfValidator) validator).canShortCircuit(null),
                "Passing null ExecutionContext should throw NPE");
    }

    @Test
    void throwsNullPointerExceptionWhenExecutionConfigIsNull() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getExecutionConfig()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext),
                "Null ExecutionConfig should cause NPE");
    }

    @Test
    void propagatesIllegalStateExceptionFromIsAnnotationCollectionEnabled() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ExecutionConfig executionConfig = mock(ExecutionConfig.class);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionConfig.isAnnotationCollectionEnabled())
                .thenThrow(new IllegalStateException("boom"));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void propagatesIllegalStateExceptionFromGetExecutionConfig() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getExecutionConfig())
                .thenThrow(new IllegalStateException("config error"));

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext));
        assertEquals("config error", ex.getMessage());
    }

    @Test
    void propagatesNullPointerExceptionFromGetExecutionConfig() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getExecutionConfig())
                .thenThrow(new NullPointerException("null config"));

        // Act & Assert
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext));
        assertEquals("null config", ex.getMessage());
    }

    @Test
    void verifiesSingleInvocationWhenAnnotationCollectionEnabled() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ExecutionConfig executionConfig = mock(ExecutionConfig.class);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);

        // Act
        boolean result = ((TestableAnyOfValidator) validator).canShortCircuit(executionContext);

        // Assert result
        assertFalse(result, "Expected false when annotation collection is enabled");

        // Verify exact interaction count
        verify(executionContext, times(1)).getExecutionConfig();
        verify(executionConfig, times(1)).isAnnotationCollectionEnabled();
        verifyNoMoreInteractions(executionContext, executionConfig);
    }
}
```