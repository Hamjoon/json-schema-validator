package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URITranslatorException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /**
     * A factory instance that can be reused for all tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * Temporary file that holds a minimal JSON schema used by the tests.
     */
    private static Path tempSchemaFile;

    /**
     * Temporary JSON and YAML files that act as “real” schemas for the file‑ and classpath‑based tests.
     */
    private static Path tempJsonFile;

    private static Path tempYamlFile;

    /**
     * Mocked translator that replaces the default network/classpath loader.
     */
    @Mock
    private URITranslator mockedTranslator;

    /**
     * Spy wrapper around the real factory – needed to replace the internal translator.
     */
    @InjectMocks
    private JsonSchemaFactory factorySpy;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a very small JSON schema: {"type":"object"}
        String schemaContent = "{\"type\":\"object\"}";
        tempSchemaFile = Files.createTempFile("minimal-schema", ".json");
        Files.writeString(tempSchemaFile, schemaContent);
        // Obtain a factory instance using the public static factory method.
        // V4 is the most common version flag; adjust if needed.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotNull(factory, "Factory should have been created");
        // ---------- create a minimal JSON schema file ----------
        String json = "{\"type\":\"object\",\"definitions\":{\"address\":{\"type\":\"object\"}}}";
        tempJsonFile = Files.createTempFile("example-schema", ".json");
        Files.writeString(tempJsonFile, json);
        // ---------- create a minimal YAML schema file ----------
        String yaml = "type: object\nproperties:\n  name:\n    type: string";
        tempYamlFile = Files.createTempFile("example-schema", ".yaml");
        Files.writeString(tempYamlFile, yaml);
        // ---------- obtain a real factory instance ----------
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(factory, "Factory must be created");
    }

    @BeforeEach
    void setUpEach() throws Exception {
        // Create a fresh spy for every test so that state (e.g. cache) does not leak.
        factorySpy = Mockito.spy(factory);
        // Replace the internal URITranslator with our mock.
        // The field name in JsonSchemaFactory is "uriTranslator" (checked against the source).
        Field translatorField = JsonSchemaFactory.class.getDeclaredField("uriTranslator");
        translatorField.setAccessible(true);
        translatorField.set(factorySpy, mockedTranslator);
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Clean up the temporary file.
        if (tempSchemaFile != null) {
            Files.deleteIfExists(tempSchemaFile);
        }
        Files.deleteIfExists(tempJsonFile);
        Files.deleteIfExists(tempYamlFile);
    }

    /**
     * Generates a {@link Stream} of {@link TestCase}s matching the JSON specification.
     *
     * <p>Only the cases that can be exercised without external network access are
     * included; for the remaining cases we rely on the mocked {@link URITranslator}
     * to throw the appropriate exception.</p>
     */
    static Stream<TestCase> testCases() {
        return Stream.of(// ---------- Valid URI – file system ----------
        new TestCase("Valid file URI", toUri(tempJsonFile.toUri()), "Returns a non‑null JsonSchema instance", "File‑system URI that points to a readable JSON schema file."), // ---------- Valid URI – YAML ----------
        new TestCase("YAML schema loading via file", toUri(tempYamlFile.toUri()), "Returns a non‑null JsonSchema instance", "YAML file should be parsed correctly."), // ---------- Null URI ----------
        new TestCase("Null URI", null, "Throws IllegalArgumentException", "The method must guard against a null argument."), // ---------- Relative URI ----------
        new TestCase("Relative URI (no scheme)", URI.create("schema.json"), "Throws IllegalArgumentException", "Relative URIs must be rejected."), // ---------- Empty path with scheme ----------
        new TestCase("Empty path with scheme", URI.create("file:"), "Throws IllegalArgumentException", "URI with scheme but no path is invalid."), // ---------- Unsupported scheme ----------
        new TestCase("Unsupported scheme", URI.create("ftp://example.com/schema.json"), "Throws JsonSchemaException", "FTP scheme is not supported."), // ---------- Upper‑case HTTP scheme ----------
        new TestCase("Upper‑case scheme handling", URI.create("HTTP://example.com/remote-schema.json"), "Returns a non‑null JsonSchema instance", "Scheme handling must be case‑insensitive."), // ---------- Meta‑schema normalization ----------
        new TestCase("Meta‑schema URI normalization", URI.create("http://json-schema.org/draft-04/schema#"), "Returns built‑in Draft‑04 JsonSchema", "Known meta‑schema URI should be cached."), // ---------- Caching behaviour ----------
        new TestCase("Caching – same URI returns same instance", toUri(tempJsonFile.toUri()), "Cache hit – same instance", "Repeated calls for the same URI must return the cached schema."), // ---------- Unsupported URN ----------
        new TestCase("Unsupported URN scheme", URI.create("urn:example:schema"), "Throws JsonSchemaException", "URN scheme is not handled."));
    }

    private static URI toUri(URI uri) {
        // Helper to make test‑data creation more readable.
        return uri;
    }

    private static InputStream simpleJsonInputStream() {
        String json = "{\"type\":\"object\"}";
        return new java.io.ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Verifies that {@code JsonSchemaFactory#getSchema(URI)} correctly loads a
     * schema from a valid {@link URI} and returns a non‑null {@link JsonSchema}
     * whose root node matches the supplied JSON.
     */
    @Test
    void getSchema_withValidUri_returnsJsonSchema() {
        // GIVEN
        URI schemaUri = tempSchemaFile.toUri();
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schemaUri);
        // THEN
        assertNotNull(jsonSchema, "Returned JsonSchema should not be null");
        JsonNode rootNode = jsonSchema.getSchemaNode();
        assertNotNull(rootNode, "Root node of the schema should not be null");
        assertEquals("object", rootNode.get("type").asText(), "The 'type' property of the loaded schema should be 'object'");
    }

    /**
     * Verifies that passing {@code null} as the URI results in an
     * {@link IllegalArgumentException}. The actual implementation of the method
     * performs a null‑check, so the test ensures the contract is honoured.
     */
    @Test
    void getSchema_withNullUri_throwsIllegalArgumentException() {
        // GIVEN
        URI nullUri = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(nullUri), "Calling getSchema(null) should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("testCases")
    void getSchema_variousScenarios(TestCase tc) throws Exception {
        // -----------------------------------------------------------------
        // Arrange – set up mock behaviour according to the expected outcome
        // -----------------------------------------------------------------
        if (tc.uri() != null) {
            String scheme = tc.uri().getScheme().toLowerCase(Locale.ROOT);
            switch(scheme) {
                case "http", "https" ->
                    {
                        // For HTTP(S) we simulate a successful fetch by returning a simple JSON document.
                        when(mockedTranslator.translate(tc.uri())).thenReturn(simpleJsonInputStream());
                    }
                case "ftp", "urn" ->
                    {
                        // Unsupported schemes cause URITranslatorException which is wrapped by JsonSchemaException.
                        when(mockedTranslator.translate(tc.uri())).thenThrow(new URITranslatorException("Scheme not supported"));
                    }
                case "file" ->
                    {
                        // The default implementation for file:// is still used (real file access).
                        // No mocking required – let the factory read the temporary file.
                    }
                default ->
                    {
                        // Any other scheme – behave like unsupported.
                        when(mockedTranslator.translate(tc.uri())).thenThrow(new URITranslatorException("Unsupported scheme"));
                    }
            }
        }
        // -----------------------------------------------------------------
        // Act / Assert – choose verification based on the expected result
        // -----------------------------------------------------------------
        switch(tc.expected()) {
            case "Throws IllegalArgumentException" ->
                {
                    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factorySpy.getSchema(tc.uri()), "Calling getSchema with illegal argument must throw");
                    assertTrue(ex.getMessage().toLowerCase().contains("schemauri"), "Exception message should mention 'schemaUri'");
                }
            case "Throws JsonSchemaException" ->
                {
                    // JsonSchemaException is a runtime exception in the library.
                    assertThrows(JsonSchemaException.class, () -> factorySpy.getSchema(tc.uri()), "Unsupported scheme must result in JsonSchemaException");
                }
            case "Returns a non‑null JsonSchema instance" ->
                {
                    JsonSchema schema = factorySpy.getSchema(tc.uri());
                    assertNotNull(schema, "Returned JsonSchema must not be null");
                    JsonNode root = schema.getSchemaNode();
                    assertNotNull(root, "Root node must be present");
                    // Minimal sanity check – the temporary JSON/YAML files contain a “type” field.
                    if (tc.description().contains("YAML")) {
                        assertEquals("object", root.get("type").asText(), "YAML schema must be parsed correctly");
                    } else {
                        assertTrue(root.has("type"), "JSON schema should contain a 'type' property");
                    }
                }
            case "Returns built‑in Draft‑04 JsonSchema" ->
                {
                    JsonSchema schema = factorySpy.getSchema(tc.uri());
                    assertNotNull(schema, "Built‑in Draft‑04 schema must be returned");
                    // The meta‑schema for Draft‑04 contains the "$schema" keyword with its own URI.
                    assertEquals("http://json-schema.org/draft-04/schema#", schema.getId(), "Meta‑schema ID must match Draft‑04 URI");
                }
            case "Cache hit – same instance" ->
                {
                    // First call – populate the cache.
                    JsonSchema first = factorySpy.getSchema(tc.uri());
                    // Second call – should hit the cache.
                    JsonSchema second = factorySpy.getSchema(tc.uri());
                    assertSame(first, second, "Factory must return the same cached JsonSchema instance");
                }
            default ->
                fail("Test case '" + tc.group() + "' has an unrecognised expected outcome: " + tc.expected());
        }
    }
}
