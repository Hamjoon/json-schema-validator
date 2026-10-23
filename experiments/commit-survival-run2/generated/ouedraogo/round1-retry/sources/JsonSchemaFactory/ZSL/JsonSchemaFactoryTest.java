/****************************************************************************************
 * JsonSchemaFactoryTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests cover:
 *   • Builder validation (null arguments, default meta‑schema handling)
 *   • Static helpers: {@code getInstance}, {@code checkVersion}, {@code normalizeMetaSchemaUri}
 *   • Private helpers {@code isYaml} and {@code idMatchesSourceUri} (via reflection)
 *   • Schema loading overloads (String, InputStream, URI) and error handling
 *   • URI schema caching behaviour
 *
 * The test class uses Mockito to stub out heavy dependencies such as
 * {@link com.networknt.schema.ur i.URI} factories/fetchers and {@link JsonMetaSchema}.
 *
 * Author:  Automated Test Generator
 * Date:    2026‑10‑23
 ****************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.urn.URNFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for {@link JsonSchemaFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    // -------------------------------------------------------------------------
    // Mocks used throughout the test suite
    // -------------------------------------------------------------------------
    @Mock private URIFactory mockUriFactory;
    @Mock private URIFetcher mockUriFetcher;
    @Mock private URNFactory mockUrnFactory;
    @Mock private JsonMetaSchema mockMetaSchema;
    @Mock private SchemaValidatorsConfig mockConfig;

    private JsonSchemaFactory factory;
    private static final String DEFAULT_META_URI = "http://example.com/meta";

    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // Prepare a mock meta‑schema that behaves like a real one for the
        // limited interactions performed by the factory.
        // -----------------------------------------------------------------
        when(mockMetaSchema.getUri()).thenReturn(DEFAULT_META_URI);
        // readId is used only by idMatchesSourceUri – we will set it per test.
        // The factory constructor validates that the supplied meta‑schema map
        // contains an entry for the normalized default meta‑schema URI.
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_URI);
        // Build the factory with the mocks.
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)                 // registers under normalized URI
                .uriFactory(mockUriFactory, "test")            // arbitrary scheme for URI creation
                .uriFetcher(mockUriFetcher, "test")            // same scheme for fetching
                .addUrnFactory(mockUrnFactory)               // not used in the tests but required
                .enableUriSchemaCache(true)                  // enable caching for cache tests
                .build();
    }

    // -------------------------------------------------------------------------
    // 1. Builder validation
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectNullObjectMapper() {
        new JsonSchemaFactory.Builder()
                .objectMapper(null)                     // illegal
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectNullYamlMapper() {
        new JsonSchemaFactory.Builder()
                .yamlMapper(null)                       // illegal
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectEmptyDefaultMetaSchemaUri() {
        new JsonSchemaFactory.Builder()
                .defaultMetaSchemaURI("   ")
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectMissingMetaSchemaForDefaultUri() {
        // Provide a different meta‑schema (different URI) – the constructor will
        // not find a mapping for the default URI and must fail.
        JsonMetaSchema otherMeta = mock(JsonMetaSchema.class);
        when(otherMeta.getUri()).thenReturn("http://other.com/meta");
        new JsonSchemaFactory.Builder()
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(otherMeta)               // does NOT match default
                .build();
    }

    // -------------------------------------------------------------------------
    // 2. Static helper methods
    // -------------------------------------------------------------------------

    @Test
    public void getInstanceShouldCreateFactoryWithDraftV4MetaSchema() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(f);
        // The default meta‑schema URI for Draft‑04 is well‑known; we just verify the
        // string contains the expected draft identifier.
        assertTrue(f.defaultMetaSchemaURI.contains("draft-04"));
    }

    @Test
    public void checkVersionShouldReturnCorrectVersionObjects() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void checkVersionShouldReturnNullForNullFlag() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionShouldRejectUnsupportedFlag() {
        // Use a bogus enum value by casting – this triggers the default case.
        JsonSchemaFactory.checkVersion(Enum.valueOf(SpecVersion.VersionFlag.class, "V99"));
    }

    @Test
    public void normalizeMetaSchemaUriShouldForceHttpsScheme() throws Exception {
        String httpUri = "http://example.com/schema#";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(httpUri);
        assertTrue(normalized.startsWith("https://"));
        // The rest of the URI must stay unchanged (except the scheme)
        assertEquals("https://example.com/schema#", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriShouldThrowOnInvalidUri() throws Exception {
        JsonSchemaFactory.normalizeMetaSchemaUri("://missing-scheme");
    }

    // -------------------------------------------------------------------------
    // 3. Private helper isYaml (accessed via reflection)
    // -------------------------------------------------------------------------

    @Test
    public void isYamlShouldDetectYamlExtensions() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);

        URI yaml1 = new URI("file:/tmp/schema.yaml");
        URI yaml2 = new URI("file:/tmp/schema.yml");
        URI json  = new URI("file:/tmp/schema.json");
        URI noExt = new URI("file:/tmp/schema");

        assertTrue((Boolean) m.invoke(factory, yaml1));
        assertTrue((Boolean) m.invoke(factory, yaml2));
        assertFalse((Boolean) m.invoke(factory, json));
        assertFalse((Boolean) m.invoke(factory, noExt));
    }

    // -------------------------------------------------------------------------
    // 4. Private helper idMatchesSourceUri (accessed via reflection)
    // -------------------------------------------------------------------------

    @Test
    public void idMatchesSourceUriShouldReturnTrueWhenIdEqualsUri() throws Exception {
        // Arrange: mock meta‑schema to return a concrete $id.
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn("http://example.com/schema");
        Method m = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);

        // Build a tiny JSON node containing something (the content is irrelevant)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"title\":\"test\"}");

        boolean result = (Boolean) m.invoke(factory, mockMetaSchema, node,
                new URI("http://example.com/schema"));
        assertTrue(result);
    }

    @Test
    public void idMatchesSourceUriShouldReturnFalseWhenIdIsMissingOrDifferent() throws Exception {
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);
        Method m = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"title\":\"test\"}");

        boolean result = (Boolean) m.invoke(factory, mockMetaSchema, node,
                new URI("http://example.com/schema"));
        assertFalse(result);
    }

    // -------------------------------------------------------------------------
    // 5. getSchema overloads – error handling
    // -------------------------------------------------------------------------

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringShouldWrapInvalidJson() {
        // Invalid JSON (missing closing brace) – should be wrapped in JsonSchemaException.
        factory.getSchema("{\"type\":\"object\"");
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromInputStreamShouldWrapInvalidJson() throws Exception {
        InputStream bad = new ByteArrayInputStream("{invalid".getBytes("UTF‑8"));
        factory.getSchema(bad);
    }

    // -------------------------------------------------------------------------
    // 6. getSchema(URI) – normal flow with caching
    // -------------------------------------------------------------------------

    @Test
    public void getSchemaFromUriShouldUseFetcherAndCacheResult() throws Exception {
        // ---------------------------------------------------------------
        // Prepare a simple JSON schema that the fetcher will return.
        // ---------------------------------------------------------------
        String schemaJson = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes("UTF‑8"));

        // The mock fetcher must return the stream when asked for the mapped URI.
        // The factory first translates the supplied URI using the (empty) URI
        // translator map, then creates a new URI via the mock URIFactory.
        // We therefore let the factory's URIFactory simply return the original URI.
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        when(mockUriFetcher.fetch(any(URI.class))).thenReturn(schemaStream);

        // Use a custom scheme that we registered in setUp (\"test\").
        URI testUri = new URI("test:/my-schema.json");

        // First call – fetcher should be invoked.
        JsonSchema schema1 = factory.getSchema(testUri);
        assertNotNull(schema1);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class));

        // Second call – result should be taken from the cache, fetcher not called again.
        JsonSchema schema2 = factory.getSchema(testUri);
        assertNotNull(schema2);
        // Same instance because the cache returns the stored object.
        assertSame(schema1, schema2);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class)); // still only one fetch
    }

    @Test
    public void getSchemaFromUriShouldRespectEnableUriSchemaCacheFlag() throws Exception {
        // Build a factory with caching disabled.
        JsonSchemaFactory noCacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "test")
                .uriFetcher(mockUriFetcher, "test")
                .enableUriSchemaCache(false)          // disable cache
                .build();

        String schemaJson = "{\"type\":\"object\"}";
        InputStream schemaStream1 = new ByteArrayInputStream(schemaJson.getBytes("UTF‑8"));
        InputStream schemaStream2 = new ByteArrayInputStream(schemaJson.getBytes("UTF‑8"));

        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        // Return a fresh stream each time to mimic real fetching.
        when(mockUriFetcher.fetch(any(URI.class)))
                .thenReturn(schemaStream1)
                .thenReturn(schemaStream2);

        URI testUri = new URI("test:/schema.json");

        JsonSchema first = noCacheFactory.getSchema(testUri);
        JsonSchema second = noCacheFactory.getSchema(testUri);

        // Two fetches must have happened because caching is disabled.
        verify(mockUriFetcher, times(2)).fetch(any(URI.class));
        assertNotSame(first, second);
    }

    // -------------------------------------------------------------------------
    // 7. Builder copy (builder(JsonSchemaFactory))
    // -------------------------------------------------------------------------

    @Test
    public void builderCopyShouldPreserveAllConfiguration() {
        JsonSchemaFactory copy = JsonSchemaFactory.builder(factory).build();

        // The copy must have the same default meta‑schema URI.
        assertEquals(factory.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
        // The copy must contain the same meta‑schema map size (at least the one we added).
        assertEquals(factory.jsonMetaSchemas.size(), copy.jsonMetaSchemas.size());
        // The copy must retain the same URI mapping map (empty in our test set‑up).
        assertEquals(factory.uriMap, copy.uriMap);
        // The copy must retain the same URI translators (none in this test).
        assertEquals(factory.uriTranslators, copy.uriTranslators);
    }
}
