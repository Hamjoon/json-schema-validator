```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Comprehensive tests for the protected {@code preloadJsonSchemas} method of {@link BaseJsonValidator}.
 *
 * <p>The method simply iterates over the supplied {@link JsonSchema} collection and calls
 * {@code initializeValidators()} on each element. The tests cover normal behaviour, edge cases,
 * collection‑type variations, side‑effects, and exception propagation.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

    // --------------------------------------------------------------------- //
    // Test fixture
    // --------------------------------------------------------------------- //

    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;

    /** Concrete subclass exposing the protected method for testing. */
    static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
        }

        /** Helper that forwards to the protected method under test. */
        void callPreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }

        // Minimal implementations required by the JsonValidator interface.
        @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Set.of();
        }
        @Override public String getKeyword() { return null; }
        @Override public JsonNodePath getEvaluationPath() { return null; }
    }

    private TestValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                                      parentSchema, validatorType, validationContext);
    }

    // --------------------------------------------------------------------- //
    // Helper factories for custom collections used in exceptional tests
    // --------------------------------------------------------------------- //

    /** Collection whose iterator() throws IllegalStateException. */
    static class IteratorThrowingCollection implements Collection<JsonSchema> {
        @Override public Iterator<JsonSchema> iterator() {
            throw new IllegalStateException("iterator() failure");
        }
        // The remaining methods are not used in the test – provide minimal stubs.
        @Override public int size() { return 0; }
        @Override public boolean isEmpty() { return true; }
        @Override public boolean contains(Object o) { return false; }
        @Override public Object[] toArray() { return new Object[0]; }
        @Override public <T> T[] toArray(T[] a) { return a; }
        @Override public boolean add(JsonSchema jsonSchema) { return false; }
        @Override public boolean remove(Object o) { return false; }
        @Override public boolean containsAll(Collection<?> c) { return false; }
        @Override public boolean addAll(Collection<? extends JsonSchema> c) { return false; }
        @Override public boolean removeAll(Collection<?> c) { return false; }
        @Override public boolean retainAll(Collection<?> c) { return false; }
        @Override public void clear() {}
    }

    /** Collection whose iterator's hasNext() throws RuntimeException. */
    static class HasNextThrowingCollection implements Collection<JsonSchema> {
        @Override public Iterator<JsonSchema> iterator() {
            return new Iterator<>() {
                @Override public boolean hasNext() {
                    throw new RuntimeException("hasNext() failure");
                }
                @Override public JsonSchema next() {
                    return null;
                }
            };
        }
        // Minimal stubs for other methods.
        @Override public int size() { return 0; }
        @Override public boolean isEmpty() { return true; }
        @Override public boolean contains(Object o) { return false; }
        @Override public Object[] toArray() { return new Object[0]; }
        @Override public <T> T[] toArray(T[] a) { return a; }
        @Override public boolean add(JsonSchema jsonSchema) { return false; }
        @Override public boolean remove(Object o) { return false; }
        @Override public boolean containsAll(Collection<?> c) { return false; }
        @Override public boolean addAll(Collection<? extends JsonSchema> c) { return false; }
        @Override public boolean removeAll(Collection<?> c) { return false; }
        @Override public boolean retainAll(Collection<?> c) { return false; }
        @Override public void clear() {}
    }

    /** Collection whose iterator's next() throws IllegalArgumentException. */
    static class NextThrowingCollection implements Collection<JsonSchema> {
        @Override public Iterator<JsonSchema> iterator() {
            return new Iterator<>() {
                @Override public boolean hasNext() { return true; }
                @Override public JsonSchema next() {
                    throw new IllegalArgumentException("next() failure");
                }
            };
        }
        // Minimal stubs for other methods.
        @Override public int size() { return 0; }
        @Override public boolean isEmpty() { return true; }
        @Override public boolean contains(Object o) { return false; }
        @Override public Object[] toArray() { return new Object[0]; }
        @Override public <T> T[] toArray(T[] a) { return a; }
        @Override public boolean add(JsonSchema jsonSchema) { return false; }
        @Override public boolean remove(Object o) { return false; }
        @Override public boolean containsAll(Collection<?> c) { return false; }
        @Override public boolean addAll(Collection<? extends JsonSchema> c) { return false; }
        @Override public boolean removeAll(Collection<?> c) { return false; }
        @Override public boolean retainAll(Collection<?> c) { return false; }
        @Override public void clear() {}
    }

    // --------------------------------------------------------------------- //
    // Normal operation tests (parameterised)
    // --------------------------------------------------------------------- //

    static Stream<Arguments> normalOperationProvider() {
        return Stream.of(
                Arguments.of("three distinct schemas",
                        List.of(mock(JsonSchema.class), mock(JsonSchema.class), mock(JsonSchema.class))),
                Arguments.of("duplicate schema twice",
                        List.of(mock(JsonSchema.class), mock(JsonSchema.class))) // will replace second with first later
        );
    }

    @ParameterizedTest(name = "preloadJsonSchemas - normal operation: {0}")
    @MethodSource("normalOperationProvider")
    void preloadJsonSchemas_normalOperation(String description, List<JsonSchema> list) {
        // For the duplicate case we need the same mock instance twice.
        if (description.contains("duplicate")) {
            JsonSchema dup = mock(JsonSchema.class);
            list = List.of(dup, dup);
        }

        // WHEN
        validator.callPreloadJsonSchemas(list);

        // THEN each schema's initialise method is called exactly once per occurrence.
        for (JsonSchema schema : list) {
            verify(schema, times(1)).initializeValidators();
        }
    }

    // --------------------------------------------------------------------- //
    // Empty collection tests
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_withEmptyCollectionDoesNotThrow() {
        Collection<JsonSchema> empty = List.of();
        Assertions.assertDoesNotThrow(() -> validator.callPreloadJsonSchemas(empty));
    }

    @Test
    void preloadJsonSchemas_withImmutableEmptyCollectionDoesNotThrow() {
        Collection<JsonSchema> empty = Collections.emptyList();
        Assertions.assertDoesNotThrow(() -> validator.callPreloadJsonSchemas(empty));
    }

    // --------------------------------------------------------------------- //
    // Edge‑case tests
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_nullCollection_throwsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.callPreloadJsonSchemas(null));
    }

    @Test
    void preloadJsonSchemas_collectionContainingNullElement_throwsNullPointerException() {
        List<JsonSchema> listWithNull = List.of((JsonSchema) null);
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.callPreloadJsonSchemas(listWithNull));
    }

    @Test
    void preloadJsonSchemas_initializeValidatorsThrowsRuntimeException_propagates() {
        JsonSchema bad = mock(JsonSchema.class);
        doThrow(new RuntimeException("boom")).when(bad).initializeValidators();

        JsonSchema good = mock(JsonSchema.class);
        List<JsonSchema> schemas = List.of(good, bad, mock(JsonSchema.class));

        RuntimeException ex = Assertions.assertThrows(RuntimeException.class,
                () -> validator.callPreloadJsonSchemas(schemas));

        Assertions.assertEquals("boom", ex.getMessage());

        // verify that the first schema was processed before the exception
        verify(good, times(1)).initializeValidators();
        // the schema after the failing one must not be invoked
        verify(schemas.get(2), never()).initializeValidators();
    }

    @Test
    void preloadJsonSchemas_initializeValidatorsThrowsError_propagates() {
        JsonSchema errorSchema = mock(JsonSchema.class);
        doThrow(new AssertionError("assertion error")).when(errorSchema).initializeValidators();

        Assertions.assertThrows(AssertionError.class,
                () -> validator.callPreloadJsonSchemas(List.of(errorSchema)));
    }

    // --------------------------------------------------------------------- //
    // Collection type variations
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_setIteration() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        Set<JsonSchema> set = new HashSet<>(Arrays.asList(s1, s2, s3));

        validator.callPreloadJsonSchemas(set);

        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
        verify(s3, times(1)).initializeValidators();
    }

    @Test
    void preloadJsonSchemas_unmodifiableList() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        List<JsonSchema> list = Collections.unmodifiableList(Arrays.asList(s1, s2));

        validator.callPreloadJsonSchemas(list);

        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    // --------------------------------------------------------------------- //
    // Side‑effect verification
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_sideEffectCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        JsonSchema schema = mock(JsonSchema.class);
        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(schema).initializeValidators();

        JsonSchema schema2 = mock(JsonSchema.class);
        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(schema2).initializeValidators();

        JsonSchema schema3 = mock(JsonSchema.class);
        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(schema3).initializeValidators();

        validator.callPreloadJsonSchemas(List.of(schema, schema2, schema3));

        Assertions.assertEquals(3, counter.get(),
                "Counter should reflect the number of initialise calls");
    }

    // --------------------------------------------------------------------- //
    // Concurrent modification test
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_concurrentModification_throwsConcurrentModificationException() {
        List<JsonSchema> mutable = new ArrayList<>();
        JsonSchema first = mock(JsonSchema.class);
        JsonSchema second = mock(JsonSchema.class);
        mutable.add(first);
        mutable.add(second);

        // first schema mutates the list during initialise
        doAnswer(invocation -> {
            mutable.add(mock(JsonSchema.class)); // structural change
            return null;
        }).when(first).initializeValidators();

        Assertions.assertThrows(ConcurrentModificationException.class,
                () -> validator.callPreloadJsonSchemas(mutable));
    }

    // --------------------------------------------------------------------- //
    // Iterator exception tests
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_iteratorThrowsException_propagates() {
        Collection<JsonSchema> coll = new IteratorThrowingCollection();
        Assertions.assertThrows(IllegalStateException.class,
                () -> validator.callPreloadJsonSchemas(coll));
    }

    @Test
    void preloadJsonSchemas_hasNextThrowsException_propagates() {
        Collection<JsonSchema> coll = new HasNextThrowingCollection();
        Assertions.assertThrows(RuntimeException.class,
                () -> validator.callPreloadJsonSchemas(coll));
    }

    @Test
    void preloadJsonSchemas_nextThrowsException_propagates() {
        Collection<JsonSchema> coll = new NextThrowingCollection();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> validator.callPreloadJsonSchemas(coll));
    }

    // --------------------------------------------------------------------- //
    // Duplicate instance test (already covered in parameterised normal test)
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_duplicateInstanceInvokedTwice() {
        JsonSchema dup = mock(JsonSchema.class);
        List<JsonSchema> list = List.of(dup, dup);
        validator.callPreloadJsonSchemas(list);
        verify(dup, times(2)).initializeValidators();
    }
}
```