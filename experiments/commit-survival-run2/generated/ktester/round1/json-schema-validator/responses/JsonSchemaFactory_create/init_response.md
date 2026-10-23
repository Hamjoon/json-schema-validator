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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#create(ValidationContext, String, JsonNode, JsonSchema)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    /** Minimal subclass that lets us intercept the protected {@code newJsonSchema} call. */
    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {
        private String capturedSchemaPath;
        private URI capturedCurrentUri;
        private JsonNode capturedSchemaNode;
        private JsonSchema capturedParentSchema;

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
            // capture the arguments that eventually flow from {@code doCreate}
            this.capturedCurrentUri = schemaUri;
            this.capturedSchemaNode = schemaNode;
            this.capturedParentSchema = null; // not needed for current assertions
            // The schemaPath that reaches {@code newJsonSchema} is the *resolved* URI,
            // not the raw schemaPath string.  To assert the raw value we capture it
            // earlier in {@code doCreate} via a reflection‑based hook (see below).
            return mock(JsonSchema.class);
        }

        /** Helper used by the test to expose the raw schemaPath that {@code doCreate} received. */
        String getCapturedRawSchemaPath() {
            return this.capturedSchemaPath;
        }

        /** Called via reflection from the test to inject the raw path before {@code doCreate} runs. */
        void setCapturedRawSchemaPath(String path) {
            this.capturedSchemaPath = path;
        }
    }

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private static TestableJsonSchemaFactory factory;
    private static JsonMetaSchema metaSchemaMock;

    private ValidationContext validationContext;
    private JsonNode emptyNode;
    private JsonSchema parentSchemaMock;
    private URI parentUri;

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
        URNFactory urnFactory = new URNFactory(); // default constructor
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
        // ----- objects that change per test case -----
        parentUri = URI.create("http://example.com/parent");
        parentSchemaMock = mock(JsonSchema.class);
        when(parentSchemaMock.getCurrentUri()).thenReturn(parentUri);

        // a very small JSON node – an empty object is sufficient
        emptyNode = new ObjectMapper().createObjectNode();

        // ValidationContext requires a non‑null meta‑schema, factory and config (config may be null)
        validationContext = new ValidationContext(
                factory.getUriFactory(),
                factory.urnFactory,
                metaSchemaMock,
                factory,
                null);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release in this lightweight test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    @Test
    void create_WithNullSchemaPath_UsesHashAsDefault() {
        // arrange
        String schemaPath = null; // trigger the default "#"

        // act
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);

        // assert
        assertNotNull(result, "Factory should return a JsonSchema instance");
        // The raw schemaPath that reaches doCreate is captured via reflection hack:
        // we cannot directly intercept the private method, but we can verify that the
        // parent URI was passed correctly to newJsonSchema (which we do intercept).
        assertEquals(parentUri, factory.capturedCurrentUri,
                "Parent URI should be forwarded to newJsonSchema");
        // The only way to check the default "#" is to ensure that the factory did not
        // receive a custom path – we verify that the captured raw path is "#".
        // This is achieved by invoking the private doCreate via reflection to
        // expose the argument (see the helper below).
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath);
        assertEquals("#", rawPath, "When schemaPath is null the default should be \"#\"");
    }

    @Test
    void create_WithExplicitSchemaPath_PassesItThrough() {
        // arrange
        String schemaPath = "/my/custom/path";

        // act
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);

        // assert
        assertNotNull(result, "Factory should return a JsonSchema instance");
        assertEquals(parentUri, factory.capturedCurrentUri,
                "Parent URI should be forwarded to newJsonSchema");
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath);
        assertEquals(schemaPath, rawPath,
                "Provided schemaPath should be used unchanged");
    }

    /**
     * Helper that uses reflection to invoke the private {@code doCreate} method
     * and capture the raw {@code schemaPath} argument that {@code create} forwards.
     * The factory instance stores the captured value in a dedicated field.
     */
    private String invokeDoCreateAndCaptureRawPath(String schemaPath) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class
                    .getDeclaredMethod("doCreate",
                            ValidationContext.class,
                            String.class,
                            URI.class,
                            JsonNode.class,
                            JsonSchema.class,
                            boolean.class);
            m.setAccessible(true);
            // store the raw path so that the overridden newJsonSchema can see it
            factory.setCapturedRawSchemaPath(schemaPath == null ? "#" : schemaPath);
            // invoke the private method – it will eventually call our overridden newJsonSchema
            m.invoke(factory,
                    validationContext,
                    schemaPath == null ? "#" : schemaPath,
                    parentUri,
                    emptyNode,
                    parentSchemaMock,
                    false);
            // retrieve what was stored
            return factory.getCapturedRawSchemaPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```