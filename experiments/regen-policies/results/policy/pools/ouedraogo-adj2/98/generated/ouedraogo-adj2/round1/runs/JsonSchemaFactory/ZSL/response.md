###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.networknt.schema.serialization.JsonNodeReader;
import com.networknt.schema.serialization.JsonNodeReader.DefaultJsonNodeReader;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class JsonSchemaFactoryTest {

    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";
    private static final String SIMPLE_SCHEMA_WITH_DOLLAR_ID = "{\"$id\":\"http://example.com/simple\",\"type\":\"object\"}";
    private static final String SCHEMA_WITH_DOLLAR_SCHEMA_V4 = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\"}";
    private static final String SCHEMA_WITH_DOLLAR_SCHEMA_V7 = "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}";

    private JsonSchemaFactory factoryV4;
    private JsonSchemaFactory factoryV7;

    @Before
    public void setUp() {
        factoryV4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        factoryV7 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /*** Builder basic configuration ***/
    @Test
    public void testBuilderCustomMappers() {
        ObjectMapper customJsonMapper = new ObjectMapper();
        ObjectMapper customYamlMapper = new ObjectMapper(); // just for test, not real yaml

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .jsonMapper(customJsonMapper)
                .yamlMapper(customYamlMapper)
                .build();

        // use reflection to access protected getObjectMapper / getYamlMapper
        try {
            Object jsonMapper = factory.getClass()
                    .getDeclaredMethod("getJsonMapper")
                    .invoke(factory);
            Object yamlMapper = factory.getClass()
                    .getDeclaredMethod("getYamlMapper")
                    .invoke(factory);
            assertSame(customJsonMapper, jsonMapper);
            assertSame(customYamlMapper, yamlMapper);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsEmptyDefaultMetaSchemaIri() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("")
                .metaSchema(JsonMetaSchema.getV4())
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsMissingMetaSchemas() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsDefaultMetaSchemaNotProvided() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV7().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .build();
    }

    /*** getInstance and customizer ***/
    @Test
    public void testGetInstanceWithCustomizer() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6, b -> {
            b.enableSchemaCache(false);
        });
        // cache disabled -> internal flag should be false
        try {
            boolean flag = (boolean) factory.getClass()
                    .getDeclaredField("enableSchemaCache")
                    .get(factory);
            assertFalse(flag);
        } catch (Exception e) {
            fail("Unable to read enableSchemaCache: " + e.getMessage());
        }
    }

    /*** checkVersion handling ***/
    @Test
    public void testCheckVersionSupported() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull(version);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupported() {
        // create a fake enum value via reflection is not possible; instead pass null and expect null
        JsonSchemaFactory.checkVersion(null);
    }

    /*** builder(blueprint) copies configuration ***/
    @Test
    public void testBuilderFromBlueprintCopiesMetaSchemas() {
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        // both factories should contain the same default meta schema IRI
        try {
            String originalIri = (String) original.getClass()
                    .getDeclaredField("defaultMetaSchemaIri")
                    .get(original);
            String copyIri = (String) copy.getClass()
                    .getDeclaredField("defaultMetaSchemaIri")
                    .get(copy);
            assertEquals(originalIri, copyIri);
        } catch (Exception e) {
            fail("Reflection error: " + e.getMessage());
        }
    }

    /*** getSchema overloads (string) ***/
    @Test
    public void testGetSchemaFromString() {
        JsonSchema schema = factoryV4.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema);
        assertEquals("object", schema.getNode("type").asText());
    }

    @Test
    public void testGetSchemaFromStringWithFormat() {
        JsonSchema schema = factoryV4.getSchema(SIMPLE_SCHEMA, InputFormat.JSON);
        assertNotNull(schema);
        assertEquals("object", schema.getNode("type").asText());
    }

    @Test
    public void testGetSchemaFromStringWithYaml() {
        // same JSON content but ask for YAML parser – it should still parse because content is JSON
        JsonSchema schema = factoryV4.getSchema(SIMPLE_SCHEMA, InputFormat.YAML);
        assertNotNull(schema);
        assertEquals("object", schema.getNode("type").asText());
    }

    /*** getSchema from InputStream ***/
    @Test
    public void testGetSchemaFromInputStream() {
        InputStream is = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
        JsonSchema schema = factoryV4.getSchema(is);
        assertNotNull(schema);
        assertEquals("object", schema.getNode("type").asText());
    }

    /*** getSchema from URI with custom loader ***/
    @Test
    public void testGetSchemaFromUriWithCustomLoaderAndCache() {
        final String testIri = "http://example.com/custom-schema.json";

        // custom loader that returns the simple schema for the above IRI
        SchemaLoader customLoader = new SchemaLoader() {
            @Override
            public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
                return new InputStreamSource() {
                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
                    }
                };
            }
        };

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .schemaLoaders(b -> b.add(customLoader))
                .enableSchemaCache(true)
                .build();

        // First load
        JsonSchema schema1 = factory.getSchema(URI.create(testIri));
        assertNotNull(schema1);
        assertEquals("object", schema1.getNode("type").asText());

        // Second load – should come from cache (same instance)
        JsonSchema schema2 = factory.getSchema(URI.create(testIri));
        assertSame(schema1, schema2);
    }

    @Test
    public void testGetSchemaFromUriWithCacheDisabled() {
        final String testIri = "http://example.com/custom-schema-no-cache.json";

        SchemaLoader customLoader = new SchemaLoader() {
            @Override
            public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
                return new InputStreamSource() {
                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
                    }
                };
            }
        };

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .schemaLoaders(b -> b.add(customLoader))
                .enableSchemaCache(false)
                .build();

        JsonSchema schema1 = factory.getSchema(URI.create(testIri));
        JsonSchema schema2 = factory.getSchema(URI.create(testIri));
        // Without cache we expect different instances
        assertNotSame(schema1, schema2);
    }

    /*** withMetaSchema switches meta schema when $schema differs ***/
    @Test
    public void testWithMetaSchemaSwitchesWhenDifferent() throws Exception {
        // factory default meta schema is V4
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonNode nodeV7 = new ObjectMapper().readTree(SCHEMA_WITH_DOLLAR_SCHEMA_V7);
        // Use reflection to call private withMetaSchema
        ValidationContext originalCtx = (ValidationContext) factory.getClass()
                .getDeclaredMethod("createValidationContext", JsonNode.class, SchemaValidatorsConfig.class)
                .invoke(factory, nodeV7, new SchemaValidatorsConfig());

        // invoke withMetaSchema
        ValidationContext newCtx = (ValidationContext) factory.getClass()
                .getDeclaredMethod("withMetaSchema", ValidationContext.class, JsonNode.class)
                .invoke(factory, originalCtx, nodeV7);

        assertNotEquals(originalCtx.getMetaSchema().getIri(), newCtx.getMetaSchema().getIri());
        assertEquals(JsonMetaSchema.getV7().getIri(), newCtx.getMetaSchema().getIri());
    }

    /*** getMetaSchemaOrDefault throws when $schema is non‑textual ***/
    @Test(expected = JsonSchemaException.class)
    public void testGetMetaSchemaOrDefaultInvalidIriNode() throws Exception {
        JsonNode badNode = new ObjectMapper().readTree("{\"$schema\":123}");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        // invoke private method
        factory.getClass()
                .getDeclaredMethod("getMetaSchemaOrDefault", JsonNode.class, SchemaValidatorsConfig.class)
                .invoke(factory, badNode, new SchemaValidatorsConfig());
    }

    /*** normalizeMetaSchemaUri handling ***/
    @Test
    public void testNormalizeMetaSchemaUriRecognized() {
        // recognized flag should stay unchanged
        String iri = SpecVersion.VersionFlag.V7.getId();
        String normalized = (String) JsonSchemaFactory.class
                .getDeclaredMethod("normalizeMetaSchemaUri", String.class)
                .invoke(null, iri);
        assertEquals(iri, normalized);
    }

    @Test
    public void testNormalizeMetaSchemaUriUnnormalizedDraft07() throws Exception {
        String unnorm = "http://json-schema.org/draft-07/schema#";
        String normalized = (String) JsonSchemaFactory.class
                .getDeclaredMethod("normalizeMetaSchemaUri", String.class)
                .invoke(null, unnorm);
        assertEquals(SchemaId.V7, normalized);
    }

    @Test
    public void testNormalizeMetaSchemaUriUnnormalizedDraft201909() throws Exception {
        String unnorm = "http://json-schema.org/draft/2019-09/schema#";
        String normalized = (String) JsonSchemaFactory.class
                .getDeclaredMethod("normalizeMetaSchemaUri", String.class)
                .invoke(null, unnorm);
        assertEquals(SchemaId.V201909, normalized);
    }

    /*** isYaml detection ***/
    @Test
    public void testIsYamlDetection() throws Exception {
        SchemaLocation yamlLocation = SchemaLocation.of("http://example.com/schema.yaml");
        SchemaLocation jsonLocation = SchemaLocation.of("http://example.com/schema.json");
        SchemaLocation noExtLocation = SchemaLocation.of("http://example.com/schema");

        boolean yaml1 = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", SchemaLocation.class)
                .invoke(factoryV4, yamlLocation);
        boolean yaml2 = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", SchemaLocation.class)
                .invoke(factoryV4, jsonLocation);
        boolean yaml3 = (boolean) JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", SchemaLocation.class)
                .invoke(factoryV4, noExtLocation);

        assertTrue(yaml1);
        assertFalse(yaml2);
        assertFalse(yaml3);
    }

    /*** getSchema with JsonNode directly ***/
    @Test
    public void testGetSchemaFromJsonNode() throws Exception {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_SCHEMA_WITH_DOLLAR_ID);
        JsonSchema schema = factoryV4.getSchema(node);
        assertNotNull(schema);
        // the $id should be recognized as the schema id
        assertEquals("http://example.com/simple", schema.getId());
    }

    /*** getSchema with SchemaLocation and JsonNode ***/
    @Test
    public void testGetSchemaFromLocationAndNode() throws Exception {
        JsonNode node = new ObjectMapper().readTree(SIMPLE_SCHEMA);
        SchemaLocation location = SchemaLocation.of("http://example.com/base");
        JsonSchema schema = factoryV4.getSchema(location, node);
        assertNotNull(schema);
        // Since no $id, getId() may be null; we just ensure schema is created
        assertNull(schema.getId());
    }

    /*** preload does not throw when initializeValidators fails ***/
    @Test
    public void testPreloadHandlesInitializeExceptionGracefully() throws Exception {
        // Create a schema that references a non‑existent $ref to trigger exception during initializeValidators
        String schemaWithBadRef = "{\"$ref\":\"http://nonexistent/does-not-exist.json\"}";
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setPreloadJsonSchema(true); // enable preload

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .build();

        // The call should not throw, even though the reference cannot be resolved
        JsonSchema schema = factory.getSchema(schemaWithBadRef, config);
        assertNotNull(schema);
    }

    /*** getSchemaLoader returns non‑null ***/
    @Test
    public void testGetSchemaLoaderNotNull() {
        SchemaLoader loader = factoryV4.getSchemaLoader();
        assertNotNull(loader);
    }

    /*** createSchemaValidatorsConfig returns a fresh instance each time ***/
    @Test
    public void testCreateSchemaValidatorsConfigProducesNewInstance() throws Exception {
        SchemaValidatorsConfig cfg1 = (SchemaValidatorsConfig) factoryV4.getClass()
                .getDeclaredMethod("createSchemaValidatorsConfig")
                .invoke(factoryV4);
        SchemaValidatorsConfig cfg2 = (SchemaValidatorsConfig) factoryV4.getClass()
                .getDeclaredMethod("createSchemaValidatorsConfig")
                .invoke(factoryV4);
        assertNotSame(cfg1, cfg2);
    }

    /*** getMetaSchema loads via metaSchemaFactory if supplied ***/
    @Test
    public void testGetMetaSchemaUsesCustomFactory() {
        JsonMetaSchemaFactory customFactory = (iri, factory, cfg) -> {
            // Return V6 for any request
            return JsonMetaSchema.getV6();
        };

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV6().getIri())
                .metaSchema(JsonMetaSchema.getV6())
                .metaSchemaFactory(customFactory)
                .build();

        JsonMetaSchema meta = factory.getMetaSchema("any-iri", new SchemaValidatorsConfig());
        assertEquals(JsonMetaSchema.getV6().getIri(), meta.getIri());
    }

    /*** loadMetaSchema delegates correctly ***/
    @Test
    public void testLoadMetaSchemaDelegatesToDefaultFactory() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonMetaSchema meta = (JsonMetaSchema) factory.getClass()
                .getDeclaredMethod("loadMetaSchema", String.class, SchemaValidatorsConfig.class)
                .invoke(factory, JsonMetaSchema.getV4().getIri(), new SchemaValidatorsConfig());
        assertEquals(JsonMetaSchema.getV4().getIri(), meta.getIri());
    }

    /*** getSchema(URI) uses internal getMappedSchema ***/
    @Test
    public void testGetSchemaFromUriUsesMappedSchema() {
        final String testIri = "http://example.com/uri-schema.json";

        SchemaLoader customLoader = new SchemaLoader() {
            @Override
            public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
                return new InputStreamSource() {
                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
                    }
                };
            }
        };

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .schemaLoaders(b -> b.add(customLoader))
                .build();

        JsonSchema schema = factory.getSchema(URI.create(testIri));
        assertNotNull(schema);
        assertEquals("object", schema.getNode("type").asText());
    }

    /*** getSchema(Location) without fragment returns full schema ***/
    @Test
    public void testGetSchemaLocationWithoutFragment() throws Exception {
        SchemaLocation location = SchemaLocation.of("http://example.com/full-schema.json");
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .schemaLoaders(b -> b.add(new SchemaLoader() {
                    @Override
                    public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
                        return () -> new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
                    }
                }))
                .build();

        JsonSchema schema = factory.getSchema(location);
        assertNotNull(schema);
        assertEquals("object", schema.getNode("type").asText());
    }

    /*** getSchema(Location) with fragment returns sub‑schema ***/
    @Test
    public void testGetSchemaLocationWithFragment() throws Exception {
        // schema with a nested property schema
        String complex = "{\"type\":\"object\",\"properties\":{\"inner\":{\"type\":\"string\"}}}";
        SchemaLocation location = SchemaLocation.of("http://example.com/complex-schema.json#/properties/inner");
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .schemaLoaders(b -> b.add(new SchemaLoader() {
                    @Override
                    public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
                        return () -> new ByteArrayInputStream(complex.getBytes());
                    }
                }))
                .build();

        JsonSchema subSchema = factory.getSchema(location);
        assertNotNull(subSchema);
        // the sub-schema should be the schema for the inner property (type string)
        assertEquals("string", subSchema.getNode("type").asText());
    }
}
###Test END##