
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:10: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:19: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:22: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:25: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_findLexicalRoot_Test
7 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.JsonSchema.hasId;\n\trelated functions:\n\t\tmethod `public JsonSchema findAncestor()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `private Map<String, JsonValidator> read(JsonNode)` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Please note that the key in {@link #validators} map is a schema path. It is\n     * used in {@link com.networknt.schema.walk.DefaultKeywordWalkListenerRunner} to derive the keyword.\n     ;\n\t\tmethod `private JsonNode getMessageNode(JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\nclass `com.networknt.schema.utils.JsonNodeUtil`:\n\trelated functions:\n\t\tmethod `private static boolean isEnumObjectSchema(JsonSchema)` returns `boolean`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>)` returns `void`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


