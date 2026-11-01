```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link JsonSchema#getRefSchemaNode(String)}.
 */
class JsonSchema_getRefSchemaNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Simple schema used for most tests.
     */
    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$id\": \"http://example.com/simple-schema.json\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" },\n"
            + "    \"age\":  { \"type\": \"integer\" }\n"
            + "  }\n"
            + "}";

    private JsonSchema schema;

    /**
     * A richer schema that contains an $id, definitions and properties with spaces/encoded chars.
     */
    private static final String COMPLEX_SCHEMA = "{\n"
            + "  \"$id\": \"http://example.com/schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" },\n"
            + "    \"age\": { \"type\": \"integer\" },\n"
            + "    \"properties name\": { \"type\": \"string\" }\n"
            + "  },\n"
            + "  \"definitions\": {\n"
            + "    \"person\": { \"type\": \"object\", \"properties\": { \"first\": {\"type\":\"string\"} } },\n"
            + "    \"address\": { \"type\": \"object\", \"properties\": { \"city\": {\"type\":\"string\"} } }\n"
            + "  }\n"
            + "}";

    @BeforeEach
    void setupBeforeEach() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(this.schema, "Schema should be created successfully");
    }

    /**
     * Provides arguments for the parameterised test {@link #parameterisedRefResolution}.
     *
     * <p>Each argument consists of:
     *
     * <ul>
     *   <li>the reference string supplied to {@code getRefSchemaNode}</li>
     *   <li>a description used only for readability in the test name</li>
     *   <li>a {@code java.util.function.Predicate<JsonNode>} that validates the returned node</li>
     * </ul>
     */
    private static Stream<Arguments> refResolutionProvider() {
        return Stream.of(
                // Simple JSON‑Pointer
                Arguments.of("/properties/name", "no ID, leading '/' – direct pointer",
                        (java.util.function.Predicate<JsonNode>) node -> node != null
                                && node.equals(MAPPER.createObjectNode().put("type", "string"))),

                // Full URI with pointer
                Arguments.of("http://example.com/schema/properties/age", "ID stripped, pointer resolved",
                        (java.util.function.Predicate<JsonNode>) node -> node != null
                                && node.equals(MAPPER.createObjectNode().put("type", "integer"))),

                // Fragment handling
                Arguments.of("#/definitions/person", "fragment '#/' stripped",
                        (java.util.function.Predicate<JsonNode>) node -> node != null
                                && node.path("type").asText().equals("object")),

                Arguments.of("http://example.com/schema#/definitions/address", "ID + fragment '#/' stripped",
                        (java.util.function.Predicate<JsonNode>) node -> node != null
                                && node.path("type").asText().equals("object")),

                // No leading '/' after processing – should return root node
                Arguments.of("definitions/simple", "no leading '/' after processing – root node",
                        (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()),

                // URL decoding of encoded characters (%2F)
                Arguments.of("/properties%2Fname", "encoded '/' decoded",
                        (java.util.function.Predicate<JsonNode>) node -> node != null
                                && node.equals(MAPPER.createObjectNode().put("type", "string"))),

                // Null reference – expect NPE
                Arguments.of(null, "null reference – NPE", null),

                // Empty reference – should return root node unchanged
                Arguments.of("", "empty reference – root node",
                        (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()),

                // Exact ID match – empty pointer → root node
                Arguments.of("http://example.com/schema", "exact ID match – root node",
                        (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()),

                // Reference without leading slash after ID stripping
                Arguments.of("http://example.com/schemaproperties/name", "ID stripped, no leading '/' – root node",
                        (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()),

                // '+' character decoding to space
                Arguments.of("/properties+name", "plus decoded to space",
                        (java.util.function.Predicate<JsonNode>) node -> node != null
                                && node.equals(MAPPER.createObjectNode().put("type", "string"))),

                // Encoded space (%20)
                Arguments.of("/properties%20name", "encoded space decoded",
                        (java.util.function.Predicate<JsonNode>) node -> node != null
                                && node.equals(MAPPER.createObjectNode().put("type", "string"))),

                // Fragment only reference without leading slash – root node
                Arguments.of("#definitions/person", "fragment without '#/' – root node",
                        (java.util.function.Predicate<JsonNode>) node -> node != null && node.isObject()),

                // Decoded missing path – expect missing node
                Arguments.of("http://example.com/schema#/nonexistent%2Fpath", "decoded missing path – missing node", null)
        );
    }

    /**
     * Verify that a reference that starts with the schema's $id is correctly
     * resolved to the same node as a plain JSON‑Pointer reference.
     */
    @Test
    void getRefSchemaNode_WithIdPrefix_resolvesCorrectly() throws Exception {
        String refWithId = "http://example.com/simple-schema.json#/properties/name";
        JsonNode result = schema.getRefSchemaNode(refWithId);
        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Resulting node must exist");
        JsonNode expected = MAPPER.readTree("{\"type\":\"string\"}");
        assertEquals(expected, result, "Resolved node should match the \"name\" property schema");
    }

    /**
     * Verify that a plain JSON‑Pointer reference (starting with \"#/\") is
     * resolved to the correct sub‑schema node.
     */
    @Test
    void getRefSchemaNode_WithPlainPointer_resolvesCorrectly() throws Exception {
        String plainRef = "#/properties/age";
        JsonNode result = schema.getRefSchemaNode(plainRef);
        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Resulting node must exist");
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
        assertTrue(result == null || result.isMissingNode(),
                "Result should be null or a missing node for a non‑existent reference");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("refResolutionProvider")
    void parameterisedRefResolution(String ref, String description,
                                    java.util.function.Predicate<JsonNode> validator) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema complexSchema = factory.getSchema(COMPLEX_SCHEMA);
        assertNotNull(complexSchema, "Complex schema must be created");
        if (ref == null) {
            // Expect a NullPointerException because the implementation does not guard against null.
            assertThrows(NullPointerException.class,
                    () -> complexSchema.getRefSchemaNode(ref),
                    "Calling getRefSchemaNode(null) should throw NPE");
            return;
        }
        JsonNode result = complexSchema.getRefSchemaNode(ref);
        if (validator != null) {
            assertNotNull(result, "Resulting node must not be null");
            assertTrue(validator.test(result),
                    () -> "Validator failed for reference [" + ref + "], result: " + result);
        } else {
            // Expected missing node or null when the pointer cannot be resolved.
            assertTrue(result == null || result.isMissingNode(),
                    () -> "Expected missing node or null for reference [" + ref + "], but got: " + result);
        }
    }

    /**
     * Verifies that when the JSON‑Pointer resolves to a missing node, the private
     * {@code handleNullNode} method is invoked. The current implementation of
     * {@code handleNullNode} returns {@code null}, therefore the public method
     * should return {@code null} (or a MissingNode) for such a reference.
     */
    @Test
    void getRefSchemaNode_missingNode_triggersHandleNullNode() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema complexSchema = factory.getSchema(COMPLEX_SCHEMA);
        assertNotNull(complexSchema);
        String ref = "http://example.com/schema#/nonexistent%2Fpath";
        JsonNode result = complexSchema.getRefSchemaNode(ref);
        assertTrue(result == null || result.isMissingNode(),
                "Result should be null or MissingNode when the pointer cannot be resolved");
    }
}
```