###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests are organised as if they were produced by three independent experts
 * that each contributed one test per public method, then merged the results.
 *
 * The suite covers:
 *   • Normal behaviour of all public API methods
 *   • Edge‑case inputs (nulls, empty strings, malformed URIs, etc.)
 *   • Exception handling paths
 *   • Private helper methods accessed via reflection (isYaml, normalizeMetaSchemaUri,
 *     idMatchesSourceUri, etc.)
 *
 * The tests rely only on the public API of the library and on standard JDK/JUnit
 * classes – no external mocking framework is required.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.uri.URITranslator;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    /** A factory built with the default draft‑V4 meta‑schema – used by most tests. */
    private JsonSchemaFactory factoryV4;

    /** A minimal valid JSON schema used for several tests. */
    private static final String SIMPLE_SCHEMA = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }";

    @Before
    public void setUp() {
        // Use the official static factory method – it creates a fully‑initialised instance.
        factoryV4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull("Factory must be created", factoryV4);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Builder creation – static Builder builder()                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert1_builderReturnsNonNullBuilder() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder();
        assertNotNull("builder() must not return null", builder);
    }

    /* --------------------------------------------------------------------- */
    /* 2. Deprecated getInstance() – static JsonSchemaFactory getInstance()   */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert2_deprecatedGetInstanceReturnsFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(); // uses V4 internally
        assertNotNull("Deprecated getInstance() must return a factory", factory);
        // The default meta‑schema URI should be the Draft‑V4 URI.
        String defaultMeta = (String) getPrivateField(factory, "defaultMetaSchemaURI");
        assertTrue("Default meta‑schema URI must contain draft‑04", defaultMeta.contains("draft-04"));
    }

    /* --------------------------------------------------------------------- */
    /* 3. getInstance(VersionFlag) – static JsonSchemaFactory getInstance(...) */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert3_getInstanceWithAllSupportedVersionFlags() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaFactory f = JsonSchemaFactory.getInstance(flag);
            assertNotNull("Factory for flag " + flag + " must not be null", f);
            // Verify that the internal defaultMetaSchemaURI matches the expected dialect.
            String defaultMeta = (String) getPrivateField(f, "defaultMetaSchemaURI");
            assertNotNull("defaultMetaSchemaURI must be set for " + flag, defaultMeta);
            assertFalse("defaultMetaSchemaURI must not be empty", defaultMeta.trim().isEmpty());
        }
    }

    @Test(expected = NullPointerException.class)
    public void expert3_getInstanceWithNullVersionFlagThrowsNPE() {
        // The implementation forwards the null to checkVersion() which returns null,
        // then dereferences it – resulting in NPE.
        JsonSchemaFactory.getInstance(null);
    }

    /* --------------------------------------------------------------------- */
    /* 4. checkVersion – static JsonSchemaVersion checkVersion(...)          */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert1_checkVersionReturnsCorrectImplementation() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull("checkVersion must return non‑null for " + flag, version);
            // Each version class implements JsonSchemaVersion; we simply verify the class name.
            String expectedClass = flag.name().replace("V", "Version");
            assertTrue("Returned class should contain " + expectedClass,
                    version.getClass().getSimpleName().contains(expectedClass));
        }
    }

    @Test
    public void expert1_checkVersionWithNullReturnsNull() {
        assertNull("checkVersion(null) must return null", JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void expert1_checkVersionWithUnsupportedEnumThrows() throws Exception {
        // Create a dummy enum value that is not part of the switch (simulated via reflection).
        // Since the switch covers all defined enum constants, we simulate by calling the
        // private method that throws for the default case.
        Method m = JsonSchemaFactory.class.getDeclaredMethod("checkVersion", SpecVersion.VersionFlag.class);
        m.setAccessible(true);
        // Use a null to trigger the default branch – the method will throw IllegalArgumentException.
        m.invoke(null, (Object) null);
    }

    /* --------------------------------------------------------------------- */
    /* 5. Builder from blueprint – static Builder builder(JsonSchemaFactory) */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert2_builderFromBlueprintCopiesConfiguration() throws Exception {
        // Build a new factory from the existing one.
        JsonSchemaFactory copy = JsonSchemaFactory.builder(factoryV4).build();
        assertNotNull("Copy must not be null", copy);

        // The defaultMetaSchemaURI should be identical.
        String originalUri = (String) getPrivateField(factoryV4, "defaultMetaSchemaURI");
        String copyUri = (String) getPrivateField(copy, "defaultMetaSchemaURI");
        assertEquals("defaultMetaSchemaURI must be copied", originalUri, copyUri);
    }

    /* --------------------------------------------------------------------- */
    /* 6. getUriFactory – public URIFactory getUriFactory()                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert3_getUriFactoryReturnsNonNull() {
        URIFactory uf = factoryV4.getUriFactory();
        assertNotNull("getUriFactory() must return a non‑null instance", uf);
    }

    /* --------------------------------------------------------------------- */
    /* 7. getUriTranslator – public URITranslator getUriTranslator()          */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert1_getUriTranslatorHonoursMappings() {
        // Add a simple mapping via the builder to the blueprint.
        Map<String, String> map = new HashMap<>();
        map.put("http://example.com/schema", "classpath:/my-schema.json");
        JsonSchemaFactory custom = JsonSchemaFactory.builder(factoryV4)
                .addUriMappings(map)
                .build();

        URITranslator translator = custom.getUriTranslator();
        assertNotNull(translator);
        URI translated = translator.translate(URI.create("http://example.com/schema"));
        assertEquals("Mapping must be applied", "classpath:/my-schema.json", translated.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 8. getSchema(String) – JSON string input                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert2_getSchemaFromStringReturnsValidSchema() {
        JsonSchema schema = factoryV4.getSchema(SIMPLE_SCHEMA);
        assertNotNull("Schema must be parsed from valid JSON string", schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void expert2_getSchemaFromInvalidStringThrows() {
        // Malformed JSON should be wrapped in JsonSchemaException.
        factoryV4.getSchema("{ invalid json }");
    }

    @Test(expected = IllegalArgumentException.class)
    public void expert2_getSchemaFromNullStringThrows() {
        factoryV4.getSchema((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void expert2_getSchemaFromEmptyStringThrows() {
        factoryV4.getSchema("");
    }

    /* --------------------------------------------------------------------- */
    /* 9. getSchema(InputStream) – stream input                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert3_getSchemaFromInputStreamWorks() throws IOException {
        InputStream is = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes("UTF-8"));
        JsonSchema schema = factoryV4.getSchema(is);
        assertNotNull("Schema must be parsed from InputStream", schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void expert3_getSchemaFromInvalidInputStreamThrows() throws IOException {
        InputStream is = new ByteArrayInputStream("not a json".getBytes("UTF-8"));
        factoryV4.getSchema(is);
    }

    /* --------------------------------------------------------------------- */
    /* 10. getSchema(URI) – loading from a file (caches enabled)              */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert1_getSchemaFromFileUriUsesCache() throws Exception {
        // Create a temporary file containing a simple schema.
        File temp = File.createTempFile("schema", ".json");
        temp.deleteOnExit();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(temp), "UTF-8")) {
            w.write(SIMPLE_SCHEMA);
        }

        URI uri = temp.toURI();

        JsonSchema first = factoryV4.getSchema(uri);
        JsonSchema second = factoryV4.getSchema(uri);

        assertNotNull(first);
        assertNotNull(second);
        // With caching enabled the same instance should be returned.
        assertSame("Cached schema instance must be returned", first, second);
    }

    @Test(expected = JsonSchemaException.class)
    public void expert1_getSchemaFromNonExistingFileThrows() throws Exception {
        URI uri = new URI("file:///non/existing/schema.json");
        factoryV4.getSchema(uri);
    }

    @Test(expected = JsonSchemaException.class)
    public void expert1_getSchemaWithNullUriThrows() {
        factoryV4.getSchema((URI) null);
    }

    /* --------------------------------------------------------------------- */
    /* 11. Private helper isYaml(URI) – accessed via reflection                */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert2_isYamlDetectsYamlExtension() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI yaml = new URI("file:///tmp/config.yaml");
        URI yml  = new URI("file:///tmp/config.yml");
        URI json = new URI("file:///tmp/config.json");
        URI noExt = new URI("file:///tmp/config");

        assertTrue((Boolean) isYaml.invoke(factoryV4, yaml));
        assertTrue((Boolean) isYaml.invoke(factoryV4, yml));
        assertFalse((Boolean) isYaml.invoke(factoryV4, json));
        assertFalse((Boolean) isYaml.invoke(factoryV4, noExt));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Private static normalizeMetaSchemaUri(String) – reflection          */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert3_normalizeMetaSchemaUriConvertsToHttps() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);

        String httpUrl = "http://json-schema.org/draft-04/schema#";
        String normalized = (String) normalize.invoke(null, httpUrl);
        assertTrue("Result should start with https", normalized.startsWith("https://"));
        assertTrue("Result should contain original host/path", normalized.contains("json-schema.org/draft-04/schema"));
    }

    @Test(expected = JsonSchemaException.class)
    public void expert3_normalizeMetaSchemaUriWithInvalidInputThrows() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        normalize.invoke(null, ":::: not a uri");
    }

    /* --------------------------------------------------------------------- */
    /* 13. Edge case for findMetaSchemaForSchema – meta‑schema missing "$schema" */
    /* --------------------------------------------------------------------- */
    @Test
    public void expert1_findMetaSchemaFallsBackToDefault() throws Exception {
        // Using reflection to call the protected method.
        Method findMeta = JsonSchemaFactory.class.getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        findMeta.setAccessible(true);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{ \"type\": \"string\" }"); // no $schema

        JsonMetaSchema meta = (JsonMetaSchema) findMeta.invoke(factoryV4, node);
        assertNotNull("Meta schema should be resolved to default", meta);
        // The URI of the meta‑schema must match the factory's default.
        String defaultMeta = (String) getPrivateField(factoryV4, "defaultMetaSchemaURI");
        assertEquals(defaultMeta, meta.getUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void expert1_findMetaSchemaWithNonTextual$schemaThrows() throws Exception {
        Method findMeta = JsonSchemaFactory.class.getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        findMeta.setAccessible(true);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{ \"$schema\": 123 }"); // non‑textual

        findMeta.invoke(factoryV4, node);
    }

    /* --------------------------------------------------------------------- */
    /* Utility: reflection helper to read private fields                       */
    /* --------------------------------------------------------------------- */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read private field " + fieldName, e);
        }
    }
}
```
###Test END##