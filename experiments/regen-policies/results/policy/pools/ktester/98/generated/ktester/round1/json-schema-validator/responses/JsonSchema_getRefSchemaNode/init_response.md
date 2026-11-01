```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#getRefSchemaNode(String)}.
 *
 * The tests instantiate a real {@link JsonSchema} instance using the package‑private
 * {@code JsonSchema.from(...)} factory method. All heavy collaborators are mocked so
 * that the constructor does not try to resolve real schema resources.
 *
 * The focus is on the logic inside {@code getRefSchemaNode}:
 *   – handling of the JSON‑Pointer prefix,
 *   – URL‑decoding,
 *   – navigation of the underlying {@link JsonNode} tree.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The schema under test. */
    private JsonSchema schema;

    /** Sample schema node used by the tests. */
    private JsonNode schemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Prepare a minimal JSON schema document.
        // -----------------------------------------------------------------
        String json = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "definitions": {
                    "foo": {
                      "type": "string",
                      "description": "A simple string"
                    },
                    "bar": {
                      "type": "integer"
                    }
                  }
                }
                """;
        schemaNode = MAPPER.readTree(json);

        // -----------------------------------------------------------------
        // 2. Mock the heavy collaborators required by the JsonSchema ctor.
        // -----------------------------------------------------------------
        ValidationContext validationContext = mock(ValidationContext.class);
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parent = null; // root schema – no parent.

        // ValidationContext stubs
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());

        // JsonMetaSchema stubs – no anchors in our tiny schema.
        when(metaSchema.readAnchor(any())).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any())).thenReturn(null);

        // SchemaLocation stub – we do not need a real IRI.
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);

        // -----------------------------------------------------------------
        // 3. Create the JsonSchema instance via the package‑private factory.
        // -----------------------------------------------------------------
        schema = JsonSchema.from(validationContext,
                                 schemaLocation,
                                 evaluationPath,
                                 schemaNode,
                                 parent,
                                 false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up.
    }

    /**
     * Verify that a valid JSON‑Pointer (starting with {@code "#/"} ) is resolved
     * correctly to the target node.
     */
    @Test
    void testGetRefSchemaNode_ValidPointer() {
        // "#/definitions/foo" points to the object { "type":"string", ... }
        JsonNode result = schema.getRefSchemaNode("#/definitions/foo");

        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Result should not be a missing node");
        assertEquals("string", result.get("type").asText(),
                     "The resolved node should contain the expected type");
        assertEquals("A simple string", result.get("description").asText(),
                     "The resolved node should contain the expected description");
    }

    /**
     * Verify that a pointer without the leading {@code "#"} is also handled
     * (the method strips the leading {@code "#"} internally).
     */
    @Test
    void testGetRefSchemaNode_PointerWithoutHash() {
        // The method internally removes a leading "#" – we pass "/definitions/bar".
        JsonNode result = schema.getRefSchemaNode("/definitions/bar");

        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("integer", result.get("type").asText(),
                     "Should resolve to the integer definition");
    }

    /**
     * Verify that an unknown pointer results in a missing node and that the
     * private {@code handleNullNode} fallback is exercised (it returns a missing
     * node in our minimal setup).
     */
    @Test
    void testGetRefSchemaNode_UnknownPointer() {
        JsonNode result = schema.getRefSchemaNode("#/definitions/unknown");

        // In our test configuration {@code handleNullNode} ends up returning a
        // missing node because there is no sub‑schema to follow.
        assertTrue(result.isMissingNode(),
                   "An unknown pointer should yield a missing node after fallback");
    }

    /**
     * Verify that URL‑encoded pointers are decoded before navigation.
     */
    @Test
    void testGetRefSchemaNode_UrlEncodedPointer() {
        // Encode the '/' character: "%2Fdefinitions%2Ffoo"
        JsonNode result = schema.getRefSchemaNode("#%2Fdefinitions%2Ffoo");

        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("string", result.get("type").asText(),
                     "URL‑decoded pointer should resolve correctly");
    }
}
```