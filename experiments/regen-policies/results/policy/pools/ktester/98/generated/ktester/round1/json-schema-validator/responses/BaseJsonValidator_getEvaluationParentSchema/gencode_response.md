```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getEvaluationParentSchema()}.
 *
 * <p>The tests cover all scenarios described in the supplied JSON test‑case
 * specification, including the handling of {@code null} values, invocation
 * counting, and exception propagation.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationParentSchema_Test {

    // ---------------------------------------------------------------------
    // Shared mocks
    // ---------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock MessageSource messageSource;
    @Mock Keyword keyword;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    // ---------------------------------------------------------------------
    // Helper concrete validator used for most tests
    // ---------------------------------------------------------------------
    /**
     * Concrete subclass of {@link BaseJsonValidator} that allows us to
     * control the behaviour of {@code getParentSchema()} and to count its
     * invocations.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final JsonSchema parentSchemaToReturn;
        private final RuntimeException parentException;
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
                       RuntimeException parentException) {
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

    // ---------------------------------------------------------------------
    // Lifecycle hooks (unchanged from the original framework)
    // ---------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required beyond the @Mock fields
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    // ---------------------------------------------------------------------
    // Test cases
    // ---------------------------------------------------------------------

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaIsSet
    // -----------------------------------------------------------------
    @Test
    void shouldReturnEvaluationParentSchemaWhenItIsSet() {
        // GIVEN an evaluationParentSchema is supplied
        JsonSchema evaluationParent = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                mock(JsonSchema.class), // parentSchema (won’t be used)
                schemaLocation,
                evaluationPath,
                evaluationParent,      // evaluationParentSchema
                null,                  // parentSchemaToReturn (unused)
                null);                 // parentException (none)

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN it returns the supplied evaluationParentSchema
        assertSame(evaluationParent, result,
                "When evaluationParentSchema is non‑null, getEvaluationParentSchema() must return it");
    }

    @Test
    void shouldNotInvokeGetParentSchemaWhenEvaluationParentSchemaIsSet() {
        // GIVEN a validator with a non‑null evaluationParentSchema and a getParentSchema()
        // implementation that throws if called
        JsonSchema evaluationParent = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,                  // parentSchema (won’t be used)
                schemaLocation,
                evaluationPath,
                evaluationParent,      // evaluationParentSchema
                null,                  // parentSchemaToReturn (unused)
                new AssertionError("getParentSchema should not be called")); // will be thrown if called

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN no exception is thrown and the evaluationParent is returned
        assertSame(evaluationParent, result);
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaIsNull
    // -----------------------------------------------------------------
    @Test
    void shouldReturnParentSchemaWhenEvaluationParentSchemaIsNull() {
        // GIVEN a null evaluationParentSchema and a mock parent schema
        JsonSchema parent = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,                 // parentSchema (not used by BaseJsonValidator)
                schemaLocation,
                evaluationPath,
                null,                 // evaluationParentSchema is null
                parent,               // parentSchemaToReturn
                null);                // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN it falls back to the parent schema
        assertSame(parent, result);
    }

    @Test
    void shouldInvokeGetParentSchemaExactlyOnceWhenEvaluationParentSchemaIsNull() {
        // GIVEN a validator with a mock parent schema
        JsonSchema parent = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                null,                 // evaluationParentSchema is null
                parent,              // parentSchemaToReturn
                null);                // no exception

        // WHEN the method is invoked
        validator.getEvaluationParentSchema();

        // THEN getParentSchema() is called exactly once
        assertEquals(1, validator.getParentCallCount(),
                "getParentSchema() should be invoked once when evaluationParentSchema is null");
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaNullParentNull
    // -----------------------------------------------------------------
    @Test
    void shouldReturnNullWhenBothEvaluationParentSchemaAndParentSchemaAreNull() {
        // GIVEN both evaluationParentSchema and the parent schema are null
        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                null,   // evaluationParentSchema
                null,   // parentSchemaToReturn
                null);  // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the result is null
        assertNull(result);
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaNullParentThrows
    // -----------------------------------------------------------------
    @Test
    void shouldPropagateRuntimeExceptionWhenParentSchemaThrows() {
        // GIVEN a parent schema that throws a RuntimeException
        RuntimeException boom = new RuntimeException("boom");
        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                null,   // evaluationParentSchema
                null,   // parentSchemaToReturn (unused)
                boom);  // exception to throw

        // WHEN the method is invoked -> THEN the exception propagates
        RuntimeException ex = assertThrows(RuntimeException.class,
                validator::getEvaluationParentSchema);
        assertSame(boom, ex);
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaSetParentThrows
    // -----------------------------------------------------------------
    @Test
    void shouldReturnEvaluationParentSchemaEvenIfParentSchemaThrows() {
        // GIVEN an evaluationParentSchema and a parent schema that would throw
        JsonSchema evaluationParent = mock(JsonSchema.class);
        RuntimeException boom = new RuntimeException("boom");

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                evaluationParent, // evaluationParentSchema is set
                null,            // parentSchemaToReturn (unused)
                boom);           // parent would throw if called

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the evaluationParentSchema is returned and no exception is thrown
        assertSame(evaluationParent, result);
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaSetSameAsParent
    // -----------------------------------------------------------------
    @Test
    void shouldReturnSameInstanceWhenEvaluationParentSchemaAndParentSchemaAreIdentical() {
        // GIVEN the same mock instance used for both fields
        JsonSchema sameInstance = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                sameInstance, // evaluationParentSchema
                sameInstance, // parentSchemaToReturn
                null);        // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the exact same instance is returned
        assertSame(sameInstance, result);
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaSetDifferentFromParent
    // -----------------------------------------------------------------
    @Test
    void shouldPreferEvaluationParentSchemaWhenItDiffersFromParentSchema() {
        // GIVEN different mock instances for evaluationParentSchema and parent
        JsonSchema evaluationParent = mock(JsonSchema.class);
        JsonSchema parent = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                evaluationParent, // evaluationParentSchema
                parent,          // parentSchemaToReturn (different)
                null);           // no exception

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN the evaluationParentSchema is returned, ignoring the parent
        assertSame(evaluationParent, result);
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaNullParentThrowsIllegalArgument
    // -----------------------------------------------------------------
    @Test
    void shouldPropagateIllegalArgumentExceptionFromParentSchema() {
        // GIVEN a parent schema that throws IllegalArgumentException
        IllegalArgumentException iae = new IllegalArgumentException("bad arg");
        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                null,   // evaluationParentSchema
                null,   // parentSchemaToReturn
                iae);   // exception to throw

        // WHEN the method is invoked -> THEN the same exception propagates
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                validator::getEvaluationParentSchema);
        assertSame(iae, ex);
    }

    // -----------------------------------------------------------------
    // Group: evaluationParentSchemaNullParentThrowsError
    // -----------------------------------------------------------------
    @Test
    void shouldPropagateErrorFromParentSchema() {
        // GIVEN a parent schema that throws an AssertionError (subclass of Error)
        AssertionError error = new AssertionError("assertion failed");
        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                null,
                schemaLocation,
                evaluationPath,
                null,   // evaluationParentSchema
                null,   // parentSchemaToReturn
                error); // exception to throw

        // WHEN the method is invoked -> THEN the Error propagates unchanged
        AssertionError ex = assertThrows(AssertionError.class,
                validator::getEvaluationParentSchema);
        assertSame(error, ex);
    }
}
```