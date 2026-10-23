package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link JsonSchema}.
 *
 * The tests use Mockito to stub out the heavy infrastructure required by {@link JsonSchema}
 * (e.g. {@link ValidationContext}, {@link JsonMetaSchema}, {@link JsonValidator}, etc.).
 * Only the logic inside {@link JsonSchema} itself is exercised.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private JsonValidator validatorMock;

    @Mock
    private JsonValidator requiredValidatorMock;

    @Mock
    private JsonValidator typeValidatorMock;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private URIFactory uriFactory;

    private ObjectMapper mapper = new ObjectMapper();

    private final URI baseUri = URI.create("http://example.com/schema.json");

    @Before
    public void setUp() {
        // ---- ValidationContext stubs -------------------------------------------------
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        // ResolveSchemaId will be defined per‑test (null by default)
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // newValidator returns a generic mock unless the test overrides it
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validatorMock);
        // active dialect defaults to the highest value (no special handling)
        when(validationContext.activeDialect()).thenReturn(java.util.Optional.empty());

        // ---- JsonMetaSchema stubs ----------------------------------------------------
        when(metaSchema.getIdKeyword()).thenReturn("$id");
        when(metaSchema.getNodeByFragmentRef(anyString(), any(JsonNode.class))).thenReturn(null);

        // ---- Config stubs -------------------------------------------------------------
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        when(config.getExecutionContextSupplier()).thenReturn(null);
    }

    /**
     * Helper to create a JsonSchema instance from a JSON string.
     */
    private JsonSchema createSchema(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        return JsonSchema.from(validationContext, "#", baseUri, node, null, false);
    }

    @Test
    public void testCombineCurrentUriWithIds_NoId_ReturnsSameUri() throws Exception {
        // schema without $id
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        // currentUri should stay unchanged
        assertEquals(baseUri, schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_WithAbsoluteId() throws Exception {
        // make resolveSchemaId return an absolute id
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn("http://example.com/other.json");
        when(uriFactory.create(baseUri, "http://example.com/other.json"))
                .thenReturn(URI.create("http://example.com/other.json"));

        JsonSchema schema = createSchema("{\"$id\":\"http://example.com/other.json\"}");
        assertEquals(URI.create("http://example.com/other.json"), schema.getCurrentUri());
    }

    @Test
    public void testCombineCurrentUriWithIds_WithFragmentIdAndNullBase() throws Exception {
        // currentUri null, id is a fragment – should return null
        JsonSchema schema = JsonSchema.from(validationContext, "#", null,
                mapper.readTree("{\"$id\":\"#fragment\"}"), null, false);
        assertNull(schema.getCurrentUri());
    }

    @Test
    public void testUriRefersToSubschema_TrueWhenFragmentAndRootPath() throws Exception {
        // Use reflection to invoke the private static method
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("uriRefersToSubschema", URI.class, String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, URI.create("http://example.com/schema.json#foo"), "#");
        assertTrue(result);
    }

    @Test
    public void testIsUriFragmentWithNoContext_TrueWhenFragmentAndNoBase() throws Exception {
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("isUriFragmentWithNoContext", URI.class, String.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, null, "#/defs");
        assertTrue(result);
    }

    @Test
    public void testGetRefSchemaNode_JsonPointerResolution() throws Exception {
        String schemaJson = "{\n" +
                "  \"properties\": {\n" +
                "    \"name\": {\"type\":\"string\"}\n" +
                "  }\n" +
                "}";
        JsonSchema schema = createSchema(schemaJson);
        JsonNode refNode = schema.getRefSchemaNode("#/properties/name");
        assertNotNull(refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testFindLexicalRoot_WithIdPropagation() throws Exception {
        // parent schema has $id, child does not
        String parentJson = "{\"$id\":\"http://example.com/parent\"}";
        String childJson = "{\"type\":\"object\"}";
        JsonSchema parent = createSchema(parentJson);
        // make child schema using createChildSchema
        JsonSchema child = parent.createChildSchema("#/child", mapper.readTree(childJson));
        // child.hasId should be false, so lexical root should be parent
        JsonSchema lexicalRoot = child.findLexicalRoot();
        assertSame(parent, lexicalRoot);
    }

    @Test
    public void testFindAncestor_ReturnsRootWhenNoParent() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        JsonSchema ancestor = schema.findAncestor();
        assertSame(schema, ancestor);
    }

    @Test
    public void testHasRequiredAndTypeValidator() throws Exception {
        // override newValidator to return specific mocks for "required" and "type"
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(requiredValidatorMock);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(typeValidatorMock);

        JsonSchema schema = createSchema("{\"required\":[\"a\"],\"type\":\"object\"}");
        assertTrue(schema.hasRequiredValidator());
        assertSame(requiredValidatorMock, schema.getRequiredValidator());

        assertTrue(schema.hasTypeValidator());
        assertSame(typeValidatorMock, schema.getTypeValidator());
    }

    @Test
    public void testDynamicAnchorFlag() throws Exception {
        JsonSchema schema = createSchema("{\"$recursiveAnchor\":true}");
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testValidate_NoValidators_ReturnsEmptySet() throws Exception {
        // schema without any keywords – read() will produce an empty validator map
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        // stub validator to return empty set
        when(validatorMock.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());

        JsonNode instance = mapper.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> errors = schema.validate(instance);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAndCollect_CollectsNoErrors() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        JsonNode instance = mapper.readTree("{\"foo\":1}");
        // validator returns empty set
        when(validatorMock.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());

        ValidationResult result = schema.validateAndCollect(instance);
        assertTrue(result.getMessages().isEmpty());
        // ensure collector context was created (no exception means success)
        assertNotNull(result.getExecutionContext());
    }

    @Test
    public void testWalk_DelegatesToValidators() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        // make the validator also implement JsonSchemaWalker (the real validators do)
        JsonSchemaWalker walkerMock = mock(JsonSchemaWalker.class);
        when(walkerMock.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());
        // replace the validator in the map with our walker mock
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, Collections.singletonMap("dummy", walkerMock));

        JsonNode instance = mapper.readTree("{\"a\":1}");
        ExecutionContext execCtx = schema.createExecutionContext();
        Set<ValidationMessage> messages = schema.walk(execCtx, instance, instance, "$", true);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testToString_Format() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        String s = schema.toString();
        assertTrue(s.contains("\"#\""));
        assertTrue(s.contains("\"type\":\"object\""));
    }

    @Test
    public void testInitializeValidators_PreloadsEachValidator() throws Exception {
        // create a schema that will generate two validators (type and required)
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(typeValidatorMock);
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(requiredValidatorMock);

        JsonSchema schema = createSchema("{\"type\":\"object\",\"required\":[\"a\"]}");

        // preload should be called exactly once per validator
        schema.initializeValidators();

        verify(typeValidatorMock, times(1)).preloadJsonSchema();
        verify(requiredValidatorMock, times(1)).preloadJsonSchema();
    }

    @Test
    public void testCreateExecutionContext_UsesConfigSupplierIfPresent() throws Exception {
        // supply a custom ExecutionContext via the config
        ExecutionContext customCtx = new ExecutionContext(new CollectorContext(false, false));
        when(config.getExecutionContextSupplier()).thenReturn(() -> customCtx);

        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = schema.createExecutionContext();

        assertSame(customCtx, ctx);
    }

    @Test
    public void testCreateExecutionContext_DefaultsWhenNoSupplier() throws Exception {
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }
}
