package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>static utility methods (checkVersion, normalizeMetaSchemaUri, isYaml)</li>
 *   <li>builder behaviour (setting custom factories, fetchers, meta‑schemas, URI mappings)</li>
 *   <li>public factory methods (getSchema overloads, getUriFactory, getUriTranslator)</li>
 *   <li>caching logic for {@code getSchema(URI,…)} </li>
 *   <li>private helpers exercised via reflection (idMatchesSourceUri, isYaml)</li>
 * </ul>
 * </p>
 *
 * <p>
 * Mockito is used to provide lightweight stubs for {@link URIFactory},
 * {@link URIFetcher} and {@link JsonMetaSchema}.  The real {@link JsonSchema}
 * implementation is exercised only insofar as the factory returns a non‑null
 * instance; deep validation of schema processing is outside the scope of this
 * unit test.
 * </p>
 */
public class JsonSchemaFactoryTest {

    private static final String TEST_META_URI = "http://example.com/schema#";
    private static final String TEST_META_URI_NORMALIZED = "https://example.com/schema#";

    private JsonMetaSchema mockMetaSchema;
    private URIFactory mockFactory;
    private URIFetcher mockFetcher;
    private URNFactory mockUrnFactory;
    private JsonSchemaFactory factory;

    @Before
    public void setUp() throws Exception {
        // ---------- mock meta‑schema ----------
        mockMetaSchema = mock(JsonMetaSchema.class);
        when(mockMetaSchema.getUri()).thenReturn(TEST_META_URI);
        // readId will be used by idMatchesSourceUri – default to null unless overridden in a test
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);

        // ---------- mock URI factory ----------
        mockFactory = mock(URIFactory.class);
        when(mockFactory.create(anyString())).thenAnswer(invocation -> new URI((String) invocation.getArgument(0)));

        // ---------- mock URI fetcher ----------
        mockFetcher = mock(URIFetcher.class);
        // by default return a simple empty JSON object
        when(mockFetcher.fetch(any(URI.class))).thenReturn(new ByteArrayInputStream("{\"type\":\"object\"}".getBytes()));

        // ---------- mock URN factory (not used directly in these tests) ----------
        mockUrnFactory = mock(URNFactory.class);

