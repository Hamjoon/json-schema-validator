```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI, JsonNode, SchemaValidatorsConfig)}.
 *
 * <p>The test suite follows the groups and cases described in the supplied JSON test
 * description.  Parameterised tests are used where the verification logic is the same
 * (e.g. the method returns a {@link JsonSchema} whose location and node match the inputs).
 * Cases that expect an exception are tested separately.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_16_Test {

    /** Shared factory used for all tests */
    private static JsonSchemaFactory factory;

    /** ObjectMapper to create JsonNode instances from JSON strings */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default factory for Draft‑2020‑12 – it is fully functional for the
        // simple schemas used in the test suite.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test clean‑up required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release.
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods to introspect private fields of JsonSchema / ValidationContext */
    /* --------------------------------------------------------------------- */

    /** Extract the {@code SchemaLocation} string stored inside a {@link JsonSchema}. */
    private static String getSchemaLocationString(JsonSchema schema) throws Exception {
        // JsonSchema has a private field named "schemaLocation" of type SchemaLocation.
        Field f = JsonSchema.class.getDeclaredField("schemaLocation");
        f.setAccessible(true);
        Object loc = f.get(schema);
        return Objects.toString(loc, null); // SchemaLocation overrides toString()
    }

    /** Extract the {@link SchemaValidatorsConfig} used by the {@link JsonSchema}. */
    private static SchemaValidatorsConfig getConfigFromSchema(JsonSchema schema) throws Exception {
        // JsonSchema holds a ValidationContext in a private field "validationContext".
        Field vcField = JsonSchema.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        Object validationContext = vcField.get(schema);

        // ValidationContext has a method getConfig() – use reflection to invoke it.
        return (SchemaValidatorsConfig) validationContext.getClass()
                .getMethod("getConfig")
                .invoke(validationContext);
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised happy‑path tests */
    /* --------------------------------------------------------------------- */

    /**
     * Provides arguments for the happy‑path groups:
     * <ul>
     *   <li>Valid inputs</li>
     *   <li>Edge URI cases</li>
     *   <li>Relative URI handling</li>
     *   <li>Empty URI string</li>
     *   <li>Special characters in URI</li>
     *   <li>Non‑object JsonNode</li>
     *   <li>NullNode JsonNode</li>
     *   <li>URI with user info and port</li>
     *   <li>URI with fragment and query together</li>
     * </ul>
     */
    private static Stream<Arguments> happyPathProvider() {
        return Stream.of(
                // Valid inputs – string schema
                Arguments.of(
                        URI.create("http://example.com/schema.json"),
                        json("{\"type\":\"string\"}"),
                        new SchemaValidatorsConfig(),
                        "http://example.com/schema.json",
                        json("{\"type\":\"string\"}")
                ),
                // Edge URI – fragment
                Arguments.of(
                        URI.create("http://example.com/schema.json#fragment"),
                        json("{\"type\":\"boolean\"}"),
                        new SchemaValidatorsConfig(),
                        "http://example.com/schema.json#fragment",
                        json("{\"type\":\"boolean\"}")
                ),
                // Edge URI – query
                Arguments.of(
                        URI.create("http://example.com/schema.json?version=1"),
                        json("{\"type\":\"null\"}"),
                        new SchemaValidatorsConfig(),
                        "http://example.com/schema.json?version=1",
                        json("{\"type\":\"null\"}")
                ),
                // Relative URI
                Arguments.of(
                        URI.create("/relative/path/schema.json"),
                        json("{\"type\":\"object\"}"),
                        new SchemaValidatorsConfig(),
                        "/relative/path/schema.json",
                        json("{\"type\":\"object\"}")
                ),
                // Empty URI string
                Arguments.of(
                        URI.create(""),
                        json("{\"type\":\"string\"}"),
                        new SchemaValidatorsConfig(),
                        "",
                        json("{\"type\":\"string\"}")
                ),
                // Special characters (encoded space)
                Arguments.of(
                        URI.create("http://example.com/schemas/my%20schema.json"),
                        json("{\"type\":\"number\"}"),
                        new SchemaValidatorsConfig(),
                        "http://example.com/schemas/my%20schema.json",
                        json("{\"type\":\"number\"}")
                ),
                // Non‑object JsonNode (TextNode)
                Arguments.of(
                        URI.create("http://example.com/schema.json"),
                        new TextNode("just a string"),
                        new SchemaValidatorsConfig(),
                        "http://example.com/schema.json",
                        new TextNode("just a string")
                ),
                // NullNode JsonNode
                Arguments.of(
                        URI.create("http://example.com/schema.json"),
                        NullNode.getInstance(),
                        new SchemaValidatorsConfig(),
                        "http://example.com/schema.json",
                        NullNode.getInstance()
                ),
                // URI with user info and port
                Arguments.of(
                        URI.create("http://user:pass@example.com:8080/schema.json"),
                        json("{\"type\":\"array\"}"),
                        new SchemaValidatorsConfig(),
                        "http://user:pass@example.com:8080/schema.json",
                        json("{\"type\":\"array\"}")
                ),
                // URI with fragment and query together
                Arguments.of(
                        URI.create("http://example.com/schema.json?ver=2#section"),
                        json("{\"type\":\"boolean\"}"),
                        new SchemaValidatorsConfig(),
                        "http://example.com/schema.json?ver=2#section",
                        json("{\"type\":\"boolean\"}")
                )
        );
    }

    @ParameterizedTest(name = "[{index}] URI={0} – expect location \"{3}\"")
    @MethodSource("happyPathProvider")
    void getSchema_happyPath_returnsJsonSchema(URI schemaUri,
                                               JsonNode jsonNode,
                                               SchemaValidatorsConfig config,
                                               String expectedLocation,
                                               JsonNode expectedNode) throws Exception {
        // WHEN
        JsonSchema schema = factory.getSchema(schemaUri, jsonNode, config);

        // THEN
        assertNotNull(schema, "Factory must return a non‑null JsonSchema");
        // verify stored node
        assertEquals(expectedNode, schema.getSchemaNode(),
                "JsonSchema should retain the supplied JsonNode");
        // verify location string (via reflection because the method is not public)
        assertEquals(expectedLocation, getSchemaLocationString(schema),
                "SchemaLocation string must match the supplied URI");
    }

    /* --------------------------------------------------------------------- */
    /* Config‑variation tests – we inspect the internal config via reflection */
    /* --------------------------------------------------------------------- */

    private static Stream<Arguments> configVariationProvider() {
        return Stream.of(
                // failFast = true
                Arguments.of(
                        URI.create("http://example.com/schema.json"),
                        json("{\"type\":\"object\",\"properties\":{}}"),
                        configWith(c -> c.setFailFast(true)),
                        true,
                        false,
                        false
                ),
                // typeLoose = true
                Arguments.of(
                        URI.create("http://example.com/schema.json"),
                        json("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"),
                        configWith(c -> c.setTypeLoose(true)),
                        false,
                        true,
                        false
                ),
                // combined flags
                Arguments.of(
                        URI.create("http://example.com/schema.json"),
                        json("{\"type\":\"object\",\"properties\":{}}"),
                        configWith(c -> {
                            c.setFailFast(true);
                            c.setTypeLoose(true);
                            c.setReadOnly(true);
                        }),
                        true,
                        true,
                        true
                )
        );
    }

    @ParameterizedTest(name = "[{index}] flags – failFast={3} typeLoose={4} readOnly={5}")
    @MethodSource("configVariationProvider")
    void getSchema_configFlags_arePropagated(URI schemaUri,
                                            JsonNode jsonNode,
                                            SchemaValidatorsConfig config,
                                            boolean expectFailFast,
                                            boolean expectTypeLoose,
                                            boolean expectReadOnly) throws Exception {
        JsonSchema schema = factory.getSchema(schemaUri, jsonNode, config);
        assertNotNull(schema, "Should obtain a JsonSchema instance");

        SchemaValidatorsConfig actualConfig = getConfigFromSchema(schema);
        assertNotNull(actualConfig, "ValidationContext should contain a config object");

        assertEquals(expectFailFast, actualConfig.isFailFast(),
                "failFast flag must be propagated");
        assertEquals(expectTypeLoose, actualConfig.isTypeLoose(),
                "typeLoose flag must be propagated");
        assertEquals(expectReadOnly, actualConfig.isReadOnly(),
                "readOnly flag must be propagated");
    }

    /* --------------------------------------------------------------------- */
    /* Null handling tests */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchema_nullUri_throwsNPE() {
        JsonNode node = json("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        assertThrows(NullPointerException.class,
                () -> factory.getSchema(null, node, cfg),
                "A null URI should cause NullPointerException");
    }

    @Test
    void getSchema_nullJsonNodeAndNullConfig_returnsSchema() throws Exception {
        URI uri = URI.create("http://example.com/schema.json");
        JsonSchema schema = factory.getSchema(uri, null, null);
        assertNotNull(schema, "Factory must still create a JsonSchema when both node and config are null");
        assertNull(schema.getSchemaNode(), "Schema node should be null as supplied");
        // config should be default – we just verify that we can retrieve a non‑null config
        SchemaValidatorsConfig cfg = getConfigFromSchema(schema);
        assertNotNull(cfg, "A default SchemaValidatorsConfig should be created when null is supplied");
    }

    /* --------------------------------------------------------------------- */
    /* Invalid URI handling tests */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchema_uriWithIllegalChar_throwsIllegalArgumentException() {
        URI badUri = URI.create("http://example.com/invalid|char.json");
        JsonNode node = json("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(badUri, node, cfg),
                "Illegal characters in the URI should cause IllegalArgumentException");
    }

    @Test
    void getSchema_unsupportedScheme_throwsIllegalArgumentException() {
        URI ftpUri = URI.create("ftp://example.com/schema.json");
        JsonNode node = json("{\"type\":\"object\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(ftpUri, node, cfg),
                "Unsupported URI scheme (ftp) should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* Utility methods for building JsonNode and config instances */
    /* --------------------------------------------------------------------- */

    /** Convenience wrapper around ObjectMapper.readTree for inline use. */
    private static JsonNode json(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON used in test data", e);
        }
    }

    /** Helper to create a {@link SchemaValidatorsConfig} and apply customisation. */
    private static SchemaValidatorsConfig configWith(java.util.function.Consumer<SchemaValidatorsConfig> consumer) {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        consumer.accept(cfg);
        return cfg;
    }
}
```