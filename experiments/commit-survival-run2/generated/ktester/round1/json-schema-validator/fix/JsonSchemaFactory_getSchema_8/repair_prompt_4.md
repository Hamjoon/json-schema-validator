


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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchemaFactory_getSchema_8_Test {

    //     @Mock
    ObjectMapper jsonMapper;

    //     @Mock
    YAMLMapper yamlMapper;

    // Additional objects needed for construction
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema";

    private JsonSchemaFactory factorySpy;

    /**
     * The factory under test – we use the public default instance.
     */
    private JsonSchemaFactory factory;

    //     @BeforeEach
    void setupBeforeEach() {
        // Use the public factory instance – it is fully initialised and does not need
        // any Mockito stubbing.
        factory = JsonSchemaFactory.getInstance();
    }

    /* -----------------------------------------------------------------------
     * Helper to turn a JSON string into a JsonNode.  Using a real ObjectMapper
     * keeps the test independent of the mocked jsonMapper that is only needed
     * for the factory construction.
     * ----------------------------------------------------------------------- */
    private static JsonNode parseJson(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid test JSON", e);
        }
    }

    /* -----------------------------------------------------------------------
     * SUCCESSFUL CASES – the factory should return a non‑null JsonSchema.
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> successCases() {
        return Stream.of(Arguments.of("http JSON schema", "http://example.com/schema.json", "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"), Arguments.of("file URI with YAML‑compatible JSON node", "file:/tmp/local-schema.yaml", "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"), Arguments.of("empty JSON object", "http://example.com/empty-schema.json", "{}"));
    }

    /* -----------------------------------------------------------------------
     * EXCEPTION CASES – the factory should propagate the exception thrown by
     * newJsonSchema (or by URI construction when the literal is malformed).
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null arguments
        Arguments.of("null URI", null, "{ \"type\": \"object\" }", NullPointerException.class), Arguments.of("null JsonNode", "http://example.com/any-schema.json", null, NullPointerException.class), Arguments.of("both null", null, null, NullPointerException.class), // Unsupported / malformed URIs
        Arguments.of("unsupported scheme (ftp)", "ftp://example.com/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("relative URI", "schemas/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("data URI scheme", "data:application/json,{\"type\":\"object\"}", "{ \"type\": \"object\" }", IllegalArgumentException.class), // Invalid JSON node shapes
        Arguments.of("JSON array node", "http://example.com/schema.json", "[ { \"type\": \"object\" } ]", IllegalArgumentException.class), Arguments.of("primitive JSON node", "http://example.com/schema.json", "\"string\"", IllegalArgumentException.class), // Invalid schema content (unknown type)
        Arguments.of("unknown type keyword", "http://example.com/schema.json", "{ \"type\": \"unknown\" }", IllegalArgumentException.class));
    }

    @Test
    void getSchema_withUriAndJsonNode_returnsJsonSchema() throws Exception {
        // GIVEN
        URI schemaUri = new URI("http://example.com/my-schema");
        JsonNode schemaNode = parseJson("{ \"type\": \"object\" }");
        // WHEN
        JsonSchema actual = factory.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(actual, "The returned JsonSchema should not be null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void getSchema_successful(String caseName, String uriString, String json) throws Exception {
        // GIVEN
        URI schemaUri = new URI(uriString);
        JsonNode schemaNode = parseJson(json);
        // WHEN
        JsonSchema result = factory.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(result, "Factory should return a non‑null JsonSchema for case: " + caseName);
    }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("exceptionCases")
//     void getSchema_exceptions(String caseName, String uriString, String json, Class<? extends Throwable> expectedException) throws Exception {
//         // -------------------------------------------------
//         // Build parameters – handling nulls and malformed URIs
//         // -------------------------------------------------
//         URI schemaUri = null;
//         JsonNode schemaNode = null;
//         if (uriString != null) {
//             try {
//                 schemaUri = new URI(uriString);
//             } catch (Exception e) {
//                 // If the URI literal itself is malformed we expect the test
//                 // data to map to IllegalArgumentException.
//                 assertTrue(IllegalArgumentException.class.isAssignableFrom(expectedException), "Test data inconsistency: malformed URI should map to IllegalArgumentException");
//                 assertThrows(expectedException, () -> factory.getSchema(new URI(uriString), parseJson("{\"type\":\"object\"}")));
//                 return;
//             }
//         }
//         if (json != null) {
//             schemaNode = parseJson(json);
//         }
//         // -------------------------------------------------
//         // Exercise the method under test and verify the exception.
//         // -------------------------------------------------
//         assertThrows(expectedException, () -> factory.getSchema(schemaUri, schemaNode), () -> "Expected " + expectedException.getSimpleName() + " for case: " + caseName);
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_getSchema_8_Test ✔
│     ├─ getSchema_withUriAndJsonNode_returnsJsonSchema() ✘ Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
│     └─ getSchema_successful(String, String, String) ✔
│        ├─ [1] http JSON schema ✘ Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
│        ├─ [2] file URI with YAML‑compatible JSON node ✘ Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
│        └─ [3] empty JSON object ✘ Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (4):
  JUnit Jupiter:JsonSchemaFactory_getSchema_8_Test:getSchema_withUriAndJsonNode_returnsJsonSchema()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_8_Test', methodName = 'getSchema_withUriAndJsonNode_returnsJsonSchema', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
       com.networknt.schema.JsonSchemaFactory_getSchema_8_Test.getSchema_withUriAndJsonNode_returnsJsonSchema(JsonSchemaFactory_getSchema_8_Test.java:79)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_8_Test:getSchema_successful(String, String, String):[1] http JSON schema
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_8_Test', methodName = 'getSchema_successful', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
       com.networknt.schema.JsonSchemaFactory_getSchema_8_Test.getSchema_successful(JsonSchemaFactory_getSchema_8_Test.java:91)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_8_Test:getSchema_successful(String, String, String):[2] file URI with YAML‑compatible JSON node
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_8_Test', methodName = 'getSchema_successful', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
       com.networknt.schema.JsonSchemaFactory_getSchema_8_Test.getSchema_successful(JsonSchemaFactory_getSchema_8_Test.java:91)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_8_Test:getSchema_successful(String, String, String):[3] empty JSON object
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_8_Test', methodName = 'getSchema_successful', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.JsonSchemaFactory.getSchema(java.net.URI, com.fasterxml.jackson.databind.JsonNode)" because "this.factory" is null
       com.networknt.schema.JsonSchemaFactory_getSchema_8_Test.getSchema_successful(JsonSchemaFactory_getSchema_8_Test.java:91)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]

Test run finished after 101 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[         4 tests found           ]
[         0 tests skipped         ]
[         4 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         4 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode)`;
		method `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
