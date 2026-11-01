


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
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchema_validate_3_Test {

    private static ObjectMapper mapper;

    private JsonSchema schema;

    /* Null handling                                                          */
    @Nested
    class NullHandling {

        @Test
        void whenBothParametersNull_thenThrowNPE() {
            assertThrows(NullPointerException.class, () -> schema.validate(null, (Consumer<ExecutionContext>) null));
        }

        @Test
        void whenRootNodeNullAndCustomizerPresent_thenThrowNPE() {
            Consumer<ExecutionContext> customizer = ctx -> {
                /* no‑op */
            };
            assertThrows(NullPointerException.class, () -> schema.validate(null, customizer));
        }

        @Test
        void whenCustomizerNullAndRootValid_thenReturnEmptySet() throws Exception {
            // Simple schema that allows any object.
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"any\":\"value\"}");
            Set<ValidationMessage> messages = schema.validate(node, (Consumer<ExecutionContext>) null);
            assertTrue(messages.isEmpty(), "A null customizer should behave like a no‑op");
        }
    }

    /* Execution customizer effects                                            */
    @Nested
    class ExecutionCustomizerEffects {

        @Test
        void whenCustomizerThrowsIllegalStateException_thenPropagate() throws Exception {
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"a\":1}");
            Consumer<ExecutionContext> badCustomizer = ctx -> {
                throw new IllegalStateException("customizer failure");
            };
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(node, badCustomizer));
            assertEquals("customizer failure", ex.getMessage());
        }

        @Test
        void whenCustomizerThrowsRuntimeException_thenPropagate() throws Exception {
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"a\":1}");
            Consumer<ExecutionContext> badCustomizer = ctx -> {
                throw new RuntimeException("unexpected");
            };
            RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, badCustomizer));
            assertEquals("unexpected", ex.getMessage());
        }

        @Test
        void whenCustomizerThrowsCheckedExceptionWrappedInRuntime_thenPropagate() throws Exception {
            String simpleSchema = "{ \"type\": \"object\" }";
            schema = createSchema(simpleSchema);
            JsonNode node = mapper.readTree("{\"a\":1}");
            Consumer<ExecutionContext> customizer = ctx -> {
                try {
                    throw new java.io.IOException("io fail");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, customizer));
            assertTrue(ex.getCause() instanceof java.io.IOException);
            assertEquals("io fail", ex.getCause().getMessage());
        }
    }

    /* Successful validation                                                   */
    @Nested
    class SuccessfulValidation {

        @Test
        void whenObjectSatisfiesSchema_thenEmptySet() throws Exception {
            String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": { \"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"} },\n" + "  \"required\": [\"name\"]\n" + "}";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"name\":\"John\",\"age\":30}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertTrue(msgs.isEmpty(), "Valid object should produce no validation messages");
        }

        @Test
        void whenArrayConformsToSchema_thenEmptySet() throws Exception {
            String schemaJson = "{ \"type\": \"array\", \"items\": { \"type\": \"string\" } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("[\"a\",\"b\",\"c\"]");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertTrue(msgs.isEmpty(), "Valid array should produce no validation messages");
        }
    }

    /* Validation failures                                                    */
    @Nested
    class ValidationFailures {

        @Test
        void whenPropertyHasWrongType_thenTypeMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"age\": {\"type\":\"integer\"} } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"age\":\"thirty\"}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            assertTrue(vm.getMessage().contains("/age"), "Message should reference the offending property");
        }

        @Test
        void whenRequiredPropertyMissing_thenRequiredMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": {\"type\":\"string\"} }, \"required\": [\"name\"] }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("required", vm.getType());
            // For a missing required property the message mentions the property name
            assertTrue(vm.getMessage().contains("required"), "Message should indicate a required‑property problem");
        }

        @Test
        void whenAdditionalPropertiesNotAllowed_thenAdditionalPropertiesMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"additionalProperties\": false }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"extra\":true}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("additionalProperties", vm.getType());
            assertTrue(vm.getMessage().contains("additionalProperties"), "Message should mention additionalProperties");
        }

        @Test
        void whenRootNodeTypeMismatch_thenTypeMessageAtRoot() throws Exception {
            String schemaJson = "{ \"type\": \"object\" }";
            schema = createSchema(schemaJson);
            JsonNode arrayNode = mapper.readTree("[1,2,3]");
            Set<ValidationMessage> msgs = schema.validate(arrayNode, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            assertTrue(vm.getMessage().toLowerCase().contains("type"), "Root‑type mismatch should be reported");
        }

        @Test
        void whenRootNodePrimitiveStringMismatch_thenTypeMessageAtRoot() throws Exception {
            String schemaJson = "{ \"type\": \"object\" }";
            schema = createSchema(schemaJson);
            JsonNode stringNode = mapper.readTree("\"just a string\"");
            Set<ValidationMessage> msgs = schema.validate(stringNode, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            assertTrue(vm.getMessage().toLowerCase().contains("type"));
        }

        @Test
        void whenPatternDoesNotMatch_thenFormatMessage() throws Exception {
            // Using the built‑in "format":"email" validator.
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"email\": { \"type\": \"string\", \"format\": \"email\" } } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"email\":\"not-an-email\"}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("format", vm.getType());
            assertTrue(vm.getMessage().toLowerCase().contains("email"));
        }

        @Test
        void whenEnumViolation_thenEnumMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"status\": { \"type\": \"string\", \"enum\": [\"ACTIVE\",\"INACTIVE\"] } } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"status\":\"UNKNOWN\"}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("enum", vm.getType());
            assertTrue(vm.getMessage().toLowerCase().contains("enum"));
        }

        @Test
        void whenOneOfNotSatisfied_thenOneOfMessage() throws Exception {
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"value\": { \"oneOf\": [ {\"type\":\"integer\"}, {\"type\":\"string\"} ] } } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"value\":true}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("oneOf", vm.getType());
            assertTrue(vm.getMessage().toLowerCase().contains("oneof"));
        }

        @Test
        void whenArrayItemHasWrongType_thenItemTypeMessage() throws Exception {
            String schemaJson = "{ \"type\": \"array\", \"items\": { \"type\": \"string\" } }";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("[\"a\",2,\"c\"]");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertEquals(1, msgs.size());
            ValidationMessage vm = msgs.iterator().next();
            assertEquals("type", vm.getType());
            // The message should indicate the offending index.
            assertTrue(vm.getMessage().contains("/1"));
        }

        @Test
        void whenDepthExceedsMaximum_thenMaxDepthMessage() throws Exception {
            // The library’s default max depth is 20; we create a deeper structure.
            String schemaJson = "{ \"type\": \"object\", \"properties\": { \"level1\": { \"$ref\": \"#\" } } }";
            schema = createSchema(schemaJson);
            // Build a deep JSON object (depth 25).
            StringBuilder sb = new StringBuilder("{");
            for (int i = 1; i <= 25; i++) {
                sb.append("\"level").append(i).append("\":{");
            }
            sb.append("\"leaf\":{}");
            for (int i = 1; i <= 25; i++) {
                sb.append("}");
            }
            sb.append("}");
            JsonNode deepNode = mapper.readTree(sb.toString());
            Set<ValidationMessage> msgs = schema.validate(deepNode, ctx -> {
            });
            assertFalse(msgs.isEmpty(), "Expected at least one validation message for maxDepth");
            boolean hasMaxDepth = msgs.stream().anyMatch(m -> "maxDepth".equals(m.getType()));
            assertTrue(hasMaxDepth, "One of the messages should be of type 'maxDepth'");
        }
    }

    /* Recursive anchor handling                                              */
    @Nested
    class RecursiveAnchorHandling {

        @Test
        void whenRecursiveAnchorResolvesCorrectly_thenEmptySet() throws Exception {
            String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"$recursiveAnchor\": true,\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"self\": { \"$recursiveRef\": \"#\" }\n" + "  }\n" + "}";
            schema = createSchema(schemaJson);
            JsonNode node = mapper.readTree("{\"self\":{}}");
            Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
            });
            assertTrue(msgs.isEmpty(), "Recursive anchor with simple self‑reference should validate successfully");
        }

        @Test
        void whenRecursiveAnchorExceedsDepth_thenMaxDepthMessage() throws Exception {
            String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"$recursiveAnchor\": true,\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"self\": { \"$recursiveRef\": \"#\" }\n" + "  }\n" + "}";
            schema = createSchema(schemaJson);
            // Build a deeply nested self‑reference (depth 30)
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < 30; i++) {
                sb.append("\"self\":{");
            }
            sb.append("\"leaf\":{}}");
            for (int i = 0; i < 30; i++) {
                sb.append("}");
            }
            JsonNode deepNode = mapper.readTree(sb.toString());
            Set<ValidationMessage> msgs = schema.validate(deepNode, ctx -> {
            });
            assertFalse(msgs.isEmpty(), "Deep recursion should trigger a maxDepth validation message");
            boolean hasMaxDepth = msgs.stream().anyMatch(m -> "maxDepth".equals(m.getType()));
            assertTrue(hasMaxDepth, "Expected a 'maxDepth' message");
        }
    }

    /* Invalid schema definitions                                             */
    @Nested
    class InvalidSchemaDefinitions {

        @Test
        void whenSchemaHasUnknownType_thenIllegalArgumentException() {
            String schemaJson = "{ \"type\": \"unknownType\" }";
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> createSchema(schemaJson));
            assertTrue(ex.getMessage().toLowerCase().contains("unknown"));
        }

        @Test
        void whenSchemaHasUnsupportedKeyword_thenUnsupportedOperationException() {
            String schemaJson = "{ \"type\": \"object\", \"fooValidator\": true }";
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> createSchema(schemaJson));
            assertTrue(ex.getMessage().toLowerCase().contains("foovalidator"));
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper is thread‑safe; initialise once for the whole test class.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Simple schema that requires a string property "name".
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        this.schema = factory.getSchema(schemaData);
    }

    @AfterEach
    void teardownAfterEach() {
        this.schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        mapper = null;
    }

    /**
     * Helper to build a {@link JsonSchema} from a JSON string.
     */
    private static JsonSchema createSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    private static Stream<Arguments> provideNullCustomizerValidObject() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode valid = om.readTree("{\"name\":\"Alice\",\"age\":25}");
        return Stream.of(Arguments.of(valid, null, "Null customizer with a valid object should produce empty set"));
    }

    @Test
    void validate_WithConsumerCustomizer_ReturnsExpectedMessages() throws Exception {
        // JSON instance where "name" is an integer – violates the schema.
        String jsonData = "{ \"name\": 1 }";
        JsonNode rootNode = mapper.readTree(jsonData);
        // A no‑op customizer; we only need to exercise the overload.
        Consumer<ExecutionContext> customizer = ctx -> {
            // No modifications required for this test.
        };
        Set<ValidationMessage> messages = schema.validate(rootNode, customizer);
        // The schema should produce exactly one validation error (type mismatch).
        assertEquals(1, messages.size(), "Expected a single validation error");
        ValidationMessage message = messages.iterator().next();
        // The default message contains the word "type" for a type mismatch.
        assertTrue(message.getMessage().toLowerCase().contains("type"), "Validation message should indicate a type problem");
    }

    /* Schema resource root flag (simple smoke test)                         */
    @Test
    void whenSchemaIsResourceRoot_thenValidatesSuccessfully() throws Exception {
        String schemaJson = "{\n" + "  \"$id\": \"http://example.com/schema\",\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": { \"id\": { \"type\": \"string\", \"format\": \"uri\" } }\n" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"id\":\"http://example.com/item\"}");
        Set<ValidationMessage> msgs = schema.validate(node, ctx -> {
        });
        assertTrue(msgs.isEmpty(), "Schema marked as resource root should validate normally");
    }

    /* Malformed $ref handling                                                */
    @Test
    void whenSchemaContainsMalformedRef_thenUnsupportedEncodingException() throws Exception {
        String schemaJson = "{ \"$ref\": \"%ZZ\" }";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");
        // The exception is thrown during validation when URLDecoder.decode is invoked.
        assertThrows(UnsupportedEncodingException.class, () -> schema.validate(node, ctx -> {
        }));
    }

    /* Infinite recursion without recursive anchor                           */
    @Test
    void whenCircularRefWithoutRecursiveAnchor_thenStackOverflowError()throws Exception {
        String schemaJson = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"definitions\": { \"self\": { \"$ref\": \"#/definitions/self\" } },\n" + "  \"$ref\": \"#/definitions/self\"\n" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");
        assertThrows(StackOverflowError.class, () -> schema.validate(node, ctx -> {
        }));
    }

    /* Parameterized tests for groups that share the same structure           */
    @ParameterizedTest(name = "{index} => root={0}, expected={2}")
    @MethodSource("provideNullCustomizerValidObject")
    void whenNullCustomizerAndValidObject_thenEmptySet(JsonNode rootNode, Consumer<ExecutionContext> customizer, String description) {
        // Schema that requires a non‑null "name" string.
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": {\"type\":\"string\"} }, \"required\": [\"name\"] }";
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(rootNode, customizer);
        assertTrue(msgs.isEmpty(), description);
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

18:31:57.565 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword fooValidator - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_validate_3_Test ✔
│     ├─ whenNullCustomizerAndValidObject_thenEmptySet(JsonNode, Consumer, String) ✔
│     │  └─ 1 => root={"name":"Alice","age":25}, expected=Null customizer with a valid objec... ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│     ├─ validate_WithConsumerCustomizer_ReturnsExpectedMessages() ✘ Validation message should indicate a type problem ==> expected: <true> but was: <false>
│     ├─ whenSchemaIsResourceRoot_thenValidatesSuccessfully() ✔
│     ├─ whenCircularRefWithoutRecursiveAnchor_thenStackOverflowError() ✔
│     ├─ whenSchemaContainsMalformedRef_thenUnsupportedEncodingException() ✘ Unexpected exception type thrown, expected: <java.io.UnsupportedEncodingException> but was: <java.lang.IllegalArgumentException>
│     ├─ InvalidSchemaDefinitions ✔
│     │  ├─ whenSchemaHasUnknownType_thenIllegalArgumentException() ✘ Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     │  └─ whenSchemaHasUnsupportedKeyword_thenUnsupportedOperationException() ✘ Expected java.lang.UnsupportedOperationException to be thrown, but nothing was thrown.
│     ├─ RecursiveAnchorHandling ✔
│     │  ├─ whenRecursiveAnchorExceedsDepth_thenMaxDepthMessage() ✘ Deep recursion should trigger a maxDepth validation message ==> expected: <false> but was: <true>
│     │  └─ whenRecursiveAnchorResolvesCorrectly_thenEmptySet() ✔
│     ├─ ValidationFailures ✔
│     │  ├─ whenAdditionalPropertiesNotAllowed_thenAdditionalPropertiesMessage() ✘ Message should mention additionalProperties ==> expected: <true> but was: <false>
│     │  ├─ whenRootNodeTypeMismatch_thenTypeMessageAtRoot() ✘ Root‑type mismatch should be reported ==> expected: <true> but was: <false>
│     │  ├─ whenDepthExceedsMaximum_thenMaxDepthMessage() ✘ Expected at least one validation message for maxDepth ==> expected: <false> but was: <true>
│     │  ├─ whenEnumViolation_thenEnumMessage() ✔
│     │  ├─ whenPatternDoesNotMatch_thenFormatMessage() ✘ expected: <1> but was: <0>
│     │  ├─ whenOneOfNotSatisfied_thenOneOfMessage() ✘ expected: <1> but was: <3>
│     │  ├─ whenRequiredPropertyMissing_thenRequiredMessage() ✔
│     │  ├─ whenRootNodePrimitiveStringMismatch_thenTypeMessageAtRoot() ✘ expected: <true> but was: <false>
│     │  ├─ whenArrayItemHasWrongType_thenItemTypeMessage() ✘ expected: <true> but was: <false>
│     │  └─ whenPropertyHasWrongType_thenTypeMessage() ✘ Message should reference the offending property ==> expected: <true> but was: <false>
│     ├─ SuccessfulValidation ✔
│     │  ├─ whenArrayConformsToSchema_thenEmptySet() ✔
│     │  └─ whenObjectSatisfiesSchema_thenEmptySet() ✔
│     ├─ ExecutionCustomizerEffects ✔
│     │  ├─ whenCustomizerThrowsIllegalStateException_thenPropagate() ✔
│     │  ├─ whenCustomizerThrowsRuntimeException_thenPropagate() ✔
│     │  └─ whenCustomizerThrowsCheckedExceptionWrappedInRuntime_thenPropagate() ✔
│     └─ NullHandling ✔
│        ├─ whenCustomizerNullAndRootValid_thenReturnEmptySet() ✘ Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
│        ├─ whenRootNodeNullAndCustomizerPresent_thenThrowNPE() ✔
│        └─ whenBothParametersNull_thenThrowNPE() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (15):
  JUnit Jupiter:JsonSchema_validate_3_Test:whenNullCustomizerAndValidObject_thenEmptySet(JsonNode, Consumer, String):1 => root={"name":"Alice","age":25}, expected=Null customizer with a valid object should produce empty set
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test', methodName = 'whenNullCustomizerAndValidObject_thenEmptySet', methodParameterTypes = 'com.fasterxml.jackson.databind.JsonNode, java.util.function.Consumer, java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$validate$2(JsonSchema.java:775)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:927)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:774)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:715)
       com.networknt.schema.JsonSchema_validate_3_Test.whenNullCustomizerAndValidObject_thenEmptySet(JsonSchema_validate_3_Test.java:418)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:validate_WithConsumerCustomizer_ReturnsExpectedMessages()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test', methodName = 'validate_WithConsumerCustomizer_ReturnsExpectedMessages', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Validation message should indicate a type problem ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_validate_3_Test.validate_WithConsumerCustomizer_ReturnsExpectedMessages(JsonSchema_validate_3_Test.java:376)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:whenSchemaContainsMalformedRef_thenUnsupportedEncodingException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test', methodName = 'whenSchemaContainsMalformedRef_thenUnsupportedEncodingException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.io.UnsupportedEncodingException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_3_Test.whenSchemaContainsMalformedRef_thenUnsupportedEncodingException(JsonSchema_validate_3_Test.java:397)
       [...]
     Caused by: java.lang.IllegalArgumentException: Malformed escape pair at index 0: %ZZ
       java.base/java.net.URI.create(URI.java:906)
       com.networknt.schema.resource.UriSchemaLoader.toURI(UriSchemaLoader.java:52)
       com.networknt.schema.resource.UriSchemaLoader.getSchema(UriSchemaLoader.java:35)
       com.networknt.schema.resource.DefaultSchemaLoader.getSchema(DefaultSchemaLoader.java:65)
       com.networknt.schema.JsonSchemaFactory.getMappedSchema(JsonSchemaFactory.java:670)
       [...]
     Caused by: java.net.URISyntaxException: Malformed escape pair at index 0: %ZZ
       java.base/java.net.URI$Parser.fail(URI.java:2976)
       java.base/java.net.URI$Parser.scanEscape(URI.java:3104)
       java.base/java.net.URI$Parser.scan(URI.java:3127)
       java.base/java.net.URI$Parser.checkChars(URI.java:3145)
       java.base/java.net.URI$Parser.parseHierarchical(URI.java:3229)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:InvalidSchemaDefinitions:whenSchemaHasUnknownType_thenIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$InvalidSchemaDefinitions', methodName = 'whenSchemaHasUnknownType_thenIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_3_Test$InvalidSchemaDefinitions.whenSchemaHasUnknownType_thenIllegalArgumentException(JsonSchema_validate_3_Test.java:312)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:InvalidSchemaDefinitions:whenSchemaHasUnsupportedKeyword_thenUnsupportedOperationException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$InvalidSchemaDefinitions', methodName = 'whenSchemaHasUnsupportedKeyword_thenUnsupportedOperationException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected java.lang.UnsupportedOperationException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3115)
       com.networknt.schema.JsonSchema_validate_3_Test$InvalidSchemaDefinitions.whenSchemaHasUnsupportedKeyword_thenUnsupportedOperationException(JsonSchema_validate_3_Test.java:319)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:RecursiveAnchorHandling:whenRecursiveAnchorExceedsDepth_thenMaxDepthMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$RecursiveAnchorHandling', methodName = 'whenRecursiveAnchorExceedsDepth_thenMaxDepthMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Deep recursion should trigger a maxDepth validation message ==> expected: <false> but was: <true>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertFalse.failNotFalse(AssertFalse.java:63)
       org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:36)
       org.junit.jupiter.api.Assertions.assertFalse(Assertions.java:239)
       com.networknt.schema.JsonSchema_validate_3_Test$RecursiveAnchorHandling.whenRecursiveAnchorExceedsDepth_thenMaxDepthMessage(JsonSchema_validate_3_Test.java:299)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenAdditionalPropertiesNotAllowed_thenAdditionalPropertiesMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenAdditionalPropertiesNotAllowed_thenAdditionalPropertiesMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Message should mention additionalProperties ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenAdditionalPropertiesNotAllowed_thenAdditionalPropertiesMessage(JsonSchema_validate_3_Test.java:162)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenRootNodeTypeMismatch_thenTypeMessageAtRoot()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenRootNodeTypeMismatch_thenTypeMessageAtRoot', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Root‑type mismatch should be reported ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenRootNodeTypeMismatch_thenTypeMessageAtRoot(JsonSchema_validate_3_Test.java:175)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenDepthExceedsMaximum_thenMaxDepthMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenDepthExceedsMaximum_thenMaxDepthMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected at least one validation message for maxDepth ==> expected: <false> but was: <true>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertFalse.failNotFalse(AssertFalse.java:63)
       org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:36)
       org.junit.jupiter.api.Assertions.assertFalse(Assertions.java:239)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenDepthExceedsMaximum_thenMaxDepthMessage(JsonSchema_validate_3_Test.java:263)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenPatternDoesNotMatch_thenFormatMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenPatternDoesNotMatch_thenFormatMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:145)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:531)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenPatternDoesNotMatch_thenFormatMessage(JsonSchema_validate_3_Test.java:199)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenOneOfNotSatisfied_thenOneOfMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenOneOfNotSatisfied_thenOneOfMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <1> but was: <3>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:145)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:531)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenOneOfNotSatisfied_thenOneOfMessage(JsonSchema_validate_3_Test.java:225)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenRootNodePrimitiveStringMismatch_thenTypeMessageAtRoot()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenRootNodePrimitiveStringMismatch_thenTypeMessageAtRoot', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:31)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenRootNodePrimitiveStringMismatch_thenTypeMessageAtRoot(JsonSchema_validate_3_Test.java:188)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenArrayItemHasWrongType_thenItemTypeMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenArrayItemHasWrongType_thenItemTypeMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:31)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:183)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenArrayItemHasWrongType_thenItemTypeMessage(JsonSchema_validate_3_Test.java:242)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:ValidationFailures:whenPropertyHasWrongType_thenTypeMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures', methodName = 'whenPropertyHasWrongType_thenTypeMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Message should reference the offending property ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_validate_3_Test$ValidationFailures.whenPropertyHasWrongType_thenTypeMessage(JsonSchema_validate_3_Test.java:135)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_3_Test:NullHandling:whenCustomizerNullAndRootValid_thenReturnEmptySet()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_3_Test$NullHandling', methodName = 'whenCustomizerNullAndRootValid_thenReturnEmptySet', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "java.util.function.Consumer.accept(Object)" because "executionCustomizer" is null
       com.networknt.schema.JsonSchema.lambda$validate$2(JsonSchema.java:775)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:927)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:774)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:715)
       com.networknt.schema.JsonSchema_validate_3_Test$NullHandling.whenCustomizerNullAndRootValid_thenReturnEmptySet(JsonSchema_validate_3_Test.java:45)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 418 ms
[        11 containers found      ]
[         0 containers skipped    ]
[        11 containers started    ]
[         0 containers aborted    ]
[        11 containers successful ]
[         0 containers failed     ]
[        27 tests found           ]
[         0 tests skipped         ]
[        27 tests started         ]
[         0 tests aborted         ]
[        12 tests successful      ]
[        15 tests failed          ]


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
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, Consumer<ExecutionContext>)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
