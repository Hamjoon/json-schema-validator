
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
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

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
    }

    /**
     * Helper to create a {@link JsonSchema} from a raw schema string.
     */
    private static JsonSchema schemaFrom(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /**
     * Walk a *valid* JSON document. The result must contain no validation messages.
     */
    @Test
    void walkValidInput_returnsEmptyResult() {
        // GIVEN
        String validJson = """
            { "name": "John Doe" }""";
        ExecutionContextCustomizer customizer = ctx -> {
            // No customisation needed for this test – the lambda simply satisfies the API.
        };
        // WHEN
        ValidationResult result = schema.walk(executionContext, validJson, InputFormat.JSON, // ask the walk to perform validation
        true, customizer);
        // THEN
        assertNotNull(result, "walk should never return null");
        // ValidationResult exposes the messages via getMessages(); an empty set means success.
        assertTrue(result.getMessages().isEmpty(), () -> "Expected no validation messages, but got: " + result.getMessages());
    }

    /**
     * Walk an *invalid* JSON document. The result must contain at least one validation message.
     */
    @Test
    void walkInvalidInput_returnsMessages() {
        // GIVEN
        // The schema requires a string for the property "name", but we supply a number.
        String invalidJson = """
            { "name": 123 }""";
        ExecutionContextCustomizer customizer = ctx -> {
            // No customisation needed; the lambda is only required to match the functional interface.
        };
        // WHEN
        ValidationResult result = schema.walk(executionContext, invalidJson, InputFormat.JSON, true, customizer);
        // THEN
        assertNotNull(result, "walk should never return null");
        assertFalse(result.getMessages().isEmpty(), "Expected validation messages for an invalid document");
        // Optionally verify that the message mentions the property "name".
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
        ExecutionContextCustomizer customizer = ctx -> {
        };
        // WHEN
        ValidationResult result = schema.walk(executionContext, invalidJson, InputFormat.JSON, // validation disabled
        false, customizer);
        // THEN
        assertNotNull(result, "walk should never return null");
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled, the result must contain no messages");
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
        String valid = """
            { "name": "John Doe" }""";
        ValidationResult result = schema.walk(executionContext, valid, InputFormat.JSON, true, ctx -> {
            /* no‑op customiser */
        });
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), () -> "Expected no validation messages but got: " + result.getMessages());
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
        ValidationResult result = schema.walk(executionContext, yaml, InputFormat.YAML, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), () -> "Expected empty validation messages but got: " + result.getMessages());
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
        ValidationResult result = schema.walk(executionContext, invalid, InputFormat.JSON, false, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled, result must contain no messages");
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
        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("custom", true);
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, customizer);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "The document is valid, so no messages expected");
        assertEquals(Boolean.TRUE, executionContext.getAttribute("custom"), "Customizer should have set attribute \"custom\" to true");
    }

    /* --------------------------------------------------------------
     *  5. Invalid JSON – deserialize throws exception
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedJson_throwsIOException() {
        // missing closing brace
        String malformed = """
            { "unclosedObject": true""";
        assertThrows(IOException.class, () -> schema.walk(executionContext, malformed, InputFormat.JSON, true, ctx -> {
        }));
    }

    /* --------------------------------------------------------------
     *  6. Null or empty input string
     * -------------------------------------------------------------- */
    @Test
    void walkEmptyString_returnsInvalidResult() {
        ValidationResult result = schema.walk(executionContext, "", InputFormat.JSON, true, ctx -> {
        });
        assertNotNull(result);
        assertFalse(result.isValid(), "Empty input should produce an invalid ValidationResult");
        assertFalse(result.getMessages().isEmpty(), "There should be at least one message describing the empty input");
    }

    @Test
    void walkNullString_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, InputFormat.JSON, true, ctx -> {
        }));
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
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(executionContext, """
            { "id": 1 }""", InputFormat.JSON, true, failing));
        assertEquals("customizer failure", ex.getMessage(), "Exception message should be propagated unchanged");
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
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, ctx -> {
        });
        assertNotNull(result);
        assertFalse(result.isValid(), "Unresolved $ref should make the result invalid");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("unresolved") || m.getMessage().contains("$ref")), "Expected at least one message about an unresolved $ref");
    }

    /* --------------------------------------------------------------
     *  9. Valid empty schema {} returns a successful ValidationResult
     * -------------------------------------------------------------- */
    @Test
    void walkEmptySchema_isValid() {
        schema = schemaFrom("{}");
        String json = "{}";
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty(), "Empty schema should produce no validation messages");
    }

    /* --------------------------------------------------------------
     * 10. Schema with invalid "type" value fails validation
     * -------------------------------------------------------------- */
    @Test
    void walkSchemaWithInvalidType_returnsMessages() {
        schema = schemaFrom("""
            { "type": "invalidType" }""");
        String json = "{}";
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, ctx -> {
        });
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")), "Message should mention an unsupported type value");
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
        ValidationResult result = schema.walk(executionContext, urlEncoded, InputFormat.JSON, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty(), "Decoded input should validate successfully");
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
        ValidationResult result = schema.walk(executionContext, instance, InputFormat.JSON, true, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty(), "YAML with anchors/aliases should be parsed and validated successfully");
    }

    /* --------------------------------------------------------------
     * 13. Validate flag false with a schema that would otherwise be invalid
     * -------------------------------------------------------------- */
    @Test
    void walkValidateFalseIgnoresInvalidSchema() {
        schema = schemaFrom("""
            { "type": "invalid" }""");
        String json = "{}";
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, false, ctx -> {
        });
        assertNotNull(result);
        assertTrue(result.isValid(), "When validation is disabled, the result must be considered valid");
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is skipped");
    }

    /* --------------------------------------------------------------
     * 14. Null ExecutionContext – throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walkNullExecutionContext_throwsNPE() {
        assertThrows(NullPointerException.class, () -> schema.walk(null, """
            { "type":"object" }""", InputFormat.JSON, true, ctx -> {
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
        assertThrows(IOException.class, () -> schema.walk(executionContext, brokenYaml, InputFormat.YAML, true, ctx -> {
        }));
    }

    /* --------------------------------------------------------------
     * 17. Malformed URL‑encoded input – throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void walkMalformedUrlEncoded_throwsIllegalArgumentException() {
        // %ZZ is illegal
        String malformed = "%7B%22type%22%3A%22object%22%ZZ";
        assertThrows(IllegalArgumentException.class, () -> schema.walk(executionContext, malformed, InputFormat.JSON, true, ctx -> {
        }));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:90: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> {
                                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:99: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), () -> "Expected no validation messages, but got: " + result.getMessages());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:99: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), () -> "Expected no validation messages, but got: " + result.getMessages());
                                                                                                              ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:111: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> {
                                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:118: error: cannot find symbol
        assertFalse(result.getMessages().isEmpty(), "Expected validation messages for an invalid document");
                          ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:120: error: cannot find symbol
        boolean mentionsName = result.getMessages().stream().anyMatch(msg -> msg.getPath().contains("name"));
                                     ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:134: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> {
                                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:141: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled, the result must contain no messages");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:160: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, valid, InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:164: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), () -> "Expected no validation messages but got: " + result.getMessages());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:164: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), () -> "Expected no validation messages but got: " + result.getMessages());
                                                                                                             ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:185: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, yaml, InputFormat.YAML, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:188: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), () -> "Expected empty validation messages but got: " + result.getMessages());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:188: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), () -> "Expected empty validation messages but got: " + result.getMessages());
                                                                                                                ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:206: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, invalid, InputFormat.JSON, false, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:209: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled, result must contain no messages");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:226: error: cannot find symbol
        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("custom", true);
                                                          ^
  symbol:   method setAttribute(String,boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:226: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("custom", true);
                                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:229: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "The document is valid, so no messages expected");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:230: error: cannot find symbol
        assertEquals(Boolean.TRUE, executionContext.getAttribute("custom"), "Customizer should have set attribute \"custom\" to true");
                                                   ^
  symbol:   method getAttribute(String)
  location: variable executionContext of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:241: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        assertThrows(IOException.class, () -> schema.walk(executionContext, malformed, InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:250: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, "", InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:253: error: cannot find symbol
        assertFalse(result.isValid(), "Empty input should produce an invalid ValidationResult");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:254: error: cannot find symbol
        assertFalse(result.getMessages().isEmpty(), "There should be at least one message describing the empty input");
                          ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:259: error: no suitable method found for walk(ExecutionContext,<null>,InputFormat,boolean,(ctx)->{ })
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, InputFormat.JSON, true, ctx -> {
                                                             ^
    method JsonSchema.<T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) is not applicable
      (cannot infer type-variable(s) T#1
        (argument mismatch; InputFormat cannot be converted to OutputFormat<T#1>))
    method JsonSchema.<T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) is not applicable
      (cannot infer type-variable(s) T#2
        (argument mismatch; InputFormat cannot be converted to OutputFormat<T#2>))
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
      (argument mismatch; InputFormat cannot be converted to JsonNode)
  where T#1,T#2,T#3,T#4,T#5 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
    T#3 extends Object declared in method <T#3>walk(ExecutionContext,String,InputFormat,OutputFormat<T#3>,boolean)
    T#4 extends Object declared in method <T#4>walk(ExecutionContext,String,InputFormat,OutputFormat<T#4>,boolean,ExecutionContextCustomizer)
    T#5 extends Object declared in method <T#5>walk(JsonNode,OutputFormat<T#5>,boolean)
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:269: error: cannot find symbol
            ctx.setAttribute("before", true);
               ^
  symbol:   method setAttribute(String,boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:268: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer failing = ctx -> {
                                             ^
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:276: error: cannot find symbol
        assertEquals(Boolean.TRUE, executionContext.getAttribute("before"));
                                                   ^
  symbol:   method getAttribute(String)
  location: variable executionContext of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:292: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:295: error: cannot find symbol
        assertFalse(result.isValid(), "Unresolved $ref should make the result invalid");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:296: error: cannot find symbol
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("unresolved") || m.getMessage().contains("$ref")), "Expected at least one message about an unresolved $ref");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:306: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:309: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:310: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "Empty schema should produce no validation messages");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:321: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:324: error: cannot find symbol
        assertFalse(result.isValid());
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:325: error: cannot find symbol
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")), "Message should mention an unsupported type value");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:342: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, urlEncoded, InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:345: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:346: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "Decoded input should validate successfully");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:376: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, instance, InputFormat.JSON, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:379: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:380: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "YAML with anchors/aliases should be parsed and validated successfully");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:391: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        ValidationResult result = schema.walk(executionContext, json, InputFormat.JSON, false, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:394: error: cannot find symbol
        assertTrue(result.isValid(), "When validation is disabled, the result must be considered valid");
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:395: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is skipped");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:403: error: no suitable method found for walk(<null>,String,InputFormat,boolean,(ctx)->{ })
        assertThrows(NullPointerException.class, () -> schema.walk(null, """
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:413: error: no suitable method found for walk(ExecutionContext,String,<null>,boolean,(ctx)->{ })
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:428: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        assertThrows(IOException.class, () -> schema.walk(executionContext, brokenYaml, InputFormat.YAML, true, ctx -> {
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
src/test/java/com/networknt/schema/JsonSchema_walk_14_Test.java:439: error: no suitable method found for walk(ExecutionContext,String,InputFormat,boolean,(ctx)->{ })
        assertThrows(IllegalArgumentException.class, () -> schema.walk(executionContext, malformed, InputFormat.JSON, true, ctx -> {
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
53 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)': '\n     * Walk the input.\n     * \n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     * @return the validation result\n     ', 'return type': 'com.networknt.schema.ValidationResult', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Result com.networknt.schema.OutputFormat.RESULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public T walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the input.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param node             the input\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


