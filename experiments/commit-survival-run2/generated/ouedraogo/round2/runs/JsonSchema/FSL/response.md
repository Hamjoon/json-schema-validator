###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link JsonSchema}. The tests use Mockito to mock the heavy
 * collaborators (ValidationContext, JsonValidator, etc.) and focus on the
 * behaviour of the public API as well as the private helpers that are exercised
 * via reflection.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private JsonValidator mockValidator;

    @Mock
    private JsonValidator mockTypeValidator;

    @Mock
    private JsonValidator mockRequiredValidator;

    @Mock
    private URIFactory uriFactory;

    @Before
    public void setUp() {
        // Basic stubbing used by many tests
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(metaSchema.getIdKeyword()).thenReturn("$id");
        when(metaSchema.getNodeByFragmentRef(anyString(), any(JsonNode.class))).thenReturn(null);
    }

    /**
     * Helper to create a JsonSchema instance using the package‑private static
     * {@code from} method. The method is accessible because the test class lives
     * in the same package.
     */
    private JsonSchema createSchema(String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent) {
        return JsonSchema.from(validationContext, schemaPath, currentUri, schemaNode, parent, false);
    }

    @Test
    public void testCombineCurrentUriWithIds_IdAbsent_ReturnsOriginalUri() throws Exception {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode(); // empty node, no id
        when(validationContext.resolveSchemaId(schemaNode)).thenReturn(null);

        JsonSchema schema = createSchema("#", new URI("http://example.com/base"), schemaNode, null);
        assertEquals(new URI("http://example.com/base"), schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_IdPresent_ReturnsCombinedUri() throws Exception {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("$id", "sub/schema.json");

        when(validationContext.resolveSchemaId(schemaNode)).thenReturn("sub/schema.json");
        when(uriFactory.create(new URI("http://example.com/base"), "sub/schema.json"))
                .thenReturn(new URI("http://example.com/base/sub/schema.json"));

        JsonSchema schema = createSchema("#", new URI("http://example.com/base"), schemaNode, null);
        assertEquals(new URI("http://example.com/base/sub/schema.json"), schema.getCurrentUri());
    }

    @Test
    public void testIsUriFragmentWithNoContext() throws Exception {
        Method method = JsonSchema.class.getDeclaredMethod("isUriFragmentWithNoContext", URI.class, String.class);
        method.setAccessible(true);

        // fragment with no base URI
        assertTrue((Boolean) method.invoke(null, null, "#/def"));
        // fragment with base URI – should be false
        assertFalse((Boolean) method.invoke(null, new URI("http://example.com"), "#/def"));
        // non‑fragment – false
        assertFalse((Boolean) method.invoke(null, null, "http://example.com"));
    }

    @Test
    public void testUriRefersToSubschema() throws Exception {
        Method method = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        method.setAccessible(true);

        URI uriWithFragment = new URI("http://example.com/schema.json#def");
        // Empty schemaPath → true
        assertTrue((Boolean) method.invoke(null, uriWithFragment, ""));
        // schemaPath "#" → true
        assertTrue((Boolean) method.invoke(null, uriWithFragment, "#"));
        // non‑empty schemaPath → false (already inside a subschema)
        assertFalse((Boolean) method.invoke(null, uriWithFragment, "/properties"));
    }

    @Test
    public void testGetRefSchemaNode_JsonPointer() throws Exception {
        // root schema containing a property "foo"
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        ObjectNode fooNode = JsonNodeFactory.instance.objectNode();
        fooNode.put("type", "string");
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.set("foo", fooNode);
        rootNode.set("properties", properties);

        JsonSchema rootSchema = createSchema("#", null, rootNode, null);
        JsonSchema childSchema = rootSchema.createChildSchema("#/properties/foo", JsonNodeFactory.instance.objectNode());

        JsonNode refNode = childSchema.getRefSchemaNode("#/properties/foo");
        assertNotNull(refNode);
        assertTrue(refNode.isObject());
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testFindLexicalRoot_WithId() throws Exception {
        // root has $id, child does not
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        rootNode.put("$id", "http://example.com/root");
        JsonSchema rootSchema = createSchema("#", null, rootNode, null);

        ObjectNode childNode = JsonNodeFactory.instance.objectNode(); // no $id
        JsonSchema childSchema = rootSchema.createChildSchema("#/child", childNode);

        JsonSchema lexicalRoot = childSchema.findLexicalRoot();
        assertSame(rootSchema, lexicalRoot);
    }

    @Test
    public void testInitializeValidators_PreloadsValidators() throws Exception {
        // schema with a simple "type" keyword
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");

        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(mockTypeValidator);
        when(mockTypeValidator.preloadJsonSchema()).thenReturn(null);

        JsonSchema schema = createSchema("#", null, schemaNode, null);
        // At this point validators are not yet loaded
        schema.initializeValidators();

        // Verify that preloadJsonSchema was called exactly once
        verify(mockTypeValidator, times(1)).preloadJsonSchema();

        // Second call must not trigger another preload
        schema.initializeValidators();
        verifyNoMoreInteractions(mockTypeValidator);
    }

    @Test
    public void testIsDynamicAnchor_WhenRecursiveAnchorTrue() throws Exception {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("$recursiveAnchor", true);

        JsonSchema schema = createSchema("#", null, schemaNode, null);
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testCreateExecutionContext_UsesSupplierWhenPresent() throws Exception {
        ExecutionContext customContext = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> customContext);
        when(validationContext.getConfig()).thenReturn(config);

        JsonSchema schema = createSchema("#", null, JsonNodeFactory.instance.objectNode(), null);
        ExecutionContext ec = schema.createExecutionContext();
        assertSame(customContext, ec);
    }

    @Test
    public void testToString_Format() throws Exception {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "integer");

        JsonSchema schema = createSchema("#/mySchema", null, schemaNode, null);
        String expected = "\"#/mySchema\" : " + schemaNode.toString();
        assertEquals(expected, schema.toString());
    }

    @Test
    public void testHasRequiredAndTypeValidators() throws Exception {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.putArray("required").add("id");
        schemaNode.put("type", "object");

        // Stub validator creation for "required" and "type"
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(mockRequiredValidator);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(mockTypeValidator);

        JsonSchema schema = createSchema("#", null, schemaNode, null);

        assertTrue(schema.hasRequiredValidator());
        assertSame(mockRequiredValidator, schema.getRequiredValidator());

        assertTrue(schema.hasTypeValidator());
        assertSame(mockTypeValidator, schema.getTypeValidator());
    }

    @Test
    public void testValidate_DelegatesToValidatorsAndCollectsErrors() throws Exception {
        // Prepare a schema with a single validator that will return an error
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");

        ValidationMessage errorMessage = ValidationMessage.of("type", CustomErrorMessageType.of("type.error"),
                new MessageFormat("type error"), "type");
        Set<ValidationMessage> errorSet = new LinkedHashSet<>();
        errorSet.add(errorMessage);

        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(mockValidator);
        when(mockValidator.validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorSet);

        JsonSchema schema = createSchema("#", null, schemaNode, null);
        ExecutionContext ec = schema.createExecutionContext();

        JsonNode instance = JsonNodeFactory.instance.textNode(123); // not a string
        Set<ValidationMessage> result = schema.validate(ec, instance, instance, "$");

        assertEquals(1, result.size());
        assertTrue(result.contains(errorMessage));
    }

    @Test
    public void testWalk_InvokesKeywordWalkListenerRunnerWhenPresent() throws Exception {
        // Set up a schema with a dummy validator that also implements JsonSchemaWalker
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");

        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // Our mock validator will also be a walker
        JsonValidator validator = mock(JsonValidator.class, withSettings().extraInterfaces(JsonSchemaWalker.class));
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(validator);
        when(((JsonSchemaWalker) validator).walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // Mock a KeywordWalkListenerRunner that always returns true for pre‑walk
        KeywordWalkListenerRunner kwRunner = mock(KeywordWalkListenerRunner.class);
        when(kwRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any())).thenReturn(true);
        when(kwRunner.runPostWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any(), any())).thenReturn(null);

        // Force the schema to use the mocked runner
        JsonSchema schema = createSchema("#", null, schemaNode, null);
        // Use reflection to set the private field
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        f.setAccessible(true);
        f.set(schema, kwRunner);

        ExecutionContext ec = schema.createExecutionContext();
        JsonNode data = JsonNodeFactory.instance.objectNode();

        Set<ValidationMessage> messages = schema.walk(ec, data, true);
        // No validation errors expected
        assertTrue(messages.isEmpty());

        // Verify that pre‑walk listener was consulted
        verify(kwRunner, times(1)).runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any(), any());
    }
}
###Test END##