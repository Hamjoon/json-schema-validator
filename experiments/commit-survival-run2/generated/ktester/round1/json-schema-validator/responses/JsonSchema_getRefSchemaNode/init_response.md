```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    // ----- Mocks -------------------------------------------------------------
    @Mock
    ValidationContext validationContext;          // used by JsonSchema
    @Mock
    JsonMetaSchema metaSchema;                  // returned by validationContext.getMetaSchema()

    // ----- Shared test data -------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonNode rootSchemaNode;            // the JSON schema used for all tests
    private JsonSchema jsonSchema;              // instance under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1. Build a simple schema JSON tree.
        //    {
        //      "properties": {
        //        "name": { "type": "string" },
        //        "age":  { "type": "integer" }
        //      },
        //      "definitions": {
        //        "address": { "type": "object" }
        //      }
        //    }
        String schemaJson = """
                {
                  "properties": {
                    "name": { "type": "string" },
                    "age":  { "type": "integer" }
                  },
                  "definitions": {
                    "address": { "type": "object" }
                  }
                }
                """;
        rootSchemaNode = MAPPER.readTree(schemaJson);

        // 2. Stub the ValidationContext behaviour required by JsonSchema.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // The configuration is optional – returning null keeps the constructor simple.
        when(validationContext.getConfig()).thenReturn(null);

        // 3. Create the JsonSchema instance using the package‑private factory method.
        //    The method is accessible because the test resides in the same package.
        jsonSchema = JsonSchema.from(
                validationContext,
                "test-schema.json",
                new URI("http://example.com/test-schema.json"),
                rootSchemaNode,
                null,                     // no parent
                false                     // do not suppress sub‑schema retrieval
        );
        assertNotNull(jsonSchema, "JsonSchema instance should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // ------------------------------------------------------------------------
    // 1. Reference that starts with "#/" – JSON‑Pointer handling.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithHashSlashPointer_ReturnsTargetNode() {
        // "#/properties/name" -> should resolve to the node { "type":"string" }
        JsonNode result = jsonSchema.getRefSchemaNode("#/properties/name");
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isObject(), "Result should be an object node");
        assertEquals("string", result.get("type").asText(), "The 'type' field should be 'string'");
    }

    // ------------------------------------------------------------------------
    // 2. Reference that starts with "/" (no leading '#') – same logic as above.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithSlashPointer_ReturnsTargetNode() {
        JsonNode result = jsonSchema.getRefSchemaNode("/properties/age");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    // ------------------------------------------------------------------------
    // 3. Fragment reference (e.g. "#/definitions/address") that is resolved via
    //    JsonMetaSchema#getNodeByFragmentRef. We mock the meta‑schema to return
    //    a predefined node.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithFragmentReference_DelegatesToMetaSchema() {
        // Prepare a node that the mocked meta‑schema will return.
        JsonNode addressNode = rootSchemaNode.at("/definitions/address");
        assertFalse(addressNode.isMissingNode(), "Address node must exist in the test schema");

        // Configure the mock: when getNodeByFragmentRef is called with the fragment
        // "#/definitions/address" and the root schema node, return the address node.
        when(metaSchema.getNodeByFragmentRef("#/definitions/address", rootSchemaNode))
                .thenReturn(addressNode);

        JsonNode result = jsonSchema.getRefSchemaNode("#/definitions/address");
        assertNotNull(result);
        assertEquals(addressNode, result);
    }

    // ------------------------------------------------------------------------
    // 4. Fragment reference where the meta‑schema cannot resolve the fragment.
    //    In this situation getRefSchemaNode should fall back to handleNullNode.
    //    Because handleNullNode is private we cannot inspect its exact behaviour,
    //    but we can verify that the method returns a non‑missing node (or the
    //    original missing node) without throwing.
    // ------------------------------------------------------------------------
    @Test
    void getRefSchemaNode_WithUnresolvableFragment_ReturnsMissingNodeOrFallback() {
        // meta‑schema returns null for an unknown fragment.
        when(metaSchema.getNodeByFragmentRef("#/unknown", rootSchemaNode))
                .thenReturn(null);

        // The method should not throw; it will invoke the private handleNullNode.
        JsonNode result = jsonSchema.getRefSchemaNode("#/unknown");
        assertNotNull(result, "Result should never be null even for unknown fragments");
        // The contract of handleNullNode (private) is to attempt another lookup.
        // If that lookup also fails, the original missing node is returned.
        // Therefore we accept either a missing node or any node (the test simply
        // ensures no exception and a non‑null return).
        // No further assertions are made because the internal fallback is private.
    }
}
```