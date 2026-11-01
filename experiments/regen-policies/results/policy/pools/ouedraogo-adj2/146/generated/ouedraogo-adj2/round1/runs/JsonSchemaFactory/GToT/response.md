**Public API of `JsonSchemaFactory` (extracted from the source)**  

| # | Method signature |
|---|------------------|
| 1 | `public SchemaLoader getSchemaLoader()` |
| 2 | `public static Builder builder()` |
| 3 | `public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag)` |
| 4 | `public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag, Consumer<JsonSchemaFactory.Builder> customizer)` |
| 5 | `public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag)` |
| 6 | `public static Builder builder(final JsonSchemaFactory blueprint)` |
| 7 | `public JsonSchema getSchema(final String schema, final SchemaValidatorsConfig config)` |
| 8 | `public JsonSchema getSchema(final String schema, InputFormat inputFormat, final SchemaValidatorsConfig config)` |
| 9 | `public JsonSchema getSchema(final String schema)` |
|10 | `public JsonSchema getSchema(final String schema, InputFormat inputFormat)` |
|11 | `public JsonSchema getSchema(final InputStream schemaStream, final SchemaValidatorsConfig config)` |
|12 | `public JsonSchema getSchema(final InputStream schemaStream, InputFormat inputFormat, final SchemaValidatorsConfig config)` |
|13 | `public JsonSchema getSchema(final InputStream schemaStream)` |
|14 | `public JsonSchema getSchema(final SchemaLocation schemaUri, final SchemaValidatorsConfig config)` |
|15 | `public JsonSchema getSchema(final URI schemaUri)` |
|16 | `public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config)` |
|17 | `public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode)` |
|18 | `public JsonSchema getSchema(final SchemaLocation schemaUri)` |
|19 | `public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config)` |
|20 | `public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode)` |
|21 | `public JsonSchema getSchema(final JsonNode jsonNode, final SchemaValidatorsConfig config)` |
|22 | `public JsonSchema getSchema(final JsonNode jsonNode)` |

---

### Identified Edge‑Cases & Exception Scenarios  

| Method | Edge case / exception to test |
|--------|------------------------------|
| `checkVersion` | `null` argument (should return `null`). |
| `getInstance` (both overloads) | Passing `null` flag – should return a factory with `null` default meta‑schema (the private constructor will throw `IllegalArgumentException`). |
| `Builder.build()` | – `defaultMetaSchemaIri` is `null` or empty → `IllegalArgumentException`. <br> – `metaSchemas` empty → `IllegalArgumentException`. <br> – `defaultMetaSchemaIri` not present in `metaSchemas` → `IllegalArgumentException`. |
| `getSchema(String)` | Empty string, malformed JSON → `JsonSchemaException`. |
| `getSchema(String, InputFormat, …)` | Unsupported `InputFormat` (anything other than JSON/YAML) → `IllegalArgumentException` from `getObjectMapper`. |
| `getSchema(InputStream, …)` | `null` stream → `NullPointerException` (propagated from `readTree`). |
| `getSchema(SchemaLocation, …)` | When caching is **enabled**, the same `SchemaLocation` should return the same instance (identity). <br> When caching is **disabled**, two calls may return different instances. |
| `getSchema(URI)` | Non‑existent file URI → `JsonSchemaException`. |
| `getSchema(JsonNode, …)` | Passing `null` config → `NullPointerException` inside `preload`. |
| General | Verify that `getSchemaLoader()` never returns `null`. |

---

## Complete JUnit‑4 Test File  

