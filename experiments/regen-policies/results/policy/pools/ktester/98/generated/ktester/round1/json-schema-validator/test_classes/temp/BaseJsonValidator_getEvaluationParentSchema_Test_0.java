package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationParentSchema_Test {

    // Mocks that are shared between tests
    @Mock
    ValidationContext validationContext;

    @Mock
    MessageSource messageSource;

    @Mock
    Keyword keyword;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    /**
     * Concrete subclass of {@link BaseJsonValidator} that allows us to
     * control the behaviour of {@code getParentSchema()} and to count its
     * invocations.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final JsonSchema parentSchemaToReturn;

        private final RuntimeException parentException;

        private int parentCallCount = 0;

        TestValidator(boolean suppressSubSchemaRetrieval, JsonNode schemaNode, ValidationContext validationContext, ErrorMessageType errorMessageType, String errorMessageKeyword, MessageSource messageSource, Keyword keyword, JsonSchema parentSchema, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonSchema evaluationParentSchema, JsonSchema parentSchemaToReturn, RuntimeException parentException) {
            super(suppressSubSchemaRetrieval, schemaNode, validationContext, errorMessageType, errorMessageKeyword, messageSource, keyword, parentSchema, schemaLocation, evaluationPath, evaluationParentSchema, Collections.emptyMap());
            this.parentSchemaToReturn = parentSchemaToReturn;
            this.parentException = parentException;
        }

        @Override
        public JsonSchema getParentSchema() {
            parentCallCount++;
            if (parentException != null) {
                throw parentException;
            }
            return parentSchemaToReturn;
        }

        int getParentCallCount() {
            return parentCallCount;
        }

        // Minimal implementations required by the JsonValidator interface
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
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

    @Test
    void shouldReturnEvaluationParentSchemaWhenItIsSet() {
        // GIVEN an evaluationParentSchema is supplied
        JsonSchema evaluationParent = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, // parentSchema (won’t be used)
        mock(JsonSchema.class), schemaLocation, evaluationPath, // evaluationParentSchema
        evaluationParent, // parentSchemaToReturn (unused)
        null, // parentException (none)
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN it returns the supplied evaluationParentSchema
        assertSame(evaluationParent, result, "When evaluationParentSchema is non‑null, getEvaluationParentSchema() must return it");
    }

    @Test
    void shouldReturnParentSchemaWhenEvaluationParentSchemaIsNull() {
        // GIVEN no evaluationParentSchema (null) and a parent schema
        JsonSchema parent = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, // parentSchema
        parent, schemaLocation, evaluationPath, // evaluationParentSchema is null
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN it falls back to the parent schema
        assertSame(parent, result, "When evaluationParentSchema is null, getEvaluationParentSchema() must return the parent schema");
    }

    @Test
    void shouldNotInvokeGetParentSchemaWhenEvaluationParentSchemaIsSet() {
        // GIVEN a validator with a non‑null evaluationParentSchema and a getParentSchema()
        // implementation that throws if called
        JsonSchema evaluationParent = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, // parentSchema (won’t be used)
        null, schemaLocation, evaluationPath, // evaluationParentSchema
        evaluationParent, // parentSchemaToReturn (unused)
        null, // will be thrown if called
        new AssertionError("getParentSchema should not be called"));
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN no exception is thrown and the evaluationParent is returned
        assertSame(evaluationParent, result);
    }

    @Test
    void shouldInvokeGetParentSchemaExactlyOnceWhenEvaluationParentSchemaIsNull() {
        // GIVEN a validator with a mock parent schema
        JsonSchema parent = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema is null
        null, // parentSchemaToReturn
        parent, // no exception
        null);
        // WHEN the method is invoked
        validator.getEvaluationParentSchema();
        // THEN getParentSchema() is called exactly once
        assertEquals(1, validator.getParentCallCount(), "getParentSchema() should be invoked once when evaluationParentSchema is null");
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnNullWhenBothEvaluationParentSchemaAndParentSchemaAreNull() {
        // GIVEN both evaluationParentSchema and the parent schema are null
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema
        null, // parentSchemaToReturn
        null, // no exception
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the result is null
        assertNull(result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPropagateRuntimeExceptionWhenParentSchemaThrows() {
        // GIVEN a parent schema that throws a RuntimeException
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema
        null, // parentSchemaToReturn (unused)
        null, // exception to throw
        boom);
        // WHEN the method is invoked -> THEN the exception propagates
        RuntimeException ex = assertThrows(RuntimeException.class, validator::getEvaluationParentSchema);
        assertSame(boom, ex);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnEvaluationParentSchemaEvenIfParentSchemaThrows() {
        // GIVEN an evaluationParentSchema and a parent schema that would throw
        JsonSchema evaluationParent = mock(JsonSchema.class);
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema is set
        evaluationParent, // parentSchemaToReturn (unused)
        null, // parent would throw if called
        boom);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the evaluationParentSchema is returned and no exception is thrown
        assertSame(evaluationParent, result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldReturnSameInstanceWhenEvaluationParentSchemaAndParentSchemaAreIdentical() {
        // GIVEN the same mock instance used for both fields
        JsonSchema sameInstance = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema
        sameInstance, // parentSchemaToReturn
        sameInstance, // no exception
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the exact same instance is returned
        assertSame(sameInstance, result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPreferEvaluationParentSchemaWhenItDiffersFromParentSchema() {
        // GIVEN different mock instances for evaluationParentSchema and parent
        JsonSchema evaluationParent = mock(JsonSchema.class);
        JsonSchema parent = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema
        evaluationParent, // parentSchemaToReturn (different)
        parent, // no exception
        null);
        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();
        // THEN the evaluationParentSchema is returned, ignoring the parent
        assertSame(evaluationParent, result);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPropagateIllegalArgumentExceptionFromParentSchema() {
        // GIVEN a parent schema that throws IllegalArgumentException
        IllegalArgumentException iae = new IllegalArgumentException("bad arg");
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema
        null, // parentSchemaToReturn
        null, // exception to throw
        iae);
        // WHEN the method is invoked -> THEN the same exception propagates
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, validator::getEvaluationParentSchema);
        assertSame(iae, ex);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldPropagateErrorFromParentSchema() {
        // GIVEN a parent schema that throws an AssertionError (subclass of Error)
        AssertionError error = new AssertionError("assertion failed");
        TestValidator validator = new TestValidator(false, schemaNode, validationContext, null, null, messageSource, keyword, null, schemaLocation, evaluationPath, // evaluationParentSchema
        null, // parentSchemaToReturn
        null, // exception to throw
        error);
        // WHEN the method is invoked -> THEN the Error propagates unchanged
        AssertionError ex = assertThrows(AssertionError.class, validator::getEvaluationParentSchema);
        assertSame(error, ex);
    }
}
