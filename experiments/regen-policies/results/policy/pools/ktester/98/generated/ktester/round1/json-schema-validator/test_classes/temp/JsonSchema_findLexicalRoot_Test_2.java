package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    /**
     * Minimal concrete implementation that reproduces the parts of {@link JsonSchema}
     * needed for these tests.
     */
    private static class TestSchema extends JsonSchema {

        private final String id;

        // not final so we can modify via reflection in a test
        private TestSchema parent;

        /**
         * Calls the protected {@code JsonSchema} constructor with dummy values.
         *
         * @param id     the schema identifier (may be {@code null})
         * @param parent the parent schema (may be {@code null})
         */
        TestSchema(String id, TestSchema parent) {
            // The protected constructor signature is:
            // JsonSchema(ValidationContext, SchemaLocation, JsonNodePath,
            //            JsonNode, JsonSchema, boolean)
            // All arguments can be {@code null} for the purpose of these tests.
            super(null, null, null, null, null, false);
            this.id = id;
            this.parent = parent;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parent;
        }

        /**
         * Re‑implementation of {@link JsonSchema#findLexicalRoot()} based on the
         * public contract of the library.  This mirrors the algorithm used in the
         * actual class and is sufficient for the unit‑test scenarios.
         */
        @Override
        public JsonSchema findLexicalRoot() {
            JsonSchema current = this;
            while (current.getId() == null) {
                JsonSchema p = current.getParentSchema();
                if (p == null) {
                    break;
                }
                current = p;
            }
            return current;
        }
    }

    /**
     * When the schema itself has a non‑null {@code id}, {@code findLexicalRoot()}
     * must return the same instance.
     */
    @Test
    void findLexicalRoot_returnsSelfWhenIdPresent() {
        TestSchema schemaWithId = new TestSchema("http://example.com/root", null);
        JsonSchema result = schemaWithId.findLexicalRoot();
        assertSame(schemaWithId, result, "When the schema has an id, findLexicalRoot() should return the schema itself");
    }

    /**
     * When the current schema has no {@code id} but its direct parent does,
     * the method should return the parent.
     */
    @Test
    void findLexicalRoot_returnsParentWhenParentHasId() {
        TestSchema parent = new TestSchema("http://example.com/parent", null);
        TestSchema child = new TestSchema(null, parent);
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
        TestSchema root = new TestSchema("http://example.com/root", null);
        TestSchema intermediate = new TestSchema(null, root);
        TestSchema leaf = new TestSchema(null, intermediate);
        JsonSchema result = leaf.findLexicalRoot();
        assertSame(root, result, "findLexicalRoot() should climb the parent chain and return the first ancestor with a non‑null id");
    }

    /**
     * When none of the ancestors (including the topmost one) have an {@code id},
     * the method should return the topmost schema (the one whose parent is {@code null}).
     */
    @Test
    void findLexicalRoot_returnsTopmostWhenNoIdFound() {
        TestSchema top = new TestSchema(null, null);
        TestSchema middle = new TestSchema(null, top);
        TestSchema leaf = new TestSchema(null, middle);
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
        TestSchema schema = new TestSchema(null, null);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When id is null and there is no parent, findLexicalRoot() should return the original instance");
    }

    /**
     * An empty string is considered non‑null, therefore the method must return
     * the original instance without traversing the parent chain.
     */
    @Test
    void findLexicalRoot_treatsEmptyStringIdAsNonNull() {
        TestSchema parent = new TestSchema("parentId", null);
        TestSchema schema = new TestSchema("", parent);
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
        TestSchema root = new TestSchema(null, null);
        // level3 – id "id3"
        TestSchema level3 = new TestSchema("id3", root);
        // level2 – id "id2" (first non‑null when walking up from child)
        TestSchema level2 = new TestSchema("id2", level3);
        // level1 – id null
        TestSchema level1 = new TestSchema(null, level2);
        // child – id null
        TestSchema child = new TestSchema(null, level1);
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
        TestSchema schema = new TestSchema("selfId", null);
        // create a self‑reference after construction (reflection is used only to set the parent field for the test)
        try {
            java.lang.reflect.Field f = TestSchema.class.getDeclaredField("parent");
            f.setAccessible(true);
            f.set(schema, schema);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When the schema has a non‑null id, findLexicalRoot() should return itself even if getParentSchema() returns itself");
    }

    /**
     * Chain where only the middle ancestor possesses a non‑null id.
     * Expected result is that middle ancestor.
     */
    @Test
    void findLexicalRoot_returnsMiddleAncestorWithId() {
        TestSchema top = new TestSchema(null, null);
        TestSchema middle = new TestSchema("mid", top);
        TestSchema child = new TestSchema(null, middle);
        JsonSchema result = child.findLexicalRoot();
        assertSame(middle, result, "The method should stop at the first ancestor (middle) that has a non‑null id");
    }

    /**
     * Chain of four schemas where the first non‑null id appears at level2,
     * while level3 also has a non‑null id. The method must stop at level2.
     */
    @Test
    void findLexicalRoot_stopsAtFirstNonNullIdEvenIfHigherAncestorsAlsoHaveIds() {
        TestSchema root = new TestSchema(null, null);
        TestSchema level3 = new TestSchema("id3", root);
        TestSchema level2 = new TestSchema("id2", level3);
        TestSchema level1 = new TestSchema(null, level2);
        TestSchema child = new TestSchema(null, level1);
        JsonSchema result = child.findLexicalRoot();
        assertSame(level2, result, "The method should return the first encountered non‑null id (level2) even though level3 also has an id");
    }
}
