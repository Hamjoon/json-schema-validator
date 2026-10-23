###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.utils.StringUtils;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import com.networknt.schema.walk.WalkListenerRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link JsonSchema}. The tests use Mockito to mock the heavy
 * dependencies ( {@link ValidationContext}, {@link JsonMetaSchema},
 * {@link JsonValidator}, etc.) and focus on the logic inside {@code JsonSchema}
 * itself.
 *
 * The test class covers:
 *   • static helper methods (combineCurrentUriWithIds, isUriFragmentWithNoContext,
 *     uriRefersToSubschema)
 *   • public getters (getCurrentUri, hasRequiredValidator, hasTypeValidator,
 *     isDynamicAnchor, etc.)
 *   • private methods accessed via reflection (updateThisAsSubschema,
 *     read, getCustomMessage, getMessageNode)
 *   • execution of {@code initializeValidators}
 *   • creation of {@code ExecutionContext}
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private JsonValidator validatorTrue;

    @Mock
    private JsonValidator validatorFalse;

    @Mock
    private JsonValidator refValidator;

    @Mock
    private JsonValidator requiredValidator;

    @Mock
    private TypeValidator typeValidator;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private SchemaValidatorsConfig config;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        // Basic stubbing for ValidationContext
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // activeDialect returns a very high value (so V201909 block is not triggered unless overridden)
        when(validationContext.activeDialect()).thenReturn(Optional.empty());
        // newValidator stub – returns the mocked validators based on the keyword
        when(validationContext.newValidator(anyString(), eq("true"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(validatorTrue);
        when(validationContext.newValidator(anyString(), eq("false"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(validatorFalse);
        when(validationContext.newValidator(anyString(), eq("$ref"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(refValidator);
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(requiredValidator);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), anyString()))
                .thenReturn(typeValidator);
    }

    /**
     * Helper to create a JsonSchema instance with a given {@code JsonNode}.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI currentUri) {
        return JsonSchema.from(validationContext, "#", currentUri, schemaNode, null, false);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for static helper methods                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsUriFragmentWithNoContext_true() {
        assertTrue(JsonSchema.isUriFragmentWithNoContext(null, "#/definitions/foo"));
    }

    @Test
    public void testIsUriFragmentWithNoContext_falseWhenUriPresent() {
        assertFalse(JsonSchema.isUriFragmentWithNoContext(URI.create("http://example.com/schema.json"), "#/defs"));
    }

    @Test
    public void testIsUriFragmentWithNoContext_falseWhenIdNotFragment() {
        assertFalse(JsonSchema.isUriFragmentWithNoContext(null, "http://example.com/foo"));
    }

    @Test
    public void testUriRefersToSubschema_true() {
        URI uri = URI.create("http://example.com/schema.json#foo");
        assertTrue(JsonSchema.uriRefersToSubschema(uri, "#"));
    }

    @Test
    public void testUriRefersToSubschema_falseWhenNoFragment() {
        URI uri = URI.create("http://example.com/schema.json");
        assertFalse(JsonSchema.uriRefersToSubschema(uri, "#"));
    }

    @Test
    public void testCombineCurrentUriWithIds_idIsNull_returnsOriginal() {
        JsonNode node = mapper.createObjectNode(); // empty object -> no "id"
        JsonSchema schema = createSchema(node, URI.create("http://example.com/base"));
        // Use reflection to call the private method
        try {
            Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
            m.setAccessible(true);
            URI result = (URI) m.invoke(schema, URI.create("http://example.com/base"), node);
            assertEquals(URI.create("http://example.com/base"), result);
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    @Test
    public void testCombineCurrentUriWithIds_fragmentWithNoContext_returnsNull() {
        JsonNode node = mapper.createObjectNode().put("$id", "#/defs");
        when(validationContext.resolveSchemaId(node)).thenReturn("#/defs");
        JsonSchema schema = createSchema(node, null);
        try {
            Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
            m.setAccessible(true);
            URI result = (URI) m.invoke(schema, null, node);
            assertNull(result);
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    @Test
    public void testCombineCurrentUriWithIds_invalidUri_throwsJsonSchemaException() {
        JsonNode node = mapper.createObjectNode().put("$id", "http://[invalid");
        when(validationContext.resolveSchemaId(node)).thenReturn("http://[invalid");
        // make uriFactory throw IllegalArgumentException
        when(uriFactory.create(any(URI.class), eq("http://[invalid"))).thenThrow(new IllegalArgumentException("bad uri"));
        JsonSchema schema = createSchema(node, URI.create("http://example.com/base"));
        try {
            Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
            m.setAccessible(true);
            m.invoke(schema, URI.create("http://example.com/base"), node);
            fail("Expected JsonSchemaException");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getTargetException() instanceof JsonSchemaException);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for getRefSchemaNode                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetRefSchemaNode_jsonPointer_resolvesNode() throws Exception {
        // Build a simple schema with an inner definition
        String json = "{ \"definitions\": { \"person\": { \"type\": \"object\" } } }";
        JsonNode root = mapper.readTree(json);
        JsonSchema schema = createSchema(root, null);

        JsonNode result = schema.getRefSchemaNode("#/definitions/person");
        assertNotNull(result);
        assertEquals("object", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_fragment_resolvesViaMetaSchema() throws Exception {
        JsonNode root = mapper.readTree("{ \"type\": \"string\" }");
        JsonSchema schema = createSchema(root, null);

        // metaSchema.getNodeByFragmentRef should be called
        JsonNode expected = mapper.readTree("{ \"type\": \"string\" }");
        when(metaSchema.getNodeByFragmentRef(eq("#/defs/foo"), any(JsonNode.class))).thenReturn(expected);

        JsonNode result = schema.getRefSchemaNode("#/defs/foo");
        assertSame(expected, result);
        verify(metaSchema).getNodeByFragmentRef("#/defs/foo", root);
    }

    @Test
    public void testGetRefSchemaNode_missingNode_callsHandleNullNode() throws Exception {
        JsonNode root = mapper.readTree("{ \"type\": \"string\" }");
        JsonSchema schema = spy(createSchema(root, null));
        // Force getRefSchemaNode to go down the path where node.isMissingNode()
        doReturn(root.at("/nonexistent")).when(schema).getRefSchemaNode(anyString());
        // Since fetchSubSchemaNode returns null, handleNullNode should return null
        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertNull(result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for lexical/ancestor navigation                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindLexicalRootStopsAtFirstId() {
        // Build a three‑level hierarchy: child -> parent -> grandparent
        JsonNode childNode = mapper.createObjectNode().put("prop", "c");
        JsonNode parentNode = mapper.createObjectNode().put("$id", "http://example.com/parent");
        JsonNode grandNode = mapper.createObjectNode(); // no id

        JsonSchema grand = createSchema(grandNode, null);
        JsonSchema parent = JsonSchema.from(validationContext, "#/parent", URI.create("http://example.com/parent"), parentNode, grand, false);
        JsonSchema child = JsonSchema.from(validationContext, "#/parent/child", URI.create("http://example.com/parent/child"), childNode, parent, false);

        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame(parent, lexicalRoot);
    }

    @Test
    public void testFindAncestorReturnsTopMostParent() {
        JsonNode leaf = mapper.createObjectNode();
        JsonSchema grand = createSchema(leaf, null);
        JsonSchema parent = JsonSchema.from(validationContext, "#/p", null, leaf, grand, false);
        JsonSchema child = JsonSchema.from(validationContext, "#/p/c", null, leaf, parent, false);
        JsonSchema ancestor = child.findAncestor();
        assertSame(grand, ancestor);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for custom message handling (private methods)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetCustomMessageWhenSupportedAndPresent() throws Exception {
        // Enable custom messages
        when(config.isCustomMessageSupported()).thenReturn(true);

        // Build a schema node with a custom message for "type"
        String json = "{ \"type\": \"string\", \"message\": { \"type\": \"must be a string\" } }";
        JsonNode node = mapper.readTree(json);
        JsonSchema schema = createSchema(node, null);

        // Use reflection to invoke private getCustomMessage
        Method m = JsonSchema.class.getDeclaredMethod("getCustomMessage", JsonNode.class, String.class);
        m.setAccessible(true);
        String msg = (String) m.invoke(schema, node, "type");
        assertEquals("must be a string", msg);
    }

    @Test
    public void testGetMessageNodeSearchesParentWhenAbsentInCurrent() throws Exception {
        // parent schema with message, child without
        String parentJson = "{ \"message\": { \"required\": \"must exist\" } }";
        String childJson = "{ \"required\": [\"a\"] }";
        JsonNode parentNode = mapper.readTree(parentJson);
        JsonNode childNode = mapper.readTree(childJson);

        JsonSchema parent = createSchema(parentNode, null);
        JsonSchema child = JsonSchema.from(validationContext, "#/child", null, childNode, parent, false);

        Method m = JsonSchema.class.getDeclaredMethod("getMessageNode", JsonNode.class, JsonSchema.class, String.class);
        m.setAccessible(true);
        JsonNode messageNode = (JsonNode) m.invoke(child, childNode, parent, "required");
        assertNotNull(messageNode);
        assertEquals("must exist", messageNode.get("required").asText());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for read() method (private) – boolean schema, $ref handling      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testReadBooleanTrueCreatesValidator() throws Exception {
        JsonNode boolNode = mapper.readTree("true");
        JsonSchema schema = createSchema(boolNode, null);
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) readMethod.invoke(schema, boolNode);
        assertTrue(map.containsKey("#/true"));
        assertSame(validatorTrue, map.get("#/true"));
    }

    @Test
    public void testReadBooleanFalseCreatesValidator() throws Exception {
        JsonNode boolNode = mapper.readTree("false");
        JsonSchema schema = createSchema(boolNode, null);
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) readMethod.invoke(schema, boolNode);
        assertTrue(map.containsKey("#/false"));
        assertSame(validatorFalse, map.get("#/false"));
    }

    @Test
    public void testReadWithRefAndOlderDialectKeepsOnlyRef() throws Exception {
        // Simulate a $ref node with an older dialect (activeDialect < V201909)
        JsonNode refNode = mapper.readTree("{ \"$ref\": \"#/defs/foo\", \"type\": \"string\" }");
        JsonSchema schema = createSchema(refNode, null);
        // Force the dialect to be older than V201909
        when(validationContext.activeDialect()).thenReturn(Optional.of(VersionFlag.V2018_02));
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) readMethod.invoke(schema, refNode);
        // Only $ref should be present
        assertEquals(1, map.size());
        assertTrue(map.keySet().iterator().next().endsWith("/$ref"));
        assertSame(refValidator, map.values().iterator().next());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for initializeValidators() and dynamic anchor handling          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInitializeValidatorsCallsPreloadOnce() {
        // Build a schema with two validators (type and required)
        String json = "{ \"type\": \"string\", \"required\": [\"a\"] }";
        JsonNode node = mapper.readTree(json);
        JsonSchema schema = createSchema(node, null);
        // First call – should trigger preload
        schema.initializeValidators();
        // Second call – should NOT trigger another preload
        schema.initializeValidators();

        verify(typeValidator, times(1)).preloadJsonSchema();
        verify(requiredValidator, times(1)).preloadJsonSchema();
    }

    @Test
    public void testIsDynamicAnchorWhenRecursiveAnchorTrue() throws Exception {
        String json = "{ \"$recursiveAnchor\": true }";
        JsonNode node = mapper.readTree(json);
        JsonSchema schema = createSchema(node, null);
        // invoke the private read method to populate fields
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        readMethod.invoke(schema, node);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchorWhenRecursiveAnchorFalse() throws Exception {
        String json = "{ \"$recursiveAnchor\": false }";
        JsonNode node = mapper.readTree(json);
        JsonSchema schema = createSchema(node, null);
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        readMethod.invoke(schema, node);
        assertFalse(schema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testReadRecursiveAnchorNonBooleanThrows() throws Exception {
        // $recursiveAnchor must be boolean – use a string to provoke exception
        String json = "{ \"$recursiveAnchor\": \"yes\" }";
        JsonNode node = mapper.readTree(json);
        JsonSchema schema = createSchema(node, null);
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        readMethod.invoke(schema, node);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for createExecutionContext()                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateExecutionContextUsesSupplierIfProvided() {
        ExecutionContext supplied = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);
        JsonSchema schema = createSchema(mapper.createObjectNode(), null);
        ExecutionContext ctx = schema.createExecutionContext();
        assertSame(supplied, ctx);
    }

    @Test
    public void testCreateExecutionContextCreatesDefaultCollectorContext() {
        when(config.getExecutionContextSupplier()).thenReturn(null);
        JsonSchema schema = createSchema(mapper.createObjectNode(), null);
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toString() and simple getters                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringFormatsCorrectly() {
        JsonNode node = mapper.readTree("{ \"type\": \"string\" }");
        JsonSchema schema = createSchema(node, null);
        String str = schema.toString();
        assertTrue(str.contains("\"#\""));
        assertTrue(str.contains("\"type\":\"string\""));
    }

    @Test
    public void testHasRequiredValidatorAndGetter() throws Exception {
        String json = "{ \"required\": [\"a\"] }";
        JsonNode node = mapper.readTree(json);
        JsonSchema schema = createSchema(node, null);
        // invoke read to populate requiredValidator
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        readMethod.invoke(schema, node);
        assertTrue(schema.hasRequiredValidator());
        assertSame(requiredValidator, schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidatorAndGetter() throws Exception {
        String json = "{ \"type\": \"string\" }";
        JsonNode node = mapper.readTree(json);
        JsonSchema schema = createSchema(node, null);
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", JsonNode.class);
        readMethod.setAccessible(true);
        readMethod.invoke(schema, node);
        assertTrue(schema.hasTypeValidator());
        assertSame(typeValidator, schema.getTypeValidator());
    }

    @Test
    public void testGetCurrentUriReflectsCombineResult() throws Exception {
        JsonNode node = mapper.createObjectNode().put("$id", "http://example.com/base#sub");
        when(validationContext.resolveSchemaId(node)).thenReturn("http://example.com/base#sub");
        // uriFactory should concatenate correctly
        when(uriFactory.create(any(URI.class), eq("http://example.com/base#sub")))
                .thenReturn(URI.create("http://example.com/base#sub"));
        JsonSchema schema = createSchema(node, URI.create("http://example.com/base"));
        assertEquals(URI.create("http://example.com/base#sub"), schema.getCurrentUri());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for updateThisAsSubschema (private) – exercised via reflection   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testUpdateThisAsSubschemaCreatesParentAndUpdatesFields() throws Exception {
        // Schema with a fragment URI and an internal definition to resolve
        String json = "{ \"definitions\": { \"inner\": { \"type\": \"string\" } } }";
        JsonNode root = mapper.readTree(json);
        JsonSchema schema = createSchema(root, URI.create("http://example.com/schema.json#definitions/inner"));
        // Force getRefSchemaNode to return the inner definition when called with the fragment
        JsonNode innerNode = root.at("/definitions/inner");
        JsonSchema spySchema = spy(schema);
        doReturn(innerNode).when(spySchema).getRefSchemaNode(anyString());

        // Invoke private updateThisAsSubschema
        Method m = JsonSchema.class.getDeclaredMethod("updateThisAsSubschema", URI.class);
        m.setAccessible(true);
        m.invoke(spySchema, URI.create("http://example.com/schema.json#definitions/inner"));

        // Verify that after the call:
        //   – parentSchema is not null,
        //   – schemaPath has been changed to the fragment,
        //   – schemaNode now points to the inner definition.
        assertNotNull(spySchema.getParentSchema());
        assertEquals("#/definitions/inner", spySchema.schemaPath);
        assertSame(innerNode, spySchema.schemaNode);
    }
}
```
###Test END##