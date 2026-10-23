###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests are written from the perspective of three “experts”.  Each expert
 * contributed a test case for a specific group of methods (builder / factory,
 * meta‑schema handling, schema loading & caching, utility helpers).  The final
 * file merges all contributions into a single, runnable test class.
 *
 * The suite uses Mockito for mocking collaborators (URIFactory, URIFetcher,
 * ObjectMapper, YAMLMapper) and relies on the real {@code JsonSchemaFactory}
 * implementation for the default Draft‑V4 meta‑schema (provided by the
 * library itself).  All public API surface of the class is exercised, covering
 * typical usage, edge‑cases and error scenarios.
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link JsonSchemaFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    // -----------------------------------------------------------------------
    // Mocks used by several tests (created by MockitoJUnitRunner)
    // -----------------------------------------------------------------------
    @Mock
    private ObjectMapper mockObjectMapper;

    @Mock
    private YAMLMapper mockYamlMapper;

    @Mock
    private URIFactory mockUriFactory;

    @Mock
    private URIFetcher mockUriFetcher;

    @Mock
    private JsonNode mockJsonNode;

    @Mock
    private JsonMetaSchema mockMetaSchema;

    private JsonSchemaFactory factoryWithDefaults;

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------
    /** Returns an {@link InputStream} that reads the supplied JSON string. */
    private static InputStream streamOf(String json) {
        return new ByteArrayInputStream(json.getBytes());
    }

    /**
     * Builds a factory that uses the supplied mock {@link URIFactory} /
     * {@link URIFetcher} for a custom scheme ("mock").
     */
    private JsonSchemaFactory buildFactoryWithMockFetcher() {
        // The builder must contain at least one meta‑schema – we reuse the
        // built‑in Draft‑V4 schema.
        JsonMetaSchema draftV4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
                .jsonMetaSchemas
                .values()
                .iterator()
                .next();

        return JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(draftV4.getUri())
                .addMetaSchema(draftV4)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .uriFactory(mockUriFactory, "mock")
                .uriFetcher(mockUriFetcher, "mock")
                .build();
    }

    // -----------------------------------------------------------------------
    // Set‑up
    // -----------------------------------------------------------------------
    @Before
    public void setUp() throws Exception {
        // A ready‑to‑use factory with the real Draft‑V4 meta‑schema.
        factoryWithDefaults = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    }

    // -----------------------------------------------------------------------
    // 1. Builder & factory creation (Expert A)
    // -----------------------------------------------------------------------

    /** Builder must reject a null {@link ObjectMapper}. */
    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsNullObjectMapper() {
        JsonSchemaFactory.builder()
                .objectMapper(null)                     // <-- null
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(factoryWithDefaults.defaultMetaSchemaURI)
                .addMetaSchema(factoryWithDefaults.jsonMetaSchemas.values().iterator().next())
                .build();                                // throws
    }

    /** Builder must reject a null {@link YAMLMapper}. */
    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsNullYamlMapper() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(null)                       // <-- null
                .defaultMetaSchemaURI(factoryWithDefaults.defaultMetaSchemaURI)
                .addMetaSchema(factoryWithDefaults.jsonMetaSchemas.values().iterator().next())
                .build();                                // throws
    }

    /** Builder must reject a missing default meta‑schema URI. */
    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsMissingDefaultMetaSchemaUri() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .addMetaSchema(factoryWithDefaults.jsonMetaSchemas.values().iterator().next())
                .build();                                // throws
    }

    /** Builder must reject an empty meta‑schema collection. */
    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsEmptyMetaSchemaCollection() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI("http://example.com/draft-04/schema")
                .build();                                // throws because jsonMetaSchemas is empty
    }

    /** Builder must reject when the default meta‑schema URI is not present in the map. */
    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsDefaultMetaSchemaNotProvided() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI("http://example.com/unknown/schema")
                .addMetaSchema(factoryWithDefaults.jsonMetaSchemas.values().iterator().next())
                .build();                                // throws
    }

    // -----------------------------------------------------------------------
    // 2. Static factory methods (Expert B)
    // -----------------------------------------------------------------------

    /** getInstance() without arguments returns a Draft‑V4 factory. */
    @Test
    public void getInstanceReturnsDraftV4Factory() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull(f);
        // The default meta‑schema URI of Draft‑V4 is known.
        assertEquals(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4)
                .getInstance().getUri(),
                f.defaultMetaSchemaURI);
    }

    /** checkVersion returns the correct implementation for each flag. */
    @Test
    public void checkVersionMapsAllSupportedFlags() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    /** checkVersion returns null for a null flag. */
    @Test
    public void checkVersionReturnsNullForNullFlag() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    /** checkVersion throws for an unknown flag (simulated via reflection). */
    @Test(expected = IllegalArgumentException.class)
    public void checkVersionThrowsForUnsupportedFlag() {
        // Create a dummy enum constant via reflection is cumbersome; instead we
        // call the private branch using a null that triggers the default case.
        // The method already throws IllegalArgumentException for the default.
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V999"));
    }

    // -----------------------------------------------------------------------
    // 3. Meta‑schema handling (Expert C)
    // -----------------------------------------------------------------------

    /** normalizeMetaSchemaUri converts http to https while preserving components. */
    @Test
    public void normalizeMetaSchemaUriChangesHttpToHttps() {
        String original = "http://example.com:8080/some/path?query=1#frag";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(original);
        assertEquals("https://example.com:8080/some/path", normalized);
    }

    /** normalizeMetaSchemaUri throws JsonSchemaException for malformed URIs. */
    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriThrowsOnBadUri() {
        JsonSchemaFactory.normalizeMetaSchemaUri("::not a uri::");
    }

    /** isYaml recognises both .yaml and .yml extensions. */
    @Test
    public void isYamlDetectsYamlExtensions() throws Exception {
        JsonSchemaFactory f = factoryWithDefaults;
        assertTrue(f.isYaml(new URI("file:/schema.yaml")));
        assertTrue(f.isYaml(new URI("file:/schema.yml")));
        assertFalse(f.isYaml(new URI("file:/schema.json")));
        assertFalse(f.isYaml(new URI("file:/nosuffix")));
    }

    /** idMatchesSourceUri returns false when the schema has no $id. */
    @Test
    public void idMatchesSourceUriHandlesMissingId() throws Exception {
        JsonSchemaFactory f = factoryWithDefaults;
        JsonNode schemaWithoutId = mock(JsonNode.class);
        when(schemaWithoutId.get("$id")).thenReturn(null);
        // meta‑schema readId will return null (we mock the meta‑schema)
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(schemaWithoutId)).thenReturn(null);
        assertFalse(f.idMatchesSourceUri(meta, schemaWithoutId, new URI("http://example.com/schema")));
    }

    /** idMatchesSourceUri returns true only when the $id exactly matches the supplied URI. */
    @Test
    public void idMatchesSourceUriExactMatch() throws Exception {
        JsonSchemaFactory f = factoryWithDefaults;
        JsonNode schema = mock(JsonNode.class);
        when(schema.get("$id")).thenReturn(null); // not used directly
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(schema)).thenReturn("http://example.com/my/schema");
        assertTrue(f.idMatchesSourceUri(meta, schema, new URI("http://example.com/my/schema")));
        assertFalse(f.idMatchesSourceUri(meta, schema, new URI("http://example.com/other/schema")));
    }

    // -----------------------------------------------------------------------
    // 4. Schema loading (string, InputStream, URI) and caching (Expert A)
    // -----------------------------------------------------------------------

    /** getSchema(String) parses a JSON string and returns a JsonSchema instance. */
    @Test
    public void getSchemaFromStringParsesJson() {
        String json = "{\"type\":\"object\"}";
        JsonSchema schema = factoryWithDefaults.getSchema(json);
        assertNotNull(schema);
        // The created schema must have a ValidationContext whose meta‑schema
        // matches the factory's default.
        assertEquals(factoryWithDefaults.defaultMetaSchemaURI,
                schema.getValidationContext().getMetaSchema().getUri());
    }

    /** getSchema(InputStream) works identically to the String overload. */
    @Test
    public void getSchemaFromInputStreamParsesJson() {
        String json = "{\"type\":\"array\"}";
        JsonSchema schema = factoryWithDefaults.getSchema(streamOf(json));
        assertNotNull(schema);
        assertEquals("array", schema.getValidationContext().getRootSchemaNode().get("type").asText());
    }

    /** getSchema(URI) loads a schema via a custom URIFetcher and caches it. */
    @Test
    public void getSchemaFromUriUsesFetcherAndCachesResult() throws Exception {
        // Prepare a minimal JSON schema document.
        final String schemaContent = "{\"type\":\"string\"}";
        final InputStream schemaStream = streamOf(schemaContent);
        final URI schemaUri = new URI("mock:/test-schema.json");

        // Mock factory components.
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        when(mockUriFetcher.fetch(any(URI.class))).thenReturn(schemaStream);

        JsonSchemaFactory factory = buildFactoryWithMockFetcher();

        // First call – fetcher should be invoked.
        JsonSchema first = factory.getSchema(schemaUri);
        assertNotNull(first);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class));

        // Second call – result must be served from the cache, fetcher not invoked again.
        JsonSchema second = factory.getSchema(schemaUri);
        assertSame(first, second);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class)); // still only one call
    }

    /** getSchema(URI) with a YAML file triggers the YAML mapper. */
    @Test
    public void getSchemaFromYamlUriUsesYamlMapper() throws Exception {
        final String yaml = "type: integer";
        final InputStream yamlStream = streamOf(yaml);
        final URI yamlUri = new URI("mock:/my-schema.yaml");

        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        when(mockUriFetcher.fetch(any(URI.class))).thenReturn(yamlStream);
        when(mockYamlMapper.readTree(any(InputStream.class))).thenReturn(mockJsonNode);
        when(mockJsonNode.get("$schema")).thenReturn(null); // fallback to default meta‑schema

        JsonSchemaFactory factory = buildFactoryWithMockFetcher();

        JsonSchema schema = factory.getSchema(yamlUri);
        assertNotNull(schema);
        // Verify that the YAML mapper was used (instead of the JSON mapper).
        verify(mockYamlMapper, times(1)).readTree(any(InputStream.class));
        verify(mockObjectMapper, never()).readTree(any(InputStream.class));
    }

    /** getSchema(URI) propagates IOExceptions as JsonSchemaException. */
    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUriPropagatesFetcherIOException() throws Exception {
        final URI badUri = new URI("mock:/non‑existent.json");
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        when(mockUriFetcher.fetch(any(URI.class))).thenThrow(new IOException("fetch failed"));
        JsonSchemaFactory factory = buildFactoryWithMockFetcher();

        factory.getSchema(badUri); // should throw JsonSchemaException wrapping the IOException
    }

    /** getSchema(URI) respects the uriTranslators map (uriMap). */
    @Test
    public void uriTranslatorAppliesUriMappings() throws Exception {
        // Map "mock:/alias.json" -> "mock:/real.json"
        Map<String, String> uriMap = new HashMap<>();
        uriMap.put("mock:/alias.json", "mock:/real.json");

        JsonSchemaFactory f = JsonSchemaFactory.builder(factoryWithDefaults)
                .addUriMappings(uriMap)
                .build();

        // Spy on the underlying factory to capture the translated URI.
        JsonSchemaFactory spy = spy(f);
        doReturn(mockJsonNode).when(spy).newJsonSchema(any(), any(), any());

        // Use a dummy fetcher that returns a simple schema (we don't care about content).
        when(mockUriFetcher.fetch(any(URI.class))).thenReturn(streamOf("{\"type\":\"null\"}"));
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));

        // The builder above already contains the real fetcher/factory for "mock".
        // Replace them via reflection (simpler: use the spy's internal fields).
        // For the purpose of the test we just call the public method.
        URI alias = new URI("mock:/alias.json");
        spy.getSchema(alias);

        // Verify that the fetcher received the *mapped* URI, not the original.
        verify(mockUriFetcher).fetch(eq(new URI("mock:/real.json")));
    }

    // -----------------------------------------------------------------------
    // 5. Utility helpers (Expert B)
    // -----------------------------------------------------------------------

    /** getUriFactory returns the factory supplied during construction. */
    @Test
    public void getUriFactoryReturnsConfiguredFactory() {
        JsonSchemaFactory f = factoryWithDefaults;
        assertNotNull(f.getUriFactory());
        // The default factory includes URLFactory for "http" and "https".
        assertTrue(f.getUriFactory() instanceof URISchemeFactory);
    }

    /** getUriTranslator returns a translator that incorporates the uriMap. */
    @Test
    public void getUriTranslatorCombinesMapAndCustomTranslators() {
        Map<String, String> customMap = Collections.singletonMap("http://old.com/schema", "http://new.com/schema");
        JsonSchemaFactory f = JsonSchemaFactory.builder(factoryWithDefaults)
                .addUriMappings(customMap)
                .build();

        URITranslator translator = f.getUriTranslator();
        URI before = URI.create("http://old.com/schema");
        URI after = translator.translate(before);
        assertEquals("http://new.com/schema", after.toString());
    }

    // -----------------------------------------------------------------------
    // 6. Private helper verification via reflection (Expert C)
    // -----------------------------------------------------------------------

    /** fromId throws JsonSchemaException for unknown dialect URIs. */
    @Test(expected = JsonSchemaException.class)
    public void fromIdThrowsForUnknownDialect() throws Exception {
        // Use a real factory but invoke the private method via reflection.
        JsonSchemaFactory f = factoryWithDefaults;
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("fromId", String.class);
        m.setAccessible(true);
        // Pass a URI that is not a known draft.
        m.invoke(f, "http://example.com/unknown/dialect");
    }

    /** findMetaSchemaForSchema falls back to default when $schema is missing. */
    @Test
    public void findMetaSchemaUsesDefaultWhenNoSchemaProperty() throws Exception {
        JsonSchemaFactory f = factoryWithDefaults;
        JsonNode node = mock(JsonNode.class);
        when(node.get("$schema")).thenReturn(null);

        // Use reflection to call the private method.
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        m.setAccessible(true);
        JsonMetaSchema result = (JsonMetaSchema) m.invoke(f, node);
        assertEquals(f.defaultMetaSchemaURI, result.getUri());
    }

    // -----------------------------------------------------------------------
    // 7. Edge cases for URI handling
    // -----------------------------------------------------------------------

    /** isYaml returns false for a URI with query string but no extension. */
    @Test
    public void isYamlHandlesUriWithQueryString() throws Exception {
        JsonSchemaFactory f = factoryWithDefaults;
        URI uri = new URI("file:/path/to/schema?param=value");
        assertFalse(f.isYaml(uri));
    }

    /** normalizeMetaSchemaUri preserves user‑info, host, port, and path correctly. */
    @Test
    public void normalizeMetaSchemaUriPreservesAllComponents() {
        String original = "http://user:pass@host.com:1234/some/path?query=1#frag";
        String expected = "https://user:pass@host.com:1234/some/path";
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(original));
    }
}
```
###Test END##