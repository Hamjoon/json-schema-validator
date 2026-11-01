package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    /**
     * Concrete subclass that lets the test set the validator list
     *  and the initial {@code validatorsLoaded} flag.
     */
    private static class TestJsonSchema extends JsonSchema {

        private List<JsonValidator> testValidators;

        private boolean throwOnGetValidators;

        private RuntimeException exceptionFromGetValidators;

        TestJsonSchema(List<JsonValidator> validators, boolean validatorsLoaded) {
            // Most constructor arguments are irrelevant for these tests,
            // so we forward dummy values to the protected super‑constructor.
            super(// validators
            validators, // validatorsLoaded
            validatorsLoaded, // recursiveAnchor
            false, // typeValidator
            null, // id
            null, // suppressSubSchemaRetrieval
            false, // schemaNode
            null, // validationContext
            null, // errorMessageType
            null, // errorMessageKeyword
            null, // messageSource
            null, // keyword
            null, // parentSchema
            null, // schemaLocation
            null, // evaluationPath
            null, // evaluationParentSchema
            null, // errorMessage map
            Collections.emptyMap());
            this.testValidators = validators;
        }

        /**
         * Allows the test to replace the validator list between calls.
         */
        void setTestValidators(List<JsonValidator> validators) {
            this.testValidators = validators;
        }

        /**
         * Configures the getter to throw a supplied exception.
         */
        void setExceptionOnGetValidators(RuntimeException ex) {
            this.exceptionFromGetValidators = ex;
        }

        /**
         * Configures the getter to throw a generic exception (simulating a bug).
         */
        void setThrowOnGetValidators(boolean flag) {
            this.throwOnGetValidators = flag;
        }

        @Override
        public List<JsonValidator> getValidators() {
            if (throwOnGetValidators) {
                throw new IllegalStateException("getValidators forced failure");
            }
            if (exceptionFromGetValidators != null) {
                throw exceptionFromGetValidators;
            }
            return testValidators;
        }
    }

    /**
     * Helper to read the private {@code validatorsLoaded} flag via reflection.
     */
    private static boolean isValidatorsLoaded(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        return (boolean) f.get(schema);
    }

    @Test
    void initializeValidators_callsPreloadWhenNotYetLoaded(@Mock JsonValidator validator1, @Mock JsonValidator validator2) {
        // GIVEN a JsonSchema whose validators have not been loaded yet
        List<JsonValidator> validators = Arrays.asList(validator1, validator2);
        JsonSchema schema = new TestJsonSchema(validators, false);
        // WHEN initializeValidators() is invoked
        schema.initializeValidators();
        // THEN each validator's preloadJsonSchema() should be called exactly once
        verify(validator1, times(1)).preloadJsonSchema();
        verify(validator2, times(1)).preloadJsonSchema();
    }

    @Test
    void initializeValidators_doesNotCallPreloadWhenAlreadyLoaded(@Mock JsonValidator validator) {
        // GIVEN a JsonSchema whose validators are already marked as loaded
        List<JsonValidator> validators = Collections.singletonList(validator);
        JsonSchema schema = new TestJsonSchema(validators, true);
        // WHEN initializeValidators() is invoked
        schema.initializeValidators();
        // THEN no preloadJsonSchema() call should occur
        verify(validator, never()).preloadJsonSchema();
    }

    @Test
    void initializeValidators_preloadThrowsException_stopsIteration(@Mock JsonValidator goodValidator, @Mock JsonValidator badValidator) throws Exception {
        // badValidator will throw when preloadJsonSchema() is called
        doThrow(new RuntimeException("preload failure")).when(badValidator).preloadJsonSchema();
        List<JsonValidator> validators = Arrays.asList(badValidator, goodValidator);
        TestJsonSchema schema = new TestJsonSchema(validators, false);
        RuntimeException ex = assertThrows(RuntimeException.class, schema::initializeValidators, "Exception from preloadJsonSchema should propagate");
        assertEquals("preload failure", ex.getMessage());
        // Verify only the first (failing) validator was invoked
        verify(badValidator, times(1)).preloadJsonSchema();
        verify(goodValidator, never()).preloadJsonSchema();
        // Flag must remain false because loading did not complete
        assertFalse(isValidatorsLoaded(schema), "validatorsLoaded must remain false after exception");
    }

    @Test
    void initializeValidators_emptyValidatorList_setsFlag(@Mock JsonValidator validator) throws Exception {
        List<JsonValidator> empty = Collections.emptyList();
        TestJsonSchema schema = new TestJsonSchema(empty, false);
        schema.initializeValidators();
        // No interaction with any validator
        verifyNoInteractions(validator);
        assertTrue(isValidatorsLoaded(schema), "validatorsLoaded should be true even with no validators");
    }

    @Test
    void initializeValidators_recoveryAfterException(@Mock JsonValidator healthy1, @Mock JsonValidator healthy2) throws Exception {
        // First call: a validator that throws
        JsonValidator failing = mock(JsonValidator.class);
        doThrow(new RuntimeException("first failure")).when(failing).preloadJsonSchema();
        TestJsonSchema schema = new TestJsonSchema(Arrays.asList(failing), false);
        // First invocation – expect exception, flag stays false
        assertThrows(RuntimeException.class, schema::initializeValidators);
        assertFalse(isValidatorsLoaded(schema), "Flag must stay false after first failure");
        // Replace the validator list with healthy ones
        schema.setTestValidators(Arrays.asList(healthy1, healthy2));
        // Second invocation – should succeed
        schema.initializeValidators();
        verify(healthy1, times(1)).preloadJsonSchema();
        verify(healthy2, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(schema), "Flag should become true after successful retry");
    }

    @Test
    void initializeValidators_nullElementInList_throwsNPE(@Mock JsonValidator afterNull) throws Exception {
        List<JsonValidator> listWithNull = Arrays.asList(null, afterNull);
        TestJsonSchema schema = new TestJsonSchema(listWithNull, false);
        NullPointerException npe = assertThrows(NullPointerException.class, schema::initializeValidators, "Calling preloadJsonSchema on a null element should throw NPE");
        // Ensure the second validator was never touched
        verify(afterNull, never()).preloadJsonSchema();
        assertFalse(isValidatorsLoaded(schema), "validatorsLoaded must remain false after NPE");
    }

    @Test
    void initializeValidators_duplicateValidators_callsPreloadPerEntry(@Mock JsonValidator dup) throws Exception {
        List<JsonValidator> dupList = Arrays.asList(dup, dup, dup);
        TestJsonSchema schema = new TestJsonSchema(dupList, false);
        schema.initializeValidators();
        verify(dup, times(3)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(schema), "Flag should be true after processing duplicates");
    }

    @Test
    void initializeValidators_getValidatorsReturnsNull_throwsNPE() throws Exception {
        TestJsonSchema schema = new TestJsonSchema(null, false);
        // ensure getter returns null
        schema.setTestValidators(null);
        NullPointerException npe = assertThrows(NullPointerException.class, schema::initializeValidators, "Null collection from getValidators should cause NPE");
        assertFalse(isValidatorsLoaded(schema), "validatorsLoaded must stay false");
    }

    @Test
    void initializeValidators_getValidatorsThrowsException_propagates() throws Exception {
        TestJsonSchema schema = new TestJsonSchema(Collections.emptyList(), false);
        // force getter to throw
        schema.setThrowOnGetValidators(true);
        IllegalStateException ise = assertThrows(IllegalStateException.class, schema::initializeValidators, "Exception from getValidators should propagate");
        assertEquals("getValidators forced failure", ise.getMessage());
        assertFalse(isValidatorsLoaded(schema), "Flag must remain false when getter fails");
    }
}
