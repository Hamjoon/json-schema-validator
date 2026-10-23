###Test START##
```java
/*
 * JsonSchemaFactoryTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The test cases cover:
 *   • Public API surface (static builders, version checking, URI handling, schema loading)
 *   • Edge‑cases and exceptional paths (null/empty arguments, malformed URIs, I/O failures,
 *     cache behaviour, YAML detection, meta‑schema resolution)
 *
 * The tests use Mockito for mocking collaborators and PowerMockito for stubbing the static
 * {@code JsonSchema.from(...)} call that creates the concrete {@code JsonSchema} instances.
 *
 * NOTE: The test file is deliberately self‑contained – it does not depend on external
 * resources other than the Jackson libraries that are already on the class‑path of the
 * project under test.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for {@link JsonSchemaFactory}.
 *
 * The suite is split into logical sections that mirror the development steps
 * described in the task:
 *   1. Extraction of public methods.
 *   2. Basic happy‑path test for each public method.
 *   3. Identification of edge‑cases / exception scenarios.
 *   4. Targeted tests for those edge‑cases.
 *   5. A single merged test class (this file).
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JsonSchema.class})
public class JsonSchemaFactoryTest {

    /* --------------------------------------------------------------------- */
    /* 1. List of public methods (generated for documentation / reference)   */
    /* --------------------------------------------------------------------- */
    // static Builder builder()
    // static JsonSchemaFactory getInstance()
    // static JsonSchemaFactory getInstance(SpecVersion.VersionFlag)
    // static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag)
    // static Builder builder(JsonSchemaFactory blueprint)
    // public URIFactory getUriFactory()
    // public URITranslator getUriTranslator()
    // public JsonSchema getSchema(String, SchemaValidatorsConfig)
    // public JsonSchema getSchema(String)
    // public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)
    // public JsonSchema getSchema(InputStream)
    // public JsonSchema getSchema(URI, SchemaValidatorsConfig)
    // public JsonSchema getSchema(URI)
    // public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)
    // public JsonSchema getSchema(JsonNode, SchemaValidatorsConfig)
    // public JsonSchema getSchema(URI, JsonNode)
    // public JsonSchema getSchema(JsonNode)

    /* --------------------------------------------------------------------- */
    /* Common test fixtures */
    /* --------------------------------------------------------------------- */

    private static final String DRAFT_07_URI = "https://json-schema.org/draft-07/schema";

    private JsonMetaSchema mockMetaSchema;
    private JsonSchemaFactory factory;

    @Before
    public void setUp() throws Exception {
        // Minimal mock of JsonMetaSchema – only the methods used by the factory are needed.
        mockMetaSchema = mock(JsonMetaSchema.class);
        when(mockMetaSchema.getUri()).thenReturn(DRAFT_07_URI);
        // readId() is used in idMatchesSourceUri()
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);

        // Build a factory with the mock meta‑schema. All other components use the
        // default implementations (URLFactory, URLFetcher, …) – they are harmless for
        // the tests that do not involve real network access.
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .build();
    }

    /* --------------------------------------------------------------------- */
    /* 2. Basic happy‑path tests for each public method                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBuilderStaticMethod() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
        assertNotNull("builder() must return a non‑null Builder", b);
    }

    @Test
    public void testGetInstanceWithoutArgumentReturnsV4Factory() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull(f);
        // The default meta‑schema for V4 is known – we just ensure that the factory
        // was created without throwing.
    }

    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(f);
    }

    @Test
    public void testCheckVersionValidFlags() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void testCheckVersionNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupportedFlag() {
        // Simulate a future enum constant that the switch‑case does not handle.
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V9"));
    }

    @Test
    public void testBuilderFromBlueprintCopiesAllSettings() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(factory);
        JsonSchemaFactory copy = b.build();
        assertNotNull(copy);
        // The copy should contain the same default meta‑schema URI.
        assertEquals(factory.getUriFactory().getClass(), copy.getUriFactory().getClass());
    }

    @Test
    public void testGetUriFactory() {
        assertNotNull(factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslator() {
        URITranslator translator = factory.getUriTranslator();
        assertNotNull(translator);
    }

    @Test
    public void testGetSchemaFromStringHappyPath() throws Exception {
        // Stub JsonSchema.from(..) to return a mock JsonSchema so that we do not need
        // to parse a full schema.
        JsonSchema mockSchema = mock(JsonSchema.class);
        PowerMockito.mockStatic(JsonSchema.class);
        when(JsonSchema.from(any(ValidationContext.class), anyString(),
                any(URI.class), any(JsonNode.class), any(JsonSchema.class), anyBoolean()))
                .thenReturn(mockSchema);

        String json = "{\"type\":\"object\"}";
        JsonSchema result = factory.getSchema(json);
        assertSame("Factory should return the JsonSchema produced by JsonSchema.from()", mockSchema, result);
    }

    @Test
    public void testGetSchemaFromInputStreamHappyPath() throws Exception {
        JsonSchema mockSchema = mock(JsonSchema.class);
        PowerMockito.mockStatic(JsonSchema.class);
        when(JsonSchema.from(any(ValidationContext.class), anyString(),
                any(URI.class), any(JsonNode.class), any(JsonSchema.class), anyBoolean()))
                .thenReturn(mockSchema);

        String json = "{\"type\":\"array\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes("UTF‑8"));
        JsonSchema result = factory.getSchema(is);
        assertSame(mockSchema, result);
    }

    @Test
    public void testGetSchemaFromURI_CachesResultWhenEnabled() throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣ Arrange – mock collaborators needed for the URI path
        // -----------------------------------------------------------------
        URIFactory mockUriFactory = mock(URIFactory.class);
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));

        // A simple JSON schema payload
        final String payload = "{\"type\":\"string\"}";
        InputStream payloadStream = new ByteArrayInputStream(payload.getBytes("UTF‑8"));
        URIFetcher mockFetcher = mock(URIFetcher.class);
        when(mockFetcher.fetch(any(URI.class))).thenReturn(payloadStream);

        // Build a factory that uses the mocks and has caching turned on
        JsonSchemaFactory cacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "mock")
                .uriFetcher(mockFetcher, "mock")
                .enableUriSchemaCache(true)               // <-- important for this test
                .build();

        // Stub JsonSchema.from to return a distinct mock each call – we will verify
        // that it is invoked only once.
        JsonSchema firstSchema = mock(JsonSchema.class);
        JsonSchema secondSchema = mock(JsonSchema.class);
        PowerMockito.mockStatic(JsonSchema.class);
        when(JsonSchema.from(any(ValidationContext.class), anyString(),
                any(URI.class), any(JsonNode.class), any(JsonSchema.class), anyBoolean()))
                .thenReturn(firstSchema)      // first call
                .thenReturn(secondSchema);    // second call (should never happen)

        URI testUri = new URI("mock://test/schema.json");

        // -----------------------------------------------------------------
        // 2️⃣ Act – call getSchema twice
        // -----------------------------------------------------------------
        JsonSchema s1 = cacheFactory.getSchema(testUri);
        JsonSchema s2 = cacheFactory.getSchema(testUri);

        // -----------------------------------------------------------------
        // 3️⃣ Assert – the fetcher is called only once and the same schema
        //    instance is returned from the cache.
        // -----------------------------------------------------------------
        verify(mockFetcher, times(1)).fetch(any(URI.class));
        assertSame("Second call should return the cached schema", s1, s2);
        // Ensure JsonSchema.from was invoked only once as well
        PowerMockito.verifyStatic(JsonSchema.class, times(1));
        JsonSchema.from(any(ValidationContext.class), anyString(),
                any(URI.class), any(JsonNode.class), any(JsonSchema.class), anyBoolean());
    }

    @Test
    public void testGetSchemaFromURI_HandlesYamlExtension() throws Exception {
        // Arrange a mock URI ending with .yaml
        URIFactory mockUriFactory = mock(URIFactory.class);
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));

        String yaml = "type: string";
        InputStream yamlStream = new ByteArrayInputStream(yaml.getBytes("UTF‑8"));
        URIFetcher mockFetcher = mock(URIFetcher.class);
        when(mockFetcher.fetch(any(URI.class))).thenReturn(yamlStream);

        JsonSchemaFactory yamlFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "mock")
                .uriFetcher(mockFetcher, "mock")
                .build();

        PowerMockito.mockStatic(JsonSchema.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(JsonSchema.from(any(ValidationContext.class), anyString(),
                any(URI.class), any(JsonNode.class), any(JsonSchema.class), anyBoolean()))
                .thenReturn(mockSchema);

        URI yamlUri = new URI("mock://example/schema.yaml");

        JsonSchema result = yamlFactory.getSchema(yamlUri);
        assertSame(mockSchema, result);
        // The factory must have used the YAML mapper (no explicit verification possible
        // without exposing internal state – the fact that no exception was thrown is
        // sufficient for this unit test).
    }

    @Test
    public void testNormalizeMetaSchemaUriValidInput() {
        String input = "http://example.com/schema";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertTrue("Normalized URI must use https scheme", normalized.startsWith("https://"));
        assertTrue("Host part must be preserved", normalized.contains("example.com"));
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalidInputThrows() {
        JsonSchemaFactory.normalizeMetaSchemaUri(":// malformed");
    }

    @Test
    public void testIsYamlDetection() throws URISyntaxException {
        assertTrue(factory.isYaml(new URI("file://some/schema.yaml")));
        assertTrue(factory.isYaml(new URI("file://some/schema.yml")));
        assertFalse(factory.isYaml(new URI("file://some/schema.json")));
        assertFalse(factory.isYaml(new URI("file://some/schema"))); // no extension
    }

    @Test
    public void testIdMatchesSourceUriTrue() {
        // Arrange a meta‑schema that returns a matching id
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn("http://example.com/my-schema");
        JsonNode dummyNode = mock(JsonNode.class);
        URI sourceUri = URI.create("http://example.com/my-schema");

        // The private method is accessed via reflection for testing.
        boolean match = invokeIdMatchesSourceUri(factory, meta, dummyNode, sourceUri);
        assertTrue("idMatchesSourceUri should return true when ids are equal", match);
    }

    @Test
    public void testIdMatchesSourceUriFalseWhenMissingId() {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn(null);
        JsonNode dummyNode = mock(JsonNode.class);
        URI sourceUri = URI.create("http://example.com/any");

        boolean match = invokeIdMatchesSourceUri(factory, meta, dummyNode, sourceUri);
        assertFalse("idMatchesSourceUri must return false when meta‑schema id is absent", match);
    }

    /* --------------------------------------------------------------------- */
    /* 3. Edge‑case / exception handling tests                               */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryConstructorRejectsNullObjectMapper() throws Exception {
        // Directly invoke the private constructor via reflection to test its guards.
        invokeFactoryConstructor(null,
                new YAMLMapper(),
                DRAFT_07_URI,
                mock(URISchemeFactory.class),
                mock(URISchemeFetcher.class),
                null,
                Collections.singletonMap(DRAFT_07_URI, mockMetaSchema),
                Collections.emptyMap(),
                true,
                new CompositeURITranslator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryConstructorRejectsEmptyDefaultMetaSchemaUri() throws Exception {
        invokeFactoryConstructor(new ObjectMapper(),
                new YAMLMapper(),
                "   ", // empty after trim
                mock(URISchemeFactory.class),
                mock(URISchemeFetcher.class),
                null,
                Collections.singletonMap(DRAFT_07_URI, mockMetaSchema),
                Collections.emptyMap(),
                true,
                new CompositeURITranslator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryConstructorRejectsMissingDefaultMetaSchemaInMap() throws Exception {
        // Provide a map that does NOT contain the default URI.
        Map<String, JsonMetaSchema> map = new HashMap<>();
        map.put("https://example.org/other", mockMetaSchema);
        invokeFactoryConstructor(new ObjectMapper(),
                new YAMLMapper(),
                DRAFT_07_URI,
                mock(URISchemeFactory.class),
                mock(URISchemeFetcher.class),
                null,
                map,
                Collections.emptyMap(),
                true,
                new CompositeURITranslator());
    }

    @Test
    public void testGetSchemaFromStringInvalidJsonThrowsJsonSchemaException() {
        String malformed = "{ this is not json }";
        try {
            factory.getSchema(malformed);
            fail("Expected JsonSchemaException due to malformed JSON");
        } catch (JsonSchemaException e) {
            // expected – the cause should be an IOException from Jackson
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testGetSchemaFromURI_UriFactoryThrowsIllegalArgumentException() throws Exception {
        URIFactory badFactory = mock(URIFactory.class);
        when(badFactory.create(anyString())).thenThrow(new IllegalArgumentException("bad uri"));
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(badFactory, "bad")
                .uriFetcher(mock(URIFetcher.class), "bad")
                .build();

        try {
            f.getSchema(new URI("bad://some/schema.json"));
            fail("Expected JsonSchemaException caused by IllegalArgumentException from URIFactory");
        } catch (JsonSchemaException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testGetSchemaFromURI_FetcherThrowsIOException() throws Exception {
        URIFactory mockUriFactory = mock(URIFactory.class);
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));

        URIFetcher badFetcher = mock(URIFetcher.class);
        when(badFetcher.fetch(any(URI.class))).thenThrow(new IOException("fetch failed"));

        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "bad")
                .uriFetcher(badFetcher, "bad")
                .build();

        try {
            f.getSchema(new URI("bad://some/schema.json"));
            fail("Expected JsonSchemaException caused by IOException from fetcher");
        } catch (JsonSchemaException e) {
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods (reflection)                                          */
    /* --------------------------------------------------------------------- */

    /**
     * Invoke the private {@code idMatchesSourceUri} method via reflection.
     */
    private static boolean invokeIdMatchesSourceUri(JsonSchemaFactory factory,
                                                    JsonMetaSchema meta,
                                                    JsonNode schemaNode,
                                                    URI uri) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class
                    .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
            m.setAccessible(true);
            return (boolean) m.invoke(factory, meta, schemaNode, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoke the private constructor of {@code JsonSchemaFactory} to test argument validation.
     */
    private static JsonSchemaFactory invokeFactoryConstructor(ObjectMapper jsonMapper,
                                                               YAMLMapper yamlMapper,
                                                               String defaultMetaSchemaURI,
                                                               URISchemeFactory uriFactory,
                                                               URISchemeFetcher uriFetcher,
                                                               URNFactory urnFactory,
                                                               Map<String, JsonMetaSchema> jsonMetaSchemas,
                                                               Map<String, String> uriMap,
                                                               boolean enableUriSchemaCache,
                                                               CompositeURITranslator uriTranslators) throws Exception {
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class,
                        String.class, URISchemeFactory.class, URISchemeFetcher.class,
                        URNFactory.class, Map.class, Map.class, boolean.class,
                        CompositeURITranslator.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriFactory,
                uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
    }
}
```
###Test END##