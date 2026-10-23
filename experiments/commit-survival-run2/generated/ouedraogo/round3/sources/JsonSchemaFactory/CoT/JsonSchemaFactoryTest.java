package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.uri.URITranslator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public method signatures.</li>
 *   <li>Normal behaviour (happy path).</li>
 *   <li>Edge‑cases and expected exception handling.</li>
 *   <li>Cache behaviour for {@code getSchema(URI,…)}.</li>
 * </ul>
 */
public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factory;

    /** Temporary file used for URI‑based schema loading tests. */
    private File tempJsonFile;
    private File tempYamlFile;

    @Before
    public void setUp() throws IOException {
        // Obtain a ready‑to‑use factory (draft‑v4 is the most common default).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull("Factory must be created", factory);

        // Create a simple JSON schema file on the file‑system.
        tempJsonFile = File.createTempFile("simple", ".json");
        try (Writer w = new FileWriter(tempJsonFile)) {
            w.write("{\"type\":\"object\"}");
        }

        // Create a simple YAML schema file (used to test YAML detection).
        tempYamlFile = File.createTempFile("simple", ".yaml");
        try (Writer w = new FileWriter(tempYamlFile)) {
            w.write("type: object");
        }
    }

    @After
    public void tearDown() {
        if (tempJsonFile != null && tempJsonFile.exists()) {
            tempJsonFile.delete();
        }
        if (tempYamlFile != null && tempYamlFile.exists()) {
            tempYamlFile.delete();
        }
    }

    /*=====================================================================
     *  1.  Public static factory methods
     *====================================================================*/

    @Test
    public void testBuilderWithoutArguments() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
        assertNotNull("builder() must not return null", b);
    }

    @Test
    @Deprecated
    public void testDeprecatedGetInstance() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull("Deprecated getInstance() must return a factory", f);
    }

    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull("Factory for V7 must be created", f);
    }

    @Test(expected = NullPointerException.class)
    public void testGetInstanceWithNullVersionFlag() {
        // The implementation calls jsonSchemaVersion.getInstance() without a null‑check,
        // therefore a NPE is expected.
        JsonSchemaFactory.getInstance((SpecVersion.VersionFlag) null);
    }

    @Test
    public void testCheckVersionValidValues() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void testCheckVersionNull() {
        assertNull("checkVersion(null) must return null", JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testBuilderFromBlueprint() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(factory);
        assertNotNull("builder(factory) must not be null", b);
    }

    /*=====================================================================
     *  2.  Instance methods – basic behaviour
     *====================================================================*/

    @Test
    public void testGetUriFactory() {
        URIFactory uriFactory = factory.getUriFactory();
        assertNotNull("getUriFactory() must return a non‑null factory", uriFactory);
    }

    @Test
    public void testGetUriTranslator() {
        URITranslator translator = factory.getUriTranslator();
        assertNotNull("getUriTranslator() must return a non‑null translator", translator);
    }

    @Test
    public void testGetSchemaFromString() {
        String schema = "{\"type\":\"string\"}";
        JsonSchema js = factory.getSchema(schema);
        assertNotNull("Schema parsed from a valid JSON string must not be null", js);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJson() {
        // malformed JSON should result in JsonSchemaException
        factory.getSchema("{invalid json}");
    }

    @Test
    public void testGetSchemaFromStringWithConfig() {
        String schema = "{\"type\":\"number\"}";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchema js = factory.getSchema(schema, cfg);
        assertNotNull(js);
        // Verify that the config is attached to the validation context
        assertEquals(Boolean.TRUE, js.getValidationContext().getConfig().isFailFast());
    }

    @Test
    public void testGetSchemaFromInputStream() throws IOException {
        String json = "{\"type\":\"boolean\"}";
        try (InputStream is = new ByteArrayInputStream(json.getBytes())) {
            JsonSchema js = factory.getSchema(is);
            assertNotNull(js);
        }
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalidJson() throws IOException {
        try (InputStream is = new ByteArrayInputStream("not a json".getBytes())) {
            factory.getSchema(is);
        }
    }

    @Test
    public void testGetSchemaFromURI_JSONFile() throws URISyntaxException {
        URI uri = tempJsonFile.toURI();
        JsonSchema js = factory.getSchema(uri);
        assertNotNull("Loading a schema from a JSON file must succeed", js);
    }

    @Test
    public void testGetSchemaFromURI_YAMLFile() throws URISyntaxException {
        URI uri = tempYamlFile.toURI();
        JsonSchema js = factory.getSchema(uri);
        assertNotNull("Loading a schema from a YAML file must succeed", js);
    }

    @Test
    public void testGetSchemaFromURI_CacheBehaviour() throws URISyntaxException {
        URI uri = tempJsonFile.toURI();

        // First call – schema is loaded and cached.
        JsonSchema first = factory.getSchema(uri);
        assertNotNull(first);

        // Second call – should retrieve the same instance from the cache.
        JsonSchema second = factory.getSchema(uri);
        assertSame("When cache is enabled the same instance should be returned", first, second);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromURI_UnsupportedScheme() throws URISyntaxException {
        // Use a made‑up scheme that the default URIFactory does not understand.
        URI badUri = new URI("unsupported://some/path/schema.json");
        factory.getSchema(badUri);
    }

    @Test
    public void testGetSchemaFromURI_WithConfig() throws URISyntaxException {
        URI uri = tempJsonFile.toURI();
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchema js = factory.getSchema(uri, cfg);
        assertNotNull(js);
        assertEquals(Boolean.TRUE, js.getValidationContext().getConfig().isFailFast());
    }

    @Test
    public void testGetSchemaFromURINullConfig() throws URISyntaxException {
        // The overload that supplies a default config must still work.
        URI uri = tempJsonFile.toURI();
        JsonSchema js = factory.getSchema(uri);
        assertNotNull(js);
        assertNotNull(js.getValidationContext().getConfig());
    }

    @Test
    public void testGetSchemaFromURI_JsonNodeAndConfig() {
        // Directly feed a JsonNode – the method should just delegate to newJsonSchema().
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createObjectNode().put("type", "null");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema js = factory.getSchema((URI) null, node, cfg);
        assertNotNull(js);
        assertEquals(node, js.getSchemaNode());
    }

    @Test
    public void testGetSchemaFromJsonNodeOnly() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.createObjectNode().put("type", "array");
        JsonSchema js = factory.getSchema(node);
        assertNotNull(js);
        assertEquals(node, js.getSchemaNode());
    }

    @Test
    public void testCreateMethod() {
        // Build a minimal validation context using the factory's internal helpers.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.createObjectNode().put("type", "string");
        ValidationContext ctx = factory.createValidationContext(schemaNode);
        JsonSchema parent = factory.getSchema("{\"type\":\"string\"}");
        JsonSchema child = factory.create(ctx, "#/properties/name", schemaNode, parent);
        assertNotNull(child);
        assertEquals(parent.getCurrentUri(), child.getParentSchema().getCurrentUri());
    }

    /*=====================================================================
     *  3.  Edge cases & exception handling
     *====================================================================*/

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullObjectMapper() {
        // The Builder will replace a null ObjectMapper with a new instance,
        // but passing null explicitly to the constructor of JsonSchemaFactory
        // (via Builder) should trigger the guard clause.
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder()
                .objectMapper(null)
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema")
                .addMetaSchema(new Version4().getInstance());
        b.build(); // should throw IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNullYamlMapper() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder()
                .yamlMapper(null)
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema")
                .addMetaSchema(new Version4().getInstance());
        b.build(); // guard clause in constructor
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderEmptyDefaultMetaSchemaURI() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("   "); // blank string
        b.build(); // should fail because defaultMetaSchemaURI is empty
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderMissingMetaSchemaForDefaultURI() {
        // Provide a default URI that does not correspond to any added meta‑schema.
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://unknown/meta/schema")
                .addMetaSchema(new Version4().getInstance()); // adds only V4
        b.build(); // throws because defaultMetaSchemaURI cannot be resolved
    }

    @Test
    public void testNormalizeMetaSchemaUriValid() {
        // Use reflection to access the protected static method.
        String input = "http://example.com:8080/path?query=1#frag";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals("https://example.com:8080/path", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalid() {
        JsonSchemaFactory.normalizeMetaSchemaUri(":// malformed uri");
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromString_NullArgument() {
        factory.getSchema((String) null);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStream_NullArgument() throws IOException {
        factory.getSchema((InputStream) null);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromURI_NullArgument() throws URISyntaxException {
        factory.getSchema((URI) null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetSchemaFromJsonNode_NullNode() {
        factory.getSchema((JsonNode) null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetSchemaFromJsonNodeAndConfig_NullNode() {
        factory.getSchema((JsonNode) null, new SchemaValidatorsConfig());
    }

    @Test
    public void testAddUriMappingDeprecated() {
        // The deprecated method should still store mappings.
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema")
                .addMetaSchema(new Version4().getInstance())
                .addUriMappings(Collections.singletonMap("http://old.com", "http://new.com"));
        JsonSchemaFactory f = b.build();
        // The internal map is not publicly exposed, but we can verify that the
        // translator incorporates the mapping.
        URITranslator translator = f.getUriTranslator();
        URI original = URI.create("http://old.com/some/schema.json");
        URI translated = translator.translate(original);
        assertEquals("http://new.com/some/schema.json", translated.toString());
    }

    @Test
    public void testEnableUriSchemaCacheToggle() {
        // Build a factory with cache disabled and ensure a new instance is returned each time.
        JsonSchemaFactory noCacheFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema")
                .addMetaSchema(new Version4().getInstance())
                .enableUriSchemaCache(false)
                .build();

        URI uri = tempJsonFile.toURI();
        JsonSchema first = noCacheFactory.getSchema(uri);
        JsonSchema second = noCacheFactory.getSchema(uri);
        assertNotSame("When cache is disabled, distinct instances must be returned", first, second);
    }

    /*=====================================================================
     *  Helper – expose the protected createValidationContext for testing
     *====================================================================*/
    private ValidationContext createValidationContext(JsonNode node) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class
                    .getDeclaredMethod("createValidationContext", JsonNode.class);
            m.setAccessible(true);
            return (ValidationContext) m.invoke(factory, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
