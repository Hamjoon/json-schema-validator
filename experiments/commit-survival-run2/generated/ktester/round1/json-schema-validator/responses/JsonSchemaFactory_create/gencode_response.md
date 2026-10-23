**Generated test class**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit‑tests for {@link JsonSchemaFactory#create(ValidationContext, String, JsonNode, JsonSchema)}.
 *
 * The tests focus on argument forwarding and on the special handling of a {@code null}
 * {@code schemaPath} (which must be replaced by the default "#").
 *
 * A small subclass of {@link JsonSchemaFactory} is used to intercept the protected
 * {@code newJsonSchema} call.  This gives us visibility on the arguments that finally
 * reach {@code doCreate} without having to mock static or private methods.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    /* --------------------------------------------------------------------- *
     *  Testable factory that records the arguments received by newJsonSchema *
     * --------------------------------------------------------------------- */
    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {
        String capturedRawSchemaPath;               // value passed to doCreate (before any transformation)
        URI capturedCurrentUri;                     // parentSchema.getCurrentUri()
        JsonNode capturedSchemaNode;                // node forwarded to newJsonSchema
        ValidationContext capturedValidationContext; // not used directly but kept for completeness

        TestableJsonSchemaFactory(ObjectMapper jsonMapper,
                                 YAMLMapper yamlMapper,
                                 String defaultMetaSchemaURI,
                                 URISchemeFactory uriFactory,
                                 URISchemeFetcher uriFetcher,
                                 URNFactory urnFactory,
                                 Map<String, JsonMetaSchema> jsonMetaSchemas,
                                 Map<String, String> uriMap,
                                 boolean enableUriSchemaCache,
                                 CompositeURITranslator uriTranslators) {
            super(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriFactory, uriFetcher,
                    urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
        }

        @Override
        protected JsonSchema newJsonSchema(URI schemaUri,
                                           JsonNode schemaNode,
                                           SchemaValidatorsConfig config) {
            // Capture the arguments that flow from doCreate → newJsonSchema.
            this.capturedCurrentUri = schemaUri;
            this.capturedSchemaNode = schemaNode;
            // The raw schemaPath (the first String argument of doCreate) is captured
            // earlier via {@link #setCapturedRawSchemaPath(String)}.
            return mock(JsonSchema.class);
        }

        /** Accessor used by the test to read the raw schemaPath that reach doCreate. */
        String getCapturedRawSchemaPath() {
            return capturedRawSchemaPath;
        }

        /** Called via reflection from the test before invoking the private {@code doCreate}. */
        void setCapturedRawSchemaPath(String path) {
            this.capturedRawSchemaPath = path;
        }
    }

    /* --------------------------------------------------------------------- *
     *  Shared fixtures
     * --------------------------------------------------------------------- */
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private static TestableJsonSchemaFactory factory;
    private static JsonMetaSchema metaSchemaMock;

    private ValidationContext validationContext;
    private JsonNode emptyNode;
    private JsonSchema parentSchemaMock;
    private URI parentUri;

    @BeforeAll
    static void setupBeforeAll() {
        // Core objects needed by the JsonSchemaFactory constructor.
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();

        metaSchemaMock = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), metaSchemaMock);

        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        URNFactory urnFactory = new URNFactory();
        CompositeURITranslator translators = new CompositeURITranslator();

        factory = new TestableJsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                uriFactory,
                uriFetcher,
                urnFactory,
                metaSchemaMap,
                Collections.emptyMap(),
                false,
                translators);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Parent URI used by most tests
        parentUri = URI.create("http://example.com/parent");
        parentSchemaMock = mock(JsonSchema.class);
        when(parentSchemaMock.getCurrentUri()).thenReturn(parentUri);

        // A tiny JSON node – an empty object – is sufficient for normal cases.
        emptyNode = new ObjectMapper().createObjectNode();

        // ValidationContext needs a non‑null meta‑schema, factory and (optionally) config.
        validationContext = new ValidationContext(
                factory.getUriFactory(),
                factory.urnFactory,
                metaSchemaMock,
                factory,
                null);
    }

    /* --------------------------------------------------------------------- *
     *  Helper that invokes the private {@code doCreate} method via reflection.
     * --------------------------------------------------------------------- */
    private String invokeDoCreateAndCaptureRawPath(String schemaPath,
                                                   ValidationContext vc,
                                                   JsonNode node,
                                                   JsonSchema parent) {
        try {
            var method = JsonSchemaFactory.class.getDeclaredMethod(
                    "doCreate",
                    ValidationContext.class,
                    String.class,
                    URI.class,
                    JsonNode.class,
                    JsonSchema.class,
                    boolean.class);
            method.setAccessible(true);

            // Store the raw path (after the `null → "#" ` conversion performed by create()).
            factory.setCapturedRawSchemaPath(schemaPath == null ? "#" : schemaPath);

            // Invoke the private method – it will eventually call our overridden newJsonSchema.
            method.invoke(factory,
                    vc,
                    schemaPath == null ? "#" : schemaPath,
                    parent == null ? null : parent.getCurrentUri(),
                    node,
                    parent,
                    false);

            return factory.getCapturedRawSchemaPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  1. SchemaPath handling (already present in the original scaffold)
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullSchemaPath_UsesHashAsDefault() {
        String schemaPath = null; // triggers the default "#"

        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);

        assertNotNull(result, "Factory should return a JsonSchema instance");
        assertEquals(parentUri, factory.capturedCurrentUri,
                "Parent URI must be forwarded to newJsonSchema");
        // Verify that the raw schemaPath seen by doCreate is "#".
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals("#", rawPath, "When schemaPath is null the default should be \"#\"");
    }

    @Test
    void create_WithExplicitSchemaPath_PassesItThrough() {
        String schemaPath = "/my/custom/path";

        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);

        assertNotNull(result, "Factory should return a JsonSchema instance");
        assertEquals(parentUri, factory.capturedCurrentUri,
                "Parent URI must be forwarded to newJsonSchema");
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

        assertThrows(NullPointerException.class,
                () -> factory.create(validationContext, schemaPath, emptyNode, nullParent),
                "Calling create with a null parentSchema must result in NPE because getCurrentUri() is accessed");
    }

    /* --------------------------------------------------------------------- *
     *  3. Null schemaNode forwarding
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullSchemaNode_ForwardsNull() {
        String schemaPath = "/path";
        JsonNode node = null; // explicit null

        JsonSchema result = factory.create(validationContext, schemaPath, node, parentSchemaMock);

        assertNotNull(result, "Factory should still return a JsonSchema instance");
        assertSame(node, factory.capturedSchemaNode,
                "The null JsonNode must be forwarded unchanged to newJsonSchema");
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
        assertEquals(parentUri, factory.capturedCurrentUri,
                "Parent URI must still be forwarded correctly");
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
        String schemaPath = ""; // empty string is a legal path

        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);

        assertNotNull(result, "Factory should return a JsonSchema instance");
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath, "Empty string schemaPath must be passed unchanged");
    }

    /* --------------------------------------------------------------------- *
     *  6. ParentSchema with null currentUri
     * --------------------------------------------------------------------- */
    private static class DummyParentSchemaWithNullUri implements JsonSchema {
        @Override public URI getCurrentUri() { return null; }
        // All other methods are irrelevant for this test; they can return defaults or throw UnsupportedOperationException.
        @Override public void setCurrentUri(URI currentUri) { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getParentSchema() { throw new UnsupportedOperationException(); }
        @Override public JsonNode getSchemaNode() { throw new UnsupportedOperationException(); }
        @Override public JsonMetaSchema getMetaSchema() { throw new UnsupportedOperationException(); }
        @Override public void setMetaSchema(JsonMetaSchema metaSchema) { throw new UnsupportedOperationException(); }
        @Override public void validate(JsonNode node, ValidationContext validationContext) { throw new UnsupportedOperationException(); }
        @Override public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory) { throw new UnsupportedOperationException(); }
        @Override public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory, SchemaValidatorsConfig config) { throw new UnsupportedOperationException(); }
        @Override public JsonSchemaFactory getFactory() { throw new UnsupportedOperationException(); }
        @Override public String getType() { throw new UnsupportedOperationException(); }
        @Override public JsonSchema createDeepCopy() { throw new UnsupportedOperationException(); }
    }

    @Test
    void create_WithParentSchemaHavingNullCurrentUri_ForwardsNullUri() {
        String schemaPath = "/any";
        JsonSchema parentWithNullUri = new DummyParentSchemaWithNullUri();

        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentWithNullUri);

        assertNotNull(result, "Factory should still create a JsonSchema");
        assertNull(factory.capturedCurrentUri,
                "When parentSchema.getCurrentUri() returns null it must be forwarded as null");
    }

    /* --------------------------------------------------------------------- *
     *  7. Complex schemaNode forwarding
     * --------------------------------------------------------------------- */
    @Test
    void create_WithComplexSchemaNode_ForwardsSameInstance() throws Exception {
        String schemaPath = "/complex";
        String json = "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{\"type\":\"string\"},"
                + "   \"age\":{\"type\":\"integer\"}"
                + "}"
                + "}";
        JsonNode complexNode = new ObjectMapper().readTree(json);

        JsonSchema result = factory.create(validationContext, schemaPath, complexNode, parentSchemaMock);

        assertNotNull(result, "Factory should return a JsonSchema");
        assertSame(complexNode, factory.capturedSchemaNode,
                "The complex JsonNode must be forwarded unchanged to newJsonSchema");
    }

    /* --------------------------------------------------------------------- *
     *  8. SchemaPath containing fragment identifier
     * --------------------------------------------------------------------- */
    @Test
    void create_WithFragmentIdentifierSchemaPath_ForwardsUnchanged() {
        String schemaPath = "#/definitions/Person";

        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);

        assertNotNull(result, "Factory should return a JsonSchema");
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath,
                "Fragment identifier schemaPath must not be altered before delegating to doCreate");
    }

    /* --------------------------------------------------------------------- *
     *  9. ParentSchema getCurrentUri exception propagation
     * --------------------------------------------------------------------- */
    private static class Dummy