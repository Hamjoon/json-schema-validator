package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    /**
     * The JsonSchema instance under test (a spy so we can stub getValidators()).
     */
    private JsonSchema jsonSchemaSpy;

    /**
     * A mock validator that will be returned by getValidators().
     */
    @Mock
    JsonValidator mockValidator;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣  Create a real JsonSchema instance via reflection (its constructor is private)
        // --------------------------------------------------------------------
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonSchema realInstance = ctor.newInstance(validationContext, schemaLocation, evaluationPath, schemaNode, // parent
        null, // suppressSubSchemaRetrieval
        false);
        // --------------------------------------------------------------------
        // 2️⃣  Spy the instance so we can stub getValidators()
        // --------------------------------------------------------------------
        jsonSchemaSpy = spy(realInstance);
        // --------------------------------------------------------------------
        // 3️⃣  Stub getValidators() to return a list containing our mock validator
        // --------------------------------------------------------------------
        List<JsonValidator> validators = Collections.singletonList(mockValidator);
        doReturn(validators).when(jsonSchemaSpy).getValidators();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks after each test
        reset(validationContext, schemaLocation, evaluationPath, schemaNode);
    }

    /**
     * Helper to set the private field {@code validatorsLoaded} via reflection.
     */
    private void setValidatorsLoaded(JsonSchema schema, boolean value) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        f.setBoolean(schema, value);
    }

    /**
     * Helper to read the private field {@code validatorsLoaded} via reflection.
     */
    private boolean isValidatorsLoaded(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        return f.getBoolean(schema);
    }

    @Test
    void initializeValidators_firstCall_loadsValidatorsAndSetsFlag() throws Exception {
        // Pre‑condition: flag must be false (default)
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        // WHEN
        jsonSchemaSpy.initializeValidators();
        // THEN
        // 1️⃣  The validator's preloadJsonSchema() must have been invoked exactly once
        verify(mockValidator, times(1)).preloadJsonSchema();
        // 2️⃣  The internal flag must now be true
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
    }

    @Test
    void initializeValidators_subsequentCall_doesNotReloadWhenFlagIsTrue() throws Exception {
        JsonValidator mockValidator = mock(JsonValidator.class);
        doReturn(Collections.singletonList(mockValidator)).when(jsonSchemaSpy).getValidators();
        setValidatorsLoaded(jsonSchemaSpy, true);
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
        jsonSchemaSpy.initializeValidators();
        verify(mockValidator, never()).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
    }

    /* --------------------------------------------------------------
     * 3️⃣  Exception during preload – flag must stay false
     * -------------------------------------------------------------- */
    @Test
    void initializeValidators_exceptionDuringPreload_propagatesAndKeepsFlagFalse() throws Exception {
        JsonValidator mockValidator = mock(JsonValidator.class);
        doThrow(new RuntimeException("preload failure")).when(mockValidator).preloadJsonSchema();
        doReturn(Collections.singletonList(mockValidator)).when(jsonSchemaSpy).getValidators();
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchemaSpy.initializeValidators());
        assertEquals("preload failure", ex.getMessage());
        // flag must stay false because loading aborted
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        // verify that the method was indeed called once before exception
        verify(mockValidator, times(1)).preloadJsonSchema();
    }

    /* --------------------------------------------------------------
     * 4️⃣  Empty validators list – flag becomes true, no preload calls
     * -------------------------------------------------------------- */
    @Test
    void initializeValidators_emptyValidatorsList_setsFlagWithoutCallingPreload() throws Exception {
        doReturn(Collections.emptyList()).when(jsonSchemaSpy).getValidators();
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        jsonSchemaSpy.initializeValidators();
        // No validator => nothing to verify, just ensure flag is true
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
    }

    /* --------------------------------------------------------------
     * 5️⃣  Partial failure – middle validator throws, later ones not called
     * -------------------------------------------------------------- */
    @Test
    void initializeValidators_partialFailure_abortsAndKeepsFlagFalse() throws Exception {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        JsonValidator v3 = mock(JsonValidator.class);
        // v2 throws, v1 succeeds, v3 should never be invoked
        doNothing().when(v1).preloadJsonSchema();
        doThrow(new RuntimeException("second fails")).when(v2).preloadJsonSchema();
        doReturn(Arrays.asList(v1, v2, v3)).when(jsonSchemaSpy).getValidators();
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchemaSpy.initializeValidators());
        assertEquals("second fails", ex.getMessage());
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        // verify first called, second called (exception), third never called
        verify(v1, times(1)).preloadJsonSchema();
        verify(v2, times(1)).preloadJsonSchema();
        verify(v3, never()).preloadJsonSchema();
    }

    /* --------------------------------------------------------------
     * 6️⃣  Retry after previous failure – second call succeeds
     * -------------------------------------------------------------- */
    @Test
    void initializeValidators_retryAfterFailure_successfulSecondCall() throws Exception {
        JsonValidator v = mock(JsonValidator.class);
        // First call: throw exception
        doThrow(new RuntimeException("first fail")).when(v).preloadJsonSchema();
        // Stub getValidators() to always return the same list
        doReturn(Collections.singletonList(v)).when(jsonSchemaSpy).getValidators();
        // ---- first attempt ----
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchemaSpy.initializeValidators());
        assertEquals("first fail", ex.getMessage());
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        // called once
        verify(v, times(1)).preloadJsonSchema();
        // ---- reset mock for second attempt ----
        reset(v);
        // now succeeds
        doNothing().when(v).preloadJsonSchema();
        // second attempt
        jsonSchemaSpy.initializeValidators();
        // called once more
        verify(v, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
    }

    /* --------------------------------------------------------------
     * 7️⃣  Concurrent first‑time initialization – only one preload per validator
     * -------------------------------------------------------------- */
    @Test
    void initializeValidators_concurrentFirstTime_initializesOnlyOnce() throws Exception {
        // Use a real mock that blocks to increase chance of race condition
        JsonValidator mockValidator = mock(JsonValidator.class);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        // Make preloadJsonSchema wait until both threads have entered the method
        doAnswer(invocation -> {
            // signal entry
            startLatch.countDown();
            // wait for test to release
            finishLatch.await(5, TimeUnit.SECONDS);
            return null;
        }).when(mockValidator).preloadJsonSchema();
        doReturn(Collections.singletonList(mockValidator)).when(jsonSchemaSpy).getValidators();
        ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            // Two concurrent tasks calling initializeValidators()
            Callable<Void> task = () -> {
                jsonSchemaSpy.initializeValidators();
                return null;
            };
            Future<Void> f1 = exec.submit(task);
            Future<Void> f2 = exec.submit(task);
            // Wait until both threads have reached preloadJsonSchema()
            assertTrue(startLatch.await(2, TimeUnit.SECONDS), "Both threads should reach preload");
            // Release the latch so both can finish
            finishLatch.countDown();
            // Ensure both complete without exception
            f1.get(2, TimeUnit.SECONDS);
            f2.get(2, TimeUnit.SECONDS);
        } finally {
            exec.shutdownNow();
        }
        // Even though two threads called the method, preloadJsonSchema should have been invoked only once
        verify(mockValidator, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
    }

    /* --------------------------------------------------------------
     * 8️⃣  Null validators list – NullPointerException and flag stays false
     * -------------------------------------------------------------- */
    @Test
    void initializeValidators_nullValidatorsList_throwsNPEAndKeepsFlagFalse() throws Exception {
        doReturn(null).when(jsonSchemaSpy).getValidators();
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
        NullPointerException npe = assertThrows(NullPointerException.class, () -> jsonSchemaSpy.initializeValidators());
        // The exact message is not defined; we only assert that NPE occurs
        assertNotNull(npe);
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));
    }
}
