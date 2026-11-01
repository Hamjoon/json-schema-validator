package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.Set;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaLocation_Test {

    /**
     * The concrete validator used in the tests.
     */
    private TestValidator validator;

    /**
     * Mocks for the constructor arguments.
     */
    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    /**
     * Simple concrete subclass of {@link BaseJsonValidator}. It provides the
     * mandatory abstract {@code validate} method and a dummy {@code getKeyword}
     * implementation. No additional behaviour is required for the tests.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for these tests.
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return "test";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The mock ValidationContext is used only for the constructor; its
        // methods are never invoked because the constructor guards against null
        // config objects.
        validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
    }

    @Test
    void getSchemaLocation_returnsSameInstanceProvidedInConstructor() {
        // Act
        SchemaLocation returned = validator.getSchemaLocation();
        // Assert
        assertSame(mockSchemaLocation, returned, "BaseJsonValidator#getSchemaLocation should return the exact SchemaLocation instance supplied to the constructor");
    }

    @Test
    void getSchemaLocation_returnsNull_whenConstructorWasGivenNull() {
        // Arrange – create a validator with a null SchemaLocation
        TestValidator nullLocationValidator = new TestValidator(null, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // Act
        SchemaLocation returned = nullLocationValidator.getSchemaLocation();
        // Assert
        assertNull(returned, "BaseJsonValidator#getSchemaLocation should return null when the validator was constructed with a null SchemaLocation");
    }
}
