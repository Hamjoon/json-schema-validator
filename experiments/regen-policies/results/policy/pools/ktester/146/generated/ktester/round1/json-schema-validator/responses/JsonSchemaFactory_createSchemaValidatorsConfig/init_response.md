```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

/**
 * Unit tests for the {@code protected SchemaValidatorsConfig createSchemaValidatorsConfig()}
 * method of {@link JsonSchemaFactory}.
 *
 * <p>The method is protected, therefore the test accesses it via reflection.
 * The test verifies that:
 * <ul>
 *   <li>the method returns a non‑null {@link SchemaValidatorsConfig} instance,</li>
 *   <li>each invocation yields a distinct object (no caching), and</li>
 *   <li>the returned object is of the expected type.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createSchemaValidatorsConfig_Test {

    /** Factory instance used for all tests. */
    private static JsonSchemaFactory factory;

    /** Reflection handle for the protected method under test. */
    private static Method createConfigMethod;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain a factory instance using the public API.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // Prepare reflective access to the protected method.
        createConfigMethod = JsonSchemaFactory.class
                .getDeclaredMethod("createSchemaValidatorsConfig");
        createConfigMethod.setAccessible(true);
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
        // Release static references.
        factory = null;
        createConfigMethod = null;
    }

    /**
     * Verify that the protected {@code createSchemaValidatorsConfig()} method returns a
     * fresh {@link SchemaValidatorsConfig} instance on each call.
     */
    @Test
    void createSchemaValidatorsConfig_returnsNewInstance() throws Exception {
        // First invocation
        Object result1 = createConfigMethod.invoke(factory);
        assertNotNull(result1, "First invocation should not return null");
        assertTrue(result1 instanceof SchemaValidatorsConfig,
                "First invocation should return a SchemaValidatorsConfig");

        // Second invocation
        Object result2 = createConfigMethod.invoke(factory);
        assertNotNull(result2, "Second invocation should not return null");
        assertTrue(result2 instanceof SchemaValidatorsConfig,
                "Second invocation should return a SchemaValidatorsConfig");

        // Ensure distinct objects (no caching)
        assertNotSame(result1, result2,
                "Each call should produce a new SchemaValidatorsConfig instance");
    }
}
```