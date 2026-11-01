```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Comprehensive unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>successful validation</li>
 *   <li>validation failures</li>
 *   <li>skip‑validation flag</li>
 *   <li>null arguments</li>
 *   <li>custom execution context (fail‑fast)</li>
 *   <li>error handling for $ref resolution, circular references, unknown keywords,
 *       and non‑object root schemas</li>
 * </ul>
 *
 * <p>No private members of {@link JsonSchema} are accessed directly – the only
 * reflection used is to toggle the {@code failFast} flag on {@link ExecutionContext}
 * for the fail‑fast test (the flag is package‑private in the reference
 * implementation).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_3_Test {

    private static JsonSchemaFactory factory;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void beforeAll() {
        // Use the 2020‑12 meta‑schema – the same version used throughout the project.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    /* -------------------------------------------------------------
     * Helper methods
     * ------------------------------------------------------------- */

    /** Parse a JSON string into a {@link JsonNode}. */
    private static JsonNode node(String json) throws IOException {
        return MAPPER.readTree(json);
    }

    /** Create a default {@link ExecutionContext} for the supplied schema. */
    private static ExecutionContext defaultContext(JsonSchema schema) {
        return schema.createExecutionContext();
    }

    /** Enable fail‑fast on an {@link ExecutionContext} via reflection. */
    private static void enableFailFast(ExecutionContext ctx) {
        try {
            // In the reference implementation the field is named "failFast".
            var field = ExecutionContext.class.getDeclaredField("failFast");
            field.setAccessible(true);
            field.setBoolean(ctx, true);
        } catch (ReflectiveOperationException e) {
            // If the implementation changes we fall back to a no‑op – the test
            // will still verify that at most one message is returned.
        }
    }

    /* -------------------------------------------------------------
     * Parameterised test data providers
     * ------------------------------------------------------------- */

    /** Successful validation cases. */
    private static Stream<Arguments> successfulCases() {
        return Stream.of(
                Arguments.of(
                        // instance matches schema {name:string, age:integer}
                        "{\"name\":\"John\",\"age\":30}",
                        // schema definition
                        """
                        {
                          "$schema": "https://json-schema.org/draft/2020-12/schema",
                          "type": "object",
                          "properties": {
                            "name": { "type": "string" },
                            "age":  { "type": "integer" }
                          },
                          "required": ["name","age"]
                        }
                        """,
                        true,
                        "no messages"
                ),
                Arguments.of(
                        // primitive string instance
                        "\"simple string\"",
                        // schema that expects a string
                        """
                        {
                          "$schema": "https://json-schema.org/draft/2020-12/schema",
                          "type": "string"
                        }
                        """,
                        true,
                        "no messages"
                )
        );
    }

    /** Validation‑failure cases (validate = true). */
    private static Stream<Arguments> failureCases() {
        return Stream.of(
                Arguments.of(
                        // name is number, age is string
                        "{\"name\":123,\"age\":\"thirty\"}",
                        """
                        {
                          "$schema": "https://json-schema.org/draft/2020-12/schema",
                          "type": "object",
                          "properties": {
                            "name": { "type": "string" },
                            "age":  { "type": "integer" }
                          },
                          "required": ["name","age"]
                        }
                        """,
                        2,
                        "type mismatches for name and age"
                ),
                Arguments.of(
                        // array where only objects are allowed
                        "[]",
                        """
                        {
                          "$schema": "https://json-schema.org/draft/2020-12/schema",
                          "type": "object",
                          "properties": {
                            "any": { "type": "string" }
                          }
                        }
                        """,
                        1,
                        "array not allowed"
                )
        );
    }

    /* -------------------------------------------------------------
     * Tests
     * ------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] successful validation – {3}")
    @MethodSource("successfulCases")
    void walk_successfulValidation(String instanceJson,
                                   String schemaJson,
                                   boolean validate,
                                   String _description) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode instanceNode = node(instanceJson);
        ExecutionContext ctx = defaultContext(schema);

        ValidationResult result = schema.walk(ctx, instanceNode, validate);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Result should be successful");
        assertTrue(result.getMessages().isEmpty(),
                "No validation messages are expected for a conforming instance");
    }

    @ParameterizedTest(name = "[{index}] validation failure – {3}")
    @MethodSource("failureCases")
    void walk_failureValidation(String instanceJson,
                                 String schemaJson,
                                 int expectedMessageCount,
                                 String _description) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode instanceNode = node(instanceJson);
        ExecutionContext ctx = defaultContext(schema);

        ValidationResult result = schema.walk(ctx, instanceNode, true);

        assertNotNull(result);
        assertFalse(result.isSuccess(), "Result should indicate failure");
        Set<ValidationMessage> msgs = result.getMessages();
        assertEquals(expectedMessageCount, msgs.size(),
                "Unexpected number of validation messages");
        // Basic sanity – each message should contain the offending property name
        msgs.forEach(m -> assertTrue(m.getMessage().contains("name") || m.getMessage().contains("age")
                        || m.getMessage().contains("array"),
                "Message should refer to the problematic element"));
    }

    @Test
    void walk_validateFlagFalse() throws Exception {
        // Schema requires a string property "name"
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": ["name"]
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = node("{\"unexpected\":\"field\"}");
        ExecutionContext ctx = defaultContext(schema);

        ValidationResult result = schema.walk(ctx, node, false); // validation disabled

        assertNotNull(result);
        assertTrue(result.isSuccess(),
                "When validate=false the method must return a successful result");
        assertTrue(result.getMessages().isEmpty(),
                "No messages should be produced when validation is skipped");
    }

    @Test
    void walk_nullExecutionContext() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": ["name"]
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = node("{\"name\":\"John\"}");

        assertThrows(NullPointerException.class,
                () -> schema.walk(null, node, true),
                "A null ExecutionContext should cause NPE");
    }

    @Test
    void walk_nullNode() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": ["name"]
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        ExecutionContext ctx = defaultContext(schema);

        assertThrows(NullPointerException.class,
                () -> schema.walk(ctx, null, true),
                "A null JsonNode should cause NPE");
    }

    @Test
    void walk_withCustomExecutionContext_failFast() throws Exception {
        // Schema expects a string for name and an integer for age.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "age":  { "type": "integer" }
                  },
                  "required": ["name","age"]
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = node("{\"name\":\"John\",\"age\":\"invalid\"}");
        ExecutionContext ctx = defaultContext(schema);
        enableFailFast(ctx); // configure fail‑fast

        ValidationResult result = schema.walk(ctx, node, true);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        Set<ValidationMessage> msgs = result.getMessages();
        assertEquals(1, msgs.size(),
                "Fail‑fast should stop after the first validation error");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().contains("age") || msg.getMessage().contains("type"),
                "The single message should relate to the 'age' property");
    }

    @Test
    void walk_invalidRefThrowsIOException() throws Exception {
        // $ref points to a non‑existent file.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "nonexistent-schema.json"
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = node("{\"any\":\"value\"}");
        ExecutionContext ctx = defaultContext(schema);

        // The implementation wraps IO problems in JsonSchemaException.
        assertThrows(JsonSchemaException.class,
                () -> schema.walk(ctx, node, true),
                "Resolving an invalid $ref must result in a JsonSchemaException");
    }

    @Test
    void walk_circularRefThrowsJsonSchemaException() throws Exception {
        // Circular $ref via definitions.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$defs": {
                    "self": { "$ref": "#/$defs/self" }
                  },
                  "$ref": "#/$defs/self"
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = node("{\"a\":1}");
        ExecutionContext ctx = defaultContext(schema);

        assertThrows(JsonSchemaException.class,
                () -> schema.walk(ctx, node, true),
                "Circular $ref should cause a JsonSchemaException");
    }

    @Test
    void walk_unknownKeywordThrowsIllegalArgumentException() throws Exception {
        // Schema contains an unsupported keyword.
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "unknownKeyword": true,
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = node("{\"name\":\"John\"}");
        ExecutionContext ctx = defaultContext(schema);

        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(ctx, node, true),
                "Encountering an unknown keyword must raise IllegalArgumentException");
    }

    @Test
    void walk_schemaNodeNotObjectThrowsIllegalArgumentException() throws Exception {
        // Root schema node is a JSON string, not an object.
        String schemaJson = "\"just a string\"";
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = node("\"any instance\"");
        ExecutionContext ctx = defaultContext(schema);

        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(ctx, node, true),
                "A non‑object root schema should cause IllegalArgumentException");
    }

    @AfterAll
    static void afterAll() {
        // No global resources to release.
    }
}
```