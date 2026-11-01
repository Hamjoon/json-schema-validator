```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Extended tests for {@link JsonSchemaFactory#getSchema(InputStream)}.
 *
 * <p>These tests cover the scenarios described in the supplied JSON test matrix,
 * ensuring branch and edge‑case coverage.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    /** Shared objects for all tests */
    private static JsonSchemaFactory factory;
    private static ObjectMapper jsonMapper;
    private static ObjectMapper yamlMapper;
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";
    private static java.util.Map<String, JsonMetaSchema> metaSchemaMap;

    @BeforeAll
    static void setupBeforeAll() {
        // Real mappers – they work for the tests that need JSON/YAML parsing
        jsonMapper = new ObjectMapper();
        yamlMapper = new ObjectMapper(); // real YAML mapper is not required for these tests

        // Minimal meta‑schema map required by the constructor
        metaSchemaMap = new java.util.HashMap<>();
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        // The dummy meta‑schema reports its URI via getUri()
        when(dummyMetaSchema.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);

        // Build a real factory instance – the default loaders are sufficient for the tests
        factory = new JsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                metaSchemaMap,
                false);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple tests (kept unchanged)                               */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that a simple JSON schema supplied via an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance.
     */
    @Test
    void testGetSchemaWithValidInputStream_returnsSchema() {
        String schemaJson = "{}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes(StandardCharsets.UTF_8));

        JsonSchema result = factory.getSchema(schemaStream);

        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that {@code getSchema(InputStream)} delegates to the overloaded
     * {@code getSchema(InputStream, SchemaValidatorsConfig)} method using the
     * default configuration.
     */
    @Test
    void testGetSchemaDelegatesToConfigMethod() {
        JsonSchemaFactory spyFactory = Mockito.spy(factory);
        JsonSchema mockSchema = mock(JsonSchema.class);
        doReturn(mockSchema).when(spyFactory).getSchema(any(InputStream.class), any(SchemaValidatorsConfig.class));

        InputStream dummyStream = new ByteArrayInputStream("{ }".getBytes(StandardCharsets.UTF_8));

        JsonSchema result = spyFactory.getSchema(dummyStream);

        verify(spyFactory, times(1)).getSchema(eq(dummyStream), any(SchemaValidatorsConfig.class));
        assertSame(mockSchema, result, "The result should be the mock returned from the delegated method");
    }

    /* --------------------------------------------------------------------- */
    /* New tests derived from the JSON test matrix                           */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that passing {@code null} as the stream fails fast.
     */
    @Test
    void testGetSchema_nullInputStream_throwsException() {
        assertThrows(NullPointerException.class,
                () -> factory.getSchema((InputStream) null),
                "A null InputStream should cause a NullPointerException");
    }

    /**
     * An empty stream cannot be parsed – the factory should propagate a parsing exception.
     */
    @Test
    void testGetSchema_emptyStream_throwsParsingException() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        assertThrows(RuntimeException.class, // JsonSchemaException extends RuntimeException
                () -> factory.getSchema(empty),
                "Parsing an empty stream should result in a JsonSchemaException (or similar)");
    }

    /**
     * Minimal valid JSON schema – meta‑schema id and root node type are verified.
     */
    @Test
    void testGetSchema_validJsonSchema_parsesCorrectly() {
        String json = "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema must not be null");

        // Verify meta‑schema URI
        assertEquals("http://json-schema.org/draft-07/schema#",
                schema.getMetaSchema().getUri(),
                "Meta‑schema URI should match the $schema value");

        // Verify root node type
        JsonNode root = schema.getSchemaNode();
        assertEquals("object", root.path("type").asText(),
                "Root node type should be OBJECT");
    }

    /**
     * YAML input should be accepted and parsed into the same logical schema as JSON.
     */
    @Test
    void testGetSchema_validYamlSchema_parsesCorrectly() {
        String yaml = "type: object\n$schema: http://json-schema.org/draft-07/schema#\n";
        InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema must not be null");
        assertEquals("http://json-schema.org/draft-07/schema#",
                schema.getMetaSchema().getUri(),
                "Meta‑schema URI should be read from YAML");
        assertEquals("object", schema.getSchemaNode().path("type").asText(),
                "Root node type should be OBJECT");
    }

    /**
     * Malformed JSON should cause a parsing exception wrapped in a runtime exception.
     */
    @Test
    void testGetSchema_malformedJson_throwsException() {
        String malformed = "{ invalid json }";
        InputStream stream = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(stream),
                "Malformed JSON should result in a JsonSchemaException (runtime)");
    }

    /**
     * A schema containing a $ref that points to an existing classpath resource
     * should be resolved successfully.
     *
     * <p>For the purpose of the test we place a tiny JSON schema file in the
     * test classpath (src/test/resources/external-schema.json).</p>
     */
    @Test
    void testGetSchema_schemaWithRef_resolvesReference() throws Exception {
        // The external schema is a simple empty object – it lives under src/test/resources
        String withRef = "{\"$ref\":\"classpath:/external-schema.json\"}";
        InputStream stream = new ByteArrayInputStream(withRef.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema, "Schema with $ref should be resolved and not be null");
        // The resolved schema is an empty object, therefore it should not have a "type" property
        assertFalse(schema.getSchemaNode().has("type"),
                "Resolved schema (empty object) should not contain a 'type' property");
    }

    /**
     * UTF‑8 BOM should be ignored and the schema parsed correctly.
     */
    @Test
    void testGetSchema_utf8Bom_isIgnored() {
        String json = "\uFEFF{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
        assertEquals("http://json-schema.org/draft-07/schema#",
                schema.getMetaSchema().getUri(),
                "BOM must not prevent meta‑schema detection");
        assertEquals("object", schema.getSchemaNode().path("type").asText(),
                "Root type must be OBJECT");
    }

    /**
     * Leading whitespace (including line‑breaks and tabs) must be tolerated.
     */
    @Test
    void testGetSchema_leadingWhitespace_isTolerated() {
        String json = "   \n\t{ \"$schema\":\"http://json-schema.org/draft-07/schema#\", \"type\":\"object\" }";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().path("type").asText(),
                "Whitespace before the document must be ignored");
    }

    /**
     * Simulate a failing InputStream that throws an {@link IOException} on read.
     */
    @Test
    void testGetSchema_failingInputStream_throwsWrappedException() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
        };

        assertThrows(RuntimeException.class, // JsonSchemaException wraps the IOException
                () -> factory.getSchema(failing),
                "An IOException from the stream should be wrapped in a JsonSchemaException");
    }

    /**
     * Draft‑04 meta‑schema must be recognised and loaded.
     */
    @Test
    void testGetSchema_draft04_isRecognised() {
        String json = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
        assertEquals("http://json-schema.org/draft-04/schema#",
                schema.getMetaSchema().getUri(),
                "Factory should load the Draft‑04 meta‑schema");
    }

    /**
     * When a schema defines an explicit $id, the resulting {@link JsonSchema}
     * should expose that identifier via its {@link SchemaLocation}.
     */
    @Test
    void testGetSchema_withId_exposesSchemaLocation() {
        String json = "{\"$id\":\"http://example.com/my-schema#\",\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonSchema schema = factory.getSchema(stream);
        assertNotNull(schema);
        assertEquals("http://example.com/my-schema#",
                schema.getSchemaLocation().toString(),
                "SchemaLocation should reflect the $id value");
    }

    /**
     * $ref to a missing classpath resource must result in a {@link JsonSchemaException}.
     */
    @Test
    void testGetSchema_missingRef_throwsException() {
        String json = "{\"$ref\":\"classpath:/missing-schema.json\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(stream),
                "Missing referenced resource should cause a JsonSchemaException");
    }

    /**
     * Unknown $schema URI (draft‑99) must be rejected.
     */
    @Test
    void testGetSchema_unknownMetaSchema_throwsException() {
        String json = "{\"$schema\":\"http://json-schema.org/draft-99/schema#\",\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(stream),
                "Unknown meta‑schema URI should result in an exception");
    }

    /**
     * Invalid $id URI syntax must cause an {@link IllegalArgumentException}
     * (or a JsonSchemaException) when the factory attempts to create a {@link SchemaLocation}.
     */
    @Test
    void testGetSchema_invalidId_throwsException() {
        String json = "{\"$id\":\"ht!tp://[invalid]\",\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(stream),
                "Malformed $id should cause an exception during schema location creation");
    }

    /**
     * A schema that references itself (circular $ref) must be detected and result in an exception.
     */
    @Test
    void testGetSchema_circularRef_throwsException() {
        String json = "{\"$ref\":\"#\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        assertThrows(RuntimeException.class,
                () -> factory.getSchema(stream),
                "Circular $ref should be detected and cause an exception");
    }
}
```