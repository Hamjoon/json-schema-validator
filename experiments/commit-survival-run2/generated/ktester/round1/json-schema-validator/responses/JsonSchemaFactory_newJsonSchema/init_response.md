```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.CompositeURITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private JsonSchemaFactory factory;
    private ObjectMapper jsonMapper;
    private YAMLMapper yamlMapper;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // real mappers – they are lightweight and needed for JsonNode creation
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();

        // minimal but valid constructor arguments
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        URNFactory urnFactory = new URNFactory();               // default ctor (does nothing)
        CompositeURITranslator uriTranslators = new CompositeURITranslator();

        // a dummy meta‑schema entry – the factory only checks presence, not content
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);

        Map<String, String> uriMap = new HashMap<>();

        // use reflection because the constructor is private
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class,
                                         YAMLMapper.class,
                                         String.class,
                                         URISchemeFactory.class,
                                         URISchemeFetcher.class,
                                         URNFactory.class,
                                         Map.class,
                                         Map.class,
                                         boolean.class,
                                         CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(jsonMapper,
                                   yamlMapper,
                                   DEFAULT_META_SCHEMA_URI,
                                   uriFactory,
                                   uriFetcher,
                                   urnFactory,
                                   jsonMetaSchemas,
                                   uriMap,
                                   false,
                                   uriTranslators);
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // no global teardown needed
    }

    @Test
    void newJsonSchema_WithAllParameters_ShouldReturnJsonSchema() throws Exception {
        // given
        URI schemaUri = URI.create("http://example.com/schema");
        JsonNode schemaNode = jsonMapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when
        Method m = JsonSchemaFactory.class
                .getDeclaredMethod("newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        JsonSchema result = (JsonSchema) m.invoke(factory, schemaUri, schemaNode, config);

        // then
        assertNotNull(result, "newJsonSchema should return a non‑null JsonSchema instance");
    }

    @Test
    void newJsonSchema_WithNullUriAndConfig_ShouldReturnJsonSchema() throws Exception {
        // given
        JsonNode schemaNode = jsonMapper.readTree("{\"type\":\"string\"}");

        // when
        Method m = JsonSchemaFactory.class
                .getDeclaredMethod("newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        JsonSchema result = (JsonSchema) m.invoke(factory, null, schemaNode, null);

        // then
        assertNotNull(result, "newJsonSchema should handle null URI and null config and still return a JsonSchema");
    }
}
```