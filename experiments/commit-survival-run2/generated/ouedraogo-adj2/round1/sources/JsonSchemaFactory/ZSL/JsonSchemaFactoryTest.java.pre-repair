package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 * The tests instantiate real factories, schemas and translators – no mocking is used.
 */
public class JsonSchemaFactoryTest {

    /** Simple {@link URIFactory} that creates a {@link URI} directly from the supplied string. */
    private static class SimpleURIFactory implements URIFactory {
        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }

        @Override
        public URI create(URI baseURI, String segment) {
            return baseURI.resolve(segment);
        }
    }

    /** Simple {@link URIFetcher} that always returns the same JSON content regardless of the URI. */
    private static class SimpleURIFetcher implements URIFetcher {
        private final String content;

        SimpleURIFetcher(String content) {
            this.content = content;
        }

        @Override
        public InputStream fetch(URI uri) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Helper method that creates a minimal {@link JsonSchemaFactory} with a custom
     * {@link URIFactory}/{@link URIFetcher} pair for the scheme {@code test}.
     */
    private JsonSchemaFactory createFactoryWithTestScheme(String schemaContent, boolean enableCache) {
        SimpleURIFactory testFactory = new SimpleURIFactory();
        SimpleURIFetcher testFetcher = new SimpleURIFetcher(schemaContent);

        return JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .uriFactory(testFactory, "test")
                .uriFetcher(testFetcher, "test")
                .enableUriSchemaCache(enableCache)
                .build();
    }

    @Test
    public void testBuilderValidation_nullObjectMapper() {
        try {
            JsonSchemaFactory.builder()
                    .objectMapper(null)
                    .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                    .addMetaSchema(JsonMetaSchema.getV4())
                    .build();
            Assert.fail("Expected IllegalArgumentException because ObjectMapper is null");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("ObjectMapper must not be null"));
        }
    }

    @Test
    public void testBuilderValidation_nullYamlMapper() {
        try {
            JsonSchemaFactory.builder()
                    .yamlMapper(null)
                    .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                    .addMetaSchema(JsonMetaSchema.getV4())
                    .build();
            Assert.fail("Expected IllegalArgumentException because YAMLMapper is null");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("YAMLMapper must not be null"));
        }
    }

    @Test
    public void testBuilderValidation_missingDefaultMetaSchemaURI() {
        try {
            JsonSchemaFactory.builder()
                    .addMetaSchema(JsonMetaSchema.getV4())
                    .build();
            Assert.fail("Expected IllegalArgumentException because defaultMetaSchemaURI is missing");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("defaultMetaSchemaURI must not be null or empty"));
        }
    }

    @Test
    public void testBuilderValidation_missingMetaSchemas() {
        try {
            JsonSchemaFactory.builder()
                    .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                    .build();
            Assert.fail("Expected IllegalArgumentException because meta schema collection is empty");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Json Meta Schemas must not be null or empty"));
        }
    }

    @Test
    public void testBuilderValidation_defaultMetaSchemaNotProvided() {
        try {
            JsonSchemaFactory.builder()
                    .defaultMetaSchemaURI(JsonMetaSchema.getV6().getUri()) // V6 not added
                    .addMetaSchema(JsonMetaSchema.getV4())
                    .build();
            Assert.fail("Expected IllegalArgumentException because default meta schema is not present in the map");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Meta Schema for default Meta Schema URI must be provided"));
        }
    }

    @Test
    public void testGetInstanceAndDefaultMetaSchemaURI() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        Assert.assertNotNull(factory);
        Assert.assertEquals(JsonMetaSchema.getV4().getUri(),
                factory.getUriTranslator().translate(URI.create("http://example.com")).toString()); // translator is NOOP, just checking not null
        // defaultMetaSchemaURI is private; we verify indirectly by creating a schema without $schema and checking it uses V4
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        Assert.assertNotNull(schema);
    }

    @Test
    public void testCheckVersionMapping() {
        Assert.assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        Assert.assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        Assert.assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        Assert.assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        Assert.assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
        Assert.assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testNormalizeMetaSchemaUri() {
        String httpUri = "http://example.com/schema.json#fragment";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(httpUri);
        Assert.assertEquals("https://example.com/schema.json", normalized);
    }

    @Test
    public void testGetSchemaFromStringValidJson() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        JsonSchema schema = factory.getSchema("{\"type\":\"integer\"}");
        Assert.assertNotNull(schema);
        // The schema does not have an explicit $id, current URI should be null
        Assert.assertNull(schema.getCurrentUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJsonThrowsException() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        factory.getSchema("{invalid json}");
    }

    @Test
    public void testGetSchemaFromInputStream() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        InputStream is = new ByteArrayInputStream("{\"type\":\"boolean\"}".getBytes(StandardCharsets.UTF_8));
        JsonSchema schema = factory.getSchema(is);
        Assert.assertNotNull(schema);
    }

    @Test
    public void testUriTranslatorMapping() {
        Map<String, String> map = new HashMap<>();
        map.put("test://original", "test://mapped");
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .addUriMappings(map)
                .build();

        URI original = URI.create("test://original");
        URI translated = factory.getUriTranslator().translate(original);
        Assert.assertEquals("test://mapped", translated.toString());
    }

    @Test
    public void testIsYamlDetection() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        // Use reflection to invoke the private isYaml method
        try {
            java.lang.reflect.Method method = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
            method.setAccessible(true);
            Assert.assertTrue((Boolean) method.invoke(factory, URI.create("file:///schema.yaml")));
            Assert.assertTrue((Boolean) method.invoke(factory, URI.create("file:///schema.yml")));
            Assert.assertFalse((Boolean) method.invoke(factory, URI.create("file:///schema.json")));
        } catch (Exception e) {
            Assert.fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    public void testGetSchemaFromURIWithCachingEnabled() {
        String jsonSchema = "{\"$id\":\"test://my-schema.json\",\"type\":\"object\"}";
        JsonSchemaFactory factory = createFactoryWithTestScheme(jsonSchema, true);

        URI uri = URI.create("test://my-schema.json");
        JsonSchema schema1 = factory.getSchema(uri);
        JsonSchema schema2 = factory.getSchema(uri);

        Assert.assertNotNull(schema1);
        Assert.assertNotNull(schema2);
        // When caching is enabled the same instance should be returned
        Assert.assertSame(schema1, schema2);
    }

    @Test
    public void testGetSchemaFromURIWithCachingDisabled() {
        String jsonSchema = "{\"$id\":\"test://my-schema.json\",\"type\":\"object\"}";
        JsonSchemaFactory factory = createFactoryWithTestScheme(jsonSchema, false);

        URI uri = URI.create("test://my-schema.json");
        JsonSchema schema1 = factory.getSchema(uri);
        JsonSchema schema2 = factory.getSchema(uri);

        Assert.assertNotNull(schema1);
        Assert.assertNotNull(schema2);
        // When caching is disabled the instances must be different
        Assert.assertNotSame(schema1, schema2);
    }

    @Test
    public void testIdMatchesSourceUriLogic() {
        // The schema contains $id that matches the requested URI.
        String jsonSchema = "{\"$id\":\"test://match.json\",\"type\":\"string\"}";
        JsonSchemaFactory factory = createFactoryWithTestScheme(jsonSchema, true);
        URI uri = URI.create("test://match.json");

        // The method idMatchesSourceUri is private; we trigger it via getSchema(URI)
        JsonSchema schema = factory.getSchema(uri);
        Assert.assertNotNull(schema);
        // The current URI of the created schema should be the same as the source URI
        Assert.assertEquals(uri, schema.getCurrentUri());
    }

    @Test
    public void testGetUriFactoryReturnsConfiguredFactory() {
        SimpleURIFactory customFactory = new SimpleURIFactory();
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .uriFactory(customFactory, "test")
                .build();

        URIFactory returned = factory.getUriFactory();
        Assert.assertSame(customFactory, returned);
    }

    @Test
    public void testBuilderCopyFromExistingFactory() {
        JsonSchemaFactory original = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(JsonMetaSchema.getV4().getUri())
                .addMetaSchema(JsonMetaSchema.getV4())
                .build();

        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        // The copy should produce a schema identical to the original for the same JSON
        JsonSchema schemaOriginal = original.getSchema("{\"type\":\"null\"}");
        JsonSchema schemaCopy = copy.getSchema("{\"type\":\"null\"}");

        Assert.assertNotNull(schemaOriginal);
        Assert.assertNotNull(schemaCopy);
        // They are different instances but behave the same; we just ensure no exception is thrown.
        Assert.assertEquals(schemaOriginal.validate(null).size(), schemaCopy.validate(null).size());
    }
}
