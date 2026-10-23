/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests focus on the public and package‑private behaviour of JsonSchema
 * without requiring the full validation engine.  Heavy dependencies are
 * replaced by lightweight Mockito mocks so that the tests run fast and are
 * deterministic.
 *
 * The test class covers:
 * <ul>
 *   <li>URI handling (combineCurrentUriWithIds, fragment handling, subschema detection)</li>
 *   <li>Reference resolution (getRefSchemaNode with JSON‑Pointer and fragment refs)</li>
 *   <li>Message handling (custom messages, message inheritance)</li>
 *   <li>Ancestor/lexical root lookup</li>
 *   <li>Special keywords ($recursiveAnchor, required, type)</li>
 *   <li>Validator initialisation (initializeValidators)</li>
 *   <li>Utility methods (toString, isDynamicAnchor, createExecutionContext)</li>
 * </ul>
 *
 * The test uses Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper}
 * to build real {@link com.fasterxml.jackson.databind.JsonNode} objects for the
 * schema, while all other collaborators are mocked.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.utils.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Test class for {@link JsonSchema}.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonMetaSchema metaSchema;
    private SchemaValidatorsConfig config;
    private URIFactory uriFactory;

    @Before
    public void setUp() {
        // ----- common mocks -------------------------------------------------
        validationContext = mock(ValidationContext.class);
        metaSchema = mock(JsonMetaSchema.class);
        config = mock(SchemaValidatorsConfig.class);
        uriFactory = mock(URIFactory.class);

        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(metaSchema.getIdKeyword()).thenReturn("$id");
        // by default no custom messages are supported
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        // a dummy validator that does nothing
        JsonValidator dummyValidator = mock(JsonValidator.class);
        when(dummyValidator.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(dummyValidator.preloadJsonSchema()).thenReturn(null);
        when(validationContext.newValidator(anyString(), anyString(), any(), any(), any()))
                .thenReturn(dummyValidator);
        // URIFactory simply returns the supplied arguments for simplicity
        when(uriFactory.create(any(), anyString()))
                .thenAnswer(inv -> {
                    URI base = inv.getArgument(0);
                    String id = inv.getArgument(1);
                    return base.resolve(id);
                });
    }

    /**
     * Helper to create a JsonSchema instance using the package‑private static factory.
     */
    private JsonSchema createSchema(String json, URI baseUri) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return JsonSchema.from(validationContext, "#", baseUri, node, null, false);
    }

    /* ---------------------------------------------------------------------- */
    /*  URI handling tests                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCombineCurrentUriWithIds_NoId_ReturnsOriginal() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}", new URI("http://example.com/schema.json"));
        // No "$id" in schema, combineCurrentUriWithIds should return the original URI
        URI result = (URI) getPrivateMethod(schema, "combineCurrentUriWithIds")
                .invoke(schema, new URI("http://example.com/base"), schema.getSchemaNode());
        assertEquals(new URI("http://example.com/base"), result);
    }

    @Test
    public void testCombineCurrentUriWithIds_WithId_AppendsId() throws Exception {
        JsonSchema schema = createSchema("{\"$id\":\"sub/part\"}", new URI("http://example.com/base"));
        URI result = (URI) getPrivateMethod(schema, "combineCurrentUriWithIds")
                .invoke(schema, new URI("http://example.com/base"), schema.getSchemaNode());
        assertEquals(new URI("http://example.com/base/sub/part"), result);
    }

    @Test
    public void testCombineCurrentUriWithIds_IdIsFragmentAndNoBase_ReturnsNull() throws Exception {
        JsonSchema schema = createSchema("{\"$id\":\"#fragment\"}", null);
        URI result = (URI) getPrivateMethod(schema, "combineCurrentUriWithIds")
                .invoke(schema, null, schema.getSchemaNode());
        assertNull(result);
    }

    @Test
    public void testUriRefersToSubschema_TrueWhenFragmentAndRootPathEmpty() throws Exception {
        // Use reflection to call the private static method
        boolean result = (boolean) getPrivateMethod(JsonSchema.class, "uriRefersToSubschema")
                .invoke(null, new URI("http://example.com/schema.json#sub"), "#");
        assertTrue(result);
    }

    @Test
    public void testUriRefersToSubschema_FalseWhenNoFragment() throws Exception {
        boolean result = (boolean) getPrivateMethod(JsonSchema.class, "uriRefersToSubschema")
                .invoke(null, new URI("http://example.com/schema.json"), "#");
        assertFalse(result);
    }

    /* ---------------------------------------------------------------------- */
    /*  $recursiveAnchor handling                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRecursiveAnchorSetsDynamicAnchorFlag() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":true}", new URI("http://example.com/base"));
        // The constructor already reads the validators, which will process $recursiveAnchor
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testRecursiveAnchorInvalidTypeThrowsException() throws Exception {
        try {
            createSchema("{\"$recursiveAnchor\":\"yes\"}", new URI("http://example.com/base"));
            fail("Expected JsonSchemaException because $recursiveAnchor is not boolean");
        } catch (JsonSchemaException ex) {
            // expected
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Reference resolution (JSON‑Pointer)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetRefSchemaNode_JsonPointerResolvesCorrectly() throws Exception {
        String schemaJson = "{"
                + "\"properties\":{"
                + "   \"name\":{\"type\":\"string\"},"
                + "   \"age\":{\"type\":\"integer\"}"
                + "}"
                + "}";
        JsonSchema schema = createSchema(schemaJson, new URI("http://example.com/base"));
        JsonNode node = schema.getRefSchemaNode("#/properties/name");
        assertNotNull(node);
        assertEquals("string", node.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerMissingReturnsNull() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}", new URI("http://example.com/base"));
        JsonNode node = schema.getRefSchemaNode("#/nonexistent");
        assertNull(node);
    }

    @Test
    public void testGetRefSchemaNode_FragmentReferenceUsesMetaSchema() throws Exception {
        // metaSchema.getNodeByFragmentRef should be called; we stub it to return a node
        JsonNode fragmentNode = MAPPER.readTree("{\"type\":\"boolean\"}");
        when(metaSchema.getNodeByFragmentRef(eq("#/defs/flag"), any(JsonNode.class)))
                .thenReturn(fragmentNode);

        JsonSchema schema = createSchema("{\"$defs\":{\"flag\":{\"type\":\"boolean\"}}}", new URI("http://example.com/base"));
        JsonNode result = schema.getRefSchemaNode("#/defs/flag");
        assertNotNull(result);
        assertEquals("boolean", result.get("type").asText());
    }

    /* ---------------------------------------------------------------------- */
    /*  Custom message handling                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetCustomMessage_EnabledAndPresent() throws Exception {
        when(config.isCustomMessageSupported()).thenReturn(true);
        String schemaJson = "{"
                + "\"type\":\"string\","
                + "\"message\":{\"type\":\"must be a string\"}"
                + "}";
        JsonSchema schema = createSchema(schemaJson, new URI("http://example.com/base"));
        // invoke private getCustomMessage for keyword "type"
        String custom = (String) getPrivateMethod(schema, "getCustomMessage")
                .invoke(schema, schema.getSchemaNode(), "type");
        assertEquals("must be a string", custom);
    }

    @Test
    public void testGetCustomMessage_NotSupportedReturnsNull() throws Exception {
        String schemaJson = "{\"type\":\"string\",\"message\":{\"type\":\"msg\"}}";
        JsonSchema schema = createSchema(schemaJson, new URI("http://example.com/base"));
        String custom = (String) getPrivateMethod(schema, "getCustomMessage")
                .invoke(schema, schema.getSchemaNode(), "type");
        assertNull(custom);
    }

    @Test
    public void testGetMessageNode_InheritsFromParent() throws Exception {
        // parent schema provides a message for "type"
        String parentJson = "{\"type\":\"object\",\"message\":{\"type\":\"parent msg\"}}";
        JsonSchema parent = createSchema(parentJson, new URI("http://example.com/parent"));
        // child overrides nothing, but inherits the message node
        String childJson = "{\"properties\":{\"p\":{}}}";
        JsonSchema child = parent.createChildSchema("#/properties/p", MAPPER.readTree(childJson));
        // Use reflection to call getMessageNode on child
        JsonNode messageNode = (JsonNode) getPrivateMethod(child, "getMessageNode")
                .invoke(child, child.getSchemaNode(), parent, "type");
        assertNotNull(messageNode);
        assertEquals("parent msg", messageNode.get("type").asText());
    }

    /* ---------------------------------------------------------------------- */
    /*  Ancestor / lexical root lookup                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindAncestor_ReturnsRootWhenNoParent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}", new URI("http://example.com/root"));
        JsonSchema ancestor = schema.findAncestor();
        assertSame(schema, ancestor);
    }

    @Test
    public void testFindAncestor_ReturnsTopMostParent() throws Exception {
        JsonSchema root = createSchema("{\"type\":\"object\"}", new URI("http://example.com/root"));
        JsonSchema child = root.createChildSchema("#/child", MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema grand = child.createChildSchema("#/grand", MAPPER.readTree("{\"type\":\"integer\"}"));
        assertSame(root, grand.findAncestor());
    }

    @Test
    public void testFindLexicalRoot_StopsAtFirstSchemaWithId() throws Exception {
        // Root has $id, child does not
        JsonSchema root = createSchema("{\"$id\":\"rootId\"}", new URI("http://example.com/root"));
        JsonSchema child = root.createChildSchema("#/c", MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame(root, lexicalRoot);
    }

    @Test
    public void testFindLexicalRoot_ReturnsSelfIfItHasId() throws Exception {
        JsonSchema schema = createSchema("{\"$id\":\"myId\"}", new URI("http://example.com/hasId"));
        JsonSchema lexicalRoot = schema.findLexicalRoot();
        assertSame(schema, lexicalRoot);
    }

    /* ---------------------------------------------------------------------- */
    /*  Required / type validator detection                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasRequiredValidator_Detected() throws Exception {
        String schemaJson = "{\"required\":[\"name\"]}";
        JsonSchema schema = createSchema(schemaJson, new URI("http://example.com/base"));
        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_Detected() throws Exception {
        String schemaJson = "{\"type\":\"array\"}";
        JsonSchema schema = createSchema(schemaJson, new URI("http://example.com/base"));
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    /* ---------------------------------------------------------------------- */
    /*  Validator initialisation                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInitializeValidators_CallsPreloadOnce() throws Exception {
        String schemaJson = "{\"type\":\"string\"}";
        JsonSchema schema = createSchema(schemaJson, new URI("http://example.com/base"));
        // first call – should invoke preload on each validator
        schema.initializeValidators();
        // second call – should NOT invoke again (idempotent)
        schema.initializeValidators();

        // Capture the validator that was created by the ValidationContext
        ArgumentCaptor<JsonValidator> captor = ArgumentCaptor.forClass(JsonValidator.class);
        verify(validationContext, atLeastOnce()).newValidator(anyString(), anyString(), any(), any(), any());

        // All created validators should have had preloadJsonSchema called exactly once
        verify(captor.getAllValues().get(0), times(1)).preloadJsonSchema();
    }

    /* ---------------------------------------------------------------------- */
    /*  Utility methods                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_FormatsCorrectly() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"null\"}", new URI("http://example.com/base"));
        String str = schema.toString();
        assertTrue(str.startsWith("\"#\""));
        assertTrue(str.contains("\"type\":\"null\""));
    }

    @Test
    public void testCreateExecutionContext_UsesSupplierIfPresent() throws Exception {
        ExecutionContext supplied = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);
        JsonSchema schema = createSchema("{\"type\":\"object\"}", new URI("http://example.com/base"));
        ExecutionContext ctx = schema.createExecutionContext();
        assertSame(supplied, ctx);
    }

    @Test
    public void testCreateExecutionContext_DefaultCreatesCollectorContext() throws Exception {
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(true);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        JsonSchema schema = createSchema("{\"type\":\"object\"}", new URI("http://example.com/base"));
        ExecutionContext ctx = schema.createExecutionContext();

        assertNotNull(ctx);
        CollectorContext collector = ctx.getCollectorContext();
        assertTrue(collector.isUnevaluatedItemsAnalysisDisabled());
        assertFalse(collector.isUnevaluatedPropertiesAnalysisDisabled());
    }

    /* ---------------------------------------------------------------------- */
    /*  Reflection helper – avoids clutter in the test body                    */
    /* ---------------------------------------------------------------------- */

    /**
     * Retrieves a private (or package‑private) method as a {@link java.lang.reflect.Method}
     * and makes it accessible.
     *
     * @param targetClass the class that declares the method
     * @param name        the method name
     * @param parameterTypes optional parameter types
     * @return accessible Method
     */
    private static java.lang.reflect.Method getPrivateMethod(Class<?> targetClass, String name,
                                                             Class<?>... parameterTypes) throws Exception {
        java.lang.reflect.Method m = targetClass.getDeclaredMethod(name, parameterTypes);
        m.setAccessible(true);
        return m;
    }

    /**
     * Convenience overload for instance methods.
     */
    private java.lang.reflect.Method getPrivateMethod(Object instance, String name,
                                                      Class<?>... parameterTypes) throws Exception {
        return getPrivateMethod(instance.getClass(), name, parameterTypes);
    }
}
