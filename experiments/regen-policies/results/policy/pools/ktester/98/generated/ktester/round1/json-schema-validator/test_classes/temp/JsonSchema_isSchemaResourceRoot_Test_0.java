package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Objects;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchema_isSchemaResourceRoot_Test {

    @Mock
    ValidationContext // not used directly but required for construction if needed
    validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    SchemaLocation parentSchemaLocation;

    @Mock
    AbsoluteIri // represents the IRI of a schema location
    absoluteIri;

    @Mock
    AbsoluteIri // a different IRI instance
    otherAbsoluteIri;

    // the instance under test
    JsonSchema schema;

    // possible parent schema
    JsonSchema parentSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real JsonSchema spy. The real constructor is not executed;
        // we rely on CALLS_REAL_METHODS to invoke the actual method logic while
        // allowing us to stub the dependent methods.
        schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        parentSchema = mock(JsonSchema.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        reset(validationContext, schemaLocation, parentSchemaLocation, absoluteIri, otherAbsoluteIri, schema, parentSchema);
    }

    /**
     * Provides combinations of schema‑IRI / parent‑IRI values and the expected
     * result when {@code getId()} is {@code null} and a parent schema exists.
     */
    private static Stream<Arguments> nullIriProvider() {
        return Stream.of(// both IRIs null  → Objects.equals(null, null) == true → false
        Arguments.of(null, null, false, "Both schema and parent IRI are null, expect false"), // schema IRI null, parent non‑null → Objects.equals(null, X) == false → true
        Arguments.of(null, "parentIri", true, "Schema IRI null, parent IRI non‑null, expect true"), // schema non‑null, parent null → Objects.equals(X, null) == false → true
        Arguments.of("schemaIri", null, true, "Schema IRI non‑null, parent IRI null, expect true"));
    }

    /**
     * Scenario 1: {@code getId()} returns a non‑null value.
     * The method should return {@code true} regardless of other state.
     */
    @Test
    void whenIdIsNotNull_thenTrue() {
        when(schema.getId()).thenReturn("someId");
        // other stubs are irrelevant but we set them for completeness
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when id is present");
    }

    /**
     * Scenario 2: No id and no parent schema.
     * Expected result is {@code true}.
     */
    @Test
    void whenNoIdAndNoParent_thenTrue() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(null);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when there is no id and no parent");
    }

    /**
     * Scenario 3: No id, a parent exists, but the absolute IRIs differ.
     * Expected result is {@code true}.
     */
    @Test
    void whenNoIdAndParentWithDifferentIri_thenTrue() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(otherAbsoluteIri);
        // Ensure the two IRIs are not equal
        assertNotEquals(schemaLocation.getAbsoluteIri(), parentSchemaLocation.getAbsoluteIri());
        assertTrue(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be true when IRIs differ");
    }

    /**
     * Scenario 4: No id, a parent exists, and the absolute IRIs are equal.
     * Expected result is {@code false}.
     */
    @Test
    void whenNoIdAndParentWithSameIri_thenFalse() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        // Objects.equals should evaluate to true for the same mock instance
        assertTrue(Objects.equals(schemaLocation.getAbsoluteIri(), parentSchemaLocation.getAbsoluteIri()));
        assertFalse(schema.isSchemaResourceRoot(), "isSchemaResourceRoot should be false when IRIs are the same and no id is set");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullIriProvider")
    void whenIdNullAndParentExists_withVariousIriValues(String schemaIri, String parentIri, boolean expectedResult, String description) {
        // common stubbing
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        // stub getAbsoluteIri() to return the supplied values (or null)
        when(schemaLocation.getAbsoluteIri()).thenReturn(schemaIri);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(parentIri);
        if (expectedResult) {
            assertTrue(schema.isSchemaResourceRoot(), "Expected true – " + description);
        } else {
            assertFalse(schema.isSchemaResourceRoot(), "Expected false – " + description);
        }
    }

    /**
     * Scenario: schema location is {@code null} while parent location is non‑null.
     * The method attempts to call {@code getAbsoluteIri()} on a {@code null}
     * reference and should throw {@link NullPointerException}.
     */
    @Test
    void whenSchemaLocationIsNull_thenNPE() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        // schema.getSchemaLocation() returns null
        when(schema.getSchemaLocation()).thenReturn(null);
        // parent schema location is present (non‑null)
        when(parentSchema.getSchemaLocation()).thenReturn(parentSchemaLocation);
        when(parentSchemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when schema's location is null");
    }

    /**
     * Scenario: parent schema location is {@code null} while schema location is non‑null.
     * Expected to throw {@link NullPointerException}.
     */
    @Test
    void whenParentLocationIsNull_thenNPE() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(schemaLocation);
        when(schemaLocation.getAbsoluteIri()).thenReturn(absoluteIri);
        // parent schema location is null
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when parent's location is null");
    }

    /**
     * Scenario: both schema and parent locations are {@code null}.
     * The first dereference of {@code getAbsoluteIri()} on the schema location
     * triggers a {@link NullPointerException}.
     */
    @Test
    void whenBothLocationsAreNull_thenNPE() {
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(parentSchema);
        when(schema.getSchemaLocation()).thenReturn(null);
        when(parentSchema.getSchemaLocation()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schema.isSchemaResourceRoot(), "Expected NPE when both locations are null");
    }
}
