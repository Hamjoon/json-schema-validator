```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaFactory_getJsonMapper_Test {

    /** The factory instance created by the public API – it does **not** receive a custom
     *  {@code ObjectMapper} so the internal {@code jsonMapper} field is {@code null}. */
    private static JsonSchemaFactory factory;

    /** Holds the {@link Method} object for the protected {@code getJsonMapper()} method.
     *  Using reflection allows us to call the method without changing its visibility. */
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

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
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
        assertSame(expected, actual,
                "When the internal jsonMapper is null, getJsonMapper() should return JsonMapperFactory.getInstance()");
    }

    /**
     * Calls {@code getJsonMapper()} a second time to ensure the method is deterministic
     * and does not create a new mapper on each invocation.
     */
    @Test
    void getJsonMapper_isIdempotent() throws Exception {
        ObjectMapper first = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        ObjectMapper second = (ObjectMapper) getJsonMapperMethod.invoke(factory);
        assertSame(first, second,
                "Subsequent calls to getJsonMapper() should return the same ObjectMapper instance");
    }
}
```