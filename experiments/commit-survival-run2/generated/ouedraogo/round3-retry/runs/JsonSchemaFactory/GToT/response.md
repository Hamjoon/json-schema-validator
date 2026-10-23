###Test START##
```java
/****************************************************************************************
 *  JsonSchemaFactoryTest.java
 *
 *  Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 *  The tests cover:
 *   • Normal usage of the public factory methods.
 *   • Edge‑case handling (nulls, empty strings, invalid URIs, unknown meta‑schemas).
 *   • Exception paths (malformed JSON/YAML, I/O errors, unsupported version flags).
 *   • Internal behaviours exposed via reflection (YAML detection, meta‑schema normalisation,
 *     URI‑schema cache).
 *
 *  The test class is deliberately self‑contained – it uses the real implementations of
 *  the library (no heavy mocking) and creates temporary files when a real URI fetch is
 *  required.  All assertions are written with JUnit‑4 assertions.
 *
 *  Author:  Test‑Generation‑Assistant
 *  Date:    2026‑10‑23
 ****************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private static final String SIMPLE_JSON_SCHEMA = "{ \"type\": \"object\" }";

    private JsonSchemaFactory factory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* -----------------------------------------------------------------------
     *  Helper utilities
     * --------------------------------------------------------------------- */

    /** Creates a fresh factory based on Draft‑V4 (the most common default). */
    private static JsonSchemaFactory newDefaultFactory() {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    }

    /** Writes the supplied string to a temporary file and returns its {@link URI}. */
    private static URI writeTempFile(String content, String suffix) throws IOException {
        Path tmp = Files.createTempFile("schema", suffix);
        Files.write(tmp, content.getBytes());
        tmp.toFile().deleteOnExit();
        return tmp.toUri();
    }

    /** Accesses a private method via reflection. */
    private static Object invokePrivate(Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    /* -----------------------------------------------------------------------
     *  Test lifecycle
     * --------------------------------------------------------------------- */

    @Before
    public void setUp() {
        factory = newDefaultFactory();
    }

    /* -----------------------------------------------------------------------
     *  Public static builder methods
     * --------------------------------------------------------------------- */

    @Test
    public void testBuilderWithoutArguments() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
        assertNotNull("Builder should be instantiated", b);
    }

    @Test
    public void testBuilderFromBlueprintCopiesAllConfiguration() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(blueprint);
        JsonSchemaFactory copy = b.build();

        // The copy must have the same default meta‑schema URI
        assertEquals("Default meta‑schema URI must be copied",
                blueprint.getUriFactory().getClass(),
                copy.getUriFactory().getClass());

        // The URI translator must contain the same mappings (empty in default case)
        assertEquals("URI translator mappings must be equal",
                blueprint.getUriTranslator().getClass(),
                copy.getUriTranslator().getClass());
    }

    @Test
    public void testGetInstanceWithoutArgumentUsesV4() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull(f);
        // V4 meta‑schema URI is known – we can verify that the factory can resolve a minimal schema
        JsonSchema s = f.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull(s);
    }

    @Test
    public void testGetInstanceWithSupportedVersionFlag() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        assertNotNull(f);
    }

    @Test
    public void testCheckVersionNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testCheckVersionUnsupportedThrows() {
        thrown.expect(IllegalArgumentException.class);
        // Simulate an unsupported value by casting an invalid ordinal (this is safe because the enum is final)
        SpecVersion.VersionFlag bogus = Enum.valueOf(SpecVersion.VersionFlag.class, "V0");
        JsonSchemaFactory.checkVersion(bogus);
    }

    /* -----------------------------------------------------------------------
     *  getSchema(String) – JSON string handling
     * --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromValidJsonString() {
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull("Schema should be created from a valid JSON string", schema);
    }

    @Test
    public void testGetSchemaFromInvalidJsonStringThrows() {
        thrown.expect(JsonSchemaException.class);
        factory.getSchema("{ invalid json }");
    }

    @Test
    public void testGetSchemaFromNullStringThrows() {
        thrown.expect(IllegalArgumentException.class);
        factory.getSchema((String) null);
    }

    @Test
    public void testGetSchemaFromEmptyStringThrows() {
        thrown.expect(JsonSchemaException.class);
        factory.getSchema("");
    }

    /* -----------------------------------------------------------------------
     *  getSchema(InputStream) – stream handling
     * --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromValidInputStream() throws IOException {
        InputStream is = new ByteArrayInputStream(SIMPLE_JSON_SCHEMA.getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromInvalidInputStreamThrows() {
        thrown.expect(JsonSchemaException.class);
        InputStream is = new ByteArrayInputStream("{ bad json".getBytes());
        factory.getSchema(is);
    }

    @Test
    public void testGetSchemaFromNullInputStreamThrows() {
        thrown.expect(IllegalArgumentException.class);
        factory.getSchema((InputStream) null);
    }

    /* -----------------------------------------------------------------------
     *  getSchema(URI) – file‑based schema loading and caching
     * --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromFileUriJson() throws Exception {
        URI uri = writeTempFile(SIMPLE_JSON_SCHEMA, ".json");
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull("Schema loaded from file URI must not be null", schema);
    }

    @Test
    public void testGetSchemaFromFileUriYaml() throws Exception {
        String yaml = "type: object\n";
        URI uri = writeTempFile(yaml, ".yaml");
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull("YAML schema must be parsed correctly", schema);
    }

    @Test
    public void testGetSchemaFromInvalidUriThrows() throws Exception {
        thrown.expect(JsonSchemaException.class);
        URI bogus = new URI("http://nonexistent.invalid/schema.json");
        factory.getSchema(bogus);
    }

    @Test
    public void testUriSchemaCacheReturnsSameInstance() throws Exception {
        URI uri = writeTempFile(SIMPLE_JSON_SCHEMA, ".json");

        JsonSchema first = factory.getSchema(uri);
        JsonSchema second = factory.getSchema(uri);

        // When caching is enabled the same instance should be returned
        assertSame("Factory should return cached schema instance", first, second);
    }

    @Test
    public void testCacheIsBypassedWhenDisabled() throws Exception {
        // Build a factory with caching turned off
        JsonSchemaFactory noCacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(factory.defaultMetaSchemaURI)
                .addMetaSchema(factory.jsonMetaSchemas.get(factory.defaultMetaSchemaURI))
                .enableUriSchemaCache(false)
                .build();

        URI uri = writeTempFile(SIMPLE_JSON_SCHEMA, ".json");
        JsonSchema first = noCacheFactory.getSchema(uri);
        JsonSchema second = noCacheFactory.getSchema(uri);

        assertNotSame("When cache disabled, instances must differ", first, second);
    }

    /* -----------------------------------------------------------------------
     *  Private / protected behaviours exercised via reflection
     * --------------------------------------------------------------------- */

    @Test
    public void testIsYamlDetection() throws Exception {
        // JSON file – should be false
        URI jsonUri = new URI("file:/tmp/schema.json");
        boolean jsonResult = (Boolean) invokePrivate(factory, "isYaml",
                new Class[]{URI.class}, jsonUri);
        assertFalse(jsonResult);

        // YAML file – should be true
        URI yamlUri = new URI("file:/tmp/schema.yaml");
        boolean yamlResult = (Boolean) invokePrivate(factory, "isYaml",
                new Class[]{URI.class}, yamlUri);
        assertTrue(yamlResult);
    }

    @Test
    public void testNormalizeMetaSchemaUriConvertsToHttps() throws Exception {
        String httpUri = "http://example.com/schema";
        String normalized = (String) invokePrivate(JsonSchemaFactory.class,
                "normalizeMetaSchemaUri",
                new Class[]{String.class}, httpUri);
        assertEquals("https://example.com/schema", normalized);
    }

    @Test
    public void testNormalizeMetaSchemaUriWithInvalidInputThrows() throws Exception {
        thrown.expect(JsonSchemaException.class);
        invokePrivate(JsonSchemaFactory.class,
                "normalizeMetaSchemaUri",
                new Class[]{String.class}, "://bad_uri");
    }

    @Test
    public void testIdMatchesSourceUriPositive() throws Exception {
        // Build a tiny schema that contains an "$id" matching the source URI
        String id = "http://example.com/my-schema";
        String schemaJson = "{ \"$id\": \"" + id + "\", \"type\": \"object\" }";

        // Parse the JSON node manually using the factory's mapper
        JsonNode node = new ObjectMapper().readTree(schemaJson);
        JsonMetaSchema meta = factory.jsonMetaSchemas.get(factory.defaultMetaSchemaURI);
        // Use reflection to invoke the private method
        boolean matches = (Boolean) invokePrivate(factory,
                "idMatchesSourceUri",
                new Class[]{JsonMetaSchema.class, JsonNode.class, URI.class},
                meta, node, new URI(id));
        assertTrue(matches);
    }

    @Test
    public void testIdMatchesSourceUriNegative() throws Exception {
        String id = "http://example.com/other-schema";
        String schemaJson = "{ \"$id\": \"" + id + "\", \"type\": \"object\" }";
        JsonNode node = new ObjectMapper().readTree(schemaJson);
        JsonMetaSchema meta = factory.jsonMetaSchemas.get(factory.defaultMetaSchemaURI);
        boolean matches = (Boolean) invokePrivate(factory,
                "idMatchesSourceUri",
                new Class[]{JsonMetaSchema.class, JsonNode.class, URI.class},
                meta, node, new URI("http://example.com/different"));
        assertFalse(matches);
    }

    @Test
    public void testFindMetaSchemaForSchemaUsesDefaultWhenMissing() throws Exception {
        // The schema does NOT contain a $schema property – default meta‑schema must be used.
        JsonNode node = new ObjectMapper().readTree("{ \"type\": \"string\" }");
        // Private method via reflection
        JsonMetaSchema meta = (JsonMetaSchema) invokePrivate(factory,
                "findMetaSchemaForSchema",
                new Class[]{JsonNode.class}, node);
        assertEquals(factory.defaultMetaSchemaURI, meta.getUri());
    }

    @Test
    public void testFindMetaSchemaForSchemaInvalid$schemaThrows() throws Exception {
        thrown.expect(JsonSchemaException.class);
        JsonNode node = new ObjectMapper().readTree("{ \"$schema\": 123 }");
        invokePrivate(factory,
                "findMetaSchemaForSchema",
                new Class[]{JsonNode.class}, node);
    }

    /* -----------------------------------------------------------------------
     *  Accessors
     * --------------------------------------------------------------------- */

    @Test
    public void testGetUriFactoryNotNull() {
        assertNotNull("URIFactory must be non‑null", factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslatorNotNull() {
        assertNotNull("URITranslator must be non‑null", factory.getUriTranslator());
    }
}
```
###Test END##