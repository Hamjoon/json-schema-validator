


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

// @ExtendWith(MockitoExtension.class)
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
        Consumer<ExecutionContext> customizer = ctx -> {
        };
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
//     static Stream<Arguments> customizerInvokedProvider() {
//         JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
//         OutputFormat<String> format = (schema, messages, ctx, vc) -> messages.isEmpty() ? "OK" : "FAIL";
//         Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("visited", Boolean.TRUE);
//         return Stream.of(Arguments.of(null, node, format, true, customizer, "OK"));
//     }

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
//     static Stream<Arguments> preExistingAttributesProvider() {
//         ExecutionContext ctx = new ExecutionContext();
//         ctx.setAttribute("existing", Boolean.TRUE);
//         JsonNode node = JsonNodeFactory.instance.objectNode().put("valid", true);
//         OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
//             Boolean existing = (Boolean) ec.getAttribute("existing");
//             Boolean added = (Boolean) ec.getAttribute("new");
//             return Boolean.TRUE.equals(existing) && Boolean.TRUE.equals(added);
//         };
//         Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("new", Boolean.TRUE);
//         return Stream.of(Arguments.of(ctx, node, format, true, customizer, true));
//     }

    /**
     * Data provider for “Null ExecutionContext with non‑null customizer”.
     */
//     static Stream<Arguments> nullExecCtxWithCustomizerProvider() {
//         JsonNode node = JsonNodeFactory.instance.objectNode().put("flag", true);
//         OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
//             Boolean custom = (Boolean) ec.getAttribute("custom");
//             return Boolean.TRUE.equals(custom);
//         };
//         Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("custom", Boolean.TRUE);
//         return Stream.of(Arguments.of(null, node, format, true, customizer, true));
//     }

    /**
     * Data provider for “Validation disabled with customizer invoked”.
     */
