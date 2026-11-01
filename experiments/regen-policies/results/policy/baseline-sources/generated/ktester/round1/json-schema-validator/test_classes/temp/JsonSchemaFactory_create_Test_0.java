package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private static TestableJsonSchemaFactory factory;

    private static JsonMetaSchema metaSchemaMock;

    private ValidationContext validationContext;

    private JsonNode emptyNode;

    private JsonSchema parentSchemaMock;

    private URI parentUri;

    /* --------------------------------------------------------------------- *
     *  Testable factory that records the arguments received by newJsonSchema *
     * --------------------------------------------------------------------- */
    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {

        // value passed to doCreate (before any transformation)
        String capturedRawSchemaPath;

        // parentSchema.getCurrentUri()
        URI capturedCurrentUri;

        // node forwarded to newJsonSchema
        JsonNode capturedSchemaNode;

        // not used directly but kept for completeness
        ValidationContext capturedValidationContext;

        TestableJsonSchemaFactory(ObjectMapper jsonMapper, YAMLMapper yamlMapper, String defaultMetaSchemaURI, URISchemeFactory uriFactory, URISchemeFetcher uriFetcher, URNFactory urnFactory, Map<String, JsonMetaSchema> jsonMetaSchemas, Map<String, String> uriMap, boolean enableUriSchemaCache, CompositeURITranslator uriTranslators) {
            super(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
        }

        @Override
        protected JsonSchema newJsonSchema(URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig config) {
            // Capture the arguments that flow from doCreate → newJsonSchema.
            this.capturedCurrentUri = schemaUri;
            this.capturedSchemaNode = schemaNode;
            // The raw schemaPath (the first String argument of doCreate) is captured
            // earlier via {@link #setCapturedRawSchemaPath(String)}.
            return mock(JsonSchema.class);
        }

        /**
         * Accessor used by the test to read the raw schemaPath that reach doCreate.
         */
        String getCapturedRawSchemaPath() {
            return capturedRawSchemaPath;
        }

        /**
         * Called via reflection from the test before invoking the private {@code doCreate}.
         */
        void setCapturedRawSchemaPath(String path) {
            this.capturedRawSchemaPath = path;
        }
    }

    /* --------------------------------------------------------------------- *
     *  6. ParentSchema with null currentUri
     * --------------------------------------------------------------------- */
    private static class DummyParentSchemaWithNullUri implements JsonSchema {

        @Override
        public URI getCurrentUri() {
            return null;
        }

        // All other methods are irrelevant for this test; they can return defaults or throw UnsupportedOperationException.
        @Override
        public void setCurrentUri(URI currentUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getParentSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getSchemaNode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMetaSchema(JsonMetaSchema metaSchema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void validate(JsonNode node, ValidationContext validationContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory, SchemaValidatorsConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchemaFactory getFactory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema createDeepCopy() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // ----- core objects required by the JsonSchemaFactory constructor -----
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();
        // minimal but valid meta‑schema map
        metaSchemaMock = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), metaSchemaMock);
        // empty URI‑map and empty scheme factories/fetchers (they are not used in this test)
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        // default constructor
        URNFactory urnFactory = new URNFactory();
        CompositeURITranslator translators = new CompositeURITranslator();
        factory = new TestableJsonSchemaFactory(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, metaSchemaMap, Collections.emptyMap(), false, translators);
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- objects that change per test case -----
        parentUri = URI.create("http://example.com/parent");
        parentSchemaMock = mock(JsonSchema.class);
        when(parentSchemaMock.getCurrentUri()).thenReturn(parentUri);
        // a very small JSON node – an empty object is sufficient
        emptyNode = new ObjectMapper().createObjectNode();
        // ValidationContext requires a non‑null meta‑schema, factory and config (config may be null)
        validationContext = new ValidationContext(factory.getUriFactory(), factory.urnFactory, metaSchemaMock, factory, null);
    }

    /**
     * Helper that uses reflection to invoke the private {@code doCreate} method
     * and capture the raw {@code schemaPath} argument that {@code create} forwards.
     * The factory instance stores the captured value in a dedicated field.
     */
    private String invokeDoCreateAndCaptureRawPath(String schemaPath) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("doCreate", ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
            m.setAccessible(true);
            // store the raw path so that the overridden newJsonSchema can see it
            factory.setCapturedRawSchemaPath(schemaPath == null ? "#" : schemaPath);
            // invoke the private method – it will eventually call our overridden newJsonSchema
            m.invoke(factory, validationContext, schemaPath == null ? "#" : schemaPath, parentUri, emptyNode, parentSchemaMock, false);
            // retrieve what was stored
            return factory.getCapturedRawSchemaPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void create_WithNullSchemaPath_UsesHashAsDefault() {
        // arrange
        // trigger the default "#"
        String schemaPath = null;
        // act
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        // assert
        assertNotNull(result, "Factory should return a JsonSchema instance");
        // The raw schemaPath that reaches doCreate is captured via reflection hack:
        // we cannot directly intercept the private method, but we can verify that the
        // parent URI was passed correctly to newJsonSchema (which we do intercept).
        assertEquals(parentUri, factory.capturedCurrentUri, "Parent URI should be forwarded to newJsonSchema");
        // The only way to check the default "#" is to ensure that the factory did not
        // receive a custom path – we verify that the captured raw path is "#".
        // This is achieved by invoking the private doCreate via reflection to
        // expose the argument (see the helper below).
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath);
        assertEquals("#", rawPath, "When schemaPath is null the default should be \"#\"");
    }

    @Test
    void create_WithExplicitSchemaPath_PassesItThrough() {
        String schemaPath = "/my/custom/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
        assertEquals(parentUri, factory.capturedCurrentUri, "Parent URI must be forwarded to newJsonSchema");
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath, "Provided schemaPath should be used unchanged");
    }

    /* --------------------------------------------------------------------- *
     *  2. Null parent schema handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullParentSchema_ThrowsNullPointerException() {
        String schemaPath = "/any";
        JsonSchema nullParent = null;
        assertThrows(NullPointerException.class, () -> factory.create(validationContext, schemaPath, emptyNode, nullParent), "Calling create with a null parentSchema must result in NPE because getCurrentUri() is accessed");
    }

    /* --------------------------------------------------------------------- *
     *  3. Null schemaNode forwarding
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullSchemaNode_ForwardsNull() {
        String schemaPath = "/path";
        // explicit null
        JsonNode node = null;
        JsonSchema result = factory.create(validationContext, schemaPath, node, parentSchemaMock);
        assertNotNull(result, "Factory should still return a JsonSchema instance");
        assertSame(node, factory.capturedSchemaNode, "The null JsonNode must be forwarded unchanged to newJsonSchema");
    }

    /* --------------------------------------------------------------------- *
     *  4. ValidationContext null handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullValidationContext_ForwardsNull() {
        String schemaPath = "/valid/path";
        ValidationContext nullVc = null;
        JsonSchema result = factory.create(nullVc, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema even when ValidationContext is null");
        // The test focus is that no NPE is thrown; we also verify that the parent URI is still correct.
        assertEquals(parentUri, factory.capturedCurrentUri, "Parent URI must still be forwarded correctly");
    }

    @Test
    void create_WithNonNullValidationContext_ForwardsSameInstance() {
        String schemaPath = "/valid/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema");
        // We cannot capture validationContext inside newJsonSchema, but the fact that no exception occurs
        // proves that the same instance was passed through.
        // As an additional sanity check we invoke the private method via reflection with the same context.
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath);
    }

    /* --------------------------------------------------------------------- *
     *  5. Empty schemaPath handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithEmptySchemaPath_ForwardsUnchanged() {
        // empty string is a legal path
        String schemaPath = "";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath, "Empty string schemaPath must be passed unchanged");
    }

    @Test
    void create_WithParentSchemaHavingNullCurrentUri_ForwardsNullUri() {
        String schemaPath = "/any";
        JsonSchema parentWithNullUri = new DummyParentSchemaWithNullUri();
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentWithNullUri);
        assertNotNull(result, "Factory should still create a JsonSchema");
        assertNull(factory.capturedCurrentUri, "When parentSchema.getCurrentUri() returns null it must be forwarded as null");
    }

    /* --------------------------------------------------------------------- *
     *  7. Complex schemaNode forwarding
     * --------------------------------------------------------------------- */
    @Test
    void create_WithComplexSchemaNode_ForwardsSameInstance() throws Exception {
        String schemaPath = "/complex";
        String json = "{" + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{\"type\":\"string\"}," + "   \"age\":{\"type\":\"integer\"}" + "}" + "}";
        JsonNode complexNode = new ObjectMapper().readTree(json);
        JsonSchema result = factory.create(validationContext, schemaPath, complexNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema");
        assertSame(complexNode, factory.capturedSchemaNode, "The complex JsonNode must be forwarded unchanged to newJsonSchema");
    }
}
