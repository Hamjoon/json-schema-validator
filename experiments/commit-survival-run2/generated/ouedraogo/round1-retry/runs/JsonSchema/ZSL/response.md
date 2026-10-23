###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * <p>
 * The real {@code JsonSchema} class collaborates with many other classes
 * from the *networknt* JSON‑Schema library (e.g. {@code ValidationContext},
 * {@code JsonValidator}, {@code CollectorContext}, …).  Creating real
 * instances of those collaborators would make the test suite extremely heavy
 * and brittle.  Instead we use Mockito to provide thin, well‑behaved stubs
 * that allow us to focus on the logic inside {@code JsonSchema} itself.
 * </p>
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>URI handling – {@code combineCurrentUriWithIds},
 *       {@code uriRefersToSubschema} and the constructor side‑effects.</li>
 *   <li>Schema navigation – {@code findAncestor}, {@code findLexicalRoot},
 *       {@code getRefSchemaNode} (different reference forms).</li>
 *   <li>Validator creation – boolean schemas, $ref handling, custom messages
 *       and the {@code $recursiveAnchor} flag.</li>
 *   <li>Utility methods – {@code toString}, {@code hasRequiredValidator},
 *       {@code hasTypeValidator}, {@code isDynamicAnchor},
 *       {@code initializeValidators} and {@code createExecutionContext}.</li>
 *   <li>Validation / walk entry points – that they delegate to the
 *       underlying {@code JsonValidator}s and correctly interact with the
 *       {@code CollectorContext}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The test class deliberately does not attempt to validate a full JSON
 * document against a complex schema – that is the responsibility of the
 * library itself.  The goal is to assert that the public API of
 * {@code JsonSchema} behaves as documented when its collaborators are
 * mocked.
 * </p>
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.utils.StringUtils;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import com.networknt.schema.walk.WalkListenerRunner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;

