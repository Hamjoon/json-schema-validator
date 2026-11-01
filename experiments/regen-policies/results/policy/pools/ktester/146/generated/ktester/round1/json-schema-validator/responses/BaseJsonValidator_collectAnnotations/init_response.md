```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

/**
 * Tests for the {@link BaseJsonValidator#collectAnnotations(ExecutionContext)} method.
 *
 * The method under test simply delegates to the overloaded
 * {@code collectAnnotations(ExecutionContext, String)} passing the validator's keyword.
 * Therefore the tests focus on verifying that delegation works correctly and that the
 * returned boolean value is propagated.
 *
 * A minimal concrete subclass of {@link BaseJsonValidator} is created because the class
 * is abstract. All required constructor arguments are supplied as Mockito mocks (or
 * simple real objects where construction is cheap). The protected method
 * {@code collectAnnotations(ExecutionContext, String)} is stubbed on a spy to control
 * its return value.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_Test {

    // -----------------------------------------------------------------------
    // Shared test fixtures
    // -----------------------------------------------------------------------
    @Mock
    SchemaLocation schemaLocation;
    @Mock
    JsonNodePath evaluationPath;
    @Mock
    JsonNode schemaNode;
    @Mock
    JsonSchema parentSchema;
    @Mock
    ValidatorTypeCode validatorType;
    @Mock
    ValidationContext validationContext;

    // a real ExecutionContext – it has a no‑arg constructor and does not require
    // any external resources for the purpose of these tests
    ExecutionContext executionContext;

    // -----------------------------------------------------------------------
    // Lifecycle methods
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these unit tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // initialise a fresh ExecutionContext before each test
        executionContext = new ExecutionContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    // -----------------------------------------------------------------------
    // Helper concrete validator
    // -----------------------------------------------------------------------
    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     * Only the abstract {@code getKeyword()} and {@code validate(...)} methods are
     * implemented; the rest of the behaviour is inherited from the base class.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Validation is irrelevant for the collectAnnotations tests
            return Collections.emptySet();
        }
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------
    @Test
    void collectAnnotations_delegatesAndReturnsTrue() {
        // Arrange – create a spy so we can stub the protected overload
        TestValidator realValidator = new TestValidator(
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);
        BaseJsonValidator spyValidator = Mockito.spy(realValidator);

        // Stub the protected method to return true regardless of the ExecutionContext
        doReturn(true)
                .when(spyValidator)
                .collectAnnotations(any(ExecutionContext.class), eq("testKeyword"));

        // Act
        boolean result = spyValidator.collectAnnotations(executionContext);

        // Assert
        assertTrue(result, "collectAnnotations should return true when the protected overload returns true");
        // Verify that the overload was called exactly once with the expected arguments
        verify(spyValidator, times(1))
                .collectAnnotations(eq(executionContext), eq("testKeyword"));
    }

    @Test
    void collectAnnotations_delegatesAndReturnsFalse() {
        // Arrange
        TestValidator realValidator = new TestValidator(
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);
        BaseJsonValidator spyValidator = Mockito.spy(realValidator);

        // Stub the protected method to return false
        doReturn(false)
                .when(spyValidator)
                .collectAnnotations(any(ExecutionContext.class), eq("testKeyword"));

        // Act
        boolean result = spyValidator.collectAnnotations(executionContext);

        // Assert
        assertFalse(result, "collectAnnotations should return false when the protected overload returns false");
        verify(spyValidator, times(1))
                .collectAnnotations(eq(executionContext), eq("testKeyword"));
    }
}
```