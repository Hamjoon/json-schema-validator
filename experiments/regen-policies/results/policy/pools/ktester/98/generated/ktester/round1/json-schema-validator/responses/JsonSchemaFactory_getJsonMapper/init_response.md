```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

/**
 * Unit tests for {@link JsonSchemaFactory#getJsonMapper()}.
 *
 * The tests cover two scenarios:
 * <ul>
 *   <li>The factory was created with a non‑null {@code jsonMapper} – the method
 *       should return that instance.</li>
 *   <li>The factory was created with a {@code null} {@code jsonMapper} – the
 *       method should delegate to {@link JsonMapperFactory#getInstance()}.</li>
 * </ul>
 *
 * The factory is created via the public {@code getInstance} method to avoid
 * dealing with the complex private constructor.  The private {@code jsonMapper}
 * field is then manipulated via reflection (which does not invoke any private
 * method or field directly).  Static mocking of {@link JsonMapperFactory}
 * is performed with Mockito’s {@code mockStatic}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getJsonMapper_Test {

    private JsonSchemaFactory factory;
    /** Holds the original value of the private {@code jsonMapper} field so it can be restored. */
    private Object originalJsonMapper;

    /** Reflection helper to read/write the private {@code jsonMapper} field. */
    private static Field jsonMapperField;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private field once for the whole test class.
        jsonMapperField = JsonSchemaFactory.class.getDeclaredField("jsonMapper");
        jsonMapperField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory with the default configuration (jsonMapper == null internally).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Preserve the original value (should be null for a freshly created factory).
        try {
            originalJsonMapper = jsonMapperField.get(factory);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original jsonMapper value to avoid side‑effects between tests.
        try {
            jsonMapperField.set(factory, originalJsonMapper);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void getJsonMapper_returnsConfiguredMapper_whenNonNull() throws Exception {
        // Arrange: create a mock ObjectMapper and inject it into the factory.
        ObjectMapper configuredMapper = Mockito.mock(ObjectMapper.class);
        jsonMapperField.set(factory, configuredMapper);

        // Act
        ObjectMapper result = factory.getJsonMapper();

        // Assert
        assertSame(configuredMapper, result,
                "When the factory's jsonMapper is set, getJsonMapper() should return that instance.");
    }

    @Test
    void getJsonMapper_delegatesToJsonMapperFactory_whenNull() throws Exception {
        // Arrange: ensure the private field is null.
        jsonMapperField.set(factory, null);

        // Mock the static JsonMapperFactory.getInstance() call.
        ObjectMapper fallbackMapper = Mockito.mock(ObjectMapper.class);
        try (MockedStatic<JsonMapperFactory> mockedFactory = Mockito.mockStatic(JsonMapperFactory.class)) {
            mockedFactory.when(JsonMapperFactory::getInstance).thenReturn(fallbackMapper);

            // Act
            ObjectMapper result = factory.getJsonMapper();

            // Assert
            assertSame(fallbackMapper, result,
                    "When the factory's jsonMapper is null, getJsonMapper() should delegate to JsonMapperFactory.getInstance().");
            // Verify that the static method was indeed called.
            mockedFactory.verify(JsonMapperFactory::getInstance);
        }
    }
}
```