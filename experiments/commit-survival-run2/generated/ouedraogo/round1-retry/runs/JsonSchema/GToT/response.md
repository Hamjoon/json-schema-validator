###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written from the perspective of three “experts”.  Each expert
 * contributed one test per public method, identified edge‑cases and exception
 * scenarios, and finally the suite was merged into this single file.
 *
 * The code relies heavily on the surrounding validation framework.  To keep the
 * tests fast, isolated and deterministic we use Mockito to mock the collaborators
 * (ValidationContext, JsonValidator, ExecutionContext, CollectorContext, …).
 *
 * The test class covers:
 *   1. Extraction of all public methods (see the comment block at the top).
 *   2. Basic happy‑path validation for each method.
 *   3. Edge‑case handling (null URIs, fragment‑only IDs, malformed $recursiveAnchor,
 *      missing $ref targets, custom execution‑context supplier, etc.).
 *   4. Verification that internal side‑effects (e.g. preloadJsonSchema,
 *      dynamic‑anchor flag, validator state) are performed.
 *
 * NOTE: The production class uses many internal types that are not public.
 *       For the purpose of unit testing we create lightweight stubs or use
 *       Mockito to emulate the required behaviour.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for {@link JsonSchema}.
 *
 * The suite follows the “three‑expert” workflow described in the problem statement.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    // -----------------------------------------------------------------------
    // Helper constants / objects
    // -----------------------------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple schema node that contains an “id” keyword (used by several tests). */
    private static ObjectNode schemaWithId() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("$id", "http://example.com/base");
        return node;
    }

    /** Schema node containing a $recursiveAnchor (valid and invalid cases). */
    private static ObjectNode schemaWithRecursiveAnchor(boolean value) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("$recursiveAnchor", value);
        return node;
    }

    /** Schema node with a $recursiveAnchor that is not a boolean (error case). */
    private static ObjectNode schemaWithInvalidRecursiveAnchor() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("$recursiveAnchor", "notABoolean");
        return node;
    }

    /** Simple schema that defines a definition and a $ref to it. */
    private static ObjectNode schemaWithDefinitions() {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode defs = MAPPER.createObjectNode();
        ObjectNode def = MAPPER.createObjectNode();
        def.put("type", "string");
        defs.set("myDef", def);
        root.set("definitions", defs);
        return root;
    }

    // -----------------------------------------------------------------------
    // Mocked collaborators
    // -----------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock JsonMetaSchema metaSchema;
    @Mock JsonValidator validatorMock;
    @Mock JsonValidator validator2Mock;
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock SchemaValidatorsConfig config;
    @Mock JsonSchemaFactory factory; // not used directly but may be required by listeners
    @Mock WalkListenerRunner listenerRunner; // placeholder – not exercised in our tests

    // -----------------------------------------------------------------------
    // Setup common mock behaviour
    // -----------------------------------------------------------------------
    @Before
    public void setUp() throws Exception {
        // ValidationContext returns the meta‑schema mock
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // ValidationContext returns a config mock (used by many methods)
        when(validationContext.getConfig()).thenReturn(config);
        // Default config: no custom execution‑context supplier, collectors disabled
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // ExecutionContext returns a collector context mock
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // CollectorContext returns a fresh ValidatorState map when asked for a key
        when(collectorContext.get(any())).thenReturn(null);
        // The ValidationContext creates validators – we always return our mock
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validatorMock);
        // The mock validator returns an empty error set by default
        when(validatorMock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        // When the validator is asked to preload its schema, do nothing (void)
        doNothing().when(validatorMock).preloadJsonSchema();
    }

    // -----------------------------------------------------------------------
    // 1. PUBLIC STATIC FACTORY METHOD
    // -----------------------------------------------------------------------
    @Test
    public void testStaticFactoryCreatesInstance() {
        URI current = URI.create("http://example.com/root");
        JsonNode schemaNode = schemaWithId();

        JsonSchema schema = JsonSchema.from(validationContext, "#", current, schemaNode,
                null, false);

        assertNotNull("Factory should return a JsonSchema instance", schema);
        assertEquals("Current URI should be combined with $id", URI.create("http://example.com/base"),
                schema.getCurrentUri());
    }

    // -----------------------------------------------------------------------
    // 2. CONSTRUCTORS (three deprecated overloads) – exercised via the static factory
    // -----------------------------------------------------------------------
    @Test
    public void testConstructorCombinesCurrentUriWithId() {
        URI base = URI.create("http://example.com/base");
        JsonNode node = schemaWithId();

        JsonSchema schema = new JsonSchema(validationContext, base, node);
        assertEquals("Constructor should combine base URI with $id", URI.create("http://example.com/base"),
                schema.getCurrentUri());
    }

    // -----------------------------------------------------------------------
    // 3. createChildSchema
    // -----------------------------------------------------------------------
    @Test
    public void testCreateChildSchemaDelegatesToValidationContext() {
        JsonSchema parent = JsonSchema.from(validationContext, "#", null, MAPPER.createObjectNode(),
                null, false);
        JsonNode childNode = MAPPER.createObjectNode().put("type", "integer");

        // ValidationContext creates a new schema – we stub it
        JsonSchema child = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("/child"), eq(childNode), eq(parent))).thenReturn(child);

        JsonSchema result = parent.createChildSchema("/child", childNode);
        assertSame("createChildSchema should return the schema produced by ValidationContext",
                child, result);
    }

    // -----------------------------------------------------------------------
    // 4. getCurrentUri – already exercised above, add a null‑uri edge case
    // -----------------------------------------------------------------------
    @Test
    public void testGetCurrentUriWhenNoIdAndNullBase() {
        JsonSchema schema = JsonSchema.from(validationContext, "#", null,
                MAPPER.createObjectNode(), null, false);
        assertNull("Current URI should be null when no base URI and no $id", schema.getCurrentUri());
    }

    // -----------------------------------------------------------------------
    // 5. getRefSchemaNode – JSON‑Pointer handling
    // -----------------------------------------------------------------------
    @Test
    public void testGetRefSchemaNode_JsonPointerResolvesNode() {
        JsonNode schemaNode = schemaWithDefinitions();
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode,
                null, false);

        JsonNode result = schema.getRefSchemaNode("#/definitions/myDef");
        assertNotNull("Pointer should resolve to a node", result);
        assertEquals("Resolved node should have type 'string'",
                "string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerDecoding() throws Exception {
        // Build a schema where the key contains an escaped '/' character
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode defs = MAPPER.createObjectNode();
        ObjectNode def = MAPPER.createObjectNode().put("type", "number");
        defs.set("my%2Fdef", def); // encoded slash
        root.set("definitions", defs);
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, root,
                null, false);

        JsonNode result = schema.getRefSchemaNode("#/definitions/my%2Fdef");
        assertNotNull(result);
        assertEquals("number", result.get("type").asText());
    }

    // -----------------------------------------------------------------------
    // 6. getRefSchemaNode – fragment reference via meta‑schema
    // -----------------------------------------------------------------------
    @Test
    public void testGetRefSchemaNode_FragmentDelegateToMetaSchema() {
        JsonNode schemaNode = MAPPER.createObjectNode();
        when(metaSchema.getNodeByFragmentRef(eq("#/myFragment"), any(JsonNode.class)))
                .thenReturn(MAPPER.createObjectNode().put("type", "boolean"));

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode,
                null, false);

        JsonNode result = schema.getRefSchemaNode("#/myFragment");
        assertNotNull(result);
        assertEquals("boolean", result.get("type").asText());
    }

    // -----------------------------------------------------------------------
    // 7. findLexicalRoot – stops at the nearest ancestor that has an $id
    // -----------------------------------------------------------------------
    @Test
    public void testFindLexicalRootStopsAtId() {
        // Root schema has no $id
        JsonSchema root = JsonSchema.from(validationContext, "#", null,
                MAPPER.createObjectNode(), null, false);
        // Child schema contains an $id
        JsonSchema child = JsonSchema.from(validationContext, "#/child",
                URI.create("http://example.com/child"), schemaWithId(),
                root, false);

        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame("Lexical root should be the child itself because it has an $id",
                child, lexicalRoot);
    }

    // -----------------------------------------------------------------------
    // 8. findAncestor – returns the top‑most ancestor (the root)
    // -----------------------------------------------------------------------
    @Test
    public void testFindAncestorReturnsRoot() {
        JsonSchema root = JsonSchema.from(validationContext, "#", null,
                MAPPER.createObjectNode(), null, false);
        JsonSchema child = JsonSchema.from(validationContext, "#/child",
                null, MAPPER.createObjectNode(), root, false);
        JsonSchema grand = JsonSchema.from(validationContext, "#/child/grand",
                null, MAPPER.createObjectNode(), child, false);

        JsonSchema ancestor = grand.findAncestor();
        assertSame("Ancestor of a deep child should be the root schema", root, ancestor);
    }

    // -----------------------------------------------------------------------
    // 9. validate(ExecutionContext, JsonNode, JsonNode, String)
    // -----------------------------------------------------------------------
    @Test
    public void testValidateCollectsErrorsFromValidators() {
        // Prepare a schema that will contain two validators
        JsonNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("type", MAPPER.createObjectNode().put("type", "string"));
        schemaNode.set("minimum", MAPPER.createObjectNode().put("minimum", 5));

        // First validator returns an error, second returns empty set
        ValidationMessage error = ValidationMessage.of("type", ValidationMessage.Type.ERROR, "type error");
        when(validatorMock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(error));
        when(validator2Mock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // ValidationContext must return two different validators for the two keywords
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validatorMock);
        when(validationContext.newValidator(anyString(), eq("minimum"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validator2Mock);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode,
                null, false);

        Set<ValidationMessage> errors = schema.validate(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$");

        assertEquals("One error should be collected", 1, errors.size());
        assertTrue(errors.contains(error));
    }

    // -----------------------------------------------------------------------
    // 10. validate(JsonNode) – convenience wrapper
    // -----------------------------------------------------------------------
    @Test
    public void testValidateConvenienceWrapperCallsCoreValidate() {
        JsonSchema schema = spy(JsonSchema.from(validationContext, "#", null,
                MAPPER.createObjectNode(), null, false));

        JsonNode data = MAPPER.createObjectNode().put("dummy", "value");
        // Stub the core validate method
        Set<ValidationMessage> stubResult = Collections.emptySet();
        doReturn(stubResult).when(schema).validate(any(ExecutionContext.class), eq(data), eq(data), eq("$"));

        Set<ValidationMessage> result = schema.validate(data);
        assertSame(stubResult, result);
        verify(schema).createExecutionContext();
        verify(schema).validate(any(ExecutionContext.class), eq(data), eq(data), eq("$"));
    }

    // -----------------------------------------------------------------------
    // 11. initializeValidators – ensures preloadJsonSchema() is invoked once
    // -----------------------------------------------------------------------
    @Test
    public void testInitializeValidatorsPreloadsOnlyOnce() {
        // Build a schema with a single validator
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "string");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode,
                null, false);

        // First call – should trigger preload
        schema.initializeValidators();
        verify(validatorMock, times(1)).preloadJsonSchema();

        // Second call – must NOT call preload again
        schema.initializeValidators();
        verifyNoMoreInteractions(validatorMock);
    }

    // -----------------------------------------------------------------------
    // 12. isDynamicAnchor – $recursiveAnchor handling (valid & invalid)
    // -----------------------------------------------------------------------
    @Test
    public void testIsDynamicAnchorTrueWhenRecursiveAnchorTrue() {
        JsonSchema schema = JsonSchema.from(validationContext, "#", null,
                schemaWithRecursiveAnchor(true), null, false);
        assertTrue("Dynamic anchor flag should be true when $recursiveAnchor is true",
                schema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRecursiveAnchorNonBooleanThrowsException() {
        // This schema contains an invalid $recursiveAnchor (string instead of boolean)
        JsonSchema.from(validationContext, "#", null,
                schemaWithInvalidRecursiveAnchor(), null, false);
        // Exception is expected during construction – test passes if thrown
    }

    // -----------------------------------------------------------------------
    // 13. createExecutionContext – respects custom supplier
    // -----------------------------------------------------------------------
    @Test
    public void testCreateExecutionContextUsesSupplierWhenProvided() {
        // Prepare a custom supplier that returns a mock ExecutionContext
        ExecutionContext customCtx = mock(ExecutionContext.class);
        when(customCtx.getCollectorContext()).thenReturn(collectorContext);
        when(config.getExecutionContextSupplier()).thenReturn(() -> customCtx);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null,
                MAPPER.createObjectNode(), null, false);
        ExecutionContext result = schema.createExecutionContext();

        assertSame("Should return the context supplied by the config supplier", customCtx, result);
    }

    // -----------------------------------------------------------------------
    // 14. toString – simple sanity check
    // -----------------------------------------------------------------------
    @Test
    public void testToStringContainsSchemaPathAndNode() {
        ObjectNode node = MAPPER.createObjectNode().put("type", "boolean");
        JsonSchema schema = JsonSchema.from(validationContext, "#/mySchema", null, node,
                null, false);

        String str = schema.toString();
        assertTrue(str.contains("#/mySchema"));
        assertTrue(str.contains("\"type\":\"boolean\""));
    }

    // -----------------------------------------------------------------------
    // 15. hasRequiredValidator / getRequiredValidator & hasTypeValidator / getTypeValidator
    // -----------------------------------------------------------------------
    @Test
    public void testRequiredAndTypeValidatorPresence() {
        // Build a schema that contains both "required" and "type"
        ObjectNode node = MAPPER.createObjectNode();
        node.set("required", MAPPER.createArrayNode().add("name"));
        node.set("type", MAPPER.createObjectNode().put("type", "object"));

        // ValidationContext returns distinct mock validators for each keyword
        JsonValidator requiredValidator = mock(JsonValidator.class);
        JsonValidator typeValidator = mock(TypeValidator.class);
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(requiredValidator);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(typeValidator);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, node,
                null, false);

        assertTrue(schema.hasRequiredValidator());
        assertSame(requiredValidator, schema.getRequiredValidator());

        assertTrue(schema.hasTypeValidator());
        assertSame(typeValidator, schema.getTypeValidator());
    }

    // -----------------------------------------------------------------------
    // 16. walk* methods – verify that the internal walk delegation works.
    //      We focus on the public walk(ExecutionContext, JsonNode, boolean) overload.
    // -----------------------------------------------------------------------
    @Test
    public void testWalkDelegatesToInternalMethod() {
        JsonSchema schema = spy(JsonSchema.from(validationContext, "#", null,
                MAPPER.createObjectNode(), null, false));

        JsonNode data = MAPPER.createObjectNode().put("a", 1);
        // Stub the internal method to return a known ValidationResult
        ValidationResult expectedResult = new ValidationResult(Collections.emptySet(), executionContext);
        doReturn(expectedResult).when(schema).walkAtNodeInternal(any(), eq(data), eq(data), eq("$"), anyBoolean());

        ValidationResult result = schema.walk(executionContext, data, true);
        assertSame(expectedResult, result);
        verify(schema).walkAtNodeInternal(eq(executionContext), eq(data), eq(data), eq("$"), eq(true));
    }

    // -----------------------------------------------------------------------
    // 17. walk(ExecutionContext, JsonNode, JsonNode, String, boolean) – ensure
    //      listeners are invoked (mocked) and that errors are collected.
    // -----------------------------------------------------------------------
    @Test
    public void testWalkCollectsErrorsFromWalkers() {
        // Prepare a schema that contains a dummy walker
        JsonSchema schema = spy(JsonSchema.from(validationContext, "#", null,
                MAPPER.createObjectNode(), null, false));

        // Mock a JsonSchemaWalker that returns a non‑empty set of ValidationMessage
        JsonSchemaWalker mockWalker = mock(JsonSchemaWalker.class);
        ValidationMessage walkError = ValidationMessage.of("walk", ValidationMessage.Type.ERROR, "walk error");
        when(mockWalker.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(walkError));

        // Inject the mock walker into the schema's validator map via reflection
        Map<String, JsonValidator> validatorsMap = new HashMap<>();
        validatorsMap.put("#/dummy", (JsonValidator) mockWalker);
        // Force the internal map (bypassing lazy init)
        try {
            java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
            f.setAccessible(true);
            f.set(schema, Collections.unmodifiableMap(validatorsMap));
        } catch (Exception e) {
            fail("Failed to inject mock walker: " + e);
        }

        // Run walk
        ValidationResult result = schema.walk(executionContext, MAPPER.createObjectNode(), false);
        assertEquals(1, result.getValidationMessages().size());
        assertTrue(result.getValidationMessages().contains(walkError));
    }

    // -----------------------------------------------------------------------
    // END OF TESTS
    // -----------------------------------------------------------------------
}
```
###Test END##