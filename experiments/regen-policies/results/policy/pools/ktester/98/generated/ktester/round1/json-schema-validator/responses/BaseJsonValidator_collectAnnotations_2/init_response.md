```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tests for {@link BaseJsonValidator#collectAnnotations(ExecutionContext, String)}.
 *
 * The method simply returns {@code true} when annotation collection is enabled
 * in the {@link ExecutionConfig} **and** the supplied keyword passes the
 * {@link ExecutionConfig#getAnnotationCollectionFilter()} predicate.
 *
 * The test creates a concrete subclass of {@link BaseJsonValidator} that
 * provides the minimal abstract implementations required for instantiation.
 * All collaborators are mocked, therefore no private members or external
 * resources are accessed.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_2_Test {

    /* --------------------------------------------------------------------- */
    /*  Mocks required for constructing a BaseJsonValidator concrete class   */
    /* --------------------------------------------------------------------- */
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorTypeCode;
    @Mock ValidationContext validationContext;

    @Mock ExecutionContext executionContext;
    @Mock ExecutionConfig executionConfig;

    /* --------------------------------------------------------------------- */
    /*  Helper – concrete validator used only for testing the protected method */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        private final String keyword;

        TestValidator(String keyword,
                      SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorTypeCode,
                      ValidationContext validationContext) {
            // The super‑constructor used by the public API of BaseJsonValidator
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorTypeCode, validatorTypeCode,
                  validationContext, false);
            this.keyword = keyword;
        }

        /* Minimal implementations of abstract members – they are never used
           by the method under test, so returning simple values is sufficient. */
        @Override
        public String getKeyword() {
            return keyword;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Test lifecycle – nothing special is required for the static hooks    */
    /* --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global initialisation needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // ExecutionContext returns the mocked ExecutionConfig
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        Mockito.reset(executionContext, executionConfig,
                     schemaLocation, evaluationPath, schemaNode,
                     parentSchema, validatorTypeCode, validationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed
    }

    /* --------------------------------------------------------------------- */
    /*  Helper to build the concrete validator used in each test case        */
    /* --------------------------------------------------------------------- */
    private TestValidator createValidator(String keyword) {
        return new TestValidator(
                keyword,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorTypeCode,
                validationContext);
    }

    /* --------------------------------------------------------------------- */
    /*  Test cases                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void whenAnnotationCollectionDisabled_thenCollectAnnotationsReturnsFalse() {
        // arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        // the filter should never be consulted, but we provide a harmless stub
        when(executionConfig.getAnnotationCollectionFilter())
                .thenReturn(s -> true);

        TestValidator validator = createValidator("anyKeyword");

        // act
        boolean result = validator.collectAnnotations(executionContext, "anyKeyword");

        // assert
        assertFalse(result, "collectAnnotations must return false when collection is disabled");
    }

    @Test
    void whenCollectionEnabledAndFilterAcceptsKeyword_thenCollectAnnotationsReturnsTrue() {
        // arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> "allowed".equals(kw);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("allowed");

        // act
        boolean result = validator.collectAnnotations(executionContext, "allowed");

        // assert
        assertTrue(result, "collectAnnotations must return true when enabled and filter accepts the keyword");
    }

    @Test
    void whenCollectionEnabledAndFilterRejectsKeyword_thenCollectAnnotationsReturnsFalse() {
        // arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> "accepted".equals(kw);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("rejected");

        // act
        boolean result = validator.collectAnnotations(executionContext, "rejected");

        // assert
        assertFalse(result, "collectAnnotations must return false when enabled but filter rejects the keyword");
    }
}
```