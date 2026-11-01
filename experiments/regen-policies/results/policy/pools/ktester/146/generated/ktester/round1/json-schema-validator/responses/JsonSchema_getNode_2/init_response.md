```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

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
        // obtain a JsonSchema instance via the public factory – this gives us a concrete
        // instance on which we can reflectively call the protected getNode method.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema("{\"type\":\"object\"}");
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up for now
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

    @Test
    void getNode_shouldReturnPropertyValue_whenObjectNodeAndStringKey() throws Exception {
        // given
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("name", "John");
        objectNode.put("age", 30);

        // when
        JsonNode result = invokeGetNode(objectNode, "name");

        // then
        assertNotNull(result);
        assertTrue(result.isTextual());
        assertEquals("John", result.asText());
    }

    @Test
    void getNode_shouldReturnArrayElement_whenArrayNodeAndIntegerIndex() throws Exception {
        // given
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add("first");
        arrayNode.add("second");
        arrayNode.add("third");

        // when
        JsonNode result = invokeGetNode(arrayNode, 1); // index 1 => "second"

        // then
        assertNotNull(result);
        assertTrue(result.isTextual());
        assertEquals("second", result.asText());
    }

    @Test
    void getNode_shouldReturnNull_whenPropertyDoesNotExist() throws Exception {
        // given
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("present", "yes");

        // when
        JsonNode result = invokeGetNode(objectNode, "missing");

        // then
        assertNull(result);
    }

    @Test
    void getNode_shouldReturnNull_whenArrayIndexOutOfBounds() throws Exception {
        // given
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add("only");

        // when
        JsonNode result = invokeGetNode(arrayNode, 5); // out‑of‑range index

        // then
        assertNull(result);
    }
}
```