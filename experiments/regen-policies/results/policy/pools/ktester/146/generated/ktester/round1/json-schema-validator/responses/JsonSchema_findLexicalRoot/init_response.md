```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#findLexicalRoot()}.
 *
 * The tests use Mockito to create lightweight {@link JsonSchema} mocks.
 * The real {@code findLexicalRoot} implementation is exercised while
 * {@code getId()} and {@code getParentSchema()} are stubbed to simulate
 * various hierarchy configurations.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
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
     * When the schema itself already has an {@code id}, {@code findLexicalRoot}
     * should return the same instance.
     */
    @Test
    void testFindLexicalRootWhenSelfHasId() {
        // Arrange
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("rootId");
        when(schema.getParentSchema()).thenReturn(null);

        // Act
        JsonSchema result = schema.findLexicalRoot();

        // Assert
        assertSame(schema, result,
                "Lexical root should be the schema itself when it has a non‑null id");
    }

    /**
     * When the current schema has no {@code id} but its parent does,
     * {@code findLexicalRoot} should return the parent.
     */
    @Test
    void testFindLexicalRootFindsAncestorWithId() {
        // Arrange
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn("parentId");
        when(parent.getParentSchema()).thenReturn(null);

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(parent);

        // Act
        JsonSchema result = child.findLexicalRoot();

        // Assert
        assertSame(parent, result,
                "Lexical root should be the nearest ancestor with a non‑null id");
    }

    /**
     * When neither the schema nor any of its ancestors have an {@code id},
     * {@code findLexicalRoot} should return the top‑most schema in the chain.
     */
    @Test
    void testFindLexicalRootWhenNoAncestorHasId() {
        // Arrange
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(root);

        JsonSchema grandChild = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(grandChild.getId()).thenReturn(null);
        when(grandChild.getParentSchema()).thenReturn(child);

        // Act
        JsonSchema result = grandChild.findLexicalRoot();

        // Assert
        assertSame(root, result,
                "When no ancestors have an id, the lexical root should be the topmost schema");
    }

    /**
     * A root schema without an {@code id} should be its own lexical root.
     */
    @Test
    void testFindLexicalRootRootWithoutId() {
        // Arrange
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);

        // Act
        JsonSchema result = root.findLexicalRoot();

        // Assert
        assertSame(root, result,
                "A root schema without an id should be its own lexical root");
    }
}
```