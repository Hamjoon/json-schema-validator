```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#loadSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * <p>The method is {@code protected}, therefore the tests invoke it via reflection.
 * A {@link JsonSchemaFactory} spy is used to stub {@code getMappedSchema} and to
 * inspect the internal {@code schemaCache}. All scenarios described in the
 * provided JSON test matrix are covered.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadSchema_Test {

    /** Factory with caching disabled – created via the public builder. */
    private JsonSchemaFactory nonCachingFactory;

    /** Factory with caching enabled – created via the public builder. */
    private JsonSchemaFactory cachingFactory;

    /** Reflection handle for the protected {@code loadSchema} method. */
    private Method loadSchemaMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Builder API is assumed to have enableSchemaCache(boolean) as described.
        nonCachingFactory = JsonSchemaFactory.builder()
                .enableSchemaCache(false)
                .build();

        cachingFactory = JsonSchemaFactory.builder()
                .enableSchemaCache(true)
                .build();

        // Obtain the protected method and make it accessible.
        loadSchemaMethod = JsonSchemaFactory.class
                .getDeclaredMethod("loadSchema", SchemaLocation.class, SchemaValidatorsConfig.class);
        loadSchemaMethod.setAccessible(true);
    }

    /**
     * Helper to invoke the protected {@code loadSchema} method on a factory instance.
     */
    private JsonSchema invokeLoadSchema(JsonSchemaFactory factory,
                                        SchemaLocation uri,
                                        SchemaValidatorsConfig cfg) throws Exception {
        return (JsonSchema) loadSchemaMethod.invoke(factory, uri, cfg);
    }

    /**
     * Helper to obtain the internal cache map via reflection.
     */
    @SuppressWarnings("unchecked")
    private ConcurrentMap<SchemaLocation, JsonSchema> getCache(JsonSchemaFactory factory) throws Exception {
        Field cacheField = JsonSchemaFactory.class.getDeclaredField("schemaCache");
        cacheField.setAccessible(true);
        return (ConcurrentMap<SchemaLocation, JsonSchema>) cacheField.get(factory);
    }

    /* --------------------------------------------------------------------- */
    /*  Test groups – each corresponds to a JSON test case group            */
    /* --------------------------------------------------------------------- */

    @Nested
    class CacheDisabledDirectMapping {

        @Test
        void whenCacheDisabled_andMapperReturnsSchema_thenReturnedInstanceIsMapperResult()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(nonCachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);
            JsonSchema mappedSchema = mock(JsonSchema.class);

            doReturn(mappedSchema).when(spy).getMappedSchema(mockLocation, mockConfig);

            // Act
            JsonSchema result = invokeLoadSchema(spy, mockLocation, mockConfig);

            // Assert
            assertSame(mappedSchema, result,
                    "With caching disabled the method must return exactly what getMappedSchema returns");
            verify(spy, times(1)).getMappedSchema(mockLocation, mockConfig);
        }

        @Test
        void whenCacheDisabled_andConfigIsNull_thenNullConfigIsPassedThrough()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(nonCachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            JsonSchema mappedSchema = mock(JsonSchema.class);

            doReturn(mappedSchema).when(spy).getMappedSchema(mockLocation, null);

            // Act
            JsonSchema result = invokeLoadSchema(spy, mockLocation, null);

            // Assert
            assertSame(mappedSchema, result);
            verify(spy, times(1)).getMappedSchema(mockLocation, null);
        }
    }

    @Nested
    class CacheEnabledCacheMissMapperReturnsNonNull {

        @Test
        void whenCacheEnabled_andCacheMiss_andMapperReturnsSchema_thenSchemaIsCachedAndWithConfigApplied()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(cachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);
            JsonSchema mappedSchema = mock(JsonSchema.class);
            JsonSchema afterConfig = mock(JsonSchema.class);

            doReturn(mappedSchema).when(spy).getMappedSchema(mockLocation, mockConfig);
            when(mappedSchema.withConfig(mockConfig)).thenReturn(afterConfig);

            // Act
            JsonSchema result = invokeLoadSchema(spy, mockLocation, mockConfig);

            // Assert
            assertSame(afterConfig, result, "Returned instance must be the result of withConfig");
            verify(mappedSchema, times(1)).withConfig(mockConfig);
            // Verify that the schema was stored in the internal cache
            ConcurrentMap<SchemaLocation, JsonSchema> cache = getCache(spy);
            assertTrue(cache.containsKey(mockLocation), "Cache must contain the schema after a miss");
            assertSame(mappedSchema, cache.get(mockLocation), "Cache must store the original schema (pre‑withConfig)");
        }

        @Test
        void whenCacheEnabled_andCacheMiss_andMapperReturnsNull_thenResultIsNullAndCacheUnchanged()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(cachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);

            doReturn(null).when(spy).getMappedSchema(mockLocation, mockConfig);
            ConcurrentMap<SchemaLocation, JsonSchema> cache = getCache(spy);
            assertTrue(cache.isEmpty(), "Cache should start empty");

            // Act
            JsonSchema result = invokeLoadSchema(spy, mockLocation, mockConfig);

            // Assert
            assertNull(result, "Result must be null when mapper returns null");
            assertTrue(cache.isEmpty(), "Cache must remain empty when mapper returns null");
            verify(spy, times(1)).getMappedSchema(mockLocation, mockConfig);
        }

        @Test
        void whenCacheEnabled_andCacheMiss_andMapperThrowsRuntimeException_thenExceptionPropagates()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(cachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);
            RuntimeException thrown = new RuntimeException("mapper failure");

            doThrow(thrown).when(spy).getMappedSchema(mockLocation, mockConfig);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> invokeLoadSchema(spy, mockLocation, mockConfig));
            assertSame(thrown, ex, "Exception from getMappedSchema must propagate unchanged");
        }

        @Test
        void whenCacheEnabled_andCacheMiss_andConfigIsNull_thenNullConfigIsPassedAndResultCached()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(cachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            JsonSchema mappedSchema = mock(JsonSchema.class);
            JsonSchema afterConfig = mock(JsonSchema.class);

            doReturn(mappedSchema).when(spy).getMappedSchema(mockLocation, null);
            when(mappedSchema.withConfig(null)).thenReturn(afterConfig);

            // Act
            JsonSchema result = invokeLoadSchema(spy, mockLocation, null);

            // Assert
            assertSame(afterConfig, result);
            verify(mappedSchema, times(1)).withConfig(null);
            ConcurrentMap<SchemaLocation, JsonSchema> cache = getCache(spy);
            assertTrue(cache.containsKey(mockLocation));
            assertSame(mappedSchema, cache.get(mockLocation));
        }
    }

    @Nested
    class CacheEnabledCacheHit {

        @Test
        void whenCacheEnabled_andCacheHit_thenWithConfigIsInvokedAndMapperIsNotCalled()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(cachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);
            JsonSchema cachedSchema = mock(JsonSchema.class);
            JsonSchema afterConfig = mock(JsonSchema.class);

            // Pre‑populate cache
            ConcurrentMap<SchemaLocation, JsonSchema> cache = getCache(spy);
            cache.put(mockLocation, cachedSchema);

            when(cachedSchema.withConfig(mockConfig)).thenReturn(afterConfig);

            // Act
            JsonSchema result = invokeLoadSchema(spy, mockLocation, mockConfig);

            // Assert
            assertSame(afterConfig, result);
            verify(cachedSchema, times(1)).withConfig(mockConfig);
            verify(spy, never()).getMappedSchema(any(), any());
        }

        @Test
        void whenCacheEnabled_andCacheHit_andWithConfigReturnsNewInstance_thenReturnedInstanceDiffersFromCache()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(cachingFactory);
            SchemaLocation mockLocation = mock(SchemaLocation.class);
            SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);
            JsonSchema cachedSchema = mock(JsonSchema.class);
            JsonSchema newInstance = mock(JsonSchema.class);

            ConcurrentMap<SchemaLocation, JsonSchema> cache = getCache(spy);
            cache.put(mockLocation, cachedSchema);
            when(cachedSchema.withConfig(mockConfig)).thenReturn(newInstance);

            // Act
            JsonSchema result = invokeLoadSchema(spy, mockLocation, mockConfig);

            // Assert
            assertSame(newInstance, result);
            assertNotSame(cachedSchema, result, "Result may be a different instance than the cached one");
            verify(spy, never()).getMappedSchema(any(), any());
        }

        @Test
        void whenCacheEnabled_andSchemaLocationIsNull_thenNullPointerExceptionIsThrown()
                throws Exception {
            // Arrange
            JsonSchemaFactory spy = spy(cachingFactory);
            SchemaValidatorsConfig mockConfig = mock(SchemaValidatorsConfig.class);

            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> invokeLoadSchema(spy, null, mockConfig),
                    "Passing a null SchemaLocation must result in NPE");
        }
    }
}
```