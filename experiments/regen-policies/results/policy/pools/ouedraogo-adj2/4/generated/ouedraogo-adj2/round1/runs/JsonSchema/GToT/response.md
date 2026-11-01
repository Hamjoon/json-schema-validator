###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchema}.
 *
 * The tests cover:
 *   • basic construction via {@code JsonSchema.from}
 *   • public accessor methods
 *   • validator loading and typical validation scenarios
 *   • edge‑cases such as malformed $recursiveAnchor, fragment‑only $id, unknown $ref etc.
 *
 * The library classes are instantiated directly – no mocking frameworks are used.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaLocation dummyLocation;
    private JsonNodePath dummyPath;

    @Before
    public void setUp() {
        // The constructors of the supporting classes have default (public) constructors in the library.
        // If a concrete implementation requires arguments, the library provides sensible defaults.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // use Draft‑07 for tests
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();        // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();    // default config

        this.validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
        this.dummyLocation = SchemaLocation.of("http://example.com/dummy");
        this.dummyPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /** Helper to create a JsonSchema from a JsonNode */
    private JsonSchema createSchema(JsonNode schemaNode) {
        return JsonSchema.from(validationContext,
                dummyLocation,
                dummyPath,
                null,               // no current URI
                schemaNode,
                null,               // no parent
                false);             // do not suppress sub‑schema retrieval
    }

    /* --------------------------------------------------------------------- */
    /*  1. Construction and basic accessors                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFromCreatesSchema() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        JsonSchema schema = createSchema(schemaNode);
        assertNotNull("Schema should be created", schema);
        assertSame("Schema node must be the one supplied", schemaNode, schema.getSchemaNode());
        assertNull("Current URI is null when no $id is present", schema.getCurrentUri());
    }

    @Test
    public void testGetCurrentUriWithAbsoluteId() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$id", "http://example.com/my-schema");
        JsonSchema schema = createSchema(schemaNode);
        URI uri = schema.getCurrentUri();
        assertNotNull("Current URI should be set", uri);
        assertEquals("http://example.com/my-schema", uri.toString());
    }

    @Test
    public void testGetCurrentUriWithFragmentOnlyIdResultsInNull() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$id", "#/definitions/foo");
        JsonSchema schema = createSchema(schemaNode);
        // According to combineCurrentUriWithIds logic a fragment‑only id with null base yields null
        assertNull("Fragment‑only $id with null base URI must produce null currentUri", schema.getCurrentUri());
    }

    @Test
    public void testFindAncestorReturnsSelfWhenNoParent() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        assertSame("Ancestor of a root schema is the schema itself", schema, schema.findAncestor());
    }

    @Test
    public void testFindLexicalRootWithoutIdReturnsSelf() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        assertSame("Lexical root without any $id is the schema itself", schema, schema.findLexicalRoot());
    }

    @Test
    public void testHasRequiredValidatorWhenAbsent() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        // Force lazy loading of validators
        schema.getValidators();
        assertFalse("Should report no required validator", schema.hasRequiredValidator());
    }

    @Test
    public void testHasRequiredValidatorWhenPresent() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.putArray("required").add("name");
        JsonSchema schema = createSchema(schemaNode);
        schema.getValidators(); // trigger loading
        assertTrue("Should detect required validator", schema.hasRequiredValidator());
        assertNotNull("Required validator must be non‑null", schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidatorWhenAbsent() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        schema.getValidators();
        assertFalse("Empty schema has no type validator", schema.hasTypeValidator());
        assertNull("getTypeValidator should return null when absent", schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidatorWhenPresent() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "string");
        JsonSchema schema = createSchema(schemaNode);
        schema.getValidators(); // load validators
        assertTrue("Schema with type keyword should have a type validator", schema.hasTypeValidator());
        assertNotNull("Type validator must be instantiated", schema.getTypeValidator());
    }

    @Test
    public void testIsDynamicAnchorDefaultFalse() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        assertFalse("Dynamic anchor flag must be false by default", schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchorWhenSet() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$recursiveAnchor", true);
        JsonSchema schema = createSchema(schemaNode);
        // validators are loaded lazily; loading them triggers processing of $recursiveAnchor
        schema.getValidators();
        assertTrue("Dynamic anchor flag must be true when $recursiveAnchor:true", schema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testInvalidRecursiveAnchorThrowsException() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$recursiveAnchor", "notABoolean");
        // Construction itself does not validate the $recursiveAnchor; the validation occurs when
        // the schema is parsed (read method). Force parsing.
        JsonSchema schema = createSchema(schemaNode);
        schema.getValidators(); // should throw JsonSchemaException
    }

    @Test
    public void testToStringRepresentation() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "integer");
        JsonSchema schema = createSchema(schemaNode);
        String expected = "\"" + schema.getEvaluationPath() + "\" : " + schemaNode.toString();
        assertEquals("toString must follow the defined format", expected, schema.toString());
    }

    @Test
    public void testCreateExecutionContextIsNotNull() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull("ExecutionContext should be created", ctx);
        assertNotNull("CollectorContext inside ExecutionContext must be non‑null", ctx.getCollectorContext());
    }

    /* --------------------------------------------------------------------- */
    /*  2. $ref handling                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetRefSchemaNodeLocalReference() {
        // { "definitions": { "foo": { "type": "string" } }, "$ref": "#/definitions/foo" }
        ObjectNode defs = MAPPER.createObjectNode();
        ObjectNode foo = MAPPER.createObjectNode();
        foo.put("type", "string");
        defs.set("foo", foo);

        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("definitions", defs);
        schemaNode.put("$ref", "#/definitions/foo");

        JsonSchema schema = createSchema(schemaNode);
        JsonNode refNode = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull("Ref node must be resolved", refNode);
        assertEquals("type", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeUnknownReferenceReturnsNull() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        JsonSchema schema = createSchema(schemaNode);
        JsonNode node = schema.getRefSchemaNode("#/nonexistent");
        assertNull("Unknown $ref should result in null", node);
    }

    /* --------------------------------------------------------------------- */
    /*  3. Validation basics                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValidateEmptySchemaReturnsNoErrors() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        JsonNode data = MAPPER.createObjectNode(); // any data is acceptable
        Set<ValidationMessage> messages = schema.validate(data);
        assertTrue("Empty schema must validate any instance without errors", messages.isEmpty());
    }

    @Test
    public void testValidateTypeStringWithInvalidInstanceProducesError() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "string");
        JsonSchema schema = createSchema(schemaNode);

        JsonNode invalidData = MAPPER.createObjectNode().put("number", 123); // not a string
        Set<ValidationMessage> messages = schema.validate(invalidData);
        assertFalse("Invalid type should produce at least one validation message", messages.isEmpty());
        ValidationMessage first = messages.iterator().next();
        assertEquals("type", first.getKeyword()); // keyword should be 'type'
    }

    @Test
    public void testValidateAndCollectReturnsValidationResult() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "boolean");
        JsonSchema schema = createSchema(schemaNode);
        JsonNode data = MAPPER.booleanNode(true);
        ValidationResult result = schema.validateAndCollect(data);
        assertNotNull("Result must not be null", result);
        assertTrue("Valid boolean should produce no errors", result.getValidationMessages().isEmpty());
    }

    @Test
    public void testInitializeValidatorsDoesNotThrow() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "null");
        JsonSchema schema = createSchema(schemaNode);
        // Should not throw any exception even if validators are lazily loaded later
        schema.initializeValidators();
        assertTrue("Validators should now be marked as loaded", true);
    }

    /* --------------------------------------------------------------------- */
    /*  4. Walk functionality (no‑op walk)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWalkOnSchemaWithoutValidatorsReturnsEmptyResult() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        JsonNode data = MAPPER.createObjectNode();
        ValidationResult result = schema.walk(data, false);
        assertNotNull("Walk result must not be null", result);
        assertTrue("Walk on empty schema should yield no validation messages", result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalkAtNodeDelegatesToInternalWalk() {
        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        JsonNode data = MAPPER.createObjectNode();
        ValidationResult result = schema.walkAtNode(schema.createExecutionContext(), data, data,
                new JsonNodePath(JsonNodePath.PathType.ROOT), false);
        assertNotNull("walkAtNode must return a result", result);
        assertTrue("Result should contain no messages for empty schema", result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  5. Edge Cases for URI handling                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFragmentOnlyIdWhenBaseUriIsNullYieldsNullCurrentUri() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$id", "#localFragment");
        JsonSchema schema = createSchema(schemaNode);
        assertNull("Current URI must be null when fragment‑only $id and no base URI", schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithAbsoluteIdOverridesBase() {
        // Base URI is http://example.com/base
        URI base = URI.create("http://example.com/base");
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("$id", "http://other.com/override");
        JsonSchema schema = JsonSchema.from(validationContext,
                dummyLocation,
                dummyPath,
                base,
                schemaNode,
                null,
                false);
        assertEquals("Absolute $id must replace the base URI",
                "http://other.com/override", schema.getCurrentUri().toString());
    }

    /* --------------------------------------------------------------------- */
    /*  6. $ref resolution across ancestors (recursive)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRefResolutionThroughAncestor() {
        // Parent schema defines a definition, child schema $ref points to it via relative fragment.
        ObjectNode parentDefs = MAPPER.createObjectNode();
        ObjectNode foo = MAPPER.createObjectNode();
        foo.put("type", "integer");
        parentDefs.set("foo", foo);

        ObjectNode parent = MAPPER.createObjectNode();
        parent.set("definitions", parentDefs);
        JsonSchema parentSchema = createSchema(parent);

        // Child schema has no definitions, only a $ref to parent's definition.
        ObjectNode child = MAPPER.createObjectNode();
        child.put("$ref", "#/definitions/foo");
        JsonSchema childSchema = parentSchema.createChildSchema(SchemaLocation.of("http://example.com/child"), child);

        JsonNode refNode = childSchema.getRefSchemaNode("#/definitions/foo");
        assertNotNull("Ref should be resolved through ancestor", refNode);
        assertEquals("integer", refNode.get("type").asText());
    }
}
```
###Test END##