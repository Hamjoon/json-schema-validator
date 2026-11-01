
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.networknt.schema.SpecVersion.VersionFlag;

@ExtendWith(MockitoExtension.class)
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
        // The factory creates a thread‑safe JsonSchema instance.
        schema = factory.getSchema(SIMPLE_SCHEMA);
        // A plain ExecutionContext is sufficient for these tests.
        executionContext = new ExecutionContext();
        // Use a simple dummy schema unless a test overrides it.
        schema = schemaFrom(DUMMY_SCHEMA);
        executionContext = new ExecutionContext();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // start with a dummy schema – individual tests replace it when needed
        this.schema = factory.getSchema(DUMMY_SCHEMA);
        this.executionContext = new ExecutionContext();
    }

    /**
     * Clean up references after each test – helps the GC when many tests run.
     */
    @AfterEach
    void teardownAfterEach() {
        schema = null;
        executionContext = null;
        schema = null;
        executionContext = null;
        this.schema = null;
        this.executionContext = null;
    }

    /**
     * Helper to create a {@link JsonSchema} from a raw schema string.
     */
    private static JsonSchema schemaFrom(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
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

    /**
     * Walk a *valid* JSON document. The result must contain no validation messages.
     */
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

    /**
     * Walk an *invalid* JSON document. The result must contain at least one validation message.
     */
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
        boolean mentionsName = result.getMessages().stream().anyMatch(msg -> msg.getPath().contains("name"));
        assertTrue(mentionsName, "At least one message should refer to the 'name' property");
    }

    /**
     * Walk with validation disabled (the {@code validate} flag set to {@code false}).
     * The method must still return a {@link ValidationResult} but it should contain no messages
     * even if the input is invalid, because validation was skipped.
     */
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
     *  1. Successful validation – JSON input, validate = true, no customiser
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
        // Jackson can read simple YAML as JSON for the purpose of this test
        JsonNode node = toNode(yaml);
        ValidationResult result = schema.walk(executionContext, node, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
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
     *  5. Invalid JSON – deserialize throws exception
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
     *  6. Null or empty input string
     * -------------------------------------------------------------- */
    @Test
    void walkEmptyString_returnsInvalidResult() {
        // an empty JSON object
        JsonNode emptyNode = MAPPER.createObjectNode();
        ValidationResult result = schema.walk(executionContext, emptyNode, true, ctx -> {
        });
        assertNotNull(result);
        // Since the dummy schema does not require any properties, an empty object is actually valid.
        // The original test expected invalid – we keep the assertion aligned with the current schema.
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
     *  7. Custom ExecutionContextCustomizer that throws
     * -------------------------------------------------------------- */
    @Test
    void walkCustomizerThrows_propagatesException() {
        ExecutionContextCustomizer failing = ctx -> {
            ctx.setAttribute("before", Boolean.TRUE);
            throw new RuntimeException("customizer failure");
        };
        JsonNode node = toNode("""
            { "id": 1 }""");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(executionContext, node, true, failing));
        assertEquals("customizer failure", ex.getMessage());
        assertEquals(Boolean.TRUE, executionContext.getAttribute("before"));
    }

    /* --------------------------------------------------------------
     *  8. Schema with unresolved $ref produces validation errors
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
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("unresolved") || m.getMessage().contains("$ref")));
    }

    /* --------------------------------------------------------------
     *  9. Valid empty schema {} returns a successful ValidationResult
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
     * 10. Schema with invalid "type" value fails validation
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
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")));
    }

    /* --------------------------------------------------------------
     * 11. URL‑encoded JSON input is correctly decoded before validation
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
     * 13. Validate flag false with a schema that would otherwise be invalid
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
     * 14. Null ExecutionContext – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullExecutionContext_throwsNPE() {
        JsonNode node = toNode("""
            { "type":"object" }""");
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, true, ctx -> {
        }));
    }

    /* --------------------------------------------------------------
     * 15. Null InputFormat – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullInputFormat_throwsNPE() {
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, """
            { "type":"object" }""", null, true, ctx -> {
        }));
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
        assertThrows(IOException.class, () -> toNode(brokenYaml));
    }

    /* --------------------------------------------------------------
     * 17. Malformed URL‑encoded input – throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedUrlEncoded_throwsIllegalArgumentException() {
        String malformed = "%7B%22type%22%3A%22object%22%ZZ";
        assertThrows(IllegalArgumentException.class, () -> java.net.URLDecoder.decode(malformed, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Null InputFormat – not applicable for the JsonNode overload, so we test null node.
     */
    @Test
    void walkNullNode_throwsNPE() {
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, (JsonNode) null, true, ctx -> {
        }));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:65: error: variable factory is already defined in method setupBeforeEach()
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
                          ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:117: error: cannot find symbol
        assertTrue(result.isValid(), "Result should be valid for a correct document");
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:118: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:135: error: cannot find symbol
        assertFalse(result.isValid(), "Result should be invalid for an incorrect document");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:136: error: cannot find symbol
        assertFalse(result.getMessages().isEmpty(), "Validation messages expected");
                          ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:137: error: cannot find symbol
        boolean mentionsName = result.getMessages().stream().anyMatch(msg -> msg.getPath().contains("name"));
                                     ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:157: error: cannot find symbol
        assertTrue(result.isValid(), "When validation is disabled the result is considered valid");
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:158: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is skipped");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:180: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:181: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:207: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:208: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:230: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:231: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:249: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
                                                          ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:252: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:253: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:254: error: cannot find symbol
        assertEquals(Boolean.TRUE, executionContext.getAttribute("custom"));
                                                   ^
  symbol:   method getAttribute(String)
  location: variable executionContext of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:282: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:300: error: cannot find symbol
            ctx.setAttribute("before", Boolean.TRUE);
               ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:299: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer failing = ctx -> {
                                             ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:307: error: cannot find symbol
        assertEquals(Boolean.TRUE, executionContext.getAttribute("before"));
                                                   ^
  symbol:   method getAttribute(String)
  location: variable executionContext of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:325: error: cannot find symbol
        assertFalse(result.isValid());
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:326: error: cannot find symbol
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("unresolved") || m.getMessage().contains("$ref")));
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:339: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:340: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:354: error: cannot find symbol
        assertFalse(result.isValid());
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:355: error: cannot find symbol
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")));
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:376: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:377: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:410: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:411: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:425: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:426: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:445: error: no suitable method found for walk(ExecutionContext,String,<null>,boolean,(ctx)->{ })
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, """
                                                             ^
    method JsonSchema.<T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#1
        (argument mismatch; String cannot be converted to JsonNode))
    method JsonSchema.<T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) is not applicable
      (cannot infer type-variable(s) T#2
        (argument mismatch; String cannot be converted to JsonNode))
    method JsonSchema.<T#3>walk(ExecutionContext,String,InputFormat,OutputFormat<T#3>,boolean) is not applicable
      (cannot infer type-variable(s) T#3
        (argument mismatch; boolean cannot be converted to OutputFormat<T#3>))
    method JsonSchema.walk(ExecutionContext,String,InputFormat,boolean,ExecutionContextCustomizer) is not applicable
      (argument mismatch; incompatible parameter types in lambda expression)
    method JsonSchema.<T#4>walk(ExecutionContext,String,InputFormat,OutputFormat<T#4>,boolean,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#4
        (actual and formal argument lists differ in length))
    method JsonSchema.<T#5>walk(JsonNode,OutputFormat<T#5>,boolean) is not applicable
      (cannot infer type-variable(s) T#5
        (actual and formal argument lists differ in length))
    method JsonSchema.walk(ExecutionContext,JsonNode,JsonNode,JsonNodePath,boolean) is not applicable
      (argument mismatch; String cannot be converted to JsonNode)
  where T#1,T#2,T#3,T#4,T#5 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
    T#3 extends Object declared in method <T#3>walk(ExecutionContext,String,InputFormat,OutputFormat<T#3>,boolean)
    T#4 extends Object declared in method <T#4>walk(ExecutionContext,String,InputFormat,OutputFormat<T#4>,boolean,ExecutionContextCustomizer)
    T#5 extends Object declared in method <T#5>walk(JsonNode,OutputFormat<T#5>,boolean)
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
37 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)': '\n     * Walk the input.\n     * \n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     * @return the validation result\n     ', 'return type': 'com.networknt.schema.ValidationResult', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Result com.networknt.schema.OutputFormat.RESULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public T walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the input.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param node             the input\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


