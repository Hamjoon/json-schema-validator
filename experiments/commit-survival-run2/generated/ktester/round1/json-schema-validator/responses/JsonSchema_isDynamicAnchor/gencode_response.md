```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#isDynamicAnchor()} covering:
 * <ul>
 *   <li>default value of a freshly created schema</li>
 *   <li>value after manually setting the private field via reflection</li>
 *   <li>value when the schema is created via {@link JsonSchema#from(...)} with the $dynamicAnchor keyword</li>
 *   <li>inheritance behaviour when creating a child schema – the child must start with {@code false}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isDynamicAnchor_Test {

    @Mock
    ValidationContext validationContext;   // mock – no real behaviour needed

    @Mock
    JsonNode dummySchemaNode;              // dummy – used only for the private‑ctor path

    private JsonSchema jsonSchema;

    /**
     * Helper that creates a {@link JsonSchema} instance using the **private**
     * constructor and forces the {@code dynamicAnchor} field to the desired value.
     *
     * @param dynamicAnchor the value we want {@link JsonSchema#isDynamicAnchor()} to return
     * @return a fully‑initialised {@link JsonSchema}
     * @throws Exception if reflection fails
     */
    private JsonSchema createJsonSchema(boolean dynamicAnchor) throws Exception {
        // private constructor signature:
        // JsonSchema(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                String.class,
                URI.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonSchema instance = ctor.newInstance(
                validationContext,
                "dummySchemaPath",
                null,               // currentUri – not required for these tests
                dummySchemaNode,
                null,               // parent – top‑level schema
                false);             // suppressSubSchemaRetrieval – default

        // flip the private field
        Field field = JsonSchema.class.getDeclaredField("dynamicAnchor");
        field.setAccessible(true);
        field.setBoolean(instance, dynamicAnchor);
        return instance;
    }

    @BeforeEach
    void setUp() throws Exception {
        // default instance where dynamicAnchor == false
        jsonSchema = createJsonSchema(false);
    }

    @AfterEach
    void tearDown() {
        jsonSchema = null;
    }

    /* -------------------------------------------------------------
     *  1. Default behaviour – flag is false when not explicitly set
     * ------------------------------------------------------------- */
    @Test
    void whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse() {
        assertFalse(jsonSchema.isDynamicAnchor(),
                "isDynamicAnchor() should return false for a freshly created schema");
    }

    /* -------------------------------------------------------------
     *  2. Reflection – we force the flag to true and expect true back
     * ------------------------------------------------------------- */
    @Test
    void whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue() throws Exception {
        JsonSchema schemaWithTrue = createJsonSchema(true);
        assertTrue(schemaWithTrue.isDynamicAnchor(),
                "isDynamicAnchor() should reflect a true value set via reflection");
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

        JsonSchema schema = JsonSchema.from(
                validationContext,
                "schemaWithDynamicAnchor",
                null,          // currentUri – not needed for this unit test
                node,
                null,          // parent – top‑level
                false);        // suppressSubSchemaRetrieval

        assertTrue(schema.isDynamicAnchor(),
                "Schema created from a node containing \"$dynamicAnchor\": true must report true");
    }

    /* -------------------------------------------------------------
     *  4. Creation via JsonSchema.from – $dynamicAnchor absent (defaults to false)
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse() throws Exception {
        // Empty schema node: { }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode(); // no $dynamicAnchor field

        JsonSchema schema = JsonSchema.from(
                validationContext,
                "schemaWithoutDynamicAnchor",
                null,
                node,
                null,
                false);

        assertFalse(schema.isDynamicAnchor(),
                "Schema created from a node without \"$dynamicAnchor\" must report false");
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
        //    For the purpose of the test we only need a minimal child schema node.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode childNode = mapper.createObjectNode();
        childNode.put("type", "string");

        JsonSchema child = parent.createChildSchema("#/properties/name", childNode);

        // 3️⃣ Verify that the child starts with false, i.e. it does not inherit the flag
        assertFalse(child.isDynamicAnchor(),
                "Child schemas must initialise dynamicAnchor to false independent of the parent");
    }
}
```