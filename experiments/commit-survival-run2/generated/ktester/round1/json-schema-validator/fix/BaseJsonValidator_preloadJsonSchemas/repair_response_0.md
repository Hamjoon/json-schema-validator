```java
package com.networknt.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link BaseJsonValidator#preloadJsonSchemas(Collection)}.
 *
 * <p>This version does not depend on Mockito.  Simple manual “mock” objects are created
 * via {@link MockJsonSchema} which records calls to {@code initializeValidators()}
 * and can be configured to throw exceptions or execute side‑effects.</p>
 */
@ExtendWith(MockitoExtension.class) // retained for compatibility; the extension does nothing without Mockito
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

        /** Minimal implementation of the abstract validate method. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }
    }

    /** Helper that acts as a lightweight Mockito‑like mock for {@link JsonSchema}. */
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
            this.proxy = (JsonSchema) Proxy.newProxyInstance(
                    JsonSchema.class.getClassLoader(),
                    new Class[]{JsonSchema.class},
                    handler);
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
                        return true; // infinite for test purposes
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
                return 2; // logical size
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