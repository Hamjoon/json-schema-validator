```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#getValidators()}.
 *
 * <p>The tests cover the behaviour described in the JSON test‑case specification:
 *
 * <ul>
 *   <li>Initialisation when {@code validators == null} – the private {@code read}
 *       method must be invoked exactly once and its result wrapped with
 *       {@link Collections#unmodifiableList(Object)}.</li>
 *   <li>Caching – a second call must return the same list instance without invoking
 *       {@code read} again.</li>
 *   <li>Handling of an empty validator list.</li>
 *   <li>Propagation of exceptions thrown by {@code read} or {@code getSchemaNode}.</li>
 *   <li>Thread‑safety of the lazy initialisation.</li>
 * </ul>
 *
 * <p>Because {@code read(JsonNode)} and {@code getSchemaNode()} are private / protected
 * members of {@link JsonSchema}, the tests use reflection to replace their behaviour
 * where required.  No production code is modified – the reflection logic is confined
 * to the test class.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidators_Test {

    private JsonSchemaFactory factory;

    @BeforeAll
    static void beforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void beforeEach() {
        // Use the latest draft – the exact version does not matter for the
        // behaviour we are testing.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterEach
    void afterEach() {
        // Nothing to clean up.
    }

    /**
     * Helper that creates a fresh {@link JsonSchema} instance from the supplied
     * JSON schema text and makes the private {@code validators} field {@code null}
     * (the default state after construction).  The method also returns the
     * {@code read} method as a {@link Method} object for later invocation via
     * reflection.
     */
    private JsonSchema createSchema(String schemaJson) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        // Ensure the internal cache is cleared – the field is private, so we use reflection.
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        validatorsField.set(schema, null);
        return schema;
    }

    /**
     * Replaces the private {@code read(JsonNode)} method with a stub that returns
     * the supplied list.  The replacement is achieved by creating a spy of the
     * original {@link JsonSchema} and using reflection to set the {@code read}
     * method accessible; the spy then delegates to the stub via a {@code Method}
     * handle.
     *
     * <p>This technique allows us to verify that {@code getValidators()} correctly
     * wraps the list and caches it without needing a full schema parsing
     * implementation.
     */
    private JsonSchema spyWithReadResult(JsonSchema original, List<JsonValidator> stubResult) throws Exception {
        JsonSchema spy = org.mockito.Mockito.spy(original);
        // Obtain the private read method.
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", com.fasterxml.jackson.databind.JsonNode.class);
        readMethod.setAccessible(true);
        // Stub the method – when called, return the supplied list.
        org.mockito.Mockito.doAnswer(invocation -> stubResult)
                .when(spy, readMethod.getName(), org.mockito.ArgumentMatchers.any());
        return spy;
    }

    /**
     * Replaces the private {@code read(JsonNode)} method with a stub that throws
     * the supplied {@link RuntimeException}.
     */
    private JsonSchema spyWithReadException(JsonSchema original, RuntimeException toThrow) throws Exception {
        JsonSchema spy = org.mockito.Mockito.spy(original);
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", com.fasterxml.jackson.databind.JsonNode.class);
        readMethod.setAccessible(true);
        org.mockito.Mockito.doThrow(toThrow)
                .when(spy, readMethod.getName(), org.mockito.ArgumentMatchers.any());
        return spy;
    }

    /**
     * Replaces the protected {@code getSchemaNode()} method with a stub that throws
     * the supplied {@link RuntimeException}.
     */
    private JsonSchema spyWithGetSchemaNodeException(JsonSchema original, RuntimeException toThrow) throws Exception {
        JsonSchema spy = org.mockito.Mockito.spy(original);
        Method getSchemaNodeMethod = JsonSchema.class.getDeclaredMethod("getSchemaNode");
        getSchemaNodeMethod.setAccessible(true);
        org.mockito.Mockito.doThrow(toThrow)
                .when(spy, getSchemaNodeMethod.getName());
        return spy;
    }

    /* --------------------------------------------------------------------- */
    /* 1. Validators Initialization – first call (null branch)                */
    /* --------------------------------------------------------------------- */

    @Test
    void getValidators_initialisesAndWrapsResult() throws Exception {
        // Prepare a stub list that the private read() will return.
        JsonValidator v1 = org.mockito.Mockito.mock(JsonValidator.class);
        JsonValidator v2 = org.mockito.Mockito.mock(JsonValidator.class);
        List<JsonValidator> stub = java.util.Arrays.asList(v1, v2);

        JsonSchema raw = createSchema("{\"type\":\"object\"}");
        JsonSchema schema = spyWithReadResult(raw, stub);

        // First call – should invoke read() exactly once and return an unmodifiable list.
        List<JsonValidator> result = schema.getValidators();

        assertNotNull(result, "Result must not be null");
        assertEquals(2, result.size(), "Result list size must match stub size");
        assertSame(v1, result.get(0), "First element must be the first stub validator");
        assertSame(v2, result.get(1), "Second element must be the second stub validator");

        // The list must be unmodifiable.
        assertThrows(UnsupportedOperationException.class,
                () -> result.add(org.mockito.Mockito.mock(JsonValidator.class)),
                "Adding to the validators list must throw UnsupportedOperationException");

        // Verify that the private read method was called exactly once.
        org.mockito.Mockito.verify(schema, org.mockito.Mockito.times(1))
                .read(org.mockito.ArgumentMatchers.any());
    }

    /* --------------------------------------------------------------------- */
    /* 2. Validators Caching – subsequent call (non‑null branch)              */
    /* --------------------------------------------------------------------- */

    @Test
    void getValidators_cachesResultOnSecondCall() throws Exception {
        JsonValidator v = org.mockito.Mockito.mock(JsonValidator.class);
        List<JsonValidator> stub = Collections.singletonList(v);

        JsonSchema raw = createSchema("{\"type\":\"object\"}");
        JsonSchema schema = spyWithReadResult(raw, stub);

        // First call – initialise.
        List<JsonValidator> first = schema.getValidators();
        // Second call – should return the same instance without invoking read again.
        List<JsonValidator> second = schema.getValidators();

        assertSame(first, second, "Subsequent calls must return the same cached list instance");
        // read() must have been called only once.
        org.mockito.Mockito.verify(schema, org.mockito.Mockito.times(1))
                .read(org.mockito.ArgumentMatchers.any());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Validators Initialization – empty list                               */
    /* --------------------------------------------------------------------- */

    @Test
    void getValidators_initialisesWithEmptyList() throws Exception {
        List<JsonValidator> empty = Collections.emptyList();

        JsonSchema raw = createSchema("{\"type\":\"object\"}");
        JsonSchema schema = spyWithReadResult(raw, empty);

        List<JsonValidator> result = schema.getValidators();

        assertNotNull(result, "Result must not be null even when read returns an empty list");
        assertTrue(result.isEmpty(), "Result list must be empty");
        // Unmodifiable behaviour.
        assertThrows(UnsupportedOperationException.class,
                () -> result.remove(0),
                "Removing from an empty validators list must throw UnsupportedOperationException");

        org.mockito.Mockito.verify(schema, org.mockito.Mockito.times(1))
                .read(org.mockito.ArgumentMatchers.any());
    }

    /* --------------------------------------------------------------------- */
    /* 4. Validators Initialization – read throws RuntimeException            */
    /* --------------------------------------------------------------------- */

    @Test
    void getValidators_propagatesRuntimeExceptionFromRead() throws Exception {
        RuntimeException cause = new RuntimeException("simulated read failure");

        JsonSchema raw = createSchema("{\"type\":\"object\"}");
        JsonSchema schema = spyWithReadException(raw, cause);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                schema::getValidators,
                "RuntimeException from read() must propagate");

        assertSame(cause, thrown, "The thrown exception must be the original one");

        // Verify that the internal cache is still null.
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        assertNull(validatorsField.get(schema), "validators field must remain null after exception");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Validators Initialization – getSchemaNode throws RuntimeException   */
    /* --------------------------------------------------------------------- */

    @Test
    void getValidators_propagatesRuntimeExceptionFromGetSchemaNode() throws Exception {
        RuntimeException cause = new RuntimeException("schema node failure");

        JsonSchema raw = createSchema("{\"type\":\"object\"}");
        JsonSchema schema = spyWithGetSchemaNodeException(raw, cause);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                schema::getValidators,
                "RuntimeException from getSchemaNode() must propagate");

        assertSame(cause, thrown, "The thrown exception must be the original one");

        // The cache must still be null.
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        assertNull(validatorsField.get(schema), "validators field must remain null after exception");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Validators Initialization – read returns null (NPE from unmodifiable) */
    /* --------------------------------------------------------------------- */

    @Test
    void getValidators_propagatesNullPointerExceptionWhenReadReturnsNull() throws Exception {
        // Stub read() to return null.
        JsonSchema raw = createSchema("{\"type\":\"object\"}");
        JsonSchema schema = spyWithReadResult(raw, null);

        assertThrows(NullPointerException.class,
                schema::getValidators,
                "NullPointerException must propagate when read() returns null");

        // Verify cache is still null.
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        assertNull(validatorsField.get(schema), "validators field must remain null after NPE");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Validators Caching – concurrent calls                               */
    /* --------------------------------------------------------------------- */

    @Test
    void getValidators_concurrentCallsResultInSingleReadInvocation() throws Exception {
        // Use a stub list with a single validator.
        JsonValidator v = org.mockito.Mockito.mock(JsonValidator.class);
        List<JsonValidator> stub = Collections.singletonList(v);

        JsonSchema raw = createSchema("{\"type\":\"object\"}");
        JsonSchema schema = spyWithReadResult(raw, stub);

        // Counter to verify that read() is called only once.
        AtomicInteger readInvocationCount = new AtomicInteger(0);
        // Replace the spy's read method with a wrapper that increments the counter.
        Method readMethod = JsonSchema.class.getDeclaredMethod("read", com.fasterxml.jackson.databind.JsonNode.class);
        readMethod.setAccessible(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            readInvocationCount.incrementAndGet();
            return stub;
        }).when(schema, readMethod.getName(), org.mockito.ArgumentMatchers.any());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Callable<List<JsonValidator>> task = schema::getValidators;

        // Submit many concurrent calls.
        List<Future<List<JsonValidator>>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(task));
        }

        // Wait for all tasks to complete and collect the results.
        List<JsonValidator> firstResult = null;
        for (Future<List<JsonValidator>> f : futures) {
            List<JsonValidator> result = f.get(5, TimeUnit.SECONDS);
            assertNotNull(result, "Result from concurrent getValidators must not be null");
            if (firstResult == null) {
                firstResult = result;
            } else {
                assertSame(firstResult, result, "All threads must receive the identical list instance");
            }
        }

        executor.shutdownNow();

        // read() must have been invoked exactly once.
        assertEquals(1, readInvocationCount.get(),
                "read(JsonNode) must be executed only once despite concurrent access");
    }
}
```