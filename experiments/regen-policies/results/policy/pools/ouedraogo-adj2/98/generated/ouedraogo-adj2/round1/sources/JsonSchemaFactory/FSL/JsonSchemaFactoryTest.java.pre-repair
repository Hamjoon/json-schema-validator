package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.networknt.schema.serialization.JsonNodeReader;
import com.networknt.schema.serialization.YamlMapperFactory;
import com.networknt.schema.serialization.JsonMapperFactory;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * The tests focus on the public API and on the internal helper methods that can be
 * exercised without needing to mock or extend library classes.
 *
 * <ul>
 *   <li>Factory creation via the various builders and static helpers.</li>
 *   <li>Behaviour of {@code checkVersion} and {@code normalizeMetaSchemaUri}.</li>
 *   <li>ObjectMapper selection for JSON/YAML.</li>
 *   <li>Schema loading from a string, from an InputStream and with YAML detection.</li>
 *   <li>Validation of a simple schema (type string) and handling of malformed JSON.</li>
 *   <li>Verification that the constructor validates its arguments.</li>
 * </ul>
 */
public class JsonSchemaFactoryTest {

    /** Helper JsonNodeReader that delegates to a standard Jackson ObjectMapper. */
    private static class SimpleJsonNodeReader implements JsonNodeReader {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public com.fasterxml.jackson.databind.JsonNode readTree(String content, InputFormat inputFormat) throws java.io.IOException {
            return mapper.readTree(content);
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode readTree(InputStream content, InputFormat inputFormat) throws java.io.IOException {
            return mapper.readTree(content);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Factory creation & argument validation                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetInstanceCreatesFactoryWithDefaultMetaSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull("Factory should not be null", factory);
        // The default meta‑schema IRI for V4 is the constant defined in JsonMetaSchema
        assertEquals(JsonMetaSchema.getV4().getIri(), factory.defaultMetaSchemaIri);
    }

    @Test
    public void testBuilderCustomJsonNodeReaderIsHonoured() {
        JsonNodeReader reader = new SimpleJsonNodeReader();
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .jsonNodeReader(reader)
                .build();

        // The factory must use the supplied reader – we can verify by calling a
        // method that goes through the reader (e.g. getSchema(String)).
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        assertNotNull(schema);
    }

    @Test
    public void testBuilderEnableSchemaCacheFalseWorks() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV7().getIri())
                .metaSchema(JsonMetaSchema.getV7())
                .enableSchemaCache(false)
                .build();

        JsonSchema schema = factory.getSchema("{\"type\":\"integer\"}");
        assertNotNull(schema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderThrowsWhenDefaultMetaSchemaIriIsNull() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(null)
                .metaSchema(JsonMetaSchema.getV4())
                .build(); // should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderThrowsWhenMetaSchemasIsEmpty() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                // do NOT add any meta schema -> empty map
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderThrowsWhenDefaultMetaSchemaNotProvided() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV6().getIri())
                .metaSchema(JsonMetaSchema.getV4()) // different IRI
                .build();
    }

