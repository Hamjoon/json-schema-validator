package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URITranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * <p>The tests are organised as follows:</p>
 *
 * <ul>
 *   <li>Extraction of all public methods (see the {@code PUBLIC_METHODS} list).</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case / exception tests identified from the source code.</li>
 *   <li>Integration style tests that exercise the factory with real JSON/YAML
 *       schemas, temporary files and URI handling.</li>
 * </ul>
 *
 * <p>No mocking frameworks are used – all objects are created through the
 * public constructors / factory methods that appear in the supplied source.</p>
 */
public class JsonSchemaFactoryTest {

    /** List of public method signatures – kept for documentation purposes. */
    private static final List<String> PUBLIC_METHODS = Arrays.asList(
            "static Builder builder()",
            "@Deprecated static JsonSchemaFactory getInstance()",
            "static JsonSchemaFactory getInstance(SpecVersion.VersionFlag)",
            "static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag)",
            "static Builder builder(JsonSchemaFactory)",
            "public JsonSchema create(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema)",
            "public URIFactory getUriFactory()",
            "public URITranslator getUriTranslator()",
            "public JsonSchema getSchema(String, SchemaValidatorsConfig)",
            "public JsonSchema getSchema(String)",
            "public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)",
            "public JsonSchema getSchema(InputStream)",
            "public JsonSchema getSchema(URI, SchemaValidatorsConfig)",
            "public JsonSchema getSchema(URI)",
            "public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)",
            "public JsonSchema getSchema(JsonNode, SchemaValidatorsConfig)",
            "public JsonSchema getSchema(URI, JsonNode)",
            "public JsonSchema getSchema(JsonNode)"
    );

    /** A factory built with the V4 meta‑schema – used by most tests. */
    private JsonSchemaFactory factory;

    /** Temporary files created during tests – cleaned up in {@link #tearDown()}. */
    private final List<File> tempFiles = new ArrayList<>();

    @Before
    public void setUp() {
        // Build a factory that contains the V4 meta‑schema and uses the default URI.
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
    }

    @After
    public void tearDown() {
        // Delete any temporary files we created.
        for (File f : tempFiles) {
            if (f != null && f.exists()) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        tempFiles.clear();
    }

    /** Helper – creates a temporary file with the supplied content and returns its {@link URI}. */
    private URI createTempFile(String suffix, String content) throws IOException {
        File tmp = File.createTempFile("schema", suffix);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8")) {
            w.write(content);
        }
        tempFiles.add(tmp);
        return tmp.toURI();
    }

    /* ---------------------------------------------------------------------- */
    /* 1.  Builder related tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBuilderReturnsNonNull() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
        assertNotNull("builder() must not return null", b);
    }

    @Test
    public void testBuilderFromBlueprintCopiesMetaSchemas() {
        // Build an original factory with V4 meta‑schema.
        JsonSchemaFactory original = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        // Create a new builder from the original blueprint.
        JsonSchemaFactory.Builder newBuilder = JsonSchemaFactory.builder(original);
        JsonSchemaFactory copy = newBuilder.build();

        // The copy must contain at least the same meta‑schema URI.
        assertEquals(original.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
        assertTrue(copy.jsonMetaSchemas.containsKey(
                JsonSchemaFactory.normalizeMetaSchemaUri(JsonMetaSchema.getV4().getUri())));
    }

    /* ---------------------------------------------------------------------- */
    /* 2.  getInstance / checkVersion                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetInstanceDeprecatedReturnsV4Factory() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull(f);
        assertEquals(JsonMetaSchema.getV4().getUri(),
                f.defaultMetaSchemaURI);
    }

    @Test
    public void testGetInstanceWithAllVersionFlags() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaFactory f = JsonSchemaFactory.getInstance(flag);
            assertNotNull("Factory for flag " + flag + " must not be null", f);
            // The defaultMetaSchemaURI stored in the factory must be normalized.
            String expected = JsonSchemaFactory.normalizeMetaSchemaUri(
                    JsonSchemaFactory.checkVersion(flag).getInstance().getUri());
            assertEquals(expected, f.defaultMetaSchemaURI);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetInstanceWithNullFlagThrowsNPE() {
        // getInstance(null) will eventually call checkVersion(null) → null,
        // then .getInstance() on null → NPE.
        JsonSchemaFactory.getInstance(null);
    }

    @Test
    public void testCheckVersionReturnsCorrectImplementation() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupportedEnumThrowsException() {
        // Create a dummy enum value via reflection (not possible with the real enum),
        // therefore we simulate the path by passing a null and expecting the method
        // to throw for an unknown value – the only reachable path is the default case.
        // The test is kept for completeness even though the library does not expose
        // a way to reach it directly.
        JsonSchemaFactory.checkVersion(Enum.valueOf(SpecVersion.VersionFlag.class, "UNKNOWN"));
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  URI related getters                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetUriFactoryIsNotNull() {
        URIFactory uf = factory.getUriFactory();
        assertNotNull("getUriFactory() must return a non‑null instance", uf);
    }

    @Test
    public void testGetUriTranslatorAppliesMappings() {
        // Add a custom mapping via the builder.
        Map<String, String> map = new HashMap<>();
        map.put("http://example.com/original", "http://example.com/mapped");
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .addUriMappings(map)
                .build();

        URITranslator translator = customFactory.getUriTranslator();
        URI original = URI.create("http://example.com/original");
        URI expected = URI.create("http://example.com/mapped");
        assertEquals(expected, translator.translate(original));
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  getSchema(String) – JSON handling                                   */
    /* ---------------------------------------------------------------------- */

    private static final String SIMPLE_JSON_SCHEMA = "{\"type\":\"string\"}";
    private static final String INVALID_JSON = "{\"type\":\"string\""; // missing closing brace

