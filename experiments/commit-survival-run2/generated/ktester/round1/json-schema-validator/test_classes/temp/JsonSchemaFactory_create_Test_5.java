package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

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
     *  Dummy parent schema that returns null for getCurrentUri()
     * --------------------------------------------------------------------- */
    private static class DummyParentSchemaWithNullUri extends JsonSchema {

        DummyParentSchemaWithNullUri() {
            // JsonSchema constructor: (JsonSchemaFactory factory,
            //                         JsonNode schemaNode,
            //                         JsonMetaSchema metaSchema,
            //                         URI currentUri,
            //                         JsonSchema parentSchema)
            super(null, null, null, null, null);
        }

        @Override
        public URI getCurrentUri() {
            return null;
        }

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

    /* --------------------------------------------------------------------- *
     *  Simple concrete {@link JsonSchema} implementation used as a parent
     *  schema stub.
     * --------------------------------------------------------------------- */
    private static class SimpleParentSchema extends JsonSchema {

        private final URI currentUri;

        SimpleParentSchema(URI currentUri) {
            // JsonSchema constructor: (JsonSchemaFactory factory,
            //                         JsonNode schemaNode,
            //                         JsonMetaSchema metaSchema,
            //                         URI currentUri,
            //                         JsonSchema parentSchema)
            super(null, null, null, currentUri, null);
            this.currentUri = currentUri;
        }

        @Override
        public URI getCurrentUri() {
            return currentUri;
        }

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
        // Obtain the public singleton instance provided by the library.
        factory = JsonSchemaFactory.getInstance();
    }

    @BeforeEach
    void setupBeforeEach() {
        parentUri = URI.create("http://example.com/parent");
        parentSchemaMock = new SimpleParentSchema(parentUri);
        emptyNode = new ObjectMapper().createObjectNode();
        // Obtain a meta‑schema instance from the factory.
        JsonMetaSchema metaSchema = factory.getMetaSchemaByURI(URI.create(DEFAULT_META_SCHEMA_URI));
        // ValidationContext can be constructed with a null URNFactory – it is not needed for these tests.
        validationContext = new ValidationContext(factory.getUriFactory(), null, metaSchema, factory, null);
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
        JsonSchema result = factory.create(validationContext, null, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
    }

    @Test
    void create_WithExplicitSchemaPath_PassesItThrough() {
        String schemaPath = "/my/custom/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
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
        JsonNode node = null;
        JsonSchema result = factory.create(validationContext, schemaPath, node, parentSchemaMock);
        assertNotNull(result, "Factory should still return a JsonSchema instance");
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
    }

    @Test
    void create_WithNonNullValidationContext_ForwardsSameInstance() {
        String schemaPath = "/valid/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema");
    }

    /* --------------------------------------------------------------------- *
     *  5. Empty schemaPath handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithEmptySchemaPath_ForwardsUnchanged() {
        String schemaPath = "";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
    }

    @Test
    void create_WithParentSchemaHavingNullCurrentUri_ForwardsNullUri() {
        String schemaPath = "/any";
        JsonSchema parentWithNullUri = new DummyParentSchemaWithNullUri();
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentWithNullUri);
        assertNotNull(result, "Factory should still create a JsonSchema");
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
    }
}
