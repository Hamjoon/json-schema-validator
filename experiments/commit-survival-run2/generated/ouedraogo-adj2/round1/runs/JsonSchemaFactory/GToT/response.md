###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public factory methods and builder configuration.</li>
 *   <li>Normal operation of the various {@code getSchema(...)} overloads.</li>
 *   <li>Edge‑cases and error handling (null arguments, malformed JSON/YAML, unknown meta‑schema, etc.).</li>
 *   <li>Internal behaviours such as URI‑caching, YAML detection, meta‑schema normalisation and
 *       private helper methods via reflection.</li>
 * </ul>
 */
public class JsonSchemaFactoryTest {

    private static final String SIMPLE_JSON_SCHEMA = "{\"type\":\"object\"}";
    private static final String INVALID_JSON = "{type:object}";
    private static final String SIMPLE_YAML_SCHEMA = "type: object";

    private JsonSchemaFactory factory;

    @Before
    public void setUp() {
        // Build a minimal factory that knows about Draft‑V4 meta‑schema.
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
    }

    /* --------------------------------------------------------------------- */
    /*  Builder & static factory methods                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
        assertNotNull(f);
    }

    @Test
    public void testGetInstanceDefault() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(f);
    }

    @Test
    public void testCheckVersionNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testNormalizeMetaSchemaUri() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, "http://example.com/schema");
        assertEquals("https://example.com/schema", result);
    }

    @Test
    public void testGetUriFactoryNotNull() {
        assertNotNull(factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslatorNotNull() {
        assertNotNull(factory.getUriTranslator());
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(String) overloads                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromString() {
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidString() {
        factory.getSchema(INVALID_JSON);
    }

    @Test
    public void testGetSchemaFromStringWithConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA, cfg);
        assertNotNull(schema);
        // config should be attached to the validation context
        assertSame(cfg, schema.getValidationContext().getConfig());
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(InputStream) overloads                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromInputStream() {
        InputStream is = new ByteArrayInputStream(SIMPLE_JSON_SCHEMA.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidInputStream() {
        InputStream is = new ByteArrayInputStream(INVALID_JSON.getBytes(StandardCharsets.UTF_8));
        factory.getSchema(is);
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(URI) overloads – including caching and YAML handling      */
    /* --------------------------------------------------------------------- */

    /**
     * Simple {@link URIFactory} implementation used in the tests.
     */
    private static class TestURIFactory implements URIFactory {
        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }

