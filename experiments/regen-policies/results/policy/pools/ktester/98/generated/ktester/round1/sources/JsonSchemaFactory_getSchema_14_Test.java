package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_14_Test {

    /**
     * Shared factory used by all tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * ObjectMapper used to build {@link JsonNode} instances from JSON strings.
     */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft – this is the most common version used in the
        // library’s own test‑suite.  The factory is thread‑safe and does not require any special
        // configuration for the simple scenarios exercised here.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    /**
     * Verifies that {@code getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)} returns a
     * non‑null {@link JsonSchema} instance when supplied with a valid location, a parsed schema
     * node and a default configuration.
     */
    @Test
    void getSchema_withValidLocationNodeAndConfig_returnsJsonSchema() throws Exception {
        // ----- given ---------------------------------------------------------
        // a minimal JSON schema (object type)
        String schemaJson = "{\"type\":\"object\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // a concrete {@link SchemaLocation}
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/simple-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);
        // a default configuration – the builder is part of the public API
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when ----------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(location, schemaNode, config);
        // ----- then ----------------------------------------------------------
        assertNotNull(jsonSchema, "The factory should produce a non‑null JsonSchema instance");
        // The JsonSchema API does not expose the original SchemaLocation directly, but we can
        // at least verify that the underlying schema node is the one we supplied.
        assertEquals(schemaNode, jsonSchema.getSchemaNode(), "The JsonSchema should contain the same schema node that was passed to the factory");
    }

    /**
     * Ensures that invoking {@code getSchema} repeatedly with the same {@link SchemaLocation}
     * and configuration yields the same {@link JsonSchema} instance when schema caching is enabled
     * (the default behaviour of the factory).
     */
    @Test
    void getSchema_multipleCallsWithSameParameters_returnSameInstance() throws Exception {
        // ----- given ---------------------------------------------------------
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/string-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when ----------------------------------------------------------
        JsonSchema first = factory.getSchema(location, schemaNode, config);
        JsonSchema second = factory.getSchema(location, schemaNode, config);
        // ----- then ----------------------------------------------------------
        assertSame(first, second, "When caching is enabled the factory should return the same JsonSchema instance");
    }

    /**
     * Happy‑path: all arguments are non‑null and refer to a valid schema.
     * The method should forward the parameters to {@code newJsonSchema} and return a usable {@link JsonSchema}.
     */
    @Test
    void getSchema_happyPath_allArgumentsNonNull_returnsJsonSchema() throws Exception {
        // ----- given ---------------------------------------------------------
        // a classpath resource that exists (src/test/resources/schema/example-schema.json)
        SchemaLocation location = new SchemaLocation("classpath:/schema/example-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when ----------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(location, node, config);
        // ----- then ----------------------------------------------------------
        assertNotNull(jsonSchema, "Factory must return a non‑null JsonSchema");
        // The schema node we supplied should be stored inside the JsonSchema.
        assertEquals(node, jsonSchema.getSchemaNode(), "The JsonSchema should contain the same schema node that was passed in");
        // The meta‑schema of the returned JsonSchema should be resolved from the supplied location.
        assertNotNull(jsonSchema.getMetaSchema(), "Meta‑schema must be resolved");
        // In most implementations the meta‑schema URI ends with the draft identifier,
        // e.g. http://json-schema.org/draft/2020-12/schema
        assertTrue(jsonSchema.getMetaSchema().getUri().contains("2020-12"), "Meta‑schema should correspond to the 2020‑12 draft");
    }

    /**
     * {@code jsonNode} is {@code null}. The implementation should treat it as an empty
     * {@link com.fasterxml.jackson.databind.node.ObjectNode} and still create a {@link JsonSchema}.
     */
    @Test
    void getSchema_nullJsonNode_returnsEmptyObjectSchema() throws Exception {
        // ----- given ---------------------------------------------------------
        SchemaLocation location = new SchemaLocation("classpath:/schema/example-schema.json");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when ----------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(location, null, config);
        // ----- then ----------------------------------------------------------
        assertNotNull(jsonSchema, "Factory must return a JsonSchema even when jsonNode is null");
        JsonNode schemaNode = jsonSchema.getSchemaNode();
        assertNotNull(schemaNode, "Internal schema node must not be null");
        assertTrue(schemaNode.isObject(), "When jsonNode is null the factory should create an empty ObjectNode");
        assertEquals(0, schemaNode.size(), "The empty ObjectNode should contain no fields");
    }

    /**
     * {@code config} is {@code null}. The factory should fall back to its default configuration.
     */
    @Test
    void getSchema_nullConfig_usesDefaultConfig() throws Exception {
        // ----- given ---------------------------------------------------------
        SchemaLocation location = new SchemaLocation("classpath:/schema/example-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        // ----- when ----------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(location, node, null);
        // ----- then ----------------------------------------------------------
        assertNotNull(jsonSchema, "Factory must return a JsonSchema when config is null");
        // The default config is internal – we verify that the schema was still created successfully.
        assertEquals(node, jsonSchema.getSchemaNode(), "Schema node must be the one we supplied even when config is null");
    }

    @Test
    void getSchema_nullSchemaUri_throwsNullPointerException() {
        // ----- given ---------------------------------------------------------
        JsonNode node = mapper.createObjectNode();
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when & then --------------------------------------------------
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, node, config), "A null SchemaLocation must cause a NullPointerException");
    }

    @Test
    void getSchema_allParametersNull_throwsNullPointerException() {
        // ----- when & then --------------------------------------------------
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, null, null), "All‑null parameters must result in a NullPointerException");
    }

    /**
     * The supplied {@link SchemaLocation} points to a non‑existent classpath resource.
     * The factory should propagate a {@link SchemaLoadingException} (or IOException).
     */
    @Test
    void getSchema_nonExistentResource_throwsSchemaLoadingException() throws Exception {
        // ----- given ---------------------------------------------------------
        SchemaLocation location = new SchemaLocation("classpath:/schema/non-existent.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when & then --------------------------------------------------
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(location, node, config), "Loading a non‑existent resource must raise an exception");
        assertTrue(ex.getClass().getSimpleName().endsWith("SchemaLoadingException") || ex instanceof IOException, "Expected SchemaLoadingException or IOException, got: " + ex);
    }

    /**
     * The supplied {@link SchemaLocation} uses an unsupported URI scheme (ftp).
     * The factory should reject it with {@link IllegalArgumentException}.
     */
    @Test
    void getSchema_unsupportedScheme_throwsIllegalArgumentException() throws Exception {
        // ----- given ---------------------------------------------------------
        SchemaLocation location = new SchemaLocation("ftp://example.com/schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when & then --------------------------------------------------
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, node, config), "Unsupported URI scheme must cause IllegalArgumentException");
    }

    /**
     * The supplied {@code jsonNode} is a JSON array rather than an object.
     * The factory expects the root of a JSON Schema to be an object node.
     */
    @Test
    void getSchema_jsonNodeIsArray_throwsIllegalArgumentException() throws Exception {
        // ----- given ---------------------------------------------------------
        SchemaLocation location = new SchemaLocation("classpath:/schema/example-schema.json");
        JsonNode arrayNode = mapper.readTree("[\"not\",\"an\",\"object\"]");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when & then --------------------------------------------------
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, arrayNode, config), "A JSON array as the schema root must cause IllegalArgumentException");
    }

    /**
     * The schema contains a $schema reference that points to a non‑existent meta‑schema.
     * The factory should attempt to load the meta‑schema and propagate a {@link SchemaLoadingException}.
     */
    @Test
    void getSchema_invalidMetaSchemaReference_throwsSchemaLoadingException() throws Exception {
        // ----- given ---------------------------------------------------------
        SchemaLocation location = new SchemaLocation("classpath:/schema/example-schema.json");
        String badMetaSchema = "{\"$schema\":\"http://invalid-meta-schema.org/draft-07/schema#\",\"type\":\"object\"}";
        JsonNode node = mapper.readTree(badMetaSchema);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ----- when & then --------------------------------------------------
        Exception ex = assertThrows(Exception.class, () -> factory.getSchema(location, node, config), "Invalid $schema reference must result in a loading exception");
        assertTrue(ex.getClass().getSimpleName().endsWith("SchemaLoadingException") || ex instanceof IOException, "Expected SchemaLoadingException or IOException, got: " + ex);
    }
}
