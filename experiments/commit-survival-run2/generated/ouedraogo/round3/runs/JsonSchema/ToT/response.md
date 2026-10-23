###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * <p>
 * The tests are written from the perspective of three “experts”.  Each expert
 * contributes a test that exercises a distinct area of functionality:
 * <ul>
 *   <li>Expert 1 – URI handling and fragment resolution (typical, edge and error cases)</li>
 *   <li>Expert 2 – Validator initialisation, dynamic anchors and ancestor handling</li>
 *   <li>Expert 3 – Execution‑context creation, walk‑related flags and toString()</li>
 * </ul>
 * The three contributions are merged into a single, comprehensive test class.
 * </p>
 *
 * <p>
 * The library under test has many collaborators ( {@link ValidationContext},
 * {@link JsonValidator}, {@link JsonMetaSchema}, etc.).  To keep the tests fast,
 * isolated and deterministic we use Mockito to create lightweight stubs.
 * </p>
 *
 * <p>
 * All tests run with JUnit‑4; the class is annotated with
 * {@code @RunWith(MockitoJUnitRunner.class)} so that {@code @Mock} fields are
 * automatically initialised.
 * </p>
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    // -----------------------------------------------------------------------
    // Common test fixtures (used by all three experts)
    // -----------------------------------------------------------------------
    @Mock
    private ValidationContext mockContext;

    @Mock
    private JsonMetaSchema mockMetaSchema;

    @Mock
    private URIFactory mockUriFactory; // internal helper used by ValidationContext

    @Mock
    private JsonValidator mockValidator;

    @Mock
    private Config mockConfig; // Config is an inner class of ValidationContext – we mock it

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        // -------------------------------------------------------------------
        // Minimal behaviour needed for every test
        // -------------------------------------------------------------------
        when(mockContext.getMetaSchema()).thenReturn(mockMetaSchema);
        when(mockContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isCustomMessageSupported()).thenReturn(false);
        when(mockContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(mockContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(mockValidator);
        when(mockValidator.preloadJsonSchema()).thenReturn(null);
        // The URIFactory simply delegates to the real java.net.URI constructor
        when(mockUriFactory.create(any(URI.class), anyString())).thenAnswer(
                inv -> new URI((URI) inv.getArgument(0), (String) inv.getArgument(1)));
    }

    // -----------------------------------------------------------------------
    // Expert 1 – URI handling & fragment resolution
    // -----------------------------------------------------------------------

    /**
     * Verify that {@code combineCurrentUriWithIds} correctly appends an {@code id}
     * to the current URI and that it returns {@code null} when the {@code id}
     * is a fragment without a base URI.
     */
    @Test
    public void testCombineCurrentUriWithIds_AppendsIdAndHandlesFragmentWithoutBase() throws Exception {
        // ---- set‑up a schema node that contains an "$id" ----
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("$id", "sub/schema.json");

        // ---- build a JsonSchema instance with a base URI ----
        URI base = new URI("http://example.com/root.json");
        JsonSchema schema = JsonSchema.from(mockContext, "#", base, schemaNode, null, false);

        // ---- invoke the private method via reflection ----
        Method combineMethod = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class,
                JsonNode.class);
        combineMethod.setAccessible(true);
        URI combined = (URI) combineMethod.invoke(schema, base, schemaNode);

        assertEquals("http://example.com/sub/schema.json", combined.toString());

        // ---- now test the fragment‑only case (should return null) ----
        schemaNode = mapper.createObjectNode();
        schemaNode.put("$id", "#/definitions/foo");
        JsonSchema schema2 = JsonSchema.from(mockContext, "#", null, schemaNode, null, false);
        URI result = (URI) combineMethod.invoke(schema2, null, schemaNode);
        assertNull(result);
    }

    @Test
    public void testIsUriFragmentWithNoContext() throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("isUriFragmentWithNoContext", URI.class, String.class);
        m.setAccessible(true);

        // fragment with no base URI -> true
        assertTrue((Boolean) m.invoke(null, null, "#/foo"));
        // fragment with a base URI -> false
        assertFalse((Boolean) m.invoke(null, new URI("http://example.com/base.json"), "#/foo"));
        // non‑fragment -> false
        assertFalse((Boolean) m.invoke(null, null, "http://example.com/other"));
    }

    @Test
    public void testUriRefersToSubschema() throws Exception {
        Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);

        URI uriWithFragment = new URI("http://example.com/schema.json#foo");
        // schemaPath empty, fragment present -> true
        assertTrue((Boolean) m.invoke(null, uriWithFragment, "#"));
        // schemaPath already a subschema -> false
        assertFalse((Boolean) m.invoke(null, uriWithFragment, "#/bar"));
        // no fragment -> false
        assertFalse((Boolean) m.invoke(null, new URI("http://example.com/schema.json"), "#"));
    }

    /**
     * Test {@code getRefSchemaNode} with a JSON‑Pointer reference.
     * The method should resolve the pointer, URL‑decode it and return the
     * correct sub‑node.
     */
    @Test
    public void testGetRefSchemaNode_JsonPointerResolution() {
        // ----- build a complex schema node -----
        ObjectNode root = mapper.createObjectNode();
        ObjectNode inner = mapper.createObjectNode();
        inner.put("type", "string");
        inner.put("maxLength", 5);
        root.set("properties", mapper.createObjectNode().set("name", inner));

        // ----- create JsonSchema -----
        JsonSchema schema = JsonSchema.from(mockContext, "#", null, root, null, false);

        // ----- resolve a pointer to the inner node -----
        JsonNode result = schema.getRefSchemaNode("#/properties/name");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
        assertEquals(5, result.get("maxLength").asInt());
    }

    // -----------------------------------------------------------------------
    // Expert 2 – Validators, dynamic anchors, ancestor handling
    // -----------------------------------------------------------------------

    /**
     * Ensure that a validator marked as {@code $recursiveAnchor:true} sets the
     * {@code dynamicAnchor} flag on the schema.
     */
    @Test
    public void testRecursiveAnchorSetsDynamicAnchorFlag() {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("$recursiveAnchor", true);

        JsonSchema schema = JsonSchema.from(mockContext, "#", null, schemaNode, null, false);
        // the private field is accessed via the public getter
        assertTrue(schema.isDynamicAnchor());
    }

    /**
     * Verify that {@code initializeValidators()} calls {@code preloadJsonSchema()}
     * on each validator exactly once, even when invoked repeatedly.
     */
    @Test
    public void testInitializeValidatorsCallsPreloadOnce() {
        // ----- a schema node that yields a single validator (the mock) -----
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "string"); // any keyword will cause newValidator() to be called

        JsonSchema schema = JsonSchema.from(mockContext, "#", null, schemaNode, null, false);

        // first call – should preload
        schema.initializeValidators();
        verify(mockValidator, times(1)).preloadJsonSchema();

        // second call – must NOT preload again
        schema.initializeValidators();
        verifyNoMoreInteractions(mockValidator);
    }

    /**
     * The {@code findAncestor()} method should walk up the parent chain and
     * return the top‑most schema (the root).  We construct a three‑level hierarchy
     * and assert the correct ancestor is returned.
     */
    @Test
    public void testFindAncestorReturnsRootSchema() {
        // leaf schema (no parent)
        JsonSchema leaf = JsonSchema.from(mockContext, "#/leaf", null, mapper.createObjectNode(), null, false);
        // middle schema – parent is leaf
        JsonSchema middle = JsonSchema.from(mockContext, "#/middle", null, mapper.createObjectNode(), leaf, false);
        // root schema – parent is middle
        JsonSchema root = JsonSchema.from(mockContext, "#/root", null, mapper.createObjectNode(), middle, false);

        // leaf.findAncestor() should return root
        assertSame(root, leaf.findAncestor());
        // middle.findAncestor() should also return root
        assertSame(root, middle.findAncestor());
    }

    /**
     * {@code findLexicalRoot()} stops at the first schema that has an {@code id}
     * keyword.  The test creates a chain where the middle schema declares an {@code id}
     * and verifies that the lexical root is the middle one.
     */
    @Test
    public void testFindLexicalRootStopsAtIdKeyword() {
        // schema without $id
        JsonSchema child = JsonSchema.from(mockContext, "#/child", null, mapper.createObjectNode(), null, false);
        // schema with $id
        ObjectNode middleNode = mapper.createObjectNode();
        middleNode.put("$id", "http://example.com/middle");
        JsonSchema middle = JsonSchema.from(mockContext, "#/middle", null, middleNode, child, false);
        // root schema without $id
        JsonSchema root = JsonSchema.from(mockContext, "#/root", null, mapper.createObjectNode(), middle, false);

        // lexical root of child should be middle (the first ancestor that has an id)
        assertSame(middle, child.findLexicalRoot());
    }

    // -----------------------------------------------------------------------
    // Expert 3 – Execution context, walk flags, toString()
    // -----------------------------------------------------------------------

    /**
     * Verify that {@code createExecutionContext()} respects the supplier
     * provided in {@link SchemaValidatorsConfig#getExecutionContextSupplier()}.
     */
    @Test
    public void testCreateExecutionContextUsesSupplierWhenProvided() {
        // custom supplier that returns a pre‑configured ExecutionContext
        CollectorContext customCollector = new CollectorContext(false, false);
        ExecutionContext customCtx = new ExecutionContext(customCollector);
        when(mockConfig.getExecutionContextSupplier()).thenReturn(() -> customCtx);

        JsonSchema schema = JsonSchema.from(mockContext, "#", null, mapper.createObjectNode(), null, false);
        ExecutionContext created = schema.createExecutionContext();

        assertSame(customCtx, created);
    }

    /**
     * The {@code setValidatorState} method should initialise a {@link ValidatorState}
     * in the collector context when none exists, and set the walk/validation flags.
     */
    @Test
    public void testSetValidatorStateInitialisesValidatorState() throws Exception {
        // we invoke the private static helper via reflection
        Method setState = JsonSchema.class.getDeclaredMethod("setValidatorState",
                ExecutionContext.class, boolean.class, boolean.class);
        setState.setAccessible(true);

        // create a fresh ExecutionContext with an empty collector
        ExecutionContext execCtx = new ExecutionContext(new CollectorContext(false, false));

        // call the helper
        setState.invoke(null, execCtx, true, false);

        // retrieve the state from the collector
        Object stateObj = execCtx.getCollectorContext().get(ValidatorState.VALIDATOR_STATE_KEY);
        assertNotNull(stateObj);
        assertTrue(stateObj instanceof ValidatorState);
        ValidatorState state = (ValidatorState) stateObj;
        assertTrue(state.isWalkEnabled());
        assertFalse(state.isValidationEnabled());
    }

    /**
     * {@code toString()} must return a JSON‑like representation containing the
     * schema path and the raw schema node.
     */
    @Test
    public void testToStringProducesReadableOutput() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "integer");
        JsonSchema schema = JsonSchema.from(mockContext, "#/mySchema", null, node, null, false);
        String result = schema.toString();

        assertTrue(result.contains("\"#/mySchema\""));
        assertTrue(result.contains("\"type\":\"integer\""));
    }

    /**
     * The {@code walk} method should honour the walk‑enabled flag set by
     * {@code setValidatorState}.  We simulate a validator that records whether
     * it was invoked.
     */
    @Test
    public void testWalkRespectsWalkEnabledFlag() {
        // ----- validator that records the call -----
        JsonValidator walkValidator = mock(JsonValidator.class);
        when(walkValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // ----- stub ValidationContext to return our validator for any keyword -----
        when(mockContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(walkValidator);

        // ----- create a simple schema (any keyword will trigger the validator) -----
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "string");
        JsonSchema schema = JsonSchema.from(mockContext, "#", null, schemaNode, null, false);

        // ----- execution context with default collector -----
        ExecutionContext execCtx = schema.createExecutionContext();

        // ----- perform walk (should invoke the validator) -----
        ValidationResult result = schema.walk(execCtx, mapper.createObjectNode(), true);
        assertTrue(result.getMessages().isEmpty());

        // verify validator was called once
        verify(walkValidator, times(1)).validate(eq(execCtx), any(JsonNode.class), any(JsonNode.class), eq(atRoot()));
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Returns the JSON‑Pointer used for the root node ("/").
     */
    private static String atRoot() {
        return "";
    }
}
```
###Test END##