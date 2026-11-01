```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#isSchemaResourceRoot()}.
 *
 * The method logic is:
 * <pre>
 *   if (getId() != null)               → true
 *   if (getParentSchema() == null)     → true
 *   return !Objects.equals(getSchemaLocation().getAbsoluteIri(),
 *                         getParentSchema().getSchemaLocation().getAbsoluteIri());
 * </pre>
 *
 * The tests cover all branches using Mockito spies that call the real method
 * while stubbing the dependent getters.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isSchemaResourceRoot_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
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

        assertTrue(schema.isSchemaResourceRoot(),
                "Schema with a non‑null id should be considered a resource root");
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

        assertTrue(schema.isSchemaResourceRoot(),
                "Schema with null id and no parent should be a resource root");
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
        assertTrue(child.isSchemaResourceRoot(),
                "Schema with different absolute IRIs from its parent should be a resource root");
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

        assertFalse(child.isSchemaResourceRoot(),
                "Schema with the same absolute IRI as its parent should not be a resource root");
    }
}
```