```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Objects;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#isSchemaResourceRoot()}.
 *
 * The tests use Mockito spies/mocks to control the behaviour of the
 * {@code getId()}, {@code getParentSchema()} and {@code getSchemaLocation()}
 * methods without invoking any private members of {@link JsonSchema}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isSchemaResourceRoot_Test {

    @Mock
    ValidationContext validationContext; // not used directly but required for construction if needed

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    SchemaLocation parentSchemaLocation;

    @Mock
    AbsoluteIri absoluteIri; // represents the IRI of a schema location

    @Mock
    AbsoluteIri otherAbsoluteIri; // a different IRI instance

    JsonSchema schema;          // the instance under test
    JsonSchema parentSchema;    // possible parent schema

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
        // Reset mocks to avoid cross‑test interference.
        reset(validationContext, schemaLocation, parentSchemaLocation,
                absoluteIri, otherAbsoluteIri, schema, parentSchema);
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

        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when id is present");
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

        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when there is no id and no parent");
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

        assertTrue(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be true when IRIs differ");
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
        assertTrue(Objects.equals(schemaLocation.getAbsoluteIri(),
                parentSchemaLocation.getAbsoluteIri()));

        assertFalse(schema.isSchemaResourceRoot(),
                "isSchemaResourceRoot should be false when IRIs are the same and no id is set");
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global setup required for these tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }
}
```