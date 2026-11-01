```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unit tests for {@link BaseJsonValidator#collectAnnotations(ExecutionContext)}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * implementation – {@link TestValidator} – is provided inside the test class.
 * The implementation allows us to control the behaviour of the overloaded
 * {@code collectAnnotations(ExecutionContext, String)} method, the value (or
 * exception) returned by {@code getKeyword()}, and to verify that the protected
 * {@code collectAnnotations(ExecutionContext)} method correctly delegates to the
 * overload while honouring the contract described in the API docs.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_Test {

    /* --------------------------------------------------------------------- *
     *  Mocks required for the BaseJsonValidator constructor                     *
     * --------------------------------------------------------------------- */
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;
    @Mock ValidationContext validationContext;

    /* --------------------------------------------------------------------- *
     *  Helper concrete validator                                             *
     * --------------------------------------------------------------------- */
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

        /** Value that the overload should return (ignored if {@code overloadException} is set). */
        private final Boolean overloadResult;
        /** Keyword to be returned by {@code getKeyword()} – may be {@code null} or empty. */
        private final String keyword;
        /** Exception to be thrown by the overload (if non‑null). */
        private final RuntimeException overloadException;
        /** Exception to be thrown by {@code getKeyword()} (if non‑null). */
        private final RuntimeException keywordException;

        TestValidator(Boolean overloadResult,
                      String keyword,
                      RuntimeException overloadException,
                      RuntimeException keywordException,
                      SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
            this.overloadResult = overloadResult;
            this.keyword = keyword;
            this.overloadException = overloadException;
            this.keywordException = keywordException;
        }

        /** Expose the protected method for the tests. */
        boolean invokeCollectAnnotations(ExecutionContext executionContext) {
            return super.collectAnnotations(executionContext);
        }

        /** The keyword used by the validator – may be {@code null} or empty. */
        @Override
        public String getKeyword() {
            if (keywordException != null) {
                throw keywordException;
            }
            return keyword;
        }

        /**
         * Overloaded implementation – either returns the preset {@code overloadResult}
         * or throws the configured {@code overloadException}.
         */
        @Override
        protected boolean collectAnnotations(ExecutionContext executionContext, String keyword) {
            if (overloadException != null) {
                throw overloadException;
            }
            // In a real validator the keyword would influence the behaviour.
            // Here we simply return the preset value (defaulting to {@code false}
            // when {@code null} to keep the method non‑null‑returning.
            return overloadResult != null ? overloadResult : false;
        }

        // Remaining abstract methods are inherited from the hierarchy and are not
        // required for these unit tests.
    }

    /* --------------------------------------------------------------------- *
     *  Test fixtures                                                          *
     * --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    /* --------------------------------------------------------------------- *
     *  Test cases                                                             *
     * --------------------------------------------------------------------- */

    /** Basic happy‑path: non‑null context, collection enabled, non‑null keyword → true. */
    @Test
    void collectAnnotations_happyPath_returnsTrue() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);

        TestValidator validator = new TestValidator(
                true,                     // overload returns true
                "testKeyword",            // non‑null keyword
                null,                     // no overload exception
                null,                     // no keyword exception
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        assertTrue(validator.invokeCollectAnnotations(ctx),
                "When collection is enabled and keyword is present, the wrapper should delegate and return true");
    }

    /** When the overload returns false, the wrapper must propagate the false result. */
    @Test
    void collectAnnotations_overloadReturnsFalse_propagatesResult() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);

        TestValidator validator = new TestValidator(
                false,                    // overload returns false
                "type",                   // non‑null keyword
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        assertFalse(validator.invokeCollectAnnotations(ctx),
                "The wrapper should return false when the overloaded method returns false");
    }

    /** Null {@link ExecutionContext} should be guarded against – expected result is {@code false}. */
    @Test
    void collectAnnotations_nullExecutionContext_returnsFalse() {
        TestValidator validator = new TestValidator(
                true,
                "testKeyword",
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        assertFalse(validator.invokeCollectAnnotations(null),
                "A null ExecutionContext must cause the method to return false without NPE");
    }

    /** If {@code isCollectAnnotationsEnabled()} is {@code false}, the method short‑circuits to {@code false}. */
    @Test
    void collectAnnotations_collectionDisabled_returnsFalse() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(false);

        TestValidator validator = new TestValidator(
                true,
                "testKeyword",
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        assertFalse(validator.invokeCollectAnnotations(ctx),
                "When annotation collection is disabled the wrapper must return false");
    }

    /** When {@code getKeyword()} returns {@code null}, the wrapper must return {@code false}. */
    @Test
    void collectAnnotations_nullKeyword_returnsFalse() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);

        TestValidator validator = new TestValidator(
                true,
                null,                     // keyword is null
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        assertFalse(validator.invokeCollectAnnotations(ctx),
                "A null keyword should be treated as missing and cause a false result");
    }

    /** An empty keyword (\"\") is also considered missing – expect {@code false}. */
    @Test
    void collectAnnotations_emptyKeyword_returnsFalse() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);

        TestValidator validator = new TestValidator(
                true,
                "",                       // empty keyword
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        assertFalse(validator.invokeCollectAnnotations(ctx),
                "An empty keyword should be treated as missing and cause a false result");
    }

    /** If the overloaded method throws a {@link RuntimeException}, it must propagate unchanged. */
    @Test
    void collectAnnotations_overloadThrowsRuntimeException_propagates() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);

        RuntimeException overloadEx = new RuntimeException("overload failure");
        TestValidator validator = new TestValidator(
                null,
                "testKeyword",
                overloadEx,               // overload will throw
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        RuntimeException thrown = assertThrows(RuntimeException.class, call,
                "The RuntimeException from the overload must propagate");
        assertSame(overloadEx, thrown, "Propagated exception should be the original one");
    }

    /** If {@code getKeyword()} itself throws, the wrapper must not catch it. */
    @Test
    void collectAnnotations_keywordThrowsRuntimeException_propagates() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);

        RuntimeException keywordEx = new RuntimeException("keyword failure");
        TestValidator validator = new TestValidator(
                true,
                "unused",                 // value never used because getKeyword() throws
                null,
                keywordEx,                // keyword will throw
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        RuntimeException thrown = assertThrows(RuntimeException.class, call,
                "Exception from getKeyword() must propagate");
        assertSame(keywordEx, thrown, "Propagated exception should be the original one");
    }

    /** When {@code isCollectAnnotationsEnabled()} throws {@link IllegalStateException}, it must propagate. */
    @Test
    void collectAnnotations_isCollectAnnotationsEnabledThrowsIllegalStateException_propagates() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenThrow(new IllegalStateException("illegal state"));

        TestValidator validator = new TestValidator(
                true,
                "testKeyword",
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, call,
                "Exception from isCollectAnnotationsEnabled() must propagate");
        assertEquals("illegal state", thrown.getMessage());
    }

    /** When {@code isCollectAnnotationsEnabled()} throws {@link NullPointerException}, it must propagate. */
    @Test
    void collectAnnotations_isCollectAnnotationsEnabledThrowsNullPointerException_propagates() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenThrow(new NullPointerException("null ptr"));

        TestValidator validator = new TestValidator(
                true,
                "testKeyword",
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        NullPointerException thrown = assertThrows(NullPointerException.class, call,
                "Exception from isCollectAnnotationsEnabled() must propagate");
        assertEquals("null ptr", thrown.getMessage());
    }

    /** When {@code isCollectAnnotationsEnabled()} throws {@link IllegalArgumentException}, it must propagate. */
    @Test
    void collectAnnotations_isCollectAnnotationsEnabledThrowsIllegalArgumentException_propagates() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenThrow(new IllegalArgumentException("bad arg"));

        TestValidator validator = new TestValidator(
                true,
                "testKeyword",
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, call,
                "Exception from isCollectAnnotationsEnabled() must propagate");
        assertEquals("bad arg", thrown.getMessage());
    }

    /** Errors (sub‑class of {@link java.lang.Error}) must also propagate unchanged. */
    @Test
    void collectAnnotations_overloadThrowsError_propagates() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);

        AssertionError error = new AssertionError("assertion failure");
        TestValidator validator = new TestValidator(
                null,
                "testKeyword",
                error,                     // overload throws an Error
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        Executable call = () -> validator.invokeCollectAnnotations(ctx);
        AssertionError thrown = assertThrows(AssertionError.class, call,
                "Errors thrown by the overload must propagate without conversion");
        assertSame(error, thrown, "Propagated error should be the original one");
    }

    /** When the underlying overload cannot add an annotation because the map is immutable,
     *  it is expected to return {@code false}. This behaviour is simulated by stubbing the
     *  overload to return {@code false}. */
    @Test
    void collectAnnotations_immutableAnnotationsMap_returnsFalse() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotationsEnabled()).thenReturn(true);
        // Simulate that the underlying collectAnnotations cannot add anything and returns false.
        TestValidator validator = new TestValidator(
                false,                     // overload result = false
                "testKeyword",
                null,
                null,
                schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);

        assertFalse(validator.invokeCollectAnnotations(ctx),
                "When the overload cannot add an annotation (simulated by returning false), the wrapper must return false");
    }
}
```