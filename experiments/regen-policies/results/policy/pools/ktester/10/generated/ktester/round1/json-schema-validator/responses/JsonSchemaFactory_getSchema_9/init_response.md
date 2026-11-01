```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, JsonNode)}.
 *
 * The test creates a concrete subclass of {@link JsonSchemaFactory} that overrides the
 * {@code newJsonSchema} protected method. This allows us to verify that the public
 * {@code getSchema} method delegates correctly without needing the full internal
 * implementation of {@code JsonSchema}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    // Mocks that are shared across all test methods
    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private JsonMetaSchema metaSchemaMock;

    // Instance under test – a spy of a concrete subclass that overrides newJsonSchema
    private TestableJsonSchemaFactory factory;

    // Captured arguments from the overridden newJsonSchema method
    private SchemaLocation capturedSchemaLocation;
    private JsonNode capturedJsonNode;
    private SchemaValidatorsConfig capturedConfig;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);

        // Build the minimal map required by the factory constructor
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), metaSchemaMock);

        // Builders can be empty – we rely on the defaults inside the factory
        SchemaLoaders.Builder loadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder mappersBuilder = new SchemaMappers.Builder();

        // Create the testable factory; it will capture the arguments passed to newJsonSchema
        factory = new TestableJsonSchemaFactory(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                loadersBuilder,
                mappersBuilder,
                metaSchemas,
                false,
                // The JsonSchema that newJsonSchema should return
                mock(JsonSchema.class)
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset captured values for the next test
        capturedSchemaLocation = null;
        capturedJsonNode = null;
        capturedConfig = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    @Test
    void getSchema_shouldDelegateToNewJsonSchema_andReturnResult() {
        // ----- Given ---------------------------------------------------------
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNode jsonNode = mock(JsonNode.class);
        SchemaValidatorsConfig config = null; // we are calling the overload without config

        // ----- When ----------------------------------------------------------
        JsonSchema result = factory.getSchema(schemaLocation, jsonNode);

        // ----- Then ----------------------------------------------------------
        assertNotNull(result, "Resulting JsonSchema must not be null");
        // The factory returns exactly the mock supplied by the overridden method
        assertSame(factory.getExpectedJsonSchema(), result, "Returned JsonSchema should be the one created by newJsonSchema");

        // Verify that the protected newJsonSchema method received the same arguments
        assertSame(schemaLocation, capturedSchemaLocation, "SchemaLocation passed to newJsonSchema must match");
        assertSame(jsonNode, capturedJsonNode, "JsonNode passed to newJsonSchema must match");
        assertSame(config, capturedConfig, "Config passed to newJsonSchema must match (null in this case)");
    }

    /**
     * Concrete subclass of {@link JsonSchemaFactory} that overrides {@code newJsonSchema}
     * so that we can control its output and capture the arguments it receives.
     */
    private class TestableJsonSchemaFactory extends JsonSchemaFactory {

        private final JsonSchema expectedJsonSchema;

        TestableJsonSchemaFactory(ObjectMapper jsonMapper,
                                  ObjectMapper yamlMapper,
                                  String defaultMetaSchemaURI,
                                  SchemaLoaders.Builder schemaLoadersBuilder,
                                  SchemaMappers.Builder schemaMappersBuilder,
                                  Map<String, JsonMetaSchema> jsonMetaSchemas,
                                  boolean enableUriSchemaCache,
                                  JsonSchema jsonSchemaToReturn) {
            super(jsonMapper,
                  yamlMapper,
                  defaultMetaSchemaURI,
                  schemaLoadersBuilder,
                  schemaMappersBuilder,
                  jsonMetaSchemas,
                  enableUriSchemaCache);
            this.expectedJsonSchema = jsonSchemaToReturn;
        }

        JsonSchema getExpectedJsonSchema() {
            return expectedJsonSchema;
        }

        @Override
        protected JsonSchema newJsonSchema(SchemaLocation schemaUri,
                                          JsonNode schemaNode,
                                          SchemaValidatorsConfig config) {
            // Capture the arguments for verification in the test method
            capturedSchemaLocation = schemaUri;
            capturedJsonNode = schemaNode;
            capturedConfig = config;
            // Return the pre‑configured mock
            return expectedJsonSchema;
        }
    }
}
```