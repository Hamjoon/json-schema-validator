/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written from the perspective of three independent “experts”.
 * Each expert contributes a set of test cases that together cover:
 *
 * 1. Normal/typical usage of public API.
 * 2. Edge‑case handling (nulls, fragments, missing nodes, etc.).
 * 3. Error‑scenario validation (invalid IDs, recursive anchors, etc.).
 *
 * Mockito is used to isolate {@link JsonSchema} from the heavy
 * {@link com.networknt.schema.ValidationContext}, {@link JsonValidator},
 * {@link JsonMetaSchema} and configuration hierarchy.
 *
 * The suite deliberately accesses a few private helpers via reflection – this
 * mirrors the “expert” approach of probing internal behaviour without breaking
 * encapsulation for production code.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link JsonSchema}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    /** Common Jackson mapper used to build JSON nodes. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Mocked collaborators. */
    @Mock private ValidationContext validationContext;
    @Mock private JsonMetaSchema metaSchema;
    @Mock private SchemaValidatorsConfig config;
    @Mock private JsonValidator mockValidator;
    @Mock private JsonValidator trueValidator;
    @Mock private JsonValidator falseValidator;

    /** A minimal JsonSchema instance created for each test. */
    private JsonSchema jsonSchema;

    /** Helper method to build a JsonSchema from a given JsonNode. */
    private JsonSchema buildSchema(JsonNode schemaNode, URI currentUri) {
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        // The validator factory is asked for a validator for each keyword; we return a harmless mock.
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);
        // For boolean schemas the factory is called twice – once for "true"/"false".
        when(validationContext.newValidator(anyString(), eq("true"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(trueValidator);
        when(validationContext.newValidator(anyString(), eq("false"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(falseValidator);

        // New validators do not produce validation messages.
        when(mockValidator.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
        when(trueValidator.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
        when(falseValidator.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());

        // Factory method used by the production code.
        return JsonSchema.from(validationContext, "#", currentUri, schemaNode, null, false);
    }

    /** --------------------------------------------------------------------- *
     *  BASIC CONSTRUCTION & URI HANDLING
     *  --------------------------------------------------------------------- */

    @Test
    public void combineCurrentUriWithIds_nullId_returnsOriginalUri() throws Exception {
        // GIVEN
        JsonNode schema = MAPPER.createObjectNode(); // no "id"
        JsonSchema schemaObj = buildSchema(schema, new URI("http://example.com/base.json"));

        // WHEN – invoke private method via reflection
        Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        m.setAccessible(true);
        URI result = (URI) m.invoke(schemaObj, new URI("http://example.com/base.json"), schema);

        // THEN
        assertEquals("URI must be unchanged when schema has no id", new URI("http://example.com/base.json"), result);
    }

    @Test
    public void combineCurrentUriWithIds_fragmentWithoutBase_returnsNull() throws Exception {
        JsonNode schema = MAPPER.createObjectNode().put("$id", "#/defs/foo");
        JsonSchema schemaObj = buildSchema(schema, null);

        Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        m.setAccessible(true);
        URI result = (URI) m.invoke(schemaObj, null, schema);

        assertNull("Fragment id with no base URI should yield null", result);
    }

    @Test
    public void combineCurrentUriWithIds_absoluteId_resolvesCorrectly() throws Exception {
        JsonNode schema = MAPPER.createObjectNode().put("$id", "http://example.com/schemas/foo.json");
        JsonSchema schemaObj = buildSchema(schema, new URI("http://example.com/base.json"));

        Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        m.setAccessible(true);
        URI result = (URI) m.invoke(schemaObj, new URI("http://example.com/base.json"), schema);

        assertEquals("http://example.com/schemas/foo.json", result.toString());
    }

    @Test
    public void isUriFragmentWithNoContext_trueWhenFragmentAndNullBase() throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("isUriFragmentWithNoContext", URI.class, String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, null, "#/defs/bar");
        assertTrue(result);
    }

    @Test
    public void isUriFragmentWithNoContext_falseWhenBasePresent() throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("isUriFragmentWithNoContext", URI.class, String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, new URI("http://example.com/base.json"), "#/defs/bar");
        assertFalse(result);
    }

    @Test
    public void uriRefersToSubschema_trueWhenFragmentAndRootPath() throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);
        URI uri = new URI("http://example.com/schema.json#sub");
        boolean result = (boolean) m.invoke(null, uri, "#");
        assertTrue(result);
    }

    @Test
    public void uriRefersToSubschema_falseWhenNoFragment() throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);
        URI uri = new URI("http://example.com/schema.json");
        boolean result = (boolean) m.invoke(null, uri, "#");
        assertFalse(result);
    }

    /** --------------------------------------------------------------------- *
     *  getRefSchemaNode – JSON‑Pointer & Fragment handling
     *  --------------------------------------------------------------------- */

    @Test
    public void getRefSchemaNode_jsonPointerResolvesNode() {
        // Build a simple schema with a nested definition.
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode defs = root.putObject("defs");
        ObjectNode foo = defs.putObject("foo");
        foo.put("type", "string"); // leaf node

        JsonSchema schema = buildSchema(root, new URI("http://example.com/schema.json"));

        JsonNode result = schema.getRefSchemaNode("#/defs/foo");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void getRefSchemaNode_missingPointer_returnsNull() {
        ObjectNode root = MAPPER.createObjectNode();
        JsonSchema schema = buildSchema(root, new URI("http://example.com/schema.json"));

        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertTrue("Missing pointer should give a MissingNode", result.isMissingNode());
    }

    @Test
    public void getRefSchemaNode_fragmentDelegatesToMetaSchema() {
        // Simulate a $ref that points to an external fragment handled by metaSchema.
        ObjectNode root = MAPPER.createObjectNode();
        JsonSchema schema = buildSchema(root, new URI("http://example.com/schema.json"));

        // Mock metaSchema#getNodeByFragmentRef to return a dummy node.
        JsonNode dummy = MAPPER.createObjectNode().put("type", "integer");
        when(metaSchema.getNodeByFragmentRef(eq("#/components/schemas/Int"), any(JsonNode.class)))
                .thenReturn(dummy);

        JsonNode result = schema.getRefSchemaNode("#/components/schemas/Int");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    /** --------------------------------------------------------------------- *
     *  Lexical root & ancestor navigation
     *  --------------------------------------------------------------------- */

    @Test
    public void findLexicalRoot_returnsSelfWhenHasIdTrue() {
        // schema with "$id" should set hasId = true.
        ObjectNode root = MAPPER.createObjectNode().put("$id", "http://example.com/root.json");
        JsonSchema schema = buildSchema(root, new URI("http://example.com/root.json"));

        JsonSchema lexicalRoot = schema.findLexicalRoot();
        assertSame("Lexical root should be self because hasId == true", schema, lexicalRoot);
    }

    @Test
    public void findLexicalRoot_walksUpToAncestorWithId() {
        // child schema (no id) -> parent schema (has id)
        ObjectNode parentNode = MAPPER.createObjectNode().put("$id", "http://example.com/parent.json");
        JsonSchema parent = buildSchema(parentNode, new URI("http://example.com/parent.json"));

        ObjectNode childNode = MAPPER.createObjectNode(); // no id
        // Use the factory method that creates a child schema.
        JsonSchema child = parent.createChildSchema("#/child", childNode);

        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame("Lexical root should be the parent that owns an id", parent, lexicalRoot);
    }

    @Test
    public void findAncestor_returnsTopMostParent() {
        // Build three levels.
        JsonSchema top = buildSchema(MAPPER.createObjectNode(), new URI("http://example.com/top.json"));
        JsonSchema mid = top.createChildSchema("#/mid", MAPPER.createObjectNode());
        JsonSchema leaf = mid.createChildSchema("#/leaf", MAPPER.createObjectNode());

        assertSame("Ancestor of leaf should be top", top, leaf.findAncestor());
    }

    /** --------------------------------------------------------------------- *
     *  Required / Type validator detection
     *  --------------------------------------------------------------------- */

    @Test
    public void hasRequiredValidator_trueWhenSchemaContainsRequired() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.putArray("required").add("name");
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/req.json"));

        assertTrue("hasRequiredValidator should be true", schema.hasRequiredValidator());
        assertNotNull("getRequiredValidator should return non‑null", schema.getRequiredValidator());
    }

    @Test
    public void hasTypeValidator_trueWhenSchemaContainsType() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "object");
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/type.json"));

        assertTrue("hasTypeValidator should be true", schema.hasTypeValidator());
        assertNotNull("getTypeValidator should return non‑null", schema.getTypeValidator());
    }

    /** --------------------------------------------------------------------- *
     *  initializeValidators – idempotent loading
     *  --------------------------------------------------------------------- */

    @Test
    public void initializeValidators_callsPreloadOnlyOnce() throws Exception {
        // The validator factory will supply a mock validator for each keyword.
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "string");
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/init.json"));

        // First call – should trigger preload on the single validator we created.
        schema.initializeValidators();

        // Capture the validator that was stored inside the schema.
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        Map<String, JsonValidator> validators = (Map<String, JsonValidator>) validatorsField.get(schema);
        JsonValidator validator = validators.values().iterator().next();

        // Verify preload was invoked exactly once.
        verify(validator, times(1)).preloadJsonSchema();

        // Second call – no additional preload invocations.
        schema.initializeValidators();
        verify(validator, times(1)).preloadJsonSchema();
    }

    /** --------------------------------------------------------------------- *
     *  ExecutionContext creation
     *  --------------------------------------------------------------------- */

    @Test
    public void createExecutionContext_usesSupplierIfProvided() {
        // Prepare a custom supplier that returns a pre‑configured ExecutionContext.
        CollectorContext customCollector = new CollectorContext(false, false);
        ExecutionContext customEC = new ExecutionContext(customCollector);
        Supplier<ExecutionContext> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(customEC);
        when(config.getExecutionContextSupplier()).thenReturn(supplier);

        JsonSchema schema = buildSchema(MAPPER.createObjectNode(), new URI("http://example.com/ec.json"));
        ExecutionContext ec = schema.createExecutionContext();

        assertSame("ExecutionContext should be the one supplied by the config", customEC, ec);
        verify(supplier, times(1)).get();
    }

    @Test
    public void createExecutionContext_createsDefaultWhenNoSupplier() {
        when(config.getExecutionContextSupplier()).thenReturn(null);
        JsonSchema schema = buildSchema(MAPPER.createObjectNode(), new URI("http://example.com/ec2.json"));
        ExecutionContext ec = schema.createExecutionContext();

        assertNotNull("ExecutionContext should never be null", ec);
        assertNotNull("CollectorContext inside ExecutionContext must be non‑null", ec.getCollectorContext());
    }

    /** --------------------------------------------------------------------- *
     *  Validation – happy path (no errors) and collector interaction
     *  --------------------------------------------------------------------- */

    @Test
    public void validate_returnsEmptySetWhenAllValidatorsPass() {
        // Simple schema – validators are mocked to return empty sets.
        ObjectNode schemaNode = MAPPER.createObjectNode().put("type", "string");
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/valid.json"));

        JsonNode instance = new TextNode("hello");
        Set<ValidationMessage> messages = schema.validate(instance);

        assertTrue("No validation errors expected", messages.isEmpty());
    }

    @Test
    public void validate_collectsErrorsFromFailingValidator() {
        // Make one validator return a message.
        JsonValidator failingValidator = mock(JsonValidator.class);
        ValidationMessage msg = ValidationMessage.of("type", ValidatorTypeCode.TYPE, null);
        when(failingValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(msg));

        // Configure factory to return the failing validator for the "type" keyword.
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(failingValidator);

        ObjectNode schemaNode = MAPPER.createObjectNode().put("type", "integer");
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/fail.json"));

        JsonNode instance = new TextNode("not-an-integer");
        Set<ValidationMessage> result = schema.validate(instance);

        assertEquals("Exactly one error should be reported", 1, result.size());
        assertTrue("Error set must contain the mocked message", result.contains(msg));
    }

    /** --------------------------------------------------------------------- *
     *  Walk – ensure listener runner is invoked and that walk delegates to
     *  JsonValidator walkers.
     *  --------------------------------------------------------------------- */

    @Test
    public void walk_callsKeywordWalkListenersAndValidatorWalkers() throws Exception {
        // Mock a JsonValidator that also implements JsonSchemaWalker.
        JsonValidator walkerValidator = mock(JsonValidator.class, withSettings().extraInterfaces(JsonSchemaWalker.class));
        when(walkerValidator.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());

        // Listener runner – we verify that pre‑walk returns true (allow walk) and that post‑walk is called.
        WalkListenerRunner mockRunner = mock(WalkListenerRunner.class);
        when(mockRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(JsonNode.class), any(), any(), any()))
                .thenReturn(true);
        // post‑walk returns void – just verify invocation later.

        // Inject the mock runner via reflection.
        JsonSchema schema = buildSchema(MAPPER.createObjectNode(), new URI("http://example.com/walk.json"));
        Field runnerField = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        runnerField.setAccessible(true);
        runnerField.set(schema, mockRunner);

        // Replace the validator map with a single walker validator.
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        Map<String, JsonValidator> map = new TreeMap<>();
        map.put("#/walker", walkerValidator);
        validatorsField.set(schema, Collections.unmodifiableMap(map));

        JsonNode instance = MAPPER.createObjectNode();
        ValidationResult result = schema.walk(instance, false);

        // Verify that pre‑walk and post‑walk were each called once.
        verify(mockRunner, times(1)).runPreWalkListeners(any(), eq("#/walker"), any(), any(),
                anyString(), anyString(), any(JsonNode.class), any(), any(), any());
        verify(mockRunner, times(1)).runPostWalkListeners(any(), eq("#/walker"), any(), any(),
                anyString(), anyString(), any(JsonNode.class), any(), any(),
                any(JsonSchemaFactory.class), anySet());

        // The walker validator should have been invoked.
        verify((JsonSchemaWalker) walkerValidator, times(1))
                .walk(any(), any(), any(), anyString(), anyBoolean());

        // No validation errors expected.
        assertTrue(result.getMessages().isEmpty());
    }

    /** --------------------------------------------------------------------- *
     *  toString – human readable representation
     *  --------------------------------------------------------------------- */

    @Test
    public void toString_includesPathAndNode() {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("type", "null");
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/str.json"));
        String s = schema.toString();

        assertTrue("String should contain the schema path", s.contains("\"#\""));
        assertTrue("String should contain the raw node", s.contains("\"type\":\"null\""));
    }

    /** --------------------------------------------------------------------- *
     *  Dynamic Anchor handling – $recursiveAnchor keyword
     *  --------------------------------------------------------------------- */

    @Test
    public void read_setsDynamicAnchorWhenRecursiveAnchorIsTrue() throws Exception {
        // Build a schema with $recursiveAnchor : true.
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$recursiveAnchor", true);
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/anchor.json"));

        // Force the private read() method to execute (it runs lazily on first getValidators()).
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        readMethod.invoke(schema, schemaNode);

        // Verify the flag.
        assertTrue("Dynamic anchor flag should be true after processing $recursiveAnchor", schema.isDynamicAnchor());
    }

    /** --------------------------------------------------------------------- *
     *  Private helper – activeDialect()
     *  --------------------------------------------------------------------- */

    @Test
    public void activeDialect_returnsConfiguredVersionOrMax() throws Exception {
        // ValidationContext#activeDialect() will be stubbed to return empty (=> MAX_VALUE).
        when(validationContext.activeDialect()).thenReturn(Optional.empty());

        JsonSchema schema = buildSchema(MAPPER.createObjectNode(), new URI("http://example.com/dialect.json"));
        Method m = JsonSchema.class.getDeclaredMethod("activeDialect");
        m.setAccessible(true);
        long result = (long) m.invoke(schema);
        assertEquals(Long.MAX_VALUE, result);
    }

    /** --------------------------------------------------------------------- *
     *  Custom message handling (positive path)
     *  --------------------------------------------------------------------- */

    @Test
    public void getCustomMessage_returnsMessageWhenSupported() {
        // Enable custom messages.
        when(config.isCustomMessageSupported()).thenReturn(true);
        // Build a schema with a "message" child.
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ObjectNode msgNode = schemaNode.putObject("message");
        msgNode.put("type", "my custom message");

        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/msg.json"));
        // Use reflection to invoke the private method.
        Method m = JsonSchema.class.getDeclaredMethod("getCustomMessage", JsonNode.class, String.class);
        m.setAccessible(true);
        String custom = (String) m.invoke(schema, schemaNode, "type");

        assertEquals("my custom message", custom);
    }

    /** --------------------------------------------------------------------- *
     *  Edge case – invalid $recursiveAnchor (non‑boolean) throws JsonSchemaException
     *  --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void read_throwsWhenRecursiveAnchorNotBoolean() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$recursiveAnchor", "notABoolean");
        JsonSchema schema = buildSchema(schemaNode, new URI("http://example.com/invalidAnchor.json"));
        // Directly invoke the private read method which contains the validation.
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        readMethod.invoke(schema, schemaNode);
    }

    /** --------------------------------------------------------------------- *
     *  Helper for building ExecutionContext without a supplier.
     *  --------------------------------------------------------------------- */

    @Test
    public void createExecutionContext_withoutSupplierCreatesCollectorWithConfigFlags() {
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(true);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        when(config.getExecutionContextSupplier()).thenReturn(null);

        JsonSchema schema = buildSchema(MAPPER.createObjectNode(), new URI("http://example.com/collectorFlags.json"));
        ExecutionContext ec = schema.createExecutionContext();

        CollectorContext cc = ec.getCollectorContext();
        assertTrue("Unevaluated items analysis should be disabled per config", cc.isUnevaluatedItemsAnalysisDisabled());
        assertFalse("Unevaluated properties analysis should be enabled per config", cc.isUnevaluatedPropertiesAnalysisDisabled());
    }
}
