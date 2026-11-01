package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    /**
     * Helper that creates a {@link JsonSchema} instance and injects the
     * {@code id} and {@code parentSchema} fields via reflection.
     *
     * @param id     the lexical identifier (may be {@code null})
     * @param parent the parent schema (may be {@code null})
     * @return a {@code JsonSchema} with the supplied state
     */
    private JsonSchema createSchema(String id, JsonSchema parent) {
        try {
            // JsonSchema has a public no‑arg constructor in the library.
            JsonSchema schema = JsonSchema.class.getDeclaredConstructor().newInstance();
            // The library stores the identifier in a field named "id".
            // The parent reference is stored in a field named "parentSchema".
            // Both fields are private, so we make them accessible.
            Field idField = JsonSchema.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(schema, id);
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(schema, parent);
            return schema;
        } catch (Exception e) {
            // If reflection fails we wrap the exception in a RuntimeException,
            // causing the test to fail with a clear message.
            throw new RuntimeException("Unable to create test JsonSchema instance", e);
        }
    }

    /**
     * When the schema itself already has an {@code id}, {@code findLexicalRoot}
     * should return the same instance.
     */
    @Test
    void testFindLexicalRootWhenSelfHasId() {
        // Arrange
        JsonSchema schema = createSchema("rootId", null);
        // Act
        JsonSchema result = schema.findLexicalRoot();
        // Assert
        assertSame(schema, result, "Lexical root should be the schema itself when it has a non‑null id");
    }

    /**
     * When the current schema has no {@code id} but its parent does,
     * {@code findLexicalRoot} should return the parent.
     */
    @Test
    void testFindLexicalRootFindsAncestorWithId() {
        // Arrange
        JsonSchema parent = createSchema("parentId", null);
        JsonSchema child = createSchema(null, parent);
        // Act
        JsonSchema result = child.findLexicalRoot();
        // Assert
        assertSame(parent, result, "Lexical root should be the nearest ancestor with a non‑null id");
    }

    /**
     * When neither the schema nor any of its ancestors have an {@code id},
     * {@code findLexicalRoot} should return the top‑most schema in the chain.
     */
    @Test
    void testFindLexicalRootWhenNoAncestorHasId() {
        // Arrange
        JsonSchema root = createSchema(null, null);
        JsonSchema child = createSchema(null, root);
        JsonSchema grandChild = createSchema(null, child);
        // Act
        JsonSchema result = grandChild.findLexicalRoot();
        // Assert
        assertSame(root, result, "When no ancestors have an id, the lexical root should be the topmost schema");
    }

    /**
     * A root schema without an {@code id} should be its own lexical root.
     */
    @Test
    void testFindLexicalRootRootWithoutId() {
        // Arrange
        JsonSchema root = createSchema(null, null);
        // Act
        JsonSchema result = root.findLexicalRoot();
        // Assert
        assertSame(root, result, "A root schema without an id should be its own lexical root");
    }

    /**
     * When the schema has a {@code null} id and no parent, it should be its own
     * lexical root.
     */
    @Test
    void testFindLexicalRootSelfNoIdNoParent() {
        // Arrange
        JsonSchema schema = createSchema(null, null);
        // Act
        JsonSchema result = schema.findLexicalRoot();
        // Assert
        assertSame(schema, result, "A schema without id and without parent should be its own lexical root");
    }

    /**
     * When the immediate parent has no {@code id} but the grand‑parent does,
     * the method should climb two levels and return the grand‑parent.
     */
    @Test
    void testFindLexicalRootGrandParentHasId() {
        // Arrange
        JsonSchema grandParent = createSchema("grandParentId", null);
        JsonSchema parent = createSchema(null, grandParent);
        JsonSchema child = createSchema(null, parent);
        // Act
        JsonSchema result = child.findLexicalRoot();
        // Assert
        assertSame(grandParent, result, "Lexical root should be the first ancestor with a non‑null id (grand‑parent in this case)");
    }
}
