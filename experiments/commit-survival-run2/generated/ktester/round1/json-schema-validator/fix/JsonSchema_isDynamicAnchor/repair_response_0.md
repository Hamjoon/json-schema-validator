```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchema_isDynamicAnchor_Test {

    /** No real ValidationContext is required for these unit‑tests – the private
     *  constructor and the public factory method tolerate a {@code null}
     *  value. */
    private final ValidationContext validationContext = null;

    /** No real schema node is required – the tests either set the
     *  {@code dynamicAnchor} flag via reflection or use a minimal node
     *  created with Jackson. */
    private final JsonNode dummySchemaNode = null;

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
                dummySchemaNode,    // schema node – not required
                null,               // parent – top‑level schema
                false);             // suppressSubSchemaRetrieval – default

        // Set the private {@code dynamicAnchor} field to the desired value.
        Field field = JsonSchema.class.getDeclaredField("dynamicAnchor");
        field.setAccessible(true);
        field.setBoolean(instance, dynamicAnchor);
        return instance;
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

        JsonSchema schema = JsonSchema.from(
                validationContext,
                "schemaWithDynamicAnchor", // currentUri – not needed for this unit test
                null,                     // parent – top‑level
                node,
                null,                     // suppressSubSchemaRetrieval
                false);
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