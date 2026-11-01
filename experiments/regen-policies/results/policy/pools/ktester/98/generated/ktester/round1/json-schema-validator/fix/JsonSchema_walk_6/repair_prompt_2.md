


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_walk_6_Test {

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

    private JsonSchema jsonSchema;

    private ObjectMapper mapper;

    @BeforeEach
    void setupBeforeEach() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema(SIMPLE_SCHEMA);
        mapper = new ObjectMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
        mapper = null;
    }

    /**
     * Verify that {@code walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)}
     * correctly validates a valid JSON instance when {@code validate} is {@code true}.
     */
    @Test
    void walk_WithValidNodeAndValidateTrue_ShouldReturnEmptyValidationResult() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"John Doe\"}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, noOpCustomizer);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(), "Expected no validation messages for a valid instance");
    }

    /**
     * Verify that the same method returns a {@link ValidationResult} containing
     * messages when validation is requested and the instance is invalid.
     */
    @Test
    void walk_WithInvalidNodeAndValidateTrue_ShouldReturnValidationMessages() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, invalidNode, true, noOpCustomizer);
        assertNotNull(result, "Result must not be null");
        assertFalse(result.getValidationMessages().isEmpty(), "Expected validation messages for an invalid instance");
    }

    /**
     * Verify that when {@code validate} is {@code false} the walk operation still
     * returns a {@link ValidationResult} but does not perform validation, i.e.
     * the message set is empty regardless of the node content.
     */
    @Test
    void walk_WithValidateFalse_ShouldSkipValidation() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode anyNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, anyNode, false, noOpCustomizer);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(), "Validation messages should be empty when validation is disabled");
    }

    @Test
    void walk_WithExecutionCustomizer_ShouldInjectAttributeBeforeValidation() throws Exception {
        // given
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"Alice\"}");
        // The ExecutionContext implementation used in this project does not expose
        // a public setAttribute/getAttribute API.  We therefore manipulate the
        // internal attribute map via reflection.
        Consumer<ExecutionContext> customizer = ctx -> {
            try {
                var field = ctx.getClass().getDeclaredField("attributes");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) field.get(ctx);
                map.put("custom", "value");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        // when
        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, customizer);
        // then
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(), "Expected no validation errors");
        // Verify that the attribute set by the customizer is present.
        Object customValue;
        try {
            var field = executionContext.getClass().getDeclaredField("attributes");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) field.get(executionContext);
            customValue = map.get("custom");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals("value", customValue, "The execution customizer should have stored the attribute \"custom\"=\"value\"");
    }

    @Test
    void walk_NullNode_ShouldReturnValidationMessageIndicatingMissingInstance() {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, null, true, noOpCustomizer);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(), "Expected at least one validation message for null node");
    }

    @Test
    void walk_NullExecutionContext_ShouldThrowNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(null, node, true, noOpCustomizer), "Passing a null ExecutionContext must result in NPE");
    }

    @Test
    void walk_NullExecutionContextWithCustomizer_ShouldThrowNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        Consumer<ExecutionContext> customizer = ctx -> {
            /* no‑op */
        };
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(null, node, true, customizer), "Null ExecutionContext with a customizer must result in NPE");
    }

    @Test
    void walk_ExecutionCustomizerThrowsException_ShouldPropagateRuntimeException() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        Consumer<ExecutionContext> explodingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(executionContext, node, true, explodingCustomizer), "Exception from customizer must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void walk_WithSchemaReferenceResolution_ShouldValidateSuccessfully() throws Exception {
        String schemaWithRef = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/root.json",
              "$ref": "#/definitions/refSchema",
              "definitions": {
                "refSchema": {
                  "type": "object",
                  "properties": {
                    "refProp": { "type": "string" }
                  },
                  "required": ["refProp"]
                }
              }
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema refSchema = factory.getSchema(schemaWithRef);
        JsonNode instance = mapper.readTree("{\"refProp\":\"value\"}");
        ExecutionContext ctx = refSchema.createExecutionContext();
        ValidationResult result = refSchema.walk(ctx, instance, true, ec -> {
            /* no‑op */
        });
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(), "Instance satisfies the referenced subschema, so no errors are expected");
    }

    @Test
    void walk_WithRecursiveAnchor_ShouldValidateWithoutInfiniteLoop() throws Exception {
        String recursiveSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/recursive.json",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "self": { "$recursiveRef": "#" }
              }
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(recursiveSchema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, instance, true, ec -> {
            /* no‑op */
        });
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(), "Recursive anchor should be handled gracefully without errors");
    }

    @Test
    void walk_MissingRequiredProperty_ShouldReturnValidationMessage() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode node = mapper.readTree("{\"age\":30}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> {
            /* no‑op */
        };
        ValidationResult result = jsonSchema.walk(executionContext, node, true, noOpCustomizer);
        assertNotNull(result);
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertFalse(messages.isEmpty(), "Expected a validation message for missing required property");
        boolean mentionsName = messages.stream().anyMatch(m -> m.getMessage().contains("name"));
        assertTrue(mentionsName, "Validation message should mention the missing 'name' property");
    }

    @Test
    void walk_WithExecutionCustomizer_ShouldBeInvoked() throws Exception {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"Alice\"}");
        AtomicBoolean invoked = new AtomicBoolean(false);
        Consumer<ExecutionContext> customizer = ctx -> invoked.set(true);
        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, customizer);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty(), "Expected no validation errors");
        assertTrue(invoked.get(), "Customizer should have been invoked");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

