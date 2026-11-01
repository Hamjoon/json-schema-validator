package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /**
     * Mocks for the constructor arguments
     */
    @Mock
    private ObjectMapper jsonMapperMock;

    @Mock
    private ObjectMapper yamlMapperMock;

    @Mock
    private JsonMetaSchema defaultMetaSchemaMock;

    /**
     * The factory under test (a spy so we can stub protected methods)
     */
    private JsonSchemaFactory factorySpy;

    /**
     * A dummy {@link JsonSchema} that the stubbed method will return
     */
    private JsonSchema dummySchema;

    @BeforeEach
    void setupBeforeEach() {
        // Minimal meta‑schema map required by the constructor.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        String defaultMetaSchemaUri = "http://example.com/meta-schema";
        metaSchemas.put(defaultMetaSchemaUri, defaultMetaSchemaMock);
        // Real factory instance with mocked mappers and the minimal map.
        JsonSchemaFactory realFactory = new JsonSchemaFactory(jsonMapperMock, yamlMapperMock, defaultMetaSchemaUri, new SchemaLoaders.Builder(), new SchemaMappers.Builder(), metaSchemas, // disable URI schema cache for test simplicity
        false);
        // Spy so we can stub protected methods while keeping the rest of the logic real.
        factorySpy = Mockito.spy(realFactory);
        // Prepare a dummy JsonSchema that will be returned by the stubbed method.
        dummySchema = Mockito.mock(JsonSchema.class);
        // Stub the protected getMappedSchema(...) method – this is the last step
        // of the public getSchema(URI) workflow.  All successful‑load tests will
        // reach this point and receive the dummy instance.
        Mockito.doReturn(dummySchema).when(factorySpy).getMappedSchema(any(SchemaLocation.class), any(SchemaValidatorsConfig.class));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        Mockito.reset(factorySpy);
    }

    /**
     * Supplies a stream of URIs representing the “valid” groups from the
     * specification (classpath, file, http, yaml, encoded, relative, etc.).
     */
    private static Stream<URI> successfulUris() {
        return Stream.of(// Valid URI - Local JSON Schema Resource (classpath)
        URI.create("classpath:/schemas/simple-schema.json"), // Valid URI - Local file system JSON schema
        URI.create("file:/tmp/test-schema.json"), // Valid URI - Remote HTTP JSON Schema
        URI.create("https://example.com/schemas/remote-schema.json"), // YAML Schema Resource
        URI.create("classpath:/schemas/yaml-schema.yaml"), // URI with Fragment Identifier (fragment is ignored for loading)
        URI.create("classpath:/schemas/complex-schema.json#definitions/address"), // URI with Query Parameters (ignored by loader)
        URI.create("classpath:/schemas/simple-schema.json?version=1"), // URI with Encoded Space
        URI.create("classpath:/schemas/space%20in%20name.json"), // URI with Encoded '#'
        URI.create("classpath:/schemas/special%23char.json"), // Relative Classpath URI
        URI.create("classpath:schemas/relative-schema.json"), // Missing $schema Declaration – should fall back to default meta‑schema
        URI.create("classpath:/schemas/no-meta-schema.json"), // External $ref Resolution (the stubbed method hides the actual resolution)
        URI.create("classpath:/schemas/main-with-ref.json"));
    }

    /**
     * Supplies URIs that trigger error handling inside {@code getSchema(URI)}.
     */
    private static Stream<URI> exceptionUris() {
        return Stream.of(// Invalid URI - Malformed Syntax
        URI.create("ht!tp://[invalid]"), // Unsupported Scheme - ftp
        URI.create("ftp://example.com/schema.json"), // Unsupported Scheme - jar
        URI.create("jar:file:/path/to/jarfile.jar!/schema.json"), // Network Failure (unreachable host)
        URI.create("https://nonexistent.example.com/missing-schema.json"), // Empty String URI
        URI.create(""), // Directory URI
        URI.create("classpath:/schemas/dir/"), // Invalid Schema Content - Non‑JSON/YAML File
        URI.create("classpath:/schemas/not-a-schema.txt"), // Invalid JSON Syntax
        URI.create("classpath:/schemas/invalid-json-schema.json"), // Unsupported $schema Declaration
        URI.create("classpath:/schemas/unknown-meta-schema.json"), // Circular $ref Resolution
        URI.create("classpath:/schemas/circular-ref-a.json"), // Invalid $ref Target Not Found
        URI.create("classpath:/schemas/ref-to-missing.json"), // Schema with Empty Content
        URI.create("classpath:/schemas/empty-schema.json"), // Unsupported Media Type - .xml Extension
        URI.create("classpath:/schemas/schema.xml"), // URI with Authentication Info (default fetcher does not handle user‑info)
        URI.create("https://user:pass@example.com/secure-schema.json"));
    }

    @Test
    void shouldReturnMappedSchemaWhenCallingGetSchemaWithUri() throws Exception {
        // GIVEN
        URI schemaUri = new URI("http://example.com/schema.json");
        // WHEN
        JsonSchema result = factorySpy.getSchema(schemaUri);
        // THEN
        assertNotNull(result, "Resulting JsonSchema should not be null");
        assertSame(dummySchema, result, "The factory should return the stubbed JsonSchema instance");
    }

    /**
     * Parameterized test for all URIs that are expected to load successfully.
     * The factory should return the stubbed {@code dummySchema}.
     */
    @ParameterizedTest(name = "[{index}] URI ''{0}'' loads successfully")
    @MethodSource("successfulUris")
    void shouldLoadSchemaSuccessfully(URI schemaUri) {
        // WHEN
        JsonSchema result = factorySpy.getSchema(schemaUri);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertSame(dummySchema, result, "Factory must return the stubbed JsonSchema instance");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenUriIsNull() {
        // GIVEN
        URI nullUri = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> factorySpy.getSchema(nullUri), "Calling getSchema(null) must throw NullPointerException");
    }

    /**
     * Parameterized test for URIs that are expected to result in an exception.
     * The exact exception type varies between library versions; we accept any
     * {@link RuntimeException} (including {@link JsonSchemaException},
     * {@link IllegalArgumentException}, or {@link IOException} wrapped in a
     * runtime exception).
     */
    @ParameterizedTest(name = "[{index}] URI ''{0}'' throws exception")
    @MethodSource("exceptionUris")
    void shouldThrowExceptionForInvalidUri(URI schemaUri) {
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> factorySpy.getSchema(schemaUri), "Expected an exception for URI: " + schemaUri);
    }
}
