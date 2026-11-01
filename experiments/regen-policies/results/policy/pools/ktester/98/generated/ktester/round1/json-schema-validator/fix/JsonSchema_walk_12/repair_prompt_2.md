


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
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

// @ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper mapper;

//     @Mock
    private OutputFormat<String> // mocked OutputFormat used in all tests
    outputFormatMock;

    private SimpleOutputFormat outputFormat;

    /**
     * Simple test double for {@link OutputFormat}.
     */
    private static class SimpleOutputFormat implements OutputFormat<Object> {

        // By default just return the set of validation messages.
        private Function<Set<ValidationMessage>, Object> function = msgs -> msgs;

        private int callCount = 0;

        void setFunction(Function<Set<ValidationMessage>, Object> f) {
            this.function = f;
        }

        void reset() {
            callCount = 0;
            function = msgs -> msgs;
        }

        int getCallCount() {
            return callCount;
        }

        @Override
        public Object format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            callCount++;
            return function.apply(messages);
        }
    }

    /**
     * Simple test double for {@link ExecutionContextCustomizer}.
     */
    private static class TestCustomizer implements ExecutionContextCustomizer {

        private int invokeCount = 0;

        @Override
        public void customize(ExecutionContext ctx, ValidationContext vc) {
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
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "formatted-result");
        Object result = schema.walk(executionContext, node, outputFormat, false, (ExecutionContextCustomizer) null);
        assertEquals("formatted-result", result);
        assertEquals(1, outputFormat.getCallCount(), "OutputFormat should be invoked exactly once");
    }

    /**
     * Verifies that {@code walk(..., validate = true, customizer = null)} also returns the value
     * from the {@link OutputFormat} when the schema validates successfully.
     */
    @Test
    void walkWithValidationAndWithoutCustomizer_successfulValidation_returnsOutputFormatResult() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "ok");
        Object result = schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) null);
        assertEquals("ok", result);
        assertEquals(1, outputFormat.getCallCount());
    }

    /**
     * Verifies that a non‑null {@link ExecutionContextCustomizer} is accepted and does not interfere
     * with the normal flow of {@code walk}.
     */
    @Test
    void walkWithCustomizer_isInvokedAndReturnsOutputFormatResult() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"age\":30}");
        ExecutionContext executionContext = new ExecutionContext();
        TestCustomizer customizer = new TestCustomizer();
        outputFormat.setFunction(vr -> "customized");
        Object result = schema.walk(executionContext, node, outputFormat, false, (ExecutionContextCustomizer) customizer);
        assertEquals("customized", result);
        assertEquals(1, outputFormat.getCallCount());
        assertEquals(1, customizer.getInvokeCount(), "Customizer should be invoked exactly once");
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validStringNode_validatesSuccessfully
     * -------------------------------------------------------------- */
    @Test
    void walk_validStringNode_validatesSuccessfully() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"hello\"");
        ExecutionContext executionContext = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "When the node matches the schema, walk should return an empty set of ValidationMessage");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_invalidStringNode_validationFails
     * -------------------------------------------------------------- */
    @Test
    void walk_invalidStringNode_validationFails() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) null);
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
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, false, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "When validation flag is false, walk should skip validation and return an empty set");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_withExecutionCustomizerAddsCustomMessage
     * -------------------------------------------------------------- */
    @Test
    void walk_withExecutionCustomizerAddsCustomMessage() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"test\"");
        ExecutionContext executionContext = new ExecutionContext();
        TestCustomizer customizer = new TestCustomizer();
        Set<ValidationMessage> customSet = new HashSet<>();
        ValidationMessage customMessage = ValidationMessage.builder().message("custom: added by ExecutionContextCustomizer").build();
        customSet.add(customMessage);
        outputFormat.setFunction(vr -> customSet);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) customizer);
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
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext executionContext = new ExecutionContext();
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, outputFormat, true, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullExecutionContext_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext_throwsNullPointerException() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"valid\":\"yes\"}");
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, outputFormat, true, (ExecutionContextCustomizer) null));
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_walk_12_Test ✔
│     ├─ walkWithoutValidationAndWithoutCustomizer_returnsOutputFormatResult() ✔
│     ├─ walk_nullNode_throwsNullPointerException() ✘ Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ walk_validateFlagFalse_skipsValidation() ✔
│     ├─ walkWithCustomizer_isInvokedAndReturnsOutputFormatResult() ✔
│     ├─ walk_validStringNode_validatesSuccessfully() ✔
│     ├─ walk_invalidStringNode_validationFails() ✘ The message should mention the expected type String ==> expected: <true> but was: <false>
│     ├─ walk_withExecutionCustomizerAddsCustomMessage() ✔
│     ├─ walkWithValidationAndWithoutCustomizer_successfulValidation_returnsOutputFormatR... ✔
│     └─ walk_nullExecutionContext_throwsNullPointerException() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:JsonSchema_walk_12_Test:walk_nullNode_throwsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_12_Test', methodName = 'walk_nullNode_throwsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_walk_12_Test.walk_nullNode_throwsNullPointerException(JsonSchema_walk_12_Test.java:204)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_walk_12_Test:walk_invalidStringNode_validationFails()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_12_Test', methodName = 'walk_invalidStringNode_validationFails', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: The message should mention the expected type String ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_walk_12_Test.walk_invalidStringNode_validationFails(JsonSchema_walk_12_Test.java:158)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 332 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         9 tests found           ]
[         0 tests skipped         ]
[         9 tests started         ]
[         0 tests aborted         ]
[         7 tests successful      ]
[         2 tests failed          ]


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
 

api document of method walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer): 

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
	
class `com.networknt.schema.ExecutionContextCustomizer`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`, api document: 
     * Walk at the node.
     * 
     * @param executionContext the execution content
     * @param node             the current node
     * @param rootNode         the root node
     * @param instanceLocation the instance location
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
		method `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`, api document: 
     * Walk the input.
     * 
     * @param executionContext the execution context
     * @param input            the input
     * @param inputFormat      the input format
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
		method `public ValidationResult walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`, api document: 
     * Walk the JSON node.
     * 
     * @param executionContext the execution context
     * @param node             the input
     * @param validate         true to validate the input against the schema
     * @param executionCustomizer the customizer
     *
     * @return the validation result
     ;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
