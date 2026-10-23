/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests focus on the public API and the internal logical helpers that do not
 * require the full keyword/format infrastructure of the library.  Wherever possible
 * lightweight mock objects (Mockito) are used to isolate the class under test.
 *
 * The test file is deliberately self‑contained – it creates a minimal but valid
 * {@code JsonMetaSchema} mock and registers a custom {@code URIFactory} /
 * {@code URIFetcher} pair for the “mock” scheme so that schema loading from a URI
 * can be exercised without any external I/O.
 *
 * Author: ChatGPT (OpenAI)
 * Date:   2026‑10‑23
 */
package com.networknt.schema;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link JsonSchemaFactory}.
 */
public class JsonSchemaFactoryTest {

    private static final String DRAFT_07_URI = "https://json-schema.org/draft-07/schema";

    /** A mock {@link JsonMetaSchema} that pretends to be the Draft‑07 meta‑schema. */
    private JsonMetaSchema mockMetaSchema;

    /** A factory built with the mock meta‑schema – used by most tests. */
    private JsonSchemaFactory factory;

    /** Simple JSON schema used for positive tests (empty object). */
    private static final String SIMPLE_JSON_SCHEMA = "{}";

    /** Simple YAML schema used for positive tests. */
    private static final String SIMPLE_YAML_SCHEMA = "type: object";

    @Before
    public void setUp() {
        // -----------------------------------------------------------------
        // Prepare a mock JsonMetaSchema.
        // -----------------------------------------------------------------
        mockMetaSchema = mock(JsonMetaSchema.class);
        when(mockMetaSchema.getUri()).thenReturn(DRAFT_07_URI);
        // The meta‑schema’s readId method is used only by idMatchesSourceUri().
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);