19:10:06.808 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword $recursiveAnchor - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
19:10:06.810 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword $recursiveRef - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_walk_6_Test ✔
│     ├─ walk_WithRecursiveAnchor_ShouldValidateWithoutInfiniteLoop() ✔
│     ├─ walk_ExecutionCustomizerThrowsException_ShouldPropagateRuntimeException() ✔
│     ├─ walk_MissingRequiredProperty_ShouldReturnValidationMessage() ✔
│     ├─ walk_NullExecutionContextWithCustomizer_ShouldThrowNullPointerException() ✔
│     ├─ walk_WithValidNodeAndValidateTrue_ShouldReturnEmptyValidationResult() ✔
│     ├─ walk_NullExecutionContext_ShouldThrowNullPointerException() ✔
│     ├─ walk_WithValidateFalse_ShouldSkipValidation() ✔
│     ├─ walk_WithExecutionCustomizer_ShouldInjectAttributeBeforeValidation() ✘ java.lang.NoSuchFieldException: attributes
│     ├─ walk_WithInvalidNodeAndValidateTrue_ShouldReturnValidationMessages() ✔
│     ├─ walk_WithExecutionCustomizer_ShouldBeInvoked() ✔
│     ├─ walk_NullNode_ShouldReturnValidationMessageIndicatingMissingInstance() ✘ Expected at least one validation message for null node ==> expected: <false> but was: <true>
│     └─ walk_WithSchemaReferenceResolution_ShouldValidateSuccessfully() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:JsonSchema_walk_6_Test:walk_WithExecutionCustomizer_ShouldInjectAttributeBeforeValidation()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_6_Test', methodName = 'walk_WithExecutionCustomizer_ShouldInjectAttributeBeforeValidation', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: attributes
       com.networknt.schema.JsonSchema_walk_6_Test.lambda$walk_WithExecutionCustomizer_ShouldInjectAttributeBeforeValidation$3(JsonSchema_walk_6_Test.java:109)
       com.networknt.schema.JsonSchema.lambda$walkAtNodeInternal$4(JsonSchema.java:1245)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1253)
       com.networknt.schema.JsonSchema.walkAtNodeInternal(JsonSchema.java:1243)
       com.networknt.schema.JsonSchema.walk(JsonSchema.java:1058)
       [...]
     Caused by: java.lang.NoSuchFieldException: attributes
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_walk_6_Test.lambda$walk_WithExecutionCustomizer_ShouldInjectAttributeBeforeValidation$3(JsonSchema_walk_6_Test.java:103)
       [...]
  JUnit Jupiter:JsonSchema_walk_6_Test:walk_NullNode_ShouldReturnValidationMessageIndicatingMissingInstance()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_6_Test', methodName = 'walk_NullNode_ShouldReturnValidationMessageIndicatingMissingInstance', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected at least one validation message for null node ==> expected: <false> but was: <true>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertFalse.failNotFalse(AssertFalse.java:63)
       org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:36)
       org.junit.jupiter.api.Assertions.assertFalse(Assertions.java:239)
       com.networknt.schema.JsonSchema_walk_6_Test.walk_NullNode_ShouldReturnValidationMessageIndicatingMissingInstance(JsonSchema_walk_6_Test.java:140)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 421 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        12 tests found           ]
[         0 tests skipped         ]
[        12 tests started         ]
[         0 tests aborted         ]
[        10 tests successful      ]
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
 

api document of method walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>): 

     * Walk the JSON node.
     * 
     * @param executionContext the execution context
     * @param node             the input
     * @param validate         true to validate the input against the schema
     * @param executionCustomizer the customizer
     *
     * @return the validation result
     

return type: 
com.networknt.schema.ValidationResult

dependent classes: 
class `com.networknt.schema.ExecutionContext`:
	
class `com.networknt.schema.OutputFormat`:
	dependent fields:
		com.networknt.schema.OutputFormat.Result com.networknt.schema.OutputFormat.RESULT;
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public ValidationResult walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)`, api document: 
     * Walk the JSON node.
     * 
     * @param executionContext the execution context
     * @param node             the input
     * @param validate         true to validate the input against the schema
     * @param executionCustomizer the customizer
     *
     * @return the validation result
     ;
		method `public T walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)`, api document: 
     * Walk the input.
     * 
     * @param <T>         the result type
     * @param executionContext the execution context
     * @param input            the input
     * @param inputFormat      the input format
     * @param outputFormat     the output format
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
		method `public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)`, api document: 
     * Walk at the node.
     * 
     * @param executionContext the execution content
     * @param node             the current node
     * @param rootNode         the root node
     * @param instanceLocation the instance location
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
		method `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)`, api document: 
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
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
