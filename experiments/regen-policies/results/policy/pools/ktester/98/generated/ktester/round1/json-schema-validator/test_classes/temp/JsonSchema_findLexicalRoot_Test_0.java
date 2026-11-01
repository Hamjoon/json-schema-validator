package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

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
        assertSame(schemaWithId, result, "When the schema has an id, findLexicalRoot() should return the schema itself");
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
        assertSame(parent, result, "When the child has no id but its parent does, findLexicalRoot() should return the parent");
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
        assertSame(root, result, "findLexicalRoot() should climb the parent chain and return the first ancestor with a non‑null id");
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
        assertSame(top, result, "If no ancestor has an id, findLexicalRoot() should return the topmost schema");
    }

    /**
     * The schema has a null {@code id} and no parent. The method should return
     * the schema itself because the loop breaks when {@code getParentSchema()}
     * is {@code null}.
     */
    @Test
    void findLexicalRoot_returnsSelfWhenIdNullAndNoParent() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(null);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When id is null and there is no parent, findLexicalRoot() should return the original instance");
    }

    /**
     * An empty string is considered non‑null, therefore the method must return
     * the original instance without traversing the parent chain.
     */
    @Test
    void findLexicalRoot_treatsEmptyStringIdAsNonNull() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("");
        // even if a parent exists, it should never be consulted
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn("parentId");
        when(schema.getParentSchema()).thenReturn(parent);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "An empty string id should be treated as non‑null; the method must return the schema itself");
    }

    /**
     * A deep chain where the first encountered non‑null id is at level 2
     * (even though higher levels also have ids). The method must stop at the
     * first non‑null id encountered while walking upwards.
     */
    @Test
    void findLexicalRoot_returnsFirstEncounteredIdInDeepChain() {
        // root (level4) – id null
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);
        // level3 – id "id3"
        JsonSchema level3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level3.getId()).thenReturn("id3");
        when(level3.getParentSchema()).thenReturn(root);
        // level2 – id "id2" (first non‑null when walking up from child)
        JsonSchema level2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level2.getId()).thenReturn("id2");
        when(level2.getParentSchema()).thenReturn(level3);
        // level1 – id null
        JsonSchema level1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level1.getId()).thenReturn(null);
        when(level1.getParentSchema()).thenReturn(level2);
        // child – id null
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(level1);
        JsonSchema result = child.findLexicalRoot();
        assertSame(level2, result, "The method should return the first ancestor with a non‑null id (level2) even though higher ancestors also have ids");
    }

    /**
     * Self‑referential parent with a non‑null id. The loop condition fails
     * immediately, so the method must return the original instance and must
     * not enter an infinite loop.
     */
    @Test
    void findLexicalRoot_selfReferentialParentWithIdReturnsSelf() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("selfId");
        // self‑reference
        when(schema.getParentSchema()).thenReturn(schema);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When the schema has a non‑null id, findLexicalRoot() should return itself even if getParentSchema() returns itself");
    }

    /**
     * Chain where only the middle ancestor possesses a non‑null id.
     * Expected result is that middle ancestor.
     */
    @Test
    void findLexicalRoot_returnsMiddleAncestorWithId() {
        // top – id null
        JsonSchema top = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(top.getId()).thenReturn(null);
        when(top.getParentSchema()).thenReturn(null);
        // middle – id "mid"
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.getId()).thenReturn("mid");
        when(middle.getParentSchema()).thenReturn(top);
        // child – id null
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(middle);
        JsonSchema result = child.findLexicalRoot();
        assertSame(middle, result, "The method should stop at the first ancestor (middle) that has a non‑null id");
    }

    /**
     * Chain of four schemas where the first non‑null id appears at level2,
     * while level3 also has a non‑null id. The method must stop at level2.
     */
    @Test
    void findLexicalRoot_stopsAtFirstNonNullIdEvenIfHigherAncestorsAlsoHaveIds() {
        // root – id null
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);
        // level3 – id "id3"
        JsonSchema level3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level3.getId()).thenReturn("id3");
        when(level3.getParentSchema()).thenReturn(root);
        // level2 – id "id2" (first non‑null)
        JsonSchema level2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level2.getId()).thenReturn("id2");
        when(level2.getParentSchema()).thenReturn(level3);
        // level1 – id null
        JsonSchema level1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level1.getId()).thenReturn(null);
        when(level1.getParentSchema()).thenReturn(level2);
        // child – id null
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(level1);
        JsonSchema result = child.findLexicalRoot();
        assertSame(level2, result, "The method should return the first encountered non‑null id (level2) even though level3 also has an id");
    }
}
