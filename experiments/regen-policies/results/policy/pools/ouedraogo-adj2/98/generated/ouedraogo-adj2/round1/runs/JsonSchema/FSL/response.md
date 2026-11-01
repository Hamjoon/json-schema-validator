###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link JsonSchema}.  The tests focus on the public behaviour
 * that can be exercised without needing to mock any of the library internals.
 * All objects are created via the real constructors / factory methods that
 * appear in the source code or the provided signatures.
 */
public class JsonSchemaTest {

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private SchemaValidatorsConfig defaultConfig;
    private JsonNode emptySchemaNode;
    private SchemaLocation baseLocation;
    private JsonNodePath basePath;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        // create a very small configuration – the defaults are sufficient for the tests
        defaultConfig = new SchemaValidatorsConfig();
        defaultConfig.setLocale(Locale.US);

        // JsonMetaSchema and JsonSchemaFactory are part of the library and have a static factory method.
        // The exact method name may differ between versions; the most common one is getInstance(VersionFlag).
        JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(VersionFlag.V201909);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);

        validationContext = new ValidationContext(metaSchema, factory, defaultConfig);

        emptySchemaNode = mapper.createObjectNode(); // "{}"
        baseLocation = SchemaLocation.of("http://example.com/schema.json");
        basePath = new JsonNodePath(PathType.JSON_POINTER);
    }

    /**
     * Helper to create a JsonSchema from a JSON string.
     */
    private JsonSchema createSchema(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        return JsonSchema.from(validationContext, baseLocation, basePath, node, null, false);
    }

    @Test
    public void testFromRefCreatesNewInstanceWithSameIdAndSchemaNode() throws Exception {
        JsonSchema original = createSchema("{\"type\":\"string\"}");
        // give the original a non‑null id via a $id keyword
        ((ObjectNode) original.getSchemaNode()).put("$id", "http://example.com/base");

        JsonSchema parentRef = createSchema("{}"); // dummy parent for the reference
        JsonNodePath refPath = new JsonNodePath(PathType.JSON_POINTER).append("ref");

        JsonSchema derived = original.fromRef(parentRef, refPath);

        // The derived schema must keep the original id
        assertEquals(original.getId(), derived.getId());

        // The schema node must be the same as the original (the JSON object that defines the schema)
        assertSame(original.getSchemaNode(), derived.getSchemaNode());

        // The evaluation path must be the one supplied to fromRef
        assertEquals(refPath, derived.getEvaluationPath());

        // The parent schema of the derived instance must be the supplied parentRef
        assertSame(parentRef, derived.getParentSchema());
    }

    @Test
    public void testWithConfigReturnsSameInstanceWhenConfigUnchanged() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"number\"}");
        JsonSchema result = schema.withConfig(defaultConfig);
        // When the config object is exactly the same, the method should return the original instance
        assertSame(schema, result);
    }

    @Test
    public void testWithConfigCreatesNewInstanceWhenConfigChanged() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"boolean\"}");

        // create a different configuration object
        SchemaValidatorsConfig newConfig = new SchemaValidatorsConfig();
        newConfig.setLocale(Locale.CANADA);
        newConfig.setFailFast(true); // change something

        JsonSchema result = schema.withConfig(newConfig);

        // a new JsonSchema should be returned
        assertNotSame(schema, result);
        // the new instance must keep the original id (null in this case)
        assertNull(result.getId());
        // the validator list must be lazily re‑created – we only check that the object exists
        assertNotNull(result.getValidators());
    }

    @Test
    public void testIsSchemaResourceRootBasedOnIdAndParent() throws Exception {
        // Schema with an explicit $id – should be a resource root
        JsonSchema withId = createSchema("{\"$id\":\"http://example.com/withId\"}");
        assertTrue(withId.isSchemaResourceRoot());

        // Schema without $id and without a parent – also a root (document root)
        JsonSchema documentRoot = createSchema("{\"type\":\"object\"}");
        assertTrue(documentRoot.isSchemaResourceRoot());

        // Child schema without $id but with a parent that shares the same absolute IRI – NOT a resource root
        JsonSchema parent = createSchema("{\"type\":\"object\"}");
        JsonSchema child = JsonSchema.from(validationContext, baseLocation, basePath, emptySchemaNode, parent, false);
        assertFalse(child.isSchemaResourceRoot());

        // Child schema with a different absolute IRI (simulated by a different SchemaLocation) – should be a root
        SchemaLocation otherLocation = SchemaLocation.of("http://example.com/other.json");
        JsonSchema childDifferentIri = JsonSchema.from(validationContext, otherLocation, basePath, emptySchemaNode, parent, false);
        assertTrue(childDifferentIri.isSchemaResourceRoot());
    }

    @Test
    public void testFindLexicalRootStopsAtFirstAncestorHavingId() throws Exception {
        // Grand‑parent with $id
        JsonSchema grandParent = createSchema("{\"$id\":\"http://example.com/grand\"}");
        // Parent without $id
        JsonSchema parent = JsonSchema.from(validationContext, baseLocation, basePath, emptySchemaNode, grandParent, false);
        // Child without $id
        JsonSchema child = JsonSchema.from(validationContext, baseLocation, basePath, emptySchemaNode, parent, false);

        JsonSchema lexicalRoot = child.findLexicalRoot();

        // The lexical root must be the first ancestor that has a non‑null id (grandParent)
        assertSame(grandParent, lexicalRoot);
    }

    @Test
    public void testFindAncestorReturnsTopmostParent() throws Exception {
        JsonSchema root = createSchema("{\"type\":\"object\"}");
        JsonSchema level1 = JsonSchema.from(validationContext, baseLocation, basePath, emptySchemaNode, root, false);
        JsonSchema level2 = JsonSchema.from(validationContext, baseLocation, basePath, emptySchemaNode, level1, false);
        JsonSchema level3 = JsonSchema.from(validationContext, baseLocation, basePath, emptySchemaNode, level2, false);

        JsonSchema ancestor = level3.findAncestor();
        assertSame(root, ancestor);
    }

    @Test
    public void testInitializeValidatorsLoadsValidatorsAndSetsFlag() throws Exception {
        // Simple schema containing two validators: "type" and "minimum"
        JsonSchema schema = createSchema("{\"type\":\"integer\",\"minimum\":5}");

        // validators are lazy – before initialise the flag must be false
        assertFalse(schema.validatorsLoaded);
        assertTrue(schema.getValidators().size() >= 2); // ensure validators have been created

        // invoke initialise
        schema.initializeValidators();

        // after initialise the flag must be true
        assertTrue(schema.validatorsLoaded);
    }

    @Test
    public void testHasTypeValidatorDetectsPresenceOfTypeKeyword() throws Exception {
        JsonSchema withType = createSchema("{\"type\":\"string\"}");
        assertTrue(withType.hasTypeValidator());

        JsonSchema withoutType = createSchema("{\"minimum\":0}");
        assertFalse(withoutType.hasTypeValidator());
    }

    @Test
    public void testCreateExecutionContextCopiesConfiguration() throws Exception {
        // enable format assertions via the config
        defaultConfig.setFormatAssertionsEnabled(Boolean.TRUE);
        defaultConfig.setFailFast(Boolean.TRUE);
        JsonSchema schema = createSchema("{\"type\":\"null\"}");

        ExecutionContext ctx = schema.createExecutionContext();

        // The execution config should reflect the values from the validation config
        assertEquals(Boolean.TRUE, ctx.getExecutionConfig().getFormatAssertionsEnabled());
        assertTrue(ctx.isFailFast());
    }

    @Test
    public void testGetRefSchemaNodeHandlesSimpleJsonPointer() throws Exception {
        // schema with a simple property definition
        String schemaJson = "{\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonSchema schema = createSchema(schemaJson);

        // reference to the "name" property using a JSON Pointer
        JsonNode refNode = schema.getRefSchemaNode("#/properties/name");
        assertNotNull(refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeReturnsMissingWhenPointerNotFound() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        // The implementation returns a missing node (Jackson's MissingNode) when not found
        assertTrue(result.isMissingNode());
    }

    @Test
    public void testToStringProducesReadableRepresentation() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"boolean\"}");
        String representation = schema.toString();
        // Should contain the evaluation path (which for the root is empty) and the JSON text
        assertTrue(representation.startsWith("\"\""));
        assertTrue(representation.contains("\"type\":\"boolean\""));
    }
}
###Test END##