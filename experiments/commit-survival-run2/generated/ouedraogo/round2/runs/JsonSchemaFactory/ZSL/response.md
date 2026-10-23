###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JsonSchemaFactoryTest {

    private JsonMetaSchema mockMetaSchema;
    private ObjectMapper mockObjectMapper;
    private YAMLMapper mockYamlMapper;
    private URIFactory mockUriFactory;
    private URIFetcher mockUriFetcher;
    private JsonSchemaFactory factory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        // ----- meta schema mock -------------------------------------------------
        mockMetaSchema = mock(JsonMetaSchema.class);
        when(mockMetaSchema.getUri()).thenReturn("https://example.com/schema");
        // readId is used only in idMatchesSourceUri – return null by default
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);

        // ----- object mapper mock ------------------------------------------------
        mockObjectMapper = mock(ObjectMapper.class);
        mockYamlMapper = mock(YAMLMapper.class);

        // ----- URI factory / fetcher mock ----------------------------------------
        mockUriFactory = mock(URIFactory.class);
        mockUriFetcher = mock(URIFetcher.class);

        // The factory under test – use the builder to inject all mocks
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("https://example.com/schema")
                .addMetaSchema(mockMetaSchema)
                .objectMapper(mockObjectMapper)
                .yamlMapper(mockYamlMapper)
                .uriFactory(mockUriFactory, "http")
                .uriFetcher(mockUriFetcher, "http")
                .enableUriSchemaCache(true)          // enable cache for the caching test
                .build();
    }

    // -------------------------------------------------------------------------
    //  Builder / construction tests
    // -------------------------------------------------------------------------

    @Test
    public void testBuilderCreatesFactory() {
        assertNotNull(factory);
    }

    // -------------------------------------------------------------------------
    //  checkVersion / getInstance tests
    // -------------------------------------------------------------------------

    @Test
    public void testCheckVersionValidAndNull() {
        // null input should return null
        assertNull(JsonSchemaFactory.checkVersion(null));

        // each enum constant should map to a concrete JsonSchemaVersion
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            assertNotNull(JsonSchemaFactory.checkVersion(flag));
        }
    }

    @Test
    public void testGetInstanceCreatesFactoryWithCorrectDefaultMetaSchema() throws Exception {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        // The default meta‑schema URI of the created factory should be the URI of the V4 meta‑schema
        String expected = JsonSchemaFactory.normalizeMetaSchemaUri(
                JsonSchemaVersion.DRAFTV4.getInstance().getUri());
        // Use reflection to read the private field
        java.lang.reflect.Field field = JsonSchemaFactory.class.getDeclaredField("defaultMetaSchemaURI");
        field.setAccessible(true);
        String actual = (String) field.get(f);
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    //  normalizeMetaSchemaUri tests
    // -------------------------------------------------------------------------

    @Test
    public void testNormalizeMetaSchemaUriConvertsToHttps() {
        String original = "http://example.com/schema";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(original);
        assertTrue(normalized.startsWith("https://"));
        assertTrue(normalized.endsWith("/schema"));
    }

    @Test
    public void testNormalizeMetaSchemaUriInvalidThrowsException() {
        thrown.expect(JsonSchemaException.class);
        JsonSchemaFactory.normalizeMetaSchemaUri("::invalid::");
    }

    // -------------------------------------------------------------------------
    //  isYaml detection tests
    // -------------------------------------------------------------------------

    @Test
    public void testIsYamlDetection() throws Exception {
        java.lang.reflect.Method method = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        method.setAccessible(true);

        URI yaml1 = new URI("file:/tmp/schema.yaml");
        URI yaml2 = new URI("file:/tmp/schema.yml");
        URI json = new URI("file:/tmp/schema.json");
        URI noExt = new URI("file:/tmp/schema");

        assertTrue((Boolean) method.invoke(factory, yaml1));
        assertTrue((Boolean) method.invoke(factory, yaml2));
        assertFalse((Boolean) method.invoke(factory, json));
        assertFalse((Boolean) method.invoke(factory, noExt));
    }

    // -------------------------------------------------------------------------
    //  idMatchesSourceUri tests (private method)
    // -------------------------------------------------------------------------

    @Test
    public void testIdMatchesSourceUriTrueAndFalse() throws Exception {
        java.lang.reflect.Method method = JsonSchemaFactory.class.getDeclaredMethod(
                "idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);

        // Prepare a schema node (empty is fine – readId is mocked)
        JsonNode schemaNode = new ObjectNode(JsonNodeFactory.instance);

        // Case 1 – meta schema returns null → false
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);
        boolean result = (Boolean) method.invoke(factory, mockMetaSchema, schemaNode,
                new URI("http://example.com/schema"));
        assertFalse(result);

        // Case 2 – meta schema returns exact URI string → true
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn("http://example.com/schema");
        result = (Boolean) method.invoke(factory, mockMetaSchema, schemaNode,
                new URI("http://example.com/schema"));
        assertTrue(result);
    }

    // -------------------------------------------------------------------------
    //  getSchema(String) – happy path and error handling
    // -------------------------------------------------------------------------

    @Test
    public void testGetSchemaFromStringDelegatesToNewJsonSchema() throws Exception {
        // Mock the parsing of the JSON string
        ObjectNode parsedNode = new ObjectNode(JsonNodeFactory.instance);
        when(mockObjectMapper.readTree("{\"type\":\"object\"}")).thenReturn(parsedNode);

        // Spy the factory so we can verify that newJsonSchema is called
        JsonSchemaFactory spyFactory = Mockito.spy(factory);
        JsonSchema mockSchema = mock(JsonSchema.class);
        doReturn(mockSchema).when(spyFactory).newJsonSchema(any(), any(JsonNode.class), any());

        JsonSchema result = spyFactory.getSchema("{\"type\":\"object\"}");
        assertSame(mockSchema, result);
        verify(spyFactory, times(1)).newJsonSchema(isNull(), eq(parsedNode), isNull());
    }

    @Test
    public void testGetSchemaFromStringIOExceptionWrapsJsonSchemaException() throws Exception {
        when(mockObjectMapper.readTree(anyString())).thenThrow(new IOException("boom"));
        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("boom");
        factory.getSchema("{invalid}");
    }

    // -------------------------------------------------------------------------
    //  getSchema(URI) – caching behaviour and error handling
    // -------------------------------------------------------------------------

    @Test
    public void testGetSchemaFromUriUsesFetcherAndCachesResult() throws Exception {
        // --- prepare a simple JSON schema as InputStream -----------------------
        String json = "{\"type\":\"object\"}";
        InputStream schemaStream = new ByteArrayInputStream(json.getBytes());

        // URI translation / creation
        URI originalUri = new URI("http://example.com/my-schema.json");
        URI mappedUri = new URI("http://example.com/my-schema.json");
        when(mockUriFactory.create(anyString())).thenReturn(mappedUri);
        when(mockUriFetcher.fetch(mappedUri)).thenReturn(schemaStream);

        // parsing the InputStream
        ObjectNode parsedNode = new ObjectNode(JsonNodeFactory.instance);
        when(mockObjectMapper.readTree(schemaStream)).thenReturn(parsedNode);

        // Mock static JsonSchema.from to avoid the heavy internal creation
        JsonSchema mockSchema = mock(JsonSchema.class);
        try (MockedStatic<JsonSchema> mocked = Mockito.mockStatic(JsonSchema.class)) {
            mocked.when(() -> JsonSchema.from(any(), anyString(), any(), any(), any(), anyBoolean()))
                    .thenReturn(mockSchema);

            // First call – should invoke the fetcher
            JsonSchema first = factory.getSchema(originalUri);
            assertSame(mockSchema, first);
            verify(mockUriFetcher, times(1)).fetch(mappedUri);

            // Second call – should hit the cache, fetcher not called again
            JsonSchema second = factory.getSchema(originalUri);
            assertSame(mockSchema, second);
            verifyNoMoreInteractions(mockUriFetcher);
        }
    }

    @Test
    public void testGetSchemaFromUriFetcherIOExceptionWrapped() throws Exception {
        URI uri = new URI("http://example.com/bad-schema.json");
        URI mapped = new URI("http://example.com/bad-schema.json");
        when(mockUriFactory.create(anyString())).thenReturn(mapped);
        when(mockUriFetcher.fetch(mapped)).thenThrow(new IOException("fetch failed"));

        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("fetch failed");
        factory.getSchema(uri);
    }

    @Test
    public void testGetSchemaFromUriParsingIOExceptionWrapped() throws Exception {
        URI uri = new URI("http://example.com/invalid.json");
        URI mapped = new URI("http://example.com/invalid.json");
        when(mockUriFactory.create(anyString())).thenReturn(mapped);
        InputStream stream = new ByteArrayInputStream("not a json".getBytes());
        when(mockUriFetcher.fetch(mapped)).thenReturn(stream);
        when(mockObjectMapper.readTree(stream)).thenThrow(new IOException("parse error"));

        thrown.expect(JsonSchemaException.class);
        thrown.expectMessage("parse error");
        factory.getSchema(uri);
    }

    // -------------------------------------------------------------------------
    //  getUriTranslator merges custom mappings
    // -------------------------------------------------------------------------

    @Test
    public void testGetUriTranslatorMergesMap() {
        // Add a custom mapping via the builder (already present in factory)
        Map<String, String> customMap = new HashMap<>();
        customMap.put("http://old.com/schema", "http://new.com/schema");
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder(factory)
                .addUriMappings(customMap)
                .build();

        URITranslator translator = customFactory.getUriTranslator();
        // The translator should contain the mapping we added
        URI original = URI.create("http://old.com/schema");
        URI translated = translator.translate(original);
        assertEquals("http://new.com/schema", translated.toString());
    }

    // -------------------------------------------------------------------------
    //  Additional public helper methods
    // -------------------------------------------------------------------------

    @Test
    public void testGetUriFactoryReturnsConfiguredFactory() {
        assertSame(mockUriFactory, factory.getUriFactory());
    }
}
###Test END##