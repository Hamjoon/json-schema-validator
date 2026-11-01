```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests for {@link BaseJsonValidator#collectAnnotations(ExecutionContext)}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * implementation is provided inside the test class. The concrete implementation
 * overrides the {@code collectAnnotations(ExecutionContext, String)} method so that
 * we can control its return value and verify that the protected
 * {@code collectAnnotations(ExecutionContext)} method correctly delegates to it.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_Test {

    /** Mocks required for the BaseJsonValidator constructor. */
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;
    @Mock ValidationContext validationContext;

    /** The concrete validator used in the tests. */
    private TestValidator validator;

    /** Simple concrete subclass of {@link BaseJsonValidator}. */
    private static class TestValidator extends BaseJsonValidator {

        /** The value that the overridden {@code collectAnnotations(..., String)} should return. */
        private final boolean returnValue;

        TestValidator(boolean returnValue,
                      SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
            this.returnValue = returnValue;
        }

        /** Expose the protected method for the test. */
        boolean invokeCollectAnnotations(ExecutionContext executionContext) {
            return super.collectAnnotations(executionContext);
        }

        /** The keyword associated with this validator (used only for delegating). */
        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        /** Delegated implementation – simply returns the preset {@code returnValue}. */
        @Override
        protected boolean collectAnnotations(ExecutionContext executionContext, String keyword) {
            // In a real validator the keyword would influence the behaviour.
            // For the purpose of this test we ignore it and return the preset value.
            return returnValue;
        }

        // The remaining abstract methods are either defaulted in the hierarchy
        // or not required for this test, so we keep the default implementations.
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a validator that will return true from collectAnnotations.
        validator = new TestValidator(true,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
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
    void collectAnnotations_shouldDelegateToOverriddenMethod_andReturnTrue() {
        ExecutionContext ctx = new ExecutionContext(); // real instance – no special config needed
        boolean result = validator.invokeCollectAnnotations(ctx);
        assertTrue(result, "collectAnnotations should return true when the overridden method returns true");
    }

    @Test
    void collectAnnotations_shouldDelegateToOverriddenMethod_andReturnFalse() {
        // Create a validator that returns false.
        TestValidator falseValidator = new TestValidator(false,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);

        ExecutionContext ctx = new ExecutionContext();
        boolean result = falseValidator.invokeCollectAnnotations(ctx);
        assertFalse(result, "collectAnnotations should return false when the overridden method returns false");
    }

    @Test
    void collectAnnotations_shouldHandleNullExecutionContext() {
        // The implementation does not use the ExecutionContext, so null should be safe.
        ExecutionContext nullCtx = null;
        boolean result = validator.invokeCollectAnnotations(nullCtx);
        assertTrue(result, "collectAnnotations should work with a null ExecutionContext when the overridden method returns true");
    }
}
```