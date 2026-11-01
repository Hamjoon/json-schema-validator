package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

//     @Mock
    ValidationContext // used only to satisfy the constructor
    validationContext;

//     @Mock
    SchemaLocation // used only to satisfy the constructor
    schemaLocation;

//     @Mock
    JsonNodePath // used only to satisfy the constructor
    evaluationPath;

//     @Mock
//     JsonNode // used only to satisfy the constructor
    schemaNode;

//     @Mock
    JsonSchema // used only to satisfy the constructor
    parentSchema;

//     @Mock
    ValidatorTypeCode // used only to satisfy the constructor
    validatorType;

    private TestValidator validator;

    /**
     * Simple test double for {@link JsonSchema}. Only {@code initializeValidators()}
     * is required for the tests; all other behaviour is inherited from {@link JsonSchema}.
     */
    static class TestJsonSchema extends JsonSchema {

        private final AtomicInteger initCount = new AtomicInteger(0);

        private Throwable toThrow = null;

        private Runnable sideEffect = null;

        TestJsonSchema() {
            // Call a super‑constructor that exists in the library.
            // The exact parameters are not used in the tests, so we pass null / false.
//             super(null, null, null, null, null, false);
        }

        /**
         * Record a side‑effect to be executed when {@code initializeValidators()} is called.
         */
        void setSideEffect(Runnable r) {
            this.sideEffect = r;
        }

        /**
         * Configure an exception (or error) to be thrown from {@code initializeValidators()}.
         */
        void setException(Throwable e) {
            this.toThrow = e;
        }

        /**
         * Number of times {@code initializeValidators()} has been invoked.
         */
        int getInitCount() {
            return initCount.get();
        }

        @Override
        public void initializeValidators() {
            initCount.incrementAndGet();
            if (sideEffect != null) {
                sideEffect.run();
            }
            if (toThrow != null) {
                if (toThrow instanceof RuntimeException) {
                    throw (RuntimeException) toThrow;
                } else if (toThrow instanceof Error) {
                    throw (Error) toThrow;
                } else {
                    throw new RuntimeException(toThrow);
                }
            }
        }
    }

    /**
     * Concrete subclass exposing the protected method for testing.
     */
    static class TestValidator extends BaseJsonValidator {

//         TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * Forward to the protected method under test.
         */
        void callPreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }

