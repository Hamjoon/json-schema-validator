```java
package com.networknt.schema;

import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;

/**
 * Unit tests for {@link BaseJsonValidator#preloadJsonSchemas(Collection)}.
 */
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
         * Minimal implementation of the abstract {@code validate} method.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    /**
     * Helper that acts as a lightweight Mockito‑like mock for {@link JsonSchema}.
     */
    private static class MockJsonSchema {

        private int initCalls = 0;

        private RuntimeException toThrow = null;

        private Runnable answer = null;

        private final JsonSchema proxy;

        MockJsonSchema() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("initializeValidators".equals(method.getName())) {
                    initCalls++;
                    if (toThrow != null) {
                        throw toThrow;
                    }
                    if (answer != null) {
                        answer.run();
                    }
                    return null;
                }
                // All other methods return null / default values.
                return null;
            };
            this.proxy = (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(),
                    new Class[] { JsonSchema.class }, handler);
        }

        JsonSchema get() {
            return proxy;
        }

        int getInitCalls() {
            return initCalls;
        }

        void setThrow(RuntimeException e) {
            this.toThrow = e;
        }

        void setAnswer(Runnable r) {
            this.answer = r;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        validator = new TestValidator();
    }

    @Test
    void preloadJsonSchemas_callsInitializeValidators_onEachSchema() {
        MockJsonSchema m1 = new MockJsonSchema();
        MockJsonSchema m2 = new MockJsonSchema();
        Collection<JsonSchema> schemas = Arrays.asList(m1.get(), m2.get());
        validator.exposePreloadJsonSchemas(schemas);
        assertEquals(1, m1.getInitCalls(), "schema1 should be initialized once");
        assertEquals(1, m2.getInitCalls(), "schema2 should be initialized once");
    }

    /* -------------------------------------------------------------
     *  Empty collection – expect no interaction and no exception.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_emptyCollection_noInteraction() {
        Collection<JsonSchema> empty = Collections.emptyList();
        assertDoesNotThrow(() -> validator.exposePreloadJsonSchemas(empty));
    }

    /* -------------------------------------------------------------
     *  Null element inside collection – NPE is expected.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_nullElement_throwsNPE() {
        Collection<JsonSchema> schemas = Collections.singletonList(null);
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
        MockJsonSchema faulty = new MockJsonSchema();
        faulty.setThrow(new RuntimeException("init error"));
        Collection<JsonSchema> schemas = Collections.singletonList(faulty.get());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.exposePreloadJsonSchemas(schemas));
        assertEquals("init error", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Duplicate schema instances – method should call three times.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_duplicateInstances_multipleInvocations() {
        MockJsonSchema schemaX = new MockJsonSchema();
        Collection<JsonSchema> schemas = Arrays.asList(schemaX.get(), schemaX.get(), schemaX.get());
        validator.exposePreloadJsonSchemas(schemas);
        assertEquals(3, schemaX.getInitCalls(), "initializeValidators should be called three times");
    }

    /* -------------------------------------------------------------
     *  Unmodifiable collection – method works without modification.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_unmodifiableCollection_callsInitialize() {
        MockJsonSchema s1 = new MockJsonSchema();
        MockJsonSchema s2 = new MockJsonSchema();
        List<JsonSchema> mutable = Arrays.asList(s1.get(), s2.get());
        Collection<JsonSchema> unmodifiable = Collections.unmodifiableList(mutable);
        validator.exposePreloadJsonSchemas(unmodifiable);
        assertEquals(1, s1.getInitCalls());
        assertEquals(1, s2.getInitCalls());
    }

    /* -------------------------------------------------------------
     *  Iterator throws exception during traversal.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_iteratorThrowsException_propagates() {
        MockJsonSchema good = new MockJsonSchema();
        // Custom collection whose iterator throws on the second call to next()
        Collection<JsonSchema> faultyIterable = new AbstractCollection<JsonSchema>() {

            @Override
            public Iterator<JsonSchema> iterator() {
                return new Iterator<JsonSchema>() {

                    private int count = 0;

                    @Override
                    public boolean hasNext() {
                        // infinite for test purposes
                        return true;
                    }

                    @Override
                    public JsonSchema next() {
                        if (count == 0) {
                            count++;
                            return good.get();
                        } else {
                            throw new IllegalStateException("iterator failure");
                        }
                    }
                };
            }

            @Override
            public int size() {
                // logical size
                return 2;
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.exposePreloadJsonSchemas(faultyIterable));
        assertEquals("iterator failure", ex.getMessage());
        assertEquals(1, good.getInitCalls(),
                "first schema should have been initialized before iterator failure");
    }

    /* -------------------------------------------------------------
     *  Mixed valid and null schemas – NPE after processing first element.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_mixedNullAndValid_throwsNPEAfterFirst() {
        MockJsonSchema first = new MockJsonSchema();
        Collection<JsonSchema> schemas = Arrays.asList(first.get(), null);
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> validator.exposePreloadJsonSchemas(schemas));
        assertEquals(1, first.getInitCalls(),
                "first schema should have been processed before NPE");
    }

    /* -------------------------------------------------------------
     *  Side‑effect verification using an AtomicInteger counter.
     * ------------------------------------------------------------- */
    @Test
    void preloadJsonSchemas_sideEffect_counterIncremented() {
        AtomicInteger counter = new AtomicInteger(0);
        MockJsonSchema s1 = new MockJsonSchema();
        MockJsonSchema s2 = new MockJsonSchema();
        s1.setAnswer(counter::incrementAndGet);
        s2.setAnswer(counter::incrementAndGet);
        Collection<JsonSchema> schemas = Arrays.asList(s1.get(), s2.get());
        validator.exposePreloadJsonSchemas(schemas);
        assertEquals(2, counter.get(), "Counter should be incremented once per schema");
        assertEquals(1, s1.getInitCalls());
        assertEquals(1, s2.getInitCalls());
    }

    /* -------------------------------------------------------------
     *  Raw collection with an element of wrong type – ClassCastException.
     * ------------------------------------------------------------- */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void preloadJsonSchemas_rawCollectionWrongElement_throwsClassCastException() {
        List raw = new ArrayList();
        raw.add("not a schema");
        // unchecked cast
        Collection<JsonSchema> schemas = (Collection) raw;
        assertThrows(ClassCastException.class, () -> validator.exposePreloadJsonSchemas(schemas));
    }
}
```