        @Override
        public URI create(URI baseURI, String segment) {
            return baseURI.resolve(segment);
        }
    }

    /**
     * Simple {@link URIFetcher} that returns pre‑configured content.
     */
    private static class TestURIFetcher implements URIFetcher {
        private final Map<URI, String> content;
        private int fetchCount = 0;

        TestURIFetcher(Map<URI, String> content) {
            this.content = content;
        }

        @Override
        public InputStream fetch(URI uri) throws IOException {
            fetchCount++;
            String data = content.get(uri);
            if (data == null) {
                throw new IOException("Resource not found: " + uri);
            }
            return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        }

        int getFetchCount() {
            return fetchCount;
        }
    }

    @Test
    public void testGetSchemaFromUriJson() throws Exception {
        URI uri = URI.create("test://schema.json");
        Map<URI, String> map = Collections.singletonMap(uri, SIMPLE_JSON_SCHEMA);
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .uriFactory(new TestURIFactory(), "test")
                .uriFetcher(new TestURIFetcher(map), "test")
                .enableUriSchemaCache(false)
                .build();

        JsonSchema schema = f.getSchema(uri);
        assertNotNull(schema);
        assertEquals(uri, schema.getCurrentUri());
    }

    @Test
    public void testGetSchemaFromUriYamlDetection() throws Exception {
        URI uri = URI.create("test://schema.yaml");
        Map<URI, String> map = Collections.singletonMap(uri, SIMPLE_YAML_SCHEMA);
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .uriFactory(new TestURIFactory(), "test")
                .uriFetcher(new TestURIFetcher(map), "test")
                .enableUriSchemaCache(false)
                .build();

        // No exception means the YAML parser was used successfully.
        JsonSchema schema = f.getSchema(uri);
        assertNotNull(schema);
        // Verify that the underlying node contains the expected "type" property.
        JsonNode node = schema.getValidationContext().getJsonMetaSchema().getNodeByFragmentRef("/", schema.getValidationContext().getSchemaNode());
        assertEquals("object", node.get("type").asText());
    }

    @Test
    public void testUriSchemaCacheBehaviour() throws Exception {
        URI uri = URI.create("test://cached.json");
        Map<URI, String> map = Collections.singletonMap(uri, SIMPLE_JSON_SCHEMA);
        TestURIFetcher fetcher = new TestURIFetcher(map);

        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .uriFactory(new TestURIFactory(), "test")
                .uriFetcher(fetcher, "test")
                .enableUriSchemaCache(true)
                .build();

        JsonSchema first = f.getSchema(uri);
        JsonSchema second = f.getSchema(uri);

        // The same instance should be returned from the cache.
        assertSame(first, second);
        // Fetcher should have been invoked only once.
        assertEquals(1, fetcher.getFetchCount());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromUriUnsupportedScheme() {
        // No factory/fetcher registered for the "unknown" scheme → exception.
        factory.getSchema(URI.create("unknown://schema.json"));
    }

    /* --------------------------------------------------------------------- */
    /*  Private / protected helper methods (via reflection)                 */
    /* --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testFindMetaSchemaForSchemaWithNonTextual$schema() throws Exception {
        // Build a node where "$schema" is a number.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$schema\":123}");
        Method findMeta = JsonSchemaFactory.class.getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        findMeta.setAccessible(true);
        findMeta.invoke(factory, node);
    }

    @Test(expected = JsonSchemaException.class)
    public void testFromIdUnknownMetaSchema() throws Exception {
        Method fromId = JsonSchemaFactory.class.getDeclaredMethod("fromId", String.class);
        fromId.setAccessible(true);
        fromId.invoke(factory, "http://unknown/meta/schema");
    }

    @Test
    public void testIdMatchesSourceUriTrue() throws Exception {
        // Schema with "$id" matching the provided URI.
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"$id\":\"test://my-schema.json\",\"type\":\"object\"}";
        JsonNode node = mapper.readTree(json);
        URI uri = URI.create("test://my-schema.json");

        // Use the real meta‑schema (draft‑V4) – its readId method recognises "$id".
        JsonMetaSchema meta = JsonMetaSchema.getV4();

        Method method = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(factory, meta, node, uri);
        assertTrue(result);
    }

    @Test
    public void testIdMatchesSourceUriFalse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"$id\":\"test://other-schema.json\",\"type\":\"object\"}";
        JsonNode node = mapper.readTree(json);
        URI uri = URI.create("test://my-schema.json");
        JsonMetaSchema meta = JsonMetaSchema.getV4();

        Method method = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(factory, meta, node, uri);
        assertFalse(result);
    }

    @Test
    public void testIsYamlDetection() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        assertTrue((boolean) isYaml.invoke(factory, URI.create("file://schema.yaml")));
        assertTrue((boolean) isYaml.invoke(factory, URI.create("file://schema.yml")));
        assertFalse((boolean) isYaml.invoke(factory, URI.create("file://schema.json")));
        assertFalse((boolean) isYaml.invoke(factory, URI.create("file://noextension")));
    }

    /* --------------------------------------------------------------------- */
    /*  Validation of the protected newJsonSchema method via subclassing    */
    /* --------------------------------------------------------------------- */

    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {
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
        public JsonSchema newJsonSchema(URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig config) {
            return super.newJsonSchema(schemaUri, schemaNode, config);
        }
    }

    @Test
    public void testProtectedNewJsonSchema() throws Exception {
        TestableJsonSchemaFactory f = new TestableJsonSchemaFactory(
                new ObjectMapper(),
                new YAMLMapper(),
                JsonMetaSchema.getV4().getUri(),
                new URISchemeFactory(Collections.emptyMap()),
                new URISchemeFetcher(Collections.emptyMap()),
                null,
                Collections.singletonMap(JsonMetaSchema.getV4().getUri(), JsonMetaSchema.getV4()),
                Collections.emptyMap(),
                false,
                new CompositeURITranslator()
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(SIMPLE_JSON_SCHEMA);
        JsonSchema schema = f.newJsonSchema(null, node, null);
        assertNotNull(schema);
    }
}
```
###Test END##