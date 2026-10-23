package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    private static ObjectMapper mapper;

    @Mock
    private URIFactory uriFactoryMock;

    @Mock
    private URNFactory urnFactoryMock;

    @Mock
    private JsonMetaSchema metaSchemaMock;

    @Mock
    private JsonSchemaFactory jsonSchemaFactoryMock;

    private ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // ValidationContext does not require non‑null factories for the tests we run.
        // Passing nulls is sufficient because the factories are not used by the
        // findLexicalRoot() logic under test.
        validationContext = new ValidationContext(null, null, null, null, null);
    }

    /**
     * Scenario: a child schema without an "$id" whose parent has an "$id".
     * The lexical root should be the parent schema.
     */
    @Test
    void findLexicalRoot_ShouldReturnAncestorWithId() throws Exception {
        // --- given ---
        // parent schema node containing an "$id"
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("$id", "http://example.com/root");
        // child schema node without "$id"
        ObjectNode childNode = mapper.createObjectNode();
        // create the root JsonSchema (no parent)
        JsonSchema rootSchema = JsonSchema.from(validationContext, "root", new URI("http://example.com/root"), rootNode, null, false);
        // create the child JsonSchema with the root as its parent
        JsonSchema childSchema = JsonSchema.from(validationContext, "root/child", new URI("http://example.com/root/child"), childNode, rootSchema, false);
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
        // root schema node without "$id"
        ObjectNode rootNode = mapper.createObjectNode();
        // child schema node without "$id"
        ObjectNode childNode = mapper.createObjectNode();
        // create the root JsonSchema (no parent)
        JsonSchema rootSchema = JsonSchema.from(validationContext, "root", new URI("http://example.com/root"), rootNode, null, false);
        // create the child JsonSchema with the root as its parent
        JsonSchema childSchema = JsonSchema.from(validationContext, "root/child", new URI("http://example.com/root/child"), childNode, rootSchema, false);
        // --- when ---
        JsonSchema lexicalRoot = childSchema.findLexicalRoot();
        // --- then ---
        assertNotNull(lexicalRoot, "Lexical root should not be null");
        // Because no schema in the chain has an $id, the method should return the topmost schema.
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
        JsonSchema rootSchema = JsonSchema.from(validationContext, "root", new URI("http://example.com/root"), rootNode, null, false);
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
        // no $id
        ObjectNode leafNode = mapper.createObjectNode();
        JsonSchema leafSchema = JsonSchema.from(validationContext, "leaf", new URI("http://example.com/leaf"), leafNode, null, false);
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
        // root with $id
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("$id", "http://example.com/root");
        // mid schema without $id
        ObjectNode midNode = mapper.createObjectNode();
        // leaf schema without $id
        ObjectNode leafNode = mapper.createObjectNode();
        JsonSchema rootSchema = JsonSchema.from(validationContext, "root", new URI("http://example.com/root"), rootNode, null, false);
        JsonSchema midSchema = JsonSchema.from(validationContext, "root/mid", new URI("http://example.com/root/mid"), midNode, rootSchema, false);
        JsonSchema leafSchema = JsonSchema.from(validationContext, "root/mid/leaf", new URI("http://example.com/root/mid/leaf"), leafNode, midSchema, false);
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
        // level1 (topmost) without $id
        ObjectNode level1Node = mapper.createObjectNode();
        // level2 without $id
        ObjectNode level2Node = mapper.createObjectNode();
        // level3 without $id (deep leaf)
        ObjectNode level3Node = mapper.createObjectNode();
        JsonSchema level1 = JsonSchema.from(validationContext, "level1", new URI("http://example.com/level1"), level1Node, null, false);
        JsonSchema level2 = JsonSchema.from(validationContext, "level1/level2", new URI("http://example.com/level1/level2"), level2Node, level1, false);
        JsonSchema level3 = JsonSchema.from(validationContext, "level1/level2/level3", new URI("http://example.com/level1/level2/level3"), level3Node, level2, false);
        // --- when ---
        JsonSchema lexicalRoot = level3.findLexicalRoot();
        // --- then ---
        assertNotNull(lexicalRoot);
        assertSame(level1, lexicalRoot, "When no schema in the chain has an $id, the topmost schema should be returned");
    }
}
