package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory}.
 *
 * The tests focus on the public API and on the internal validation logic that can be exercised
 * without needing the full JSON‑Schema implementation.  Heavy collaborators are mocked with
 * Mockito so that the tests remain fast and deterministic.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    /* --------------------------------------------------------------------- *
     *  Static helper methods / constants
     * --------------------------------------------------------------------- */

    private static final String HTTP_SCHEMA_URI = "http://example.com/schema";
    private static final String HTTPS_SCHEMA_URI = "https://example.com/schema";

    /**
     * Helper that creates a simple {@link JsonMetaSchema} mock whose {@code getUri()}
     * returns the supplied URI string.  All other methods return default values or
     * are left unimplemented because they are not used by the factory constructor.
     */
    private static JsonMetaSchema mockMetaSchema(String uri) {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getUri()).thenReturn(uri);
        // readId is used only by {@code idMatchesSourceUri}; default to null.
        when(meta.readId(any(JsonNode.class))).thenReturn(null);
        return meta;
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the static utility methods
     * --------------------------------------------------------------------- */

    @Test
    public void testCheckVersionValidFlags() {
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
        // Simulate an unknown enum constant by casting an illegal int value.
        // This will trigger the default branch of the switch.
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V999"));
    }

    @Test
    public void testNormalizeMetaSchemaUriConvertsToHttps() {
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(HTTP_SCHEMA_URI);
        assertEquals(HTTPS_SCHEMA_URI, normalized);
    }

    @Test
    public void testNormalizeMetaSchemaUriLeavesHttpsUnchanged() {
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(HTTPS_SCHEMA_URI);
        assertEquals(HTTPS_SCHEMA_URI, normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalidThrows() {
        JsonSchemaFactory.normalizeMetaSchemaUri("http://[invalid");
    }

    @Test
    public void testIsYamlDetectsYamlExtension() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        // .yaml
        URI yamlUri = new URI("file:/tmp/schema.yaml");
        assertTrue(factory.isYaml(yamlUri));

        // .yml
        URI ymlUri = new URI("file:/tmp/schema.yml");
        assertTrue(factory.isYaml(ymlUri));
    }

    @Test
    public void testIsYamlRejectsNonYaml() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        URI jsonUri = new URI("http://example.com/schema.json");
        assertFalse(factory.isYaml(jsonUri));

        URI noExt = new URI("http://example.com/schema");
        assertFalse(factory.isYaml(noExt));
    }

    /* --------------------------------------------------------------------- *
     *  Tests for idMatchesSourceUri (using a mock JsonMetaSchema)
     * --------------------------------------------------------------------- */

    @Test
    public void testIdMatchesSourceUriTrueWhenIdsMatch() throws Exception {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn("http://example.com/id");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        // Use reflection to call the private method (or make it package‑private for testing)
        java.lang.reflect.Method method = JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);

        JsonNode dummyNode = mock(JsonNode.class);
        boolean result = (Boolean) method.invoke(factory, meta, dummyNode, new URI("http://example.com/id"));
        assertTrue(result);
    }

    @Test
    public void testIdMatchesSourceUriFalseWhenIdsDoNotMatch() throws Exception {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn("http://example.com/other");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        java.lang.reflect.Method method = JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);

        JsonNode dummyNode = mock(JsonNode.class);
        boolean result = (Boolean) method.invoke(factory, meta, dummyNode, new URI("http://example.com/id"));
        assertFalse(result);
    }

    @Test
    public void testIdMatchesSourceUriFalseWhenIdMissing() throws Exception {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any(JsonNode.class))).thenReturn(null);

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        java.lang.reflect.Method method = JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);

        JsonNode dummyNode = mock(JsonNode.class);
        boolean result = (Boolean) method.invoke(factory, meta, dummyNode, new URI("http://example.com/id"));
        assertFalse(result);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for getSchema(String) – error handling
     * --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidJsonStringThrows() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        // malformed JSON
        factory.getSchema("{ invalid json }");
    }

    @Test
    public void testGetSchemaFromValidJsonStringReturnsNonNull() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        String json = "{}"; // minimal valid JSON schema
        JsonSchema schema = factory.getSchema(json);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromInputStream() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();

        String json = "{\"type\":\"object\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes("UTF-8"));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    /* --------------------------------------------------------------------- *
     *  Builder validation tests – constructor argument checks
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenObjectMapperIsNull() {
        JsonSchemaFactory.builder()
                .objectMapper(null)               // <-- illegal
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build(); // should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenYamlMapperIsNull() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(null)                  // <-- illegal
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaUriIsNull() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(null)        // <-- illegal
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenMetaSchemaMapIsEmpty() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                // No meta schemas added
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaNotProvidedInMap() {
        JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema("https://other/schema")) // does NOT match default
                .build();
    }

    /* --------------------------------------------------------------------- *
     *  getInstance / getInstance(version) sanity checks
     * --------------------------------------------------------------------- */

    @Test
    public void testGetInstanceReturnsFactoryForV4() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(factory);
        // The default meta‑schema URI should be the one of Draft‑V4.
        assertEquals(JsonSchemaFactory.normalizeMetaSchemaUri(
                JsonSchemaVersion.DRAFTV4.getInstance().getUri()),
                factory.defaultMetaSchemaURI);
    }

    @Test
    public void testDeprecatedGetInstanceDelegatesToV4() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(); // deprecated
        assertNotNull(factory);
        assertEquals(JsonSchemaFactory.normalizeMetaSchemaUri(
                JsonSchemaVersion.DRAFTV4.getInstance().getUri()),
                factory.defaultMetaSchemaURI);
    }

    /* --------------------------------------------------------------------- *
     *  getUriTranslator – mapping behaviour
     * --------------------------------------------------------------------- */

    @Test
    public void testGetUriTranslatorAppliesMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .addUriMappings(Collections.singletonMap("http://old.com/schema", "http://new.com/schema"))
                .build();

        URITranslator translator = factory.getUriTranslator();
        URI original = URI.create("http://old.com/schema");
        URI translated = translator.translate(original);
        assertEquals(URI.create("http://new.com/schema"), translated);
    }

    /* --------------------------------------------------------------------- *
     *  getSchema(URI) – cache behaviour (basic verification)
     * --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromUriCachesResultWhenEnabled() throws Exception {
        // Prepare a simple JSON schema file in the classpath.
        // For the purpose of the unit test we use a data: URI (which is supported by the default URLFactory).
        String json = "{\"type\":\"object\"}";
        String dataUri = "data:application/json," + java.net.URLEncoder.encode(json, "UTF-8");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .enableUriSchemaCache(true)
                .build();

        JsonSchema first = factory.getSchema(URI.create(dataUri));
        JsonSchema second = factory.getSchema(URI.create(dataUri));

        // The two instances should be the same object because of caching.
        assertSame(first, second);
    }

    @Test
    public void testGetSchemaFromUriDoesNotCacheWhenDisabled() throws Exception {
        String json = "{\"type\":\"object\"}";
        String dataUri = "data:application/json," + java.net.URLEncoder.encode(json, "UTF-8");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(HTTPS_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema(HTTPS_SCHEMA_URI))
                .enableUriSchemaCache(false)
                .build();

        JsonSchema first = factory.getSchema(URI.create(dataUri));
        JsonSchema second = factory.getSchema(URI.create(dataUri));

        // When caching is disabled we expect distinct instances.
        assertNotSame(first, second);
    }
}
