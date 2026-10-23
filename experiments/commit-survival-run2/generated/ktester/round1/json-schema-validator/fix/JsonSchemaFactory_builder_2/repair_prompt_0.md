
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    /**
     * A mock {@link JsonMetaSchema} used as the sole meta‑schema for the blueprint.
     */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /**
     * A mock {@link URIFactory} for a custom scheme.
     */
    @Mock
    private URIFactory mockUriFactory;

    /**
     * A mock {@link URIFetcher} for the same custom scheme.
     */
    @Mock
    private URIFetcher mockUriFetcher;

    /**
     * A mock {@link URITranslator} that could be added to the translator chain.
     */
    @Mock
    private URITranslator mockUriTranslator;

    /**
     * A mock {@link URNFactory} – the builder accepts {@code null} but we provide a mock
     *  to exercise the code path that stores it.
     */
    @Mock
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
    @Mock
    private URITranslator mockUriTranslator2;

    @BeforeEach
    void setUp() {
        // ----------------------------------------------------------------------
        // 1. Prepare a minimal but complete set of constructor arguments.
        // ----------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();
        // The meta‑schema map must contain an entry for the default meta‑schema URI.
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        // The factory normalises the URI, therefore we store the normalised key.
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), mockMetaSchema);
        // Simple URI mapping – can be empty, but we add one entry to make the test
        // observable.
        Map<String, String> uriMap = Collections.singletonMap("http", "https");
        // Build the blueprint factory using the public Builder API.
        blueprintFactory = JsonSchemaFactory.builder().objectMapper(jsonMapper).yamlMapper(yamlMapper).defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI).addMetaSchema(mockMetaSchema).addUriMappings(uriMap).addUriTranslator(mockUriTranslator).uriFactory(mockUriFactory, CUSTOM_SCHEME).uriFetcher(mockUriFetcher, CUSTOM_SCHEME).addUrnFactory(mockUrnFactory).build();
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
        // ----------------------------------------------------------------------
        // WHEN: we request a builder based on the blueprint factory
        // ----------------------------------------------------------------------
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        // ----------------------------------------------------------------------
        // THEN: the returned builder must be a distinct, non‑null instance
        // ----------------------------------------------------------------------
        assertNotNull(derivedBuilder, "The derived builder must not be null");
        assertNotSame(blueprintFactory, derivedBuilder, "The derived builder must not be the same instance as the blueprint factory");
        // ----------------------------------------------------------------------
        // AND: mutating the derived builder must not affect the original blueprint
        // ----------------------------------------------------------------------
        // a different mapper instance
        ObjectMapper newMapper = new ObjectMapper();
        derivedBuilder.objectMapper(newMapper);
        // Build a new factory from the mutated builder.
        JsonSchemaFactory mutatedFactory = derivedBuilder.build();
        // The blueprint's ObjectMapper should remain unchanged.
        // We can verify this indirectly by checking that the blueprint can still
        // create a schema from a simple JSON string – if the mapper were replaced,
        // the behaviour would still be correct, but the reference equality would
        // differ.  Since ObjectMapper does not expose identity via public API,
        // we simply ensure that the blueprint's factory is still functional.
        assertDoesNotThrow(() -> blueprintFactory.getSchema("{\"type\":\"string\"}"), "Blueprint factory must remain functional after the derived builder is mutated");
        assertDoesNotThrow(() -> mutatedFactory.getSchema("{\"type\":\"string\"}"), "Mutated factory must be functional with the new ObjectMapper");
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) can be used to create a fully functional factory")
    void testBuilderProducesUsableFactory() {
        // ----------------------------------------------------------------------
        // GIVEN: a builder derived from the blueprint
        // ----------------------------------------------------------------------
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);
        // ----------------------------------------------------------------------
        // WHEN: we build a new factory from the derived builder
        // ----------------------------------------------------------------------
        JsonSchemaFactory newFactory = derivedBuilder.build();
        // ----------------------------------------------------------------------
        // THEN: the new factory must be able to resolve the default meta‑schema URI
        // ----------------------------------------------------------------------
        // The public API does not expose the default meta‑schema directly,
        // but we can exercise it by creating a schema that references it via "$schema".
        String schemaWithMeta = String.format("{\"$schema\":\"%s\",\"type\":\"string\"}", DEFAULT_META_SCHEMA_URI);
        JsonSchema schema = newFactory.getSchema(schemaWithMeta);
        // The schema creation should not throw and the resulting schema must not be null.
        assertNotNull(schema, "Schema created from the new factory must not be null");
        // Validate a simple JSON instance against the created schema – it should succeed.
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
        JsonMetaSchema meta1 = mock(JsonMetaSchema.class);
        JsonMetaSchema meta2 = mock(JsonMetaSchema.class);
        when(meta1.getUri()).thenReturn("urn:meta:1");
        when(meta2.getUri()).thenReturn("urn:meta:2");
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
        JsonSchemaFactory factory = JsonSchemaFactory.builder().uriFactory(mockUriFactory, CUSTOM_SCHEME).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        URISchemeFactory schemeFactory = getPrivateField(derivedFactory, "uriFactory");
        // The factory provides a public accessor for its internal map via reflection.
        Map<String, URIFactory> map = getPrivateField(schemeFactory, "uriFactories");
        assertEquals(mockUriFactory, map.get(CUSTOM_SCHEME));
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies custom URIFetcher mappings")
    void testBuilderCopiesUriFetcherMappings() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().uriFetcher(mockUriFetcher, CUSTOM_SCHEME).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        URISchemeFetcher schemeFetcher = getPrivateField(derivedFactory, "uriFetcher");
        Map<String, URIFetcher> map = getPrivateField(schemeFetcher, "uriFetchers");
        assertEquals(mockUriFetcher, map.get(CUSTOM_SCHEME));
    }

    // ----------------------------------------------------------------------
    @Test
    @DisplayName("builder(JsonSchemaFactory) - copies URITranslators")
    void testBuilderCopiesUriTranslators() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().addUriTranslator(mockUriTranslator).addUriTranslator(mockUriTranslator2).build();
        JsonSchemaFactory derivedFactory = JsonSchemaFactory.builder(factory).build();
        CompositeURITranslator translator = getPrivateField(derivedFactory, "uriTranslators");
        List<URITranslator> list = getPrivateField(translator, "translators");
        assertEquals(2, list.size());
        assertSame(mockUriTranslator, list.get(0));
        assertSame(mockUriTranslator2, list.get(1));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:14: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:20: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:21: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:22: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:24: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:42: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:48: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:55: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:73: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:224: error: cannot find symbol
        JsonMetaSchema meta1 = mock(JsonMetaSchema.class);
                               ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:225: error: cannot find symbol
        JsonMetaSchema meta2 = mock(JsonMetaSchema.class);
                               ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:226: error: cannot find symbol
        when(meta1.getUri()).thenReturn("urn:meta:1");
        ^
  symbol:   method when(String)
  location: class JsonSchemaFactory_builder_2_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java:227: error: cannot find symbol
        when(meta2.getUri()).thenReturn("urn:meta:2");
        ^
  symbol:   method when(String)
  location: class JsonSchemaFactory_builder_2_Test
