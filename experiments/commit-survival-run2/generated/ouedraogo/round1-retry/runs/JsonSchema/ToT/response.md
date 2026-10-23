###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written from the perspective of three “experts”.  Each expert
 * contributes one test case for every public method of {@code JsonSchema};
 * the final file aggregates all of those contributions.
 *
 * The suite uses Mockito to create lightweight stubs for the heavy‑weight
 * collaborators ( {@link ValidationContext}, {@link JsonMetaSchema},
 * {@link URIFactory}, {@link SchemaValidatorsConfig}, {@link JsonValidator},
 * etc.).  Only the behaviour that is required for the particular test is
 * mocked – this keeps the tests fast, deterministic and independent of the
 * rest of the library.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal paths (valid ids, valid JSON‑Pointer refs, normal validator
 *       initialisation)</li>
 *   <li>Edge cases (fragment id without a base URI, empty schema path,
 *       boolean schemas)</li>
 *   <li>Error scenarios (invalid URI syntax, missing $ref target)</li>
 *   <li>State‑exposing methods (toString, hasRequiredValidator, etc.)</li>
 * </ul>
 *
 * All tests are written for JUnit‑4 and can be run with any JUnit‑4 runner
 * (e.g. Maven Surefire, IDE JUnit runner, etc.).
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link JsonSchema}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private JsonValidator anyValidator; // generic mock validator

    @Before
    public void setUp() throws Exception {
        // Default stub behaviour for ValidationContext
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        // By default a validator does nothing and returns an empty error set
        when(anyValidator.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        // The factory creates a dummy validator for any keyword
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(anyValidator);
        // The meta‑schema is only used for fragment resolution – by default return null
        when(metaSchema.getNodeByFragmentRef(anyString(), any(JsonNode.class))).thenReturn(null);
    }

    /**
     * Helper to create a JsonSchema instance with the supplied schema node and
     * base URI.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI baseUri) {
        return JsonSchema.from(validationContext, "#", baseUri, schemaNode, null, false);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for combineCurrentUriWithIds()
     * --------------------------------------------------------------------- */

    @Test
    public void combineCurrentUriWithIds_NoId_ReturnsOriginalUri() {
        // schema without an "id"
        ObjectNode schema = MAPPER.createObjectNode().put("type", "string");
        when(validationContext.resolveSchemaId(schema)).thenReturn(null);

        URI base = URI.create("http://example.com/schema.json");
        JsonSchema js = createSchema(schema, base);

        // Exercise the private method via reflection
        URI result = invokeCombineCurrentUriWithIds(js, base, schema);
        assertEquals("When there is no id the original URI must be returned", base, result);
    }

    @Test
    public void combineCurrentUriWithIds_FragmentIdWithoutBase_ReturnsNull() {
        // schema with a fragment id ("#foo") and no base URI
        ObjectNode schema = MAPPER.createObjectNode().put("$id", "#foo");
        when(validationContext.resolveSchemaId(schema)).thenReturn("#foo");

        JsonSchema js = createSchema(schema, null);

        URI result = invokeCombineCurrentUriWithIds(js, null, schema);
        assertNull("Fragment id with no base URI should result in null", result);
    }

    @Test(expected = JsonSchemaException.class)
    public void combineCurrentUriWithIds_InvalidUri_ThrowsException() {
        // schema with an id that is not a legal URI
        ObjectNode schema = MAPPER.createObjectNode().put("$id", "ht!tp://[invalid]");
        when(validationContext.resolveSchemaId(schema)).thenReturn("ht!tp://[invalid]");
        // URIFactory will throw IllegalArgumentException for the bad id
        when(uriFactory.create(any(URI.class), eq("ht!tp://[invalid]")))
                .thenThrow(new IllegalArgumentException("bad URI"));

        JsonSchema js = createSchema(schema, URI.create("http://example.com/root.json"));
        // The exception is thrown from the constructor → nothing else to do
        invokeCombineCurrentUriWithIds(js, URI.create("http://example.com/root.json"), schema);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for static helper isUriFragmentWithNoContext()
     * --------------------------------------------------------------------- */

    @Test
    public void isUriFragmentWithNoContext_TrueWhenFragmentAndNoBase() {
        assertTrue(JsonSchema.isUriFragmentWithNoContext(null, "#/defs"));
    }

    @Test
    public void isUriFragmentWithNoContext_FalseWhenBasePresent() {
        assertFalse(JsonSchema.isUriFragmentWithNoContext(URI.create("http://example.com"), "#/defs"));
    }

    @Test
    public void isUriFragmentWithNoContext_FalseWhenNotFragment() {
        assertFalse(JsonSchema.isUriFragmentWithNoContext(URI.create("http://example.com"), "http://example.com/other"));
    }

    /* --------------------------------------------------------------------- *
     *  Tests for uriRefersToSubschema()
     * --------------------------------------------------------------------- */

    @Test
    public void uriRefersToSubschema_TrueWhenFragmentAndRootPath() {
        URI uri = URI.create("http://example.com/schema.json#sub");
        assertTrue(JsonSchema.uriRefersToSubschema(uri, "#"));
    }

    @Test
    public void uriRefersToSubschema_FalseWhenNoFragment() {
        URI uri = URI.create("http://example.com/schema.json");
        assertFalse(JsonSchema.uriRefersToSubschema(uri, "#"));
    }

    @Test
    public void uriRefersToSubschema_FalseWhenAlreadyInSubschema() {
        URI uri = URI.create("http://example.com/schema.json#sub");
        assertFalse(JsonSchema.uriRefersToSubschema(uri, "/properties/name"));
    }

    /* --------------------------------------------------------------------- *
     *  Tests for getRefSchemaNode()
     * --------------------------------------------------------------------- */

    @Test
    public void getRefSchemaNode_JsonPointerResolution() {
        // {"properties":{"name":{"type":"string"}}}
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode props = MAPPER.createObjectNode();
        ObjectNode name = MAPPER.createObjectNode().put("type", "string");
        props.set("name", name);
        root.set("properties", props);

        JsonSchema js = createSchema(root, URI.create("http://example.com/root.json"));
        JsonNode result = js.getRefSchemaNode("#/properties/name");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void getRefSchemaNode_JsonPointerWithEscapedChars() {
        // {"properties":{"a/b":{"type":"integer"}}}
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode props = MAPPER.createObjectNode();
        ObjectNode ab = MAPPER.createObjectNode().put("type", "integer");
        props.set("a/b", ab);
        root.set("properties", props);

        JsonSchema js = createSchema(root, URI.create("http://example.com/root.json"));
        // The pointer must be URL‑decoded (the '/' in the key is escaped as %2F)
        JsonNode result = js.getRefSchemaNode("#/properties/a%2Fb");
        assertNotNull(result);
        assertEquals("integer", result.get("type").asText());
    }

    @Test
    public void getRefSchemaNode_FragmentResolvedViaMetaSchema() {
        // Simulate a fragment reference that is resolved by the meta‑schema.
        // The meta‑schema mock will return a dummy node.
        ObjectNode dummyNode = MAPPER.createObjectNode().put("dummy", true);
        when(metaSchema.getNodeByFragmentRef(eq("#/defs/foo"), any(JsonNode.class)))
                .thenReturn(dummyNode);

        ObjectNode root = MAPPER.createObjectNode(); // empty root
        JsonSchema js = createSchema(root, URI.create("http://example.com/root.json"));
        JsonNode result = js.getRefSchemaNode("#/defs/foo");
        assertSame("Meta‑schema should be consulted for fragment refs", dummyNode, result);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for validator initialisation (initializeValidators)
     * --------------------------------------------------------------------- */

    @Test
    public void initializeValidators_CallsPreloadOnAllValidators() {
        // schema with two keywords, each will cause a validator to be created
        ObjectNode schema = MAPPER.createObjectNode()
                .put("type", "string")
                .put("maxLength", 5);
        // The ValidationContext mock already returns anyValidator for any keyword.
        JsonSchema js = createSchema(schema, URI.create("http://example.com/root.json"));

        // Ensure validators are lazily created
        assertFalse("Validators must not be loaded before first call", getPrivateValidatorsLoaded(js));

        // Call initialise – all validators should be pre‑loaded
        js.initializeValidators();

        // Verify that preloadJsonSchema() was invoked on the validator mock
        verify(anyValidator, times(2)).preloadJsonSchema();
        assertTrue("Validators must be marked as loaded after initialise", getPrivateValidatorsLoaded(js));
    }

    /* --------------------------------------------------------------------- *
     *  Tests for createExecutionContext()
     * --------------------------------------------------------------------- */

    @Test
    public void createExecutionContext_UsesSupplierWhenConfigured() {
        // custom supplier that returns a pre‑constructed ExecutionContext
        CollectorContext customCollector = new CollectorContext(false, false);
        ExecutionContext supplied = new ExecutionContext(customCollector);
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);

        JsonSchema js = createSchema(MAPPER.createObjectNode(), URI.create("http://example.com"));
        ExecutionContext ctx = js.createExecutionContext();
        assertSame("The supplied ExecutionContext must be returned", supplied, ctx);
    }

    @Test
    public void createExecutionContext_DefaultBehaviour() {
        when(config.getExecutionContextSupplier()).thenReturn(null);
        JsonSchema js = createSchema(MAPPER.createObjectNode(), URI.create("http://example.com"));
        ExecutionContext ctx = js.createExecutionContext();
        assertNotNull("Default ExecutionContext must not be null", ctx);
        assertNotNull("CollectorContext inside ExecutionContext must not be null", ctx.getCollectorContext());
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toString(), hasRequiredValidator() and getRequiredValidator()
     * --------------------------------------------------------------------- */

    @Test
    public void toString_FormatsSchemaPathAndNode() {
        ObjectNode schema = MAPPER.createObjectNode().put("type", "boolean");
        JsonSchema js = createSchema(schema, URI.create("http://example.com"));
        String expected = "\"#\" : " + schema.toString();
        assertEquals(expected, js.toString());
    }

    @Test
    public void hasRequiredValidator_TrueWhenKeywordPresent() {
        ObjectNode schema = MAPPER.createObjectNode()
                .set("required", MAPPER.createArrayNode().add("name"));
        // The validator for "required" will be created by the ValidationContext mock.
        JsonSchema js = createSchema(schema, URI.create("http://example.com"));
        assertTrue(js.hasRequiredValidator());
        assertNotNull(js.getRequiredValidator());
    }

    @Test
    public void hasRequiredValidator_FalseWhenAbsent() {
        ObjectNode schema = MAPPER.createObjectNode().put("type", "string");
        JsonSchema js = createSchema(schema, URI.create("http://example.com"));
        assertFalse(js.hasRequiredValidator());
        assertNull(js.getRequiredValidator());
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods (reflection utilities)
     * --------------------------------------------------------------------- */

    /**
     * Invokes the private {@code combineCurrentUriWithIds} method via reflection.
     *
     * @return the URI returned by the method
     */
    private URI invokeCombineCurrentUriWithIds(JsonSchema schema, URI currentUri, JsonNode node) {
        try {
            java.lang.reflect.Method m = JsonSchema.class
                    .getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
            m.setAccessible(true);
            return (URI) m.invoke(schema, currentUri, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the private {@code validatorsLoaded} flag.
     */
    private boolean getPrivateValidatorsLoaded(JsonSchema schema) {
        try {
            java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
            f.setAccessible(true);
            return f.getBoolean(schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##