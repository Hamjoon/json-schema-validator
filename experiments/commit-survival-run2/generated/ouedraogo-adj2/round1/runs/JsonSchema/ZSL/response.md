###Test START##
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

public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple stub for the library's {@code URIFactory}. */
    public static class SimpleURIFactory implements URIFactory {
        @Override
        public URI create(URI base, String id) {
            if (base == null) {
                return URI.create(id);
            }
            // Very naive resolution – sufficient for the unit‑tests.
            return base.resolve(id);
        }
    }

    /** Minimal {@code ValidationContext} implementation that delegates to a real {@code JsonMetaSchema}. */
    public static class TestValidationContext extends ValidationContext {

        private final JsonMetaSchema metaSchema;
        private final SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        public TestValidationContext(JsonMetaSchema metaSchema) {
            // The parent constructor stores the factories – we give simple stubs / nulls where not required.
            super(new SimpleURIFactory(), null, metaSchema, null, new SchemaValidatorsConfig());
            this.metaSchema = metaSchema;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return metaSchema;
        }

        @Override
        public String resolveSchemaId(JsonNode schemaNode) {
            JsonNode idNode = schemaNode.get(metaSchema.getIdKeyword());
            return (idNode != null) ? idNode.asText() : null;
        }

        @Override
        public JsonValidator newValidator(String schemaPath, String keyword, JsonNode schemaNode,
                                          JsonSchema parentSchema, String customMessage) {
            // Delegate to the meta‑schema – this creates the real validator objects used by the library.
            return metaSchema.newValidator(this, schemaPath, keyword, schemaNode, parentSchema, customMessage);
        }

        @Override
        public URIFactory getURIFactory() {
            // Return the simple factory defined above.
            return new SimpleURIFactory();
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        // The remaining methods are not required for the tests and therefore inherit the default behaviour.
    }

    private ValidationContext vctxV7;
    private ValidationContext vctxV201909;

    @Before
    public void setUp() {
        vctxV7 = new TestValidationContext(JsonMetaSchema.getV7());
        vctxV201909 = new TestValidationContext(JsonMetaSchema.getV201909());
    }

    private JsonSchema createSchema(ValidationContext ctx, String json, URI baseUri) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return JsonSchema.from(ctx, "#", baseUri, node, null, false);
    }

    @Test
    public void testCurrentUriCombination_WithId() throws Exception {
        String schema = "{ \"$id\": \"http://example.com/base.json\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        URI expected = URI.create("http://example.com/base.json");
        assertEquals(expected, js.getCurrentUri());
    }

    @Test
    public void testFindAncestor() throws Exception {
        JsonSchema parent = createSchema(vctxV7, "{}", null);
        JsonSchema child = parent.createChildSchema("#/child", MAPPER.readTree("{}"));
        assertSame(parent, child.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_WithIdPropagation() throws Exception {
        // Grand‑parent has no $id, parent has $id, child has no $id.
        JsonSchema grandParent = createSchema(vctxV7, "{}", null);
        JsonSchema parent = grandParent.createChildSchema("#/parent",
                MAPPER.readTree("{ \"$id\": \"urn:example:parent\" }"));
        JsonSchema child = parent.createChildSchema("#/child", MAPPER.readTree("{}"));
        // The lexical root for the child should be the parent (the nearest ancestor that has an id).
        assertSame(parent, child.findLexicalRoot());
    }

    @Test
    public void testHasRequiredAndTypeValidators() throws Exception {
        String schema = "{ \"required\": [\"name\"], \"type\": \"object\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        assertTrue(js.hasRequiredValidator());
        assertNotNull(js.getRequiredValidator());

        assertTrue(js.hasTypeValidator());
        assertNotNull(js.getTypeValidator());
    }

    @Test
    public void testInitializeValidators_IsIdempotent() throws Exception {
        String schema = "{ \"type\": \"string\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        // First call – should preload without error.
        js.initializeValidators();
        // Second call – should be a no‑op and must not throw.
        js.initializeValidators();
    }

    @Test
    public void testDynamicAnchorDetection() throws Exception {
        String schema = "{ \"$recursiveAnchor\": true }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        assertTrue(js.isDynamicAnchor());
    }

    @Test
    public void testCreateChildSchema_ParentLink() throws Exception {
        JsonSchema parent = createSchema(vctxV7, "{}", null);
        JsonSchema child = parent.createChildSchema("#/child", MAPPER.readTree("{\"type\":\"string\"}"));
        // The child schema's parent must be the one that created it.
        assertSame(parent, child.getParentSchema());
    }

    @Test
    public void testValidateAndCollect_ValidString() throws Exception {
        String schema = "{ \"type\": \"string\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        JsonNode instance = MAPPER.readTree("\"hello\"");
        ValidationResult result = js.validateAndCollect(instance);
        assertTrue("No validation errors expected", result.getValidationMessages().isEmpty());
    }

    @Test
    public void testValidateAndCollect_InvalidString() throws Exception {
        String schema = "{ \"type\": \"string\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        JsonNode instance = MAPPER.readTree("123");
        ValidationResult result = js.validateAndCollect(instance);
        Set<ValidationMessage> msgs = result.getValidationMessages();
        assertFalse("Validation errors expected", msgs.isEmpty());
        // At least one message must refer to the 'type' keyword.
        boolean containsTypeError = msgs.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue("Expected a type validation error", containsTypeError);
    }

    @Test
    public void testToStringRepresentation() throws Exception {
        String schema = "{ \"type\": \"boolean\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        String str = js.toString();
        assertTrue(str.contains("\"#/type\""));
        assertTrue(str.contains("\"boolean\""));
    }

    @Test
    public void testWalk_WithNoKeywordWalkListeners() throws Exception {
        // Walk without any listeners – the method should still return a ValidationResult (empty for a valid instance).
        String schema = "{ \"type\": \"integer\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        JsonNode instance = MAPPER.readTree("42");
        ExecutionContext execCtx = js.createExecutionContext();
        ValidationResult result = js.walk(execCtx, instance, true);
        assertTrue("Walk on valid data must produce no errors", result.getValidationMessages().isEmpty());
    }

    @Test
    public void testActiveDialect_UsesV201909() throws Exception {
        // The V201909 dialect disables the “ignore siblings for older drafts” rule.
        String schema = "{ \"$ref\": \"#/definitions/foo\", \"definitions\": { \"foo\": true } }";
        JsonSchema js = createSchema(vctxV201909, schema, null);
        // The schema contains a $ref and another keyword; with V201909 the extra keyword must be retained.
        // Hence the validator map should contain more than one entry.
        // Trigger lazy loading of validators via a simple validate call.
        JsonNode instance = MAPPER.readTree("null");
        Set<ValidationMessage> errors = js.validate(instance);
        // The presence of the extra “definitions” keyword does not cause a validation error for a null instance,
        // but we can assert that the validator map size is > 1 via reflection (allowed for test purposes).
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, JsonValidator> map = (java.util.Map<String, JsonValidator>) f.get(js);
        assertTrue("With V201909 the validator map should contain more than just $ref", map.size() > 1);
    }

    @Test
    public void testGetRefSchemaNode_FragmentResolution() throws Exception {
        // Create a schema with a nested fragment and ensure getRefSchemaNode resolves it.
        String schema = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        JsonNode node = js.getRefSchemaNode("#/properties/name");
        assertNotNull("Fragment should resolve to a node", node);
        assertEquals("string", node.get("type").asText());
    }

    @Test
    public void testHandleNullNode_DelegatesToSubSchema() throws Exception {
        // Build a schema where a $ref points to a non‑existent node, forcing handleNullNode logic.
        String schema = "{ \"$ref\": \"#/nonexistent\" }";
        JsonSchema js = createSchema(vctxV7, schema, null);
        // The $ref cannot be resolved – the method should return null without throwing.
        JsonNode result = js.getRefSchemaNode("#/nonexistent");
        assertNull("Unresolvable $ref should return null", result);
    }
}
###Test END##