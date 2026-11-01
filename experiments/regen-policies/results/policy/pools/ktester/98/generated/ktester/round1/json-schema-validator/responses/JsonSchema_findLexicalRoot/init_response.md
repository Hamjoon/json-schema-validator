```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    /***  No global resources are required for these tests, the setup methods are kept
     *   for completeness and future extensions. ***/
    @BeforeAll
    static void setupBeforeAll() {
        // nothing to initialise globally
    }

    @BeforeEach
    void setupBeforeEach() {
        // nothing to initialise per test
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up per test
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up globally
    }

    /**
     * When the schema itself has a non‑null {@code id}, {@code findLexicalRoot()}
     * must return the same instance.
     */
    @Test
    void findLexicalRoot_returnsSelfWhenIdPresent() {
        // Create a mock that calls real methods for findLexicalRoot()
        JsonSchema schemaWithId = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaWithId.getId()).thenReturn("http://example.com/root");
        when(schemaWithId.getParentSchema()).thenReturn(null);

        JsonSchema result = schemaWithId.findLexicalRoot();

        assertSame(schemaWithId, result,
                "When the schema has an id, findLexicalRoot() should return the schema itself");
    }

    /**
     * When the current schema has no {@code id} but its direct parent does,
     * the method should return the parent.
     */
    @Test
    void findLexicalRoot_returnsParentWhenParentHasId() {
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn("http://example.com/parent");
        when(parent.getParentSchema()).thenReturn(null);

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(parent);

        JsonSchema result = child.findLexicalRoot();

        assertSame(parent, result,
                "When the child has no id but its parent does, findLexicalRoot() should return the parent");
    }

    /**
     * When both the current schema and its immediate parent lack an {@code id},
     * the method must walk up the chain until it finds an ancestor with a non‑null
     * {@code id}.
     */
    @Test
    void findLexicalRoot_walksUpToAncestorWithId() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn("http://example.com/root");
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema intermediate = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(intermediate.getId()).thenReturn(null);
        when(intermediate.getParentSchema()).thenReturn(root);

        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.getId()).thenReturn(null);
        when(leaf.getParentSchema()).thenReturn(intermediate);

        JsonSchema result = leaf.findLexicalRoot();

        assertSame(root, result,
                "findLexicalRoot() should climb the parent chain and return the first ancestor with a non‑null id");
    }

    /**
     * When none of the ancestors (including the topmost one) have an {@code id},
     * the method should return the topmost schema (the one whose parent is {@code null}).
     */
    @Test
    void findLexicalRoot_returnsTopmostWhenNoIdFound() {
        JsonSchema top = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(top.getId()).thenReturn(null);
        when(top.getParentSchema()).thenReturn(null);

        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.getId()).thenReturn(null);
        when(middle.getParentSchema()).thenReturn(top);

        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.getId()).thenReturn(null);
        when(leaf.getParentSchema()).thenReturn(middle);

        JsonSchema result = leaf.findLexicalRoot();

        assertSame(top, result,
                "If no ancestor has an id, findLexicalRoot() should return the topmost schema");
    }
}
```