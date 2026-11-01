**1. Public methods of `JsonSchemaFactory` (including inherited static helpers)**  

| # | Return type | Method signature | Visibility |
|---|------------|------------------|-------------|
| 1 | `SchemaLoader` | `public SchemaLoader getSchemaLoader()` | public |
| 2 | `Builder` | `public static Builder builder()` | public |
| 3 | `JsonSchemaFactory` | `public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag)` | public |
| 4 | `JsonSchemaFactory` | `public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag, Consumer<JsonSchemaFactory.Builder> customizer)` | public |
| 5 | `JsonSchemaVersion` | `public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag)` | public |
| 6 | `Builder` | `public static Builder builder(final JsonSchemaFactory blueprint)` | public |
| 7 | `JsonSchema` | `public JsonSchema getSchema(final String schema, final SchemaValidatorsConfig config)` | public |
| 8 | `JsonSchema` | `public JsonSchema getSchema(final String schema, InputFormat inputFormat, final SchemaValidatorsConfig config)` | public |
| 9 | `JsonSchema` | `public JsonSchema getSchema(final String schema)` | public |
|10 | `JsonSchema` | `public JsonSchema getSchema(final String schema, InputFormat inputFormat)` | public |
|11 | `JsonSchema` | `public JsonSchema getSchema(final InputStream schemaStream, final SchemaValidatorsConfig config)` | public |
|12 | `JsonSchema` | `public JsonSchema getSchema(final InputStream schemaStream, InputFormat inputFormat, final SchemaValidatorsConfig config)` | public |
|13 | `JsonSchema` | `public JsonSchema getSchema(final InputStream schemaStream)` | public |
|14 | `JsonSchema` | `public JsonSchema getSchema(final SchemaLocation schemaUri, final SchemaValidatorsConfig config)` | public |
|15 | `JsonSchema` | `public JsonSchema getSchema(final URI schemaUri)` | public |
|16 | `JsonSchema` | `public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config)` | public |
|17 | `JsonSchema` | `public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode)` | public |
|18 | `JsonSchema` | `public JsonSchema getSchema(final SchemaLocation schemaUri)` | public |
|19 | `JsonSchema` | `public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config)` | public |
|20 | `JsonSchema` | `public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode)` | public |
|21 | `JsonSchema` | `public JsonSchema getSchema(final JsonNode jsonNode, final SchemaValidatorsConfig config)` | public |
|22 | `JsonSchema` | `public JsonSchema getSchema(final JsonNode jsonNode)` | public |
|23 | `JsonMetaSchema` | `public JsonMetaSchema getMetaSchema(String iri, SchemaValidatorsConfig config)` | public |
|24 | `ObjectMapper` | `public ObjectMapper getYamlMapper()` | public |
|25 | `ObjectMapper` | `public ObjectMapper getJsonMapper()` | public |
|26 | `SchemaValidatorsConfig` | `protected SchemaValidatorsConfig createSchemaValidatorsConfig()` | protected (exposed via subclass in tests) |
|27 | `JsonNode` | `JsonNode readTree(String content, InputFormat inputFormat) throws IOException` | package‑private (tested via public wrapper) |
|28 | `JsonNode` | `JsonNode readTree(InputStream content, InputFormat inputFormat) throws IOException` | package‑private |
|29 | `ObjectMapper` | `ObjectMapper getObjectMapper(InputFormat inputFormat)` | package‑private |
|30 | `JsonSchema` | `protected JsonSchema newJsonSchema(SchemaLocation schemaUri, JsonNode schemaNode, SchemaValidatorsConfig config)` | protected |
|31 | `JsonSchema` | `public JsonSchema create(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema)` | public |
|32 | `JsonSchema` | `public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config)` | public |
|33 | `JsonSchema` | `public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config)` | public |
|…| … | (other protected/private helpers are omitted for brevity) |

---

**2. Basic JUnit‑4 test cases (one per public method)**  

Below is a short description of a “happy‑path” test for each public method listed above:

