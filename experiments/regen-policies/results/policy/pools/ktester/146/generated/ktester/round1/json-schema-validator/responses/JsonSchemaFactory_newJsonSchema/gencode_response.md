```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)}.
 *
 * <p>The method under test is {@code protected}, therefore a thin subclass {@code TestableJsonSchemaFactory}
 * is used to expose it. The tests focus on the observable behaviour of the created {@link JsonSchema}
 * and on the method's handling of edge‑case arguments (nulls, custom config, malformed inputs, etc.).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    /** Factory used to create a fully‑initialised base instance (once per test run). */
    private static JsonSchemaFactory baseFactory;

    /**
     * Test subclass that makes the protected {@code newJsonSchema} method public.
     * All other behaviour is delegated to the real implementation.
     */
    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {
        TestableJsonSchemaFactory(ObjectMapper jsonMapper,
                                 ObjectMapper yamlMapper,
                                 JsonNodeReader jsonNodeReader,
                                 String defaultMetaSchemaIri,
                                 SchemaLoaders.Builder schemaLoadersBuilder,
                                 SchemaMappers.Builder schemaMappersBuilder,
                                 ConcurrentHashMap<String, JsonMetaSchema> metaSchemas,
                                 boolean enableSchemaCache,
                                 JsonMetaSchemaFactory metaSchemaFactory) {
            super(jsonMapper, yamlMapper, jsonNodeReader, defaultMetaSchemaIri,
                  schemaLoadersBuilder, schemaMappersBuilder, metaSchemas,
                  enableSchemaCache, metaSchemaFactory);
        }

        /** Exposes the protected method for testing. */
        JsonSchema exposeNewJsonSchema(SchemaLocation schemaLocation,
                                      JsonNode schemaNode,
                                      SchemaValidatorsConfig config) {
            return super.newJsonSchema(schemaLocation, schemaNode, config);
        }
    }

    /** Instance of the testable factory used in each test method. */
    private TestableJsonSchemaFactory factoryUnderTest;

    /** Simple {@link ObjectMapper} used to build JSON nodes for the tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain a ready‑made factory for the 2020‑12 draft.
        baseFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Re‑use the internal components of the base factory via its public getters.
        factoryUnderTest = new TestableJsonSchemaFactory(
                baseFactory.getJsonMapper(),
                baseFactory.getYamlMapper(),
                // A minimal JsonNodeReader delegating to the shared ObjectMapper.
                new JsonNodeReader() {
                    @Override
                    public JsonNode readTree(String content) throws IOException {
                        return MAPPER.readTree(content);
                    }

                    @Override
                    public JsonNode readTree(java.io.InputStream content) throws IOException {
                        return MAPPER.readTree(content);
                    }
                },
                "https://json-schema.org/draft/2020-12/schema",
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                new ConcurrentHashMap<>(baseFactory.metaSchemas),
                true,
                new JsonMetaSchemaFactory() {}
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to dispose globally.
    }

    /* ---------------------------------------------------------------------- */
    /*  Existing happy‑path tests (kept unchanged)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    void newJsonSchema_createsStringSchema_andValidatesCorrectly() throws Exception {
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:schema"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(location, schemaNode, config);

        assertNotNull(jsonSchema, "newJsonSchema should return a non‑null JsonSchema instance");

        Set<ValidationMessage> ok = jsonSchema.validate(MAPPER.readTree("\"hello\""));
        assertTrue(ok.isEmpty(), "Valid string should produce no validation messages");

        Set<ValidationMessage> notOk = jsonSchema.validate(MAPPER.readTree("123"));
        assertFalse(notOk.isEmpty(), "Non‑string value should produce validation messages");
        assertTrue(notOk.iterator().next().getMessage().toLowerCase().contains("type"),
                "Validation message should mention the type constraint");
    }

    @Test
    void newJsonSchema_respectsCustomConfig() throws Exception {
        String schemaJson = "{\"type\":\"integer\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:int-schema"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .preloadJsonSchema(false)   // custom flag
                .build();

        JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(location, schemaNode, config);

        assertNotNull(jsonSchema);
        Set<ValidationMessage> valid = jsonSchema.validate(MAPPER.readTree("42"));
        assertTrue(valid.isEmpty(), "Integer value should be valid");

        Set<ValidationMessage> invalid = jsonSchema.validate(MAPPER.readTree("\"not an int\""));
        assertFalse(invalid.isEmpty(), "String value should be invalid for integer schema");
    }

    @Test
    void newJsonSchema_nullSchemaNode_throwsException() {
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:null-schema"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        assertThrows(NullPointerException.class, () ->
                factoryUnderTest.exposeNewJsonSchema(location, null, config));
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional test groups derived from the supplied JSON specification   */
    /* ---------------------------------------------------------------------- */

    @Nested
    class NullConfigTests {

        /** Verifies that a {@code null} config is replaced by the factory's default configuration. */
        @Test
        void nullConfigFallsBackToDefault() throws Exception {
            String schemaJson = "{\"type\":\"boolean\"}";
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            SchemaLocation location = SchemaLocation.of(URI.create("urn:test:null-config"));

            // Pass null as the config argument.
            JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(location, schemaNode, null);

            assertNotNull(jsonSchema, "Schema should be created even when config is null");
            Set<ValidationMessage> ok = jsonSchema.validate(MAPPER.readTree("true"));
            assertTrue(ok.isEmpty(), "Valid boolean should pass validation");
        }
    }

    @Nested
    class NullSchemaLocationTests {

        /** The factory should tolerate a {@code null} schema location. */
        @Test
        void nullSchemaLocationIsAccepted() throws Exception {
            String schemaJson = "{\"type\":\"null\"}";
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

            JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(null, schemaNode, config);

            assertNotNull(jsonSchema, "Schema should be created even when schemaLocation is null");
            Set<ValidationMessage> ok = jsonSchema.validate(MAPPER.readTree("null"));
            assertTrue(ok.isEmpty(), "null value should be valid for type null");
        }

        /** Both schemaLocation and config are null – should still produce a usable schema. */
        @Test
        void bothLocationAndConfigNull() throws Exception {
            String schemaJson = "{\"type\":\"number\"}";
            JsonNode schemaNode = MAPPER.readTree(schemaJson);

            JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(null, schemaNode, null);

            assertNotNull(jsonSchema);
            Set<ValidationMessage> ok = jsonSchema.validate(MAPPER.readTree("3.14"));
            assertTrue(ok.isEmpty(), "Number should be valid");
        }
    }

    @Nested
    class CustomPathTypeTests {

        /** When the config specifies a custom PathType, the created JsonSchema must still work. */
        @Test
        void customPathTypeIsHonoured() throws Exception {
            String schemaJson = "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}";
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            SchemaLocation location = SchemaLocation.of(URI.create("urn:test:path-type"));
            // Use JSON_POINTER as the path type (the default is JSON_PATH)
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                    .pathType(SchemaValidatorsConfig.PathType.JSON_POINTER)
                    .build();

            JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(location, schemaNode, config);

            assertNotNull(jsonSchema);
            // Valid array
            Set<ValidationMessage> ok = jsonSchema.validate(MAPPER.readTree("[\"a\",\"b\"]"));
            assertTrue(ok.isEmpty(), "Valid array of strings should pass");
            // Invalid array (contains a number)
            Set<ValidationMessage> notOk = jsonSchema.validate(MAPPER.readTree("[\"a\",1]"));
            assertFalse(notOk.isEmpty(), "Array containing non‑string should fail");
        }
    }

    @Nested
    class InvalidInputsTests {

        /** Passing a non‑object JsonNode (e.g., a primitive) should result in IllegalArgumentException. */
        @Test
        void nonObjectRootNodeThrows() throws Exception {
            // Primitive node – a simple string literal
            JsonNode primitiveNode = MAPPER.readTree("\"just a string\"");
            SchemaLocation location = SchemaLocation.of(URI.create("urn:test:primitive"));
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

            assertThrows(IllegalArgumentException.class, () ->
                    factoryUnderTest.exposeNewJsonSchema(location, primitiveNode, config));
        }

        /** An unsupported $schema identifier must be rejected with IllegalArgumentException. */
        @Test
        void unsupportedMetaSchemaIdentifierThrows() throws Exception {
            String schemaJson = "{ \"$schema\": \"http://example.com/unsupported\", \"type\": \"object\" }";
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            SchemaLocation location = SchemaLocation.of(URI.create("urn:test:unsupported"));
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

            assertThrows(IllegalArgumentException.class, () ->
                    factoryUnderTest.exposeNewJsonSchema(location, schemaNode, config));
        }

        /** A config whose path type is {@code null} should cause a NullPointerException when constructing JsonNodePath. */
        @Test
        void configWithNullPathTypeThrows() throws Exception {
            String schemaJson = "{\"type\":\"string\"}";
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            SchemaLocation location = SchemaLocation.of(URI.create("urn:test:null-pathtype"));

            // Build a config normally, then use reflection to set the internal pathType field to null.
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
            Field pathTypeField = SchemaValidatorsConfig.class.getDeclaredField("pathType");
            pathTypeField.setAccessible(true);
            pathTypeField.set(config, null);

            assertThrows(NullPointerException.class, () ->
                    factoryUnderTest.exposeNewJsonSchema(location, schemaNode, config));
        }

        /** An obviously malformed URI used for SchemaLocation should trigger IllegalArgumentException. */
        @Test
        void malformedSchemaLocationThrows() throws Exception {
            // The URI contains illegal characters that cause the factory's normalisation to fail.
            SchemaLocation malformedLocation = SchemaLocation.of(URI.create("invalid://@@@"));
            String schemaJson = "{\"type\":\"string\"}";
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

            assertThrows(IllegalArgumentException.class, () ->
                    factoryUnderTest.exposeNewJsonSchema(malformedLocation, schemaNode, config));
        }
    }
}
```