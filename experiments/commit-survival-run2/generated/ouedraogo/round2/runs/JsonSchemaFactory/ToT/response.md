###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests are written from the perspective of three “experts”.  Each expert
 * contributes a test for a particular method (typical use, edge case and error
 * scenario).  The final file aggregates all of them so that the whole class
 * is exercised.
 *
 * The test suite uses Mockito to isolate the factory from the heavy‑weight
 * infrastructure (URI factories, fetchers, meta‑schemas, etc.).  A small
 * concrete implementation of {@link com.networknt.schema.JsonMetaSchema}
 * is provided only for the purpose of the tests – the real library supplies
 * many more features but they are not required for the behaviour we want to
 * verify here.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The test class is deliberately written in a single file so that it can be
 * compiled and run without any external test harness.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    /** A very small {@link JsonMetaSchema} implementation used in the tests. */
    private static class DummyMetaSchema extends JsonMetaSchema {
        private final String uri;

        DummyMetaSchema(String uri) {
            this.uri = uri;
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public String readId(JsonNode schemaNode) {
            JsonNode idNode = schemaNode.get("$id");
            return (idNode != null && idNode.isTextual()) ? idNode.textValue() : null;
        }

        // All other abstract methods are irrelevant for the current tests.
        @Override
        public String getIdKeyword() {
            return "$id";
        }

        @Override
        public String getSchemaKeyword() {
            return "$schema";
        }

        @Override
        public String getRefKeyword() {
            return "$ref";
        }

        @Override
        public JsonSchema getValidator(String keyword) {
            return null;
        }
    }

    /** A simple {@link JsonSchema} stub – we only need its identity. */
    private static class DummyJsonSchema extends JsonSchema {
        DummyJsonSchema(ValidationContext ctx, String path, URI uri, JsonNode node,
                         JsonSchema parent, boolean suppress) {
            super(ctx, path, uri, node, parent, suppress);
        }
    }

    /** Helper to build a factory with the minimal required configuration. */
    private JsonSchemaFactory buildFactory(DummyMetaSchema meta,
                                           URIFactory uriFactory,
                                           URIFetcher uriFetcher,
                                           boolean enableCache) {

        return JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(meta.getUri())
                .addMetaSchema(meta)
                .uriFactory(uriFactory, "test")
                .uriFetcher(uriFetcher, "test")
                .enableUriSchemaCache(enableCache)
                .build();
    }

    /** --------------------------------------------------------------------- *
     *  1. Builder validation – missing mandatory meta‑schema must raise an
     *     IllegalArgumentException.
     *  --------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void expert1_builderWithoutMetaSchema_throwsException() {
        // The builder is supplied with a defaultMetaSchemaURI but **no** meta‑schema.
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://example.com/draft-04/schema")
                .build(); // <-- should fail
    }

    /** --------------------------------------------------------------------- *
     *  2. getInstance(V4) – verifies that the factory is created with the
     *     correct default meta‑schema URI.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert2_getInstanceV4_returnsFactoryWithDraftV4Uri() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(factory);
        // The default meta‑schema URI for draft‑04 is defined in Version4.
        String expected = Version4.SCHEMA_URI; // "http://json-schema.org/draft-04/schema#"
        // Use reflection to read the private field (allowed in tests).
        try {
            java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("defaultMetaSchemaURI");
            f.setAccessible(true);
            String actual = (String) f.get(factory);
            assertEquals(JsonSchemaFactory.normalizeMetaSchemaUri(expected), actual);
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    /** --------------------------------------------------------------------- *
     *  3. checkVersion – null returns null; unsupported flag throws.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert3_checkVersion_nullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void expert3_checkVersion_unsupportedFlag_throws() {
        // Simulate an unknown enum constant by casting.
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V99"));
    }

    /** --------------------------------------------------------------------- *
     *  4. normalizeMetaSchemaUri – converts any scheme to https and throws on
     *     malformed input.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert4_normalizeMetaSchemaUri_convertsToHttps() {
        String httpUri = "http://example.com/some/schema.json";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(httpUri);
        assertTrue(normalized.startsWith("https://"));
        assertTrue(normalized.endsWith("/some/schema.json"));
    }

    @Test(expected = JsonSchemaException.class)
    public void expert4_normalizeMetaSchemaUri_malformedUri_throws() {
        JsonSchemaFactory.normalizeMetaSchemaUri("::::");
    }

    /** --------------------------------------------------------------------- *
     *  5. isYaml – true for .yaml/.yml extensions, false otherwise.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert5_isYaml_detectionWorks() throws URISyntaxException {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("https://example.com/draft/schema#")
                .addMetaSchema(new DummyMetaSchema("https://example.com/draft/schema#"))
                .build();

        // Use reflection to invoke the private method.
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);

        assertTrue((Boolean) m.invoke(factory, new URI("test://file.yaml")));
        assertTrue((Boolean) m.invoke(factory, new URI("test://file.yml")));
        assertFalse((Boolean) m.invoke(factory, new URI("test://file.json")));
        assertFalse((Boolean) m.invoke(factory, new URI("test://noextension")));
    }

    /** --------------------------------------------------------------------- *
     *  6. idMatchesSourceUri – verifies matching and non‑matching cases.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert6_idMatchesSourceUri_behaviour() throws Exception {
        DummyMetaSchema meta = new DummyMetaSchema("https://example.com/draft/schema#");
        JsonSchemaFactory factory = buildFactory(meta,
                (s) -> new URI(s), // very simple uriFactory
                (u) -> new ByteArrayInputStream("{\"$id\":\"http://example.com/foo\"}".getBytes()),
                false);

        // Access the private method.
        java.lang.reflect.Method method = JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);

        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.readTree("{\"$id\":\"http://example.com/foo\"}");
        URI source = new URI("http://example.com/foo");

        boolean match = (Boolean) method.invoke(factory, meta, node, source);
        assertTrue(match);

        URI different = new URI("http://example.com/bar");
        boolean noMatch = (Boolean) method.invoke(factory, meta, node, different);
        assertFalse(noMatch);
    }

    /** --------------------------------------------------------------------- *
     *  7. getSchema(String) – happy path (valid JSON) and error path (invalid JSON).
     *  --------------------------------------------------------------------- */
    @Test
    public void expert7_getSchemaString_validJson_returnsSchema() {
        DummyMetaSchema meta = new DummyMetaSchema("https://example.com/draft/schema#");
        JsonSchemaFactory factory = buildFactory(meta,
                (s) -> URI.create(s),
                (u) -> new ByteArrayInputStream("{\"type\":\"object\"}".getBytes()),
                false);

        // The JSON string does not contain $schema – factory should fall back to default.
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        assertNotNull(schema);
        // The schema should have the default meta‑schema attached.
        assertEquals(meta.getUri(), schema.getValidationContext().getMetaSchema().getUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void expert7_getSchemaString_invalidJson_throws() {
        DummyMetaSchema meta = new DummyMetaSchema("https://example.com/draft/schema#");
        JsonSchemaFactory factory = buildFactory(meta,
                (s) -> URI.create(s),
                (u) -> new ByteArrayInputStream("not a json".getBytes()),
                false);

        factory.getSchema("not a json");
    }

    /** --------------------------------------------------------------------- *
     *  8. getSchema(URI) with cache enabled – the same instance is returned
     *     on the second call and the config is refreshed.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert8_getSchemaUri_cacheWorks() throws Exception {
        // Simple factory that returns the same JSON for any URI.
        URIFetcher fetcher = mock(URIFetcher.class);
        when(fetcher.fetch(any())).thenAnswer(inv -> new ByteArrayInputStream("{\"type\":\"string\"}".getBytes()));

        DummyMetaSchema meta = new DummyMetaSchema("https://example.com/draft/schema#");
        JsonSchemaFactory factory = buildFactory(meta,
                (s) -> URI.create(s),
                fetcher,
                true); // enable cache

        URI schemaUri = new URI("test://my-schema.json");

        SchemaValidatorsConfig cfg1 = new SchemaValidatorsConfig();
        cfg1.setFailFast(true);
        JsonSchema schema1 = factory.getSchema(schemaUri, cfg1);

        // second call with a different config – should return the cached instance
        SchemaValidatorsConfig cfg2 = new SchemaValidatorsConfig();
        cfg2.setFailFast(false);
        JsonSchema schema2 = factory.getSchema(schemaUri, cfg2);

        assertSame("Cache should return the same JsonSchema instance", schema1, schema2);
        // The config of the cached schema must have been refreshed.
        assertEquals(false, schema2.getValidationContext().getConfig().isFailFast());
    }

    /** --------------------------------------------------------------------- *
     *  9. fromId – unknown dialect throws JsonSchemaException.
     *  --------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void expert9_fromId_unknownDialect_throws() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(Version4.SCHEMA_URI)
                .addMetaSchema(new DummyMetaSchema(Version4.SCHEMA_URI))
                .build();

        // Access private method.
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("fromId", String.class);
        m.setAccessible(true);
        m.invoke(factory, "http://unknown-dialect.org/schema#");
    }

    /** --------------------------------------------------------------------- *
     *  10. getUriTranslator – ensures that the map supplied via addUriMappings
     *      is honoured.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert10_getUriTranslator_appliesMappings() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("http://old.com/schema", "https://new.com/schema");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(Version4.SCHEMA_URI)
                .addMetaSchema(new DummyMetaSchema(Version4.SCHEMA_URI))
                .addUriMappings(map)
                .build();

        URITranslator translator = factory.getUriTranslator();

        URI original = new URI("http://old.com/schema");
        URI translated = translator.translate(original);
        assertEquals("https://new.com/schema", translated.toString());
    }

    /** --------------------------------------------------------------------- *
     *  11. Builder cloning – builder(blueprint) creates a builder that reproduces
     *      the original configuration.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert11_builderClone_retainsConfiguration() {
        DummyMetaSchema meta = new DummyMetaSchema("https://example.com/draft/schema#");
        JsonSchemaFactory original = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(meta.getUri())
                .addMetaSchema(meta)
                .enableUriSchemaCache(false)
                .build();

        JsonSchemaFactory.Builder clonedBuilder = JsonSchemaFactory.builder(original);
        JsonSchemaFactory cloned = clonedBuilder.build();

        // Reflection to compare a few private fields.
        try {
            java.lang.reflect.Field fDefault = JsonSchemaFactory.class.getDeclaredField("defaultMetaSchemaURI");
            fDefault.setAccessible(true);
            String origDef = (String) fDefault.get(original);
            String cloneDef = (String) fDefault.get(cloned);
            assertEquals(origDef, cloneDef);

            java.lang.reflect.Field fCache = JsonSchemaFactory.class.getDeclaredField("enableUriSchemaCache");
            fCache.setAccessible(true);
            assertEquals(fCache.get(original), fCache.get(cloned));
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    /** --------------------------------------------------------------------- *
     *  12. createValidationContext – should return a ValidationContext that
     *      contains the meta‑schema resolved from the $schema keyword.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert12_createValidationContext_respectsSchemaKeyword() throws Exception {
        DummyMetaSchema metaV4 = new DummyMetaSchema(Version4.SCHEMA_URI);
        DummyMetaSchema metaV7 = new DummyMetaSchema(Version7.SCHEMA_URI);

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(metaV4.getUri())
                .addMetaSchema(metaV4)
                .addMetaSchema(metaV7)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree("{\"$schema\":\"" + Version7.SCHEMA_URI + "\"}");

        // Use reflection to call the protected method.
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("createValidationContext", JsonNode.class);
        m.setAccessible(true);
        ValidationContext ctx = (ValidationContext) m.invoke(factory, schemaNode);
        assertNotNull(ctx);
        assertEquals(Version7.SCHEMA_URI, ctx.getMetaSchema().getUri());
    }

    /** --------------------------------------------------------------------- *
     *  13. findMetaSchemaForSchema – when $schema is missing it must fall back
     *      to the default meta‑schema.
     *  --------------------------------------------------------------------- */
    @Test
    public void expert13_findMetaSchemaForSchema_missingSchemaKeyword_usesDefault()
            throws Exception {
        DummyMetaSchema defaultMeta = new DummyMetaSchema("https://example.com/default#");
        JsonSchemaFactory factory = buildFactory(defaultMeta,
                (s) -> URI.create(s),
                (u) -> new ByteArrayInputStream("{\"type\":\"string\"}".getBytes()),
                false);

        // Access the private method.
        java.lang.reflect.Method m = JsonSchemaFactory.class
                .getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        m.setAccessible(true);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        JsonMetaSchema resolved = (JsonMetaSchema) m.invoke(factory, node);
        assertEquals(defaultMeta.getUri(), resolved.getUri());
    }
}
```
###Test END##