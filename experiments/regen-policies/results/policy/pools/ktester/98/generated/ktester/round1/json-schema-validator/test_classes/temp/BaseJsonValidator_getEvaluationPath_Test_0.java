package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.Set;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationPath_Test {

    // Mocks for constructor dependencies
    @Mock
    SchemaLocation mockSchemaLocation;

    @Mock
    JsonNodePath mockEvaluationPath;

    @Mock
    JsonNode mockSchemaNode;

    @Mock
    JsonSchema mockParentSchema;

    @Mock
    ValidatorTypeCode mockValidatorType;

    @Mock
    ValidationContext mockValidationContext;

    // Instance under test
    BaseJsonValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real instance of the abstract class using CALLS_REAL_METHODS.
        // This allows us to call the concrete getEvaluationPath() implementation
        // while keeping abstract methods unimplemented (they will not be invoked).
        validator = mock(BaseJsonValidator.class, withSettings().useConstructor(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext).defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    void getEvaluationPath_ShouldReturnConstructorProvidedPath() {
        // Act
        JsonNodePath result = validator.getEvaluationPath();
        // Assert
        assertSame(mockEvaluationPath, result, "The evaluation path returned by getEvaluationPath() must be the same instance supplied to the constructor");
    }

    @Test
    void getEvaluationPath_ShouldNeverBeNullWhenConstructedWithNonNullPath() {
        // Act
        JsonNodePath result = validator.getEvaluationPath();
        // Assert
        assertNotNull(result, "Evaluation path should not be null when a non‑null path is given to the constructor");
    }

    // Additional sanity test: when the constructor receives a different mock, the getter reflects it.
    @Test
    void getEvaluationPath_DifferentInstance_ShouldReflectNewPath() {
        // Arrange – create a second mock path
        JsonNodePath anotherPath = mock(JsonNodePath.class);
        BaseJsonValidator anotherValidator = mock(BaseJsonValidator.class, withSettings().useConstructor(mockSchemaLocation, anotherPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        // Act
        JsonNodePath result = anotherValidator.getEvaluationPath();
        // Assert
        assertSame(anotherPath, result, "The getter must return the exact path instance supplied during construction");
    }

    /**
     * Verify that the getter returns {@code null} when the internal
     * {@code evaluationPath} field has been cleared via reflection.
     *
     * This simulates a validator that was instantiated without a path
     * (or where the path was later removed).  The method must simply
     * return the current field value – i.e. {@code null}.
     */
    @Test
    void getEvaluationPath_ShouldReturnNullWhenFieldIsNull() throws Exception {
        // Use reflection to set the protected field defined in ValidationMessageHandler
        Field field = ValidationMessageHandler.class.getDeclaredField("evaluationPath");
        field.setAccessible(true);
        // force the internal path to null
        field.set(validator, null);
        JsonNodePath result = validator.getEvaluationPath();
        assertNull(result, "When the internal evaluationPath field is null, the getter must return null");
    }

    /**
     * The evaluation path must be the same object on repeated calls.
     * This ensures the getter does not create a new instance each time.
     */
    @Test
    void getEvaluationPath_ShouldReturnSameInstanceOnRepeatedCalls() {
        JsonNodePath firstCall = validator.getEvaluationPath();
        JsonNodePath secondCall = validator.getEvaluationPath();
        assertSame(firstCall, secondCall, "Multiple invocations of getEvaluationPath() must return the identical object (reference equality)");
    }

    /**
     * Two distinct validator instances must each hold their own
     * {@link JsonNodePath}.  The paths must not be shared statically.
     */
    @Test
    void getEvaluationPath_DistinctValidators_ShouldHoldDifferentPaths() {
        JsonNodePath pathA = mock(JsonNodePath.class);
        JsonNodePath pathB = mock(JsonNodePath.class);
        BaseJsonValidator validatorA = mock(BaseJsonValidator.class, withSettings().useConstructor(mockSchemaLocation, pathA, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        BaseJsonValidator validatorB = mock(BaseJsonValidator.class, withSettings().useConstructor(mockSchemaLocation, pathB, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        assertNotSame(validatorA.getEvaluationPath(), validatorB.getEvaluationPath(), "Each validator instance must retain its own JsonNodePath object");
    }

    /**
     * Verify that a complex (realistic) path object is returned unchanged.
     * We use a mock that returns a known string from {@code toString()} to
     * simulate a JSON Pointer such as \"#/properties/address/properties/street\".
     */
    @Test
    void getEvaluationPath_ComplexPathContent_ShouldMatchExpectedString() {
        String expectedPointer = "#/properties/address/properties/street";
        JsonNodePath complexPath = mock(JsonNodePath.class);
        when(complexPath.toString()).thenReturn(expectedPointer);
        BaseJsonValidator complexValidator = mock(BaseJsonValidator.class, withSettings().useConstructor(mockSchemaLocation, complexPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        JsonNodePath result = complexValidator.getEvaluationPath();
        assertSame(complexPath, result, "The getter must return the exact path instance supplied");
        assertEquals(expectedPointer, result.toString(), "The path object's string representation should match the expected JSON Pointer");
    }

    /**
     * Ensure that a mock {@link JsonNodePath} injected via the constructor
     * is returned without any side‑effects.  No additional interactions
     * with the mock should occur during the getter call.
     */
    @Test
    void getEvaluationPath_MockPathObject_NoInteractions() {
        JsonNodePath mockPath = mock(JsonNodePath.class);
        BaseJsonValidator mockPathValidator = mock(BaseJsonValidator.class, withSettings().useConstructor(mockSchemaLocation, mockPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        // Act
        JsonNodePath result = mockPathValidator.getEvaluationPath();
        // Assert that the exact mock instance is returned
        assertSame(mockPath, result, "Getter must return the same mock instance supplied to the constructor");
        // Verify that the getter did not invoke any other method on the mock
        verifyNoInteractions(mockPath);
    }
}
