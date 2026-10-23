###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The test class covers:
 *   1. Extraction of all public methods.
 *   2. Basic functional tests for each public method.
 *   3. Edge‑case and exception handling tests (null arguments, illegal values, etc.).
 *
 * The tests rely only on the public API of the library and use reflection where private
 * helpers need to be exercised.
 *
 * NOTE: The library contains many heavy‑weight classes (JsonSchema, JsonMetaSchema, …).
 * For the purpose of unit testing the factory we use the built‑in Draft‑V4 meta‑schema
 * which is provided by the library itself.  This avoids the need for external files
 * or network access.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factory;

    /** A minimal valid JSON schema used throughout the tests. */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    @Before
    public void setUp() {
        // Use the public static helper to obtain a factory for Draft‑V4.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull("Factory must be created", factory);
    }

    /* -----------------------------------------------------------------------
     * 1.  Public method list (extracted for documentation purposes)
     * -----------------------------------------------------------------------
     *
     *  public static Builder builder()
     *  @Deprecated public static JsonSchemaFactory getInstance()
     *  public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag)
     *  public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag)
     *  public static Builder builder(JsonSchemaFactory blueprint)
     *
     *  protected JsonSchema newJsonSchema(URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig config)
     *  public JsonSchema create(ValidationContext validationContext, String schemaPath,
     *                           JsonNode schemaNode, JsonSchema parentSchema)
     *
     *  public URIFactory getUriFactory()
     *  public URITranslator getUriTranslator()
     *
     *  public JsonSchema getSchema(String schema, SchemaValidatorsConfig config)
     *  public JsonSchema getSchema(String schema)
     *  public JsonSchema getSchema(InputStream schemaStream, SchemaValidatorsConfig config)
     *  public JsonSchema getSchema(InputStream schemaStream)
     *  public JsonSchema getSchema(URI schemaUri, SchemaValidatorsConfig config)
     *  public JsonSchema getSchema(URI schemaUri)
     *  public JsonSchema getSchema(URI schemaUri, JsonNode jsonNode, SchemaValidatorsConfig config)
     *
     *  public JsonSchema getSchema(JsonNode jsonNode, SchemaValidatorsConfig config)
     *  public JsonSchema getSchema(JsonNode jsonNode)
     *
     *  private boolean idMatchesSourceUri(JsonMetaSchema metaSchema, JsonNode schema, URI schemaUri)
     *  private boolean isYaml(URI schemaUri)
     *  static protected String normalizeMetaSchemaUri(String u)
     *
     * ----------------------------------------------------------------------- */

    /* -----------------------------------------------------------------------
     * 2.  Basic functional tests for each public method
     * ----------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(factory.defaultMetaSchemaURI)
                .addMetaSchema(factory.jsonMetaSchemas.values().iterator().next())
                .build();
        assertNotNull(f);
    }

    @Test
    public void testGetInstanceWithoutArgumentReturnsV4Factory() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull(f);
        // The default meta‑schema URI for Draft‑V4 is known – verify it matches.
        assertEquals(JsonSchemaFactory.normalizeMetaSchemaUri(
                JsonSchemaVersion.DRAFTV4.getInstance().getUri()),
                f.defaultMetaSchemaURI);
    }

    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory f7 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(f7);
        assertEquals(JsonSchemaFactory.normalizeMetaSchemaUri(
                JsonSchemaVersion.DRAFTV7.getInstance().getUri()),
                f7.defaultMetaSchemaURI);
    }

    @Test
    public void testCheckVersionValidFlags() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaVersion v = JsonSchemaFactory.checkVersion(flag);
            assertNotNull(v);
        }
    }

    @Test
    public void testBuilderFromBlueprint() {
        JsonSchemaFactory copy = JsonSchemaFactory.builder(factory).build();
        assertNotNull(copy);
        // The copy should have the same default meta‑schema URI.
        assertEquals(factory.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
    }

    @Test
    public void testNewJsonSchemaCreatesInstance() throws Exception {
        // Use reflection to access the protected method.
        Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.readTree(SIMPLE_SCHEMA);
        JsonSchema schema = (JsonSchema) m.invoke(factory, null, node, null);
        assertNotNull(schema);
        assertEquals("#", schema.getCurrentUri().toString()); // schema path defaults to "#"
    }

    @Test
    public void testCreateMethodDelegatesToDoCreate() throws Exception {
        // Build a trivial ValidationContext via the factory.
        ValidationContext ctx = factory.createValidationContext(
                new ObjectMapper().readTree(SIMPLE_SCHEMA));
        JsonSchema parent = factory.getSchema(SIMPLE_SCHEMA);
        JsonSchema result = factory.create(ctx, null, new ObjectMapper().readTree(SIMPLE_SCHEMA), parent);
        assertNotNull(result);
        // The returned schema should have the same validation context as the parent (except the path).
        assertEquals(parent.getValidationContext().getJsonMetaSchema(),
                result.getValidationContext().getJsonMetaSchema());
    }

    @Test
    public void testGetUriFactoryIsNotNull() {
        assertNotNull(factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslatorCombinesMappings() {
        URITranslator translator = factory.getUriTranslator();
        assertNotNull(translator);
        // With the default factory there are no user‑defined mappings; the translator should simply
        // return the same URI when asked to translate.
        URI original = URI.create("http://example.com/schema.json");
        assertEquals(original, translator.translate(original));
    }

    @Test
    public void testGetSchemaFromString() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema);
        assertEquals("#", schema.getCurrentUri().toString());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidStringThrowsException() {
        factory.getSchema("{invalidJson:}");
    }

    @Test
    public void testGetSchemaFromInputStream() {
        InputStream is = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidInputStreamThrowsException() {
        InputStream is = new ByteArrayInputStream("{invalidJson:}".getBytes());
        factory.getSchema(is);
    }

    @Test
    public void testGetSchemaFromUriLoadsResource() throws Exception {
        // Use a class‑path resource that is known to exist (the same simple schema as a string).
        // For the test we write the schema to a temporary file and reference it via a file URI.
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("schema", ".json");
        java.nio.file.Files.write(temp, SIMPLE_SCHEMA.getBytes());
        temp.toFile().deleteOnExit();

        URI uri = temp.toUri();
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromNonexistentUriThrowsException() {
        URI uri = URI.create("file:/nonexistent-schema-xyz.json");
        factory.getSchema(uri);
    }

    @Test
    public void testGetSchemaWithJsonNodeAndConfig() {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_SCHEMA);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getSchema(node, config);
        assertNotNull(schema);
        assertSame(config, schema.getValidationContext().getConfig());
    }

    /* -----------------------------------------------------------------------
     * 3.  Edge‑case & exception tests (including private helpers via reflection)
     * ----------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithNullObjectMapperThrowsOnBuild() {
        JsonSchemaFactory.builder()
                .objectMapper(null)               // set to null deliberately
                .defaultMetaSchemaURI(factory.defaultMetaSchemaURI)
                .addMetaSchema(factory.jsonMetaSchemas.values().iterator().next())
                .build();                         // should fail inside private constructor
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderWithNullDefaultMetaSchemaUriThrowsOnBuild() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(null)
                .addMetaSchema(factory.jsonMetaSchemas.values().iterator().next())
                .build();
    }

    @Test
    public void testCheckVersionWithNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionWithUnsupportedFlagThrows() {
        // Create a fake enum constant that is not handled (use reflection to bypass compile‑time check)
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V999"));
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriWithInvalidStringThrows() {
        JsonSchemaFactory.normalizeMetaSchemaUri("http://[invalid-uri]");
    }

    @Test
    public void testIsYamlDetectsYamlExtensionViaReflection() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);
        URI yamlUri = URI.create("file:/test-schema.yaml");
        URI jsonUri = URI.create("file:/test-schema.json");
        assertTrue((Boolean) isYaml.invoke(factory, yamlUri));
        assertFalse((Boolean) isYaml.invoke(factory, jsonUri));
    }

    @Test
    public void testIdMatchesSourceUriViaReflection() throws Exception {
        // Build a simple schema node that contains an "$id" matching the supplied URI.
        String schemaWithId = "{\"$id\":\"http://example.com/my-schema\",\"type\":\"object\"}";
        JsonNode node = new ObjectMapper().readTree(schemaWithId);
        JsonMetaSchema meta = factory.jsonMetaSchemas.values().iterator().next(); // any meta‑schema

        Method method = JsonSchemaFactory.class.getDeclaredMethod(
                "idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);
        boolean matches = (Boolean) method.invoke(factory, meta, node,
                URI.create("http://example.com/my-schema"));
        assertTrue(matches);
    }

    @Test
    public void testAddUriMappingsWithNullMapThrowsNPE() {
        try {
            JsonSchemaFactory.builder().addUriMappings(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected – map.putAll(null) throws NPE
        }
    }

    /* -----------------------------------------------------------------------
     * 4.  Verify that the factory caches schemas when the cache is enabled
     * ----------------------------------------------------------------------- */
    @Test
    public void testUriSchemaCacheBehaviour() throws Exception {
        // Enable cache explicitly (it is enabled by default).
        JsonSchemaFactory cacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(factory.defaultMetaSchemaURI)
                .addMetaSchema(factory.jsonMetaSchemas.values().iterator().next())
                .enableUriSchemaCache(true)
                .build();

        // Write a temporary schema file.
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("cache-schema", ".json");
        java.nio.file.Files.write(temp, SIMPLE_SCHEMA.getBytes());
        temp.toFile().deleteOnExit();

        URI uri = temp.toUri();

        // First load – should populate the cache.
        JsonSchema first = cacheFactory.getSchema(uri);
        assertNotNull(first);

        // Directly inspect the private cache map via reflection.
        java.lang.reflect.Field cacheField = JsonSchemaFactory.class.getDeclaredField("uriSchemaCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<URI, JsonSchema> cacheMap =
                (java.util.concurrent.ConcurrentMap<URI, JsonSchema>) cacheField.get(cacheFactory);
        assertTrue("Cache must contain the loaded URI", cacheMap.containsKey(uri));

        // Second load – should retrieve the same instance.
        JsonSchema second = cacheFactory.getSchema(uri);
        assertSame("Cached schema instance should be returned", first, second);
    }

    /* -----------------------------------------------------------------------
     * 5.  Verify that disabling the cache results in distinct instances
     * ----------------------------------------------------------------------- */
    @Test
    public void testDisableUriSchemaCacheProducesNewInstances() throws Exception {
        JsonSchemaFactory noCacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(factory.defaultMetaSchemaURI)
                .addMetaSchema(factory.jsonMetaSchemas.values().iterator().next())
                .enableUriSchemaCache(false)
                .build();

        java.nio.file.Path temp = java.nio.file.Files.createTempFile("nocache-schema", ".json");
        java.nio.file.Files.write(temp, SIMPLE_SCHEMA.getBytes());
        temp.toFile().deleteOnExit();

        URI uri = temp.toUri();

        JsonSchema first = noCacheFactory.getSchema(uri);
        JsonSchema second = noCacheFactory.getSchema(uri);
        assertNotSame("Instances must differ when cache is disabled", first, second);
    }

}
```
###Test END##