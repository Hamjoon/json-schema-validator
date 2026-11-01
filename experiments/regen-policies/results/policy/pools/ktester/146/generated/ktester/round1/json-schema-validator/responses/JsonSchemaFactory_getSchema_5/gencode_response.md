```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String)}.
 *
 * <p>The test suite covers a wide range of input scenarios described in the
 * original JSON test‑case specification.  Only the public API is used – no
 * reflection or private‑member access is required.</p>
 *
 * <p>Cases that would require external resources (HTTP, file system, class‑path)
 * are omitted because they depend on I/O.  Those scenarios are better suited for
 * integration tests.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_5_Test {

    /** Factory shared by all tests – created once for the whole test run. */
    private static JsonSchemaFactory factory;

    /** Initialise the factory before any test executes. */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema for the supplied version flag.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory should be created successfully");
    }

    /** No per‑test state required at the moment. */
    @BeforeEach
    void setupBeforeEach() {
    }

    @AfterEach
    void teardownAfterEach() {
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /* --------------------------------------------------------------------- *
     *  Successful parsing cases – the method must return a non‑null schema *
     * --------------------------------------------------------------------- */

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

    /** Supplies a stream of valid schema strings. */
    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(
                Arguments.of("{ \"type\": \"string\" }", "simple string type"),
                Arguments.of("{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }",
                        "object with integer property"),
                Arguments.of("{ }", "empty object – valid according to Draft‑07"),
                Arguments.of("   { \"type\": \"object\" }   ", "whitespace trimmed correctly"),
                Arguments.of("\ufeff{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }",
                        "UTF‑8 BOM is ignored"),
                Arguments.of("{ \"$ref\": \"#/definitions/foo\", \"definitions\": { \"foo\": { \"type\": \"string\" } } }",
                        "internal $ref resolves correctly"),
                // a schema that explicitly declares a different meta‑schema via $schema
                Arguments.of("{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }",
                        "explicit Draft‑04 meta‑schema")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Failure parsing / validation cases                                   *
     * --------------------------------------------------------------------- */

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
    void getSchema_invalidInputs_throwsExpectedException(String schema,
                                                         Class<? extends Throwable> expectedException,
                                                         String description) {
        // WHEN / THEN
        Throwable thrown = assertThrows(expectedException,
                () -> factory.getSchema(schema),
                "Expected " + expectedException.getSimpleName() + " for: " + description);
        // optionally log the message for debugging
        assertNotNull(thrown.getMessage());
    }

    /** Supplies a stream of invalid schema strings together with the expected exception type. */
    private static Stream<Arguments> invalidSchemaProvider() {
        return Stream.of(
                // null – the implementation checks for null and throws NPE
                Arguments.of(null, NullPointerException.class, "null argument"),
                // empty string
                Arguments.of("", IllegalArgumentException.class, "empty string"),
                // whitespace only
                Arguments.of("   ", IllegalArgumentException.class, "whitespace‑only string"),
                // malformed JSON (missing closing brace)
                Arguments.of("{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" }", 
                        JsonProcessingException.class, "malformed JSON"),
                // YAML content – default format is JSON, so parsing fails
                Arguments.of("---\ntype: object\nproperties:\n  name:\n    type: string\n",
                        JsonProcessingException.class, "YAML supplied to JSON parser"),
                // unknown type – meta‑schema validation should reject it
                Arguments.of("{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"unknown\" } } }",
                        JsonSchemaException.class, "unknown type in schema"),
                // root node is an array
                Arguments.of("[{\"type\":\"object\"}]", JsonSchemaException.class, "array as root node"),
                // malformed URI (single slash after scheme)
                Arguments.of("http:/example.com/malformed-uri",
                        IllegalArgumentException.class, "malformed URI"),
                // unsupported scheme (ftp)
                Arguments.of("ftp://example.com/schema.json",
                        IllegalArgumentException.class, "unsupported URI scheme"),
                // circular $ref (mutually recursive definitions)
                Arguments.of("{ \"$ref\": \"#/definitions/a\", \"definitions\": { \"a\": { \"$ref\": \"#/definitions/b\" }, \"b\": { \"$ref\": \"#/definitions/a\" } } }",
                        JsonSchemaException.class, "circular $ref detection")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Additional focused tests (single‑case)                                 *
     * --------------------------------------------------------------------- */

    /** Verify that a schema containing only a BOM character is parsed successfully. */
    @Test
    void getSchema_schemaWithBom_isParsed() {
        String schema = "\ufeff{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        JsonNode node = jsonSchema.getSchemaNode();
        assertEquals("object", node.get("type").asText());
    }

    /** Verify that a schema declaring Draft‑04 via $schema is still accepted. */
    @Test
    void getSchema_schemaWithExplicitDraft04_isAccepted() {
        String schema = "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }";
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        // the meta‑schema used can be inspected via JsonSchema.getMetaSchema()
        JsonMetaSchema meta = jsonSchema.getMetaSchema();
        assertEquals(SpecVersion.VersionFlag.V4, meta.getSpecVersion().getVersionFlag(),
                "Meta‑schema should be Draft‑04 as declared by $schema");
    }

    /** Verify that a schema with an internal $ref resolves correctly. */
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
```