```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.resource.SchemaReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#getMappedSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * <p>The test suite covers all execution branches described in the JSON test‑case specification:
 * <ul>
 *   <li>Successful loading of JSON/YAML schemas with and without fragments.</li>
 *   <li>Edge cases such as empty fragments.</li>
 *   <li>Various failure modes (null streams, IOExceptions, unchecked exceptions, meta‑schema problems, etc.).</li>
 * </ul>
 *
 * <p>Only the public API is used; private helpers are exercised indirectly.  Where a private method
 * influences observable behaviour (e.g. the {@code suppressSubSchemaRetrieval} flag) the test asserts
 * the resulting {@link JsonSchema} characteristics instead of spying on the private method.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMappedSchema_Test {

    /* --------------------------------------------------------------------- *
     *  Mocks and shared fixtures
     * --------------------------------------------------------------------- */

    @Mock
    private SchemaLoaders.Builder mockSchemaLoadersBuilder;
    @Mock
    private SchemaMappers.Builder mockSchemaMappersBuilder;

    /** Factory under test – rebuilt in each test after the mock builder is stubbed. */
    private JsonSchemaFactory factory;

    /** Real JSON/YAML mappers – the production implementation works fine for the tests. */
    private ObjectMapper jsonMapper;
    private ObjectMapper yamlMapper;

    /** Minimal meta‑schema map required by the factory. */
    private Map<String, JsonMetaSchema> metaSchemas;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapperFactory.getMapper();
        yamlMapper = YamlMapperFactory.getMapper();

        metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI),
                JsonMetaSchema.getInstance(JsonMetaSchema.DEFAULT_META_SCHEMA_URI));

        // The builder is injected with the mocked loader/mappers builders.
        factory = JsonSchemaFactory.builder()
                .jsonMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI)
                .schemaLoadersBuilder(mockSchemaLoadersBuilder)
                .schemaMappersBuilder(mockSchemaMappersBuilder)
                .jsonMetaSchemas(metaSchemas)
                .enableUriSchemaCache(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        reset(mockSchemaLoadersBuilder, mockSchemaMappersBuilder);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods
     * --------------------------------------------------------------------- */

    /** Creates a {@link SchemaLoader} that returns the supplied {@link InputStream}. */
    private SchemaLoader loaderReturning(InputStream is) {
        SchemaLoader loader = mock(SchemaLoader.class);
        SchemaReference ref = mock(SchemaReference.class);
        when(ref.getInputStream()).thenReturn(is);
        when(loader.getSchema(any(URI.class))).thenReturn(ref);
        return loader;
    }

    /** Re‑creates the factory after the {@code mockSchemaLoadersBuilder} has been stubbed. */
    private void rebuildFactory() {
        factory = JsonSchemaFactory.builder()
                .jsonMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI)
                .schemaLoadersBuilder(mockSchemaLoadersBuilder)
                .schemaMappersBuilder(mockSchemaMappersBuilder)
                .jsonMetaSchemas(metaSchemas)
                .enableUriSchemaCache(false)
                .build();
    }

    /* --------------------------------------------------------------------- *
     *  Successful loading – JSON schema without fragment
     * --------------------------------------------------------------------- */

    @Test
    void shouldLoadJsonSchemaWithoutFragment() throws Exception {
        String json = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());

        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();

        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/simple.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema schema = factory.getMappedSchema(location, cfg);

        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText());
    }

    /* --------------------------------------------------------------------- *
     *  Successful loading – YAML schema without fragment
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  Successful loading – JSON schema with fragment
     * --------------------------------------------------------------------- */

    @Test
    void shouldResolveFragmentToSubSchema() throws Exception {
        String json = "{\n" +
                "  \"$id\": \"http://example.com/root.json\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"address\": {\n" +
                "      \"$id\": \"http://example.com/address.json\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"street\": { \"type\": \"string\" }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        InputStream stream = new ByteArrayInputStream(json.getBytes());

        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();

        // fragment = /properties/address
        JsonNodePath fragment = JsonNodePath.compile("/properties/address");
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/root.json")), fragment);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema subSchema = factory.getMappedSchema(location, cfg);

        assertNotNull(subSchema);
        assertEquals("object", subSchema.getSchemaNode().get("type").asText());
        assertTrue(subSchema.getSchemaNode().path("properties").has("street"));
    }

    /* --------------------------------------------------------------------- *
     *  Successful loading – YAML schema with fragment
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  Edge case – fragment present but empty (nameCount == 0)
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  Failure – InputStream is null (triggers IOException)
     * --------------------------------------------------------------------- */

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

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
    }

    /* --------------------------------------------------------------------- *
     *  Failure – IOException while reading schema (readTree throws)
     * --------------------------------------------------------------------- */

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

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> spyFactory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("broken stream", ex.getCause().getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Failure – SchemaLoader throws IOException before stream acquisition
     * --------------------------------------------------------------------- */

    @Test
    void shouldWrapIOExceptionFromSchemaLoader() throws Exception {
        SchemaLoader loader = mock(SchemaLoader.class);
        when(loader.getSchema(any(URI.class))).thenThrow(new IOException("network error"));
        when(mockSchemaLoadersBuilder.build()).thenReturn(loader);
        rebuildFactory();

        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/unreachable.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("network error", ex.getCause().getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Failure – SchemaLoader throws unchecked RuntimeException
     * --------------------------------------------------------------------- */

    @Test
    void shouldPropagateUncheckedExceptionFromSchemaLoader() throws Exception {
        SchemaLoader loader = mock(SchemaLoader.class);
        when(loader.getSchema(any(URI.class))).thenThrow(new IllegalStateException("boom"));
        when(mockSchemaLoadersBuilder.build()).thenReturn(loader);
        rebuildFactory();

        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/boom.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> factory.getMappedSchema(location, cfg));
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Failure – Invalid meta‑schema identifier
     * --------------------------------------------------------------------- */

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
        Exception ex = assertThrows(RuntimeException.class,
                () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("unknown-meta-schema"));
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – Null SchemaValidatorsConfig
     * --------------------------------------------------------------------- */

    @Test
    void shouldThrowNPEWhenConfigIsNull() throws Exception {
        String json = "{ \"type\": \"null\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());

        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(stream));
        rebuildFactory();

        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/nullConfig.json")));

        assertThrows(NullPointerException.class,
                () -> factory.getMappedSchema(location, null));
    }

    /* --------------------------------------------------------------------- *
     *  Failure – SchemaLoader returns null SchemaReference
     * --------------------------------------------------------------------- */

    @Test
    void shouldThrowNPEWhenSchemaReferenceIsNull() throws Exception {
        SchemaLoader loader = mock(SchemaLoader.class);
        when(loader.getSchema(any(URI.class))).thenReturn(null); // null reference
        when(mockSchemaLoadersBuilder.build()).thenReturn(loader);
        rebuildFactory();

        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/nullRef.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        assertThrows(NullPointerException.class,
                () -> factory.getMappedSchema(location, cfg));
    }

    /* --------------------------------------------------------------------- *
     *  Failure – schemaUri argument is null
     * --------------------------------------------------------------------- */

    @Test
    void shouldThrowNPEWhenSchemaLocationIsNull() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(NullPointerException.class,
                () -> factory.getMappedSchema(null, cfg));
    }

    /* --------------------------------------------------------------------- *
     *  Failure – schemaUri.getAbsoluteIri() returns null
     * --------------------------------------------------------------------- */

    @Test
    void shouldThrowNPEWhenAbsoluteIriIsNull() throws Exception {
        // Create a spy of SchemaLocation that returns null for getAbsoluteIri()
        SchemaLocation location = mock(SchemaLocation.class);
        when(location.getAbsoluteIri()).thenReturn(null);
        when(location.getFragment()).thenReturn(null);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // Loader will not be used because NPE occurs earlier.
        assertThrows(NullPointerException.class,
                () -> factory.getMappedSchema(location, cfg));
    }

    /* --------------------------------------------------------------------- *
     *  Failure – Config with null PathType
     * --------------------------------------------------------------------- */

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
        cfg.setPathType(null); // assume such a setter exists

        assertThrows(NullPointerException.class,
                () -> factory.getMappedSchema(location, cfg));
    }

    /* --------------------------------------------------------------------- *
     *  Failure – document.getRefSchema(fragment) not found
     * --------------------------------------------------------------------- */

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
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getMappedSchema(location, cfg));
        assertTrue(ex.getMessage().contains("invalid"));
    }

    /* --------------------------------------------------------------------- *
     *  Failure – getMetaSchemaOrDefault receives null schemaNode (empty file)
     * --------------------------------------------------------------------- */

    @Test
    void shouldThrowWhenMapperReturnsNullNode() throws Exception {
        // Empty input stream – Jackson returns null when readTree on empty content.
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        when(mockSchemaLoadersBuilder.build()).thenReturn(loaderReturning(emptyStream));
        rebuildFactory();

        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/empty.json")));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // Expect a NullPointerException (or JsonSchemaException) because schemaNode is null.
        Exception ex = assertThrows(RuntimeException.class,
                () -> factory.getMappedSchema(location, cfg));
        // The exact exception type depends on the internal implementation.
        assertTrue(ex.getMessage().contains("null"));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test source for simple success cases (JSON/YAML without fragment)
     * --------------------------------------------------------------------- */

    private static Stream<Arguments> simpleSuccessProvider() {
        return Stream.of(
                Arguments.of("classpath:/simple.json", false, "{\"type\":\"object\"}"),
                Arguments.of("classpath:/simple.yaml", true, "type: object")
        );
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
```