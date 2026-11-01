package com.networknt.schema;

import java.util.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    /**
     * Concrete subclass that lets the test set the validator list
     * and the initial {@code validatorsLoaded} flag.
     */
    private static class TestJsonSchema extends JsonSchema {

        private List<JsonValidator> testValidators;

        private boolean throwOnGetValidators;

        private RuntimeException exceptionFromGetValidators;

        TestJsonSchema(List<JsonValidator> validators, boolean validatorsLoaded) {
            // Use the simplest protected constructor of JsonSchema.
            super(null, null, null, null, null, false);
            this.testValidators = validators;
            // Initialise the private flag via reflection.
            try {
                Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
                f.setAccessible(true);
                f.set(this, validatorsLoaded);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
     * Simple {@link JsonValidator} implementation that records how many times
     * {@code preloadJsonSchema()} is invoked and can be configured to throw an
     * exception.
     */
    private static class StubValidator implements JsonValidator {

        private final AtomicInteger preloadCalls = new AtomicInteger(0);

        private RuntimeException toThrow;

        void setException(RuntimeException ex) {
            this.toThrow = ex;
        }

        int getPreloadCalls() {
            return preloadCalls.get();
        }

        @Override
        public void preloadJsonSchema() {
            if (toThrow != null) {
                throw toThrow;
            }
            preloadCalls.incrementAndGet();
        }

        /* The remaining methods of the JsonValidator interface are not needed for
         * these tests, so they are implemented as no‑ops. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean failFast) {
            return Collections.emptySet();
        }

        @Override
        public void setSchema(JsonSchema schema) {
            // no‑op
        }

        @Override
        public JsonSchema getSchema() {
            return null;
        }

        @Override
        public String getErrorMessageKey() {
            return null;
        }

        @Override
        public String getErrorMessage() {
            return null;
        }

        @Override
        public Keyword getKeyword() {
            return null;
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
        StubValidator validator1 = new StubValidator();
        StubValidator validator2 = new StubValidator();
        List<JsonValidator> validators = Arrays.asList(validator1, validator2);
        JsonSchema schema = new TestJsonSchema(validators, false);
        // WHEN initializeValidators() is invoked
        schema.initializeValidators();
        // THEN each validator's preloadJsonSchema() should be called exactly once
        Assertions.assertEquals(1, validator1.getPreloadCalls(), "validator1 should be preloaded once");
        Assertions.assertEquals(1, validator2.getPreloadCalls(), "validator2 should be preloaded once");
    }

    @Test
    void initializeValidators_doesNotCallPreloadWhenAlreadyLoaded(@Mock JsonValidator validator) {
        // GIVEN a JsonSchema whose validators are already marked as loaded
        StubValidator validator = new StubValidator();
        List<JsonValidator> validators = Collections.singletonList(validator);
        JsonSchema schema = new TestJsonSchema(validators, true);
        // WHEN initializeValidators() is invoked
        schema.initializeValidators();
        // THEN no preloadJsonSchema() call should occur
        Assertions.assertEquals(0, validator.getPreloadCalls(), "validator should not be preloaded when flag is already true");
    }

    @Test
    void initializeValidators_preloadThrowsException_stopsIteration(@Mock JsonValidator goodValidator, @Mock JsonValidator badValidator) throws Exception {
        // badValidator will throw when preloadJsonSchema() is called
        StubValidator badValidator = new StubValidator();
        badValidator.setException(new RuntimeException("preload failure"));
        StubValidator goodValidator = new StubValidator();
        List<JsonValidator> validators = Arrays.asList(badValidator, goodValidator);
        TestJsonSchema schema = new TestJsonSchema(validators, false);
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, schema::initializeValidators, "Exception from preloadJsonSchema should propagate");
        Assertions.assertEquals("preload failure", ex.getMessage());
        // Verify only the first (failing) validator was invoked
        Assertions.assertEquals(1, badValidator.getPreloadCalls(), "badValidator should have been invoked once");
        Assertions.assertEquals(0, goodValidator.getPreloadCalls(), "goodValidator must not have been invoked");
        // Flag must remain false because loading did not complete
        Assertions.assertFalse(isValidatorsLoaded(schema), "validatorsLoaded must remain false after exception");
    }

    @Test
    void initializeValidators_emptyValidatorList_setsFlag(@Mock JsonValidator validator) throws Exception {
        List<JsonValidator> empty = Collections.emptyList();
        TestJsonSchema schema = new TestJsonSchema(empty, false);
        schema.initializeValidators();
        // No validator to interact with – just check the flag
        Assertions.assertTrue(isValidatorsLoaded(schema), "validatorsLoaded should be true even with no validators");
    }

    @Test
    void initializeValidators_recoveryAfterException(@Mock JsonValidator healthy1, @Mock JsonValidator healthy2) throws Exception {
        // First call: a validator that throws
        StubValidator failing = new StubValidator();
        failing.setException(new RuntimeException("first failure"));
        TestJsonSchema schema = new TestJsonSchema(Collections.singletonList(failing), false);
        // First invocation – expect exception, flag stays false
        Assertions.assertThrows(RuntimeException.class, schema::initializeValidators);
        Assertions.assertFalse(isValidatorsLoaded(schema), "Flag must stay false after first failure");
        // Replace the validator list with healthy ones
        StubValidator healthy1 = new StubValidator();
        StubValidator healthy2 = new StubValidator();
        schema.setTestValidators(Arrays.asList(healthy1, healthy2));
        // Second invocation – should succeed
        schema.initializeValidators();
        Assertions.assertEquals(1, healthy1.getPreloadCalls(), "healthy1 should be preloaded once");
        Assertions.assertEquals(1, healthy2.getPreloadCalls(), "healthy2 should be preloaded once");
        Assertions.assertTrue(isValidatorsLoaded(schema), "Flag should become true after successful retry");
    }

    @Test
    void initializeValidators_nullElementInList_throwsNPE(@Mock JsonValidator afterNull) throws Exception {
        StubValidator afterNull = new StubValidator();
        List<JsonValidator> listWithNull = Arrays.asList(null, afterNull);
        TestJsonSchema schema = new TestJsonSchema(listWithNull, false);
        NullPointerException npe = Assertions.assertThrows(NullPointerException.class, schema::initializeValidators, "Calling preloadJsonSchema on a null element should throw NPE");
        // Ensure the second validator was never touched
        Assertions.assertEquals(0, afterNull.getPreloadCalls(), "afterNull must not have been invoked");
        Assertions.assertFalse(isValidatorsLoaded(schema), "validatorsLoaded must remain false after NPE");
    }

    @Test
    void initializeValidators_duplicateValidators_callsPreloadPerEntry(@Mock JsonValidator dup) throws Exception {
        StubValidator dup = new StubValidator();
        List<JsonValidator> dupList = Arrays.asList(dup, dup, dup);
        TestJsonSchema schema = new TestJsonSchema(dupList, false);
        schema.initializeValidators();
        Assertions.assertEquals(3, dup.getPreloadCalls(), "Duplicate validator should be preloaded for each occurrence");
        Assertions.assertTrue(isValidatorsLoaded(schema), "Flag should be true after processing duplicates");
    }

    @Test
    void initializeValidators_getValidatorsReturnsNull_throwsNPE() throws Exception {
        TestJsonSchema schema = new TestJsonSchema(null, false);
        // ensure getter returns null
        schema.setTestValidators(null);
        NullPointerException npe = Assertions.assertThrows(NullPointerException.class, schema::initializeValidators, "Null collection from getValidators should cause NPE");
        Assertions.assertFalse(isValidatorsLoaded(schema), "validatorsLoaded must stay false");
    }

    @Test
    void initializeValidators_getValidatorsThrowsException_propagates() throws Exception {
        TestJsonSchema schema = new TestJsonSchema(Collections.emptyList(), false);
        // force getter to throw
        schema.setThrowOnGetValidators(true);
        IllegalStateException ise = Assertions.assertThrows(IllegalStateException.class, schema::initializeValidators, "Exception from getValidators should propagate");
        Assertions.assertEquals("getValidators forced failure", ise.getMessage());
        Assertions.assertFalse(isValidatorsLoaded(schema), "Flag must remain false when getter fails");
    }
}
