```java
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getMappedSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * The tests focus on the two main execution paths inside the method:
 * <ul>
 *   <li>Loading a schema without a fragment (full document schema).</li>
 *   <li>Loading a schema with a fragment (sub‑schema retrieval via $ref).</li>
 *   <li>Handling I/O errors when the underlying {@link SchemaLoader} cannot provide an {@link InputStream}.</li>
 * </ul>
 *
 * A {@link JsonSchemaFactory} instance is created via the public {@code builder()} API.
 * The builder is supplied with mock {@link SchemaLoaders.Builder} and {@link SchemaMappers.Builder}
 * so that we can control the behaviour of the internal {@link SchemaLoader} without touching
 * private fields directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMappedSchema_Test {

    /** Mocks used to control the behaviour of the internal SchemaLoader. */
    @Mock
    private SchemaLoaders.Builder mockSchemaLoadersBuilder;
    @Mock
    private SchemaMappers.Builder mockSchemaMappersBuilder;

    /** The factory under test. */
    private JsonSchemaFactory factory;

    /** Common objects used by several tests. */
    private ObjectMapper jsonMapper;
    private ObjectMapper yamlMapper;
    private Map<String, JsonMetaSchema> metaSchemas;

    @BeforeAll
    static void setupBeforeAll() {
        // No global initialisation required for the current test suite.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create simple JSON/YAML mappers – the real implementation is fine for the test.
        jsonMapper = JsonMapperFactory.getMapper();
        yamlMapper = YamlMapperFactory.getMapper();

        // Minimal meta‑schema map – the factory only checks that the default URI is present.
        metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI),
                JsonMetaSchema.getInstance(JsonMetaSchema.DEFAULT_META_SCHEMA_URI));

        // Build the factory using the public builder API.
        // The builder allows us to inject the mocked loader/mappers builders.
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
    void teardownAfterEach() {
        // Reset the mocks after each test to avoid cross‑test interference.
        reset(mockSchemaLoadersBuilder, mockSchemaMappersBuilder);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
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
        factory = JsonSchemaFactory.builder()
                .jsonMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI)
                .schemaLoadersBuilder(mockSchemaLoadersBuilder)
                .schemaMappersBuilder(mockSchemaMappersBuilder)
                .jsonMetaSchemas(metaSchemas)
                .enableUriSchemaCache(false)
                .build();

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

        factory = JsonSchemaFactory.builder()
                .jsonMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI)
                .schemaLoadersBuilder(mockSchemaLoadersBuilder)
                .schemaMappersBuilder(mockSchemaMappersBuilder)
                .jsonMetaSchemas(metaSchemas)
                .enableUriSchemaCache(false)
                .build();

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
        InputStream jsonStream = new ByteArrayInputStream(json.getBytes());

        SchemaLoader mockLoader = mock(SchemaLoader.class);
        when(mockLoader.getSchema(any(URI.class))).thenReturn(() -> jsonStream);
        when(mockSchemaLoadersBuilder.build()).thenReturn(mockLoader);

        factory = JsonSchemaFactory.builder()
                .jsonMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI)
                .schemaLoadersBuilder(mockSchemaLoadersBuilder)
                .schemaMappersBuilder(mockSchemaMappersBuilder)
                .jsonMetaSchemas(metaSchemas)
                .enableUriSchemaCache(false)
                .build();

        // Fragment points to the "address" subschema.
        JsonNodePath fragment = JsonNodePath.compile("/properties/address");
        SchemaLocation location = new SchemaLocation(
                new AbsoluteIri(URI.create("classpath:/root.json")),
                fragment);
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

        factory = JsonSchemaFactory.builder()
                .jsonMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaUri(JsonMetaSchema.DEFAULT_META_SCHEMA_URI)
                .schemaLoadersBuilder(mockSchemaLoadersBuilder)
                .schemaMappersBuilder(mockSchemaMappersBuilder)
                .jsonMetaSchemas(metaSchemas)
                .enableUriSchemaCache(false)
                .build();

        SchemaLocation location = new SchemaLocation(new AbsoluteIri(URI.create("classpath:/missing.json")));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // --------------------------------------------------------------------
        // WHEN / THEN
        // --------------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getMappedSchema(location, config));
        assertTrue(ex.getMessage().contains("Failed to load json schema"));
    }
}
```