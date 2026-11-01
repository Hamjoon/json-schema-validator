package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_14_Test {

    /**
     * Factory used for all tests in this class.
     */
    private static JsonSchemaFactory factory;

    /**
     * Simple JSON mapper used to create {@link JsonNode} instances.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* --------------------------------------------------------------
     * Simple holder class for parameterised arguments
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final SchemaLocation location;

        final JsonNode jsonNode;

        final SchemaValidatorsConfig config;

        final Class<? extends Throwable> expectedException;

        final String description;

        ArgumentsHolder(SchemaLocation location, JsonNode jsonNode, SchemaValidatorsConfig config, Class<? extends Throwable> expectedException, String description) {
            this.location = location;
            this.jsonNode = jsonNode;
            this.config = config;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema for the 2020‑12 draft.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    /**
     * Creates a temporary file containing the supplied JSON text and returns its {@link URI}.
     */
    private static URI writeTempSchemaFile(String json) throws IOException {
        File temp = Files.createTempFile("schema", ".json").toFile();
        temp.deleteOnExit();
        try (Writer w = new FileWriter(temp)) {
            w.write(json);
        }
        return temp.toURI();
    }

    private static Stream<ArgumentsHolder> edgeCaseProvider() throws IOException {
        // valid schema content for tests that need a real file
        String validSchema = "{\"type\":\"object\"}";
        URI validUri = writeTempSchemaFile(validSchema);
        URI nonExistentUri = URI.create("file:/non-existent-schema.json");
        URI malformedUri = writeTempSchemaFile("{invalid json");
        return Stream.of(// null schemaUri
        new ArgumentsHolder(null, MAPPER.readTree("{\"type\":\"object\"}"), SchemaValidatorsConfig.builder().build(), NullPointerException.class, "null schemaUri should raise NPE"), // both jsonNode and config null – default config must be used
        new ArgumentsHolder(new SchemaLocation(validUri), null, null, null, "both jsonNode and config null should still produce a schema"), // non‑existent resource
        new ArgumentsHolder(new SchemaLocation(nonExistentUri), null, SchemaValidatorsConfig.builder().build(), IllegalArgumentException.class, "non‑existent classpath resource must raise an exception"), // malformed URI scheme
        new ArgumentsHolder(new SchemaLocation(URI.create("ht!tp://bad-schema-uri")), MAPPER.readTree("{\"type\":\"object\"}"), SchemaValidatorsConfig.builder().build(), IllegalArgumentException.class, "bad URI syntax must raise an exception"), // custom config with failFast=true (behaviour is verified by non‑null result)
        new ArgumentsHolder(new SchemaLocation(validUri), MAPPER.readTree("{}"), new SchemaValidatorsConfig().setFailFast(true), null, "custom config should be accepted"), // unsupported scheme (ftp)
        new ArgumentsHolder(new SchemaLocation(URI.create("ftp://example.com/schema.json")), MAPPER.readTree("{\"type\":\"object\"}"), SchemaValidatorsConfig.builder().build(), IllegalArgumentException.class, "unsupported scheme should raise an exception"));
    }

    /**
     * Verifies that {@code getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)}
     * returns a non‑null {@link JsonSchema} when supplied with a valid location,
     * a parsed schema node and a default configuration.
     */
    @Test
    void getSchema_withValidArguments_returnsJsonSchema() throws Exception {
        // ---- given -----------------------------------------------------------
        String schemaContent = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonNode schemaNode = MAPPER.readTree(schemaContent);
        // AbsoluteIri and JsonNodePath are part of the public API; they have a
        // public constructor that accepts a string and a static constant for the
        // root JSON‑pointer respectively.
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/test-schema.json");
        // fragment defaults to JSON_POINTER
        SchemaLocation location = new SchemaLocation(absoluteIri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ---- when ------------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(location, schemaNode, config);
        // ---- then ------------------------------------------------------------
        assertNotNull(jsonSchema, "The factory should produce a non‑null JsonSchema instance");
        // The created JsonSchema should expose the original schema node (the public
        // API provides getSchemaNode() for this purpose).
        assertEquals(schemaNode, jsonSchema.getSchemaNode(), "The schema node stored inside JsonSchema must match the one supplied to the factory");
    }

    /**
     * Ensures that repeated calls with the same {@link SchemaLocation} and
     * {@link SchemaValidatorsConfig} return the *same* {@link JsonSchema}
     * instance – the factory caches schemas for performance.
     */
    @Test
    void getSchema_calledTwiceWithSameParameters_returnsSameInstance() throws Exception {
        // ---- given -----------------------------------------------------------
        String schemaContent = "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}";
        JsonNode schemaNode = MAPPER.readTree(schemaContent);
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/array-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // ---- when ------------------------------------------------------------
        JsonSchema first = factory.getSchema(location, schemaNode, config);
        JsonSchema second = factory.getSchema(location, schemaNode, config);
        // ---- then ------------------------------------------------------------
        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second, "Repeated calls with identical arguments should return the cached JsonSchema instance");
    }

    /**
     * Checks that providing a {@code null} {@link SchemaValidatorsConfig}
     * results in an {@link IllegalArgumentException}. The factory validates its
     * inputs before delegating to internal helpers.
     */
    @Test
    void getSchema_withNullConfig_throwsException() throws Exception {
        // ---- given -----------------------------------------------------------
        String schemaContent = "{\"type\":\"null\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaContent);
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/null-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);
        // ---- when / then ----------------------------------------------------
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, schemaNode, null), "Passing a null SchemaValidatorsConfig should raise an IllegalArgumentException");
    }

    @Test
    void getSchema_withNullJsonNode_loadsSchemaFromLocation() throws Exception {
        // given
        String schema = "{\"type\":\"object\"}";
        URI uri = writeTempSchemaFile(schema);
        SchemaLocation location = new SchemaLocation(uri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // when
        JsonSchema jsonSchema = factory.getSchema(location, null, config);
        // then
        assertNotNull(jsonSchema, "Factory should load the schema from the location when jsonNode is null");
        assertEquals(MAPPER.readTree(schema), jsonSchema.getSchemaNode(), "The loaded schema node must equal the content of the file");
    }

    @Test
    void getSchema_withNullConfig_usesDefaultConfig() throws Exception {
        // given
        String schema = "{\"type\":\"object\"}";
        JsonNode schemaNode = MAPPER.readTree(schema);
        URI uri = writeTempSchemaFile(schema);
        SchemaLocation location = new SchemaLocation(uri);
        // when
        JsonSchema jsonSchema = factory.getSchema(location, schemaNode, null);
        // then
        assertNotNull(jsonSchema, "Factory should tolerate a null config and create a default one");
        assertEquals(schemaNode, jsonSchema.getSchemaNode(), "The schema node must be the one supplied");
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("edgeCaseProvider")
    void getSchema_edgeCases(ArgumentsHolder args) {
        // when / then
        if (args.expectedException != null) {
            assertThrows(args.expectedException, () -> factory.getSchema(args.location, args.jsonNode, args.config));
        } else {
            JsonSchema schema = factory.getSchema(args.location, args.jsonNode, args.config);
            assertNotNull(schema, "Schema should be created for: " + args.description);
        }
    }

    @Test
    void getSchema_withNullInternalUri_throwsException() {
        // SchemaLocation does not expose a public constructor with a null URI,
        // so we create one via reflection to simulate the situation.
        SchemaLocation location = new SchemaLocation(URI.create("file:/tmp/dummy.json"));
        try {
            java.lang.reflect.Field uriField = SchemaLocation.class.getDeclaredField("uri");
            uriField.setAccessible(true);
            uriField.set(location, null);
        } catch (Exception e) {
            // If reflection fails, the test cannot be performed – fail fast.
            fail("Unable to corrupt SchemaLocation via reflection", e);
        }
        assertThrows(NullPointerException.class, () -> factory.getSchema(location, MAPPER.createObjectNode(), SchemaValidatorsConfig.builder().build()), "Null internal URI should cause NPE");
    }

    @Test
    void getSchema_withJsonNodeNotObject_throwsException() throws Exception {
        // given – a JSON array node
        JsonNode arrayNode = MAPPER.readTree("[1,2,3]");
        URI uri = writeTempSchemaFile("{\"type\":\"array\"}");
        SchemaLocation location = new SchemaLocation(uri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, arrayNode, config), "Root node that is not an object must be rejected");
    }

    @Test
    void getSchema_withMalformedJsonResource_throwsIOException() throws Exception {
        // create a file with malformed JSON
        URI uri = writeTempSchemaFile("{ invalid json }");
        SchemaLocation location = new SchemaLocation(uri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(IOException.class, () -> factory.getSchema(location, null, config), "Malformed JSON content must raise an IOException");
    }

    @Test
    void getSchema_withInvalidFragment_throwsException() throws Exception {
        // schema containing a simple object, no definitions
        String schema = "{\"type\":\"object\"}";
        URI uri = writeTempSchemaFile(schema);
        // fragment points to a non‑existent definition
        SchemaLocation location = new SchemaLocation(URI.create(uri.toString() + "#/nonexistent"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, null, config), "Invalid fragment identifier must cause an exception");
    }
}
