


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.Mockito;

/**
 * Tests for {@link JsonSchema#validate(String, InputFormat)} covering a variety of
 * valid and invalid inputs.
 */
// @ExtendWith(MockitoExtension.class)
class JsonSchema_validate_6_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * Schema under test.
     */
    private JsonSchema schema;

    /**
     * Configuration used when creating the schema.
     */
    private static SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft (the most recent draft supported by the library)
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Simple config – we do not need any special behaviour for these tests
        config = SchemaValidatorsConfig.builder().build();
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal schema that requires a string property "name"
        String schemaData = "{" +
                "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," +
                "\"type\":\"object\"," +
                "\"properties\":{" +
                "   \"name\":{ \"type\":\"string\" }" +
                "}," +
                "\"required\":[\"name\"]" +
                "}";
        // Create the JsonSchema instance that will be exercised in each test
        schema = factory.getSchema(schemaData, config);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
        config = null;
    }

    private static Stream<Arguments> provideInvalidInputs() {
        return Stream.of(
                Arguments.of("", InputFormat.JSON, IllegalArgumentException.class), // malformed JSON
                Arguments.of("   \n\t", InputFormat.JSON, IllegalArgumentException.class), // JSON as YAML
                Arguments.of("{name:\"John\"}", InputFormat.JSON, RuntimeException.class),
                Arguments.of("{\"name\":\"John\"}", InputFormat.YAML, RuntimeException.class)
        );
    }

    @Test
    void validate_validInput_returnsEmptySet() {
        // GIVEN a JSON document that satisfies the schema
        String validJson = "{ \"name\": \"John Doe\" }";
        // WHEN the validate method is invoked
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);
        // THEN no validation messages should be produced
        assertTrue(messages.isEmpty(),
                () -> "Expected no validation messages for valid input, but got: " + messages);
    }

    @Test
    void validate_invalidInput_returnsMessages() {
        // GIVEN a JSON document that violates the schema (name is not a string)
        String invalidJson = "{ \"name\": 123 }";
        // WHEN the validate method is invoked
        Set<ValidationMessage> messages = schema.validate(invalidJson, InputFormat.JSON);
        // THEN at least one validation message should be produced
        assertFalse(messages.isEmpty(), "Expected validation messages for invalid input, but the set was empty");
        // The schema only has one rule, so we expect exactly one message
        assertEquals(1, messages.size(),
                () -> "Expected exactly one validation message, but got: " + messages.size());
        // OPTIONAL: verify that the message refers to a type error
        ValidationMessage msg = messages.iterator().next();
        assertEquals("type", msg.getType(),
                () -> "Expected a type validation error, but got: " + msg.getType());
    }

    @Test
    void validate_validJson_returnsEmptySet() {
        String validJson = "{ \"name\": \"John Doe\" }";
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);
        assertTrue(messages.isEmpty(),
                () -> "Expected no validation messages for valid JSON, but got: " + messages);
    }

    @Test
    void validate_validYaml_returnsEmptySet() {
        String validYaml = "name: John Doe\nage: 30";
        Set<ValidationMessage> messages = schema.validate(validYaml, InputFormat.YAML);
        assertTrue(messages.isEmpty(),
                () -> "Expected no validation messages for valid YAML, but got: " + messages);
    }

    @Test
    void validate_extraProperties_returnsEmptySet() {
        String jsonWithExtra = "{\"name\":\"John\",\"extra\":\"value\"}";
        Set<ValidationMessage> messages = schema.validate(jsonWithExtra, InputFormat.JSON);
        assertTrue(messages.isEmpty(),
                () -> "Extra properties should be allowed by default, but got: " + messages);
    }

    @Test
    void validate_missingRequiredProperty_returnsMessage() {
        String jsonMissingName = "{\"age\":30}";
        Set<ValidationMessage> messages = schema.validate(jsonMissingName, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for missing required property");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("required", msg.getType(),
                () -> "Expected a 'required' validation error, but got: " + msg.getType());
    }

    @Test
    void validate_typeMismatch_returnsMessage() {
        String jsonTypeMismatch = "{\"name\":123}";
        Set<ValidationMessage> messages = schema.validate(jsonTypeMismatch, InputFormat.JSON);
        assertFalse(messages.isEmpty(), "Expected a validation message for type mismatch");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("type", msg.getType(),
                () -> "Expected a 'type' validation error, but got: " + msg.getType());
    }

    @Test
    void validate_yamlTypeMismatch_returnsMessage() {
        String yamlTypeMismatch = "name: John\nage: thirty";
        Set<ValidationMessage> messages = schema.validate(yamlTypeMismatch, InputFormat.YAML);
        assertFalse(messages.isEmpty(), "Expected a validation message for YAML type mismatch");
        ValidationMessage msg = messages.iterator().next();
        // The schema does not define "age", but we simply verify that at least one message is produced.
        assertNotNull(msg.getMessage(),
                () -> "Expected a non‑null validation message, but got null");
    }

    @Test
    void validate_emptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate("", InputFormat.JSON),
                "Empty input should trigger IllegalArgumentException");
    }

    @Test
    void validate_whitespaceOnly_throwsIllegalArgumentException() {
        String ws = "   \n\t  ";
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(ws, InputFormat.JSON),
                "Whitespace‑only input should trigger IllegalArgumentException");
    }

    @Test
    void validate_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> schema.validate(null, InputFormat.JSON),
                "Null input should trigger NullPointerException");
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"John\"}", null),
                "Null InputFormat should trigger NullPointerException");
    }

    @Test
    void validate_malformedJson_throwsJsonParseException() {
        String malformed = "{name:\"John\"}";
        // The library wraps parsing errors in a RuntimeException; we assert that an exception
        // is thrown and that its cause is JsonParseException.
        Exception ex = assertThrows(RuntimeException.class,
                () -> schema.validate(malformed, InputFormat.JSON),
                "Malformed JSON should result in a parsing exception");
        assertTrue(ex.getCause() instanceof JsonParseException || ex instanceof JsonParseException,
                () -> "Expected JsonParseException as cause, but got: " + ex);
    }

    @Test
    void validate_jsonContentWithYamlFormat_throwsException() {
        String json = "{\"name\":\"John\"}";
        Exception ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, InputFormat.YAML),
                "Providing JSON while declaring YAML format should fail");
        // The exact exception type may vary; we only assert that it is a runtime exception.
        assertNotNull(ex, "Exception should not be null");
    }

    @Test
    void validate_malformedPercentEncoding_throwsUnsupportedEncodingException() {
        String badEncoding = "%ZZ%invalid%encoding";
        // The library may wrap the UnsupportedEncodingException; we check the cause.
        Exception ex = assertThrows(RuntimeException.class,
                () -> schema.validate(badEncoding, InputFormat.JSON),
                "Malformed percent‑encoding should trigger UnsupportedEncodingException");
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof UnsupportedEncodingException,
                () -> "Expected UnsupportedEncodingException as cause, but got: " + cause);
    }

