```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getJsonMapper()}.
 *
 * <p>The method is {@code protected}, therefore we obtain a {@link Method} instance
 * via reflection and invoke it on a {@link JsonSchemaFactory} instance.
 *
 * <p>Three behavioural groups are covered:
 *
 * <ul>
 *   <li>When the internal {@code jsonMapper} field is {@code null} – the method must
 *       return the singleton supplied by {@link JsonMapperFactory#getInstance()}.</li>
 *   <li>When the internal {@code jsonMapper} field is non‑null – the method must
 *       return the injected instance.</li>
 *   <li>If {@link JsonMapperFactory#getInstance()} throws, the exception must be
 *       propagated unchanged.</li>
 * </ul>
 *
 * <p>Mockito is used for static mocking of {@link JsonMapperFactory}.
 */
class JsonSchemaFactory_getJsonMapper_Test {

    /** Factory created via the public API – it does **not** receive a custom
     *  {@code ObjectMapper} so the internal {@code jsonMapper} field is {@code null}. */
    private static JsonSchemaFactory factory;

    /** Reflective handle for the protected {@code getJsonMapper()} method. */
    private static Method getJsonMapperMethod;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // any version flag works – V4 is the default in the library
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

        // obtain the protected method via reflection
        getJsonMapperMethod = JsonSchemaFactory.class.getDeclaredMethod("getJsonMapper");
        getJsonMapperMethod.setAccessible(true);
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
        getJsonMapperMethod = null;
    }

    /* --------------------------------------------------------------
     *  Tests for the *null* jsonMapper branch (already present in the
     *  skeleton – kept unchanged for completeness).
     * -------------------------------------------------------------- */

    @Test
    void getJsonMapper_returnsDefaultMapper_whenJsonMapperIsNull() throws Exception {
        ObjectMapper actual = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        assertNotNull(actual, "The returned ObjectMapper should not be null");
        ObjectMapper expected = JsonMapperFactory.getInstance();
        assertSame(expected, actual,
                "When the internal jsonMapper is null, getJsonMapper() should return JsonMapperFactory.getInstance()");
    }

    @Test
    void getJsonMapper_isIdempotent_whenJsonMapperIsNull() throws Exception {
        ObjectMapper first = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        ObjectMapper second = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        assertSame(first, second,
                "Subsequent calls to getJsonMapper() should return the same ObjectMapper instance");
    }

    /* --------------------------------------------------------------
     *  Tests for the *non‑null* jsonMapper branch.
     * -------------------------------------------------------------- */

    /**
     * Verifies that when a custom {@link ObjectMapper} is injected (via reflection),
     * {@code getJsonMapper()} returns exactly that instance.
     */
    @Test
    void getJsonMapper_returnsInjectedMapper_whenJsonMapperIsNonNull() throws Exception {
        // arrange – create a fresh factory and inject a custom mapper
        JsonSchemaFactory customFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        ObjectMapper injected = new ObjectMapper();

        Field jsonMapperField = JsonSchemaFactory.class.getDeclaredField("jsonMapper");
        jsonMapperField.setAccessible(true);
        jsonMapperField.set(customFactory, injected);   // bypass final modifier for test purposes

        // act
        ObjectMapper actual = (ObjectMapper) getJsonMapperMethod.invoke(customFactory);

        // assert
        assertSame(injected, actual,
                "When jsonMapper field is non‑null, getJsonMapper() must return the injected instance");
    }

    /**
     * Ensures the method is deterministic when a custom mapper is present:
     * repeated invocations must return the *same* injected instance.
     */
    @Test
    void getJsonMapper_isIdempotent_whenJsonMapperIsNonNull() throws Exception {
        JsonSchemaFactory customFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        ObjectMapper injected = new ObjectMapper();

        Field jsonMapperField = JsonSchemaFactory.class.getDeclaredField("jsonMapper");
        jsonMapperField.setAccessible(true);
        jsonMapperField.set(customFactory, injected);

        ObjectMapper first = (ObjectMapper) getJsonMapperMethod.invoke(customFactory);
        ObjectMapper second = (ObjectMapper) getJsonMapperMethod.invoke(customFactory);

        assertSame(first, second,
                "Repeated calls should return the same injected ObjectMapper instance");
    }

    /* --------------------------------------------------------------
     *  Test for exception propagation from JsonMapperFactory.
     * -------------------------------------------------------------- */

    /**
     * Mocks {@link JsonMapperFactory#getInstance()} to throw an {@link IllegalStateException}
     * and verifies that {@code getJsonMapper()} does **not** swallow the exception.
     */
    @Test
    void getJsonMapper_propagatesException_whenJsonMapperFactoryThrows() throws Exception {
        // arrange – static mock that throws
        try (MockedStatic<JsonMapperFactory> mocked = Mockito.mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance)
                  .thenThrow(new IllegalStateException("Factory failure"));

            // factory created with null internal mapper (default path)
            JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

            // act & assert – the IllegalStateException must bubble up
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> getJsonMapperMethod.invoke(f),
                    "Expected IllegalStateException to be propagated from JsonMapperFactory.getInstance()");
            assertEquals("Factory failure", thrown.getMessage());
        }
    }
}
```