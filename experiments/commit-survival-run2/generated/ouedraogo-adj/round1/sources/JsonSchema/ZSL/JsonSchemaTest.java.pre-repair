package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive tests for {@link JsonSchema}.
 *
 * The tests are written in the same package as {@code JsonSchema} to allow access to
 * package‑private factory methods such as {@code JsonSchema.from(...)}.
 *
 * The test suite covers:
 * <ul>
 *   <li>URI handling</li>
 *   <li>Ancestor / lexical root resolution</li>
 *   <li>Detection of special validators (required, type, $recursiveAnchor)</li>
 *   <li>String representation</li>
 *   <li>Execution context creation</li>
 *   <li>Basic validation (valid & invalid data)</li>
 *   <li>Initializer for eager validator loading</li>
 * </ul>
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    @Before
    public void setUp() {
        // Create a minimal ValidationContext.  The factories are not required for the
        // behaviours exercised in this test suite because the schemas used do NOT
        // contain an "$id" or "$ref".  The JsonMetaSchema provides the necessary
        // keyword information.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(
                null,                     // URIFactory – not needed for these tests
                null,                     // URNFactory – not needed
                JsonMetaSchema.getV4(),   // Use Draft‑4 meta‑schema
                null,                     // JsonSchemaFactory – not needed
                config);                  // Configuration
    }

    /** Helper to create a JsonSchema from a JSON string. */
    private JsonSchema createSchema(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return JsonSchema.from(validationContext, "#", null, node, null, false);
    }

    @Test
    public void testGetCurrentUri_NoId_ReturnsNull() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        assertNull("Current URI should be null when no $id is present", schema.getCurrentUri());
    }

    @Test
    public void testGetCurrentUri_WithBaseUri_NoId_ReturnsProvidedUri() throws Exception {
        URI base = new URI("http://example.com/schema.json");
        JsonNode node = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", base, node, null, false);
        // Because the schema does not contain an $id, the current URI stays as the base URI.
        assertEquals(base, schema.getCurrentUri());
    }

    @Test
    public void testFindAncestor_ReturnsRootSchema() throws Exception {
        JsonSchema parent = createSchema("{\"type\":\"object\"}");
        JsonSchema child = parent.createChildSchema("#/properties/name", MAPPER.readTree("{\"type\":\"string\"}"));
        // child.findAncestor() should walk up to the root (parent) because the parent has no further parent.
        assertSame("Ancestor of child should be the parent schema", parent, child.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_WithIdStopsAtAncestorHavingId() throws Exception {
        // Parent schema contains an $id, child does not.
        JsonSchema parent = createSchema("{\"$id\":\"http://example.com/parent\",\"type\":\"object\"}");
        JsonSchema child = parent.createChildSchema("#/properties/name", MAPPER.readTree("{\"type\":\"string\"}"));
        // The lexical root for the child should be the parent because it has an $id.
        assertSame("Lexical root should be the ancestor that has an $id", parent, child.findLexicalRoot());
    }

    @Test
    public void testHasRequiredValidator_TrueWhenRequiredPresent() throws Exception {
        JsonSchema schema = createSchema("{\"required\":[\"name\"]}");
        assertTrue("Schema should report having a required validator", schema.hasRequiredValidator());
        assertNotNull("Required validator instance should not be null", schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidator_FalseWhenAbsent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        assertFalse("Schema should not report a required validator", schema.hasRequiredValidator());
        assertNull("Required validator should be null", schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_TrueWhenTypePresent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        assertTrue("Schema should report having a type validator", schema.hasTypeValidator());
        assertNotNull("Type validator instance should not be null", schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidator_FalseWhenAbsent() throws Exception {
        JsonSchema schema = createSchema("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        assertFalse("Schema should not report a type validator at the root level", schema.hasTypeValidator());
        assertNull("Root type validator should be null", schema.getTypeValidator());
    }

    @Test
    public void testIsDynamicAnchor_TrueWhenRecursiveAnchorTrue() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":true}");
        assertTrue("Schema should recognise a dynamic anchor when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchor_FalseWhenRecursiveAnchorFalse() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":false}");
        assertFalse("Schema should not be dynamic when $recursiveAnchor is false", schema.isDynamicAnchor());
    }

    @Test
    public void testInitializeValidators_EagerLoading() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\",\"required\":[\"name\"]}");
        // First call – validators should be loaded.
        schema.initializeValidators();
        // Second call – should be a no‑op and not throw.
        schema.initializeValidators();

        // Use reflection to verify the internal flag was set.
        java.lang.reflect.Field loadedField = JsonSchema.class.getDeclaredField("validatorsLoaded");
        loadedField.setAccessible(true);
        boolean loaded = (boolean) loadedField.get(schema);
        assertTrue("validatorsLoaded flag should be true after initialization", loaded);
    }

    @Test
    public void testToStringFormat() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        String str = schema.toString();
        assertTrue("toString should contain the schema path", str.contains("\"#\""));
        assertTrue("toString should contain the JSON representation of the schema", str.contains("\"type\":\"string\""));
    }

    @Test
    public void testCreateExecutionContext_ReturnsValidContext() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull("ExecutionContext should not be null", ctx);
        assertNotNull("CollectorContext inside ExecutionContext should not be null", ctx.getCollectorContext());
    }

    @Test
    public void testValidateValidString() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        JsonNode data = MAPPER.readTree("\"hello world\"");
        Set<ValidationMessage> errors = schema.validate(data);
        assertTrue("Valid string should produce no validation errors", errors.isEmpty());
    }

    @Test
    public void testValidateInvalidTypeProducesError() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"string\"}");
        JsonNode data = MAPPER.readTree("123");
        Set<ValidationMessage> errors = schema.validate(data);
        assertFalse("Invalid type should produce validation errors", errors.isEmpty());
        // The error code for type mismatch is typically "type". Verify at least one message contains it.
        boolean containsTypeError = errors.stream().anyMatch(v -> "type".equals(v.getCode()));
        assertTrue("At least one validation message should have code 'type'", containsTypeError);
    }
}
