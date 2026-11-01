package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

    private TestValidator validator;

    /**
     * Simple concrete subclass exposing the protected method for testing.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator() {
            // All constructor arguments are irrelevant for these tests.
            super("testSchemaPath", null, null, null, null);
        }

        /**
         * Expose the protected method to the test suite.
         */
        void exposePreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }

        /**
         * Minimal implementation of the abstract validate method.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        validator = new TestValidator();
    }

    @Test
    void preloadJsonSchemas_callsInitializeValidators_onEachSchema() {
        // Arrange – create two mock JsonSchema instances
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);
        // Act – invoke the method under test via the exposing wrapper
        validator.exposePreloadJsonSchemas(schemas);
        // Assert – each mock must have received exactly one call to initializeValidators()
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
        // No other interactions should have occurred
        verifyNoMoreInteractions(schema1, schema2);
    }

    /* -------------------------------------------------------------
     *  Empty collection – expect no interaction and no exception.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_emptyCollection_noInteraction() {
        Collection<JsonSchema> empty = Collections.emptyList();
        // Act & Assert – method should complete silently.
        assertDoesNotThrow(() -> validator.exposePreloadJsonSchemas(empty));
    }

    /* -------------------------------------------------------------
     *  Null element inside collection – NPE is expected.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_nullElement_throwsNPE() {
        Collection<JsonSchema> schemas = Collections.singletonList(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> validator.exposePreloadJsonSchemas(schemas));
    }

    /* -------------------------------------------------------------
     *  Null collection argument – NPE is expected.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_nullCollection_throwsNPE() {
        assertThrows(NullPointerException.class, () -> validator.exposePreloadJsonSchemas(null));
    }

    /* -------------------------------------------------------------
     *  Schema throws unchecked exception – exception propagates.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_schemaThrowsException_propagates() {
        JsonSchema faulty = mock(JsonSchema.class);
        doThrow(new RuntimeException("init error")).when(faulty).initializeValidators();
        Collection<JsonSchema> schemas = Collections.singletonList(faulty);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.exposePreloadJsonSchemas(schemas));
        assertEquals("init error", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Duplicate schema instances – method should call three times.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_duplicateInstances_multipleInvocations() {
        JsonSchema schemaX = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schemaX, schemaX, schemaX);
        validator.exposePreloadJsonSchemas(schemas);
        verify(schemaX, times(3)).initializeValidators();
    }

    /* -------------------------------------------------------------
     *  Unmodifiable collection – method works without modification.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_unmodifiableCollection_callsInitialize() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> mutable = Arrays.asList(s1, s2);
        Collection<JsonSchema> unmodifiable = Collections.unmodifiableList(mutable);
        validator.exposePreloadJsonSchemas(unmodifiable);
        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    /* -------------------------------------------------------------
     *  Iterator throws exception during traversal.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_iteratorThrowsException_propagates() {
        // First element is a normal mock; the second triggers an exception from iterator.next()
        JsonSchema good = mock(JsonSchema.class);
        Iterable<JsonSchema> faultyIterable = () -> new Iterator<JsonSchema>() {

            private int count = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public JsonSchema next() {
                if (count == 0) {
                    count++;
                    return good;
                } else {
                    throw new IllegalStateException("iterator failure");
                }
            }
        };
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.exposePreloadJsonSchemas(faultyIterable));
        assertEquals("iterator failure", ex.getMessage());
        // Verify that the first schema's method was indeed called before the failure.
        verify(good, times(1)).initializeValidators();
    }

    /* -------------------------------------------------------------
     *  Mixed valid and null schemas – NPE after processing first element.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_mixedNullAndValid_throwsNPEAfterFirst() {
        JsonSchema first = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(first, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> validator.exposePreloadJsonSchemas(schemas));
        // The first schema should have been processed.
        verify(first, times(1)).initializeValidators();
    }

    /* -------------------------------------------------------------
     *  Side‑effect verification using an AtomicInteger counter.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_sideEffect_counterIncremented() {
        AtomicInteger counter = new AtomicInteger(0);
        // Two schemas whose initializeValidators() increments the counter.
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(s1).initializeValidators();
        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(s2).initializeValidators();
        Collection<JsonSchema> schemas = Arrays.asList(s1, s2);
        validator.exposePreloadJsonSchemas(schemas);
        assertEquals(2, counter.get(), "Counter should be incremented once per schema");
        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    /* -------------------------------------------------------------
     *  Raw collection with an element of wrong type – ClassCastException.
     * ------------------------------------------------------------- */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void preloadJsonSchemas_rawCollectionWrongElement_throwsClassCastException() {
        // Build a raw List containing a String; then cast to Collection<JsonSchema>.
        List raw = new ArrayList();
        raw.add("not a schema");
        // unchecked cast
        Collection<JsonSchema> schemas = (Collection) raw;
        assertThrows(ClassCastException.class, () -> validator.exposePreloadJsonSchemas(schemas));
    }
}
