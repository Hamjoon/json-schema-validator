


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.util.Set;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_walk_8_Test {

    private static final String SIMPLE_SCHEMA = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}";

    private JsonSchema schema;

    @BeforeEach
    void setupBeforeEach() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should have been created");
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> provideValidJsonWithCustomizer() {
        return Stream.of(Arguments.of("{\"name\":\"John\",\"age\":30}", true));
    }

    /**
     * Walk the schema with {@code validate = false}. The method should still return a
     * {@link ValidationResult} but without performing any validation – i.e. the
     * result must contain no validation messages.
     */
    @Test
    void walkWithoutValidationReturnsEmptyResult() {
        String validJson = "{\"name\":\"John\"}";
        ValidationResult result = schema.walk(validJson, InputFormat.JSON, false, (Consumer<ExecutionContext>) executionContext -> {
            assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
        });
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertTrue(messages.isEmpty(), "When validation is disabled there should be no messages");
    }

    /**
     * Walk the schema with {@code validate = true} on an invalid instance.
     * The supplied {@link java.util.function.Consumer} must be invoked and the
     * resulting {@link ValidationResult} must contain the expected validation
     * messages.
     */
    @Test
    void walkWithValidationAndCustomizerDetectsErrors() {
        String invalidJson = "{\"name\":1}";
        AtomicBoolean customiserInvoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true, (Consumer<ExecutionContext>) executionContext -> {
            assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
            customiserInvoked.set(true);
        });
        assertTrue(customiserInvoked.get(), "The execution customiser should have been invoked");
        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(), "Invalid input should produce validation messages");
        boolean mentionsName = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("name"));
        assertTrue(mentionsName, "At least one validation message should refer to the 'name' property");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void walkValidJsonWithCustomizerValidationEnabled() {
//         String json = "{\"name\":\"John\",\"age\":30}";
//         AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
//         ValidationResult result = schema.walk(json, InputFormat.JSON, true, (Consumer<ExecutionContext>) ctx -> {
//             ctx.setAttribute("custom", Boolean.TRUE);
//             ctxRef.set(ctx);
//         });
//         assertNotNull(result, "Result must not be null");
//         assertTrue(result.getValidationMessages().isEmpty(), "Result should be valid for correct JSON");
//         assertNotNull(ctxRef.get(), "Consumer must have received a non‑null ExecutionContext");
//         assertEquals(Boolean.TRUE, ctxRef.get().getAttribute("custom"), "Custom attribute should be set by the consumer");
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonValidationDisabledNoCustomizer() {
        String json = "{\"name\":\"Jane\"}";
        ValidationResult result = schema.walk(json, InputFormat.JSON, false, (Consumer<ExecutionContext>) null);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(), "Result should be valid when validation is disabled");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkMalformedJsonThrowsException() {
        String malformed = "{\"name\":\"Bob\", \"age\":}";
        assertThrows(RuntimeException.class, () -> schema.walk(malformed, InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.walk(null, InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputFormatThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> schema.walk("{\"name\":\"Alice\"}", null, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void walkUnsupportedUrlEncodedThrowsException() {
//         // %E0%A4%A is an illegal percent‑encoding sequence
//         String badEncoded = "%E0%A4%A";
//         assertThrows(RuntimeException.class, () -> schema.walk(badEncoded, InputFormat.URL_ENCODED, true, (Consumer<ExecutionContext>) null));
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void walkConsumerThrowsRuntimeExceptionPropagates() {
        String json = "{\"id\":1}";
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> schema.walk(json, InputFormat.JSON, true, (Consumer<ExecutionContext>) ctx -> {
            throw new RuntimeException("customizer failure");
        }));
        assertEquals("customizer failure", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkValidYamlNoCustomizer() {
        String yaml = "name: John\nage: 30";
        ValidationResult result = schema.walk(yaml, InputFormat.YAML, true, (Consumer<ExecutionContext>) null);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(), "YAML payload should be valid");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void walkValidJsonValidationDisabledWithCustomizer() {
//         String json = "{\"city\":\"Paris\"}";
//         AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
//         ValidationResult result = schema.walk(json, InputFormat.JSON, false, (Consumer<ExecutionContext>) ctx -> {
//             ctx.setAttribute("skip", Boolean.TRUE);
//             ctxRef.set(ctx);
//         });
//         assertNotNull(result);
//         assertTrue(result.getValidationMessages().isEmpty(), "Result must be valid when validation is disabled");
//         assertNotNull(ctxRef.get());
//         assertEquals(Boolean.TRUE, ctxRef.get().getAttribute("skip"));
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void walkEmptyStringThrowsException() {
        assertThrows(RuntimeException.class, () -> schema.walk("", InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walkWhitespaceOnlyThrowsException() {
        assertThrows(RuntimeException.class, () -> schema.walk("   \t\n", InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void walkValidUrlEncodedNoCustomizer() {
//         // name=John&age=30 URL‑encoded
//         String encoded = "name%3DJohn%26age%3D30";
//         ValidationResult result = schema.walk(encoded, InputFormat.URL_ENCODED, true, (Consumer<ExecutionContext>) null);
//         assertNotNull(result);
//         assertTrue(result.isSuccess(), "Decoded JSON should be valid");
//         assertTrue(result.getValidationMessages().isEmpty());
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void walkJsonArrayFailsValidation() {
        String jsonArray = "[1, 2, 3]";
        ValidationResult result = schema.walk(jsonArray, InputFormat.JSON, true, (Consumer<ExecutionContext>) null);
        assertNotNull(result);
        assertFalse(result.getValidationMessages().isEmpty(), "At least one validation message expected for type mismatch");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void walkConsumerSetsAttributeValidationEnabled() {
//         String json = "{\"id\":10}";
//         AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
//         ValidationResult result = schema.walk(json, InputFormat.JSON, true, (Consumer<ExecutionContext>) ctx -> {
//             ctx.setAttribute("requestId", "abc-123");
//             ctxRef.set(ctx);
//         });
//         assertNotNull(result);
//         assertTrue(result.getValidationMessages().isEmpty(), "Valid input should produce a valid result");
//         assertNotNull(ctxRef.get());
//         assertEquals("abc-123", ctxRef.get().getAttribute("requestId"));
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void walkNoOpConsumerValidationEnabled() {
        String json = "{\"status\":\"ok\"}";
        AtomicBoolean invoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(json, InputFormat.JSON, true, (Consumer<ExecutionContext>) ctx -> invoked.set(true));
        assertTrue(invoked.get(), "Consumer must be invoked even if it does nothing");
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void walkUnsupportedInputFormatThrowsException() {
//         // Assuming InputFormat enum contains XML – which is not supported by walk()
//         assertThrows(IllegalArgumentException.class, () -> schema.walk("{\"name\":\"Test\"}", InputFormat.XML, true, null));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void walkWhenCreateExecutionContextReturnsNull() {
//         // Sub‑class JsonSchema to override createExecutionContext()
//         JsonSchema broken = new JsonSchema() {
// 
//             @Override
//             public ExecutionContext createExecutionContext() {
//                 // deliberately break contract
//                 return null;
//             }
//         };
//         // The broken instance still needs a schema – we reuse the same node tree
//         // via reflection to set the internal fields (the factory is not easily
//         // configurable for a subclass).  For the purpose of this test we only
//         // need to call the overloaded walk that accepts an ExecutionContext,
//         // which we can invoke via reflection.
//         assertThrows(NullPointerException.class, () -> {
//             // Use the public overload that creates its own ExecutionContext
//             broken.walk("{\"valid\":\"json\"}", InputFormat.JSON, true, null);
//         });
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void walkLiteralNullInputThrowsException() {
        assertThrows(RuntimeException.class, () -> schema.walk("null", InputFormat.JSON, true, (Consumer<ExecutionContext>) null));
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_walk_8_Test ✔
│     ├─ walkNullInputFormatThrowsIllegalArgumentException() ✔
│     ├─ walkWhitespaceOnlyThrowsException() ✔
│     ├─ walkValidYamlNoCustomizer() ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     ├─ walkMalformedJsonThrowsException() ✔
│     ├─ walkNoOpConsumerValidationEnabled() ✘ expected: <true> but was: <false>
│     ├─ walkConsumerThrowsRuntimeExceptionPropagates() ✔
│     ├─ walkWithValidationAndCustomizerDetectsErrors() ✔
│     ├─ walkEmptyStringThrowsException() ✔
│     ├─ walkLiteralNullInputThrowsException() ✔
│     ├─ walkWithoutValidationReturnsEmptyResult() ✔
│     ├─ walkNullInputThrowsIllegalArgumentException() ✔
│     ├─ walkJsonArrayFailsValidation() ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     └─ walkValidJsonValidationDisabledNoCustomizer() ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (4):
  JUnit Jupiter:JsonSchema_walk_8_Test:walkValidYamlNoCustomizer()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_8_Test', methodName = 'walkValidYamlNoCustomizer', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1058)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1222)
       com.networknt.schema.JsonSchema_walk_8_Test.walkValidYamlNoCustomizer(JsonSchema_walk_8_Test.java:134)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_8_Test:walkNoOpConsumerValidationEnabled()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_8_Test', methodName = 'walkNoOpConsumerValidationEnabled', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:31)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)
       com.networknt.schema.JsonSchema_walk_8_Test.walkNoOpConsumerValidationEnabled(JsonSchema_walk_8_Test.java:209)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_walk_8_Test:walkJsonArrayFailsValidation()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_8_Test', methodName = 'walkJsonArrayFailsValidation', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1058)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1222)
       com.networknt.schema.JsonSchema_walk_8_Test.walkJsonArrayFailsValidation(JsonSchema_walk_8_Test.java:181)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_walk_8_Test:walkValidJsonValidationDisabledNoCustomizer()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_8_Test', methodName = 'walkValidJsonValidationDisabledNoCustomizer', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1058)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1222)
       com.networknt.schema.JsonSchema_walk_8_Test.walkValidJsonValidationDisabledNoCustomizer(JsonSchema_walk_8_Test.java:88)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 378 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        13 tests found           ]
[         0 tests skipped         ]
[        13 tests started         ]
[         0 tests aborted         ]
[         9 tests successful      ]
[         4 tests failed          ]


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
 

api document of method walk(String, InputFormat, boolean, Consumer<ExecutionContext>): 

     * Walk the input.
     * 
     * @param input       the input
     * @param inputFormat the input format
     * @param validate    true to validate the input against the schema
     * @param executionCustomizer the customizer
     * @return the validation result
     

return type: 
com.networknt.schema.ValidationResult

dependent classes: 
class `com.networknt.schema.InputFormat`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, Consumer<ExecutionContext>)`, api document: 
     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, Consumer<ExecutionContext>)`, api document: 
     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param rootNode            the root node
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;
		method `public ValidationResult walk(String, InputFormat, boolean, Consumer<ExecutionContext>)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, Consumer<ExecutionContext>)`, api document: 
     * Walk the input.
     * 
     * @param input       the input
     * @param inputFormat the input format
     * @param validate    true to validate the input against the schema
     * @param executionCustomizer the customizer
     * @return the validation result
     ;
		method `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(String, InputFormat, boolean, Consumer<ExecutionContext>)`, api document: 
     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>                 the result type
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param format              the formatter
     * @param executionCustomizer the execution customizer
     * @return the result
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