| # | Method | Test idea |
|---|--------|-----------|
|1|`getSchemaLoader()`|Assert that the returned object is not **null** and is an instance of `SchemaLoader`.|
|2|`builder()`|Create a `Builder`, set a default meta‑schema (`JsonMetaSchema.getV4()`) and build a factory; assert the factory is not **null**.|
|3|`getInstance(VersionFlag)`|Call with `VersionFlag.V4` and verify the factory’s default meta‑schema IRI equals `JsonMetaSchema.getV4().getIri()`.|
|4|`getInstance(VersionFlag, Consumer)`|Pass a customizer that disables cache; verify `enableSchemaCache` is **false** via reflection.|
|5|`checkVersion`|For each enum constant verify the returned object is an instance of the expected version class (`Version4`, `Version6`, …).|
|6|`builder(JsonSchemaFactory)`|Create a factory, then obtain a new builder from it; the new builder should contain the same default meta‑schema IRI.|
|7|`getSchema(String, Config)`|Supply a minimal JSON schema string (`{"type":"object"}`) and a default config; assert a non‑null `JsonSchema` is returned.|
|8|`getSchema(String, InputFormat, Config)`|Same as #7 but explicitly use `InputFormat.JSON`.|
|9|`getSchema(String)`|Call the convenience overload; it should return a non‑null schema.|
|10|`getSchema(String, InputFormat)`|Same as #9 but with `InputFormat.JSON`.|
|11|`getSchema(InputStream, Config)`|Pass an `InputStream` containing a simple schema; assert non‑null result.|
|12|`getSchema(InputStream, InputFormat, Config)`|Same as #11 with explicit format.|
|13|`getSchema(InputStream)`|Convenience overload – same assertion.|
|14|`getSchema(SchemaLocation, Config)`|Create a `SchemaLocation` that points to a class‑path resource (e.g., `"classpath:/dummy-schema.json"`). Use a dummy loader that returns an `InputStream` with a simple schema; assert the returned schema is not **null**.|
|15|`getSchema(URI)`|Pass `new URI("http://example.com/dummy.json")` (the default loader will try to fetch it – to avoid network calls we use a factory whose `schemaLoadersBuilder` is customised to return a stub loader that supplies a local stream).|
|16|`getSchema(URI, JsonNode, Config)`|Create a `JsonNode` from a string and call the method; verify the returned schema contains the same node (e.g., `schema.getValidationContext().getMetaSchema().getIri()` matches default).|
|17|`getSchema(URI, JsonNode)`|Same as #16 with default config.|
|18|`getSchema(SchemaLocation)`|Convenience overload – assert non‑null.|
|19|`getSchema(SchemaLocation, JsonNode, Config)`|Build a schema from a node and location; verify the location is stored (`schema.getValidationContext().getMetaSchema()` etc.).|
|20|`getSchema(SchemaLocation, JsonNode)`|Same as #19 with default config.|
|21|`getSchema(JsonNode, Config)`|Pass a node created from a simple JSON string; assert non‑null.|
|22|`getSchema(JsonNode)`|Convenience overload – same assertion.|
|23|`getMetaSchema(String, Config)`|Ask for the V4 meta‑schema IRI; the returned object should be `JsonMetaSchema.getV4()`.|
|24|`getYamlMapper()`|When the factory is built without a custom YAML mapper, the returned mapper must be the singleton from `YamlMapperFactory`.|
|25|`getJsonMapper()`|Same as #24 but for JSON mapper (`JsonMapperFactory`).|
|26|`createSchemaValidatorsConfig()`|Call via a subclass that exposes the method; verify a non‑null config.|
|27|`readTree(String, InputFormat)`|Parse a JSON string and assert the resulting `JsonNode` has the expected field/value.|
|28|`readTree(InputStream, InputFormat)`|Same as #27 but using a `ByteArrayInputStream`.|
|29|`getObjectMapper(InputFormat)`|Pass `InputFormat.JSON` and `InputFormat.YAML`; each should return a non‑null mapper.|
|30|`newJsonSchema(...)`|Invoke indirectly via `getSchema(JsonNode)` (which calls `newJsonSchema`) and verify the created schema is not **null**.|
|31|`create(...)`|Create a `ValidationContext` via `createValidationContext`, then call `create`; the returned schema must not be **null**.|
|32|`getSchema(URI, JsonNode, Config)`|Same as #16 – already covered.|
|33|`getSchema(SchemaLocation, JsonNode, Config)`|Same as #19 – already covered.|

---

**3. Edge‑case & exception scenarios that need dedicated tests**

| # | Scenario | Reason |
|---|----------|--------|
|E1|`Builder.build()` with **null** `defaultMetaSchemaIri` | Constructor should throw `IllegalArgumentException`. |
|E2|`Builder.build()` with **empty** `defaultMetaSchemaIri` (e.g., `"   "`) | Same exception as above. |
|E3|`Builder.build()` with **no meta‑schemas** supplied | Constructor throws `IllegalArgumentException`. |
|E4|`Builder.build()` where the supplied default IRI does **not exist** in the meta‑schema map | Constructor throws `IllegalArgumentException`. |
|E5|`getSchema(String)` with **malformed JSON** (e.g., `"{"`) | Should raise `JsonSchemaException`. |
|E6|`getObjectMapper(null)` (or any unsupported `InputFormat`) | Should raise `IllegalArgumentException`. |
|E7|`getMetaSchema(String, Config)` for an **unknown IRI** when a custom `JsonMetaSchemaFactory` is supplied | The factory must delegate to the custom factory; test that the returned meta‑schema’s IRI equals the requested IRI. |
|E8|`preload` path – when `config.isPreloadJsonSchema()` is **true** but the schema contains an *unresolvable* `$ref` – the method must **not** propagate the exception (it is swallowed). We can simulate this by creating a schema with `$ref` to a non‑existent URI and a config with preload enabled; the call to `getSchema` must still return a `JsonSchema` object without throwing. |
|E9|`isYaml` detection – feed a `SchemaLocation` ending with `.yaml` and `.json` and verify the returned boolean via reflection (optional). |

