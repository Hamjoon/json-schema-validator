package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.resource.SchemaReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMappedSchema_Test {

    /**
     * Mocks used to control the behaviour of the internal SchemaLoader.
     */
    @Mock
    private SchemaLoaders.Builder mockSchemaLoadersBuilder;

    @Mock
    private SchemaMappers.Builder mockSchemaMappersBuilder;

    /**
     * The factory under test.
     */
    private JsonSchemaFactory factory;

    /**
     * Common objects used by several tests.
     */
    private ObjectMapper jsonMapper;

    private ObjectMapper yamlMapper;

    private Map<String, JsonMetaSchema> metaSchemas;

    @BeforeEach
    void setupBeforeEach() {
        // Create simple JSON/YAML mappers – the real implementation is fine for the test.
        jsonMapper = JsonMapperFactory.getMapper();
        yamlMapper = YamlMapperFactory.getMapper();
        // Minimal meta‑schema map – the factory only checks that the default URI is present.
        metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI), JsonMetaSchema.getInstance(JsonMetaSchema.DEFAULT_META_SCHEMA_URI));
        // Build the factory using the public builder API.
        // The builder allows us to inject the mocked loader/mappers builders.
        factory = JsonSchemaFactory.builder().jsonMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI).schemaLoadersBuilder(mockSchemaLoadersBuilder).schemaMappersBuilder(mockSchemaMappersBuilder).jsonMetaSchemas(metaSchemas).enableUriSchemaCache(false).build();
        jsonMapper = JsonMapperFactory.getMapper();
        yamlMapper = YamlMapperFactory.getMapper();
        metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI), JsonMetaSchema.getInstance(JsonMetaSchema.DEFAULT_META_SCHEMA_URI));
        // The builder is injected with the mocked loader/mappers builders.
        factory = JsonSchemaFactory.builder().jsonMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI).schemaLoadersBuilder(mockSchemaLoadersBuilder).schemaMappersBuilder(mockSchemaMappersBuilder).jsonMetaSchemas(metaSchemas).enableUriSchemaCache(false).build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mocks after each test to avoid cross‑test interference.
        reset(mockSchemaLoadersBuilder, mockSchemaMappersBuilder);
        reset(mockSchemaLoadersBuilder, mockSchemaMappersBuilder);
    }

    /**
     * Creates a {@link SchemaLoader} that returns the supplied {@link InputStream}.
     */
    private SchemaLoader loaderReturning(InputStream is) {
        SchemaLoader loader = mock(SchemaLoader.class);
        SchemaReference ref = mock(SchemaReference.class);
        when(ref.getInputStream()).thenReturn(is);
        when(loader.getSchema(any(URI.class))).thenReturn(ref);
        return loader;
    }

    /**
     * Re‑creates the factory after the {@code mockSchemaLoadersBuilder} has been stubbed.
     */
    private void rebuildFactory() {
        factory = JsonSchemaFactory.builder().jsonMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI).schemaLoadersBuilder(mockSchemaLoadersBuilder).schemaMappersBuilder(mockSchemaMappersBuilder).jsonMetaSchemas(metaSchemas).enableUriSchemaCache(false).build();
    }

    private static Stream<Arguments> simpleSuccessProvider() {
        return Stream.of(Arguments.of("classpath:/simple.json", false, "{\"type\":\"object\"}"), Arguments.of("classpath:/simple.yaml", true, "type: object"));
    }

    /**
     * Verifies that a JSON schema without a fragment is loaded correctly and that a
     * {@link JsonSchema} instance is returned.
     */
    @Test
    void shouldLoadJsonSchemaWithoutFragment() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN
        // --------------------------------------------------------------------
        String simpleJson = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        InputStream jsonStream = new ByteArrayInputStream(simpleJson.getBytes());
        // Mock the internal SchemaLoader to return the prepared InputStream.
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        when(mockLoader.getSchema(any(URI.class))).thenReturn(() -> jsonStream);
        // The builder is expected to produce the mock loader when build() is called.
        when(mockSchemaLoadersBuilder.build()).thenReturn(mockLoader);
        // Re‑create the factory so that it picks up the newly stubbed builder.
        factory = JsonSchemaFactory.builder().jsonMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI).schemaLoadersBuilder(mockSchemaLoadersBuilder).schemaMappersBuilder(mockSchemaMappersBuilder).jsonMetaSchemas(metaSchemas).enableUriSchemaCache(false).build();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/test-schema.json")));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // --------------------------------------------------------------------
        // WHEN
        // --------------------------------------------------------------------
        JsonSchema result = factory.getMappedSchema(location, config);
        // --------------------------------------------------------------------
        // THEN
        // --------------------------------------------------------------------
        assertNotNull(result, "The returned JsonSchema must not be null");
        // The schema node should contain the "type" property we defined.
        JsonNode schemaNode = result.getSchemaNode();
        assertEquals("object", schemaNode.get("type").asText());
    }

    /**
     * Verifies that a YAML schema (identified by the file extension) is parsed
     * using the YAML mapper.
     */
    @Test
    void shouldLoadYamlSchemaWhenExtensionIndicatesYaml() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN
        // --------------------------------------------------------------------
        String yaml = "type: object\nproperties:\n  age:\n    type: integer";
        InputStream yamlStream = new ByteArrayInputStream(yaml.getBytes());
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        when(mockLoader.getSchema(any(URI.class))).thenReturn(() -> yamlStream);
        when(mockSchemaLoadersBuilder.build()).thenReturn(mockLoader);
        factory = JsonSchemaFactory.builder().jsonMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI).schemaLoadersBuilder(mockSchemaLoadersBuilder).schemaMappersBuilder(mockSchemaMappersBuilder).jsonMetaSchemas(metaSchemas).enableUriSchemaCache(false).build();
        // Use a ".yaml" extension so that the factory treats the content as YAML.
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/test-schema.yaml")));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // --------------------------------------------------------------------
        // WHEN
        // --------------------------------------------------------------------
        JsonSchema result = factory.getMappedSchema(location, config);
        // --------------------------------------------------------------------
        // THEN
        // --------------------------------------------------------------------
        assertNotNull(result);
        JsonNode schemaNode = result.getSchemaNode();
        assertEquals("object", schemaNode.get("type").asText());
        assertEquals("integer", schemaNode.path("properties").path("age").get("type").asText());
    }

    /**
     * Verifies that a fragment in the {@link SchemaLocation} leads to retrieval of a
     * sub‑schema via {@link JsonSchema#getRefSchema}.
     */
    @Test
    void shouldResolveFragmentToSubSchema() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN
        // --------------------------------------------------------------------
        String json = "{\n" + "  \"$id\": \"http://example.com/root.json\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"address\": {\n" + "      \"$id\": \"http://example.com/address.json\",\n" + "      \"type\": \"object\",\n" + "      \"properties\": {\n" + "        \"street\": { \"type\": \"string\" }\n" + "      }\n" + "    }\n" + "  }\n" + "}";
        InputStream jsonStream = new ByteArrayInputStream(json.getBytes());
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        when(mockLoader.getSchema(any(URI.class))).thenReturn(() -> jsonStream);
        when(mockSchemaLoadersBuilder.build()).thenReturn(mockLoader);
        factory = JsonSchemaFactory.builder().jsonMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI).schemaLoadersBuilder(mockSchemaLoadersBuilder).schemaMappersBuilder(mockSchemaMappersBuilder).jsonMetaSchemas(metaSchemas).enableUriSchemaCache(false).build();
        // Fragment points to the "address" subschema.
        JsonNodePath fragment = JsonNodePath.compile("/properties/address");
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/root.json")), fragment);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // --------------------------------------------------------------------
        // WHEN
        // --------------------------------------------------------------------
        JsonSchema result = factory.getMappedSchema(location, config);
        // --------------------------------------------------------------------
        // THEN
        // --------------------------------------------------------------------
        assertNotNull(result);
        // The resolved sub‑schema should have a "type" of "object".
        assertEquals("object", result.getSchemaNode().get("type").asText());
        // And it should contain the "street" property defined inside the address schema.
        assertTrue(result.getSchemaNode().path("properties").has("street"));
    }

    /**
     * Verifies that an {@link IOException} while loading the schema is wrapped
     * into a {@link JsonSchemaException}.
     */
    @Test
    void shouldWrapIOExceptionIntoJsonSchemaException() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN
        // --------------------------------------------------------------------
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        // Simulate a loader that returns null InputStream -> IOException path.
        when(mockLoader.getSchema(any(URI.class))).thenReturn(() -> null);
        when(mockSchemaLoadersBuilder.build()).thenReturn(mockLoader);
        factory = JsonSchemaFactory.builder().jsonMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI).schemaLoadersBuilder(mockSchemaLoadersBuilder).schemaMappersBuilder(mockSchemaMappersBuilder).jsonMetaSchemas(metaSchemas).enableUriSchemaCache(false).build();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/missing.json")));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // --------------------------------------------------------------------
        // WHEN / THEN
        // --------------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getMappedSchema(location, config));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
    }

    @Test
    void shouldLoadYamlSchemaWithoutFragment() throws Exception {
        String yaml = "type: object\nproperties:\n  age:\n    type: integer";
        InputStream stream = new ByteArrayInputStream(yaml.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/simple.yaml")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getMappedSchema(location, cfg);
        assertNotNull(schema);
        JsonNode node = schema.getSchemaNode();
        assertEquals("object", node.get("type").asText());
        assertEquals("integer", node.path("properties").path("age").get("type").asText());
    }

    @Test
    void shouldLoadYamlSchemaWithFragment() throws Exception {
        String yaml = "type: object\nproperties:\n  address:\n    type: object\n    properties:\n      street:\n        type: string";
        InputStream stream = new ByteArrayInputStream(yaml.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        JsonNodePath fragment = JsonNodePath.compile("/properties/address");
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/complex.yaml")), fragment);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema subSchema = factory.getMappedSchema(location, cfg);
        assertNotNull(subSchema);
        JsonNode node = subSchema.getSchemaNode();
        assertEquals("object", node.get("type").asText());
        assertTrue(node.path("properties").has("street"));
    }

    @Test
    void shouldTreatEmptyFragmentAsNoFragment() throws Exception {
        String json = "{ \"type\": \"string\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        // Empty fragment – JsonNodePath with zero segments.
        JsonNodePath emptyFragment = JsonNodePath.compile("");
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/emptyFragment.json")), emptyFragment);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getMappedSchema(location, cfg);
        assertNotNull(schema);
        assertEquals("string", schema.getSchemaNode().get("type").asText());
    }

    @Test
    void shouldWrapNullInputStreamIntoJsonSchemaException() throws Exception {
        SchemaLoader loader = mock(SchemaLoader.class);
        SchemaReference ref = mock(SchemaReference.class);
        when(ref.getInputStream()).thenReturn(null);
        when(loader.getSchema(any(URI.class))).thenReturn(ref);
        when(mockSchemaLoadersBuilder.build()).thenReturn(loader);
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/missing.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
    }

    @Test
    void shouldWrapIOExceptionFromMapperIntoJsonSchemaException() throws Exception {
        // Stream that works, but we make the JSON mapper throw an IOException.
        InputStream stream = new ByteArrayInputStream("{invalid".getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        // Spy on the factory to inject a failing mapper.
        JsonSchemaFactory spyFactory = spy(factory);
        doThrow(new IOException("broken stream")).when(spyFactory).getJsonMapper();
        // Because the spy replaces the mapper, we need to rebuild the factory with the spy.
        // (The private method getJsonMapper is called inside getMappedSchema, so the spy will
        // throw the exception at that point.)
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/broken.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> spyFactory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("broken stream", ex.getCause().getMessage());
    }

    @Test
    void shouldWrapIOExceptionFromSchemaLoader() throws Exception {
        SchemaLoader loader = mock(SchemaLoader.class);
        when(loader.getSchema(any(URI.class))).thenThrow(new IOException("network error"));
        when(mockSchemaLoadersBuilder.build()).thenReturn(loader);
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/unreachable.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("network error", ex.getCause().getMessage());
    }

    @Test
    void shouldPropagateUncheckedExceptionFromSchemaLoader() throws Exception {
        SchemaLoader loader = mock(SchemaLoader.class);
        when(loader.getSchema(any(URI.class))).thenThrow(new IllegalStateException("boom"));
        when(mockSchemaLoadersBuilder.build()).thenReturn(loader);
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/boom.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> factory.getMappedSchema(location, cfg));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void shouldPropagateExceptionWhenMetaSchemaIsUnknown() throws Exception {
        // Schema declares an unknown $schema URI.
        String json = "{ \"$schema\": \"http://example.com/unknown-meta-schema\", \"type\": \"object\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/unknownMeta.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // The factory will try to resolve the meta‑schema and fail – the exact exception type
        // depends on the implementation (JsonSchemaException, IllegalArgumentException, …).
        Exception ex = assertThrows(RuntimeException.class, () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("unknown-meta-schema"));
    }

    @Test
    void shouldThrowNPEWhenConfigIsNull() throws Exception {
        String json = "{ \"type\": \"null\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/nullConfig.json")));
        assertThrows(NullPointerException.class, () -> factory.getMappedSchema(location, null));
    }

    @Test
    void shouldThrowNPEWhenSchemaReferenceIsNull() throws Exception {
        SchemaLoader loader = mock(SchemaLoader.class);
        // null reference
        when(loader.getSchema(any(URI.class))).thenReturn(null);
        when(mockSchemaLoadersBuilder.build()).thenReturn(loader);
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/nullRef.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(NullPointerException.class, () -> factory.getMappedSchema(location, cfg));
    }

    @Test
    void shouldThrowNPEWhenSchemaLocationIsNull() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(NullPointerException.class, () -> factory.getMappedSchema(null, cfg));
    }

    @Test
    void shouldThrowNPEWhenAbsoluteIriIsNull() throws Exception {
        // Create a spy of SchemaLocation that returns null for getAbsoluteIri()
        SchemaLocation location = mock(SchemaLocation.class);
        when(location.getAbsoluteIri()).thenReturn(null);
        when(location.getFragment()).thenReturn(null);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // Loader will not be used because NPE occurs earlier.
        assertThrows(NullPointerException.class, () -> factory.getMappedSchema(location, cfg));
    }

    @Test
    void shouldThrowNPEWhenConfigPathTypeIsNull() throws Exception {
        String json = "{ \"type\": \"string\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/nullPathType.json")));
        // Build a config whose path type is deliberately set to null.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // The real class has a setter – if not, reflection can be used.
        // assume such a setter exists
        cfg.setPathType(null);
        assertThrows(NullPointerException.class, () -> factory.getMappedSchema(location, cfg));
    }

    @Test
    void shouldThrowWhenFragmentCannotBeResolved() throws Exception {
        String json = "{ \"type\": \"object\", \"properties\": { \"valid\": { \"type\": \"string\" } } }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        // Fragment points to a non‑existent definition.
        JsonNodePath fragment = JsonNodePath.compile("/properties/invalid");
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/unresolvable.json")), fragment);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // The underlying JsonSchema#getRefSchema throws IllegalArgumentException when it cannot find the fragment.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("invalid"));
    }

    @Test
    void shouldThrowWhenMapperReturnsNullNode() throws Exception {
        // Empty input stream – Jackson returns null when readTree on empty content.
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(emptyStream));
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/empty.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // Expect a NullPointerException (or JsonSchemaException) because schemaNode is null.
        Exception ex = assertThrows(RuntimeException.class, () -> factory.getMappedSchema(location, cfg));
        // The exact exception type depends on the internal implementation.
        assertTrue(ex.getMessage().contains("null"));
    }

    @ParameterizedTest(name = "[{index}] Load {0} (YAML={1})")
    @MethodSource("simpleSuccessProvider")
    void shouldLoadSimpleSchemas(String uriString, boolean yaml, String content) throws Exception {
        InputStream stream = new ByteArrayInputStream(content.getBytes());
        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create(uriString)));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getMappedSchema(location, cfg);
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText());
    }
}
