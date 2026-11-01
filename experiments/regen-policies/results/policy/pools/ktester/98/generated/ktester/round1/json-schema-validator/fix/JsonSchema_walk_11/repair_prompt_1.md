
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_11_Test {

    private static ObjectMapper MAPPER;

    private static JsonSchemaFactory FACTORY;

    private static JsonSchema SCHEMA;

    /**
     * Simple schema: an object with a required string property {@code name}.
     */
    private static final String SCHEMA_JSON = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": ["name"]
        }
        """;

    // "{}"
    private static JsonSchema EMPTY_SCHEMA;

    // expects array of strings
    private static JsonSchema ARRAY_STRING_SCHEMA;

    private static final String EMPTY_SCHEMA_JSON = "{}";

    private static final String ARRAY_STRING_SCHEMA_JSON = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "array",
          "items": { "type": "string" }
        }
        """;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        MAPPER = new ObjectMapper();
        FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SCHEMA = FACTORY.getSchema(SCHEMA_JSON);
        EMPTY_SCHEMA = FACTORY.getSchema(EMPTY_SCHEMA_JSON);
        ARRAY_STRING_SCHEMA = FACTORY.getSchema(ARRAY_STRING_SCHEMA_JSON);
    }

    /**
     * Data provider for the “Normal operation – validation enabled” case.
     */
    static Stream<Arguments> normalEnabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "John");
        OutputFormat<String> format = (schema, messages, ctx, vc) -> "OK";
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /**
     * Data provider for the “Normal operation – validation disabled” case.
     */
    static Stream<Arguments> normalDisabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", 30);
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(null, node, format, false, null, 0));
    }

    /**
     * Data provider for “Null ExecutionContext – should be created internally”.
     */
    static Stream<Arguments> nullExecCtxProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("id", 1);
        OutputFormat<Boolean> format = (schema, messages, ctx, vc) -> true;
        return Stream.of(Arguments.of(null, node, format, true, null, true));
    }

    /**
     * Data provider for “ExecutionCustomizer is invoked”.
     */
    static Stream<Arguments> customizerInvokedProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<String> format = (schema, messages, ctx, vc) -> messages.isEmpty() ? "OK" : "FAIL";
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("visited", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /**
     * Data provider for “Validation failure – messages present”.
     */
    static Stream<Arguments> validationFailureProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", "twenty");
        JsonSchema schema = FACTORY.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """);
        OutputFormat<Integer> format = (schema1, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(schema, node, format, true, null, 1));
    }

    /**
     * Data provider for “Array node validation – type mismatch”.
     */
    static Stream<Arguments> arrayMismatchProvider() {
        JsonNode node = JsonNodeFactory.instance.arrayNode().add(1).add(2);
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(ARRAY_STRING_SCHEMA, node, format, true, null, 1));
    }

    /**
     * Data provider for “Empty schema – no validators, validation enabled”.
     */
    static Stream<Arguments> emptySchemaProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Integer> format = (schema, messages, ctx, vc) -> messages.size();
        return Stream.of(Arguments.of(EMPTY_SCHEMA, node, format, true, null, 0));
    }

    /**
     * Data provider for “ExecutionContext pre‑existing attributes preserved”.
     */
    static Stream<Arguments> preExistingAttributesProvider() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setAttribute("existing", Boolean.TRUE);
        JsonNode node = JsonNodeFactory.instance.objectNode().put("valid", true);
        OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
            Boolean existing = (Boolean) ec.getAttribute("existing");
            Boolean added = (Boolean) ec.getAttribute("new");
            return Boolean.TRUE.equals(existing) && Boolean.TRUE.equals(added);
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("new", Boolean.TRUE);
        return Stream.of(Arguments.of(ctx, node, format, true, customizer, true));
    }

    /**
     * Data provider for “Null ExecutionContext with non‑null customizer”.
     */
    static Stream<Arguments> nullExecCtxWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
            Boolean custom = (Boolean) ec.getAttribute("custom");
            return Boolean.TRUE.equals(custom);
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("custom", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, true, customizer, true));
    }

    /**
     * Data provider for “Validation disabled with customizer invoked”.
     */
    static Stream<Arguments> disabledWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("any", "value");
        OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
            Boolean visited = (Boolean) ec.getAttribute("visited");
            return Boolean.TRUE.equals(visited);
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("visited", Boolean.TRUE);
        return Stream.of(Arguments.of(null, node, format, false, customizer, true));
    }

    /**
     * Data provider for “OutputFormat returns null”.
     */
    static Stream<Arguments> outputFormatReturnsNullProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Object> format = (schema, messages, ec, vc) -> null;
        return Stream.of(Arguments.of(null, node, format, true, null, null));
    }

    /* ----------------------------------------------------------------------
     * Helper that runs a walk with the supplied parameters.
     * ---------------------------------------------------------------------- */
    private static <T> T walk(JsonSchema schema, ExecutionContext execCtx, JsonNode node, OutputFormat<T> format, boolean validate, Consumer<ExecutionContext> customizer) {
        JsonSchema effective = (schema != null) ? schema : SCHEMA;
        return effective.walk(execCtx, node, format, validate, customizer);
    }

    /**
     * When {@code validate} is {@code true} the walk method must perform validation and
     * return the set of {@link ValidationMessage}s produced by the schema.
     */
    @Test
    void walk_validatesWhenFlagIsTrue() throws Exception {
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        OutputFormat<Set<ValidationMessage>> format = (schema, messages, ctx, vc) -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, true, customizer);
        assertNotNull(result, "Resulting message set must not be null");
        assertFalse(result.isEmpty(), "Invalid input should produce at least one validation message");
        boolean typeErrorPresent = result.stream().anyMatch(m -> "type".equals(m.getType()) && "$.name".equals(m.getPath()));
        assertTrue(typeErrorPresent, "Expected a type‑error message for $.name");
    }

    /**
     * When {@code validate} is {@code false} the walk method must skip validation and
     * therefore return an empty set of messages.
     */
    @Test
    void walk_skipsValidationWhenFlagIsFalse() throws Exception {
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        OutputFormat<Set<ValidationMessage>> format = (schema, messages, ctx, vc) -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, false, customizer);
        assertNotNull(result, "Resulting message set must not be null");
        assertTrue(result.isEmpty(), "When validation is disabled the result set should be empty");
    }

    @ParameterizedTest(name = "[{index}] Normal operation – validation enabled")
    @MethodSource("normalEnabledProvider")
    void testNormalOperationEnabled(ExecutionContext execCtx, JsonNode node, OutputFormat<String> format, boolean validate, Consumer<ExecutionContext> customizer, String expected) {
        String result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Normal operation – validation disabled")
    @MethodSource("normalDisabledProvider")
    void testNormalOperationDisabled(ExecutionContext execCtx, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Null ExecutionContext – internal creation")
    @MethodSource("nullExecCtxProvider")
    void testNullExecutionContext(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] ExecutionCustomizer is invoked")
    @MethodSource("customizerInvokedProvider")
    void testCustomizerInvoked(ExecutionContext execCtx, JsonNode node, OutputFormat<String> format, boolean validate, Consumer<ExecutionContext> customizer, String expected) {
        String result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation failure – messages present")
    @MethodSource("validationFailureProvider")
    void testValidationFailure(JsonSchema schema, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Array node validation – type mismatch")
    @MethodSource("arrayMismatchProvider")
    void testArrayTypeMismatch(JsonSchema schema, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Empty schema – no validators")
    @MethodSource("emptySchemaProvider")
    void testEmptySchema(JsonSchema schema, JsonNode node, OutputFormat<Integer> format, boolean validate, Consumer<ExecutionContext> customizer, Integer expected) {
        Integer result = walk(schema, null, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] ExecutionContext pre‑existing attributes preserved")
    @MethodSource("preExistingAttributesProvider")
    void testPreExistingAttributes(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Null ExecutionContext with non‑null customizer")
    @MethodSource("nullExecCtxWithCustomizerProvider")
    void testNullExecCtxWithCustomizer(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation disabled with customizer invoked")
    @MethodSource("disabledWithCustomizerProvider")
    void testDisabledWithCustomizer(ExecutionContext execCtx, JsonNode node, OutputFormat<Boolean> format, boolean validate, Consumer<ExecutionContext> customizer, Boolean expected) {
        Boolean result = walk(null, execCtx, node, format, validate, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] OutputFormat returns null")
    @MethodSource("outputFormatReturnsNullProvider")
    void testOutputFormatReturnsNull(ExecutionContext execCtx, JsonNode node, OutputFormat<Object> format, boolean validate, Consumer<ExecutionContext> customizer, Object expected) {
        Object result = walk(null, execCtx, node, format, validate, customizer);
        assertNull(result);
    }

    @Test
    void testNullJsonNodeThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = (s, m, c, v) -> "";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, null, format, true, (Consumer<ExecutionContext>) null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testNullOutputFormatThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, node, null, true, (Consumer<ExecutionContext>) null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testCustomizerThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<String> format = (s, m, c, v) -> "unused";
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> SCHEMA.walk(ctx, node, format, true, c -> {
            throw new RuntimeException("customizer failure");
        }));
        assertEquals("customizer failure", thrown.getMessage());
    }

    @Test
    void testOutputFormatThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "John");
        OutputFormat<String> format = new OutputFormat<String>() {

            @Override
            public String format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
                throw new RuntimeException("format failure");
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> SCHEMA.walk(ctx, node, format, true, (Consumer<ExecutionContext>) null));
        assertEquals("format failure", thrown.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:19: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:73: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
                                                          ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:101: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("visited", Boolean.TRUE);
                                                          ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:144: error: cannot find symbol
        ctx.setAttribute("existing", Boolean.TRUE);
           ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:147: error: cannot find symbol
            Boolean existing = (Boolean) ec.getAttribute("existing");
                                           ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:148: error: cannot find symbol
            Boolean added = (Boolean) ec.getAttribute("new");
                                        ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:151: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("new", Boolean.TRUE);
                                                        ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:161: error: cannot find symbol
            Boolean custom = (Boolean) ec.getAttribute("custom");
                                         ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:164: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("custom", Boolean.TRUE);
                                                        ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:174: error: cannot find symbol
            Boolean visited = (Boolean) ec.getAttribute("visited");
                                          ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:177: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("visited", Boolean.TRUE);
                                                        ^
  symbol:   method setAttribute(String,Boolean)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:212: error: cannot find symbol
        boolean typeErrorPresent = result.stream().anyMatch(m -> "type".equals(m.getType()) && "$.name".equals(m.getPath()));
                                                                                                                ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
13 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)': '\n     * Walk the JSON node.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param node             the input\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`, api document: \n     * Walk the input.\n     * \n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


