```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#createSchemaValidatorsConfig()}.
 *
 * <p>The method is {@code protected}, but because this test resides in the same
 * package it can be accessed directly. The test suite covers:
 *
 * <ul>
 *   <li>Non‑null return value.</li>
 *   <li>Distinct instances on successive calls.</li>
 *   <li>Default configuration flag values.</li>
 *   <li>Independence of mutable flags between instances.</li>
 *   <li>Thread‑safety – each thread receives its own config object.</li>
 *   <li>Independence across different {@link JsonSchemaFactory} objects.</li>
 *   <li>Invocation via reflection (useful when the method visibility changes).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createSchemaValidatorsConfig_Test {

    /** Factory created once for the whole test class. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public builder API; the default meta‑schema is supplied internally.
        factory = JsonSchemaFactory.builder().build();
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /* Basic contract tests */
    /* --------------------------------------------------------------------- */

    @Test
    void createSchemaValidatorsConfig_returnsNonNullInstance() {
        SchemaValidatorsConfig config = factory.createSchemaValidatorsConfig();
        assertNotNull(config, "createSchemaValidatorsConfig() should not return null");
        assertTrue(config instanceof SchemaValidatorsConfig,
                "Returned object should be an instance of SchemaValidatorsConfig");
    }

    @Test
    void createSchemaValidatorsConfig_returnsDistinctInstances() {
        SchemaValidatorsConfig first = factory.createSchemaValidatorsConfig();
        SchemaValidatorsConfig second = factory.createSchemaValidatorsConfig();

        assertNotNull(first, "First config instance should not be null");
        assertNotNull(second, "Second config instance should not be null");
        assertNotSame(first, second,
                "Each call should produce a new SchemaValidatorsConfig instance");
    }

    @Test
    void createSchemaValidatorsConfig_worksWithMultipleFactoryInstances() {
        JsonSchemaFactory anotherFactory = JsonSchemaFactory.builder().build();

        SchemaValidatorsConfig configFromFirst = factory.createSchemaValidatorsConfig();
        SchemaValidatorsConfig configFromSecond = anotherFactory.createSchemaValidatorsConfig();

        assertNotNull(configFromFirst);
        assertNotNull(configFromSecond);
        assertNotSame(configFromFirst, configFromSecond,
                "Configs from different factories must be distinct");
    }

    /* --------------------------------------------------------------------- */
    /* Default values verification */
    /* --------------------------------------------------------------------- */

    @Test
    void createSchemaValidatorsConfig_hasLibraryDefaultValues() {
        SchemaValidatorsConfig cfg = factory.createSchemaValidatorsConfig();

        // The actual default flags are defined in SchemaValidatorsConfig.
        // The test assumes the following typical defaults – adjust if the
        // library changes.
        assertFalse(cfg.isFailFast(), "Default failFast should be false");
        assertFalse(cfg.isTypeLoose(), "Default typeLoose should be false");
        assertFalse(cfg.isReadOnly(), "Default readOnly should be false");
        assertFalse(cfg.isOpenAPI30(), "Default openAPI30 should be false");
        // Add more assertions here if SchemaValidatorsConfig exposes additional flags.
    }

    /* --------------------------------------------------------------------- */
    /* Independence of mutable flags between instances */
    /* --------------------------------------------------------------------- */

    @Test
    void modifyingFirstInstanceDoesNotAffectSecond() {
        SchemaValidatorsConfig first = factory.createSchemaValidatorsConfig();
        SchemaValidatorsConfig second = factory.createSchemaValidatorsConfig();

        // Mutate a flag on the first config.
        first.setFailFast(true);

        // Verify that the second config still holds the default value.
        assertTrue(first.isFailFast(), "First config should reflect the mutated value");
        assertFalse(second.isFailFast(),
                "Second config must retain the default value, proving independence");
    }

    /* --------------------------------------------------------------------- */
    /* Thread‑safety test – each thread should obtain its own instance */
    /* --------------------------------------------------------------------- */

    @Test
    void eachThreadReceivesDistinctConfigInstance() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        Set<SchemaValidatorsConfig> configs = Collections.synchronizedSet(new HashSet<>());

        Callable<Void> task = () -> {
            SchemaValidatorsConfig cfg = factory.createSchemaValidatorsConfig();
            assertNotNull(cfg, "Config obtained in thread must not be null");
            configs.add(cfg);
            return null;
        };

        // Submit tasks and wait for completion.
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(task);
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
                "Executor did not terminate in time");

        assertEquals(THREAD_COUNT, configs.size(),
                "All threads should have received distinct SchemaValidatorsConfig instances");
    }

    /* --------------------------------------------------------------------- */
    /* Independence across different factories (already partially covered) */
    /* --------------------------------------------------------------------- */

    @Test
    void configsFromDifferentFactoriesAreIndependent() {
        JsonSchemaFactory factoryA = JsonSchemaFactory.builder().build();
        JsonSchemaFactory factoryB = JsonSchemaFactory.builder().build();

        SchemaValidatorsConfig cfgA = factoryA.createSchemaValidatorsConfig();
        SchemaValidatorsConfig cfgB = factoryB.createSchemaValidatorsConfig();

        // Mutate cfgA and ensure cfgB stays untouched.
        cfgA.setReadOnly(true);
        assertTrue(cfgA.isReadOnly(), "cfgA should reflect the mutation");
        assertFalse(cfgB.isReadOnly(),
                "cfgB must retain default value, proving no cross‑factory sharing");
    }

    /* --------------------------------------------------------------------- */
    /* Reflection based invocation */
    /* --------------------------------------------------------------------- */

    @Test
    void createSchemaValidatorsConfig_viaReflection_returnsNonNullInstance() throws Exception {
        // Locate the protected method via reflection.
        Method method = JsonSchemaFactory.class.getDeclaredMethod("createSchemaValidatorsConfig");
        method.setAccessible(true);

        // Invoke on the existing factory instance.
        Object result = method.invoke(factory);
        assertNotNull(result, "Reflection invocation should not return null");
        assertTrue(result instanceof SchemaValidatorsConfig,
                "Reflection result must be a SchemaValidatorsConfig");
    }
}
```