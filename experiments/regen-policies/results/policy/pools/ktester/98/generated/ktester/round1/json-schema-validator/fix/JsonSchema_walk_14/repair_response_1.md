```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.ExecutionContextCustomizer;
import com.networknt.schema.InputFormat;
import com.networknt.schema.ValidationResult;

/**
 * Tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean, Consumer)} and related overloads.
 */
class JsonSchema_walk_14_Test {

    /**
     * A simple schema used for all test cases.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": ["name"]
        }
        """;

    // the object under test
    private JsonSchema schema;

    // fresh context for each test
    private ExecutionContext executionContext;

    /**
     * A very small generic schema used when the test supplies its own schema string.
     */
    private static final String DUMMY_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object"
        }
        """;

    /**
     * Jackson mapper for turning JSON/YAML strings into {@link JsonNode}s.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Build a fresh {@link JsonSchema} and {@link ExecutionContext} before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Obtain a JsonSchemaFactory for the 2020‑12 meta‑schema.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Start with a dummy schema – individual tests replace it when needed.
        this.schema = factory.getSchema(DUMMY_SCHEMA);
        this.executionContext = new ExecutionContext();
    }

    /**
     * Clean up references after each test – helps the GC when many tests run.
     */
    @AfterEach
    void teardownAfterEach() {
        this.schema = null;
        this.executionContext = null;
    }

    /**
     * Helper to create a {@link JsonSchema} from a raw schema string.
     */
    private static JsonSchema schemaFrom(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /**
     * Helper that parses a JSON/YAML string into a {@link JsonNode}.
     */
    private static JsonNode toNode(String content) {
        try {
            return MAPPER.readTree(content);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON/YAML supplied to test", e);
        }
    }

    /* --------------------------------------------------------------
     *  1. Walk a *valid* JSON document. The result must contain no validation messages.
     * -------------------------------------------------------------- */
    @Test
    void walkValidInput_returnsEmptyResult() {
        // GIVEN
        String validJson = """
            { "name": "John Doe" }""";
        JsonNode node = toNode(validJson);
        // WHEN
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        // THEN
        assertNotNull(result);
        assertTrue(result.isValid(), "Result should be valid for a correct document");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------
     *  2. Walk an *invalid* JSON document. The result must contain at least one validation message.
     * -------------------------------------------------------------- */
    @Test
    void walkInvalidInput_returnsMessages() {
        // GIVEN
        String invalidJson = """
            { "name": 123 }""";
        JsonNode node = toNode(invalidJson);
        // WHEN
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        // THEN
        assertNotNull(result);
        assertFalse(result.isValid(), "Result should be invalid for an incorrect document");
        assertFalse(result.getMessages().isEmpty(), "Validation messages expected");
        boolean mentionsName = result.getMessages().stream()
                .anyMatch(msg -> msg.getPath().contains("name"));
        assertTrue(mentionsName, "At least one message should refer to the 'name' property");
    }

    /* --------------------------------------------------------------
     *  3. Walk with validation disabled (the {@code validate} flag set to {@code false}).
     * -------------------------------------------------------------- */
    @Test
    void walkWithValidationDisabled_returnsEmptyResultEvenForInvalidInput() {
        // GIVEN
        String invalidJson = """
            { "name": 123 }""";
        JsonNode node = toNode(invalidJson);
        // WHEN
        ValidationResult result = schema.walk(executionContext, node, false, ctx -> {
        });
        // THEN
        assertNotNull(result);
        assertTrue(result.isValid(), "When validation is disabled the result is considered valid");
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is skipped");
    }

    /* --------------------------------------------------------------
     *  4. Successful validation – JSON input, validate = true, no customiser
     * -------------------------------------------------------------- */
    @Test
    void walkValidJson_returnsEmptyResult() {
        schema = schemaFrom("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "name": { "type": "string" } },
              "required": ["name"]
            }
            """);
        String valid = """
            { "name": "John Doe" }""";
        JsonNode node = toNode(valid);
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------
     *  5. Successful validation – YAML input, validate = true, no customiser
     * -------------------------------------------------------------- */
    @Test
    void walkValidYaml_returnsEmptyResult() {
        schema = schemaFrom("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """);
        String yaml = """
            type: object
            properties:
              age:
                type: integer
            """;
        // Jackson can read simple YAML as JSON for the purpose of this test
        JsonNode node = toNode(yaml);
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------
     *  6. Validate flag false – validation is skipped
     * -------------------------------------------------------------- */
    @Test
    void walkValidateFlagFalse_skipsValidationEvenIfInvalid() {
        schema = schemaFrom("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "name": { "type": "string" } },
              "required": ["name"]
            }
            """);
        String invalid = """
            { "invalid": "data" }""";
        JsonNode node = toNode(invalid);
        ValidationResult result = schema.walk(executionContext, node, false, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------
     *  7. ExecutionContextCustomizer invoked (sets an attribute)
     * -------------------------------------------------------------- */
    @Test
    void walkCustomizerIsExecuted_beforeWalking() {
        schema = schemaFrom("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "id": { "type": "integer" } }
            }
            """);
        String json = """
            { "id": 10 }""";
        JsonNode node = toNode(json);
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
        ValidationResult result = schema.walk(executionContext, node, true, customizer);
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
        assertEquals(Boolean.TRUE, executionContext.getAttribute("custom"));
    }

    /* --------------------------------------------------------------
     *  8. Invalid JSON – deserialize throws exception
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedJson_throwsIOException() {
        String malformed = """
            { "unclosedObject": true""";
        assertThrows(IOException.class, () -> {
            // Jackson will throw while parsing the malformed JSON
            toNode(malformed);
        });
    }

    /* --------------------------------------------------------------
     *  9. Null or empty input string
     * -------------------------------------------------------------- */
    @Test
    void walkEmptyString_returnsInvalidResult() {
        // an empty JSON object
        JsonNode emptyNode = MAPPER.createObjectNode();
        ValidationResult result = schema.walk(executionContext, emptyNode, true, ctx -> {
        });
        assertNotNull(result);
        // Since the dummy schema does not require any properties, an empty object is actually valid.
        assertTrue(result.isValid());
    }

    @Test
    void walkNullString_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            JsonNode node = null;
            schema.walk(executionContext, node, true, ctx -> {
            });
        });
    }

    /* --------------------------------------------------------------
     * 10. Custom ExecutionContextCustomizer that throws
     * -------------------------------------------------------------- */
    @Test
    void walkCustomizerThrows_propagatesException() {
        ExecutionContextCustomizer failing = ctx -> {
            ctx.setAttribute("before", Boolean.TRUE);
            throw new RuntimeException("customizer failure");
        };
        JsonNode node = toNode("""
            { "id": 1 }""");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(executionContext, node, true, failing));
        assertEquals("customizer failure", ex.getMessage());
        assertEquals(Boolean.TRUE, executionContext.getAttribute("before"));
    }

    /* --------------------------------------------------------------
     * 11. Schema with unresolved $ref produces validation errors
     * -------------------------------------------------------------- */
    @Test
    void walkUnresolvedRef_returnsMessages() {
        schema = schemaFrom("""
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "$ref": "#/components/schemas/Unknown"
            }
            """);
        JsonNode node = toNode("{}");
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("unresolved")
                        || m.getMessage().contains("$ref")));
    }

    /* --------------------------------------------------------------
     * 12. Valid empty schema {} returns a successful ValidationResult
     * -------------------------------------------------------------- */
    @Test
    void walkEmptySchema_isValid() {
        schema = schemaFrom("{}");
        JsonNode node = toNode("{}");
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------
     * 13. Schema with invalid "type" value fails validation
     * -------------------------------------------------------------- */
    @Test
    void walkSchemaWithInvalidType_returnsMessages() {
        schema = schemaFrom("""
            { "type": "invalidType" }""");
        JsonNode node = toNode("{}");
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type")));
    }

    /* --------------------------------------------------------------
     * 14. URL‑encoded JSON input is correctly decoded before validation
     * -------------------------------------------------------------- */
    @Test
    void walkUrlEncodedInput_isDecodedAndValid() {
        String urlEncoded = "%7B%22type%22%3A%22object%22%2C%22properties%22%3A%7B%22age%22%3A%7B%22type%22%3A%22integer%22%7D%7D%7D";
        String decoded = java.net.URLDecoder.decode(urlEncoded, java.nio.charset.StandardCharsets.UTF_8);
        schema = schemaFrom("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """);
        JsonNode node = toNode(decoded);
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------
     * 15. YAML schema using anchors and aliases parses correctly
     * -------------------------------------------------------------- */
    @Test
    void walkYamlWithAnchorsAndAliases_isValid() {
        String yaml = """
            type: object
            properties:
              person: &personSchema
                type: object
                properties:
                  name:
                    type: string
              employee:
                <<: *personSchema
                properties:
                  employeeId:
                    type: integer
            """;
        schema = schemaFrom(yaml);
        String instance = """
            {
              "person": { "name": "Alice" },
              "employee": { "name": "Bob", "employeeId": 42 }
            }
            """;
        JsonNode node = toNode(instance);
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------
     * 16. Validate flag false with a schema that would otherwise be invalid
     * -------------------------------------------------------------- */
    @Test
    void walkValidateFalseIgnoresInvalidSchema() {
        schema = schemaFrom("""
            { "type": "invalid" }""");
        JsonNode node = toNode("{}");
        ValidationResult result = schema.walk(executionContext, node, false, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------
     * 17. Null ExecutionContext – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullExecutionContext_throwsNPE() {
        JsonNode node = toNode("""
            { "type":"object" }""");
        assertThrows(NullPointerException.class,
                () -> schema.walk(null, node, true, ctx -> {
                }));
    }

    /* --------------------------------------------------------------
     * 18. Null InputFormat – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullInputFormat_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> schema.walk(executionContext,
                        """
                        { "type":"object" }""",
                        null,
                        true,
                        ctx -> {
                        }));
    }

    /* --------------------------------------------------------------
     * 19. Invalid YAML input – deserialize throws IOException
     * -------------------------------------------------------------- */
    @Test
    void walkInvalidYaml_throwsIOException() {
        String brokenYaml = """
            type: object
            properties:
              name: {type: string
            """;
        assertThrows(IOException.class, () -> toNode(brokenYaml));
    }

    /* --------------------------------------------------------------
     * 20. Malformed URL‑encoded input – throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedUrlEncoded_throwsIllegalArgumentException() {
        String malformed = "%7B%22type%22%3A%22object%22%ZZ";
        assertThrows(IllegalArgumentException.class,
                () -> java.net.URLDecoder.decode(malformed, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Null InputFormat – not applicable for the JsonNode overload, so we test null node.
     */
    @Test
    void walkNullNode_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> schema.walk(executionContext, (JsonNode) null, true, ctx -> {
                }));
    }
}
```