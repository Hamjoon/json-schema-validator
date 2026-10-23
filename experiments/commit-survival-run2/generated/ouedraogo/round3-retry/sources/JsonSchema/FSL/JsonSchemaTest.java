package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link JsonSchema}. The tests use Mockito to mock the heavy
 * dependencies (ValidationContext, JsonMetaSchema, JsonValidator, etc.) and
 * focus on the behaviour of the public methods of {@link JsonSchema}.
 *
 * The test class follows the same style as the examples shown in the
 * description (JUnit 4, assertions from org.junit.Assert, and a single test
 * class per source class).
 */
public class JsonSchemaTest {

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private JsonMetaSchema metaSchema;
    private URIFactory uriFactory;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();

        // Mock the ValidationContext and its collaborators
        validationContext = mock(ValidationContext.class);
        metaSchema = mock(JsonMetaSchema.class);
        uriFactory = mock(URIFactory.class);
        config = mock(SchemaValidatorsConfig.class);

        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getConfig()).thenReturn(config);
        // By default, the config does not enable custom messages and does not load collectors
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // activeDialect defaults to the highest value (no special handling)
        when(validationContext.activeDialect()).thenReturn(java.util.Optional.empty());
    }

    /**
     * Helper that creates a JsonSchema instance using the static factory method.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI currentUri) {
        return JsonSchema.from(validationContext, "#", currentUri, schemaNode, null, false);
    }

    @Test
    public void testGetCurrentUri_NoId() throws Exception {
        // Schema without an "id" keyword – combineCurrentUriWithIds should return the supplied URI (null)
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);

        JsonSchema schema = createSchema(schemaNode, null);
        assertNull("Current URI should be null when schema has no id", schema.getCurrentUri());
    }

    @Test
    public void testGetCurrentUri_WithId() throws Exception {
        // Schema contains an "id" and the URIFactory creates a proper URI
        JsonNode schemaNode = mapper.readTree("{\"id\":\"http://example.com/my-schema\"}");
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("http://example.com/my-schema");
        when(uriFactory.create(null, "http://example.com/my-schema"))
                .thenReturn(new URI("http://example.com/my-schema"));

        JsonSchema schema = createSchema(schemaNode, null);
        assertEquals("http://example.com/my-schema", schema.getCurrentUri().toString());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerFragment() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"a\":{\"b\":1}}");
        JsonSchema schema = createSchema(schemaNode, null);

        JsonNode result = schema.getRefSchemaNode("#/a/b");
        assertTrue(result.isInt());
        assertEquals(1, result.intValue());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerEncoded() throws Exception {
        // Path contains an encoded slash (%2F) which should be decoded before navigation
        JsonNode schemaNode = mapper.readTree("{\"a/b\": {\"c\":2}}");
        JsonSchema schema = createSchema(schemaNode, null);

        JsonNode result = schema.getRefSchemaNode("#/a%2Fb/c");
        assertTrue(result.isInt());
        assertEquals(2, result.intValue());
    }

    @Test
    public void testGetRefSchemaNode_FragmentRef() throws Exception {
        // Simulate a fragment reference that is resolved by the meta‑schema
        JsonNode schemaNode = mapper.readTree("{\"definitions\":{\"myDef\":{\"type\":\"string\"}}}");
        when(metaSchema.getNodeByFragmentRef("#/definitions/myDef", schemaNode))
                .thenReturn(schemaNode.at("/definitions/myDef"));

        JsonSchema schema = createSchema(schemaNode, null);
        JsonNode result = schema.getRefSchemaNode("#/definitions/myDef");
        assertTrue(result.isObject());
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_UrnReference() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"some\":\"value\"}");
        when(metaSchema.getNodeByFragmentRef("urn:example:mydef", schemaNode)).thenReturn(schemaNode);

        JsonSchema schema = createSchema(schemaNode, null);
        JsonNode result = schema.getRefSchemaNode("urn:example:mydef");
        assertEquals(schemaNode, result);
    }

    @Test
    public void testFindLexicalRoot_WithIdInAncestor() throws Exception {
        // Parent schema has an id, child does not – lexical root should be the parent
        JsonNode parentNode = mapper.readTree("{\"id\":\"http://example.com/parent\"}");
        JsonNode childNode = mapper.readTree("{\"type\":\"string\"}");

        when(validationContext.resolveSchemaId(parentNode)).thenReturn("http://example.com/parent");
        when(uriFactory.create(null, "http://example.com/parent"))
                .thenReturn(new URI("http://example.com/parent"));

        JsonSchema parentSchema = createSchema(parentNode, null);
        JsonSchema childSchema = parentSchema.createChildSchema("#/child", childNode);

        JsonSchema lexicalRoot = childSchema.findLexicalRoot();
        assertSame("Lexical root should be the parent schema that carries an id", parentSchema, lexicalRoot);
    }

    @Test
    public void testFindAncestor_MultipleLevels() throws Exception {
        JsonNode rootNode = mapper.readTree("{\"type\":\"object\"}");
        JsonNode level1Node = mapper.readTree("{\"type\":\"object\"}");
        JsonNode level2Node = mapper.readTree("{\"type\":\"string\"}");

        JsonSchema rootSchema = createSchema(rootNode, null);
        JsonSchema level1Schema = rootSchema.createChildSchema("#/lvl1", level1Node);
        JsonSchema level2Schema = level1Schema.createChildSchema("#/lvl1/lvl2", level2Node);

        JsonSchema ancestor = level2Schema.findAncestor();
        assertSame("Ancestor should be the top‑most schema (root)", rootSchema, ancestor);
    }

    @Test
    public void testInitializeValidators_CallsPreloadOnce() throws Exception {
        // Build a schema with two keywords so that two JsonValidator mocks are created
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\",\"required\":[\"a\"]}");
        JsonValidator typeValidator = mock(JsonValidator.class);
        JsonValidator requiredValidator = mock(JsonValidator.class);

        // newValidator is called for each keyword; we return the appropriate mock
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(typeValidator);
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(requiredValidator);

        JsonSchema schema = createSchema(schemaNode, null);

        // First call – validators are loaded
        schema.initializeValidators();

        // Verify that preloadJsonSchema was called exactly once on each validator
        verify(typeValidator, times(1)).preloadJsonSchema();
        verify(requiredValidator, times(1)).preloadJsonSchema();

        // Second call – should *not* invoke preload again
        schema.initializeValidators();
        verify(typeValidator, times(1)).preloadJsonSchema(); // still only one invocation
        verify(requiredValidator, times(1)).preloadJsonSchema();
    }

    @Test
    public void testIsDynamicAnchor_RecursiveAnchorTrue() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"$recursiveAnchor\":true}");
        JsonSchema schema = createSchema(schemaNode, null);
        assertTrue("Dynamic anchor should be true when $recursiveAnchor is true", schema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testIsDynamicAnchor_RecursiveAnchorNonBoolean() throws Exception {
        // $recursiveAnchor with a non‑boolean value must cause an exception
        JsonNode schemaNode = mapper.readTree("{\"$recursiveAnchor\":\"yes\"}");
        createSchema(schemaNode, null);
    }

    @Test
    public void testCreateExecutionContext_UsesSupplier() {
        // Supply a custom ExecutionContext via the config
        ExecutionContext suppliedContext = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> suppliedContext);

        JsonSchema schema = createSchema(mapper.createObjectNode(), null);
        ExecutionContext ctx = schema.createExecutionContext();

        assertSame("createExecutionContext should return the supplied ExecutionContext", suppliedContext, ctx);
    }

    @Test
    public void testCreateExecutionContext_DefaultSupplier() {
        // When no supplier is configured, a default CollectorContext should be created
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);

        JsonSchema schema = createSchema(mapper.createObjectNode(), null);
        ExecutionContext ctx = schema.createExecutionContext();

        assertNotNull("ExecutionContext should not be null", ctx);
        assertNotNull("CollectorContext inside ExecutionContext should not be null", ctx.getCollectorContext());
    }

    @Test
    public void testToString_Format() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"number\"}");
        JsonSchema schema = createSchema(schemaNode, null);
        String expected = "\"#\" : " + schemaNode.toString();
        assertEquals(expected, schema.toString());
    }

    @Test
    public void testHasRequiredValidatorAndGetter() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"required\":[\"name\"]}");
        JsonValidator requiredValidator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(requiredValidator);

        JsonSchema schema = createSchema(schemaNode, null);
        // Trigger lazy loading of validators
        assertTrue(schema.hasRequiredValidator());
        assertSame(requiredValidator, schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidatorAndGetter() throws Exception {
        JsonNode schemaNode = mapper.readTree("{\"type\":\"integer\"}");
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(typeValidator);

        JsonSchema schema = createSchema(schemaNode, null);
        // Trigger lazy loading of validators
        assertTrue(schema.hasTypeValidator());
        assertSame(typeValidator, schema.getTypeValidator());
    }

    @Test
    public void testRead_BooleanSchema_True() throws Exception {
        JsonNode trueNode = mapper.readTree("true");
        JsonValidator validator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("true"), eq(trueNode), any(JsonSchema.class), anyString()))
                .thenReturn(validator);

        JsonSchema schema = createSchema(trueNode, null);
        // Force loading of validators
        schema.initializeValidators();

        // The internal map should contain a single entry for the true keyword
        Set<String> keys = schema.getValidators().keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.iterator().next().endsWith("/true"));
    }

    @Test
    public void testRead_BooleanSchema_False() throws Exception {
        JsonNode falseNode = mapper.readTree("false");
        JsonValidator validator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("false"), eq(falseNode), any(JsonSchema.class), anyString()))
                .thenReturn(validator);

        JsonSchema schema = createSchema(falseNode, null);
        schema.initializeValidators();

        Set<String> keys = schema.getValidators().keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.iterator().next().endsWith("/false"));
    }

    @Test
    public void testRead_RefOnly_OlderDialect() throws Exception {
        // Simulate an older draft where only $ref should be kept
        JsonNode schemaNode = mapper.readTree("{\"$ref\":\"#/defs/addr\",\"type\":\"string\"}");

        JsonValidator refValidator = mock(JsonValidator.class);
        JsonValidator typeValidator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("$ref"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(refValidator);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(typeValidator);
        // Force the dialect to be older than 2019‑09
        when(validationContext.activeDialect()).thenReturn(java.util.Optional.of(VersionFlag.V201909.minusOne()));

        JsonSchema schema = createSchema(schemaNode, null);
        schema.initializeValidators();

        // Only the $ref validator must remain
        Set<String> keys = schema.getValidators().keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.iterator().next().endsWith("/$ref"));
    }
}