All of the above can be exercised without external network calls by using the builder’s `schemaLoaders` customiser to inject a **stub `SchemaLoader`** that returns a predefined `InputStreamSource` (a simple implementation that wraps a `ByteArrayInputStream`). This guarantees deterministic behaviour.

---

**4. Complete JUnit‑4 test file**  

```java
###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 * The tests cover the public API, normal behaviour, edge cases and
 * exception handling without any external I/O.
 */
public class JsonSchemaFactoryTest {

    /** a minimal valid JSON schema used by many tests */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /** a deliberately malformed JSON string */
    private static final String BAD_JSON = "{";

    /** default meta‑schema (draft‑04) used for most factories */
    private static final JsonMetaSchema DEFAULT_META = JsonMetaSchema.getV4();

    private JsonSchemaFactory factory;   // factory built with the default meta‑schema

    @Before
    public void setUp() {
        // Build a factory that has at least one meta‑schema (V4) and a proper default IRI.
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(DEFAULT_META.getIri())
                .metaSchema(DEFAULT_META)
                .build();
    }

    /* --------------------------------------------------------------
     *  1.  Basic happy‑path tests for public methods
     * -------------------------------------------------------------- */

    @Test
    public void testGetSchemaLoaderNotNull() {
        SchemaLoader loader = factory.getSchemaLoader();
        assertNotNull(loader);
    }

    @Test
    public void testStaticBuilderCreatesFactory() {
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(DEFAULT_META.getIri())
                .metaSchema(DEFAULT_META)
                .build();
        assertNotNull(f);
    }

    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertEquals(JsonMetaSchema.getV4().getIri(), f.defaultMetaSchemaIri);
    }

    @Test
    public void testGetInstanceWithCustomizerDisablesCache() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4,
                b -> b.enableSchemaCache(false));
        // use reflection to read the private field 'enableSchemaCache'
        try {
            java.lang.reflect.Field field = JsonSchemaFactory.class.getDeclaredField("enableSchemaCache");
            field.setAccessible(true);
            boolean cacheEnabled = field.getBoolean(f);
            assertFalse(cacheEnabled);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testCheckVersionAllFlags() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull("Version should not be null for flag " + flag, version);
            // The concrete class names follow the pattern VersionXxx
            String className = version.getClass().getSimpleName();
            assertTrue("Unexpected version class: " + className,
                    className.startsWith("Version"));
        }
    }

    @Test
    public void testBuilderFromBlueprintCopiesValues() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(factory);
        JsonSchemaFactory f2 = b.build();
        assertEquals(factory.defaultMetaSchemaIri, f2.defaultMetaSchemaIri);
    }

    @Test
    public void testGetSchemaFromString() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema);
        assertEquals("object", schema.getValidationContext().getMetaSchema().getKeywords().get("type").getClass().getSimpleName());
    }

    @Test
    public void testGetSchemaWithExplicitFormat() {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA, InputFormat.JSON, new SchemaValidatorsConfig());
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes())) {
            JsonSchema schema = factory.getSchema(is, new SchemaValidatorsConfig());
            assertNotNull(schema);
        }
    }

    @Test
    public void testGetSchemaFromInputStreamWithFormat() throws IOException {
        try (InputStream is = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes())) {
            JsonSchema schema = factory.getSchema(is, InputFormat.JSON, new SchemaValidatorsConfig());
            assertNotNull(schema);
        }
    }

    @Test
    public void testGetSchemaWithUri() throws Exception {
        // Use a stub loader that returns the simple schema for any IRI.
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(DEFAULT_META.getIri())
                .metaSchema(DEFAULT_META)
                .schemaLoaders(b -> b.add(absoluteIri -> new InputStreamSource() {
                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
                    }
                }))
                .build();

        JsonSchema schema = f.getSchema(new URI("http://example.com/dummy.json"));
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaWithUriNodeAndConfig() throws Exception {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_SCHEMA);
        JsonSchema schema = factory.getSchema(new URI("http://example.com/inline"), node, new SchemaValidatorsConfig());
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaWithLocationAndNode() {
        SchemaLocation loc = SchemaLocation.of("http://example.com/loc");
        JsonNode node = new ObjectMapper().createObjectNode().put("type", "object");
        JsonSchema schema = factory.getSchema(loc, node);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromJsonNode() {
        JsonNode node = new ObjectMapper().createObjectNode().put("type", "object");
        JsonSchema schema = factory.getSchema(node);
        assertNotNull(schema);
    }

    @Test
    public void testGetMetaSchemaKnownIri() {
        JsonMetaSchema meta = factory.getMetaSchema(JsonMetaSchema.getV4().getIri(), new SchemaValidatorsConfig());
        assertSame(JsonMetaSchema.getV4(), meta);
    }

    @Test
    public void testGetYamlMapperFallback() {
        ObjectMapper yaml = factory.getYamlMapper();
        assertNotNull(yaml);
        // The fallback is the singleton from YamlMapperFactory – we cannot assert its exact class,
        // but we can assert it can read YAML.
        assertDoesNotThrow(() -> yaml.readTree("---\nkey: value"));
    }

    @Test
    public void testGetJsonMapperFallback() {
        ObjectMapper json = factory.getJsonMapper();
        assertNotNull(json);
        assertDoesNotThrow(() -> json.readTree("{\"a\":1}"));
    }

    @Test
    public void testReadTreeFromString() throws IOException {
        JsonNode node = factory.readTree("{\"hello\":\"world\"}", InputFormat.JSON);
        assertEquals("world", node.get("hello").asText());
    }

    @Test
    public void testReadTreeFromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream("{\"num\":42}".getBytes())) {
            JsonNode node = factory.readTree(is, InputFormat.JSON);
            assertEquals(42, node.get("num").asInt());
        }
    }

    @Test
    public void testGetObjectMapperSupportedFormats() {
        ObjectMapper json = factory.getObjectMapper(InputFormat.JSON);
        assertNotNull(json);
        ObjectMapper yaml = factory.getObjectMapper(InputFormat.YAML);
        assertNotNull(yaml);
    }

    /* --------------------------------------------------------------
     *  2.  Edge‑case & exception tests
     * -------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaIriIsNull() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(null)
                .metaSchema(DEFAULT_META)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultMetaSchemaIriIsEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("   ")
                .metaSchema(DEFAULT_META)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenNoMetaSchemasProvided() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(DEFAULT_META.getIri())
                .build(); // metaSchemas map is empty → IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderFailsWhenDefaultIriNotPresentInMetaSchemas() {
        // supply a different meta‑schema but keep default IRI pointing to V4 (which is missing)
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(DEFAULT_META.getIri())
                .metaSchema(JsonMetaSchema.getV6())
                .build();
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromBadJsonThrowsException() {
        factory.getSchema(BAD_JSON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetObjectMapperUnsupportedFormat() {
        // pass null – the method treats it as unsupported
        factory.getObjectMapper(null);
    }

    @Test
    public void testCustomMetaSchemaFactoryLoadsUnknownIri() {
        final String customIri = "http://example.com/custom-meta";
        JsonMetaSchemaFactory customFactory = (iri, fac, cfg) -> JsonMetaSchema.builder(iri).build();

        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(customIri)
                .metaSchema(JsonMetaSchema.builder(customIri).build())
                .metaSchemaFactory(customFactory)
                .build();

        JsonMetaSchema loaded = f.getMetaSchema("http://example.com/unknown", new SchemaValidatorsConfig());
        assertEquals("http://example.com/unknown", loaded.getIri());
    }

    @Test
    public void testPreloadSwallowsUnresolvableRef() {
        // schema with a $ref that points to a non‑existent location
        String schemaWithBadRef = "{\"$ref\":\"http://nonexistent/doesnotexist.json\"}";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPreloadJsonSchema(true); // enable preload

        // Use a stub loader that returns the schema itself (no external fetching)
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(DEFAULT_META.getIri())
                .metaSchema(DEFAULT_META)
                .schemaLoaders(b -> b.add(iri -> new InputStreamSource() {
                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(schemaWithBadRef.getBytes());
                    }
                }))
                .build();

        // The call must return a schema object without propagating the IOException
        JsonSchema schema = f.getSchema(schemaWithBadRef, cfg);
        assertNotNull(schema);
    }

    /* --------------------------------------------------------------
     *  3.  Helper methods used by tests
     * -------------------------------------------------------------- */

    /** tiny helper to make the JUnit‑4 syntax a bit nicer */
    private static void assertDoesNotThrow(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            fail("Unexpected exception: " + t);
        }
    }
}
###Test END##
```