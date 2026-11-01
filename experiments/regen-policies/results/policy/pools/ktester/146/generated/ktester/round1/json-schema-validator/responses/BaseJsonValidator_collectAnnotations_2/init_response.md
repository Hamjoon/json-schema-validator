```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#collectAnnotations(ExecutionContext, String)}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * subclass is created solely for exercising the protected {@code collectAnnotations}
 * method. All collaborators are mocked so that the test focuses on the logic
 * inside the method under test.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_2_Test {

    // ----- Mocks for the constructor dependencies -----
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorTypeCode;
    @Mock ValidationContext validationContext;

    // ----- Mocks for the method parameters -----
    @Mock ExecutionContext executionContext;
    @Mock ExecutionConfig executionConfig;

    // The concrete validator under test
    TestValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the execution context to return our mocked execution config
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);

        // Create a concrete instance of BaseJsonValidator
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorTypeCode,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up – Mockito handles mock lifecycle.
    }

    /*** Test cases ***/

    @Test
    void shouldReturnFalseWhenAnnotationCollectionDisabled() {
        // Arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        // The filter should never be consulted, but we stub it anyway.
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(keyword -> true);

        // Act
        boolean result = validator.callCollectAnnotations(executionContext, "anyKeyword");

        // Assert
        assertFalse(result, "collectAnnotations must return false when collection is disabled");
        // Verify that the filter was not invoked
        verify(executionConfig, never()).getAnnotationCollectionFilter();
    }

    @Test
    void shouldReturnFalseWhenFilterRejectsKeyword() {
        // Arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        // Filter returns false for the supplied keyword
        Predicate<String> filter = kw -> false;
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        // Act
        boolean result = validator.callCollectAnnotations(executionContext, "rejectedKeyword");

        // Assert
        assertFalse(result, "collectAnnotations must return false when filter rejects the keyword");
        verify(executionConfig).getAnnotationCollectionFilter();
    }

    @Test
    void shouldReturnTrueWhenEnabledAndFilterAcceptsKeyword() {
        // Arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        // Filter returns true only for the expected keyword
        Predicate<String> filter = kw -> "acceptedKeyword".equals(kw);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        // Act
        boolean result = validator.callCollectAnnotations(executionContext, "acceptedKeyword");

        // Assert
        assertTrue(result, "collectAnnotations must return true when enabled and filter accepts the keyword");
        verify(executionConfig).getAnnotationCollectionFilter();
    }

    /* --------------------------------------------------------------------- */
    /* Helper concrete class – provides only the abstract validate method. */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  validatorType, validatorType, validationContext, false);
        }

        // Expose the protected method for testing
        boolean callCollectAnnotations(ExecutionContext ec, String keyword) {
            return collectAnnotations(ec, keyword);
        }

        // Minimal implementation – not used in these tests.
        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return java.util.Collections.emptySet();
        }
    }
}
```