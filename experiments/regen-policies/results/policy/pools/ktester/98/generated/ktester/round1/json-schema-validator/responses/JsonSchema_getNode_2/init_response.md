```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#getNode(JsonNode, Object)}.
 *
 * The method is {@code protected}, therefore it is accessed via reflection.
 * A real {@link JsonSchema} instance is created with {@link JsonSchemaFactory}
 * (the simplest possible schema is sufficient because the method under test
 * does not depend on the schema contents).
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
        // release static references
        jsonSchema = null;
        mapper = null;
        getNodeMethod = null;
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
}
```