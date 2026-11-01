package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    /**
     * Factory under test.
     */
    private JsonSchemaFactory factory;

    /**
     * Simple JSON schema used for the test.
     */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /**
     * IRI used to identify the test schema.
     */
    private static final String TEST_SCHEMA_IRI = "https://example.com/simple-schema";

    /**
     * A more complex schema that contains a $ref to an external definition.
     */
    private static final String EXTERNAL_REF_SCHEMA = """
        {
          "$id": "classpath:/schemas/root-with-ref.json",
          "type": "object",
          "properties": {
            "child": { "$ref": "classpath:/schemas/externally-referenced.json" }
          }
        }
        """;

    /**
     * The external definition referenced by {@link #EXTERNAL_REF_SCHEMA}.
     */
    private static final String EXTERNALLY_REFERENCED_SCHEMA = """
        {
          "$id": "classpath:/schemas/externally-referenced.json",
          "type": "string"
        }
        """;

    /**
     * YAML schema (the factory should use the YamlMapper).
     */
    private static final String YAML_SCHEMA = """
        type: object
        properties:
          name:
            type: string
        """;

    /**
     * Invalid JSON – used to provoke a parsing exception.
     */
    private static final String INVALID_JSON = "{ this is not valid JSON }";

    /**
     * Empty schema – an empty string should cause a parsing error.
     */
    private static final String EMPTY_SCHEMA = "";

    @BeforeEach
    void setupBeforeEach() {
        // Build a factory that knows how to load the simple schema from the given IRI.
        Map<String, String> schemas = Collections.singletonMap(TEST_SCHEMA_IRI, SIMPLE_SCHEMA);
        this.factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(sl -> sl.schemas(schemas)));
    }

    /**
     * Helper to build a map of IRI → schema content for the in‑memory loader.
     */
    private static Map<String, String> baseSchemas() {
        return Map.of("https://example.com/simple-schema", SIMPLE_SCHEMA, "classpath:/schemas/simple-schema.json", SIMPLE_SCHEMA, // content not important for flag test
        "classpath:/schemas/complex-schema.json", // content not important for flag test
        SIMPLE_SCHEMA, "classpath:/schemas/root-with-ref.json", EXTERNAL_REF_SCHEMA, "classpath:/schemas/externally-referenced.json", EXTERNALLY_REFERENCED_SCHEMA, "classpath:/schemas/yaml-schema.yaml", YAML_SCHEMA, "classpath:/schemas/invalid-json-schema.json", INVALID_JSON, "classpath:/schemas/empty-schema.json", EMPTY_SCHEMA, "classpath:/schemas/type-loose-schema.json", SIMPLE_SCHEMA, "classpath:/schemas/unknown-meta-schema.json", SIMPLE_SCHEMA);
    }

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(Arguments.of(SchemaLocation.of("classpath:/schemas/simple-schema.json"), new SchemaValidatorsConfig()), Arguments.of(SchemaLocation.of("classpath:/schemas/complex-schema.json"), new SchemaValidatorsConfig().setFailFast(true)));
    }

    @Test
    void getSchema_shouldReturnParsedSchema() {
        // GIVEN – a SchemaLocation pointing to the test IRI and a default validators config
        SchemaLocation location = SchemaLocation.of(TEST_SCHEMA_IRI);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // WHEN – the factory creates a JsonSchema for the given location
        JsonSchema schema = factory.getSchema(location, config);
        // THEN – a non‑null JsonSchema is returned and its underlying JsonNode matches the source JSON
        assertNotNull(schema, "JsonSchema should not be null");
        // The JsonSchema implementation exposes the original JsonNode via getSchemaNode()
        // (this is part of the public API, not a private field).
        JsonNode node = schema.getSchemaNode();
        assertNotNull(node, "Underlying JsonNode should not be null");
        assertEquals("object", node.get("type").asText(), "Schema type should be 'object'");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} ⇒ schemaUri={0}, configFailFast={1}")
    @MethodSource("normalOperationProvider")
    void normalOperation_validInputs(SchemaLocation location, SchemaValidatorsConfig config) {
        // GIVEN – factory with a simple in‑memory loader
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        // WHEN
        JsonSchema schema = factory.getSchema(location, config);
        // THEN – a non‑null schema is returned and its node matches the supplied JSON
        assertNotNull(schema, "JsonSchema must not be null");
        JsonNode node = schema.getSchemaNode();
        assertNotNull(node, "Underlying JsonNode must not be null");
        assertEquals("object", node.get("type").asText(), "The schema type should be 'object'");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullSchemaUri_throwsException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, cfg), "Null schemaUri should result in NPE");
    }

    @Test
    void nullConfig_usesDefaultConfig() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/simple-schema.json");
        JsonSchema schema = factory.getSchema(loc, null);
        assertNotNull(schema, "Schema must be created even when config is null");
        // The default config should have failFast == false
        assertFalse(schema.getValidationContext().getConfig().isFailFast(), "Default config should have failFast disabled");
    }

    @Test
    void nullSchemaUriAndConfig_throwsException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, null), "Both arguments null should result in NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonExistentSchema_throwsRuntimeException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/non-existent.json");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Missing schema should raise JsonSchemaException");
    }

    @Test
    void invalidJsonSchema_throwsException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/invalid-json-schema.json");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Invalid JSON content should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void yamlSchema_isParsedCorrectly() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/yaml-schema.yaml");
        JsonSchema schema = factory.getSchema(loc, new SchemaValidatorsConfig());
        assertNotNull(schema, "Schema must be created from YAML");
        JsonNode node = schema.getSchemaNode();
        assertEquals("object", node.get("type").asText(), "YAML schema should be parsed to an object type");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void malformedUri_throwsIllegalArgumentException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("ht!tp://invalid@@uri");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Malformed URI should raise IllegalArgumentException");
    }

    @Test
    void emptyUriString_throwsIllegalArgumentException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Empty URI string should raise IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptySchemaFile_throwsException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/empty-schema.json");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Empty schema content should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void externalRef_isResolvedDuringPreload() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/root-with-ref.json");
        JsonSchema schema = factory.getSchema(loc, new SchemaValidatorsConfig());
        assertNotNull(schema, "Schema with external $ref should be loaded");
        // The preload step resolves the $ref – we can verify by checking that the child property exists
        JsonNode node = schema.getSchemaNode();
        JsonNode child = node.get("properties").get("child");
        assertNotNull(child, "Child property should be present after $ref resolution");
        assertEquals("string", child.get("type").asText(), "Resolved external schema should have type 'string'");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cacheEnabled_returnsSameInstance() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/simple-schema.json");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setCacheEnabled(true);
        JsonSchema first = factory.getSchema(loc, cfg);
        JsonSchema second = factory.getSchema(loc, cfg);
        assertSame(first, second, "When caching is enabled the same JsonSchema instance should be returned");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void typeLooseFlag_isPreservedInCreatedSchema() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/type-loose-schema.json");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setTypeLoose(true);
        JsonSchema schema = factory.getSchema(loc, cfg);
        assertTrue(schema.getValidationContext().getConfig().isTypeLoose(), "typeLoose flag must be preserved in the schema's config");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void remoteHttpSchema_isLoadedViaMockedLoader(@Mock SchemaLoader httpLoader) throws Exception {
        // Simulate a remote HTTP response containing a simple schema
        InputStream httpContent = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes());
        Mockito.when(httpLoader.loadSchema(ArgumentMatchers.any())).thenReturn(httpContent);
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.loader(httpLoader)));
        SchemaLocation loc = SchemaLocation.of("http://example.com/schemas/remote-schema.json");
        JsonSchema schema = factory.getSchema(loc, new SchemaValidatorsConfig());
        assertNotNull(schema, "Remote HTTP schema should be loaded via mocked loader");
        JsonNode node = schema.getSchemaNode();
        assertEquals("object", node.get("type").asText(), "Remote schema content should be parsed correctly");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void concurrentAccess_returnsSameCachedInstance() throws Exception {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/simple-schema.json");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setCacheEnabled(true);
        Callable<JsonSchema> task = () -> factory.getSchema(loc, cfg);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            // Submit several concurrent calls
            var futures = executor.invokeAll(Collections.nCopies(5, task));
            JsonSchema first = futures.get(0).get();
            for (Future<JsonSchema> f : futures) {
                assertSame(first, f.get(), "All concurrent calls should receive the same cached JsonSchema instance");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void unsupportedScheme_throwsIllegalArgumentException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("ftp://example.com/schemas/schema.json");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Unsupported URI scheme should raise IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void directoryPath_throwsException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        // The map does not contain an entry for a directory – the loader will treat it as missing.
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Providing a directory path should result in a loading exception");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void missingInternalRef_throwsException() {
        // Build a schema that references a non‑existent internal definition
        String schemaWithBadRef = """
            {
              "$id": "classpath:/schemas/missing-internal-ref.json",
              "type": "object",
              "properties": {
                "bad": { "$ref": "#/definitions/NonExistent" }
              }
            }
            """;
        Map<String, String> schemas = new ConcurrentHashMap<>(baseSchemas());
        schemas.put("classpath:/schemas/missing-internal-ref.json", schemaWithBadRef);
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(schemas)));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/missing-internal-ref.json");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Missing internal $ref target should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void unknownMetaSchemaIri_throwsException() {
        // Schema declaring an unknown $schema IRI
        String unknownMetaSchema = """
            {
              "$schema": "https://example.com/unknown-meta-schema",
              "type": "object"
            }
            """;
        Map<String, String> schemas = new ConcurrentHashMap<>(baseSchemas());
        schemas.put("classpath:/schemas/unknown-meta-schema.json", unknownMetaSchema);
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(schemas)));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/unknown-meta-schema.json");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Unknown meta‑schema IRI should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void remoteHttp404_throwsException(@Mock SchemaLoader httpLoader) throws Exception {
        // Simulate a 404 error by throwing an IOException from the loader
        Mockito.when(httpLoader.loadSchema(ArgumentMatchers.any())).thenThrow(new java.io.IOException("404 Not Found"));
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.loader(httpLoader)));
        SchemaLocation loc = SchemaLocation.of("http://example.com/schemas/not-found.json");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "HTTP 404 should be wrapped as JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void deepRecursiveRef_throwsException() {
        // Simulate a deep recursive schema (the actual depth handling is internal;
        // we provoke the failure by providing a schema that the factory treats as too deep)
        String deepRecursive = """
            {
              "$id": "classpath:/schemas/deep-recursive-ref.json",
              "$ref": "classpath:/schemas/deep-recursive-ref.json"
            }
            """;
        Map<String, String> schemas = new ConcurrentHashMap<>(baseSchemas());
        schemas.put("classpath:/schemas/deep-recursive-ref.json", deepRecursive);
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(schemas)));
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/deep-recursive-ref.json");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig().setFailFast(true)), "Excessive recursion should be detected and raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void fragmentOnlyLocation_throwsIllegalArgumentException() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, b -> b.schemaLoaders(sl -> sl.schemas(baseSchemas())));
        SchemaLocation loc = SchemaLocation.of("#/definitions/MyDef");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, new SchemaValidatorsConfig()), "Fragment‑only location should raise IllegalArgumentException");
    }
}
