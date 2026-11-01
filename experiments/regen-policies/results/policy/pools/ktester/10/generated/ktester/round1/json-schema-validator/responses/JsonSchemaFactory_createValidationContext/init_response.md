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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#createValidationContext(JsonNode, SchemaValidatorsConfig)}.
 * The protected method is exercised via a test‑specific subclass that exposes it as a public method.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    // ----- Mocks & test data -------------------------------------------------
    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private JsonMetaSchema metaSchemaMock;
    @Mock
    private JsonNode schemaNodeMock;

    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/draft-07/schema";

    private JsonSchemaFactory factory;          // the concrete factory under test
    private SchemaValidatorsConfig config;      // simple config instance

    // ----- Helper subclass ---------------------------------------------------
    /**
     * Subclass that simply forwards the protected {@code createValidationContext}
     * method so the test can call it directly.
     */
    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {
        TestableJsonSchemaFactory(ObjectMapper jsonMapper,
                                 ObjectMapper yamlMapper,
                                 String defaultMetaSchemaURI,
                                 SchemaLoaders.Builder schemaLoadersBuilder,
                                 SchemaMappers.Builder schemaMappersBuilder,
                                 Map<String, JsonMetaSchema> jsonMetaSchemas,
                                 boolean enableUriSchemaCache) {
            super(jsonMapper, yamlMapper, defaultMetaSchemaURI,
                  schemaLoadersBuilder, schemaMappersBuilder,
                  jsonMetaSchemas, enableUriSchemaCache);
        }

        ValidationContext exposeCreateValidationContext(JsonNode schemaNode,
                                                         SchemaValidatorsConfig cfg) {
            return super.createValidationContext(schemaNode, cfg);
        }
    }

    // ----- Lifecycle ---------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled by MockitoExtension, but kept for clarity)
        MockitoAnnotations.openMocks(this);

        // Prepare a minimal map of meta‑schemas required by the factory constructor.
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        // The factory normalises the URI via JsonSchemaFactory.normalizeMetaSchemaUri.
        // For the purpose of the test we use the same literal string.
        metaSchemaMap.put(DEFAULT_META_SCHEMA_URI, metaSchemaMock);

        // Build the factory instance.
        factory = new TestableJsonSchemaFactory(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                metaSchemaMap,
                false   // URI schema cache disabled for simplicity
        );

        // Simple (empty) configuration object.
        config = new SchemaValidatorsConfig();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(jsonMapperMock, yamlMapperMock, metaSchemaMock, schemaNodeMock);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    // ----- Tests -------------------------------------------------------------
    @Test
    void createValidationContext_returnsContextWithExpectedComponents() {
        // Act: invoke the protected method via the testable subclass.
        ValidationContext ctx = ((TestableJsonSchemaFactory) factory)
                .exposeCreateValidationContext(schemaNodeMock, config);

        // Assert: the returned context must not be null.
        assertNotNull(ctx, "ValidationContext should not be null");

        // The context should hold the same factory instance that created it.
        assertSame(factory, ctx.getFactory(),
                "ValidationContext should reference the originating JsonSchemaFactory");

        // The supplied configuration must be stored unchanged.
        assertSame(config, ctx.getConfig(),
                "ValidationContext should contain the provided SchemaValidatorsConfig");

        // The meta‑schema resolved for the supplied schema node must be the one
        // we placed in the factory's meta‑schema map.
        assertSame(metaSchemaMock, ctx.getJsonMetaSchema(),
                "ValidationContext should contain the expected JsonMetaSchema");
    }
}
```