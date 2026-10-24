package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class JsonSchemaTest {

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        // Minimal ValidationContext – factories are not needed for the tests we run
        config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(
                null,                    // URIFactory (unused because schemas have no $id)
                null,                    // URNFactory (unused)
                JsonMetaSchema.getV7(), // meta‑schema
                null,                    // JsonSchemaFactory (unused)
                config);                 // configuration
    }

    /** Helper to create a JsonSchema from a JSON string */
    private JsonSchema createSchema(String json, URI currentUri) throws Exception {
        JsonNode node = mapper.readTree(json);
        return JsonSchema.from(validationContext, "#", currentUri, node, null, false);
    }

    @Test
    public void testGetCurrentUriWhenNoId() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}", null);
        assertNull("Current URI should be null when no base URI and no $id", schema.getCurrentUri());
    }

    @Test
    public void testGetCurrentUriWithIdResolution() throws Exception {
        URI base = new URI("http://example.com/base.json");
        JsonSchema schema = createSchema("{\"$id\":\"http://example.com/inner.json\"}", base);
        assertEquals("http://example.com/inner.json", schema.getCurrentUri().toString());
    }

    @Test
    public void testGetRefSchemaNodeFragmentResolution() throws Exception {
        String schemaJson = "{\n" +
                "  \"definitions\": {\n" +
                "    \"foo\": {\"type\":\"string\"}\n" +
                "  },\n" +
                "  \"$ref\": \"#/definitions/foo\"\n" +
                "}";
        JsonSchema schema = createSchema(schemaJson, null);
        JsonNode refNode = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull("Ref node should not be null", refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testFindLexicalRootStopsAtId() throws Exception {
        // Parent schema with $id
        JsonSchema parent = createSchema("{\"$id\":\"http://example.com/parent\"}", null);
        // Child schema without $id
        JsonSchema child = parent.createChildSchema("#/child", mapper.readTree("{\"type\":\"object\"}"));
        // The lexical root of child should be the parent (the first schema that has an id)
        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame("Lexical root should be the parent schema that has an $id", parent, lexicalRoot);
    }

    @Test
    public void testFindAncestorReturnsRoot() throws Exception {
        JsonSchema root = createSchema("{\"type\":\"object\"}", null);
        JsonSchema child = root.createChildSchema("#/child", mapper.readTree("{\"type\":\"string\"}"));
        JsonSchema grandChild = child.createChildSchema("#/grand", mapper.readTree("{\"type\":\"integer\"}"));
        assertSame("Ancestor of grandChild should be the root schema", root, grandChild.findAncestor());
    }

    @Test
    public void testHasRequiredAndTypeValidators() throws Exception {
        String schemaJson = "{\n" +
                "  \"type\":\"object\",\n" +
                "  \"required\":[\"name\"]\n" +
                "}";
        JsonSchema schema = createSchema(schemaJson, null);
        assertTrue("Schema should have a required validator", schema.hasRequiredValidator());
        assertTrue("Schema should have a type validator", schema.hasTypeValidator());
        assertNotNull("Required validator should be accessible", schema.getRequiredValidator());
        assertNotNull("Type validator should be accessible", schema.getTypeValidator());
    }

    @Test
    public void testDynamicAnchorRecognition() throws Exception {
        String schemaJson = "{ \"$recursiveAnchor\": true }";
        JsonSchema schema = createSchema(schemaJson, null);
        assertTrue("Dynamic anchor flag should be true when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test
    public void testInitializeValidatorsIdempotent() throws Exception {
        String schemaJson = "{ \"type\":\"string\" }";
        JsonSchema schema = createSchema(schemaJson, null);
        // First call should set the internal flag
        schema.initializeValidators();
        // Second call must not throw or change state
        schema.initializeValidators();
        // No explicit assertion – the absence of exception proves idempotence
    }

    @Test
    public void testToStringFormatting() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"boolean\"}", null);
        String expectedStart = "\"#\" : {\"type\":\"boolean\"}";
        assertEquals("toString should produce a JSON‑like representation", expectedStart, schema.toString());
    }

    @Test
    public void testValidateEmptySchemaReturnsNoErrors() throws Exception {
        JsonSchema schema = createSchema("{}", null);
        JsonNode instance = mapper.readTree("{\"any\":\"thing\"}");
        Set<ValidationMessage> errors = schema.validate(instance);
        assertTrue("Empty schema should produce no validation errors", errors.isEmpty());
    }

    @Test
    public void testValidateAndCollectReturnsEmptyResult() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}", null);
        JsonNode instance = mapper.readTree("{\"a\":1}");
        ValidationResult result = schema.validateAndCollect(instance);
        assertNotNull("ValidationResult must not be null", result);
        assertTrue("No errors expected for valid instance", result.getValidationMessages().isEmpty());
        // CollectorContext inside the result should be non‑null
        assertNotNull("CollectorContext must be present", result.getCollectorContext());
    }

    @Test
    public void testWalkWithNoValidatorsReturnsEmptySet() throws Exception {
        JsonSchema schema = createSchema("{}", null);
        JsonNode instance = mapper.readTree("{\"foo\":\"bar\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        Set<ValidationMessage> messages = schema.walk(ctx, instance, false);
        assertTrue("Walk on schema with no validators should return empty set", messages.isEmpty());
    }
}
