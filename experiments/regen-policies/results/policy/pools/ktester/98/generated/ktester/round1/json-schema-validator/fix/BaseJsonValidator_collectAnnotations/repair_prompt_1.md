
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_Test {

    /**
     * Mocks required for the BaseJsonValidator constructor.
     */
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

    /**
     * The concrete validator used in the tests.
     */
    private TestValidator validator;

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} that lets the test
     * control:
     * <ul>
     *   <li>the keyword returned by {@link #getKeyword()},</li>
     *   <li>the boolean result (or exception) of the overloaded
     *       {@code collectAnnotations(ExecutionContext, String)} method,</li>
     *   <li>optional exceptions thrown by {@code getKeyword()}.</li>
     * </ul>
     */
    private static class TestValidator extends BaseJsonValidator {

        /**
         * Value that the overload should return (ignored if {@code overloadException} is set).
         */
        private final Boolean overloadResult;

        /**
         * Keyword to be returned by {@code getKeyword()} – may be {@code null} or empty.
         */
        private final String keyword;

        /**
         * Exception to be thrown by the overload (if non‑null).
         */
        private final RuntimeException overloadException;

        /**
         * Exception to be thrown by {@code getKeyword()} (if non‑null).
         */
        private final RuntimeException keywordException;

        /* ----------------------------------------------------------------- */
        /*  Constructors used by the various test cases                        */
        /* ----------------------------------------------------------------- */
        /**
         * Constructor used by the first two tests – keyword defaults to a non‑null value.
         */
        TestValidator(Boolean overloadResult, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            this(overloadResult, "default", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * Full constructor used by the remaining tests.
         */
        TestValidator(Boolean overloadResult, String keyword, RuntimeException overloadException, RuntimeException keywordException, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.overloadResult = overloadResult;
            this.keyword = keyword;
            this.overloadException = overloadException;
            this.keywordException = keywordException;
        }

        /**
         * Expose the protected method for the tests.
         */
        boolean invokeCollectAnnotations(ExecutionContext executionContext) {
            return super.collectAnnotations(executionContext);
        }

        @Override
        public String getKeyword() {
            if (keywordException != null) {
                throw keywordException;
            }
            return keyword;
        }

        @Override
        protected boolean collectAnnotations(ExecutionContext executionContext, String keyword) {
            if (overloadException != null) {
                throw overloadException;
            }
            // Return the preset value (default to false if null)
            return overloadResult != null ? overloadResult : false;
        }
        // Other abstract methods from JsonValidator are not needed for these tests.
    }

    /**
     * Helper subclass that allows us to control the value returned by {@code isCollectAnnotationsEnabled()}.
     */
    private static class SimpleExecutionContext extends ExecutionContext {

        private final boolean enabled;

        private final RuntimeException toThrow;

        SimpleExecutionContext(boolean enabled) {
            this.enabled = enabled;
            this.toThrow = null;
        }

        SimpleExecutionContext(RuntimeException toThrow) {
            this.enabled = false;
            this.toThrow = toThrow;
        }

        @Override
        public boolean isCollectAnnotationsEnabled() {
            if (toThrow != null) {
                throw toThrow;
            }
            return enabled;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a validator that will return true from collectAnnotations.
        validator = new TestValidator(true, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    @Test
    void collectAnnotations_shouldDelegateToOverriddenMethod_andReturnTrue() {
        ExecutionContext ctx = new ExecutionContext();
        boolean result = validator.invokeCollectAnnotations(ctx);
        assertTrue(result, "collectAnnotations should return true when the overridden method returns true");
    }

    @Test
    void collectAnnotations_shouldDelegateToOverriddenMethod_andReturnFalse() {
        // Create a validator that returns false.
        TestValidator falseValidator = new TestValidator(false, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
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

    /**
     * Basic happy‑path: non‑null context, collection enabled, non‑null keyword → true.
     */
    @Test
    void collectAnnotations_happyPath_returnsTrue() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        TestValidator validator = new TestValidator(// overload returns true
        true, // non‑null keyword
        "testKeyword", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        assertTrue(validator.invokeCollectAnnotations(ctx), "When collection is enabled and keyword is present, the wrapper should delegate and return true");
    }

    /**
     * When the overload returns false, the wrapper must propagate the false result.
     */
    @Test
    void collectAnnotations_overloadReturnsFalse_propagatesResult() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        TestValidator validator = new TestValidator(// overload returns false
        false, "type", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        assertFalse(validator.invokeCollectAnnotations(ctx), "The wrapper should return false when the overloaded method returns false");
    }

    /**
     * Null {@link ExecutionContext} should be guarded against – expected result is {@code false}.
     */
    @Test
    void collectAnnotations_nullExecutionContext_returnsFalse() {
        TestValidator validator = new TestValidator(true, "testKeyword", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        assertFalse(validator.invokeCollectAnnotations(null), "A null ExecutionContext must cause the method to return false without NPE");
    }

    /**
     * If {@code isCollectAnnotationsEnabled()} is {@code false}, the method short‑circuits to {@code false}.
     */
    @Test
    void collectAnnotations_collectionDisabled_returnsFalse() {
        ExecutionContext ctx = new SimpleExecutionContext(false);
        TestValidator validator = new TestValidator(true, "testKeyword", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        assertFalse(validator.invokeCollectAnnotations(ctx), "When annotation collection is disabled the wrapper must return false");
    }

    /**
     * When {@code getKeyword()} returns {@code null}, the wrapper must return {@code false}.
     */
    @Test
    void collectAnnotations_nullKeyword_returnsFalse() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        TestValidator validator = new TestValidator(true, null, null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        assertFalse(validator.invokeCollectAnnotations(ctx), "A null keyword should be treated as missing and cause a false result");
    }

    /**
     * An empty keyword (\"\") is also considered missing – expect {@code false}.
     */
    @Test
    void collectAnnotations_emptyKeyword_returnsFalse() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        TestValidator validator = new TestValidator(true, "", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        assertFalse(validator.invokeCollectAnnotations(ctx), "An empty keyword should be treated as missing and cause a false result");
    }

    /**
     * If the overloaded method throws a {@link RuntimeException}, it must propagate unchanged.
     */
    @Test
    void collectAnnotations_overloadThrowsRuntimeException_propagates() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        RuntimeException overloadEx = new RuntimeException("overload failure");
        TestValidator validator = new TestValidator(null, "testKeyword", overloadEx, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        RuntimeException thrown = assertThrows(RuntimeException.class, call, "The RuntimeException from the overload must propagate");
        assertSame(overloadEx, thrown, "Propagated exception should be the original one");
    }

    /**
     * If {@code getKeyword()} itself throws, the wrapper must not catch it.
     */
    @Test
    void collectAnnotations_keywordThrowsRuntimeException_propagates() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        RuntimeException keywordEx = new RuntimeException("keyword failure");
        TestValidator validator = new TestValidator(true, "unused", null, keywordEx, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        RuntimeException thrown = assertThrows(RuntimeException.class, call, "Exception from getKeyword() must propagate");
        assertSame(keywordEx, thrown, "Propagated exception should be the original one");
    }

    /**
     * When {@code isCollectAnnotationsEnabled()} throws {@link IllegalStateException}, it must propagate.
     */
    @Test
    void collectAnnotations_isCollectAnnotationsEnabledThrowsIllegalStateException_propagates() {
        ExecutionContext ctx = new SimpleExecutionContext(new IllegalStateException("illegal state"));
        TestValidator validator = new TestValidator(true, "testKeyword", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, call, "Exception from isCollectAnnotationsEnabled() must propagate");
        assertEquals("illegal state", thrown.getMessage());
    }

    /**
     * When {@code isCollectAnnotationsEnabled()} throws {@link NullPointerException}, it must propagate.
     */
    @Test
    void collectAnnotations_isCollectAnnotationsEnabledThrowsNullPointerException_propagates() {
        ExecutionContext ctx = new SimpleExecutionContext(new NullPointerException("null ptr"));
        TestValidator validator = new TestValidator(true, "testKeyword", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        NullPointerException thrown = assertThrows(NullPointerException.class, call, "Exception from isCollectAnnotationsEnabled() must propagate");
        assertEquals("null ptr", thrown.getMessage());
    }

    /**
     * When {@code isCollectAnnotationsEnabled()} throws {@link IllegalArgumentException}, it must propagate.
     */
    @Test
    void collectAnnotations_isCollectAnnotationsEnabledThrowsIllegalArgumentException_propagates() {
        ExecutionContext ctx = new SimpleExecutionContext(new IllegalArgumentException("bad arg"));
        TestValidator validator = new TestValidator(true, "testKeyword", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, call, "Exception from isCollectAnnotationsEnabled() must propagate");
        assertEquals("bad arg", thrown.getMessage());
    }

    /**
     * Errors (sub‑class of {@link java.lang.Error}) must also propagate unchanged.
     */
    @Test
    void collectAnnotations_overloadThrowsError_propagates() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        AssertionError error = new AssertionError("assertion failure");
        TestValidator validator = new TestValidator(null, "testKeyword", error, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        AssertionError thrown = assertThrows(AssertionError.class, call, "Errors thrown by the overload must propagate without conversion");
        assertSame(error, thrown, "Propagated error should be the original one");
    }

    /**
     * When the underlying overload cannot add an annotation because the map is immutable,
     *  it is expected to return {@code false}. This behaviour is simulated by stubbing the
     *  overload to return {@code false}.
     */
    @Test
    void collectAnnotations_immutableAnnotationsMap_returnsFalse() {
        ExecutionContext ctx = new SimpleExecutionContext(true);
        TestValidator validator = new TestValidator(false, "testKeyword", null, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        assertFalse(validator.invokeCollectAnnotations(ctx), "When the overload cannot add an annotation (simulated by returning false), the wrapper must return false");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:9: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:11: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:17: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:20: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:23: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:26: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:29: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:32: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_collectAnnotations_Test
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:50: error: TestValidator is not abstract and does not override abstract method validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath) in JsonValidator
    private static class TestValidator extends BaseJsonValidator {
                   ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:138: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_collectAnnotations_Test.java:308: error: incompatible types: AssertionError cannot be converted to RuntimeException
        TestValidator validator = new TestValidator(null, "testKeyword", error, null, schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
                                                                         ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
12 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method collectAnnotations(ExecutionContext)': '\n     * Determine if annotations should be reported.\n     * \n     * @param executionContext the execution context\n     * @return true if annotations should be reported\n     ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected void putAnnotation(ExecutionContext, Consumer<JsonNodeAnnotation.Builder>)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`, api document: \n     * Puts an annotation.\n     * \n     * @param executionContext the execution context\n     * @param customizer to customize the annotation\n     ;\n\t\tmethod `protected boolean collectAnnotations(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`, api document: \n     * Determine if annotations should be reported.\n     * \n     * @param executionContext the execution context\n     * @return true if annotations should be reported\n     ;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`;\nclass `com.networknt.schema.ItemsValidator202012`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`;\nclass `com.networknt.schema.PrefixItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`;\nclass `com.networknt.schema.ContainsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.collectAnnotations(ExecutionContext)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


