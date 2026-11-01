


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_2_Test {

    /**
     * Shared {@link JsonSchema} instance used by all tests.
     */
    private static JsonSchema jsonSchema;

    /**
     * Jackson mapper to build {@link JsonNode} test data.
     */
    private static ObjectMapper mapper;

    /**
     * Cached reflective {@code Method} instance for {@code getNode(JsonNode, Object)}.
     */
    private static Method getNodeMethod;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // initialise Jackson helper
        mapper = new ObjectMapper();
        // create a minimal JsonSchema – the actual schema content is irrelevant for getNode()
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // empty schema is enough
        jsonSchema = factory.getSchema("{}");
        // obtain the protected method via reflection and make it accessible
        getNodeMethod = JsonSchema.class.getDeclaredMethod("getNode", JsonNode.class, Object.class);
        getNodeMethod.setAccessible(true);
    }

    @AfterAll
    static void teardownAfterAll() {
        // release static references
        jsonSchema = null;
        mapper = null;
        getNodeMethod = null;
    }

    private static Stream<Arguments> objectNodeExistingPropertyProvider() {
        return Stream.of(Arguments.of("{\"name\":\"John\",\"age\":30}", "name", "\"John\""), Arguments.of("{\"name\":\"John\",\"age\":30}", "age", "30"));
    }

    private static Stream<Arguments> objectNodeMissingPropertyProvider() {
        return Stream.of(Arguments.of("{\"name\":\"John\",\"age\":30}", "address"));
    }

    private static Stream<Arguments> arrayNodeValidIndexProvider() {
        return Stream.of(Arguments.of("[\"apple\",\"banana\",\"cherry\"]", 1, "\"banana\""), Arguments.of("[10,20,30]", 0, "10"));
    }

    private static Stream<Arguments> arrayNodeOutOfBoundsProvider() {
        return Stream.of(Arguments.of("[\"a\",\"b\"]", 5), Arguments.of("[]", -1));
    }

    private static Stream<Arguments> invalidPropertyOrIndexProvider() {
        return Stream.of(// Boolean not supported
        Arguments.of("{\"key\":\"value\"}", Boolean.TRUE), // Non‑integer number not supported
        Arguments.of("[1,2,3]", 3.14));
    }

    private static Stream<Arguments> nullNodeProvider() {
        return Stream.of(Arguments.of("any"), Arguments.of(0));
    }

    private static Stream<Arguments> scalarNodeProvider() {
        return Stream.of(Arguments.of("\"plain text\"", "any"), Arguments.of("42", 0));
    }

    private static Stream<Arguments> nullPropertyOrIndexProvider() {
        return Stream.of(Arguments.of("{\"k\":\"v\"}"), Arguments.of("[\"a\"]"));
    }

    private static Stream<Arguments> emptyStringPropertyProvider() {
        return Stream.of(Arguments.of("{\"\":\"empty\"}", "\"empty\""), Arguments.of("{\"name\":\"John\"}", null));
    }

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
        assertNotNull(result, "Resulting node must be null");
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

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] property={1} expected={2}")
    @MethodSource("objectNodeExistingPropertyProvider")
    void getNode_objectNodeExistingProperty(String json, Object key, String expected) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.isTextual() ? ("\"" + result.textValue() + "\"") : result.toString(), "Unexpected value returned");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] property={1}")
    @MethodSource("objectNodeMissingPropertyProvider")
    void getNode_objectNodeMissingProperty(String json, Object key) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        // JsonNodes.get returns MissingNode for unknown fields – treat it as “null” for the test
        assertTrue(result == null || result.isMissingNode(), "Expected null or MissingNode when property does not exist");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] index={1}")
    @MethodSource("arrayNodeValidIndexProvider")
    void getNode_arrayNodeValidIndex(String json, Object index, String expected) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, index);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.isTextual() ? ("\"" + result.textValue() + "\"") : result.toString(), "Unexpected array element");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] index={1}")
    @MethodSource("arrayNodeOutOfBoundsProvider")
    void getNode_arrayNodeOutOfBounds(String json, Object index) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, index);
        assertTrue(result == null || result.isMissingNode(), "Expected null or MissingNode for out‑of‑bounds index");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={1}")
    @MethodSource("invalidPropertyOrIndexProvider")
    void getNode_invalidPropertyOrIndexThrows(String json, Object key) throws Exception {
        JsonNode root = mapper.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> getNodeMethod.invoke(jsonSchema, root, key), "Expected IllegalArgumentException for unsupported key type");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={1}")
    @MethodSource("nullNodeProvider")
    void getNode_nullNodeReturnsNull(Object key) throws Exception {
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, null, key);
        assertTrue(result == null || result.isMissingNode(), "Expected null or MissingNode when input node is null");
    }

    // -----------------------------------------------------------------
    @Test
    void getNode_objectNodeNumericIndexReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("{\"a\":1,\"b\":2}");
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, 0);
        assertTrue(result == null || result.isMissingNode(), "Numeric index on object node should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    @Test
    void getNode_arrayNodeStringIndexReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("[\"x\",\"y\"]");
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, "1");
        assertTrue(result == null || result.isMissingNode(), "String index on array node should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] scalar={1}")
    @MethodSource("scalarNodeProvider")
    void getNode_scalarNodeReturnsNull(String json, Object key) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        assertTrue(result == null || result.isMissingNode(), "Any key on scalar node should return null/MissingNode");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] node={1}")
    @MethodSource("nullPropertyOrIndexProvider")
    void getNode_nullPropertyOrIndexReturnsNull(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, null);
        assertTrue(result == null || result.isMissingNode(), "Null propertyOrIndex should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] expected={1}")
    @MethodSource("emptyStringPropertyProvider")
    void getNode_emptyStringProperty(String json, String expected) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, "");
        if (expected == null) {
            assertTrue(result == null || result.isMissingNode(), "Empty string property not present → null/MissingNode");
        } else {
            assertNotNull(result, "Result must not be null");
            assertEquals(expected, result.isTextual() ? ("\"" + result.textValue() + "\"") : result.toString(), "Unexpected value for empty string property");
        }
    }

    // -----------------------------------------------------------------
    @Test
    void getNode_largeIndexOutOfRangeReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("[0,1]");
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, Integer.MAX_VALUE);
        assertTrue(result == null || result.isMissingNode(), "Very large index should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    @Test
    void getNode_characterKeyThrowsIllegalArgumentException() throws Exception {
        JsonNode root = mapper.readTree("{\"c\":\"value\"}");
        Character key = Character.valueOf('c');
        assertThrows(IllegalArgumentException.class, () -> getNodeMethod.invoke(jsonSchema, root, key), "Character key should cause IllegalArgumentException");
    }

    // -----------------------------------------------------------------
    @Test
    void getNode_byteKeyOnObjectReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("{\"b\":10}");
        Byte key = Byte.valueOf((byte) 5);
        JsonNode result = (JsonNode) getNodeMethod.invoke(jsonSchema, root, key);
        assertTrue(result == null || result.isMissingNode(), "Byte key on object node should yield null/MissingNode");
    }

    // -----------------------------------------------------------------
    @Test
    void getNode_longKeyThrowsIllegalArgumentException() throws Exception {
        JsonNode root = mapper.readTree("{\"key\":\"value\"}");
        Long key = 1234567890123L;
        assertThrows(IllegalArgumentException.class, () -> getNodeMethod.invoke(jsonSchema, root, key), "Long key should cause IllegalArgumentException");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_getNode_2_Test ✔
│     ├─ getNode_nullPropertyOrIndexReturnsNull(String) ✔
│     │  ├─ [1] node={1} ✘ Cannot invoke "Object.toString()" because "propertyOrIndex" is null
│     │  └─ [2] node={1} ✘ Cannot invoke "Object.toString()" because "propertyOrIndex" is null
│     ├─ getNode_invalidPropertyOrIndexThrows(String, Object) ✔
│     │  ├─ [1] key=true ✘ Expected IllegalArgumentException for unsupported key type ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     │  └─ [2] key=3.14 ✘ Expected IllegalArgumentException for unsupported key type ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ getNode_arrayNodeOutOfBounds(String, Object) ✔
│     │  ├─ [1] index=5 ✔
│     │  └─ [2] index=-1 ✔
│     ├─ getNode_returnsPropertyFromObjectNode() ✔
│     ├─ getNode_scalarNodeReturnsNull(String, Object) ✔
│     │  ├─ [1] scalar=any ✔
│     │  └─ [2] scalar=0 ✔
│     ├─ getNode_missingPropertyReturnsMissingNode() ✘ Result should be a non‑null node ==> expected: not <null>
│     ├─ getNode_byteKeyOnObjectReturnsNull() ✔
│     ├─ getNode_objectNodeExistingProperty(String, Object, String) ✔
│     │  ├─ [1] property=name expected="John" ✔
│     │  └─ [2] property=age expected=30 ✔
│     ├─ getNode_arrayNodeValidIndex(String, Object, String) ✔
│     │  ├─ [1] index=1 ✔
│     │  └─ [2] index=0 ✔
│     ├─ getNode_objectNodeMissingProperty(String, Object) ✔
│     │  └─ [1] property=address ✔
│     ├─ getNode_objectNodeNumericIndexReturnsNull() ✔
│     ├─ getNode_nullNodeReturnsNull(Object) ✔
│     │  ├─ [1] key={1} ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because "node" is null
│     │  └─ [2] key={1} ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isArray()" because "node" is null
│     ├─ getNode_emptyStringProperty(String, String) ✔
│     │  ├─ [1] expected="empty" ✔
│     │  └─ [2] expected=null ✔
│     ├─ getNode_returnsElementFromArrayNode() ✔
│     ├─ getNode_longKeyThrowsIllegalArgumentException() ✘ Long key should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ getNode_characterKeyThrowsIllegalArgumentException() ✘ Character key should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ getNode_arrayNodeStringIndexReturnsNull() ✔
│     └─ getNode_largeIndexOutOfRangeReturnsNull() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (9):
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_nullPropertyOrIndexReturnsNull(String):[1] node={1}
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_nullPropertyOrIndexReturnsNull', methodParameterTypes = 'java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "Object.toString()" because "propertyOrIndex" is null
       com.networknt.schema.utils.JsonNodes.get(JsonNodes.java:73)
       com.networknt.schema.JsonSchema.getNode(JsonSchema.java:429)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_nullPropertyOrIndexReturnsNull(JsonSchema_getNode_2_Test.java:219)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_nullPropertyOrIndexReturnsNull(String):[2] node={1}
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_nullPropertyOrIndexReturnsNull', methodParameterTypes = 'java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "Object.toString()" because "propertyOrIndex" is null
       com.networknt.schema.utils.JsonNodes.get(JsonNodes.java:73)
       com.networknt.schema.JsonSchema.getNode(JsonSchema.java:429)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_nullPropertyOrIndexReturnsNull(JsonSchema_getNode_2_Test.java:219)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_invalidPropertyOrIndexThrows(String, Object):[1] key=true
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_invalidPropertyOrIndexThrows', methodParameterTypes = 'java.lang.String, java.lang.Object']
    => org.opentest4j.AssertionFailedError: Expected IllegalArgumentException for unsupported key type ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_invalidPropertyOrIndexThrows(JsonSchema_getNode_2_Test.java:178)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_invalidPropertyOrIndexThrows(String, Object):[2] key=3.14
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_invalidPropertyOrIndexThrows', methodParameterTypes = 'java.lang.String, java.lang.Object']
    => org.opentest4j.AssertionFailedError: Expected IllegalArgumentException for unsupported key type ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_invalidPropertyOrIndexThrows(JsonSchema_getNode_2_Test.java:178)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_missingPropertyReturnsMissingNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_missingPropertyReturnsMissingNode', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Result should be a non‑null node ==> expected: not <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNotNull.failNull(AssertNotNull.java:49)
       org.junit.jupiter.api.AssertNotNull.assertNotNull(AssertNotNull.java:35)
       org.junit.jupiter.api.Assertions.assertNotNull(Assertions.java:312)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_missingPropertyReturnsMissingNode(JsonSchema_getNode_2_Test.java:130)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_nullNodeReturnsNull(Object):[1] key={1}
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_nullNodeReturnsNull', methodParameterTypes = 'java.lang.Object']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because "node" is null
       com.networknt.schema.utils.JsonNodes.get(JsonNodes.java:73)
       com.networknt.schema.JsonSchema.getNode(JsonSchema.java:429)
       jdk.internal.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_nullNodeReturnsNull(JsonSchema_getNode_2_Test.java:185)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_nullNodeReturnsNull(Object):[2] key={1}
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_nullNodeReturnsNull', methodParameterTypes = 'java.lang.Object']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isArray()" because "node" is null
       com.networknt.schema.utils.JsonNodes.get(JsonNodes.java:70)
       com.networknt.schema.JsonSchema.getNode(JsonSchema.java:429)
       jdk.internal.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_nullNodeReturnsNull(JsonSchema_getNode_2_Test.java:185)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_longKeyThrowsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_longKeyThrowsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Long key should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_longKeyThrowsIllegalArgumentException(JsonSchema_getNode_2_Test.java:267)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_getNode_2_Test:getNode_characterKeyThrowsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getNode_2_Test', methodName = 'getNode_characterKeyThrowsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Character key should cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_getNode_2_Test.getNode_characterKeyThrowsIllegalArgumentException(JsonSchema_getNode_2_Test.java:250)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 572 ms
[        13 containers found      ]
[         0 containers skipped    ]
[        13 containers started    ]
[         0 containers aborted    ]
[        13 containers successful ]
[         0 containers failed     ]
[        26 tests found           ]
[         0 tests skipped         ]
[        26 tests started         ]
[         0 tests aborted         ]
[        17 tests successful      ]
[         9 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

return type: 
com.fasterxml.jackson.databind.JsonNode

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `protected JsonNode getNode(JsonNode, Object)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.getNode(JsonNode, Object)`;
class `com.networknt.schema.utils.JsonNodes`:
	related functions:
		method `public static T get(JsonNode, JsonNodePath)` returns `T`, related with `com.networknt.schema.JsonSchema.getNode(JsonNode, Object)`, api document: 
     * Gets the node found at the path.
     *
     * @param <T> the type of the node
     * @param node the node
     * @param path the path
     * @return the node found at the path or null
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.getNode(JsonNode, Object)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
