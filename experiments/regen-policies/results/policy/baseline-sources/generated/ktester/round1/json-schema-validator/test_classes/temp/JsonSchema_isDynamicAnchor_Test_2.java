package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class JsonSchema_isDynamicAnchor_Test {

    @Mock
    ValidationContext // mock – no real behavior needed
    validationContext;

    @Mock
    JsonNode // mock – schema content not required for this test
    schemaNode;

    private JsonSchema jsonSchema;

    @Mock
    JsonNode // dummy – used only for the private‑ctor path
    dummySchemaNode;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // By default create a schema with dynamicAnchor = false.
        jsonSchema = createJsonSchema(false);
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
    }

    /**
     * Creates a {@link JsonSchema} instance using the private constructor.
     *
     * @param dynamicAnchor the value that the {@code isDynamicAnchor()} method should return
     * @throws Exception if reflection fails
     */
    private JsonSchema createJsonSchema(boolean dynamicAnchor) throws Exception {
        // private constructor signature:
        // JsonSchema(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonSchema instance = ctor.newInstance(// ValidationContext – not needed for these tests
        null, // schema path
        "dummySchemaPath", // currentUri – not required
        null, // schema node – not required
        null, // parent – top‑level schema
        null, // suppressSubSchemaRetrieval – default
        false);
        // Set the private {@code dynamicAnchor} field to the desired value.
        Field field = JsonSchema.class.getDeclaredField("dynamicAnchor");
        field.setAccessible(true);
        field.setBoolean(instance, dynamicAnchor);
        return instance;
    }

    @Test
    void whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse() {
        // jsonSchema was created in @BeforeEach with dynamicAnchor = false
        assertFalse(jsonSchema.isDynamicAnchor(), "isDynamicAnchor() should return false when the internal flag is false");
    }

    @Test
    void whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue() throws Exception {
        // Re‑create the instance with the flag set to true
        JsonSchema schemaWithDynamicAnchor = createJsonSchema(true);
        assertTrue(schemaWithDynamicAnchor.isDynamicAnchor(), "isDynamicAnchor() should return true when the internal flag is true");
    }

    /* -------------------------------------------------------------
     *  3. Creation via JsonSchema.from – $dynamicAnchor = true
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue() throws Exception {
        // Build a JSON node: { "$dynamicAnchor": true }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("$dynamicAnchor", true);
        JsonSchema schema = JsonSchema.from(// ValidationContext – not needed for this unit test
        null, // schema path (parent)
        "schemaWithDynamicAnchor", // currentUri – not needed
        null, // schema node containing $dynamicAnchor
        node, // suppressSubSchemaRetrieval
        null, // suppressSubSchemaRetrieval flag
        false);
        assertTrue(schema.isDynamicAnchor(), "Schema created from a node containing \"$dynamicAnchor\": true must report true");
    }

    /* -------------------------------------------------------------
     *  4. Creation via JsonSchema.from – $dynamicAnchor absent (defaults to false)
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse() throws Exception {
        // Empty schema node: { }
        ObjectMapper mapper = new ObjectMapper();
        // no $dynamicAnchor field
        ObjectNode node = mapper.createObjectNode();
        JsonSchema schema = JsonSchema.from(null, "schemaWithoutDynamicAnchor", null, node, null, false);
        assertFalse(schema.isDynamicAnchor(), "Schema created from a node without \"$dynamicAnchor\" must report false");
    }

    /* -------------------------------------------------------------
     *  5. Child schema must NOT inherit the parent's dynamicAnchor value
     * ------------------------------------------------------------- */
    @Test
    void childSchemaDoesNotInheritParentDynamicAnchor() throws Exception {
        // 1️⃣ Create a parent schema where dynamicAnchor is true (via reflection)
        JsonSchema parent = createJsonSchema(true);
        assertTrue(parent.isDynamicAnchor(), "Pre‑condition: parent dynamicAnchor should be true");
        // 2️⃣ Create a child schema through the public API
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode childNode = mapper.createObjectNode();
        childNode.put("type", "string");
        JsonSchema child = parent.createChildSchema("#/properties/name", childNode);
        // 3️⃣ Verify that the child starts with false, i.e. it does not inherit the flag
        assertFalse(child.isDynamicAnchor(), "Child schemas must initialise dynamicAnchor to false independent of the parent");
    }
}
