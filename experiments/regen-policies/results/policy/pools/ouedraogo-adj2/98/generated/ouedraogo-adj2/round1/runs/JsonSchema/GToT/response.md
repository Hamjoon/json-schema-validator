###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written by three “experts” that first listed the public API,
 * then added a basic test for each method, identified edge‑cases / exception
 * scenarios and finally merged everything into a single test class.
 *
 * The test class uses only the public constructors and factory methods that are
 * part of the supplied source code and its declared dependencies – no mocks or
 * subclassing of library types are used.
 *
 * NOTE: The library under test (networknt‑json‑schema) is fairly large.  The
 * tests below focus on the behaviour that can be exercised without external
 * resources (e.g. remote $ref) and therefore keep the schemas tiny and
 * self‑contained.
 */
package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Comprehensive JUnit‑4 test class for {@link JsonSchema}.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A very small schema used by most tests – an empty object schema. */
    private static final String SIMPLE_SCHEMA = "{}";

    /** A schema that contains a $ref (used for getRefSchemaNode / getRefSchema tests). */
    private static final String REF_SCHEMA = "{\n"
            + "  \"$id\": \"http://example.com/root.json\",\n"
            + "  \"definitions\": {\n"
            + "    \"person\": { \"type\": \"object\", \"properties\": { \"name\": {\"type\":\"string\"} } }\n"
            + "  },\n"
            + "  \"$ref\": \"#\\/definitions\\/person\"\n"
            + "}";

    /** A schema that contains a $recursiveAnchor (used for isRecursiveAnchor test). */
    private static final String RECURSIVE_ANCHOR_SCHEMA = "{ \"$recursiveAnchor\": true }";

    private JsonSchemaFactory factory;
    private JsonSchema simpleSchema;
    private JsonSchema refSchema;
    private JsonSchema recursiveAnchorSchema;

    @Before
    public void setUp() {
        // Use Draft‑4 – any version works for the simple tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        simpleSchema = factory.getSchema(SIMPLE_SCHEMA);
        refSchema = factory.getSchema(REF_SCHEMA);
        recursiveAnchorSchema = factory.getSchema(RECURSIVE_ANCHOR_SCHEMA);
    }

    /* --------------------------------------------------------------------- */
    /* 1.  PUBLIC METHODS – basic sanity checks                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFromRefCreatesNewSchema() {
        JsonNodePath newPath = new JsonNodePath(PathType.JSON_POINTER).append("new");
        JsonSchema derived = simpleSchema.fromRef(simpleSchema, newPath);
        assertNotNull(derived);
        // The derived schema must have a different evaluation path.
        assertEquals("new", derived.getEvaluationPath().getName(-1));
        // The original schema must stay unchanged.
        assertEquals("", simpleSchema.getEvaluationPath().toString());
    }

    @Test
    public void testWithConfigReturnsSameWhenUnchanged() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema result = simpleSchema.withConfig(cfg);
        // No change – should return the same instance.
        assertSame(simpleSchema, result);
    }

    @Test
    public void testWithConfigCreatesNewWhenChanged() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchema result = simpleSchema.withConfig(cfg);
        assertNotSame(simpleSchema, result);
        assertTrue(result.getValidationContext().getConfig().isFailFast());
    }

    @Test
    public void testGetValidationContext() {
        ValidationContext ctx = simpleSchema.getValidationContext();
        assertNotNull(ctx);
        assertSame(factory.getMetaSchema(), ctx.getMetaSchema());
    }

    @Test
    public void testGetRefSchemaNodeExistingPointer() {
        // "#/definitions/person" exists in refSchema.
        JsonNode node = refSchema.getRefSchemaNode("#/definitions/person");
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals("object", node.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeMissingPointerReturnsMissingNode() {
        JsonNode node = refSchema.getRefSchemaNode("#/nonexistent");
        // The implementation returns a MissingNode (Jackson) when the pointer cannot be resolved.
        assertNotNull(node);
        assertTrue(node.isMissingNode());
    }

    @Test
    public void testGetRefSchemaWithJsonPointer() {
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER).append("definitions")
                .append("person");
        JsonSchema sub = refSchema.getRefSchema(fragment);
        assertNotNull(sub);
        assertEquals("object", sub.getSchemaNode().get("type").asText());
    }

    @Test
    public void testGetRefSchemaWithUnknownAnchorThrows() {
        JsonNodePath unknownAnchor = new JsonNodePath(PathType.ANCHOR).append("unknown");
        try {
            refSchema.getRefSchema(unknownAnchor);
            fail("Expected JsonSchemaException because anchor does not exist");
        } catch (JsonSchemaException e) {
            assertTrue(e.getMessage().contains("Unable to find anchor"));
        }
    }

    @Test
    public void testGetSubSchemaValidPointer() {
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER).append("definitions")
                .append("person");
        JsonSchema sub = refSchema.getSubSchema(fragment);
        assertNotNull(sub);
        assertEquals("object", sub.getSchemaNode().get("type").asText());
    }

    @Test
    public void testGetSubSchemaInvalidPointerThrowsInvalidSchemaRefException() {
        JsonNodePath badFragment = new JsonNodePath(PathType.JSON_POINTER).append("bad");
        try {
            refSchema.getSubSchema(badFragment);
            fail("Expected InvalidSchemaRefException for non‑existent fragment");
        } catch (InvalidSchemaRefException e) {
            // Expected
        }
    }

    @Test
    public void testFindLexicalRoot() {
        JsonSchema root = simpleSchema.findLexicalRoot();
        // For a schema without $id the lexical root is itself.
        assertSame(simpleSchema, root);
    }

    @Test
    public void testFindSchemaResourceRoot() {
        JsonSchema root = simpleSchema.findSchemaResourceRoot();
        // With no $id the document root is the resource root.
        assertSame(simpleSchema, root);
    }

    @Test
    public void testIsSchemaResourceRoot() {
        assertTrue(simpleSchema.isSchemaResourceRoot());
        // A schema that has an $id is also a resource root.
        assertTrue(refSchema.isSchemaResourceRoot());
    }

    @Test
    public void testGetId() {
        // The refSchema defines $id – it should be stored.
        assertEquals("http://example.com/root.json", refSchema.getId());
        // The simple schema has no $id – getId returns null.
        assertNull(simpleSchema.getId());
    }

    @Test
    public void testFindAncestor() {
        // Simple schema has no parent – findAncestor returns itself.
        assertSame(simpleSchema, simpleSchema.findAncestor());
    }

    @Test
    public void testInitializeValidatorsIdempotent() {
        // First call – validators are loaded.
        simpleSchema.initializeValidators();
        // Second call – should not throw and should keep the flag true.
        simpleSchema.initializeValidators();
        // No public getter for the flag – the test succeeds if no exception is thrown.
    }

    @Test
    public void testIsRecursiveAnchor() {
        assertFalse(simpleSchema.isRecursiveAnchor());
        assertTrue(recursiveAnchorSchema.isRecursiveAnchor());
    }

    @Test
    public void testCreateExecutionContextRespectsConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setLocale(Locale.GERMAN);
        cfg.setFailFast(true);
        JsonSchema custom = simpleSchema.withConfig(cfg);
        ExecutionContext ctx = custom.createExecutionContext();
        assertEquals(Locale.GERMAN, ctx.getExecutionConfig().getLocale());
        assertTrue(ctx.isFailFast());
    }

    @Test
    public void testHasTypeValidatorAndGetTypeValidator() {
        // simpleSchema has no "type" keyword – no TypeValidator.
        assertFalse(simpleSchema.hasTypeValidator());
        assertNull(simpleSchema.getTypeValidator());

        // Build a schema that contains a "type" keyword.
        JsonSchema typeSchema = factory.getSchema("{\"type\":\"string\"}");
        assertTrue(typeSchema.hasTypeValidator());
        assertNotNull(typeSchema.getTypeValidator());
        assertEquals(JsonType.STRING, typeSchema.getTypeValidator().getSchemaType());
    }

    @Test
    public void testGetValidatorsIsLazyAndSorted() {
        // The validator list is created lazily.
        JsonSchema schema = factory.getSchema("{\"properties\":{\"a\":{}},\"required\":[\"a\"]}");
        assertNotNull(schema.getValidators());
        // The sort order must place "properties" before "required".
        assertEquals("properties", schema.getValidators().get(0).getEvaluationPath().getName(-1));
        assertEquals("required", schema.getValidators().get(1).getEvaluationPath().getName(-1));
    }

    @Test
    public void testValidateSimpleSchema() {
        // An empty object validates against "{}".
        JsonNode data = MAPPER.createObjectNode();
        Set<ValidationMessage> msgs = simpleSchema.validate(data);
        assertTrue(msgs.isEmpty());
    }

    @Test
    public void testValidateWithStringInput() {
        Set<ValidationMessage> msgs = simpleSchema.validate("{}", InputFormat.JSON);
        assertTrue(msgs.isEmpty());
    }

    @Test
    public void testValidateStringInputInvalidJsonThrows() {
        try {
            simpleSchema.validate("{invalid json", InputFormat.JSON);
            fail("Expected IllegalArgumentException because input is not valid JSON");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid input"));
        }
    }

    @Test
    public void testValidateWithCustomExecutionContext() {
        ExecutionContextCustomizer customizer = (ctx, vctx) -> ctx.setFailFast(true);
        Set<ValidationMessage> msgs = simpleSchema.validate(MAPPER.createObjectNode(),
                customizer);
        // No validation errors, but the customizer should have been applied without throwing.
        assertTrue(msgs.isEmpty());
    }

    @Test
    public void testWalkReturnsResultObject() {
        ValidationResult result = simpleSchema.walk(MAPPER.createObjectNode(), true);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testWalkWithStringInputAndCustomizer() {
        ExecutionContextCustomizer customizer = (ctx, vctx) -> ctx.setFailFast(true);
        ValidationResult result = simpleSchema.walk("{\"a\":1}", InputFormat.JSON, true, customizer);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 2.  EDGE‑CASE / EXCEPTION SCENARIOS                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFromRefPreservesIdWhenParentHasId() {
        // refSchema has an $id – the derived schema should keep the same id.
        JsonSchema derived = refSchema.fromRef(refSchema, new JsonNodePath(PathType.JSON_POINTER));
        assertEquals(refSchema.getId(), derived.getId());
    }

    @Test
    public void testWithConfigWhenDiscriminatorKeywordDisabledAndSchemaHasDiscriminator() {
        // Build a schema that contains a discriminator keyword.
        String discSchemaStr = "{ \"discriminator\": { \"propertyName\": \"type\" } }";
        JsonSchema discSchema = factory.getSchema(discSchemaStr);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setDiscriminatorKeywordEnabled(false); // user disables it
        JsonSchema result = discSchema.withConfig(cfg);
        // The method forces discriminatorKeywordEnabled true internally.
        assertTrue(result.getValidationContext().getConfig().isDiscriminatorKeywordEnabled());
    }

    @Test
    public void testGetRefSchemaNodeHandlesUrlEncodedPointer() throws Exception {
        // Create a schema with a property that contains a slash in its name.
        String encodedSchema = "{ \"properties\": { \"a/b\": { \"type\": \"string\" } } }";
        JsonSchema s = factory.getSchema(encodedSchema);
        // Use an encoded pointer "%2F" for the slash.
        JsonNode node = s.getRefSchemaNode("#/properties/a%2Fb");
        assertNotNull(node);
        assertEquals("string", node.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeWhenIdPrefixMatches() {
        // Schema with $id and a reference that starts with the id.
        String schemaWithId = "{ \"$id\": \"http://example.com/base\", \"definitions\": { \"x\": {} }, \"$ref\": \"http://example.com/base#/definitions/x\" }";
        JsonSchema s = factory.getSchema(schemaWithId);
        JsonNode node = s.getRefSchemaNode("http://example.com/base#/definitions/x");
        assertNotNull(node);
        assertTrue(node.isObject());
    }

    @Test
    public void testValidateThrowsFailFastWhenConfigured() {
        // Schema that always fails (type mismatch).
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchema fastSchema = schema.withConfig(cfg);
        ExecutionContext ctx = fastSchema.createExecutionContext();
        // The first validation error should cause a FailFastAssertionException wrapped in validation.
        try {
            fastSchema.validate(ctx, MAPPER.createObjectNode(), OutputFormat.DEFAULT, (ec, vc) -> {});
            fail("Expected FailFastAssertionException");
        } catch (FailFastAssertionException e) {
            // Expected – the message list contains at least one error.
            assertFalse(e.getValidationMessages().isEmpty());
        }
    }

    /* --------------------------------------------------------------------- */
    /* 3.  TO_STRING AND TO_STRING‑RELATED BEHAVIOUR                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringFormat() {
        String s = simpleSchema.toString();
        // Expected format: "<evaluationPath> : <schemaNode>"
        assertTrue(s.startsWith("\"\""));
        assertTrue(s.contains("{}"));
    }
}
```
###Test END##