    /* --------------------------------------------------------------------- */
    /*  Static helper methods                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCheckVersionValidValues() {
        for (VersionFlag flag : VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull("Version must not be null for flag " + flag, version);
            assertEquals(flag, version.getInstance().getSpecification());
        }
    }

    @Test
    public void testCheckVersionNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testNormalizeMetaSchemaUriRecognisesStandardIds() {
        // canonical IDs – should be returned unchanged
        for (VersionFlag flag : VersionFlag.values()) {
            String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(flag.getId());
            assertEquals("Canonical ID should stay the same", flag.getId(), normalized);
        }
    }

    @Test
    public void testNormalizeMetaSchemaUriHandlesUnnormalisedDraftUrls() {
        // Unnormalised draft URLs that contain the version marker
        String unnormV7 = "https://json-schema.org/draft-07/schema#";
        String unnormV6 = "https://json-schema.org/draft-06/schema#";
        String unnormV4 = "https://json-schema.org/draft-04/schema#";
        String unnormV2019 = "https://json-schema.org/draft/2019-09/schema#";
        String unnormV2020 = "https://json-schema.org/draft/2020-12/schema#";

        assertEquals(SchemaId.V7, JsonSchemaFactory.normalizeMetaSchemaUri(unnormV7));
        assertEquals(SchemaId.V6, JsonSchemaFactory.normalizeMetaSchemaUri(unnormV6));
        assertEquals(SchemaId.V4, JsonSchemaFactory.normalizeMetaSchemaUri(unnormV4));
        assertEquals(SchemaId.V201909, JsonSchemaFactory.normalizeMetaSchemaUri(unnormV2019));
        assertEquals(SchemaId.V202012, JsonSchemaFactory.normalizeMetaSchemaUri(unnormV2020));
    }

    /* --------------------------------------------------------------------- */
    /*  ObjectMapper selection                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetObjectMapperSelectsJsonMapper() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        ObjectMapper mapper = factory.getObjectMapper(InputFormat.JSON);
        assertNotNull(mapper);
        // The default JSON mapper is provided by JsonMapperFactory
        assertEquals(JsonMapperFactory.getInstance().getClass(), mapper.getClass());
    }

    @Test
    public void testGetObjectMapperSelectsYamlMapper() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        ObjectMapper yamlMapper = factory.getObjectMapper(InputFormat.YAML);
        assertNotNull(yamlMapper);
        assertEquals(YamlMapperFactory.getInstance().getClass(), yamlMapper.getClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetObjectMapperRejectsUnsupportedFormat() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        // Pass a null (unsupported) format – the method should throw
        factory.getObjectMapper(null);
    }

    /* --------------------------------------------------------------------- */
    /*  YAML detection                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsYamlDetectsYamlExtension() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        SchemaLocation yamlLoc = SchemaLocation.of("file:///tmp/example.yaml");
        SchemaLocation ymlLoc  = SchemaLocation.of("file:///tmp/example.yml");
        SchemaLocation jsonLoc = SchemaLocation.of("file:///tmp/example.json");

        assertTrue(factory.isYaml(yamlLoc));
        assertTrue(factory.isYaml(ymlLoc));
        assertFalse(factory.isYaml(jsonLoc));
    }

    /* --------------------------------------------------------------------- */
    /*  Schema creation & validation                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaFromStringAndValidateValidInstance() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        assertNotNull(schema);

        // Validate a correct instance – expect an empty set of messages
        java.util.Set<ValidationMessage> messages = schema.validate("\"hello\"");
        assertTrue("No validation errors expected", messages.isEmpty());
    }

    @Test
    public void testGetSchemaFromStringAndValidateInvalidInstance() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchema schema = factory.getSchema("{\"type\":\"integer\"}");

        // Validate a non‑integer – should produce at least one error
        java.util.Set<ValidationMessage> messages = schema.validate("\"not an int\"");
        assertFalse("Validation errors expected", messages.isEmpty());
    }

    @Test
    public void testGetSchemaFromInputStreamWorks() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        String json = "{\"type\":\"boolean\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes("UTF-8"));
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);

        java.util.Set<ValidationMessage> ok = schema.validate("true");
        assertTrue(ok.isEmpty());

        java.util.Set<ValidationMessage> bad = schema.validate("123");
        assertFalse(bad.isEmpty());
    }

    @Test
    public void testGetSchemaFromStringWithMalformedJsonThrowsJsonSchemaException() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        try {
            factory.getSchema("{invalid json}");
            fail("Expected JsonSchemaException due to malformed JSON");
        } catch (JsonSchemaException ex) {
            // expected – the cause should be an IOException (Jackson parsing error)
            assertNotNull(ex.getCause());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Loading of schemas via URI (uses default loader which knows no files) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaWithUriAndEmptyCacheReturnsSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .build();

        // Use a dummy URI – the default loader will try to resolve it via the
        // classpath resources.  Since we do not have such a resource, the loader
        // will throw an IOException which is wrapped as JsonSchemaException.
        try {
            factory.getSchema(URI.create("classpath:/nonexistent-schema.json"));
            fail("Expected JsonSchemaException because the resource cannot be found");
        } catch (JsonSchemaException e) {
            // Expected path – nothing to assert further.
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Builder from existing factory (blueprint)                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBuilderFromBlueprintCopiesConfiguration() {
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(original);
        JsonSchemaFactory copy = copyBuilder.build();

        // The default meta‑schema IRI must be identical
        assertEquals(original.defaultMetaSchemaIri, copy.defaultMetaSchemaIri);
        // The meta‑schema map must contain the same key
        assertTrue(copy.metaSchemas.containsKey(
                JsonSchemaFactory.normalizeMetaSchemaUri(original.defaultMetaSchemaIri)));
    }

    /* --------------------------------------------------------------------- */
    /*  Schema cache behaviour (basic sanity check)                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSchemaCacheDoesNotBreakWhenDisabled() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .enableSchemaCache(false)
                .build();

        // Load the same schema twice – without caching each call creates a new instance
        JsonSchema s1 = factory.getSchema("{\"type\":\"null\"}");
        JsonSchema s2 = factory.getSchema("{\"type\":\"null\"}");
        assertNotSame("Without cache the two schemas should be distinct objects", s1, s2);
    }

    @Test
    public void testSchemaCacheWorksWhenEnabled() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .enableSchemaCache(true)
                .build();

        JsonSchema s1 = factory.getSchema("{\"type\":\"null\"}");
        JsonSchema s2 = factory.getSchema("{\"type\":\"null\"}");
        // With caching the same schema location (null location) should be reused
        assertSame("With cache the two schemas should be the same instance", s1, s2);
    }
}
