```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.networknt.schema.i18n.MessageSource;

/**
 * Unit tests for {@link BaseJsonValidator#getEvaluationParentSchema()}.
 *
 * <p>The original test used Mockito for field injection, but the project does not
 * depend on Mockito.  All mock objects have therefore been removed and the
 * test now creates the minimal objects required for the logic under test.</p>
 */
class BaseJsonValidator_getEvaluationParentSchema_Test {

    /**
     * Concrete subclass of {@link BaseJsonValidator} that allows us to
     * control the behaviour of {@code getParentSchema()} and to count its
     * invocations.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final JsonSchema parentSchemaToReturn;
        private final Throwable parentException;   // may be RuntimeException or Error
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
                // wrap any other throwable
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
    /** Helper that creates a {@link JsonSchema} instance with a constructor
     *  that does not require a full configuration.  All arguments are set to
     *  {@code null} or {@code false} because the tests only need a distinct
     *  object identity. */
    private static JsonSchema newSchema() {
        // JsonSchema(ValidationContext, SchemaLocation, JsonNodePath,
        //            JsonNode, JsonSchema, boolean)
        return new JsonSchema(null, null, null, null, null, false);
    }

    @Test
    void shouldReturnEvaluationParentSchemaWhenItIsSet() {
        // GIVEN an evaluationParentSchema is supplied
        JsonSchema evaluationParent = newSchema();
        // not used
        JsonSchema dummyParent = newSchema();
        TestValidator validator = new TestValidator(
                false,               // suppressSubSchemaRetrieval
                null,               // schemaNode
                null,               // validationContext
                null,               // errorMessageType
                null,               // errorMessageKeyword
                null,               // messageSource
                null,               // keyword
                null,               // parentSchema (won’t be used)
                dummyParent,        // schemaLocation
                null,               // evaluationPath
                null,               // evaluationParentSchema
                evaluationParent,   // parentSchemaToReturn (unused)
                null);              // parentException

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN it returns the supplied evaluationParentSchema
        assertSame(evaluationParent, result,
                "When evaluationParentSchema is non‑null, getEvaluationParentSchema() must return it");
    }

    @Test
    void shouldReturnParentSchemaWhenEvaluationParentSchemaIsNull() {
        // GIVEN no evaluationParentSchema (null) and a parent schema
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                parent,          // parentSchema (used as fallback)
                null, null,     // schemaLocation, evaluationPath
                null,            // evaluationParentSchema is null
                null,            // parentSchemaToReturn
                null);           // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN it falls back to the parent schema
        assertSame(parent, result,
                "When evaluationParentSchema is null, getEvaluationParentSchema() must return the parent schema");
    }

    @Test
    void shouldNotInvokeGetParentSchemaWhenEvaluationParentSchemaIsSet() {
        // GIVEN a validator with a non‑null evaluationParentSchema and a
        // getParentSchema() implementation that would throw if called
        JsonSchema evaluationParent = newSchema();
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                evaluationParent,                 // evaluationParentSchema
                evaluationParent,                 // parentSchemaToReturn (unused)
                new AssertionError("getParentSchema should not be called")); // exception if called

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN no exception is thrown and the evaluationParent is returned
        assertSame(evaluationParent, result);
        // also verify that getParentSchema was not called
        assertEquals(0, validator.getParentCallCount(),
                "getParentSchema must not be invoked when evaluationParentSchema is present");
    }

    @Test
    void shouldInvokeGetParentSchemaExactlyOnceWhenEvaluationParentSchemaIsNull() {
        // GIVEN a validator with a mock parent schema
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                null,                             // evaluationParentSchema is null
                parent,                           // parentSchemaToReturn
                null);                            // no exception

        // WHEN the method is invoked
        validator.getEvaluationParentSchema();

        // THEN getParentSchema() is called exactly once
        assertEquals(1, validator.getParentCallCount(),
                "getParentSchema() should be invoked once when evaluationParentSchema is null");
    }

    @Test
    void shouldReturnNullWhenBothEvaluationParentSchemaAndParentSchemaAreNull() {
        // GIVEN both evaluationParentSchema and the parent schema are null
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                null,                             // evaluationParentSchema
                null,                             // parentSchemaToReturn
                null);                            // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the result is null
        assertNull(result);
    }

    @Test
    void shouldPropagateRuntimeExceptionWhenParentSchemaThrows() {
        // GIVEN a parent schema that throws a RuntimeException
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                null,                             // evaluationParentSchema
                null,                             // parentSchemaToReturn (unused)
                boom);                            // exception to throw

        // WHEN the method is invoked -> THEN the exception propagates
        RuntimeException ex = assertThrows(RuntimeException.class,
                validator::getEvaluationParentSchema);
        assertSame(boom, ex);
    }

    @Test
    void shouldReturnEvaluationParentSchemaEvenIfParentSchemaThrows() {
        // GIVEN an evaluationParentSchema and a parent schema that would throw
        JsonSchema evaluationParent = newSchema();
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                evaluationParent,                // evaluationParentSchema is set
                null,                             // parentSchemaToReturn (unused)
                boom);                            // parent would throw if called

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the evaluationParentSchema is returned and no exception is thrown
        assertSame(evaluationParent, result);
    }

    @Test
    void shouldReturnSameInstanceWhenEvaluationParentSchemaAndParentSchemaAreIdentical() {
        // GIVEN the same mock instance used for both fields
        JsonSchema sameInstance = newSchema();
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                sameInstance,                     // evaluationParentSchema
                sameInstance,                     // parentSchemaToReturn
                null);                            // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the exact same instance is returned
        assertSame(sameInstance, result);
    }

    @Test
    void shouldPreferEvaluationParentSchemaWhenItDiffersFromParentSchema() {
        // GIVEN different mock instances for evaluationParentSchema and parent
        JsonSchema evaluationParent = newSchema();
        JsonSchema parent = newSchema();
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                evaluationParent,                 // evaluationParentSchema
                parent,                           // parentSchemaToReturn
                null);                            // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the evaluationParentSchema is returned, ignoring the parent
        assertSame(evaluationParent, result);
    }

    @Test
    void shouldPropagateIllegalArgumentExceptionFromParentSchema() {
        // GIVEN a parent schema that throws IllegalArgumentException
        IllegalArgumentException iae = new IllegalArgumentException("bad arg");
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                null,                             // evaluationParentSchema
                null,                             // parentSchemaToReturn
                iae);                             // exception to throw

        // WHEN the method is invoked -> THEN the same exception propagates
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                validator::getEvaluationParentSchema);
        assertSame(iae, ex);
    }

    @Test
    void shouldPropagateErrorFromParentSchema() {
        // GIVEN a parent schema that throws an AssertionError (subclass of Error)
        AssertionError error = new AssertionError("assertion failed");
        TestValidator validator = new TestValidator(
                false, null, null, null, null, null, null,
                null, null, null,                 // parentSchema, schemaLocation, evaluationPath
                null,                             // evaluationParentSchema
                null,                             // parentSchemaToReturn
                error);                           // exception to throw

        // WHEN the method is invoked -> THEN the Error propagates unchanged
        AssertionError ex = assertThrows(AssertionError.class,
                validator::getEvaluationParentSchema);
        assertSame(error, ex);
    }
}
```