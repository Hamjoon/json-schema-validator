```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#getNode(JsonNode, Object)}.
 *
 * The method is {@code protected}, therefore it is accessed via reflection.
 * A real {@link JsonSchema} instance is created with {@link JsonSchemaFactory}
 * (the simplest possible schema is sufficient because the method under test
 * does not depend on the schema contents).
 *
 * The test cases are derived from the JSON specification supplied with the
 * task.  They are grouped by the nature of the input and the expected
 * behaviour (value, {@code null}, or {@link IllegalArgumentException}).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_2_Test {

    /** Shared {@link JsonSchema} instance used by all tests. */
    private static JsonSchema jsonSchema;

    /** Jackson mapper to build {@link JsonNode} test data. */
    private static ObjectMapper mapper;

    /** Cached reflective {@code Method} instance for {@code getNode(JsonNode, Object)}. */
    private static Method getNodeMethod;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // initialise Jackson helper
        mapper = new ObjectMapper();

        // create a minimal JsonSchema – the actual schema content is irrelevant for getNode()
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        jsonSchema = factory.getSchema("{}");   // empty schema is enough

        // obtain the protected method via reflection and make it accessible
        getNodeMethod = JsonSchema.class.getDeclaredMethod("getNode", JsonNode.class, Object.class);
        getNodeMethod.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // no per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        jsonSchema = null;
        mapper = null;
        getNodeMethod = null;
    }

    /* --------------------------------------------------------------------- */
    /*  Existing tests (kept unchanged)                                      */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that {@code getNode(JsonNode, Object)} correctly retrieves a child
     * property from an object node.
     */
    @Test
    void getNode_returnsPropertyFromObjectNode() throws Exception {
        // {"name":"John","age":30}
        JsonNode root = mapper.readTree("{\"name\":\"John\",\"age\":30}");

        // invoke the protected method
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, "name");

        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isTextual(), "Resulting node should be a text node");
        assertEquals("John", result.textValue(), "Unexpected property value");
    }

    /**
     * Verify that {@code getNode(JsonNode, Object)} correctly retrieves an
     * element from an array node using an integer index.
     */
    @Test
    void getNode_returnsElementFromArrayNode() throws Exception {
        // [10,20,30]
        JsonNode root = mapper.readTree("[10,20,30]");

        // request the element at index 1 (the value 20)
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, 1);

        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isNumber(), "Resulting node should be numeric");
        assertEquals(20, result.intValue(), "Unexpected array element value");
    }

    /**
     * When the requested property/key does not exist the underlying
     * {@link JsonNodes#get(JsonNode, Object)} returns a {@link com.fasterxml.jackson.databind.node.MissingNode}.
     * This test ensures that the behaviour is preserved.
     */
    @Test
    void getNode_missingPropertyReturnsMissingNode() throws Exception {
        JsonNode root = mapper.readTree("{\"present\":true}");

        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, "absent");

        assertNotNull(result, "Result should be a non‑null node");
        assertTrue(result.isMissingNode(), "Result should be a MissingNode for unknown property");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests for the remaining groups                         */
    /* --------------------------------------------------------------------- */

    // -----------------------------------------------------------------
    //  Object node – existing property (value expectations)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] property={1} expected={2}")
    @MethodSource("objectNodeExistingPropertyProvider")
    void getNode_objectNodeExistingProperty(String json, Object key, String expected) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.isTextual() ? ("\"" + result.textValue() + "\"") : result.toString(),
                "Unexpected value returned");
    }

    private static Stream<Arguments> objectNodeExistingPropertyProvider() {
        return Stream.of(
                Arguments.of("{\"name\":\"John\",\"age\":30}", "name", "\"John\""),
                Arguments.of("{\"name\":\"John\",\"age\":30}", "age", "30")
        );
    }

    // -----------------------------------------------------------------
    //  Object node – non‑existing property (null expectation)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] property={1}")
    @MethodSource("objectNodeMissingPropertyProvider")
    void getNode_objectNodeMissingProperty(String json, Object key) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        // JsonNodes.get returns MissingNode for unknown fields – treat it as “null” for the test
        assertTrue(result == null || result.isMissingNode(),
                "Expected null or MissingNode when property does not exist");
    }

    private static Stream<Arguments> objectNodeMissingPropertyProvider() {
        return Stream.of(
                Arguments.of("{\"name\":\"John\",\"age\":30}", "address")
        );
    }

    // -----------------------------------------------------------------
    //  Array node – valid index (value expectations)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] index={1}")
    @MethodSource("arrayNodeValidIndexProvider")
    void getNode_arrayNodeValidIndex(String json, Object index, String expected) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, index);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected,
                result.isTextual() ? ("\"" + result.textValue() + "\"") : result.toString(),
                "Unexpected array element");
    }

    private static Stream<Arguments> arrayNodeValidIndexProvider() {
        return Stream.of(
                Arguments.of("[\"apple\",\"banana\",\"cherry\"]", 1, "\"banana\""),
                Arguments.of("[10,20,30]", 0, "10")
        );
    }

    // -----------------------------------------------------------------
    //  Array node – out‑of‑bounds index (null expectation)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] index={1}")
    @MethodSource("arrayNodeOutOfBoundsProvider")
    void getNode_arrayNodeOutOfBounds(String json, Object index) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, index);
        assertTrue(result == null || result.isMissingNode(),
                "Expected null or MissingNode for out‑of‑bounds index");
    }

    private static Stream<Arguments> arrayNodeOutOfBoundsProvider() {
        return Stream.of(
                Arguments.of("[\"a\",\"b\"]", 5),
                Arguments.of("[]", -1)
        );
    }

    // -----------------------------------------------------------------
    //  Invalid propertyOrIndex type – expect IllegalArgumentException
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={1}")
    @MethodSource("invalidPropertyOrIndexProvider")
    void getNode_invalidPropertyOrIndexThrows(String json, Object key) throws Exception {
        JsonNode root = mapper.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> getNodeMethod.invoke(jsonSchema, root, key),
                "Expected IllegalArgumentException for unsupported key type");
    }

    private static Stream<Arguments> invalidPropertyOrIndexProvider() {
        return Stream.of(
                // Boolean not supported
                Arguments.of("{\"key\":\"value\"}", Boolean.TRUE),
                // Non‑integer number not supported
                Arguments.of("[1,2,3]", 3.14)
        );
    }

    // -----------------------------------------------------------------
    //  Null node handling (null expectation)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={1}")
    @MethodSource("nullNodeProvider")
    void getNode_nullNodeReturnsNull(Object key) throws Exception {
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, null, key);
        assertTrue(result == null || result.isMissingNode(),
                "Expected null or MissingNode when input node is null");
    }

    private static Stream<Arguments> nullNodeProvider() {
        return Stream.of(
                Arguments.of("any"),
                Arguments.of(0)
        );
    }

    // -----------------------------------------------------------------
    //  Object node – numeric index (null expectation)
    // -----------------------------------------------------------------
    @Test
    void getNode_objectNodeNumericIndexReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("{\"a\":1,\"b\":2}");
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, 0);
        assertTrue(result == null || result.isMissingNode(),
                "Numeric index on object node should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    //  Array node – string index (null expectation)
    // -----------------------------------------------------------------
    @Test
    void getNode_arrayNodeStringIndexReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("[\"x\",\"y\"]");
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, "1");
        assertTrue(result == null || result.isMissingNode(),
                "String index on array node should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    //  Scalar node handling (null expectation)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] scalar={1}")
    @MethodSource("scalarNodeProvider")
    void getNode_scalarNodeReturnsNull(String json, Object key) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        assertTrue(result == null || result.isMissingNode(),
                "Any key on scalar node should return null/MissingNode");
    }

    private static Stream<Arguments> scalarNodeProvider() {
        return Stream.of(
                Arguments.of("\"plain text\"", "any"),
                Arguments.of("42", 0)
        );
    }

    // -----------------------------------------------------------------
    //  Null propertyOrIndex (null expectation)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] node={1}")
    @MethodSource("nullPropertyOrIndexProvider")
    void getNode_nullPropertyOrIndexReturnsNull(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, null);
        assertTrue(result == null || result.isMissingNode(),
                "Null propertyOrIndex should yield null/MissingNode");
    }

    private static Stream<Arguments> nullPropertyOrIndexProvider() {
        return Stream.of(
                Arguments.of("{\"k\":\"v\"}"),
                Arguments.of("[\"a\"]")
        );
    }

    // -----------------------------------------------------------------
    //  Empty string property (value / null)
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] expected={1}")
    @MethodSource("emptyStringPropertyProvider")
    void getNode_emptyStringProperty(String json, String expected) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, "");
        if (expected == null) {
            assertTrue(result == null || result.isMissingNode(),
                    "Empty string property not present → null/MissingNode");
        } else {
            assertNotNull(result, "Result must not be null");
            assertEquals(expected,
                    result.isTextual() ? ("\"" + result.textValue() + "\"") : result.toString(),
                    "Unexpected value for empty string property");
        }
    }

    private static Stream<Arguments> emptyStringPropertyProvider() {
        return Stream.of(
                Arguments.of("{\"\":\"empty\"}", "\"empty\""),
                Arguments.of("{\"name\":\"John\"}", null)
        );
    }

    // -----------------------------------------------------------------
    //  Large index out of range (null expectation)
    // -----------------------------------------------------------------
    @Test
    void getNode_largeIndexOutOfRangeReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("[0,1]");
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, Integer.MAX_VALUE);
        assertTrue(result == null || result.isMissingNode(),
                "Very large index should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    //  Unsupported propertyOrIndex type – Character (exception)
    // -----------------------------------------------------------------
    @Test
    void getNode_characterKeyThrowsIllegalArgumentException() throws Exception {
        JsonNode root = mapper.readTree("{\"c\":\"value\"}");
        Character key = Character.valueOf('c');
        assertThrows(IllegalArgumentException.class,
                () -> getNodeMethod.invoke(jsonSchema, root, key),
                "Character key should cause IllegalArgumentException");
    }

    // -----------------------------------------------------------------
    //  Unsupported propertyOrIndex type – Byte (null expectation)
    // -----------------------------------------------------------------
    @Test
    void getNode_byteKeyOnObjectReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("{\"b\":10}");
        Byte key = Byte.valueOf((byte) 5);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        assertTrue(result == null || result.isMissingNode(),
                "Byte key on object node should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    //  Unsupported propertyOrIndex type – Long (exception)
    // -----------------------------------------------------------------
    @Test
    void getNode_longKeyThrowsIllegalArgumentException() throws Exception {
        JsonNode root = mapper.readTree("{\"key\":\"value\"}");
        Long key = 1234567890123L;
        assertThrows(IllegalArgumentException.class,
                () -> getNodeMethod.invoke(jsonSchema, root, key),
                "Long key should cause IllegalArgumentException");
    }
}
```