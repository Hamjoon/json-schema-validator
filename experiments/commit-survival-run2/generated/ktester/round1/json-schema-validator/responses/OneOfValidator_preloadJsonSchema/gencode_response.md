```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentModificationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    ValidationContext validationContext;          // mock for the ValidationContext dependency

    @Mock
    JsonSchema parentSchema;                       // parent schema – not used in the tested method

    @Mock
    JsonSchema childSchema1;                       // generic mock schema

    @Mock
    JsonSchema childSchema2;                       // generic mock schema

    OneOfValidator validator;                       // instance under test

    /**
     * Helper that creates an {@link OneOfValidator} from a JSON array string.
     *
     * @param jsonArray the JSON text representing an array of subschemas
     */
    private OneOfValidator createValidator(String jsonArray) throws IOException {
        JsonNode schemaNode = MAPPER.readTree(jsonArray);
        // The constructor will invoke validationContext.newSchema for each array element.
        // Stubbing of those calls is performed in the test methods that need it.
        return new OneOfValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    /**
     * Utility to replace the private {@code schemas} field via reflection.
     *
     * @param validator the validator instance whose field we want to set
     * @param list      the list (or null) to assign to the field
     */
    private void setSchemasField(OneOfValidator validator, List<JsonSchema> list) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, list);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mocks before each test to avoid cross‑test interference
        reset(validationContext, childSchema1, childSchema2);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up for these unit tests
    }

    /* --------------------------------------------------------------------- *
     * Existing tests from the scaffold (kept unchanged)                     *
     * --------------------------------------------------------------------- */

    @Test
    void preloadJsonSchema_callsInitializeValidatorsOnAllChildSchemas() throws Exception {
        // GIVEN – a OneOfValidator built from an array with two subschemas
        validator = createValidator("[{}, {}]");

        // stub ValidationContext.newSchema to return our two mock child schemas
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1, childSchema2);

        // Re‑create the validator now that the stubbing is in place – the constructor will
        // populate the internal `schemas` list with the two mocks.
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}, {}]"), parentSchema, validationContext);

        // WHEN – preloadJsonSchema is invoked
        validator.preloadJsonSchema();

        // THEN – each child schema should have its initializeValidators() method called exactly once
        verify(childSchema1, times(1)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();
        // No other interactions should happen on the child schemas
        verifyNoMoreInteractions(childSchema1, childSchema2);
    }

    @Test
    void preloadJsonSchema_withEmptySchemaArray_doesNotInvokeInitializeValidators() throws Exception {
        // GIVEN – a OneOfValidator built from an empty array
        validator = createValidator("[]");

        // No need to stub newSchema because the constructor will not call it (array is empty)

        // WHEN – preloadJsonSchema is invoked
        validator.preloadJsonSchema();

        // THEN – there are no child schemas, therefore no interactions with any mock
        verifyNoInteractions(childSchema1, childSchema2);
    }

    /* --------------------------------------------------------------------- *
     * Additional test groups derived from the JSON specification            *
     * --------------------------------------------------------------------- */

    @Test
    void preloadJsonSchema_emptySchemasList_noInteraction() throws Exception {
        // GROUP: Empty schemas list
        validator = createValidator("[]");
        // Ensure internal list is really empty (constructor already did that)
        validator.preloadJsonSchema();
        verifyNoInteractions(childSchema1, childSchema2);
    }

    @Test
    void preloadJsonSchema_singleSchema_initializeOnce() throws Exception {
        // GROUP: Single schema
        validator = createValidator("[{}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1);
        // Re‑create to have the mock inside the list
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}]"), parentSchema, validationContext);
        validator.preloadJsonSchema();
        verify(childSchema1, times(1)).initializeValidators();
    }

    @Test
    void preloadJsonSchema_multipleSchemas_initializeOncePerSchema() throws Exception {
        // GROUP: Multiple schemas (three)
        validator = createValidator("[{}, {}, {}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1, childSchema2, childSchema1); // reuse mocks for brevity
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}, {}, {}]"), parentSchema, validationContext);
        validator.preloadJsonSchema();

        // childSchema1 appears twice, so it should be called twice
        verify(childSchema1, times(2)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();
    }

    @Test
    void preloadJsonSchema_exceptionPropagation_runtimeException() throws Exception {
        // GROUP: Exception propagation (RuntimeException)
        validator = createValidator("[{}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1);
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}]"), parentSchema, validationContext);
        doThrow(new RuntimeException("boom")).when(childSchema1).initializeValidators();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.preloadJsonSchema());
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void preloadJsonSchema_duplicateSchemaInstances_callsTwice() throws Exception {
        // GROUP: Duplicate schema instances
        validator = createValidator("[{}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1);
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}]"), parentSchema, validationContext);

        // Insert the same mock twice via reflection
        List<JsonSchema> list = new ArrayList<>();
        list.add(childSchema1);
        list.add(childSchema1);
        setSchemasField(validator, list);

        validator.preloadJsonSchema();

        // The mock should be called twice (once per list entry)
        verify(childSchema1, times(2)).initializeValidators();
    }

    @Test
    void preloadJsonSchema_nullSchemaElement_throwsNullPointerException() throws Exception {
        // GROUP: Null schema element
        validator = createValidator("[{}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1);
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}]"), parentSchema, validationContext);

        List<JsonSchema> list = new ArrayList<>();
        list.add(childSchema1);
        list.add(null); // null entry
        list.add(childSchema1);
        setSchemasField(validator, list);

        assertThrows(NullPointerException.class, () -> validator.preloadJsonSchema());
    }

    @Test
    void preloadJsonSchema_concurrentModificationDuringIteration_throwsException() throws Exception {
        // GROUP: Concurrent modification during iteration
        validator = createValidator("[{}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1);
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}]"), parentSchema, validationContext);

        List<JsonSchema> list = new ArrayList<>();
        list.add(childSchema1);
        setSchemasField(validator, list);

        // When initializeValidators() is called, add another element to the list
        doAnswer(invocation -> {
            list.add(mock(JsonSchema.class));
            return null;
        }).when(childSchema1).initializeValidators();

        assertThrows(ConcurrentModificationException.class, () -> validator.preloadJsonSchema());
    }

    @Test
    void preloadJsonSchema_idempotentCall_callsInitializeEachTime() throws Exception {
        // GROUP: Idempotent call
        validator = createValidator("[{}, {}, {}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1, childSchema2, mock(JsonSchema.class));
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}, {}, {}]"), parentSchema, validationContext);

        // first invocation
        validator.preloadJsonSchema();

        // second invocation
        validator.preloadJsonSchema();

        // Verify each mock was called twice
        verify(childSchema1, times(2)).initializeValidators();
        verify(childSchema2, times(2)).initializeValidators();
        // The third mock (created inline) is not stored; we can't verify it directly.
    }

    @Test
    void preloadJsonSchema_nullInternalSchemasList_throwsNullPointerException() throws Exception {
        // GROUP: Null internal schemas list
        validator = createValidator("[]");
        // Force the private field to null
        setSchemasField(validator, null);
        assertThrows(NullPointerException.class, () -> validator.preloadJsonSchema());
    }

    @Test
    void preloadJsonSchema_errorPropagation_assertionError() throws Exception {
        // GROUP: Error propagation (AssertionError)
        validator = createValidator("[{}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1);
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}]"), parentSchema, validationContext);
        doThrow(new AssertionError("assertion failed")).when(childSchema1).initializeValidators();

        AssertionError err = assertThrows(AssertionError.class, () -> validator.preloadJsonSchema());
        assertEquals("assertion failed", err.getMessage());
    }
}
```