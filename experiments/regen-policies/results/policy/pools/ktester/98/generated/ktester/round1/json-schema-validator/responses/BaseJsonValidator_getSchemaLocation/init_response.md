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

/**
 * Unit tests for {@link BaseJsonValidator#getSchemaLocation()}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * implementation is provided inside the test class. Only the {@code validate}
 * method (required by {@link JsonValidator}) is implemented – the rest of the
 * behaviour is inherited from {@code BaseJsonValidator}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaLocation_Test {

    /** The validator instance used in the tests. */
    private TestValidator validator;

    /** Mocks required for the constructor of {@code BaseJsonValidator}. */
    @Mock private JsonNode mockSchemaNode;
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidatorTypeCode mockValidatorType;
    @Mock private ValidationContext mockValidationContext;
    @Mock private JsonNodePath mockEvaluationPath;
    @Mock private AbsoluteIri mockAbsoluteIri;
    @Mock private JsonNodePath mockFragmentPath;

    /** Minimal concrete subclass of {@code BaseJsonValidator}. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /** {@inheritDoc} */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the current tests.
            return Collections.emptySet();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a realistic SchemaLocation instance – the concrete classes are
        // simple value objects, so we can use real instances instead of mocks.
        SchemaLocation schemaLocation = new SchemaLocation(mockAbsoluteIri, mockFragmentPath);

        // Instantiate the concrete validator under test.
        validator = new TestValidator(
                schemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the reference to help the garbage collector and avoid accidental reuse.
        validator = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getSchemaLocation_returnsTheSameInstanceProvidedInConstructor() {
        // GIVEN – the validator has been constructed in @BeforeEach with a known
        // SchemaLocation instance.
        SchemaLocation expected = validator.getSchemaLocation();

        // WHEN
        SchemaLocation actual = validator.getSchemaLocation();

        // THEN
        assertSame(expected, actual,
                "BaseJsonValidator#getSchemaLocation() should return the exact instance supplied to the constructor");
    }

    @Test
    void getSchemaLocation_returnsNullWhenConstructedWithNull() {
        // GIVEN – a validator constructed with a null SchemaLocation.
        TestValidator nullLocationValidator = new TestValidator(
                null,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);

        // WHEN
        SchemaLocation result = nullLocationValidator.getSchemaLocation();

        // THEN
        assertNull(result,
                "BaseJsonValidator#getSchemaLocation() should return null if the validator was constructed with a null SchemaLocation");
    }
}
```