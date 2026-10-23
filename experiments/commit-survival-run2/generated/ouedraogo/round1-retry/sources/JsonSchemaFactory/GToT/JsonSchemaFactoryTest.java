/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The test class is organized as if three experts had contributed one test
 * per public method, then merged the contributions.  The suite covers:
 *
 * 1. Extraction of all public methods (see the comment block at the top).
 * 2. Basic “happy‑path” tests that verify normal behaviour.
 * 3. Edge‑case / exception tests (null arguments, malformed URIs,
 *    unsupported version flags, I/O errors, etc.).
 *
 * The tests use only the public API of the library and Mockito for
 * stubbing where creation of a full {@link JsonSchema} would otherwise
 * require heavy external resources.
 *
 * NOTE: The library under test (networknt JSON‑Schema validator) brings
 * a large set of transitive dependencies (Jackson, SLF4J, etc.).  The
 * test class assumes those are present on the class‑path together with
 * JUnit‑4 (4.13.2) and Mockito‑core (3.x).  No additional helper classes
 * are required.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * List of public methods extracted from {@link JsonSchemaFactory} (for reference):
 *
 * <pre>
 * public static Builder builder()
 * public static JsonSchemaFactory getInstance()
 * public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag)
 * public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag)
 * public static Builder builder(JsonSchemaFactory blueprint)
 * protected JsonSchema newJsonSchema(URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig config)
 * public JsonSchema create(ValidationContext validationContext, String schemaPath,
 *                          JsonNode schemaNode, JsonSchema parentSchema)
 * private JsonSchema doCreate(ValidationContext validationContext, String schemaPath,
 *                            URI currentUri, JsonNode schemaNode,
 *                            JsonSchema parentSchema, boolean suppressSubSchemaRetrieval)
 * protected ValidationContext createValidationContext(JsonNode schemaNode)
 * private JsonMetaSchema findMetaSchemaForSchema(JsonNode schemaNode)
 * private JsonMetaSchema fromId(String id)
 * public URIFactory getUriFactory()
 * public URITranslator getUriTranslator()
 * public JsonSchema getSchema(String schema, SchemaValidatorsConfig config)
 * public JsonSchema getSchema(String schema)
 * public JsonSchema getSchema(InputStream schemaStream, SchemaValidatorsConfig config)
 * public JsonSchema getSchema(InputStream schemaStream)
 * public JsonSchema getSchema(URI schemaUri, SchemaValidatorsConfig config)
 * public JsonSchema getSchema(URI schemaUri)
 * public JsonSchema getSchema(URI schemaUri, JsonNode jsonNode, SchemaValidatorsConfig config)
 * public JsonSchema getSchema(JsonNode jsonNode, SchemaValidatorsConfig config)
 * public JsonSchema getSchema(URI schemaUri, JsonNode jsonNode)
 * public JsonSchema getSchema(JsonNode jsonNode)
 * private boolean idMatchesSourceUri(JsonMetaSchema metaSchema, JsonNode schema, URI schemaUri)
 * private boolean isYaml(URI schemaUri)
 * static protected String normalizeMetaSchemaUri(String u)
 * </pre>
 */
