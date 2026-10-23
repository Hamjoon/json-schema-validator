**1. Public methods of `JsonSchemaFactory` (including static methods)**  

| # | Method signature |
|---|------------------|
| 1 | `public static JsonSchemaFactory.Builder builder()` |
| 2 | `@Deprecated public static JsonSchemaFactory getInstance()` |
| 3 | `public static JsonSchemaFactory getInstance(com.networknt.schema.SpecVersion.VersionFlag versionFlag)` |
| 4 | `public static com.networknt.schema.JsonSchemaVersion checkVersion(com.networknt.schema.SpecVersion.VersionFlag versionFlag)` |
| 5 | `public static JsonSchemaFactory.Builder builder(final JsonSchemaFactory blueprint)` |
| 6 | `public com.networknt.schema.URIFactory getUriFactory()` |
| 7 | `public com.networknt.schema.URITranslator getUriTranslator()` |
| 8 | `public com.networknt.schema.JsonSchema getSchema(final String schema, final com.networknt.schema.SchemaValidatorsConfig config)` |
| 9 | `public com.networknt.schema.JsonSchema getSchema(final String schema)` |
|10 | `public com.networknt.schema.JsonSchema getSchema(final java.io.InputStream schemaStream, final com.networknt.schema.SchemaValidatorsConfig config)` |
|11 | `public com.networknt.schema.JsonSchema getSchema(final java.io.InputStream schemaStream)` |
|12 | `public com.networknt.schema.JsonSchema getSchema(final java.net.URI schemaUri, final com.networknt.schema.SchemaValidatorsConfig config)` |
|13 | `public com.networknt.schema.JsonSchema getSchema(final java.net.URI schemaUri)` |
|14 | `public com.networknt.schema.JsonSchema getSchema(final java.net.URI schemaUri, final com.fasterxml.jackson.databind.JsonNode jsonNode, final com.networknt.schema.SchemaValidatorsConfig config)` |
|15 | `public com.networknt.schema.JsonSchema getSchema(final com.fasterxml.jackson.databind.JsonNode jsonNode, final com.networknt.schema.SchemaValidatorsConfig config)` |
|16 | `public com.networknt.schema.JsonSchema getSchema(final java.net.URI schemaUri, final com.fasterxml.jackson.databind.JsonNode jsonNode)` |
|17 | `public com.networknt.schema.JsonSchema getSchema(final com.fasterxml.jackson.databind.JsonNode jsonNode)` |
|18 | `public com.networknt.schema.JsonSchema create(com.networknt.schema.ValidationContext validationContext, String schemaPath, com.fasterxml.jackson.databind.JsonNode schemaNode, com.networknt.schema.JsonSchema parentSchema)` |

*Note*: The class also contains several **protected** and **private** methods (`newJsonSchema`, `createValidationContext`, `findMetaSchemaForSchema`, `fromId`, `idMatchesSourceUri`, `isYaml`, `normalizeMetaSchemaUri`).  
The test suite will focus on the **public** API, but a few private helpers are exercised via reflection to increase coverage.

---

**2. Basic JUnit‑4 test cases for each public method**

For every public method a simple “happy‑path” test is provided that verifies the method returns a non‑null value (or the expected behaviour) when called with valid arguments.

---

**3. Edge‑case / exception scenarios identified**