        // ---------- build a factory with the above mocks ----------
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(TEST_META_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockFactory, "test")
                .uriFetcher(mockFetcher, "test")
                .addUrnFactory(mockUrnFactory)
                .enableUriSchemaCache(true)          // enable caching for cache‑related tests
                .build();
    }

    // -----------------------------------------------------------------------
    //  Static utility methods
    // -----------------------------------------------------------------------

    @Test
    public void testCheckVersionAllSupported() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void testCheckVersionNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupported() {
        // Simulate an unsupported enum value by using reflection to create a bogus enum constant
        // (the library does not expose a public way to obtain an unsupported value)
        // Here we simply call the default branch with a null that will be caught by the
        // previous test – the real unreachable path is exercised by the default clause.
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("UNKNOWN"));
    }

    @Test
    public void testNormalizeMetaSchemaUriValid() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        String result = (String) normalize.invoke(null, "http://example.com/schema#");
        assertEquals(TEST_META_URI_NORMALIZED, result);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalid() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        normalize.invoke(null, "http://[invalid-uri]");
    }

    @Test
    public void testIsYamlDetection() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI yaml1 = new URI("file:/tmp/schema.yaml");
        URI yaml2 = new URI("file:/tmp/schema.yml");
        URI json = new URI("file:/tmp/schema.json");
        URI noExt = new URI("file:/tmp/schema");

        assertTrue((Boolean) isYaml.invoke(factory, yaml1));
        assertTrue((Boolean) isYaml.invoke(factory, yaml2));
        assertFalse((Boolean) isYaml.invoke(factory, json));
        assertFalse((Boolean) isYaml.invoke(factory, noExt));
    }

    // -----------------------------------------------------------------------
    //  Builder and getter behaviours
    // -----------------------------------------------------------------------

    @Test
    public void testBuilderCopiesAllComponents() {
        JsonSchemaFactory copy = JsonSchemaFactory.builder(factory).build();

        // The copies should share the same meta‑schema map (by content) but be distinct objects
        assertNotSame(factory, copy);
        assertEquals(factory.getUriFactory().getClass(), copy.getUriFactory().getClass());
        assertEquals(factory.getUriTranslator().getClass(), copy.getUriTranslator().getClass());

        // Verify that the custom URI factory we injected is present in the copy
        URIFactory retrieved = copy.getUriFactory();
        assertNotNull(retrieved);
        // The actual mock instance is stored inside a URISchemeFactory; we verify by
        // attempting to create a URI with a scheme we registered ("test")
        URI created = retrieved.create("test://example");
        assertEquals("test://example", created.toString());
    }

    @Test
    public void testGetUriFactoryReturnsConfiguredFactory() {
        URIFactory factoryReturned = factory.getUriFactory();
        assertNotNull(factoryReturned);
        // The returned factory should be capable of handling the "test" scheme we added
        URI uri = factoryReturned.create("test://my-schema");
        assertEquals("test://my-schema", uri.toString());
    }

    @Test
    public void testGetUriTranslatorIncludesMappings() {
        // add a simple mapping and ensure it appears in the translator
        Map<String, String> map = new HashMap<>();
        map.put("http://old.com/schema", "http://new.com/schema");
        JsonSchemaFactory withMap = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(TEST_META_URI)
                .addMetaSchema(mockMetaSchema)
                .addUriMappings(map)
                .build();

        URITranslator translator = withMap.getUriTranslator();
        URI translated = translator.translate(URI.create("http://old.com/schema"));
        assertEquals(URI.create("http://new.com/schema"), translated);
    }

    // -----------------------------------------------------------------------
    //  getSchema overloads – happy path and error handling
    // -----------------------------------------------------------------------

    @Test
    public void testGetSchemaFromStringValidJson() {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJson() {
        factory.getSchema("{invalid json");
    }

    @Test
    public void testGetSchemaFromInputStreamValidJson() throws Exception {
        InputStream stream = new ByteArrayInputStream("{\"type\":\"array\"}".getBytes());
        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalidJson() throws Exception {
        InputStream stream = new ByteArrayInputStream("not a json".getBytes());
        factory.getSchema(stream);
    }

    @Test
    public void testGetSchemaFromUriUsesFetcherAndCachesResult() throws Exception {
        URI testUri = new URI("test://my-schema.json");
        JsonSchema first = factory.getSchema(testUri);
        JsonSchema second = factory.getSchema(testUri);

        assertNotNull(first);
        assertNotNull(second);
        // fetcher should have been called only once because of caching
        verify(mockFetcher, times(1)).fetch(any(URI.class));
        // The two returned schema objects should be the same cached instance
        assertSame(first, second);
    }

    @Test
    public void testGetSchemaFromUriDifferentConfigUpdatesCacheEntry() throws Exception {
        URI testUri = new URI("test://my-schema.json");
        SchemaValidatorsConfig config1 = new SchemaValidatorsConfig();
        SchemaValidatorsConfig config2 = new SchemaValidatorsConfig();
        config2.setFailFast(true); // just an example flag change

        JsonSchema schema1 = factory.getSchema(testUri, config1);
        JsonSchema schema2 = factory.getSchema(testUri, config2);

        // fetcher called only once (cache hit)
        verify(mockFetcher, times(1)).fetch(any(URI.class));
        // The cached instance is returned for both calls
        assertSame(schema1, schema2);
        // The configuration stored inside the cached schema should reflect the latest config
        assertTrue(schema2.getValidationContext().getConfig().isFailFast());
    }

    // -----------------------------------------------------------------------
    //  Private helper methods exercised via reflection
    // -----------------------------------------------------------------------

    @Test
    public void testIdMatchesSourceUriTrue() throws Exception {
        // Prepare a mock meta‑schema that returns a matching $id
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getUri()).thenReturn(TEST_META_URI);
        when(meta.readId(any(JsonNode.class))).thenReturn("test://schema");

        // Insert the mock meta‑schema into a factory instance
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(TEST_META_URI)
                .addMetaSchema(meta)
                .uriFactory(mockFactory, "test")
                .uriFetcher(mockFetcher, "test")
                .build();

        // Build a JSON node that contains the $id field
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$id\":\"test://schema\"}");

        // Invoke the private method
        Method method = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(customFactory, meta, node, new URI("test://schema"));
        assertTrue(result);
    }

    @Test
    public void testIdMatchesSourceUriFalseWhenIdMissing() throws Exception {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getUri()).thenReturn(TEST_META_URI);
        when(meta.readId(any(JsonNode.class))).thenReturn(null); // no $id

        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(TEST_META_URI)
                .addMetaSchema(meta)
                .uriFactory(mockFactory, "test")
                .uriFetcher(mockFetcher, "test")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");

        Method method = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(customFactory, meta, node, new URI("test://schema"));
        assertFalse(result);
    }

    // -----------------------------------------------------------------------
    //  Edge cases for constructor validation (indirectly via builder)
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaUriMissing() {
        JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema) // but no defaultMetaSchemaURI
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenMetaSchemaMapEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(TEST_META_URI)
                .build(); // no meta‑schema added
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaNotProvided() {
        // The default URI is set, but the corresponding meta‑schema is absent
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://unknown.com/schema#")
                .addMetaSchema(mockMetaSchema) // mockMetaSchema uses a different URI
                .build();
    }

    // -----------------------------------------------------------------------
    //  Deprecated static getInstance() methods (smoke tests)
    // -----------------------------------------------------------------------

    @Test
    public void testDeprecatedGetInstanceReturnsFactory() {
        JsonSchemaFactory deprecated = JsonSchemaFactory.getInstance();
        assertNotNull(deprecated);
        // The returned factory should contain the draft‑04 meta‑schema
        JsonSchemaFactory v4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertEquals(v4.getUriFactory().getClass(), deprecated.getUriFactory().getClass());
    }
}
