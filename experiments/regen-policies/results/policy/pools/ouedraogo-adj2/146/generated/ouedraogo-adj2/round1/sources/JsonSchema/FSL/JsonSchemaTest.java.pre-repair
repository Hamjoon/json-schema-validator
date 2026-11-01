package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link JsonSchema}. The tests create real {@link JsonSchema}
 * instances using the public factory methods and exercise the public API.
 * No mocking is used – only classes that appear in the supplied source or in the
 * documented public APIs are instantiated.
 */
public class JsonSchemaTest {

    private ValidationContext validationContext;
    private SchemaLocation rootLocation;
    private JsonNodePath rootPath;
    private ObjectNode emptySchemaNode;
    private JsonSchema emptySchema;

    @Before
    public void setUp() {
        // Build the minimal objects required to create a JsonSchema instance.
        // The library provides a default meta‑schema (2020‑12) and a default
        // JsonSchemaFactory.  The default configuration is sufficient for the
        // majority of tests that do not rely on specific keywords.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();                 // standard meta‑schema
        JsonSchemaFactory factory = new JsonSchemaFactory();                     // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();           // default config

        validationContext = new ValidationContext(metaSchema, factory, config);

        rootLocation = SchemaLocation.DOCUMENT;                                   // the document root
        rootPath = new JsonNodePath(PathType.JSON_POINTER);                      // empty JSON‑Pointer path

        emptySchemaNode = JsonNodeFactory.instance.objectNode();                  // {}  – an empty schema
        emptySchema = JsonSchema.from(validationContext, rootLocation, rootPath,
                emptySchemaNode, null, false);
    }

    /**
     * Verify that an empty schema has no $id, is considered a schema‑resource root,
     * and that {@code findSchemaResourceRoot()} returns the instance itself.
     */
    @Test
    public void testRootIdentification() {
        assertNull("Empty schema should have no $id", emptySchema.getId());
        assertTrue("Empty schema should be a schema‑resource root",
                emptySchema.isSchemaResourceRoot());
        assertSame("findSchemaResourceRoot must return itself for a root schema",
                emptySchema, emptySchema.findSchemaResourceRoot());
        assertSame("findLexicalRoot must return itself when there is no ancestor with an id",
                emptySchema, emptySchema.findLexicalRoot());
    }

    /**
     * The default schema has no type validator, no recursive anchor and an empty
     * validator list.
     */
    @Test
    public void testDefaultState() {
        assertFalse("No type validator should be present", emptySchema.hasTypeValidator());
        assertNull("getTypeValidator must return null when none exists", emptySchema.getTypeValidator());
        assertFalse("Recursive anchor flag must be false by default", emptySchema.isRecursiveAnchor());

        List<JsonValidator> validators = emptySchema.getValidators();
        assertNotNull("Validator list must never be null", validators);
        assertTrue("Empty schema should have zero validators", validators.isEmpty());
    }

    /**
     * {@code initializeValidators()} must be safe to call even when there are no
     * validators to preload.
     */
    @Test
    public void testInitializeValidatorsOnEmptySchema() {
        emptySchema.initializeValidators(); // should not throw
        // After initialization the flag must be true
        assertTrue("validatorsLoaded flag should be true after initialization",
                (Boolean) getPrivateField(emptySchema, "validatorsLoaded"));
    }

    /**
     * The {@code toString()} implementation should contain the evaluation path
     * (which is empty for the root) and the JSON representation of the schema node.
     */
    @Test
    public void testToStringRepresentation() {
        String s = emptySchema.toString();
        assertTrue("toString must start with a quoted empty path", s.startsWith("\"\""));
        assertTrue("toString must contain the JSON node representation", s.contains("{}"));
    }

    /**
     * Create a schema that contains a simple property and verify that
     * {@code getSubSchema(JsonNodePath)} correctly resolves the sub‑schema.
     */
    @Test
    public void testGetSubSchemaByJsonPointer() {
        // {"foo": {"type":"string"}}
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ObjectNode foo = JsonNodeFactory.instance.objectNode();
        foo.put("type", "string");
        root.set("foo", foo);

        JsonSchema schema = JsonSchema.from(validationContext, rootLocation, rootPath,
                root, null, false);

        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER).append("foo");
        JsonSchema subSchema = schema.getSubSchema(fragment);

