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
 * Tests for {@link BaseJsonValidator#getParentSchema()}.
 *
 * The abstract {@code BaseJsonValidator} cannot be instantiated directly,
 * therefore a minimal concrete subclass is provided inside the test class.
 * The subclass supplies dummy implementations for the abstract methods of
 * {@link JsonValidator}.  The tests verify that the {@code parentSchema}
 * supplied via the constructor is returned unchanged (including the {@code null}
 * case).
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    // -----------------------------------------------------------------------
    // Mocks required for the constructor of BaseJsonValidator
    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation mockSchemaLocation;
    @Mock
    private JsonNodePath mockEvaluationPath;
    @Mock
    private JsonNode mockSchemaNode;
    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private ValidatorTypeCode mockValidatorTypeCode;
    @Mock
    private JsonSchema mockParentSchema;

    private TestValidator validatorWithParent;
    private TestValidator validatorWithoutParent;

    // -----------------------------------------------------------------------
    // Helper concrete implementation of the abstract BaseJsonValidator
    // -----------------------------------------------------------------------
    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} used only for
     * exercising the {@code getParentSchema()} method.
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

        // -------------------------------------------------------------------
        // JsonValidator abstract methods – provide trivial implementations
        // -------------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle methods
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a validator where a non‑null parent schema is supplied
        validatorWithParent = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorTypeCode,
                mockValidationContext);

        // Create a validator where the parent schema is null
        validatorWithoutParent = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,
                mockValidatorTypeCode,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks between test methods to avoid cross‑test interference
        reset(mockSchemaLocation, mockEvaluationPath, mockSchemaNode,
                mockValidationContext, mockValidatorTypeCode, mockParentSchema);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    // -----------------------------------------------------------------------
    // Test cases
    // -----------------------------------------------------------------------
    @Test
    void getParentSchema_returnsSuppliedParentSchema() {
        // Act
        JsonSchema result = validatorWithParent.getParentSchema();

        // Assert
        assertNotNull(result, "Parent schema should not be null when supplied via constructor");
        assertSame(mockParentSchema, result,
                "The returned parent schema must be the exact instance supplied to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        // Act
        JsonSchema result = validatorWithoutParent.getParentSchema();

        // Assert
        assertNull(result, "Parent schema should be null when the constructor receives a null argument");
    }
}
```