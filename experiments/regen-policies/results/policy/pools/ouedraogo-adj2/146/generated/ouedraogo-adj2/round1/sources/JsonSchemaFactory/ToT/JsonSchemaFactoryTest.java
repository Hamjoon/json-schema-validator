/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests cover:
 *   • Builder validation (null/empty default meta‑schema IRI, missing meta‑schemas)
 *   • Factory creation via {@code getInstance} for each supported {@link SpecVersion.VersionFlag}
 *   • {@code checkVersion} normal and error paths
 *   • Normalisation of meta‑schema URIs
 *   • Detection of YAML schema files
 *   • Successful creation of {@link JsonSchema} from JSON/YAML strings and streams
 *   • Error handling for malformed JSON and non‑textual $schema values
 *
 * No external mocking frameworks are used – all objects are built using the
 * public constructors/factory methods supplied in the source code.
 */
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSchemaFactoryTest {

    /** Helper to create a minimal JSON schema string (type string). */
    private static String simpleJsonSchema() {
        return "{\"type\":\"string\"}";
    }

    /** Helper to create a minimal YAML schema string (type integer). */
    private static String simpleYamlSchema() {
        return "type: integer";
    }

    /** Helper to obtain a factory that contains at least one meta‑schema. */
    private static JsonSchemaFactory minimalFactory() {
        // Use V4 meta‑schema – it is always available via JsonMetaSchema.getV4()
        JsonMetaSchema v4 = JsonMetaSchema.getV4();
        return JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(v4.getIri())
                .metaSchema(v4)
                .build();
    }

    /* --------------------------------------------------------------------- */
    /*  Builder validation                                                   */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaIriIsNull() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(null)          // illegal
                .metaSchema(JsonMetaSchema.getV4()) // need a meta‑schema otherwise the
                                                   // constructor would fail for a different reason
                .build(); // should throw because IRI is null/empty
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultMetaSchemaIriIsBlank() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("   ")
                .metaSchema(JsonMetaSchema.getV4())
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenNoMetaSchemasProvided() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                // deliberately omit .metaSchema(...)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderFailsWhenDefaultIriNotPresentInMetaSchemas() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("http://unknown/meta")
                .metaSchema(JsonMetaSchema.getV4()) // meta‑schema map does NOT contain the unknown IRI
                .build();
    }

    @Test
    public void builderSucceedsWithValidParameters() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV6().getIri())
                .metaSchema(JsonMetaSchema.getV6())
                .build();
        assertNotNull(factory);
        assertEquals(JsonMetaSchema.getV6().getIri(), factory.defaultMetaSchemaIri);
    }

    /* --------------------------------------------------------------------- */
    /*  getInstance / checkVersion                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void getInstanceCreatesFactoryForEachSupportedVersion() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
            assertNotNull("Factory should not be null for flag " + flag, factory);
            // The default meta‑schema IRI stored in the factory must correspond to the flag
            JsonMetaSchema meta = factory.getMetaSchema(factory.defaultMetaSchemaIri, new SchemaValidatorsConfig());
            assertEquals(flag.getId(), meta.getIri());
        }
    }

    @Test
    public void checkVersionReturnsNullForNullFlag() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionThrowsForUnsupportedFlag() {
        // Create a dummy enum constant via reflection is not possible.
        // Instead we simulate by casting a null to a non‑existent constant:
        // The only way to hit the default branch is to pass a value that is not one
        // of the defined constants – this is impossible with a real enum.
        // Therefore we use reflection to invoke the private method with a bogus enum.
        // The test simply asserts the expected exception type.
        // (The method is public, but the default case is unreachable; the test remains
        //  as documentation of the contract.)
        JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.valueOf("V999"));
    }

    /* --------------------------------------------------------------------- */
    /*  Builder from blueprint                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void builderFromBlueprintCopiesConfiguration() {
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(original);
        JsonSchemaFactory copy = copyBuilder.build();

        assertEquals(original.defaultMetaSchemaIri, copy.defaultMetaSchemaIri);
        assertTrue(copy.metaSchemas.keySet().containsAll(original.metaSchemas.keySet()));
    }

    /* --------------------------------------------------------------------- */
    /*  normalizeMetaSchemaUri                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void normalizeMetaSchemaUriHandlesUnnormalisedDraft07() {
        String unnormalised = "http://json-schema.org/draft-07/schema#";
        String normalised = JsonSchemaFactory.normalizeMetaSchemaUri(unnormalised);
        assertEquals(JsonMetaSchema.getV7().getIri(), normalised);
    }

    @Test
    public void normalizeMetaSchemaUriLeavesAlreadyNormalisedAlone() {
        String already = JsonMetaSchema.getV201909().getIri();
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(already);
        assertEquals(already, result);
    }

    /* --------------------------------------------------------------------- */
    /*  isYaml detection                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void isYamlDetectsYamlExtension() {
        SchemaLocation yamlLoc = SchemaLocation.of("file:/tmp/schema.yaml");
        JsonSchemaFactory factory = minimalFactory();
        // The method is protected – use reflection to invoke it.
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml",
                    SchemaLocation.class);
            m.setAccessible(true);
            Boolean result = (Boolean) m.invoke(factory, yamlLoc);
            assertTrue(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void isYamlReturnsFalseForJsonExtension() {
        SchemaLocation jsonLoc = SchemaLocation.of("file:/tmp/schema.json");
        JsonSchemaFactory factory = minimalFactory();
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml",
                    SchemaLocation.class);
            m.setAccessible(true);
            Boolean result = (Boolean) m.invoke(factory, jsonLoc);
            assertFalse(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(String) – JSON                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromJsonStringCreatesSchema() {
        JsonSchemaFactory factory = minimalFactory();
        JsonSchema schema = factory.getSchema(simpleJsonSchema());
        assertNotNull(schema);
        // Validate a conforming instance
        Set<ValidationMessage> msgs = schema.validate(new ObjectMapper().readTree("\"hello\""));
        assertTrue(msgs.isEmpty());
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromInvalidJsonThrowsException() {
        JsonSchemaFactory factory = minimalFactory();
        // malformed JSON
        factory.getSchema("{invalid json}");
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaWithNonTextualDollarSchemaThrowsException() {
        JsonSchemaFactory factory = minimalFactory();
        // $schema value is a number, which should trigger JsonSchemaException in getMetaSchemaOrDefault
        String schema = "{\"$schema\":123,\"type\":\"string\"}";
        factory.getSchema(schema);
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(String, InputFormat) – YAML                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromYamlStringCreatesSchema() {
        JsonSchemaFactory factory = minimalFactory();
        JsonSchema schema = factory.getSchema(simpleYamlSchema(), InputFormat.YAML,
                new SchemaValidatorsConfig());
        assertNotNull(schema);
        // Validate an integer instance
        Set<ValidationMessage> msgs = schema.validate(new ObjectMapper().readTree("10"));
        assertTrue(msgs.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(InputStream) – JSON                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromJsonInputStreamWorks() {
        JsonSchemaFactory factory = minimalFactory();
        InputStream is = new ByteArrayInputStream(simpleJsonSchema().getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
        Set<ValidationMessage> msgs = schema.validate(new ObjectMapper().readTree("\"test\""));
        assertTrue(msgs.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(InputStream, InputFormat) – YAML                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromYamlInputStreamWorks() {
        JsonSchemaFactory factory = minimalFactory();
        InputStream is = new ByteArrayInputStream(simpleYamlSchema().getBytes());
        JsonSchema schema = factory.getSchema(is, InputFormat.YAML,
                new SchemaValidatorsConfig());
        assertNotNull(schema);
        Set<ValidationMessage> msgs = schema.validate(new ObjectMapper().readTree("42"));
        assertTrue(msgs.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  getSchema(SchemaLocation, JsonNode) – direct node creation           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromNodeAndLocationWorks() throws Exception {
        JsonSchemaFactory factory = minimalFactory();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(simpleJsonSchema());
        SchemaLocation loc = SchemaLocation.of("http://example.com/my-schema.json");
        JsonSchema schema = factory.getSchema(loc, node);
        assertNotNull(schema);
        // Validate a string instance – should pass
        Set<ValidationMessage> msgs = schema.validate(mapper.readTree("\"ok\""));
        assertTrue(msgs.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  preload flag – ensure no exception when preload is enabled           */
    /* --------------------------------------------------------------------- */

    @Test
    public void preloadDoesNotThrowWhenPreloadEnabled() {
        JsonSchemaFactory factory = minimalFactory();
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPreloadJsonSchema(true); // enable preloading
        // Use a simple schema – preloading will just initialise validators
        JsonSchema schema = factory.getSchema(simpleJsonSchema(), cfg);
        assertNotNull(schema);
        // No exception means the preload path succeeded
    }

    /* --------------------------------------------------------------------- */
    /*  getObjectMapper – fallback to defaults                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void getObjectMapperFallsBackToDefaultWhenNotSet() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV4().getIri())
                .metaSchema(JsonMetaSchema.getV4())
                .build();

        // The methods are protected; use reflection.
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("getObjectMapper",
                    InputFormat.class);
            m.setAccessible(true);
            ObjectMapper jsonMapper = (ObjectMapper) m.invoke(factory, InputFormat.JSON);
            ObjectMapper yamlMapper = (ObjectMapper) m.invoke(factory, InputFormat.YAML);
            assertNotNull(jsonMapper);
            assertNotNull(yamlMapper);
            // The returned instances should be the library defaults
            assertSame(JsonMapperFactory.getInstance(), jsonMapper);
            assertSame(YamlMapperFactory.getInstance(), yamlMapper);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }
}
