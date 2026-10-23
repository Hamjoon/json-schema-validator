package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private JsonValidator mockValidator;

    @Mock
    private ExecutionContext suppliedExecutionContext;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        // Basic stubbing used by many tests
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(metaSchema.getIdKeyword()).thenReturn("$id");
    }

    /**
     * Helper to create a JsonSchema instance with the supplied schema node.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI currentUri) {
        return JsonSchema.from(validationContext, "#", currentUri, schemaNode, null, false);
    }

    @Test
    public void testCombineCurrentUriWithIds_NoId_ReturnsOriginalUri() throws URISyntaxException {
        JsonNode schemaNode = mapper.createObjectNode(); // empty node -> no id
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);
        URI current = new URI("http://example.com/base.json");

        JsonSchema schema = createSchema(schemaNode, current);
        // The private method is exercised via public getCurrentUri()
        assertEquals(current, schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_FragmentNoContext_ReturnsNull() throws URISyntaxException {
        JsonNode schemaNode = mapper.createObjectNode();
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("#frag");
        // currentUri is null to trigger fragment‑no‑context case
        JsonSchema schema = createSchema(schemaNode, null);
        assertNull(schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_ValidId_ReturnsCombinedUri() throws URISyntaxException {
        JsonNode schemaNode = mapper.createObjectNode();
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("sub.json#def");

        URI base = new URI("http://example.com/base.json");
        URI combined = new URI("http://example.com/sub.json#def");
        when(uriFactory.create(base, "sub.json#def")).thenReturn(combined);

        JsonSchema schema = createSchema(schemaNode, base);
        assertEquals(combined, schema.getCurrentUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIds_InvalidId_ThrowsJsonSchemaException() throws URISyntaxException {
        JsonNode schemaNode = mapper.createObjectNode();
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("!invalid#uri");

        URI base = new URI("http://example.com/base.json");
        when(uriFactory.create(base, "!invalid#uri")).thenThrow(new IllegalArgumentException("bad uri"));

        // The constructor will invoke combineCurrentUriWithIds and therefore throw
        createSchema(schemaNode, base);
    }

    @Test
    public void testIsUriFragmentWithNoContext() {
        // static method – direct call
        assertTrue(JsonSchema.isUriFragmentWithNoContext(null, "#frag"));
        assertFalse(JsonSchema.isUriFragmentWithNoContext(new URI("http://example.com/"), "#frag"));
        assertFalse(JsonSchema.isUriFragmentWithNoContext(null, "http://example.com"));
    }

    @Test
    public void testUriRefersToSubschema() throws URISyntaxException {
        URI uriWithFragment = new URI("http://example.com/schema.json#def");
        assertTrue(JsonSchema.uriRefersToSubschema(uriWithFragment, "#"));
        assertTrue(JsonSchema.uriRefersToSubschema(uriWithFragment, ""));
        assertFalse(JsonSchema.uriRefersToSubschema(uriWithFragment, "/properties"));
        assertFalse(JsonSchema.uriRefersToSubschema(null, "#"));
    }

    @Test
    public void testInitializeValidators_CallsPreloadOnce() {
        // schema with a simple "type" keyword so that read() creates a validator
        JsonNode node = mapper.createObjectNode().put("type", "string");

        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);
        when(mockValidator.preloadJsonSchema()).thenReturn(null);

        JsonSchema schema = createSchema(node, null);
        // first call – should trigger preload
        schema.initializeValidators();
        // second call – should be a no‑op
        schema.initializeValidators();

        verify(mockValidator, times(1)).preloadJsonSchema();
    }

    @Test
    public void testIsDynamicAnchor_TrueWhenRecursiveAnchorTrue() {
        JsonNode node = mapper.createObjectNode().put("$recursiveAnchor", true);
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(validationContext.newValidator(anyString(), eq("$recursiveAnchor"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);
        when(mockValidator.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        JsonSchema schema = createSchema(node, null);
        // force validator loading
        schema.getValidators();

        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testCreateExecutionContext_UsesSupplier() {
        when(config.getExecutionContextSupplier()).thenReturn(() -> suppliedExecutionContext);
        JsonNode node = mapper.createObjectNode();
        when(validationContext.resolveSchemaId(any())).thenReturn(null);

        JsonSchema schema = createSchema(node, null);
        ExecutionContext ctx = schema.createExecutionContext();
        assertSame(suppliedExecutionContext, ctx);
    }

    @Test
    public void testGetCustomMessage_WithSupportedConfigAndMessage() {
        // Build schema node with a message object
        JsonNode node = mapper.createObjectNode()
                .set("message", mapper.createObjectNode().put("type", "my custom message"))
                .put("type", "string");

        when(config.isCustomMessageSupported()).thenReturn(true);
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);
        when(mockValidator.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        JsonSchema schema = createSchema(node, null);
        // trigger validator creation so that getCustomMessage is exercised
        JsonValidator typeValidator = schema.getValidators().get("#/type");
        assertNotNull(typeValidator);

        // Use reflection to call the private getCustomMessage method
        try {
            java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("getCustomMessage", JsonNode.class, String.class);
            m.setAccessible(true);
            String message = (String) m.invoke(schema, node, "type");
            assertEquals("my custom message", message);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testFindLexicalRoot_StopsAtId() {
        // parent schema with $id
        JsonNode parentNode = mapper.createObjectNode().put("$id", "http://example.com/parent");
        JsonNode childNode = mapper.createObjectNode(); // no id

        when(validationContext.resolveSchemaId(parentNode)).thenReturn("http://example.com/parent");
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);

        JsonSchema parent = createSchema(parentNode, null);
        JsonSchema child = parent.createChildSchema("#/child", childNode);

        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame(parent, lexicalRoot);
    }

    @Test
    public void testFindAncestor_ReturnsRootWhenNoParent() {
        JsonNode node = mapper.createObjectNode();
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        JsonSchema schema = createSchema(node, null);
        assertSame(schema, schema.findAncestor());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointer() {
        // Schema node contains a nested property
        JsonNode schemaNode = mapper.createObjectNode()
                .set("properties", mapper.createObjectNode()
                        .set("name", mapper.createObjectNode().put("type", "string")));

        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);

        JsonSchema schema = createSchema(schemaNode, null);

        JsonNode result = schema.getRefSchemaNode("#/properties/name");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_FragmentFallback() {
        // Simulate metaSchema handling a fragment reference
        JsonNode schemaNode = mapper.createObjectNode().put("title", "Root");
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);
        when(metaSchema.getNodeByFragmentRef(eq("#/defs/foo"), any(JsonNode.class))).thenReturn(mapper.createObjectNode().put("type", "integer"));

        JsonSchema schema = createSchema(schemaNode, null);
        JsonNode result = schema.getRefSchemaNode("#/defs/foo");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    @Test
    public void testValidate_CollectsValidatorErrors() {
        // Create two mock validators – one returns an error, the other succeeds
        JsonValidator errorValidator = mock(JsonValidator.class);
        JsonValidator okValidator = mock(JsonValidator.class);
        ValidationMessage msg = ValidationMessage.of("type", CustomErrorMessageType.of("type.error"), new java.text.MessageFormat("{0} error"), "test");
        when(errorValidator.validate(any(), any(), any(), any())).thenReturn(Collections.singleton(msg));
        when(okValidator.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        JsonNode schemaNode = mapper.createObjectNode()
                .put("type", "string")
                .put("maxLength", 5);
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        // Return our two validators in order
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(errorValidator);
        when(validationContext.newValidator(anyString(), eq("maxLength"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(okValidator);

        JsonSchema schema = createSchema(schemaNode, null);
        // Trigger validator loading
        schema.getValidators();

        JsonNode data = mapper.createObjectNode().put("field", "value");
        Set<ValidationMessage> errors = schema.validate(data);
        assertEquals(1, errors.size());
        assertTrue(errors.contains(msg));
    }

    @Test
    public void testToString_IncludesPathAndNode() {
        JsonNode node = mapper.createObjectNode().put("type", "string");
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        JsonSchema schema = createSchema(node, null);
        String str = schema.toString();
        assertTrue(str.contains("\"#\""));
        assertTrue(str.contains("\"type\":\"string\""));
    }
}
