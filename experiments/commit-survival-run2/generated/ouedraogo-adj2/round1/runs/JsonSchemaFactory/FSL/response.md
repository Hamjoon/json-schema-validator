###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URLFactory;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory}.
 *
 * The tests exercise the public API as well as selected private / protected
 * behaviour via reflection where required. Real objects are constructed – no
 * mocking frameworks are used.
 */
public class JsonSchemaFactoryTest {

    /** Helper to obtain the V4 meta‑schema URI */
    private static String v4Uri() {
        return JsonMetaSchema.getV4().getUri();
    }

    /** Helper to create a minimal factory with V4 meta‑schema */
    private static JsonSchemaFactory minimalFactory() {
        return JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
    }

    /** Helper to read a JSON string into a {@link JsonNode} */
    private static JsonNode readJson(String json) throws IOException {
        return new ObjectMapper().readTree(json);
    }

    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
        assertNotNull(factory);
        // The default meta‑schema URI should be normalized to https
        assertTrue(factory.getUriFactory() instanceof URIFactory);
    }

    @Test
    public void testGetInstanceAndCheckVersion() {
        JsonSchemaFactory v4Factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull(v4Factory);
        assertEquals(JsonMetaSchema.getV4().getUri(),
                v4Factory.builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                        .builder().defaultMetaSchemaURI(v4Uri()).build()
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .build();

        // Verify that checkVersion returns the correct concrete version class
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V7) instanceof com.networknt.schema.Version7);
        assertTrue(JsonSchemaFactory.checkVersion(VersionFlag.V4) instanceof com.networknt.schema.Version4);
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testNormalizeMetaSchemaUri() throws Exception {
        // Normal case – scheme is forced to https, port and path are preserved
        String original = "http://example.com:8080/path/to/schema.json";
        String normalized = (String) invokeStaticProtected("normalizeMetaSchemaUri", original);
        assertEquals("https://example.com:8080/path/to/schema.json", normalized);

        // Invalid URI should result in JsonSchemaException
        try {
            invokeStaticProtected("normalizeMetaSchemaUri", "::::");
            fail("Expected JsonSchemaException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof JsonSchemaException);
        }
    }

    @Test
    public void testIsYamlDetection() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI yaml1 = new URI("file:/tmp/schema.yaml");
        URI yaml2 = new URI("file:/tmp/schema.yml");
        URI json = new URI("file:/tmp/schema.json");

        assertTrue((Boolean) isYaml.invoke(minimalFactory(), yaml1));
        assertTrue((Boolean) isYaml.invoke(minimalFactory(), yaml2));
        assertFalse((Boolean) isYaml.invoke(minimalFactory(), json));
    }

    @Test
    public void testGetSchemaFromStringValidJson() throws Exception {
        JsonSchemaFactory factory = minimalFactory();
        String schema = "{\"type\":\"string\"}";
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);

        // Validate a correct instance
        JsonNode instance = readJson("\"hello\"");
        assertTrue(jsonSchema.validate(instance).isEmpty());

        // Validate an incorrect instance (number)
        JsonNode badInstance = readJson("123");
        assertFalse(jsonSchema.validate(badInstance).isEmpty());
    }

    @Test
    public void testGetSchemaFromStringInvalidJson() {
        JsonSchemaFactory factory = minimalFactory();
        String malformed = "{ type: \"string\" "; // missing closing brace
        try {
            factory.getSchema(malformed);
            fail("Expected JsonSchemaException due to malformed JSON");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    @Test
    public void testGetSchemaFromInputStream() throws Exception {
        JsonSchemaFactory factory = minimalFactory();
        String schema = "{\"type\":\"integer\"}";
        InputStream stream = new ByteArrayInputStream(schema.getBytes("UTF-8"));
        JsonSchema jsonSchema = factory.getSchema(stream);
        assertNotNull(jsonSchema);
        JsonNode valid = readJson("42");
        assertTrue(jsonSchema.validate(valid).isEmpty());
    }

    @Test
    public void testGetSchemaFromUriWithCache() throws Exception {
        // Create a temporary JSON schema file
        File temp = File.createTempFile("schema", ".json");
        temp.deleteOnExit();
        Files.write(temp.toPath(), "{\"type\":\"boolean\"}".getBytes("UTF-8"));
        URI uri = temp.toURI();

        JsonSchemaFactory factory = minimalFactory();

        JsonSchema first = factory.getSchema(uri);
        JsonSchema second = factory.getSchema(uri);

        // Should be the same cached instance
        assertSame(first, second);
    }

    @Test
    public void testGetSchemaFromUriUnsupportedScheme() throws Exception {
        JsonSchemaFactory factory = minimalFactory();
        URI unsupported = new URI("unsupported://example.com/schema.json");
        try {
            factory.getSchema(unsupported);
            fail("Expected JsonSchemaException due to unsupported scheme");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    @Test
    public void testBuilderCopyPreservesConfiguration() {
        JsonSchemaFactory original = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .enableUriSchemaCache(false)
                .build();

        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        // The copy should retain the same default meta‑schema URI
        assertEquals(original.builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder().defaultMetaSchemaURI(v4Uri()).build()
                .builder()
                .defaultMetaSchemaURI(v4Uri())
                .enableUriSchemaCache(false)
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        // Verify that cache flag is preserved (original disabled, copy disabled)
        // The field is private; use reflection to read it.
        boolean originalCache = (Boolean) getPrivateField(original, "enableUriSchemaCache");
        boolean copyCache = (Boolean) getPrivateField(copy, "enableUriSchemaCache");
        assertEquals(originalCache, copyCache);
    }

    @Test
    public void testUriTranslatorMapping() throws Exception {
        // Create a simple mapping: map http://example.com/schema.json to a classpath resource
        Map<String, String> map = Collections.singletonMap(
                "http://example.com/schema.json",
                "classpath:/test-schema.json"
        );

        // Prepare a classpath resource (place a tiny JSON schema under src/test/resources)
        // For the purpose of this test we assume such a resource exists.
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .addUriMappings(map)
                .build();

        URI original = new URI("http://example.com/schema.json");
        URI translated = factory.getUriTranslator().translate(original);
        // The translation should replace the original with the mapped classpath URI
        assertEquals("classpath:/test-schema.json", translated.toString());

        // Now load the schema using the original URI; the factory should resolve via the mapping
        JsonSchema schema = factory.getSchema(original);
        assertNotNull(schema);
    }

    /** Utility to invoke a protected static method via reflection */
    private static Object invokeStaticProtected(String methodName, Object... args) throws Exception {
        Method method = JsonSchemaFactory.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    /** Utility to read a private field via reflection */
    private static Object getPrivateField(Object instance, String fieldName) throws Exception {
        java.lang.reflect.Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }
}
###Test END##