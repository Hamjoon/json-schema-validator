```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.jupiter.api.*;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema objectSchema; // schema node is an ObjectNode
    private JsonSchema arraySchema;  // schema node is an ArrayNode

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* -------------------------------------------------
         * Mock the ValidationContext that JsonSchema needs.
         * Only the methods that are invoked in the constructor
         * are stubbed.
         * ------------------------------------------------- */
        ValidationContext validationContext = mock(ValidationContext.class);
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());

        // Minimal SchemaLocation – absolute IRI is not required for these tests
        SchemaLocation schemaLocation = new SchemaLocation(null);

        // Evaluation path – any non‑null value works
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        /* -------------------------------------------------
         * Prepare the JSON nodes that act as the “schema”
         * for the getNode method.
         * ------------------------------------------------- */
        String objectJson = """
                {
                  "simple": "value",
                  "a~b": 1,
                  "c/d": 2,
                  "encoded space": 4
                }
                """;
        JsonNode objectNode = MAPPER.readTree(objectJson);

        String arrayJson = """
                ["zero","one"]
                """;
        JsonNode arrayNode = MAPPER.readTree(arrayJson);

        /* -------------------------------------------------
         * Create JsonSchema instances via the package‑private
         * factory method.  Because the test class lives in the
         * same package (com.networknt.schema) we can call it
         * directly.
         * ------------------------------------------------- */
        objectSchema = JsonSchema.from(validationContext,
                schemaLocation,
                evaluationPath,
                objectNode,
                null,
                false);

        arraySchema = JsonSchema.from(validationContext,
                schemaLocation,
                evaluationPath,
                arrayNode,
                null,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required
    }

    /* -------------------------------------------------
     *  Tests for the String‑based lookup (including
     *  escaped JSON‑Pointer characters and percent‑encoding)
     * ------------------------------------------------- */
    @Test
    void getNode_simpleString() {
        JsonNode result = objectSchema.getNode("simple");
        assertNotNull(result);
        assertEquals("value", result.asText());
    }

    @Test
    void getNode_escapedTildeZero() {
        // "a~0b" should be unescaped to "a~b"
        JsonNode result = objectSchema.getNode("a~0b");
        assertNotNull(result);
        assertEquals(1, result.asInt());
    }

    @Test
    void getNode_escapedTildeOne() {
        // "c~1d" should be unescaped to "c/d"
        JsonNode result = objectSchema.getNode("c~1d");
        assertNotNull(result);
        assertEquals(2, result.asInt());
    }

    @Test
    void getNode_percentEncoded() {
        // "encoded%20space" should be URL‑decoded to "encoded space"
        JsonNode result = objectSchema.getNode("encoded%20space");
        assertNotNull(result);
        assertEquals(4, result.asInt());
    }

    /* -------------------------------------------------
     *  Test for numeric index – array handling
     * ------------------------------------------------- */
    @Test
    void getNode_numericIndex() {
        // Index 1 should retrieve the second element ("one")
        JsonNode result = arraySchema.getNode(1);
        assertNotNull(result);
        assertEquals("one", result.asText());
    }

    @Test
    void getNode_numericIndexOutOfBounds() {
        // Index that does not exist should return null
        JsonNode result = arraySchema.getNode(5);
        assertNull(result);
    }
}
```