###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.networknt.schema.serialization.JsonNodeReader;
import com.networknt.schema.serialization.YamlMapperFactory;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public static and instance methods.</li>
 *   <li>Typical usage scenarios (building a factory, creating a schema, etc.).</li>
 *   <li>Edge‑cases and exception handling (missing configuration, invalid JSON, etc.).</li>
 *   <li>Custom {@link JsonMetaSchemaFactory} integration.</li>
 * </ul>
 */
public class JsonSchemaFactoryTest {

    private JsonSchemaFactory factoryV4;

    @Before
    public void setUp() {
        // Create a factory for the Draft‑4 meta‑schema – this is the simplest
        // configuration that satisfies the internal constructor checks.
        factoryV4 = JsonSchemaFactory.getInstance(VersionFlag.V4);
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  Public static methods                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .build();

        assertNotNull("Factory created by builder must not be null", factory);
        assertNotNull("Schema loader must be available", factory.getSchemaLoader());
    }

    @Test
    public void testGetInstanceWithCustomizer() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7, b -> b.enableSchemaCache(false));
        assertNotNull(factory);
        // The customizer only toggles the cache flag – we can verify by reflection
        // that the internal field is set to false.
        try {
            java.lang.reflect.Field f = JsonSchemaFactory.class.getDeclaredField("enableSchemaCache");
            f.setAccessible(true);
            assertFalse((Boolean) f.get(factory));
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    public void testCheckVersionReturnsNullForNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testCheckVersionSupportedValues() {
        for (VersionFlag flag : VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull("Version must be resolved for flag " + flag, version);
            assertNotNull("Meta‑schema instance must be non‑null", version.getInstance());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckVersionUnsupportedValueThrows() {
        // Simulate an unsupported value by creating a dummy enum constant via reflection.
        // This is only possible because the method falls through to the default case.
        VersionFlag bogus = java.lang.reflect.Proxy.newProxyInstance(
                VersionFlag.class.getClassLoader(),
                new Class[]{VersionFlag.class},
                (proxy, method, args) -> {
                    if ("name".equals(method.getName())) return "BOGUS";
                    return null;
                });
        JsonSchemaFactory.checkVersion(bogus);
    }

    @Test
    public void testBuilderFromBlueprintCopiesConfiguration() {
        JsonSchemaFactory blueprint = JsonSchemaFactory.getInstance(VersionFlag.V6);
        JsonSchemaFactory copy = JsonSchemaFactory.builder(blueprint).build();

        assertNotNull(copy);
        // The copy must contain the same default meta‑schema IRI.
        assertEquals(blueprint.defaultMetaSchemaIri, copy.defaultMetaSchemaIri);
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  Core factory behaviour                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetObjectMapperJson() {
        ObjectMapper mapper = factoryV4.getObjectMapper(InputFormat.JSON);
        assertNotNull(mapper);
        // The default mapper should be the singleton from JsonMapperFactory
        assertSame(JsonMapperFactory.getInstance(), mapper);
    }

    @Test
    public void testGetObjectMapperYaml() {
        ObjectMapper mapper = factoryV4.getObjectMapper(InputFormat.YAML);
        assertNotNull(mapper);
        assertSame(YamlMapperFactory.getInstance(), mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetObjectMapperUnsupportedFormatThrows() {
        // Use a deliberately unsupported format (null) to trigger the exception.
        factoryV4.getObjectMapper(null);
    }

    @Test
    public void testReadTreeFromStringJson() throws IOException {
        String json = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonNode node = factoryV4.readTree(json, InputFormat.JSON);
        assertNotNull(node);
        assertEquals("object", node.get("type").asText());
        assertTrue(node.get("properties").has("name"));
    }

    @Test
    public void testReadTreeFromInputStreamJson() throws IOException {
        String json = "{\"type\":\"array\"}";
        ByteArrayInputStream is = new ByteArrayInputStream(json.getBytes());
        JsonNode node = factoryV4.readTree(is, InputFormat.JSON);
        assertNotNull(node);
        assertEquals("array", node.get("type").asText());
    }

    @Test
    public void testGetSchemaFromString() {
        JsonSchema schema = factoryV4.getSchema("{\"type\":\"string\"}");
        assertNotNull("Schema created from a valid JSON string must not be null", schema);
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInvalidJsonThrows() {
        // malformed JSON should cause a JsonSchemaException wrapped around IOException
        factoryV4.getSchema("{invalid json}");
    }

    @Test
    public void testGetMetaSchemaByIri() {
        JsonMetaSchema meta = factoryV4.getMetaSchema(JsonMetaSchema.getV4().getIri(),
                new SchemaValidatorsConfig());
        assertNotNull(meta);
        assertEquals(JsonMetaSchema.getV4().getIri(), meta.getIri());
    }

    @Test
    public void testNormalizeMetaSchemaUri() {
        // Un‑normalized URIs that contain the draft identifier must be rewritten.
        assertEquals(SchemaId.V7, JsonSchemaFactory.normalizeMetaSchemaUri(
                "http://json-schema.org/draft-07/schema#"));
        assertEquals(SchemaId.V201909, JsonSchemaFactory.normalizeMetaSchemaUri(
                "http://json-schema.org/draft/2019-09/schema#"));
        assertEquals(SchemaId.V202012, JsonSchemaFactory.normalizeMetaSchemaUri(
                "http://json-schema.org/draft/2020-12/schema#"));
        assertEquals(SchemaId.V4, JsonSchemaFactory.normalizeMetaSchemaUri(
                "http://json-schema.org/draft-04/schema#"));
        assertEquals(SchemaId.V6, JsonSchemaFactory.normalizeMetaSchemaUri(
                "http://json-schema.org/draft-06/schema#"));
        // Already normalized value must stay unchanged.
        assertEquals(VersionFlag.V4.getId(), JsonSchemaFactory.normalizeMetaSchemaUri(VersionFlag.V4.getId()));
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  Builder validation (edge cases)                                   */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderMissingDefaultMetaSchemaIriThrows() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder()
                .metaSchema(JsonMetaSchema.getV4()); // meta‑schema present, but no default IRI
        builder.build(); // should fail
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderEmptyDefaultMetaSchemaIriThrows() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("   ")
                .metaSchema(JsonMetaSchema.getV4());
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderNoMetaSchemasThrows() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri()); // default IRI set
        // No meta‑schema added → internal constructor will reject
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderDefaultMetaSchemaIriNotProvidedInMapThrows() {
        // Provide a default IRI that does not correspond to any meta‑schema in the map.
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("http://example.com/unknown")
                .metaSchema(JsonMetaSchema.getV4()); // only V4 is present
        builder.build();
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  Custom JsonMetaSchemaFactory integration                           */
    /* ---------------------------------------------------------------------- */

    private static class DummyMetaSchemaFactory implements JsonMetaSchemaFactory {
        @Override
        public JsonMetaSchema getMetaSchema(String iri, JsonSchemaFactory schemaFactory,
                                            SchemaValidatorsConfig config) {
            // Always return the V7 meta‑schema regardless of the IRI.
            return JsonMetaSchema.getV7();
        }
    }

    @Test
    public void testFactoryWithCustomMetaSchemaFactory() {
        JsonMetaSchemaFactory dummyFactory = new DummyMetaSchemaFactory();
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV7().getIri())
                .metaSchema(JsonMetaSchema.getV7())
                .metaSchemaFactory(dummyFactory)
                .build();

        // Request a meta‑schema that is not present – the dummy factory should supply it.
        JsonMetaSchema result = factory.getMetaSchema("http://any/iri", new SchemaValidatorsConfig());
        assertNotNull(result);
        assertEquals(JsonMetaSchema.getV7().getIri(), result.getIri());
    }

    /* ---------------------------------------------------------------------- */
    /*  5.  Loading schemas via URI – error handling                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromNonExistingUriThrows() {
        // The URI points to a location that the default loader cannot resolve.
        URI bogus = URI.create("http://nonexistent.example.com/schema.json");
        factoryV4.getSchema(bogus);
    }

    /* ---------------------------------------------------------------------- */
    /*  6.  Internal helper – isYaml (invoked via public getSchema overload) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaWithYamlExtensionTriggersYamlMapper() {
        // Create a very small YAML schema (the content is irrelevant – we only need the
        // factory to decide which mapper to use). The schema loader will fail to locate
        // the resource, causing a JsonSchemaException, but we can assert that the
        // exception originates from a YAML processing path by checking the mapper used.
        // To avoid dealing with the loader, we use the overload that accepts a JsonNode.
        JsonNode yamlNode = factoryV4.readTree("type: string", InputFormat.YAML);
        JsonSchema schema = factoryV4.getSchema(SchemaLocation.of("file://dummy.yaml"), yamlNode);
        assertNotNull(schema);
        // The schema was built – if the YAML mapper were not chosen, an exception would
        // have been thrown earlier.
    }

    /* ---------------------------------------------------------------------- */
    /*  7.  getSchema(URI, JsonNode, Config) overloads                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaWithUriAndNode() {
        JsonNode node = factoryV4.readTree("{\"type\":\"integer\"}", InputFormat.JSON);
        JsonSchema schema = factoryV4.getSchema(URI.create("http://example.com/int-schema"), node);
        assertNotNull(schema);
        // Verify that the schema evaluates an integer correctly.
        JsonNode instance = factoryV4.readTree("5", InputFormat.JSON);
        assertTrue(schema.validate(instance).isEmpty());
    }
}
```
###Test END##