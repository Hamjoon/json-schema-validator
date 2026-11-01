**Generated test class**

```java
package com.networknt.schema;

import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    // ----- shared test fixtures -------------------------------------------------
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private SchemaLoaders.Builder schemaLoadersBuilderMock;
    @Mock
    private SchemaMappers.Builder schemaMappersBuilderMock;
    @Mock
    private JsonMetaSchema metaSchemaMock;

    private Map<String, JsonMetaSchema> metaSchemaMap;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a map containing the default meta‑schema – the factory validates this
        metaSchemaMap = new HashMap<>();
        // The key must be the *normalized* form of the default URI.
        // The method normalizeMetaSchemaUri is protected static, so we invoke it via reflection.
        try {
            java.lang.reflect.Method normMethod = JsonSchemaFactory.class
                    .getDeclaredMethod("normalizeMetaSchemaUri", String.class);
            normMethod.setAccessible(true);
            String normalized = (String) normMethod.invoke(null, DEFAULT_META_SCHEMA_URI);
            metaSchemaMap.put(normalized, metaSchemaMock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – all mocks are reset automatically by MockitoExtension
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    // -------------------------------------------------------------------------
    // Helper: create a JsonSchemaFactory instance via the private constructor.
    // -------------------------------------------------------------------------
    private JsonSchemaFactory createFactory(boolean enableCache) throws Exception {
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class,
                        ObjectMapper.class,
                        String.class,
                        SchemaLoaders.Builder.class,
                        SchemaMappers.Builder.class,
                        Map.class,
                        boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                schemaLoadersBuilderMock,
                schemaMappersBuilderMock,
                metaSchemaMap,
                enableCache);
    }

    // -------------------------------------------------------------------------
    // Test 1 – cache enabled: the underlying getMappedSchema() must be invoked
    // only once, subsequent calls must reuse the cached JsonSchema.
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithCache_ShouldReuseCachedSchema() throws Exception {
        // Arrange
        JsonSchemaFactory realFactory = createFactory(true);
        JsonSchemaFactory factorySpy = Mockito.spy(realFactory);

        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg1 = new SchemaValidatorsConfig();
        SchemaValidatorsConfig cfg2 = new SchemaValidatorsConfig();

        JsonSchema mappedSchema = mock(JsonSchema.class);
        // The factory returns the same mapped schema for every call to getMappedSchema()
        doReturn(mappedSchema).when(factorySpy).getMappedSchema(any(SchemaLocation.class), any());

        // withConfig() must return the schema itself (the real implementation does this)
        when(mappedSchema.withConfig(any(SchemaValidatorsConfig.class))).thenReturn(mappedSchema);

        // Act – first call
        JsonSchema result1 = factorySpy.getSchema(schemaLocation, cfg1);
        // Act – second call with a *different* config but same location
        JsonSchema result2 = factorySpy.getSchema(schemaLocation, cfg2);

        // Assert
        assertSame(mappedSchema, result1, "First call should return the mapped schema");
        assertSame(mappedSchema, result2, "Second call should return the cached schema");

        // getMappedSchema must be called only once
        verify(factorySpy, times(1)).getMappedSchema(eq(schemaLocation), any());

        // withConfig must be called twice – once for each config supplied
        verify(mappedSchema, times(2)).withConfig(any(SchemaValidatorsConfig.class));
    }

    // -------------------------------------------------------------------------
    // Test 2 – cache disabled: each invocation must delegate to getMappedSchema().
    // -------------------------------------------------------------------------
    @Test
    void getSchema_WithoutCache_ShouldCallMappedSchemaEveryTime() throws Exception {
        // Arrange
        JsonSchemaFactory realFactory = createFactory(false);
        JsonSchemaFactory factorySpy = Mockito.spy(realFactory);

        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonSchema mappedSchema1 = mock(JsonSchema.class);
        JsonSchema mappedSchema2 = mock(JsonSchema.class);

        // Return different mocks for successive calls to illustrate that the method is hit each time
        doReturn(mappedSchema1).doReturn(mappedSchema2)
                .when(factorySpy).getMappedSchema(eq(schemaLocation), any());

        when(mappedSchema1.withConfig(any())).thenReturn(mappedSchema1);
        when(mappedSchema2.withConfig(any())).thenReturn(mappedSchema2);

        // Act – first call
        JsonSchema result1 = factorySpy.getSchema(schemaLocation, cfg);
        // Act – second call
        JsonSchema result2 = factorySpy.getSchema(schemaLocation, cfg);

        // Assert
        assertSame(mappedSchema1, result1, "First call should return the first mapped schema");
        assertSame(mappedSchema2, result2, "Second call should return the second mapped schema");

        // getMappedSchema must be called twice (once per invocation)
        verify(factorySpy, times(2)).getMappedSchema(eq(schemaLocation), any());

        // withConfig must be called once per result
        verify(mappedSchema1, times(1)).withConfig(any());
        verify(mappedSchema2, times(1)).withConfig(any());
    }
}
```