        assertNotNull("Sub‑schema for '/foo' must not be null", subSchema);
        JsonNode subNode = subSchema.getSchemaNode();
        assertTrue("Sub‑schema node must contain a 'type' property", subNode.has("type"));
        assertEquals("type must be 'string'", "string", subNode.get("type").asText());
    }

    /**
     * Verify that {@code getRefSchemaNode(String)} correctly resolves a JSON
     * Pointer reference inside the same document.
     */
    @Test
    public void testGetRefSchemaNode() {
        // {"bar": {"type":"integer"}}
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ObjectNode bar = JsonNodeFactory.instance.objectNode();
        bar.put("type", "integer");
        root.set("bar", bar);

        JsonSchema schema = JsonSchema.from(validationContext, rootLocation, rootPath,
                root, null, false);

        JsonNode node = schema.getRefSchemaNode("#/bar");
        assertNotNull("Reference '#/bar' must resolve to a node", node);
        assertTrue("Resolved node must have a 'type' field", node.has("type"));
        assertEquals("type must be 'integer'", "integer", node.get("type").asText());
    }

    /**
     * The {@code fromRef} method should produce a new {@link JsonSchema}
     * instance that re‑uses the original schema node but has a different
     * evaluation path.
     */
    @Test
    public void testFromRefCreatesNewSchema() {
        JsonSchema refParent = JsonSchema.from(validationContext, rootLocation, rootPath,
                emptySchemaNode, null, false);
        JsonNodePath refPath = new JsonNodePath(PathType.JSON_POINTER).append("ref");

        JsonSchema derived = emptySchema.fromRef(refParent, refPath);

        assertNotSame("fromRef must create a distinct JsonSchema instance", emptySchema, derived);
        assertSame("The schema node must be shared", emptySchemaNode, derived.getSchemaNode());
        assertEquals("Evaluation path must be the one supplied to fromRef",
                refPath, derived.getEvaluationPath());
        assertSame("Parent schema must be the supplied reference parent", refParent,
                derived.getParentSchema());
    }

    /**
     * {@code withConfig} returns the same instance when the supplied configuration
     * equals the current configuration, otherwise it returns a new instance.
     */
    @Test
    public void testWithConfigBehaviour() {
        // The config used to build the original schema
        SchemaValidatorsConfig originalConfig = validationContext.getConfig();

        // Same configuration – should return the same instance
        JsonSchema same = emptySchema.withConfig(originalConfig);
        assertSame("withConfig with identical config must return the same instance", emptySchema, same);

        // Different configuration – expect a new instance
        SchemaValidatorsConfig other = new SchemaValidatorsConfig();
        other.setFailFast(!originalConfig.isFailFast()); // toggle a flag
        JsonSchema altered = emptySchema.withConfig(other);
        assertNotSame("withConfig with different config must create a new instance", emptySchema, altered);
    }

    /**
     * Verify that {@code getRefSchema(JsonNodePath)} works for an anchor reference.
     * The test builds a schema with an explicit anchor and checks that the returned
     * JsonSchema matches the anchored sub‑schema.
     */
    @Test
    public void testGetRefSchemaWithAnchor() {
        // {"$id":"http://example.com/root","components":{"myAnchor":{"$anchor":"myAnchor","type":"boolean"}}}
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("$id", "http://example.com/root");

        ObjectNode components = JsonNodeFactory.instance.objectNode();
        ObjectNode myAnchor = JsonNodeFactory.instance.objectNode();
        myAnchor.put("$anchor", "myAnchor");
        myAnchor.put("type", "boolean");
        components.set("myAnchor", myAnchor);
        root.set("components", components);

        JsonSchema schema = JsonSchema.from(validationContext, SchemaLocation.of("http://example.com/root"),
                rootPath, root, null, false);

        // The fragment representing the anchor (just the name, not a JSON‑Pointer)
        JsonNodePath anchorFragment = new JsonNodePath(PathType.ANCHOR).append("myAnchor");
        JsonSchema anchored = schema.getRefSchema(anchorFragment);

        assertNotNull("Anchor lookup must return a schema", anchored);
        assertEquals("Anchored schema must have type boolean",
                "boolean", anchored.getSchemaNode().get("type").asText());
    }

    /**
     * Helper to retrieve a private field value via reflection – used only for
     * assertions on internal flags that have no public accessor.
     */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