public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private JsonValidator mockValidator;

    @Mock
    private JsonValidator mockRefValidator;

    @Mock
    private JsonValidator mockRequiredValidator;

    @Mock
    private TypeValidator mockTypeValidator;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @InjectMocks
    private JsonSchema jsonSchema; // will be created in each test via factory method

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Default behaviour for the ValidationContext
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.activeDialect()).thenReturn(Optional.empty());

        // Default config behaviour
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        when(config.getExecutionContextSupplier()).thenReturn(null);

        // ExecutionContext / CollectorContext plumbing
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(null);
    }

    /**
     * Helper that builds a JsonSchema instance using the private constructor.
     */
    private JsonSchema buildSchema(String schemaPath, URI currentUri, JsonNode schemaNode) {
        return new JsonSchema(validationContext, schemaPath, currentUri, schemaNode, null, false);
    }

    /* -------------------------------------------------------------
     *  URI handling tests
     * ------------------------------------------------------------- */

    @Test
    public void testCombineCurrentUriWithIds_NoId_ReturnsOriginal() throws Exception {
        URI base = new URI("http://example.com/schema.json");
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");

        // resolveSchemaId returns null → combineCurrentUriWithIds should return the original URI
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);

        JsonSchema schema = buildSchema("#", base, schemaNode);
        assertEquals(base, schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_WithAbsoluteId_ReplacesBaseUri() throws Exception {
        URI base = new URI("http://example.com/schema.json");
        URI id = new URI("http://example.org/other.json");
        JsonNode schemaNode = MAPPER.readTree("{\"$id\":\"http://example.org/other.json\"}");

        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(id.toString());
        when(uriFactory.create(base, id.toString())).thenReturn(id);

        JsonSchema schema = buildSchema("#", base, schemaNode);
        assertEquals(id, schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_FragmentWithoutBase_ReturnsNull() throws Exception {
        URI base = null;
        JsonNode schemaNode = MAPPER.readTree("{\"$id\":\"#/defs/foo\"}");

        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("#/defs/foo");
        // no URI factory call – method should return null directly
        JsonSchema schema = buildSchema("#", base, schemaNode);
        assertNull(schema.getCurrentUri());
    }

    @Test
    public void testUriRefersToSubschema_PositiveAndNegativeCases() throws URISyntaxException {
        URI withFragment = new URI("http://example.com/schema.json#sub");
        assertTrue(JsonSchema.uriRefersToSubschema(withFragment, "#"));
        assertTrue(JsonSchema.uriRefersToSubschema(withFragment, ""));
        assertFalse(JsonSchema.uriRefersToSubschema(withFragment, "/properties"));
        assertFalse(JsonSchema.uriRefersToSubschema(null, "#"));
    }

    /* -------------------------------------------------------------
     *  Schema navigation tests
     * ------------------------------------------------------------- */

    @Test
    public void testFindAncestor_ParentChain() throws Exception {
        JsonNode rootNode = MAPPER.readTree("{\"type\":\"object\"}");
        JsonNode childNode = MAPPER.readTree("{\"type\":\"string\"}");

        JsonSchema root = buildSchema("#", null, rootNode);
        JsonSchema child = root.createChildSchema("#/properties/name", childNode);

        // child.getParentSchema() is root; findAncestor should return root
        assertSame(root, child.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_StopsAtId() throws Exception {
        // Root schema has an $id
        JsonNode rootNode = MAPPER.readTree("{\"$id\":\"http://example.com/root\",\"type\":\"object\"}");
        when(validationContext.resolveSchemaId(rootNode)).thenReturn("http://example.com/root");

        JsonSchema root = buildSchema("#", null, rootNode);
        // Child without its own id
        JsonNode childNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema child = root.createChildSchema("#/properties/name", childNode);

        // child.hasId == false, so lexical root should be root (which hasId == true)
        assertSame(root, child.findLexicalRoot());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerAndFragment() throws Exception {
        // Build a simple schema with nested properties
        JsonNode schemaNode = MAPPER.readTree(
                "{ \"properties\": { \"foo\": { \"type\": \"string\" } }, \"definitions\": { \"bar\": { \"type\": \"integer\" } } }");

        JsonSchema schema = buildSchema("#", null, schemaNode);

        // Mock metaSchema.getNodeByFragmentRef for fragment style refs
        JsonNode defBarNode = schemaNode.at("/definitions/bar");
        when(metaSchema.getNodeByFragmentRef("#/definitions/bar", schemaNode)).thenReturn(defBarNode);

        // 1) JSON‑pointer style "#/properties/foo"
        JsonNode fooNode = schema.getRefSchemaNode("#/properties/foo");
        assertTrue(fooNode.isObject());
        assertEquals("string", fooNode.get("type").asText());

        // 2) Fragment style "#/definitions/bar"
        JsonNode barNode = schema.getRefSchemaNode("#/definitions/bar");
        assertEquals(defBarNode, barNode);
    }

    /* -------------------------------------------------------------
     *  Validator creation / read() tests
     * ------------------------------------------------------------- */

    @Test
    public void testReadBooleanTrueCreatesValidator() throws Exception {
        JsonNode trueNode = MAPPER.readTree("true");
        JsonSchema schema = buildSchema("#", null, trueNode);

        // ValidationContext should be asked to create a validator for "true"
        when(validationContext.newValidator(anyString(), eq("true"), eq(trueNode), eq(schema), anyString()))
                .thenReturn(mockValidator);

        Map<String, JsonValidator> validators = schema.getValidators();
        assertEquals(1, validators.size());
        assertTrue(validators.containsKey("#/true"));
    }

    @Test
    public void testReadBooleanFalseCreatesValidator() throws Exception {
        JsonNode falseNode = MAPPER.readTree("false");
        JsonSchema schema = buildSchema("#", null, falseNode);

        when(validationContext.newValidator(anyString(), eq("false"), eq(falseNode), eq(schema), anyString()))
                .thenReturn(mockValidator);

        Map<String, JsonValidator> validators = schema.getValidators();
        assertEquals(1, validators.size());
        assertTrue(validators.containsKey("#/false"));
    }

    @Test
    public void testReadWithRefAndOldDialectKeepsOnlyRef() throws Exception {
        // Mock active dialect to be older than V201909 (e.g. draft‑07)
        when(validationContext.activeDialect()).thenReturn(Optional.of(VersionFlag.V7));

        JsonNode schemaNode = MAPPER.readTree("{ \"$ref\":\"#/defs/foo\", \"type\":\"string\" }");
        JsonSchema schema = buildSchema("#", null, schemaNode);

        // The $ref validator
        when(validationContext.newValidator(anyString(), eq("$ref"), any(JsonNode.class), eq(schema), anyString()))
                .thenReturn(mockRefValidator);
        // The type validator (should be ignored for old dialect)
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), eq(schema), anyString()))
                .thenReturn(mockTypeValidator);

        Map<String, JsonValidator> validators = schema.getValidators();
        // Only $ref should survive
        assertEquals(1, validators.size());
        assertTrue(validators.containsKey("#/$ref"));
    }

    @Test
    public void testReadRecursiveAnchorSetsDynamicFlag() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{ \"$recursiveAnchor\": true }");
        JsonSchema schema = buildSchema("#", null, schemaNode);
        // No need to stub validator creation – we only care about the flag
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testReadRequiredAndTypeValidatorsAreStored() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{ \"required\":[\"id\"], \"type\":\"object\" }");
        JsonSchema schema = buildSchema("#", null, schemaNode);

        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), eq(schema), anyString()))
                .thenReturn(mockRequiredValidator);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), eq(schema), anyString()))
                .thenReturn(mockTypeValidator);

        // Trigger lazy loading of validators
        Map<String, JsonValidator> validators = schema.getValidators();

        assertTrue(schema.hasRequiredValidator());
        assertSame(mockRequiredValidator, schema.getRequiredValidator());

        assertTrue(schema.hasTypeValidator());
        assertSame(mockTypeValidator, schema.getTypeValidator());
    }

    @Test
    public void testCustomMessageSupport() throws Exception {
        // Enable custom messages in config
        when(config.isCustomMessageSupported()).thenReturn(true);

        // Schema with a custom message for "type"
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.putObject("message").put("type", "my custom type message");
        schemaNode.put("type", "string");

        JsonSchema schema = buildSchema("#", null, schemaNode);

        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), eq(schema), eq("my custom type message")))
                .thenReturn(mockTypeValidator);

        // Force validator creation
        schema.getValidators();

        // Verify that the validator was created with the custom message
        verify(validationContext).newValidator(anyString(), eq("type"), any(JsonNode.class), eq(schema), eq("my custom type message"));
    }

    /* -------------------------------------------------------------
     *  Utility methods
     * ------------------------------------------------------------- */

    @Test
    public void testToStringFormatting() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = buildSchema("#/properties/name", null, node);
        String expected = "\"#/properties/name\" : " + node.toString();
        assertEquals(expected, schema.toString());
    }

    @Test
    public void testInitializeValidatorsCallsPreloadOnce() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = buildSchema("#", null, node);

        // stub a validator that will be returned by the context
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), eq(schema), anyString()))
                .thenReturn(mockValidator);

        // First call – should preload
        schema.initializeValidators();
        verify(mockValidator, times(1)).preloadJsonSchema();

        // Second call – should NOT preload again
        schema.initializeValidators();
        verify(mockValidator, times(1)).preloadJsonSchema(); // still 1
    }

    @Test
    public void testCreateExecutionContext_UsesSupplierWhenPresent() {
        // Prepare a supplier that returns a pre‑configured ExecutionContext
        ExecutionContext supplied = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);
        JsonSchema schema = buildSchema("#", null, MAPPER.createObjectNode());

        ExecutionContext ctx = schema.createExecutionContext();
        assertSame(supplied, ctx);
    }

    @Test
    public void testCreateExecutionContext_DefaultBehaviour() {
        JsonSchema schema = buildSchema("#", null, MAPPER.createObjectNode());

        ExecutionContext ctx = schema.createExecutionContext();

        // Default supplier is null → a new ExecutionContext should be created
        assertNotNull(ctx);
        // The collector context inside should respect the config flags
        CollectorContext cc = ctx.getCollectorContext();
        assertNotNull(cc);
    }

    /* -------------------------------------------------------------
     *  Validation / Walk entry points – delegate to validators
     * ------------------------------------------------------------- */

    @Test
    public void testValidateDelegatesToValidatorsAndCollectsErrors() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = buildSchema("#", null, schemaNode);

        // mock a validator that returns a single ValidationMessage
        ValidationMessage msg = ValidationMessage.of("type", ValidationMessage.Type.ERROR, "type error");
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), eq(schema), anyString()))
                .thenReturn(mockValidator);
        when(mockValidator.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(msg));

        // Mock collector context behaviour
        when(collectorContext.enterDynamicScope(schema)).thenReturn(mock(CollectorContext.Scope.class));
        when(collectorContext.exitDynamicScope()).thenReturn(mock(CollectorContext.Scope.class));

        JsonNode data = MAPPER.readTree("\"not-a-string\"");
        Set<ValidationMessage> errors = schema.validate(data);

        assertEquals(1, errors.size());
        assertTrue(errors.contains(msg));
    }

    @Test
    public void testWalkInvokesKeywordWalkListenersAndJsonWalkers() throws Exception {
        // Build a schema that contains a simple "type" keyword
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        JsonSchema schema = buildSchema("#", null, schemaNode);

        // Mock validator creation – we need a JsonSchemaWalker for the walk path
        JsonSchemaWalker mockWalker = mock(JsonSchemaWalker.class);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), eq(schema), anyString()))
                .thenReturn(mockValidator);
        when(mockValidator.getKeyword()).thenReturn("type");
        when(mockValidator instanceof JsonSchemaWalker ? (JsonSchemaWalker) mockValidator : null).thenReturn(mockWalker);
        when(mockWalker.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // Mock walk listeners runner – always allow the walk to proceed
        WalkListenerRunner runner = mock(DefaultKeywordWalkListenerRunner.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(JsonNode.class), any(), any(ValidationContext.class), any()))
                .thenReturn(true);
        schema.keywordWalkListenerRunner = runner;

        JsonNode data = MAPPER.readTree("\"some string\"");
        ValidationResult result = schema.walk(data, false);
        // No errors expected
        assertTrue(result.getMessages().isEmpty());

        // Verify that the pre‑walk listener was called
        verify(runner).runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(JsonNode.class), any(), any(ValidationContext.class), any());
    }

    /* -------------------------------------------------------------
     *  Internal helper tests (private methods accessed via reflection)
     * ------------------------------------------------------------- */

    @Test
    public void testActiveDialectReturnsMaxWhenAbsent() throws Exception {
        JsonSchema schema = buildSchema("#", null, MAPPER.createObjectNode());
        // No active dialect set in the mock -> should return Long.MAX_VALUE
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("activeDialect");
        m.setAccessible(true);
        long value = (Long) m.invoke(schema);
        assertEquals(Long.MAX_VALUE, value);
    }
}
```
###Test END##