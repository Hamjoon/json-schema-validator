###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URLFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factory;
    private JsonMetaSchema v4MetaSchema;
    private String v4Uri;

    @Before
    public void setUp() {
        // Use the built‑in V4 meta‑schema as the default
        v4MetaSchema = JsonMetaSchema.getV4();
        v4Uri = v4MetaSchema.getUri();

        factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri)
                .addMetaSchema(v4MetaSchema)
                .enableUriSchemaCache(true) // we want to test the cache
                .build();
    }

    @After
    public void tearDown() {
        factory = null;
    }

    /*** Builder validation ***/
    @Test
    public void builderCreatesFactoryWithValidArguments() {
        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri)
                .addMetaSchema(v4MetaSchema)
                .build();

        assertNotNull(f);
        assertEquals(v4Uri, getPrivateField(f, "defaultMetaSchemaURI"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaIsNull() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(null)
                .addMetaSchema(v4MetaSchema)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenMetaSchemasEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri)
                .build(); // no meta‑schema added
    }

    /*** checkVersion and getInstance ***/
    @Test
    public void checkVersionReturnsCorrectVersionObjects() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void deprecatedGetInstanceReturnsFactoryForV4() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull(f);
        // the default meta‑schema must be V4
        assertEquals(v4Uri, getPrivateField(f, "defaultMetaSchemaURI"));
    }

    /*** getUriFactory and getUriTranslator ***/
    @Test
    public void getUriFactoryReturnsConfiguredFactory() {
        URIFactory uf = factory.getUriFactory();
        assertNotNull(uf);
        assertTrue(uf instanceof URISchemeFactory);
    }

    @Test
    public void getUriTranslatorAppliesMappings() {
        // add a simple mapping via the builder
        Map<String, String> map = new HashMap<>();
        map.put("http://example.com/old", "http://example.com/new");

        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri)
                .addMetaSchema(v4MetaSchema)
                .addUriMappings(map)
                .build();

        URITranslator translator = f.getUriTranslator();
        URI original = URI.create("http://example.com/old");
        URI translated = translator.translate(original);
        assertEquals("http://example.com/new", translated.toString());
    }

    /*** getSchema(String) – JSON string ***/
    @Test
    public void getSchemaFromStringParsesJson() {
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertNotNull(schema);
        // a basic validation: the root schema should accept a valid instance
        JsonNode instance = new ObjectMapper().readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> messages = schema.validate(instance);
        assertTrue(messages.isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromInvalidStringThrowsException() {
        // malformed JSON
        factory.getSchema("{invalid json}");
    }

    /*** getSchema(InputStream) ***/
    @Test
    public void getSchemaFromInputStreamWorks() throws IOException {
        String schemaJson = "{\"type\":\"string\"}";
        InputStream is = new ByteArrayInputStream(schemaJson.getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
        JsonNode instance = new ObjectMapper().readTree("\"hello\"");
        Set<ValidationMessage> messages = schema.validate(instance);
        assertTrue(messages.isEmpty());
    }

    /*** getSchema(URI) – file based schema and cache behaviour ***/
    @Test
    public void getSchemaFromFileUriUsesCache() throws Exception {
        // write a tiny schema to a temporary file
        String schemaJson = "{\"type\":\"number\"}";
        File tmp = File.createTempFile("schema", ".json");
        tmp.deleteOnExit();
        Files.write(tmp.toPath(), schemaJson.getBytes());

        URI fileUri = tmp.toURI(); // e.g. file:/tmp/xxxx.json

        JsonSchema first = factory.getSchema(fileUri);
        JsonSchema second = factory.getSchema(fileUri);

        // because caching is enabled, the same object instance should be returned
        assertSame(first, second);
    }

    /*** isYaml detection – private method accessed via reflection ***/
    @Test
    public void isYamlDetectsYamlExtension() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI yamlUri = new URI("file:/tmp/example.yaml");
        URI ymlUri = new URI("file:/tmp/example.yml");
        URI jsonUri = new URI("file:/tmp/example.json");

        assertTrue((Boolean) isYaml.invoke(factory, yamlUri));
        assertTrue((Boolean) isYaml.invoke(factory, ymlUri));
        assertFalse((Boolean) isYaml.invoke(factory, jsonUri));
    }

    /*** normalizeMetaSchemaUri – static method ***/
    @Test
    public void normalizeMetaSchemaUriConvertsToHttps() {
        String httpUri = "http://json-schema.org/draft-04/schema";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(httpUri);
        assertTrue(normalized.startsWith("https://"));
        assertEquals("https://json-schema.org/draft-04/schema", normalized);
    }

    /*** idMatchesSourceUri – private method via reflection ***/
    @Test
    public void idMatchesSourceUriWorksWhenIdEqualsUri() throws Exception {
        // Build a schema that contains an $id equal to the source URI
        String id = "http://example.com/my-schema";
        String schemaJson = "{\"$id\":\"" + id + "\",\"type\":\"object\"}";
        JsonNode node = new ObjectMapper().readTree(schemaJson);
        JsonSchema schema = factory.getSchema(node, null);

        // invoke private method
        Method idMatches = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        idMatches.setAccessible(true);
        boolean result = (Boolean) idMatches.invoke(factory, v4MetaSchema, node, URI.create(id));
        assertTrue(result);
    }

    /*** findMetaSchemaForSchema – via getSchema(String) with $schema pointer ***/
    @Test
    public void schemaWithExplicitMetaSchemaIsResolved() {
        // V7 meta‑schema URI (the V4 factory knows only V4; we add V7 meta‑schema as well)
        JsonMetaSchema v7Meta = JsonMetaSchema.getV7();
        String v7Uri = v7Meta.getUri();

        JsonSchemaFactory f = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri)
                .addMetaSchema(v4MetaSchema)
                .addMetaSchema(v7Meta)
                .build();

        String schema = "{\"$schema\":\"" + v7Uri + "\",\"type\":\"null\"}";
        JsonSchema s = f.getSchema(schema);
        // The factory should have used the V7 meta‑schema (no exception means success)
        assertNotNull(s);
    }

    /*** fromId – unknown dialect leads to exception ***/
    @Test(expected = JsonSchemaException.class)
    public void fromIdWithUnknownDialectThrows() throws Exception {
        Method fromId = JsonSchemaFactory.class.getDeclaredMethod("fromId", String.class);
        fromId.setAccessible(true);
        // an arbitrary, non‑dialect URI
        fromId.invoke(factory, "http://unknown/dialect");
    }

    /*** builder(blueprint) copies configuration ***/
    @Test
    public void builderFromBlueprintCopiesAllSettings() {
        // add a custom URI mapping to the original factory
        Map<String, String> map = new HashMap<>();
        map.put("http://a", "http://b");
        JsonSchemaFactory original = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(v4Uri)
                .addMetaSchema(v4MetaSchema)
                .addUriMappings(map)
                .enableUriSchemaCache(false)
                .build();

        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        // verify that the map was copied
        URITranslator translator = copy.getUriTranslator();
        URI translated = translator.translate(URI.create("http://a"));
        assertEquals("http://b", translated.toString());

        // verify that the cache flag is also copied
        boolean cacheEnabled = (Boolean) getPrivateField(copy, "enableUriSchemaCache");
        assertFalse(cacheEnabled);
    }

    /*** create(ValidationContext, ...) – internal path handling ***/
    @Test
    public void createCreatesChildSchemaWithCorrectPath() {
        // simple parent schema
        String parentJson = "{\"type\":\"object\",\"properties\":{\"inner\":{\"type\":\"string\"}}}";
        JsonSchema parent = factory.getSchema(parentJson);

        // child node (the "inner" property)
        JsonNode childNode = parent.getCurrentUri(); // not needed; we fetch directly from node
        JsonNode schemaNode = new ObjectMapper().readTree("{\"type\":\"string\"}");

        // use the public create method
        ValidationContext ctx = new ValidationContext(
                factory.getUriFactory(),
                null,
                v4MetaSchema,
                factory,
                null);

        JsonSchema child = factory.create(ctx, "#/properties/inner", schemaNode, parent);
        assertNotNull(child);
        // the child's current URI should be the same as the parent (since we passed null base)
        assertEquals(parent.getCurrentUri(), child.getCurrentUri());
    }

    /*** private helper to fetch field values via reflection ***/
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
```
###Test END##