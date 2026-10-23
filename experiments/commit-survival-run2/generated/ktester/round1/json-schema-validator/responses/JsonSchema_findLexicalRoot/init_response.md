```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // ValidationContext does not require a non‑null config for the tests we run
        validationContext = new ValidationContext(
                uriFactoryMock,
                urnFactoryMock,
                metaSchemaMock,
                jsonSchemaFactoryMock,
                null);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for these unit tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed
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
        JsonSchema rootSchema = JsonSchema.from(
                validationContext,
                "root",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        // create the child JsonSchema with the root as its parent
        JsonSchema childSchema = JsonSchema.from(
                validationContext,
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
        // root schema node without "$id"
        ObjectNode rootNode = mapper.createObjectNode();

        // child schema node without "$id"
        ObjectNode childNode = mapper.createObjectNode();

        // create the root JsonSchema (no parent)
        JsonSchema rootSchema = JsonSchema.from(
                validationContext,
                "root",
                new URI("http://example.com/root"),
                rootNode,
                null,
                false);

        // create the child JsonSchema with the root as its parent
        JsonSchema childSchema = JsonSchema.from(
                validationContext,
                "root/child",
                new URI("http://example.com/root/child"),
                childNode,
                rootSchema,
                false);

        // --- when ---
        JsonSchema lexicalRoot = childSchema.findLexicalRoot();

        // --- then ---
        assertNotNull(lexicalRoot, "Lexical root should not be null");
        // Because no schema in the chain has an $id, the method should return the topmost schema.
        assertSame(rootSchema, lexicalRoot, "Lexical root should be the topmost schema when no $id is present");
    }
}
```