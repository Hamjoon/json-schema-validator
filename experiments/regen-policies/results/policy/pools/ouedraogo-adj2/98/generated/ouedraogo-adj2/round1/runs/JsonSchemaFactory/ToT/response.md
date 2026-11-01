###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests cover:
 *   • Builder validation (null/empty/default meta‑schema handling)
 *   • Factory creation via {@code getInstance}
 *   • Version flag handling
 *   • ObjectMapper selection for JSON/YAML
 *   • readTree behaviour with and without a {@link JsonNodeReader}
 *   • Schema parsing (JSON & YAML) and validation
 *   • Normalisation of meta‑schema URIs
 *   • Pre‑loading of schemas when enabled
 *
 * The test class lives in the same package as {@link JsonSchemaFactory}
 * so that package‑private static methods (e.g. {@code normalizeMetaSchemaUri})
 * can be accessed directly.
 */
public class JsonSchemaFactoryTest {

    /** Helper to create a minimal factory with the V4 meta‑schema. */
    private JsonSchemaFactory createFactoryWithV4() {
        return JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .build();
    }

    /** Helper to create a factory that uses a custom {@link JsonNodeReader}. */
    private JsonSchemaFactory createFactoryWithCustomReader() {
        JsonNodeReader reader = new JsonNodeReader() {
            private final ObjectMapper mapper = JsonMapperFactory.getInstance();

            @Override
            public JsonNode readTree(String content, InputFormat inputFormat) throws IOException {
                // ignore the format – the mapper can read both JSON and YAML strings
                return mapper.readTree(content);
            }

            @Override
            public JsonNode readTree(InputStream content, InputFormat inputFormat) throws IOException {
                return mapper.readTree(content);
            }
        };
        return JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .jsonNodeReader(reader)
                .build();
    }

