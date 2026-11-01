```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BaseJsonValidator_getEvaluationParentSchema_Test {

    /**
     * Concrete subclass of {@link BaseJsonValidator} that allows us to
     * control the behaviour of {@code getParentSchema()} and to count its
     * invocations.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final JsonSchema parentSchemaToReturn;
        private final Throwable parentException;
        private int parentCallCount = 0;

        TestValidator(boolean suppressSubSchemaRetrieval,
                      JsonNode schemaNode,
                      ValidationContext validationContext,
                      ErrorMessageType errorMessageType,
                      String errorMessageKeyword,
                      MessageSource messageSource,
                      Keyword keyword,
                      JsonSchema parentSchema,
                      SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonSchema evaluationParentSchema,
                      JsonSchema parentSchemaToReturn,
                      Throwable parentException) {
            super(suppressSubSchemaRetrieval,
                  schemaNode,
                  validationContext,
                  errorMessageType,
                  errorMessageKeyword,
                  messageSource,
                  keyword,
                  parentSchema,
                  schemaLocation,
                  evaluationPath,
                  evaluationParentSchema,
                  Collections.emptyMap());
            this.parentSchemaToReturn = parentSchemaToReturn;
            this.parentException = parentException;
        }

        @Override
        public JsonSchema getParentSchema() {
            parentCallCount++;
            if (parentException != null) {
                if (parentException instanceof RuntimeException) {
                    throw (RuntimeException) parentException;
                }
                if (parentException instanceof Error) {
                    throw (Error) parentException;
                }
                throw new RuntimeException(parentException);
            }
            return parentSchemaToReturn;
        }

        int getParentCallCount() {
            return parentCallCount;
        }

        // -----------------------------------------------------------------
        // Minimal implementations required by the JsonValidator interface
        // -----------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath path) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return "test-keyword";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }
    }

    // -----------------------------------------------------------------
    /** Helper that creates a {@link JsonSchema} instance via reflection,
     * bypassing its private constructor. */
    private static JsonSchema newSchema() {
        try {
            Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                    ValidationContext.class,
                    SchemaLocation.class,
                    JsonNodePath.class,
                    JsonNode.class,
                    JsonSchema.class,
                    boolean.class);
            ctor.setAccessible(true);
            return ctor.newInstance(null, null, null, null, null, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReturnEvaluationParentSchemaWhenItIsSet() {
        JsonSchema evaluationParent = newSchema();
        TestValidator validator = new TestValidator(
                false,               // suppressSubSchemaRetrieval
                null,                // schemaNode
                null,                // validationContext
                null,                // errorMessageType
                null,                // errorMessageKeyword
                null,                // messageSource
                null,                // keyword
                null,                // parentSchema (won’t be used)
                null,                // schemaLocation
                null,                // evaluationPath
                evaluationParent,    // evaluationParentSchema
                null,                // parentSchemaToReturn (unused)
                null);               // parentException
        JsonSchema result = validator.getEvaluationParentSchema();
        assertSame(evaluationParent, result,
                "When evaluationParentSchema is non‑null, getEvaluationParentSchema() must return it");
    }

    @Test
    void shouldReturnParentSchemaWhenEvaluationParentSchemaIsNull() {
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                parent,   // parentSchema (fallback)
                null,
                null,
                null,     // evaluationParentSchema is null
                null,
                null);
        JsonSchema result = validator.getEvaluationParentSchema();
        assertSame(parent, result,
                "When evaluationParentSchema is null, getEvaluationParentSchema() must return the parent schema");
    }

    @Test
    void shouldNotInvokeGetParentSchemaWhenEvaluationParentSchemaIsSet() {
        JsonSchema evaluationParent = newSchema();
        AssertionError shouldNotBeCalled = new AssertionError("getParentSchema should not be called");
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,                // parentSchema (unused)
                null,
                null,
                evaluationParent,    // evaluationParentSchema
                null,                // parentSchemaToReturn (unused)
                shouldNotBeCalled); // exception if getParentSchema is called
        JsonSchema result = validator.getEvaluationParentSchema();
        assertSame(evaluationParent, result);
        assertEquals(0, validator.getParentCallCount(),
                "getParentSchema must not be invoked when evaluationParentSchema is present");
    }

    @Test
    void shouldInvokeGetParentSchemaExactlyOnceWhenEvaluationParentSchemaIsNull() {
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,   // parentSchema (null)
                null,
                null,
                null,   // evaluationParentSchema is null
                parent, // parentSchemaToReturn
                null);  // no exception
        validator.getEvaluationParentSchema();
        assertEquals(1, validator.getParentCallCount(),
                "getParentSchema() should be invoked once when evaluationParentSchema is null");
    }

    @Test
    void shouldReturnNullWhenBothEvaluationParentSchemaAndParentSchemaAreNull() {
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        JsonSchema result = validator.getEvaluationParentSchema();
        assertNull(result);
    }

    @Test
    void shouldPropagateRuntimeExceptionWhenParentSchemaThrows() {
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                boom);
        RuntimeException ex = assertThrows(RuntimeException.class, validator::getEvaluationParentSchema);
        assertSame(boom, ex);
    }

    @Test
    void shouldReturnEvaluationParentSchemaEvenIfParentSchemaThrows() {
        JsonSchema evaluationParent = newSchema();
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                evaluationParent, // evaluationParentSchema
                null,
                boom);             // parentException (should not be reached)
        JsonSchema result = validator.getEvaluationParentSchema();
        assertSame(evaluationParent, result);
    }

    @Test
    void shouldReturnSameInstanceWhenEvaluationParentSchemaAndParentSchemaAreIdentical() {
        JsonSchema sameInstance = newSchema();
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                sameInstance,   // evaluationParentSchema
                sameInstance,   // parentSchemaToReturn
                null);
        JsonSchema result = validator.getEvaluationParentSchema();
        assertSame(sameInstance, result);
    }

    @Test
    void shouldPreferEvaluationParentSchemaWhenItDiffersFromParentSchema() {
        JsonSchema evaluationParent = newSchema();
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                evaluationParent, // evaluationParentSchema
                parent,           // parentSchemaToReturn
                null);
        JsonSchema result = validator.getEvaluationParentSchema();
        assertSame(evaluationParent, result);
    }

    @Test
    void shouldPropagateIllegalArgumentExceptionFromParentSchema() {
        IllegalArgumentException iae = new IllegalArgumentException("bad arg");
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                iae);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                validator::getEvaluationParentSchema);
        assertSame(iae, ex);
    }

    @Test
    void shouldPropagateErrorFromParentSchema() {
        AssertionError error = new AssertionError("assertion failed");
        TestValidator validator = new TestValidator(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                error);
        AssertionError ex = assertThrows(AssertionError.class,
                validator::getEvaluationParentSchema);
        assertSame(error, ex);
    }
}
```