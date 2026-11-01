package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    // ----- Mocks & test data -------------------------------------------------
    @Mock
    private ObjectMapper jsonMapperMock;

    @Mock
    private ObjectMapper yamlMapperMock;

    @Mock
    private JsonMetaSchema metaSchemaMock;

    @Mock
    private JsonNode schemaNodeMock;

    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/draft-07/schema";

    // the concrete factory under test
    private JsonSchemaFactory factory;

    // simple config instance
    private SchemaValidatorsConfig config;

    @Mock
    private JsonMetaSchema defaultMetaSchemaMock;

    @Mock
    private JsonMetaSchema draft04MetaSchemaMock;

    @Mock
    private JsonMetaSchema draft202012MetaSchemaMock;

    private static final String DRAFT_04_URI = "http://json-schema.org/draft-04/schema#";

    private static final String DRAFT_2020_12_URI = "https://json-schema.org/draft/2020-12/schema";

    // used only for building JsonNode test data
    private ObjectMapper realMapper;

    /**
     * Subclass that simply forwards the protected {@code createValidationContext}
     * method so the test can call it directly.
     */
    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {

        TestableJsonSchemaFactory(ObjectMapper jsonMapper, ObjectMapper yamlMapper, String defaultMetaSchemaURI, SchemaLoaders.Builder schemaLoadersBuilder, SchemaMappers.Builder schemaMappersBuilder, Map<String, JsonMetaSchema> jsonMetaSchemas, boolean enableUriSchemaCache) {
            super(jsonMapper, yamlMapper, defaultMetaSchemaURI, schemaLoadersBuilder, schemaMappersBuilder, jsonMetaSchemas, enableUriSchemaCache);
        }

        ValidationContext exposeCreateValidationContext(JsonNode schemaNode, SchemaValidatorsConfig cfg) {
            return super.createValidationContext(schemaNode, cfg);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled by MockitoExtension, but kept for clarity)
        MockitoAnnotations.openMocks(this);
        // Prepare a minimal map of meta‑schemas required by the factory constructor.
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        // The factory normalises the URI via JsonSchemaFactory.normalizeMetaSchemaUri.
        // For the purpose of the test we use the same literal string.
        metaSchemaMap.put(DEFAULT_META_SCHEMA_URI, metaSchemaMock);
        // Build the factory instance.
        factory = new TestableJsonSchemaFactory(jsonMapperMock, yamlMapperMock, DEFAULT_META_SCHEMA_URI, new SchemaLoaders.Builder(), new SchemaMappers.Builder(), metaSchemaMap, // URI schema cache disabled for simplicity
        false);
        // Simple (empty) configuration object.
        config = new SchemaValidatorsConfig();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(jsonMapperMock, yamlMapperMock, metaSchemaMock, schemaNodeMock);
    }

    /**
     * Parses a JSON string into a {@link JsonNode}.  The literal string {@code "null"} yields {@code null}.
     */
    private JsonNode parseJson(String json) {
        if (json == null || "null".equals(json)) {
            return null;
        }
        try {
            return realMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON supplied to test case", e);
        }
    }

    /**
     * Returns a fresh {@link SchemaValidatorsConfig} according to the description of the test case.
     */
    private SchemaValidatorsConfig buildConfig(String description) {
        // The description strings used in the test matrix are simple Java‑like snippets.
        // For readability we interpret only the patterns that appear in the matrix.
        if (description == null) {
            return null;
        }
        if (description.contains("new SchemaValidatorsConfig()")) {
            return new SchemaValidatorsConfig();
        }
        if (description.contains("setValidateAgainstMetaSchema(false)")) {
            return SchemaValidatorsConfig.builder().setValidateAgainstMetaSchema(false).build();
        }
        if (description.contains("setValidateAgainstMetaSchema(true)")) {
            return SchemaValidatorsConfig.builder().setValidateAgainstMetaSchema(true).setTypeLoose(true).setFailFast(true).build();
        }
        // Fallback – return a default instance.
        return new SchemaValidatorsConfig();
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(// Default meta‑schema selection (no $schema)
        Arguments.of("{\"type\":\"object\"}", "new SchemaValidatorsConfig()", DEFAULT_META_SCHEMA_URI, null), // Default meta‑schema selection with null config
        Arguments.of("{\"type\":\"object\"}", "null", DEFAULT_META_SCHEMA_URI, null), // Custom meta‑schema via known $schema (draft‑04)
        Arguments.of("{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\"}", "new SchemaValidatorsConfig()", DRAFT_04_URI, null), // Unknown $schema falls back to default
        Arguments.of("{\"$schema\":\"http://unknown/schema#\",\"type\":\"object\"}", "new SchemaValidatorsConfig()", DEFAULT_META_SCHEMA_URI, null), // Custom configuration influencing meta‑schema selection
        Arguments.of("{\"type\":\"object\"}", "SchemaValidatorsConfig.builder().setValidateAgainstMetaSchema(false).build()", DEFAULT_META_SCHEMA_URI, null), // Known $schema with null config
        Arguments.of("{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\"}", "null", DRAFT_04_URI, null), // Empty $schema value falls back to default
        Arguments.of("{\"$schema\":\"\",\"type\":\"object\"}", "new SchemaValidatorsConfig()", DEFAULT_META_SCHEMA_URI, null), // Whitespace‑padded $schema treated as unknown → default
        Arguments.of("{\"$schema\":\"   http://json-schema.org/draft-04/schema#   \",\"type\":\"object\"}", "new SchemaValidatorsConfig()", DEFAULT_META_SCHEMA_URI, null), // $schema attribute with explicit null value → default
        Arguments.of("{\"$schema\":null,\"type\":\"object\"}", "new SchemaValidatorsConfig()", DEFAULT_META_SCHEMA_URI, null), // Configuration with multiple flags (still default meta‑schema)
        Arguments.of("{\"type\":\"object\"}", "SchemaValidatorsConfig.builder().setValidateAgainstMetaSchema(true).setTypeLoose(true).setFailFast(true).build()", DEFAULT_META_SCHEMA_URI, null), // Recognised newer draft (2020‑12)
        Arguments.of("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}", "new SchemaValidatorsConfig()", DRAFT_2020_12_URI, null), // $schema URI containing fragment identifier (fragment ignored)
        Arguments.of("{\"$schema\":\"http://json-schema.org/draft-04/schema#/definitions/foo\",\"type\":\"object\"}", "new SchemaValidatorsConfig()", DRAFT_04_URI, null));
    }

    private static Stream<Arguments> exceptionalCases() {
        return Stream.of(// Null schemaNode
        Arguments.of("null", "new SchemaValidatorsConfig()", NullPointerException.class), // Non‑object schemaNode – array
        Arguments.of("[\"item1\", \"item2\"]", "new SchemaValidatorsConfig()", IllegalArgumentException.class), // Non‑object schemaNode – primitive number
        Arguments.of("42", "null", IllegalArgumentException.class), // Non‑text $schema value – number
        Arguments.of("{\"$schema\":123,\"type\":\"object\"}", "new SchemaValidatorsConfig()", IllegalArgumentException.class), // Non‑text $schema value – boolean
        Arguments.of("{\"$schema\":true,\"type\":\"object\"}", "null", IllegalArgumentException.class), // Non‑text $schema value – object
        Arguments.of("{\"$schema\":{},\"type\":\"object\"}", "new SchemaValidatorsConfig()", IllegalArgumentException.class), // Malformed $schema URI
        Arguments.of("{\"$schema\":\"http://[invalid]/schema#\",\"type\":\"object\"}", "new SchemaValidatorsConfig()", IllegalArgumentException.class), // Another malformed $schema URI
        Arguments.of("{\"$schema\":\"ht!tp://bad^uri\",\"type\":\"object\"}", "null", IllegalArgumentException.class));
    }

    // ----- Tests -------------------------------------------------------------
    @Test
    void createValidationContext_returnsContextWithExpectedComponents() {
        // Act: invoke the protected method via the testable subclass.
        ValidationContext ctx = ((TestableJsonSchemaFactory) factory).exposeCreateValidationContext(schemaNodeMock, config);
        // Assert: the returned context must not be null.
        assertNotNull(ctx, "ValidationContext should not be null");
        // The context should hold the same factory instance that created it.
        assertSame(factory, ctx.getFactory(), "ValidationContext should reference the originating JsonSchemaFactory");
        // The supplied configuration must be stored unchanged.
        assertSame(config, ctx.getConfig(), "ValidationContext should contain the provided SchemaValidatorsConfig");
        // The meta‑schema resolved for the supplied schema node must be the one
        // we placed in the factory's meta‑schema map.
        assertSame(metaSchemaMock, ctx.getJsonMetaSchema(), "ValidationContext should contain the expected JsonMetaSchema");
    }

    @ParameterizedTest(name = "[{index}] schema={0}, config={1}")
    @MethodSource("successfulCases")
    void createValidationContext_successful(String schemaJson, String configExpression, String expectedMetaSchemaUri, Class<? extends Throwable> unexpectedException) {
        // Arrange
        JsonNode schemaNode = parseJson(schemaJson);
        SchemaValidatorsConfig cfg = buildConfig(configExpression);
        // Act
        ValidationContext ctx = ((TestableJsonSchemaFactory) factory).exposeCreateValidationContext(schemaNode, cfg);
        // Assert – basic sanity
        assertNotNull(ctx, "ValidationContext must not be null");
        assertSame(factory, ctx.getFactory(), "Factory reference must be preserved");
        assertSame(cfg, ctx.getConfig(), "Provided config must be stored unchanged");
        // Resolve the expected mock based on the URI we supplied in the test data.
        JsonMetaSchema expectedMetaSchema;
        switch(expectedMetaSchemaUri) {
            case DEFAULT_META_SCHEMA_URI:
                expectedMetaSchema = defaultMetaSchemaMock;
                break;
            case DRAFT_04_URI:
                expectedMetaSchema = draft04MetaSchemaMock;
                break;
            case DRAFT_2020_12_URI:
                expectedMetaSchema = draft202012MetaSchemaMock;
                break;
            default:
                fail("Test case refers to an unknown meta‑schema URI: " + expectedMetaSchemaUri);
                return;
        }
        assertSame(expectedMetaSchema, ctx.getJsonMetaSchema(), "ValidationContext should contain the expected JsonMetaSchema");
    }

    @ParameterizedTest(name = "[{index}] schema={0}, config={1}")
    @MethodSource("exceptionalCases")
    void createValidationContext_exceptions(String schemaJson, String configExpression, Class<? extends Throwable> expectedException) {
        JsonNode schemaNode = parseJson(schemaJson);
        SchemaValidatorsConfig cfg = buildConfig(configExpression);
        // Act & Assert
        assertThrows(expectedException, () -> ((TestableJsonSchemaFactory) factory).exposeCreateValidationContext(schemaNode, cfg), () -> "Expected " + expectedException.getSimpleName() + " for schema: " + schemaJson);
    }
}
