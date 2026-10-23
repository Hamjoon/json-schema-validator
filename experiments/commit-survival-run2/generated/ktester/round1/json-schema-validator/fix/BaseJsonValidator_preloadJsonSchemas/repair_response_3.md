```java
package com.networknt.schema;

import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import com.networknt.schema.ValidationMessage;

/**
 * Test suite for {@link BaseJsonValidator#preloadJsonSchemas(Collection)}.
 *
 * The original test used a dynamic proxy to mock {@link JsonSchema}, but in the
 * current version of the library {@code JsonSchema} is a concrete (abstract)
 * class, not an interface.  Creating a proxy for a class is illegal and caused
 * the test failures.  The fix replaces the proxy‑based mock with a lightweight
 * concrete subclass that implements only the methods required for the tests.
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

        /** Expose the protected method to the test suite. */
        void exposePreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }

        /** Minimal implementation of the abstract {@code validate} method. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            return Collections.emptySet();
        }
    }

    /**
     * Lightweight mock of {@link JsonSchema}.  It extends the real class and
     * overrides only the {@code initializeValidators} method, which is the
     * method exercised by {@link BaseJsonValidator#preloadJsonSchemas}.
     *
     * The mock records how many times {@code initializeValidators} has been
     * called, can be configured to throw an exception, and can execute an
     * arbitrary side‑effect (via {@code setAnswer}).
     */
    private static class MockJsonSchema extends JsonSchema {

        private int initCalls = 0;
        private RuntimeException toThrow = null;
        private Runnable answer = null;

        /** Construct a mock with a no‑arg super‑constructor. */
        MockJsonSchema() {
            // The real {@code JsonSchema} constructor requires a factory, schema node
            // and a schema path.  For the purpose of these tests they can all be {@code null}.
            super(null, null, null);
        }

        /** Return the mock itself – the test code works with the {@code JsonSchema}
         *  type, so no extra wrapper is needed. */
        JsonSchema get() {
            return this;
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

        /** The method invoked by {@code BaseJsonValidator#preloadJsonSchemas}. */
        @Override
        public void initializeValidators(BaseJsonValidator validator) {
            initCalls++;
            if (toThrow != null) {
                throw toThrow;
            }
            if (answer != null) {
                answer.run();
            }
        }

        /* -----------------------------------------------------------------
         *  The remaining abstract methods of {@code JsonSchema} are supplied
         *  with trivial implementations because the tests never call them.
         * ----------------------------------------------------------------- */

        @Override
        public Set<ValidationMessage> validate(JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public String getSchemaLocation() {
            return null;
        }

        @Override
        public String getSchemaLocation(String base) {
            return null;
        }

        @Override
        public JsonSchemaFactory getFactory() {
            return null;
        }

        @Override
        public void setFactory(JsonSchemaFactory factory) {
            // no‑op for the mock
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode) {
            // no‑op for the mock
        }

        @Override
        public void setSchemaPath(String schemaPath) {
            // no‑op for the mock
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

    @Test
    void preloadJsonSchemas_emptyCollection_noInteraction() {
        Collection<JsonSchema> empty = Collections.emptyList();
        assertDoesNotThrow(() -> validator.exposePreloadJsonSchemas(empty));
    }

    @Test
    void preloadJsonSchemas_nullElement_throwsNPE() {
        Collection<JsonSchema> schemas = Collections.singletonList(null);
        assertThrows(NullPointerException.class,
                () -> validator.exposePreloadJsonSchemas(schemas));
    }

    @Test
    void preloadJsonSchemas_nullCollection_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> validator.exposePreloadJsonSchemas(null));
    }

    @Test
    void preloadJsonSchemas_schemaThrowsException_propagates() {
        MockJsonSchema faulty = new MockJsonSchema();
        faulty.setThrow(new RuntimeException("init error"));
        Collection<JsonSchema> schemas = Collections.singletonList(faulty.get());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.exposePreloadJsonSchemas(schemas));
        assertEquals("init error", ex.getMessage());
    }

    @Test
    void preloadJsonSchemas_duplicateInstances_multipleInvocations() {
        MockJsonSchema schemaX = new MockJsonSchema();
        Collection<JsonSchema> schemas = Arrays.asList(schemaX.get(),
                schemaX.get(),
                schemaX.get());
        validator.exposePreloadJsonSchemas(schemas);
        assertEquals(3, schemaX.getInitCalls(),
                "initializeValidators should be called three times");
    }

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

    @Test
    void preloadJsonSchemas_mixedNullAndValid_throwsNPEAfterFirst() {
        MockJsonSchema first = new MockJsonSchema();
        Collection<JsonSchema> schemas = Arrays.asList(first.get(), null);
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> validator.exposePreloadJsonSchemas(schemas));
        assertEquals(1, first.getInitCalls(),
                "first schema should have been processed before NPE");
    }

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void preloadJsonSchemas_rawCollectionWrongElement_throwsClassCastException() {
        List raw = new ArrayList();
        raw.add("not a schema");
        // unchecked cast
        Collection<JsonSchema> schemas = (Collection) raw;
        assertThrows(ClassCastException.class,
                () -> validator.exposePreloadJsonSchemas(schemas));
    }
}
```