/****************************************************************************************
 *  JsonSchemaTest.java
 *
 *  Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 *  The tests focus on the public behaviour of the class while using Mockito to stub
 *  collaborators (ValidationContext, JsonMetaSchema, Config, URIFactory, JsonValidator,
 *  etc.).  Private helper methods are exercised indirectly through the public API.
 *
 *  The test class is deliberately verbose to make the intent of each test clear.
 *
 *  Author:  OpenAI ChatGPT
 *  Date:    2026‑10‑23
 ****************************************************************************************/
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.text.MessageFormat;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.utils.StringUtils;
import com.networknt.schema.walk.JsonSchemaWalker;
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

    // -------------------------------------------------------------------------
    // Mocks used across many tests
    // -------------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private JsonMetaSchema metaSchema;
    @Mock private Config config;
    @Mock private URIFactory uriFactory;
    @Mock private JsonValidator mockValidator;
    @Mock private JsonValidator refValidator;
    @Mock private JsonValidator requiredValidator;
    @Mock private TypeValidator typeValidator;
    @Mock private JsonSchemaWalker mockWalker;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;

    private ObjectNode rootSchemaNode;      // the node passed to JsonSchema ctor
    private final JsonNodeFactory nf = JsonNodeFactory.instance;

    // -------------------------------------------------------------------------
    // Helper to create a JsonSchema instance with the supplied schema node
    // -------------------------------------------------------------------------
    private JsonSchema createSchema(JsonNode schemaNode, URI baseUri) {
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        // default behaviours for the config
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        // the factory method that creates validators
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class),
                any(JsonSchema.class), anyString()))
                .thenAnswer(invocation -> {
                    String keyword = invocation.getArgument(1);
                    switch (keyword) {
                        case "$ref":      return refValidator;
                        case "required":  return requiredValidator;
                        case "type":      return typeValidator;
                        default:          return mockValidator;
                    }
                });
        // create the schema
        return JsonSchema.from(validationContext, "#", baseUri, schemaNode, null, false);
    }

    // -------------------------------------------------------------------------
    // Setup common objects before each test
    // -------------------------------------------------------------------------
    @Before
    public void setUp() {
        // a generic simple schema node – individual tests will add fields as needed
        rootSchemaNode = nf.objectNode();

        // ExecutionContext / CollectorContext stubbing – used by validate / walk
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // CollectorContext map handling (ValidatorState)
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(null);
        // stub config supplier for ExecutionContext creation
        when(config.getExecutionContextSupplier()).thenReturn(null);
    }

    // -------------------------------------------------------------------------
    // Static helper method tests
    // -------------------------------------------------------------------------
    @Test
    public void testIsUriFragmentWithNoContext() {
        assertTrue(JsonSchema.isUriFragmentWithNoContext(null, "#/someFragment"));
        assertFalse(JsonSchema.isUriFragmentWithNoContext(URI.create("http://example.com"), "#/frag"));
        assertFalse(JsonSchema.isUriFragmentWithNoContext(null, "http://example.com"));
    }

    @Test
    public void testUriRefersToSubschema() {
        URI uriWithFragment = URI.create("http://example.com/schema.json#foo");
        assertTrue(JsonSchema.uriRefersToSubschema(uriWithFragment, "#"));
        assertTrue(JsonSchema.uriRefersToSubschema(uriWithFragment, ""));
        assertFalse(JsonSchema.uriRefersToSubschema(uriWithFragment, "/somePath"));
        assertFalse(JsonSchema.uriRefersToSubschema(null, "#"));
    }

    // -------------------------------------------------------------------------
    // combineCurrentUriWithIds – various branches
    // -------------------------------------------------------------------------
    @Test
    public void testCombineCurrentUriWithIds_NoId() {
        rootSchemaNode.put("type", "object"); // no id keyword
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, URI.create("http://base"));
        assertEquals(URI.create("http://base"), schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_FragNoContext() {
        // id is a fragment and currentUri is null → method returns null
        rootSchemaNode.put("$id", "#/frag");   // assume "$id" is the id keyword
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn("#/frag");
        JsonSchema schema = createSchema(rootSchemaNode, null);
        assertNull(schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_ValidResolution() throws Exception {
        rootSchemaNode.put("$id", "/sub"); // relative id
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn("/sub");
        // uriFactory.create should concatenate base and id
        when(uriFactory.create(URI.create("http://example.com/base"), "/sub"))
                .thenReturn(new URI("http://example.com/sub"));
        JsonSchema schema = createSchema(rootSchemaNode, URI.create("http://example.com/base"));
        assertEquals(new URI("http://example.com/sub"), schema.getCurrentUri());
    }

    // -------------------------------------------------------------------------
    // getRefSchemaNode – JSON‑Pointer handling and fragment handling
    // -------------------------------------------------------------------------
    @Test
    public void testGetRefSchemaNode_JsonPointer() {
        // root schema: { "defs": { "inner": { "type":"string"} } }
        ObjectNode defs = nf.objectNode();
        defs.set("inner", nf.objectNode().put("type", "string"));
        rootSchemaNode.set("defs", defs);
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        when(metaSchema.getNodeByFragmentRef(anyString(), any())).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, null);

        // reference using JSON‑pointer
        JsonNode result = schema.getRefSchemaNode("#/defs/inner");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_FragmentLookup() {
        // meta‑schema fragment handling – we stub metaSchema to return a node
        JsonNode fragmentNode = nf.objectNode().put("type", "integer");
        when(metaSchema.getNodeByFragmentRef("#/myFragment", rootSchemaNode)).thenReturn(fragmentNode);
        when(validationContext.resolveSchemaId(any())).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, null);

        JsonNode result = schema.getRefSchemaNode("#/myFragment");
        assertSame(fragmentNode, result);
    }

    // -------------------------------------------------------------------------
    // findLexicalRoot – respects presence of id keyword
    // -------------------------------------------------------------------------
    @Test
    public void testFindLexicalRoot_WithIdStopsAtSelf() {
        rootSchemaNode.put("$id", "http://example.com/root");
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn("http://example.com/root");
        JsonSchema schema = createSchema(rootSchemaNode, null);
        // the schema itself has an id, so lexical root should be itself
        assertSame(schema, schema.findLexicalRoot());
    }

    @Test
    public void testFindLexicalRoot_WithoutIdWalksUp() {
        // child schema without id
        ObjectNode childNode = nf.objectNode().put("type", "string");
        when(validationContext.resolveSchemaId(childNode)).thenReturn(null);
        JsonSchema child = JsonSchema.from(validationContext, "#/child", null, childNode, null, false);
        // parent schema with id
        rootSchemaNode.put("$id", "http://example.com/parent");
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn("http://example.com/parent");
        JsonSchema parent = JsonSchema.from(validationContext, "#", null, rootSchemaNode, null, false);
        // link parent/child manually (normally done via constructor)
        child.parentSchema = parent;

        assertSame(parent, child.findLexicalRoot());
    }

    // -------------------------------------------------------------------------
    // findAncestor – simple recursion test
    // -------------------------------------------------------------------------
    @Test
    public void testFindAncestor() {
        JsonSchema child = JsonSchema.from(validationContext, "#/c", null, nf.objectNode(), null, false);
        JsonSchema parent = JsonSchema.from(validationContext, "#", null, nf.objectNode(), null, false);
        child.parentSchema = parent;

        assertSame(parent, child.findAncestor());
        assertSame(parent, parent.findAncestor()); // top‑level returns itself
    }

    // -------------------------------------------------------------------------
    // initializeValidators – ensure preloadJsonSchema invoked once
    // -------------------------------------------------------------------------
    @Test
    public void testInitializeValidatorsPreloadsAllValidators() {
        // schema containing keywords that will trigger creation of three validators
        rootSchemaNode.put("type", "string");
        rootSchemaNode.put("required", nf.arrayNode().add("name"));
        rootSchemaNode.put("$ref", "#/defs/other");

        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, null);

        // first call – validators should be loaded
        schema.initializeValidators();
        verify(mockValidator, times(1)).preloadJsonSchema();
        verify(requiredValidator, times(1)).preloadJsonSchema();
        verify(refValidator, times(1)).preloadJsonSchema();

        // second call – should not preload again
        reset(mockValidator, requiredValidator, refValidator);
        schema.initializeValidators();
        verifyNoInteractions(mockValidator, requiredValidator, refValidator);
    }

    // -------------------------------------------------------------------------
    // $recursiveAnchor handling – dynamicAnchor flag
    // -------------------------------------------------------------------------
    @Test
    public void testDynamicAnchorFlagSetWhenRecursiveAnchorTrue() {
        rootSchemaNode.put("$recursiveAnchor", true);
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, null);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testDynamicAnchorInvalidThrowsException() {
        rootSchemaNode.put("$recursiveAnchor", "notABoolean");
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        // exception is thrown during construction
        createSchema(rootSchemaNode, null);
    }

    // -------------------------------------------------------------------------
    // hasRequiredValidator / getRequiredValidator and typeValidator
    // -------------------------------------------------------------------------
    @Test
    public void testRequiredAndTypeValidatorsPresence() {
        rootSchemaNode.put("required", nf.arrayNode().add("id"));
        rootSchemaNode.put("type", "object");
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, null);

        assertTrue(schema.hasRequiredValidator());
        assertSame(requiredValidator, schema.getRequiredValidator());

        assertTrue(schema.hasTypeValidator());
        assertSame(typeValidator, schema.getTypeValidator());
    }

    // -------------------------------------------------------------------------
    // toString – simple representation check
    // -------------------------------------------------------------------------
    @Test
    public void testToStringRepresentation() {
        rootSchemaNode.put("type", "null");
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, null);
        String expected = "\"#\" : " + rootSchemaNode.toString();
        assertEquals(expected, schema.toString());
    }

    // -------------------------------------------------------------------------
    // createExecutionContext – returns non‑null ExecutionContext with CollectorContext
    // -------------------------------------------------------------------------
    @Test
    public void testCreateExecutionContextUsesConfigSupplierIfPresent() {
        // supplier that returns a pre‑created ExecutionContext
        ExecutionContext supplied = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);
        JsonSchema schema = createSchema(rootSchemaNode, null);
        assertSame(supplied, schema.createExecutionContext());
    }

    @Test
    public void testCreateExecutionContextCreatesDefaultWhenNoSupplier() {
        JsonSchema schema = createSchema(rootSchemaNode, null);
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        // the collector context inside should be the one created by the default constructor
        assertNotNull(ctx.getCollectorContext());
    }

    // -------------------------------------------------------------------------
    // validate(JsonNode) – delegates to validate(ExecutionContext,…)
    // -------------------------------------------------------------------------
    @Test
    public void testValidateRootNodeDelegatesCorrectly() {
        // Schema with a trivial "true" validator (boolean schema)
        JsonNode trueSchema = BooleanNode.TRUE;
        when(validationContext.resolveSchemaId(trueSchema)).thenReturn(null);
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, trueSchema, null, false);

        // stub the internal validator to return an empty error set
        JsonValidator trueValidator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("true"), eq(trueSchema), any(JsonSchema.class), anyString()))
                .thenReturn(trueValidator);
        when(trueValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> errors = schema.validate(nf.objectNode()); // any node – validator always passes
        assertTrue(errors.isEmpty());
    }

    // -------------------------------------------------------------------------
    // walk methods – ensure keywordWalkListenerRunner is invoked safely
    // -------------------------------------------------------------------------
    @Test
    public void testWalkInvokesKeywordWalkListenerRunnerAndWalker() {
        // set up a schema with a dummy keyword that maps to a JsonSchemaWalker
        rootSchemaNode.set("dummy", nf.objectNode());
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        // make the validationContext return a walker for the dummy keyword
        JsonValidator dummyValidator = mock(JsonValidator.class);
        when(validationContext.newValidator(anyString(), eq("dummy"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(dummyValidator);
        // walkers are stored in the validator map; we need to stub getValidators()
        // to contain a walker entry – easiest is to replace the internal map via reflection
        JsonSchema schema = createSchema(rootSchemaNode, null);
        // inject a walker into the private validators map (reflection)
        try {
            java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
            f.setAccessible(true);
            Map<String, JsonValidator> map = new TreeMap<>();
            map.put("#/dummy", dummyValidator);
            // also put a walker entry (the map is of JsonValidator, but walk() iterates over
            // getValidators().forEach((k, JsonSchemaWalker) -> …) – we therefore need a mock
            // JsonSchemaWalker that also implements JsonValidator (the real code stores walkers
            // separately, but for the test we can simulate by casting).
            JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
            when(walker.walk(any(), any(), any(), anyString(), anyBoolean()))
                    .thenReturn(Collections.singleton(ValidationMessage.of("dummy", "dummy", new MessageFormat("msg"))));
            // store the walker as a validator (the code treats it as JsonSchemaWalker)
            map.put("#/dummyWalker", (JsonValidator) walker);
            f.set(schema, Collections.unmodifiableMap(map));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // mock the listener runner – it should be called but we don't care about its result
        schema.keywordWalkListenerRunner = mock(DefaultKeywordWalkListenerRunner.class);
        when(schema.keywordWalkListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any(), any()))
                .thenReturn(true);
        when(schema.keywordWalkListenerRunner.runPostWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any(),
                any(), anySet()))
                .thenReturn(null);

        // invoke walk – it should return the dummy validation message from the walker
        ValidationResult result = schema.walk(nf.objectNode(), false);
        assertFalse(result.getValidationMessages().isEmpty());
        assertEquals("dummy", result.getValidationMessages().iterator().next().getMessage());
    }

    // -------------------------------------------------------------------------
    // getMessageNode and getCustomMessage – exercised via a schema that defines messages
    // -------------------------------------------------------------------------
    @Test
    public void testGetCustomMessageWhenSupported() {
        // enable custom messages
        when(config.isCustomMessageSupported()).thenReturn(true);
        // schema with a custom message for "type"
        ObjectNode messageNode = nf.objectNode();
        messageNode.put("type", "Custom type error");
        ObjectNode schemaWithMessage = nf.objectNode();
        schemaWithMessage.set("message", messageNode);
        schemaWithMessage.put("type", "string");

        when(validationContext.resolveSchemaId(schemaWithMessage)).thenReturn(null);
        JsonSchema schema = createSchema(schemaWithMessage, null);

        // The private getCustomMessage is indirectly called during validator creation.
        // We verify that the validator receives the custom message by checking the
        // argument passed to newValidator for the "type" keyword.
        ArgumentCaptor<String> customMsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(validationContext).newValidator(eq("#"), eq("type"), any(JsonNode.class),
                eq(schema), customMsgCaptor.capture());
        assertEquals("Custom type error", customMsgCaptor.getValue());
    }

    // -------------------------------------------------------------------------
    // validateAndCollect – ensure collector context is used and ValidationResult returned
    // -------------------------------------------------------------------------
    @Test
    public void testValidateAndCollectCollectsErrorsAndReturnsResult() {
        // schema with a required validator that will fail
        ArrayNode requiredArray = nf.arrayNode().add("missing");
        rootSchemaNode.set("required", requiredArray);
        when(validationContext.resolveSchemaId(rootSchemaNode)).thenReturn(null);
        JsonSchema schema = createSchema(rootSchemaNode, null);

        // stub requiredValidator to return a validation message
        ValidationMessage msg = ValidationMessage.of("required", "required", new MessageFormat("{0}"));
        when(requiredValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(msg));

        // stub collector context to be a simple mock
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(null);
        when(config.doLoadCollectors()).thenReturn(false);

        ValidationResult result = schema.validateAndCollect(executionContext, nf.objectNode());
        assertFalse(result.getValidationMessages().isEmpty());
        assertTrue(result.getValidationMessages().contains(msg));
    }
}
