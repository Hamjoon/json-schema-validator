/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests focus on the public API of {@code JsonSchema} and on the
 * edge‑case handling that is performed inside the class.  Because the
 * implementation depends on a large number of collaborators (e.g.
 * {@link com.networknt.schema.ValidationContext},
 * {@link com.networknt.schema.JsonValidator}, etc.) the tests use
 * Mockito to provide lightweight stubs for those collaborators.
 *
 * The test class follows the steps requested:
 * <ul>
 *   <li>list all public methods with signatures,</li>
 *   <li>provide a basic test for each method,</li>
 *   <li>identify edge‑cases / exception scenarios,</li>
 *   <li>add dedicated tests for those scenarios,</li>
 *   <li>merge everything into a single JUnit‑4 test file.</li>
 * </ul>
 *
 * NOTE: The test class uses only public API – the private helper
 * {@code combineCurrentUriWithIds} is exercised indirectly through the
 * constructor and {@link #getCurrentUri()}.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test suite for {@link JsonSchema}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    // -----------------------------------------------------------------------
    // 1.  Public methods of JsonSchema (extracted from the source)
    // -----------------------------------------------------------------------
    //   JsonSchema createChildSchema(String schemaPath, JsonNode schemaNode)
    //   URI getCurrentUri()
    //   JsonNode getRefSchemaNode(String ref)
    //   JsonSchema findLexicalRoot()
    //   JsonSchema findAncestor()
    //   Set<ValidationMessage> validate(ExecutionContext executionContext,
    //                                   JsonNode jsonNode,
    //                                   JsonNode rootNode,
    //                                   String at)
    //   Set<ValidationMessage> validate(JsonNode rootNode)
    //   ValidationResult validateAndCollect(ExecutionContext executionContext,
    //                                       JsonNode node)
    //   ValidationResult validateAndCollect(JsonNode node)
    //   ValidationResult walk(ExecutionContext executionContext,
    //                         JsonNode node,
    //                         boolean shouldValidateSchema)
    //   ValidationResult walk(JsonNode node,
    //                         boolean shouldValidateSchema)
    //   ValidationResult walkAtNode(ExecutionContext executionContext,
    //                               JsonNode node,
    //                               JsonNode rootNode,
    //                               String at,
    //                               boolean shouldValidateSchema)
    //   Set<ValidationMessage> walk(ExecutionContext executionContext,
    //                               JsonNode node,
    //                               JsonNode rootNode,
    //                               String at,
    //                               boolean shouldValidateSchema)
    //   String toString()
    //   boolean hasRequiredValidator()
    //   JsonValidator getRequiredValidator()
    //   boolean hasTypeValidator()
    //   TypeValidator getTypeValidator()
    //   void initializeValidators()
    //   boolean isDynamicAnchor()
    //   ExecutionContext createExecutionContext()
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private URIFactory uriFactory; // inner interface of ValidationContext

    @Mock
    private JsonValidator mockValidator;

    @Mock
    private TypeValidator mockTypeValidator;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Common stubbing used by most tests.
     */
    @Before
    public void setUp() throws Exception {
        // Basic stubbing – the factory returns the same meta‑schema for
        // every call.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        // The URI factory simply resolves against the base URI.
        when(uriFactory.create(any(URI.class), anyString()))
                .thenAnswer(invocation -> {
                    URI base = invocation.getArgument(0);
                    String id = invocation.getArgument(1);
                    return base.resolve(id);
                });
        // newValidator returns a dummy validator that does nothing.
        when(validationContext.newValidator(anyString(), anyString(),
                any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(mockValidator);
    }

    // -----------------------------------------------------------------------
    // 2.  Basic tests for each public method
    // -----------------------------------------------------------------------

    @Test
    public void testGetCurrentUri_returnsProvidedUri() throws Exception {
        URI uri = new URI("http://example.com/schema.json");
        JsonNode schemaNode = mapper.readTree("{}");
        JsonSchema schema = JsonSchema.from(validationContext, "testPath", uri, schemaNode, null, false);

        assertEquals(uri, schema.getCurrentUri());
    }

    @Test
    public void testToString_containsSchemaPathAndNode() throws Exception {
        JsonNode node = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "/myPath", null, node, null, false);
        String txt = schema.toString();

        assertTrue(txt.contains("\"/myPath\""));
        assertTrue(txt.contains("\"type\":\"string\""));
    }

    @Test
    public void testHasRequiredValidator_andGetter_whenPresent() throws Exception {
        // schema with a required keyword
        ObjectNode node = mapper.createObjectNode();
        node.putArray("required").add("a");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);

        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidator_whenAbsent() throws Exception {
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);

        assertFalse(schema.hasRequiredValidator());
        assertNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_andGetter_whenPresent() throws Exception {
        // schema with a type keyword – we need the validator to be a TypeValidator
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "string");
        // make the factory return a TypeValidator instance
        when(validationContext.newValidator(anyString(), eq("type"),
                any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(mockTypeValidator);
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);

        assertTrue(schema.hasTypeValidator());
        assertEquals(mockTypeValidator, schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidator_whenAbsent() throws Exception {
        JsonNode node = mapper.readTree("{\"properties\":{}}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);

        assertFalse(schema.hasTypeValidator());
        assertNull(schema.getTypeValidator());
    }

    @Test
    public void testIsDynamicAnchor_falseByDefault() throws Exception {
        JsonNode node = mapper.readTree("{}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);
        assertFalse(schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchor_trueWhenRecursiveAnchorTrue() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("$recursiveAnchor", true);
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testInitializeValidators_callsPreloadOnce() throws Exception {
        // two dummy validators
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), anyString(),
                any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(v1, v2);

        JsonNode node = mapper.readTree("{\"type\":\"string\",\"maxLength\":5}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);

        // first call – validators are loaded
        schema.initializeValidators();
        verify(v1, times(1)).preloadJsonSchema();
        verify(v2, times(1)).preloadJsonSchema();

        // second call – should be a no‑op
        reset(v1, v2);
        schema.initializeValidators();
        verify(v1, never()).preloadJsonSchema();
        verify(v2, never()).preloadJsonSchema();
    }

    @Test
    public void testCreateExecutionContext_usesSupplierIfPresent() throws Exception {
        ExecutionContext supplied = mock(ExecutionContext.class);
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(supplied);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null,
                mapper.readTree("{}"), null, false);
        ExecutionContext ec = schema.createExecutionContext();

        assertSame(supplied, ec);
    }

    @Test
    public void testCreateExecutionContext_defaultWhenNoSupplier() throws Exception {
        when(config.getExecutionContextSupplier()).thenReturn(null);
        JsonSchema schema = JsonSchema.from(validationContext, "#", null,
                mapper.readTree("{}"), null, false);
        ExecutionContext ec = schema.createExecutionContext();

        assertNotNull(ec);
        // the collector context must be created with the config flags
        CollectorContext cc = ec.getCollectorContext();
        assertNotNull(cc);
    }

    @Test
    public void testFindAncestor_returnsSelfWhenNoParent() throws Exception {
        JsonNode node = mapper.readTree("{}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);
        assertSame(schema, schema.findAncestor());
    }

    @Test
    public void testFindAncestor_returnsRootWhenParentExists() throws Exception {
        // parent schema
        ObjectNode parentNode = mapper.createObjectNode();
        parentNode.put("$id", "http://example.com/parent");
        JsonSchema parent = JsonSchema.from(validationContext, "#", new URI("http://example.com/parent"),
                parentNode, null, false);

        // child schema
        ObjectNode childNode = mapper.createObjectNode();
        JsonSchema child = JsonSchema.from(validationContext, "#/child", null,
                childNode, parent, false);

        // child.findAncestor() should traverse up to the root (parent)
        assertSame(parent, child.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_stopsAtFirstSchemaWithId() throws Exception {
        // root schema with $id
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("$id", "http://example.com/root");
        JsonSchema root = JsonSchema.from(validationContext, "#", new URI("http://example.com/root"),
                rootNode, null, false);

        // intermediate schema without $id
        ObjectNode middleNode = mapper.createObjectNode();
        JsonSchema middle = JsonSchema.from(validationContext, "#/middle", null,
                middleNode, root, false);

        // leaf schema without $id
        JsonSchema leaf = JsonSchema.from(validationContext, "#/leaf", null,
                mapper.readTree("{}"), middle, false);

        assertSame(root, leaf.findLexicalRoot());
    }

    // -----------------------------------------------------------------------
    // 3.  Edge‑case and exception handling tests
    // -----------------------------------------------------------------------

    @Test
    public void testCombineCurrentUriWithIds_nullId_returnsOriginalUri() throws Exception {
        // schema node without "$id"
        JsonNode node = mapper.readTree("{}");
        URI base = new URI("http://example.com/base.json");
        JsonSchema schema = JsonSchema.from(validationContext, "#", base, node, null, false);
        assertEquals(base, schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_fragmentNoContext_returnsNull() throws Exception {
        // schema node with a fragment id and no base URI
        ObjectNode node = mapper.createObjectNode();
        node.put("$id", "#/fragmentOnly");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);
        assertNull(schema.getCurrentUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIds_invalidUriThrowsJsonSchemaException() throws Exception {
        // Force the URI factory to throw IllegalArgumentException
        when(uriFactory.create(any(URI.class), anyString()))
                .thenThrow(new IllegalArgumentException("bad uri"));

        ObjectNode node = mapper.createObjectNode();
        node.put("$id", "http://[bad-uri]"); // any string – the factory will throw
        // constructor should propagate JsonSchemaException
        JsonSchema.from(validationContext, "#", new URI("http://example.com/base.json"),
                node, null, false);
    }

    @Test
    public void testGetRefSchemaNode_jsonPointerResolvesNode() throws Exception {
        // Build a schema that contains a definitions object
        ObjectNode def = mapper.createObjectNode();
        def.put("type", "string");
        ObjectNode root = mapper.createObjectNode();
        root.set("definitions", mapper.createObjectNode().set("myDef", def));

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, root, null, false);
        JsonNode result = schema.getRefSchemaNode("#/definitions/myDef");

        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_jsonPointerWithEscapedChars() throws Exception {
        // a property name that includes a '/' which must be escaped as '~1'
        ObjectNode child = mapper.createObjectNode();
        child.put("type", "integer");
        ObjectNode root = mapper.createObjectNode();
        root.set("prop/with/slash", child); // Jackson will keep the literal '/'

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, root, null, false);
        // the reference uses the escaped form
        JsonNode result = schema.getRefSchemaNode("#/prop~1with~1slash");

        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_missingNode_returnsNull() throws Exception {
        JsonNode node = mapper.readTree("{\"properties\":{}}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);
        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertNull(result);
    }

    @Test
    public void testGetRefSchemaNode_fragmentResolutionViaMetaSchema() throws Exception {
        // Simulate a meta‑schema that can resolve a fragment reference.
        // We mock JsonMetaSchema#getNodeByFragmentRef to return a known node.
        JsonNode fragmentNode = mapper.readTree("{\"type\":\"number\"}");
        when(metaSchema.getNodeByFragmentRef(eq("#/myFragment"), any(JsonNode.class)))
                .thenReturn(fragmentNode);

        JsonNode root = mapper.readTree("{}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, root, null, false);
        JsonNode result = schema.getRefSchemaNode("#/myFragment");

        assertSame(fragmentNode, result);
    }

    // -----------------------------------------------------------------------
    // 4.  Minimal validation/walk tests (they just exercise the code path;
    //    full validator logic is out of scope for unit tests of JsonSchema)
    // -----------------------------------------------------------------------

    @Test
    public void testValidate_returnsEmptySetWhenNoValidators() throws Exception {
        // The schema node is empty – read() will produce no validators.
        JsonNode node = mapper.readTree("{}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);
        Set<ValidationMessage> msgs = schema.validate(mapper.readTree("{}"));
        assertTrue(msgs.isEmpty());
    }

    @Test
    public void testValidateAndCollect_returnsEmptyResultWhenNoErrors() throws Exception {
        JsonNode node = mapper.readTree("{}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node, null, false);
        ValidationResult result = schema.validateAndCollect(mapper.readTree("{}"));
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalkCallsKeywordWalkListenerRunnerWhenPresent() throws Exception {
        // Prepare a dummy walker map – we need at least one entry.
        JsonValidator walkerValidator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), anyString(),
                any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(walkerValidator);
        // Mock the walker itself (JsonSchemaWalker) to return a set with one message.
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        ValidationMessage vm = ValidationMessage.of("type", CustomErrorMessageType.of("type"), "msg");
        when(walker.walk(any(ExecutionContext.class), any(JsonNode.class),
                any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(vm));

        // Inject the walker into the internal validators map via reflection.
        // The JsonSchema class creates the map lazily; we force creation by
        // calling a method that reads validators.
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "string");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);

        // Replace the internal validators map with a map containing our walker.
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, Collections.singletonMap("type", walker));

        // Ensure the keyword walk listener runner is non‑null (it is created in constructor)
        // but we don't need to stub its behaviour – we just want to make sure walk() executes.
        ValidationResult res = schema.walk(mapper.readTree("{\"a\":1}"), false);
        assertNotNull(res);
        assertEquals(1, res.getValidationMessages().size());
        assertTrue(res.getValidationMessages().contains(vm));
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Helper that creates a simple {@link ExecutionContext} with a fresh
     * {@link CollectorContext}. Used in a few tests that need an execution
     * context but do not care about its internal state.
     */
    private ExecutionContext simpleExecutionContext() {
        CollectorContext cc = new CollectorContext(false, false);
        return new ExecutionContext(cc);
    }
}
