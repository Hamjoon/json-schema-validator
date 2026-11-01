```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

    // -------------------------------------------------------------------------
    // Mocks required for the BaseJsonValidator constructor
    // -------------------------------------------------------------------------
    @Mock SchemaLocation mockSchemaLocation;
    @Mock JsonNodePath mockEvaluationPath;
    @Mock JsonNode mockSchemaNode;
    @Mock JsonSchema mockParentSchema;
    @Mock ValidatorTypeCode mockValidatorType;
    @Mock ValidationContext mockValidationContext;

    // Concrete subclass exposing the protected method
    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        Mockito.reset(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    // -------------------------------------------------------------------------
    // Helper concrete subclass
    // -------------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validatorType, validationContext);
        }

        /** Public wrapper that delegates to the protected method under test. */
        void callPreloadJsonSchemas(Collection<JsonSchema> schemas) {
            super.preloadJsonSchemas(schemas);
        }
    }

    // -------------------------------------------------------------------------
    // Existing basic tests (kept unchanged)
    // -------------------------------------------------------------------------
    @Test
    void shouldInvokeInitializeValidatorsOnEachSchema() {
        // GIVEN a collection of JsonSchema mocks
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);

        // WHEN the protected method is exercised via the public wrapper
        validator.callPreloadJsonSchemas(schemas);

        // THEN each schema's initializeValidators() must be called exactly once
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
        verifyNoMoreInteractions(schema1, schema2);
    }

    @Test
    void shouldDoNothingWhenCollectionIsEmpty() {
        // GIVEN an empty collection
        Collection<JsonSchema> empty = Collections.emptyList();

        // WHEN the method is invoked
        validator.callPreloadJsonSchemas(empty);

        // THEN no exception is thrown – the test passes.
    }

    // -------------------------------------------------------------------------
    // Additional test cases derived from the JSON specification
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowNPEWhenCollectionIsNull() {
        // GIVEN a null reference
        Collection<JsonSchema> nullCollection = null;

        // WHEN/THEN the method propagates a NullPointerException
        assertThrows(NullPointerException.class,
                () -> validator.callPreloadJsonSchemas(nullCollection));
    }

    @Test
    void shouldPropagateRuntimeExceptionFromSchema() {
        // GIVEN a schema whose initializeValidators throws RuntimeException
        JsonSchema badSchema = mock(JsonSchema.class);
        doThrow(new RuntimeException("boom")).when(badSchema).initializeValidators();

        Collection<JsonSchema> schemas = Collections.singletonList(badSchema);

        // WHEN/THEN the RuntimeException bubbles up unchanged
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.callPreloadJsonSchemas(schemas));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void shouldPropagateErrorFromSchema() {
        // GIVEN a schema whose initializeValidators throws an Error
        JsonSchema errorSchema = mock(JsonSchema.class);
        doThrow(new AssertionError("assertion failure")).when(errorSchema).initializeValidators();

        Collection<JsonSchema> schemas = Collections.singletonList(errorSchema);

        // WHEN/THEN the Error propagates unchanged
        AssertionError err = assertThrows(AssertionError.class,
                () -> validator.callPreloadJsonSchemas(schemas));
        assertEquals("assertion failure", err.getMessage());
    }

    @Test
    void shouldStopIterationWhenExceptionOccurs() {
        // GIVEN three schemas: good, bad (throws), later (should not be touched)
        JsonSchema good = mock(JsonSchema.class);
        JsonSchema bad = mock(JsonSchema.class);
        JsonSchema later = mock(JsonSchema.class);

        doThrow(new RuntimeException("bad schema")).when(bad).initializeValidators();

        Collection<JsonSchema> schemas = Arrays.asList(good, bad, later);

        // WHEN the method is invoked, the RuntimeException should propagate
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.callPreloadJsonSchemas(schemas));
        assertEquals("bad schema", ex.getMessage());

        // THEN good.initializeValidators() is called, bad is called, later is never called
        verify(good, times(1)).initializeValidators();
        verify(bad, times(1)).initializeValidators();
        verify(later, never()).initializeValidators();
    }

    @Test
    void shouldProcessDuplicateSchemaReferencesIndependently() {
        // GIVEN the same mock appears twice in the collection
        JsonSchema dup = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(dup, dup);

        // WHEN
        validator.callPreloadJsonSchemas(schemas);

        // THEN initializeValidators is invoked twice on the same instance
        verify(dup, times(2)).initializeValidators();
    }

    @Test
    void shouldThrowNPEWhenEncounteringNullElementInCollection() {
        // GIVEN a collection containing a null element between two valid schemas
        JsonSchema first = mock(JsonSchema.class);
        JsonSchema second = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(first, null, second);

        // WHEN the method is invoked, a NullPointerException is expected
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> validator.callPreloadJsonSchemas(schemas));

        // THEN the first schema's method was called before the exception
        verify(first, times(1)).initializeValidators();
        // The second schema must never be touched
        verify(second, never()).initializeValidators();
    }

    @Test
    void shouldWorkWithSetImplementation() {
        // GIVEN a HashSet (order not guaranteed)
        JsonSchema a = mock(JsonSchema.class);
        JsonSchema b = mock(JsonSchema.class);
        Set<JsonSchema> schemas = new HashSet<>(Arrays.asList(a, b));

        // WHEN
        validator.callPreloadJsonSchemas(schemas);

        // THEN each schema is processed exactly once
        verify(a, times(1)).initializeValidators();
        verify(b, times(1)).initializeValidators();
    }

    @Test
    void shouldPropagateConcurrentModificationException() {
        // GIVEN a mutable list; one schema modifies the list during iteration
        List<JsonSchema> schemas = new ArrayList<>();
        JsonSchema modifying = mock(JsonSchema.class);
        JsonSchema other = mock(JsonSchema.class);
        schemas.add(modifying);
        schemas.add(other);

        // The modifying schema adds a new element when its method is called
        doAnswer(invocation -> {
            schemas.add(mock(JsonSchema.class)); // triggers structural change
            return null;
        }).when(modifying).initializeValidators();

        // WHEN/THEN a ConcurrentModificationException should bubble up
        assertThrows(ConcurrentModificationException.class,
                () -> validator.callPreloadJsonSchemas(schemas));

        // The modifying schema's method must have been invoked once
        verify(modifying, times(1)).initializeValidators();
        // The second original schema may or may not have been reached depending on timing;
        // we only assert that no unexpected interactions occurred.
        verifyNoMoreInteractions(other);
    }

    @Test
    void shouldPropagateExceptionFromIteratorCreation() {
        // GIVEN a collection whose iterator() throws IllegalStateException
        Collection<JsonSchema> badIterCollection = new Collection<>() {
            @Override public int size() { return 0; }
            @Override public boolean isEmpty() { return true; }
            @Override public boolean contains(Object o) { return false; }
            @Override public Iterator<JsonSchema> iterator() {
                throw new IllegalStateException("iterator failure");
            }
            @Override public Object[] toArray() { return new Object[0]; }
            @Override public <T> T[] toArray(T[] a) { return a; }
            @Override public boolean add(JsonSchema jsonSchema) { return false; }
            @Override public boolean remove(Object o) { return false; }
            @Override public boolean containsAll(Collection<?> c) { return false; }
            @Override public boolean addAll(Collection<? extends JsonSchema> c) { return false; }
            @Override public boolean removeAll(Collection<?> c) { return false; }
            @Override public boolean retainAll(Collection<?> c) { return false; }
            @Override public void clear() {}
        };

        // WHEN/THEN the IllegalStateException propagates
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.callPreloadJsonSchemas(badIterCollection));
        assertEquals("iterator failure", ex.getMessage());
    }

    @Test
    void shouldThrowNPEWhenIteratorIsNull() {
        // GIVEN a collection whose iterator() returns null
        Collection<JsonSchema> nullIterCollection = new Collection<>() {
            @Override public int size() { return 0; }
            @Override public boolean isEmpty() { return true; }
            @Override public boolean contains(Object o) { return false; }
            @Override public Iterator<JsonSchema> iterator() { return null; }
            @Override public Object[] toArray() { return new Object[0]; }
            @Override public <T> T[] toArray(T[] a) { return a; }
            @Override public boolean add(JsonSchema jsonSchema) { return false; }
            @Override public boolean remove(Object o) { return false; }
            @Override public boolean containsAll(Collection<?> c) { return false; }
            @Override public boolean addAll(Collection<? extends JsonSchema> c) { return false; }
            @Override public boolean removeAll(Collection<?> c) { return false; }
            @Override public boolean retainAll(Collection<?> c) { return false; }
            @Override public void clear() {}
        };

        // WHEN/THEN a NullPointerException is thrown during iteration
        assertThrows(NullPointerException.class,
                () -> validator.callPreloadJsonSchemas(nullIterCollection));
    }
}
```