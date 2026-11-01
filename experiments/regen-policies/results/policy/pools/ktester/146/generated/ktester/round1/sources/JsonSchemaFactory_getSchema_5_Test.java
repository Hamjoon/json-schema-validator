package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /**
     * Shared factory used by all test methods.
     */
    private static JsonSchemaFactory factory;

    /**
     * Called once before any test runs.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema for the supplied version flag.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory should be created successfully");
    }

    /**
     * Called once after all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release; the static factory will be garbage‑collected.
        factory = null;
    }

    /**
     * Supplies a stream of valid schema strings.
     */
    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"string\" }", "simple string type"), Arguments.of("{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }", "object with integer property"), Arguments.of("{ }", "empty object – valid according to Draft‑07"), Arguments.of("   { \"type\": \"object\" }   ", "whitespace trimmed correctly"), Arguments.of("\ufeff{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }", "UTF‑8 BOM is ignored"), Arguments.of("{ \"$ref\": \"#/definitions/foo\", \"definitions\": { \"foo\": { \"type\": \"string\" } } }", "internal $ref resolves correctly"), // a schema that explicitly declares a different meta‑schema via $schema
        Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }", "explicit Draft‑04 meta‑schema"));
    }

    /**
     * Supplies a stream of invalid schema strings together with the expected exception type.
     */
    private static Stream<Arguments> invalidSchemaProvider() {
        return Stream.of(// null – the implementation checks for null and throws NPE
        Arguments.of(null, NullPointerException.class, "null argument"), // empty string
        Arguments.of("", IllegalArgumentException.class, "empty string"), // whitespace only
        Arguments.of("   ", IllegalArgumentException.class, "whitespace‑only string"), // malformed JSON (missing closing brace)
        Arguments.of("{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" }", JsonProcessingException.class, "malformed JSON"), // YAML content – default format is JSON, so parsing fails
        Arguments.of("---\ntype: object\nproperties:\n  name:\n    type: string\n", JsonProcessingException.class, "YAML supplied to JSON parser"), // unknown type – meta‑schema validation should reject it
        Arguments.of("{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"unknown\" } } }", JsonSchemaException.class, "unknown type in schema"), // root node is an array
        Arguments.of("[{\"type\":\"object\"}]", JsonSchemaException.class, "array as root node"), // malformed URI (single slash after scheme)
        Arguments.of("http:/example.com/malformed-uri", IllegalArgumentException.class, "malformed URI"), // unsupported scheme (ftp)
        Arguments.of("ftp://example.com/schema.json", IllegalArgumentException.class, "unsupported URI scheme"), // circular $ref (mutually recursive definitions)
        Arguments.of("{ \"$ref\": \"#/definitions/a\", \"definitions\": { \"a\": { \"$ref\": \"#/definitions/b\" }, \"b\": { \"$ref\": \"#/definitions/a\" } } }", JsonSchemaException.class, "circular $ref detection"));
    }

    /**
     * Verify that a well‑formed JSON schema string results in a non‑null {@link JsonSchema}
     * instance.
     */
    @Test
    void getSchema_withValidJsonString_returnsSchema() {
        // GIVEN a simple JSON schema that defines a string type
        String schema = "{ \"type\": \"string\" }";
        // WHEN the factory parses the schema string
        JsonSchema jsonSchema = factory.getSchema(schema);
        // THEN a JsonSchema object is returned and it contains the expected node
        assertNotNull(jsonSchema, "JsonSchema should not be null for a valid schema string");
        // The underlying JsonNode can be inspected via the public getSchemaNode() method
        // (available on JsonSchema implementations).  If the method is not present in a
        // particular version, the test still validates the non‑null contract.
        JsonNode schemaNode = jsonSchema.getSchemaNode();
        assertNotNull(schemaNode, "Underlying schema node must be present");
        assertEquals("string", schemaNode.get("type").asText(), "The schema should declare type 'string'");
    }

    /**
     * Verify that an empty string is treated as an invalid schema and results in an
     * {@link IllegalArgumentException}.  The exact exception type is based on the
     * current implementation of {@link JsonSchemaFactory#getSchema(String)}.
     */
    @Test
    void getSchema_withEmptyString_throwsException() {
        // GIVEN an empty schema string
        String emptySchema = "";
        // WHEN/THEN an exception is thrown
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(emptySchema), "Empty schema string should cause IllegalArgumentException");
    }

    /**
     * Verify that a {@code null} argument results in a {@link NullPointerException}.
     */
    @Test
    void getSchema_withNull_throwsException() {
        // GIVEN a null reference
        String nullSchema = null;
        // WHEN/THEN a NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullSchema), "Null schema argument should cause NullPointerException");
    }

    /**
     * Parameterised test that verifies a variety of *valid* schema strings.
     *
     * <p>Each argument consists of the raw schema string and a short description
     * used only for test readability.</p>
     */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("validSchemaProvider")
    void getSchema_validInputs_returnsNonNullSchema(String schema, String description) {
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schema);
        // THEN
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema");
        // The concrete implementation always exposes the parsed JsonNode.
        JsonNode node = jsonSchema.getSchemaNode();
        assertNotNull(node, "Underlying schema node must be present");
        // Basic sanity – the node must be an OBJECT (schemas are JSON objects)
        assertTrue(node.isObject(), "Root node of a schema must be a JSON object");
    }

    /**
     * Parameterised test for inputs that must cause an exception.
     *
     * <p>The expected exception class is provided together with a short
     * description.  The test asserts that the concrete exception is either the
     * expected type or a subclass of it (to stay robust against implementation
     * changes).</p>
     */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidSchemaProvider")
    void getSchema_invalidInputs_throwsExpectedException(String schema, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        Throwable thrown = assertThrows(expectedException, () -> factory.getSchema(schema), "Expected " + expectedException.getSimpleName() + " for: " + description);
        // optionally log the message for debugging
        assertNotNull(thrown.getMessage());
    }

    /**
     * Verify that a schema containing only a BOM character is parsed successfully.
     */
    @Test
    void getSchema_schemaWithBom_isParsed() {
        String schema = "\ufeff{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        JsonNode node = jsonSchema.getSchemaNode();
        assertEquals("object", node.get("type").asText());
    }

    /**
     * Verify that a schema declaring Draft‑04 via $schema is still accepted.
     */
    @Test
    void getSchema_schemaWithExplicitDraft04_isAccepted() {
        String schema = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }";
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        // the meta‑schema used can be inspected via JsonSchema.getMetaSchema()
        JsonMetaSchema meta = jsonSchema.getMetaSchema();
        assertEquals(SpecVersion.VersionFlag.V4, meta.getSpecVersion().getVersionFlag(), "Meta‑schema should be Draft‑04 as declared by $schema");
    }

    /**
     * Verify that a schema with an internal $ref resolves correctly.
     */
    @Test
    void getSchema_schemaWithInternalRef_isResolved() {
        String schema = "{ \"$ref\": \"#/definitions/foo\", \"definitions\": { \"foo\": { \"type\": \"string\" } } }";
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        // Resolve a sample instance against the schema to ensure the $ref works.
        JsonNode instance = factory.getJsonMapper().readTree("{\"value\":\"test\"}");
        // The schema expects a string directly (no property wrapper), so validation
        // should succeed for a raw string node.
        assertDoesNotThrow(() -> jsonSchema.validate(factory.getJsonMapper().readTree("\"hello\"")));
    }
}
