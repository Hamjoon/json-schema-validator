package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonSchema}. The tests focus on public behaviour and
 * on edge‑case handling that can be exercised without needing the full network
 * of library implementations (the library allows many of its collaborators to be
 * {@code null} for simple use‑cases).
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() {
        // Most collaborators can be passed as {@code null} – the JsonSchema code only
        // accesses them when a feature is actually used (e.g. $id resolution).  This
        // is sufficient for the tests below.
        config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(
                null,                 // URIFactory – not needed for our tests
                null,                 // URNFactory – not needed
                JsonMetaSchema.getV7(),
                null,                 // JsonSchemaFactory – not needed
                config);
    }

    /** ----------------------------------------------------------------------
     *  Helper: creates a JsonSchema instance from a JSON string.
     *  ---------------------------------------------------------------------- */
    private JsonSchema createSchema(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return new JsonSchema(validationContext, URI.create("http://example.com/base"), node);
    }

    /** ----------------------------------------------------------------------
     *  Test public constructors and basic getters.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testGetCurrentUri_NoId() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        // No $id – currentUri should be the base URI we passed.
        assertEquals(URI.create("http://example.com/base"), schema.getCurrentUri());
    }

    @Test
    public void testGetCurrentUri_WithFragmentIdAndNoBase() throws Exception {
        // Build a ValidationContext that has no base URI (null) and a schema
        // containing a fragment $id.  According to the implementation the
        // resulting currentUri must be {@code null}.
        ValidationContext ctx = new ValidationContext(
                null,
                null,
                JsonMetaSchema.getV7(),
                null,
                config);
        JsonNode node = MAPPER.readTree("{\"$id\":\"#/fragment\"}");
        JsonSchema schema = new JsonSchema(ctx, null, node);
        assertNull("Fragment $id without a parent URI must result in null currentUri",
                schema.getCurrentUri());
    }

    @Test
    public void testToString_IncludesPathAndNode() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        String s = schema.toString();
        assertTrue("toString should contain the schema path", s.contains(schema.getSchemaPath()));
        assertTrue("toString should contain the JSON representation", s.contains("\"type\":\"string\""));
    }

    /** ----------------------------------------------------------------------
     *  Test ancestor handling.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testFindAncestor_RootIsSelf() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"null\"}");
        assertSame("Root schema should be its own ancestor", schema, schema.findAncestor());
    }

    @Test
    public void testFindAncestor_WithParent() throws Exception {
        JsonSchema parent = createSchema("{\"type\":\"object\"}");
        JsonSchema child = parent.createChildSchema("#/properties/name", MAPPER.readTree("{\"type\":\"string\"}"));
        assertSame("Parent should be returned as ancestor of child", parent, child.findAncestor());
    }

    /** ----------------------------------------------------------------------
     *  Test $recursiveAnchor handling – this flag is stored in the instance.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testRecursiveAnchor_TrueSetsDynamicAnchor() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":true}");
        assertTrue("Dynamic anchor should be true when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test
    public void testRecursiveAnchor_FalseLeavesDynamicAnchorFalse() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":false}");
        assertFalse("Dynamic anchor should be false when $recursiveAnchor is false", schema.isDynamicAnchor());
    }

    /** ----------------------------------------------------------------------
     *  Test getRefSchemaNode – normal resolution and missing node handling.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testGetRefSchemaNode_ExistingPointer() throws Exception {
        JsonSchema schema = createSchema("{\"definitions\":{\"foo\":{\"type\":\"integer\"}}}");
        JsonNode node = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull("Existing pointer should return a node", node);
        assertEquals("integer", node.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_NonExistingPointerReturnsNull() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        JsonNode node = schema.getRefSchemaNode("#/nonexistent");
        assertNull("Non‑existent pointer should return null", node);
    }

    /** ----------------------------------------------------------------------
     *  Test validator‑related helpers.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testHasRequiredValidator_Absent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        assertFalse("Schema without 'required' keyword must report false", schema.hasRequiredValidator());
        assertNull("Getter should return null when absent", schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidator_Present() throws Exception {
        JsonSchema schema = createSchema("{\"required\":[\"id\"]}");
        assertTrue("Schema with 'required' keyword must report true", schema.hasRequiredValidator());
        assertNotNull("Getter should return a validator instance", schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_Absent() throws Exception {
        JsonSchema schema = createSchema("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        assertFalse("Missing top‑level 'type' keyword must report false", schema.hasTypeValidator());
        assertNull(schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidator_Present() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"array\"}");
        assertTrue("Present 'type' keyword must report true", schema.hasTypeValidator());
        assertNotNull("Getter must return a TypeValidator", schema.getTypeValidator());
    }

    /** ----------------------------------------------------------------------
     *  Test validator initialisation – ensures that the lazy loading flag is set
     *  and that calling the method twice does not throw.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testInitializeValidators_Idempotent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"boolean\"}");
        // First call – should load validators.
        schema.initializeValidators();
        // Second call – must be safe and leave the flag true.
        schema.initializeValidators();
        // No exception means success; we also check that the flag is indeed true
        // via reflection (the field is private, so we use the public behaviour:
        // after initialise, calling any validator should not cause NPE).
        Set<ValidationMessage> errors = schema.validate(MAPPER.readTree("true"));
        assertTrue("A valid boolean instance must produce no errors", errors.isEmpty());
    }

    /** ----------------------------------------------------------------------
     *  Test the simple validate(JsonNode) overload.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testValidateSimple_Valid() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        Set<ValidationMessage> errors = schema.validate(MAPPER.readTree("\"hello\""));
        assertTrue("Valid string must produce no validation messages", errors.isEmpty());
    }

    @Test
    public void testValidateSimple_Invalid() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        Set<ValidationMessage> errors = schema.validate(MAPPER.readTree("123"));
        assertFalse("Invalid instance must produce at least one validation message", errors.isEmpty());
    }

    /** ----------------------------------------------------------------------
     *  Test walk() – the walk implementation delegates to the validator walk
     *  methods.  Using a schema without any walk‑aware validators results in an
     *  empty ValidationResult.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testWalk_NoValidators() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"null\"}");
        ValidationResult result = schema.walk(MAPPER.readTree("null"), false);
        assertTrue("Walking a simple schema should yield no validation messages", result.getValidationMessages().isEmpty());
    }

    /** ----------------------------------------------------------------------
     *  Helper to invoke the overloaded validate(JsonNode) that internally
     *  creates an ExecutionContext.
     *  ---------------------------------------------------------------------- */
    private Set<ValidationMessage> validate(JsonNode node) {
        JsonSchema schema;
        try {
            schema = createSchema("{\"type\":\"number\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return schema.validate(node);
    }

    /** ----------------------------------------------------------------------
     *  Edge case: calling getRefSchemaNode with a URI fragment that points to
     *  the root schema (\"#\") must return the root node itself.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testGetRefSchemaNode_RootFragment() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        JsonNode node = schema.getRefSchemaNode("#");
        assertSame("Root fragment should resolve to the schema node itself", schema.getSchemaNode(), node);
    }

    /** ----------------------------------------------------------------------
     *  Edge case: createChildSchema should produce a child that shares the same
     *  ValidationContext but has a different schemaPath.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testCreateChildSchema_InheritsContextAndPath() throws Exception {
        JsonSchema parent = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode childNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema child = parent.createChildSchema("#/properties/name", childNode);
        assertEquals("Child schema path must be the one supplied", "#/properties/name", child.getSchemaPath());
        assertSame("Child must share the same ValidationContext as the parent", parent.getValidationContext(), child.getValidationContext());
    }
}
