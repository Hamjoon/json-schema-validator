package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_canShortCircuit_Test {

    @Mock
    private ValidationContext // mock – not used directly in the test
    validationContext;

    @Mock
    private JsonSchema // mock – not used directly in the test
    parentSchema;

    private AnyOfValidator validator;

    /**
     * A concrete subclass that makes the protected method visible for testing.
     */
    private static class TestableAnyOfValidator extends AnyOfValidator {

        TestableAnyOfValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        @Override
        public boolean canShortCircuit(ExecutionContext executionContext) {
            return super.canShortCircuit(executionContext);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the minimal JSON schema node required by the AnyOfValidator constructor:
        // it must be an array (even if empty) because the constructor validates that.
        JsonNodeFactory factory = JsonNodeFactory.instance;
        // empty array is sufficient for the test
        ArrayNode schemaArray = factory.arrayNode();
        // Create dummy SchemaLocation and JsonNodePath – they are not exercised in this test.
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // Instantiate the validator under test.
        validator = new TestableAnyOfValidator(schemaLocation, evaluationPath, schemaArray, parentSchema, validationContext);
    }

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
        assertTrue(result, "Expected canShortCircuit to return true when annotation collection is disabled");
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
        assertFalse(result, "Expected canShortCircuit to return false when annotation collection is enabled");
    }

    @Test
    void throwsNullPointerExceptionWhenExecutionContextIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ((TestableAnyOfValidator) validator).canShortCircuit(null), "Passing null ExecutionContext should throw NPE");
    }

    @Test
    void throwsNullPointerExceptionWhenExecutionConfigIsNull() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getExecutionConfig()).thenReturn(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext), "Null ExecutionConfig should cause NPE");
    }

    @Test
    void propagatesIllegalStateExceptionFromIsAnnotationCollectionEnabled() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ExecutionConfig executionConfig = mock(ExecutionConfig.class);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionConfig.isAnnotationCollectionEnabled()).thenThrow(new IllegalStateException("boom"));
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void propagatesIllegalStateExceptionFromGetExecutionConfig() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getExecutionConfig()).thenThrow(new IllegalStateException("config error"));
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext));
        assertEquals("config error", ex.getMessage());
    }

    @Test
    void propagatesNullPointerExceptionFromGetExecutionConfig() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getExecutionConfig()).thenThrow(new NullPointerException("null config"));
        // Act & Assert
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ((TestableAnyOfValidator) validator).canShortCircuit(executionContext));
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
