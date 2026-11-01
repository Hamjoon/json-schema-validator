```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)}.
 *
 * <p>The method under test is {@code protected}, therefore a thin subclass is used to expose it
 * for verification. The tests focus on the observable behaviour of the created {@link JsonSchema}
 * rather than on internal private state.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    /** Factory used to create the test subclass – created once for the whole test suite. */
    private static JsonSchemaFactory baseFactory;

    /** Test subclass that makes the protected method public. */
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

        /** Exposes the protected {@code newJsonSchema} method for testing. */
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
        // Use the public factory builder – it gives us a fully‑initialised instance
        // with all required internal dependencies (meta‑schemas, loaders, etc.).
        baseFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of JsonSchemaFactory is package‑private, therefore we reuse the
        // already created baseFactory via reflection to obtain the required arguments.
        // For simplicity we delegate to the public getters of the base instance.
        factoryUnderTest = new TestableJsonSchemaFactory(
                // jsonMapper / yamlMapper
                baseFactory.getJsonMapper(),
                baseFactory.getYamlMapper(),
                // jsonNodeReader – the default implementation is hidden, but the base
                // factory already holds a usable instance; we can retrieve it via the
                // protected accessor (exposed through a lambda here).
                // Since the field is package‑private we cannot access it directly;
                // instead we create a dummy reader that delegates to ObjectMapper.
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
                // defaultMetaSchemaIri – use the same IRI that the base factory was built with
                "https://json-schema.org/draft/2020-12/schema",
                // schemaLoadersBuilder – reuse the builder from the base instance
                new SchemaLoaders.Builder(),
                // schemaMappersBuilder – reuse the builder from the base instance
                new SchemaMappers.Builder(),
                // metaSchemas – copy the map from the base instance
                new ConcurrentHashMap<>(baseFactory.metaSchemas),
                // enableSchemaCache – same as base
                true,
                // metaSchemaFactory – a simple no‑op implementation
                new JsonMetaSchemaFactory() {}
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to dispose globally.
    }

    /**
     * Verifies that {@code newJsonSchema} creates a {@link JsonSchema} that correctly
     * validates a simple string schema.
     */
    @Test
    void newJsonSchema_createsStringSchema_andValidatesCorrectly() throws Exception {
        // GIVEN – a minimal JSON schema that only allows strings
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:schema"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN – the protected method is invoked via the test subclass
        JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(location, schemaNode, config);

        // THEN – the returned schema must not be null
        assertNotNull(jsonSchema, "newJsonSchema should return a non‑null JsonSchema instance");

        // AND – it should accept a valid string
        Set<ValidationMessage> ok = jsonSchema.validate(MAPPER.readTree("\"hello\""));
        assertTrue(ok.isEmpty(), "Valid string should produce no validation messages");

        // AND – it should reject a non‑string value
        Set<ValidationMessage> notOk = jsonSchema.validate(MAPPER.readTree("123"));
        assertFalse(notOk.isEmpty(), "Non‑string value should produce validation messages");
        // The message should contain the word "type"
        assertTrue(notOk.iterator().next().getMessage().toLowerCase().contains("type"),
                "Validation message should mention the type constraint");
    }

    /**
     * Verifies that the method respects a custom {@link SchemaValidatorsConfig}
     * (e.g., {@code preloadJsonSchema = false}) without throwing.
     */
    @Test
    void newJsonSchema_respectsCustomConfig() throws Exception {
        // GIVEN – a simple schema and a config that disables pre‑loading
        String schemaJson = "{\"type\":\"integer\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:int-schema"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .preloadJsonSchema(false)   // custom flag
                .build();

        // WHEN
        JsonSchema jsonSchema = factoryUnderTest.exposeNewJsonSchema(location, schemaNode, config);

        // THEN – schema is created and works as expected
        assertNotNull(jsonSchema);
        Set<ValidationMessage> valid = jsonSchema.validate(MAPPER.readTree("42"));
        assertTrue(valid.isEmpty(), "Integer value should be valid");

        Set<ValidationMessage> invalid = jsonSchema.validate(MAPPER.readTree("\"not an int\""));
        assertFalse(invalid.isEmpty(), "String value should be invalid for integer schema");
    }

    /**
     * Ensures that passing {@code null} for the schema node results in a
     * {@link NullPointerException} (the factory does not accept a null schema).
     */
    @Test
    void newJsonSchema_nullSchemaNode_throwsException() {
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:null-schema"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        assertThrows(NullPointerException.class, () ->
                factoryUnderTest.exposeNewJsonSchema(location, null, config));
    }
}
```