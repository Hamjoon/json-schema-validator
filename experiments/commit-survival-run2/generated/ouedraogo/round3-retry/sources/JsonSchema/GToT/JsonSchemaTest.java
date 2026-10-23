/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public factory method {@code from(...)} and the public constructors via the factory.</li>
 *   <li>Schema creation helpers ({@code createChildSchema}, {@code createExecutionContext}).</li>
 *   <li>URI handling ({@code getCurrentUri}, {@code isDynamicAnchor}).</li>
 *   <li>Reference resolution ({@code getRefSchemaNode}).</li>
 *   <li>Validator presence helpers ({@code hasRequiredValidator}, {@code hasTypeValidator}).</li>
 *   <li>Basic validation paths ({@code validate(JsonNode)}, {@code validateAndCollect(JsonNode)}).</li>
 *   <li>Edge‑case handling such as boolean schemas, missing $ref targets and recursive anchors.</li>
 *   <li>Lazy‑loading of validators via {@code initializeValidators()}.</li>
 * </ul>
 *
 * The test suite uses the real {@link com.networknt.schema.JsonSchemaFactory}
 * together with Jackson for JSON parsing and Mockito for spying on internal
 * {@link com.networknt.schema.JsonValidator} instances where necessary.
 *
 * All tests are written for JUnit‑4.
 */

