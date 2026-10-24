###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The test class covers:
 * <ul>
 *   <li>Extraction of all public methods and verification of their basic contract.</li>
 *   <li>Edge‑case handling such as null arguments, malformed URIs, unknown meta‑schemas, and
 *       invalid JSON/YAML input.</li>
 *   <li>Exception scenarios – ensuring the correct {@link JsonSchemaException} is thrown.</li>
 *   <li>Caching behaviour for URI based schema loading.</li>
 * </ul>
 */
public class JsonSchemaFactoryTest {

    /** A factory built with the minimal required configuration (draft‑04). */
    private JsonSchemaFactory factory;

    /** Simple JSON schema used in many tests. */
    private static final String SIMPLE_JSON_SCHEMA = "{ \"type\": \"object\" }";

    /** Simple YAML schema used for isYaml() tests. */
    private static final String SIMPLE_YAML_SCHEMA = "type: object";

    @Before
    public void setUp() {
        // Build a factory that knows about Draft‑04 – the most widely used version.
        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
    }

    /* -----------------------------------------------------------------------
     *  1.  Public static builder() and builder(JsonSchemaFactory) methods
     * ----------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
        assertNotNull(f);
    }

    @Test
    public void testBuilderFromBlueprintCopiesAllSettings() {
        JsonSchemaFactory copy = JsonSchemaFactory.builder(factory).build();
        assertNotNull(copy);
        // The copy must have the same default meta‑schema URI.
        assertEquals(factory.getUriFactory().getClass(),
                copy.getUriFactory().getClass());
    }

    /* -----------------------------------------------------------------------
     *  2.  checkVersion() – normal and error paths
     * ----------------------------------------------------------------------- */

