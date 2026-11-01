package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationPath_Test {

    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation schemaLocationMock;

    @Mock
    private JsonNodePath evaluationPathMock;

    @Mock
    private JsonNode schemaNodeMock;

    @Mock
    private JsonSchema parentSchemaMock;

    @Mock
    private ValidatorTypeCode validatorTypeMock;

    @Mock
    private ValidationContext validationContextMock;

    // -----------------------------------------------------------------------
    private TestValidator validator;

    @Mock
    private JsonNodePath // second path instance (for reassignment)
    otherEvaluationPathMock;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     * Only the abstract {@link #validate(ExecutionContext, JsonNode)} method
     * and {@link #getKeyword()} are implemented because they are irrelevant for
     * the {@code getEvaluationPath()} tests.
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
            return "testKeyword";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh validator instance for each test using a non‑null path.
        validator = new TestValidator(schemaLocationMock, evaluationPathMock, schemaNodeMock, parentSchemaMock, validatorTypeMock, validationContextMock);
    }

    // -----------------------------------------------------------------------
    @Test
    void getEvaluationPath_returnsPathProvidedInConstructor() {
        // Act
        JsonNodePath result = validator.getEvaluationPath();
        // Assert
        assertSame(evaluationPathMock, result, "getEvaluationPath() should return the exact JsonNodePath instance supplied to the constructor");
    }

    @Test
    void getEvaluationPath_isConsistentAcrossMultipleCalls() {
        // Act
        JsonNodePath firstCall = validator.getEvaluationPath();
        JsonNodePath secondCall = validator.getEvaluationPath();
        // Assert
        assertSame(firstCall, secondCall, "Multiple invocations BaseJsonValidator_getEvaluationPath_TestgetEvaluationPath() must return the same instance");
    }

    @Test
    void getEvaluationPath_returnsNullWhenFieldNotInitialised() {
        // Arrange: construct a validator with a null evaluationPath.
        TestValidator nullPathValidator = new TestValidator(schemaLocationMock, // explicit null
        null, schemaNodeMock, parentSchemaMock, validatorTypeMock, validationContextMock);
        // Act
        JsonNodePath result = nullPathValidator.getEvaluationPath();
        // Assert
        assertNull(result, "When the internal evaluationPath field is null, getEvaluationPath() should return null");
    }

    @Test
    void getEvaluationPath_reflectivelyReassigned_returnsNewInstance() throws Exception {
        // Arrange: ensure the validator currently returns the original mock.
        assertSame(evaluationPathMock, validator.getEvaluationPath(), "Pre‑condition: the validator should initially hold evaluationPathMock");
        // Use reflection to replace the protected field in ValidationMessageHandler.
        Field field = ValidationMessageHandler.class.getDeclaredField("evaluationPath");
        field.setAccessible(true);
        field.set(validator, otherEvaluationPathMock);
        // Act
        JsonNodePath result = validator.getEvaluationPath();
        // Assert
        assertSame(otherEvaluationPathMock, result, "After reflective reassignment, getEvaluationPath() must return the newly assigned JsonNodePath instance");
    }

    @Test
    void getEvaluationPath_independentInstancesMaintainSeparatePaths() {
        // Arrange: create a second validator with a distinct path.
        JsonNodePath secondPathMock = mock(JsonNodePath.class);
        TestValidator secondValidator = new TestValidator(schemaLocationMock, secondPathMock, schemaNodeMock, parentSchemaMock, validatorTypeMock, validationContextMock);
        // Act
        JsonNodePath firstResult = validator.getEvaluationPath();
        JsonNodePath secondResult = secondValidator.getEvaluationPath();
        // Assert
        assertAll(() -> assertSame(evaluationPathMock, firstResult, "First validator should return its own evaluationPath instance"), () -> assertSame(secondPathMock, secondResult, "Second validator should return its own evaluationPath instance"), () -> assertNotSame(firstResult, secondResult, "Different validator instances must not share the same evaluationPath reference"));
    }
}
