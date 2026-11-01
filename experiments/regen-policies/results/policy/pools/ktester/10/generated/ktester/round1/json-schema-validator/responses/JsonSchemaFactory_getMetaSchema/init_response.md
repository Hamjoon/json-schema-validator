```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMetaSchema_Test {

    private static AutoCloseable mocks;

    private JsonSchemaFactory factory;
    private JsonMetaSchema mockMetaSchema;
    private Map<String, JsonMetaSchema> metaSchemas;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise Mockito annotations (needed for @Mock if used later)
        mocks = MockitoAnnotations.openMocks(JsonSchemaFactory_getMetaSchema_Test.class);
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Release Mockito resources
        if (mocks != null) {
            mocks.close();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mock the ObjectMappers – they are not used in the getMetaSchema logic we test
        ObjectMapper jsonMapper = mock(ObjectMapper.class);
        ObjectMapper yamlMapper = mock(ObjectMapper.class);

        // Builders for schema loaders / mappers – use the real empty builders
        SchemaLoaders.Builder loadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder mappersBuilder = new SchemaMappers.Builder();

        // Prepare a default meta‑schema entry (draft‑07) – required by the constructor
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        mockMetaSchema = mock(JsonMetaSchema.class);
        metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(defaultMetaSchemaURI), mockMetaSchema);

        // Create a concrete instance of JsonSchemaFactory, overriding the protected
        // loadMetaSchema method so we can control the result for a custom id.
        factory = new JsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                defaultMetaSchemaURI,
                loadersBuilder,
                mappersBuilder,
                metaSchemas,
                false) {

            @Override
            protected JsonMetaSchema loadMetaSchema(String id, SchemaValidatorsConfig config) {
                // For the purpose of the test we simply return a distinct mock
                // when a custom id is requested.
                JsonMetaSchema custom = mock(JsonMetaSchema.class);
                // Store it so that subsequent calls return the same instance
                metaSchemas.put(normalizeMetaSchemaUri(id), custom);
                return custom;
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // No special cleanup required for this test class
    }

    @Test
    void testGetMetaSchema_WithCustomId_UsesLoadMetaSchema() {
        // GIVEN
        String customId = "custom://my/meta/schema";
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(customId, config);

        // THEN
        assertNotNull(result, "Resulting JsonMetaSchema should not be null");
        // The factory should have stored the custom schema in its internal map
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(customId);
        assertSame(result, metaSchemas.get(normalized),
                "The returned schema should be the one stored in the internal map");
    }

    @Test
    void testGetMetaSchema_WithDefaultId_ReturnsPreloadedMetaSchema() {
        // GIVEN
        String defaultId = "http://json-schema.org/draft-07/schema#";
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(defaultId, config);

        // THEN
        assertNotNull(result, "Resulting JsonMetaSchema should not be null");
        // It should be the same instance that we placed into the map during setup
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(defaultId);
        assertSame(metaSchemas.get(normalized), result,
                "The default meta‑schema should be retrieved from the pre‑populated map");
    }
}
```