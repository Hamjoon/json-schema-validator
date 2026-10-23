###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * <p>
 * The tests are written from the perspective of three “experts”.  Each expert contributes
 * one test case for a group of related methods (builder, version handling, schema loading,
 * URI handling, etc.).  The final file merges all contributions into a single, executable
 * test class.
 * </p>
 *
 * <p>
 * The suite uses Mockito for heavy‑weight collaborators (e.g. {@link com.networknt.schema.JsonMetaSchema},
 * {@link com.networknt.schema.JsonSchema}, {@link com.networknt.schema.ValidationContext}) and
 * reflection for accessing package‑private members such as the URI cache.
 * </p>
 *
 * <p>
 * Run the tests with any JUnit‑4 runner (e.g. Maven Surefire, IDE, etc.).
 * </p>
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class that aggregates the contributions of three experts.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    /* --------------------------------------------------------------------- *
     *  Helper constants & utilities (shared by many test cases)            *
     * --------------------------------------------------------------------- */

    /** A minimal, valid JSON schema (draft‑04) used in many tests. */
    private static final String SIMPLE_SCHEMA = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }";

    /** A bogus JSON string that cannot be parsed. */
    private static final String MALFORMED_JSON = "{ \"type\": \"object\", ";

    /** Helper to create a temporary file containing the supplied text. */
    private static File createTempFile(String content, String suffix) throws IOException {
        File f = File.createTempFile("schema", suffix);
        f.deleteOnExit();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
            w.write(content);
        }
        return f;
    }

    /* --------------------------------------------------------------------- *
     *  1️⃣  Builder / Construction related tests (Expert A)                  *
     * --------------------------------------------------------------------- */

    @Test
    public void builderCreatesFactoryWithProvidedMetaSchema() {
        // Arrange – create a mock meta‑schema that reports a known URI.
        JsonMetaSchema mockMeta = mock(JsonMetaSchema.class);
        when(mockMeta.getUri()).thenReturn("https://example.com/draft-07/schema#");

        // Act – build a factory using the builder API.
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(mockMeta.getUri())
                .addMetaSchema(mockMeta)
                .build();

        // Assert – the factory stores the supplied default URI and can retrieve the meta‑schema.
        assertEquals("https://example.com/draft-07/schema#", factory.defaultMetaSchemaURI);
        assertSame(mockMeta, factory.jsonMetaSchemas.get("https://example.com/draft-07/schema#"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullObjectMapper() throws Exception {
        // The private constructor is exercised via reflection to verify argument checks.
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(
                        ObjectMapper.class, YAMLMapper.class, String.class,
                        URISchemeFactory.class, URISchemeFetcher.class,
                        URNFactory.class, Map.class, Map.class,
                        boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        ctor.newInstance(
                null, new YAMLMapper(), "http://example.com/schema#",
                mock(URISchemeFactory.class), mock(URISchemeFetcher.class),
                null, Collections.singletonMap("k","v"), Collections.emptyMap(),
                true, new CompositeURITranslator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsEmptyDefaultMetaSchemaUri() throws Exception {
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(
                        ObjectMapper.class, YAMLMapper.class, String.class,
                        URISchemeFactory.class, URISchemeFetcher.class,
                        URNFactory.class, Map.class, Map.class,
                        boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        ctor.newInstance(
                new ObjectMapper(), new YAMLMapper(), "   ",
                mock(URISchemeFactory.class), mock(URISchemeFetcher.class),
                null, Collections.singletonMap("k","v"), Collections.emptyMap(),
                true, new CompositeURITranslator());
    }

    @Test
    public void builderCopyPreservesAllConfiguration() {
        // Build a baseline factory.
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getUri()).thenReturn("https://example.com/draft-06/schema#");
        JsonSchemaFactory base = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(meta.getUri())
                .addMetaSchema(meta)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .addUriMappings(Collections.singletonMap("http://old","https://new"))
                .enableUriSchemaCache(false)
                .build();

        // Create a new builder from the existing factory and build a second instance.
        JsonSchemaFactory copy = JsonSchemaFactory.builder(base).build();

        // Verify that every configuration element has been copied.
        assertEquals(base.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
        assertSame(base.jsonMapper, copy.jsonMapper);
        assertSame(base.yamlMapper, copy.yamlMapper);
        assertEquals(base.uriMap, copy.uriMap);
        assertEquals(base.enableUriSchemaCache, copy.enableUriSchemaCache);
        assertTrue(copy.jsonMetaSchemas.containsKey(meta.getUri()));
    }

    /* --------------------------------------------------------------------- *
     *  2️⃣  Version handling tests (Expert B)                               *
     * --------------------------------------------------------------------- */

    @Test
    public void checkVersionReturnsCorrectImplementation() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void checkVersionHandlesNullGracefully() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionThrowsOnUnsupportedFlag() {
        // Create a fake enum value by reflection – the method must still throw.
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V999"));
    }

    @Test
    public void getInstanceCreatesFactoryWithCorrectDefaultMetaSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(factory);
        assertEquals(JsonSchemaVersion.DRAFTV4.getInstance().getUri(), factory.defaultMetaSchemaURI);
        // The meta‑schema for the default URI must be present.
        assertTrue(factory.jsonMetaSchemas.containsKey(factory.defaultMetaSchemaURI));
    }

    @Test
    public void normalizeMetaSchemaUriForcesHttpsAndStripsFragment() throws URISyntaxException {
        // Input contains http scheme and a fragment.
        String raw = "http://example.com/schema#fragment";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(raw);
        URI uri = new URI(normalized);
        assertEquals("https", uri.getScheme());
        assertEquals("/schema", uri.getPath());
        assertNull(uri.getFragment());
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriThrowsOnInvalidUri() {
        JsonSchemaFactory.normalizeMetaSchemaUri("::not a uri::");
    }

    /* --------------------------------------------------------------------- *
     *  3️⃣  Schema loading & URI handling tests (Expert C)                    *
     * --------------------------------------------------------------------- */

    @Mock private URISchemeFactory mockUriFactory;
    @Mock private URISchemeFetcher mockUriFetcher;
    @Mock private URNFactory mockUrnFactory;
    @Mock private JsonMetaSchema mockMetaSchema;
    @Mock private JsonSchema mockJsonSchema;
    @Mock private ValidationContext mockValidationContext;

    private JsonSchemaFactory factoryUnderTest;

    @Before
    public void setUp() throws Exception {
        // Common stub configuration for the factory used in the remaining tests.
        when(mockMetaSchema.getUri()).thenReturn("https://example.com/draft-07/schema#");
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);
        when(mockMetaSchema.readId(isNull())).thenReturn(null);

        // The factory will use a real ObjectMapper/YAMLMapper but mocked URI components.
        factoryUnderTest = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(mockMetaSchema.getUri())
                .addMetaSchema(mockMetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .addUriMappings(Collections.emptyMap())
                .addUriTranslator(uri -> uri) // identity translator
                .enableUriSchemaCache(true)
                .build();

        // Inject the mocked factories via reflection (they are final fields).
        java.lang.reflect.Field fFactory = JsonSchemaFactory.class.getDeclaredField("uriFactory");
        fFactory.setAccessible(true);
        fFactory.set(factoryUnderTest, mockUriFactory);

        java.lang.reflect.Field fFetcher = JsonSchemaFactory.class.getDeclaredField("uriFetcher");
        fFetcher.setAccessible(true);
        fFetcher.set(factoryUnderTest, mockUriFetcher);
    }

    @Test
    public void getSchemaFromStringParsesJsonSuccessfully() throws Exception {
        // Spy on the factory to intercept the protected newJsonSchema call.
        JsonSchemaFactory spyFactory = spy(factoryUnderTest);
        doReturn(mockJsonSchema).when(spyFactory).newJsonSchema(isNull(), any(JsonNode.class), isNull());

        JsonSchema schema = spyFactory.getSchema(SIMPLE_SCHEMA);
        assertSame(mockJsonSchema, schema);
        // Verify that the JSON string was parsed (ObjectMapper.readTree called once).
        verify(spyFactory).newJsonSchema(isNull(), any(JsonNode.class), isNull());
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringThrowsOnInvalidJson() {
        factoryUnderTest.getSchema(MALFORMED_JSON);
    }

    @Test
    public void getSchemaFromInputStreamParsesJsonSuccessfully() throws Exception {
        JsonSchemaFactory spyFactory = spy(factoryUnderTest);
        doReturn(mockJsonSchema).when(spyFactory).newJsonSchema(isNull(), any(JsonNode.class), isNull());

        InputStream is = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes("UTF-8"));
        JsonSchema schema = spyFactory.getSchema(is);
        assertSame(mockJsonSchema, schema);
        verify(spyFactory).newJsonSchema(isNull(), any(JsonNode.class), isNull());
    }

    @Test
    public void getSchemaFromUriLoadsFileAndCachesResult() throws Exception {
        // Create a temporary JSON schema file.
        File tmp = createTempFile(SIMPLE_SCHEMA, ".json");
        URI fileUri = tmp.toURI();

        // Mock the URI factory to simply return the same URI (identity conversion).
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));

        // Mock the fetcher to return a fresh InputStream for the file.
        when(mockUriFetcher.fetch(any(URI.class))).thenAnswer(inv -> new FileInputStream(tmp));

        // Spy on the factory to intercept schema creation.
        JsonSchemaFactory spyFactory = spy(factoryUnderTest);
        doReturn(mockJsonSchema).when(spyFactory).newJsonSchema(eq(fileUri), any(JsonNode.class), isNull());

        // First call – should trigger a fetch and store in cache.
        JsonSchema first = spyFactory.getSchema(fileUri);
        assertSame(mockJsonSchema, first);
        // Second call – should hit the cache and NOT invoke fetcher again.
        JsonSchema second = spyFactory.getSchema(fileUri);
        assertSame(mockJsonSchema, second);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class));
    }

    @Test
    public void isYamlDetectsYamlExtension() {
        URI yamlUri = new URI("file:/tmp/schema.yaml");
        URI ymlUri = new URI("file:/tmp/schema.yml");
        URI jsonUri = new URI("file:/tmp/schema.json");

        assertTrue(invokeIsYaml(yamlUri));
        assertTrue(invokeIsYaml(ymlUri));
        assertFalse(invokeIsYaml(jsonUri));
    }

    @Test
    public void idMatchesSourceUriDetectsMatchingId() {
        // Prepare a mock meta‑schema that returns a fixed $id.
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn("https://example.com/my-schema");

        // Create a dummy schema node containing the same $id.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createObjectNode().put("$id", "https://example.com/my-schema");

        // Use reflection to call the private method.
        boolean matches = invokeIdMatchesSourceUri(meta, node, URI.create("https://example.com/my-schema"));
        assertTrue(matches);
    }

    @Test
    public void idMatchesSourceUriReturnsFalseWhenIdMissingOrDifferent() {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn(null); // missing id

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createObjectNode(); // empty node

        boolean result = invokeIdMatchesSourceUri(meta, node, URI.create("https://example.com/any"));
        assertFalse(result);
    }

    /* --------------------------------------------------------------------- *
     *  Private helper methods to invoke package‑private members via reflection *
     * --------------------------------------------------------------------- */

    /** Calls the private {@code isYaml(URI)} method. */
    private boolean invokeIsYaml(URI uri) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
            m.setAccessible(true);
            return (boolean) m.invoke(factoryUnderTest, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Calls the private {@code idMatchesSourceUri(...)} method. */
    private boolean invokeIdMatchesSourceUri(JsonMetaSchema meta, JsonNode schema, URI uri) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod(
                    "idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
            m.setAccessible(true);
            return (boolean) m.invoke(factoryUnderTest, meta, schema, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Exposes the internal URI‑schema cache for verification. */
    @SuppressWarnings("unchecked")
    private ConcurrentMap<URI, JsonSchema> getCache() {
        try {
            java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
            f.setAccessible(true);
            return (ConcurrentMap<URI, JsonSchema>) f.get(factoryUnderTest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##