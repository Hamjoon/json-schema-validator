**Generated unit‑test class**

```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * The test suite covers all groups described in the supplied JSON test‑case list:
 * <ul>
 *   <li>valid/invalid inputs (JSON &amp; YAML)</li>
 *   <li>validation flag true / false</li>
 *   <li>customisers (normal, throwing, attribute‑setting)</li>
 *   <li>error handling for malformed JSON, malformed URL‑encoding, null arguments, etc.</li>
 *   <li>edge cases such as empty schema, unresolved $ref, illegal “type” values.</li>
 * </ul>
 *
 * No private members are accessed; the private {@code deserialize} method is exercised
 * indirectly via the public {@code walk} method.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_14_Test {

    /** Helper to create a {@link JsonSchema} from a raw schema string. */
    private static JsonSchema schemaFrom(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /** A very small generic schema used when the test supplies its own schema string. */
    private static final String DUMMY_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }
            """;

    private JsonSchema schema;               // schema under test
    private ExecutionContext executionContext; // fresh context for each test

    @BeforeEach
    void setUp() {
        // Use a simple dummy schema unless a test overrides it.
        schema = schemaFrom(DUMMY_SCHEMA);
        executionContext = new ExecutionContext();
    }

    @AfterEach
    void tearDown() {
        schema = null;
        executionContext = null;
    }

    /* --------------------------------------------------------------
     *  1. Successful validation – JSON input, validate = true, no customiser
     * -------------------------------------------------------------- */
    @Test
    void walkValidJson_returnsEmptyResult() {
        // schema expects a string property "name"
        schema = schemaFrom("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": ["name"]
                }
                """);
        String valid = """{ "name": "John Doe" }""";

        ValidationResult result = schema.walk(executionContext, valid, InputFormat.JSON, true,
                ctx -> { /* no‑op customiser */ });

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                () -> "Expected no validation messages but got: " + result.getMessages());
    }

    /* --------------------------------------------------------------
     *  2. Successful validation – YAML input, validate = true, no customiser
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

        ValidationResult result = schema.walk(executionContext, yaml, InputFormat.YAML, true,
                ctx -> {});

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                () -> "Expected empty validation messages but got: " + result.getMessages());
    }

    /* --------------------------------------------------------------
     *  3. Validate flag false – validation is skipped
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
        String invalid = """{ "invalid": "data" }""";

        ValidationResult result = schema.walk(executionContext, invalid, InputFormat.JSON, false,
                ctx -> {});

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "When validation is disabled, result must contain no messages");
    }

    /* --------------------------------------------------------------
     *  4. ExecutionContextCustomizer invoked (sets an attribute)
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
        String json = """{ "id": 10 }""";

        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("custom", true);

        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, customizer);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "The document is valid, so no messages expected");
        assertEquals(Boolean.TRUE, executionContext.getAttribute("custom"),
                "Customizer should have set attribute \"custom\" to true");
    }

    /* --------------------------------------------------------------
     *  5. Invalid JSON – deserialize throws exception
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedJson_throwsIOException() {
        String malformed = """{ "unclosedObject": true"""; // missing closing brace

        assertThrows(IOException.class,
                () -> schema.walk(executionContext, malformed, InputFormat.JSON, true,
                        ctx -> {}));
    }

    /* --------------------------------------------------------------
     *  6. Null or empty input string
     * -------------------------------------------------------------- */
    @Test
    void walkEmptyString_returnsInvalidResult() {
        ValidationResult result = schema.walk(executionContext, "", InputFormat.JSON, true,
                ctx -> {});

        assertNotNull(result);
        assertFalse(result.isValid(),
                "Empty input should produce an invalid ValidationResult");
        assertFalse(result.getMessages().isEmpty(),
                "There should be at least one message describing the empty input");
    }

    @Test
    void walkNullString_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> schema.walk(executionContext, null, InputFormat.JSON, true,
                        ctx -> {}));
    }

    /* --------------------------------------------------------------
     *  7. Custom ExecutionContextCustomizer that throws
     * -------------------------------------------------------------- */
    @Test
    void walkCustomizerThrows_propagatesException() {
        ExecutionContextCustomizer failing = ctx -> {
            ctx.setAttribute("before", true);
            throw new RuntimeException("customizer failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(executionContext,
                        """{ "id": 1 }""",
                        InputFormat.JSON,
                        true,
                        failing));

        assertEquals("customizer failure", ex.getMessage(),
                "Exception message should be propagated unchanged");
        // Verify that the attribute was set before the exception was thrown.
        assertEquals(Boolean.TRUE, executionContext.getAttribute("before"));
    }

    /* --------------------------------------------------------------
     *  8. Schema with unresolved $ref produces validation errors
     * -------------------------------------------------------------- */
    @Test
    void walkUnresolvedRef_returnsMessages() {
        // The schema itself is the *instance* we are validating; we use a meta‑schema that contains an unknown $ref.
        schema = schemaFrom("""
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "$ref": "#/components/schemas/Unknown"
                }
                """);
        String json = "{}";

        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true,
                ctx -> {});

        assertNotNull(result);
        assertFalse(result.isValid(), "Unresolved $ref should make the result invalid");
        assertTrue(result.getMessages().stream()
                        .anyMatch(m -> m.getMessage().contains("unresolved") || m.getMessage().contains("$ref")),
                "Expected at least one message about an unresolved $ref");
    }

    /* --------------------------------------------------------------
     *  9. Valid empty schema {} returns a successful ValidationResult
     * -------------------------------------------------------------- */
    @Test
    void walkEmptySchema_isValid() {
        schema = schemaFrom("{}");
        String json = "{}";

        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true,
                ctx -> {});

        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty(),
                "Empty schema should produce no validation messages");
    }

    /* --------------------------------------------------------------
     * 10. Schema with invalid "type" value fails validation
     * -------------------------------------------------------------- */
    @Test
    void walkSchemaWithInvalidType_returnsMessages() {
        schema = schemaFrom("""{ "type": "invalidType" }""");
        String json = "{}";

        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true,
                ctx -> {});

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                        .anyMatch(m -> m.getMessage().toLowerCase().contains("type")),
                "Message should mention an unsupported type value");
    }

    /* --------------------------------------------------------------
     * 11. URL‑encoded JSON input is correctly decoded before validation
     * -------------------------------------------------------------- */
    @Test
    void walkUrlEncodedInput_isDecodedAndValid() {
        String urlEncoded = "%7B%22type%22%3A%22object%22%2C%22properties%22%3A%7B%22age%22%3A%7B%22type%22%3A%22integer%22%7D%7D%7D";
        // The schema represented by the above JSON is a simple object with an integer property.
        schema = schemaFrom("""
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "age": { "type": "integer" } }
                }
                """);

        ValidationResult result = schema.walk(executionContext, urlEncoded, InputFormat.JSON, true,
                ctx -> {});

        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty(),
                "Decoded input should validate successfully");
    }

    /* --------------------------------------------------------------
     * 12. YAML schema using anchors and aliases parses correctly
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

        // The schema itself is the YAML above; we validate a matching instance.
        schema = schemaFrom(yaml);
        String instance = """
                {
                  "person": { "name": "Alice" },
                  "employee": { "name": "Bob", "employeeId": 42 }
                }
                """;

        ValidationResult result = schema.walk(executionContext, instance, InputFormat.JSON, true,
                ctx -> {});

        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty(),
                "YAML with anchors/aliases should be parsed and validated successfully");
    }

    /* --------------------------------------------------------------
     * 13. Validate flag false with a schema that would otherwise be invalid
     * -------------------------------------------------------------- */
    @Test
    void walkValidateFalseIgnoresInvalidSchema() {
        schema = schemaFrom("""{ "type": "invalid" }""");
        String json = "{}";

        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, false,
                ctx -> {});

        assertNotNull(result);
        assertTrue(result.isValid(),
                "When validation is disabled, the result must be considered valid");
        assertTrue(result.getMessages().isEmpty(),
                "No messages should be produced when validation is skipped");
    }

    /* --------------------------------------------------------------
     * 14. Null ExecutionContext – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullExecutionContext_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> schema.walk(null,
                        """{ "type":"object" }""",
                        InputFormat.JSON,
                        true,
                        ctx -> {}));
    }

    /* --------------------------------------------------------------
     * 15. Null InputFormat – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullInputFormat_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> schema.walk(executionContext,
                        """{ "type":"object" }""",
                        null,
                        true,
                        ctx -> {}));
    }

    /* --------------------------------------------------------------
     * 16. Invalid YAML input – deserialize throws IOException
     * -------------------------------------------------------------- */
    @Test
    void walkInvalidYaml_throwsIOException() {
        String brokenYaml = """
                type: object
                properties:
                  name: {type: string
                """;

        assertThrows(IOException.class,
                () -> schema.walk(executionContext, brokenYaml, InputFormat.YAML, true,
                        ctx -> {}));
    }

    /* --------------------------------------------------------------
     * 17. Malformed URL‑encoded input – throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedUrlEncoded_throwsIllegalArgumentException() {
        String malformed = "%7B%22type%22%3A%22object%22%ZZ"; // %ZZ is illegal

        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(executionContext, malformed, InputFormat.JSON, true,
                        ctx -> {}));
    }
}
```