


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

// @ExtendWith(MockitoExtension.class)
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

//     private URISchemeFactory uriFactory;

//     private URISchemeFetcher uriFetcher;

//     private URNFactory urnFactory;

//     private CompositeURITranslator uriTranslators;

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
        // Build a factory using the public builder API.
        factory = JsonSchemaFactory.builder().objectMapper(new ObjectMapper()).yamlMapper(new YAMLMapper()).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).build();
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
│     │  ├─ 1 ⇒ Valid inputs – simple object schema ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 2 ⇒ Edge case – Empty JSON object as schema ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 3 ⇒ Valid inputs – custom configuration applied ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 4 ⇒ URI with fragment identifier ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 5 ⇒ URI with query parameters (null config) ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 6 ⇒ Non‑object JsonNode – array schema ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 7 ⇒ File scheme URI (null config) ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 8 ⇒ Meta‑schema URI (draft‑07) ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 9 ⇒ Relative URI resolved against base ✘ Json Meta Schemas must not be null or empty
│     │  └─ 10 ⇒ Null handling – Config is null (default config used) ✘ Json Meta Schemas must not be null or empty
│     ├─ shouldThrowIllegalArgumentException_WhenConfigIsNull() ✘ Json Meta Schemas must not be null or empty
│     ├─ shouldThrowIllegalArgumentException_WhenSchemaUriIsNull() ✘ Json Meta Schemas must not be null or empty
│     ├─ shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfi... ✔
│     │  ├─ 1 ⇒ Null handling – URI is null ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 2 ⇒ Null handling – JsonNode is null ✘ Json Meta Schemas must not be null or empty
│     │  ├─ 3 ⇒ Malformed URI – illegal characters ✘ Json Meta Schemas must not be null or empty
│     │  └─ 4 ⇒ Unsupported schema version – unknown $schema value ✘ Json Meta Schemas must not be null or empty
│     ├─ shouldThrowIllegalArgumentException_WhenJsonNodeIsNull() ✘ Json Meta Schemas must not be null or empty
│     └─ shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig() ✘ Json Meta Schemas must not be null or empty
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (18):
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):1 ⇒ Valid inputs – simple object schema
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):2 ⇒ Edge case – Empty JSON object as schema
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):3 ⇒ Valid inputs – custom configuration applied
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):4 ⇒ URI with fragment identifier
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):5 ⇒ URI with query parameters (null config)
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):6 ⇒ Non‑object JsonNode – array schema
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):7 ⇒ File scheme URI (null config)
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):8 ⇒ Meta‑schema URI (draft‑07)
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):9 ⇒ Relative URI resolved against base
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldCreateSchemaSuccessfully(String, URI, JsonNode, SchemaValidatorsConfig):10 ⇒ Null handling – Config is null (default config used)
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldCreateSchemaSuccessfully', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowIllegalArgumentException_WhenConfigIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowIllegalArgumentException_WhenConfigIsNull', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowIllegalArgumentException_WhenSchemaUriIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowIllegalArgumentException_WhenSchemaUriIsNull', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):1 ⇒ Null handling – URI is null
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):2 ⇒ Null handling – JsonNode is null
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):3 ⇒ Malformed URI – illegal characters
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowExceptionForInvalidInput(String, URI, JsonNode, SchemaValidatorsConfig, Class):4 ⇒ Unsupported schema version – unknown $schema value
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowExceptionForInvalidInput', methodParameterTypes = 'java.lang.String, java.net.URI, com.fasterxml.jackson.databind.JsonNode, com.networknt.schema.SchemaValidatorsConfig, java.lang.Class']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldThrowIllegalArgumentException_WhenJsonNodeIsNull()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldThrowIllegalArgumentException_WhenJsonNodeIsNull', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       jdk.internal.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getSchema_10_Test:shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getSchema_10_Test', methodName = 'shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_getSchema_10_Test.setupBeforeEach(JsonSchemaFactory_getSchema_10_Test.java:67)
       jdk.internal.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]

Test run finished after 126 ms
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
[         0 tests successful      ]
[        18 tests failed          ]


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
