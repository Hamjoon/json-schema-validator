package com.networknt.schema;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.urn.URNFactory;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests are written by three “experts” that each contributed a set of
 * test cases covering typical usage, edge cases and error scenarios.
 *
 * All objects are created through the public constructors / factory methods
 * that appear in the source code – no mocking or external libraries are used.
 */
public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factory;

    /**
     * Simple in‑memory URI factory / fetcher used for tests that need a
     * deterministic source without touching the real class‑path or network.
     */
    private static class MemURIFactory implements URIFactory {
        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }

        @Override
        public URI create(URI baseURI, String segment) {
            return baseURI.resolve(segment);
        }
    }

    private static class MemURIFetcher implements URIFetcher {
        private final Map<String, String> content = new HashMap<>();

        void put(String uri, String json) {
            content.put(uri, json);
        }

        @Override
        public InputStream fetch(URI uri) {
            String s = content.get(uri.toString());
            if (s == null) {
                throw new IllegalArgumentException("No content for " + uri);
            }
            return new ByteArrayInputStream(s.getBytes());
        }
    }

    /**
     * Helper subclass exposing protected methods for direct unit testing.
     */
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
        protected SchemaLocation getSchemaLocation(URI schemaRetrievalUri, JsonNode schemaNode,
                                                   ValidationContext validationContext) {
            return super.getSchemaLocation(schemaRetrievalUri, schemaNode, validationContext);
        }

        @Override
        protected ValidationContext createValidationContext(JsonNode schemaNode) {
            return super.createValidationContext(schemaNode);
        }

        @Override
        protected JsonMetaSchema findMetaSchemaForSchema(JsonNode schemaNode) {
            return super.findMetaSchemaForSchema(schemaNode);
        }

        @Override
        protected JsonMetaSchema fromId(String id) {
            return super.fromId(id);
        }

        @Override
        protected boolean idMatchesSourceUri(JsonMetaSchema metaSchema, JsonNode schema, URI schemaUri) {
            return super.idMatchesSourceUri(metaSchema, schema, schemaUri);
        }

        @Override
        protected boolean isYaml(URI schemaUri) {
            return super.isYaml(schemaUri);
        }

        public static String normalizeMetaSchemaUriPublic(String u) {
            return JsonSchemaFactory.normalizeMetaSchemaUri(u);
        }
    }

    @Before
    public void setUp() {
        // Build a factory that knows about V4 meta‑schema and also supports a
        // custom “mem:” scheme for deterministic testing.
        MemURIFactory memFactory = new MemURIFactory();
        MemURIFetcher memFetcher = new MemURIFetcher();

        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .uriFactory(memFactory, "mem")
                .uriFetcher(memFetcher, "mem")
                .enableUriSchemaCache(true) // we want to test caching
                .build();
    }

    /* --------------------------------------------------------------------- */
    /* 1. Builder and basic construction validation                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void builderCreatesFactoryWithDefaultMetaSchema() {
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV7().getUri())
                .addMetaSchema(JsonMetaSchema.getV7())
                .build();

        assertNotNull(f);
        // The factory must contain the V7 meta‑schema under the normalized key
        String normalized = TestableJsonSchemaFactory.normalizeMetaSchemaUriPublic(JsonMetaSchema.getV7().getUri());
        assertTrue(f.jsonMetaSchemas.containsKey(normalized));
    }

    @Test
    public void builderRejectsNullObjectMapper() {
        try {
            new JsonSchemaFactory.Builder()
                    .objectMapper(null) // will be overridden later with null
                    .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                    .addMetaSchema(JsonMetaSchema.getV4())
                    .build();
            fail("Expected IllegalArgumentException because ObjectMapper is null");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ObjectMapper must not be null"));
        }
    }

    @Test
    public void constructorRejectsEmptyDefaultMetaSchemaUri() {
        try {
            new JsonSchemaFactory.Builder()
                    .defaultMetaSchemaURI("   ")
                    .addMetaSchema(JsonMetaSchema.getV4())
                    .build();
            fail("Expected IllegalArgumentException for empty defaultMetaSchemaURI");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("defaultMetaSchemaURI must not be null or empty"));
        }
    }

    @Test
    public void checkVersionHandlesNullAndUnsupported() {
        assertNull(JsonSchemaFactory.checkVersion(null));

        try {
            JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("UNKNOWN"));
            fail("Expected IllegalArgumentException for unsupported version");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /* 2. normalizeMetaSchemaUri – typical and error cases                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void normalizeMetaSchemaUriConvertsToHttps() {
        String httpUri = "http://example.com/schema";
        String normalized = TestableJsonSchemaFactory.normalizeMetaSchemaUriPublic(httpUri);
        assertTrue(normalized.startsWith("https://"));
        assertFalse(normalized.contains("?"));
    }

    @Test
    public void normalizeMetaSchemaUriRejectsInvalidUri() {
        try {
            TestableJsonSchemaFactory.normalizeMetaSchemaUriPublic("ht!tp://bad uri");
            fail("Expected JsonSchemaException for malformed URI");
        } catch (JsonSchemaException e) {
            assertTrue(e.getMessage().contains("Wrong MetaSchema URI"));
        }
    }

    /* --------------------------------------------------------------------- */
    /* 3. getSchema(String) – success and failure                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromStringParsesJsonSuccessfully() {
        String json = "{\"type\":\"string\"}";
        JsonSchema schema = factory.getSchema(json);
        assertNotNull(schema);
        // The schema should have a type validator (V4 meta‑schema defines it)
        assertTrue(schema.hasTypeValidator());
    }

    @Test
    public void getSchemaFromStringThrowsOnInvalidJson() {
        String badJson = "{invalid json}";
        try {
            factory.getSchema(badJson);
            fail("Expected JsonSchemaException due to parsing error");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /* 4. getSchema(URI) – loading, caching and yaml detection               */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromMemUriLoadsJsonAndCachesResult() {
        String uriStr = "mem://test-schema.json";
        String schemaContent = "{\"type\":\"integer\"}";
        // put the content into the fetcher that the factory uses
        ((MemURIFetcher) ((URISchemeFetcher) factory.uriFetcher).getURIFetchers().get("mem")).put(uriStr, schemaContent);

        URI uri = URI.create(uriStr);
        JsonSchema first = factory.getSchema(uri);
        JsonSchema second = factory.getSchema(uri); // should be cached

        assertNotNull(first);
        assertNotNull(second);
        // The cached instance must be the same object (factory caches by URI)
        assertSame(first, second);
    }

    @Test
    public void getSchemaFromMemUriDetectsYamlExtension() {
        String uriStr = "mem://test-schema.yaml";
        String yamlContent = "type: string";
        ((MemURIFetcher) ((URISchemeFetcher) factory.uriFetcher).getURIFetchers().get("mem")).put(uriStr, yamlContent);

        URI uri = URI.create(uriStr);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        // YAML parser also creates a type validator
        assertTrue(schema.hasTypeValidator());
    }

    @Test
    public void getSchemaFromUriThrowsWhenFetcherFails() {
        String uriStr = "mem://non‑existent.json";
        URI uri = URI.create(uriStr);
        try {
            factory.getSchema(uri);
            fail("Expected JsonSchemaException because fetcher cannot find resource");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /* 5. idMatchesSourceUri – positive and negative cases                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void idMatchesSourceUriReturnsTrueWhenIdEqualsUri() throws Exception {
        // Build a tiny schema node containing "$id"
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$id\":\"mem://my-schema.json\",\"type\":\"null\"}");

        // Use the V4 meta‑schema (it knows the "$id" keyword)
        JsonMetaSchema meta = JsonMetaSchema.getV4();
        URI uri = URI.create("mem://my-schema.json");

        boolean matches = factory.idMatchesSourceUri(meta, node, uri);
        assertTrue(matches);
    }

    @Test
    public void idMatchesSourceUriReturnsFalseWhenIdDiffers() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$id\":\"mem://other-schema.json\"}");

        JsonMetaSchema meta = JsonMetaSchema.getV4();
        URI uri = URI.create("mem://my-schema.json");

        boolean matches = factory.idMatchesSourceUri(meta, node, uri);
        assertFalse(matches);
    }

    /* --------------------------------------------------------------------- */
    /* 6. getSchemaLocation – $id handling                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaLocationPrefersIdOverRetrievalUri() throws Exception {
        // Schema with $id
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$id\":\"mem://my-schema.json\"}");

        ValidationContext ctx = factory.createValidationContext(node);
        SchemaLocation loc = factory.getSchemaLocation(null, node, ctx);
        assertEquals("mem://my-schema.json", loc.toString());
    }

    @Test
    public void getSchemaLocationUsesRetrievalUriWhenNoId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");
        ValidationContext ctx = factory.createValidationContext(node);
        URI retrieval = URI.create("mem://fallback.json");
        SchemaLocation loc = factory.getSchemaLocation(retrieval, node, ctx);
        assertEquals("mem://fallback.json", loc.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 7. createValidationContext – meta‑schema resolution                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void createValidationContextSelectsDefaultMetaSchemaWhenNo$schema() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");
        ValidationContext ctx = factory.createValidationContext(node);
        assertEquals(JsonMetaSchema.getV4().getUri(),
                ctx.getMetaSchema().getUri());
    }

    @Test
    public void createValidationContextSelectsExplicitMetaSchema() throws Exception {
        String metaUri = JsonMetaSchema.getV7().getUri();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$schema\":\"" + metaUri + "\"}");

        // Build a factory that knows V7 as well
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .addMetaSchema(JsonMetaSchema.getV7())
                .build();

        ValidationContext ctx = f.createValidationContext(node);
        assertEquals(metaUri, ctx.getMetaSchema().getUri());
    }

    @Test
    public void createValidationContextThrowsOnUnknownMetaSchema() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$schema\":\"http://unknown/schema\"}");

        try {
            factory.createValidationContext(node);
            fail("Expected JsonSchemaException for unknown meta‑schema");
        } catch (JsonSchemaException e) {
            assertTrue(e.getMessage().contains("Unknown MetaSchema"));
        }
    }

    /* --------------------------------------------------------------------- */
    /* 8. fromId – detection of well‑known dialects                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void fromIdResolvesKnownDialect() {
        // V202012 is a known dialect identifier
        JsonMetaSchema meta = factory.fromId(JsonMetaSchema.getV202012().getUri());
        assertEquals(JsonMetaSchema.getV202012().getUri(), meta.getUri());
    }

    @Test
    public void fromIdThrowsWhenDialectIsUnknown() {
        try {
            factory.fromId("http://example.com/unknown-dialect");
            fail("Expected JsonSchemaException for unknown dialect");
        } catch (JsonSchemaException e) {
            assertTrue(e.getMessage().contains("Unknown MetaSchema"));
        }
    }

    /* --------------------------------------------------------------------- */
    /* 9. getUriFactory / getUriTranslator – basic sanity checks            */
    /* --------------------------------------------------------------------- */

    @Test
    public void getUriFactoryReturnsConfiguredFactory() {
        URIFactory uf = factory.getUriFactory();
        assertNotNull(uf);
        // The factory knows the "mem" scheme we added
        URI created = uf.create("mem://sample");
        assertEquals("mem://sample", created.toString());
    }

    @Test
    public void getUriTranslatorAppliesMappings() {
        // Add a mapping via the config
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.getUriMappings().put("mem://old", "mem://new");

        // Build a factory with the mapping
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .addUriMappings(cfg.getUriMappings())
                .uriFactory(new MemURIFactory(), "mem")
                .uriFetcher(new MemURIFetcher(), "mem")
                .build();

        URITranslator translator = f.getUriTranslator();
        URI translated = translator.translate(URI.create("mem://old"));
        assertEquals("mem://new", translated.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 10. isYaml – extension detection                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void isYamlDetectsYamlExtensions() {
        assertTrue(factory.isYaml(URI.create("mem://file.yaml")));
        assertTrue(factory.isYaml(URI.create("mem://file.yml")));
        assertFalse(factory.isYaml(URI.create("mem://file.json")));
        assertFalse(factory.isYaml(URI.create("mem://noextension")));
    }

    /* --------------------------------------------------------------------- */
    /* 11. Builder(blueprint) – cloning works                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void builderFromBlueprintCopiesAllSettings() {
        JsonSchemaFactory clone = JsonSchemaFactory.builder(factory).build();

        // The clone must contain the same meta‑schemas and default URI
        assertEquals(factory.defaultMetaSchemaURI, clone.defaultMetaSchemaURI);
        assertEquals(factory.jsonMetaSchemas.keySet(), clone.jsonMetaSchemas.keySet());

        // The URI factory must still create the custom "mem" scheme
        assertEquals("mem://test", clone.getUriFactory().create("mem://test").toString());
    }

    /* --------------------------------------------------------------------- */
    /* 12. getSchema(URI, JsonNode, Config) – direct node creation          */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaWithExplicitNodeCreatesSchemaWithoutFetching() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\":\"boolean\"}");

        JsonSchema schema = factory.getSchema(URI.create("mem://inline.json"), node, new SchemaValidatorsConfig());
        assertNotNull(schema);
        assertTrue(schema.hasTypeValidator());
        // The schema's current URI must be the supplied one
        assertEquals(URI.create("mem://inline.json"), schema.getCurrentUri());
    }

    /* --------------------------------------------------------------------- */
    /* 13. getSchema(InputStream) – stream handling                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromInputStreamParsesJson() {
        String json = "{\"type\":\"number\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes());

        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
        assertTrue(schema.hasTypeValidator());
    }

    @Test
    public void getSchemaFromInputStreamThrowsOnInvalidJson() {
        String bad = "{ not json }";
        InputStream is = new ByteArrayInputStream(bad.getBytes());

        try {
            factory.getSchema(is);
            fail("Expected JsonSchemaException for malformed JSON");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /* 14. getSchema(String, Config) – config propagation test               */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaWithConfigPropagatesConfigToSchema() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);

        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}", cfg);
        assertNotNull(schema);
        // The internal ValidationContext should hold the same config
        assertTrue(schema.getValidationContext().getConfig().isFailFast());
    }

    /* --------------------------------------------------------------------- */
    /* 15. getSchema(URI) – default config creation                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaUriUsesDefaultConfigWhenNoneProvided() {
        // Prepare a simple schema in the mem fetcher
        String uriStr = "mem://default-config.json";
        ((MemURIFetcher) ((URISchemeFetcher) factory.uriFetcher).getURIFetchers().get("mem"))
                .put(uriStr, "{\"type\":\"null\"}");

        JsonSchema schema = factory.getSchema(URI.create(uriStr));
        assertNotNull(schema);
        // Default config should be non‑null
        assertNotNull(schema.getValidationContext().getConfig());
    }
}
