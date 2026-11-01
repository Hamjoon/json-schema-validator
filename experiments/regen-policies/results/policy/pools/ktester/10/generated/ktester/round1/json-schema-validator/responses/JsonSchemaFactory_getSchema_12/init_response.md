```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_12_Test {

    // ----- shared test data -------------------------------------------------
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    // ----- collaborators -----------------------------------------------------
    @Mock
    private ObjectMapper jsonMapperMock;   // from com.fasterxml.jackson.databind
    @Mock
    private ObjectMapper yamlMapperMock;   // from com.fasterxml.jackson.databind

    private static JsonSchemaFactory factory;

    private static SchemaLoaders.Builder schemaLoadersBuilder;
    private static SchemaMappers.Builder schemaMappersBuilder;
    private static Map<String, JsonMetaSchema> metaSchemas;

    // ------------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // Builders have public no‑arg constructors, no special configuration needed.
        schemaLoadersBuilder = new SchemaLoaders.Builder();
        schemaMappersBuilder = new SchemaMappers.Builder();

        // Prepare a minimal meta‑schema map – the factory only checks that the
        // default meta‑schema URI is present.  A simple mock is sufficient.
        JsonMetaSchema mockMetaSchema = mock(JsonMetaSchema.class);
        metaSchemas = new HashMap<>();
        metaSchemas.put(DEFAULT_META_SCHEMA_URI, mockMetaSchema);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create the factory instance with the mocked ObjectMappers and the
        // minimal meta‑schema map.  The boolean flag disables URI‑schema caching,
        // which is irrelevant for this unit test.
        factory = new JsonSchemaFactory(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                schemaLoadersBuilder,
                schemaMappersBuilder,
                metaSchemas,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    // ------------------------------------------------------------------------
    @Test
    void getSchema_WithLocationNodeAndConfig_ReturnsNonNullJsonSchema() {
        // ----- given ---------------------------------------------------------
        // Mock the required input objects.  Their internal behaviour is not
        // exercised by the method under test, so simple Mockito mocks are enough.
        SchemaLocation location = mock(SchemaLocation.class);
        JsonNode schemaNode = mock(JsonNode.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // ----- when ----------------------------------------------------------
        JsonSchema result = factory.getSchema(location, schemaNode, config);

        // ----- then ----------------------------------------------------------
        assertNotNull(result, "The returned JsonSchema should not be null");
        // Additional sanity check – the result should be an instance of JsonSchema.
        assertTrue(result instanceof JsonSchema, "Result should be a JsonSchema");
    }
}
```