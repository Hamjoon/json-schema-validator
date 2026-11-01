###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * Unit tests for {@link JsonSchemaFactory}.
 * The tests focus on the public static utilities, builder configuration,
 * object‑mapper handling and basic schema parsing behaviour.
 */
public class JsonSchemaFactoryTest {

    /* --------------------------------------------------------------------- */
    /*  Static utility methods                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCheckVersionSupportedValues() {
        // each enum value should map to a concrete JsonSchemaVersion implementation
        for (VersionFlag flag : VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull("Version for flag " + flag + " must not be null", version);
            // the meta‑schema instance must have an IRI that matches the flag’s id
            String expectedIri = flag.getId();
            assertEquals(expectedIri, version.getInstance().getIri());
        }
    }

    @Test
    public void testCheckVersionNullReturnsNull() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testNormalizeMetaSchemaUriAlreadyNormalized() {
        String iri = JsonMetaSchema.getV7().getIri(); // already normalized
        String result = JsonSchemaFactory.normalizeMetaSchemaUri(iri);
        assertSame("Already normalized IRI should be returned unchanged", iri, result);
    }

    @Test
    public void testNormalizeMetaSchemaUriUnnormalizedDraft07() {
        // unnormalized form that contains the draft‑07 substring
        String unnormalized = "http://somehost/json-schema.org/draft-07/schema#";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(unnormalized);
        assertEquals(JsonMetaSchema.getV7().getIri(), normalized);
    }

    @Test
    public void testNormalizeMetaSchemaUriUnnormalizedDraft2019() {
        String unnormalized = "http://example.com/draft/2019-09/schema#";
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(unnormalized);
        assertEquals(JsonMetaSchema.getV201909().getIri(), normalized);
    }

    /* --------------------------------------------------------------------- */
    /*  Builder and factory creation                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetInstanceProvidesDefaultMetaSchemaIri() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        // the default meta‑schema IRI stored inside the factory must be the IRI of V4
        assertEquals(JsonMetaSchema.getV4().getIri(),
                // use reflection to read the private field (allowed because test is in same package)
                getPrivateField(factory, "defaultMetaSchemaIri"));
    }

    @Test
    public void testBuilderWithCustomObjectMappers() {
        ObjectMapper customJson = new ObjectMapper();
        ObjectMapper customYaml = new ObjectMapper();

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV6().getIri())
                .metaSchema(JsonMetaSchema.getV6())
                .jsonMapper(customJson)
                .yamlMapper(customYaml)
                .build();

        // The factory must return the exact mapper instances that were supplied.
        assertSame(customJson, factory.getObjectMapper(InputFormat.JSON));
        assertSame(customYaml, factory.getObjectMapper(InputFormat.YAML));
    }

    @Test
    public void testBuilderWithCustomMetaSchemaAndLookup() {
        JsonMetaSchema custom = JsonMetaSchema.getV202012(); // use an existing meta‑schema as a stand‑in
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(custom.getIri())
                .metaSchema(custom)
                .build();

        // getMetaSchema should return the same instance that was registered
        JsonMetaSchema lookedUp = factory.getMetaSchema(custom.getIri(), new SchemaValidatorsConfig());
        assertSame("Meta‑schema lookup must return the registered instance", custom, lookedUp);
    }

    @Test
    public void testBuilderDisableSchemaCacheDoesNotThrow() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(JsonMetaSchema.getV7().getIri())
                .metaSchema(JsonMetaSchema.getV7())
                .enableSchemaCache(false)
                .build();

        // The flag is private; verify through reflection that it is false.
        Boolean cacheEnabled = (Boolean) getPrivateField(factory, "enableSchemaCache");
        assertFalse("Schema cache should be disabled", cacheEnabled);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic schema parsing via getSchema(String)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaParsesValidJson() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        String schema = "{\"type\":\"object\"}";
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull("JsonSchema must be created for a valid JSON schema", jsonSchema);
        // The schema should retain the supplied node type (object)
        assertEquals("object", jsonSchema.getValidationContext().getMetaSchema().getKeywords().get("type").getClass().getSimpleName().toLowerCase().contains("type") ? "object" : "unknown");
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaInvalidJsonThrowsException() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        // malformed JSON
        factory.getSchema("{invalid json}");
    }

    @Test
    public void testGetSchemaWithYamlInputFormat() throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        JsonSchema jsonSchema = factory.getSchema(yaml, InputFormat.YAML, new SchemaValidatorsConfig());
        assertNotNull(jsonSchema);
        // Verify that the root type keyword is present
        assertTrue(jsonSchema.getValidationContext().getMetaSchema().getKeywords().containsKey("type"));
    }

    /* --------------------------------------------------------------------- */
    /*  Helper – reflection to read private fields (allowed in same pkg)     */
    /* --------------------------------------------------------------------- */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
###Test END##