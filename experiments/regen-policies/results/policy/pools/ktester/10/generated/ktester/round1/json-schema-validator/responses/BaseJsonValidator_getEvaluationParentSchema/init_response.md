**Generated test class**

```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.Set;

/**
 * Unit tests for {@link BaseJsonValidator#getEvaluationParentSchema()}.
 *
 * The tests cover the two possible execution paths:
 * <ul>
 *   <li>When {@code evaluationParentSchema} is {@code null} the method must return the
 *       {@code parentSchema} supplied to the validator.</li>
 *   <li>When {@code evaluationParentSchema} is non‑null the method must return that
 *       schema instead of the parent.</li>
 * </ul>
 *
 * A minimal concrete subclass of {@link BaseJsonValidator} is created inside the test
 * suite because the class is abstract. All abstract methods are stubbed with dummy
 * implementations – they are not exercised by the tests.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationParentSchema_Test {

    /** Mocks required for constructing a {@link BaseJsonValidator}. */
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock JsonSchema evaluationParentSchema;
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
        // GIVEN – the validator has been constructed with a non‑null parent schema
        // (already done in {@code setupBeforeEach}).

        // WHEN
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN
        assertSame(parentSchema, result,
                "When evaluationParentSchema is null the parent schema should be returned");
    }

    /** When {@code evaluationParentSchema} is set the method must return that schema. */
    @Test
    void shouldReturnEvaluationParentWhenItIsSet() {
        // GIVEN – manually set the protected field (it is visible to the subclass)
        validator.evaluationParentSchema = evaluationParentSchema;

        // WHEN
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN
        assertSame(evaluationParentSchema, result,
                "When evaluationParentSchema is set it should be returned");
    }

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used only for
     * exercising {@code getEvaluationParentSchema()}.
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

        /* --------------------------------------------------------------------- */
        /* The following methods are required by the JsonValidator interface.  */
        /* They are not relevant for the current tests and therefore return   */
        /* simple stub values.                                                  */
        /* --------------------------------------------------------------------- */

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
            // The parent schema is already stored in the superclass; delegate to it.
            return super.getParentSchema();
        }
    }
}
```