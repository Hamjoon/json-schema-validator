/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests are written from the perspective of three “experts”:
 *   1. Builder & construction logic
 *   2. Schema loading / URI handling
 *   3. Internal helpers and edge‑case validation
 *
 * Each expert contributes at least one test that targets a distinct functionality.
 * Together they give a wide coverage of typical use‑cases, edge‑cases and error‑scenarios.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /* --------------------------------------------------------------------- */
    /* 1. Builder & construction tests (Expert #1)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void builderCreatesFactoryWithDefaults() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).jsonMetaSchemas
                        .get(JsonSchemaFactory.normalizeMetaSchemaUri("http://json-schema.org/draft-04/schema#")))
                .build();

        assertNotNull("Factory must be created", factory);
        assertEquals("Default meta‑schema URI must be normalized to https",
                "https://json-schema.org/draft-04/schema#", factory.defaultMetaSchemaURI);
        assertNotNull("URI factory must be present", factory.getUriFactory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildFailsWhenObjectMapperIsNull() {
        JsonSchemaFactory.builder()
                .objectMapper(null)                     // <-- illegal
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).jsonMetaSchemas
                        .get(JsonSchemaFactory.normalizeMetaSchemaUri("http://json-schema.org/draft-04/schema#")))
                .build();                               // should throw
    }

    @Test
    public void builderCopyProducesEquivalentFactory() {
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        // compare a few critical fields – they must be equal but not the same object
        assertEquals(original.defaultMetaSchemaURI, copy.defaultMetaSchemaURI);
        assertEquals(original.jsonMetaSchemas.keySet(), copy.jsonMetaSchemas.keySet());
        assertNotSame(original.uriFactory, copy.uriFactory);
    }

    @Test
    public void checkVersionValidAndInvalid() {
        // valid values
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);

        // invalid value – expect IllegalArgumentException
        try {
            JsonSchemaFactory.checkVersion(null);
            fail("null flag should return null (no exception)");
        } catch (Exception ignored) {
            // the method returns null – this is OK, we only test the illegal branch below
        }

        try {
            JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("UNKNOWN"));
            fail("Unsupported enum should raise IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void normalizeMetaSchemaUriConvertsToHttps() {
        String httpUri = "http://example.com/schema#";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(httpUri);
        assertEquals("https://example.com/schema#", normalized);
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriRejectsBadSyntax() {
        JsonSchemaFactory.normalizeMetaSchemaUri("ht!tp://[bad]");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Schema loading & URI handling tests (Expert #2)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromStringValidJson() {
        String schema = "{\"type\":\"string\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        // the schema must be able to validate a simple string
        JsonNode valid = new ObjectMapper().readTree("\"hello\"");
        assertTrue(jsonSchema.validate(valid, new SchemaValidatorsConfig()).isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringInvalidJsonThrows() {
        String malformed = "{\"type\":\"string\""; // missing closing brace
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        factory.getSchema(malformed);
    }

    @Test
    public void getSchemaFromInputStreamYaml() throws IOException {
        // Create a temporary YAML file
        File yamlFile = tmp.newFile("test-schema.yaml");
        try (Writer w = new FileWriter(yamlFile)) {
            w.write("type: string\n");
        }

        URI yamlUri = yamlFile.toURI();
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).jsonMetaSchemas
                        .get(JsonSchemaFactory.normalizeMetaSchemaUri("http://json-schema.org/draft-04/schema#")))
                .build();

        JsonSchema schema = factory.getSchema(yamlUri);
        assertNotNull(schema);
        JsonNode node = new ObjectMapper().readTree("\"abc\"");
        assertTrue(schema.validate(node, new SchemaValidatorsConfig()).isEmpty());
    }

    @Test
    public void uriSchemaCacheIsUsedWhenEnabled() throws Exception {
        // Write a tiny schema to a temporary file
        File jsonFile = tmp.newFile("cached-schema.json");
        try (Writer w = new FileWriter(jsonFile)) {
            w.write("{\"type\":\"integer\"}");
        }

        URI schemaUri = jsonFile.toURI();
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).jsonMetaSchemas
                        .get(JsonSchemaFactory.normalizeMetaSchemaUri("http://json-schema.org/draft-04/schema#")))
                .enableUriSchemaCache(true)
                .build();

        // First call – loads from file
        JsonSchema first = factory.getSchema(schemaUri);
        assertNotNull(first);

        // Second call – should hit the cache (no new fetch)
        JsonSchema second = factory.getSchema(schemaUri);
        assertSame("When caching is enabled the same instance must be returned", first, second);
    }

    @Test
    public void idMatchesSourceUriBehaviour() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        // Build a minimal schema node with $id matching the source URI
        ObjectMapper om = new ObjectMapper();
        JsonNode matching = om.createObjectNode().put("$id", "http://example.com/schema.json");
        JsonNode nonMatching = om.createObjectNode().put("$id", "http://example.com/other.json");

        JsonMetaSchema meta = factory.jsonMetaSchemas.get(JsonSchemaFactory.normalizeMetaSchemaUri(factory.defaultMetaSchemaURI));
        assertTrue(factory.idMatchesSourceUri(meta, matching, URI.create("http://example.com/schema.json")));
        assertFalse(factory.idMatchesSourceUri(meta, nonMatching, URI.create("http://example.com/schema.json")));
    }

    @Test
    public void isYamlDetectsExtensionsCorrectly() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertTrue(factory.isYaml(URI.create("file:///tmp/schema.yaml")));
        assertTrue(factory.isYaml(URI.create("file:///tmp/schema.yml")));
        assertFalse(factory.isYaml(URI.create("file:///tmp/schema.json")));
        assertFalse(factory.isYaml(URI.create("file:///tmp/schema"))); // no extension → assume JSON
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUnknownDialectThrows() {
        // Use a meta‑schema URI that is syntactically valid but unknown to the factory
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).jsonMetaSchemas
                        .get(JsonSchemaFactory.normalizeMetaSchemaUri("http://json-schema.org/draft-04/schema#")))
                .build();

        // The schema node contains an unknown $schema value
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.createObjectNode().put("$schema", "http://example.com/unknown-dialect#");
        factory.getSchema(node, null);
    }

    /* --------------------------------------------------------------------- */
    /* 3. Internal helper and edge‑case tests (Expert #3)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void createValidationContextUsesDefaultMetaSchemaWhenMissing() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        ObjectMapper om = new ObjectMapper();
        JsonNode schemaNode = om.createObjectNode(); // No $schema property
        ValidationContext ctx = factory.createValidationContext(schemaNode);
        assertNotNull(ctx);
        assertEquals(factory.defaultMetaSchemaURI, ctx.getMetaSchema().getUri());
    }

    @Test
    public void findMetaSchemaForSchemaRespectsExplicit$schema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.createObjectNode().put("$schema", "http://json-schema.org/draft-07/schema#");
        JsonMetaSchema meta = factory.findMetaSchemaForSchema(node);
        // draft‑07 must be present because the factory was bootstrapped with it
        assertEquals("https://json-schema.org/draft-07/schema#", meta.getUri());
    }

    @Test(expected = JsonSchemaException.class)
    public void findMetaSchemaForSchemaRejectsNonTextual$schema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.createObjectNode().set("$schema", om.createArrayNode()); // array instead of text
        factory.findMetaSchemaForSchema(node);
    }

    @Test
    public void fromIdDetectsWellKnownDialect() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonMetaSchema meta = factory.fromId("http://json-schema.org/draft-04/schema#");
        assertNotNull(meta);
        assertEquals("https://json-schema.org/draft-04/schema#", meta.getUri());
    }

    @Test
    public void getUriTranslatorCombinesMappingsAndCustomTranslators() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).jsonMetaSchemas
                        .get(JsonSchemaFactory.normalizeMetaSchemaUri("http://json-schema.org/draft-04/schema#")));

        // simple static map mapping "http://old" -> "http://new"
        Map<String, String> map = new HashMap<>();
        map.put("http://old.com/schema.json", "http://new.com/schema.json");
        builder.addUriMappings(map);

        // custom translator that prefixes "file:" URIs with a base directory
        builder.addUriTranslator(uri -> {
            if (uri.getScheme().equals("file")) {
                return URI.create(uri.toString().replace("file:", "file:/tmp"));
            }
            return uri;
        });

        JsonSchemaFactory factory = builder.build();
        URITranslator translator = factory.getUriTranslator();

        // mapping from map
        URI translated = translator.translate(URI.create("http://old.com/schema.json"));
        assertEquals(URI.create("http://new.com/schema.json"), translated);

        // custom translator
        URI fileUri = URI.create("file:/example.json");
        URI translatedFile = translator.translate(fileUri);
        assertEquals(URI.create("file:/tmp/example.json"), translatedFile);
    }

    @Test
    public void getSchemaHandlesNullConfigGracefully() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema schema = factory.getSchema("{\"type\":\"boolean\"}", (SchemaValidatorsConfig) null);
        assertNotNull(schema);
        JsonNode node = new ObjectMapper().readTree("true");
        assertTrue(schema.validate(node, null).isEmpty());
    }

    @Test
    public void getSchemaFromUriWhenFetchingFailsPropagatesException() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("http://json-schema.org/draft-04/schema#")
                .addMetaSchema(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).jsonMetaSchemas
                        .get(JsonSchemaFactory.normalizeMetaSchemaUri("http://json-schema.org/draft-04/schema#")))
                .build();

        // Use a non‑existent file URI
        URI bogus = new File(tmp.getRoot(), "nonexistent.json").toURI();

        try {
            factory.getSchema(bogus);
            fail("Expected JsonSchemaException because the underlying fetcher cannot locate the file");
        } catch (JsonSchemaException e) {
            // expected – the cause should be an IOException
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods that are not part of the public API (package‑private)   */
    /* --------------------------------------------------------------------- */

    // The following methods expose package‑private members for test validation.
    // They are placed inside the test class to avoid polluting the production code.

    private static class TestHelper {
        static ValidationContext createValidationContext(JsonSchemaFactory factory, JsonNode node) {
            return factory.createValidationContext(node);
        }

        static JsonMetaSchema findMetaSchemaForSchema(JsonSchemaFactory factory, JsonNode node) {
            return factory.findMetaSchemaForSchema(node);
        }
    }
}
