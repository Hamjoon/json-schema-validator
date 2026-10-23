/**
 * JUnit‑4 test suite that exercises the public and (via reflection) the
 * package‑private logic of {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests focus on:
 *
 * 1. Builder validation – ensuring that illegal arguments are rejected.
 * 2. Static helper methods – {@code checkVersion}, {@code normalizeMetaSchemaUri},
 *    and the private {@code isYaml}.
 * 3. Private instance helpers – {@code idMatchesSourceUri} and
 *    {@code findMetaSchemaForSchema} (the latter is exercised indirectly through
 *    {@code getSchema(String)}).
 * 4. Error handling – malformed JSON, malformed Meta‑Schema URIs and unknown
 *    Meta‑Schema identifiers.
 *
 * Mockito is used to create lightweight stubs for {@link JsonMetaSchema},
 * {@link URIFactory} and {@link URIFetcher}.  The test class lives in the same
 * package as {@code JsonSchemaFactory} so that protected/static‑protected members
 * are directly accessible.
 */
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link JsonSchemaFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    /**
     * Helper to create a minimal {@link JsonMetaSchema} mock that returns the
     * supplied URI from {@code getUri()} and {@code null} from {@code readId(...)}.
     */
    private static JsonMetaSchema mockMetaSchema(String uri) {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getUri()).thenReturn(uri);
        // readId is used only by idMatchesSourceUri – returning null means “no id”.
        when(meta.readId(any())).thenReturn(null);
        return meta;
    }

    /* --------------------------------------------------------------
     * 1. Builder validation
     * -------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsNullObjectMapper() {
        JsonSchemaFactory.builder()
                .objectMapper(null)                     // <-- illegal
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsNullYamlMapper() {
        JsonSchemaFactory.builder()
                .yamlMapper(null)                       // <-- illegal
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsNullOrEmptyDefaultMetaSchemaURI() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("   ")            // empty after trim
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderRejectsMissingDefaultMetaSchemaInMap() {
        // defaultMetaSchemaURI is present but no matching meta‑schema has been added
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .build();
    }

    @Test
    public void builderAcceptsAllValidParameters() {
        JsonMetaSchema meta = mockMetaSchema("http://example.com/schema#");
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(meta)
                .enableUriSchemaCache(false)            // toggle a non‑default flag
                .build();

        assertNotNull(factory);
        assertEquals("http://example.com/schema#", factory.defaultMetaSchemaURI);
    }

    /* --------------------------------------------------------------
     * 2. Static helper methods
     * -------------------------------------------------------------- */

    @Test
    public void checkVersionMapsEnumToCorrectImplementation() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionThrowsOnUnsupportedFlag() {
        // Simulate an unknown enum value by passing null – the method returns null,
        // but the switch’s default case throws IllegalArgumentException for any
        // value that is not matched (null is handled before the switch).
        JsonSchemaFactory.checkVersion(null);
    }

    @Test
    public void normalizeMetaSchemaUriConvertsToHttpsAndStripsFragmentAndQuery() {
        String raw = "http://user:pass@host:8080/path/to/schema.json?query=123#frag";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(raw);

        // Expected: scheme forced to https, user‑info, host, port and path retained,
        // query and fragment removed.
        assertEquals("https://user:pass@host:8080/path/to/schema.json", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriThrowsOnBadInput() {
        JsonSchemaFactory.normalizeMetaSchemaUri("ht!tp://[invalid]");
    }

    /* --------------------------------------------------------------
     * 3. Private instance helpers – accessed via reflection
     * -------------------------------------------------------------- */

    /**
     * Utility to invoke a private method on an instance using reflection.
     */
    private static Object invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    @Test
    public void isYamlDetectsYamlExtensions() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();

        // .yaml
        URI yaml = new URI("file:///tmp/example.yaml");
        boolean yamlResult = (boolean) invokePrivate(factory, "isYaml", new Class[]{URI.class}, yaml);
        assertTrue(yamlResult);

        // .yml
        URI yml = new URI("file:///tmp/example.yml");
        assertTrue((boolean) invokePrivate(factory, "isYaml", new Class[]{URI.class}, yml));

        // .json – should be false
        URI json = new URI("file:///tmp/example.json");
        assertFalse((boolean) invokePrivate(factory, "isYaml", new Class[]{URI.class}, json));

        // No extension – false
        URI noExt = new URI("file:///tmp/example");
        assertFalse((boolean) invokePrivate(factory, "isYaml", new Class[]{URI.class}, noExt));
    }

    @Test
    public void idMatchesSourceUriDetectsMatchingId() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();

        // Prepare a meta‑schema mock that returns a specific id.
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.readId(any())).thenReturn("http://example.com/hello.json");

        URI sourceUri = new URI("http://example.com/hello.json");
        boolean matches = (boolean) invokePrivate(factory, "idMatchesSourceUri",
                new Class[]{JsonMetaSchema.class, JsonNode.class, URI.class},
                meta, null, sourceUri);
        assertTrue(matches);
    }

    @Test
    public void idMatchesSourceUriReturnsFalseWhenIdMissingOrDifferent() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();

        JsonMetaSchema metaNoId = mock(JsonMetaSchema.class);
        when(metaNoId.readId(any())).thenReturn(null);
        URI anyUri = new URI("http://example.com/whatever.json");
        boolean resultNoId = (boolean) invokePrivate(factory, "idMatchesSourceUri",
                new Class[]{JsonMetaSchema.class, JsonNode.class, URI.class},
                metaNoId, null, anyUri);
        assertFalse(resultNoId);

        JsonMetaSchema metaDiff = mock(JsonMetaSchema.class);
        when(metaDiff.readId(any())).thenReturn("http://example.com/other.json");
        boolean resultDiff = (boolean) invokePrivate(factory, "idMatchesSourceUri",
                new Class[]{JsonMetaSchema.class, JsonNode.class, URI.class},
                metaDiff, null, anyUri);
        assertFalse(resultDiff);
    }

    /* --------------------------------------------------------------
     * 4. Error handling in public API
     * -------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringThrowsOnInvalidJson() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();

        // malformed JSON (missing closing brace)
        factory.getSchema("{\"type\":\"object\"");
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringDetectsUnknownMetaSchema() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();

        // JSON that specifies an unknown $schema value – the factory should fail
        // when it tries to resolve the meta‑schema.
        String json = "{ \"$schema\": \"http://unknown.com/does-not-exist#\", \"type\":\"object\" }";
        factory.getSchema(json);
    }

    @Test
    public void getSchemaFromInputStreamWorksForValidJson() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .build();

        String json = "{ \"type\": \"object\" }";
        InputStream is = new ByteArrayInputStream(json.getBytes("UTF-8"));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test
    public void builderCopiesAllPropertiesFromBlueprint() {
        // Prepare a “blueprint” factory with a few custom settings.
        JsonMetaSchema meta = mockMetaSchema("http://example.com/schema#");
        Map<String, String> uriMap = new HashMap<>();
        uriMap.put("http://old.com", "http://new.com");

        JsonSchemaFactory blueprint = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(meta)
                .addUriMappings(uriMap)
                .enableUriSchemaCache(false)
                .build();

        // Build a new factory from the blueprint.
        JsonSchemaFactory copy = JsonSchemaFactory.builder(blueprint).build();

        // Verify that the copied instance has the same configuration values.
        assertEquals(blueprint.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
        assertEquals(blueprint.enableUriSchemaCache, copy.enableUriSchemaCache);
        assertEquals(blueprint.uriMap, copy.uriMap);
        assertEquals(blueprint.jsonMetaSchemas.keySet(), copy.jsonMetaSchemas.keySet());
    }

    /* --------------------------------------------------------------
     * 5. getUriTranslator – ensure the map‑based translator is combined
     *    with any custom translators added via the builder.
     * -------------------------------------------------------------- */

    @Test
    public void getUriTranslatorCombinesMapAndCustomTranslators() {
        // Custom translator that simply appends “/extra”.
        URITranslator custom = uri -> URI.create(uri.toString() + "/extra");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/schema#")
                .addMetaSchema(mockMetaSchema("http://example.com/schema#"))
                .addUriMappings(Collections.singletonMap("http://a.com", "http://b.com"))
                .addUriTranslator(custom)
                .build();

        URITranslator combined = factory.getUriTranslator();

        // 1) map translation should apply
        URI afterMap = combined.translate(URI.create("http://a.com/resource"));
        assertEquals(URI.create("http://b.com/resource/extra"), afterMap);

        // 2) an unmapped URI should only get the custom suffix
        URI afterCustomOnly = combined.translate(URI.create("http://c.com/resource"));
        assertEquals(URI.create("http://c.com/resource/extra"), afterCustomOnly);
    }
}
