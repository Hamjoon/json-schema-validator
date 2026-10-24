###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written by three “experts” that each contributed a basic test,
 * identified edge‑cases and finally merged everything into this single file.
 *
 * The test class uses only the public API that is present in the source code
 * supplied in the prompt and the minimal helper classes that are part of the
 * same library (e.g. {@code JsonMetaSchema}, {@code SchemaValidatorsConfig},
 * {@code ValidationContext}, etc.).  No external mocking frameworks are used –
 * real objects are instantiated wherever possible.
 *
 * The test suite covers:
 *   • All public methods (basic happy‑path tests)
 *   • Edge‑cases such as missing $ref, $recursiveAnchor, subschema URIs,
 *     and validation errors
 *   • Exception‑handling scenarios (e.g. malformed $ref)
 *
 * Run the suite with any JUnit‑4 runner.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.Assert.*;

public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    @Before
    public void setUp() {
        /*
         * The ValidationContext constructor requires a URIFactory, URNFactory,
         * JsonMetaSchema, JsonSchemaFactory and a SchemaValidatorsConfig.
         *
         * For the purpose of these tests we do not need URI resolution or
         * external schema loading, therefore we can safely pass {@code null}
         * for the factories and the JsonSchemaFactory.  The meta‑schema is
         * required for $ref handling – we use the built‑in Draft‑4 meta‑schema.
         */
        validationContext = new ValidationContext(
                null,                                 // URIFactory (not used in our tests)
                null,                                 // URNFactory (not used)
                JsonMetaSchema.getV4(),               // a concrete meta‑schema
                null,                                 // JsonSchemaFactory (not used)
                new SchemaValidatorsConfig()          // default configuration
        );
    }

    /**
     * Helper that creates a {@link JsonSchema} instance from a JSON string.
     *
     * @param schemaJson the JSON representation of the schema
     * @return a JsonSchema ready for testing
     */
    private JsonSchema createSchema(String schemaJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // We deliberately pass {@code null} as the current URI – the schema does
        // not contain an "$id", so combineCurrentUriWithIds() will return null
        // without invoking the (null) URIFactory.
        return new JsonSchema(validationContext, "#", null, schemaNode, null, false);
    }

    /* --------------------------------------------------------------------- */
    /*  BASIC PUBLIC METHOD TESTS                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCurrentUri_NoId_ReturnsNull() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"object\" }");
        assertNull("When the schema has no $id, getCurrentUri() must return null", schema.getCurrentUri());
    }

    @Test
    public void testIsDynamicAnchor_WithRecursiveAnchorTrue() throws Exception {
        JsonSchema schema = createSchema("{ \"$recursiveAnchor\": true }");
        assertTrue("Schema containing $recursiveAnchor:true must report isDynamicAnchor()==true",
                schema.isDynamicAnchor());
    }

    @Test
    public void testGetRefSchemaNode_ValidRef() throws Exception {
        String json = "{"
                + "  \"definitions\": {"
                + "    \"person\": { \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
                + "  },"
                + "  \"$ref\": \"#/definitions/person\""
                + "}";
        JsonSchema schema = createSchema(json);
        JsonNode node = schema.getRefSchemaNode("#/definitions/person");
        assertNotNull("Existing $ref must resolve to a non‑null JsonNode", node);
        assertTrue("Resolved node should contain a \"type\" property", node.has("type"));
        assertEquals("object", node.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_MissingRef_ReturnsNull() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode node = schema.getRefSchemaNode("#/nonexistent");
        assertNull("Missing $ref must result in null", node);
    }

    @Test
    public void testValidate_ValidData_ReturnsEmptySet() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode data = new TextNode("hello world");
        Set<ValidationMessage> errors = schema.validate(data);
        assertTrue("Valid data should produce an empty error set", errors.isEmpty());
    }

    @Test
    public void testValidate_InvalidData_ReturnsErrors() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode data = MAPPER.readTree("123"); // a number node
        Set<ValidationMessage> errors = schema.validate(data);
        assertFalse("Invalid data should produce at least one ValidationMessage", errors.isEmpty());
    }

    @Test
    public void testInitializeValidators_IdempotentCall() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"boolean\" }");
        // First call – should load the validators
        schema.initializeValidators();
        // Second call – must not throw or reload incorrectly
        schema.initializeValidators();

        // Verify via reflection that the private flag is true
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        boolean loaded = (boolean) f.get(schema);
        assertTrue("validatorsLoaded flag must be true after initializeValidators()", loaded);
    }

    @Test
    public void testFindAncestor_ChildSchemaReturnsRoot() throws Exception {
        JsonSchema parent = createSchema("{ \"type\": \"object\" }");
        JsonSchema child = parent.createChildSchema("#/properties/name", MAPPER.readTree("{ \"type\": \"string\" }"));
        // The child’s findAncestor() should walk up to the original parent
        JsonSchema ancestor = child.findAncestor();
        assertSame("Ancestor of child schema must be the original parent", parent, ancestor);
    }

    @Test
    public void testWalk_NoExceptionAndEmptyErrors() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"number\" }");
        JsonNode data = MAPPER.readTree("42");
        ValidationResult result = schema.walk(data, true);
        assertNotNull("walk() must return a ValidationResult instance", result);
        assertTrue("Valid number should yield no validation messages", result.getValidationMessages().isEmpty());
    }

    @Test
    public void testToString_ContainsSchemaPathAndNode() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"null\" }");
        String s = schema.toString();
        assertTrue("toString() must contain the schema path", s.contains("\"#\""));
        assertTrue("toString() must contain the schema JSON", s.contains("\"type\":\"null\""));
    }

    @Test
    public void testHasRequiredValidator_AbsentReturnsFalse() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        assertFalse("Schema without a required keyword must report hasRequiredValidator()==false",
                schema.hasRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_PresentReturnsTrue() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"integer\" }");
        assertTrue("Schema with a type keyword must report hasTypeValidator()==true",
                schema.hasTypeValidator());
        assertNotNull("getTypeValidator() must return a non‑null instance", schema.getTypeValidator());
    }

    /* --------------------------------------------------------------------- */
    /*  EDGE‑CASE / INTERNAL LOGIC TESTS                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testUriRefersToSubschema_FalseWhenUriIsNull() throws Exception {
        // The private static method is exercised via reflection.
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, null, "#");
        assertFalse("uriRefersToSubschema must be false when originalUri is null", result);
    }

    @Test
    public void testUriRefersToSubschema_TrueWhenFragmentPresent() throws Exception {
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);
        URI uri = new URI("http://example.com/schema.json#foo");
        boolean result = (boolean) m.invoke(null, uri, "#");
        assertTrue("uriRefersToSubschema should be true when URI has a fragment and schemaPath is '#'",
                result);
    }

    @Test
    public void testGetRefSchemaNode_UrnReference_HandledGracefully() throws Exception {
        // Using a URN that the meta‑schema cannot resolve – should fall back to null
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode node = schema.getRefSchemaNode("urn:nonexistent");
        assertNull("Unresolvable URN reference must return null", node);
    }

    @Test
    public void testCreateExecutionContext_DefaultImplementation() {
        JsonSchema schema = null;
        try {
            schema = createSchema("{ \"type\": \"boolean\" }");
        } catch (Exception e) {
            fail("Schema creation failed: " + e.getMessage());
        }
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull("createExecutionContext must never return null", ctx);
        assertNotNull("CollectorContext inside ExecutionContext must be non‑null", ctx.getCollectorContext());
    }

    /* --------------------------------------------------------------------- */
    /*  EXCEPTION / FAILURE SCENARIOS                                        */
    /* --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testInvalidRecursiveAnchor_ThrowsException() throws Exception {
        // $recursiveAnchor must be a boolean – using a string should trigger the exception
        createSchema("{ \"$recursiveAnchor\": \"notABoolean\" }");
        // Exception is expected from the constructor; test will pass if it is thrown
    }

    @Test
    public void testValidate_WithRefAndOlderDialect_IgnoresSiblings() throws Exception {
        /*
         * To trigger the “ignore siblings for older drafts” branch we need:
         *   – a schema that contains a $ref *and* another keyword (e.g. “type”)
         *   – a ValidationContext whose active dialect is older than V201909.
         *
         * The default ValidationContext uses the meta‑schema V4, therefore
         * activeDialect() < V201909_VALUE and the code path is exercised.
         */
        String schemaJson = "{"
                + "  \"$ref\": \"#/definitions/num\","
                + "  \"type\": \"string\","
                + "  \"definitions\": {"
                + "    \"num\": { \"type\": \"number\" }"
                + "  }"
                + "}";
        JsonSchema schema = createSchema(schemaJson);
        // The data is a number – it matches the $ref target but would violate “type:string”
        JsonNode data = MAPPER.readTree("3.14");
        Set<ValidationMessage> errors = schema.validate(data);
        // Because the dialect is older, the “type” keyword should be ignored → no errors.
        assertTrue("Older dialect should ignore sibling keywords when $ref is present", errors.isEmpty());
    }
}
```
###Test END##