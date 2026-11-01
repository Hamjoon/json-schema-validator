```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getEvaluationParentSchema()}.
 *
 * The tests cover all scenarios described in the JSON test‑case specification,
 * including null handling, exception propagation and short‑circuit behaviour.
 *
 * A minimal concrete subclass {@link TestValidator} is used because {@code BaseJsonValidator}
 * is abstract. Additional inner subclasses override {@code getParentSchema()} to simulate
 * various behaviours (returning {@code null}, throwing exceptions, etc.).
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationParentSchema_Test {

    /** Mocks required for constructing a {@link BaseJsonValidator}. */
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;               // default parent schema mock
    @Mock JsonSchema evaluationParentSchema;    // mock used as evaluationParentSchema
    @Mock ValidatorTypeCode validatorType;

    /** The concrete validator used in the tests. */
    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a validator that initially has no evaluationParentSchema.
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        Mockito.reset(validationContext, schemaLocation, evaluationPath,
                schemaNode, parentSchema, evaluationParentSchema, validatorType);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /** When {@code evaluationParentSchema} is {@code null} the method must return the parent schema. */
    @Test
    void shouldReturnParentSchemaWhenEvaluationParentIsNull() {
        // GIVEN – validator constructed with a non‑null parent schema (setupBeforeEach).

        // WHEN
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN
        assertSame(parentSchema, result,
                "When evaluationParentSchema is null the parent schema should be returned");
    }

    /** When {@code evaluationParentSchema} is set the method must return that schema. */
    @Test
    void shouldReturnEvaluationParentWhenItIsSet() {
        // GIVEN – manually set the protected field (visible to the subclass)
        validator.evaluationParentSchema = evaluationParentSchema;

        // WHEN
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN
        assertSame(evaluationParentSchema, result,
                "When evaluationParentSchema is set it should be returned");
    }

    /** evaluationParentSchema null and parent schema null -> result should be null. */
    @Test
    void shouldReturnNullWhenBothEvaluationParentAndParentAreNull() {
        // GIVEN – a validator whose getParentSchema() returns null
        NullParentValidator nullParentValidator = new NullParentValidator(
                schemaLocation, evaluationPath, schemaNode, null, validatorType, validationContext);

        // WHEN
        JsonSchema result = nullParentValidator.getEvaluationParentSchema();

        // THEN
        assertNull(result, "Both evaluationParentSchema and parent schema are null, result must be null");
    }

    /** evaluationParentSchema non‑null prevents any call to getParentSchema(). */
    @Test
    void shouldNotCallGetParentSchemaWhenEvaluationParentIsNonNull() {
        // GIVEN – a spy that can verify method calls
        TestValidator spyValidator = spy(validator);
        spyValidator.evaluationParentSchema = evaluationParentSchema;

        // WHEN
        JsonSchema result = spyValidator.getEvaluationParentSchema();

        // THEN
        assertSame(evaluationParentSchema, result,
                "Should return the non‑null evaluationParentSchema");
        // Verify that the overridden getParentSchema() was never invoked.
        verify(spyValidator, never()).getParentSchema();
    }

    /** evaluationParentSchema can be set after construction and is honoured. */
    @Test
    void shouldReturnLaterAssignedEvaluationParentSchema() {
        // GIVEN – field is set after the validator has been constructed
        validator.evaluationParentSchema = evaluationParentSchema;

        // WHEN
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN
        assertSame(evaluationParentSchema, result,
                "The schema assigned after construction must be returned");
    }

    /** When evaluationParentSchema is null and getParentSchema() throws RuntimeException,
     * the exception must propagate unchanged. */
    @Test
    void shouldPropagateRuntimeExceptionFromParentWhenEvaluationParentIsNull() {
        // GIVEN – validator whose getParentSchema() throws a RuntimeException
        RuntimeExceptionParentValidator exceptionValidator = new RuntimeExceptionParentValidator(
                schemaLocation, evaluationPath, schemaNode, parentSchema,
                validatorType, validationContext, new RuntimeException("parent schema failure"));

        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                exceptionValidator::getEvaluationParentSchema,
                "RuntimeException from getParentSchema should be propagated");
        assertEquals("parent schema failure", ex.getMessage());
    }

    /** When evaluationParentSchema is non‑null, a RuntimeException from getParentSchema()
     * must NOT be propagated – the method should short‑circuit. */
    @Test
    void shouldIgnoreParentExceptionWhenEvaluationParentIsNonNull() {
        // GIVEN – validator that throws in getParentSchema() but has a non‑null evaluationParentSchema
        RuntimeExceptionParentValidator exceptionValidator = new RuntimeExceptionParentValidator(
                schemaLocation, evaluationPath, schemaNode, parentSchema,
                validatorType, validationContext, new RuntimeException("parent schema failure"));
        exceptionValidator.evaluationParentSchema = evaluationParentSchema;

        // WHEN
        JsonSchema result = exceptionValidator.getEvaluationParentSchema();

        // THEN
        assertSame(evaluationParentSchema, result,
                "Non‑null evaluationParentSchema must be returned even if parent throws");
    }

    /** When evaluationParentSchema is null and getParentSchema() throws NullPointerException,
     * the NPE should bubble up. */
    @Test
    void shouldPropagateNullPointerExceptionFromParentWhenEvaluationParentIsNull() {
        // GIVEN – validator whose getParentSchema() throws NPE
        NullPointerExceptionParentValidator npeValidator = new NullPointerExceptionParentValidator(
                schemaLocation, evaluationPath, schemaNode, parentSchema,
                validatorType, validationContext);

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                npeValidator::getEvaluationParentSchema,
                "NullPointerException from getParentSchema should be propagated");
    }

    /** When evaluationParentSchema is null and getParentSchema() throws a RuntimeException
     * wrapping a checked exception (IllegalStateException), the wrapper must propagate. */
    @Test
    void shouldPropagateWrappedCheckedExceptionFromParentWhenEvaluationParentIsNull() {
        // GIVEN – validator whose getParentSchema() throws RuntimeException(IllegalStateException)
        RuntimeException wrapped = new RuntimeException(new IllegalStateException("illegal state"));
        RuntimeExceptionParentValidator wrappedValidator = new RuntimeExceptionParentValidator(
                schemaLocation, evaluationPath, schemaNode, parentSchema,
                validatorType, validationContext, wrapped);

        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                wrappedValidator::getEvaluationParentSchema,
                "Wrapped RuntimeException should be propagated");
        assertSame(wrapped, ex, "The exact RuntimeException instance must be thrown");
        assertTrue(ex.getCause() instanceof IllegalStateException,
                "Cause should be the original IllegalStateException");
        assertEquals("illegal state", ex.getCause().getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Helper concrete subclasses used for special test scenarios.          */
    /* --------------------------------------------------------------------- */

    /**
     * Minimal concrete implementation used for the majority of tests.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return null;
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonSchema getParentSchema() {
            // Delegate to the superclass implementation which returns the field set in the constructor.
            return super.getParentSchema();
        }
    }

    /**
     * Subclass that overrides {@code getParentSchema()} to return {@code null}.
     */
    private static class NullParentValidator extends TestValidator {
        NullParentValidator(SchemaLocation schemaLocation,
                           JsonNodePath evaluationPath,
                           JsonNode schemaNode,
                           JsonSchema parentSchema,
                           ValidatorTypeCode validatorType,
                           ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }
    }

    /**
     * Subclass that overrides {@code getParentSchema()} to throw a supplied RuntimeException.
     */
    private static class RuntimeExceptionParentValidator extends TestValidator {
        private final RuntimeException toThrow;

        RuntimeExceptionParentValidator(SchemaLocation schemaLocation,
                                        JsonNodePath evaluationPath,
                                        JsonNode schemaNode,
                                        JsonSchema parentSchema,
                                        ValidatorTypeCode validatorType,
                                        ValidationContext validationContext,
                                        RuntimeException toThrow) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.toThrow = toThrow;
        }

        @Override
        public JsonSchema getParentSchema() {
            throw toThrow;
        }
    }

    /**
     * Subclass that overrides {@code getParentSchema()} to throw a NullPointerException.
     */
    private static class NullPointerExceptionParentValidator extends TestValidator {
        NullPointerExceptionParentValidator(SchemaLocation schemaLocation,
                                            JsonNodePath evaluationPath,
                                            JsonNode schemaNode,
                                            JsonSchema parentSchema,
                                            ValidatorTypeCode validatorType,
                                            ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public JsonSchema getParentSchema() {
            throw new NullPointerException("parent NPE");
        }
    }
}
```