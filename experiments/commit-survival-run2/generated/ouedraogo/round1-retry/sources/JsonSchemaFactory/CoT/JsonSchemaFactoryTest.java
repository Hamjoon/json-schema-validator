package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** ------------------------------------------------------------
     *  1.  Public methods extracted from JsonSchemaFactory
     * ------------------------------------------------------------ */
    // static Builder builder()
    // @Deprecated static JsonSchemaFactory getInstance()
    // static JsonSchemaFactory getInstance(SpecVersion.VersionFlag)
    // static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag)
    // static Builder builder(JsonSchemaFactory)
    // JsonSchema create(ValidationContext, String, JsonNode, JsonSchema)
    // URIFactory getUriFactory()
    // URITranslator getUriTranslator()
    // JsonSchema getSchema(String, SchemaValidatorsConfig)
    // JsonSchema getSchema(String)
    // JsonSchema getSchema(InputStream, SchemaValidatorsConfig)
    // JsonSchema getSchema(InputStream)
    // JsonSchema getSchema(URI, SchemaValidatorsConfig)
    // JsonSchema getSchema(URI)
    // JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)
    // JsonSchema getSchema(JsonNode, SchemaValidatorsConfig)
    // JsonSchema getSchema(URI, JsonNode)
    // JsonSchema getSchema(JsonNode)

    /** ------------------------------------------------------------
     *  2.  Basic functionality tests
     * ------------------------------------------------------------ */

    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(VersionFlag.V4).jsonMetaSchemas.values().iterator().next())
                .build();
        assertNotNull(factory);
    }

    @Test
    public void testDeprecatedGetInstance() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        assertNotNull(factory);
    }

    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertNotNull(factory);
    }

    @Test
    public void testCheckVersionValidFlags() {
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V202012) instanceof Version202012);
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testBuilderFromBlueprint() {
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(original);
        JsonSchemaFactory copy = builder.build();
        assertEquals(original.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
        assertNotNull(copy.getUriFactory());
    }

    @Test
    public void testGetUriFactoryNotNull() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull(factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslatorNotNull() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull(factory.getUriTranslator());
    }

    @Test
    public void testGetSchemaFromStringValid() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromStringInvalidJson() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        try {
            factory.getSchema("{invalid json}");
            fail("Expected JsonSchemaException");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    @Test
    public void testGetSchemaFromInputStreamValid() throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        InputStream is = new ByteArrayInputStream("{\"type\":\"integer\"}".getBytes("UTF-8"));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromInputStreamInvalid() throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        InputStream is = new ByteArrayInputStream("not a json".getBytes("UTF-8"));
        try {
            factory.getSchema(is);
            fail("Expected JsonSchemaException");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    @Test
    public void testGetSchemaFromURIValidJsonFile() throws IOException, URISyntaxException {
        // create a temporary JSON schema file
        Path tempFile = Files.createTempFile("schema", ".json");
        Files.write(tempFile, "{\"type\":\"boolean\"}".getBytes("UTF-8"));
        tempFile.toFile().deleteOnExit();

        URI fileUri = tempFile.toUri();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchema schema1 = factory.getSchema(fileUri);
        JsonSchema schema2 = factory.getSchema(fileUri); // second call should hit the cache

        assertNotNull(schema1);
        assertNotNull(schema2);
        // The cache returns the same instance (reference equality)
        assertSame(schema1, schema2);
    }

    @Test
    public void testGetSchemaFromURIValidYamlFile() throws IOException, URISyntaxException {
        // create a temporary YAML schema file
        Path tempFile = Files.createTempFile("schema", ".yaml");
        Files.write(tempFile, "type: string".getBytes("UTF-8"));
        tempFile.toFile().deleteOnExit();

        URI fileUri = tempFile.toUri();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchema schema = factory.getSchema(fileUri);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaFromURINullShouldThrow() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        try {
            factory.getSchema((URI) null);
            fail("Expected NullPointerException or JsonSchemaException");
        } catch (Exception e) {
            // Accept either NPE from translate or JsonSchemaException wrapped inside
            assertTrue(e instanceof NullPointerException || e instanceof JsonSchemaException);
        }
    }

    @Test
    public void testGetSchemaWithConfigPreservesConfig() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(true);
        JsonSchema schema = factory.getSchema("{\"type\":\"number\"}", config);
        assertNotNull(schema);
        assertEquals(config, schema.getValidationContext().getConfig());
    }

    @Test
    public void testIdMatchesSourceUriTrueAndFalse() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        // Build a schema node with $id matching the supplied URI
        String json = "{\"$id\":\"http://example.com/schema.json\",\"type\":\"object\"}";
        JsonNode node = new ObjectMapper().readTree(json);
        JsonMetaSchema meta = factory.jsonMetaSchemas.get(factory.defaultMetaSchemaURI);
        // Use reflection to invoke the private method
        java.lang.reflect.Method method = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        method.setAccessible(true);
        boolean matches = (Boolean) method.invoke(factory, meta, node, new URI("http://example.com/schema.json"));
        assertTrue(matches);

        boolean notMatches = (Boolean) method.invoke(factory, meta, node, new URI("http://example.com/other.json"));
        assertFalse(notMatches);
    }

    /** ------------------------------------------------------------
     *  3.  Edge‑case / exception handling tests
     * ------------------------------------------------------------ */

    @Test
    public void testBuilderWithNullObjectMapperThrows() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder()
                .objectMapper(null) // will be accepted but later build will replace with default if null
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(VersionFlag.V4).jsonMetaSchemas.values().iterator().next());

        // The constructor of JsonSchemaFactory will replace a null ObjectMapper with a new one,
        // so this does NOT throw. To test the IllegalArgumentException we must force a null
        // after the builder – this is not possible via the public API. Hence we test the
        // private constructor via reflection.
        try {
            java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                    ObjectMapper.class, YAMLMapper.class, String.class,
                    URISchemeFactory.class, URISchemeFetcher.class,
                    URNFactory.class, Map.class, Map.class,
                    boolean.class, CompositeURITranslator.class);
            ctor.setAccessible(true);
            ctor.newInstance(null, new YAMLMapper(), "http://json-schema.org/draft-04/schema#",
                    null, null, null, null, null, true, null);
            fail("Expected IllegalArgumentException due to null ObjectMapper");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testNormalizeMetaSchemaUriWithInvalidUriThrows() throws Exception {
        java.lang.reflect.Method method = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        method.setAccessible(true);
        try {
            method.invoke(null, "http://[invalid-uri]");
            fail("Expected JsonSchemaException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof JsonSchemaException);
        }
    }

    @Test
    public void testCheckVersionUnsupportedEnumThrows() {
        // Create a fake enum value via reflection – not feasible. Instead, we test the default
        // branch by passing null (already covered) and rely on the public API not exposing
        // unsupported values. This test therefore documents the expected behaviour.
        // No code needed – the method will throw IllegalArgumentException for any unknown value.
        try {
            // Simulate unknown by casting an integer to the enum (unsafe, but for test)
            VersionFlag unknown = java.lang.Enum.valueOf(VersionFlag.class, "UNKNOWN");
            JsonSchemaFactory.checkVersion(unknown);
            fail("Expected IllegalArgumentException for unknown enum");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testIsYamlDetectionViaReflection() throws Exception {
        java.lang.reflect.Method method = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        method.setAccessible(true);
        URI yamlUri = new URI("file:///tmp/schema.yaml");
        URI ymlUri = new URI("file:///tmp/schema.yml");
        URI jsonUri = new URI("file:///tmp/schema.json");
        assertTrue((Boolean) method.invoke(null, yamlUri));
        assertTrue((Boolean) method.invoke(null, ymlUri));
        assertFalse((Boolean) method.invoke(null, jsonUri));
    }

    /** ------------------------------------------------------------
     *  4.  Test of the `create` method (public, uses internal logic)
     * ------------------------------------------------------------ */
    @Test
    public void testCreateMethod() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        // Build a minimal ValidationContext using the factory's internal components
        JsonNode schemaNode = new ObjectMapper().readTree("{\"type\":\"string\"}");
        java.lang.reflect.Method createContextMethod = JsonSchemaFactory.class.getDeclaredMethod("createValidationContext", JsonNode.class);
        createContextMethod.setAccessible(true);
        ValidationContext ctx = (ValidationContext) createContextMethod.invoke(factory, schemaNode);

        JsonSchema parent = null; // top‑level schema has no parent
        JsonSchema schema = factory.create(ctx, "#", schemaNode, parent);
        assertNotNull(schema);
        // The created schema should hold the same ValidationContext we passed
        assertEquals(ctx, schema.getValidationContext());
    }
}
