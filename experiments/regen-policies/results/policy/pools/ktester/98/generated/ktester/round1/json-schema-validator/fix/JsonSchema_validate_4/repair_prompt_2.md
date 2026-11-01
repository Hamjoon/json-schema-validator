


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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.util.concurrent.atomic.AtomicInteger;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_validate_4_Test {

    private static JsonSchema schema;

    private static ObjectMapper mapper;

    // Basic schema used by the original two tests (object with required string "name")
    private static JsonSchema basicSchema;

    // Numeric schema with inclusive minimum 0 and maximum 100
    private static JsonSchema numericSchema;

    // Enum & pattern schema (status enum, email pattern)
    private static JsonSchema enumPatternSchema;

    // Array schema with enum items and min/max items
    private static JsonSchema arraySchema;

    // $ref schema (person definition, additionalProperties = false)
    private static JsonSchema refSchema;

    // Recursive anchor schema
    private static JsonSchema recursiveSchema;

    // Malformed $ref schema (invalid percent‑encoding)
    private static JsonSchema malformedRefSchema;

    // Unresolvable $ref schema (points to non‑existent definition)
    private static JsonSchema unresolvableRefSchema;

    // Invalid validator type schema (unknown "type")
    private static JsonSchema invalidValidatorSchema;

    /**
     * Simple customizer that records how many times it has been invoked.
     */
    private static class CountingCustomizer implements ExecutionContextCustomizer {

        final AtomicInteger count = new AtomicInteger(0);

        ExecutionContext lastContext;

        @Override
        public void customize(ExecutionContext ctx, ValidationContext validationContext) {
            count.incrementAndGet();
            lastContext = ctx;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        /* ---------- basic schema (already used in the starter test) ---------- */
        String basic = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"name\":{\"type\":\"string\"}}," + "\"required\":[\"name\"]" + "}";
        basicSchema = factory.getSchema(basic);
        /* ---------- numeric schema (min 0, max 100) ---------- */
        String numeric = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"value\":{\"type\":\"number\",\"minimum\":0,\"maximum\":100}}," + "\"required\":[\"value\"]" + "}";
        numericSchema = factory.getSchema(numeric);
        /* ---------- enum & pattern schema ---------- */
        String enumPattern = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "\"status\":{\"type\":\"string\",\"enum\":[\"ACTIVE\",\"INACTIVE\",\"PENDING\"]}," + "\"email\":{\"type\":\"string\",\"pattern\":\"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\"}" + "}," + "\"required\":[\"status\",\"email\"]" + "}";
        enumPatternSchema = factory.getSchema(enumPattern);
        /* ---------- array schema ---------- */
        String array = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "\"tags\":{" + "\"type\":\"array\"," + "\"minItems\":1," + "\"maxItems\":5," + "\"items\":{\"type\":\"string\",\"enum\":[\"red\",\"green\",\"blue\",\"yellow\"]}" + "}" + "}," + "\"required\":[\"tags\"]" + "}";
        arraySchema = factory.getSchema(array);
        /* ---------- $ref schema (person definition) ---------- */
        String ref = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"person\":{\"$ref\":\"#/definitions/Person\"}}," + "\"definitions\":{" + "\"Person\":{" + "\"type\":\"object\"," + "\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}," + "\"required\":[\"name\",\"age\"]," + "\"additionalProperties\":false" + "}" + "}," + "\"required\":[\"person\"]" + "}";
        refSchema = factory.getSchema(ref);
        /* ---------- recursive anchor schema ---------- */
        String recursive = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"$recursiveAnchor\":true," + "\"type\":\"object\"," + "\"properties\":{" + "\"node\":{\"$ref\":\"#\"}," + "\"value\":{\"type\":\"string\"}" + "}," + "\"required\":[\"value\"]" + "}";
        recursiveSchema = factory.getSchema(recursive);
        /* ---------- malformed $ref (invalid percent‑encoding) ---------- */
        String malformedRef = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"bad\":{\"$ref\":\"%ZZ\"}}" + "}";
        malformedRefSchema = factory.getSchema(malformedRef);
        /* ---------- unresolvable $ref ---------- */
        String unresolvableRef = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"unknown\":{\"$ref\":\"#/definitions/Unknown\"}}" + "}";
        unresolvableRefSchema = factory.getSchema(unresolvableRef);
        /* ---------- invalid validator type ---------- */
        String invalidValidator = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"field\":{\"type\":\"unknownType\"}}" + "}";
        invalidValidatorSchema = factory.getSchema(invalidValidator);
    }

    private static Stream<Arguments> numericBoundaryProvider() throws Exception {
        ObjectMapper om = new ObjectMapper();
        return Stream.of(Arguments.of(om.readTree("{\"value\":0}"), true), Arguments.of(om.readTree("{\"value\":100}"), true), Arguments.of(om.readTree("{\"value\":-0.0001}"), false), Arguments.of(om.readTree("{\"value\":100.01}"), false));
    }

//     @Test
//     void validate_validNode_callsCustomizerAndReturnsEmptySet(@Mock ExecutionContextCustomizer customizer) throws Exception {
//         JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
//         CountingCustomizer customizer = new CountingCustomizer();
//         Set<ValidationMessage> messages = basicSchema.validate(validNode, customizer);
//         assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
//         assertEquals(1, customizer.count.get(), "Customizer should be invoked exactly once");
//     }

//     @Test
//     void validate_invalidNode_callsCustomizerAndReturnsMessages(@Mock ExecutionContextCustomizer customizer) throws Exception {
//         JsonNode invalidNode = mapper.readTree("{\"name\":123}");
//         CountingCustomizer customizer = new CountingCustomizer();
//         Set<ValidationMessage> messages = basicSchema.validate(invalidNode, customizer);
//         assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
//         assertEquals(1, customizer.count.get(), "Customizer should be invoked exactly once");
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_nullRootNode_andNullCustomizer_throwsNPE() {
        assertThrows(NullPointerException.class, () -> basicSchema.validate(null, (ExecutionContextCustomizer) null), "Both arguments null – should throw NPE");
    }

//     @Test
//     void validate_nullRootNode_withCustomizer_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
//         CountingCustomizer customizer = new CountingCustomizer();
//         assertThrows(NullPointerException.class, () -> basicSchema.validate(null, customizer), "Null rootNode with a customizer – should throw NPE");
//     }

    @Test
    void validate_nonNullRootNode_withNullCustomizer_throwsNPE() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        assertThrows(NullPointerException.class, () -> basicSchema.validate(node, (ExecutionContextCustomizer) null), "Null customizer with a valid node – should throw NPE");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void validate_customizerThrowsException_propagatesRuntimeException(@Mock ExecutionContextCustomizer customizer) {
//         ExecutionContextCustomizer throwingCustomizer = (ctx, vc) -> {
//             throw new RuntimeException("custom error");
//         };
//         JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "Test");
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> basicSchema.validate(node, throwingCustomizer), "Customizer exception must propagate");
//         assertEquals("custom error", ex.getMessage());
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void validate_localeFrench_returnsFrenchMessage(@Mock ExecutionContextCustomizer customizer) throws Exception {
//         // customizer sets locale to French
//         ExecutionContextCustomizer localeCustomizer = (ctx, vc) -> ctx.setLocale(Locale.FRENCH);
//         String schemaJson = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"age\":{\"type\":\"integer\",\"minimum\":0}}," + "\"required\":[\"age\"]" + "}";
//         JsonSchema ageSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode node = mapper.readTree("{\"age\":-1}");
//         Set<ValidationMessage> messages = ageSchema.validate(node, localeCustomizer);
//         assertFalse(messages.isEmpty(), "Expected validation error for negative age");
//         ValidationMessage msg = messages.iterator().next();
//         assertTrue(msg.getMessage().toLowerCase(Locale.FRENCH).contains("doit"), "Message should be localized to French");
//     }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("numericBoundaryProvider")
    void validate_numericBoundaries(JsonNode node, boolean expectValid) {
        Set<ValidationMessage> messages = numericSchema.validate(node, (ctx, vc) -> {
        });
        if (expectValid) {
            assertTrue(messages.isEmpty(), "Expected no messages for boundary value: " + node);
        } else {
            assertFalse(messages.isEmpty(), "Expected validation error for value: " + node);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_enumAndPattern_validValues() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"ACTIVE\",\"email\":\"john.doe@example.com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Both enum and pattern should be satisfied");
    }

    @Test
    void validate_enumAndPattern_invalidEnum() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"UNKNOWN\",\"email\":\"john.doe@example.com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Invalid enum value must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("status")), "Message should reference the 'status' property");
    }

    @Test
    void validate_enumAndPattern_invalidPattern() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"ACTIVE\",\"email\":\"invalid-email@@example..com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Invalid email pattern must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("email")), "Message should reference the 'email' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_array_valid() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"green\",\"blue\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Array satisfies minItems, maxItems and enum constraints");
    }

    @Test
    void validate_array_belowMinItems() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Empty array should violate minItems");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("tags")), "Message should reference the 'tags' property");
    }

    @Test
    void validate_array_invalidEnumItem() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"purple\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Second element not in enum must be reported");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("/tags/1")), "Message should point to the second array element");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_ref_validObject() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40}}");
        Set<ValidationMessage> messages = refSchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Valid $ref object should produce no messages");
    }

    @Test
    void validate_ref_additionalProperty() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40,\"extra\":\"value\"}}");
        Set<ValidationMessage> messages = refSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Additional property should be flagged");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("extra")), "Message should reference the unexpected 'extra' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_recursiveAnchor_validDeepStructure() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"node\":{\"value\":\"leaf\"}}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, (ctx, vc) -> {
        });
        assertTrue(messages.isEmpty(), "Deep recursive structure should validate without error");
    }

    @Test
    void validate_recursiveAnchor_typeMismatch() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"value\":123}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, (ctx, vc) -> {
        });
        assertFalse(messages.isEmpty(), "Type mismatch at deepest level must be reported");
        assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("value")), "Message should reference the 'value' property");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void validate_customizerSetsNullAttributeKey_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
