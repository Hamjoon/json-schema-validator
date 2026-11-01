
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper mapper;

    @Mock
    private OutputFormat<String> // mocked OutputFormat used in all tests
    outputFormatMock;

    private SimpleOutputFormat outputFormat;

    /**
     * Simple test double for {@link OutputFormat}.
     */
    private static class SimpleOutputFormat implements OutputFormat<Object> {

        // default
        private Function<ValidationResult, Object> function = vr -> vr.getMessages();

        private int callCount = 0;

        void setFunction(Function<ValidationResult, Object> f) {
            this.function = f;
        }

        void reset() {
            callCount = 0;
            function = vr -> vr.getMessages();
        }

        int getCallCount() {
            return callCount;
        }

        @Override
        public Object apply(ValidationResult validationResult) {
            callCount++;
            return function.apply(validationResult);
        }
    }

    /**
     * Simple test double for {@link ExecutionContextCustomizer}.
     */
    private static class TestCustomizer implements ExecutionContextCustomizer {

        private int invokeCount = 0;

        @Override
        public void customize(ExecutionContext ctx) {
            invokeCount++;
        }

        int getInvokeCount() {
            return invokeCount;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use draft‑07 – it is sufficient for the tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        outputFormat = new SimpleOutputFormat();
        outputFormat.reset();
    }

    /**
     * Verifies that {@code walk(..., validate = false, customizer = null)} returns the value
     * produced by the supplied {@link OutputFormat}.
     */
    @Test
    void walkWithoutValidationAndWithoutCustomizer_returnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "formatted-result");
        // WHEN
        Object result = schema.walk(executionContext, node, outputFormat, false, null);
        // THEN
        assertEquals("formatted-result", result);
        assertEquals(1, outputFormat.getCallCount(), "OutputFormat should be invoked exactly once");
    }

    /**
     * Verifies that {@code walk(..., validate = true, customizer = null)} also returns the value
     * from the {@link OutputFormat} when the schema validates successfully.
     */
    @Test
    void walkWithValidationAndWithoutCustomizer_successfulValidation_returnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "ok");
        // WHEN
        Object result = schema.walk(executionContext, node, outputFormat, true, null);
        // THEN
        assertEquals("ok", result);
        assertEquals(1, outputFormat.getCallCount());
    }

    /**
     * Verifies that a non‑null {@link ExecutionContextCustomizer} is accepted and does not interfere
     * with the normal flow of {@code walk}.
     */
    @Test
    void walkWithCustomizer_isInvokedAndReturnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"age\":30}");
        ExecutionContext executionContext = new ExecutionContext();
        TestCustomizer customizer = new TestCustomizer();
        outputFormat.setFunction(vr -> "customized");
        // WHEN
        Object result = schema.walk(executionContext, node, outputFormat, false, customizer);
        // THEN
        assertEquals("customized", result);
        assertEquals(1, outputFormat.getCallCount());
        assertEquals(1, customizer.getInvokeCount(), "Customizer should be invoked exactly once");
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validStringNode_validatesSuccessfully
     * -------------------------------------------------------------- */
    @Test
    void walk_validStringNode_validatesSuccessfully() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"hello\"");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, null);
        // THEN
        assertTrue(result.isEmpty(), "When the node matches the schema, walk should return an empty set of ValidationMessage");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_invalidStringNode_validationFails
     * -------------------------------------------------------------- */
    @Test
    void walk_invalidStringNode_validationFails() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, null);
        // THEN
        assertEquals(1, result.size(), "A type‑mismatch should produce a single ValidationMessage");
        ValidationMessage message = result.iterator().next();
        assertTrue(message.getMessage().contains("expected type: String"), "The message should mention the expected type String");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validateFlagFalse_skipsValidation
     * -------------------------------------------------------------- */
    @Test
    void walk_validateFlagFalse_skipsValidation() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, false, null);
        // THEN
        assertTrue(result.isEmpty(), "When validation flag is false, walk should skip validation and return an empty set");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_withExecutionCustomizerAddsCustomMessage
     * -------------------------------------------------------------- */
    @Test
    void walk_withExecutionCustomizerAddsCustomMessage() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"test\"");
        ExecutionContext executionContext = new ExecutionContext();
        TestCustomizer customizer = new TestCustomizer();
        // The OutputFormat will return a synthetic set containing a custom ValidationMessage.
        Set<ValidationMessage> customSet = new HashSet<>();
        ValidationMessage customMessage = ValidationMessage.of("custom: added by ExecutionContextCustomizer");
        customSet.add(customMessage);
        outputFormat.setFunction(vr -> customSet);
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, customizer);
        // THEN
        assertEquals(1, customizer.getInvokeCount(), "Customizer must have been called exactly once");
        assertEquals(1, result.size(), "Result set should contain exactly the custom message injected by the test setup");
        assertTrue(result.contains(customMessage), "Result set must contain the expected custom ValidationMessage");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullNode_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullNode_throwsNullPointerException() {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, outputFormat, true, null));
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullExecutionContext_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext_throwsNullPointerException() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"valid\":\"yes\"}");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, outputFormat, true, null));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:12: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:22: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_12_Test
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:31: error: SimpleOutputFormat is not abstract and does not override abstract method format(JsonSchema,Set<ValidationMessage>,ExecutionContext,ValidationContext) in OutputFormat
    private static class SimpleOutputFormat implements OutputFormat<Object> {
                   ^
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:34: error: cannot find symbol
        private Function<ValidationResult, Object> function = vr -> vr.getMessages();
                                                                      ^
  symbol:   method getMessages()
  location: variable vr of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:44: error: cannot find symbol
            function = vr -> vr.getMessages();
                               ^
  symbol:   method getMessages()
  location: variable vr of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:51: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:61: error: TestCustomizer is not abstract and does not override abstract method customize(ExecutionContext,ValidationContext) in ExecutionContextCustomizer
    private static class TestCustomizer implements ExecutionContextCustomizer {
                   ^
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:65: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:100: error: reference to walk is ambiguous
        Object result = schema.walk(executionContext, node, outputFormat, false, null);
                              ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:118: error: reference to walk is ambiguous
        Object result = schema.walk(executionContext, node, outputFormat, true, null);
                              ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:155: error: reference to walk is ambiguous
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, null);
                                                                       ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:172: error: reference to walk is ambiguous
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, null);
                                                                       ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:191: error: reference to walk is ambiguous
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, false, null);
                                                                       ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:209: error: cannot find symbol
        ValidationMessage customMessage = ValidationMessage.of("custom: added by ExecutionContextCustomizer");
                                                           ^
  symbol:   method of(String)
  location: class ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:231: error: reference to walk is ambiguous
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, outputFormat, true, null));
                                                             ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
src/test/java/com/networknt/schema/JsonSchema_walk_12_Test.java:243: error: reference to walk is ambiguous
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, outputFormat, true, null));
                                                             ^
  both method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer) in JsonSchema and method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>) in JsonSchema match
  where T#1,T#2 are type-variables:
    T#1 extends Object declared in method <T#1>walk(ExecutionContext,JsonNode,OutputFormat<T#1>,boolean,ExecutionContextCustomizer)
    T#2 extends Object declared in method <T#2>walk(ExecutionContext,JsonNode,OutputFormat<T#2>,boolean,Consumer<ExecutionContext>)
18 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)': '\n     * Walk the JSON node.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param node             the input\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk at the node.\n     * \n     * @param executionContext the execution content\n     * @param node             the current node\n     * @param rootNode         the root node\n     * @param instanceLocation the instance location\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the input.\n     * \n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


