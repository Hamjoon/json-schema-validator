```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code JsonSchema.isDynamicAnchor()} method.
 *
 * <p>The original tests tried to instantiate {@code JsonSchema} via its private
 * constructor supplying {@code null} for {@link ValidationContext}.  The
 * constructor dereferences the context (calls {@code getMetaSchema()}) which
 * caused a {@link NullPointerException} in every test.</p>
 *
 * <p>To fix the problem we use the public {@link JsonSchemaFactory} API to
 * create a valid {@code JsonSchema} instance.  After creation we set the
 * private {@code dynamicAnchor} field via reflection for the tests that need
 * to control its value explicitly.  This approach provides a fully‑initialised
 * schema (including a non‑null {@code ValidationContext}) while keeping the
 * original test intentions unchanged.</p>
 */
class JsonSchema_isDynamicAnchor_Test {

    private JsonSchema jsonSchema;

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
     * Creates a {@link JsonSchema} instance using the public {@link JsonSchemaFactory}
     * and then forces the {@code dynamicAnchor} flag to the supplied value.
     *
     * @param dynamicAnchor the value that {@code isDynamicAnchor()} should return
     * @return a {@code JsonSchema} with the requested {@code dynamicAnchor} state
     * @throws Exception if reflection fails
     */
    private JsonSchema createJsonSchema(boolean dynamicAnchor) throws Exception {
        // Create a minimal (empty) schema node – the content does not matter for
        // the dynamic‑anchor flag.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode emptyNode = mapper.createObjectNode();

        // Use the public factory to obtain a fully‑initialised JsonSchema.
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(emptyNode);

        // Force the private 'dynamicAnchor' field to the desired value.
        Field field = JsonSchema.class.getDeclaredField("dynamicAnchor");
        field.setAccessible(true);
        field.setBoolean(schema, dynamicAnchor);
        return schema;
    }

    @Test
    void whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse() {
        // jsonSchema was created in @BeforeEach with dynamicAnchor = false
        assertFalse(jsonSchema.isDynamicAnchor(),
                "isDynamicAnchor() should return false when the internal flag is false");
    }

    @Test
    void whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue() throws Exception {
        // Re‑create the instance with the flag set to true
        JsonSchema schemaWithDynamicAnchor = createJsonSchema(true);
        assertTrue(schemaWithDynamicAnchor.isDynamicAnchor(),
                "isDynamicAnchor() should return true when the internal flag is true");
    }

    /* -------------------------------------------------------------
     *  3. Creation via JsonSchemaFactory – $dynamicAnchor = true
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue() throws Exception {
        // Build a JSON node: { "$dynamicAnchor": true }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("$dynamicAnchor", true);

        // Use the factory – it will read the $dynamicAnchor keyword.
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(node);
        assertTrue(schema.isDynamicAnchor(),
                "Schema created from a node containing \"$dynamicAnchor\": true must report true");
    }

    /* -------------------------------------------------------------
     *  4. Creation via JsonSchemaFactory – $dynamicAnchor absent (defaults to false)
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse() throws Exception {
        // Empty schema node: { }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();

        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(node);
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
        assertTrue(parent.isDynamicAnchor(),
                "Pre‑condition: parent dynamicAnchor should be true");

        // 2️⃣ Create a child schema through the public API
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