```java
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

/**
 * Tests for {@link AnyOfValidator#canShortCircuit(ExecutionContext)}.
 *
 * The method simply returns the negation of
 * {@code executionContext.getExecutionConfig().isAnnotationCollectionEnabled()}.
 * We therefore verify the two possible outcomes by mocking the
 * {@link ExecutionContext} and its {@link ExecutionConfig}.
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
    private ValidationContext validationContext;   // mock – not used directly in the test
    @Mock
    private JsonSchema parentSchema;               // mock – not used directly in the test

    private AnyOfValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the minimal JSON schema node required by the AnyOfValidator constructor:
        // it must be an array (even if empty) because the constructor validates that.
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ArrayNode schemaArray = factory.arrayNode(); // empty array is sufficient for the test

        // Create dummy SchemaLocation and JsonNodePath – they are not exercised in this test.
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // Instantiate the validator under test.
        validator = new TestableAnyOfValidator(schemaLocation,
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
}
```