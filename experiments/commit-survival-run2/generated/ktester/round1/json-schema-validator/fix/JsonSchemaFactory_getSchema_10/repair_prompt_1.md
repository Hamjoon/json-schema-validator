
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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.urn.URNFactory;

@ExtendWith(MockitoExtension.class)
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

    private URISchemeFactory uriFactory;

    private URISchemeFetcher uriFetcher;

    private URNFactory urnFactory;

    private CompositeURITranslator uriTranslators;

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
        /* -------------------------------------------------
         * 1️⃣  Build the constructor arguments for JsonSchemaFactory
         * ------------------------------------------------- */
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();
        // Minimal (empty) but non‑null maps for the scheme factories/fetchers
        uriFactory = new URISchemeFactory(Collections.emptyMap());
        uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        // URNFactory has a public no‑arg constructor
        urnFactory = new URNFactory();
        // Composite translator – we do not need any real translators for the test
        uriTranslators = new CompositeURITranslator();
        // JsonMetaSchema map must contain an entry for the default meta‑schema URI.
        // We can mock JsonMetaSchema because the factory never inspects its content
        // during the execution of getSchema(...).
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);
        // Empty URI mapping and cache flag – not relevant for this unit test
        uriMap = Collections.emptyMap();
        boolean enableUriSchemaCache = false;
        /* -------------------------------------------------
         * 2️⃣  Create a real factory instance and then spy it
         * ------------------------------------------------- */
        JsonSchemaFactory realFactory = new JsonSchemaFactory(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
        // Spy allows us to stub the protected newJsonSchema method without touching private fields
        factorySpy = Mockito.spy(realFactory);
        // Stub the protected method to return a mock JsonSchema – this isolates the test
        JsonSchema mockSchema = mock(JsonSchema.class);
        doReturn(mockSchema).when(factorySpy).newJsonSchema(any(URI.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));
        /* -------------------------------------------------
         * 3️⃣  Prepare the parameters for the focal method
         * ------------------------------------------------- */
        sampleUri = new URI("http://example.com/my-schema.json");
        sampleNode = jsonMapper.readTree("{\"type\":\"object\"}");
        config = new SchemaValidatorsConfig();
        /*
         * Build a real factory instance using the public API.
         * JsonSchemaFactory provides a static builder that can be customised.
         */
        factory = JsonSchemaFactory.builder().objectMapper(new ObjectMapper()).yamlMapper(new YAMLMapper()).defaultMetaSchemaUri(DEFAULT_META_SCHEMA_URI).build();
        // Sample inputs for the basic test
        sampleUri = new URI("http://example.com/my-schema.json");
        sampleNode = new ObjectMapper().readTree("{\"type\":\"object\"}");
        config = new SchemaValidatorsConfig();
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(Arguments.of("Valid inputs – simple object schema", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("Edge case – Empty JSON object as schema", URI.create("http://example.com/empty"), readJson("{}"), new SchemaValidatorsConfig()), Arguments.of("Valid inputs – custom configuration applied", URI.create("http://example.com/custom-config"), readJson("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}"), customConfig()), Arguments.of("URI with fragment identifier", URI.create("http://example.com/schema#myFragment"), readJson("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("URI with query parameters (null config)", URI.create("http://example.com/schema?version=1&debug=true"), readJson("{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"), null), Arguments.of("Non‑object JsonNode – array schema", URI.create("http://example.com/array-schema"), readJson("[{\"type\":\"string\"},{\"type\":\"integer\"}]"), new SchemaValidatorsConfig()), Arguments.of("File scheme URI (null config)", URI.create("file:/tmp/test-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"path\":{\"type\":\"string\"}}}"), null), Arguments.of("Meta‑schema URI (draft‑07)", URI.create("http://json-schema.org/draft-07/schema#"), readJson("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig()), Arguments.of("Relative URI resolved against base", URI.create("schemas/relative-schema.json"), readJson("{\"type\":\"object\",\"properties\":{\"rel\":{\"type\":\"string\"}}}"), new SchemaValidatorsConfig()), Arguments.of("Null handling – Config is null (default config used)", URI.create("http://example.com/schema"), readJson("{\"type\":\"object\"}"), null));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of("Null handling – URI is null", null, readJson("{\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class), Arguments.of("Null handling – JsonNode is null", URI.create("http://example.com/schema"), null, new SchemaValidatorsConfig(), IllegalArgumentException.class), // The factory is expected to replace a null config with a default,
        // therefore the following case is covered by a successful test.
        Arguments.of("Malformed URI – illegal characters", createSafeUri("http://exa mple.com/ bad"), readJson("{\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class), Arguments.of("Unsupported schema version – unknown $schema value", URI.create("http://example.com/unknown-schema"), readJson("{\"$schema\":\"http://example.com/unknown/schema#\",\"type\":\"object\"}"), new SchemaValidatorsConfig(), IllegalArgumentException.class));
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:38: error: cannot find symbol
    private URISchemeFactory uriFactory;
            ^
  symbol:   class URISchemeFactory
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:40: error: cannot find symbol
    private URISchemeFetcher uriFetcher;
            ^
  symbol:   class URISchemeFetcher
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:23: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:79: error: cannot find symbol
        uriFactory = new URISchemeFactory(Collections.emptyMap());
                         ^
  symbol:   class URISchemeFactory
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:80: error: cannot find symbol
        uriFetcher = new URISchemeFetcher(Collections.emptyMap());
                         ^
  symbol:   class URISchemeFetcher
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:82: error: URNFactory is abstract; cannot be instantiated
        urnFactory = new URNFactory();
                     ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:88: error: cannot find symbol
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
                                         ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:99: error: cannot find symbol
        factorySpy = Mockito.spy(realFactory);
                     ^
  symbol:   variable Mockito
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:101: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:102: error: cannot find symbol
        doReturn(mockSchema).when(factorySpy).newJsonSchema(any(URI.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:102: error: cannot find symbol
        doReturn(mockSchema).when(factorySpy).newJsonSchema(any(URI.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));
                                                            ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:102: error: cannot find symbol
        doReturn(mockSchema).when(factorySpy).newJsonSchema(any(URI.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));
                                                                            ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:102: error: cannot find symbol
        doReturn(mockSchema).when(factorySpy).newJsonSchema(any(URI.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));
                                                                                                 ^
  symbol:   method any(Class<SchemaValidatorsConfig>)
  location: class JsonSchemaFactory_getSchema_10_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_10_Test.java:113: error: cannot find symbol
        factory = JsonSchemaFactory.builder().objectMapper(new ObjectMapper()).yamlMapper(new YAMLMapper()).defaultMetaSchemaUri(DEFAULT_META_SCHEMA_URI).build();
                                                                                                           ^
  symbol:   method defaultMetaSchemaUri(String)
  location: class Builder
15 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


