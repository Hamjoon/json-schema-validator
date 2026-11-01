package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

class JsonSchema_findLexicalRoot_Test {

    /**
     * Helper that creates a {@link JsonSchema} instance and injects the
     * {@code id} and {@code parentSchema} fields via reflection.
     *
     * @param id     the lexical identifier (may be {@code null})
     * @param parent the parent schema (may be {@code null})
     * @return a {@code JsonSchema} with the supplied state
     */
//     private JsonSchema createSchema(String id, JsonSchema parent) {
//         try {
//             // Obtain a factory – the default version (V4) works for our tests.
//             JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
//             // An empty object node is enough; the validators are not exercised here.
//             JsonNode dummyNode = JsonNodeFactory.instance.objectNode();
//             // The constructor we need is package‑private; locate it via reflection.
//             Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(JsonSchemaFactory.class, JsonNode.class, JsonSchema.class, String.class, boolean.class);
//             ctor.setAccessible(true);
//             // Create the schema instance with the supplied id / parent.
//             return ctor.newInstance(factory, dummyNode, parent, id, false);
//         } catch (Exception e) {
//             // Wrap any reflection problem so the test failure is obvious.
//             throw new RuntimeException("Unable to create test JsonSchema instance", e);
//         }
//     }

    /**
     * When the schema itself already has an {@code id}, {@code findLexicalRoot}
     * should return the same instance.
     */
    @Test
    void testFindLexicalRootWhenSelfHasId() {
        JsonSchema schema = createSchema("rootId", null);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "Lexical root should be the schema itself when it has a non‑null id");
    }

    /**
     * When the current schema has no {@code id} but its parent does,
     * {@code findLexicalRoot} should return the parent.
     */
    @Test
    void testFindLexicalRootFindsAncestorWithId() {
        JsonSchema parent = createSchema("parentId", null);
        JsonSchema child = createSchema(null, parent);
        JsonSchema result = child.findLexicalRoot();
        assertSame(parent, result, "Lexical root should be the nearest ancestor with a non‑null id");
    }

    /**
     * When neither the schema nor any of its ancestors have an {@code id},
     * {@code findLexicalRoot} should return the top‑most schema in the chain.
     */
    @Test
    void testFindLexicalRootWhenNoAncestorHasId() {
        JsonSchema root = createSchema(null, null);
        JsonSchema child = createSchema(null, root);
        JsonSchema grandChild = createSchema(null, child);
        JsonSchema result = grandChild.findLexicalRoot();
        assertSame(root, result, "When no ancestors have an id, the lexical root should be the topmost schema");
    }

    /**
     * A root schema without an {@code id} should be its own lexical root.
     */
    @Test
    void testFindLexicalRootRootWithoutId() {
        JsonSchema root = createSchema(null, null);
        JsonSchema result = root.findLexicalRoot();
        assertSame(root, result, "A root schema without an id should be its own lexical root");
    }

    /**
     * When the schema has a {@code null} id and no parent, it should be its own
     * lexical root.
     */
    @Test
    void testFindLexicalRootSelfNoIdNoParent() {
        JsonSchema schema = createSchema(null, null);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "A schema without id and without parent should be its own lexical root");
    }

    /**
     * When the immediate parent has no {@code id} but the grand‑parent does,
     * the method should climb two levels and return the grand‑parent.
     */
    @Test
    void testFindLexicalRootGrandParentHasId() {
        JsonSchema grandParent = createSchema("grandParentId", null);
        JsonSchema parent = createSchema(null, grandParent);
        JsonSchema child = createSchema(null, parent);
        JsonSchema result = child.findLexicalRoot();
        assertSame(grandParent, result, "Lexical root should be the first ancestor with a non‑null id (grand‑parent in this case)");
    }
}