package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test class for {@link JsonSchema}.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaFactory factoryV7;

    @Before
    public void setUp() {
        // Use Draft‑07 which is stable and has all basic keywords.
        factoryV7 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     * Helper to create a JsonSchema from a JSON string.
     */
    private JsonSchema schemaFromString(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return factoryV7.getSchema(node);
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Factory method & basic getters                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFactoryFromCreatesSchemaWithCorrectUri() throws Exception {
        // Schema with an explicit $id – the currentUri should be that id.
        String schemaJson = "{ \"$id\": \"http://example.com/person\", \"type\": \"object\" }";
        JsonNode node = MAPPER.readTree(schemaJson);
        ValidationContext vc = factoryV7.getValidationContext();
        JsonSchema schema = JsonSchema.from(vc, "#", new URI("http://example.com/base"), node, null, false);

        assertNotNull("Current URI must be resolved from $id", schema.getCurrentUri());
        assertEquals("http://example.com/person", schema.getCurrentUri().toString());

        // toString should contain the schema path and the original schema node.
        assertTrue(schema.toString().contains("\"#/\""));
        assertTrue(schema.toString().contains("\"$id\""));
    }

    @Test
    public void testGetValidationContextReturnsSameInstance() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        ValidationContext ctx = schema.getValidationContext();
        assertSame("The returned ValidationContext must be the one used during construction",
                ctx, schema.getValidationContext());
    }

    @Test
    public void testCreateChildSchemaCreatesIndependentInstance() throws Exception {
        JsonSchema parent = schemaFromString("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode childNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema child = parent.createChildSchema("#/properties/name", childNode);

        assertNotNull(child);
        assertEquals("#/properties/name", child.getSchemaPath());
        assertSame(parent.getValidationContext(), child.getValidationContext());
    }

    /* ---------------------------------------------------------------------- */
    /* 2. $ref handling (getRefSchemaNode)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetRefSchemaNodeResolvesLocalReference() throws Exception {
        String schemaJson = "{\n" +
                "  \"$defs\": {\n" +
                "    \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 0 }\n" +
                "  },\n" +
                "  \"$ref\": \"#/$defs/positiveInteger\"\n" +
                "}";
        JsonSchema schema = schemaFromString(schemaJson);
        JsonNode refNode = schema.getRefSchemaNode("#/$defs/positiveInteger");

        assertNotNull("Reference node must be resolved", refNode);
        assertEquals("integer", refNode.get("type").asText());
        assertEquals(0, refNode.get("minimum").asInt());
    }

    @Test
    public void testGetRefSchemaNodeReturnsNullWhenReferenceMissing() throws Exception {
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchema schema = schemaFromString(schemaJson);
        JsonNode missing = schema.getRefSchemaNode("#/nonexistent");
        assertNull("Missing reference should return null", missing);
    }

    @Test
    public void testGetRefSchemaNodeHandlesBooleanSchemaTrue() throws Exception {
        // A schema that is simply "true" – any data is valid.
        JsonSchema schema = schemaFromString("true");
        JsonNode refNode = schema.getRefSchemaNode("#"); // reference to the root itself
        assertTrue("Boolean true schema should be returned as a JsonNode", refNode.isBoolean());
        assertTrue(refNode.booleanValue());
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Ancestor / lexical root helpers                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindAncestorReturnsRootWhenNoParent() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        assertSame("Ancestor of a root schema is itself", schema, schema.findAncestor());
    }

    @Test
    public void testFindLexicalRootStopsAtSchemaWithId() throws Exception {
        String schemaJson = "{\n" +
                "  \"$id\": \"http://example.com/root\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"child\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": { \"inner\": { \"type\": \"string\" } }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonSchema root = schemaFromString(schemaJson);
        JsonSchema child = root.createChildSchema("#/properties/child", root.getSchemaNode().get("properties").get("child"));
        JsonSchema inner = child.createChildSchema("#/properties/inner", child.getSchemaNode().get("properties").get("inner"));

        // inner has no $id, so lexical root should be the first ancestor with an $id (root)
        assertSame(root, inner.findLexicalRoot());
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Validation basics                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testValidateValidStringReturnsNoErrors() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        JsonNode data = MAPPER.readTree("\"hello world\"");
        Set<ValidationMessage> errors = schema.validate(data);
        assertTrue("Valid string should produce no errors", errors.isEmpty());
    }

    @Test
    public void testValidateInvalidTypeReturnsError() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        JsonNode data = MAPPER.readTree("123");
        Set<ValidationMessage> errors = schema.validate(data);
        assertFalse("Invalid type must produce at least one error", errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        assertEquals("type", first.getType()); // the validator type for type mismatch is "type"
    }

    @Test
    public void testValidateAndCollectCollectsErrorsAndContext() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"object\",\"required\":[\"name\"]}");
        JsonNode data = MAPPER.readTree("{\"age\":30}");
        ValidationResult result = schema.validateAndCollect(data);

        assertFalse("Missing required property should cause validation failure", result.isValid());
        Set<ValidationMessage> errors = result.getValidationMessages();
        assertEquals(1, errors.size());
        ValidationMessage vm = errors.iterator().next();
        assertEquals("required", vm.getType());

        // Collector context should have been created (no NPE)
        assertNotNull(result.getExecutionContext().getCollectorContext());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Walk methods – simple pass‑through (no custom listeners)             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testWalkExecutesWithoutException() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        JsonNode data = MAPPER.readTree("[1,2,3]");
        ValidationResult result = schema.walk(data, true);

        assertTrue("Array of integers should be valid", result.isValid());
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalkAtNodeWithInvalidDataCollectsError() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        JsonNode data = MAPPER.readTree("[1,\"two\",3]");
        ValidationResult result = schema.walkAtNode(factoryV7.createExecutionContext(), data, data, "$", true);

        assertFalse(result.isValid());
        assertEquals(1, result.getValidationMessages().size());
        ValidationMessage vm = result.getValidationMessages().iterator().next();
        assertEquals("type", vm.getType());
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Required / Type validator presence helpers                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasRequiredValidatorTrueWhenRequiredKeywordPresent() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"object\",\"required\":[\"id\"]}");
        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidatorFalseWhenAbsent() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"object\"}");
        assertFalse(schema.hasRequiredValidator());
        assertNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidatorTrueWhenTypeKeywordPresent() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\"}");
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidatorFalseWhenAbsent() throws Exception {
        JsonSchema schema = schemaFromString("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        assertFalse(schema.hasTypeValidator());
        assertNull(schema.getTypeValidator());
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Dynamic anchor handling ($recursiveAnchor)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsDynamicAnchorTrueWhenRecursiveAnchorTrue() throws Exception {
        JsonSchema schema = schemaFromString("{\"$recursiveAnchor\": true}");
        assertTrue("Dynamic anchor flag must be true when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchorFalseWhenRecursiveAnchorFalse() throws Exception {
        JsonSchema schema = schemaFromString("{\"$recursiveAnchor\": false}");
        assertFalse("Dynamic anchor flag must be false when $recursiveAnchor is false", schema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRecursiveAnchorNonBooleanThrowsException() throws Exception {
        // The validator should reject a non‑boolean value for $recursiveAnchor.
        schemaFromString("{\"$recursiveAnchor\": \"yes\"}");
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Lazy loading of validators (initializeValidators)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInitializeValidatorsPreloadsAllValidators() throws Exception {
        // Build a schema with multiple validators.
        JsonSchema schema = schemaFromString("{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\"type\":\"integer\"},\n" +
                "    \"name\": {\"type\":\"string\"}\n" +
                "  },\n" +
                "  \"required\": [\"id\"]\n" +
                "}");

        // Spy on the underlying JsonValidator map.
        // The internal map is created lazily, so we trigger it first.
        schema.validate(MAPPER.readTree("{\"id\":1,\"name\":\"Bob\"}"));
        // Get the internal map via reflection (since getValidators() is private).
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, JsonValidator> validators = (java.util.Map<String, JsonValidator>) f.get(schema);

        // Replace each validator with a Mockito spy that records preloadJsonSchema().
        for (java.util.Map.Entry<String, JsonValidator> e : validators.entrySet()) {
            JsonValidator original = e.getValue();
            JsonValidator spy = Mockito.spy(original);
            e.setValue(spy);
        }

        // Reset the loaded flag so we can call initializeValidators again.
        java.lang.reflect.Field loaded = JsonSchema.class.getDeclaredField("validatorsLoaded");
        loaded.setAccessible(true);
        loaded.set(schema, false);

        // Call the method under test.
        schema.initializeValidators();

        // Verify preloadJsonSchema() was called exactly once per validator.
        for (JsonValidator v : validators.values()) {
            Mockito.verify(v, Mockito.times(1)).preloadJsonSchema();
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 9. ExecutionContext creation                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateExecutionContextUsesSupplierIfConfigured() throws Exception {
        // Custom supplier that returns a distinct ExecutionContext.
        ExecutionContext supplied = new ExecutionContext(new CollectorContext(false, false));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setExecutionContextSupplier(() -> supplied);

        ValidationContext vc = new ValidationContext(factoryV7.getMetaSchema(), cfg);
        JsonSchema schema = JsonSchema.from(vc, "#", null, MAPPER.readTree("{}"), null, false);

        ExecutionContext ctx = schema.createExecutionContext();
        assertSame("ExecutionContext should be the one supplied by the config supplier", supplied, ctx);
    }

    @Test
    public void testCreateExecutionContextCreatesDefaultWhenNoSupplier() throws Exception {
        JsonSchema schema = schemaFromString("{}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. toString sanity check                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringContainsPathAndNode() throws Exception {
        JsonSchema schema = schemaFromString("{\"type\":\"string\",\"maxLength\":5}");
        String s = schema.toString();
        assertTrue(s.startsWith("\"#/\""));
        assertTrue(s.contains("\"type\":\"string\""));
        assertTrue(s.contains("\"maxLength\":5"));
    }
}
