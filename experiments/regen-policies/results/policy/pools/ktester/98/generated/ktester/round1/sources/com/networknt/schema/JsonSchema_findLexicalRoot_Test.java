package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

// @ExtendWith(MockitoExtension.class)
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
//             super(null, null, null, null, null, false);
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
     * Helper that creates a {@link JsonSchema} instance with the given {@code id}
     * and {@code parent}. All other constructor arguments are {@code null}
     * because they are irrelevant for the {@code findLexicalRoot} logic.
     */
    private static JsonSchema createSchema(String id, JsonSchema parent) {
        try {
            // Obtain the private constructor
            Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
            ctor.setAccessible(true);
            // Instantiate with nulls for everything except the parent reference
            JsonSchema schema = ctor.newInstance(null, null, null, null, parent, false);
            // Set the private 'id' field
            Field idField = JsonSchema.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(schema, id);
            // The constructor already set the parent (fifth argument), but we
            // expose the field as well for tests that need to modify it later
            // (e.g., self‑referential parent).
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(schema, parent);
            return schema;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JsonSchema instance via reflection", e);
        }
    }

    /**
     * When the schema itself has a non‑null {@code id}, {@code findLexicalRoot()}
     * must return the same instance.
     */
    @Test
    void findLexicalRoot_returnsSelfWhenIdPresent() {
        JsonSchema schemaWithId = createSchema("http://example.com/root", null);
        JsonSchema result = schemaWithId.findLexicalRoot();
        assertSame(schemaWithId, result, "When the schema has an id, findLexicalRoot() should return the schema itself");
    }

    /**
     * When the current schema has no {@code id} but its direct parent does,
     * the method should return the parent.
     */
    @Test
    void findLexicalRoot_returnsParentWhenParentHasId() {
        JsonSchema parent = createSchema("http://example.com/parent", null);
        JsonSchema child = createSchema(null, parent);
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
        JsonSchema root = createSchema("http://example.com/root", null);
        JsonSchema intermediate = createSchema(null, root);
        JsonSchema leaf = createSchema(null, intermediate);
        JsonSchema result = leaf.findLexicalRoot();
        assertSame(root, result, "findLexicalRoot() should climb the parent chain and return the first ancestor with a non‑null id");
    }

    /**
     * When none of the ancestors (including the topmost one) have an {@code id},
     * the method should return the topmost schema (the one whose parent is {@code null}).
     */
    @Test
    void findLexicalRoot_returnsTopmostWhenNoIdFound() {
        JsonSchema top = createSchema(null, null);
        JsonSchema middle = createSchema(null, top);
        JsonSchema leaf = createSchema(null, middle);
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
        JsonSchema schema = createSchema(null, null);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When id is null and there is no parent, findLexicalRoot() should return the original instance");
    }

    /**
     * An empty string is considered non‑null, therefore the method must return
     * the original instance without traversing the parent chain.
     */
    @Test
    void findLexicalRoot_treatsEmptyStringIdAsNonNull() {
        JsonSchema parent = createSchema("parentId", null);
        JsonSchema schema = createSchema("", parent);
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
        JsonSchema root = createSchema(null, null);
        // level3 – id "id3"
        JsonSchema level3 = createSchema("id3", root);
        // level2 – id "id2" (first non‑null when walking up from child)
        JsonSchema level2 = createSchema("id2", level3);
        // level1 – id null
        JsonSchema level1 = createSchema(null, level2);
        // child – id null
        JsonSchema child = createSchema(null, level1);
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
        JsonSchema schema = createSchema("selfId", null);
        // make the parent reference point to itself
        try {
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(schema, schema);
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
        JsonSchema top = createSchema(null, null);
        JsonSchema middle = createSchema("mid", top);
        JsonSchema child = createSchema(null, middle);
        JsonSchema result = child.findLexicalRoot();
        assertSame(middle, result, "The method should stop at the first ancestor (middle) that has a non‑null id");
    }

    /**
     * Chain of four schemas where the first non‑null id appears at level2,
     * while level3 also has a non‑null id. The method must stop at level2.
     */
    @Test
    void findLexicalRoot_stopsAtFirstNonNullIdEvenIfHigherAncestorsAlsoHaveIds() {
        JsonSchema root = createSchema(null, null);
        JsonSchema level3 = createSchema("id3", root);
        JsonSchema level2 = createSchema("id2", level3);
        JsonSchema level1 = createSchema(null, level2);
        JsonSchema child = createSchema(null, level1);
        JsonSchema result = child.findLexicalRoot();
        assertSame(level2, result, "The method should return the first encountered non‑null id (level2) even though level3 also has an id");
    }
}