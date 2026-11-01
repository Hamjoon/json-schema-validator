```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link BaseJsonValidator#collectAnnotations(ExecutionContext, String)}.
 *
 * <p>The method returns {@code true} only when
 * <ul>
 *   <li>annotation collection is enabled in the {@link ExecutionConfig}, and</li>
 *   <li>the supplied keyword satisfies the {@link ExecutionConfig#getAnnotationCollectionFilter()} predicate.</li>
 * </ul>
 *
 * <p>All tests use a minimal concrete subclass of {@link BaseJsonValidator}
 * (named {@code TestValidator}) that provides the abstract members required for
 * instantiation.  Dependencies are fully mocked, so no private fields are accessed
 * directly.  When a {@code null} argument is required we pass it explicitly and
 * assert that a {@link NullPointerException} is thrown, mirroring the real
 * implementation's behaviour.</p>
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
            // The protected constructor of BaseJsonValidator (as used by the library)
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorTypeCode, validatorTypeCode,
                  validationContext, false);
            this.keyword = keyword;
        }

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
    /*  Test lifecycle – reset mocks after each test to avoid cross‑test bleed */
    /* --------------------------------------------------------------------- */
    @BeforeEach
    void setUp() {
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(executionContext, executionConfig,
                     schemaLocation, evaluationPath, schemaNode,
                     parentSchema, validatorTypeCode, validationContext);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper to instantiate the concrete validator for a given keyword       */
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
    /*  Branch‑coverage tests (already present in the scaffold)               */
    /* --------------------------------------------------------------------- */
    @Test
    void whenAnnotationCollectionDisabled_thenCollectAnnotationsReturnsFalse() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(s -> true);

        TestValidator validator = createValidator("anyKeyword");

        boolean result = validator.collectAnnotations(executionContext, "anyKeyword");

        assertFalse(result, "collectAnnotations must return false when collection is disabled");
    }

    @Test
    void whenCollectionEnabledAndFilterAcceptsKeyword_thenCollectAnnotationsReturnsTrue() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> "allowed".equals(kw);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("allowed");

        boolean result = validator.collectAnnotations(executionContext, "allowed");

        assertTrue(result, "collectAnnotations must return true when enabled and filter accepts the keyword");
    }

    @Test
    void whenCollectionEnabledAndFilterRejectsKeyword_thenCollectAnnotationsReturnsFalse() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> "accepted".equals(kw);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("rejected");

        boolean result = validator.collectAnnotations(executionContext, "rejected");

        assertFalse(result, "collectAnnotations must return false when enabled but filter rejects the keyword");
    }

    /* --------------------------------------------------------------------- */
    /*  Null‑handling tests                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    void whenExecutionContextIsNull_thenThrowsNullPointerException() {
        TestValidator validator = createValidator("anyKeyword");

        assertThrows(NullPointerException.class,
                () -> validator.collectAnnotations(null, "anyKeyword"),
                "Method should NPE when executionContext is null");
    }

    @Test
    void whenExecutionConfigIsNull_thenThrowsNullPointerException() {
        // executionContext returns null for getExecutionConfig()
        when(executionContext.getExecutionConfig()).thenReturn(null);

        TestValidator validator = createValidator("anyKeyword");

        assertThrows(NullPointerException.class,
                () -> validator.collectAnnotations(executionContext, "anyKeyword"),
                "Method should NPE when executionConfig is null");
    }

    @Test
    void whenAnnotationCollectionFilterIsNull_thenThrowsNullPointerException() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(null);

        TestValidator validator = createValidator("anyKeyword");

        assertThrows(NullPointerException.class,
                () -> validator.collectAnnotations(executionContext, "anyKeyword"),
                "Method should NPE when filter predicate is null");
    }

    /* --------------------------------------------------------------------- */
    /*  Keyword edge‑case tests                                               */
    /* --------------------------------------------------------------------- */
    @Test
    void filterAcceptsNullKeyword_returnsTrue() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        // filter that returns true only for null
        Predicate<String> filter = kw -> kw == null;
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator(null);

        boolean result = validator.collectAnnotations(executionContext, null);

        assertTrue(result, "Filter accepts null keyword, should return true");
    }

    @Test
    void filterRejectsNullKeyword_returnsFalse() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(Objects::nonNull);

        TestValidator validator = createValidator(null);

        boolean result = validator.collectAnnotations(executionContext, null);

        assertFalse(result, "Objects::nonNull rejects null keyword, should return false");
    }

    @Test
    void emptyStringKeywordAcceptedByIsEmptyFilter() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = s -> s.isEmpty();
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("");

        boolean result = validator.collectAnnotations(executionContext, "");

        assertTrue(result, "Empty string should satisfy isEmpty filter");
    }

    @Test
    void emptyStringKeywordRejectedByNonEmptyFilter() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = s -> !s.isEmpty();
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("");

        boolean result = validator.collectAnnotations(executionContext, "");

        assertFalse(result, "Empty string should be rejected by non‑empty filter");
    }

    /* --------------------------------------------------------------------- */
    /*  Specific filter logic tests                                          */
    /* --------------------------------------------------------------------- */
    @Test
    void filterAllowsOnlyLiteralAllowedKeyword_trueResult() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = k -> "allowed".equals(k);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("allowed");

        boolean result = validator.collectAnnotations(executionContext, "allowed");

        assertTrue(result, "Literal \"allowed\" should be accepted by the filter");
    }

    @Test
    void filterAllowsOnlyLiteralAllowedKeyword_falseResult() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = k -> "allowed".equals(k);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("different");

        boolean result = validator.collectAnnotations(executionContext, "different");

        assertFalse(result, "Any keyword other than \"allowed\" must be rejected");
    }

    /* --------------------------------------------------------------------- */
    /*  Predicate‑exception propagation tests                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void filterThrowsIllegalStateException_isPropagated() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> {
            throw new IllegalStateException("filter failure");
        };
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("anyKeyword");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.collectAnnotations(executionContext, "anyKeyword"),
                "Exception from filter must be propagated");
        assertEquals("filter failure", ex.getMessage());
    }

    @Test
    void filterThrowsRuntimeExceptionForSpecificKeyword_isPropagated() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> {
            if ("bad".equals(kw)) {
                throw new RuntimeException("bad keyword");
            }
            return true;
        };
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        TestValidator validator = createValidator("bad");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.collectAnnotations(executionContext, "bad"),
                "RuntimeException from filter must be propagated");
        assertEquals("bad keyword", ex.getMessage());
    }
}
```