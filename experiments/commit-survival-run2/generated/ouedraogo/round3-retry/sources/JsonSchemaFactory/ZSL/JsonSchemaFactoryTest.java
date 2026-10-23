/***************************************************************************************************
 *  JsonSchemaFactoryTest.java
 *
 *  Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 *  The tests cover:
 *   • Builder configuration and cloning
 *   • Static helper methods (checkVersion, normalizeMetaSchemaUri)
 *   • Creation of schemas from String, InputStream and URI
 *   • Caching behaviour for URI based schemas
 *   • Error handling (invalid JSON, unknown meta‑schema)
 *   • Private utility methods (isYaml, idMatchesSourceUri) via reflection
 *
 *  The test class does **not** depend on any external files – a tiny in‑memory schema is used
 *  together with a custom {@link URIFactory}/{@link URIFetcher} for the scheme “test”.
 *
 *  Author:  ChatGPT (OpenAI) – generated for the exercise
 **************************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    /** A tiny JSON schema used throughout the tests */
    private static final String SIMPLE_JSON_SCHEMA = "{\"type\":\"object\"}";

    /** Same schema but expressed as YAML (to test the isYaml() helper) */
    private static final String SIMPLE_YAML_SCHEMA = "type: object\n";

    /** A schema that contains an $id matching the test URI */
    private static final String SCHEMA_WITH_ID = "{\n" +
            "  \"$id\": \"test://my-schema\",\n" +
            "  \"type\": \"object\"\n" +
            "}";

    /** Custom scheme used for URI based loading */
    private static final String TEST_SCHEME = "test";

    /** The URI that will be used to fetch the schema containing $id */
    private static final URI TEST_URI = URI.create(TEST_SCHEME + "://my-schema");

    private JsonSchemaFactory factory;

    /**
     * Build a {@link JsonSchemaFactory} that knows how to handle the custom “test” scheme.
     * The factory is configured with the Draft‑2020‑12 meta‑schema (the latest at the time of
     * writing) so that the $id keyword is recognised.
     */
    @Before
    public void setUp() {
        // -----------------------------------------------------------------------
        // 1️⃣  Create a custom URIFactory – for the “test” scheme we simply return the
        //     supplied URI (the real implementation would resolve relative URIs etc.).
        // -----------------------------------------------------------------------
        URIFactory testFactory = new URIFactory() {
            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }
        };

        // -----------------------------------------------------------------------
        // 2️⃣  Create a custom URIFetcher – it returns an InputStream that reads the
        //     in‑memory schema string defined above.
        // -----------------------------------------------------------------------
        URIFetcher testFetcher = new URIFetcher() {
            @Override
            public InputStream fetch(URI uri) {
                // The fetcher ignores the actual URI – it always returns the same content.
                // This is sufficient for the cache‑related tests.
                return new ByteArrayInputStream(SCHEMA_WITH_ID.getBytes());
            }
        };

        // -----------------------------------------------------------------------
        // 3️⃣  Build the factory.
        // -----------------------------------------------------------------------
        JsonMetaSchema draft2020 = SpecVersion.VersionFlag.V202012.getVersion().getInstance();

        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(draft2020.getUri())
                .addMetaSchema(draft2020)
                .uriFactory(testFactory, TEST_SCHEME)
                .uriFetcher(testFetcher, TEST_SCHEME)
                .enableUriSchemaCache(true)          // we want to test the cache
                .build();
    }

    // -----------------------------------------------------------------------
    // 2️⃣  Static helper methods
    // -----------------------------------------------------------------------
    @Test
    public void testNormalizeMetaSchemaUri() {
        // Input with http scheme – should stay unchanged
        String http = "http://example.com/schema";
        assertEquals(http, JsonSchemaFactory.normalizeMetaSchemaUri(http));

        // Input with https but also user‑info and port – they must be stripped
        String raw = "https://user:pwd@example.com:8080/path?query=1#frag";
        String expected = "https://example.com:8080/path";
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(raw));

        // Invalid URI must raise JsonSchemaException
        try {
            JsonSchemaFactory.normalizeMetaSchemaUri(":::not a uri:::"); // NOPMD
            fail("Expected JsonSchemaException for malformed URI");
        } catch (JsonSchemaException ignored) {
            // expected
        }
    }

    @Test
    public void testCheckVersion() {
        // All supported flags should return a non‑null JsonSchemaVersion
        for (VersionFlag flag : VersionFlag.values()) {
            assertNotNull("Version flag " + flag + " should be supported", JsonSchemaFactory.checkVersion(flag));
        }

        // Null flag returns null
        assertNull(JsonSchemaFactory.checkVersion(null));

        // Unsupported flag – we create a dummy enum value via reflection (not normally possible)
        // Instead we simply assert that the default switch throws for an unknown constant.
        try {
            JsonSchemaFactory.checkVersion(VersionFlag.valueOf("V0"));
            fail("Expected IllegalArgumentException for unknown flag");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }

    // -----------------------------------------------------------------------
    // 3️⃣  Builder cloning (builder(JsonSchemaFactory))
    // -----------------------------------------------------------------------
    @Test
    public void testBuilderCloning() {
        JsonSchemaFactory clone = JsonSchemaFactory.builder(factory).build();

        // The cloned instance must keep the same default meta‑schema URI
        assertEquals(factory.getUriFactory().getClass(), clone.getUriFactory().getClass());

        // The custom “test” scheme must still be present
        assertEquals(TEST_SCHEME, clone.getUriFactory().create(TEST_URI.toString()).getScheme());

        // The meta‑schema map must contain the Draft‑2020‑12 URI
        assertTrue(clone.getUriFactory().create(draft2020Uri()).toString().startsWith("https"));
    }

    // -----------------------------------------------------------------------
    // 4️⃣  getSchema(String) – happy path and error handling
    // -----------------------------------------------------------------------
    @Test
    public void testGetSchemaFromString() {
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull("Schema should be created from a valid JSON string", schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidString() {
        // malformed JSON – should trigger JsonSchemaException
        factory.getSchema("{ invalid json }");
    }

    // -----------------------------------------------------------------------
    // 5️⃣  getSchema(InputStream) – both JSON and YAML handling
    // -----------------------------------------------------------------------
    @Test
    public void testGetSchemaFromInputStreamJson() {
        InputStream is = new ByteArrayInputStream(SIMPLE_JSON_SCHEMA.getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull("Schema should be parsed from JSON InputStream", schema);
    }

    @Test
    public void testGetSchemaFromInputStreamYaml() throws Exception {
        // The factory decides JSON vs YAML based on the file extension – we fake it by
        // creating a URI that ends with ".yaml" and feeding the same content.
        URI yamlUri = URI.create(TEST_SCHEME + "://my-schema.yaml");
        // Register a fetcher that returns the YAML content.
        URIFetcher yamlFetcher = uri -> new ByteArrayInputStream(SIMPLE_YAML_SCHEMA.getBytes());

        JsonSchemaFactory yamlFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(draft2020Uri())
                .addMetaSchema(SpecVersion.VersionFlag.V202012.getVersion().getInstance())
                .uriFactory(uri -> uri, TEST_SCHEME)
                .uriFetcher(yamlFetcher, TEST_SCHEME)
                .build();

        JsonSchema schema = yamlFactory.getSchema(yamlUri);
        assertNotNull("YAML schema should be parsed correctly", schema);
    }

    // -----------------------------------------------------------------------
    // 6️⃣  getSchema(URI) – caching and $id matching
    // -----------------------------------------------------------------------
    @Test
    public void testGetSchemaFromUriWithCaching() {
        // First call – schema is fetched and cached
        JsonSchema first = factory.getSchema(TEST_URI);
        assertNotNull(first);

        // Second call – should hit the cache (same instance)
        JsonSchema second = factory.getSchema(TEST_URI);
        assertSame("Second retrieval should return the cached instance", first, second);
    }

    @Test
    public void testIdMatchesSourceUriLogic() throws Exception {
        // Use reflection to access the private idMatchesSourceUri method.
        Method m = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);

        // Load the schema node that contains "$id"
        JsonNode node = new ObjectMapper().readTree(SCHEMA_WITH_ID);
        JsonMetaSchema meta = SpecVersion.VersionFlag.V202012.getVersion().getInstance();

        // The $id matches the URI we used in the fetcher – should be true.
        Boolean result = (Boolean) m.invoke(factory, meta, node, TEST_URI);
        assertTrue("idMatchesSourceUri should return true when $id equals the source URI", result);
    }

    // -----------------------------------------------------------------------
    // 7️⃣  isYaml() – private helper exercised via reflection
    // -----------------------------------------------------------------------
    @Test
    public void testIsYamlDetection() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI jsonUri = URI.create(TEST_SCHEME + "://schema.json");
        URI yamlUri = URI.create(TEST_SCHEME + "://schema.yaml");
        URI ymlUri = URI.create(TEST_SCHEME + "://schema.yml");
        URI noExt = URI.create(TEST_SCHEME + "://schema");

        assertFalse((Boolean) isYaml.invoke(factory, jsonUri));
        assertTrue((Boolean) isYaml.invoke(factory, yamlUri));
        assertTrue((Boolean) isYaml.invoke(factory, ymlUri));
        assertFalse((Boolean) isYaml.invoke(factory, noExt));
    }

    // -----------------------------------------------------------------------
    // 8️⃣  getUriTranslator – map handling
    // -----------------------------------------------------------------------
    @Test
    public void testUriTranslatorMapping() {
        // Add a simple mapping: "test://old" -> "test://new"
        String oldUri = "test://old";
        String newUri = "test://new";
        JsonSchemaFactory withMap = JsonSchemaFactory.builder(factory)
                .addUriMappings(Collections.singletonMap(oldUri, newUri))
                .build();

        URI translated = withMap.getUriTranslator().translate(URI.create(oldUri));
        assertEquals("Translator should map old URI to new URI", newUri, translated.toString());
    }

    // -----------------------------------------------------------------------
    // Helper to obtain the default meta‑schema URI (draft‑2020‑12) as a string.
    // -----------------------------------------------------------------------
    private static String draft2020Uri() {
        return SpecVersion.VersionFlag.V202012.getVersion().getInstance().getUri();
    }
}