public class JsonSchemaFactoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** -------------------------------------------------------------
     *  1️⃣  Test static builder() – basic factory creation.
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderCreatesFactoryWithDefaults() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                // use the official Draft‑04 meta‑schema that ships with the library
                .defaultMetaSchemaURI(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
                        .defaultMetaSchemaURI)
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
                        .jsonMetaSchemas.get(JsonSchemaFactory.normalizeMetaSchemaUri(
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
                                        .defaultMetaSchemaURI)))
                .build();

        assertNotNull("Factory must not be null", factory);
        assertNotNull("Default meta‑schema URI must be set", factory.defaultMetaSchemaURI);
        assertEquals("Default meta‑schema URI must be the Draft‑04 URI",
                JsonSchemaFactory.normalizeMetaSchemaUri(
                        "http://json-schema.org/draft-04/schema#"),
                factory.defaultMetaSchemaURI);
    }

    /** -------------------------------------------------------------
     *  2️⃣  Test getInstance() – deprecated no‑arg overload.
     * ------------------------------------------------------------- */
    @Test
    public void testGetInstanceDeprecated() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        assertNotNull(factory);
        // the deprecated method delegates to V4
        assertEquals(JsonSchemaFactory.normalizeMetaSchemaUri(
                "http://json-schema.org/draft-04/schema#"),
                factory.defaultMetaSchemaURI);
    }

    /** -------------------------------------------------------------
     *  3️⃣  Test getInstance(versionFlag) with a supported flag.
     * ------------------------------------------------------------- */
    @Test
    public void testGetInstanceWithVersionFlag() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(factory);
        // V7 uses its own draft URI – we just verify that the factory is built.
        assertNotNull(factory.defaultMetaSchemaURI);
    }

    /** -------------------------------------------------------------
     *  4️⃣  Test checkVersion(null) returns null.
     * ------------------------------------------------------------- */
    @Test
    public void testCheckVersionWithNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    /** -------------------------------------------------------------
     *  5️⃣  Test checkVersion with unsupported enum value.
     * ------------------------------------------------------------- */
    @Test
    public void testCheckVersionUnsupported() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported value");
        // simulate an unknown enum value by casting an int that is not defined.
        SpecVersion.VersionFlag bogus = Enum.valueOf(SpecVersion.VersionFlag.class, "V0");
        JsonSchemaFactory.checkVersion(bogus);
    }

    /** -------------------------------------------------------------
     *  6️⃣  Test builder(JsonSchemaFactory) – cloning a blueprint.
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderFromBlueprint() {
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(original);
        JsonSchemaFactory clone = builder.build();

        assertNotNull(clone);
        // the clone must keep the same default meta‑schema URI
        assertEquals(original.defaultMetaSchemaURI, clone.defaultMetaSchemaURI);
        // and must contain at least the same meta‑schema entry
        assertTrue(clone.jsonMetaSchemas.containsKey(original.defaultMetaSchemaURI));
    }

    /** -------------------------------------------------------------
     *  7️⃣  Test getSchema(String) with a valid JSON schema.
     * ------------------------------------------------------------- */
    @Test
    public void testGetSchemaFromStringValid() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        // a minimal, valid JSON‑Schema document
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertNotNull(schema);
        // The returned schema should have a ValidationContext whose meta‑schema
        // matches the factory's default.
        assertEquals(factory.defaultMetaSchemaURI,
                schema.getValidationContext().getMetaSchema().getUri());
    }

    /** -------------------------------------------------------------
     *  8️⃣  Test getSchema(String) with malformed JSON – expect exception.
     * ------------------------------------------------------------- */
    @Test
    public void testGetSchemaFromStringInvalidJson() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        String badJson = "{ this is not valid JSON }";

        thrown.expect(JsonSchemaException.class);
        factory.getSchema(badJson);
    }

    /** -------------------------------------------------------------
     *  9️⃣  Test getSchema(InputStream) – happy path using a temporary file.
     * ------------------------------------------------------------- */
    @Test
    public void testGetSchemaFromInputStream() throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        String json = "{\"type\":\"string\"}";
        try (InputStream is = new ByteArrayInputStream(json.getBytes())) {
            JsonSchema schema = factory.getSchema(is);
            assertNotNull(schema);
            assertEquals("string", schema.getValidationContext().getSchemaNode().get("type").asText());
        }
    }

    /** -------------------------------------------------------------
     * 10️⃣  Test getSchema(URI) with a non‑existing resource – expect exception.
     * ------------------------------------------------------------- */
    @Test
    public void testGetSchemaFromNonExistingUri() throws URISyntaxException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        URI bogus = new URI("classpath:/does/not/exist.json");

        thrown.expect(JsonSchemaException.class);
        factory.getSchema(bogus);
    }

    /** -------------------------------------------------------------
     * 11️⃣  Test isYaml() – various extensions.
     * ------------------------------------------------------------- */
    @Test
    public void testIsYamlDetection() throws URISyntaxException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

        URI yaml1 = new URI("file:/tmp/schema.yaml");
        URI yaml2 = new URI("file:/tmp/schema.yml");
        URI json  = new URI("file:/tmp/schema.json");
        URI noExt = new URI("file:/tmp/schema");

        // Using reflection to call the private method (allowed for testing)
        assertTrue((Boolean) invokePrivate(factory, "isYaml", yaml1));
        assertTrue((Boolean) invokePrivate(factory, "isYaml", yaml2));
        assertFalse((Boolean) invokePrivate(factory, "isYaml", json));
        assertFalse((Boolean) invokePrivate(factory, "isYaml", noExt));
    }

    /** -------------------------------------------------------------
     * 12️⃣  Test normalizeMetaSchemaUri() with a well‑formed URI.
     * ------------------------------------------------------------- */
    @Test
    public void testNormalizeMetaSchemaUriValid() {
        String input = "http://example.com:8080/path?query=123#frag";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals("https://example.com:8080/path", normalized);
    }

    /** -------------------------------------------------------------
     * 13️⃣  Test normalizeMetaSchemaUri() with malformed URI – expect exception.
     * ------------------------------------------------------------- */
    @Test
    public void testNormalizeMetaSchemaUriInvalid() {
        thrown.expect(JsonSchemaException.class);
        JsonSchemaFactory.normalizeMetaSchemaUri("http://[invalid-uri]");
    }

    /** -------------------------------------------------------------
     * 14️⃣  Test idMatchesSourceUri – positive match.
     * ------------------------------------------------------------- */
    @Test
    public void testIdMatchesSourceUriPositive() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        // Build a dummy JsonMetaSchema that simply reads "$id" from the node
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
        when(dummyMeta.readId(any(JsonNode.class))).thenAnswer(invocation -> {
            JsonNode node = invocation.getArgument(0);
            JsonNode idNode = node.get("$id");
            return (idNode != null && idNode.isTextual()) ? idNode.textValue() : null;
        });

        // JSON node with matching $id
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$id\":\"http://example.com/schema\"}");

        URI sourceUri = new URI("http://example.com/schema");
        boolean matches = (Boolean) invokePrivate(factory, "idMatchesSourceUri", dummyMeta, node, sourceUri);
        assertTrue(matches);
    }

    /** -------------------------------------------------------------
     * 15️⃣  Test idMatchesSourceUri – negative match (different ID).
     * ------------------------------------------------------------- */
    @Test
    public void testIdMatchesSourceUriNegative() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
        when(dummyMeta.readId(any(JsonNode.class))).thenReturn("http://example.com/other");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"$id\":\"http://example.com/other\"}");

        URI sourceUri = new URI("http://example.com/schema");
        boolean matches = (Boolean) invokePrivate(factory, "idMatchesSourceUri", dummyMeta, node, sourceUri);
        assertFalse(matches);
    }

    /** -------------------------------------------------------------
     * 16️⃣  Test newJsonSchema() – indirect verification via getSchema(JsonNode).
     * ------------------------------------------------------------- */
    @Test
    public void testNewJsonSchemaViaJsonNode() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.createObjectNode().put("type", "integer");
        JsonSchema schema = factory.getSchema(node, null);
        assertNotNull(schema);
        assertEquals("integer", schema.getValidationContext().getSchemaNode().get("type").asText());
    }

    /** -------------------------------------------------------------
     *  Utility: invoke a private method via reflection.
     * ------------------------------------------------------------- */
    private static Object invokePrivate(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] argTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class<?>[]::new);
        java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, argTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
