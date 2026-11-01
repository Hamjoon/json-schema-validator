```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(JsonNode, SchemaValidatorsConfig)}.
 *
 * <p>The test class follows the framework that was already provided.  The
 * {@code @BeforeAll}/{@code @AfterAll} lifecycle methods are left untouched.
 * Each test case from the supplied JSON description is implemented as a
 * separate test method (or a parameterised test) to keep the suite readable
 * and maintainable.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    /** Factory used for all tests – created once because it is thread‑safe. */
    private static JsonSchemaFactory factory;

    /** Minimal JSON schema used as input for the focal method. */
    private static JsonNode simpleSchemaNode;

    /** Mocked configuration – the factory does not inspect the mock, it only forwards it. */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Use the public factory method – it builds a fully‑initialised instance.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // A tiny, valid JSON‑Schema document.
        ObjectMapper mapper = new ObjectMapper();
        simpleSchemaNode = mapper.readTree("{\"type\":\"object\"}");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test so that previous interactions do not interfere.
        reset(mockConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing special to dispose of; the factory holds no external resources that need explicit shutdown.
    }

    /* --------------------------------------------------------------
     *  Normal / happy‑path scenarios
     * -------------------------------------------------------------- */

    @Test
    void getSchema_withJsonNodeAndConfig_returnsJsonSchema() {
        // GIVEN – a factory, a simple schema node and a (mocked) configuration
        // WHEN
        JsonSchema result = factory.getSchema(simpleSchemaNode, mockConfig);
        // THEN
        assertNotNull(result, "The factory should return a non‑null JsonSchema instance");
        assertEquals(simpleSchemaNode, result.getSchemaNode(),
                "The JsonSchema should carry the original schema node");
        // The mock must not be touched – the factory only forwards it.
        verifyNoInteractions(mockConfig);
    }

    @Test
    void getSchema_withNullJsonNode_throwsNullPointerException() {
        // GIVEN – a null JsonNode
        JsonNode nullNode = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullNode, mockConfig),
                "Calling getSchema with a null JsonNode should result in NPE");
    }

    @Test
    void getSchema_withNullConfig_usesDefaultConfig() {
        // GIVEN – a valid schema node and a null configuration
        JsonNode node = new ObjectMapper().readTree("{\"type\":\"string\"}");
        // WHEN
        JsonSchema result = factory.getSchema(node, null);
        // THEN
        assertNotNull(result, "Result must not be null even when config is null");
        assertEquals(node, result.getSchemaNode(), "Schema node must be preserved");
        // The factory should have created a non‑null configuration internally.
        assertNotNull(result.getSchemaValidatorsConfig(),
                "A default SchemaValidatorsConfig should be created when null is supplied");
    }

    @Test
    void getSchema_withDraft04MetaSchema_andCustomConfig_respectsBoth() {
        // GIVEN – a schema that declares Draft‑04 and a config with typeLoose = true
        String schemaJson = "{"
                + "\"$schema\":\"http://json-schema.org/draft-04/schema#\","
                + "\"type\":\"object\","
                + "\"properties\":{\"id\":{\"type\":\"integer\"}}"
                + "}";
        JsonNode node = new ObjectMapper().readTree(schemaJson);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setTypeLoose(true);

        // WHEN
        JsonSchema result = factory.getSchema(node, cfg);

        // THEN
        assertNotNull(result, "Result must be non‑null");
        // Meta‑schema should be Draft‑04
        assertEquals("http://json-schema.org/draft-04/schema#",
                result.getMetaSchema().getIri(),
                "Meta‑schema IRI should match the $schema declaration");
        // Configuration flag must be honoured
        assertTrue(result.getSchemaValidatorsConfig().isTypeLoose(),
                "The created JsonSchema must carry the supplied typeLoose flag");
    }

    @Test
    void getSchema_withEmptyObject_returnsJsonSchema() {
        // GIVEN – an empty JSON object as schema
        JsonNode node = new ObjectMapper().readTree("{}");
        // WHEN
        JsonSchema result = factory.getSchema(node, new SchemaValidatorsConfig());
        // THEN
        assertNotNull(result, "Even an empty object should produce a JsonSchema");
        assertEquals(node, result.getSchemaNode(),
                "The empty schema node must be stored unchanged");
    }

    @Test
    void getSchema_withPrimitiveNumericNode_returnsJsonSchema() {
        // GIVEN – a numeric node (not an object)
        JsonNode node = new ObjectMapper().readTree("42");
        // WHEN
        JsonSchema result = factory.getSchema(node, null);
        // THEN
        assertNotNull(result, "Factory must wrap primitive nodes without validation");
        assertEquals(node, result.getSchemaNode(),
                "The primitive node must be stored inside the JsonSchema");
    }

    @Test
    void getSchema_withFailFastConfig_propagatesConfiguration() {
        // GIVEN – a schema node and a config that enables fail‑fast validation
        JsonNode node = new ObjectMapper().readTree("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        // WHEN
        JsonSchema result = factory.getSchema(node, cfg);
        // THEN
        assertNotNull(result, "Result must be non‑null");
        assertTrue(result.getSchemaValidatorsConfig().isFailFast(),
                "The failFast flag from the supplied config must be present in the JsonSchema");
    }

    /* --------------------------------------------------------------
     *  Exception / error scenarios
     * -------------------------------------------------------------- */

    @Test
    void getSchema_withMalformedSchemaUri_throwsException() {
        // GIVEN – a schema containing an invalid $schema URI
        String badSchema = "{"
                + "\"$schema\":\"ht!tp://bad_uri\","
                + "\"type\":\"object\""
                + "}";
        JsonNode node = new ObjectMapper().readTree(badSchema);
        // WHEN / THEN
        assertThrows(RuntimeException.class, // the concrete type may be IllegalArgumentException or JsonSchemaException
                () -> factory.getSchema(node, new SchemaValidatorsConfig()),
                "A malformed $schema URI should cause an exception during schema creation");
    }

    @Test
    void getSchema_withUnsupportedDraft_throwsException() {
        // GIVEN – a $schema value that points to an unknown draft version
        String unknownDraft = "{"
                + "\"$schema\":\"http://json-schema.org/draft-99/schema#\","
                + "\"type\":\"object\""
                + "}";
        JsonNode node = new ObjectMapper().readTree(unknownDraft);
        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(node, null),
                "An unknown $schema IRI must result in an exception");
    }

    @Test
    void getSchema_withNullSchemaFieldValue_throwsException() {
        // GIVEN – $schema field present but its value is null
        String nullSchemaField = "{"
                + "\"$schema\":null,"
                + "\"type\":\"object\""
                + "}";
        JsonNode node = new ObjectMapper().readTree(nullSchemaField);
        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(node, new SchemaValidatorsConfig()),
                "A null $schema value should be rejected");
    }

    @Test
    void getSchema_withBlankSchemaString_throwsException() {
        // GIVEN – $schema contains only whitespace
        String blankSchema = "{"
                + "\"$schema\":\"   \","
                + "\"type\":\"object\""
                + "}";
        JsonNode node = new ObjectMapper().readTree(blankSchema);
        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(node, null),
                "A blank $schema string must cause an exception");
    }

    /* --------------------------------------------------------------
     *  Helper methods (if needed)
     * -------------------------------------------------------------- */

    /**
     * Utility method to create a {@link JsonNode} from a JSON literal.
     *
     * @param json JSON text
     * @return parsed {@link JsonNode}
     */
    private static JsonNode node(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON supplied to test helper", e);
        }
    }
}
```