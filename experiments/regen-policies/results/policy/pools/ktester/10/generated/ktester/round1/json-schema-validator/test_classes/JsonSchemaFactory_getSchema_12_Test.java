package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_12_Test {

    // ----- shared test data -------------------------------------------------
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    // ----- collaborators -----------------------------------------------------
    @Mock
    private ObjectMapper // from com.fasterxml.jackson.databind
    jsonMapperMock;

    @Mock
    private ObjectMapper // from com.fasterxml.jackson.databind
    yamlMapperMock;

    private static JsonSchemaFactory factory;

    private static SchemaLoaders.Builder schemaLoadersBuilder;

    private static SchemaMappers.Builder schemaMappersBuilder;

    private static Map<String, JsonMetaSchema> metaSchemas;

    // ------------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // Builders have public no‑arg constructors, no special configuration needed.
        schemaLoadersBuilder = new SchemaLoaders.Builder();
        schemaMappersBuilder = new SchemaMappers.Builder();
        // Prepare a minimal meta‑schema map – the factory only checks that the
        // default meta‑schema URI is present.  A simple mock is sufficient.
        JsonMetaSchema mockMetaSchema = mock(JsonMetaSchema.class);
        metaSchemas = new HashMap<>();
        metaSchemas.put(DEFAULT_META_SCHEMA_URI, mockMetaSchema);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create the factory instance with the mocked ObjectMappers and the
        // minimal meta‑schema map.  The boolean flag disables URI‑schema caching,
        // which is irrelevant for this unit test.
        factory = new JsonSchemaFactory(jsonMapperMock, yamlMapperMock, DEFAULT_META_SCHEMA_URI, schemaLoadersBuilder, schemaMappersBuilder, metaSchemas, false);
    }

    // Helper to build a JsonNode from a literal JSON string using a real mapper.
    private static JsonNode node(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 1. Valid inputs – happy path
    static Stream<Arguments> happyPathProvider() {
        return Stream.of(// case 1 – all non‑null, meta‑schema should be default
        Arguments.of(SchemaLocation.of("classpath:/schemas/person-schema.json"), node("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig(), DEFAULT_META_SCHEMA_URI, "non‑null JsonSchema instance whose meta‑schema matches the supplied schema"), // case 2 – config is null, factory must fall back to default config
        Arguments.of(SchemaLocation.of("http://example.com/schema"), node("{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}"), null, DEFAULT_META_SCHEMA_URI, "non‑null JsonSchema instance (default configuration applied)"));
    }

    // 2. Null arguments validation
    static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(Arguments.of(null, node("{\"type\":\"object\"}"), new SchemaValidatorsConfig(), "schemaUri"), Arguments.of(SchemaLocation.of("classpath:/schemas/person-schema.json"), null, new SchemaValidatorsConfig(), "jsonNode"), Arguments.of(null, null, null, "schemaUri"));
    }

    // 3. Invalid SchemaLocation handling
    static Stream<Arguments> invalidLocationProvider() {
        return Stream.of(Arguments.of(SchemaLocation.of("http://[invalid-uri]"), node("{\"type\":\"object\"}"), IllegalArgumentException.class, "malformed URI"), Arguments.of(SchemaLocation.of("classpath:/schemas/non‑existent.json"), node("{\"type\":\"object\"}"), IOException.class, "resource not found"));
    }

    // 4. Invalid JsonNode content
    static Stream<Arguments> invalidNodeProvider() {
        return Stream.of(Arguments.of(SchemaLocation.of("classpath:/schemas/person-schema.json"), node("[1,2,3]"), IllegalArgumentException.class, "root node must be OBJECT"), Arguments.of(SchemaLocation.of("classpath:/schemas/person-schema.json"), node("{\"notAValidKeyword\":true}"), null, "unknown keyword – schema creation should succeed"));
    }

    // Utility method to read private fields via reflection.
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            fail("Unable to access private field '" + fieldName + "' on " + target.getClass(), e);
            // unreachable
            return null;
        }
    }

    // ------------------------------------------------------------------------
    @Test
    void getSchema_WithLocationNodeAndConfig_ReturnsNonNullJsonSchema() {
        // ----- given ---------------------------------------------------------
        // Mock the required input objects.  Their internal behaviour is not
        // exercised by the method under test, so simple Mockito mocks are enough.
        SchemaLocation location = mock(SchemaLocation.class);
        JsonNode schemaNode = mock(JsonNode.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // ----- when ----------------------------------------------------------
        JsonSchema result = factory.getSchema(location, schemaNode, config);
        // ----- then ----------------------------------------------------------
        assertNotNull(result, "The returned JsonSchema should not be null");
        // Additional sanity check – the result should be an instance of JsonSchema.
        assertTrue(result instanceof JsonSchema, "Result should be a JsonSchema");
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("happyPathProvider")
    void getSchema_HappyPath(SchemaLocation schemaUri, JsonNode jsonNode, SchemaValidatorsConfig config, String expectedMetaSchemaUri, String description) {
        // when
        JsonSchema result = factory.getSchema(schemaUri, jsonNode, config);
        // then
        assertNotNull(result, "Resulting JsonSchema must not be null");
        // meta‑schema verification (the factory resolves $schema or falls back to default)
        JsonMetaSchema meta = result.getMetaSchema();
        assertNotNull(meta, "Meta‑schema should be resolved");
        assertEquals(expectedMetaSchemaUri, meta.getUri(), "Meta‑schema URI should match expectation");
    }

    @ParameterizedTest(name = "[{index}] Null argument: {3}")
    @MethodSource("nullArgumentsProvider")
    void getSchema_NullArguments(SchemaLocation schemaUri, JsonNode jsonNode, SchemaValidatorsConfig config, String nullParameter) {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> factory.getSchema(schemaUri, jsonNode, config), "Expected NullPointerException for null " + nullParameter);
        assertTrue(npe.getMessage().toLowerCase().contains(nullParameter), "Exception message should mention the null parameter");
    }

    @ParameterizedTest(name = "[{index}] Invalid location: {3}")
    @MethodSource("invalidLocationProvider")
    void getSchema_InvalidSchemaLocation(SchemaLocation schemaUri, JsonNode jsonNode, Class<? extends Exception> expectedException, String description) {
        Exception ex = assertThrows(expectedException, () -> factory.getSchema(schemaUri, jsonNode, null), "Expected " + expectedException.getSimpleName() + " for " + description);
        // simple sanity check – message should contain something about the location
        assertNotNull(ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] Invalid JsonNode: {3}")
    @MethodSource("invalidNodeProvider")
    void getSchema_InvalidJsonNodeContent(SchemaLocation schemaUri, JsonNode jsonNode, Class<? extends Exception> expectedException, String description) {
        if (expectedException != null) {
            Exception ex = assertThrows(expectedException, () -> factory.getSchema(schemaUri, jsonNode, null), "Expected " + expectedException.getSimpleName() + " for " + description);
            assertNotNull(ex.getMessage());
        } else {
            // schema should be created; later validation would fail, but creation must succeed.
            JsonSchema schema = factory.getSchema(schemaUri, jsonNode, null);
            assertNotNull(schema, "Schema should be created even with unknown keywords");
        }
    }

    // 5. Custom SchemaValidatorsConfig propagation
    @Test
    void getSchema_CustomConfig_TypeLoosePropagation() {
        // given
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setTypeLoose(true);
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/person-schema.json");
        JsonNode node = node("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        // when
        JsonSchema schema = factory.getSchema(loc, node, cfg);
        // then
        assertNotNull(schema);
        // The ValidationContext is internal; we retrieve it via reflection.
        Object validationContext = getPrivateField(schema, "validationContext");
        assertNotNull(validationContext, "validationContext should be present");
        Object config = getPrivateField(validationContext, "config");
        assertNotNull(config, "config inside ValidationContext should be present");
        Boolean typeLoose = (Boolean) getPrivateField(config, "typeLoose");
        assertTrue(typeLoose, "typeLoose flag must be true as set in custom config");
    }

    @Test
    void getSchema_CustomConfig_FormatValidatorPropagation() {
        // given
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // accept any format
        cfg.setFormatValidatorFactory(uri -> (value) -> true);
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/person-schema.json");
        JsonNode node = node("{\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\",\"format\":\"email\"}}}");
        // when
        JsonSchema schema = factory.getSchema(loc, node, cfg);
        // then
        assertNotNull(schema);
        // Validate a value that would normally be invalid – the custom format validator should accept it.
        JsonNode instance = node("\"not-an-email\"");
        Set<ValidationMessage> messages = schema.validate(instance);
        assertTrue(messages.isEmpty(), "Custom format validator should allow any string for 'email' format");
    }

    // 6. Meta‑schema resolution via $schema keyword
    @Test
    void getSchema_MetaSchemaResolution_Draft06() {
        // given – $schema points to Draft‑06
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/unknown-meta-schema.json");
        JsonNode node = node("{\"$schema\":\"http://json-schema.org/draft-06/schema#\",\"type\":\"object\"}");
        // when
        JsonSchema schema = factory.getSchema(loc, node, null);
        // then
        assertNotNull(schema);
        JsonMetaSchema meta = schema.getMetaSchema();
        assertEquals("http://json-schema.org/draft-06/schema#", meta.getUri(), "Meta‑schema should be Draft‑06 as indicated by $schema keyword");
    }

    @Test
    void getSchema_CustomMetaSchemaViaConfig() {
        // given – config points to a custom meta‑schema URI
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setCustomMetaSchemaUri("urn:custom:meta");
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/custom-meta-schema.json");
        JsonNode node = node("{\"$schema\":\"urn:custom:meta\",\"type\":\"object\"}");
        // mock the custom meta‑schema so that the factory can resolve it
        JsonMetaSchema customMeta = mock(JsonMetaSchema.class);
        when(customMeta.getUri()).thenReturn("urn:custom:meta");
        metaSchemas.put("urn:custom:meta", customMeta);
        // when
        JsonSchema schema = factory.getSchema(loc, node, cfg);
        // then
        assertNotNull(schema);
        JsonMetaSchema meta = schema.getMetaSchema();
        assertEquals("urn:custom:meta", meta.getUri(), "Factory must use the custom meta‑schema URI supplied via config");
    }

    // 7. Unsupported URI scheme handling
    @Test
    void getSchema_UnsupportedUriScheme() {
        SchemaLocation loc = SchemaLocation.of("ftp://example.com/schema.json");
        JsonNode node = node("{\"type\":\"object\"}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, node, null), "Unsupported URI scheme should raise IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("ftp"), "Exception message should mention the unsupported scheme");
    }

    // 8. Remote URL loading failure
    @Test
    void getSchema_RemoteUrlLoadingFailure() {
        SchemaLocation loc = SchemaLocation.of("http://nonexistent.invalid/schema.json");
        JsonNode node = node("{\"type\":\"object\"}");
        // The factory attempts to read the remote resource; we expect an IOException or UncheckedIOException.
        assertThrows(IOException.class, () -> factory.getSchema(loc, node, null), "Loading a non‑existent remote URL should result in IOException");
    }

    // 9. Invalid $ref reference handling
    @Test
    void getSchema_InvalidRefReference() {
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/person-schema.json");
        JsonNode node = node("{\"$ref\":\"#/nonexistent\"}");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, node, null), "Unresolvable $ref must cause JsonSchemaException");
    }

    // 10. Cyclic $ref detection
    @Test
    void getSchema_CyclicRefDetection() {
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/cyclic-schema.json");
        JsonNode node = node("{\"$ref\":\"#\"}");
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(loc, node, null), "Cyclic $ref should be detected and raise JsonSchemaException");
    }

    // 11. Malformed custom meta‑schema URI in config
    @Test
    void getSchema_MalformedCustomMetaSchemaUri() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // malformed
        cfg.setCustomMetaSchemaUri("http://[bad]");
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/person-schema.json");
        JsonNode node = node("{\"$schema\":\"urn:custom:meta\",\"type\":\"object\"}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, node, cfg), "Malformed custom meta‑schema URI must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("uri"), "Exception message should mention URI");
    }

    // 12. Null custom meta‑schema URI in config
    @Test
    void getSchema_NullCustomMetaSchemaUriInConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setCustomMetaSchemaUri(null);
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/person-schema.json");
        JsonNode node = node("{\"$schema\":\"urn:custom:meta\",\"type\":\"object\"}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, node, cfg), "Null custom meta‑schema URI must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("null"), "Exception message should indicate null value");
    }

    // 13. Invalid configuration values (negative maxDepth)
    @Test
    void getSchema_InvalidConfigValues() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // illegal
        cfg.setMaxDepth(-1);
        SchemaLocation loc = SchemaLocation.of("classpath:/schemas/person-schema.json");
        JsonNode node = node("{\"type\":\"object\"}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(loc, node, cfg), "Negative maxDepth must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("maxdepth"), "Exception message should mention maxDepth");
    }
}
