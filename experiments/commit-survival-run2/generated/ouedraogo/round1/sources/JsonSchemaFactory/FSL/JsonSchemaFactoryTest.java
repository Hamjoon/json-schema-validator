package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests cover:
 * <ul>
 *   <li>static utility methods (normalizeMetaSchemaUri, checkVersion)</li>
 *   <li>builder validation (missing arguments, successful build)</li>
 *   <li>private helper methods (isYaml, idMatchesSourceUri) via reflection</li>
 *   <li>schema loading overloads (String, InputStream, URI) and error handling</li>
 *   <li>URI‑based schema caching behaviour</li>
 * </ul>
 *
 * Mockito is used to stub out heavy collaborators such as {@link URIFactory},
 * {@link URIFetcher} and {@link JsonMetaSchema}.
 */
public class JsonSchemaFactoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private URIFactory mockUriFactory;
    @Mock
    private URIFetcher mockUriFetcher;
    @Mock
    private URNFactory mockUrnFactory;
    @Mock
    private JsonMetaSchema mockMetaSchema;

    private static final String DRAFT_07_URI_HTTP = "http://json-schema.org/draft-07/schema";
    private static final String DRAFT_07_URI_HTTPS = "https://json-schema.org/draft-07/schema";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // default behaviour for the dummy meta‑schema
        when(mockMetaSchema.getUri()).thenReturn(DRAFT_07_URI_HTTPS);
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);
    }

    /* -------------------------------------------------------------
     *  Static helper methods
     * ------------------------------------------------------------- */
    @Test
    public void normalizeMetaSchemaUri_forcesHttpsAndDropsQueryFragment() throws Exception {
        String source = "http://user:pass@host:8080/path/to/schema.json?query=123#frag";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(source);
        assertEquals("https://user:pass@host:8080/path/to/schema.json", normalized);
    }

    @Test
    public void normalizeMetaSchemaUri_invalidUriThrowsJsonSchemaException() {
        thrown.expect(JsonSchemaException.class);
        JsonSchemaFactory.normalizeMetaSchemaUri("http://[invalid");
    }

    @Test
    public void checkVersion_returnsCorrectVersionInstances() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void checkVersion_nullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void checkVersion_unknownFlagThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V999"));
    }

    /* -------------------------------------------------------------
     *  Builder validation
     * ------------------------------------------------------------- */
    @Test
    public void builder_missingDefaultMetaSchemaUri_throws() {
        thrown.expect(IllegalArgumentException.class);
        JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)   // provides at least one meta‑schema
                .build();                        // defaultMetaSchemaURI is null -> illegal argument
    }

    @Test
    public void builder_missingMetaSchemas_throws() {
        thrown.expect(IllegalArgumentException.class);
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .build();   // no meta‑schemas added
    }

    @Test
    public void builder_defaultMetaSchemaNotRegistered_throws() {
        thrown.expect(IllegalArgumentException.class);
        // meta‑schema uri does NOT match the normalized default -> error
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/unknown")
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test
    public void builder_successfulBuild_returnsFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .addMetaSchema(mockMetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .build();

        assertNotNull(factory);
        assertEquals(DRAFT_07_URI_HTTP, factory.defaultMetaSchemaURI);
    }

    /* -------------------------------------------------------------
     *  Private helper methods (accessed via reflection)
     * ------------------------------------------------------------- */
    @Test
    public void isYaml_detectsYamlAndJsonExtensions() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .addMetaSchema(mockMetaSchema)
                .build();

        // JSON extension – should be false
        URI jsonUri = new URI("http://example.com/schema.json");
        // YAML extensions – should be true
        URI yamlUri = new URI("http://example.com/schema.yaml");
        URI ymlUri = new URI("http://example.com/schema.yml");

        // use reflection to invoke the private method
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);

        assertFalse((Boolean) m.invoke(factory, jsonUri));
        assertTrue((Boolean) m.invoke(factory, yamlUri));
        assertTrue((Boolean) m.invoke(factory, ymlUri));
    }

    @Test
    public void idMatchesSourceUri_behaviour() throws Exception {
        // meta‑schema that pretends the id is "urn:test:id"
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn("urn:test:id");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .addMetaSchema(mockMetaSchema)
                .build();

        java.lang.reflect.Method method = JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);

        // matching case
        URI matchingUri = new URI("urn:test:id");
        JsonNode dummyNode = mock(ObjectMapper.class).readTree("{}"); // dummy, content not used
        boolean matches = (Boolean) method.invoke(factory, mockMetaSchema, dummyNode, matchingUri);
        assertTrue(matches);

        // non‑matching case
        URI otherUri = new URI("urn:test:other");
        boolean notMatches = (Boolean) method.invoke(factory, mockMetaSchema, dummyNode, otherUri);
        assertFalse(notMatches);
    }

    /* -------------------------------------------------------------
     *  getSchema overloads – error handling
     * ------------------------------------------------------------- */
    @Test
    public void getSchema_fromInvalidJsonString_throwsJsonSchemaException() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .addMetaSchema(mockMetaSchema)
                .build();

        thrown.expect(JsonSchemaException.class);
        factory.getSchema("{ invalid json }");
    }

    @Test
    public void getSchema_fromInvalidJsonInputStream_throwsJsonSchemaException() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .addMetaSchema(mockMetaSchema)
                .build();

        InputStream badStream = new ByteArrayInputStream("{ not: json".getBytes());

        thrown.expect(JsonSchemaException.class);
        factory.getSchema(badStream);
    }

    /* -------------------------------------------------------------
     *  URI‑based schema loading & caching
     * ------------------------------------------------------------- */
    @Test
    public void getSchema_fromUri_usesFetcherAndCache() throws Exception {
        // a tiny JSON schema – no $schema keyword, so default meta‑schema is used
        String schemaJson = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes());

        // mock fetcher to return the stream once
        when(mockUriFetcher.fetch(any(URI.class))).thenReturn(schemaStream);

        // mock URI factory – just returns the same URI (no transformation)
        when(mockUriFactory.create(anyString())).thenAnswer(invocation -> new URI((String) invocation.getArguments()[0]));

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "test")
                .uriFetcher(mockUriFetcher, "test")
                .enableUriSchemaCache(true)
                .build();

        URI testUri = new URI("test://my-schema.json");

        // first call – fetcher should be invoked
        JsonSchema schema1 = factory.getSchema(testUri);
        assertNotNull(schema1);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class));

        // second call – cache hit, fetcher must NOT be invoked again
        JsonSchema schema2 = factory.getSchema(testUri);
        assertNotNull(schema2);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class)); // still only one call

        // the two JsonSchema instances should be the same (cached)
        assertSame(schema1, schema2);
    }

    @Test
    public void getSchema_fromUri_whenCacheDisabled_fetcherCalledEveryTime() throws Exception {
        String schemaJson = "{\"type\":\"object\"}";
        InputStream schemaStream1 = new ByteArrayInputStream(schemaJson.getBytes());
        InputStream schemaStream2 = new ByteArrayInputStream(schemaJson.getBytes());

        when(mockUriFetcher.fetch(any(URI.class)))
                .thenReturn(schemaStream1)   // first call
                .thenReturn(schemaStream2); // second call

        when(mockUriFactory.create(anyString())).thenAnswer(invocation -> new URI((String) invocation.getArguments()[0]));

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT_07_URI_HTTP)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "test")
                .uriFetcher(mockUriFetcher, "test")
                .enableUriSchemaCache(false)   // disable caching
                .build();

        URI testUri = new URI("test://my-schema.json");

        JsonSchema s1 = factory.getSchema(testUri);
        JsonSchema s2 = factory.getSchema(testUri);

        // fetcher must have been called twice (once per request)
        verify(mockUriFetcher, times(2)).fetch(any(URI.class));
        assertNotSame(s1, s2);
    }

    /* -------------------------------------------------------------
     *  getInstance & getInstance(VersionFlag) shortcuts
     * ------------------------------------------------------------- */
    @Test
    public void getInstance_deprecatedDelegatesToVersionFlag() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        assertNotNull(factory);
        // The default for the deprecated method is V4 – ensure the factory contains the V4 meta‑schema
        assertTrue(factory.jsonMetaSchemas.containsKey(JsonSchemaFactory.normalizeMetaSchemaUri(new Version4().getInstance().getUri())));
    }

    @Test
    public void getInstance_withSpecificVersionFlag_returnsCorrespondingFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(factory);
        assertTrue(factory.jsonMetaSchemas.containsKey(JsonSchemaFactory.normalizeMetaSchemaUri(new Version7().getInstance().getUri())));
    }

    @Test
    public void getInstance_withNullVersionFlag_returnsNullFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance((SpecVersion.VersionFlag) null);
        assertNull(factory);
    }
}