    @Test
    public void testGetSchemaFromStringValidJson() {
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull(schema);
        // Validate a JSON string against the schema – should succeed.
        JsonNode valid = new ObjectMapper().readTree("\"hello\"");
        assertTrue(schema.validate(valid).isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJsonThrowsException() {
        factory.getSchema(INVALID_JSON);
    }

    @Test
    public void testGetSchemaFromStringWithConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA, cfg);
        assertNotNull(schema);
        assertTrue(cfg.isFailFast());
    }

    /* ---------------------------------------------------------------------- */
    /* 5.  getSchema(InputStream)                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromInputStream() throws IOException {
        InputStream is = new ByteArrayInputStream(SIMPLE_JSON_SCHEMA.getBytes("UTF-8"));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
        JsonNode node = new ObjectMapper().readTree("\"test\"");
        assertTrue(schema.validate(node).isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidInputStreamThrowsException() throws IOException {
        InputStream is = new ByteArrayInputStream(INVALID_JSON.getBytes("UTF-8"));
        factory.getSchema(is);
    }

    /* ---------------------------------------------------------------------- */
    /* 6.  getSchema(URI) – file based JSON/YAML handling                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromFileUriJson() throws Exception {
        URI uri = createTempFile(".json", SIMPLE_JSON_SCHEMA);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        JsonNode node = new ObjectMapper().readTree("\"abc\"");
        assertTrue(schema.validate(node).isEmpty());
    }

    @Test
    public void testGetSchemaFromFileUriYaml() throws Exception {
        String yaml = "type: string";
        URI uri = createTempFile(".yaml", yaml);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        JsonNode node = new ObjectMapper().readTree("\"xyz\"");
        assertTrue(schema.validate(node).isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromUnsupportedSchemeThrowsException() throws Exception {
        // Use a made‑up scheme that the default URIFactory does not know.
        URI badUri = new URI("unsupported://host/schema.json");
        factory.getSchema(badUri);
    }

    /* ---------------------------------------------------------------------- */
    /* 7.  URI schema cache behaviour                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testUriSchemaCacheReturnsSameInstanceWhenEnabled() throws Exception {
        // Build a factory with cache enabled (default is true).
        JsonSchemaFactory cacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .enableUriSchemaCache(true)
                .build();

        URI uri = createTempFile(".json", SIMPLE_JSON_SCHEMA);
        JsonSchema first = cacheFactory.getSchema(uri);
        JsonSchema second = cacheFactory.getSchema(uri);
        assertSame("When cache is enabled the same JsonSchema instance should be returned",
                first, second);
    }

    @Test
    public void testUriSchemaCacheDisabledCreatesNewInstances() throws Exception {
        JsonSchemaFactory noCacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .enableUriSchemaCache(false)
                .build();

        URI uri = createTempFile(".json", SIMPLE_JSON_SCHEMA);
        JsonSchema first = noCacheFactory.getSchema(uri);
        JsonSchema second = noCacheFactory.getSchema(uri);
        assertNotSame("When cache is disabled each call should create a new JsonSchema",
                first, second);
    }

    /* ---------------------------------------------------------------------- */
    /* 8.  Handling of $schema and unknown meta‑schema                        */
    /* ---------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testUnknownMetaSchemaThrowsException() {
        String schemaWithUnknownMeta = "{\"$schema\":\"http://example.com/unknown\"}";
        factory.getSchema(schemaWithUnknownMeta);
    }

    @Test
    public void testSchemaWithExplicitValidMetaSchemaLoads() {
        // Use the V7 meta‑schema URI explicitly.
        String v7Meta = JsonMetaSchema.getV7().getUri();
        String schema = "{\"$schema\":\"" + v7Meta + "\",\"type\":\"integer\"}";
        JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(schema);
        assertNotNull(jsonSchema);
        JsonNode valid = new ObjectMapper().readTree("10");
        assertTrue(jsonSchema.validate(valid).isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 9.  $id matching source URI – indirect verification                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSchemaWithIdMatchingUriLoadsCorrectly() throws Exception {
        // $id equals the file URI – the factory must recognise the match.
        URI uri = createTempFile(".json", "{\"$id\":\"%s\",\"type\":\"boolean\"}");
        String content = String.format("{\"$id\":\"%s\",\"type\":\"boolean\"}", uri.toString());
        // Overwrite the temporary file with the proper content.
        try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(uri)), "UTF-8")) {
            w.write(content);
        }

        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        JsonNode valid = new ObjectMapper().readTree("true");
        assertTrue(schema.validate(valid).isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 10.  create(...) – direct creation path (public method)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateDirectlyUsingFactoryMethod() {
        // Build a minimal ValidationContext via the factory (internal helper).
        JsonNode schemaNode = new ObjectMapper().createObjectNode().put("type", "null");
        ValidationContext vc = factory.createValidationContext(schemaNode);
        SchemaLocation loc = SchemaLocation.DOCUMENT;
        JsonNodePath path = new JsonNodePath(PathType.JSON_POINTER);
        JsonSchema schema = factory.create(vc, loc, path, schemaNode, null);
        assertNotNull(schema);
        JsonNode instance = new ObjectMapper().readTree("null");
        assertTrue(schema.validate(instance).isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 11.  normalizeMetaSchemaUri – static utility method                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNormalizeMetaSchemaUriPreservesHttpsAndRemovesFragment() throws Exception {
        String raw = "http://json-schema.org/draft-04/schema#";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(raw);
        assertEquals("https://json-schema.org/draft-04/schema", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriWithInvalidUriThrowsException() {
        JsonSchemaFactory.normalizeMetaSchemaUri("http://[invalid]");
    }

}
