package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.CompositeURITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema";

    private JsonSchemaFactory factory;

    private ObjectMapper jsonMapper;

    private YAMLMapper yamlMapper;

    @BeforeEach
    void setupBeforeEach() {
        // Real mappers – they are lightweight and safe to use in unit tests
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();
        // Minimal but valid constructor arguments
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        URNFactory urnFactory = new URNFactory();
        // The factory expects a map that contains a JsonMetaSchema for the default meta‑schema URI.
        // We can safely use a mock because the test only verifies that getSchema returns a JsonSchema
        // instance; the internal meta‑schema handling is not exercised here.
        JsonMetaSchema mockMetaSchema = org.mockito.Mockito.mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(DEFAULT_META_SCHEMA_URI, mockMetaSchema);
        // Empty URI map – not needed for this scenario
        Map<String, String> uriMap = Collections.emptyMap();
        // No caching for the test
        boolean enableUriSchemaCache = false;
        CompositeURITranslator uriTranslators = new CompositeURITranslator();
        // Build the factory instance with the prepared arguments
        factory = new JsonSchemaFactory(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
    }

    /**
     * Returns the value of a (possibly private) field via reflection.
     */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (NoSuchFieldException e) {
            // Some implementations expose the field through a getter; fallback to null
            return null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience wrapper that returns the internal schema node of a JsonSchema.
     */
    private static JsonNode getSchemaNode(JsonSchema schema) {
        // Prefer a public accessor if it exists
        try {
            return (JsonNode) schema.getClass().getMethod("getSchemaNode").invoke(schema);
        } catch (Exception ignored) {
            // fall‑back to reflection on the private field named "schemaNode"
            return (JsonNode) getPrivateField(schema, "schemaNode");
        }
    }

    /**
     * Convenience wrapper that returns the config used by a JsonSchema.
     */
    private static SchemaValidatorsConfig getConfig(JsonSchema schema) {
        try {
            return (SchemaValidatorsConfig) schema.getClass().getMethod("getConfig").invoke(schema);
        } catch (Exception ignored) {
            return (SchemaValidatorsConfig) getPrivateField(schema, "config");
        }
    }

    /**
     * Provides a stream of valid arguments: JSON string for the schema node
     * and an optional {@link SchemaValidatorsConfig} (may be {@code null}).
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validInputsProvider() {
        return Stream.of(// simple object schema, default config (null)
        arguments("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}", null), // simple primitive schema, explicit config
        arguments("{\"type\":\"string\"}", new SchemaValidatorsConfig()), // empty object schema
        arguments("{}", new SchemaValidatorsConfig()), // boolean true schema
        arguments("true", new SchemaValidatorsConfig()), // array schema with constraints, config null
        arguments("{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"minItems\":2}", null), // object schema with custom config settings
        arguments("{\"type\":\"string\",\"minLength\":5}", new SchemaValidatorsConfig().setFailFast(true).setTypeLoose(true)), // schema containing $ref (no resolution)
        arguments("{\"$ref\":\"#/definitions/name\",\"definitions\":{ \"name\": {\"type\":\"string\"}}}", null), // nested definitions and $id
        arguments("{\"$id\":\"http://example.com/schemas/person\",\"type\":\"object\",\"properties\":{\"address\":{\"$ref\":\"address.json\"}},\"definitions\":{\"address\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}}}", new SchemaValidatorsConfig()));
    }

    /**
     * Provides a stream of invalid arguments that should cause the factory to
     * throw {@link IllegalArgumentException}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidInputsProvider() {
        return Stream.of(// null schema node, any config
        arguments(null, new SchemaValidatorsConfig()), // null schema node, null config
        arguments(null, null), // root numeric primitive
        arguments("42", null), // root string primitive
        arguments("\"just a string\"", null), // root empty array
        arguments("[]", null), // JSON literal null
        arguments("null", null));
    }

    @Test
    void getSchema_withValidJsonNodeAndConfig_returnsJsonSchema() throws Exception {
        // given
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonNode schemaNode = jsonMapper.readTree(schemaJson);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when
        JsonSchema result = factory.getSchema(schemaNode, config);
        // then
        assertNotNull(result, "The returned JsonSchema should not be null");
        assertTrue(result instanceof JsonSchema, "The returned object should be an instance of JsonSchema");
    }

    @ParameterizedTest(name = "[{index}] valid schema: {0}")
    @MethodSource("validInputsProvider")
    void getSchema_validInputs_returnsJsonSchema(String json, SchemaValidatorsConfig config) throws Exception {
        // given
        JsonNode schemaNode = jsonMapper.readTree(json);
        // when
        JsonSchema result = factory.getSchema(schemaNode, config);
        // then
        assertNotNull(result, "Resulting JsonSchema should not be null");
        assertEquals(schemaNode, getSchemaNode(result), "The JsonSchema should contain the exact JsonNode that was supplied");
        // Config may be null – the factory may replace it with a default internally.
        // If a non‑null config is supplied we expect the same instance to be kept.
        if (config != null) {
            assertSame(config, getConfig(result), "When a non‑null config is supplied, the JsonSchema should retain that instance");
        }
    }

    @ParameterizedTest(name = "[{index}] invalid schema node: {0}")
    @MethodSource("invalidInputsProvider")
    void getSchema_invalidInputs_throwsException(String json, SchemaValidatorsConfig config) throws Exception {
        JsonNode node = (json == null) ? null : jsonMapper.readTree(json);
        // The factory is expected to fail fast for illegal inputs.
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(node, config), "Expected IllegalArgumentException for invalid schema node: " + json);
    }

    @Test
    void getSchema_nullConfig_isHandledGracefully() throws Exception {
        // given – a valid object schema but a null config
        String json = "{\"type\":\"string\"}";
        JsonNode node = jsonMapper.readTree(json);
        // when
        JsonSchema schema = factory.getSchema(node, null);
        // then
        assertNotNull(schema, "JsonSchema should be created even when config is null");
        assertEquals(node, getSchemaNode(schema), "The internal schema node must match the supplied node");
        // When config is null, the factory usually substitutes a default instance.
        // We only verify that no NPE occurs and that the returned config (if any) is not null.
        SchemaValidatorsConfig usedConfig = getConfig(schema);
        assertNotNull(usedConfig, "Factory should replace a null config with a default instance");
    }

    @Test
    void getSchema_booleanTrueSchema_isAccepted() throws Exception {
        // given – boolean true as schema root
        JsonNode node = jsonMapper.readTree("true");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // when
        JsonSchema schema = factory.getSchema(node, cfg);
        // then
        assertNotNull(schema, "JsonSchema should be created for boolean true schema");
        assertEquals(node, getSchemaNode(schema), "The internal node should be the boolean true literal");
        assertSame(cfg, getConfig(schema), "Custom config should be retained");
    }
}
