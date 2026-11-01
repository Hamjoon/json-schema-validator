```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@link JsonSchema#getNode(JsonNode, Object)}.
 *
 * <p>The method is {@code protected}, therefore it is invoked via reflection.
 * Each test case is described by a short textual description, the input {@link JsonNode},
 * the {@code propertyOrIndex} argument and the expected {@link JsonNode} result.
 * A {@code null} expected value means the method should return {@code null};
 * a non‑null expected value (including {@link NullNode}) is compared using {@link JsonNode#equals(Object)}.
 *
 * <p>The tests are grouped by the logical scenario (object property access,
 * array index access, invalid inputs, etc.) and are implemented as
 * {@code @ParameterizedTest}s with a dedicated {@code MethodSource} per group.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_2_Test {

    private static ObjectMapper mapper;
    private JsonSchema jsonSchema; // instance used to invoke the protected method

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a concrete JsonSchema instance via the public factory.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema("{\"type\":\"object\"}");
    }

    @AfterEach
    void teardownAfterEach() {
        // No cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No cleanup required.
    }

    /**
     * Helper that uses reflection to call the protected {@code getNode(JsonNode, Object)}
     * method on the {@link JsonSchema} instance created in {@link #setupBeforeEach()}.
     */
    private JsonNode invokeGetNode(JsonNode node, Object propertyOrIndex) throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("getNode", JsonNode.class, Object.class);
        m.setAccessible(true);
        return (JsonNode) m.invoke(jsonSchema, node, propertyOrIndex);
    }

    // -------------------------------------------------------------------------
    //  Parameterized test groups
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideObjectNodePropertyAccess")
    void getNode_ObjectNodePropertyAccess(String description,
                                         JsonNode node,
                                         Object propertyOrIndex,
                                         JsonNode expected) throws Exception {
        JsonNode result = invokeGetNode(node, propertyOrIndex);
        assertResultMatches(description, result, expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideArrayNodeIndexAccess")
    void getNode_ArrayNodeIndexAccess(String description,
                                      JsonNode node,
                                      Object propertyOrIndex,
                                      JsonNode expected) throws Exception {
        JsonNode result = invokeGetNode(node, propertyOrIndex);
        assertResultMatches(description, result, expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideInvalidOrEdgeInputs")
    void getNode_InvalidOrEdgeInputs(String description,
                                     JsonNode node,
                                     Object propertyOrIndex,
                                     JsonNode expected) throws Exception {
        JsonNode result = invokeGetNode(node, propertyOrIndex);
        assertResultMatches(description, result, expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideArrayNodeEdgeCases")
    void getNode_ArrayNodeEdgeCases(String description,
                                    JsonNode node,
                                    Object propertyOrIndex,
                                    JsonNode expected) throws Exception {
        JsonNode result = invokeGetNode(node, propertyOrIndex);
        assertResultMatches(description, result, expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideObjectNodeNonStringKeys")
    void getNode_ObjectNodeNonStringKeys(String description,
                                         JsonNode node,
                                         Object propertyOrIndex,
                                         JsonNode expected) throws Exception {
        JsonNode result = invokeGetNode(node, propertyOrIndex);
        assertResultMatches(description, result, expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSpecialNodeTypes")
    void getNode_SpecialNodeTypes(String description,
                                  JsonNode node,
                                  Object propertyOrIndex,
                                  JsonNode expected) throws Exception {
        JsonNode result = invokeGetNode(node, propertyOrIndex);
        assertResultMatches(description, result, expected);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideArrayNodeNonIntegerNumericTypes")
    void getNode_ArrayNodeNonIntegerNumericTypes(String description,
                                                 JsonNode node,
                                                 Object propertyOrIndex,
                                                 JsonNode expected) throws Exception {
        JsonNode result = invokeGetNode(node, propertyOrIndex);
        assertResultMatches(description, result, expected);
    }

    // -------------------------------------------------------------------------
    //  Assertion helper
    // -------------------------------------------------------------------------

    /**
     * Centralised assertion logic used by all parameterised tests.
     *
     * @param description textual description of the test case (used only for readability)
     * @param actual      the {@link JsonNode} returned by {@code getNode}
     * @param expected    the expected {@link JsonNode} – may be {@code null}
     */
    private void assertResultMatches(String description, JsonNode actual, JsonNode expected) {
        if (expected == null) {
            assertNull(actual, description);
        } else {
            assertNotNull(actual, description);
            assertEquals(expected, actual, description);
        }
    }

    // -------------------------------------------------------------------------
    //  Method sources – one per test group
    // -------------------------------------------------------------------------

    /**
     * Test data for object node property access.
     */
    static Stream<Arguments> provideObjectNodePropertyAccess() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        // ObjectNode {"name":"Alice","age":30}
        ObjectNode aliceNode = f.objectNode()
                .put("name", "Alice")
                .put("age", 30);

        // ObjectNode {"nullValue":null}
        ObjectNode nullValueNode = f.objectNode()
                .set("nullValue", f.nullNode());

        return Stream.of(
                arguments("Object node property access – existing property",
                        aliceNode, "name", f.textNode("Alice")),
                arguments("Object node property access – missing property",
                        aliceNode, "missing", null),
                arguments("Object node property access – property value is JSON null",
                        nullValueNode, "nullValue", f.nullNode())
        );
    }

    /**
     * Test data for array node index access.
     */
    static Stream<Arguments> provideArrayNodeIndexAccess() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        // ArrayNode ["a","b","c"]
        ArrayNode abcArray = f.arrayNode()
                .add("a")
                .add("b")
                .add("c");

        return Stream.of(
                arguments("Array node index access – valid integer index",
                        abcArray, 1, f.textNode("b")),
                arguments("Array node index access – out‑of‑bounds integer index",
                        abcArray, 5, null),
                arguments("Array node index access – numeric string index",
                        abcArray, "2", f.textNode("c")),
                arguments("Array node index access – non‑numeric string index",
                        abcArray, "invalid", null)
        );
    }

    /**
     * Test data for invalid or edge inputs.
     */
    static Stream<Arguments> provideInvalidOrEdgeInputs() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        // ObjectNode {"foo":"bar"}
        ObjectNode fooBar = f.objectNode().put("foo", "bar");

        // TextNode "plain string"
        TextNode plainString = f.textNode("plain string");

        // ObjectNode {"num":42}
        ObjectNode numNode = f.objectNode().put("num", 42);

        return Stream.of(
                arguments("Invalid input – null JsonNode",
                        null, "any", null),
                arguments("Invalid input – null propertyOrIndex",
                        fooBar, null, null),
                arguments("Invalid input – scalar JsonNode (TextNode)",
                        plainString, "any", null),
                arguments("Invalid input – non‑integer numeric property (Double)",
                        numNode, 42.0, null)
        );
    }

    /**
     * Test data for array node edge cases (negative indices, long, big‑integer, etc.).
     */
    static Stream<Arguments> provideArrayNodeEdgeCases() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        // ArrayNode ["x","y","z"]
        ArrayNode xyzArray = f.arrayNode()
                .add("x")
                .add("y")
                .add("z");

        // ArrayNode ["a","b","c"]
        ArrayNode abcArray = f.arrayNode()
                .add("a")
                .add("b")
                .add("c");

        return Stream.of(
                arguments("Array node edge case – negative integer index",
                        xyzArray, -1, null),
                arguments("Array node edge case – negative numeric string",
                        xyzArray, "-2", null),
                arguments("Array node edge case – Long index within bounds",
                        abcArray, 2L, f.textNode("c")),
                arguments("Array node edge case – BigInteger index within bounds",
                        abcArray, BigInteger.valueOf(1), f.textNode("b")),
                arguments("Array node edge case – Double integral value (treated as invalid)",
                        abcArray, 1.0d, null)
        );
    }

    /**
     * Test data for object node access with non‑string keys.
     */
    static Stream<Arguments> provideObjectNodeNonStringKeys() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        // ObjectNode {"key":"value"}
        ObjectNode keyValue = f.objectNode().put("key", "value");

        // ObjectNode {"true":false}
        ObjectNode trueFalse = f.objectNode().put("true", false);

        // ObjectNode {"empty":"value"}
        ObjectNode emptyKey = f.objectNode().put("empty", "value");

        // ObjectNode {" name ":"value"} – spaces are part of the key
        ObjectNode spacedKey = f.objectNode().put(" name ", "value");

        return Stream.of(
                arguments("Object node non‑string key – numeric propertyOrIndex",
                        keyValue, 0, null),
                arguments("Object node non‑string key – boolean propertyOrIndex",
                        trueFalse, true, null),
                arguments("Object node non‑string key – empty string",
                        emptyKey, "", null),
                arguments("Object node non‑string key – spaced key matches exactly",
                        spacedKey, " name ", f.textNode("value"))
        );
    }

    /**
     * Test data for special node types (NullNode, MissingNode, BooleanNode, etc.).
     */
    static Stream<Arguments> provideSpecialNodeTypes() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        NullNode nullNode = f.nullNode();
        MissingNode missingNode = MissingNode.getInstance();
        BooleanNode trueNode = BooleanNode.TRUE;
        ObjectNode innerNode = f.objectNode().put("inner", "val");
        TextNode innerTextNode = f.textNode("inner");

        return Stream.of(
                arguments("Special node type – NullNode",
                        nullNode, "any", null),
                arguments("Special node type – MissingNode",
                        missingNode, "any", null),
                arguments("Special node type – BooleanNode",
                        trueNode, "any", null),
                arguments("Special node type – propertyOrIndex is a JsonNode (TextNode)",
                        innerNode, innerTextNode, null)
        );
    }

    /**
     * Test data for array node access with non‑integer numeric types (Float, etc.).
     */
    static Stream<Arguments> provideArrayNodeNonIntegerNumericTypes() {
        JsonNodeFactory f = JsonNodeFactory.instance;

        // ArrayNode ["a","b","c"]
        ArrayNode abcArray = f.arrayNode()
                .add("a")
                .add("b")
                .add("c");

        // ArrayNode ["first","second"]
        ArrayNode firstSecond = f.arrayNode()
                .add("first")
                .add("second");

        return Stream.of(
                arguments("Array node – Float non‑integral index",
                        abcArray, 1.5f, null),
                arguments("Array node – Long zero index",
                        firstSecond, 0L, f.textNode("first"))
        );
    }
}
```