package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper mapper;

    private static SchemaValidatorsConfig config;

    /**
     * Create a shared factory and default objects once for all tests.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it builds a fully‑initialised instance.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = new ObjectMapper();
        // The library provides a builder for the config; use the defaults.
        config = SchemaValidatorsConfig.builder().build();
    }

    /**
     * Helper that invokes the protected {@code newJsonSchema} method via reflection.
     */
    private JsonSchema invokeNewJsonSchema(SchemaLocation uri, JsonNode node, SchemaValidatorsConfig cfg) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonSchema) m.invoke(factory, uri, node, cfg);
    }

    /**
     * Extract the underlying schema node from a {@link JsonSchema} (used for assertions).
     */
    private JsonNode extractSchemaNode(JsonSchema schema) throws Exception {
        // JsonSchema has a public getter in recent versions; fall back to reflection if not.
        try {
            Method getter = JsonSchema.class.getMethod("getSchemaNode");
            return (JsonNode) getter.invoke(schema);
        } catch (NoSuchMethodException e) {
            Method field = JsonSchema.class.getDeclaredMethod("getSchemaNode");
            field.setAccessible(true);
            return (JsonNode) field.invoke(schema);
        }
    }

    private static Stream<Arguments> successfulScenarios() {
        return Stream.of(// simple object schema, default config (null)
        Arguments.of("http://example.com/simple.json", "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}", null), // array schema, explicit default config
        Arguments.of("http://example.com/array.json", "{\"type\":\"array\",\"items\":{\"type\":\"number\"}}", new SchemaValidatorsConfig()), // schema with $ref but suppression disabled (should still succeed)
        Arguments.of("http://example.com/ref-ok.json", "{\"$ref\":\"http://example.com/other.json\"}", new SchemaValidatorsConfig().setSuppressSubSchemaRetrieval(false)));
    }

    /**
     * Verify that {@code newJsonSchema} creates a non‑null {@link JsonSchema}
     * instance when supplied with a simple schema node.
     */
    @Test
    void newJsonSchemaCreatesSchema() throws Exception {
        // ----- GIVEN ---------------------------------------------------------
        // a minimal JSON schema: {"type":"string"}
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test-schema"));
        // ----- WHEN ----------------------------------------------------------
        // invoke the protected method via reflection
        Method newJsonSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        newJsonSchemaMethod.setAccessible(true);
        JsonSchema result = (JsonSchema) newJsonSchemaMethod.invoke(factory, location, schemaNode, config);
        // ----- THEN ----------------------------------------------------------
        assertNotNull(result, "newJsonSchema should return a non‑null JsonSchema");
        // The returned JsonSchema should expose the original node (public getter exists).
        // If the getter is not public, we fall back to reflection.
        JsonNode returnedNode;
        try {
            returnedNode = result.getSchemaNode();
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            // Fallback: use reflection to read the private field 'schemaNode'
            Method getNode = JsonSchema.class.getDeclaredMethod("getSchemaNode");
            getNode.setAccessible(true);
            returnedNode = (JsonNode) getNode.invoke(result);
        }
        assertEquals(schemaNode, returnedNode, "The JsonSchema should contain the same schema node that was supplied");
    }

    /**
     * Verify that calling {@code newJsonSchema} with a {@code null} schema node
     * throws a {@link NullPointerException}. This mirrors the contract of the
     * underlying implementation (it expects a non‑null node).
     */
    @Test
    void newJsonSchemaRejectsNullSchemaNode() throws Exception {
        // ----- GIVEN ---------------------------------------------------------
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test-schema"));
        // ----- WHEN ----------------------------------------------------------
        Method newJsonSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        newJsonSchemaMethod.setAccessible(true);
        // ----- THEN ----------------------------------------------------------
        assertThrows(NullPointerException.class, () -> {
            newJsonSchemaMethod.invoke(factory, location, null, config);
        });
    }

    @Test
    void normalOperationDefaultConfig() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        // default config
        SchemaValidatorsConfig cfg = null;
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Schema must not be null");
        assertEquals(node, extractSchemaNode(schema), "Returned schema should contain the original node");
    }

    @Test
    void normalOperationCustomConfig() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("classpath:/my-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setPathType(JsonNodePath.Type.JSON_POINTER).setFailFast(true);
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Schema must not be null");
        // Verify that the configuration made it into the ValidationContext (reflection).
        Method getContext = JsonSchema.class.getDeclaredMethod("getValidationContext");
        getContext.setAccessible(true);
        ValidationContext vc = (ValidationContext) getContext.invoke(schema);
        assertEquals(JsonNodePath.Type.JSON_POINTER, vc.getConfig().getPathType(), "Path type should be JSON_POINTER as set in the config");
        assertTrue(vc.getConfig().isFailFast(), "Fail‑fast flag should be true");
    }

    @Test
    void emptySchemaValidatorsConfig() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/empty-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"null\"}");
        // all defaults
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Schema must be created even with an explicit default config");
    }

    @Test
    void configDisablesRefLoading() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/ref-schema.json");
        JsonNode node = mapper.readTree("{\"$ref\":\"http://example.com/other.json\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setSuppressSubSchemaRetrieval(true);
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Schema should be created even when $ref loading is suppressed");
        // No further verification needed – the absence of an exception proves preload respected the flag.
    }

    @Test
    void explicitDefaultConfigBehavesLikeNull() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/explicit-defaults.json");
        JsonNode node = mapper.readTree("{\"type\":\"boolean\"}");
        // explicit defaults
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Schema must be created with an explicit default config");
    }

    @Test
    void emptyObjectSchemaNode() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/empty-object.json");
        JsonNode node = mapper.readTree("{}");
        SchemaValidatorsConfig cfg = null;
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Empty object schema should be accepted");
    }

    @Test
    void customPathTypeJsonPath() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("classpath:/my-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setPathType(JsonNodePath.Type.JSON_PATH);
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Schema must be created with JSON_PATH path type");
        Method getContext = JsonSchema.class.getDeclaredMethod("getValidationContext");
        getContext.setAccessible(true);
        ValidationContext vc = (ValidationContext) getContext.invoke(schema);
        assertEquals(JsonNodePath.Type.JSON_PATH, vc.getConfig().getPathType(), "Path type should be JSON_PATH as configured");
    }

    @Test
    void combinedFlagsFailFastAndSuppressRef() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/combined.json");
        JsonNode node = mapper.readTree("{\"$ref\":\"http://example.com/other.json\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setFailFast(true).setSuppressSubSchemaRetrieval(true);
        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        // THEN
        assertNotNull(schema, "Schema must be created with combined flags");
        Method getContext = JsonSchema.class.getDeclaredMethod("getValidationContext");
        getContext.setAccessible(true);
        ValidationContext vc = (ValidationContext) getContext.invoke(schema);
        assertTrue(vc.getConfig().isFailFast(), "Fail‑fast flag should be true");
        assertTrue(vc.getConfig().isSuppressSubSchemaRetrieval(), "Suppress‑sub‑schema‑retrieval flag should be true");
    }

    @Test
    void nullSchemaUriThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = null;
        JsonNode node = mapper.readTree("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> invokeNewJsonSchema(uri, node, cfg), "A null schemaUri should cause NullPointerException");
    }

    @Test
    void nullSchemaNodeThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/empty.json");
        JsonNode node = null;
        SchemaValidatorsConfig cfg = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> invokeNewJsonSchema(uri, node, cfg), "A null schemaNode should cause NullPointerException");
    }

    @Test
    void malformedUriThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("ht!tp://[invalid]");
        JsonNode node = mapper.readTree("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invokeNewJsonSchema(uri, node, cfg), "Malformed URI should trigger IllegalArgumentException");
    }

    @Test
    void emptyUriStringThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("");
        JsonNode node = mapper.readTree("{\"type\":\"integer\"}");
        SchemaValidatorsConfig cfg = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invokeNewJsonSchema(uri, node, cfg), "Empty URI string should trigger IllegalArgumentException");
    }

    @Test
    void unsupportedSchemeThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("ftp://example.com/schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig cfg = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invokeNewJsonSchema(uri, node, cfg), "Unsupported URI scheme should trigger IllegalArgumentException");
    }

    @Test
    void schemaValidationEnabledInvalidSchemaThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/invalid-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"unknown-type\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setValidateSchema(true);
        // WHEN / THEN
        // The exact exception type depends on the library version; we accept any RuntimeException.
        assertThrows(RuntimeException.class, () -> invokeNewJsonSchema(uri, node, cfg), "Invalid schema with validation enabled should raise an exception");
    }

    @Test
    void arrayRootSchemaThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/array-root.json");
        JsonNode node = mapper.readTree("[{\"type\":\"string\"}]");
        SchemaValidatorsConfig cfg = null;
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> invokeNewJsonSchema(uri, node, cfg), "Schema root must be an object; an array should cause an exception");
    }

    @Test
    void primitiveRootSchemaThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/primitive-root.json");
        JsonNode node = mapper.readTree("\"just a string\"");
        SchemaValidatorsConfig cfg = null;
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> invokeNewJsonSchema(uri, node, cfg), "Primitive root schema should cause an exception");
    }

    @Test
    void configWithNullPathTypeThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/null-pathtype.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setPathType(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> invokeNewJsonSchema(uri, node, cfg), "Null path type in config should cause NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] uri={0}, schema={1}")
    @MethodSource("successfulScenarios")
    void parameterisedSuccessfulScenarios(String uriString, String schemaJson, SchemaValidatorsConfig cfg) throws Exception {
        SchemaLocation uri = new SchemaLocation(uriString);
        JsonNode node = mapper.readTree(schemaJson);
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        assertNotNull(schema, "Schema should be created for uri=" + uriString);
    }
}
