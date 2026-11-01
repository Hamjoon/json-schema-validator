```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findSchemaResourceRoot_Test {

    /**
     * The root schema – the one that reports {@code isSchemaResourceRoot()} as true.
     */
    private JsonSchema root;

    /**
     * An intermediate schema whose parent is {@code root}.
     */
    private JsonSchema middle;

    /**
     * The leaf schema whose parent is {@code middle}.
     */
    private JsonSchema leaf;

    @BeforeEach
    void setupBeforeEach() {
        // Create real‑method mocks for JsonSchema so that the actual
        // implementation of findSchemaResourceRoot() is executed,
        // while we can stub the methods it depends on.
        root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);

        middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        when(middle.getParentSchema()).thenReturn(root);

        leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(middle);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(root, middle, leaf);
    }

    @Test
    void shouldReturnRootWhenCalledOnLeaf() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        when(middle.getParentSchema()).thenReturn(root);

        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(middle);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(root, result,
                "The root schema should be returned when starting from the leaf");
    }

    @Test
    void shouldReturnSelfWhenAlreadyRoot() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema result = root.findSchemaResourceRoot();
        assertSame(root, result,
                "When the schema itself is the resource root, it should be returned unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Null parent causes NullPointerException")
    void shouldThrowNPEWhenParentIsNullAndNoRoot() {
        JsonSchema orphan = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(orphan.isSchemaResourceRoot()).thenReturn(false);
        when(orphan.getParentSchema()).thenReturn(null);

        assertThrows(NullPointerException.class,
                orphan::findSchemaResourceRoot,
                "A null parent while searching for a resource root should result in NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Stops at the nearest true ancestor even if higher ancestors are also true")
    void shouldReturnNearestTrueAncestor() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.isSchemaResourceRoot()).thenReturn(true);
        when(middle.getParentSchema()).thenReturn(root);

        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(middle);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(middle, result,
                "The method should stop at the first true ancestor (the direct parent)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Finds root in a deep hierarchy (depth 5)")
    void shouldFindRootInDeepHierarchy() {
        JsonSchema deepRoot = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(deepRoot.isSchemaResourceRoot()).thenReturn(true);
        when(deepRoot.getParentSchema()).thenReturn(null);

        JsonSchema l4 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l4.isSchemaResourceRoot()).thenReturn(false);
        when(l4.getParentSchema()).thenReturn(deepRoot);

        JsonSchema l3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l3.isSchemaResourceRoot()).thenReturn(false);
        when(l3.getParentSchema()).thenReturn(l4);

        JsonSchema l2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l2.isSchemaResourceRoot()).thenReturn(false);
        when(l2.getParentSchema()).thenReturn(l3);

        JsonSchema l1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l1.isSchemaResourceRoot()).thenReturn(false);
        when(l1.getParentSchema()).thenReturn(l2);

        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(l1);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(deepRoot, result,
                "The method should correctly walk five levels and return the deep root");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Self‑referencing schema leads to StackOverflowError")
    void shouldOverflowWhenSelfReferencing() {
        JsonSchema self = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(self.isSchemaResourceRoot()).thenReturn(false);
        // self‑reference
        when(self.getParentSchema()).thenReturn(self);

        assertThrows(StackOverflowError.class,
                self::findSchemaResourceRoot,
                "A self‑referencing parent without a true root should eventually cause StackOverflowError");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by isSchemaResourceRoot propagates")
    void shouldPropagateExceptionFromIsSchemaResourceRoot() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema broken = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(broken.isSchemaResourceRoot()).thenThrow(new IllegalStateException("boom"));
        when(broken.getParentSchema()).thenReturn(root);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                broken::findSchemaResourceRoot,
                "The IllegalStateException from isSchemaResourceRoot should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by getParentSchema propagates")
    void shouldPropagateExceptionFromGetParentSchema() {
        JsonSchema broken = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(broken.isSchemaResourceRoot()).thenReturn(false);
        when(broken.getParentSchema()).thenThrow(new UnsupportedOperationException("no parent"));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                broken::findSchemaResourceRoot,
                "The UnsupportedOperationException from getParentSchema should propagate");
        assertEquals("no parent", ex.getMessage());
    }
}
```