Note: src/test/java/com/networknt/schema/JsonSchemaFactory_builder_2_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
16 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchemaFactory.Builder', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tcom.networknt.schema.uri.URISchemeFetcher com.networknt.schema.JsonSchemaFactory.uriFetcher;\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonMetaSchema> com.networknt.schema.JsonSchemaFactory.jsonMetaSchemas;\n\t\tcom.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;\n\t\tcom.fasterxml.jackson.dataformat.yaml.YAMLMapper com.networknt.schema.JsonSchemaFactory.yamlMapper;\n\t\tjava.lang.String com.networknt.schema.JsonSchemaFactory.defaultMetaSchemaURI;\n\t\tcom.networknt.schema.uri.URITranslator.CompositeURITranslator com.networknt.schema.JsonSchemaFactory.uriTranslators;\n\t\tjava.util.Map<java.lang.String, java.lang.String> com.networknt.schema.JsonSchemaFactory.uriMap;\n\t\tcom.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;\n\t\tmethod `private JsonMetaSchema findMetaSchemaForSchema(JsonNode)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;\nclass `com.networknt.schema.CollectorContext`:\n\trelated functions:\n\t\tmethod ` void loadCollectors()` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`, api document: \n     * Loads data from all collectors.\n     ;\nclass `com.networknt.schema.JsonMetaSchema.Builder`:\n\trelated functions:\n\t\tmethod `private static Map<String, Keyword> createKeywordsMap(Map<String, Keyword>, Map<String, Format>)` returns `java.util.Map<java.lang.String, com.networknt.schema.Keyword>`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


