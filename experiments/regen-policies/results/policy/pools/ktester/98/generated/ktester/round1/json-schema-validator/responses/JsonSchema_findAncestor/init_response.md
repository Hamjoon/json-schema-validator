```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#findAncestor()}.
 *
 * The tests use Mockito to create {@link JsonSchema} instances that call the real
 * {@code findAncestor()} implementation while stubbing {@code getParentSchema()}
 * to build a parent‑chain. This avoids the need to construct a full schema
 * hierarchy with all its heavy dependencies.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
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
     * A schema without a parent should return itself as the ancestor.
     */
    @Test
    void findAncestorWhenRoot() {
        // Create a mock that calls real methods for findAncestor()
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        // The root has no parent
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema result = root.findAncestor();

        assertSame(root, result, "Root schema should be its own ancestor");
    }

    /**
     * A child schema should resolve its ancestor to the root schema.
     */
    @Test
    void findAncestorWhenChild() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getParentSchema()).thenReturn(root);

        JsonSchema result = child.findAncestor();

        assertSame(root, result, "Ancestor of a direct child should be the root schema");
    }

    /**
     * A deeper hierarchy (grand‑child → child → root) should still resolve to the root.
     */
    @Test
    void findAncestorWhenGrandChild() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getParentSchema()).thenReturn(root);

        JsonSchema grandChild = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(grandChild.getParentSchema()).thenReturn(child);

        JsonSchema result = grandChild.findAncestor();

        assertSame(root, result, "Ancestor of a grand‑child should be the root schema");
    }
}
```