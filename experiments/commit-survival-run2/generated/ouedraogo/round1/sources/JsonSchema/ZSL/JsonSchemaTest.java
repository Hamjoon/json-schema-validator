package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema}. The tests are written against the
 * package‑private API (using reflection where needed) and rely heavily on
 * Mockito to stub out the heavy infrastructure classes such as
 * {@link ValidationContext}, {@link JsonValidator}, {@link ExecutionContext}
 * and {@link CollectorContext}.
 *
 * The goal is to achieve high coverage of the public and private behaviour
 * without needing the full library runtime.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Captor
    private ArgumentCaptor<Set<ValidationMessage>> validationMessageCaptor;

    private JsonNodeFactory factory = JsonNodeFactory.instance;
    private ObjectNode emptySchemaNode;

    @Before
    public void setUp() throws Exception {
        // basic schema node used for many tests
        emptySchemaNode = factory.objectNode();

        // default behaviour for the validation context
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(validationContext.activeDialect()).thenReturn(Optional.empty());

        // ExecutionContext always returns the mocked CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope(any())).thenReturn(mock(CollectorContext.Scope.class));
        when(collectorContext.exitDynamicScope()).thenReturn(mock(CollectorContext.Scope.class));
    }

    /**
     * Helper to create a {@link JsonSchema} instance using the package‑private
     * {@code from} factory method.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI currentUri) {
        return JsonSchema.from(validationContext, "#", currentUri, schemaNode, null, false);
    }

    /* --------------------------------------------------------------------- */
    /*                     TESTS FOR PRIVATE/STATIC HELPERS                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsUriFragmentWithNoContext_trueWhenFragmentAndNoBase() {
        assertTrue(JsonSchema.isUriFragmentWithNoContext(null, "#/defs"));
    }

    @Test
    public void testIsUriFragmentWithNoContext_falseWhenBasePresent() {
        assertFalse(JsonSchema.isUriFragmentWithNoContext(URI.create("http://example.com"), "#/defs"));
    }

    @Test
    public void testUriRefersToSubschema_trueWhenFragmentAndRootPath() {
        URI uri = URI.create("http://example.com/schema.json#sub");
        assertTrue(JsonSchema.uriRefersToSubschema(uri, "#"));
    }

    @Test
    public void testUriRefersToSubschema_falseWhenNoFragment() {
        URI uri = URI.create("http://example.com/schema.json");
        assertFalse(JsonSchema.uriRefersToSubschema(uri, "#"));
    }

    @Test
    public void testCombineCurrentUriWithIds_noIdReturnsOriginal() throws Exception {
        when(validationContext.resolveSchemaId(emptySchemaNode)).thenReturn(null);
        JsonSchema schema = createSchema(emptySchemaNode, URI.create("http://example.com/base"));
        Method combine = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        combine.setAccessible(true);
        URI result = (URI) combine.invoke(schema, URI.create("http://example.com/base"), emptySchemaNode);
        assertEquals(URI.create("http://example.com/base"), result);
    }

    @Test
    public void testCombineCurrentUriWithIds_withValidIdResolves() throws Exception {
        when(validationContext.resolveSchemaId(emptySchemaNode)).thenReturn("sub/schema.json");
        when(validationContext.getURIFactory()).thenReturn(URI::resolve);
        JsonSchema schema = createSchema(emptySchemaNode, URI.create("http://example.com/base/"));
        Method combine = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        combine.setAccessible(true);
        URI result = (URI) combine.invoke(schema, URI.create("http://example.com/base/"), emptySchemaNode);
        assertEquals(URI.create("http://example.com/base/sub/schema.json"), result);
    }

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIds_invalidIdThrowsException() throws Exception {
        when(validationContext.resolveSchemaId(emptySchemaNode)).thenReturn("://bad:uri");
        when(validationContext.getURIFactory()).thenReturn(URI::resolve);
        JsonSchema schema = createSchema(emptySchemaNode, URI.create("http://example.com"));
        Method combine = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        combine.setAccessible(true);
        // This should trigger the exception path
        combine.invoke(schema, URI.create("http://example.com"), emptySchemaNode);
    }

    /* --------------------------------------------------------------------- */
    /*                     TESTS FOR PUBLIC BEHAVIOUR                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCurrentUri_returnsConstructorValue() {
        URI uri = URI.create("http://example.com/schema");
        JsonSchema schema = createSchema(emptySchemaNode, uri);
        assertEquals(uri, schema.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNode_jsonPointerResolution() {
        // Build a schema node with a nested property
        ObjectNode root = factory.objectNode();
        ObjectNode child = factory.objectNode();
        child.put("type", "string");
        root.set("properties", child);

        // metaSchema should not be consulted for this path
        when(metaSchema.getNodeByFragmentRef(anyString(), any())).thenReturn(null);
        JsonSchema schema = createSchema(root, null);

        JsonNode result = schema.getRefSchemaNode("#/properties");
        // The node should be the child object we created
        assertTrue(result.isObject());
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testRead_booleanTrueCreatesValidator() throws Exception {
        JsonNode trueNode = factory.booleanNode(true);
        JsonSchema schema = createSchema(trueNode, null);

        // Mock the validator that the ValidationContext will create
        JsonValidator validator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("true"), eq(trueNode), eq(schema), any()))
                .thenReturn(validator);

        Method read = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        read.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> validators = (Map<String, JsonValidator>) read.invoke(schema, trueNode);

        assertEquals(1, validators.size());
        assertTrue(validators.keySet().iterator().next().endsWith("/true"));
    }

    @Test
    public void testRead_booleanFalseCreatesValidator() throws Exception {
        JsonNode falseNode = factory.booleanNode(false);
        JsonSchema schema = createSchema(falseNode, null);

        JsonValidator validator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("false"), eq(falseNode), eq(schema), any()))
                .thenReturn(validator);

        Method read = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        read.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> validators = (Map<String, JsonValidator>) read.invoke(schema, falseNode);

        assertEquals(1, validators.size());
        assertTrue(validators.keySet().iterator().next().endsWith("/false"));
    }

    @Test
    public void testRead_withRefAndOlderDialect_keepsOnlyRefValidator() throws Exception {
        // Simulate an older draft (value < V201909_VALUE)
        when(validationContext.activeDialect()).thenReturn(Optional.of(VersionFlag.DRAFT4));

        ObjectNode node = factory.objectNode();
        node.put("$ref", "#/definitions/foo");
        node.put("type", "string"); // should be ignored for old dialect

        JsonSchema schema = createSchema(node, null);

        JsonValidator refValidator = mock(JsonValidator.class);
        JsonValidator typeValidator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("$ref"), any(JsonNode.class), eq(schema), any()))
                .thenReturn(refValidator);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), eq(schema), any()))
                .thenReturn(typeValidator);

        Method read = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        read.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> validators = (Map<String, JsonValidator>) read.invoke(schema, node);

        // Only the $ref validator should be present
        assertEquals(1, validators.size());
        assertTrue(validators.keySet().iterator().next().endsWith("/$ref"));
        assertSame(refValidator, validators.values().iterator().next());
    }

    @Test
    public void testInitializeValidators_callsPreloadOnce() {
        // Prepare a schema with two dummy validators
        ObjectNode node = factory.objectNode();
        node.put("type", "string");
        node.put("maxLength", 5);

        JsonValidator validator1 = mock(JsonValidator.class);
        JsonValidator validator2 = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validator1);
        when(validationContext.newValidator(anyString(), eq("maxLength"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validator2);

        JsonSchema schema = createSchema(node, null);
        // first call – should trigger preload
        schema.initializeValidators();
        // second call – should NOT trigger another preload
        schema.initializeValidators();

        verify(validator1, times(1)).preloadJsonSchema();
        verify(validator2, times(1)).preloadJsonSchema();
    }

    @Test
    public void testIsDynamicAnchor_trueWhenRecursiveAnchorSet() throws Exception {
        ObjectNode node = factory.objectNode();
        node.put("$recursiveAnchor", true);
        JsonSchema schema = createSchema(node, null);

        // Force the private read() method to be executed (it processes $recursiveAnchor)
        Method read = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        read.setAccessible(true);
        read.invoke(schema, node);

        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testCreateExecutionContext_usesSupplierIfConfigured() {
        // Provide a supplier that returns a pre‑configured ExecutionContext
        ExecutionContext supplied = new ExecutionContext(new CollectorContext(false, false));
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);

        JsonSchema schema = createSchema(emptySchemaNode, null);
        ExecutionContext ctx = schema.createExecutionContext();

        assertSame(supplied, ctx);
    }

    @Test
    public void testValidate_collectsErrorsAndMergesScopes() {
        // Build a schema with two validators: one returns an error, the other succeeds
        ObjectNode node = factory.objectNode();
        node.put("type", "string");
        node.put("maxLength", 5);

        JsonValidator errorValidator = mock(JsonValidator.class);
        JsonValidator successValidator = mock(JsonValidator.class);
        ValidationMessage errMsg = ValidationMessage.of("type", ValidatorTypeCode.TYPE, "type error");
        when(errorValidator.validate(any(), any(), any(), any())).thenReturn(Collections.singleton(errMsg));
        when(successValidator.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(errorValidator);
        when(validationContext.newValidator(anyString(), eq("maxLength"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(successValidator);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        JsonSchema schema = createSchema(node, null);
        JsonNode instance = factory.textNode("hello");

        Set<ValidationMessage> errors = schema.validate(instance);
        assertEquals(1, errors.size());
        assertTrue(errors.contains(errMsg));
    }

    @Test
    public void testFindAncestor_returnsRootWhenNoParent() {
        JsonSchema schema = createSchema(emptySchemaNode, null);
        JsonSchema ancestor = schema.findAncestor();
        assertSame(schema, ancestor);
    }

    @Test
    public void testFindLexicalRoot_stopsAtSchemaWithId() {
        // child schema without id
        ObjectNode childNode = factory.objectNode();
        JsonSchema child = createSchema(childNode, null);

        // parent schema with an "id" keyword
        ObjectNode parentNode = factory.objectNode();
        parentNode.put("$id", "http://example.com/parent");
        JsonSchema parent = createSchema(parentNode, null);
        // attach parent manually (since constructor is private)
        // use reflection to set the parentSchema field
        try {
            java.lang.reflect.Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(child, parent);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame(parent, lexicalRoot);
    }

    @Test
    public void testGetCustomMessage_returnsMessageWhenSupported() throws Exception {
        // Enable custom messages in the config
        when(config.isCustomMessageSupported()).thenReturn(true);
        // Build a schema node with a message object
        ObjectNode node = factory.objectNode();
        ObjectNode messageObj = factory.objectNode();
        messageObj.put("type", "custom type message");
        node.set("message", messageObj);
        node.put("type", "string");

        JsonValidator validator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validator);
        when(validationContext.newValidator(anyString(), eq("message"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(null); // not used

        JsonSchema schema = createSchema(node, null);
        // invoke private getCustomMessage
        Method getCustomMessage = JsonSchema.class.getDeclaredMethod("getCustomMessage", JsonNode.class, String.class);
        getCustomMessage.setAccessible(true);
        String custom = (String) getCustomMessage.invoke(schema, node, "type");
        assertEquals("custom type message", custom);
    }

    @Test
    public void testToString_returnsExpectedFormat() {
        ObjectNode node = factory.objectNode();
        node.put("type", "string");
        JsonSchema schema = createSchema(node, null);
        String result = schema.toString();
        assertTrue(result.startsWith("\"#\" :"));
        assertTrue(result.contains("\"type\":\"string\""));
    }
}
