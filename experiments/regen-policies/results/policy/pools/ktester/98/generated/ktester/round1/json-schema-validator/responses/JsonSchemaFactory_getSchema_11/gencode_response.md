```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#getSchema(String, InputFormat)}.
 *
 * <p>The tests cover the full matrix of scenarios described in the supplied JSON
 * test‑case document, including valid JSON/YAML schemas, various malformed
 * inputs, handling of {@code null} arguments, $ref resolution, meta‑schema
 * fallback, BOM handling and detection of circular references.</p>
 *
 * <p>All tests use the public {@link JsonSchemaFactory#getInstance(VersionFlag)}
 * factory instance.  The test class resides in the same package as the
 * production code, which allows us to call protected methods when we need to
 * stub behaviour (e.g., for the external‑ref failure case).</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_11_Test {

    /** Factory shared by all test methods. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public builder to obtain a factory instance.
        // The default meta‑schema (draft‑2020‑12) is sufficient for these tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /* --------------------------------------------------------------------- */
    /*  Positive test cases                                                  */
    /* --------------------------------------------------------------------- */

    /**
     * Standard case: a well‑formed JSON schema string is supplied together with
     * {@link InputFormat#JSON}. The method must return a usable {@link JsonSchema}.
     */
    @Test
    void getSchema_withJsonString_andJsonFormat_returnsSchema() {
        // GIVEN
        String jsonSchema = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";

        // WHEN
        JsonSchema schema = factory.getSchema(jsonSchema, InputFormat.JSON);

        // THEN
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for a valid JSON schema string");
        assertEquals(JsonNodeType.OBJECT, schema.getSchemaNode().getNodeType(),
                "Root node of the parsed schema must be an OBJECT");
    }

    /**
     * When {@code inputFormat} is {@code null} the implementation falls back to the
     * default JSON format. The same well‑formed JSON schema must still be parsed
     * successfully.
     */
    @Test
    void getSchema_withJsonString_andNullInputFormat_fallsBackToJson() {
        // GIVEN
        String jsonSchema = "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }";

        // WHEN
        JsonSchema schema = factory.getSchema(jsonSchema, null);

        // THEN
        assertNotNull(schema, "Factory should fallback to JSON parsing when InputFormat is null");
        assertEquals(JsonNodeType.OBJECT, schema.getSchemaNode().getNodeType(),
                "Root node must be an OBJECT");
    }

    /**
     * Valid YAML schema parsed with {@link InputFormat#YAML}.
     */
    @Test
    void getSchema_withYamlString_andYamlFormat_returnsSchema() {
        // GIVEN
        String yamlSchema = "type: object\n"
                + "properties:\n"
                + "  name:\n"
                + "    type: string";

        // WHEN
        JsonSchema schema = factory.getSchema(yamlSchema, InputFormat.YAML);

        // THEN
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for a valid YAML schema string");
        assertEquals(JsonNodeType.OBJECT, schema.getSchemaNode().getNodeType(),
                "Root node of the parsed schema must be an OBJECT");
    }

    /**
     * Internal $ref resolution – the reference points to a definition inside the
     * same document. Parsing must succeed and the resulting schema must be non‑null.
     */
    @Test
    void getSchema_withInternalRef_resolvesSuccessfully() {
        // GIVEN
        String schema = "{\n"
                + "  \"$ref\": \"#/definitions/person\",\n"
                + "  \"definitions\": {\n"
                + "    \"person\": {\n"
                + "      \"type\": \"object\",\n"
                + "      \"properties\": { \"name\": { \"type\": \"string\" } }\n"
                + "    }\n"
                + "  }\n"
                + "}";

        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schema, InputFormat.JSON);

        // THEN
        assertNotNull(jsonSchema, "Schema with internal $ref should be parsed without errors");
        // The resolved schema node should be an OBJECT (the definition of 'person')
        assertEquals(JsonNodeType.OBJECT, jsonSchema.getSchemaNode().getNodeType(),
                "Resolved $ref should point to an OBJECT node");
    }

    /**
     * Unknown meta‑schema IRI – the factory should fall back to the default meta‑schema
     * (draft‑2020‑12) and still return a usable {@link JsonSchema}.
     */
    @Test
    void getSchema_withUnknownMetaSchema_fallsBackToDefault() {
        // GIVEN
        String schema = "{ \"$schema\": \"http://example.com/unknown-draft\", \"type\": \"object\" }";

        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schema, InputFormat.JSON);

        // THEN
        assertNotNull(jsonSchema, "Factory should fall back to default meta‑schema when $schema IRI is unknown");
        assertEquals(JsonNodeType.OBJECT, jsonSchema.getSchemaNode().getNodeType(),
                "Root node must be an OBJECT");
    }

    /**
     * Schema string prefixed with a UTF‑8 BOM (U+FEFF). The BOM must be ignored
     * and parsing should succeed.
     */
    @Test
    void getSchema_withLeadingBOM_parsesSuccessfully() {
        // GIVEN
        String schema = "\uFEFF{ \"type\": \"object\", \"properties\": { \"code\": { \"type\": \"string\" } } }";

        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schema, InputFormat.JSON);

        // THEN
        assertNotNull(jsonSchema, "Factory should ignore leading UTF‑8 BOM and parse the schema");
        assertEquals(JsonNodeType.OBJECT, jsonSchema.getSchemaNode().getNodeType(),
                "Root node must be an OBJECT");
    }

    /* --------------------------------------------------------------------- */
    /*  Negative test cases – expect exceptions                               */
    /* --------------------------------------------------------------------- */

    /**
     * Null schema argument – the method must reject the call with an
     * {@link IllegalArgumentException}.
     */
    @Test
    void getSchema_withNullSchema_throwsException() {
        // GIVEN
        String nullSchema = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(nullSchema, InputFormat.JSON),
                "Calling getSchema with a null schema string should throw IllegalArgumentException");
    }

    /**
     * Both arguments {@code null} – the null schema check should fire first.
     */
    @Test
    void getSchema_withBothParametersNull_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(null, null),
                "Calling getSchema with a null schema should throw IllegalArgumentException even if InputFormat is also null");
    }

    /**
     * Empty schema string – parsing an empty JSON document must fail.
     */
    @Test
    void getSchema_withEmptyString_throwsException() {
        // GIVEN
        String empty = "";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(empty, InputFormat.JSON),
                "Empty schema string should result in a parsing exception");
    }

    /**
     * Whitespace‑only schema – should be treated as invalid JSON.
     */
    @Test
    void getSchema_withWhitespaceOnlyString_throwsException() {
        // GIVEN
        String ws = "   \t\n";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(ws, InputFormat.JSON),
                "Whitespace‑only schema should cause a parsing exception");
    }

    /**
     * Malformed JSON content – parsing must fail.
     */
    @Test
    void getSchema_withMalformedJson_throwsException() {
        // GIVEN
        String malformed = "not a json";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(malformed, InputFormat.JSON),
                "Malformed JSON should cause a parsing exception");
    }

    /**
     * JSON string supplied with {@link InputFormat#YAML} – the parser should reject
     * the content because it is not valid YAML.
     */
    @Test
    void getSchema_jsonStringWithYamlFormat_throwsException() {
        // GIVEN
        String json = "{ \"type\": \"object\" }";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(json, InputFormat.YAML),
                "JSON content parsed as YAML should fail");
    }

    /**
     * YAML content with {@code null} {@link InputFormat} – the factory falls back
     * to JSON parsing, which cannot handle YAML syntax, thus an exception is expected.
     */
    @Test
    void getSchema_yamlContentWithNullInputFormat_throwsException() {
        // GIVEN
        String yaml = "type: object\nproperties:\n  age:\n    type: integer";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(yaml, null),
                "YAML content parsed with default JSON format should fail");
    }

    /**
     * Schema whose top‑level node is an array – JSON Schema documents must be objects.
     */
    @Test
    void getSchema_withArrayRoot_throwsException() {
        // GIVEN
        String arrayRoot = "[ { \"type\": \"object\" } ]";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(arrayRoot, InputFormat.JSON),
                "Schema with non‑object root (array) should cause an exception");
    }

    /**
     * $ref points to a missing internal definition – resolution must fail.
     */
    @Test
    void getSchema_withMissingInternalRef_throwsException() {
        // GIVEN
        String schema = "{ \"$ref\": \"#/definitions/missing\" }";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(schema, InputFormat.JSON),
                "Missing internal $ref should cause a resolution exception");
    }

    /**
     * $ref points to an external URI that cannot be fetched – loading must fail.
     *
     * <p>We spy on the factory and force the protected {@code loadSchema} method to
     * throw an {@link IOException} to simulate the unreachable resource without
     * performing a real network call.</p>
     */
    @Test
    void getSchema_withUnreachableExternalRef_throwsException() throws Exception {
        // GIVEN
        JsonSchemaFactory spyFactory = Mockito.spy(factory);
        String schema = "{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }";

        // Stub the protected loadSchema method to throw IOException
        Mockito.doThrow(new IOException("Simulated network failure"))
                .when(spyFactory)
                .loadSchema(Mockito.any(SchemaLocation.class), Mockito.any(SchemaValidatorsConfig.class));

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> spyFactory.getSchema(schema, InputFormat.JSON),
                "Unreachable external $ref should result in a loading exception");
    }

    /**
     * Circular $ref detection – the factory must detect the cycle and abort.
     */
    @Test
    void getSchema_withCircularRef_throwsException() {
        // GIVEN
        String schema = "{\n"
                + "  \"definitions\": {\n"
                + "    \"a\": { \"$ref\": \"#/definitions/b\" },\n"
                + "    \"b\": { \"$ref\": \"#/definitions/a\" }\n"
                + "  },\n"
                + "  \"$ref\": \"#/definitions/a\"\n"
                + "}";

        // WHEN / THEN
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(schema, InputFormat.JSON),
                "Circular $ref should cause an exception");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests for similar failure scenarios                    */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterised test covering a set of malformed inputs that should all
     * result in a parsing exception.
     */
    @ParameterizedTest(name = "{index} => schema=''{0}'' format={1}")
    @MethodSource("invalidSchemaProvider")
    void getSchema_invalidInputs_throwException(String schema, InputFormat format) {
        assertThrows(RuntimeException.class,
                () -> factory.getSchema(schema, format),
                "Invalid schema input should trigger a parsing exception");
    }

    private static Stream<Arguments> invalidSchemaProvider() {
        return Stream.of(
                // empty string
                Arguments.of("", InputFormat.JSON),
                // whitespace only
                Arguments.of("   \t\n", InputFormat.JSON),
                // malformed JSON
                Arguments.of("not a json", InputFormat.JSON),
                // JSON string parsed as YAML
                Arguments.of("{ \"type\": \"object\" }", InputFormat.YAML),
                // YAML content with null format (defaults to JSON)
                Arguments.of("type: object\nproperties:\n  age:\n    type: integer", null)
        );
    }
}
```