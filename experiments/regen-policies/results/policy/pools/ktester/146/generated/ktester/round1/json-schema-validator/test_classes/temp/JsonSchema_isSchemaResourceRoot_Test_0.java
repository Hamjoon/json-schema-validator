package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Objects;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_isSchemaResourceRoot_Test {

    /**
     * Supplies the edge‑case arguments.
     */
    private static Stream<Arguments> edgeCaseProvider() {
        // Helper mocks for non‑null absolute IRIs
        SchemaLocation locWithIri = mock(SchemaLocation.class);
        AbsoluteIri someIri = mock(AbsoluteIri.class);
        when(locWithIri.getAbsoluteIri()).thenReturn(someIri);
        // Both locations null → Objects.equals(null,null) == true → result false
        Arguments bothNull = Arguments.of("Both schemaLocation and parentSchemaLocation are null → result false", // id
        null, // child location
        null, // parent location
        null, false);
        // Child null, parent non‑null → Objects.equals(null, nonNull) == false → result true
        Arguments childNullParentNonNull = Arguments.of("Child location null, parent location non‑null → result true", null, null, locWithIri, true);
        // Child non‑null, parent null → Objects.equals(nonNull, null) == false → result true
        Arguments childNonNullParentNull = Arguments.of("Child location non‑null, parent location null → result true", null, locWithIri, null, true);
        // Empty string id (non‑null) → should short‑circuit to true
        Arguments emptyId = Arguments.of("Empty string id is non‑null → result true", // empty but non‑null id
        "", null, null, true);
        return Stream.of(bothNull, childNullParentNonNull, childNonNullParentNull, emptyId);
    }

    /**
     * When the schema has a non‑null {@code id}, the method must return {@code true}
     * regardless of the parent schema.
     */
    @Test
    void whenIdIsNotNull_thenRootIsTrue() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("someId");
        // parent and locations are irrelevant for this branch
        when(schema.getParentSchema()).thenReturn(null);
        when(schema.getSchemaLocation()).thenReturn(null);
        assertTrue(schema.isSchemaResourceRoot(), "Schema with a non‑null id should be considered a resource root");
    }

    /**
     * When the schema has a {@code null} id and no parent, it is a root.
     */
    @Test
    void whenIdIsNullAndParentIsNull_thenRootIsTrue() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(null);
        when(schema.getSchemaLocation()).thenReturn(null);
        assertTrue(schema.isSchemaResourceRoot(), "Schema with null id and no parent should be a resource root");
    }

    /**
     * When the schema has a {@code null} id, a parent exists and the absolute IRIs are
     * different, the method must return {@code true}.
     */
    @Test
    void whenIdIsNullAndParentExistsAndIriDiffers_thenRootIsTrue() {
        // child schema
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        // parent schema
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn(null);
        // mock locations and IRIs
        SchemaLocation childLoc = mock(SchemaLocation.class);
        SchemaLocation parentLoc = mock(SchemaLocation.class);
        AbsoluteIri childIri = mock(AbsoluteIri.class);
        AbsoluteIri parentIri = mock(AbsoluteIri.class);
        when(childLoc.getAbsoluteIri()).thenReturn(childIri);
        when(parentLoc.getAbsoluteIri()).thenReturn(parentIri);
        when(child.getSchemaLocation()).thenReturn(childLoc);
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        when(child.getParentSchema()).thenReturn(parent);
        // childIri and parentIri are different objects → not equal
        assertTrue(child.isSchemaResourceRoot(), "Schema with different absolute IRIs from its parent should be a resource root");
    }

    /**
     * When the schema has a {@code null} id, a parent exists and the absolute IRIs are
     * the same, the method must return {@code false}.
     */
    @Test
    void whenIdIsNullAndParentExistsAndIriSame_thenRootIsFalse() {
        // child schema
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        // parent schema
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn(null);
        // mock locations and a shared IRI instance
        SchemaLocation childLoc = mock(SchemaLocation.class);
        SchemaLocation parentLoc = mock(SchemaLocation.class);
        AbsoluteIri sharedIri = mock(AbsoluteIri.class);
        when(childLoc.getAbsoluteIri()).thenReturn(sharedIri);
        when(parentLoc.getAbsoluteIri()).thenReturn(sharedIri);
        when(child.getSchemaLocation()).thenReturn(childLoc);
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        when(child.getParentSchema()).thenReturn(parent);
        assertFalse(child.isSchemaResourceRoot(), "Schema with the same absolute IRI as its parent should not be a resource root");
    }

    /**
     * Parameterized edge‑case scenarios.
     *
     * @param description   human‑readable description for the test case
     * @param id            value returned by {@code getId()}
     * @param childLoc      {@code SchemaLocation} for the child (may be {@code null})
     * @param parentLoc     {@code SchemaLocation} for the parent (may be {@code null})
     * @param expected      expected result of {@code isSchemaResourceRoot()}
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseProvider")
    void edgeCases(String description, String id, SchemaLocation childLoc, SchemaLocation parentLoc, boolean expected) {
        // child schema (the one under test)
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(id);
        // parent schema (may be null – but all edge cases in this group have a parent)
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        // parent id irrelevant for edge cases
        when(parent.getId()).thenReturn(null);
        when(parent.getSchemaLocation()).thenReturn(parentLoc);
        when(child.getParentSchema()).thenReturn(parent);
        when(child.getSchemaLocation()).thenReturn(childLoc);
        boolean actual = child.isSchemaResourceRoot();
        assertEquals(expected, actual, () -> "Edge case failed: " + description);
    }

    /**
     * When {@code getSchemaLocation()} returns {@code null} while a parent exists,
     * the method dereferences a {@code null} and throws {@link NullPointerException}.
     */
    @Test
    void whenIdNullParentExistsAndChildLocationNull_thenThrowsNPE() {
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        // will cause NPE
        when(child.getSchemaLocation()).thenReturn(null);
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getSchemaLocation()).thenReturn(mock(SchemaLocation.class));
        when(child.getParentSchema()).thenReturn(parent);
        assertThrows(NullPointerException.class, child::isSchemaResourceRoot, "Expected NPE when child schemaLocation is null and parent is present");
    }

    /**
     * When the parent schema's {@code getSchemaLocation()} returns {@code null}
     * while the child schema's location is non‑null, dereferencing the parent causes
     * a {@link NullPointerException}.
     */
    @Test
    void whenIdNullParentExistsAndParentLocationNull_thenThrowsNPE() {
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        SchemaLocation childLoc = mock(SchemaLocation.class);
        when(childLoc.getAbsoluteIri()).thenReturn(mock(AbsoluteIri.class));
        when(child.getSchemaLocation()).thenReturn(childLoc);
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        // will cause NPE
        when(parent.getSchemaLocation()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(parent);
        assertThrows(NullPointerException.class, child::isSchemaResourceRoot, "Expected NPE when parent schemaLocation is null and child is present");
    }
}
