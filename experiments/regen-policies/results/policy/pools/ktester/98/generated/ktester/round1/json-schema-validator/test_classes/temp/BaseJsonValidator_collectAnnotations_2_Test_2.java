package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_2_Test {

    /* --------------------------------------------------------------------- */
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorTypeCode;

    @Mock
    ValidationContext validationContext;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionConfig executionConfig;

    /**
     * Simple stub for {@link ExecutionConfig}.
     */
    private static class SimpleExecutionConfig implements ExecutionConfig {

        private boolean annotationCollectionEnabled;

        private Predicate<String> annotationCollectionFilter;

        @Override
        public boolean isAnnotationCollectionEnabled() {
            return annotationCollectionEnabled;
        }

        void setAnnotationCollectionEnabled(boolean enabled) {
            this.annotationCollectionEnabled = enabled;
        }

        @Override
        public Predicate<String> getAnnotationCollectionFilter() {
            return annotationCollectionFilter;
        }

        void setAnnotationCollectionFilter(Predicate<String> filter) {
            this.annotationCollectionFilter = filter;
        }
    }

    /**
     * Simple stub for {@link ExecutionContext}.
     */
    private static class SimpleExecutionContext implements ExecutionContext {

        private ExecutionConfig executionConfig;

        @Override
        public ExecutionConfig getExecutionConfig() {
            return executionConfig;
        }

        void setExecutionConfig(ExecutionConfig config) {
            this.executionConfig = config;
        }
    }

    /**
     * Minimal concrete validator used to exercise the protected method.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final String keyword;

        TestValidator(String keyword, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorTypeCode, ValidationContext validationContext) {
            // The super‑constructor used by the public API
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorTypeCode, validationContext);
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

        // The abstract signature in the current library version includes the
        // root node and the path at which validation occurs.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
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

    @BeforeEach
    void setupBeforeEach() {
        executionConfig = new SimpleExecutionConfig();
        executionContext = new SimpleExecutionContext();
        executionContext.setExecutionConfig(executionConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        executionConfig = null;
        executionContext = null;
    }

    /* --------------------------------------------------------------------- */
    private TestValidator createValidator(String keyword) {
        // All other arguments are not used by the method under test, so nulls are fine.
        return new TestValidator(keyword, null, null, null, null, null, null);
    }

    @Test
    void whenAnnotationCollectionDisabled_thenCollectAnnotationsReturnsFalse() {
        executionConfig.setAnnotationCollectionEnabled(false);
        // harmless stub
        executionConfig.setAnnotationCollectionFilter(s -> true);
        TestValidator validator = createValidator("anyKeyword");
        boolean result = validator.collectAnnotations(executionContext, "anyKeyword");
        assertFalse(result, "collectAnnotations must return false when collection is disabled");
    }

    @Test
    void whenCollectionEnabledAndFilterAcceptsKeyword_thenCollectAnnotationsReturnsTrue() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = kw -> "allowed".equals(kw);
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("allowed");
        boolean result = validator.collectAnnotations(executionContext, "allowed");
        assertTrue(result, "collectAnnotations must return true when enabled and filter accepts the keyword");
    }

    @Test
    void whenCollectionEnabledAndFilterRejectsKeyword_thenCollectAnnotationsReturnsFalse() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = kw -> "accepted".equals(kw);
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("rejected");
        boolean result = validator.collectAnnotations(executionContext, "rejected");
        assertFalse(result, "collectAnnotations must return false when enabled but filter rejects the keyword");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void whenExecutionContextIsNull_thenThrowsNullPointerException() {
        TestValidator validator = createValidator("anyKeyword");
        assertThrows(NullPointerException.class, () -> validator.collectAnnotations(null, "anyKeyword"), "Method should NPE when executionContext is null");
    }

    @Test
    void whenExecutionConfigIsNull_thenThrowsNullPointerException() {
        executionContext.setExecutionConfig(null);
        TestValidator validator = createValidator("anyKeyword");
        assertThrows(NullPointerException.class, () -> validator.collectAnnotations(executionContext, "anyKeyword"), "Method should NPE when executionConfig is null");
    }

    @Test
    void whenAnnotationCollectionFilterIsNull_thenThrowsNullPointerException() {
        executionConfig.setAnnotationCollectionEnabled(true);
        executionConfig.setAnnotationCollectionFilter(null);
        TestValidator validator = createValidator("anyKeyword");
        assertThrows(NullPointerException.class, () -> validator.collectAnnotations(executionContext, "anyKeyword"), "Method should NPE when filter predicate is null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void filterAcceptsNullKeyword_returnsTrue() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = kw -> kw == null;
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator(null);
        boolean result = validator.collectAnnotations(executionContext, null);
        assertTrue(result, "Filter accepts null keyword, should return true");
    }

    @Test
    void filterRejectsNullKeyword_returnsFalse() {
        executionConfig.setAnnotationCollectionEnabled(true);
        executionConfig.setAnnotationCollectionFilter(Objects::nonNull);
        TestValidator validator = createValidator(null);
        boolean result = validator.collectAnnotations(executionContext, null);
        assertFalse(result, "Objects::nonNull rejects null keyword, should return false");
    }

    @Test
    void emptyStringKeywordAcceptedByIsEmptyFilter() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = s -> s.isEmpty();
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("");
        boolean result = validator.collectAnnotations(executionContext, "");
        assertTrue(result, "Empty string should satisfy isEmpty filter");
    }

    @Test
    void emptyStringKeywordRejectedByNonEmptyFilter() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = s -> !s.isEmpty();
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("");
        boolean result = validator.collectAnnotations(executionContext, "");
        assertFalse(result, "Empty string should be rejected by non‑empty filter");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void filterAllowsOnlyLiteralAllowedKeyword_trueResult() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = k -> "allowed".equals(k);
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("allowed");
        boolean result = validator.collectAnnotations(executionContext, "allowed");
        assertTrue(result, "Literal \"allowed\" should be accepted by the filter");
    }

    @Test
    void filterAllowsOnlyLiteralAllowedKeyword_falseResult() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = k -> "allowed".equals(k);
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("different");
        boolean result = validator.collectAnnotations(executionContext, "different");
        assertFalse(result, "Any keyword other than \"allowed\" must be rejected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void filterThrowsIllegalStateException_isPropagated() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = kw -> {
            throw new IllegalStateException("filter failure");
        };
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("anyKeyword");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.collectAnnotations(executionContext, "anyKeyword"), "Exception from filter must be propagated");
        assertEquals("filter failure", ex.getMessage());
    }

    @Test
    void filterThrowsRuntimeExceptionForSpecificKeyword_isPropagated() {
        executionConfig.setAnnotationCollectionEnabled(true);
        Predicate<String> filter = kw -> {
            if ("bad".equals(kw)) {
                throw new RuntimeException("bad keyword");
            }
            return true;
        };
        executionConfig.setAnnotationCollectionFilter(filter);
        TestValidator validator = createValidator("bad");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.collectAnnotations(executionContext, "bad"), "RuntimeException from filter must be propagated");
        assertEquals("bad keyword", ex.getMessage());
    }
}
