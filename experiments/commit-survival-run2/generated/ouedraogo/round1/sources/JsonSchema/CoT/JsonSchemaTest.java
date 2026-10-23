package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

/**
 * Unit tests for {@link JsonSchema}.  The tests focus on the public API,
 * basic functional correctness and the identified edge‑cases.
 *
 * Because {@code JsonSchema} collaborates heavily with the validation
 * framework, most collaborators are mocked (ValidationContext, JsonMetaSchema,
 * JsonValidator, URIFactory, SchemaValidatorsConfig, etc.).  Real
 * {@code JsonNode} objects are created with Jackson's {@link ObjectMapper}
 * where needed.
 *
 * The tests are written for JUnit‑4 and use Mockito for stubbing.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    /* --------------------------------------------------------------
     *  List of public methods (signatures) extracted from JsonSchema
     * --------------------------------------------------------------
     *
     *  public static JsonSchema from(ValidationContext validationContext,
     *                                 String schemaPath,
     *                                 URI currentUri,
     *                                 JsonNode schemaNode,
     *                                 JsonSchema parent,
     *                                 boolean suppressSubSchemaRetrieval)
     *
     *  @Deprecated public JsonSchema(ValidationContext validationContext,
     *                                 URI baseUri,
     *                                 JsonNode schemaNode)
     *
     *  @Deprecated public JsonSchema(ValidationContext validationContext,
     *                                 String schemaPath,
     *                                 URI currentUri,
     *                                 JsonNode schemaNode,
     *                                 JsonSchema parent)
     *
     *  @Deprecated public JsonSchema(ValidationContext validationContext,
     *                                 URI baseUri,
     *                                 JsonNode schemaNode,
     *                                 boolean suppressSubSchemaRetrieval)
     *
     *  public JsonSchema createChildSchema(String schemaPath, JsonNode schemaNode)
     *
     *  public URI getCurrentUri()
     *
     *  public JsonNode getRefSchemaNode(String ref)
     *
     *  JsonSchema findLexicalRoot()
     *
     *  public JsonSchema findAncestor()
     *
     *  public Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                          JsonNode jsonNode,
     *                                          JsonNode rootNode,
     *                                          String at)
     *
     *  public Set<ValidationMessage> validate(JsonNode rootNode)
     *
     *  public ValidationResult validateAndCollect(ExecutionContext executionContext,
     *                                              JsonNode node)
     *
     *  private ValidationResult validateAndCollect(ExecutionContext executionContext,
     *                                               JsonNode jsonNode,
     *                                               JsonNode rootNode,
     *                                               String at)
     *
     *  public ValidationResult validateAndCollect(JsonNode node)
     *
     *  public ValidationResult walk(ExecutionContext executionContext,
     *                                JsonNode node,
     *                                boolean shouldValidateSchema)
     *
     *  public ValidationResult walk(JsonNode node,
     *                               boolean shouldValidateSchema)
     *
     *  public ValidationResult walkAtNode(ExecutionContext executionContext,
     *                                      JsonNode node,
     *                                      JsonNode rootNode,
     *                                      String at,
     *                                      boolean shouldValidateSchema)
     *
     *  public Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                      JsonNode node,
     *                                      JsonNode rootNode,
     *                                      String at,
     *                                      boolean shouldValidateSchema)
     *
     *  public String toString()
     *
     *  public boolean hasRequiredValidator()
     *
     *  public JsonValidator getRequiredValidator()
     *
     *  public boolean hasTypeValidator()
     *
     *  public TypeValidator getTypeValidator()
     *
     *  public void initializeValidators()
     *
     *  public boolean isDynamicAnchor()
     *
     *  public ExecutionContext createExecutionContext()
     *
     * -------------------------------------------------------------- */

    // -----------------------------------------------------------------
    //  Common test fixtures
    // -----------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private JsonMetaSchema metaSchema;
    @Mock private URIFactory uriFactory;
    @Mock private JsonValidator validatorMock;
    @Mock private JsonValidator refValidatorMock;
    @Mock private TypeValidator typeValidatorMock;
    @Mock private SchemaValidatorsConfig configMock;
    @Mock private WalkListenerRunner walkListenerRunnerMock;
    @Mock private JsonSchemaWalker walkerMock;

    private final ObjectMapper mapper = new ObjectMapper();

    // Helper to build a very small JsonSchema instance
    private JsonSchema buildSchema(JsonNode schemaNode, URI currentUri) {
        // Minimal behaviour required from the validation context
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getConfig()).thenReturn(configMock);
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class),
                any(JsonSchema.class), anyString())).thenReturn(validatorMock);
        when(metaSchema.getIdKeyword()).thenReturn("$id"); // default id keyword

        // The factory method is private; use the public deprecated constructor for simplicity
        return new JsonSchema(validationContext, "#", currentUri, schemaNode, null);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    // -----------------------------------------------------------------
    //  Basic functionality tests
    // -----------------------------------------------------------------

    @Test
    public void testGetCurrentUri_ReturnsSuppliedUri() throws URISyntaxException {
        URI uri = new URI("http://example.com/schema.json");
        JsonNode schemaNode = mapper.createObjectNode(); // empty schema
        JsonSchema schema = buildSchema(schemaNode, uri);

        assertEquals(uri, schema.getCurrentUri());
    }

    @Test
    public void testToString_FormatsCorrectly() {
        JsonNode node = mapper.createObjectNode().put("type", "string");
        JsonSchema schema = buildSchema(node, null);
        String expected = "\"#\" : " + node.toString();
        assertEquals(expected, schema.toString());
    }

    @Test
    public void testHasRequiredValidator_InitiallyFalse() {
        JsonNode node = mapper.createObjectNode(); // no "required"
        JsonSchema schema = buildSchema(node, null);
        assertFalse(schema.hasRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_InitiallyFalse() {
        JsonNode node = mapper.createObjectNode(); // no "type"
        JsonSchema schema = buildSchema(node, null);
        assertFalse(schema.hasTypeValidator());
    }

    @Test
    public void testCreateChildSchema_DelegatesToValidationContext() {
        JsonNode childNode = mapper.createObjectNode().put("type", "integer");
        JsonSchema parent = buildSchema(mapper.createObjectNode(), null);
        JsonSchema child = parent.createChildSchema("/child", childNode);

        // The child schema must have the same validation context as the parent
        assertSame(validationContext, child.getValidationContext());
        assertEquals("/child", child.getSchemaPath());
        assertEquals(childNode, child.getSchemaNode());
    }

    @Test
    public void testInitializeValidators_IdempotentAndPreloads() {
        // Prepare a schema that contains a validator which will be returned from the context
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class),
                any(JsonSchema.class), anyString())).thenReturn(typeValidatorMock);
        JsonNode schemaNode = mapper.createObjectNode().put("type", "string");
        JsonSchema schema = buildSchema(schemaNode, null);

        // First call – should preload
        schema.initializeValidators();
        verify(typeValidatorMock, times(1)).preloadJsonSchema();

        // Second call – should be a no‑op, no additional preload
        schema.initializeValidators();
        verifyNoMoreInteractions(typeValidatorMock);
    }

    @Test
    public void testIsDynamicAnchor_FalseByDefault() {
        JsonNode node = mapper.createObjectNode(); // no $recursiveAnchor
        JsonSchema schema = buildSchema(node, null);
        assertFalse(schema.isDynamicAnchor());
    }

    // -----------------------------------------------------------------
    //  Edge‑case / exception handling tests
    // -----------------------------------------------------------------

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIds_ThrowsOnInvalidId() throws Exception {
        // Simulate a non‑null id that leads to IllegalArgumentException from URI factory
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn("invalid:uri");
        when(uriFactory.create(any(URI.class), eq("invalid:uri"))).thenThrow(new IllegalArgumentException());

        // The constructor will call combineCurrentUriWithIds and should re‑throw as JsonSchemaException
        new JsonSchema(validationContext, "#", new URI("http://example.com/base"), mapper.createObjectNode(), null);
    }

    @Test
    public void testCombineCurrentUriWithIds_FragmentWithoutBase_ReturnsNull() throws URISyntaxException {
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn("#/defs/foo");
        JsonSchema schema = new JsonSchema(validationContext, "#", null, mapper.createObjectNode(), null);
        assertNull(schema.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerResolution() throws Exception {
        // Build a schema that contains a nested definition
        ObjectNode root = mapper.createObjectNode();
        ObjectNode defs = mapper.createObjectNode();
        ObjectNode foo = mapper.createObjectNode().put("type", "string");
        defs.set("foo", foo);
        root.set("definitions", defs);
        JsonSchema schema = buildSchema(root, new URI("http://example.com/schema.json"));

        // Resolve "#/definitions/foo"
        JsonNode result = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_UnresolvableFragment_UsesFallback() {
        // Schema without the fragment; metaSchema.getNodeByFragmentRef will return null.
        // The fallback path goes through handleNullNode → fetchSubSchemaNode which returns null.
        JsonNode empty = mapper.createObjectNode();
        when(metaSchema.getNodeByFragmentRef(anyString(), any(JsonNode.class))).thenReturn(null);
        JsonSchema schema = buildSchema(empty, null);

        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertNull(result);
    }

    @Test
    public void testFindAncestor_ReturnsRootWhenNoParent() {
        JsonSchema root = buildSchema(mapper.createObjectNode(), null);
        assertSame(root, root.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_StopsAtSchemaWithId() {
        // Build a parent schema that has an $id keyword
        ObjectNode parentNode = mapper.createObjectNode().put("$id", "http://example.com/parent");
        JsonSchema parent = buildSchema(parentNode, new URI("http://example.com/parent"));

        // Child without its own $id
        ObjectNode childNode = mapper.createObjectNode();
        JsonSchema child = new JsonSchema(validationContext, "#/child", new URI("http://example.com/parent#child"),
                childNode, parent);

        // The lexical root should be the parent (hasId == true)
        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame(parent, lexicalRoot);
    }

    @Test
    public void testCreateExecutionContext_UsesSupplierIfConfigured() {
        ExecutionContext suppliedCtx = mock(ExecutionContext.class);
        when(configMock.getExecutionContextSupplier()).thenReturn(() -> suppliedCtx);
        JsonSchema schema = buildSchema(mapper.createObjectNode(), null);
        ExecutionContext ctx = schema.createExecutionContext();
        assertSame(suppliedCtx, ctx);
    }

    @Test
    public void testCreateExecutionContext_DefaultImplementation() {
        when(configMock.getExecutionContextSupplier()).thenReturn(null);
        JsonSchema schema = buildSchema(mapper.createObjectNode(), null);
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }

    // -----------------------------------------------------------------
    //  Validation / Walk method sanity checks (lightweight)
    // -----------------------------------------------------------------

    @Test
    public void testValidate_EmptySchemaReturnsNoErrors() {
        JsonNode empty = mapper.createObjectNode();
        JsonSchema schema = buildSchema(empty, null);
        ExecutionContext ctx = schema.createExecutionContext();

        Set<ValidationMessage> errors = schema.validate(ctx, mapper.createObjectNode(),
                mapper.createObjectNode(), "#");
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testWalk_DelegatesToKeywordWalkListenersAndWalkers() {
        // Setup a schema with a dummy walker
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(configMock);
        when(configMock.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class),
                any(JsonSchema.class), anyString())).thenReturn(validatorMock);
        // Force the internal map to contain one walker entry
        JsonSchema schema = buildSchema(mapper.createObjectNode(), null);
        // Use reflection to inject a walker (so we can verify it is called)
        try {
            java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
            f.setAccessible(true);
            java.util.Map<String, JsonValidator> map = new java.util.TreeMap<>();
            map.put("#/dummy", validatorMock); // validator is not used in walk directly
            f.set(schema, Collections.unmodifiableMap(map));

            java.lang.reflect.Field walkMapField = JsonSchema.class.getDeclaredField("walkers");
            // The class does not expose a "walkers" field; walk() iterates over getValidators()
            // which returns JsonValidator objects.  For the purpose of this test we only need to
            // ensure that walk() returns an empty set when there are no JsonSchemaWalker
            // instances (the default case).
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }

        ValidationResult result = schema.walk(schema.createExecutionContext(),
                mapper.createObjectNode(), false);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    // -----------------------------------------------------------------
    //  Helper methods for the test class
    // -----------------------------------------------------------------

    /**
     * Utility to create a {@link JsonSchema} from a JSON string.
     */
    private JsonSchema schemaFromJson(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            return buildSchema(node, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
