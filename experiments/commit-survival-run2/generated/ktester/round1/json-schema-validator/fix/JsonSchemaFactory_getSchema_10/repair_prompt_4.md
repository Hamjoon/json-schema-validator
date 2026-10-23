


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
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

class JsonSchemaFactory_getSchema_10_Test {

    /**
     * shared test data **
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    /**
     * objects that are part of the JsonSchemaFactory constructor **
     */
    private ObjectMapper jsonMapper;

    private YAMLMapper yamlMapper;

    private Map<String, JsonMetaSchema> jsonMetaSchemas;

    private Map<String, String> uriMap;

    /**
     * objects used in the actual test **
     */
    private JsonNode sampleNode;

    private SchemaValidatorsConfig config;

    private URI sampleUri;

    /**
     * the factory under test (spied so we can stub the protected newJsonSchema method) **
     */
    private JsonSchemaFactory factorySpy;

    /**
     * a lightweight mapper used by the static argument providers **
     */
    private static final ObjectMapper STATIC_MAPPER = new ObjectMapper();

    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * Build a factory that already contains the required meta‑schemas.
         * Using the static {@code getInstance} method is the simplest way to obtain
         * a correctly configured factory for the Draft‑07 version.
         */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        // Sample inputs for the basic test
        sampleUri = new URI("http://example.com/my-schema.json");
        sampleNode = new ObjectMapper().readTree("{\"type\":\"object\"}");
        config = new SchemaValidatorsConfig();
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(Arguments.of("Valid inputs – simple object schema", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("Edge case – Empty JSON object as schema", URI.create("http://example.com/empty"), readJson("{}"), new SchemaValidatorsConfig()), Arguments.of("Valid inputs – custom configuration applied", URI.create("http://example.com/custom-config"), readJson("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}"), customConfig()), Arguments.of("URI with fragment identifier", URI.create("http://example.com/schema#myFragment"), readJson("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("URI with query parameters (null config)", URI.create("http://example.com/schema?version=1&debug=true"), readJson("{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"), null), Arguments.of("Non‑object JsonNode – array schema", URI.create("http://example.com/array-schema"), readJson("[{\"type\":\"string\"},{\"type\":\"integer\"}]"), new SchemaValidatorsConfig()), Arguments.of("File scheme URI (null config)", URI.create("file:/tmp/test-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}}}"), null), Arguments.of("Meta‑schema URI (draft‑07)", URI.create("http://json-schema.org/draft-07/schema#"), readJson("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig()), Arguments.of("Relative URI resolved against base", URI.create("schemas/relative-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"rel\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("Null handling – Config is null (default config used)", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\"}"), null));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of("Null handling – URI is null", null, readJson("{\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class), Arguments.of("Null handling – JsonNode is null", URI.create("http://example.com/schema"), null, new SchemaValidatorsConfig(), IllegalArgumentException.class), Arguments.of("Malformed URI – illegal characters", createSafeUri("http://exa mple.com/ bad"), readJson("{\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class), Arguments.of("Unsupported schema version – unknown $schema value", URI.create("http://example.com/unknown-schema"), readJson("{\"$schema\":\"http://example.com/unknown/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class));
    }

    /**
     * Parse a JSON string into a {@link JsonNode} using the static mapper.
     *  If parsing fails the test suite should surface the problem immediately.
     */
    private static JsonNode readJson(String json) {
        try {
            return STATIC_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON in test data: " + json, e);
        }
    }

    /**
     * Safely create a {@link URI} that may be malformed.
     *  If construction fails we return null, which will cause the factory method
     *  to throw the expected IllegalArgumentException.
     */
    private static URI createSafeUri(String uriString) {
        try {
            return URI.create(uriString);
        } catch (Exception e) {
            return null;
        }
    }

    private static SchemaValidatorsConfig customConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setTypeLoose(true);
        cfg.setFailFast(true);
        return cfg;
    }

    @Test
    void shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig() {
        JsonSchema result = factory.getSchema(sampleUri, sampleNode, config);
        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenSchemaUriIsNull() {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(null, sampleNode, config), "Calling getSchema with a null URI should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenJsonNodeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(sampleUri, null, config), "Calling getSchema with a null JsonNode should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenConfigIsNull() {
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(sampleUri, sampleNode, null), "Calling getSchema with a null SchemaValidatorsConfig should raise IllegalArgumentException");
    }

    /**
     * Successful creation – we only assert that a non‑null schema is returned
     *  and that the factory method was invoked with the exact URI, node and
     *  (if supplied) configuration.
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("successfulCases")
    void shouldCreateSchemaSuccessfully(String description, URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig cfg) {
        JsonSchema result = factory.getSchema(schemaUri, schemaNode, cfg);
        assertNotNull(result, description + ": result must not be null");
    }

    /**
     * Cases that must result in an exception (normally IllegalArgumentException).
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("exceptionCases")
    void shouldThrowExceptionForInvalidInput(String description, URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig cfg, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> factory.getSchema(schemaUri, schemaNode, cfg), description + ": expected " + expectedException.getSimpleName());
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_getSchema_10_Test ✔
│     ├─ shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig) ✔
│     │  ├─ 1 ⇒ Valid inputs – simple object schema ✔
│     │  ├─ 2 ⇒ Edge case – Empty JSON object as schema ✔
│     │  ├─ 3 ⇒ Valid inputs – custom configuration applied ✔
│     │  ├─ 4 ⇒ URI with fragment identifier ✘ Fragment #myFragment cannot be resolved
│     │  ├─ 5 ⇒ URI with query parameters (null config) ✔
│     │  ├─ 6 ⇒ Non‑object JsonNode – array schema ✔
│     │  ├─ 7 ⇒ File scheme URI (null config) ✔
│     │  ├─ 8 ⇒ Meta‑schema URI (draft‑07) ✔
│     │  ├─ 9 ⇒ Relative URI resolved against base ✔
│     │  └─ 10 ⇒ Null handling – Config is null (default config used) ✔
│     ├─ shouldThrowIllegalArgumentException_WhenConfigIsNull() ✘ Calling getSchema with a null SchemaValidatorsConfig should raise IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ shouldThrowIllegalArgumentException_WhenSchemaUriIsNull() ✘ Calling getSchema with a null URI should raise IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfi... ✔
│     │  ├─ 1 ⇒ Null handling – URI is null ✘ Null handling – URI is null: expected IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     │  ├─ 2 ⇒ Null handling – JsonNode is null ✘ Null handling – JsonNode is null: expected IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     │  ├─ 3 ⇒ Malformed URI – illegal characters ✘ Malformed URI – illegal characters: expected IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     │  └─ 4 ⇒ Unsupported schema version – unknown $schema value ✘ Unsupported schema version – unknown $schema value: expected IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
│     ├─ shouldThrowIllegalArgumentException_WhenJsonNodeIsNull() ✘ Calling getSchema with a null JsonNode should raise IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     └─ shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (8):
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):4 ⇒ URI with fragment identifier
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => com.networknt.schema.JsonSchemaException: Fragment #myFragment cannot be resolved
       com.networknt.schema.JsonSchema.updateThisAsSubschema(JsonSchema.java:171)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:113)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchemaFactory.doCreate(JsonSchemaFactory.java:338)
       com.networknt.schema.JsonSchemaFactory.newJsonSchema(JsonSchemaFactory.java:330)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:468)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldCreateSchemaSuccessfully(JsonSchemaFactory_getSchema_10_Test.java:138)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowIllegalArgumentException_WhenConfigIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowIllegalArgumentException_WhenConfigIsNull', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Calling getSchema with a null SchemaValidatorsConfig should raise IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldThrowIllegalArgumentException_WhenConfigIsNull(JsonSchemaFactory_getSchema_10_Test.java:127)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowIllegalArgumentException_WhenSchemaUriIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowIllegalArgumentException_WhenSchemaUriIsNull', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Calling getSchema with a null URI should raise IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldThrowIllegalArgumentException_WhenSchemaUriIsNull(JsonSchemaFactory_getSchema_10_Test.java:117)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):1 ⇒ Null handling – URI is null
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => org.opentest4j.AssertionFailedError: Null handling – URI is null: expected IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldThrowExceptionForInvalidInput(JsonSchemaFactory_getSchema_10_Test.java:148)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):2 ⇒ Null handling – JsonNode is null
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => org.opentest4j.AssertionFailedError: Null handling – JsonNode is null: expected IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldThrowExceptionForInvalidInput(JsonSchemaFactory_getSchema_10_Test.java:148)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because "schemaNode" is null
       com.networknt.schema.JsonSchemaFactory.findMetaSchemaForSchema(JsonSchemaFactory.java:347)
       com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonSchemaFactory.java:342)
       com.networknt.schema.JsonSchemaFactory.newJsonSchema(JsonSchemaFactory.java:328)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:468)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.lambda$shouldThrowExceptionForInvalidInput$3(JsonSchemaFactory_getSchema_10_Test.java:148)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):3 ⇒ Malformed URI – illegal characters
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => org.opentest4j.AssertionFailedError: Malformed URI – illegal characters: expected IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldThrowExceptionForInvalidInput(JsonSchemaFactory_getSchema_10_Test.java:148)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):4 ⇒ Unsupported schema version – unknown $schema value
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => org.opentest4j.AssertionFailedError: Unsupported schema version – unknown $schema value: expected IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldThrowExceptionForInvalidInput(JsonSchemaFactory_getSchema_10_Test.java:148)
       [...]
     Caused by: com.networknt.schema.JsonSchemaException: Unknown MetaSchema: https://example.com/unknown/schema
       com.networknt.schema.JsonSchemaFactory.lambda$fromId$0(JsonSchemaFactory.java:361)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       com.networknt.schema.JsonSchemaFactory.fromId(JsonSchemaFactory.java:361)
       java.base/java.util.concurrent.ConcurrentHashMap.computeIfAbsent(ConcurrentHashMap.java:1708)
       com.networknt.schema.JsonSchemaFactory.findMetaSchemaForSchema(JsonSchemaFactory.java:352)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowIllegalArgumentException_WhenJsonNodeIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowIllegalArgumentException_WhenJsonNodeIsNull', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Calling getSchema with a null JsonNode should raise IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.shouldThrowIllegalArgumentException_WhenJsonNodeIsNull(JsonSchemaFactory_getSchema_10_Test.java:122)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.get(String)" because "schemaNode" is null
       com.networknt.schema.JsonSchemaFactory.findMetaSchemaForSchema(JsonSchemaFactory.java:347)
       com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonSchemaFactory.java:342)
       com.networknt.schema.JsonSchemaFactory.newJsonSchema(JsonSchemaFactory.java:328)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:468)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.lambda$shouldThrowIllegalArgumentException_WhenJsonNodeIsNull$1(JsonSchemaFactory_getSchema_10_Test.java:122)
       [...]

Test run finished after 191 ms
[         6 containers found      ]
[         0 containers skipped    ]
[         6 containers started    ]
[         0 containers aborted    ]
[         6 containers successful ]
[         0 containers failed     ]
[        18 tests found           ]
[         0 tests skipped         ]
[        18 tests started         ]
[         0 tests aborted         ]
[        10 tests successful      ]
[         8 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`;
		method `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
