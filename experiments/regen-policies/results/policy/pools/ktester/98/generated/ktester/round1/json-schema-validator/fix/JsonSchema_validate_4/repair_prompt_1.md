
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.util.concurrent.atomic.AtomicInteger;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
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
        public void customize(ExecutionContext ctx) {
            count.incrementAndGet();
            lastContext = ctx;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        /* ---------- basic schema (already used in the starter test) ---------- */
        String basic = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{ \"type\":\"string\" }" + "}," + "\"required\":[\"name\"]" + "}";
        basicSchema = factory.getSchema(basic);
        /* ---------- numeric schema (min 0, max 100) ---------- */
        String numeric = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"value\":{ \"type\":\"number\", \"minimum\":0, \"maximum\":100 }" + "}," + "\"required\":[\"value\"]" + "}";
        numericSchema = factory.getSchema(numeric);
        /* ---------- enum & pattern schema ---------- */
        String enumPattern = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"status\":{ \"type\":\"string\", \"enum\":[\"ACTIVE\",\"INACTIVE\",\"PENDING\"] }," + "   \"email\":{ \"type\":\"string\", \"pattern\":\"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\" }" + "}," + "\"required\":[\"status\",\"email\"]" + "}";
        enumPatternSchema = factory.getSchema(enumPattern);
        /* ---------- array schema ---------- */
        String array = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"tags\":{" + "       \"type\":\"array\"," + "       \"minItems\":1," + "       \"maxItems\":5," + "       \"items\":{ \"type\":\"string\", \"enum\":[\"red\",\"green\",\"blue\",\"yellow\"] }" + "   }" + "}," + "\"required\":[\"tags\"]" + "}";
        arraySchema = factory.getSchema(array);
        /* ---------- $ref schema (person definition) ---------- */
        String ref = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"person\":{ \"$ref\":\"#/definitions/Person\" }" + "}," + "\"definitions\":{" + "   \"Person\":{" + "       \"type\":\"object\"," + "       \"properties\":{" + "           \"name\":{ \"type\":\"string\" }," + "           \"age\":{ \"type\":\"integer\" }" + "       }," + "       \"required\":[\"name\",\"age\"]," + "       \"additionalProperties\":false" + "   }" + "}," + "\"required\":[\"person\"]" + "}";
        refSchema = factory.getSchema(ref);
        /* ---------- recursive anchor schema ---------- */
        String recursive = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"$recursiveAnchor\":true," + "\"type\":\"object\"," + "\"properties\":{" + "   \"node\":{ \"$ref\":\"#\" }," + "   \"value\":{ \"type\":\"string\" }" + "}," + "\"required\":[\"value\"]" + "}";
        recursiveSchema = factory.getSchema(recursive);
        /* ---------- malformed $ref (invalid percent‑encoding) ---------- */
        String malformedRef = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"bad\":{ \"$ref\":\"%ZZ\" }" + "}" + "}";
        malformedRefSchema = factory.getSchema(malformedRef);
        /* ---------- unresolvable $ref ---------- */
        String unresolvableRef = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"unknown\":{ \"$ref\":\"#/definitions/Unknown\" }" + "}" + "}";
        unresolvableRefSchema = factory.getSchema(unresolvableRef);
        /* ---------- invalid validator type ---------- */
        String invalidValidator = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"field\":{ \"type\":\"unknownType\" }" + "}" + "}";
        invalidValidatorSchema = factory.getSchema(invalidValidator);
    }

    private static Stream<Arguments> numericBoundaryProvider() throws Exception {
        ObjectMapper om = new ObjectMapper();
        return Stream.of(Arguments.of(om.readTree("{\"value\":0}"), true), Arguments.of(om.readTree("{\"value\":100}"), true), Arguments.of(om.readTree("{\"value\":-0.0001}"), false), Arguments.of(om.readTree("{\"value\":100.01}"), false));
    }

    @Test
    void validate_validNode_callsCustomizerAndReturnsEmptySet(@Mock ExecutionContextCustomizer customizer) throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
        CountingCustomizer customizer = new CountingCustomizer();
        Set<ValidationMessage> messages = basicSchema.validate(validNode, customizer);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
        assertEquals(1, customizer.count.get(), "Customizer should be invoked exactly once");
    }

    @Test
    void validate_invalidNode_callsCustomizerAndReturnsMessages(@Mock ExecutionContextCustomizer customizer) throws Exception {
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");
        CountingCustomizer customizer = new CountingCustomizer();
        Set<ValidationMessage> messages = basicSchema.validate(invalidNode, customizer);
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, customizer.count.get(), "Customizer should be invoked exactly once");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_nullRootNode_andNullCustomizer_throwsNPE() {
        assertThrows(NullPointerException.class, () -> basicSchema.validate(null, (ExecutionContextCustomizer) null), "Both arguments null – should throw NPE");
    }

    @Test
    void validate_nullRootNode_withCustomizer_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
        CountingCustomizer customizer = new CountingCustomizer();
        assertThrows(NullPointerException.class, () -> basicSchema.validate(null, customizer), "Null rootNode with a customizer – should throw NPE");
    }

    @Test
    void validate_nonNullRootNode_withNullCustomizer_throwsNPE() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        assertThrows(NullPointerException.class, () -> basicSchema.validate(node, (ExecutionContextCustomizer) null), "Null customizer with a valid node – should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_customizerThrowsException_propagatesRuntimeException(@Mock ExecutionContextCustomizer customizer) {
        ExecutionContextCustomizer throwingCustomizer = ctx -> {
            throw new RuntimeException("custom error");
        };
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", "Test");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> basicSchema.validate(node, throwingCustomizer), "Customizer exception must propagate");
        assertEquals("custom error", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_localeFrench_returnsFrenchMessage(@Mock ExecutionContextCustomizer customizer) throws Exception {
        // customizer sets locale to French
        ExecutionContextCustomizer localeCustomizer = ctx -> ctx.setLocale(Locale.FRENCH);
        String schemaJson = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"age\":{ \"type\":\"integer\", \"minimum\":0 }" + "}," + "\"required\":[\"age\"]" + "}";
        JsonSchema ageSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"age\":-1}");
        Set<ValidationMessage> messages = ageSchema.validate(node, localeCustomizer);
        assertFalse(messages.isEmpty(), "Expected validation error for negative age");
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase(Locale.FRENCH).contains("doit"), "Message should be localized to French");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("numericBoundaryProvider")
    void validate_numericBoundaries(JsonNode node, boolean expectValid) {
        Set<ValidationMessage> messages = numericSchema.validate(node, ctx -> {
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
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, ctx -> {
        });
        assertTrue(messages.isEmpty(), "Both enum and pattern should be satisfied");
    }

    @Test
    void validate_enumAndPattern_invalidEnum() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"UNKNOWN\",\"email\":\"john.doe@example.com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, ctx -> {
        });
        assertFalse(messages.isEmpty(), "Invalid enum value must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("status")), "Message should reference the 'status' property");
    }

    @Test
    void validate_enumAndPattern_invalidPattern() throws Exception {
        JsonNode node = mapper.readTree("{\"status\":\"ACTIVE\",\"email\":\"invalid-email@@example..com\"}");
        Set<ValidationMessage> messages = enumPatternSchema.validate(node, ctx -> {
        });
        assertFalse(messages.isEmpty(), "Invalid email pattern must produce a message");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("email")), "Message should reference the 'email' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_array_valid() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"green\",\"blue\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, ctx -> {
        });
        assertTrue(messages.isEmpty(), "Array satisfies minItems, maxItems and enum constraints");
    }

    @Test
    void validate_array_belowMinItems() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, ctx -> {
        });
        assertFalse(messages.isEmpty(), "Empty array should violate minItems");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("tags")), "Message should reference the 'tags' property");
    }

    @Test
    void validate_array_invalidEnumItem() throws Exception {
        JsonNode node = mapper.readTree("{\"tags\":[\"red\",\"purple\"]}");
        Set<ValidationMessage> messages = arraySchema.validate(node, ctx -> {
        });
        assertFalse(messages.isEmpty(), "Second element not in enum must be reported");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("/tags/1")), "Message should point to the second array element");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_ref_validObject() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40}}");
        Set<ValidationMessage> messages = refSchema.validate(node, ctx -> {
        });
        assertTrue(messages.isEmpty(), "Valid $ref object should produce no messages");
    }

    @Test
    void validate_ref_additionalProperty() throws Exception {
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Sam\",\"age\":40,\"extra\":\"value\"}}");
        Set<ValidationMessage> messages = refSchema.validate(node, ctx -> {
        });
        assertFalse(messages.isEmpty(), "Additional property should be flagged");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("extra")), "Message should reference the unexpected 'extra' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_recursiveAnchor_validDeepStructure() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"node\":{\"value\":\"leaf\"}}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, ctx -> {
        });
        assertTrue(messages.isEmpty(), "Deep recursive structure should validate without error");
    }

    @Test
    void validate_recursiveAnchor_typeMismatch() throws Exception {
        JsonNode node = mapper.readTree("{\"node\":{\"node\":{\"value\":123}}}");
        Set<ValidationMessage> messages = recursiveSchema.validate(node, ctx -> {
        });
        assertFalse(messages.isEmpty(), "Type mismatch at deepest level must be reported");
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("value")), "Message should reference the 'value' property");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_customizerSetsNullAttributeKey_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
        ExecutionContextCustomizer badCustomizer = ctx -> ctx.setAttribute(null, "value");
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        assertThrows(NullPointerException.class, () -> basicSchema.validate(node, badCustomizer), "Setting a null attribute key should propagate NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_malformedRef_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("{}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> malformedRefSchema.validate(node, ctx -> {
        }), "Malformed percent‑encoding in $ref must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("illegal"), "Exception message should indicate illegal argument");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_unresolvableRef_throwsJsonSchemaException() throws Exception {
        JsonNode node = mapper.readTree("{\"unknown\":{}}");
        assertThrows(JsonSchemaException.class, () -> unresolvableRefSchema.validate(node, ctx -> {
        }), "Unresolvable $ref should raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_invalidValidatorType_throwsIllegalStateException() throws Exception {
        JsonNode node = mapper.readTree("{\"field\":\"value\"}");
        assertThrows(IllegalStateException.class, () -> invalidValidatorSchema.validate(node, ctx -> {
        }), "Unknown validator type must cause IllegalStateException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_failFastStopsAfterFirstError(@Mock ExecutionContextCustomizer customizer) {
        ExecutionContextCustomizer failFastCustomizer = ctx -> ctx.setFailFast(true);
        String schemaJson = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{ \"type\":\"string\" }," + "   \"age\":{ \"type\":\"integer\" }" + "}," + "\"required\":[\"name\",\"age\"]" + "}";
        JsonSchema failFastSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaJson);
        JsonNode node = JsonNodeFactory.instance.objectNode().put("name", 123);
        Set<ValidationMessage> messages = failFastSchema.validate(node, failFastCustomizer);
        assertEquals(1, messages.size(), "Fail‑fast should stop after the first validation error");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:17: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:109: error: cannot find symbol
    void validate_validNode_callsCustomizerAndReturnsEmptySet(@Mock ExecutionContextCustomizer customizer) throws Exception {
                                                               ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:118: error: cannot find symbol
    void validate_invalidNode_callsCustomizerAndReturnsMessages(@Mock ExecutionContextCustomizer customizer) throws Exception {
                                                                 ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:133: error: cannot find symbol
    void validate_nullRootNode_withCustomizer_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
                                                         ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:146: error: cannot find symbol
    void validate_customizerThrowsException_propagatesRuntimeException(@Mock ExecutionContextCustomizer customizer) {
                                                                        ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:157: error: cannot find symbol
    void validate_localeFrench_returnsFrenchMessage(@Mock ExecutionContextCustomizer customizer) throws Exception {
                                                     ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:274: error: cannot find symbol
    void validate_customizerSetsNullAttributeKey_throwsNPE(@Mock ExecutionContextCustomizer customizer) {
                                                            ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:307: error: cannot find symbol
    void validate_failFastStopsAfterFirstError(@Mock ExecutionContextCustomizer customizer) {
                                                ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:57: error: CountingCustomizer is not abstract and does not override abstract method customize(ExecutionContext,ValidationContext) in ExecutionContextCustomizer
    private static class CountingCustomizer implements ExecutionContextCustomizer {
                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:63: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:111: error: variable customizer is already defined in method validate_validNode_callsCustomizerAndReturnsEmptySet(ExecutionContextCustomizer)
        CountingCustomizer customizer = new CountingCustomizer();
                           ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:120: error: variable customizer is already defined in method validate_invalidNode_callsCustomizerAndReturnsMessages(ExecutionContextCustomizer)
        CountingCustomizer customizer = new CountingCustomizer();
                           ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:134: error: variable customizer is already defined in method validate_nullRootNode_withCustomizer_throwsNPE(ExecutionContextCustomizer)
        CountingCustomizer customizer = new CountingCustomizer();
                           ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:147: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer throwingCustomizer = ctx -> {
                                                        ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:159: error: cannot find symbol
        ExecutionContextCustomizer localeCustomizer = ctx -> ctx.setLocale(Locale.FRENCH);
                                                                ^
  symbol:   method setLocale(Locale)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:159: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer localeCustomizer = ctx -> ctx.setLocale(Locale.FRENCH);
                                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:197: error: cannot find symbol
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("status")), "Message should reference the 'status' property");
                                                    ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:206: error: cannot find symbol
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("email")), "Message should reference the 'email' property");
                                                    ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:224: error: cannot find symbol
        assertTrue(messages.stream().anyMatch(m -> m.getPath().endsWith("tags")), "Message should reference the 'tags' property");
                                                    ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:233: error: cannot find symbol
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("/tags/1")), "Message should point to the second array element");
                                                    ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:251: error: cannot find symbol
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("extra")), "Message should reference the unexpected 'extra' property");
                                                    ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:269: error: cannot find symbol
        assertTrue(messages.stream().anyMatch(m -> m.getPath().contains("value")), "Message should reference the 'value' property");
                                                    ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:275: error: cannot find symbol
        ExecutionContextCustomizer badCustomizer = ctx -> ctx.setAttribute(null, "value");
                                                             ^
  symbol:   method setAttribute(<null>,String)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:275: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer badCustomizer = ctx -> ctx.setAttribute(null, "value");
                                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:308: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer failFastCustomizer = ctx -> ctx.setFailFast(true);
                                                        ^
27 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(JsonNode, ExecutionContextCustomizer)': '\n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param rootNode            the root node\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ', 'dependent classes': 'class `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>         the result type\n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @param format      the formatter\n     * @return the result\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param rootNode the root node\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>      the result type\n     * @param rootNode the root node\n     * @param format   the formatter\n     * @return the result\n     ;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


