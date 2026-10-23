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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema";

    private JsonSchemaFactory factory;
    private ObjectMapper jsonMapper;
    private YAMLMapper yamlMapper;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class
    }

    @BeforeEach
    void setupBeforeEach() {
        // Real mappers – they are lightweight and safe to use in unit tests
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();

        // Minimal but valid constructor arguments
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        URNFactory urnFactory = new URNFactory();

        // The factory expects a map that contains a JsonMetaSchema for the default meta‑schema URI.
        // We can safely use a mock because the test only verifies that getSchema returns a JsonSchema
        // instance; the internal meta‑schema handling is not exercised here.
        JsonMetaSchema mockMetaSchema = org.mockito.Mockito.mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(DEFAULT_META_SCHEMA_URI, mockMetaSchema);

        // Empty URI map – not needed for this scenario
        Map<String, String> uriMap = Collections.emptyMap();

        // No caching for the test
        boolean enableUriSchemaCache = false;

        CompositeURITranslator uriTranslators = new CompositeURITranslator();

        // Build the factory instance with the prepared arguments
        factory = new JsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                uriFactory,
                uriFetcher,
                urnFactory,
                jsonMetaSchemas,
                uriMap,
                enableUriSchemaCache,
                uriTranslators
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    @Test
    void getSchema_withValidJsonNodeAndConfig_returnsJsonSchema() throws Exception {
        // given
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonNode schemaNode = jsonMapper.readTree(schemaJson);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when
        JsonSchema result = factory.getSchema(schemaNode, config);

        // then
        assertNotNull(result, "The returned JsonSchema should not be null");
        assertTrue(result instanceof JsonSchema, "The returned object should be an instance of JsonSchema");
    }
}
```