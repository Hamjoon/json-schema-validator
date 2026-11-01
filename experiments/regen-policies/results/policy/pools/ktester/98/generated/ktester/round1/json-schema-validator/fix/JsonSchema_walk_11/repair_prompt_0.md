
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
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;

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
        OutputFormat<String> format = new OutputFormat<String>() {

            @Override
            public String apply(ValidationResult r) {
                return "OK";
            }
        };
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", true);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /**
     * Data provider for the “Normal operation – validation disabled” case.
     */
    static Stream<Arguments> normalDisabledProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", 30);
        OutputFormat<Integer> format = new OutputFormat<Integer>() {

            @Override
            public Integer apply(ValidationResult r) {
                return r.getMessages().size();
            }
        };
        return Stream.of(Arguments.of(null, node, format, false, null, 0));
    }

    /**
     * Data provider for “Null ExecutionContext – should be created internally”.
     */
    static Stream<Arguments> nullExecCtxProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("id", 1);
        OutputFormat<Boolean> format = r -> true;
        return Stream.of(Arguments.of(null, node, format, true, null, true));
    }

    /**
     * Data provider for “ExecutionCustomizer is invoked”.
     */
    static Stream<Arguments> customizerInvokedProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<String> format = r -> r.getMessages().isEmpty() ? "OK" : "FAIL";
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("visited", true);
        return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
    }

    /**
     * Data provider for “Validation failure – messages present”.
     */
    static Stream<Arguments> validationFailureProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("age", "twenty");
        // schema that expects integer for “age”
        JsonSchema schema = FACTORY.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """);
        OutputFormat<Integer> format = r -> r.getMessages().size();
        return Stream.of(Arguments.of(schema, node, format, true, null, 1));
    }

    /**
     * Data provider for “Array node validation – type mismatch”.
     */
    static Stream<Arguments> arrayMismatchProvider() {
        JsonNode node = JsonNodeFactory.instance.arrayNode().add(1).add(2);
        OutputFormat<Integer> format = r -> r.getMessages().size();
        return Stream.of(Arguments.of(ARRAY_STRING_SCHEMA, node, format, true, null, 1));
    }

    /**
     * Data provider for “Empty schema – no validators, validation enabled”.
     */
    static Stream<Arguments> emptySchemaProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Integer> format = r -> r.getMessages().size();
        return Stream.of(Arguments.of(EMPTY_SCHEMA, node, format, true, null, 0));
    }

    /**
     * Data provider for “ExecutionContext pre‑existing attributes preserved”.
     */
    static Stream<Arguments> preExistingAttributesProvider() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setAttribute("existing", true);
        JsonNode node = JsonNodeFactory.instance.objectNode().put("valid", true);
        OutputFormat<Boolean> format = r -> {
            ExecutionContext ec = (ExecutionContext) r.getContext();
            return Boolean.TRUE.equals(ec.getAttribute("existing")) && Boolean.TRUE.equals(ec.getAttribute("new"));
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("new", true);
        return Stream.of(Arguments.of(ctx, node, format, true, customizer, true));
    }

    /**
     * Data provider for “Null ExecutionContext with non‑null customizer”.
     */
    static Stream<Arguments> nullExecCtxWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
        OutputFormat<Boolean> format = r -> {
            ExecutionContext ec = (ExecutionContext) r.getContext();
            return Boolean.TRUE.equals(ec.getAttribute("custom"));
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("custom", true);
        return Stream.of(Arguments.of(null, node, format, true, customizer, true));
    }

    /**
     * Data provider for “Validation disabled with customizer invoked”.
     */
    static Stream<Arguments> disabledWithCustomizerProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("any", "value");
        OutputFormat<Boolean> format = r -> {
            ExecutionContext ec = (ExecutionContext) r.getContext();
            return Boolean.TRUE.equals(ec.getAttribute("visited"));
        };
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("visited", true);
        return Stream.of(Arguments.of(null, node, format, false, customizer, true));
    }

    /**
     * Data provider for “OutputFormat returns null”.
     */
    static Stream<Arguments> outputFormatReturnsNullProvider() {
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<Object> format = r -> null;
        return Stream.of(Arguments.of(null, node, format, true, null, null));
    }

    /* ----------------------------------------------------------------------
     * Helper that runs a walk with the supplied parameters.
     * ---------------------------------------------------------------------- */
    private static <T> T walk(JsonSchema schema, ExecutionContext execCtx, JsonNode node, OutputFormat<T> format, boolean validate, Consumer<ExecutionContext> customizer) {
        // If the caller passed null for the schema we fall back to the default one.
        JsonSchema effectiveSchema = (schema != null) ? schema : SCHEMA;
        return effectiveSchema.walk(execCtx, node, format, validate, customizer);
    }

    /**
     * When {@code validate} is {@code true} the walk method must perform validation and
     * return the set of {@link ValidationMessage}s produced by the schema.
     */
    @Test
    void walk_validatesWhenFlagIsTrue() throws Exception {
        // ----------------------------------------------------------------------
        // given
        // ----------------------------------------------------------------------
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        // JSON that violates the schema (name is a number, not a string)
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        // Simple OutputFormat that just returns the raw set of messages
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
        // No‑op customizer – we only need to satisfy the method signature
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        // ----------------------------------------------------------------------
        // when
        // ----------------------------------------------------------------------
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, true, customizer);
        // ----------------------------------------------------------------------
        // then
        // ----------------------------------------------------------------------
        assertNotNull(result, "Resulting message set must not be null");
        assertFalse(result.isEmpty(), "Invalid input should produce at least one validation message");
        // The message should refer to the 'type' keyword for the property 'name'
        boolean typeErrorPresent = result.stream().anyMatch(m -> "type".equals(m.getType()) && m.getPath().equals("$.name"));
        assertTrue(typeErrorPresent, "Expected a type‑error message for $.name");
    }

    /**
     * When {@code validate} is {@code false} the walk method must skip validation and
     * therefore return an empty set of messages.
     */
    @Test
    void walk_skipsValidationWhenFlagIsFalse() throws Exception {
        // ----------------------------------------------------------------------
        // given
        // ----------------------------------------------------------------------
        ExecutionContext execCtx = SCHEMA.createExecutionContext();
        // Same invalid JSON as before – it would normally cause a validation error
        JsonNode invalidNode = MAPPER.readTree("{\"name\": 1}");
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        // ----------------------------------------------------------------------
        // when
        // ----------------------------------------------------------------------
        Set<ValidationMessage> result = SCHEMA.walk(execCtx, invalidNode, format, false, customizer);
        // ----------------------------------------------------------------------
        // then
        // ----------------------------------------------------------------------
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
        OutputFormat<String> format = r -> "";
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, null, format, true, null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testNullOutputFormatThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, node, null, true, null));
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testCustomizerThrowsExceptionIsPropagated() {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        OutputFormat<String> format = r -> "unused";
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
            public String apply(ValidationResult r) {
                throw new RuntimeException("format failure");
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> SCHEMA.walk(ctx, node, format, true, null));
        assertEquals("format failure", thrown.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:71: error: <anonymous com.networknt.schema.JsonSchema_walk_11_Test$1> is not abstract and does not override abstract method format(JsonSchema,Set<ValidationMessage>,ExecutionContext,ValidationContext) in OutputFormat
        OutputFormat<String> format = new OutputFormat<String>() {
                                                                 ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:73: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:78: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("custom", true);
                                                          ^
  symbol:   method setAttribute(String,boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:87: error: <anonymous com.networknt.schema.JsonSchema_walk_11_Test$2> is not abstract and does not override abstract method format(JsonSchema,Set<ValidationMessage>,ExecutionContext,ValidationContext) in OutputFormat
        OutputFormat<Integer> format = new OutputFormat<Integer>() {
                                                                   ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:89: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:91: error: cannot find symbol
                return r.getMessages().size();
                        ^
  symbol:   method getMessages()
  location: variable r of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:102: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Boolean> format = r -> true;
                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:111: error: cannot find symbol
        OutputFormat<String> format = r -> r.getMessages().isEmpty() ? "OK" : "FAIL";
                                            ^
  symbol:   method getMessages()
  location: variable r of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:111: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<String> format = r -> r.getMessages().isEmpty() ? "OK" : "FAIL";
                                      ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:112: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("visited", true);
                                                          ^
  symbol:   method setAttribute(String,boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:129: error: cannot find symbol
        OutputFormat<Integer> format = r -> r.getMessages().size();
                                             ^
  symbol:   method getMessages()
  location: variable r of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:129: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Integer> format = r -> r.getMessages().size();
                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:138: error: cannot find symbol
        OutputFormat<Integer> format = r -> r.getMessages().size();
                                             ^
  symbol:   method getMessages()
  location: variable r of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:138: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Integer> format = r -> r.getMessages().size();
                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:147: error: cannot find symbol
        OutputFormat<Integer> format = r -> r.getMessages().size();
                                             ^
  symbol:   method getMessages()
  location: variable r of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:147: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Integer> format = r -> r.getMessages().size();
                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:156: error: cannot find symbol
        ctx.setAttribute("existing", true);
           ^
  symbol:   method setAttribute(String,boolean)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:159: error: cannot find symbol
            ExecutionContext ec = (ExecutionContext) r.getContext();
                                                      ^
  symbol:   method getContext()
  location: variable r of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:160: error: cannot find symbol
            return Boolean.TRUE.equals(ec.getAttribute("existing")) && Boolean.TRUE.equals(ec.getAttribute("new"));
                                         ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:160: error: cannot find symbol
            return Boolean.TRUE.equals(ec.getAttribute("existing")) && Boolean.TRUE.equals(ec.getAttribute("new"));
                                                                                             ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:158: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Boolean> format = r -> {
                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:162: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("new", true);
                                                        ^
  symbol:   method setAttribute(String,boolean)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:172: error: cannot find symbol
            ExecutionContext ec = (ExecutionContext) r.getContext();
                                                      ^
  symbol:   method getContext()
  location: variable r of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:173: error: cannot find symbol
            return Boolean.TRUE.equals(ec.getAttribute("custom"));
                                         ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:171: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Boolean> format = r -> {
                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:175: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("custom", true);
                                                        ^
  symbol:   method setAttribute(String,boolean)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:185: error: cannot find symbol
            ExecutionContext ec = (ExecutionContext) r.getContext();
                                                      ^
  symbol:   method getContext()
  location: variable r of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:186: error: cannot find symbol
            return Boolean.TRUE.equals(ec.getAttribute("visited"));
                                         ^
  symbol:   method getAttribute(String)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:184: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Boolean> format = r -> {
                                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:188: error: cannot find symbol
        Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("visited", true);
                                                        ^
  symbol:   method setAttribute(String,boolean)
  location: variable ec of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:197: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Object> format = r -> null;
                                      ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:223: error: incompatible types: bad return type in lambda expression
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
                                                                  ^
    JsonSchema cannot be converted to Set<ValidationMessage>
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:223: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
                                                      ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:237: error: cannot find symbol
        boolean typeErrorPresent = result.stream().anyMatch(m -> "type".equals(m.getType()) && m.getPath().equals("$.name"));
                                                                                                ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:253: error: incompatible types: bad return type in lambda expression
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
                                                                  ^
    JsonSchema cannot be converted to Set<ValidationMessage>
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:253: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<Set<ValidationMessage>> format = messages -> messages;
                                                      ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:347: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<String> format = r -> "";
                                      ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:348: error: reference to walk is ambiguous
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, null, format, true, null));
                                                                                                   ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:356: error: reference to walk is ambiguous
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> SCHEMA.walk(ctx, node, null, true, null));
                                                                                                   ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:364: error: incompatible types: incompatible parameter types in lambda expression
        OutputFormat<String> format = r -> "unused";
                                      ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:375: error: <anonymous com.networknt.schema.JsonSchema_walk_11_Test$3> is not abstract and does not override abstract method format(JsonSchema,Set<ValidationMessage>,ExecutionContext,ValidationContext) in OutputFormat
        OutputFormat<String> format = new OutputFormat<String>() {
                                                                 ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:377: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_walk_11_Test.java:382: error: reference to walk is ambiguous
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> SCHEMA.walk(ctx, node, format, true, null));
                                                                                   ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
46 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)': '\n     * Walk the JSON node.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param node             the input\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`, api document: \n     * Walk the input.\n     * \n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


