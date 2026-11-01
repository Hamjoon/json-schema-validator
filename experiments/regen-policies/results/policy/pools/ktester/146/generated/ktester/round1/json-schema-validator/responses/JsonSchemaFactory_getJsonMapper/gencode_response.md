```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getJsonMapper()}.
 *
 * <p>The method returns the {@code jsonMapper} field when it has been
 * explicitly supplied to the factory; otherwise it falls back to the
 * singleton provided by {@link JsonMapperFactory#getInstance()}.
 *
 * <p>Two scenarios are covered:
 * <ul>
 *   <li>Factory created via the public {@code getInstance(...)} API – the
 *       internal {@code jsonMapper} is {@code null} and the fallback instance
 *       should be returned.</li>
 *   <li>Factory instantiated via reflection with a custom {@link ObjectMapper}
 *       – the same mapper should be returned.</li>
 * </ul>
 *
 * <p>An additional check ensures that when the fallback path is taken,
 * repeated invocations return the very same {@link ObjectMapper} reference,
 * confirming that no new mapper is created each time.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getJsonMapper_Test {

    /** The factory that relies on the default {@link JsonMapperFactory} instance. */
    private JsonSchemaFactory factoryWithFallback;

    /** The factory that holds a custom {@link ObjectMapper}. */
    private JsonSchemaFactory factoryWithCustomMapper;

    /** A custom {@link ObjectMapper} used to verify the direct‑return path. */
    private ObjectMapper customMapper;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1️⃣ Factory that relies on the default JsonMapperFactory instance.
        factoryWithFallback = JsonSchemaFactory.getInstance(VersionFlag.V202012);

        // 2️⃣ Factory that holds a custom ObjectMapper.
        customMapper = new ObjectMapper(); // a real mapper – could also be a mock
        factoryWithCustomMapper = JsonSchemaFactory.getInstance(VersionFlag.V202012);

        // Inject the custom mapper into the private field via reflection.
        Field jsonMapperField = JsonSchemaFactory.class.getDeclaredField("jsonMapper");
        jsonMapperField.setAccessible(true);
        jsonMapperField.set(factoryWithCustomMapper, customMapper);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for these lightweight objects.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    @Test
    void getJsonMapper_returnsFallbackWhenFieldIsNull() {
        // When the factory is created via the public API the internal field is null,
        // therefore the method must return the singleton from JsonMapperFactory.
        ObjectMapper result = factoryWithFallback.getJsonMapper();

        assertNotNull(result, "Resulting ObjectMapper must not be null");
        assertSame(JsonMapperFactory.getInstance(),
                result,
                "When jsonMapper field is null, getJsonMapper() should delegate to JsonMapperFactory.getInstance()");
    }

    @Test
    void getJsonMapper_returnsCustomMapperWhenFieldIsSet() {
        // The custom mapper was injected via reflection in the @BeforeEach method.
        ObjectMapper result = factoryWithCustomMapper.getJsonMapper();

        assertNotNull(result, "Resulting ObjectMapper must not be null");
        assertSame(customMapper,
                result,
                "When jsonMapper field is non‑null, getJsonMapper() must return that exact instance");
    }

    @Test
    void getJsonMapper_returnsSameInstanceOnConsecutiveCallsWhenUsingFallback() {
        // Verify that two successive calls do not create a new mapper.
        ObjectMapper firstCall = factoryWithFallback.getJsonMapper();
        ObjectMapper secondCall = factoryWithFallback.getJsonMapper();

        assertNotNull(firstCall, "First call should return a non‑null ObjectMapper");
        assertNotNull(secondCall, "Second call should return a non‑null ObjectMapper");
        assertSame(firstCall, secondCall,
                "Consecutive calls on a factory with null jsonMapper must return the same ObjectMapper instance");
    }
}
```