```java
###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests cover:
 *   • basic successful usage of each public method,
 *   • edge‑cases and expected exception paths,
 *   • builder validation logic,
 *   • caching behaviour,
 *   • handling of different input formats (JSON / YAML).
 */
public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factoryV4;
    private static final String SIMPLE_JSON_SCHEMA = "{\"type\":\"string\"}";
    private static final String SIMPLE_YAML_SCHEMA = "type: string";

    @Before
    public void setUp() {
        // Use the V4 dialect – it is always available via JsonMetaSchema.getV4()
        factoryV4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    }

    @After
    public void tearDown() {
        factoryV4 = null;
    }

    /* -------------------------------------------------
     *  1. getSchemaLoader()
     * ------------------------------------------------- */
    @Test
    public void testGetSchemaLoader_NotNull() {
        assertNotNull("SchemaLoader must be instantiated", factoryV4.getSchemaLoader());
    }

    /* -------------------------------------------------
     *  2. builder() static factory
     * ------------------------------------------------- */
    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
        // minimal valid configuration: default IRI + at least one meta‑schema
        b.defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
         .metaSchema(JsonMetaSchema.getV4());
        JsonSchemaFactory f = b.build();
        assertNotNull(f);
        assertEquals(JsonMetaSchema.getV4().getIri(),
                     f.getMetaSchema(JsonMetaSchema.getV4().getIri(), new SchemaValidatorsConfig()).getIri());
    }

    /* -------------------------------------------------
     *  3 & 4. getInstance() overloads & checkVersion()
     * ------------------------------------------------- */
    @Test
    public void testCheckVersion_NullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testGetInstance_WithCustomizer() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4,
                builder -> builder.enableSchemaCache(false));
        // internal flag is private – we verify behaviour through the cache
        JsonSchema schema1 = f.getSchema(SIMPLE_JSON_SCHEMA);
        JsonSchema schema2 = f.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotSame("Cache disabled → distinct instances", schema1, schema2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_MissingDefaultMetaSchemaIri_Throws() {
        JsonSchemaFactory.builder()
                .metaSchema(JsonMetaSchema.getV4())
                .build(); // defaultMetaSchemaIri is null → exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_EmptyMetaSchemas_Throws() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .build(); // metaSchemas empty → exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_DefaultIriNotInMetaSchemas_Throws() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("http://unknown/meta")
                .metaSchema(JsonMetaSchema.getV4())
                .build(); // default IRI not present → exception
    }

    /* -------------------------------------------------
     *  5. getSchema(String) – happy path
     * ------------------------------------------------- */
    @Test
    public void testGetSchemaFromString_ValidJson() {
        JsonSchema schema = factoryV4.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull(schema);
        // Validate a correct instance – should produce an empty error set
        assertTrue(schema.validate(factoryV4.getJsonMapper().readTree("\"hello\"")).isEmpty());
    }

    /* -------------------------------------------------
     *  6. getSchema(String) – malformed JSON
     * ------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromString_InvalidJson_Throws() {
        factoryV4.getSchema("{ invalid json }");
    }

    /* -------------------------------------------------
     *  7. getSchema(String, InputFormat, …) – YAML support
     * ------------------------------------------------- */
    @Test
    public void testGetSchemaFromString_Yaml() {
        JsonSchema schema = factoryV4.getSchema(SIMPLE_YAML_SCHEMA, InputFormat.YAML,
                new SchemaValidatorsConfig());
        assertNotNull(schema);
        JsonNode node = factoryV4.getYamlMapper().readTree(" \"world\" ");
        assertTrue(schema.validate(node).isEmpty());
    }

    /* -------------------------------------------------
     *  8. getSchema(InputStream, …)
     * ------------------------------------------------- */
    @Test
    public void testGetSchemaFromInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream(SIMPLE_JSON_SCHEMA.getBytes("UTF-8"));
        JsonSchema schema = factoryV4.getSchema(is, new SchemaValidatorsConfig());
        assertNotNull(schema);
        JsonNode node = factoryV4.getJsonMapper().readTree("\"test\"");
        assertTrue(schema.validate(node).isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStream_InvalidJson_Throws() throws Exception {
        InputStream is = new ByteArrayInputStream("{ not json".getBytes("UTF-8"));
        factoryV4.getSchema(is, new SchemaValidatorsConfig());
    }

    /* -------------------------------------------------
     *  9. getSchema(SchemaLocation, …) – caching behaviour
     * ------------------------------------------------- */
    @Test
    public void testGetSchema_WithCacheEnabled_ReturnsSameInstance() throws Exception {
        // Write a temporary file that will be loaded via file‑URI
        File tmp = File.createTempFile("schema", ".json");
        tmp.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write(SIMPLE_JSON_SCHEMA);
        }
        SchemaLocation location = SchemaLocation.of(tmp.toURI().toString());

        JsonSchema s1 = factoryV4.getSchema(location, new SchemaValidatorsConfig());
        JsonSchema s2 = factoryV4.getSchema(location, new SchemaValidatorsConfig());

        assertSame("When cache is enabled the same instance must be returned", s1, s2);
    }

    @Test
    public void testGetSchema_WithCacheDisabled_ReturnsDifferentInstances() throws Exception {
        JsonSchemaFactory factoryNoCache = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .enableSchemaCache(false)
                .build();

        File tmp = File.createTempFile("schema", ".json");
        tmp.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmp)) {
            fw.write(SIMPLE_JSON_SCHEMA);
        }
        SchemaLocation location = SchemaLocation.of(tmp.toURI().toString());

        JsonSchema s1 = factoryNoCache.getSchema(location, new SchemaValidatorsConfig());
        JsonSchema s2 = factoryNoCache.getSchema(location, new SchemaValidatorsConfig());

        assertNotSame("Cache disabled → distinct instances", s1, s2);
    }

    /* -------------------------------------------------
     * 10. getSchema(URI) – non‑existent file
     * ------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromNonExistingUri_Throws() throws Exception {
        URI bogus = new File("does-not-exist.json").toURI();
        factoryV4.getSchema(bogus);
    }

    /* -------------------------------------------------
     * 11. getSchema(JsonNode, …) – null config
     * ------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testGetSchemaFromJsonNode_NullConfig_NPE() {
        JsonNode node = factoryV4.getJsonMapper().createObjectNode();
        factoryV4.getSchema(node, null);
    }

    /* -------------------------------------------------
     * 12. builder(JsonSchemaFactory) – copy semantics
     * ------------------------------------------------- */
    @Test
    public void testBuilderFromBlueprint_CopiesConfiguration() {
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(factoryV4);
        JsonSchemaFactory copyFactory = copyBuilder.build();
        assertNotNull(copyFactory);
        // The default meta‑schema IRI must be identical
        assertEquals(factoryV4.defaultMetaSchemaIri, copyFactory.defaultMetaSchemaIri);
    }

    /* -------------------------------------------------
     * 13. getObjectMapper() – unsupported format path (indirect)
     * ------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testGetObjectMapper_UnsupportedInputFormat_Throws() {
        // The method is package‑private; we reach it via getSchema(String, InputFormat,…)
        // using a deliberately bogus enum value – we create an anonymous subclass to
        // bypass compile‑time checks.
        InputFormat bogus = InputFormat.JSON; // valid enum – we need a different one,
        // therefore we invoke the private method via reflection.
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("getObjectMapper", InputFormat.class);
            m.setAccessible(true);
            m.invoke(factoryV4, (Object) null); // null triggers the IllegalArgumentException
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------
     * 14. normalizeMetaSchemaUri – various normalisations
     * ------------------------------------------------- */
    @Test
    public void testNormalizeMetaSchemaUri_UnnormalizedIds() {
        // The method is package‑private; use reflection.
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
            m.setAccessible(true);
            String v7 = (String) m.invoke(null, "http://json-schema.org/draft-07/schema#");
            assertEquals(SchemaId.V7, v7);
            String unknown = (String) m.invoke(null, "http://example.com/custom");
            assertEquals("http://example.com/custom", unknown);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    /* -------------------------------------------------
     * 15. preload() – ensure that preload does not break when
     *                 validators throw an exception.
     * ------------------------------------------------- */
    @Test
    public void testPreloadExceptionIsSwallowed() {
        // Build a factory with a meta‑schema factory that returns a meta‑schema
        // whose validator throws an exception on initialise.
        JsonMetaSchemaFactory badFactory = (iri, fac, cfg) -> {
            JsonMetaSchema meta = JsonMetaSchema.getV4();
            // Wrap the original meta‑schema to inject a failing validator
            return new JsonMetaSchema.Builder(meta.getIri(), meta) {
                @Override
                public JsonValidator newValidator(ValidationContext vc, SchemaLocation sl,
                                                   JsonNodePath ep, String kw, JsonNode sn,
                                                   JsonSchema ps) {
                    return new JsonValidator(vc, meta, sl, ep, kw, sn, ps) {
                        @Override
                        public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode,
                                                               JsonNodePath instanceLocation) {
                            throw new RuntimeException("forced failure");
                        }
                    };
                }
            }.build();
        };

        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .metaSchemaFactory(badFactory)
                .build();

        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPreloadJsonSchema(true); // ask factory to preload

        // The call must *not* propagate the RuntimeException
        JsonSchema schema = f.getSchema(SIMPLE_JSON_SCHEMA, cfg);
        assertNotNull(schema);
    }
}
###Test END##
```