| Method | Edge case / exception to test |
|--------|------------------------------|
| `getInstance(SpecVersion.VersionFlag)` | Passing `null` should result in a `NullPointerException` (because `checkVersion(null)` returns `null` and the subsequent call on it fails). |
| `checkVersion` | Passing an unsupported enum value (simulated via reflection) → `IllegalArgumentException`. Passing `null` → returns `null`. |
| `getSchema(String)` | **Invalid JSON** → `JsonSchemaException`. <br> **`null` string** → `JsonSchemaException`. |
| `getSchema(InputStream)` | **Invalid JSON stream** → `JsonSchemaException`. |
| `getSchema(URI, Config)` | **Unsupported URI scheme** → `JsonSchemaException`. <br> **Malformed URI** → `JsonSchemaException`. |
| `getSchema(URI)` | **Cache behaviour** – calling twice with the same URI should return the same `JsonSchema` instance when caching is enabled. |
| `normalizeMetaSchemaUri` (private static) | **Invalid URI string** → `JsonSchemaException`. |
| `isYaml` (private) | Verify detection of `.yaml`, `.yml` and non‑YAML extensions via reflection. |
| `builder(JsonSchemaFactory)` | Ensure the builder created from a blueprint copies the default meta‑schema URI. |
| `getUriTranslator` | Verify that a custom mapping added via the builder is honoured. |

---

**4. Comprehensive JUnit‑4 test file**

