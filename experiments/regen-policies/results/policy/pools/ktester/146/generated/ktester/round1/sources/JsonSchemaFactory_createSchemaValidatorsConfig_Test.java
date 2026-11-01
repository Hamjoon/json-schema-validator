package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createSchemaValidatorsConfig_Test {

    /**
     * Factory instance used for all tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * Reflection handle for the protected method under test.
     */
    private static Method createConfigMethod;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain a factory instance using the public API.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Prepare reflective access to the protected method.
        createConfigMethod = JsonSchemaFactory.class.getDeclaredMethod("createSchemaValidatorsConfig");
        createConfigMethod.setAccessible(true);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references.
        factory = null;
        createConfigMethod = null;
    }

    /**
     * Attempts to invoke a boolean getter. Tries the supplied method name first;
     * if not found, falls back to accessing a field directly.
     */
    private static boolean getBooleanProperty(Object target, String getterName, String fieldName) throws Exception {
        try {
            Object result = invokeGetter(target, getterName);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (NoSuchMethodException ignored) {
            // fall back to field access
        }
        // Direct field access (may be private)
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Boolean) field.get(target);
    }

    /**
     * Invokes a getter method with no arguments.
     */
    private static Object invokeGetter(Object target, String methodName) throws Exception {
        Method m = target.getClass().getMethod(methodName);
        return m.invoke(target);
    }

    /**
     * Invokes a setter method with a single argument.
     */
    private static void invokeSetter(Object target, String methodName, Object value) throws Exception {
        Method m = target.getClass().getMethod(methodName, value.getClass());
        m.invoke(target, value);
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
        assertTrue(result1 instanceof SchemaValidatorsConfig, "First invocation should return a SchemaValidatorsConfig");
        // Second invocation
        Object result2 = createConfigMethod.invoke(factory);
        assertNotNull(result2, "Second invocation should not return null");
        assertTrue(result2 instanceof SchemaValidatorsConfig, "Second invocation should return a SchemaValidatorsConfig");
        // Ensure distinct objects (no caching)
        assertNotSame(result1, result2, "Each call should produce a new SchemaValidatorsConfig instance");
    }

    /**
     * Verify that a newly created {@link SchemaValidatorsConfig} contains the library's
     * documented default values.
     */
    @Test
    void createSchemaValidatorsConfig_defaultFieldValuesIntegrity() throws Exception {
        SchemaValidatorsConfig config = (SchemaValidatorsConfig) createConfigMethod.invoke(factory);
        // Use public getters when available; fall back to reflection if necessary.
        assertFalse(getBooleanProperty(config, "isFailFast", "failFast"), "Default failFast should be false");
        assertFalse(getBooleanProperty(config, "isTypeLoose", "typeLoose"), "Default typeLoose should be false");
        assertFalse(getBooleanProperty(config, "isReadOnly", "readOnly"), "Default readOnly should be false");
        assertTrue(getBooleanProperty(config, "isAdditionalProperties", "additionalProperties"), "Default additionalProperties should be true");
        assertTrue(getBooleanProperty(config, "isFormat", "format"), "Default format should be true");
        assertTrue(getBooleanProperty(config, "isEnumCaseSensitive", "enumCaseSensitive"), "Default enumCaseSensitive should be true");
        // customMessage is expected to be null by default.
        Object customMessage = invokeGetter(config, "getCustomMessage");
        assertNull(customMessage, "Default customMessage should be null");
    }

    /**
     * Ensure that mutating a configuration returned from the factory does not affect
     * subsequent configurations (no shared mutable state).
     */
    @Test
    void createSchemaValidatorsConfig_isolationAfterMutation() throws Exception {
        // First config – we will mutate it.
        SchemaValidatorsConfig first = (SchemaValidatorsConfig) createConfigMethod.invoke(factory);
        // Mutate via a public setter if present; otherwise use reflection.
        invokeSetter(first, "setFailFast", true);
        // Second config – should retain default values.
        SchemaValidatorsConfig second = (SchemaValidatorsConfig) createConfigMethod.invoke(factory);
        assertFalse(getBooleanProperty(second, "isFailFast", "failFast"), "A new config should retain default failFast = false even after a previous config was mutated");
    }

    /**
     * Concurrently invoke {@code createSchemaValidatorsConfig()} from multiple threads
     * and verify each thread receives a distinct, correctly initialised instance.
     */
    @Test
    void createSchemaValidatorsConfig_concurrentCallsProduceDistinctInstances() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            List<Callable<SchemaValidatorsConfig>> tasks = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                tasks.add(() -> (SchemaValidatorsConfig) createConfigMethod.invoke(factory));
            }
            List<Future<SchemaValidatorsConfig>> futures = executor.invokeAll(tasks);
            Set<SchemaValidatorsConfig> uniqueConfigs = new HashSet<>();
            for (Future<SchemaValidatorsConfig> f : futures) {
                SchemaValidatorsConfig cfg = f.get(2, TimeUnit.SECONDS);
                assertNotNull(cfg, "Config returned from thread should not be null");
                assertTrue(cfg instanceof SchemaValidatorsConfig, "Returned object must be of type SchemaValidatorsConfig");
                // Verify default values for each instance.
                assertFalse(getBooleanProperty(cfg, "isFailFast", "failFast"));
                assertFalse(getBooleanProperty(cfg, "isTypeLoose", "typeLoose"));
                assertFalse(getBooleanProperty(cfg, "isReadOnly", "readOnly"));
                assertTrue(getBooleanProperty(cfg, "isAdditionalProperties", "additionalProperties"));
                assertTrue(getBooleanProperty(cfg, "isFormat", "format"));
                assertTrue(getBooleanProperty(cfg, "isEnumCaseSensitive", "enumCaseSensitive"));
                assertNull(invokeGetter(cfg, "getCustomMessage"));
                uniqueConfigs.add(cfg);
            }
            // Ensure every thread got a unique instance.
            assertEquals(THREAD_COUNT, uniqueConfigs.size(), "All concurrent calls should produce distinct SchemaValidatorsConfig instances");
        } finally {
            executor.shutdownNow();
        }
    }
}
