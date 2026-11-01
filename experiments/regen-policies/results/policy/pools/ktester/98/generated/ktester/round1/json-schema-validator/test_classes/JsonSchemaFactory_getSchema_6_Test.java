package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /**
     * Factory under test.
     */
    private JsonSchemaFactory factory;

    /**
     * URI of the test schema.
     */
    private URI schemaUri;

    /**
     * Simple JSON schema content.
     */
    private static final String SCHEMA_JSON = "{\n" + "  \"$id\": \"http://example.com/schema\",\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\"\n" + "}";

    /**
     * Temporary file used for file‑based URI tests – created in @BeforeEach and deleted in @AfterEach.
     */
    private Path tempFile;

    /**
     * A simple valid JSON schema used for many happy‑path tests.
     */
    private static final String SIMPLE_JSON_SCHEMA = "{\n" + "  \"$id\": \"http://example.com/simple-schema.json\",\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\"\n" + "}";

    /**
     * Same schema expressed in YAML – used for the “YAML extension” test.
     */
    private static final String SIMPLE_YAML_SCHEMA = "" + "$id: http://example.com/simple-schema.yaml\n" + "$schema: https://json-schema.org/draft/2020-12/schema\n" + "type: object\n";

    // missing closing brace
    private static final String MALFORMED_JSON = "{ \"type\": \"object\", ";

    // illegal indentation
    private static final String MALFORMED_YAML = "type: object\n  - invalid";

    /**
     * Schema containing an unknown meta‑schema IRI.
     */
    private static final String UNKNOWN_META_SCHEMA = "{\n" + "  \"$id\": \"http://example.com/unknown-meta.json\",\n" + "  \"$schema\": \"https://json-schema.org/draft/9999-99/schema\",\n" + "  \"type\": \"object\"\n" + "}";

    /**
     * Schema with a $ref that points to a non‑existent schema.
     */
    private static final String BAD_REF_SCHEMA = "{\n" + "  \"$id\": \"http://example.com/bad-ref.json\",\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"$ref\": \"nonexistent.json\"\n" + "}";

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the URI that will be used in the tests.
        schemaUri = URI.create("http://example.com/schema");
        /*
         * Build a JsonSchemaFactory that knows about the above schema.
         * The builder API allows us to inject a custom SchemaLoaders instance
         * that maps the URI string to the raw JSON schema text.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap(schemaUri.toString(), SCHEMA_JSON))));
    }

    @AfterEach
    void teardownAfterEach() {
        // Delete the temporary file if it exists.
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    /**
     * Creates a {@link JsonSchemaFactory} that resolves the supplied map
     * (URI‑string → raw schema content) via the builder's {@code schemaLoaders}
     * configuration.
     */
    private JsonSchemaFactory factoryWithMap(Map<String, String> uriToSchema) {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(sl -> sl.schemas(uriToSchema)));
    }

    /**
     * Creates a {@link JsonSchemaFactory} that uses a mocked {@link SchemaLoaders}
     * which throws the given exception when attempting to load the supplied URI.
     */
    private JsonSchemaFactory factoryWithLoaderThrowing(URI uri, RuntimeException toThrow) {
        SchemaLoaders mockLoaders = Mockito.mock(SchemaLoaders.class);
        Mockito.when(mockLoaders.loadSchema(URI.create(uri.toString()), Mockito.any())).thenThrow(toThrow);
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(sl -> sl.loader(mockLoaders)));
    }

    /**
     * Provides a stream of arguments for the happy‑path tests:
     * <ul>
     *   <li>Classpath JSON schema</li>
     *   <li>File‑system JSON schema</li>
     *   <li>Remote HTTP schema (simulated via map – no network call)</li>
     * </ul>
     */
    private static Stream<Arguments> validUriProvider() {
        return Stream.of(Arguments.of("classpath:/schemas/simple-schema.json", SIMPLE_JSON_SCHEMA, "class‑path JSON schema"), Arguments.of("file:" + System.getProperty("java.io.tmpdir") + "/test-schema.json", SIMPLE_JSON_SCHEMA, "file‑system JSON schema"), Arguments.of("http://example.com/schema.json", SIMPLE_JSON_SCHEMA, "remote HTTP JSON schema"));
    }

    @Test
    void getSchema_withValidUri_returnsNonNullSchema() {
        // given
        // (factory and schemaUri are prepared in @BeforeEach)
        // when
        JsonSchema schema = factory.getSchema(schemaUri);
        // then
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for a known URI");
        // The schema node should contain the "type" property we defined.
        assertEquals("object", schema.getSchemaNode().get("type").asText(), "The loaded schema should contain the expected 'type' definition");
    }

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

    @Test
    void getSchema_withNullUri_throwsNullPointerException() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        assertThrows(NullPointerException.class, () -> factory.getSchema((URI) null), "Calling getSchema(null) must throw NullPointerException");
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
        assertEquals("object", schema.getSchemaNode().get("type").asText(), "Loaded schema must contain the expected 'type' property");
    }

    @Test
    void getSchema_nonExistentClasspathResource_throwsException() {
        String uriString = "classpath:/schemas/nonexistent-schema.json";
        // no entry for the URI
        factory = factoryWithMap(Collections.emptyMap());
        URI uri = URI.create(uriString);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Loading a non‑existent classpath resource should result in a RuntimeException");
    }

    @Test
    void getSchema_nonExistentFileResource_throwsException() {
        String uriString = "file:/nonexistent/path/schema.json";
        factory = factoryWithMap(Collections.emptyMap());
        URI uri = URI.create(uriString);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Loading a non‑existent file should result in a RuntimeException");
    }

    @Test
    void getSchema_malformedUri_throwsIllegalArgumentException() {
        // syntactically malformed but still a URI object
        URI malformed = URI.create("ht!tp://bad_uri");
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(malformed), "A malformed URI should cause IllegalArgumentException (wrapped by the factory)");
    }

    @Test
    void getSchema_yamlExtension_parsesYaml() {
        String uriString = "classpath:/schemas/simple-schema.yaml";
        factory = factoryWithMap(Collections.singletonMap(uriString, SIMPLE_YAML_SCHEMA));
        URI uri = URI.create(uriString);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "YAML schema should be parsed and returned");
        assertEquals("object", schema.getSchemaNode().get("type").asText(), "YAML schema must contain the expected 'type' property");
    }

    @Test
    void getSchema_percentEncodedPath_decodesCorrectly() {
        String uriString = "classpath:/schemas/simple%20schema.json";
        factory = factoryWithMap(Collections.singletonMap(// map key after decoding
        "classpath:/schemas/simple schema.json", SIMPLE_JSON_SCHEMA));
        URI uri = URI.create(uriString);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "Schema with percent‑encoded path should be loaded");
        assertEquals("object", schema.getSchemaNode().get("type").asText(), "Decoded schema must contain the expected 'type' property");
    }

    @Test
    void getSchema_withFragment_ignoresFragment() {
        String base = "classpath:/schemas/simple-schema.json";
        String withFragment = base + "#section";
        Map<String, String> map = Collections.singletonMap(base, SIMPLE_JSON_SCHEMA);
        factory = factoryWithMap(map);
        URI uri = URI.create(withFragment);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "Schema with fragment should be loaded");
        assertEquals("object", schema.getSchemaNode().get("type").asText(), "Fragment‑stripped schema must contain the expected 'type' property");
    }

    @Test
    void getSchema_upperCaseFileScheme_isHandled() throws IOException {
        // Write the schema to a temporary file and build an upper‑case FILE URI.
        Path file = Files.createTempFile("upper-case-file", ".json");
        Files.writeString(file, SIMPLE_JSON_SCHEMA);
        URI uri = URI.create("FILE:" + file.toAbsolutePath());
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(uri);
        assertNotNull(schema, "Upper‑case FILE scheme should be handled like a normal file URI");
        assertEquals("object", schema.getSchemaNode().get("type").asText(), "Loaded schema must contain the expected 'type' property");
        Files.deleteIfExists(file);
    }

    @Test
    void getSchema_unsupportedScheme_throwsRuntimeException() {
        URI uri = URI.create("custom://schemas/simple-schema.json");
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Unsupported scheme should result in a RuntimeException");
    }

    @Test
    void getSchema_directoryUri_throwsException() {
        // Simulate a directory by mapping the URI to a JSON object that represents a directory (invalid content).
        String uriString = "classpath:/schemas/";
        factory = factoryWithMap(Collections.singletonMap(uriString, "{}"));
        URI uri = URI.create(uriString);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Loading a directory URI should result in a RuntimeException");
    }

    @Test
    void getSchema_malformedJson_throwsException() {
        String uriString = "classpath:/schemas/malformed-json-schema.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, MALFORMED_JSON));
        URI uri = URI.create(uriString);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Malformed JSON should cause a RuntimeException wrapping the parsing error");
    }

    @Test
    void getSchema_malformedYaml_throwsException() {
        String uriString = "classpath:/schemas/malformed-yaml-schema.yaml";
        factory = factoryWithMap(Collections.singletonMap(uriString, MALFORMED_YAML));
        URI uri = URI.create(uriString);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Malformed YAML should cause a RuntimeException wrapping the parsing error");
    }

    @Test
    void getSchema_unknownMetaSchema_throwsException() {
        String uriString = "classpath:/schemas/schema-with-unknown-meta.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, UNKNOWN_META_SCHEMA));
        URI uri = URI.create(uriString);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Unknown meta‑schema IRI should cause a RuntimeException");
    }

    @Test
    void getSchema_schemaWithBadRef_throwsException() {
        String uriString = "classpath:/schemas/schema-with-bad-ref.json";
        factory = factoryWithMap(Collections.singletonMap(uriString, BAD_REF_SCHEMA));
        URI uri = URI.create(uriString);
        assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Bad $ref should cause a RuntimeException indicating unresolved reference");
    }

    @Test
    void getSchema_unreadableFile_throwsIOException() {
        URI uri = URI.create("file:/tmp/unreadable-schema.json");
        RuntimeException ioException = new RuntimeException(new IOException("Permission denied"));
        factory = factoryWithLoaderThrowing(uri, ioException);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Unreadable file should result in a RuntimeException wrapping IOException");
        assertTrue(thrown.getCause() instanceof IOException, "Root cause must be IOException");
    }

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

    @Test
    void getSchema_circularReference_throwsException() {
        // Simulate a circular reference detection by having the loader throw the expected exception.
        URI uri = URI.create("classpath:/schemas/circular-ref-schema.json");
        RuntimeException circular = new RuntimeException("Circular reference detected");
        factory = factoryWithLoaderThrowing(uri, circular);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> factory.getSchema(uri), "Circular reference should result in a RuntimeException");
        assertTrue(thrown.getMessage().contains("Circular reference"), "Exception message should indicate circular reference");
    }
}
