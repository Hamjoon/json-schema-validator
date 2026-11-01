```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(InputStream)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_2_Test {

    /** Shared objects for all tests */
    private static JsonSchemaFactory factory;
    private static ObjectMapper jsonMapper;
    private static ObjectMapper yamlMapper;
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";
    private static Map<String, JsonMetaSchema> metaSchemaMap;

    @BeforeAll
    static void setupBeforeAll() {
        // Basic mappers – the real implementation works fine for the test
        jsonMapper = new ObjectMapper();
        yamlMapper = new ObjectMapper(); // In production a YAML mapper is used; not needed here

        // Minimal meta‑schema map required by the constructor
        metaSchemaMap = new HashMap<>();
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);

        // Build a real factory instance with empty loader/mappers (the defaults are sufficient)
        factory = new JsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                metaSchemaMap,
                false);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    /**
     * Verify that a simple JSON schema supplied via an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance.
     */
    @Test
    void testGetSchemaWithValidInputStream_returnsSchema() {
        // a minimal JSON schema (empty object is a valid schema)
        String schemaJson = "{}";
        InputStream schemaStream = new ByteArrayInputStream(schemaJson.getBytes());

        JsonSchema result = factory.getSchema(schemaStream);

        assertNotNull(result, "The returned JsonSchema should not be null");
    }

    /**
     * Verify that {@code getSchema(InputStream)} delegates to the overloaded
     * {@code getSchema(InputStream, SchemaValidatorsConfig)} method using the
     * default configuration.
     */
    @Test
    void testGetSchemaDelegatesToConfigMethod() {
        // Spy the factory so we can verify internal delegation
        JsonSchemaFactory spyFactory = Mockito.spy(factory);

        // Stub the overloaded method to return a mock JsonSchema
        JsonSchema mockSchema = mock(JsonSchema.class);
        doReturn(mockSchema).when(spyFactory).getSchema(any(InputStream.class), any(SchemaValidatorsConfig.class));

        // Input – any content works because the stub short‑circuits the call
        InputStream dummyStream = new ByteArrayInputStream("{ }".getBytes());

        JsonSchema result = spyFactory.getSchema(dummyStream);

        // Verify that the overloaded method was called exactly once with a non‑null config
        verify(spyFactory, times(1)).getSchema(eq(dummyStream), any(SchemaValidatorsConfig.class));
        assertSame(mockSchema, result, "The result should be the mock returned from the delegated method");
    }
}
```