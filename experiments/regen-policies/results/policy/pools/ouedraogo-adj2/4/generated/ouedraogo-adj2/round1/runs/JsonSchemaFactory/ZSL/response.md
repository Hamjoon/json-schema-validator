###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factory;

    @Before
    public void setUp() {
        // Use the built‑in V4 meta‑schema for all tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull("Factory must be created", factory);
    }

    /** Verify that the Builder creates a functional factory. */
    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory built = JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        assertNotNull("Factory built via builder must not be null", built);
        // a simple schema should be parsed without error
        JsonSchema schema = built.getSchema("{\"type\":\"string\"}");
        assertNotNull("Schema parsed from string must not be null", schema);
    }

    /** Test the static normalizeMetaSchemaUri utility. */
    @Test
    public void testNormalizeMetaSchemaUri() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);

        String original = "http://example.com:8080/path/to/schema.json?query=123#frag";
        String normalized = (String) normalize.invoke(null, original);
        // scheme must be forced to https and query/fragment stripped
        assertTrue("Normalized URI must start with https", normalized.startsWith("https://"));
        assertFalse("Normalized URI must not contain query", normalized.contains("?"));
        assertFalse("Normalized URI must not contain fragment", normalized.contains("#"));
        assertTrue("Normalized URI must retain host and path",
                normalized.equals("https://example.com:8080/path/to/schema.json"));
    }

    /** Verify checkVersion works for all supported flags and returns null for null input. */
    @Test
    public void testCheckVersion() {
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909));
        assertNotNull(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012));

        // null flag should return null (no exception)
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    /** Load a schema from a JSON string. */
    @Test
    public void testGetSchemaFromString() {
        String json = "{\"type\":\"integer\"}";
        JsonSchema schema = factory.getSchema(json);
        assertNotNull("Schema loaded from JSON string must not be null", schema);
        JsonNode node = schema.getSchemaNode();
        assertEquals("Node type must be object", JsonNode.class, node.getClass().getSuperclass());
    }

    /** Load a schema from an InputStream containing JSON. */
    @Test
    public void testGetSchemaFromInputStream() {
        String json = "{\"type\":\"boolean\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull("Schema loaded from InputStream must not be null", schema);
    }

    /** Helper to write temporary schema files. */
    private File writeTempFile(String prefix, String suffix, String content) throws Exception {
        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();
        try (FileWriter writer = new FileWriter(temp)) {
            writer.write(content);
        }
        return temp;
    }

    /** Load a JSON schema from a file‑based URI and verify caching works. */
    @Test
    public void testGetSchemaFromUriJsonAndCache() throws Exception {
        String jsonSchema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        File file = writeTempFile("schema", ".json", jsonSchema);
        URI uri = file.toURI();

        JsonSchema first = factory.getSchema(uri);
        assertNotNull("First load from URI must not be null", first);

        JsonSchema second = factory.getSchema(uri);
        assertNotNull("Second load from same URI must not be null", second);

        // The cache is enabled by default – the two instances must be the same object.
        assertSame("Schema instances must be cached and identical", first, second);
    }

    /** Load a YAML schema from a file‑based URI (triggers the isYaml branch). */
    @Test
    public void testGetSchemaFromUriYaml() throws Exception {
        String yamlSchema = "type: object\nproperties:\n  age:\n    type: integer\n";
        File file = writeTempFile("schema", ".yaml", yamlSchema);
        URI uri = file.toURI();

        JsonSchema schema = factory.getSchema(uri);
        assertNotNull("YAML schema loaded from URI must not be null", schema);
        // The schema node should contain the "type" property
        JsonNode node = schema.getSchemaNode();
        assertEquals("type", node.fieldNames().next());
    }

    /** Verify that a schema whose $id matches the source URI is handled correctly. */
    @Test
    public void testIdMatchesSourceUri() throws Exception {
        // Use draft‑07 meta‑schema where the id keyword is "$id"
        JsonMetaSchema meta = JsonMetaSchema.getV7();
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(meta.getUri())
                .addMetaSchema(meta)
                .build();

        String schemaId = "http://example.com/my-schema.json";
        String schemaContent = "{ \"$id\": \"" + schemaId + "\", \"type\": \"null\" }";

        // Write to a temporary file and obtain its URI
        File file = writeTempFile("idmatch", ".json", schemaContent);
        URI fileUri = file.toURI();

        // Load using the factory – because $id matches the URI, the factory should
        // construct a SchemaLocation that ends with '#'
        JsonSchema schema = customFactory.getSchema(fileUri);
        assertNotNull(schema);
        assertEquals("Current URI of the schema must be the normalized file URI",
                customFactory.getUriFactory().create(fileUri.toString()),
                schema.getCurrentUri());
    }

    /** Ensure that an unknown $schema value results in a JsonSchemaException. */
    @Test(expected = JsonSchemaException.class)
    public void testUnknownMetaSchemaThrowsException() {
        String unknownSchema = "{ \"$schema\": \"http://example.com/unknown\", \"type\": \"string\" }";
        factory.getSchema(unknownSchema);
    }

    /** Test that the URI factory can create a URI from a string. */
    @Test
    public void testGetUriFactory() {
        URIFactory uriFactory = factory.getUriFactory();
        assertNotNull(uriFactory);
        URI uri = uriFactory.create("http://example.org/test");
        assertEquals("http://example.org/test", uri.toString());
    }

    /** Verify that the URI translator incorporates custom mappings. */
    @Test
    public void testUriTranslatorWithMappings() {
        // Build a factory with a custom mapping: old -> new
        JsonSchemaFactory mappedFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .addUriMappings(Collections.singletonMap("http://old.com/schema.json", "http://new.com/schema.json"))
                .build();

        URITranslator translator = mappedFactory.getUriTranslator();
        URI original = URI.create("http://old.com/schema.json");
        URI translated = translator.translate(original);
        assertEquals("http://new.com/schema.json", translated.toString());
    }

    /** Test copying a blueprint factory via the static builder(blueprint) method. */
    @Test
    public void testBuilderFromBlueprint() {
        // Original factory
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

        // Create a new factory from the original blueprint
        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        // The default meta‑schema URI must be identical
        assertEquals("Copy must retain the same default meta‑schema URI",
                getPrivateField(original, "defaultMetaSchemaURI"),
                getPrivateField(copy, "defaultMetaSchemaURI"));
    }

    /** Helper to read private fields via reflection (used only for test assertions). */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
###Test END##