    /* --------------------------------------------------------------------- */
    /*  Builder validation tests                                            */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaIriIsNull() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(null)
                .metaSchema(JsonMetaSchema.getV4())
                .build(); // should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaIriIsEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("   ")
                .metaSchema(JsonMetaSchema.getV4())
                .build(); // should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenMetaSchemasMapIsEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                // no metaSchema added → empty map
                .build(); // should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaIsNotProvidedInMap() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                // add a *different* meta‑schema, not the default one
                .metaSchema(JsonMetaSchema.getV6())
                .build(); // should throw
    }

    /* --------------------------------------------------------------------- */
    /*  Factory creation via getInstance()                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void getInstanceCreatesFactoryWithCorrectDefaultMetaSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        Assert.assertNotNull(factory);
        Assert.assertEquals(JsonMetaSchema.getV4().getIri(), factory.defaultMetaSchemaIri);
        // the default meta‑schema must be present in the internal map
        Assert.assertSame(JsonMetaSchema.getV4(),
                factory.metaSchemas.get(JsonSchemaFactory.normalizeMetaSchemaUri(JsonMetaSchema.getV4().getIri())));
    }

    /* --------------------------------------------------------------------- */
    /*  Version flag handling                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void checkVersionReturnsCorrectImplementation() {
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(VersionFlag.V7);
        Assert.assertNotNull(version);
        // the returned instance must expose the V7 meta‑schema
        Assert.assertEquals(JsonMetaSchema.getV7().getIri(),
                version.getInstance().getIri());
    }

    @Test
    public void checkVersionReturnsNullForNullFlag() {
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(null);
        Assert.assertNull(version);
    }

    /* --------------------------------------------------------------------- */
    /*  ObjectMapper selection                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void getObjectMapperReturnsJsonMapperForJsonFormat() {
        JsonSchemaFactory factory = createFactoryWithV4();
        ObjectMapper mapper = factory.getObjectMapper(InputFormat.JSON);
        Assert.assertNotNull(mapper);
        // The default JSON mapper must be the one supplied by JsonMapperFactory
        Assert.assertSame(JsonMapperFactory.getInstance(), mapper);
    }

    @Test
    public void getObjectMapperReturnsYamlMapperForYamlFormat() {
        JsonSchemaFactory factory = createFactoryWithV4();
        ObjectMapper mapper = factory.getObjectMapper(InputFormat.YAML);
        Assert.assertNotNull(mapper);
        // The default YAML mapper must be the one supplied by YamlMapperFactory
        Assert.assertSame(YamlMapperFactory.getInstance(), mapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getObjectMapperThrowsForUnsupportedFormat() {
        JsonSchemaFactory factory = createFactoryWithV4();
        // There is no third format – provoke the exception
        factory.getObjectMapper(null);
    }

    /* --------------------------------------------------------------------- */
    /*  readTree without a JsonNodeReader (uses ObjectMapper)               */
    /* --------------------------------------------------------------------- */

    @Test
    public void readTreeParsesJsonStringWhenNoReaderIsConfigured() throws IOException {
        JsonSchemaFactory factory = createFactoryWithV4();
        String json = "{\"type\":\"object\"}";
        JsonNode node = factory.readTree(json, InputFormat.JSON);
        Assert.assertNotNull(node);
        Assert.assertEquals("object", node.get("type").asText());
    }

    @Test
    public void readTreeParsesJsonInputStreamWhenNoReaderIsConfigured() throws IOException {
        JsonSchemaFactory factory = createFactoryWithV4();
        String json = "{\"type\":\"string\"}";
        try (InputStream in = IOUtils.toInputStream(json, "UTF-8")) {
            JsonNode node = factory.readTree(in, InputFormat.JSON);
            Assert.assertEquals("string", node.get("type").asText());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  readTree with a custom JsonNodeReader                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void readTreeUsesCustomJsonNodeReaderWhenProvided() throws IOException {
        JsonSchemaFactory factory = createFactoryWithCustomReader();
        String json = "{\"type\":\"number\"}";
        JsonNode node = factory.readTree(json, InputFormat.JSON);
        Assert.assertEquals("number", node.get("type").asText());
    }

    /* --------------------------------------------------------------------- */
    /*  Schema parsing and validation (JSON)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromJsonStringParsesAndValidatesSuccessfully() {
        JsonSchemaFactory factory = createFactoryWithV4();
        String schema = "{\"type\":\"object\"}";
        JsonSchema jsonSchema = factory.getSchema(schema);
        Assert.assertNotNull(jsonSchema);

        // Valid instance – should produce an empty validation result set
        Set<ValidationMessage> messages = jsonSchema.validate("{\"a\":1}");
        Assert.assertTrue(messages.isEmpty());
    }

    @Test
    public void getSchemaFromJsonStringDetectsInvalidInstance() {
        JsonSchemaFactory factory = createFactoryWithV4();
        String schema = "{\"type\":\"object\",\"required\":[\"name\"]}";
        JsonSchema jsonSchema = factory.getSchema(schema);
        Set<ValidationMessage> messages = jsonSchema.validate("{\"age\":30}");
        Assert.assertFalse(messages.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Schema parsing and validation (YAML)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromYamlStringParsesAndValidatesSuccessfully() {
        JsonSchemaFactory factory = createFactoryWithV4();
        String yaml = "type: object\nproperties:\n  id:\n    type: integer\nrequired: [id]";
        JsonSchema jsonSchema = factory.getSchema(yaml, InputFormat.YAML, new SchemaValidatorsConfig());
        Assert.assertNotNull(jsonSchema);
        Set<ValidationMessage> messages = jsonSchema.validate("{\"id\":10}");
        Assert.assertTrue(messages.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Normalisation of meta‑schema URIs                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void normalizeMetaSchemaUriConvertsUnnormalisedDraft07ToStandard() {
        String unnormalised = "http://json-schema.org/draft-07/schema#";
        String normalised = JsonSchemaFactory.normalizeMetaSchemaUri(unnormalised);
        Assert.assertEquals(JsonMetaSchema.getV7().getIri(), normalised);
    }

    @Test
    public void normalizeMetaSchemaUriLeavesAlreadyNormalisedUriUntouched() {
        String alreadyNormal = JsonMetaSchema.getV201909().getIri();
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(alreadyNormal);
        Assert.assertSame(alreadyNormal, result);
    }

    /* --------------------------------------------------------------------- */
    /*  Pre‑loading of schemas when configuration requests it               */
    /* --------------------------------------------------------------------- */

    @Test
    public void preloadIsExecutedWhenConfigRequestsIt() {
        JsonSchemaFactory factory = createFactoryWithV4();
        String schema = "{\"type\":\"object\"}";
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setPreloadJsonSchema(true);   // request pre‑load
        // No exception must be thrown – initialiseValidators is called internally
        JsonSchema jsonSchema = factory.getSchema(schema, config);
        Assert.assertNotNull(jsonSchema);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper to create an InputStream from a String (used in readTree test)*/
    /* --------------------------------------------------------------------- */
    private static class IOUtils {
        static InputStream toInputStream(String content, String charset) {
            return new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.Charset.forName(charset)));
        }
    }
}
###Test END##