//     static Stream<Arguments> disabledWithCustomizerProvider() {
//         JsonNode node = JsonNodeFactory.instance.objectNode().put("any", "value");
//         OutputFormat<Boolean> format = (schema, messages, ec, vc) -> {
//             Boolean visited = (Boolean) ec.getAttribute("visited");
//             return Boolean.TRUE.equals(visited);
//         };
//         Consumer<ExecutionContext> customizer = ec -> ec.setAttribute("visited", Boolean.TRUE);
//         return Stream.of(Arguments.of(null, node, format, false, customizer, true));
//     }

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
        // Verify that at least one message mentions a type error (exact path checking omitted for compatibility)
        boolean typeErrorPresent = result.stream().anyMatch(m -> "type".equals(m.getType()));
        assertTrue(typeErrorPresent, "Expected a type‑error message");
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

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_walk_11_Test ✔
│     ├─ testNormalOperationDisabled(ExecutionContext, JsonNode, OutputFormat, boolean, C... ✔
│     │  └─ [1] Normal operation – validation disabled ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     ├─ walk_validatesWhenFlagIsTrue() ✔
│     ├─ testNullJsonNodeThrowsException() ✘ Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     ├─ testNullOutputFormatThrowsException() ✘ Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     ├─ testOutputFormatReturnsNull(ExecutionContext, JsonNode, OutputFormat, boolean, C... ✔
│     │  └─ [1] OutputFormat returns null ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     ├─ testDisabledWithCustomizer(ExecutionContext, JsonNode, OutputFormat, boolean, Co... ✘ Could not find factory method [disabledWithCustomizerProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
│     ├─ testArrayTypeMismatch(JsonSchema, JsonNode, OutputFormat, boolean, Consumer, Int... ✔
│     │  └─ [1] Array node validation – type mismatch ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     ├─ testOutputFormatThrowsExceptionIsPropagated() ✘ expected: <format failure> but was: <Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null>
│     ├─ testNullExecCtxWithCustomizer(ExecutionContext, JsonNode, OutputFormat, boolean,... ✘ Could not find factory method [nullExecCtxWithCustomizerProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
│     ├─ walk_skipsValidationWhenFlagIsFalse() ✔
│     ├─ testNullExecutionContext(ExecutionContext, JsonNode, OutputFormat, boolean, Cons... ✔
│     │  └─ [1] Null ExecutionContext – internal creation ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     ├─ testEmptySchema(JsonSchema, JsonNode, OutputFormat, boolean, Consumer, Integer) ✔
│     │  └─ [1] Empty schema – no validators ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     ├─ testCustomizerInvoked(ExecutionContext, JsonNode, OutputFormat, boolean, Consume... ✘ Could not find factory method [customizerInvokedProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
│     ├─ testNormalOperationEnabled(ExecutionContext, JsonNode, OutputFormat, boolean, Co... ✔
│     │  └─ [1] Normal operation – validation enabled ✘ Cannot invoke "com.networknt.schema.ExecutionContext.getExecutionConfig()" because "executionContext" is null
│     ├─ testCustomizerThrowsExceptionIsPropagated() ✔
│     ├─ testPreExistingAttributes(ExecutionContext, JsonNode, OutputFormat, boolean, Con... ✘ Could not find factory method [preExistingAttributesProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
│     └─ testValidationFailure(JsonSchema, JsonNode, OutputFormat, boolean, Consumer, Int... ✔
│        └─ [1] Validation failure – messages present ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (14):
  JUnit Jupiter:JsonSchema_walk_11_Test:testNormalOperationDisabled(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, Integer):[1] Normal operation – validation disabled
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testNormalOperationDisabled', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Integer']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.walk(JsonSchema_walk_11_Test.java:193)
       com.networknt.schema.JsonSchema_walk_11_Test.testNormalOperationDisabled(JsonSchema_walk_11_Test.java:241)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testNullJsonNodeThrowsException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testNullJsonNodeThrowsException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_walk_11_Test.testNullJsonNodeThrowsException(JsonSchema_walk_11_Test.java:312)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.lambda$testNullJsonNodeThrowsException$14(JsonSchema_walk_11_Test.java:312)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testNullOutputFormatThrowsException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testNullOutputFormatThrowsException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_walk_11_Test.testNullOutputFormatThrowsException(JsonSchema_walk_11_Test.java:320)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.lambda$testNullOutputFormatThrowsException$15(JsonSchema_walk_11_Test.java:320)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testOutputFormatReturnsNull(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, Object):[1] OutputFormat returns null
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testOutputFormatReturnsNull', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Object']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.walk(JsonSchema_walk_11_Test.java:193)
       com.networknt.schema.JsonSchema_walk_11_Test.testOutputFormatReturnsNull(JsonSchema_walk_11_Test.java:304)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testDisabledWithCustomizer(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, Boolean)
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testDisabledWithCustomizer', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Boolean']
    => org.junit.platform.commons.PreconditionViolationException: Could not find factory method [disabledWithCustomizerProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
       org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:314)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodBySimpleName(MethodArgumentsProvider.java:154)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodByFullyQualifiedName(MethodArgumentsProvider.java:134)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethod(MethodArgumentsProvider.java:77)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.lambda$provideArguments$1(MethodArgumentsProvider.java:53)
       [...]
       Suppressed: org.junit.platform.commons.PreconditionViolationException: Configuration error: You must configure at least one set of arguments for this @ParameterizedTest
         org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:299)
         org.junit.jupiter.params.ParameterizedTestExtension.lambda$provideTestTemplateInvocationContexts$5(ParameterizedTestExtension.java:98)
         java.base/java.util.stream.AbstractPipeline.close(AbstractPipeline.java:323)
         java.base/java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:273)
         [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testArrayTypeMismatch(JsonSchema, JsonNode, OutputFormat, boolean, Consumer, Integer):[1] Array node validation – type mismatch
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testArrayTypeMismatch', methodParameterTypes = 'com.networknt.schema.JsonSchema, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Integer']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.walk(JsonSchema_walk_11_Test.java:193)
       com.networknt.schema.JsonSchema_walk_11_Test.testArrayTypeMismatch(JsonSchema_walk_11_Test.java:269)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testOutputFormatThrowsExceptionIsPropagated()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testOutputFormatThrowsExceptionIsPropagated', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <format failure> but was: <Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:177)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1145)
       com.networknt.schema.JsonSchema_walk_11_Test.testOutputFormatThrowsExceptionIsPropagated(JsonSchema_walk_11_Test.java:347)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testNullExecCtxWithCustomizer(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, Boolean)
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testNullExecCtxWithCustomizer', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Boolean']
    => org.junit.platform.commons.PreconditionViolationException: Could not find factory method [nullExecCtxWithCustomizerProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
       org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:314)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodBySimpleName(MethodArgumentsProvider.java:154)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodByFullyQualifiedName(MethodArgumentsProvider.java:134)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethod(MethodArgumentsProvider.java:77)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.lambda$provideArguments$1(MethodArgumentsProvider.java:53)
       [...]
       Suppressed: org.junit.platform.commons.PreconditionViolationException: Configuration error: You must configure at least one set of arguments for this @ParameterizedTest
         org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:299)
         org.junit.jupiter.params.ParameterizedTestExtension.lambda$provideTestTemplateInvocationContexts$5(ParameterizedTestExtension.java:98)
         java.base/java.util.stream.AbstractPipeline.close(AbstractPipeline.java:323)
         java.base/java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:273)
         [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testNullExecutionContext(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, Boolean):[1] Null ExecutionContext – internal creation
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testNullExecutionContext', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Boolean']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.walk(JsonSchema_walk_11_Test.java:193)
       com.networknt.schema.JsonSchema_walk_11_Test.testNullExecutionContext(JsonSchema_walk_11_Test.java:248)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testEmptySchema(JsonSchema, JsonNode, OutputFormat, boolean, Consumer, Integer):[1] Empty schema – no validators
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testEmptySchema', methodParameterTypes = 'com.networknt.schema.JsonSchema, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Integer']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.walk(JsonSchema_walk_11_Test.java:193)
       com.networknt.schema.JsonSchema_walk_11_Test.testEmptySchema(JsonSchema_walk_11_Test.java:276)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testCustomizerInvoked(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, String)
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testCustomizerInvoked', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.String']
    => org.junit.platform.commons.PreconditionViolationException: Could not find factory method [customizerInvokedProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
       org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:314)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodBySimpleName(MethodArgumentsProvider.java:154)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodByFullyQualifiedName(MethodArgumentsProvider.java:134)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethod(MethodArgumentsProvider.java:77)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.lambda$provideArguments$1(MethodArgumentsProvider.java:53)
       [...]
       Suppressed: org.junit.platform.commons.PreconditionViolationException: Configuration error: You must configure at least one set of arguments for this @ParameterizedTest
         org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:299)
         org.junit.jupiter.params.ParameterizedTestExtension.lambda$provideTestTemplateInvocationContexts$5(ParameterizedTestExtension.java:98)
         java.base/java.util.stream.AbstractPipeline.close(AbstractPipeline.java:323)
         java.base/java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:273)
         [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testNormalOperationEnabled(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, String):[1] Normal operation – validation enabled
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testNormalOperationEnabled', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ExecutionContext.getExecutionConfig()" because "executionContext" is null
       com.networknt.schema.BaseJsonValidator.debug(BaseJsonValidator.java:135)
       com.networknt.schema.PropertiesValidator.validate(PropertiesValidator.java:66)
       com.networknt.schema.PropertiesValidator.walk(PropertiesValidator.java:133)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1287)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1256)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.walk(JsonSchema_walk_11_Test.java:193)
       com.networknt.schema.JsonSchema_walk_11_Test.testNormalOperationEnabled(JsonSchema_walk_11_Test.java:234)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testPreExistingAttributes(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer, Boolean)
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testPreExistingAttributes', methodParameterTypes = 'com.networknt.schema.ExecutionContext, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Boolean']
    => org.junit.platform.commons.PreconditionViolationException: Could not find factory method [preExistingAttributesProvider] in class [com.networknt.schema.JsonSchema_walk_11_Test]
       org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:314)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodBySimpleName(MethodArgumentsProvider.java:154)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethodByFullyQualifiedName(MethodArgumentsProvider.java:134)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.findFactoryMethod(MethodArgumentsProvider.java:77)
       org.junit.jupiter.params.provider.MethodArgumentsProvider.lambda$provideArguments$1(MethodArgumentsProvider.java:53)
       [...]
       Suppressed: org.junit.platform.commons.PreconditionViolationException: Configuration error: You must configure at least one set of arguments for this @ParameterizedTest
         org.junit.platform.commons.util.Preconditions.condition(Preconditions.java:299)
         org.junit.jupiter.params.ParameterizedTestExtension.lambda$provideTestTemplateInvocationContexts$5(ParameterizedTestExtension.java:98)
         java.base/java.util.stream.AbstractPipeline.close(AbstractPipeline.java:323)
         java.base/java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:273)
         [...]
  JUnit Jupiter:JsonSchema_walk_11_Test:testValidationFailure(JsonSchema, JsonNode, OutputFormat, boolean, Consumer, Integer):[1] Validation failure – messages present
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_11_Test', methodName = 'testValidationFailure', methodParameterTypes = 'com.networknt.schema.JsonSchema, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.OutputFormat, boolean, java.util.function.Consumer, java.lang.Integer']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1076)
       com.networknt.schema.JsonSchema_walk_11_Test.walk(JsonSchema_walk_11_Test.java:193)
       com.networknt.schema.JsonSchema_walk_11_Test.testValidationFailure(JsonSchema_walk_11_Test.java:262)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 281 ms
[        15 containers found      ]
[         0 containers skipped    ]
[        15 containers started    ]
[         0 containers aborted    ]
[        11 containers successful ]
[         4 containers failed     ]
[        13 tests found           ]
[         0 tests skipped         ]
[        13 tests started         ]
[         0 tests aborted         ]
[         3 tests successful      ]
[        10 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

api document of method walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>): 

     * Walk the JSON node.
     * 
     * @param <T>         the result type
     * @param executionContext the execution context
     * @param node             the input
     * @param outputFormat     the output format
     * @param validate         true to validate the input against the schema
     * @param executionCustomizer the customizer
     *
     * @return the validation result
     

return type: 
T

dependent classes: 
class `com.networknt.schema.ExecutionContext`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public ValidationResult walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`, api document: 
     * Walk the JSON node.
     * 
     * @param executionContext the execution context
     * @param node             the input
     * @param validate         true to validate the input against the schema
     * @param executionCustomizer the customizer
     *
     * @return the validation result
     ;
		method `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`, api document: 
     * Walk the input.
     * 
     * @param executionContext the execution context
     * @param input            the input
     * @param inputFormat      the input format
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
