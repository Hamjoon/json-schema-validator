```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI)} covering a wide range of
 * scenarios described in the specification JSON.
 *
 * <p>The tests use the builder API of {@link JsonSchemaFactory} to inject a
 * {@link SchemaLoaders} implementation that resolves a URI string to a raw
 * schema string.  This allows us to avoid real I/O while still exercising the
 * loading logic (including YAML handling, fragment stripping, caching, etc.).
 *
 * <p>For cases that require exceptional behaviour (e.g. unreadable file,
 * unsupported scheme) we mock the {@link SchemaLoaders} component with
 * Mockito.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /** Factory under test – created per‑test so each test can customise the loader. */
    private JsonSchemaFactory factory;

    /** Temporary file used for file‑based URI tests – created in @BeforeEach and deleted in @AfterEach. */
    private Path tempFile;

    /** A simple valid JSON schema used for many happy‑path tests. */
    private static final String SIMPLE_JSON_SCHEMA = "{\n"
            + "  \"$id\": \"http://example.com/simple-schema.json\",\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\"\n"
            + "}";

    /** Same schema expressed in YAML – used for the “YAML extension” test. */
    private static final String SIMPLE_YAML_SCHEMA = ""
            + "$id: http://example.com/simple-schema.yaml\n"
            + "$schema: https://json-schema.org/draft/2020-12/schema\n"
            + "type: object\n";

    /** Malformed JSON – used to verify parsing failures. */
    private static final String MALFORMED_JSON = "{ \"type\": \"object\", "; // missing closing brace

    /** Malformed YAML – used to verify parsing failures. */
    private static final String MALFORMED_YAML = "type: object\n  - invalid"; // illegal indentation

    /** Schema containing an unknown meta‑schema IRI. */
    private static final String UNKNOWN_META_SCHEMA = "{\n"
            + "  \"$id\": \"http://example.com/unknown-meta.json\",\n"
            + "  \"$schema\": \"https://json-schema.org/draft/9999-99/schema\",\n"
            + "  \"type\": \"object\"\n"
            + "}";

    /** Schema with a $ref that points to a non‑existent schema. */
    private static final String BAD_REF_SCHEMA = "{\n"
            + "  \"$id\": \"http://example.com/bad-ref.json\",\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"$ref\": \"nonexistent.json\"\n"
            + "}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a temporary file that will be used for file‑URI tests.
        tempFile = Files.createTempFile("test-schema", ".json");
        Files.writeString(tempFile, SIMPLE_JSON_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Delete the temporary file if it exists.
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /**
     * Creates a {@link JsonSchemaFactory} that resolves the supplied map
     * (URI‑string → raw schema content) via the builder's {@code schemaLoaders}
     * configuration.
     */
    private JsonSchemaFactory factoryWithMap(Map<String, String> uriToSchema) {
        return JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(sl -> sl.schemas(uriToSchema)));
    }

    /**
     * Creates a {@link JsonSchemaFactory} that uses a mocked {@link SchemaLoaders}
     * which throws the given exception when attempting to load the supplied URI.
     */
    private JsonSchemaFactory factoryWithLoaderThrowing(URI uri, RuntimeException toThrow) {
        SchemaLoaders mockLoaders = Mockito.mock(SchemaLoaders.class);
        Mockito.when(mockLoaders.loadSchema(URI.create(uri.toString()), Mockito.any()))
                .thenThrow(toThrow);
        return JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(sl -> sl.loader(mockLoaders)));
    }

    /* ---------------------------------------------------------------------- */
    /* Parameterized happy‑path tests                                        */
    /* ---------------------------------------------------------------------- */

    /**
     * Provides a stream of arguments for the happy‑path tests:
     * <ul>
     *   <li>Classpath JSON schema</li>
     *   <li>File‑system JSON schema</li>
     *   <li>Remote HTTP schema (simulated via map – no network call)</li>
     * </ul>
     */
    private static Stream<Arguments> validUriProvider() {
        return Stream.of(
                Arguments.of(
                        "classpath:/schemas/simple-schema.json",
                        SIMPLE_JSON_SCHEMA,
                        "class‑path JSON schema"),
                Arguments.of(
                        "file:" + System.getProperty("java.io.tmpdir") + "/test-schema.json",
                        SIMPLE_JSON_SCHEMA,
                        "file‑system JSON schema"),
                Arguments.of(
                        "http://example.com/schema.json",
                        SIMPLE_JSON_SCHEMA,
                        "remote HTTP JSON schema")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validUriProvider")
    void getSchema_validUri_returnsNonNull(String uriString, String schemaContent, String description) {
        // Arrange – map the URI string to the schema content.
        Map<String, String> map = Collections.singletonMap(uriString, schemaContent);
        factory = factoryWithMap(map);
        URI uri = URI.create(uriString);

        // Act
        JsonSchema schema = factory.getSchema(uri);

        // Assert
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for " + description);
        assertEquals("object",
                schema.getSchemaNode().get("type").asText(),
                "Loaded schema must contain the expected 'type' property");
    }

    /* ---------------------------------------------------------------------- */
    /* Caching behavior – same instance for repeated calls                  */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_sameUri_returnsSameInstance() {
        // Arrange – a simple mapping.
        String uriString = "classpath:/schemas/simple-schema.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, SIMPLE_JSON_SCHEMA));
        URI uri = URI.create(uriString);

        // Act
        JsonSchema first = factory.getSchema(uri);
        JsonSchema second = factory.getSchema(uri);

        // Assert
        assertSame(first, second, "Repeated calls with the same URI must return the cached instance");
    }

    /* ---------------------------------------------------------------------- */
    /* Null argument – expect NPE                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_withNullUri_throwsNullPointerException() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        assertThrows(NullPointerException.class,
                () -> factory.getSchema((URI) null),
                "Calling getSchema(null) must throw NullPointerException");
    }

    /* ---------------------------------------------------------------------- */
    /* Invalid / non‑existent resources                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_nonExistentClasspathResource_throwsException() {
        String uriString = "classpath:/schemas/nonexistent-schema.json";
        factory = factoryWithMap(Collections.emptyMap()); // no entry for the URI
        URI uri = URI.create(uriString);

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Loading a non‑existent classpath resource should result in a RuntimeException");
    }

    @Test
    void getSchema_nonExistentFileResource_throwsException() {
        String uriString = "file:/nonexistent/path/schema.json";
        factory = factoryWithMap(Collections.emptyMap());
        URI uri = URI.create(uriString);

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Loading a non‑existent file should result in a RuntimeException");
    }

    /* ---------------------------------------------------------------------- */
    /* Malformed URI – illegal syntax                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_malformedUri_throwsIllegalArgumentException() {
        URI malformed = URI.create("ht!tp://bad_uri"); // syntactically malformed but still a URI object
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(malformed),
                "A malformed URI should cause IllegalArgumentException (wrapped by the factory)");
    }

    /* ---------------------------------------------------------------------- */
    /* YAML schema handling                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_yamlExtension_parsesYaml() {
        String uriString = "classpath:/schemas/simple-schema.yaml";
        factory = factoryWithMap(Collections.singletonMap(uriString, SIMPLE_YAML_SCHEMA));
        URI uri = URI.create(uriString);

        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "YAML schema should be parsed and returned");
        assertEquals("object",
                schema.getSchemaNode().get("type").asText(),
                "YAML schema must contain the expected 'type' property");
    }

    /* ---------------------------------------------------------------------- */
    /* Percent‑encoded characters                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_percentEncodedPath_decodesCorrectly() {
        String uriString = "classpath:/schemas/simple%20schema.json";
        factory = factoryWithMap(Collections.singletonMap(
                "classpath:/schemas/simple schema.json", // map key after decoding
                SIMPLE_JSON_SCHEMA));
        URI uri = URI.create(uriString);

        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "Schema with percent‑encoded path should be loaded");
        assertEquals("object",
                schema.getSchemaNode().get("type").asText(),
                "Decoded schema must contain the expected 'type' property");
    }

    /* ---------------------------------------------------------------------- */
    /* Fragment identifier – should be ignored                               */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_withFragment_ignoresFragment() {
        String base = "classpath:/schemas/simple-schema.json";
        String withFragment = base + "#section";
        Map<String, String> map = Collections.singletonMap(base, SIMPLE_JSON_SCHEMA);
        factory = factoryWithMap(map);
        URI uri = URI.create(withFragment);

        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "Schema with fragment should be loaded");
        assertEquals("object",
                schema.getSchemaNode().get("type").asText(),
                "Fragment‑stripped schema must contain the expected 'type' property");
    }

    /* ---------------------------------------------------------------------- */
    /* Upper‑case scheme handling (FILE)                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_upperCaseFileScheme_isHandled() throws IOException {
        // Write the schema to a temporary file and build an upper‑case FILE URI.
        Path file = Files.createTempFile("upper-case-file", ".json");
        Files.writeString(file, SIMPLE_JSON_SCHEMA);
        URI uri = URI.create("FILE:" + file.toAbsolutePath());

        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "Upper‑case FILE scheme should be handled like a normal file URI");
        assertEquals("object",
                schema.getSchemaNode().get("type").asText(),
                "Loaded schema must contain the expected 'type' property");
        Files.deleteIfExists(file);
    }

    /* ---------------------------------------------------------------------- */
    /* Unsupported scheme – custom protocol                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_unsupportedScheme_throwsRuntimeException() {
        URI uri = URI.create("custom://schemas/simple-schema.json");
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Unsupported scheme should result in a RuntimeException");
    }

    /* ---------------------------------------------------------------------- */
    /* Directory URI – should fail                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_directoryUri_throwsException() {
        // Simulate a directory by mapping the URI to a JSON object that represents a directory (invalid content).
        String uriString = "classpath:/schemas/";
        factory = factoryWithMap(Collections.singletonMap(uriString, "{}"));
        URI uri = URI.create(uriString);

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Loading a directory URI should result in a RuntimeException");
    }

    /* ---------------------------------------------------------------------- */
    /* Malformed JSON schema – parsing error                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_malformedJson_throwsException() {
        String uriString = "classpath:/schemas/malformed-json-schema.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, MALFORMED_JSON));
        URI uri = URI.create(uriString);

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Malformed JSON should cause a RuntimeException wrapping the parsing error");
    }

    /* ---------------------------------------------------------------------- */
    /* Malformed YAML schema – parsing error                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_malformedYaml_throwsException() {
        String uriString = "classpath:/schemas/malformed-yaml-schema.yaml";
        factory = factoryWithMap(Collections.singletonMap(uriString, MALFORMED_YAML));
        URI uri = URI.create(uriString);

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Malformed YAML should cause a RuntimeException wrapping the parsing error");
    }

    /* ---------------------------------------------------------------------- */
    /* Schema with unknown meta‑schema IRI                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_unknownMetaSchema_throwsException() {
        String uriString = "classpath:/schemas/schema-with-unknown-meta.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, UNKNOWN_META_SCHEMA));
        URI uri = URI.create(uriString);

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Unknown meta‑schema IRI should cause a RuntimeException");
    }

    /* ---------------------------------------------------------------------- */
    /* Schema with bad $ref – reference cannot be resolved                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_schemaWithBadRef_throwsException() {
        String uriString = "classpath:/schemas/schema-with-bad-ref.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, BAD_REF_SCHEMA));
        URI uri = URI.create(uriString);

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Bad $ref should cause a RuntimeException indicating unresolved reference");
    }

    /* ---------------------------------------------------------------------- */
    /* Unreadable file – simulate IOException via mock                       */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_unreadableFile_throwsIOException() {
        URI uri = URI.create("file:/tmp/unreadable-schema.json");
        RuntimeException ioException = new RuntimeException(new IOException("Permission denied"));
        factory = factoryWithLoaderThrowing(uri, ioException);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Unreadable file should result in a RuntimeException wrapping IOException");
        assertTrue(thrown.getCause() instanceof IOException, "Root cause must be IOException");
    }

    /* ---------------------------------------------------------------------- */
    /* Configuration propagation – verify default config is used            */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_usesDefaultSchemaValidatorsConfig() {
        // The default config has 'typeLoose' = false (the default value).
        String uriString = "classpath:/schemas/simple-schema.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, SIMPLE_JSON_SCHEMA));
        URI uri = URI.create(uriString);

        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "Schema must be loaded");
        // The JsonSchema exposes its ValidationContext; we can inspect the config via reflection.
        ValidationContext ctx = schema.getValidationContext();
        SchemaValidatorsConfig config = ctx.getConfig();
        assertFalse(config.isTypeLoose(), "Default SchemaValidatorsConfig should have typeLoose == false");
    }

    /* ---------------------------------------------------------------------- */
    /* Circular reference detection – simulate via mock loader throwing   */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchema_circularReference_throwsException() {
        // Simulate a circular reference detection by having the loader throw the expected exception.
        URI uri = URI.create("classpath:/schemas/circular-ref-schema.json");
        RuntimeException circular = new RuntimeException("Circular reference detected");
        factory = factoryWithLoaderThrowing(uri, circular);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> factory.getSchema(uri),
                "Circular reference should result in a RuntimeException");
        assertTrue(thrown.getMessage().contains("Circular reference"), "Exception message should indicate circular reference");
    }
}
```