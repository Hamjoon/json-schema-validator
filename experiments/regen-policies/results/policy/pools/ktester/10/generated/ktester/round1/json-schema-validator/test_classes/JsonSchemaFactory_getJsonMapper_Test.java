package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.Field;

class JsonSchemaFactory_getJsonMapper_Test {

    /**
     * The factory instance created by the public API – it does **not** receive a custom
     *  {@code ObjectMapper} so the internal {@code jsonMapper} field is {@code null}.
     */
    private static JsonSchemaFactory factory;

    /**
     * Holds the {@link Method} object for the protected {@code getJsonMapper()} method.
     *  Using reflection allows us to call the method without changing its visibility.
     */
    private static Method getJsonMapperMethod;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Create a factory via the public static factory method.
        // Any version flag works – V4 is the most common default.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        // Prepare reflective access to the protected method.
        getJsonMapperMethod = JsonSchemaFactory.class.getDeclaredMethod("getJsonMapper");
        getJsonMapperMethod.setAccessible(true);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset static references – helps the JVM GC in larger test suites.
        factory = null;
        getJsonMapperMethod = null;
    }

    /**
     * Verifies that when the factory is created without an explicit {@code ObjectMapper},
     * {@code getJsonMapper()} falls back to the singleton provided by
     * {@link JsonMapperFactory#getInstance()}.
     */
    @Test
    void getJsonMapper_returnsDefaultMapper_whenJsonMapperIsNull() throws Exception {
        // Act – invoke the protected method via reflection.
        ObjectMapper actual = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        // Assert – the mapper must be non‑null and must be the same instance that the
        // JsonMapperFactory supplies.
        assertNotNull(actual, "The returned ObjectMapper should not be null");
        ObjectMapper expected = JsonMapperFactory.getInstance();
        assertSame(expected, actual, "When the internal jsonMapper is null, getJsonMapper() should return JsonMapperFactory.getInstance()");
    }

    /**
     * Calls {@code getJsonMapper()} a second time to ensure the method is deterministic
     * and does not create a new mapper on each invocation.
     */
    @Test
    void getJsonMapper_isIdempotent() throws Exception {
        ObjectMapper first = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        ObjectMapper second = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        assertSame(first, second, "Subsequent calls to getJsonMapper() should return the same ObjectMapper instance");
    }

    @Test
    void getJsonMapper_isIdempotent_whenJsonMapperIsNull() throws Exception {
        ObjectMapper first = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        ObjectMapper second = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        assertSame(first, second, "Subsequent calls to getJsonMapper() should return the same ObjectMapper instance");
    }

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
        // bypass final modifier for test purposes
        jsonMapperField.set(customFactory, injected);
        // act
        ObjectMapper actual = (ObjectMapper) getJsonMapperMethod.invoke(customFactory);
        // assert
        assertSame(injected, actual, "When jsonMapper field is non‑null, getJsonMapper() must return the injected instance");
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
        assertSame(first, second, "Repeated calls should return the same injected ObjectMapper instance");
    }

    /**
     * Mocks {@link JsonMapperFactory#getInstance()} to throw an {@link IllegalStateException}
     * and verifies that {@code getJsonMapper()} does **not** swallow the exception.
     */
    @Test
    void getJsonMapper_propagatesException_whenJsonMapperFactoryThrows() throws Exception {
        // arrange – static mock that throws
        try (MockedStatic<JsonMapperFactory> mocked = Mockito.mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance).thenThrow(new IllegalStateException("Factory failure"));
            // factory created with null internal mapper (default path)
            JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
            // act & assert – the IllegalStateException must bubble up
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> getJsonMapperMethod.invoke(f), "Expected IllegalStateException to be propagated from JsonMapperFactory.getInstance()");
            assertEquals("Factory failure", thrown.getMessage());
        }
    }
}
