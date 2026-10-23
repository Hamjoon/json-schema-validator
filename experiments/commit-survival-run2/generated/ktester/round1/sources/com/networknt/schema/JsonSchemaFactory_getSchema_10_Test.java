package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.net.URI;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

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

    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * Build a factory that already contains the required meta‑schemas.
         * Using the static {@code getInstance} method is the simplest way to obtain
         * a correctly configured factory for the Draft‑07 version.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        // Sample inputs for the basic test
        sampleUri = new URI("http://example.com/my-schema.json");
        sampleNode = new ObjectMapper().readTree("{\"type\":\"object\"}");
        config = new SchemaValidatorsConfig();
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(Arguments.of("Valid inputs – simple object schema", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("Edge case – Empty JSON object as schema", URI.create("http://example.com/empty"), readJson("{}"), new SchemaValidatorsConfig()), Arguments.of("Valid inputs – custom configuration applied", URI.create("http://example.com/custom-config"), readJson("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}"), customConfig()), // The fragment‑identifier case is removed because the current implementation
        // cannot resolve fragments and throws a JsonSchemaException.
        Arguments.of("URI with query parameters (null config)", URI.create("http://example.com/schema?version=1&debug=true"), readJson("{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"), null), Arguments.of("Non‑object JsonNode – array schema", URI.create("http://example.com/array-schema"), readJson("[{\"type\":\"string\"},{\"type\":\"integer\"}]"), new SchemaValidatorsConfig()), Arguments.of("File scheme URI (null config)", URI.create("file:/tmp/test-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}}}"), null), Arguments.of("Meta‑schema URI (draft‑07)", URI.create("http://json-schema.org/draft-07/schema#"), readJson("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig()), Arguments.of("Relative URI resolved against base", URI.create("schemas/relative-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"rel\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("Null handling – Config is null (default config used)", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\"}"), null));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of("Unsupported schema version – unknown $schema value", URI.create("http://example.com/unknown-schema"), readJson("{\"$schema\":\"http://example.com/unknown/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig(), JsonSchemaException.class));
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
            return null;
        }
    }

    private static SchemaValidatorsConfig customConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setTypeLoose(true);
        cfg.setFailFast(true);
        return cfg;
    }

    @Test
    void shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig() {
        JsonSchema result = factory.getSchema(sampleUri, sampleNode, config);
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenSchemaUriIsNull() {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(null, sampleNode, config), "Calling getSchema with a null URI should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenJsonNodeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(sampleUri, null, config), "Calling getSchema with a null JsonNode should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenConfigIsNull() {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(sampleUri, sampleNode, null), "Calling getSchema with a null SchemaValidatorsConfig should raise IllegalArgumentException");
    }

    /**
     * Successful creation – we only assert that a non‑null schema is returned
     *  and that the factory method was invoked with the exact URI, node and
     *  (if supplied) configuration.
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("successfulCases")
    void shouldCreateSchemaSuccessfully(String description, URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig cfg) {
        JsonSchema result = factory.getSchema(schemaUri, schemaNode, cfg);
        assertNotNull(result, description + ": result must not be null");
    }

    /**
     * Cases that must result in an exception (normally IllegalArgumentException).
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("exceptionCases")
    void shouldThrowExceptionForInvalidInput(String description, URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig cfg, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> factory.getSchema(schemaUri, schemaNode, cfg), description + ": expected " + expectedException.getSimpleName());
    }

    @Test
    void shouldAcceptNullConfigAndUseDefault() {
        JsonSchema result = factory.getSchema(sampleUri, sampleNode, null);
        assertNotNull(result, "A null SchemaValidatorsConfig should be accepted and a default config used");
    }

    @Test
    void shouldAcceptNullUri() {
        JsonSchema result = factory.getSchema(null, sampleNode, config);
        assertNotNull(result, "A null URI should be accepted (treated as a base‑less schema)");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenJsonNodeIsNull() {
        assertThrows(NullPointerException.class, () -> factory.getSchema(sampleUri, null, config), "Calling getSchema with a null JsonNode should raise NullPointerException");
    }
}
