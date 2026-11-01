package com.networknt.schema;

import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;

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

    class StubPath extends JsonNodePath {

        private boolean accessed = false;

        StubPath() {
            super(JsonNodePath.PathType.ROOT);
        }

        @Override
        public String toString() {
            accessed = true;
            return super.toString();
        }

        boolean wasAccessed() {
            return accessed;
        }
    }

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}
     * used only for exercising the {@code getEvaluationPath()} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The abstract {@code validate} method – not needed for these tests.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return Collections.emptySet();
        }
    }

    class StubPath extends JsonNodePath {

        private boolean accessed = false;

        StubPath() {
            super(JsonNodePath.PathType.ROOT);
        }

        @Override
        public String toString() {
            accessed = true;
            return super.toString();
        }

        boolean wasAccessed() {
            return accessed;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real instance of the abstract class using CALLS_REAL_METHODS.
        // This allows us to call the concrete getEvaluationPath() implementation
        // while keeping abstract methods unimplemented (they will not be invoked).
        validator = mock(BaseJsonValidator.class, withSettings().useConstructor(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        // Use a simple non‑null path for the default validator instance.
        JsonNodePath path = new JsonNodePath();
        validator = new TestValidator(null, path, null, null, null, null);
        // Use a simple, non‑null root path for the default validator instance.
        JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.ROOT);
        validator = new TestValidator(null, path, null, null, null, null);
    }

    @Test
    void getEvaluationPath_ShouldReturnConstructorProvidedPath() {
        JsonNodePath expected = validator.getEvaluationPath();
        JsonNodePath result = validator.getEvaluationPath();
        assertSame(expected, result, "The evaluation path returned by getEvaluationPath() must be the same instance supplied to the constructor");
    }

    @Test
    void getEvaluationPath_ShouldNeverBeNullWhenConstructedWithNonNullPath() {
        JsonNodePath result = validator.getEvaluationPath();
        assertNotNull(result, "Evaluation path should not be null when a non‑null path is given to the constructor");
    }

    // Additional sanity test: when the constructor receives a different mock, the getter reflects it.
    @Test
    void getEvaluationPath_DifferentInstance_ShouldReflectNewPath() {
        JsonNodePath anotherPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        TestValidator anotherValidator = new TestValidator(null, anotherPath, null, null, null, null);
        JsonNodePath result = anotherValidator.getEvaluationPath();
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
        // Force the protected field defined in ValidationMessageHandler to null via reflection.
        java.lang.reflect.Field field = ValidationMessageHandler.class.getDeclaredField("evaluationPath");
        field.setAccessible(true);
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
        JsonNodePath pathA = new JsonNodePath(JsonNodePath.PathType.ROOT);
        JsonNodePath pathB = new JsonNodePath(JsonNodePath.PathType.ROOT);
        TestValidator validatorA = new TestValidator(null, pathA, null, null, null, null);
        TestValidator validatorB = new TestValidator(null, pathB, null, null, null, null);
        assertNotSame(validatorA.getEvaluationPath(), validatorB.getEvaluationPath(), "Each validator instance must retain its own JsonNodePath object");
    }

    /**
     * Verify that a complex (realistic) path object is returned unchanged.
     * We use a mock that returns a known string from {@code toString()} to
     * simulate a JSON Pointer such as \"#/properties/address/properties/street\".
     */
    @Test
    void getEvaluationPath_ComplexPathContent_ShouldMatchExpectedString() {
        final String expectedPointer = "#/properties/address/properties/street";
        // Create a JsonNodePath subclass whose toString() returns the expected pointer.
        JsonNodePath complexPath = new JsonNodePath(JsonNodePath.PathType.ROOT) {

            @Override
            public String toString() {
                return expectedPointer;
            }
        };
        TestValidator complexValidator = new TestValidator(null, complexPath, null, null, null, null);
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
        // Instead of a Mockito mock, we use a simple stub that records method calls.
        class StubPath extends JsonNodePath {

            private boolean accessed = false;

            StubPath() {
                super(JsonNodePath.PathType.ROOT);
            }

            @Override
            public String toString() {
                accessed = true;
                return super.toString();
            }

            boolean wasAccessed() {
                return accessed;
            }
        }
        StubPath stubPath = new StubPath();
        TestValidator stubValidator = new TestValidator(null, stubPath, null, null, null, null);
        // Act
        JsonNodePath result = stubValidator.getEvaluationPath();
        // Assert
        assertSame(stubPath, result, "Getter must return the same stub instance supplied to the constructor");
        // Ensure that calling the getter itself does not trigger any other method on the path.
        assertFalse(stubPath.wasAccessed(), "Getter should not invoke methods (e.g., toString) on the path object");
    }
}
