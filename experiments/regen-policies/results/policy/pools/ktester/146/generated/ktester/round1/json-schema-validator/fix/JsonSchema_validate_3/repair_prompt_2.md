


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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import com.networknt.schema.i18n.MessageSource;
import java.util.Locale;
import java.util.function.Supplier;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_validate_3_Test {

    /**
     * Shared Jackson mapper used to create {@link JsonNode} instances.
     */
    private static ObjectMapper mapper;

    /**
     * The factory used to create {@link JsonSchema} instances.
     */
    private JsonSchemaFactory factory;

    /**
     * The schema under test – a very small schema that requires a {@code name}
     *  property of type {@code string}.
     */
    private JsonSchema schema;

    /**
     * The default schema used by the original tests (requires a string {@code name}).
     */
    private JsonSchema defaultSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the Jackson ObjectMapper once for the whole test class.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a factory for the 2020‑12 draft (the most recent stable draft in the
        // library). No special configuration is required for these tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Simple schema: an object with a required string property "name".
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;
        // Create the JsonSchema instance that will be exercised.
        schema = factory.getSchema(schemaJson);
        // The original tests also refer to a "defaultSchema" – initialise it with the same definition.
        defaultSchema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly required but keeps the test tidy.
        schema = null;
        defaultSchema = null;
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
        mapper = null;
    }

    /**
     * Verify that the {@code Consumer<ExecutionContext>} passed to {@code validate}
     * is invoked exactly once and that validation works as expected.
     */
    @Test
    void validate_withConsumer_executesConsumerAndDetectsErrors() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN – a JSON instance that violates the schema (name is a number)
        // --------------------------------------------------------------------
        String invalidJson = """
            {
              "name": 123
            }
            """;
        JsonNode invalidNode = mapper.readTree(invalidJson);
        // A flag that the consumer will toggle when it is called.
        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        // Consumer that simply records that it has been executed.
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerCalled.set(true);
        // --------------------------------------------------------------------
        // WHEN – the validation method is invoked
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = schema.validate(invalidNode, executionCustomizer);
        // --------------------------------------------------------------------
        // THEN – the consumer must have been called and a validation error must be present
        // --------------------------------------------------------------------
        assertTrue(consumerCalled.get(), "The execution customizer supplied to validate(...) should have been invoked.");
        // The schema requires a string for 'name', so we expect exactly one error.
        assertNotNull(messages, "The returned message set must not be null.");
        assertEquals(1, messages.size(), "For the provided invalid instance a single validation message is expected.");
        // Optional sanity check: the message should contain the property name.
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().contains("name"), "The validation message should reference the offending property 'name'.");
    }

    /**
     * Verify that a valid JSON instance produces an empty message set while the
     * consumer is still executed.
     */
    @Test
    void validate_withConsumer_andValidInstance_returnsEmptySet() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN – a JSON instance that satisfies the schema
        // --------------------------------------------------------------------
        String validJson = """
            {
              "name": "John Doe"
            }
            """;
        JsonNode validNode = mapper.readTree(validJson);
        AtomicBoolean consumerCalled = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerCalled.set(true);
        // --------------------------------------------------------------------
        // WHEN
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = schema.validate(validNode, executionCustomizer);
        // --------------------------------------------------------------------
        // THEN
        // --------------------------------------------------------------------
        assertTrue(consumerCalled.get(), "The execution customizer must be invoked even when validation succeeds.");
        assertNotNull(messages, "The returned message set must not be null.");
        assertTrue(messages.isEmpty(), "A valid instance should result in an empty set of validation messages.");
    }

    @Test
    void validate_nullRootNode_throwsIllegalArgumentException() {
        Consumer<ExecutionContext> customizer = ctx -> {
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> defaultSchema.validate((JsonNode) null, customizer), "Calling validate with a null rootNode must throw IllegalArgumentException");
        assertTrue(ex.getMessage().contains("rootNode"), "Exception message should mention 'rootNode'");
    }

    @Test
    void validate_nullConsumer_behavesAsNoOpCustomizer() throws Exception {
        // Use the default schema – the instance satisfies it.
        String json = """
            { "name": "John" }""";
        JsonNode node = mapper.readTree(json);
        // Passing null for the consumer should be tolerated.
        Set<ValidationMessage> messages = defaultSchema.validate(node, (Consumer<ExecutionContext>) null);
        assertNotNull(messages, "Result set must not be null");
        assertTrue(messages.isEmpty(), "Valid instance should yield an empty message set");
    }

    @Test
    void validate_consumerDisablesFailFast_validInstance_returnsEmptySet() throws Exception {
        String json = """
            { "name": "John" }""";
        JsonNode node = mapper.readTree(json);
        Consumer<ExecutionContext> customizer = ctx -> ctx.setFailFast(false);
        Set<ValidationMessage> messages = defaultSchema.validate(node, customizer);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Even with fail‑fast disabled, a valid instance must produce no messages");
    }

    @Test
    void validate_consumerEnablesFailFast_missingRequired_returnsSingleMessage() throws Exception {
        // Empty object – violates the required "name" property.
        JsonNode node = mapper.readTree("{}");
        Consumer<ExecutionContext> customizer = ctx -> ctx.setFailFast(true);
        Set<ValidationMessage> messages = defaultSchema.validate(node, customizer);
        assertNotNull(messages);
        // Fail‑fast should stop after the first error → exactly one message.
        assertEquals(1, messages.size(), "With fail‑fast enabled the validator should stop after the first error");
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("name"), "The message should refer to the missing required property 'name'");
    }

    @Test
    void validate_consumerThrowsException_propagatesRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("""
            { "name": "John" }""");
        Consumer<ExecutionContext> exploding = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> defaultSchema.validate(node, exploding), "Exception thrown by the consumer must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_multipleIndependentErrors_returnsAtLeastTwoMessages() throws Exception {
        // Schema that requires "name" (string) and "age" (minimum 0)
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer", "minimum": 0 }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // Instance violates both rules: missing name and age = -5
        JsonNode node = mapper.readTree("""
            { "age": -5 }""");
        Set<ValidationMessage> messages = schema.validate(node, ctx -> {
        });
        assertNotNull(messages);
        assertTrue(messages.size() >= 2, "Both the missing required property and the minimum violation should be reported");
        boolean hasName = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("name"));
        boolean hasMinimum = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("minimum"));
        assertTrue(hasName, "Expected a message about the missing required property 'name'");
        assertTrue(hasMinimum, "Expected a message about the 'minimum' constraint on 'age'");
    }

    @Test
    void validate_schemaWithRecursiveAnchor_resolvesWithoutInfiniteLoop() throws Exception {
        // Schema with a recursive anchor and a $ref to itself.
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "self": { "$ref": "#" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // Instance that references itself – the validator should terminate safely.
        JsonNode node = mapper.readTree("""
            { "self": { "$ref": "#" } }""");
        Set<ValidationMessage> messages = schema.validate(node, ctx -> {
        });
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Recursive anchor should be handled correctly and produce no validation errors");
    }

    @Test
    void validate_failFastTrueWithValidInstance_returnsEmptySet() throws Exception {
        // Same schema as default (requires string name). Add an optional "age".
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" },
                "age": { "type": "integer", "minimum": 0 }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "name": "Alice", "age": 30 }""");
        Consumer<ExecutionContext> customizer = ctx -> ctx.setFailFast(true);
        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "When the instance is valid, fail‑fast should not produce any messages");
    }

//     @Test
//     void validate_customMessageSource_prefixesAllMessages() throws Exception {
//         // Schema with required name and minimum on age.
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "object",
//               "required": ["name"],
//               "properties": {
//                 "name": { "type": "string" },
//                 "age": { "type": "integer", "minimum": 0 }
//               }
//             }
//             """;
//         JsonSchema schema = factory.getSchema(schemaJson);
//         JsonNode node = mapper.readTree("""
//             { "age": -1 }""");
//         MessageSource frenchSource = new MessageSource() {
// 
//             public String getMessage(String key, Object... args) {
//                 return "[FR] " + key;
//             }
// 
//             public String getMessage(String key, Supplier<String> defaultMessageSupplier, Locale locale, Object... args) {
//                 return "[FR] " + key;
//             }
//         };
//         Consumer<ExecutionContext> customizer = ctx -> ctx.getExecutionConfig().setMessageSource(frenchSource);
//         Set<ValidationMessage> messages = schema.validate(node, customizer);
//         assertNotNull(messages);
//         assertTrue(messages.size() >= 2, "Both required and minimum violations should be present");
//         for (ValidationMessage m : messages) {
//             assertTrue(m.getMessage().startsWith("[FR] "), "All messages must start with the custom prefix");
//         }
//     }

//     @Test
//     void validate_pathTypeJsonPointer_instanceLocationUsesJsonPointerNotation() throws Exception {
//         // Schema where "zip" must match a numeric pattern.
//         String schemaJson = """
//             {
//               "$schema": "https://json-schema.org/draft/2020-12/schema",
//               "type": "object",
//               "properties": {
//                 "address": {
//                   "type": "object",
//                   "properties": {
//                     "zip": { "type": "string", "pattern": "^[0-9]+$" }
//                   },
//                   "required": ["zip"]
//                 }
//               },
//               "required": ["address"]
//             }
//             """;
//         JsonSchema schema = factory.getSchema(schemaJson);
//         JsonNode node = mapper.readTree("""
//             { "address": { "zip": "123A" } }""");
//         Consumer<ExecutionContext> customizer = ctx -> ctx.getExecutionConfig().setPathType(PathType.JSON_POINTER);
//         Set<ValidationMessage> messages = schema.validate(node, customizer);
//         assertNotNull(messages);
//         assertFalse(messages.isEmpty(), "Invalid zip should generate a validation message");
//         // At least one message must have an instance location that starts with "/address/zip"
//         boolean found = messages.stream().anyMatch(m -> {
//             String loc = m.getInstanceLocation().toString();
//             return loc != null && loc.startsWith("/address/zip");
//         });
//         assertTrue(found, "Instance location should be expressed in JSON‑Pointer format");
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

18:02:33.930 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword $recursiveAnchor - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_validate_3_Test ✔
│     ├─ validate_failFastTrueWithValidInstance_returnsEmptySet() ✔
│     ├─ validate_withConsumer_andValidInstance_returnsEmptySet() ✔
│     ├─ validate_schemaWithRecursiveAnchor_resolvesWithoutInfiniteLoop() ✔
│     ├─ validate_multipleIndependentErrors_returnsAtLeastTwoMessages() ✔
│     ├─ validate_consumerDisablesFailFast_validInstance_returnsEmptySet() ✔
│     ├─ validate_consumerThrowsException_propagatesRuntimeException() ✔
│     ├─ validate_nullRootNode_throwsIllegalArgumentException() ✘ Calling validate with a null rootNode must throw IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     ├─ validate_withConsumer_executesConsumerAndDetectsErrors() ✔
│     ├─ validate_nullConsumer_behavesAsNoOpCustomizer() ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     └─ validate_consumerEnablesFailFast_missingRequired_returnsSingleMessage() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:JsonSchema_validate_3_Test:validate_nullRootNode_throwsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test', methodName = 'validate_nullRootNode_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Calling validate with a null rootNode must throw IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3153)
       com.networknt.schema.JsonSchema_validate_3_Test.validate_nullRootNode_throwsIllegalArgumentException(JsonSchema_validate_3_Test.java:148)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because "node" is null
       com.networknt.schema.PropertiesValidator.validate(PropertiesValidator.java:73)
       com.networknt.schema.PropertiesValidator.validate(PropertiesValidator.java:61)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:616)
       com.networknt.schema.BaseJsonValidator.validate(BaseJsonValidator.java:321)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:924)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:validate_nullConsumer_behavesAsNoOpCustomizer()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test', methodName = 'validate_nullConsumer_behavesAsNoOpCustomizer', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$validate$2(JsonSchema.java:771)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:920)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:771)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:712)
       com.networknt.schema.JsonSchema_validate_3_Test.validate_nullConsumer_behavesAsNoOpCustomizer(JsonSchema_validate_3_Test.java:159)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:767)
       [...]

Test run finished after 398 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        10 tests found           ]
[         0 tests skipped         ]
[        10 tests started         ]
[         0 tests aborted         ]
[         8 tests successful      ]
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
 

api document of method validate(JsonNode, Consumer<ExecutionContext>): 

     * Validate the given root JsonNode, starting at the root of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param rootNode            the root node
     * @param executionCustomizer the execution customizer
     * @return the assertions
     

dependent classes: 
class `com.networknt.schema.OutputFormat`:
	dependent fields:
		com.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> validate(String, InputFormat, Consumer<ExecutionContext>)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, Consumer<ExecutionContext>)`, api document: 
     * Validate the given input string using the input format, starting at the root
     * of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param input               the input
     * @param inputFormat         the inputFormat
     * @param executionCustomizer the execution customizer
     * @return the assertions
     ;
		method `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, Consumer<ExecutionContext>)`, api document: 
     * Validate the given root JsonNode, starting at the root of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param rootNode the root node
     * @return A list of ValidationMessage if there is any validation error, or an
     *         empty list if there is no error.
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, Consumer<ExecutionContext>)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
