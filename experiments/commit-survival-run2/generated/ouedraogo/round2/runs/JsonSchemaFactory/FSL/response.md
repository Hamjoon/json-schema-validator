###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests cover:
 * <ul>
 *   <li>Factory builder validation (null arguments, missing meta‑schema, etc.)</li>
 *   <li>Static helpers: {@code checkVersion}, {@code normalizeMetaSchemaUri},
 *       {@code isYaml}, {@code idMatchesSourceUri}</li>
 *   <li>Schema creation from {@code String}, {@code InputStream} and {@code URI}</li>
 *   <li>Caching behaviour when {@code enableUriSchemaCache} is toggled</li>
 * </ul>
 *
 * Mockito is used to stub out heavy collaborators
 * ({@link URIFactory}, {@link URIFetcher}, {@link JsonMetaSchema}) so that the
 * tests focus on the logic inside {@code JsonSchemaFactory}.
 */
public class JsonSchemaFactoryTest {

    private static final String DRAFT07_URI = "http://json-schema.org/draft-07/schema#";

    private JsonMetaSchema mockMetaSchema;
    private ObjectMapper mapper;
    private YAMLMapper yamlMapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();

        // Minimal stub for a JsonMetaSchema – only the methods used by the factory are needed.
        mockMetaSchema = mock(JsonMetaSchema.class);
        when(mockMetaSchema.getUri()).thenReturn(DRAFT07_URI);
        // By default the meta‑schema does not provide an $id.
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);
    }

    /* --------------------------------------------------------------
     *  Static helper methods
     * -------------------------------------------------------------- */
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
    public void testCheckVersionUnsupportedThrows() {
        // Simulate an unknown enum value by using reflection to create a bogus enum instance.
        // The factory will hit the default case and throw IllegalArgumentException.
        SpecVersion.VersionFlag bogus = SpecVersion.VersionFlag.valueOf("V999");
        JsonSchemaFactory.checkVersion(bogus);
    }

    @Test
    public void testNormalizeMetaSchemaUriRewritesToHttps() {
        String httpUri = "http://example.com/schemas/my-schema.json";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(httpUri);
        assertEquals("https://example.com/schemas/my-schema.json", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalidThrows() {
        JsonSchemaFactory.normalizeMetaSchemaUri("://missing-scheme");
    }

    @Test
    public void testIsYamlDetection() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();

        Method isYamlMethod = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYamlMethod.setAccessible(true);

        assertTrue((Boolean) isYamlMethod.invoke(factory, new URI("file:/tmp/schema.yaml")));
        assertTrue((Boolean) isYamlMethod.invoke(factory, new URI("file:/tmp/schema.yml")));
        assertFalse((Boolean) isYamlMethod.invoke(factory, new URI("http://example.com/schema.json")));
        assertFalse((Boolean) isYamlMethod.invoke(factory, new URI("http://example.com/schema")));
    }

    @Test
    public void testIdMatchesSourceUri() throws Exception {
        // Prepare a meta‑schema that returns a concrete $id.
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn("http://example.com/my-schema.json");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(meta)
                .build();

        Method idMatches = JsonSchemaFactory.class.getDeclaredMethod(
                "idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        idMatches.setAccessible(true);

        // Build a dummy JsonNode that would be passed to the method.
        JsonNode dummyNode = mapper.readTree("{}");

        // Positive match
        boolean match = (Boolean) idMatches.invoke(factory, meta, dummyNode,
                new URI("http://example.com/my-schema.json"));
        assertTrue(match);

        // Negative match
        boolean noMatch = (Boolean) idMatches.invoke(factory, meta, dummyNode,
                new URI("http://example.com/other-schema.json"));
        assertFalse(noMatch);
    }

    /* --------------------------------------------------------------
     *  Builder validation
     * -------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenObjectMapperIsNull() {
        JsonSchemaFactory.builder()
                .objectMapper(null)
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenYamlMapperIsNull() {
        JsonSchemaFactory.builder()
                .yamlMapper(null)
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaUriMissing() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(null)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenMetaSchemaMapIsEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                // No call to addMetaSchema()
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaNotProvidedInMap() {
        JsonMetaSchema other = mock(JsonMetaSchema.class);
        when(other.getUri()).thenReturn("http://example.com/other-schema#");

        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(other) // does NOT contain the default URI
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenUriMapIsNull() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema);
        // Use reflection to inject a null uriMap (builder does not expose a setter).
        try {
            java.lang.reflect.Field f = JsonSchemaFactory.Builder.class.getDeclaredField("uriMap");
            f.setAccessible(true);
            f.set(b, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        b.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenUriTranslatorsIsNull() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema);
        try {
            java.lang.reflect.Field f = JsonSchemaFactory.Builder.class.getDeclaredField("uriTranslators");
            f.setAccessible(true);
            f.set(b, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        b.build();
    }

    /* --------------------------------------------------------------
     *  Schema creation from String / InputStream
     * -------------------------------------------------------------- */
    @Test
    public void testGetSchemaFromStringValidJson() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();

        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJsonThrows() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();

        // malformed JSON (missing closing brace)
        factory.getSchema("{\"type\":\"object\"");
    }

    @Test
    public void testGetSchemaFromInputStreamValidJson() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();

        InputStream is = new ByteArrayInputStream("{\"type\":\"array\"}".getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalidJsonThrows() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();

        InputStream is = new ByteArrayInputStream("{invalid-json".getBytes());
        factory.getSchema(is);
    }

    /* --------------------------------------------------------------
     *  URI based loading and caching
     * -------------------------------------------------------------- */
    @Test
    public void testGetSchemaFromUriWithCacheEnabled() throws Exception {
        // Mock factory and fetcher
        URIFactory mockFactory = mock(URIFactory.class);
        URIFetcher mockFetcher = mock(URIFetcher.class);
        when(mockFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        // Return a simple JSON schema on each fetch
        InputStream schemaStream = new ByteArrayInputStream("{\"type\":\"string\"}".getBytes());
        when(mockFetcher.fetch(any(URI.class))).thenReturn(schemaStream);

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockFactory, "http")
                .uriFetcher(mockFetcher, "http")
                .enableUriSchemaCache(true)
                .build();

        URI schemaUri = new URI("http://example.com/simple.json");
        JsonSchema first = factory.getSchema(schemaUri);
        JsonSchema second = factory.getSchema(schemaUri);

        assertSame(first, second); // cache returns the same instance
        verify(mockFetcher, times(1)).fetch(any(URI.class));
    }

    @Test
    public void testGetSchemaFromUriWithCacheDisabled() throws Exception {
        URIFactory mockFactory = mock(URIFactory.class);
        URIFetcher mockFetcher = mock(URIFetcher.class);
        when(mockFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        InputStream schemaStream = new ByteArrayInputStream("{\"type\":\"integer\"}".getBytes());
        when(mockFetcher.fetch(any(URI.class))).thenReturn(schemaStream);

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockFactory, "http")
                .uriFetcher(mockFetcher, "http")
                .enableUriSchemaCache(false)
                .build();

        URI schemaUri = new URI("http://example.com/number.json");
        JsonSchema first = factory.getSchema(schemaUri);
        JsonSchema second = factory.getSchema(schemaUri);

        // Two distinct instances because caching is disabled
        assertNotSame(first, second);
        verify(mockFetcher, times(2)).fetch(any(URI.class));
    }

    @Test
    public void testGetSchemaFromUriIdMatchesSourceUriPath() throws Exception {
        // Meta‑schema that returns an $id equal to the source URI.
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getUri()).thenReturn(DRAFT07_URI);
        when(meta.readId(any(JsonNode.class))).thenReturn("http://example.com/with-id.json");

        URIFactory mockFactory = mock(URIFactory.class);
        URIFetcher mockFetcher = mock(URIFetcher.class);
        when(mockFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        InputStream schemaStream = new ByteArrayInputStream("{\"$id\":\"http://example.com/with-id.json\"}".getBytes());
        when(mockFetcher.fetch(any(URI.class))).thenReturn(schemaStream);

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(meta)
                .uriFactory(mockFactory, "http")
                .uriFetcher(mockFetcher, "http")
                .enableUriSchemaCache(true)
                .build();

        URI uri = new URI("http://example.com/with-id.json");
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);

        // Verify that the fetcher was called once and that caching works
        verify(mockFetcher, times(1)).fetch(any(URI.class));
        JsonSchema cached = factory.getSchema(uri);
        assertSame(schema, cached);
    }

    /* --------------------------------------------------------------
     *  Builder copy (blueprint) functionality
     * -------------------------------------------------------------- */
    @Test
    public void testBuilderFromBlueprintCopiesAllComponents() {
        // Create an original factory with a custom URI mapping and a custom translator.
        JsonSchemaFactory original = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT07_URI)
                .addMetaSchema(mockMetaSchema)
                .addUriMappings(Collections.singletonMap("http://old.com", "http://new.com"))
                .addUriTranslator(new URITranslator() {
                    @Override
                    public URI translate(URI uri) {
                        return URI.create(uri.toString().replace("old.com", "new.com"));
                    }
                })
                .build();

        // Build a new factory from the blueprint.
        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        // The URI map must be duplicated.
        assertEquals(original.getUriTranslator().translate(URI.create("http://old.com/resource")),
                copy.getUriTranslator().translate(URI.create("http://old.com/resource")));

        // The meta‑schema map must contain the same entry.
        assertSame(original.jsonMetaSchemas.get(JsonSchemaFactory.normalizeMetaSchemaUri(DRAFT07_URI)),
                copy.jsonMetaSchemas.get(JsonSchemaFactory.normalizeMetaSchemaUri(DRAFT07_URI)));
    }
}
```
###Test END##