package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.CompositeURITranslator;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    /**
     * shared test data **
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    /**
     * objects that are part of the JsonSchemaFactory constructor **
     */
    private ObjectMapper jsonMapper;

    private YAMLMapper yamlMapper;

    private URISchemeFactory uriFactory;

    private URISchemeFetcher uriFetcher;

    private URNFactory urnFactory;

    private CompositeURITranslator uriTranslators;

    private Map<String, JsonMetaSchema> jsonMetaSchemas;

    private Map<String, String> uriMap;

    /**
     * objects used in the actual test **
     */
    private JsonNode sampleNode;

    private SchemaValidatorsConfig config;

    private URI sampleUri;

    /**
     * the factory under test (spied so we can stub the protected newJsonSchema method) **
     */
    private JsonSchemaFactory factorySpy;

    /**
     * a lightweight mapper used by the static argument providers **
     */
    private static final ObjectMapper STATIC_MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* -------------------------------------------------
         * 1️⃣  Build the constructor arguments for JsonSchemaFactory
         * ------------------------------------------------- */
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();
        // Minimal (empty) but non‑null maps for the scheme factories/fetchers
        uriFactory = new URISchemeFactory(Collections.emptyMap());
        uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        // URNFactory has a public no‑arg constructor
        urnFactory = new URNFactory();
        // Composite translator – we do not need any real translators for the test
        uriTranslators = new CompositeURITranslator();
        // JsonMetaSchema map must contain an entry for the default meta‑schema URI.
        // We can mock JsonMetaSchema because the factory never inspects its content
        // during the execution of getSchema(...).
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);
        // Empty URI mapping and cache flag – not relevant for this unit test
        uriMap = Collections.emptyMap();
        boolean enableUriSchemaCache = false;
        /* -------------------------------------------------
         * 2️⃣  Create a real factory instance and then spy it
         * ------------------------------------------------- */
        JsonSchemaFactory realFactory = new JsonSchemaFactory(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
        // Spy allows us to stub the protected newJsonSchema method without touching private fields
        factorySpy = Mockito.spy(realFactory);
        // Stub the protected method to return a mock JsonSchema – this isolates the test
        JsonSchema mockSchema = mock(JsonSchema.class);
        doReturn(mockSchema).when(factorySpy).newJsonSchema(any(URI.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));
        /* -------------------------------------------------
         * 3️⃣  Prepare the parameters for the focal method
         * ------------------------------------------------- */
        sampleUri = new URI("http://example.com/my-schema.json");
        sampleNode = jsonMapper.readTree("{\"type\":\"object\"}");
        config = new SchemaValidatorsConfig();
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(// Valid inputs – schema is created successfully
        Arguments.of("Valid inputs – simple object schema", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), // Empty JSON object as schema
        Arguments.of("Edge case – Empty JSON object as schema", URI.create("http://example.com/empty"), readJson("{}"), new SchemaValidatorsConfig()), // Custom configuration applied
        Arguments.of("Valid inputs – custom configuration applied", URI.create("http://example.com/custom-config"), readJson("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}"), new SchemaValidatorsConfig().setTypeLoose(true).setFailFast(true)), // URI with fragment identifier
        Arguments.of("URI with fragment identifier", URI.create("http://example.com/schema#myFragment"), readJson("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), // URI containing query parameters (null config -> default)
        Arguments.of("URI with query parameters (null config)", URI.create("http://example.com/schema?version=1&debug=true"), readJson("{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"), null), // Non‑object JsonNode – array schema
        Arguments.of("Non‑object JsonNode – array schema", URI.create("http://example.com/array-schema"), readJson("[{\"type\":\"string\"},{\"type\":\"integer\"}]"), new SchemaValidatorsConfig()), // File scheme URI (null config -> default)
        Arguments.of("File scheme URI (null config)", URI.create("file:/tmp/test-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}}}"), null), // Meta‑schema URI (draft‑07)
        Arguments.of("Meta‑schema URI (draft‑07)", URI.create("http://json-schema.org/draft-07/schema#"), readJson("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig()), // Relative URI resolved against base
        Arguments.of("Relative URI resolved against base", URI.create("schemas/relative-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"rel\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), // Null config – factory must fall back to default
        Arguments.of("Null handling – Config is null (default config used)", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\"}"), null));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null handling – URI is null
        Arguments.of("Null handling – URI is null", null, readJson("{\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class), // Null handling – JsonNode is null
        Arguments.of("Null handling – JsonNode is null", URI.create("http://example.com/schema"), null, new SchemaValidatorsConfig(), IllegalArgumentException.class), // Null handling – Config is null (the factory is expected to replace it, not throw)
        // This case is already covered by a success test – here we purposely trigger the exception path
        // for illustration; the implementation chooses default handling, so we assert success instead.
        // Malformed URI – illegal characters
        Arguments.of("Malformed URI – illegal characters", // URI.create throws IllegalArgumentException for illegal chars – we capture that here
        // to keep the test focused on the factory method we wrap the creation in a try‑catch.
        createSafeUri("http://exa mple.com/ bad"), readJson("{\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class), // Unsupported schema version – unknown $schema value
        Arguments.of("Unsupported schema version – unknown $schema value", URI.create("http://example.com/unknown-schema"), readJson("{\"$schema\":\"http://example.com/unknown/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class));
    }

    /**
     * Parse a JSON string into a {@link JsonNode} using the static mapper.
     *  If parsing fails the test suite should surface the problem immediately.
     */
    private static JsonNode readJson(String json) {
        try {
            return STATIC_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON in test data: " + json, e);
        }
    }

    /**
     * Safely create a {@link URI} that may be malformed.
     *  If construction fails we return null, which will cause the factory method
     *  to throw the expected IllegalArgumentException.
     */
    private static URI createSafeUri(String uriString) {
        try {
            return URI.create(uriString);
        } catch (Exception e) {
            // will trigger the exception path in the test
            return null;
        }
    }

    @Test
    void shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig() {
        // WHEN
        JsonSchema result = factorySpy.getSchema(sampleUri, sampleNode, config);
        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
        // Verify that the protected factory method was invoked with the exact arguments we supplied
        verify(factorySpy, times(1)).newJsonSchema(eq(sampleUri), eq(sampleNode), eq(config));
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenSchemaUriIsNull() {
        // GIVEN a null URI
        URI nullUri = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> factorySpy.getSchema(nullUri, sampleNode, config), "Calling getSchema with a null URI should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenJsonNodeIsNull() {
        // GIVEN a null JsonNode
        JsonNode nullNode = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> factorySpy.getSchema(sampleUri, nullNode, config), "Calling getSchema with a null JsonNode should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenConfigIsNull() {
        // GIVEN a null config
        SchemaValidatorsConfig nullConfig = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> factorySpy.getSchema(sampleUri, sampleNode, nullConfig), "Calling getSchema with a null SchemaValidatorsConfig should raise IllegalArgumentException");
    }

    /**
     * Successful creation – we only assert that a non‑null schema is returned
     *  and that the factory method was invoked with the exact URI, node and
     *  (if supplied) configuration.
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("successfulCases")
    void shouldCreateSchemaSuccessfully(String description, URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig cfg) {
        JsonSchema result = factorySpy.getSchema(schemaUri, schemaNode, cfg);
        assertNotNull(result, description + ": result must not be null");
        // verify that the protected method was called with the same arguments
        if (cfg != null) {
            verify(factorySpy, times(1)).newJsonSchema(eq(schemaUri), eq(schemaNode), eq(cfg));
        } else {
            // cfg == null means the factory should have substituted a default config
            ArgumentCaptor<SchemaValidatorsConfig> cfgCaptor = ArgumentCaptor.forClass(SchemaValidatorsConfig.class);
            verify(factorySpy, times(1)).newJsonSchema(eq(schemaUri), eq(schemaNode), cfgCaptor.capture());
            assertNotNull(cfgCaptor.getValue(), description + ": a non‑null default SchemaValidatorsConfig must be used when null is supplied");
        }
    }

    /**
     * Cases that must result in an exception (normally IllegalArgumentException).
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("exceptionCases")
    void shouldThrowExceptionForInvalidInput(String description, URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig cfg, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> factorySpy.getSchema(schemaUri, schemaNode, cfg), description + ": expected " + expectedException.getSimpleName());
    }
}
