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

    /** Mocks that are reused for creating the concrete validator instance */
    @Mock SchemaLocation mockSchemaLocation;
    @Mock JsonNodePath mockEvaluationPath;
    @Mock JsonNode mockSchemaNode;
    @Mock ValidationContext mockValidationContext;
    @Mock ValidatorTypeCode mockValidatorType;

    /** Concrete subclass exposing the protected method */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for this test
            return Collections.emptySet();
        }
    }

    private TestValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of BaseJsonValidator only stores the arguments;
        // the mocks are sufficient – no behaviour is required.
        validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,                 // parentSchema – not needed for this test
                mockValidatorType,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release for this simple test
    }

    /* --------------------------------------------------------------------- */
    /* Normal behaviour – already provided in the skeleton */
    @Test
    void preloadJsonSchemas_callsInitializeValidatorsOnEachSchema() {
        // GIVEN – a collection of mocked JsonSchema objects
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);

        // WHEN – the protected method is invoked
        validator.preloadJsonSchemas(schemas);

        // THEN – each schema must have its initialise method called exactly once
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchemas - empty collection")
    void preloadJsonSchemas_emptyCollection_noInteraction() {
        // GIVEN – an empty collection
        Collection<JsonSchema> empty = Collections.emptyList();

        // WHEN – method is invoked
        validator.preloadJsonSchemas(empty);

        // THEN – nothing should happen (no exception, no interactions)
        // No verification needed because there are no mocks in the collection.
    }

    @Test
    @DisplayName("preloadJsonSchemas - null collection argument")
    void preloadJsonSchemas_nullCollection_throwsNPE() {
        // GIVEN – a null reference
        Collection<JsonSchema> nullCollection = null;

        // WHEN / THEN – NPE is expected before entering the loop
        assertThrows(NullPointerException.class,
                () -> validator.preloadJsonSchemas(nullCollection));
    }

    @Test
    @DisplayName("preloadJsonSchemas - collection containing null element")
    void preloadJsonSchemas_collectionWithNullElement_throwsNPE() {
        // GIVEN – a list that contains a null entry
        JsonSchema goodSchema = mock(JsonSchema.class);
        Collection<JsonSchema> list = Arrays.asList(goodSchema, null, goodSchema);

        // WHEN / THEN – NPE should be thrown when the null element is accessed
        assertThrows(NullPointerException.class,
                () -> validator.preloadJsonSchemas(list));

        // Verify that the method was called on the first (good) schema before the NPE
        verify(goodSchema, times(1)).initializeValidators();
    }

    @Test
    @DisplayName("preloadJsonSchemas - duplicate schema instances")
    void preloadJsonSchemas_duplicateInstances_callsEachTime() {
        // GIVEN – the same mock appears twice
        JsonSchema dupSchema = mock(JsonSchema.class);
        Collection<JsonSchema> list = Arrays.asList(dupSchema, dupSchema);

        // WHEN
        validator.preloadJsonSchemas(list);

        // THEN – initializeValidators should be invoked twice (once per occurrence)
        verify(dupSchema, times(2)).initializeValidators();
    }

    @Test
    @DisplayName("preloadJsonSchemas - schema throws during initialization")
    void preloadJsonSchemas_schemaThrows_exceptionPropagates() {
        // GIVEN – first schema throws, second is healthy
        JsonSchema faulty = mock(JsonSchema.class);
        JsonSchema healthy = mock(JsonSchema.class);
        doThrow(new IllegalStateException("faulty init")).when(faulty).initializeValidators();

        Collection<JsonSchema> list = Arrays.asList(faulty, healthy);

        // WHEN / THEN – the exception should bubble out; healthy schema may not be reached
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.preloadJsonSchemas(list));

        assertEquals("faulty init", ex.getMessage());

        // Verify that the healthy schema was *not* invoked because iteration stopped at the exception
        verify(healthy, never()).initializeValidators();
    }

    @Test
    @DisplayName("preloadJsonSchemas - concurrent modification during iteration")
    void preloadJsonSchemas_concurrentModification_throwsCME() {
        // GIVEN – a mutable list and a schema that removes an element during its call
        List<JsonSchema> mutable = new ArrayList<>();
        JsonSchema mutatingSchema = mock(JsonSchema.class);
        JsonSchema otherSchema = mock(JsonSchema.class);
        mutable.add(mutatingSchema);
        mutable.add(otherSchema);

        // The mutating schema will remove the second element when called
        doAnswer(invocation -> {
            mutable.remove(otherSchema);
            return null;
        }).when(mutatingSchema).initializeValidators();

        // WHEN / THEN – a ConcurrentModificationException should be thrown
        assertThrows(ConcurrentModificationException.class,
                () -> validator.preloadJsonSchemas(mutable));
    }

    @Test
    @DisplayName("preloadJsonSchemas - iterator throws unexpected RuntimeException")
    void preloadJsonSchemas_iteratorThrows_exceptionPropagates() {
        // GIVEN – a custom collection whose iterator throws IllegalStateException on next()
        Collection<JsonSchema> badCollection = new Collection<>() {
            @Override public int size() { return 1; }
            @Override public boolean isEmpty() { return false; }
            @Override public boolean contains(Object o) { return false; }
            @Override public Iterator<JsonSchema> iterator() {
                return new Iterator<>() {
                    boolean first = true;
                    @Override public boolean hasNext() { return true; }
                    @Override public JsonSchema next() {
                        if (first) {
                            first = false;
                            throw new IllegalStateException("iterator failure");
                        }
                        return null;
                    }
                };
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

        // WHEN / THEN – the IllegalStateException should propagate unchanged
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.preloadJsonSchemas(badCollection));

        assertEquals("iterator failure", ex.getMessage());
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global initialisation required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global clean‑up required
    }
}
```