//         ExecutionContextCustomizer badCustomizer = (ctx, vc) -> ctx.setAttribute(null, "value");
//         JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
//         assertThrows(NullPointerException.class, () -> basicSchema.validate(node, badCustomizer), "Setting a null attribute key should propagate NPE");
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_malformedRef_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("{}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> malformedRefSchema.validate(node, (ctx, vc) -> {
        }), "Malformed percent‑encoding in $ref must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("illegal"), "Exception message should indicate illegal argument");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_unresolvableRef_throwsJsonSchemaException() throws Exception {
        JsonNode node = mapper.readTree("{\"unknown\":{}}");
        assertThrows(JsonSchemaException.class, () -> unresolvableRefSchema.validate(node, (ctx, vc) -> {
        }), "Unresolvable $ref should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_invalidValidatorType_throwsIllegalStateException() throws Exception {
        JsonNode node = mapper.readTree("{\"field\":\"value\"}");
        assertThrows(IllegalStateException.class, () -> invalidValidatorSchema.validate(node, (ctx, vc) -> {
        }), "Unknown validator type must cause IllegalStateException");
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void validate_failFastStopsAfterFirstError(@Mock ExecutionContextCustomizer customizer) {
//         ExecutionContextCustomizer failFastCustomizer = (ctx, vc) -> ctx.setFailFast(true);
//         String schemaJson = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "\"name\":{\"type\":\"string\"}," + "\"age\":{\"type\":\"integer\"}" + "}," + "\"required\":[\"name\",\"age\"]" + "}";
//         JsonSchema failFastSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);
//         JsonNode node = JsonNodeFactory.instance.objectNode().put("name", 123);
//         Set<ValidationMessage> messages = failFastSchema.validate(node, failFastCustomizer);
//         assertEquals(1, messages.size(), "Fail‑fast should stop after the first validation error");
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

18:29:54.423 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword $recursiveAnchor - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_validate_4_Test ✔
│     ├─ validate_nullRootNode_andNullCustomizer_throwsNPE() ✔
│     ├─ validate_ref_validObject() ✔
│     ├─ validate_enumAndPattern_validValues() ✔
│     ├─ validate_recursiveAnchor_validDeepStructure() ✘ Deep recursive structure should validate without error ==> expected: <true> but was: <false>
│     ├─ validate_unresolvableRef_throwsJsonSchemaException() ✔
│     ├─ validate_recursiveAnchor_typeMismatch() ✔
│     ├─ validate_ref_additionalProperty() ✔
│     ├─ validate_nonNullRootNode_withNullCustomizer_throwsNPE() ✘ Null customizer with a valid node – should throw NPE ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ validate_enumAndPattern_invalidEnum() ✔
│     ├─ validate_array_belowMinItems() ✔
│     ├─ validate_invalidValidatorType_throwsIllegalStateException() ✘ Unknown validator type must cause IllegalStateException ==> Expected java.lang.IllegalStateException to be thrown, but nothing was thrown.
│     ├─ validate_numericBoundaries(JsonNode, boolean) ✔
│     │  ├─ [1] {"value":0}, true ✔
│     │  ├─ [2] {"value":100}, true ✔
│     │  ├─ [3] {"value":-1.0E-4}, false ✔
│     │  └─ [4] {"value":100.01}, false ✔
│     ├─ validate_malformedRef_throwsIllegalArgumentException() ✘ Malformed percent‑encoding in $ref must cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ validate_array_invalidEnumItem() ✘ Message should point to the second array element ==> expected: <true> but was: <false>
│     ├─ validate_enumAndPattern_invalidPattern() ✔
│     └─ validate_array_valid() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (5):
  JUnit Jupiter:JsonSchema_validate_4_Test:validate_recursiveAnchor_validDeepStructure()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_4_Test', methodName = 'validate_recursiveAnchor_validDeepStructure', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Deep recursive structure should validate without error ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_validate_4_Test.validate_recursiveAnchor_validDeepStructure(JsonSchema_validate_4_Test.java:255)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_4_Test:validate_nonNullRootNode_withNullCustomizer_throwsNPE()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_4_Test', methodName = 'validate_nonNullRootNode_withNullCustomizer_throwsNPE', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Null customizer with a valid node – should throw NPE ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_4_Test.validate_nonNullRootNode_withNullCustomizer_throwsNPE(JsonSchema_validate_4_Test.java:136)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_4_Test:validate_invalidValidatorType_throwsIllegalStateException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_4_Test', methodName = 'validate_invalidValidatorType_throwsIllegalStateException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unknown validator type must cause IllegalStateException ==> Expected java.lang.IllegalStateException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_4_Test.validate_invalidValidatorType_throwsIllegalStateException(JsonSchema_validate_4_Test.java:296)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_4_Test:validate_malformedRef_throwsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_4_Test', methodName = 'validate_malformedRef_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Malformed percent‑encoding in $ref must cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_4_Test.validate_malformedRef_throwsIllegalArgumentException(JsonSchema_validate_4_Test.java:279)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_4_Test:validate_array_invalidEnumItem()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_4_Test', methodName = 'validate_array_invalidEnumItem', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Message should point to the second array element ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_validate_4_Test.validate_array_invalidEnumItem(JsonSchema_validate_4_Test.java:228)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 452 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        19 tests found           ]
[         0 tests skipped         ]
[        19 tests started         ]
[         0 tests aborted         ]
[        14 tests successful      ]
[         5 tests failed          ]


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
 

api document of method validate(JsonNode, ExecutionContextCustomizer): 

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
class `com.networknt.schema.ExecutionContextCustomizer`:
	
class `com.networknt.schema.OutputFormat`:
	dependent fields:
		com.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: 
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
		method `public T validate(String, InputFormat, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: 
     * Validates the given input string using the input format, starting at the root
     * of the data path. The output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>         the result type
     * @param input       the input
     * @param inputFormat the inputFormat
     * @param format      the formatter
     * @return the result
     ;
		method `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: 
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
		method `public T validate(JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: 
     * Validates the given root JsonNode, starting at the root of the data path. The
     * output will be formatted using the formatter specified.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param <T>      the result type
     * @param rootNode the root node
     * @param format   the formatter
     * @return the result
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
