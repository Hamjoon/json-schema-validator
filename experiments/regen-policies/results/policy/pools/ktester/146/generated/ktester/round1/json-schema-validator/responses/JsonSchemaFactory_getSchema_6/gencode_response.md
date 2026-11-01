```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.networknt.schema.SpecVersion.VersionFlag;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#getSchema(URI)}.
 *
 * <p>The test suite covers:</p>
 * <ul>
 *   <li>Successful loading from classpath, file system and HTTP.</li>
 *   <li>Handling of malformed, unsupported or null URIs.</li>
 *   <li>Loading failures (missing resources, empty files, invalid JSON).</li>
 *   <li>Cache behaviour (same instance on repeated calls).</li>
 *   <li>Effect of the default {@link SchemaValidatorsConfig} (basic $ref resolution).</li>
 * </ul>
 *
 * <p>All tests use the public factory builder API – no reflection on private members
 * is required.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /** Factory used in all test cases. */
    private static JsonSchemaFactory factory;

    /** Simple schema used for the tests – a trivial object schema. */
    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"$id\": \"https://example.com/simple-schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\"name\": {\"type\": \"string\"}},\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    /** A schema that contains a $ref to {@value #SIMPLE_SCHEMA_URI}. */
    private static final String REF_SCHEMA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"$id\": \"https://example.com/ref-schema\",\n"
            + "  \"$ref\": \"https://example.com/simple-schema\"\n"
            + "}";

    /** The URI under which the simple schema is registered for class‑path tests. */
    private static final URI SIMPLE_SCHEMA_URI = URI.create("https://example.com/simple-schema");

    /** The URI under which the $ref schema is registered for class‑path tests. */
    private static final URI REF_SCHEMA_URI = URI.create("https://example.com/ref-schema");

    @BeforeAll
    static void setupBeforeAll() {
        /*
         * Build a factory that knows about the simple and ref schemas.  The builder
         * API allows us to plug a custom map of URI → schema content without touching
         * any private members of JsonSchemaFactory.
         */
        factory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Map.of(
                                        SIMPLE_SCHEMA_URI.toString(), SIMPLE_SCHEMA,
                                        REF_SCHEMA_URI.toString(), REF_SCHEMA))));
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /* --------------------------------------------------------------
     *  1.  Successful loads
     * -------------------------------------------------------------- */

    @Test
    void shouldLoadSchemaFromClasspath() {
        // classpath URI – the test resources folder contains schemas/simple-schema.json
        URI uri = URI.create("classpath:/schemas/simple-schema.json");
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText(),
                "Root type should be 'object' as defined in the simple schema");
    }

    @Test
    void shouldLoadSchemaFromFileSystem(@TempDir Path tempDir) throws IOException {
        // create a temporary file containing the simple schema
        Path file = Files.writeString(tempDir.resolve("external-schema.json"), SIMPLE_SCHEMA);
        URI uri = file.toUri(); // e.g. file:/tmp/.../external-schema.json
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText(),
                "File‑system schema should be parsed correctly");
    }

    @Test
    void shouldLoadSchemaFromHttp() throws IOException {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setBody(SIMPLE_SCHEMA)
                    .addHeader("Content-Type", "application/json"));
            server.start();

            URI uri = URI.create(server.url("/remote-schema.json").toString());
            JsonSchema schema = factory.getSchema(uri);
            assertNotNull(schema);
            assertEquals("object", schema.getSchemaNode().get("type").asText(),
                    "HTTP‑served schema should be parsed correctly");
        }
    }

    /* --------------------------------------------------------------
     *  2.  Failure & argument validation
     * -------------------------------------------------------------- */

    @Test
    void shouldThrowWhenUriIsNull() {
        assertThrows(NullPointerException.class,
                () -> factory.getSchema((URI) null),
                "Passing null should result in NPE");
    }

    @Test
    void shouldThrowWhenUriIsMalformed() {
        assertThrows(URISyntaxException.class,
                () -> new URI("ht!tp://bad[uri]"),
                "Construction of a malformed URI must fail before getSchema is called");
    }

    @ParameterizedTest
    @MethodSource("unsupportedSchemeProvider")
    void shouldRejectUnsupportedScheme(URI uri) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(uri));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported"),
                "Exception message should mention unsupported scheme");
    }

    private static Stream<Arguments> unsupportedSchemeProvider() throws URISyntaxException {
        return Stream.of(
                Arguments.of(new URI("mailto:user@example.com")),
                Arguments.of(new URI("ftp://example.com/schema.json")),
                Arguments.of(new URI("data:application/json,{\"type\":\"object\"}"))
        );
    }

    @Test
    void shouldThrowWhenResourceDoesNotExist() {
        URI missing = URI.create("classpath:/schemas/missing-schema.json");
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(missing),
                "Missing classpath resource should result in an exception");
    }

    @Test
    void shouldThrowWhenFileDoesNotExist(@TempDir Path tempDir) {
        URI fileUri = tempDir.resolve("nonexistent.json").toUri();
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(fileUri),
                "Non‑existent file should cause a runtime exception");
    }

    @Test
    void shouldThrowWhenSchemaIsEmpty(@TempDir Path tempDir) throws IOException {
        Path emptyFile = Files.createFile(tempDir.resolve("empty-schema.json"));
        URI uri = emptyFile.toUri();
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Empty schema file should cause a parsing exception");
    }

    @Test
    void shouldThrowWhenSchemaIsInvalidJson(@TempDir Path tempDir) throws IOException {
        Path invalidFile = Files.writeString(tempDir.resolve("invalid.json"),
                "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } "); // missing closing braces
        URI uri = invalidFile.toUri();
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Invalid JSON should cause a parsing exception");
    }

    @Test
    void shouldRejectRelativeUri() {
        URI relative = URI.create("schemas/simple-schema.json");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(relative));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupported")
                || ex.getMessage().toLowerCase().contains("relative"),
                "Relative URIs must be rejected");
    }

    @Test
    void shouldRejectEmptyUriString() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(URI.create("")),
                "Empty URI string should be rejected");
    }

    /* --------------------------------------------------------------
     *  3.  Cache behaviour
     * -------------------------------------------------------------- */

    @Test
    void shouldReturnSameInstanceOnRepeatedCalls() {
        URI uri = URI.create("classpath:/schemas/simple-schema.json");
        JsonSchema first = factory.getSchema(uri);
        JsonSchema second = factory.getSchema(uri);
        assertSame(first, second, "Factory should cache schemas and return the same instance");
    }

    /* --------------------------------------------------------------
     *  4.  $ref handling (default SchemaValidatorsConfig)
     * -------------------------------------------------------------- */

    @Test
    void shouldResolveRefWhenUsingDefaultConfig() {
        // The ref schema points to SIMPLE_SCHEMA_URI which is pre‑registered.
        JsonSchema schema = factory.getSchema(REF_SCHEMA_URI);
        assertNotNull(schema, "Ref schema must be loaded");

        // Validate a JSON document that satisfies the referenced simple schema.
        JsonNode valid = JsonMapperFactory.getInstance().readTree("{\"name\":\"John\"}");
        ValidationResult result = schema.validate(valid);
        assertTrue(result.isSuccess(), "Document should validate successfully via $ref resolution");
    }

    /* --------------------------------------------------------------
     *  5.  HTTP error handling (404)
     * -------------------------------------------------------------- */

    @Test
    void shouldThrowWhenHttpReturns404() throws IOException {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(404));
            server.start();

            URI uri = URI.create(server.url("/missing-schema.json").toString());
            assertThrows(RuntimeException.class,
                    () -> factory.getSchema(uri),
                    "HTTP 404 should be propagated as an exception");
        }
    }

    /* --------------------------------------------------------------
     *  6.  Unreachable host handling
     * -------------------------------------------------------------- */

    @Test
    void shouldThrowWhenHttpHostIsUnreachable() {
        // Use a non‑routable hostname; the connection attempt will fail quickly.
        URI uri = URI.create("http://nonexistent-host-12345.local/schema.json");
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Connection failures should be wrapped in a runtime exception");
    }

    /* --------------------------------------------------------------
     *  7.  Fragment & query handling – they must be ignored
     * -------------------------------------------------------------- */

    @Test
    void shouldIgnoreFragmentWhenLoading() {
        URI uri = URI.create("classpath:/schemas/simple-schema.json#section");
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText(),
                "Fragment part must be ignored during loading");
    }

    @Test
    void shouldIgnoreQueryWhenLoading() {
        URI uri = URI.create("classpath:/schemas/simple-schema.json?version=1");
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText(),
                "Query part must be ignored during loading");
    }

    /* --------------------------------------------------------------
     *  8.  Jar scheme handling – we simulate by loading a classpath resource
     *      via the jar URL format (the underlying loader treats it as classpath).
     * -------------------------------------------------------------- */

    @Test
    void shouldLoadSchemaFromJarScheme() throws URISyntaxException {
        // The jar URL points to the same classpath resource used earlier.
        // The exact path depends on the test runtime; we construct it from the
        // known classpath location.
        URI uri = new URI("jar:" + getClass().getClassLoader()
                .getResource("schemas/simple-schema.json").toString() + "!/");
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText(),
                "Jar scheme schema must be loaded correctly");
    }

    /* --------------------------------------------------------------
     *  Helper methods
     * -------------------------------------------------------------- */

    /**
     * Reads a JSON string into a {@link JsonNode} using the factory's default mapper.
     */
    private static JsonNode readJson(String json) {
        try {
            return JsonMapperFactory.getInstance().readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
```