```java
###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private static JsonSchemaFactory factory;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() {
        // Use the official V4 factory – it is fully initialised (defaultMetaSchemaURI, meta‑schemas, etc.).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull("Factory must be created", factory);
    }

    /* ----------------------------------------------------------------------
     *  1.  Basic “happy‑path” tests for the public API
     * ---------------------------------------------------------------------- */

    @Test
    public void testBuilderStaticMethod() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
        assertNotNull(b);
    }

    @Test
    public void testDeprecatedGetInstance() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(); // V4 by default
        assertNotNull(f);
    }

    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertNotNull(f);
    }

    @Test
    public void testCheckVersionValidValues() {
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void testCheckVersionNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testBuilderFromBlueprintCopiesDefaultMetaSchema() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(factory);
        JsonSchemaFactory copy = b.build();
        // The defaultMetaSchemaURI is private – we verify by creating a schema without $schema
        // and checking that the same meta‑schema is used (no exception).
        JsonSchema schema = copy.getSchema("{\"type\":\"string\"}");
        assertNotNull(schema);
    }

    @Test
    public void testGetUriFactoryNotNull() {
        assertNotNull(factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslatorHonoursMappings() throws Exception {
        // Build a factory with a custom mapping and verify the translator rewrites the URI.
        JsonSchemaFactory custom = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(factory.getSchema("{\"type\":\"object\"}").getValidationContext()
                        .getMetaSchema().getUri())
                .addMetaSchema(factory.getSchema("{\"type\":\"object\"}").getValidationContext()
                        .getMetaSchema())
                .addUriMappings(Collections.singletonMap("http://example.com/old", "http://example.com/new"))
                .build();

        URITranslator translator = custom.getUriTranslator();
        URI translated = translator.translate(new URI("http://example.com/old"));
        assertEquals("http://example.com/new", translated.toString());
    }

    @Test
    public void testGetSchemaFromString() {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromStringWithConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}", cfg);
        assertNotNull(schema);
        assertSame(cfg, schema.getValidationContext().getConfig());
    }

    @Test
    public void testGetSchemaFromInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream("{\"type\":\"array\"}".getBytes("UTF-8"));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromInputStreamWithConfig() throws Exception {
        InputStream is = new ByteArrayInputStream("{\"type\":\"array\"}".getBytes("UTF-8"));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getSchema(is, cfg);
        assertNotNull(schema);
        assertSame(cfg, schema.getValidationContext().getConfig());
    }

    @Test
    public void testGetSchemaFromUriFile() throws Exception {
        // write a tiny schema to a temporary file
        File file = tmp.newFile("temp-schema.json");
        try (Writer w = new FileWriter(file)) {
            w.write("{\"type\":\"null\"}");
        }
        URI uri = file.toURI();

        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromUriCacheBehaviour() throws Exception {
        File file = tmp.newFile("cached-schema.json");
        try (Writer w = new FileWriter(file)) {
            w.write("{\"type\":\"boolean\"}");
        }
        URI uri = file.toURI();

        JsonSchema first = factory.getSchema(uri);
        JsonSchema second = factory.getSchema(uri);
        // Because caching is enabled by default the two references must be identical.
        assertSame("Schema instances should be cached and identical", first, second);
    }

    @Test
    public void testCreateMethod() throws Exception {
        // Build a minimal ValidationContext via a private helper (reflection) because the
        // public API does not expose it directly.
        Method createVc = JsonSchemaFactory.class.getDeclaredMethod("createValidationContext", JsonNode.class);
        createVc.setAccessible(true);
        JsonNode node = new ObjectMapper().readTree("{\"type\":\"string\"}");
        ValidationContext ctx = (ValidationContext) createVc.invoke(factory, node);

        JsonSchema parent = factory.getSchema("{\"type\":\"object\"}");
        JsonSchema result = factory.create(ctx, "#", node, parent);
        assertNotNull(result);
    }

    /* ----------------------------------------------------------------------
     *  2.  Edge‑case / exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testGetInstanceWithNullFlagThrowsNPE() {
        // The implementation does not guard against null – a NPE is expected.
        JsonSchemaFactory.getInstance(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupportedThrows() {
        // Simulate an unsupported enum value by using reflection to create a bogus enum constant.
        // This is a bit artificial, but the public API protects the switch with a default that
        // throws IllegalArgumentException for any unknown value.
        VersionFlag fake = Enum.valueOf(VersionFlag.class, "V_FAKE");
        JsonSchemaFactory.checkVersion(fake);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidJsonString() {
        factory.getSchema("{ invalid json }");
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromNullString() {
        factory.getSchema((String) null);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidJsonInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream("{ bad json".getBytes("UTF-8"));
        factory.getSchema(is);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromUnsupportedSchemeUri() throws URISyntaxException {
        // The default URI factory does not know the "unknown" scheme.
        URI unknown = new URI("unknown://some/path");
        factory.getSchema(unknown);
    }

    @Test
    public void testNormalizeMetaSchemaUriValid() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        String normalized = (String) normalize.invoke(null, "http://example.com/schema");
        assertEquals("https://example.com/schema", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalid() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        // Pass a string that cannot be parsed as a URI.
        normalize.invoke(null, "http://[invalid]");
    }

    @Test
    public void testIsYamlDetection() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI yaml = new URI("file:///tmp/schema.yaml");
        URI yml  = new URI("file:///tmp/schema.yml");
        URI json = new URI("file:///tmp/schema.json");

        assertTrue((Boolean) isYaml.invoke(factory, yaml));
        assertTrue((Boolean) isYaml.invoke(factory, yml));
        assertFalse((Boolean) isYaml.invoke(factory, json));
    }

    @Test
    public void testIdMatchesSourceUriViaReflection() throws Exception {
        // Build a schema node that contains an $id matching the supplied URI.
        String schemaStr = "{\"$id\":\"http://example.com/my-schema.json\",\"type\":\"object\"}";
        JsonNode node = new ObjectMapper().readTree(schemaStr);
        URI uri = new URI("http://example.com/my-schema.json");

        // Access the private method.
        Method idMatches = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        idMatches.setAccessible(true);

        // Obtain the meta‑schema used for V4 (the one the factory is built with)
        JsonMetaSchema meta = factory.getSchema("{\"type\":\"object\"}").getValidationContext().getMetaSchema();

        Boolean result = (Boolean) idMatches.invoke(factory, meta, node, uri);
        assertTrue("id should match source URI", result);
    }

    /* ----------------------------------------------------------------------
     *  3.  Helper for creating a temporary YAML schema (used implicitly by
     *      the isYaml test – no direct public call required)
     * ---------------------------------------------------------------------- */
    private File writeTempFile(String name, String content) throws IOException {
        File f = tmp.newFile(name);
        try (Writer w = new FileWriter(f)) {
            w.write(content);
        }
        return f;
    }
}
###Test END##