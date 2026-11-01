package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.resource.SchemaLoader;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchemaLoader_Test {

    /**
     * The factory instance used in the tests.
     */
    private JsonSchemaFactory factory;

    // any valid flag
    private static final VersionFlag VERSION = VersionFlag.V201909;

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
        // Nothing to clean up after each test.
        factory = null;
    }

    @Test
    void getSchemaLoader_returnsNonNullInstance() {
        SchemaLoader loader = factory.getSchemaLoader();
        assertNotNull(loader, "SchemaLoader should not be null");
        // The concrete implementation is DefaultSchemaLoader, but we only assert the type
        // to avoid coupling to internal classes that could change.
        assertTrue(loader instanceof SchemaLoader, "Returned object should implement SchemaLoader");
    }

    @Test
    void getSchemaLoader_returnsSameInstanceOnRepeatedCalls() {
        SchemaLoader first = factory.getSchemaLoader();
        SchemaLoader second = factory.getSchemaLoader();
        assertSame(first, second, "SchemaLoader should be the same instance on repeated calls");
    }

    /**
     * Inject a mock SchemaLoader via reflection and verify the getter returns it.
     */
    @Test
    void getSchemaLoader_returnsInjectedMockInstance(@Mock SchemaLoader mockLoader) throws Exception {
        // Use reflection to replace the private final field `schemaLoader`.
        Field field = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        field.setAccessible(true);
        field.set(factory, mockLoader);
        SchemaLoader returned = factory.getSchemaLoader();
        assertSame(mockLoader, returned, "Factory should return the injected mock SchemaLoader");
    }

    /**
     * Force the internal field to null via reflection and ensure the getter returns null.
     */
    @Test
    void getSchemaLoader_returnsNullWhenFieldIsNull() throws Exception {
        // Set the private final field to null.
        Field field = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        field.setAccessible(true);
        field.set(factory, null);
        SchemaLoader returned = factory.getSchemaLoader();
        assertNull(returned, "When internal field is null, getter should return null");
    }

    /**
     * Verify that calling other factory methods does not replace the loader instance.
     */
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

    /**
     * Concurrent calls to getSchemaLoader must all return the identical reference.
     */
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
            // will re‑throw any exception
            SchemaLoader loader = future.get();
            assertSame(mainRef, loader, "All threads must receive the identical SchemaLoader instance");
        }
    }
}