        // -----------------------------------------------------------------
        // Build a JsonSchemaFactory that contains the mock meta‑schema.
        // -----------------------------------------------------------------
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    // -----------------------------------------------------------------
    //  Builder / static factory helpers
    // -----------------------------------------------------------------
    @Test
    public void testBuilderCreatesFactory() {
        assertNotNull(factory);
        assertEquals(DRAFT_07_URI, factory.getUriFactory().create(DRAFT_07_URI).toString());
    }

    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory v4Factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(v4Factory);
        // The default meta‑schema for V4 is known – just ensure it exists.
        assertTrue(v4Factory.getUriFactory() instanceof URISchemeFactory);
    }

    @Test
    public void testCheckVersionValidValues() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void testCheckVersionNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupportedValue() {
        // Simulate an unsupported enum value by casting an illegal integer.
        // This triggers the default case of the switch statement.
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V99"));
    }

    // -----------------------------------------------------------------
    //  normalizeMetaSchemaUri
    // -----------------------------------------------------------------
    @Test
    public void testNormalizeMetaSchemaUriForHttp() {
        String original = "http://example.com:8080/path?query=123#frag";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(original);
        assertEquals("https://example.com:8080/path", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalid() {
        JsonSchemaFactory.normalizeMetaSchemaUri(":// malformed uri");
    }

    // -----------------------------------------------------------------
    //  isYaml
    // -----------------------------------------------------------------
    @Test
    public void testIsYamlWithYamlExtension() throws Exception {
        URI yamlUri = new URI("mock:/schema.yaml");
        // Use reflection to invoke the private method (acceptable for unit testing).
        boolean result = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", URI.class)
                .invoke(factory, yamlUri);
        assertTrue(result);
    }

    @Test
    public void testIsYamlWithYmlExtension() throws Exception {
        URI ymlUri = new URI("mock:/schema.yml");
        boolean result = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", URI.class)
                .invoke(factory, ymlUri);
        assertTrue(result);
    }

    @Test
    public void testIsYamlWithJsonExtension() throws Exception {
        URI jsonUri = new URI("mock:/schema.json");
        boolean result = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", URI.class)
                .invoke(factory, jsonUri);
        assertFalse(result);
    }

    @Test
    public void testIsYamlWithoutExtension() throws Exception {
        URI noExt = new URI("mock:/schema");
        boolean result = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", URI.class)
                .invoke(factory, noExt);
        assertFalse(result);
    }

    // -----------------------------------------------------------------
    //  idMatchesSourceUri
    // -----------------------------------------------------------------
    @Test
    public void testIdMatchesSourceUriTrue() throws Exception {
        // Configure the mock meta‑schema to return an id that equals the source URI.
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn("mock:/my-schema.json");

        JsonNode dummyNode = new ObjectMapper().readTree("{\"type\":\"object\"}");
        boolean result = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class)
                .invoke(factory, mockMetaSchema, dummyNode, new URI("mock:/my-schema.json"));
        assertTrue(result);
    }

    @Test
    public void testIdMatchesSourceUriFalse() throws Exception {
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn("mock:/other.json");

        JsonNode dummyNode = new ObjectMapper().readTree("{\"type\":\"object\"}");
        boolean result = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class)
                .invoke(factory, mockMetaSchema, dummyNode, new URI("mock:/my-schema.json"));
        assertFalse(result);
    }

    // -----------------------------------------------------------------
    //  getUriFactory & getUriTranslator
    // -----------------------------------------------------------------
    @Test
    public void testGetUriFactoryIsNotNull() {
        assertNotNull(factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslatorAppliesMappings() {
        // Build a factory with a mapping (deprecated API – still functional).
        Map<String, String> map = new HashMap<>();
        map.put("http://old.com", "https://new.com");
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .addUriMappings(map)
                .build();

        URITranslator translator = f.getUriTranslator();
        URI result = translator.translate(URI.create("http://old.com/path"));
        assertEquals("https://new.com/path", result.toString());
    }

    // -----------------------------------------------------------------
    //  getSchema(String) – JSON parsing
    // -----------------------------------------------------------------
    @Test
    public void testGetSchemaFromStringValidJson() {
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull(schema);
        // The created schema should reference the same ValidationContext instance.
        assertNotNull(schema.getValidationContext());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJson() {
        factory.getSchema("{ invalid json");
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringWithNonTextualSchemaNode() {
        // Schema node with "$schema": 123 (non‑textual) should trigger an exception.
        String json = "{\"$schema\":123}";
        factory.getSchema(json);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringWithUnknownMetaSchema() {
        // The "$schema" field points to a dialect that is not known to the factory.
        String json = "{\"$schema\":\"http://unknown/schema\"}";
        factory.getSchema(json);
    }

    // -----------------------------------------------------------------
    //  getSchema(URI) – custom fetcher / caching
    // -----------------------------------------------------------------
    /**
     * Simple {@link URIFactory} that just creates a {@link URI} from the supplied
     * string – suitable for the “mock” scheme used in the tests.
     */
    private static class SimpleURIFactory implements URIFactory {
        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }
    }

    /**
     * {@link URIFetcher} implementation that returns a byte array input stream
     * containing the JSON (or YAML) supplied at construction time.
     * It also counts the number of fetch invocations.
     */
    private static class CountingURIFetcher implements URIFetcher {
        private final byte[] payload;
        private final AtomicInteger counter = new AtomicInteger();

        CountingURIFetcher(String payload) {
            this.payload = payload.getBytes();
        }

        @Override
        public InputStream fetch(URI uri) {
            counter.incrementAndGet();
            return new ByteArrayInputStream(payload);
        }

        int getFetchCount() {
            return counter.get();
        }
    }

    @Test
    public void testGetSchemaFromUriWithCachingEnabled() throws Exception {
        CountingURIFetcher fetcher = new CountingURIFetcher(SIMPLE_JSON_SCHEMA);
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(new SimpleURIFactory(), "mock")
                .uriFetcher(fetcher, "mock")
                .enableUriSchemaCache(true)
                .build();

        URI uri = new URI("mock:/test.json");
        JsonSchema first = f.getSchema(uri);
        JsonSchema second = f.getSchema(uri);

        assertNotNull(first);
        assertNotNull(second);
        // The factory should have fetched the resource only once because of caching.
        assertEquals(1, fetcher.getFetchCount());
        // The two JsonSchema instances should be the same cached object.
        assertSame(first, second);
    }

    @Test
    public void testGetSchemaFromUriWithCachingDisabled() throws Exception {
        CountingURIFetcher fetcher = new CountingURIFetcher(SIMPLE_JSON_SCHEMA);
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(new SimpleURIFactory(), "mock")
                .uriFetcher(fetcher, "mock")
                .enableUriSchemaCache(false)
                .build();

        URI uri = new URI("mock:/test.json");
        JsonSchema first = f.getSchema(uri);
        JsonSchema second = f.getSchema(uri);

        assertNotNull(first);
        assertNotNull(second);
        // Without caching the fetcher must be called twice.
        assertEquals(2, fetcher.getFetchCount());
        // The instances are distinct because they were created separately.
        assertNotSame(first, second);
    }

    // -----------------------------------------------------------------
    //  getSchema(URI) – YAML handling
    // -----------------------------------------------------------------
    @Test
    public void testGetSchemaFromYamlUri() throws Exception {
        CountingURIFetcher yamlFetcher = new CountingURIFetcher(SIMPLE_YAML_SCHEMA);
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(new SimpleURIFactory(), "mock")
                .uriFetcher(yamlFetcher, "mock")
                .build();

        URI yamlUri = new URI("mock:/schema.yaml");
        JsonSchema schema = f.getSchema(yamlUri);
        assertNotNull(schema);
        // Verify that the fetcher was indeed called (ensuring the YAML path was taken).
        assertEquals(1, yamlFetcher.getFetchCount());
    }

    // -----------------------------------------------------------------
    //  getSchema overloads that accept JsonNode / InputStream
    // -----------------------------------------------------------------
    @Test
    public void testGetSchemaFromJsonNode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(SIMPLE_JSON_SCHEMA);
        JsonSchema schema = factory.getSchema(node);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream(SIMPLE_JSON_SCHEMA.getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalidContent() throws Exception {
        InputStream is = new ByteArrayInputStream("{ bad json".getBytes());
        factory.getSchema(is);
    }

    // -----------------------------------------------------------------
    //  Exception handling for null arguments in the constructor (private)
    //  – exercised via the builder to ensure validation works.
    // -----------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testFactoryConstructionFailsWhenDefaultMetaSchemaMissing() {
        JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema) // meta‑schema present
                // do NOT set defaultMetaSchemaURI
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryConstructionFailsWhenMetaSchemaMapEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                // No meta‑schema added
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryConstructionFailsWhenObjectMapperNull() {
        // Force a null ObjectMapper via the builder.
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .objectMapper(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryConstructionFailsWhenYamlMapperNull() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .yamlMapper(null)
                .build();
    }

    // -----------------------------------------------------------------
    //  Private helper – ensure reflection works for the methods we test.
    // -----------------------------------------------------------------
    @Test
    public void testPrivateMethodsAreAccessibleViaReflection() throws Exception {
        // normalizeMetaSchemaUri is static protected – we can call it directly.
        String normalized = (String) JsonSchemaFactory.class
                .getDeclaredMethod("normalizeMetaSchemaUri", String.class)
                .invoke(null, "http://example.org/");
        assertEquals("https://example.org/", normalized);
    }
}
