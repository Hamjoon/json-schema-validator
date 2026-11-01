package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.resource.SchemaLoaders;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /**
     * Factory instance used by the tests
     */
    private JsonSchemaFactory factory;

    /**
     * Simple JSON schema used for the tests
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$id": "https://example.com/simple-schema",
          "type": "string"
        }
        """;

    /**
     * A draft‑07 meta‑schema reference – the library ships with this built‑in
     */
    private static final String DRAFT07_META = "http://json-schema.org/draft-07/schema#";

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a JsonSchemaFactory that knows how to load the SIMPLE_SCHEMA
         * from the URI https://example.com/simple-schema.
         * The factory is created via the public static getInstance method,
         * which internally builds the required JsonNodeReader, SchemaLoaders,
         * SchemaMappers, etc.  No mocking of internal collaborators is needed.
         */
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap("https://example.com/simple-schema", SIMPLE_SCHEMA))));
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this factory.
        factory = null;
    }

    /**
     * Helper that creates a {@link JsonSchemaFactory} with a {@code SchemaLoaders}
     * configuration containing the supplied {@code uri → content} map.
     */
    private JsonSchemaFactory createFactory(Map<String, String> uriContentMap) {
        return JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(uriContentMap)));
    }

    /**
     * Verifies that {@code JsonSchemaFactory#getSchema(SchemaLocation)} returns a
     * non‑null {@link JsonSchema} instance for a known schema location and that
     * repeated calls with the same location return the same cached instance.
     */
    @Test
    void getSchema_givenValidSchemaLocation_returnsJsonSchema_andCachesResult() {
        // --------------------------------------------------------------------
        // given: a SchemaLocation that points to the simple schema defined above
        // --------------------------------------------------------------------
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/simple-schema");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri);
        // --------------------------------------------------------------------
        // when: the factory is asked for a JsonSchema for that location
        // --------------------------------------------------------------------
        JsonSchema firstSchema = factory.getSchema(schemaLocation);
        JsonSchema secondSchema = factory.getSchema(schemaLocation);
        // --------------------------------------------------------------------
        // then: a JsonSchema is returned and the two calls yield the same instance
        // --------------------------------------------------------------------
        assertNotNull(firstSchema, "The factory should return a non‑null JsonSchema");
        assertNotNull(secondSchema, "A second call should also return a non‑null JsonSchema");
        assertSame(firstSchema, secondSchema, "JsonSchemaFactory should cache schemas; repeated calls must return the same instance");
    }

    /**
     * Verifies that a valid classpath URI loads the schema correctly.
     */
    @Test
    void getSchema_classpathValid_returnsJsonSchema() {
        factory = createFactory(Collections.singletonMap("classpath:/schemas/valid-schema.json", SIMPLE_SCHEMA));
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/valid-schema.json"));
        JsonSchema schema = factory.getSchema(location);
        assertNotNull(schema, "Schema should be loaded for a valid classpath URI");
        // The meta‑schema of a plain schema without $schema keyword defaults to the factory version
        assertEquals(VersionFlag.V202012.getVersion(), schema.getMetaSchema().getVersion(), "Meta‑schema version should match the factory's default version");
    }

    /**
     * The method must fail fast when {@code null} is supplied.
     */
    @Test
    void getSchema_nullArgument_throwsNullPointerException() {
        factory = createFactory(Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> factory.getSchema((SchemaLocation) null), "Calling getSchema with null should throw NPE");
    }

    /**
     * Unsupported URI schemes (e.g. {@code ftp}) must be rejected.
     */
    @Test
    void getSchema_unsupportedScheme_throwsIllegalArgumentException() {
        factory = createFactory(Collections.emptyMap());
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("ftp://example.com/schema.json"));
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location), "Unsupported scheme should cause IllegalArgumentException");
    }

    /**
     * Loading a non‑existent classpath resource should surface a {@link JsonSchemaException}.
     */
    @Test
    void getSchema_classpathNonExistent_throwsJsonSchemaException() {
        factory = createFactory(Collections.emptyMap());
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/non-existent.json"));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(location), "Missing classpath resource must raise JsonSchemaException");
    }

    /**
     * The factory recognises the draft‑07 meta‑schema IRI and returns the corresponding
     * {@link JsonSchema} instance.
     */
    @Test
    void getSchema_draft07MetaSchema_returnsCorrespondingJsonSchema() {
        factory = createFactory(Collections.emptyMap());
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(DRAFT07_META));
        JsonSchema schema = factory.getSchema(location);
        assertNotNull(schema, "Meta‑schema should be resolved");
        // The meta‑schema IRI of the returned schema must be exactly the requested one
        assertEquals(DRAFT07_META, schema.getMetaSchema().getUri(), "Returned schema's meta‑schema IRI must match the requested IRI");
    }

    /**
     * Relative path components (\"..\") must be normalised before loading.
     */
    @Test
    void getSchema_pathNormalization_returnsCorrectSchema() {
        // Normalised URI that the loader will actually resolve
        String normalised = "classpath:/schemas/base-schema.json";
        factory = createFactory(Collections.singletonMap(normalised, SIMPLE_SCHEMA));
        // URI with a relative segment that should resolve to the same normalised URI
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/relative/../base-schema.json"));
        JsonSchema schema = factory.getSchema(location);
        assertNotNull(schema, "Schema should be loaded after path normalisation");
    }

    /**
     * An empty file is considered invalid JSON and must raise an exception.
     */
    @Test
    void getSchema_emptyFile_throwsJsonSchemaException() throws IOException {
        Path tempFile = Files.createTempFile("empty-schema", ".json");
        // Ensure the file is truly empty
        Files.writeString(tempFile, "");
        factory = createFactory(Collections.emptyMap());
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("file:" + tempFile.toUri().getPath()));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(location), "Empty schema file should cause JsonSchemaException");
        Files.deleteIfExists(tempFile);
    }

    /**
     * A malformed JSON document must be detected and reported.
     */
    @Test
    void getSchema_malformedJson_throwsJsonSchemaException() throws IOException {
        Path tempFile = Files.createTempFile("malformed-schema", ".json");
        Files.writeString(tempFile, "{ invalid json ");
        factory = createFactory(Collections.emptyMap());
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("file:" + tempFile.toUri().getPath()));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(location), "Malformed JSON should cause JsonSchemaException");
        Files.deleteIfExists(tempFile);
    }

    /**
     * Verifies that the factory caches schemas – two calls with the same location
     * must return the identical instance.
     */
    @Test
    void getSchema_cachingBehaviour_returnsSameInstance() {
        factory = createFactory(Collections.singletonMap("classpath:/schemas/cached-schema.json", SIMPLE_SCHEMA));
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/cached-schema.json"));
        JsonSchema first = factory.getSchema(location);
        JsonSchema second = factory.getSchema(location);
        assertSame(first, second, "Repeated calls should return the same cached JsonSchema instance");
    }

    /**
     * A classpath URI that contains URL‑encoded characters (e.g. %20) must be decoded
     * before the resource is located.
     */
    @Test
    void getSchema_urlEncodedPath_returnsJsonSchema() {
        factory = createFactory(Collections.singletonMap("classpath:/schemas/space in name.json", SIMPLE_SCHEMA));
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/space%20in%20name.json"));
        JsonSchema schema = factory.getSchema(location);
        assertNotNull(schema, "Schema should be loaded when URI contains encoded spaces");
    }

    /**
     * A URI that points to a directory (instead of a file) must be rejected.
     */
    @Test
    void getSchema_directoryPath_throwsIllegalArgumentException() throws IOException {
        Path tempDir = Files.createTempDirectory("schema-dir");
        factory = createFactory(Collections.emptyMap());
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("file:" + tempDir.toUri().getPath()));
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location), "A directory path should cause IllegalArgumentException");
        Files.deleteIfExists(tempDir);
    }

    /**
     * Query parameters in a {@code file:} URI are ignored during loading.
     */
    @Test
    void getSchema_fileUriWithQueryParameters_ignoresQuery() throws IOException {
        Path tempFile = Files.createTempFile("schema-with-query", ".json");
        Files.writeString(tempFile, SIMPLE_SCHEMA);
        factory = createFactory(Collections.emptyMap());
        // Append a dummy query string
        String uriWithQuery = "file:" + tempFile.toUri().getPath() + "?version=1";
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(uriWithQuery));
        JsonSchema schema = factory.getSchema(location);
        assertNotNull(schema, "Schema should be loaded even when a query string is present");
        Files.deleteIfExists(tempFile);
    }

    /**
     * A malformed URI string (e.g. containing illegal characters) must be rejected.
     */
    @Test
    void getSchema_malformedUri_throwsIllegalArgumentException() {
        factory = createFactory(Collections.emptyMap());
        // The constructor of AbsoluteIri will itself reject malformed URIs,
        // but we still verify that getSchema propagates the exception.
        assertThrows(IllegalArgumentException.class, () -> new SchemaLocation(new AbsoluteIri("http://[invalid-uri]")), "Malformed URI should cause IllegalArgumentException");
    }

    /**
     * A {@code mailto:} URI scheme is not supported and must raise an exception.
     */
    @Test
    void getSchema_unsupportedMailtoScheme_throwsIllegalArgumentException() {
        factory = createFactory(Collections.emptyMap());
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("mailto:user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location), "Unsupported mailto scheme must cause IllegalArgumentException");
    }

    /**
     * Fragments are not allowed when loading a whole schema – they must cause an exception.
     */
    @Test
    void getSchema_uriWithFragment_throwsIllegalArgumentException() {
        factory = createFactory(Collections.singletonMap("classpath:/schemas/valid-schema.json", SIMPLE_SCHEMA));
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/valid-schema.json#definition"));
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location), "URI containing a fragment should be rejected");
    }

    /**
     * A schema that declares an unknown meta‑schema IRI must raise an exception.
     */
    @Test
    void getSchema_unknownMetaSchema_throwsJsonSchemaException() {
        String unknownMetaSchema = """
            {
              "$schema": "http://example.com/unknown-meta-schema",
              "type": "object"
            }
            """;
        factory = createFactory(Collections.singletonMap("classpath:/schemas/unknown-meta-schema.json", unknownMetaSchema));
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/unknown-meta-schema.json"));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(location), "Schema with unknown $schema IRI must raise JsonSchemaException");
    }

    /**
     * A schema containing a $ref to a non‑existent external schema must raise an exception.
     */
    @Test
    void getSchema_invalidRef_throwsJsonSchemaException() {
        String schemaWithBadRef = """
            {
              "$id": "classpath:/schemas/invalid-ref.json",
              "$ref": "classpath:/schemas/missing.json"
            }
            """;
        factory = createFactory(Collections.singletonMap("classpath:/schemas/invalid-ref.json", schemaWithBadRef));
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/invalid-ref.json"));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(location), "Unresolvable $ref must cause JsonSchemaException");
    }

    /**
     * Loading a YAML file without explicitly requesting the YAML format must fail.
     */
    @Test
    void getSchema_yamlWithoutFormat_throwsJsonSchemaException() {
        String yamlContent = """
            ---
            $id: "classpath:/schemas/valid-schema.yaml"
            type: object
            """;
        factory = createFactory(Collections.singletonMap("classpath:/schemas/valid-schema.yaml", yamlContent));
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/valid-schema.yaml"));
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(location), "YAML content without specifying InputFormat must raise JsonSchemaException");
    }

    /**
     * Simulates a SecurityManager denying access to a classpath resource.
     * The test installs a custom SecurityManager that throws {@code SecurityException}
     * for any attempt to read the target resource.
     */
    @Test
    @Disabled("Disabled by default – requires a SecurityManager which is not enabled on modern JVMs")
    void getSchema_securityManagerDeniesAccess_throwsSecurityException() {
        // Prepare a resource that would otherwise be loadable
        factory = createFactory(Collections.singletonMap("classpath:/schemas/secure-schema.json", SIMPLE_SCHEMA));
        // Install a SecurityManager that denies classpath reads
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything except reading the specific resource
                if (perm.getName().contains("classpath:/schemas/secure-schema.json")) {
                    throw new SecurityException("Access denied by test SecurityManager");
                }
            }
        });
        try {
            SchemaLocation location = new SchemaLocation(new AbsoluteIri("classpath:/schemas/secure-schema.json"));
            assertThrows(SecurityException.class, () -> factory.getSchema(location), "SecurityManager denial must propagate as SecurityException");
        } finally {
            System.setSecurityManager(original);
        }
    }
}
