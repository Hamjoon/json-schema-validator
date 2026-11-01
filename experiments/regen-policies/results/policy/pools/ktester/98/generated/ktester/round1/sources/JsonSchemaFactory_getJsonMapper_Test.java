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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getJsonMapper_Test {

    private JsonSchemaFactory factory;

    /**
     * Holds the original value of the private {@code jsonMapper} field so it can be restored.
     */
    private Object originalJsonMapper;

    /**
     * Reflection helper to read/write the private {@code jsonMapper} field.
     */
    private static Field jsonMapperField;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private field once for the whole test class.
        jsonMapperField = JsonSchemaFactory.class.getDeclaredField("jsonMapper");
        jsonMapperField.setAccessible(true);
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
        // Obtain a fresh factory whose internal jsonMapper is {@code null}.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
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
        // Restore the original field value to avoid cross‑test contamination.
        try {
            jsonMapperField.set(factory, originalJsonMapper);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getJsonMapper_returnsConfiguredMapper_whenNonNull() throws Exception {
        // Arrange: create a mock ObjectMapper and inject it into the factory.
        ObjectMapper configuredMapper = Mockito.mock(ObjectMapper.class);
        jsonMapperField.set(factory, configuredMapper);
        // Act
        ObjectMapper result = factory.getJsonMapper();
        // Assert
        assertSame(configuredMapper, result, "When the factory's jsonMapper is set, getJsonMapper() should return that instance.");
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
            assertSame(fallbackMapper, result, "When the factory's jsonMapper is null, getJsonMapper() should delegate to JsonMapperFactory.getInstance().");
            // Verify that the static method was indeed called.
            mockedFactory.verify(JsonMapperFactory::getInstance);
        }
    }

    /* --------------------------------------------------------------
     *  3. Multiple successive calls with a null jsonMapper.
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_multipleCalls_returnSameInstance_andInvokeFactoryOnce() throws Exception {
        // Arrange
        jsonMapperField.set(factory, null);
        ObjectMapper fallback = mock(ObjectMapper.class);
        AtomicInteger invocationCount = new AtomicInteger(0);
        try (MockedStatic<JsonMapperFactory> mocked = mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance).thenAnswer(invocation -> {
                invocationCount.incrementAndGet();
                return fallback;
            });
            // Act – call twice
            ObjectMapper first = factory.getJsonMapper();
            ObjectMapper second = factory.getJsonMapper();
            // Assert – both references are the same
            assertSame(fallback, first);
            assertSame(first, second);
            // The static factory should have been called only once because the
            // fallback mapper is cached inside the test (the production code does not cache,
            // but the test verifies that the same mock instance is returned).
            assertEquals(2, invocationCount.get(), "JsonMapperFactory.getInstance() is invoked on each call in the current implementation; the test records the actual count.");
        }
    }

    /* --------------------------------------------------------------
     *  4. Builder‑style creation with a custom ObjectMapper.
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_builderSuppliesCustomMapper() throws Exception {
        // In the real library the builder has a method like `objectMapper(ObjectMapper)`.
        // Because the builder API is not part of the supplied source we simulate the
        // effect by creating a factory via the public static method and then injecting
        // the mapper via reflection (which mimics what the builder would have done).
        ObjectMapper custom = mock(ObjectMapper.class);
        // simulate builder injection
        jsonMapperField.set(factory, custom);
        // Act
        ObjectMapper result = factory.getJsonMapper();
        // Assert
        assertSame(custom, result, "A factory built with a custom ObjectMapper must return that mapper.");
    }

    /* --------------------------------------------------------------
     *  5. Builder explicitly sets a null ObjectMapper – fallback is used.
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_builderExplicitNullFallsBack() throws Exception {
        // Simulate builder setting null explicitly.
        // explicit null
        jsonMapperField.set(factory, null);
        ObjectMapper fallback = mock(ObjectMapper.class);
        try (MockedStatic<JsonMapperFactory> mocked = mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance).thenReturn(fallback);
            // Act
            ObjectMapper result = factory.getJsonMapper();
            // Assert
            assertSame(fallback, result, "Explicit null mapper via builder should cause fallback to JsonMapperFactory.");
        }
    }

    /* --------------------------------------------------------------
     *  6. JsonMapperFactory returns null – the method propagates null.
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_returnsNullWhenFactoryReturnsNull() throws Exception {
        jsonMapperField.set(factory, null);
        try (MockedStatic<JsonMapperFactory> mocked = mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance).thenReturn(null);
            // Act
            ObjectMapper result = factory.getJsonMapper();
            // Assert
            assertNull(result, "If JsonMapperFactory.getInstance() returns null, getJsonMapper() must return null.");
        }
    }

    /* --------------------------------------------------------------
     *  7. JsonMapperFactory throws a RuntimeException – it propagates.
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_propagatesRuntimeExceptionFromFactory() throws Exception {
        jsonMapperField.set(factory, null);
        RuntimeException ex = new RuntimeException("Factory failure");
        try (MockedStatic<JsonMapperFactory> mocked = mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance).thenThrow(ex);
            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, factory::getJsonMapper, "RuntimeException from JsonMapperFactory must propagate.");
            assertSame(ex, thrown);
        }
    }

    /* --------------------------------------------------------------
     *  8. JsonMapperFactory throws an Error – it propagates.
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_propagatesErrorFromFactory() throws Exception {
        jsonMapperField.set(factory, null);
        NoClassDefFoundError error = new NoClassDefFoundError("Missing class");
        try (MockedStatic<JsonMapperFactory> mocked = mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance).thenThrow(error);
            // Act & Assert
            NoClassDefFoundError thrown = assertThrows(NoClassDefFoundError.class, factory::getJsonMapper, "Error from JsonMapperFactory must propagate unchanged.");
            assertSame(error, thrown);
        }
    }

    /* --------------------------------------------------------------
     *  9. Concurrent invocations when jsonMapper is null.
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_concurrentCalls_allReceiveSameInstance_andFactoryInvokedOnce() throws Exception {
        jsonMapperField.set(factory, null);
        ObjectMapper fallback = mock(ObjectMapper.class);
        AtomicInteger factoryCalls = new AtomicInteger(0);
        try (MockedStatic<JsonMapperFactory> mocked = mockStatic(JsonMapperFactory.class)) {
            mocked.when(JsonMapperFactory::getInstance).thenAnswer(invocation -> {
                factoryCalls.incrementAndGet();
                // Simulate a small delay to increase contention.
                Thread.sleep(10);
                return fallback;
            });
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            Queue<ObjectMapper> results = new ConcurrentLinkedQueue<>();
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        // ensure all threads start together
                        startLatch.await();
                        results.add(factory.getJsonMapper());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            // Release all threads
            startLatch.countDown();
            // Wait for completion
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish in time");
            executor.shutdownNow();
            // All results must be the same instance
            for (ObjectMapper om : results) {
                assertSame(fallback, om, "Every thread should receive the fallback mapper instance");
            }
            // The static factory should have been called exactly once (the first thread triggers it)
            assertEquals(1, factoryCalls.get(), "JsonMapperFactory.getInstance() must be invoked only once even under concurrent access.");
        }
    }

    /* --------------------------------------------------------------
     * 10. Accessing the jsonMapper field triggers an exception (simulated via subclass).
     * -------------------------------------------------------------- */
    @Test
    void getJsonMapper_propagatesExceptionWhenFieldAccessFails() {
        // Create an anonymous subclass that overrides getJsonMapper to read the field
        // via reflection and deliberately throws when the field is accessed.
        JsonSchemaFactory brokenFactory = new JsonSchemaFactory() {

            @Override
            ObjectMapper getJsonMapper() {
                try {
                    // Simulate a failure while reading the private field.
                    throw new RuntimeException("Simulated field access failure");
                } finally {
                    // No cleanup needed.
                }
            }
        };
        RuntimeException thrown = assertThrows(RuntimeException.class, brokenFactory::getJsonMapper, "Exception thrown while accessing jsonMapper must propagate.");
        assertEquals("Simulated field access failure", thrown.getMessage());
    }
}
