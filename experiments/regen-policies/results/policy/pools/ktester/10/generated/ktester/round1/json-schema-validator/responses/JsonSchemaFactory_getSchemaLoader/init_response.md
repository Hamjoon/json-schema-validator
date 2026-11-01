```java
package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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
        // Nothing to clean up after each test.
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void getSchemaLoader_returnsNonNullInstance() {
        SchemaLoader loader = factory.getSchemaLoader();
        assertNotNull(loader, "SchemaLoader should not be null");
        // The concrete implementation is DefaultSchemaLoader, but we only assert the type
        // to avoid coupling to internal classes that could change.
        assertTrue(loader instanceof SchemaLoader,
                "Returned object should implement SchemaLoader");
    }

    @Test
    void getSchemaLoader_returnsSameInstanceOnRepeatedCalls() {
        SchemaLoader first = factory.getSchemaLoader();
        SchemaLoader second = factory.getSchemaLoader();
        assertSame(first, second, "SchemaLoader should be the same instance on repeated calls");
    }
}
```