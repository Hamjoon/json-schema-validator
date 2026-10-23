


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    /**
     * A mock {@link JsonMetaSchema} used as the sole meta‑schema for the blueprint.
     */
//     @Mock
    private JsonMetaSchema mockMetaSchema;

    /**
     * A mock {@link URIFactory} for a custom scheme.
     */
//     @Mock
    private URIFactory mockUriFactory;

    /**
     * A mock {@link URIFetcher} for the same custom scheme.
     */
//     @Mock
    private URIFetcher mockUriFetcher;

    /**
     * A mock {@link URITranslator} that could be added to the translator chain.
     */
//     @Mock
    private URITranslator mockUriTranslator;

    /**
     * A mock {@link URNFactory} – the builder accepts {@code null} but we provide a mock
     *  to exercise the code path that stores it.
     */
//     @Mock
    private URNFactory mockUrnFactory;

    /**
     * Blueprint factory that will be passed to {@code JsonSchemaFactory.builder(…) }
     */
    private JsonSchemaFactory blueprintFactory;

    /**
     * Common constants used throughout the tests.
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta";

    private static final String CUSTOM_SCHEME = "custom";

    /**
     * A second mock {@link URITranslator} – useful for order‑checking tests.
     */
//     @Mock
    private URITranslator mockUriTranslator2;

    private final DummyURIFactory dummyUriFactory = new DummyURIFactory();

    private final DummyURIFetcher dummyUriFetcher = new DummyURIFetcher();

    private final DummyURITranslator dummyUriTranslator = new DummyURITranslator();

    private final DummyURITranslator dummyUriTranslator2 = new DummyURITranslator();

    private final DummyURNFactory dummyUrnFactory = new DummyURNFactory();

    private static class DummyURIFactory implements URIFactory {

        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }

        // Some versions of the library declare a two‑argument method.
        // We keep it without @Override to stay compatible with both versions.
        public URI create(URI base, String uri) {
            // Resolve against the base if provided, otherwise treat as absolute.
            return (base == null) ? URI.create(uri) : base.resolve(uri);
        }
    }

    private static class DummyURIFetcher implements URIFetcher {

        @Override
        public java.io.InputStream fetch(URI uri) {
            // not used in the tests
            return null;
        }
    }

    private static class DummyURITranslator implements URITranslator {

        @Override
        public URI translate(URI uri) {
            // identity translator
            return uri;
        }
    }

    private static class DummyURNFactory implements URNFactory {

        @Override
        public URI create(String urn) {
            return URI.create(urn);
        }
    }

    /**
     * A very small {@link JsonMetaSchema} implementation that only provides a
     * URI. All other methods throw {@link UnsupportedOperationException} because
     * they are never exercised by the tests.
     */
    private static class SimpleMetaSchema extends JsonMetaSchema {

        private final String uri;

        SimpleMetaSchema(String uri) {
            // JsonMetaSchema constructor: (String id, String uri, Map<String, Keyword> keywords)
            super(uri, uri, Collections.emptyMap());
            this.uri = uri;
        }

        @Override
        public String getUri() {
            return uri;
        }

//         @Override
//         public JsonSchemaFactory getFactory() {
//             throw new UnsupportedOperationException();
//         }

//         @Override
//         public JsonMetaSchema.Builder getBuilder() {
//             throw new UnsupportedOperationException();
//         }
    }

    @BeforeEach
    void setUp() {
        // ------------------------------------------------------------------
        // 1. Prepare a minimal but complete set of constructor arguments.
        // ------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();
        // The meta‑schema map must contain an entry for the default meta‑schema URI.
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), new SimpleMetaSchema(DEFAULT_META_SCHEMA_URI));
        // Simple URI mapping – can be empty, but we add one entry to make the test
        // observable.
        Map<String, String> uriMap = Collections.singletonMap("http", "https");
        // Build the blueprint factory using the public Builder API.
        blueprintFactory = JsonSchemaFactory.builder().objectMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).addMetaSchema(new SimpleMetaSchema(DEFAULT_META_SCHEMA_URI)).addUriMappings(uriMap).addUriTranslator(dummyUriTranslator).uriFactory(dummyUriFactory, CUSTOM_SCHEME).uriFetcher(dummyUriFetcher, CUSTOM_SCHEME).addUrnFactory(dummyUrnFactory).build();
        // Sanity check – the blueprint must be usable; any exception here would
        // indicate a problem with the test setup rather than the method under test.
        assertNotNull(blueprintFactory, "Blueprint factory should be created successfully");
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) returns a new Builder pre‑populated with blueprint data")
    void testBuilderCopiesBlueprintConfiguration() {
        // WHEN: we request a builder based on the blueprint factory
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        // THEN: the returned builder must be a distinct, non‑null instance
        assertNotNull(derivedBuilder, "The derived builder must not be null");
        assertNotSame(blueprintFactory, derivedBuilder, "The derived builder must not be the same instance as the blueprint factory");
        // AND: mutating the derived builder must not affect the original blueprint
        // a different mapper instance
        ObjectMapper newMapper = new ObjectMapper();
        derivedBuilder.objectMapper(newMapper);
        // Build a new factory from the mutated builder.
        JsonSchemaFactory mutatedFactory = derivedBuilder.build();
        // The blueprint's ObjectMapper should remain unchanged.
        assertDoesNotThrow(() -> blueprintFactory.getSchema("{\"type\":\"string\"}"), "Blueprint factory must remain functional after the derived builder is mutated");
        assertDoesNotThrow(() -> mutatedFactory.getSchema("{\"type\":\"string\"}"), "Mutated factory must be functional with the new ObjectMapper");
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) can be used to create a fully functional factory")
    void testBuilderProducesUsableFactory() {
        // GIVEN: a builder derived from the blueprint
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        // WHEN: we build a new factory from the derived builder
        JsonSchemaFactory newFactory = derivedBuilder.build();
        // THEN: the new factory must be able to resolve the default meta‑schema URI
        String schemaWithMeta = String.format("{\"$schema\":\"%s\",\"type\":\"string\"}", DEFAULT_META_SCHEMA_URI);
        JsonSchema schema = newFactory.getSchema(schemaWithMeta);
        assertNotNull(schema, "Schema created from the new factory must not be null");
        assertDoesNotThrow(() -> schema.validate(new ObjectMapper().readTree("\"hello\"")), "Validation of a valid instance must not throw");
    }

    // ----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - handling of empty blueprint")
    void testBuilderWithEmptyBlueprint() {
        JsonSchemaFactory emptyBlueprint = JsonSchemaFactory.builder().build();
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(emptyBlueprint);
        JsonSchemaFactory derivedFactory = derivedBuilder.build();
        // Verify that meta‑schema map is empty
        Map<String, JsonMetaSchema> metaMap = getPrivateField(derivedFactory, "jsonMetaSchemas");
        assertTrue(metaMap.isEmpty(), "Meta‑schema map should be empty for an empty blueprint");
        // defaultMetaSchemaURI must be null
        assertNull(getPrivateField(derivedFactory, "defaultMetaSchemaURI"));
        // ObjectMapper/YAMLMapper must be the SAME instances as in the empty blueprint
        assertSame(getPrivateField(emptyBlueprint, "jsonMapper"), getPrivateField(derivedFactory, "jsonMapper"), "ObjectMapper instance should be copied by reference");
        assertSame(getPrivateField(emptyBlueprint, "yamlMapper"), getPrivateField(derivedFactory, "yamlMapper"), "YAMLMapper instance should be copied by reference");
        // uriMap must be empty
        Map<String, String> uriMap = getPrivateField(derivedFactory, "uriMap");
        assertTrue(uriMap.isEmpty(), "URI map should be empty");
        // Translators list must be empty (apart from the default CompositeURITranslator internals)
        CompositeURITranslator translator = getPrivateField(derivedFactory, "uriTranslators");
        List<URITranslator> innerList = getPrivateField(translator, "translators");
        assertTrue(innerList.isEmpty(), "No custom URITranslators should be present");
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies defaultMetaSchemaURI")
    void testBuilderCopiesDefaultMetaSchemaURI() {
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder().defaultMetaSchemaURI("https://example.com/meta").build();
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(customFactory);
        JsonSchemaFactory derivedFactory = derivedBuilder.build();
        String copiedUri = getPrivateField(derivedFactory, "defaultMetaSchemaURI");
        assertEquals("https://example.com/meta", copiedUri);
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom ObjectMapper and YAMLMapper instances")
    void testBuilderCopiesCustomMappers() {
        ObjectMapper customMapper = new ObjectMapper();
        YAMLMapper customYaml = new YAMLMapper();
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder().objectMapper(customMapper).yamlMapper(customYaml).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(customFactory).build();
        assertSame(customMapper, getPrivateField(derivedFactory, "jsonMapper"));
        assertSame(customYaml, getPrivateField(derivedFactory, "yamlMapper"));
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies added meta‑schemas")
    void testBuilderCopiesMetaSchemas() {
        SimpleMetaSchema meta1 = new SimpleMetaSchema("urn:meta:1");
        SimpleMetaSchema meta2 = new SimpleMetaSchema("urn:meta:2");
        JsonSchemaFactory factory = JsonSchemaFactory.builder().addMetaSchema(meta1).addMetaSchema(meta2).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        Map<String, JsonMetaSchema> metaMap = getPrivateField(derivedFactory, "jsonMetaSchemas");
        assertEquals(2, metaMap.size());
        assertSame(meta1, metaMap.get(JsonSchemaFactory.normalizeMetaSchemaUri("urn:meta:1")));
        assertSame(meta2, metaMap.get(JsonSchemaFactory.normalizeMetaSchemaUri("urn:meta:2")));
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies deprecated URI mappings (uriMap)")
    void testBuilderCopiesUriMappings() {
        Map<String, String> map = new HashMap<>();
        map.put("http://old.com", "http://new.com");
        JsonSchemaFactory factory = JsonSchemaFactory.builder().addUriMappings(map).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        Map<String, String> copiedMap = getPrivateField(derivedFactory, "uriMap");
        assertEquals(map, copiedMap);
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom URIFactory mappings")
    void testBuilderCopiesUriFactoryMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().uriFactory(dummyUriFactory, CUSTOM_SCHEME).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        URISchemeFactory schemeFactory = getPrivateField(derivedFactory, "uriFactory");
        Map<String, URIFactory> map = getPrivateField(schemeFactory, "uriFactories");
        assertEquals(dummyUriFactory, map.get(CUSTOM_SCHEME));
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom URIFetcher mappings")
    void testBuilderCopiesUriFetcherMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().uriFetcher(dummyUriFetcher, CUSTOM_SCHEME).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        URISchemeFetcher schemeFetcher = getPrivateField(derivedFactory, "uriFetcher");
        Map<String, URIFetcher> map = getPrivateField(schemeFetcher, "uriFetchers");
        assertEquals(dummyUriFetcher, map.get(CUSTOM_SCHEME));
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies URITranslators")
    void testBuilderCopiesUriTranslators() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().addUriTranslator(dummyUriTranslator).addUriTranslator(dummyUriTranslator2).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        CompositeURITranslator translator = getPrivateField(derivedFactory, "uriTranslators");
        List<URITranslator> list = getPrivateField(translator, "translators");
        assertEquals(2, list.size());
        assertSame(dummyUriTranslator, list.get(0));
        assertSame(dummyUriTranslator2, list.get(1));
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

20:43:33.561 [main] WARN com.networknt.schema.JsonMetaSchema -- Unknown keyword $schema - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
20:43:33.562 [main] WARN com.networknt.schema.JsonMetaSchema -- Unknown keyword type - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_builder_2_Test ✔
│     ├─ builder(JsonSchemaFactory) - copies deprecated URI mappings (uriMap) ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builder(JsonSchemaFactory) can be used to create a fully functional factory ✔
│     ├─ builder(JsonSchemaFactory) - copies URITranslators ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builder(JsonSchemaFactory) - handling of empty blueprint ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builder(JsonSchemaFactory) - copies added meta‑schemas ✘ Wrong MetaSchema URI: urn:meta:1
│     ├─ builder(JsonSchemaFactory) returns a new Builder pre‑populated with blueprint da... ✘ Blueprint factory must remain functional after the derived builder is mutated ==> Unexpected exception thrown: com.networknt.schema.JsonSchemaException: Unknown MetaSchema: http://example.com/meta
│     ├─ builder(JsonSchemaFactory) - copies custom URIFactory mappings ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builder(JsonSchemaFactory) - copies defaultMetaSchemaURI ✘ Json Meta Schemas must not be null or empty
│     ├─ builder(JsonSchemaFactory) - copies custom ObjectMapper and YAMLMapper instances ✘ defaultMetaSchemaURI must not be null or empty
│     └─ builder(JsonSchemaFactory) - copies custom URIFetcher mappings ✘ defaultMetaSchemaURI must not be null or empty
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (9):
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - copies deprecated URI mappings (uriMap)
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesUriMappings', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderCopiesUriMappings(JsonSchemaFactory_builder_2_Test.java:294)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - copies URITranslators
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesUriTranslators', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderCopiesUriTranslators(JsonSchemaFactory_builder_2_Test.java:326)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - handling of empty blueprint
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderWithEmptyBlueprint', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderWithEmptyBlueprint(JsonSchemaFactory_builder_2_Test.java:231)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - copies added meta‑schemas
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesMetaSchemas', methodParameterTypes = '']
    => com.networknt.schema.JsonSchemaException: Wrong MetaSchema URI: urn:meta:1
       com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.java:514)
       com.networknt.schema.JsonSchemaFactory$Builder.addMetaSchema(JsonSchemaFactory.java:132)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderCopiesMetaSchemas(JsonSchemaFactory_builder_2_Test.java:280)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) returns a new Builder pre‑populated with blueprint data
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesBlueprintConfiguration', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Blueprint factory must remain functional after the derived builder is mutated ==> Unexpected exception thrown: com.networknt.schema.JsonSchemaException: Unknown MetaSchema: http://example.com/meta
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertDoesNotThrow.createAssertionFailedError(AssertDoesNotThrow.java:84)
       org.junit.jupiter.api.AssertDoesNotThrow.assertDoesNotThrow(AssertDoesNotThrow.java:75)
       org.junit.jupiter.api.AssertDoesNotThrow.assertDoesNotThrow(AssertDoesNotThrow.java:62)
       org.junit.jupiter.api.Assertions.assertDoesNotThrow(Assertions.java:3217)
       [...]
     Caused by: com.networknt.schema.JsonSchemaException: Unknown MetaSchema: http://example.com/meta
       com.networknt.schema.JsonSchemaFactory.lambda$fromId$0(JsonSchemaFactory.java:361)
       java.base/java.util.Optional.orElseThrow(Optional.java:403)
       com.networknt.schema.JsonSchemaFactory.fromId(JsonSchemaFactory.java:361)
       java.base/java.util.concurrent.ConcurrentHashMap.computeIfAbsent(ConcurrentHashMap.java:1708)
       com.networknt.schema.JsonSchemaFactory.findMetaSchemaForSchema(JsonSchemaFactory.java:352)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - copies custom URIFactory mappings
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesUriFactoryMappings', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderCopiesUriFactoryMappings(JsonSchemaFactory_builder_2_Test.java:304)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - copies defaultMetaSchemaURI
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesDefaultMetaSchemaURI', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderCopiesDefaultMetaSchemaURI(JsonSchemaFactory_builder_2_Test.java:255)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - copies custom ObjectMapper and YAMLMapper instances
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesCustomMappers', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderCopiesCustomMappers(JsonSchemaFactory_builder_2_Test.java:268)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_2_Test:builder(JsonSchemaFactory) - copies custom URIFetcher mappings
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_2_Test', methodName = 'testBuilderCopiesUriFetcherMappings', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_2_Test.testBuilderCopiesUriFetcherMappings(JsonSchemaFactory_builder_2_Test.java:315)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]

Test run finished after 134 ms
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
[         1 tests successful      ]
[         9 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

return type: 
com.networknt.schema.JsonSchemaFactory.Builder

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		com.networknt.schema.uri.URISchemeFetcher com.networknt.schema.JsonSchemaFactory.uriFetcher;
		java.util.Map<java.lang.String, com.networknt.schema.JsonMetaSchema> com.networknt.schema.JsonSchemaFactory.jsonMetaSchemas;
		com.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;
		com.fasterxml.jackson.dataformat.yaml.YAMLMapper com.networknt.schema.JsonSchemaFactory.yamlMapper;
		java.lang.String com.networknt.schema.JsonSchemaFactory.defaultMetaSchemaURI;
		com.networknt.schema.uri.URITranslator.CompositeURITranslator com.networknt.schema.JsonSchemaFactory.uriTranslators;
		java.util.Map<java.lang.String, java.lang.String> com.networknt.schema.JsonSchemaFactory.uriMap;
		com.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;
	related functions:
		method `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;
		method `private JsonMetaSchema findMetaSchemaForSchema(JsonNode)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;
class `com.networknt.schema.CollectorContext`:
	related functions:
		method ` void loadCollectors()` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`, api document: 
     * Loads data from all collectors.
     ;
class `com.networknt.schema.JsonMetaSchema.Builder`:
	related functions:
		method `private static Map<String, Keyword> createKeywordsMap(Map<String, Keyword>, Map<String, Format>)` returns `java.util.Map<java.lang.String, com.networknt.schema.Keyword>`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;
class `com.networknt.schema.PatternPropertiesValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
