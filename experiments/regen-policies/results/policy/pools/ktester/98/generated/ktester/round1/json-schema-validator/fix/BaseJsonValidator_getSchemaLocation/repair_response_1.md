```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#getSchemaLocation()}.
 */
class BaseJsonValidator_getSchemaLocation_Test {

    /**
     * The validator instance used in the tests.
     */
    private TestValidator validator;

    /**
     * Minimal concrete subclass of {@code BaseJsonValidator}.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * The {@code BaseJsonValidator} abstract class (in the version used for this
         * kata) defines a {@code validate} method with four parameters. The tests
         * only need a no‑op implementation, so we simply return an empty set.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath at) {
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

        /** Allows the test to change the stored URI. */
        void setUri(String newUri) {
            this.uri = newUri;
        }

        /** Exposes the current URI for verification. */
        String getUri() {
            return this.uri;
        }
    }

    /**
     * Simple concrete subclass of {@link SchemaLocation} – used as a “mock”.
     */
    private static class DummySchemaLocation extends SchemaLocation {
        DummySchemaLocation(AbsoluteIri iri, JsonNodePath fragmentPath) {
            super(iri, fragmentPath);
        }
    }

    /**
     * Custom subclass of {@link SchemaLocation} – used to verify type preservation.
     */
    private static class MySchemaLocation extends SchemaLocation {
        MySchemaLocation(AbsoluteIri iri, JsonNodePath fragmentPath) {
            super(iri, fragmentPath);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use nulls for the auxiliary constructor arguments – they are not used
        // by the getter logic we are testing.
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        validator = new TestValidator(
                schemaLocation, // evaluationPath
                null,          // schemaNode
                null,          // parentSchema
                null,          // validatorType
                null,          // validationContext
                null);         // (the sixth argument)
    }

    @AfterEach
    void teardownAfterEach() {
        validator = null;
    }

    @Test
    void getSchemaLocation_returnsTheSameInstanceProvidedInConstructor() {
        SchemaLocation expected = validator.getSchemaLocation();
        SchemaLocation actual = validator.getSchemaLocation();
        assertSame(expected, actual,
                "BaseJsonValidator#getSchemaLocation() should return the exact instance supplied to the constructor");
    }

    @Test
    void getSchemaLocation_returnsNullWhenConstructedWithNull() {
        TestValidator nullLocationValidator = new TestValidator(null, null, null, null, null, null);
        SchemaLocation result = nullLocationValidator.getSchemaLocation();
        assertNull(result,
                "BaseJsonValidator#getSchemaLocation() should return null if the validator was constructed with a null SchemaLocation");
    }

    @Test
    void getSchemaLocation_returnsMockInstanceWhenMocked() {
        // Instead of a Mockito mock we use a simple concrete subclass.
        DummySchemaLocation dummyLocation = new DummySchemaLocation(null, null);
        TestValidator dummyValidator = new TestValidator(dummyLocation, null, null, null, null, null);
        SchemaLocation actual = dummyValidator.getSchemaLocation();
        assertSame(dummyLocation, actual,
                "The getter must return the exact instance that was injected, without creating a new object");
    }

    @Test
    void getSchemaLocation_returnsSameInstanceOnMultipleCalls() {
        SchemaLocation firstCall = validator.getSchemaLocation();
        SchemaLocation secondCall = validator.getSchemaLocation();
        assertSame(firstCall, secondCall,
                "Repeated calls to getSchemaLocation() must return the same reference (no new objects should be created)");
    }

    @Test
    void getSchemaLocation_mutableLocationReflectsChanges() {
        MutableSchemaLocation mutableLocation = new MutableSchemaLocation(null, null);
        TestValidator mutableValidator = new TestValidator(mutableLocation, null, null, null, null, null);
        MutableSchemaLocation retrieved = (MutableSchemaLocation) mutableValidator.getSchemaLocation();
        retrieved.setUri("http://changed.example.com/schema");
        MutableSchemaLocation afterMutation = (MutableSchemaLocation) mutableValidator.getSchemaLocation();
        assertEquals("http://changed.example.com/schema", afterMutation.getUri(),
                "Modifications performed on the returned mutable SchemaLocation must be visible on subsequent calls");
    }

    @Test
    void getSchemaLocation_returnsSubclassInstance() {
        MySchemaLocation myLocation = new MySchemaLocation(null, null);
        TestValidator subclassValidator = new TestValidator(myLocation, null, null, null, null, null);
        SchemaLocation result = subclassValidator.getSchemaLocation();
        assertSame(myLocation, result,
                "The getter must preserve the concrete subclass type of the injected SchemaLocation");
        assertTrue(result instanceof MySchemaLocation,
                "Returned instance should still be of type MySchemaLocation");
    }

    @Test
    void getSchemaLocation_unchangedAfterValidation() {
        SchemaLocation beforeValidation = validator.getSchemaLocation();
        // Validation is a no‑op that returns an empty set; we can safely pass nulls.
        validator.validate(null, null, null, null);
        SchemaLocation afterValidation = validator.getSchemaLocation();
        assertSame(beforeValidation, afterValidation,
                "Validation should not alter the SchemaLocation stored inside the validator");
    }
}
```