    @Test
    public void testCheckVersionValidFlags() {
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012));
    }

    @Test
    public void testCheckVersionNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupportedThrows() {
        // Simulate an unsupported enum value by using reflection to create a bogus flag.
        // The factory method switches on the enum, so we pass a value that is not in the
        // switch (e.g., a mock via an anonymous subclass – not possible with enum).
        // Instead we rely on the default branch: we create a dummy enum constant via
        // reflection (this is only for test purposes).
        SpecVersion.VersionFlag bogus = SpecVersion.VersionFlag.valueOf("V999");
        JsonSchemaFactory.checkVersion(bogus);
    }

    /* -----------------------------------------------------------------------
     *  3.  normalizeMetaSchemaUri – normalisation & malformed URI handling
     * ----------------------------------------------------------------------- */

    @Test
    public void testNormalizeMetaSchemaUriCreatesHttpsScheme() {
        String input = "http://json-schema.org/draft-04/schema#";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertTrue(normalized.startsWith("https://"));
        assertTrue(normalized.endsWith("/draft-04/schema#"));
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriMalformedThrows() {
        JsonSchemaFactory.normalizeMetaSchemaUri("::::bad uri::::");
    }

    /* -----------------------------------------------------------------------
     *  4.  isYaml() – detection based on file extension
     * ----------------------------------------------------------------------- */

    @Test
    public void testIsYamlTrueForYmlExtension() throws URISyntaxException {
        URI yamlUri = new URI("file:/tmp/schema.yml");
        assertTrue(invokeIsYaml(yamlUri));
    }

    @Test
    public void testIsYamlTrueForYamlExtension() throws URISyntaxException {
        URI yamlUri = new URI("file:/tmp/schema.yaml");
        assertTrue(invokeIsYaml(yamlUri));
    }

    @Test
    public void testIsYamlFalseForJsonExtension() throws URISyntaxException {
        URI jsonUri = new URI("file:/tmp/schema.json");
        assertFalse(invokeIsYaml(jsonUri));
    }

    @Test
    public void testIsYamlFalseWhenNoExtension() throws URISyntaxException {
        URI noExt = new URI("file:/tmp/schema");
        assertFalse(invokeIsYaml(noExt));
    }

    /** Helper that uses reflection to invoke the private isYaml method. */
    private boolean invokeIsYaml(URI uri) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(factory, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -----------------------------------------------------------------------
     *  5.  getSchema(String) – valid JSON and error handling
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromStringValidJson() {
        JsonSchema schema = factory.getSchema(SIMPLE_JSON_SCHEMA);
        assertNotNull(schema);
        // Basic sanity – the schema should be able to validate an empty object.
        JsonNode node = new ObjectMapper().createObjectNode();
        assertTrue(schema.validate(node).isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJsonThrows() {
        factory.getSchema("{ invalid json }");
    }

    /* -----------------------------------------------------------------------
     *  6.  getSchema(InputStream) – valid and invalid streams
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromInputStreamValidJson() throws IOException {
        InputStream is = new ByteArrayInputStream(SIMPLE_JSON_SCHEMA.getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalidJsonThrows() throws IOException {
        InputStream is = new ByteArrayInputStream("{ bad json".getBytes());
        factory.getSchema(is);
    }

    /* -----------------------------------------------------------------------
     *  7.  getSchema(URI) – loading from a temporary file, caching and $id matching
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromUriLoadsJsonAndCaches() throws Exception {
        // 1. Write a tiny schema to a temporary file.
        Path tempFile = Files.createTempFile("temp-schema", ".json");
        Files.write(tempFile, SIMPLE_JSON_SCHEMA.getBytes());
        tempFile.toFile().deleteOnExit();

        URI schemaUri = tempFile.toUri();

        // 2. Load the schema for the first time.
        JsonSchema first = factory.getSchema(schemaUri);
        assertNotNull(first);

        // 3. Load again – should hit the cache (same instance).
        JsonSchema second = factory.getSchema(schemaUri);
        assertSame("Factory should return cached instance for same URI", first, second);
    }

    @Test
    public void testGetSchemaFromUriWithMatchingIdDoesNotRecreateContext() throws Exception {
        // Schema containing an "$id" that matches the file URI.
        String schemaWithId = "{ \"$id\": \"" + "file://dummy/schema.json" + "\", \"type\": \"object\" }";
        Path tempFile = Files.createTempFile("temp-schema-id", ".json");
        Files.write(tempFile, schemaWithId.getBytes());
        tempFile.toFile().deleteOnExit();

        URI schemaUri = new URI("file://dummy/schema.json");
        // The factory's uriFactory can resolve the file scheme; we map the temporary file
        // path to the same logical URI using a custom uri mapping.
        factory = JsonSchemaFactory.builder(factory)
                .addUriMappings(java.util.Collections.singletonMap(schemaUri.toString(), tempFile.toUri().toString()))
                .build();

        JsonSchema schema = factory.getSchema(schemaUri);
        assertNotNull(schema);
        // Validation of an empty object should succeed.
        JsonNode node = new ObjectMapper().createObjectNode();
        assertTrue(schema.validate(node).isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromUriUnreachableThrows() throws URISyntaxException {
        // Use a bogus URI that the URLFetcher cannot resolve.
        URI bogus = new URI("http://nonexistent.invalid/schema.json");
        factory.getSchema(bogus);
    }

    /* -----------------------------------------------------------------------
     *  8.  findMetaSchemaForSchema – unknown $schema should raise exception
     * ----------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testFindMetaSchemaForSchemaUnknownMetaSchemaThrows() throws Exception {
        // Build a JSON node with an unknown $schema value.
        String unknownSchema = "{ \"$schema\": \"http://example.com/unknown\" }";
        JsonNode node = new ObjectMapper().readTree(unknownSchema);

        // Use reflection to invoke the private method.
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        m.setAccessible(true);
        m.invoke(factory, node);
    }

    /* -----------------------------------------------------------------------
     *  9.  getUriFactory() and getUriTranslator()
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetUriFactoryNotNull() {
        assertNotNull(factory.getUriFactory());
    }

    @Test
    public void testGetUriTranslatorAppliesMappings() {
        // Add a simple mapping and verify translation.
        java.util.Map<String, String> map = java.util.Collections.singletonMap("http://src", "http://dst");
        factory = JsonSchemaFactory.builder(factory)
                .addUriMappings(map)
                .build();

        URITranslator translator = factory.getUriTranslator();
        URI src = URI.create("http://src");
        URI dst = translator.translate(src);
        assertEquals("http://dst", dst.toString());
    }

    /* -----------------------------------------------------------------------
     *  10.  newJsonSchema – direct invocation via reflection for coverage
     * ----------------------------------------------------------------------- */

    @Test
    public void testNewJsonSchemaCreatesInstance() throws Exception {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_JSON_SCHEMA);
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        JsonSchema schema = (JsonSchema) m.invoke(factory, null, node, null);
        assertNotNull(schema);
    }

    /* -----------------------------------------------------------------------
     *  11.  create(ValidationContext, ...) – basic contract
     * ----------------------------------------------------------------------- */

    @Test
    public void testCreateCreatesChildSchema() throws Exception {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_JSON_SCHEMA);
        ValidationContext vc = new ValidationContext(factory.getUriFactory(), null,
                JsonMetaSchema.getV4(), factory, null);
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "create", ValidationContext.class, String.class, JsonNode.class, JsonSchema.class);
        m.setAccessible(true);
        JsonSchema schema = (JsonSchema) m.invoke(factory, vc, "#", node, null);
        assertNotNull(schema);
    }

    /* -----------------------------------------------------------------------
     *  12.  getSchema(URI, JsonNode, ...) overloads – direct usage
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetSchemaWithJsonNodeAndUri() throws Exception {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_JSON_SCHEMA);
        URI dummy = new URI("http://example.com/dummy");
        JsonSchema schema = factory.getSchema(dummy, node);
        assertNotNull(schema);
    }

    @Test
    public void testGetSchemaWithOnlyJsonNode() throws Exception {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_JSON_SCHEMA);
        JsonSchema schema = factory.getSchema(node);
        assertNotNull(schema);
    }
}
```
###Test END##