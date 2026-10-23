```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.uri.DefaultURIFactory;
import org.junit.jupiter.api.*;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchema_findLexicalRoot_Test {

    private static ObjectMapper mapper;

    // @Mock
    private JsonMetaSchema metaSchemaMock;

    // @Mock
    private JsonSchemaFactory jsonSchemaFactoryMock;

    private ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * ValidationContext requires a non‑null URIFactory. The other factories
         * are not used by the findLexicalRoot() method, so they can safely be
         * passed as null. The final boolean flag (failFast) is set to false.
         */
        validationContext = new ValidationContext(
                new DefaultURIFactory(), // non‑null URIFactory
                null,                    // JsonMetaSchema – defaults will be used
                null,                    // JsonSchemaFactory – defaults will be used
                false);                  // failFast flag
    }

    /**
     * Scenario: a child schema without an "$id" whose parent has an "$id".
     * The lexical root should be the parent schema.
     */
    @Test
    void findLexicalRoot_ShouldReturnAncestorWithId() throws Exception {
        // --- given ---
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("$id", "http://example.com/root");
        ObjectNode childNode = mapper.createObjectNode();

        JsonSchema rootSchema = JsonSchema.from(validationContext,
                "root",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        JsonSchema childSchema = JsonSchema.from(validationContext,
                "root/child",
                new URI("http://example.com/root/child"),
                childNode,
                rootSchema,
                false);

        // --- when ---
        JsonSchema lexicalRoot = childSchema.findLexicalRoot();

        // --- then ---
        assertNotNull(lexicalRoot, "Lexical root should not be null");
        assertSame(rootSchema, lexicalRoot, "Lexical root should be the ancestor that has an $id");
    }

    /**
     * Scenario: a schema chain where none of the schemas contain an "$id".
     * The lexical root should be the top‑most schema (the one without a parent).
     */
    @Test
    void findLexicalRoot_NoIdInChain_ReturnsTopmostSchema() throws Exception {
        // --- given ---
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode childNode = mapper.createObjectNode();

        JsonSchema rootSchema = JsonSchema.from(validationContext,
                "root",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        JsonSchema childSchema = JsonSchema.from(validationContext,
                "root/child",
                new URI("http://example.com/root/child"),
                childNode,
                rootSchema,
                false);

        // --- when ---
        JsonSchema lexicalRoot = childSchema.findLexicalRoot();

        // --- then ---
        assertNotNull(lexicalRoot, "Lexical root should not be null");
        assertSame(rootSchema, lexicalRoot, "Lexical root should be the topmost schema when no $id is present");
    }

    /**
     * Scenario: the current schema already has an "$id".
     * The method should return the invoking instance without walking the parent chain.
     */
    @Test
    void findLexicalRoot_RootHasId_ReturnsSelf() throws Exception {
        // --- given ---
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("$id", "http://example.com/root");

        JsonSchema rootSchema = JsonSchema.from(validationContext,
                "root",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        // --- when ---
        JsonSchema lexicalRoot = rootSchema.findLexicalRoot();

        // --- then ---
        assertNotNull(lexicalRoot);
        assertSame(rootSchema, lexicalRoot, "When the schema itself has an $id, it should be returned as the lexical root");
    }

    /**
     * Scenario: a leaf schema without an "$id" and without a parent.
     * The method should return the leaf itself (the top‑most schema).
     */
    @Test
    void findLexicalRoot_LeafNoIdNoParent_ReturnsSelf() throws Exception {
        // --- given ---
        ObjectNode leafNode = mapper.createObjectNode();

        JsonSchema leafSchema = JsonSchema.from(validationContext,
                "leaf",
                new URI("http://example.com/leaf"),
                leafNode,
                null,
                false);

        // --- when ---
        JsonSchema lexicalRoot = leafSchema.findLexicalRoot();

        // --- then ---
        assertNotNull(lexicalRoot);
        assertSame(leafSchema, lexicalRoot, "A schema without $id and without a parent should return itself");
    }

    /**
     * Scenario: a grand‑child where the root has an "$id" but the intermediate parent does not.
     * The lexical root should be the root schema.
     */
    @Test
    void findLexicalRoot_GrandChild_MidNoId_RootHasId() throws Exception {
        // --- given ---
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("$id", "http://example.com/root");
        ObjectNode midNode = mapper.createObjectNode();
        ObjectNode leafNode = mapper.createObjectNode();

        JsonSchema rootSchema = JsonSchema.from(validationContext,
                "root",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        JsonSchema midSchema = JsonSchema.from(validationContext,
                "root/mid",
                new URI("http://example.com/root/mid"),
                midNode,
                rootSchema,
                false);

        JsonSchema leafSchema = JsonSchema.from(validationContext,
                "root/mid/leaf",
                new URI("http://example.com/root/mid/leaf"),
                leafNode,
                midSchema,
                false);

        // --- when ---
        JsonSchema lexicalRoot = leafSchema.findLexicalRoot();

        // --- then ---
        assertNotNull(lexicalRoot);
        assertSame(rootSchema, lexicalRoot, "The method should walk up to the first ancestor that has an $id (the root)");
    }

    /**
     * Scenario: a deep leaf where none of the ancestors have an "$id".
     * The lexical root should be the top‑most schema (the one whose parent is null).
     */
    @Test
    void findLexicalRoot_DeepLeaf_AllNoId_ReturnsTopmost() throws Exception {
        // --- given ---
        ObjectNode level1Node = mapper.createObjectNode();
        ObjectNode level2Node = mapper.createObjectNode();
        ObjectNode level3Node = mapper.createObjectNode();

        JsonSchema level1 = JsonSchema.from(validationContext,
                "level1",
                new URI("http://example.com/level1"),
                level1Node,
                null,
                false);

        JsonSchema level2 = JsonSchema.from(validationContext,
                "level1/level2",
                new URI("http://example.com/level1/level2"),
                level2Node,
                level1,
                false);

        JsonSchema level3 = JsonSchema.from(validationContext,
                "level1/level2/level3",
                new URI("http://example.com/level1/level2/level3"),
                level3Node,
                level2,
                false);

        // --- when ---
        JsonSchema lexicalRoot = level3.findLexicalRoot();

        // --- then ---
        assertNotNull(lexicalRoot);
        assertSame(level1, lexicalRoot, "When no schema in the chain has an $id, the topmost schema should be returned");
    }
}
```