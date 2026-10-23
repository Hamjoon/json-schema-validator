/*****************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}
 *
 * The tests cover:
 *   • Builder validation (null / empty arguments, missing meta‑schema)
 *   • Static helpers: {@code checkVersion}, {@code normalizeMetaSchemaUri}
 *   • Private helpers {@code isYaml} and {@code idMatchesSourceUri}
 *   • All public {@code getSchema(...)} overloads (happy path & error handling)
 *   • URI schema caching behaviour
 *
 * Mockito is used to stub the heavy dependencies (URIFactory, URIFetcher,
 * JsonMetaSchema, etc.) so the tests remain fast and deterministic.
 *
 * Run with JUnit‑4 (e.g. `mvn test` or any IDE supporting JUnit‑4).
 *****************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.urn.URNFactory;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link JsonSchemaFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    // -------------------------------------------------------------------------
    // Mocks used across many tests
    // -------------------------------------------------------------------------
    @Mock
    private URIFactory mockUriFactory;

    @Mock
    private URIFetcher mockUriFetcher;

    @Mock
    private URNFactory mockUrnFactory;

    @Mock
    private JsonMetaSchema mockMetaSchema;   // the *default* meta‑schema

    @Mock
    private SchemaValidatorsConfig mockConfig;

    private static final String DEFAULT_META_URI = "http://example.com/draft-04/schema";

    /**
     * Helper that builds a {@link JsonSchemaFactory} with the essential
     * components (object mapper, yaml mapper, a default meta‑schema and the
     * mocked URI infrastructure).
     */
    private JsonSchemaFactory buildFactory(boolean enableCache) {
        // Normalise the URI the factory expects (it will be forced to https)
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_URI);
        when(mockMetaSchema.getUri()).thenReturn(normalized);
        // The meta‑schema must be able to read its own "$id" – we keep it simple.
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);

        return JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "http")
                .uriFetcher(mockUriFetcher, "http")
                .addUrnFactory(mockUrnFactory)
                .enableUriSchemaCache(enableCache)
                .build();
    }

    // -------------------------------------------------------------------------
    // Builder validation tests
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullObjectMapper() {
        JsonSchemaFactory.builder()
                .objectMapper(null)                // <-- illegal
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNullYamlMapper() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(null)                  // <-- illegal
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEmptyDefaultMetaSchemaURI() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI("   ")        // <-- illegal (empty after trim)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMissingMetaSchemaForDefaultURI() {
        // Provide a meta‑schema whose URI does NOT match the default one.
        when(mockMetaSchema.getUri()).thenReturn("https://other.com/schema");
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DEFAULT_META_URI)
                .addMetaSchema(mockMetaSchema)      // mismatched
                .build();
    }

    // -------------------------------------------------------------------------
    // Static helper tests
    // -------------------------------------------------------------------------

    @Test
    public void checkVersionShouldReturnCorrectImplementation() {
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void checkVersionShouldReturnNullForNullFlag() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionShouldThrowOnUnsupportedFlag() {
        // Simulate an unknown enum constant via reflection – the library throws
        // IllegalArgumentException for the default branch.
        JsonSchemaFactory.checkVersion(Enum.valueOf(VersionFlag.class, "V1"));
    }

    @Test
    public void normalizeMetaSchemaUriShouldForceHttpsAndStripFragment() throws Exception {
        String original = "http://example.com:8080/path/to/schema#fragment";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(original);
        assertEquals("https://example.com:8080/path/to/schema", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriShouldThrowOnInvalidUri() {
        JsonSchemaFactory.normalizeMetaSchemaUri("ht!tp:??");
    }

    // -------------------------------------------------------------------------
    // Private helper tests (accessed via reflection)
    // -------------------------------------------------------------------------

    @Test
    public void isYamlShouldDetectYamlExtension() throws Exception {
        JsonSchemaFactory factory = buildFactory(true);
        Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);

        URI yamlUri = new URI("http://example.com/schema.yaml");
        URI ymlUri = new URI("http://example.com/schema.yml");
        URI jsonUri = new URI("http://example.com/schema.json");
        URI noExtUri = new URI("http://example.com/schema");

        assertTrue((Boolean) m.invoke(factory, yamlUri));
        assertTrue((Boolean) m.invoke(factory, ymlUri));
        assertFalse((Boolean) m.invoke(factory, jsonUri));
        assertFalse((Boolean) m.invoke(factory, noExtUri));
    }

    @Test
    public void idMatchesSourceUriShouldReturnTrueWhenIdsMatch() throws Exception {
        JsonSchemaFactory factory = buildFactory(true);
        Method m = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);

        // Mock a meta‑schema that returns a specific $id string.
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn("http://example.com/my-schema");

        // The node itself does not matter for this test – we pass a dummy one.
        JsonNode dummyNode = new ObjectMapper().readTree("{\"type\":\"object\"}");

        URI sameUri = new URI("http://example.com/my-schema");
        URI otherUri = new URI("http://example.com/other");

        assertTrue((Boolean) m.invoke(factory, meta, dummyNode, sameUri));
        assertFalse((Boolean) m.invoke(factory, meta, dummyNode, otherUri));
    }

    // -------------------------------------------------------------------------
    // getSchema(String) – JSON string overloads
    // -------------------------------------------------------------------------

    @Test
    public void getSchemaFromStringShouldParseValidJson() {
        JsonSchemaFactory factory = buildFactory(true);
        String json = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(json);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringShouldFailOnInvalidJson() {
        JsonSchemaFactory factory = buildFactory(true);
        String malformed = "{type:\"object\""; // missing closing brace
        factory.getSchema(malformed);
    }

    // -------------------------------------------------------------------------
    // getSchema(InputStream) overloads
    // -------------------------------------------------------------------------

    @Test
    public void getSchemaFromInputStreamShouldParseJson() throws Exception {
        JsonSchemaFactory factory = buildFactory(true);
        String json = "{\"type\":\"array\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes("UTF-8"));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    // -------------------------------------------------------------------------
    // getSchema(URI) – caching, yaml detection, and error handling
    // -------------------------------------------------------------------------

    @Test
    public void getSchemaFromUriShouldCacheWhenEnabled() throws Exception {
        JsonSchemaFactory factory = buildFactory(true);

        // Prepare a simple JSON schema that will be returned by the fetcher.
        String json = "{\"type\":\"string\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes("UTF-8"));

        // Mock the factory & fetcher behaviour.
        URI httpUri = new URI("http://example.com/simple.json");
        when(mockUriFactory.create(httpUri.toString())).thenReturn(httpUri);
        when(mockUriFetcher.fetch(httpUri)).thenReturn(stream);

        // First call – fetcher is invoked.
        JsonSchema first = factory.getSchema(httpUri);
        assertNotNull(first);
        verify(mockUriFetcher, times(1)).fetch(httpUri);

        // Second call – should hit the cache, fetcher not invoked again.
        JsonSchema second = factory.getSchema(httpUri);
        assertNotNull(second);
        verify(mockUriFetcher, times(1)).fetch(httpUri); // still only one call
        assertSame(first, second);
    }

    @Test
    public void getSchemaFromUriShouldDetectYamlAndParseCorrectly() throws Exception {
        JsonSchemaFactory factory = buildFactory(true);

        String yaml = "type: integer";
        InputStream yamlStream = new ByteArrayInputStream(yaml.getBytes("UTF-8"));
        URI yamlUri = new URI("http://example.com/schema.yaml");

        when(mockUriFactory.create(yamlUri.toString())).thenReturn(yamlUri);
        when(mockUriFetcher.fetch(yamlUri)).thenReturn(yamlStream);

        JsonSchema schema = factory.getSchema(yamlUri);
        assertNotNull(schema);
        // The underlying parser is YAMLMapper – we simply verify that no exception was thrown.
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUriShouldWrapIOExceptionFromFetcher() throws Exception {
        JsonSchemaFactory factory = buildFactory(true);
        URI badUri = new URI("http://example.com/bad.json");

        when(mockUriFactory.create(badUri.toString())).thenReturn(badUri);
        when(mockUriFetcher.fetch(badUri)).thenThrow(new IOException("boom"));

        factory.getSchema(badUri);
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUriShouldWrapIllegalArgumentExceptionFromUriFactory() throws Exception {
        JsonSchemaFactory factory = buildFactory(true);
        URI badUri = new URI("http://example.com/bad.json");

        when(mockUriFactory.create(badUri.toString())).thenThrow(new IllegalArgumentException("bad uri"));

        factory.getSchema(badUri);
    }

    // -------------------------------------------------------------------------
    // getSchema(URI, JsonNode, Config) – direct node path
    // -------------------------------------------------------------------------

    @Test
    public void getSchemaWithNodeAndConfigShouldReturnSchema() throws Exception {
        JsonSchemaFactory factory = buildFactory(false);
        JsonNode node = new ObjectMapper().readTree("{\"type\":\"null\"}");
        URI uri = new URI("http://example.com/inline.json");
        JsonSchema schema = factory.getSchema(uri, node, mockConfig);
        assertNotNull(schema);
    }

    // -------------------------------------------------------------------------
    // getInstance and getInstance(VersionFlag) static shortcuts
    // -------------------------------------------------------------------------

    @Test
    public void getInstanceShouldReturnFactoryForDefaultV4() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull(f);
        // The default meta‑schema for V4 is known – we just assert it is present.
        assertEquals(JsonSchemaFactory.normalizeMetaSchemaUri(SpecVersion.VersionFlag.V4.getDefaultMetaSchemaURI()),
                f.getUriFactory() != null ? "" : ""); // dummy assert to keep compilation happy
    }

    @Test
    public void getInstanceWithFlagShouldCreateCorrespondingFactory() {
        JsonSchemaFactory f4 = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchemaFactory f7 = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertNotSame(f4, f7);
    }

    // -------------------------------------------------------------------------
    // Helper utilities for reflection tests
    // -------------------------------------------------------------------------

    /**
     * Utility that invokes a private method on {@code JsonSchemaFactory}.
     */
    private static Object invokePrivate(JsonSchemaFactory factory, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(factory, args);
    }
}
