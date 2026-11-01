```java
package com.networknt.schema;

import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchemaLoader()}.
 *
 * <p>The tests cover the following scenarios:
 *
 * <ul>
 *   <li>Factory returns a non‑null loader by default.</li>
 *   <li>The same instance is returned on repeated calls.</li>
 *   <li>Injecting a mock {@link SchemaLoader} via reflection yields the exact same instance.</li>
 *   <li>When the internal {@code schemaLoader} field is forced to {@code null}, the getter returns {@code null}.</li>
 *   <li>Calling other factory methods (e.g. {@code getSchema(String)}) does not replace the loader.</li>
 *   <li>Concurrent access from multiple threads always returns the identical loader reference.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLoader_Test {

    /** The factory instance used in the tests. */
    private JsonSchemaFactory factory;

    /** The version flag that will be used to obtain a factory instance. */
    private static final VersionFlag VERSION = VersionFlag.V201909; // any valid flag

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources are required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * JsonSchemaFactory provides a public static factory method that creates a fully
         * configured instance (including the internal SchemaLoader).  Using this method
         * respects the class’s public API and avoids any reflection on private constructors.
         */
        factory = JsonSchemaFactory.getInstance(VERSION);
        assertNotNull(factory, "Factory instance should be created by getInstance()");
    }

    @AfterEach
    void teardownAfterEach() {
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /** Baseline test – default factory must provide a non‑null loader. */
    @Test
    void getSchemaLoader_returnsNonNullInstance() {
        SchemaLoader loader = factory.getSchemaLoader();
        assertNotNull(loader, "SchemaLoader should not be null");
        assertTrue(loader instanceof SchemaLoader,
                "Returned object should implement SchemaLoader");
    }

    /** Baseline test – repeated calls must return the same reference. */
    @Test
    void getSchemaLoader_returnsSameInstanceOnRepeatedCalls() {
        SchemaLoader first = factory.getSchemaLoader();
        SchemaLoader second = factory.getSchemaLoader();
        assertSame(first, second, "SchemaLoader should be the same instance on repeated calls");
    }

    /** Inject a mock SchemaLoader via reflection and verify the getter returns it. */
    @Test
    void getSchemaLoader_returnsInjectedMockInstance(@Mock SchemaLoader mockLoader) throws Exception {
        // Use reflection to replace the private final field `schemaLoader`.
        Field field = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        field.setAccessible(true);
        field.set(factory, mockLoader);

        SchemaLoader returned = factory.getSchemaLoader();
        assertSame(mockLoader, returned, "Factory should return the injected mock SchemaLoader");
    }

    /** Force the internal field to null via reflection and ensure the getter returns null. */
    @Test
    void getSchemaLoader_returnsNullWhenFieldIsNull() throws Exception {
        // Set the private final field to null.
        Field field = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        field.setAccessible(true);
        field.set(factory, null);

        SchemaLoader returned = factory.getSchemaLoader();
        assertNull(returned, "When internal field is null, getter should return null");
    }

    /** Verify that calling other factory methods does not replace the loader instance. */
    @Test
    void getSchemaLoader_unchangedAfterSchemaRetrieval() {
        SchemaLoader before = factory.getSchemaLoader();
        assertNotNull(before, "Pre‑condition: loader must be non‑null");

        // Parse a tiny JSON schema – this operation should not touch the loader reference.
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        assertNotNull(schema, "Schema parsing should succeed for a simple schema");

        SchemaLoader after = factory.getSchemaLoader();
        assertSame(before, after, "Loader reference must stay unchanged after getSchema(String)");
    }

    /** Concurrent calls to getSchemaLoader must all return the identical reference. */
    @Test
    void getSchemaLoader_threadSafety() throws Exception {
        final int THREAD_COUNT = 10;
        final SchemaLoader mainRef = factory.getSchemaLoader();
        assertNotNull(mainRef, "Main thread must obtain a non‑null loader");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Callable<SchemaLoader>> tasks = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> factory.getSchemaLoader());
        }

        List<Future<SchemaLoader>> results = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate promptly");

        for (Future<SchemaLoader> future : results) {
            SchemaLoader loader = future.get(); // will re‑throw any exception
            assertSame(mainRef, loader, "All threads must receive the identical SchemaLoader instance");
        }
    }
}
```