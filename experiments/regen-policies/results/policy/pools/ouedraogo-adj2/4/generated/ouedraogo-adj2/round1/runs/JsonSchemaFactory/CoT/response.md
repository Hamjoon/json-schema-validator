###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factory;
    private ObjectMapper objectMapper;
    private YAMLMapper yamlMapper;

    @Before
    public void setUp() {
        this.objectMapper = new ObjectMapper();
        this.yamlMapper = new YAMLMapper();

        // Build a factory that knows the Draft‑V4 meta‑schema.
        this.factory = JsonSchemaFactory.builder()
                .objectMapper(objectMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();
    }

    /*** 1. basic builder sanity ***/
    @Test
    public void testBuilderCreatesFactory() {
        assertNotNull(factory);
        assertNotNull(factory.getUriFactory());
        assertNotNull(factory.getUriTranslator());
    }

    /*** 2. checkVersion edge cases ***/
    @Test
    public void testCheckVersionNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testCheckVersionSupported() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull("Version should not be null for flag " + flag, version);
        }
    }

    /*** 3. getInstance convenience ***/
    @Test
    public void testGetInstanceDefault() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(f);
        assertEquals(factory.getUriFactory().getClass(), f.getUriFactory().getClass());
    }

    /*** 4. getSchema from a JSON string – happy path ***/
    @Test
    public void testGetSchemaFromStringValid() throws IOException {
        String schema = "{\"type\":\"string\"}";
        JsonSchema js = factory.getSchema(schema);
        assertNotNull(js);

        JsonNode instance = objectMapper.readTree("\"hello\"");
        assertTrue(js.validate(instance).isEmpty());
    }

    /*** 5. getSchema from a JSON string – malformed JSON ***/
    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJson() {
        String malformed = "{type: string}";
        factory.getSchema(malformed);
    }

    /*** 6. getSchema from InputStream – malformed JSON ***/
    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalid() throws IOException {
        String malformed = "{type: string}";
        InputStream is = new ByteArrayInputStream(malformed.getBytes());
        factory.getSchema(is);
    }

    /*** 7. getSchema from a file URI – JSON ***/
    @Test
    public void testGetSchemaFromUriFileJson() throws Exception {
        File temp = File.createTempFile("schema", ".json");
        temp.deleteOnExit();
        try (Writer w = new FileWriter(temp)) {
            w.write("{\"type\":\"integer\"}");
        }

        URI uri = temp.toURI();
        JsonSchema js = factory.getSchema(uri);
        assertNotNull(js);

        JsonNode instance = objectMapper.readTree("123");
        assertTrue(js.validate(instance).isEmpty());
    }

    /*** 8. getSchema from a file URI – YAML ***/
    @Test
    public void testGetSchemaFromUriFileYaml() throws Exception {
        File temp = File.createTempFile("schema", ".yaml");
        temp.deleteOnExit();
        try (Writer w = new FileWriter(temp)) {
            w.write("type: string");
        }

        URI uri = temp.toURI();
        JsonSchema js = factory.getSchema(uri);
        assertNotNull(js);

        JsonNode instance = objectMapper.readTree("\"world\"");
        assertTrue(js.validate(instance).isEmpty());
    }

    /*** 9. URI schema cache – enabled ***/
    @Test
    public void testUriSchemaCacheEnabled() throws Exception {
        File temp = File.createTempFile("schema", ".json");
        temp.deleteOnExit();
        try (Writer w = new FileWriter(temp)) {
            w.write("{\"type\":\"boolean\"}");
        }
        URI uri = temp.toURI();

        JsonSchema first = factory.getSchema(uri);
        JsonSchema second = factory.getSchema(uri);

        // With caching the same instance should be returned.
        assertSame("Schemas should be cached and identical", first, second);
    }

    /*** 10. URI schema cache – disabled ***/
    @Test
    public void testUriSchemaCacheDisabled() throws Exception {
        JsonSchemaFactory noCacheFactory = JsonSchemaFactory.builder()
                .objectMapper(objectMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .enableUriSchemaCache(false)
                .build();

        File temp = File.createTempFile("schema", ".json");
        temp.deleteOnExit();
        try (Writer w = new FileWriter(temp)) {
            w.write("{\"type\":\"null\"}");
        }
        URI uri = temp.toURI();

        JsonSchema first = noCacheFactory.getSchema(uri);
        JsonSchema second = noCacheFactory.getSchema(uri);

        // With caching disabled we expect distinct objects.
        assertNotSame("Schemas should not be cached", first, second);
    }

    /*** 11. isYaml detection – via reflection ***/
    @Test
    public void testIsYamlDetection() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI yaml = new URI("file:/tmp/example.yaml");
        URI yml  = new URI("file:/tmp/example.yml");
        URI json = new URI("file:/tmp/example.json");
        URI noExt = new URI("file:/tmp/example");

        assertTrue((Boolean) isYaml.invoke(factory, yaml));
        assertTrue((Boolean) isYaml.invoke(factory, yml));
        assertFalse((Boolean) isYaml.invoke(factory, json));
        assertFalse((Boolean) isYaml.invoke(factory, noExt));
    }

    /*** 12. normalizeMetaSchemaUri – malformed URI ***/
    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriInvalid() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        normalize.invoke(null, "http://[invalid-uri]");
    }

    /*** 13. URI translator mappings ***/
    @Test
    public void testUriTranslatorMappings() {
        Map<String, String> map = new HashMap<>();
        map.put("http://example.com/original.json", "http://example.com/mapped.json");

        JsonSchemaFactory mappedFactory = JsonSchemaFactory.builder()
                .objectMapper(objectMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .addUriMappings(map)
                .build();

        URI original = URI.create("http://example.com/original.json");
        URI translated = mappedFactory.getUriTranslator().translate(original);
        assertEquals("http://example.com/mapped.json", translated.toString());
    }

    /*** 14. getSchema with $id matching source URI ***/
    @Test
    public void testGetSchemaWithIdMatchingSourceUri() throws Exception {
        // Build a temporary file whose content contains an $id that matches the file URI.
        File temp = File.createTempFile("schema", ".json");
        temp.deleteOnExit();
        URI uri = temp.toURI();
        String id = uri.toString();
        try (Writer w = new FileWriter(temp)) {
            w.write("{\"$id\":\"" + id + "\", \"type\":\"string\"}");
        }

        JsonSchema js = factory.getSchema(uri);
        assertNotNull(js);
        // The schemaLocation should have been derived from the $id (including a trailing '#')
        assertTrue(js.getCurrentUri().toString().startsWith(id));
    }

    /*** 15. getSchema with unknown meta‑schema URI – expect exception ***/
    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaUnknownMetaSchema() throws IOException {
        // Schema with an unknown $schema value.
        String schema = "{\"$schema\":\"http://example.com/unknown-meta\", \"type\":\"string\"}";
        factory.getSchema(schema);
    }

    /*** 16. getSchema with null config – should not throw ***/
    @Test
    public void testGetSchemaWithNullConfig() throws IOException {
        String schema = "{\"type\":\"number\"}";
        JsonSchema js = factory.getSchema(schema, null);
        assertNotNull(js);
        JsonNode instance = objectMapper.readTree("3.14");
        assertTrue(js.validate(instance).isEmpty());
    }

    /*** 17. getSchema from JsonNode directly ***/
    @Test
    public void testGetSchemaFromJsonNode() throws IOException {
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"boolean\"}");
        JsonSchema js = factory.getSchema(schemaNode);
        assertNotNull(js);
        JsonNode instance = objectMapper.readTree("true");
        assertTrue(js.validate(instance).isEmpty());
    }

    /*** 18. getSchema from URI + JsonNode ***/
    @Test
    public void testGetSchemaFromUriAndJsonNode() throws IOException {
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"array\", \"items\":{\"type\":\"integer\"}}");
        URI dummy = URI.create("http://example.com/dummy");
        JsonSchema js = factory.getSchema(dummy, schemaNode);
        assertNotNull(js);
        JsonNode instance = objectMapper.readTree("[1,2,3]");
        assertTrue(js.validate(instance).isEmpty());
    }

    /*** 19. getSchema with custom SchemaValidatorsConfig (failFast) ***/
    @Test
    public void testGetSchemaWithCustomConfig() throws IOException {
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(true);

        String schema = "{\"type\":\"object\", \"required\":[\"name\"]}";
        JsonSchema js = factory.getSchema(schema, config);
        assertNotNull(js);

        JsonNode invalid = objectMapper.readTree("{}");
        // With failFast we still get a ValidationResult, but the set is non‑empty.
        assertFalse(js.validate(invalid).isEmpty());
    }

    /*** 20. getUriFactory correctness ***/
    @Test
    public void testGetUriFactory() {
        URIFactory uriFactory = factory.getUriFactory();
        assertNotNull(uriFactory);
        URI created = uriFactory.create("http://example.com");
        assertEquals("http://example.com", created.toString());
    }
}
```
###Test END##