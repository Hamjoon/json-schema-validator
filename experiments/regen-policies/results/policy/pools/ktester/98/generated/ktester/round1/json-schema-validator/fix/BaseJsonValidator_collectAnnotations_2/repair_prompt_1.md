
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

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
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorTypeCode, validatorTypeCode, validationContext, false);
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
        // ExecutionContext returns the mocked ExecutionConfig
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        executionConfig = new SimpleExecutionConfig();
        executionContext = new SimpleExecutionContext();
        executionContext.setExecutionConfig(executionConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        Mockito.reset(executionContext, executionConfig, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorTypeCode, validationContext);
        Mockito.reset(executionContext, executionConfig, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorTypeCode, validationContext);
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:11: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:45: error: interface expected here
    private static class SimpleExecutionConfig implements ExecutionConfig {
                                                          ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:73: error: interface expected here
    private static class SimpleExecutionContext implements ExecutionContext {
                                                           ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:18: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:21: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:39: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:51: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:60: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:77: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:146: error: cannot find symbol
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        ^
  symbol:   method when(ExecutionConfig)
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:147: error: cannot find symbol
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        ^
  symbol:   method when(ExecutionConfig)
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:148: error: incompatible types: SimpleExecutionConfig cannot be converted to ExecutionConfig
        executionConfig = new SimpleExecutionConfig();
                          ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:149: error: incompatible types: SimpleExecutionContext cannot be converted to ExecutionContext
        executionContext = new SimpleExecutionContext();
                           ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:156: error: cannot find symbol
        Mockito.reset(executionContext, executionConfig, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorTypeCode, validationContext);
        ^
  symbol:   variable Mockito
  location: class BaseJsonValidator_collectAnnotations_2_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_2_Test.java:157: error: cannot find symbol
        Mockito.reset(executionContext, executionConfig, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorTypeCode, validationContext);
        ^
  symbol:   variable Mockito
  location: class BaseJsonValidator_collectAnnotations_2_Test
22 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method collectAnnotations(ExecutionContext, String)': '\n     * Determine if annotations should be reported.\n     * \n     * @param executionContext the execution context\n     * @param keyword          the keyword\n     * @return true if annotations should be reported\n     ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected boolean collectAnnotations(ExecutionContext, String)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext, String)`, api document: \n     * Determine if annotations should be reported.\n     * \n     * @param executionContext the execution context\n     * @param keyword          the keyword\n     * @return true if annotations should be reported\n     ;\nclass `com.networknt.schema.AnyOfValidator`:\n\trelated functions:\n\t\tmethod `protected boolean canShortCircuit(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext, String)`, api document: \n     * If annotation collection is enabled cannot short circuit.\n     * \n     * @see <a href=\n     *      "https://github.com/json-schema-org/json-schema-spec/blob/f8967bcbc6cee27753046f63024b55336a9b1b54/jsonschema-core.md?plain=1#L1717-L1720">anyOf</a>\n     * @param executionContext the execution context\n     * @return true if can short circuit\n     ;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected boolean reportChildErrors(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext, String)`, api document: \r\n     * Determines if child errors should be reported.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return true if child errors should be reported\r\n     ;\nclass `com.networknt.schema.format.BaseFormatJsonValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isAssertionsEnabled(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext, String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


