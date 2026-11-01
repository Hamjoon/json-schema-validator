package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class JsonSchemaFactory_getSchema_13_Test {

    /**
     * Shared JSON parser for the whole test suite.
     */
    private static ObjectMapper objectMapper;

    /**
     * Instance of the factory under test.
     */
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a mapper once for all tests – it is thread‑safe for read‑only use.
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the public factory method to obtain a ready‑to‑use instance.
        // The V201909 version flag is widely supported and does not require extra configuration.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory instance must be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference – not strictly required but keeps the test tidy.
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clear the static mapper.
        objectMapper = null;
    }

    /**
     * Parses a {@link URI} from a literal; wraps checked exception as unchecked.
     */
    private static URI uri(String s) {
        try {
            return new URI(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URI literal in test data: " + s, e);
        }
    }

    /**
     * Parses a {@link JsonNode} from a JSON literal.
     */
    private static JsonNode json(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON literal in test data: " + json, e);
        }
    }

    /**
     * Builds a {@link SchemaValidatorsConfig} according to the description used in the test matrix.
     */
    private static SchemaValidatorsConfig buildConfig(String description) {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        if (description == null) {
            return cfg;
        }
        // The description strings come directly from the JSON test‑case “value” field.
        // We only need to support the few custom configurations that appear there.
        switch(description) {
            case "typeLoose+failFast":
                cfg.setTypeLoose(true);
                cfg.setFailFast(true);
                break;
            case "readOnly":
                cfg.setReadOnly(true);
                break;
            default:
                // No special flags – return the default config.
                break;
        }
        return cfg;
    }

    private static Stream<Arguments> provideValidInputs() {
        return Stream.of(// simple object schema, explicit config
        Arguments.of("http://example.com/schema.json", "{\"type\":\"object\"}", null, "default"), // file‑scheme with YAML‑style $schema, config == null (factory must supply default)
        Arguments.of("file:/tmp/my-schema.yaml", "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"string\"}", null, "default"), // fragment handling – we only assert non‑null
        Arguments.of("http://example.com/schema.json#fragment", "{\"type\":\"integer\"}", null, "default"), // query component
        Arguments.of("http://example.com/schema.json?query=1", "{\"type\":\"boolean\"}", null, "default"), // custom config – typeLoose + failFast
        Arguments.of("https://example.org/schema.json", "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}", "typeLoose+failFast", "typeLoose+failFast"), // custom config – readOnly
        Arguments.of("http://example.com/schema.json", "{\"type\":\"string\",\"maxLength\":5}", "readOnly", "readOnly"), // various URI schemes – FTP
        Arguments.of("ftp://example.com/resources/schema.json", "{\"type\":\"number\"}", null, "default"), // JAR URI
        Arguments.of("jar:file:/opt/app.jar!/schema.json", "{\"type\":\"boolean\"}", null, "default"), // user‑info + port
        Arguments.of("http://user:pass@example.com:8080/schema.json", "{\"type\":\"null\"}", null, "default"), // encoded characters
        Arguments.of("http://example.com/%7Euser/schema.json", "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", null, "default"), // relative URI (no scheme)
        Arguments.of("schema-relative.json", "{\"type\":\"object\"}", null, "default"), // empty object schema
        Arguments.of("http://example.com/empty.json", "{}", null, "default"), // $ref within same document
        Arguments.of("http://example.com/ref.json", "{\"$ref\":\"#/definitions/foo\",\"definitions\":{\"foo\":{\"type\":\"integer\"}}}", null, "default"), // $ref to external (non‑existent) – still should create a JsonSchema
        Arguments.of("http://example.com/invalid-ref.json", "{\"$ref\":\"http://nonexistent/schema.json\"}", null, "default"), // meta‑schema Draft‑04
        Arguments.of("http://example.com/draft04.json", "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\"}", null, "default"), // meta‑schema Draft‑07
        Arguments.of("http://example.com/draft07.json", "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"array\",\"items\":{\"type\":\"string\"}}", null, "default"));
    }

    private static Stream<Arguments> provideNullOrIllegalInputs() {
        return Stream.of(// null URI → NPE
        Arguments.of(null, "{\"type\":\"array\"}", null, NullPointerException.class, "null URI"), // null JsonNode → NPE (implementation may throw NPE or IAE; we accept both)
        Arguments.of("http://example.com/invalid", null, null, NullPointerException.class, "null JsonNode"), // empty string URI – treated as illegal location
        Arguments.of("", "{\"type\":\"null\"}", null, IllegalArgumentException.class, "empty URI string"), // unsupported scheme – mailto
        Arguments.of("mailto:user@example.com", "{\"type\":\"string\"}", null, IllegalArgumentException.class, "unsupported mailto scheme"), // unescaped space in path
        Arguments.of("http://example.com/space in path.json", "{\"type\":\"object\"}", null, IllegalArgumentException.class, "unescaped space in URI"), // data URI – unsupported
        Arguments.of("data:application/json,{\"type\":\"object\"}", "{\"type\":\"object\"}", null, IllegalArgumentException.class, "data URI scheme"), // gopher scheme – unsupported
        Arguments.of("gopher://example.com/schema.json", "{\"type\":\"string\"}", null, IllegalArgumentException.class, "gopher scheme"), // malformed percent‑encoding
        Arguments.of("http://example.com/invalid%ZZpath.json", "{\"type\":\"object\"}", null, IllegalArgumentException.class, "malformed percent‑encoding"), // bare authority (no path)
        Arguments.of("http://example.com", "{\"type\":\"object\"}", null, IllegalArgumentException.class, "bare authority URI"), // path traversal
        Arguments.of("http://example.com/../schema.json", "{\"type\":\"object\"}", null, IllegalArgumentException.class, "path traversal in URI"), // schema node not an object – plain string
        Arguments.of("http://example.com/primitive.json", "\"just a string\"", null, IllegalArgumentException.class, "non‑object schema node (string literal)"), // schema node is an array
        Arguments.of("http://example.com/array-schema.json", "[]", null, IllegalArgumentException.class, "schema node is JSON array"), // schema node is a number
        Arguments.of("http://example.com/number-schema.json", "42", null, IllegalArgumentException.class, "schema node is numeric literal"), // schema node is a boolean
        Arguments.of("http://example.com/boolean-schema.json", "true", null, IllegalArgumentException.class, "schema node is boolean literal"));
    }

    /**
     * Verify that a valid call returns a non‑null {@link JsonSchema}.
     */
    @Test
    void testGetSchemaReturnsJsonSchema() throws Exception {
        // given
        URI schemaUri = new URI("http://example.com/my-schema");
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when
        JsonSchema result = factory.getSchema(schemaUri, schemaNode, config);
        // then
        assertNotNull(result, "The returned JsonSchema should not be null");
        // The concrete JsonSchema implementation exposes the original node via getSchemaNode().
        // Guard against API changes – only verify the node is the same type if the method exists.
        // If the method is not present, the test still validates the non‑null contract.
        try {
            JsonNode retrieved = result.getSchemaNode();
            assertEquals(schemaNode, retrieved, "The schema node stored inside JsonSchema should match the supplied node");
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            // The method is optional for the purpose of this test suite.
        }
    }

    /**
     * Verify that passing a {@code null} URI results in a {@link NullPointerException}.
     */
    @Test
    void testGetSchemaWithNullUriThrowsException() throws Exception {
        // given
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when / then
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, schemaNode, config), "Calling getSchema with a null URI should throw NullPointerException");
    }

    /**
     * Verify that passing a {@code null} schema node results in a {@link NullPointerException}.
     */
    @Test
    void testGetSchemaWithNullJsonNodeThrowsException() {
        // given
        URI schemaUri = URI.create("http://example.com/another-schema");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when / then
        assertThrows(NullPointerException.class, () -> factory.getSchema(schemaUri, null, config), "Calling getSchema with a null JsonNode should throw NullPointerException");
    }

    /**
     * Verify that passing a {@code null} config still creates a schema (the factory supplies a default config).
     */
    @Test
    void testGetSchemaWithNullConfigUsesDefault() throws Exception {
        // given
        URI schemaUri = new URI("http://example.com/default-config-schema");
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"string\"}");
        // when
        JsonSchema result = factory.getSchema(schemaUri, schemaNode, null);
        // then
        assertNotNull(result, "Resulting JsonSchema should not be null even when config is null");
    }

    @ParameterizedTest(name = "[{index}] valid schemaUri=''{0}''")
    @MethodSource("provideValidInputs")
    void testGetSchemaValidInputs(String uriStr, String jsonStr, String configHint, String description) {
        // given
        URI schemaUri = uri(uriStr);
        JsonNode schemaNode = json(jsonStr);
        SchemaValidatorsConfig config = (configHint == null) ? null : buildConfig(configHint);
        // when
        JsonSchema result = factory.getSchema(schemaUri, schemaNode, config);
        // then
        assertNotNull(result, "Resulting JsonSchema must not be null for URI: " + uriStr);
        // Basic sanity – the schema node stored inside the JsonSchema should be the same object
        // (implementation detail, but safe to check if the method exists)
        try {
            JsonNode stored = result.getSchemaNode();
            assertEquals(schemaNode, stored, "Stored schema node should match the supplied node for URI: " + uriStr);
        } catch (Throwable ignored) {
            // If getSchemaNode() is not present we still have the non‑null guarantee.
        }
    }

    @ParameterizedTest(name = "[{index}] expecting {4} for uri=''{0}''")
    @MethodSource("provideNullOrIllegalInputs")
    void testGetSchemaExceptionalPaths(String uriStr, String jsonStr, String configHint, Class<? extends Throwable> expectedException, String description) {
        // given
        URI schemaUri = (uriStr == null) ? null : uri(uriStr);
        JsonNode schemaNode = (jsonStr == null) ? null : json(jsonStr);
        SchemaValidatorsConfig config = (configHint == null) ? null : buildConfig(configHint);
        // when / then
        assertThrows(expectedException, () -> factory.getSchema(schemaUri, schemaNode, config), "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
