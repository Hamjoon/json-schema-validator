/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written by three “experts”.  Each expert contributed a
 * representative test case for a distinct group of behaviours
 * (URI handling, validator initialisation, dynamic‑anchor handling, etc.).
 * The final file combines all the cases so that the whole class is exercised
 * – covering normal usage, edge conditions and error paths.
 *
 * The library under test (networknt JSON‑Schema‑Validator) is assumed to be on the
 * test class‑path.  Mockito (2.x+) is used for lightweight stubbing of the
 * heavy collaborators (ValidationContext, URIFactory, JsonValidator …).
 *
 * Run with:  mvn test   (or any JUnit‑4 runner)
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.walk.JsonSchemaWalker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContextMock;

    @Mock
    private JsonMetaSchema metaSchemaMock;

    @Mock
    private URIFactory uriFactoryMock;

    @Mock
    private JsonValidator validatorMock1;

    @Mock
    private JsonValidator validatorMock2;

    @Mock
    private JsonSchemaWalker walkerMock;

    private JsonSchema jsonSchema;

    /**
     * Helper to build a {@link JsonSchema} instance using the deprecated constructor.
     * The constructor is convenient for unit tests because it allows us to inject the
     * mocked {@link ValidationContext} directly.
     */
    private JsonSchema buildSchema(JsonNode schemaNode, URI baseUri) {
        // The deprecated constructor is still public and delegates to the private one.
        return new JsonSchema(validationContextMock, baseUri, schemaNode);
    }

    @Before
    public void setUp() throws Exception {
        // --------------------------------------------------------------------
        // Common stubbing for the ValidationContext used by many tests.
        // --------------------------------------------------------------------
        when(validationContextMock.getMetaSchema()).thenReturn(metaSchemaMock);
        when(validationContextMock.getURIFactory()).thenReturn(uriFactoryMock);
        when(validationContextMock.getConfig()).thenReturn(new SchemaValidatorsConfig());

        // --------------------------------------------------------------------
        // Default schema used by several tests – a simple empty object schema.
        // --------------------------------------------------------------------
        JsonNode emptySchema = MAPPER.readTree("{}");
        jsonSchema = buildSchema(emptySchema, new URI("http://example.com/base"));
    }

    // ------------------------------------------------------------------------
    // 1️⃣  Expert A – URI handling (combineCurrentUriWithIds, helpers)
    // ------------------------------------------------------------------------

    @Test
    public void combineCurrentUriWithIds_nullId_returnsSameUri() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{}");
        when(validationContextMock.resolveSchemaId(schemaNode)).thenReturn(null);

        Method method = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        method.setAccessible(true);
        URI base = new URI("http://example.com/schema.json");

        URI result = (URI) method.invoke(jsonSchema, base, schemaNode);
        assertEquals("When the schema has no $id the current URI must stay unchanged", base, result);
    }

    @Test
    public void combineCurrentUriWithIds_fragmentWithoutContext_returnsNull() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"$id\":\"#/foo\"}");
        when(validationContextMock.resolveSchemaId(schemaNode)).thenReturn("#/foo");

        Method method = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        method.setAccessible(true);
        // currentUri is null → fragment without context → should return null
        URI result = (URI) method.invoke(jsonSchema, null, schemaNode);
        assertNull("Fragment‑only $id with no base URI must yield null", result);
    }

    @Test
    public void combineCurrentUriWithIds_validId_isResolved() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"$id\":\"defs.json\"}");
        when(validationContextMock.resolveSchemaId(schemaNode)).thenReturn("defs.json");
        // Simulate normal URI resolution (base + relative)
        when(uriFactoryMock.create(any(URI.class), eq("defs.json")))
                .thenAnswer(invocation -> {
                    URI base = invocation.getArgument(0);
                    return base.resolve("defs.json");
                });

        Method method = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        method.setAccessible(true);
        URI base = new URI("http://example.com/base/");
        URI result = (URI) method.invoke(jsonSchema, base, schemaNode);
        assertEquals("http://example.com/base/defs.json", result.toString());
    }

    @Test
    public void isUriFragmentWithNoContext_trueWhenFragmentAndNoBase() throws Exception {
        Method method = JsonSchema.class.getDeclaredMethod("isUriFragmentWithNoContext", URI.class, String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(null, null, "#/foo");
        assertTrue(result);
    }

    @Test
    public void uriRefersToSubschema_trueWhenFragmentAndRootPath() throws Exception {
        Method method = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        method.setAccessible(true);
        URI uri = new URI("http://example.com/schema.json#sub");
        boolean result = (boolean) method.invoke(null, uri, "#");
        assertTrue(result);
    }

    // ------------------------------------------------------------------------
    // 2️⃣  Expert B – Validator initialisation, dynamic anchors, execution context
    // ------------------------------------------------------------------------

    @Test
    public void initializeValidators_callsPreloadExactlyOnce() throws Exception {
        // Prepare a JsonSchema with a pre‑populated validators map.
        Map<String, JsonValidator> map = new TreeMap<>();
        map.put("#/v1", validatorMock1);
        map.put("#/v2", validatorMock2);

        // Use reflection to inject the map (bypassing lazy loading)
        java.lang.reflect.Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        validatorsField.set(jsonSchema, Collections.unmodifiableMap(map));

        // First call – should invoke preload on each validator
        jsonSchema.initializeValidators();
        verify(validatorMock1, times(1)).preloadJsonSchema();
        verify(validatorMock2, times(1)).preloadJsonSchema();

        // Second call – should NOT invoke again
        jsonSchema.initializeValidators();
        verifyNoMoreInteractions(validatorMock1, validatorMock2);
    }

    @Test
    public void isDynamicAnchor_trueWhenRecursiveAnchorIsTrue() throws Exception {
        // Schema containing "$recursiveAnchor": true
        String schemaStr = "{ \"$recursiveAnchor\": true }";
        JsonNode node = MAPPER.readTree(schemaStr);
        JsonSchema schema = buildSchema(node, new URI("http://example.com/rec"));

        // The private field dynamicAnchor is set during read()
        // Trigger read() by calling a public method that uses validators
        // (validate() will lazily invoke read())
        schema.validate(MAPPER.readTree("{}")); // no validation errors expected

        assertTrue("Schema with $recursiveAnchor:true must report isDynamicAnchor()==true", schema.isDynamicAnchor());
    }

    @Test
    public void createExecutionContext_usesSupplierWhenConfigured() {
        // Prepare a custom config with a supplier
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        ExecutionContext supplied = mock(ExecutionContext.class);
        cfg.setExecutionContextSupplier(() -> supplied);
        when(validationContextMock.getConfig()).thenReturn(cfg);

        // Build a fresh schema (so that the config is taken into account)
        JsonNode node = MAPPER.readTree("{}");
        JsonSchema schema = buildSchema(node, URI.create("http://example.com/ctx"));

        ExecutionContext ec = schema.createExecutionContext();
        assertSame("When a supplier is present the created context must be the supplied one", supplied, ec);
    }

    // ------------------------------------------------------------------------
    // 3️⃣  Expert C – $ref handling, ancestor lookup and walk infrastructure
    // ------------------------------------------------------------------------

    @Test
    public void getRefSchemaNode_resolvesLocalFragment() throws Exception {
        // Build a schema with a definition and a $ref to it
        String schemaJson = "{\n" +
                "  \"$id\": \"http://example.com/root.json\",\n" +
                "  \"definitions\": {\n" +
                "    \"foo\": { \"type\": \"string\" }\n" +
                "  },\n" +
                "  \"$ref\": \"#/definitions/foo\"\n" +
                "}";
        JsonNode rootNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = buildSchema(rootNode, URI.create("http://example.com/root.json"));

        JsonNode refNode = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull("Reference node must be resolved", refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void getRefSchemaNode_returnsNullWhenFragmentNotFound() {
        JsonNode node = MAPPER.readTree("{\"definitions\":{}}");
        JsonSchema schema = buildSchema(node, URI.create("http://example.com/empty"));
        JsonNode result = schema.getRefSchemaNode("#/definitions/missing");
        assertNull("Missing fragment must result in null", result);
    }

    @Test
    public void findAncestor_returnsRootWhenNoParent() {
        // The schema built in setUp() has no parent – findAncestor should return itself
        JsonSchema ancestor = jsonSchema.findAncestor();
        assertSame("Root schema must be its own ancestor when no parent exists", jsonSchema, ancestor);
    }

    @Test
    public void walk_delegatesToKeywordWalkListenerRunnerAndCollectsMessages() throws Exception {
        // Stub a validator that produces a single ValidationMessage
        JsonValidator mockValidator = mock(JsonValidator.class);
        ValidationMessage vm = ValidationMessage.of("type", CustomErrorMessageType.of("type.error"), null);
        when(mockValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(vm));

        // Inject the validator via reflection (bypass lazy loading)
        Map<String, JsonValidator> map = new TreeMap<>();
        map.put("#/type", mockValidator);
        java.lang.reflect.Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        validatorsField.set(jsonSchema, Collections.unmodifiableMap(map));

        // Stub the KeywordWalkListenerRunner to always allow the walk (pre‑walk returns true)
        KeywordWalkListenerRunner runnerMock = mock(KeywordWalkListenerRunner.class);
        when(runnerMock.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        java.lang.reflect.Field runnerField = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        runnerField.setAccessible(true);
        runnerField.set(jsonSchema, runnerMock);

        // Execute the walk
        JsonNode dummy = MAPPER.readTree("{\"any\":\"value\"}");
        ValidationResult result = jsonSchema.walk(dummy, true);

        // Verify that the validator was invoked and its message appears in the result
        assertFalse("ValidationResult must contain errors", result.getValidationMessages().isEmpty());
        assertTrue(result.getValidationMessages().contains(vm));
    }

    // ------------------------------------------------------------------------
    // Helper method for creating an ExecutionContext (used by some tests)
    // ------------------------------------------------------------------------
    private ExecutionContext createDummyExecutionContext() {
        CollectorContext collectorContext = new CollectorContext(false, false);
        return new ExecutionContext(collectorContext);
    }
}