//     @Test
//     void validate_unknownInputFormat_throwsIllegalArgumentException() {
//         // Create a mock InputFormat that the library does not recognise.
//         InputFormat unknown = Mockito.mock(InputFormat.class);
//         // The mock will return a name that does not match any supported format.
//         Mockito.when(unknown.name()).thenReturn("UNKNOWN");
//         assertThrows(IllegalArgumentException.class,
//                 () -> schema.validate("{\"name\":\"John\"}", unknown),
//                 "Unsupported InputFormat should trigger IllegalArgumentException");
//     }

    @ParameterizedTest(name = "[{index}] input={0}, format={1}")
    @MethodSource("provideInvalidInputs")
    void validate_invalidInputs_throwException(String input, InputFormat format,
                                                Class<? extends Throwable> expected) {
        assertThrows(expected,
                () -> schema.validate(input, format),
                () -> "Expected " + expected.getSimpleName() + " for input: " + input);
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_validate_6_Test ✔
│     ├─ validate_malformedPercentEncoding_throwsUnsupportedEncodingException() ✘ Expected UnsupportedEncodingException as cause, but got: com.fasterxml.jackson.core.JsonParseException: Unexpected character ('%' (code 37)): expected a valid value (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
│     │         at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 1] ==> expected: <true> but was: <false>
│     ├─ validate_emptyString_throwsIllegalArgumentException() ✘ Empty input should trigger IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ validate_invalidInput_returnsMessages() ✔
│     ├─ validate_missingRequiredProperty_returnsMessage() ✔
│     ├─ validate_invalidInputs_throwException(String, InputFormat, Class) ✔
│     │  ├─ [1] input=, format=JSON ✘ Expected IllegalArgumentException for input:  ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     │  ├─ [2] input=     , format=JSON ✘ Expected IllegalArgumentException for input:    
│     │  │        	 ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     │  ├─ [3] input={name:"John"}, format=JSON ✔
│     │  └─ [4] input={"name":"John"}, format=YAML ✘ Expected RuntimeException for input: {"name":"John"} ==> Expected java.lang.RuntimeException to be thrown, but nothing was thrown.
│     ├─ validate_validYaml_returnsEmptySet() ✔
│     ├─ validate_nullInput_throwsNullPointerException() ✘ Null input should trigger NullPointerException ==> Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
│     ├─ validate_validJson_returnsEmptySet() ✔
│     ├─ validate_yamlTypeMismatch_returnsMessage() ✘ Expected a validation message for YAML type mismatch ==> expected: <false> but was: <true>
│     ├─ validate_typeMismatch_returnsMessage() ✔
│     ├─ validate_malformedJson_throwsJsonParseException() ✔
│     ├─ validate_jsonContentWithYamlFormat_throwsException() ✘ Providing JSON while declaring YAML format should fail ==> Expected java.lang.RuntimeException to be thrown, but nothing was thrown.
│     ├─ validate_validInput_returnsEmptySet() ✔
│     ├─ validate_nullInputFormat_throwsNullPointerException() ✘ Null InputFormat should trigger NullPointerException ==> Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
│     ├─ validate_whitespaceOnly_throwsIllegalArgumentException() ✘ Whitespace‑only input should trigger IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     └─ validate_extraProperties_returnsEmptySet() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (10):
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_malformedPercentEncoding_throwsUnsupportedEncodingException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_malformedPercentEncoding_throwsUnsupportedEncodingException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected UnsupportedEncodingException as cause, but got: com.fasterxml.jackson.core.JsonParseException: Unexpected character ('%' (code 37)): expected a valid value (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 1] ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:42)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:191)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_malformedPercentEncoding_throwsUnsupportedEncodingException(JsonSchema_validate_6_Test.java:230)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_emptyString_throwsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_emptyString_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Empty input should trigger IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_emptyString_throwsIllegalArgumentException(JsonSchema_validate_6_Test.java:173)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_invalidInputs_throwException(String, InputFormat, Class):[1] input=, format=JSON
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_invalidInputs_throwException', methodParameterTypes = 'java.lang.String, com.networknt.schema.InputFormat, java.lang.Class']
    => org.opentest4j.AssertionFailedError: Expected IllegalArgumentException for input:  ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:45)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3149)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_invalidInputs_throwException(JsonSchema_validate_6_Test.java:249)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_invalidInputs_throwException(String, InputFormat, Class):[2] input=   
	, format=JSON
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_invalidInputs_throwException', methodParameterTypes = 'java.lang.String, com.networknt.schema.InputFormat, java.lang.Class']
    => org.opentest4j.AssertionFailedError: Expected IllegalArgumentException for input:    
	 ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:45)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3149)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_invalidInputs_throwException(JsonSchema_validate_6_Test.java:249)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_invalidInputs_throwException(String, InputFormat, Class):[4] input={"name":"John"}, format=YAML
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_invalidInputs_throwException', methodParameterTypes = 'java.lang.String, com.networknt.schema.InputFormat, java.lang.Class']
    => org.opentest4j.AssertionFailedError: Expected RuntimeException for input: {"name":"John"} ==> Expected java.lang.RuntimeException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:45)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3149)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_invalidInputs_throwException(JsonSchema_validate_6_Test.java:249)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_nullInput_throwsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_nullInput_throwsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Null input should trigger NullPointerException ==> Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_nullInput_throwsNullPointerException(JsonSchema_validate_6_Test.java:188)
       [...]
     Caused by: java.lang.IllegalArgumentException: argument "content" is null
       com.fasterxml.jackson.databind.ObjectMapper._assertNotNull(ObjectMapper.java:5060)
       com.fasterxml.jackson.databind.ObjectMapper.readTree(ObjectMapper.java:3278)
       com.networknt.schema.JsonSchemaFactory.readTree(JsonSchemaFactory.java:475)
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:947)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:795)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_yamlTypeMismatch_returnsMessage()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_yamlTypeMismatch_returnsMessage', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected a validation message for YAML type mismatch ==> expected: <false> but was: <true>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertFalse.failNotFalse(AssertFalse.java:63)
       org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:36)
       org.junit.jupiter.api.Assertions.assertFalse(Assertions.java:239)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_yamlTypeMismatch_returnsMessage(JsonSchema_validate_6_Test.java:164)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_jsonContentWithYamlFormat_throwsException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_jsonContentWithYamlFormat_throwsException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Providing JSON while declaring YAML format should fail ==> Expected java.lang.RuntimeException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_jsonContentWithYamlFormat_throwsException(JsonSchema_validate_6_Test.java:215)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_nullInputFormat_throwsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_nullInputFormat_throwsNullPointerException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Null InputFormat should trigger NullPointerException ==> Unexpected exception type thrown, expected: <java.lang.NullPointerException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_nullInputFormat_throwsNullPointerException(JsonSchema_validate_6_Test.java:195)
       [...]
     Caused by: java.lang.IllegalArgumentException: Unsupported input format null
       com.networknt.schema.JsonSchemaFactory.getObjectMapper(JsonSchemaFactory.java:495)
       com.networknt.schema.JsonSchemaFactory.readTree(JsonSchemaFactory.java:475)
       com.networknt.schema.JsonSchema.deserialize(JsonSchema.java:947)
       com.networknt.schema.JsonSchema.validate(JsonSchema.java:795)
       com.networknt.schema.JsonSchema_validate_6_Test.lambda$validate_nullInputFormat_throwsNullPointerException$12(JsonSchema_validate_6_Test.java:196)
       [...]
  JUnit Jupiter:JsonSchema_validate_6_Test:validate_whitespaceOnly_throwsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_validate_6_Test', methodName = 'validate_whitespaceOnly_throwsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Whitespace‑only input should trigger IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_validate_6_Test.validate_whitespaceOnly_throwsIllegalArgumentException(JsonSchema_validate_6_Test.java:181)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]

Test run finished after 434 ms
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
[         9 tests successful      ]
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
 

api document of method validate(String, InputFormat): 

     * Validate the given input string using the input format, starting at the root
     * of the data path.
     * <p>
     * Note that since Draft 2019-09 by default format generates only annotations
     * and not assertions.
     * <p>
     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override
     * the default.
     * 
     * @param input       the input
     * @param inputFormat the inputFormat
     * @return A list of ValidationMessage if there is any validation error, or an
     *         empty list if there is no error.
     

dependent classes: 
class `com.networknt.schema.InputFormat`:
	
class `com.networknt.schema.OutputFormat`:
	dependent fields:
		com.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat)`, api document: 
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
		method `public T validate(String, InputFormat, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat)`, api document: 
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
		method `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat)`, api document: 
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
		method `public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat)`, api document: 
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
     ;
		method `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat)`, api document: 
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



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
