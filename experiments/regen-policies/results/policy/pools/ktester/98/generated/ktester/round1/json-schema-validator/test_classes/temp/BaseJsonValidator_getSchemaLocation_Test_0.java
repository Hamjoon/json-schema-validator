package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaLocation_Test {

    /**
     * The validator instance used in the tests.
     */
    private TestValidator validator;

    /**
     * Mocks required for the constructor of {@code BaseJsonValidator}.
     */
    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private AbsoluteIri mockAbsoluteIri;

    @Mock
    private JsonNodePath mockFragmentPath;

    /**
     * Minimal concrete subclass of {@code BaseJsonValidator}.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the current tests.
            return Collections.emptySet();
        }
    }

    /**
     * A mutable subclass of {@link SchemaLocation} used for mutation tests.
     */
    private static class MutableSchemaLocation extends SchemaLocation {

        private String uri;

        MutableSchemaLocation(AbsoluteIri iri, JsonNodePath fragmentPath) {
            super(iri, fragmentPath);
            this.uri = iri != null ? iri.toString() : null;
        }

        /**
         * Allows the test to change the stored URI.
         */
        void setUri(String newUri) {
            this.uri = newUri;
        }

        /**
         * Exposes the current URI for verification.
         */
        String getUri() {
            return this.uri;
        }
    }

    /**
     * A custom subclass of {@link SchemaLocation} that adds no extra behaviour – used to verify type preservation.
     */
    private static class MySchemaLocation extends SchemaLocation {

        MySchemaLocation(AbsoluteIri iri, JsonNodePath fragmentPath) {
            super(iri, fragmentPath);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a realistic SchemaLocation instance – the concrete classes are
        // simple value objects, so we can use real instances instead of mocks.
        SchemaLocation schemaLocation = new SchemaLocation(mockAbsoluteIri, mockFragmentPath);
        // Instantiate the concrete validator under test.
        validator = new TestValidator(schemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the reference to help the garbage collector and avoid accidental reuse.
        validator = null;
    }

    @Test
    void getSchemaLocation_returnsTheSameInstanceProvidedInConstructor() {
        // GIVEN – the validator has been constructed in @BeforeEach with a known
        // SchemaLocation instance.
        SchemaLocation expected = validator.getSchemaLocation();
        // WHEN
        SchemaLocation actual = validator.getSchemaLocation();
        // THEN
        assertSame(expected, actual, "BaseJsonValidator#getSchemaLocation() should return the exact instance supplied to the constructor");
    }

    @Test
    void getSchemaLocation_returnsNullWhenConstructedWithNull() {
        // GIVEN – a validator constructed with a null SchemaLocation.
        TestValidator nullLocationValidator = new TestValidator(null, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // WHEN
        SchemaLocation result = nullLocationValidator.getSchemaLocation();
        // THEN
        assertNull(result, "BaseJsonValidator#getSchemaLocation() should return null if the validator was constructed with a null SchemaLocation");
    }

    @Test
    void getSchemaLocation_returnsMockInstanceWhenMocked() {
        // GIVEN a pure Mockito mock of SchemaLocation
        SchemaLocation mockLocation = mock(SchemaLocation.class);
        TestValidator mockValidator = new TestValidator(mockLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // WHEN
        SchemaLocation actual = mockValidator.getSchemaLocation();
        // THEN
        assertSame(mockLocation, actual, "The getter must return the exact mock instance that was injected, without creating a new object");
    }

    @Test
    void getSchemaLocation_returnsSameInstanceOnMultipleCalls() {
        // GIVEN the validator from @BeforeEach (non‑null location)
        SchemaLocation firstCall = validator.getSchemaLocation();
        // WHEN
        SchemaLocation secondCall = validator.getSchemaLocation();
        // THEN
        assertSame(firstCall, secondCall, "Repeated calls to getSchemaLocation() must return the same reference (no new objects should be created)");
    }

    @Test
    void getSchemaLocation_mutableLocationReflectsChanges() {
        // GIVEN a mutable subclass of SchemaLocation
        MutableSchemaLocation mutableLocation = new MutableSchemaLocation(mockAbsoluteIri, mockFragmentPath);
        TestValidator mutableValidator = new TestValidator(mutableLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // WHEN – obtain the location and mutate it
        MutableSchemaLocation retrieved = (MutableSchemaLocation) mutableValidator.getSchemaLocation();
        retrieved.setUri("http://changed.example.com/schema");
        // THEN – a second call sees the mutated state
        MutableSchemaLocation afterMutation = (MutableSchemaLocation) mutableValidator.getSchemaLocation();
        assertEquals("http://changed.example.com/schema", afterMutation.getUri(), "Modifications performed on the returned mutable SchemaLocation must be visible on subsequent calls");
    }

    @Test
    void getSchemaLocation_returnsSubclassInstance() {
        // GIVEN a custom subclass of SchemaLocation
        MySchemaLocation myLocation = new MySchemaLocation(mockAbsoluteIri, mockFragmentPath);
        TestValidator subclassValidator = new TestValidator(myLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // WHEN
        SchemaLocation result = subclassValidator.getSchemaLocation();
        // THEN
        assertSame(myLocation, result, "The getter must preserve the concrete subclass type of the injected SchemaLocation");
        assertTrue(result instanceof MySchemaLocation, "Returned instance should still be of type MySchemaLocation");
    }

    @Test
    void getSchemaLocation_unchangedAfterValidation() {
        // GIVEN a validator with a concrete location
        SchemaLocation beforeValidation = validator.getSchemaLocation();
        // WHEN – execute a trivial validation (the stub returns an empty set)
        ExecutionContext mockExecutionContext = mock(ExecutionContext.class);
        JsonNode mockNode = mock(JsonNode.class);
        validator.validate(mockExecutionContext, mockNode);
        // THEN – the stored location must still be the original instance
        SchemaLocation afterValidation = validator.getSchemaLocation();
        assertSame(beforeValidation, afterValidation, "Validation should not alter the SchemaLocation stored inside the validator");
    }

    @Test
    void getSchemaLocation_spyInstanceNoUnexpectedInteractions() {
        // GIVEN a real SchemaLocation wrapped in a Mockito spy
        SchemaLocation realLocation = new SchemaLocation(mockAbsoluteIri, mockFragmentPath);
        SchemaLocation spyLocation = spy(realLocation);
        TestValidator spyValidator = new TestValidator(spyLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // Reset any interactions that might have happened during construction
        clearInvocations(spyLocation);
        // WHEN
        SchemaLocation result = spyValidator.getSchemaLocation();
        // THEN – the same spy is returned and no extra method calls were made
        assertSame(spyLocation, result, "The getter must return the exact spy instance that was injected");
        // Verify that the getter itself did not invoke any other method on the spy.
        verifyNoMoreInteractions(spyLocation);
    }
}
