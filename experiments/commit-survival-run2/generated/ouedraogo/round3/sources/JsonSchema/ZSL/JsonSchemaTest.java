package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema}.  The tests use Mockito to isolate the
 * {@link JsonSchema} class from the rest of the library (e.g. {@link ValidationContext},
 * {@link JsonValidator}, {@link CollectorContext}, etc.).  Only the public API and
 * a few package‑private helpers are exercised.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private JsonValidator validatorMock;

    @Mock
    private JsonValidator validatorMock2;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Scope parentScope;

    @Mock
    private Scope childScope;

    @Captor
    private ArgumentCaptor<Set<ValidationMessage>> validationMessageCaptor;

    @Before
    public void setUp() {
        // ----- ValidationContext basic behaviour -------------------------------------------------
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(uriFactory.create(any(URI.class), anyString())).thenAnswer(invocation -> {
            URI base = invocation.getArgument(0);
            String id = invocation.getArgument(1);
            return base.resolve(id);
        });

        // ----- Config defaults --------------------------------------------------------------------
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);

        // ----- ExecutionContext / CollectorContext ------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope(any(JsonSchema.class))).thenReturn(parentScope);
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);
        when(parentScope.mergeWith(any(Scope.class))).thenReturn(parentScope);
        when(childScope.getEvaluatedItems()).thenReturn(Collections.emptySet());
        when(childScope.getEvaluatedProperties()).thenReturn(Collections.emptySet());
    }

    /**
     * Helper to create a JsonSchema instance with the supplied node.
     */
    private JsonSchema createSchema(ObjectNode node) {
        // Use the package‑private factory method to avoid invoking the deprecated constructors.
        return JsonSchema.from(validationContext, "#", null, node, null, false);
    }

    @Test
    public void testIsUriFragmentWithNoContext() {
        assertTrue(JsonSchema.isUriFragmentWithNoContext(null, "#fragment"));
        assertFalse(JsonSchema.isUriFragmentWithNoContext(URI.create("http://example.com"), "#fragment"));
        assertFalse(JsonSchema.isUriFragmentWithNoContext(null, "http://example.com"));
    }

    @Test
    public void testUriRefersToSubschema() {
        URI uriWithFragment = URI.create("http://example.com/schema.json#def");
        assertTrue(JsonSchema.uriRefersToSubschema(uriWithFragment, "#"));
        assertFalse(JsonSchema.uriRefersToSubschema(uriWithFragment, "/properties"));
        assertFalse(JsonSchema.uriRefersToSubschema(null, "#"));
    }

    @Test
    public void testFindAncestorAndLexicalRoot() {
        // root schema (no parent)
        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.put("title", "root");
        JsonSchema root = createSchema(rootNode);

        // child schema with an explicit "id" keyword (the meta‑schema id keyword defaults to "$id")
        ObjectNode childNode = MAPPER.createObjectNode();
        childNode.put("$id", "childId");
        JsonSchema child = JsonSchema.from(validationContext, "#/child", null, childNode, root, false);

        // The child should report the root as its ancestor
        assertSame(root, child.findAncestor());

        // Because the child has an id, it should be its own lexical root
        assertSame(child, child.findLexicalRoot());

        // The root (no id) should have itself as lexical root
        assertSame(root, root.findLexicalRoot());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointer() {
        // Build a schema with a nested object under "definitions/foo"
        ObjectNode definitions = MAPPER.createObjectNode();
        definitions.set("foo", MAPPER.createObjectNode().put("type", "string"));
        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set("definitions", definitions);

        JsonSchema schema = createSchema(rootNode);

        JsonNode result = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_FragmentReference_UsesMetaSchema() {
        // The schema itself does not contain the fragment; the meta‑schema will resolve it.
        ObjectNode rootNode = MAPPER.createObjectNode();
        JsonSchema schema = createSchema(rootNode);

        JsonNode fragmentNode = MAPPER.createObjectNode().put("type", "integer");
        when(metaSchema.getNodeByFragmentRef(eq("#/myFragment"), any(JsonNode.class))).thenReturn(fragmentNode);

        JsonNode result = schema.getRefSchemaNode("#/myFragment");
        assertSame(fragmentNode, result);
    }

    @Test
    public void testGetRefSchemaNode_HandlesMissingNode_viaFetchSubSchemaNode() {
        // Simulate a schema that cannot resolve the fragment directly; it must use fetchSubSchemaNode().
        ObjectNode rootNode = MAPPER.createObjectNode();
        JsonSchema schema = createSchema(rootNode);

        // meta‑schema returns null, forcing handleNullNode()
        when(metaSchema.getNodeByFragmentRef(eq("#/missing"), any(JsonNode.class))).thenReturn(null);

        // Stub fetchSubSchemaNode() on the root schema to return a dummy sub‑schema.
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.getRefSchemaNode(anyString())).thenReturn(MAPPER.createObjectNode().put("type", "boolean"));
        // The root's fetchSubSchemaNode is protected; use a spy to override it.
        JsonSchema spy = spy(schema);
        doReturn(subSchema).when(spy).fetchSubSchemaNode(any(ValidationContext.class));

        JsonNode result = spy.getRefSchemaNode("#/missing");
        assertNotNull(result);
        assertEquals("boolean", result.get("type").asText());
    }

    @Test
    public void testDynamicAnchorRecognition() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("$recursiveAnchor", true);
        JsonSchema schema = createSchema(node);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testInitializeValidatorsCallsPreload() {
        // Mock ValidationContext to return a validator that we can verify.
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(validatorMock);

        // Simple schema with a single keyword.
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "string");
        JsonSchema schema = createSchema(node);

        // Before initialization no preload should have been called.
        verify(validatorMock, never()).preloadJsonSchema();

        // Call initializeValidators and verify that preload is invoked exactly once.
        schema.initializeValidators();
        verify(validatorMock, times(1)).preloadJsonSchema();

        // Subsequent calls must not trigger another preload.
        schema.initializeValidators();
        verify(validatorMock, times(1)).preloadJsonSchema();
    }

    @Test
    public void testValidateAggregatesErrorsAndMergesScopes() {
        // First validator returns no errors.
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(validatorMock);
        when(validatorMock.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // Second validator returns a single ValidationMessage.
        when(validationContext.newValidator(anyString(), eq("properties"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(validatorMock2);
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = new LinkedHashSet<>(Collections.singleton(msg));
        when(validatorMock2.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorSet);

        // Build a schema with both keywords.
        ObjectNode props = MAPPER.createObjectNode();
        props.set("name", MAPPER.createObjectNode().put("type", "string"));
        ObjectNode node = MAPPER.createObjectNode();
        node.set("type", MAPPER.createObjectNode().put("type", "object"));
        node.set("properties", props);
        JsonSchema schema = createSchema(node);

        // Execute validation.
        Set<ValidationMessage> result = schema.validate(executionContext, MAPPER.createObjectNode(), MAPPER.createObjectNode(), "");

        // Verify that the error set contains the mocked message.
        assertEquals(1, result.size());
        assertTrue(result.contains(msg));

        // Verify that scope merging occurs for the validator that produced no errors.
        verify(parentScope, times(1)).mergeWith(eq(childScope));
    }

    @Test
    public void testCreateExecutionContextUsesSupplierIfConfigured() {
        // Prepare a custom ExecutionContext that the supplier will return.
        ExecutionContext customCtx = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> customCtx);

        JsonSchema schema = createSchema(MAPPER.createObjectNode());
        ExecutionContext ctx = schema.createExecutionContext();

        assertSame(customCtx, ctx);
    }

    @Test
    public void testCreateChildSchemaDelegatesToValidationContext() {
        JsonSchema parent = createSchema(MAPPER.createObjectNode());
        JsonSchema child = parent.createChildSchema("#/child", MAPPER.createObjectNode());

        // The ValidationContext should have been asked to create a new schema.
        verify(validationContext, times(1)).newSchema(eq("#/child"), any(JsonNode.class), eq(parent));
        assertNotNull(child);
    }

    @Test
    public void testGetCustomMessageWhenSupported() {
        // Enable custom messages.
        when(config.isCustomMessageSupported()).thenReturn(true);

        // Schema node with a custom message for "type".
        ObjectNode messageNode = MAPPER.createObjectNode();
        messageNode.put("type", "custom type error");
        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set("message", messageNode);
        rootNode.put("type", "string");

        // The validator factory will be asked for a validator; we don't care about the result.
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(validatorMock);

        JsonSchema schema = createSchema(rootNode);
        // Force validator loading.
        schema.getValidators();

        // Verify that the validator was created with the custom message.
        verify(validationContext).newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), eq("custom type error"));
    }

    @Test
    public void testToStringProducesReadableOutput() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "string");
        JsonSchema schema = createSchema(node);
        String result = schema.toString();
        assertTrue(result.contains("\"#\""));
        assertTrue(result.contains("\"type\""));
        assertTrue(result.contains("string"));
    }
}
