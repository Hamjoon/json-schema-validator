```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

class JsonSchema_getRefSchemaNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple schema used for all tests */
    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$id\": \"http://example.com/simple-schema.json\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" },\n"
            + "    \"age\":  { \"type\": \"integer\" }\n"
            + "  }\n"
            + "}";

    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance from the simple schema string.
        // The factory will take care of constructing all required internal objects.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(this.schema, "Schema should be created successfully");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that a reference that starts with the schema's $id is correctly
     * resolved to the same node as a plain JSON‑Pointer reference.
     */
    @Test
    void getRefSchemaNode_WithIdPrefix_resolvesCorrectly() throws Exception {
        // reference that includes the $id followed by a JSON‑Pointer fragment
        String refWithId = "http://example.com/simple-schema.json#/properties/name";

        JsonNode result = schema.getRefSchemaNode(refWithId);

        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Resulting node must exist");
        // The resolved node should be the schema for the \"name\" property
        JsonNode expected = MAPPER.readTree("{\"type\":\"string\"}");
        assertEquals(expected, result, "Resolved node should match the \"name\" property schema");
    }

    /**
     * Verify that a plain JSON‑Pointer reference (starting with \"#/\") is
     * resolved to the correct sub‑schema node.
     */
    @Test
    void getRefSchemaNode_WithPlainPointer_resolvesCorrectly() throws Exception {
        // plain JSON‑Pointer reference
        String plainRef = "#/properties/age";

        JsonNode result = schema.getRefSchemaNode(plainRef);

        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Resulting node must exist");
        // The resolved node should be the schema for the \"age\" property
        JsonNode expected = MAPPER.readTree("{\"type\":\"integer\"}");
        assertEquals(expected, result, "Resolved node should match the \"age\" property schema");
    }

    /**
     * Verify that a reference that points to a non‑existent location falls back
     * to {@code handleNullNode}. In the current implementation {@code handleNullNode}
     * returns {@code null} when it cannot resolve the reference, so the method
     * should return {@code null} (or a missing node) for such a case.
     */
    @Test
    void getRefSchemaNode_NonExistingPointer_returnsMissingOrNull() {
        String missingRef = "#/properties/unknown";

        JsonNode result = schema.getRefSchemaNode(missingRef);

        // The contract of the method is to return a node; when the pointer is missing
        // the implementation ends up calling the private handleNullNode which returns null.
        // Therefore we accept either a null reference or a MissingNode.
        assertTrue(result == null || result.isMissingNode(),
                "Result should be null or a missing node for a non‑existent reference");
    }
}
```