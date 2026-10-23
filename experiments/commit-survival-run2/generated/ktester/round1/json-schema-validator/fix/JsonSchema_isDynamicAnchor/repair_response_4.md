```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests for the {@code isDynamicAnchor()} flag of {@link JsonSchema}.
 *
 * <p>The default {@link JsonSchemaFactory#getInstance()} creates a factory for
 * Draft‑4/7 which does not understand the {@code $dynamicAnchor} keyword.
 * To exercise the dynamic‑anchor behaviour we must use a factory that is
 * configured for a later draft (2020‑12).  All schema creations in this test
 * therefore use {@code JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)}.
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
     * Creates a {@link JsonSchema} instance using the private constructor.
     *
     * @param dynamicAnchor the value that the {@code isDynamicAnchor()} method should return
     * @throws Exception if reflection fails
     */
    private JsonSchema createJsonSchema(boolean dynamicAnchor) throws Exception {
        // Minimal (empty) schema node – the content does not matter for the flag.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode emptyNode = mapper.createObjectNode();

        // Use a factory that understands $dynamicAnchor (draft‑2020‑12).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(emptyNode);

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
     *  3. Creation via JsonSchema.from – $dynamicAnchor = true
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue() throws Exception {
        // Build a JSON node: { "$dynamicAnchor": true }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("$dynamicAnchor", true);

        // Use a factory that recognises the $dynamicAnchor keyword.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(node);

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
        ObjectNode node = mapper.createObjectNode();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(node);

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