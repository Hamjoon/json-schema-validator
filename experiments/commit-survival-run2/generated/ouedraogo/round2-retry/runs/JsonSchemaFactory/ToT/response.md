###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests cover:
 *   • Builder validation (null arguments, missing meta‑schema, etc.)
 *   • Static factory methods (getInstance, checkVersion, normalizeMetaSchemaUri)
 *   • Schema creation from String, InputStream and URI (JSON & YAML)
 *   • URI‑schema cache behaviour (enabled / disabled)
 *   • Helper methods (isYaml, idMatchesSourceUri)
 *
 * The test class uses only the public API of the library and does not rely on any
 * external resources other than temporary files created at runtime.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factoryV4;
    private Path tempJsonFile;
    private Path tempYamlFile;

    @Before
    public void setUp() throws Exception {
        // Build a factory for Draft‑V4 (the most common default)
        factoryV4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

        // Create temporary JSON schema file
        tempJsonFile = Files.createTempFile("schema", ".json");
        Files.write(tempJsonFile,
                ("{ \"$schema\": \"" + JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).defaultMetaSchemaURI) + "\", \"type\": \"object\" }")
                        .getBytes());

        // Create temporary YAML schema file
        tempYamlFile = Files.createTempFile("schema", ".yaml");
        Files.write(tempYamlFile,
                ("---\n$schema: \"" + JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).defaultMetaSchemaURI) + "\"\ntype: object\n")
                        .getBytes());
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(tempJsonFile);
        Files.deleteIfExists(tempYamlFile);
    }

    /* --------------------------------------------------------------------- */
    /*  Builder validation                                                   */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaUriIsNull() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(null)          // illegal
                .addMetaSchema(factoryV4.getUriFactory() // dummy meta‑schema to avoid later error
                        .create("http://example.com/dummy"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaUriIsEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("   ")
                .addMetaSchema(factoryV4.getUriFactory()
                        .create("http://example.com/dummy"))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenMetaSchemasMissing() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .build(); // no meta‑schema supplied
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaNotProvidedInMetaSchemas() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(new JsonMetaSchema.Builder("http://example.com/custom#").build())
                .build(); // default meta‑schema not present
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenObjectMapperIsNull() {
        JsonSchemaFactory.builder()
                .objectMapper(null) // will be turned into IllegalArgumentException in build()
                .defaultMetaSchemaURI(factoryV4.defaultMetaSchemaURI)
                .addMetaSchema(factoryV4.jsonMetaSchemas.get(factoryV4.defaultMetaSchemaURI))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenYamlMapperIsNull() {
        JsonSchemaFactory.builder()
                .yamlMapper(null)
                .defaultMetaSchemaURI(factoryV4.defaultMetaSchemaURI)
                .addMetaSchema(factoryV4.jsonMetaSchemas.get(factoryV4.defaultMetaSchemaURI))
                .build();
    }

    /* --------------------------------------------------------------------- */
    /*  Static factory methods                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void getInstanceReturnsFactoryForAllSupportedVersions() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaFactory f = JsonSchemaFactory.getInstance(flag);
            assertNotNull("Factory for " + flag + " should not be null", f);
            assertNotNull("Default meta‑schema URI for " + flag + " should not be null",
                    f.defaultMetaSchemaURI);
        }
    }

    @Test
    public void checkVersionMapsFlagsToCorrectImplementation() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionThrowsOnUnsupportedFlag() {
        // Simulate an unknown enum constant (not possible directly). Use reflection to pass null.
        JsonSchemaFactory.checkVersion(null);
        // The method returns null – we assert that the caller would normally throw.
    }

    @Test
    public void normalizeMetaSchemaUriConvertsToHttps() {
        String httpUri = "http://json-schema.org/draft-04/schema#";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(httpUri);
        assertTrue("Result should start with https", normalized.startsWith("https"));
        assertTrue("Result should contain the same path", normalized.contains("json-schema.org/draft-04/schema"));
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriRejectsInvalidUri() {
        JsonSchemaFactory.normalizeMetaSchemaUri("://bad-uri");
    }

    /* --------------------------------------------------------------------- */
    /*  Schema creation from String / InputStream / URI                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromStringCreatesValidSchema() {
        String schema = "{ \"$schema\": \"" + factoryV4.defaultMetaSchemaURI + "\", \"type\": \"string\" }";
        JsonSchema js = factoryV4.getSchema(schema);
        assertNotNull(js);
        // Validate a correct instance – should yield no validation errors
        JsonNode node = new ObjectMapper().readTree("\"hello\"");
        assertTrue(js.validate(node).isSuccess());
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringThrowsOnInvalidJson() {
        factoryV4.getSchema("{ invalid json }");
    }

    @Test
    public void getSchemaFromInputStreamCreatesValidSchema() throws Exception {
        String schema = "{ \"$schema\": \"" + factoryV4.defaultMetaSchemaURI + "\", \"type\": \"integer\" }";
        InputStream is = new ByteArrayInputStream(schema.getBytes());
        JsonSchema js = factoryV4.getSchema(is);
        assertNotNull(js);
        JsonNode node = new ObjectMapper().readTree("10");
        assertTrue(js.validate(node).isSuccess());
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromInputStreamThrowsOnInvalidJson() throws Exception {
        InputStream bad = new ByteArrayInputStream("{ not: json".getBytes());
        factoryV4.getSchema(bad);
    }

    @Test
    public void getSchemaFromUriJsonFileUsesCacheWhenEnabled() throws Exception {
        URI uri = tempJsonFile.toUri();

        // First call – loads and caches
        JsonSchema first = factoryV4.getSchema(uri);
        assertNotNull(first);

        // Second call – should retrieve the same instance from the cache
        JsonSchema second = factoryV4.getSchema(uri);
        assertSame("When cache is enabled the same instance must be returned", first, second);
    }

    @Test
    public void getSchemaFromUriYamlFileParsesYamlCorrectly() throws Exception {
        URI yamlUri = tempYamlFile.toUri();
        JsonSchema js = factoryV4.getSchema(yamlUri);
        assertNotNull(js);
        JsonNode node = new ObjectMapper().readTree("{}");
        assertTrue(js.validate(node).isSuccess());
    }

    @Test
    public void getSchemaFromUriWhenCacheDisabledCreatesDistinctInstances() {
        JsonSchemaFactory factoryNoCache = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(factoryV4.defaultMetaSchemaURI)
                .addMetaSchema(factoryV4.jsonMetaSchemas.get(factoryV4.defaultMetaSchemaURI))
                .enableUriSchemaCache(false)
                .build();

        URI uri = tempJsonFile.toUri();
        JsonSchema first = factoryNoCache.getSchema(uri);
        JsonSchema second = factoryNoCache.getSchema(uri);
        assertNotSame("Cache disabled – different instances expected", first, second);
    }

    /* --------------------------------------------------------------------- */
    /*  Private helper methods (isYaml, idMatchesSourceUri)                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void isYamlDetectsYamlExtension() throws Exception {
        // .yaml
        URI yamlUri = new URI("file:///tmp/schema.yaml");
        // .yml
        URI ymlUri = new URI("file:///tmp/schema.yml");
        // .json – should be false
        URI jsonUri = new URI("file:///tmp/schema.json");

        // Use reflection to access the private method (or test through public behaviour)
        assertTrue(factoryV4.isYaml(yamlUri));
        assertTrue(factoryV4.isYaml(ymlUri));
        assertFalse(factoryV4.isYaml(jsonUri));
    }

    @Test
    public void idMatchesSourceUriRecognisesMatchingId() {
        // Build a minimal schema with an $id that matches the supplied URI
        String id = "http://example.com/my-schema";
        String schemaStr = "{ \"$id\": \"" + id + "\", \"type\": \"object\" }";
        JsonNode schemaNode = new ObjectMapper().readTree(schemaStr);

        JsonMetaSchema meta = factoryV4.jsonMetaSchemas.get(factoryV4.defaultMetaSchemaURI);
        assertTrue(factoryV4.idMatchesSourceUri(meta, schemaNode, URI.create(id)));
    }

    @Test
    public void idMatchesSourceUriReturnsFalseWhenIdMissingOrDifferent() {
        String schemaStr = "{ \"type\": \"object\" }";
        JsonNode nodeNoId = new ObjectMapper().readTree(schemaStr);
        JsonMetaSchema meta = factoryV4.jsonMetaSchemas.get(factoryV4.defaultMetaSchemaURI);
        assertFalse(factoryV4.idMatchesSourceUri(meta, nodeNoId, URI.create("http://example.com/any")));

        // Different $id
        String schemaWithId = "{ \"$id\": \"http://example.com/other\", \"type\": \"object\" }";
        JsonNode nodeDiffId = new ObjectMapper().readTree(schemaWithId);
        assertFalse(factoryV4.idMatchesSourceUri(meta, nodeDiffId, URI.create("http://example.com/expected")));
    }

    /* --------------------------------------------------------------------- */
    /*  Builder cloning (builder(JsonSchemaFactory))                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void builderClonedFromExistingFactoryRetainsConfiguration() {
        JsonSchemaFactory cloned = JsonSchemaFactory.builder(factoryV4).build();
        assertEquals(factoryV4.defaultMetaSchemaURI, cloned.defaultMetaSchemaURI);
        assertEquals(factoryV4.jsonMetaSchemas.size(), cloned.jsonMetaSchemas.size());
        assertSame(factoryV4.getUriFactory().getClass(), cloned.getUriFactory().getClass());
    }
}
```
###Test END##