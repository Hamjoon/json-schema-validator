/*
 * JsonSchemaFactoryTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests cover:
 *   • All public API methods (including overloads)
 *   • Normal operation
 *   • Edge cases and expected exception handling
 *   • Caching behaviour
 *   • Private helper methods via reflection (normalizeMetaSchemaUri, isYaml, idMatchesSourceUri)
 *
 * The test class uses only the public constructors / factories that are visible in the
 * supplied source code – no mocking frameworks are employed.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factoryV4;

    @Before
    public void setUp() {
        // Build a factory for Draft‑04 – this is the most common default used in the library.
        factoryV4 = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull("Factory must be created", factoryV4);
    }

    /* ----------------------------------------------------------------------
     *  1.  Public static builder / factory methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesFactory() {
        JsonMetaSchema meta = JsonMetaSchema.getV4();
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(meta.getUri())
                .addMetaSchema(meta)
                .build();

        assertNotNull(factory);
        assertEquals(meta.getUri(), factory.defaultMetaSchemaURI);
    }

    @Test
    public void testGetInstanceDeprecated() {
        // The deprecated getInstance() should return a V4 factory.
        JsonSchemaFactory deprecated = JsonSchemaFactory.getInstance();
        assertNotNull(deprecated);
        assertEquals(JsonMetaSchema.getV4().getUri(),
                deprecated.defaultMetaSchemaURI);
    }

    @Test
    public void testCheckVersionNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testCheckVersionAllFlags() {
        Map<VersionFlag, Class<?>> expected = new LinkedHashMap<>();
        expected.put(VersionFlag.V4, Version4.class);
        expected.put(VersionFlag.V6, Version6.class);
        expected.put(VersionFlag.V7, Version7.class);
        expected.put(VersionFlag.V201909, Version201909.class);
        expected.put(VersionFlag.V202012, Version202012.class);

        for (Map.Entry<VersionFlag, Class<?>> e : expected.entrySet()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(e.getKey());
            assertNotNull("Version must not be null for flag " + e.getKey(), version);
            assertTrue("Returned version must be instance of " + e.getValue().getSimpleName(),
                    e.getValue().isInstance(version));
        }
    }

    @Test
    public void testBuilderFromBlueprint() {
        JsonSchemaFactory copy = JsonSchemaFactory.builder(factoryV4).build();
        assertNotNull(copy);
        assertEquals(factoryV4.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
        assertEquals(factoryV4.jsonMetaSchemas.size(), copy.jsonMetaSchemas.size());
    }

    /* ----------------------------------------------------------------------
     *  2.  getUriFactory / getUriTranslator
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetUriFactory() {
        URIFactory uriFactory = factoryV4.getUriFactory();
        assertNotNull(uriFactory);
        // The default factory must be able to create a simple HTTP URI.
        URI created = uriFactory.create("http://example.com/");
        assertEquals("http://example.com/", created.toString());
    }

    @Test
    public void testGetUriTranslator() {
        URITranslator translator = factoryV4.getUriTranslator();
        assertNotNull(translator);
        // By default there are no mappings – the translator should be a NOOP.
        URI original = URI.create("http://example.com/");
        URI translated = translator.translate(original);
        assertEquals(original, translated);
    }

    /* ----------------------------------------------------------------------
     *  3.  getSchema(String) – JSON string source
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromStringValid() {
        String jsonSchema = "{\"type\":\"object\"}";
        JsonSchema schema = factoryV4.getSchema(jsonSchema);
        assertNotNull(schema);
        assertEquals(JsonSchema.class, schema.getClass());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalid() {
        // malformed JSON – should result in JsonSchemaException
        String malformed = "{type:object";
        factoryV4.getSchema(malformed);
    }

    /* ----------------------------------------------------------------------
     *  4.  getSchema(InputStream) – JSON stream source
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromInputStreamValid() throws IOException {
        String jsonSchema = "{\"type\":\"array\"}";
        try (InputStream is = new ByteArrayInputStream(jsonSchema.getBytes())) {
            JsonSchema schema = factoryV4.getSchema(is);
            assertNotNull(schema);
        }
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalid() throws IOException {
        String malformed = "{\"type\":\"array\""; // missing closing brace
        try (InputStream is = new ByteArrayInputStream(malformed.getBytes())) {
            factoryV4.getSchema(is);
        }
    }

    /* ----------------------------------------------------------------------
     *  5.  getSchema(URI) – file based JSON/YAML schemas and caching
     * ---------------------------------------------------------------------- */

    private File createTempSchemaFile(String content, String suffix) throws IOException {
        File temp = Files.createTempFile("schema", suffix).toFile();
        temp.deleteOnExit();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(temp), "UTF-8")) {
            w.write(content);
        }
        return temp;
    }

    @Test
    public void testGetSchemaFromUriJsonFile() throws IOException {
        File jsonFile = createTempSchemaFile("{\"type\":\"string\"}", ".json");
        URI uri = jsonFile.toURI();

        JsonSchema schema = factoryV4.getSchema(uri);
        assertNotNull(schema);
        // Verify that the schema knows its current URI
        assertEquals(uri, schema.getCurrentUri());
    }

    @Test
    public void testGetSchemaFromUriYamlFile() throws IOException {
        String yaml = "type: string";
        File yamlFile = createTempSchemaFile(yaml, ".yaml");
        URI uri = yamlFile.toURI();

        JsonSchema schema = factoryV4.getSchema(uri);
        assertNotNull(schema);
        assertEquals(uri, schema.getCurrentUri());
    }

    @Test
    public void testUriSchemaCacheEnabled() throws IOException {
        File jsonFile = createTempSchemaFile("{\"type\":\"integer\"}", ".json");
        URI uri = jsonFile.toURI();

        JsonSchema first = factoryV4.getSchema(uri);
        JsonSchema second = factoryV4.getSchema(uri);

        // With caching enabled the same instance must be returned.
        assertSame("Schema instances should be cached", first, second);
    }

    @Test
    public void testUriSchemaCacheDisabled() throws IOException {
        // Build a factory with caching turned off.
        JsonSchemaFactory noCacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .enableUriSchemaCache(false)
                .build();

        File jsonFile = createTempSchemaFile("{\"type\":\"number\"}", ".json");
        URI uri = jsonFile.toURI();

        JsonSchema first = noCacheFactory.getSchema(uri);
        JsonSchema second = noCacheFactory.getSchema(uri);

        assertNotSame("Schemas must not be cached when disabled", first, second);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromUnsupportedScheme() {
        // The default URLFactory does not understand the "foo" scheme.
        URI badUri = URI.create("foo://some/path/schema.json");
        factoryV4.getSchema(badUri);
    }

    /* ----------------------------------------------------------------------
     *  6.  Private helper methods – exercised via reflection
     * ---------------------------------------------------------------------- */

    @Test
    public void testNormalizeMetaSchemaUriValid() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        m.setAccessible(true);
        String original = "http://json-schema.org/draft-04/schema#";
        String normalized = (String) m.invoke(null, original);
        assertEquals("https://json-schema.org/draft-04/schema#", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalid() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        m.setAccessible(true);
        // Not a valid URI – should trigger JsonSchemaException.
        m.invoke(null, "://invalid-uri");
    }

    @Test
    public void testIsYamlDetection() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);

        URI yaml = new URI("file:/tmp/schema.yaml");
        URI yml = new URI("file:/tmp/schema.yml");
        URI json = new URI("file:/tmp/schema.json");
        URI noExt = new URI("file:/tmp/schema");

        assertTrue((Boolean) m.invoke(factoryV4, yaml));
        assertTrue((Boolean) m.invoke(factoryV4, yml));
        assertFalse((Boolean) m.invoke(factoryV4, json));
        assertFalse((Boolean) m.invoke(factoryV4, noExt));
    }

    @Test
    public void testIdMatchesSourceUriPositive() throws Exception {
        // Build a tiny schema node that contains an "$id" matching the supplied URI.
        String schemaJson = "{ \"$id\": \"http://example.com/my-schema\" }";
        JsonNode node = new ObjectMapper().readTree(schemaJson);
        URI uri = URI.create("http://example.com/my-schema");

        // Use the default meta‑schema (draft‑04) for the test.
        JsonMetaSchema meta = JsonMetaSchema.getV4();

        Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);

        Boolean result = (Boolean) m.invoke(factoryV4, meta, node, uri);
        assertTrue("The $id should match the source URI", result);
    }

    @Test
    public void testIdMatchesSourceUriNegative() throws Exception {
        String schemaJson = "{ \"$id\": \"http://example.com/other\" }";
        JsonNode node = new ObjectMapper().readTree(schemaJson);
        URI uri = URI.create("http://example.com/my-schema");

        JsonMetaSchema meta = JsonMetaSchema.getV4();

        Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);

        Boolean result = (Boolean) m.invoke(factoryV4, meta, node, uri);
        assertFalse("The $id does not match the source URI", result);
    }

    /* ----------------------------------------------------------------------
     *  7.  create(ValidationContext, …) – direct schema creation path
     * ---------------------------------------------------------------------- */

    @Test
    public void testCreateViaValidationContext() {
        // Build a minimal ValidationContext using the factory's components.
        ValidationContext vc = new ValidationContext(
                factoryV4.getUriFactory(),
                null, // URNFactory not needed for this test
                JsonMetaSchema.getV4(),
                factoryV4,
                null);

        String schemaPath = "#";
        JsonNode schemaNode = new ObjectMapper().createObjectNode().put("type", "null");
        JsonSchema parent = null; // top‑level schema

        JsonSchema schema = factoryV4.create(vc, schemaPath, schemaNode, parent);
        assertNotNull(schema);
        // The created schema should have the same ValidationContext as supplied.
        assertEquals(vc, schema.getValidationContext());
    }
}
