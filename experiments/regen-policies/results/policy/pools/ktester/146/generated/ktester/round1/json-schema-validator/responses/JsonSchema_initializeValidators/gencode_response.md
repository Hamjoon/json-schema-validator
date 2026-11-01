```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#initializeValidators()} covering the
 * scenarios described in the JSON test‑case specification.
 *
 * <p>The tests use a small concrete subclass {@code TestableJsonSchema} that
 * allows us to inject a custom list of {@link JsonValidator}s (including {@code null}
 * or a list that throws an exception).  The private {@code validatorsLoaded}
 * flag is accessed via reflection to verify its state after each invocation.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    // -----------------------------------------------------------------------
    // Common mocks
    // -----------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    // -----------------------------------------------------------------------
    // Helper to read the private field {@code validatorsLoaded}
    // -----------------------------------------------------------------------
    private static boolean isValidatorsLoaded(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        return f.getBoolean(schema);
    }

    // -----------------------------------------------------------------------
    // Subclass that lets us control the list returned by getValidators()
    // -----------------------------------------------------------------------
    private static class TestableJsonSchema extends JsonSchema {
        private List<JsonValidator> injectedValidators;

        TestableJsonSchema(List<JsonValidator> validators,
                           boolean validatorsLoaded,
                           ValidationContext validationContext,
                           SchemaLocation schemaLocation,
                           JsonNodePath evaluationPath,
                           JsonNode schemaNode) {
            // The protected constructor of JsonSchema is used here.
            super(validators,
                  validatorsLoaded,
                  false,               // recursiveAnchor
                  null,                // typeValidator
                  null,                // id
                  false,               // suppressSubSchemaRetrieval
                  schemaNode,
                  validationContext,
                  null,                // errorMessageType
                  null,                // errorMessageKeyword
                  null,                // messageSource
                  null,                // keyword
                  null,                // parentSchema
                  schemaLocation,
                  evaluationPath,
                  null,                // evaluationParentSchema
                  Collections.emptyMap() // errorMessage map
            );
            this.injectedValidators = validators;
        }

        @Override
        public List<JsonValidator> getValidators() {
            return injectedValidators;
        }

        /** Allows the test to replace the validator list after construction. */
        void setValidators(List<JsonValidator> newList) {
            this.injectedValidators = newList;
        }
    }

    // -----------------------------------------------------------------------
    // Test cases
    // -----------------------------------------------------------------------

    @Test
    void initializeValidators_firstSuccessfulLoad() throws Exception {
        JsonValidator v = mock(JsonValidator.class);
        TestableJsonSchema schema = new TestableJsonSchema(
                List.of(v), false, validationContext, schemaLocation, evaluationPath, schemaNode);

        schema.initializeValidators();

        verify(v, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(schema), "validatorsLoaded should be true after successful load");
    }

    @Test
    void initializeValidators_subsequentCallAlreadyLoaded() throws Exception {
        JsonValidator v = mock(JsonValidator.class);
        TestableJsonSchema schema = new TestableJsonSchema(
                List.of(v), false, validationContext, schemaLocation, evaluationPath, schemaNode);

        // first call – loads
        schema.initializeValidators();
        verify(v, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(schema));

        reset(v); // clear interaction history

        // second call – should be a no‑op
        schema.initializeValidators();
        verifyNoInteractions(v);
        assertTrue(isValidatorsLoaded(schema), "validatorsLoaded must stay true");
    }

    @Test
    void initializeValidators_emptyValidatorList() throws Exception {
        TestableJsonSchema schema = new TestableJsonSchema(
                Collections.emptyList(), false, validationContext, schemaLocation, evaluationPath, schemaNode);

        schema.initializeValidators();

        // No exception and flag set to true
        assertTrue(isValidatorsLoaded(schema));
    }

    @Test
    void initializeValidators_preloadThrowsException() {
        JsonValidator v = mock(JsonValidator.class);
        doThrow(new RuntimeException("boom")).when(v).preloadJsonSchema();

        TestableJsonSchema schema = new TestableJsonSchema(
                List.of(v), false, validationContext, schemaLocation, evaluationPath, schemaNode);

        RuntimeException ex = assertThrows(RuntimeException.class,
                schema::initializeValidators,
                "Exception from preloadJsonSchema should propagate");
        assertEquals("boom", ex.getMessage());

        // validatorsLoaded must stay false
        assertDoesNotThrow(() -> assertFalse(isValidatorsLoaded(schema)));
    }

    @Test
    void initializeValidators_partialPreloadBeforeException() throws Exception {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        JsonValidator v3 = mock(JsonValidator.class);
        doThrow(new RuntimeException("fail")).when(v3).preloadJsonSchema();

        List<JsonValidator> list = new ArrayList<>();
        list.add(v1);
        list.add(v2);
        list.add(v3);
        list.add(mock(JsonValidator.class)); // fourth should never be called

        TestableJsonSchema schema = new TestableJsonSchema(
                list, false, validationContext, schemaLocation, evaluationPath, schemaNode);

        RuntimeException ex = assertThrows(RuntimeException.class,
                schema::initializeValidators);
        assertEquals("fail", ex.getMessage());

        // first two validators should have been called exactly once
        verify(v1, times(1)).preloadJsonSchema();
        verify(v2, times(1)).preloadJsonSchema();
        // third threw, fourth never called
        verify(v3, times(1)).preloadJsonSchema();
        verify(list.get(3), never()).preloadJsonSchema();

        // flag must remain false
        assertFalse(isValidatorsLoaded(schema));
    }

    @Test
    void initializeValidators_retryAfterPreviousFailure() throws Exception {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        // first call: v2 throws
        doThrow(new RuntimeException("first")).when(v2).preloadJsonSchema();

        List<JsonValidator> list = List.of(v1, v2);
        TestableJsonSchema schema = new TestableJsonSchema(
                list, false, validationContext, schemaLocation, evaluationPath, schemaNode);

        // first attempt – exception
        assertThrows(RuntimeException.class, schema::initializeValidators);
        assertFalse(isValidatorsLoaded(schema));

        // reset the behaviour so that v2 no longer throws
        reset(v2);
        // second attempt – should succeed
        schema.initializeValidators();

        verify(v1, times(1)).preloadJsonSchema();
        verify(v2, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(schema));
    }

    @Test
    void initializeValidators_multipleValidatorsEachCalledOnce() throws Exception {
        List<JsonValidator> validators = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            validators.add(mock(JsonValidator.class));
        }

        TestableJsonSchema schema = new TestableJsonSchema(
                validators, false, validationContext, schemaLocation, evaluationPath, schemaNode);

        schema.initializeValidators();

        for (JsonValidator v : validators) {
            verify(v, times(1)).preloadJsonSchema();
        }
        assertTrue(isValidatorsLoaded(schema));
    }

    @Test
    void initializeValidators_alreadyLoadedButValidatorListChanged() throws Exception {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);

        TestableJsonSchema schema = new TestableJsonSchema(
                List.of(v1), false, validationContext, schemaLocation, evaluationPath, schemaNode);

        // First successful load
        schema.initializeValidators();
        verify(v1, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(schema));

        // Change validator list after loading
        schema.setValidators(List.of(v1, v2));

        // Second call – should be a no‑op, v2 must not be touched
        reset(v1, v2);
        schema.initializeValidators();
        verifyNoInteractions(v1, v2);
        assertTrue(isValidatorsLoaded(schema));
    }

    @Test
    void initializeValidators_getValidatorsReturnsNull() {
        TestableJsonSchema schema = new TestableJsonSchema(
                null, false, validationContext, schemaLocation, evaluationPath, schemaNode);

        // The for‑each loop will throw NPE; we assert it propagates
        NullPointerException ex = assertThrows(NullPointerException.class,
                schema::initializeValidators);
        assertFalse(isValidatorsLoaded(schema));
    }

    @Test
    void initializeValidators_validatorListContainsNullElement() {
        JsonValidator v = mock(JsonValidator.class);
        List<JsonValidator> listWithNull = new ArrayList<>();
        listWithNull.add(v);
        listWithNull.add(null); // second element is null

        TestableJsonSchema schema = new TestableJsonSchema(
                listWithNull, false, validationContext, schemaLocation, evaluationPath, schemaNode);

        // Expect NPE when trying to call preloadJsonSchema on null
        NullPointerException ex = assertThrows(NullPointerException.class,
                schema::initializeValidators);
        // First validator should have been invoked before the failure
        verify(v, times(1)).preloadJsonSchema();
        assertFalse(isValidatorsLoaded(schema));
    }
}
```