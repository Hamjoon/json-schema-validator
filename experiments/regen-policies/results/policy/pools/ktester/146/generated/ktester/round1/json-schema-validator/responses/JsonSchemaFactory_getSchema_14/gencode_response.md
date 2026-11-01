```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)}.
 *
 * <p>The tests cover normal execution paths as well as a range of edge‑cases
 * described in the JSON test‑case specification.  Where a schema file is
 * required the test creates a temporary file on‑the‑fly so that the suite does
 * not depend on external resources.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_14_Test {

    /** Factory used for all tests in this class. */
    private static JsonSchemaFactory factory;

    /** Simple JSON mapper used to create {@link JsonNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a factory for the 2020‑12 draft – this is the default
        // version used by the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state is required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------
     * Helper methods
     * -------------------------------------------------------------- */

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

    /* --------------------------------------------------------------
     * Normal‑flow tests
     * -------------------------------------------------------------- */

    @Test
    void getSchema_withValidArguments_returnsJsonSchema() throws Exception {
        // given
        String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonNode schemaNode = MAPPER.readTree(schema);
        URI uri = writeTempSchemaFile(schema);
        SchemaLocation location = new SchemaLocation(uri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when
        JsonSchema jsonSchema = factory.getSchema(location, schemaNode, config);

        // then
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema");
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The stored schema node must match the node supplied to the factory");
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
        assertEquals(MAPPER.readTree(schema), jsonSchema.getSchemaNode(),
                "The loaded schema node must equal the content of the file");
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
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The schema node must be the one supplied");
    }

    /* --------------------------------------------------------------
     * Edge‑case tests (grouped with a parameterised test)
     * -------------------------------------------------------------- */

    private static Stream<ArgumentsHolder> edgeCaseProvider() throws IOException {
        // valid schema content for tests that need a real file
        String validSchema = "{\"type\":\"object\"}";
        URI validUri = writeTempSchemaFile(validSchema);
        URI nonExistentUri = URI.create("file:/non-existent-schema.json");
        URI malformedUri = writeTempSchemaFile("{invalid json");

        return Stream.of(
                // null schemaUri
                new ArgumentsHolder(null,
                        MAPPER.readTree("{\"type\":\"object\"}"),
                        SchemaValidatorsConfig.builder().build(),
                        NullPointerException.class,
                        "null schemaUri should raise NPE"),

                // both jsonNode and config null – default config must be used
                new ArgumentsHolder(new SchemaLocation(validUri),
                        null,
                        null,
                        null,
                        "both jsonNode and config null should still produce a schema"),

                // non‑existent resource
                new ArgumentsHolder(new SchemaLocation(nonExistentUri),
                        null,
                        SchemaValidatorsConfig.builder().build(),
                        IllegalArgumentException.class,
                        "non‑existent classpath resource must raise an exception"),

                // malformed URI scheme
                new ArgumentsHolder(new SchemaLocation(URI.create("ht!tp://bad-schema-uri")),
                        MAPPER.readTree("{\"type\":\"object\"}"),
                        SchemaValidatorsConfig.builder().build(),
                        IllegalArgumentException.class,
                        "bad URI syntax must raise an exception"),

                // custom config with failFast=true (behaviour is verified by non‑null result)
                new ArgumentsHolder(new SchemaLocation(validUri),
                        MAPPER.readTree("{}"),
                        new SchemaValidatorsConfig().setFailFast(true),
                        null,
                        "custom config should be accepted"),

                // unsupported scheme (ftp)
                new ArgumentsHolder(new SchemaLocation(URI.create("ftp://example.com/schema.json")),
                        MAPPER.readTree("{\"type\":\"object\"}"),
                        SchemaValidatorsConfig.builder().build(),
                        IllegalArgumentException.class,
                        "unsupported scheme should raise an exception")
        );
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("edgeCaseProvider")
    void getSchema_edgeCases(ArgumentsHolder args) {
        // when / then
        if (args.expectedException != null) {
            assertThrows(args.expectedException, () ->
                    factory.getSchema(args.location, args.jsonNode, args.config));
        } else {
            JsonSchema schema = factory.getSchema(args.location, args.jsonNode, args.config);
            assertNotNull(schema, "Schema should be created for: " + args.description);
        }
    }

    /* --------------------------------------------------------------
     * Additional specific edge cases
     * -------------------------------------------------------------- */

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

        assertThrows(NullPointerException.class,
                () -> factory.getSchema(location, MAPPER.createObjectNode(), SchemaValidatorsConfig.builder().build()),
                "Null internal URI should cause NPE");
    }

    @Test
    void getSchema_withJsonNodeNotObject_throwsException() throws Exception {
        // given – a JSON array node
        JsonNode arrayNode = MAPPER.readTree("[1,2,3]");
        URI uri = writeTempSchemaFile("{\"type\":\"array\"}");
        SchemaLocation location = new SchemaLocation(uri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(location, arrayNode, config),
                "Root node that is not an object must be rejected");
    }

    @Test
    void getSchema_withMalformedJsonResource_throwsIOException() throws Exception {
        // create a file with malformed JSON
        URI uri = writeTempSchemaFile("{ invalid json }");
        SchemaLocation location = new SchemaLocation(uri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        assertThrows(IOException.class,
                () -> factory.getSchema(location, null, config),
                "Malformed JSON content must raise an IOException");
    }

    @Test
    void getSchema_withInvalidFragment_throwsException() throws Exception {
        // schema containing a simple object, no definitions
        String schema = "{\"type\":\"object\"}";
        URI uri = writeTempSchemaFile(schema);
        // fragment points to a non‑existent definition
        SchemaLocation location = new SchemaLocation(URI.create(uri.toString() + "#/nonexistent"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(location, null, config),
                "Invalid fragment identifier must cause an exception");
    }

    /* --------------------------------------------------------------
     * Simple holder class for parameterised arguments
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {
        final SchemaLocation location;
        final JsonNode jsonNode;
        final SchemaValidatorsConfig config;
        final Class<? extends Throwable> expectedException;
        final String description;

        ArgumentsHolder(SchemaLocation location,
                        JsonNode jsonNode,
                        SchemaValidatorsConfig config,
                        Class<? extends Throwable> expectedException,
                        String description) {
            this.location = location;
            this.jsonNode = jsonNode;
            this.config = config;
            this.expectedException = expectedException;
            this.description = description;
        }
    }
}
```