//         @Override
//         public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
//             return Set.of();
//         }

        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }
    }

    /**
     * Collection whose {@code iterator()} throws {@code IllegalStateException}.
     */
    static class IteratorThrowingCollection implements Collection<JsonSchema> {

        @Override
        public Iterator<JsonSchema> iterator() {
            throw new IllegalStateException("iterator() failure");
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(JsonSchema e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends JsonSchema> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    /**
     * Collection whose iterator's {@code hasNext()} throws {@code RuntimeException}.
     */
    static class HasNextThrowingCollection implements Collection<JsonSchema> {

        @Override
        public Iterator<JsonSchema> iterator() {
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    throw new RuntimeException("hasNext() failure");
                }

                @Override
                public JsonSchema next() {
                    return null;
                }
            };
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(JsonSchema e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends JsonSchema> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    /**
     * Collection whose iterator's {@code next()} throws {@code IllegalArgumentException}.
     */
    static class NextThrowingCollection implements Collection<JsonSchema> {

        @Override
        public Iterator<JsonSchema> iterator() {
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public JsonSchema next() {
                    throw new IllegalArgumentException("next() failure");
                }
            };
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(JsonSchema e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends JsonSchema> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        validator = new TestValidator(null, null, null, null, null, null);
    }

    static Stream<Arguments> normalOperationProvider() {
        return Stream.of(Arguments.of("three distinct schemas", List.<JsonSchema>of(new TestJsonSchema(), new TestJsonSchema(), new TestJsonSchema())), Arguments.of("duplicate schema twice", List.of()));
    }

    /**
     * Helper to create a regular test double.
     */
    private TestJsonSchema newSchema() {
        return new TestJsonSchema();
    }

    @Test
    void preloadJsonSchemas_callsInitializeValidatorsOnEachSchema() {
        TestJsonSchema schema1 = newSchema();
        TestJsonSchema schema2 = newSchema();
        Collection<JsonSchema> schemas = List.of(schema1, schema2);
        validator.callPreloadJsonSchemas(schemas);
        Assertions.assertEquals(1, schema1.getInitCount());
        Assertions.assertEquals(1, schema2.getInitCount());
    }

    @Test
    void preloadJsonSchemas_withEmptyCollectionDoesNotThrow() {
        Collection<JsonSchema> empty = List.of();
        Assertions.assertDoesNotThrow(() -> validator.callPreloadJsonSchemas(empty));
    }

    @ParameterizedTest(name = "preloadJsonSchemas - normal operation: {0}")
    @MethodSource("normalOperationProvider")
    void preloadJsonSchemas_normalOperation(String description, List<JsonSchema> list) {
        if (description.contains("duplicate")) {
            TestJsonSchema dup = newSchema();
            list = List.of(dup, dup);
        }
        validator.callPreloadJsonSchemas(list);
        for (JsonSchema schema : list) {
            Assertions.assertEquals(1, ((TestJsonSchema) schema).getInitCount(), "Each occurrence should have been initialized once");
        }
    }

    @Test
    void preloadJsonSchemas_withImmutableEmptyCollectionDoesNotThrow() {
        Collection<JsonSchema> empty = Collections.emptyList();
        Assertions.assertDoesNotThrow(() -> validator.callPreloadJsonSchemas(empty));
    }

    @Test
    void preloadJsonSchemas_nullCollection_throwsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> validator.callPreloadJsonSchemas(null));
    }

    @Test
    void preloadJsonSchemas_collectionContainingNullElement_throwsNullPointerException() {
        List<JsonSchema> listWithNull = List.of((JsonSchema) null);
        Assertions.assertThrows(NullPointerException.class, () -> validator.callPreloadJsonSchemas(listWithNull));
    }

    @Test
    void preloadJsonSchemas_initializeValidatorsThrowsRuntimeException_propagates() {
        TestJsonSchema good = newSchema();
        TestJsonSchema bad = newSchema();
        bad.setException(new RuntimeException("boom"));
        TestJsonSchema after = newSchema();
        List<JsonSchema> schemas = List.of(good, bad, after);
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> validator.callPreloadJsonSchemas(schemas));
        Assertions.assertEquals("boom", ex.getMessage());
        Assertions.assertEquals(1, good.getInitCount());
        Assertions.assertEquals(1, bad.getInitCount());
        Assertions.assertEquals(0, after.getInitCount());
    }

    @Test
    void preloadJsonSchemas_initializeValidatorsThrowsError_propagates() {
        TestJsonSchema errorSchema = newSchema();
        errorSchema.setException(new AssertionError("assertion error"));
        Assertions.assertThrows(AssertionError.class, () -> validator.callPreloadJsonSchemas(List.of(errorSchema)));
    }

    @Test
    void preloadJsonSchemas_setIteration() {
        TestJsonSchema s1 = newSchema();
        TestJsonSchema s2 = newSchema();
        TestJsonSchema s3 = newSchema();
        Set<JsonSchema> set = new HashSet<>(Arrays.asList(s1, s2, s3));
        validator.callPreloadJsonSchemas(set);
        Assertions.assertEquals(1, s1.getInitCount());
        Assertions.assertEquals(1, s2.getInitCount());
        Assertions.assertEquals(1, s3.getInitCount());
    }

    @Test
    void preloadJsonSchemas_unmodifiableList() {
        TestJsonSchema s1 = newSchema();
        TestJsonSchema s2 = newSchema();
        List<JsonSchema> list = Collections.unmodifiableList(Arrays.asList(s1, s2));
        validator.callPreloadJsonSchemas(list);
        Assertions.assertEquals(1, s1.getInitCount());
        Assertions.assertEquals(1, s2.getInitCount());
    }

    @Test
    void preloadJsonSchemas_sideEffectCounter() {
        AtomicInteger counter = new AtomicInteger(0);
        TestJsonSchema schema = newSchema();
        schema.setSideEffect(counter::incrementAndGet);
        TestJsonSchema schema2 = newSchema();
        schema2.setSideEffect(counter::incrementAndGet);
        TestJsonSchema schema3 = newSchema();
        schema3.setSideEffect(counter::incrementAndGet);
        validator.callPreloadJsonSchemas(List.of(schema, schema2, schema3));
        Assertions.assertEquals(3, counter.get(), "Counter should reflect the number of initialise calls");
    }

    @Test
    void preloadJsonSchemas_concurrentModification_throwsConcurrentModificationException() {
        List<JsonSchema> mutable = new ArrayList<>();
        TestJsonSchema first = newSchema();
        TestJsonSchema second = newSchema();
        mutable.add(first);
        mutable.add(second);
        // first schema mutates the list during initialise
        first.setSideEffect(() -> mutable.add(newSchema()));
        Assertions.assertThrows(ConcurrentModificationException.class, () -> validator.callPreloadJsonSchemas(mutable));
    }

    @Test
    void preloadJsonSchemas_iteratorThrowsException_propagates() {
        Collection<JsonSchema> coll = new IteratorThrowingCollection();
        Assertions.assertThrows(IllegalStateException.class, () -> validator.callPreloadJsonSchemas(coll));
    }

    @Test
    void preloadJsonSchemas_hasNextThrowsException_propagates() {
        Collection<JsonSchema> coll = new HasNextThrowingCollection();
        Assertions.assertThrows(RuntimeException.class, () -> validator.callPreloadJsonSchemas(coll));
    }

    @Test
    void preloadJsonSchemas_nextThrowsException_propagates() {
        Collection<JsonSchema> coll = new NextThrowingCollection();
        Assertions.assertThrows(IllegalArgumentException.class, () -> validator.callPreloadJsonSchemas(coll));
    }

    @Test
    void preloadJsonSchemas_duplicateInstanceInvokedTwice() {
        TestJsonSchema dup = newSchema();
        List<JsonSchema> list = List.of(dup, dup);
        validator.callPreloadJsonSchemas(list);
        Assertions.assertEquals(2, dup.getInitCount());
    }
}