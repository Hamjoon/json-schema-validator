```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PrefixItemsValidator#preloadJsonSchema()}.
 *
 * <p>The original version of this test used Mockito to create spies and mocks.
 * The execution environment does not provide Mockito, so the test has been
 * rewritten to use simple reflection‑based stubs and Java dynamic proxies.</p>
 */
class PrefixItemsValidator_preloadJsonSchema_Test {

    /** The validator instance under test – a concrete subclass that captures the argument
     * passed to {@code preloadJsonSchemas(List)}. */
    TestablePrefixItemsValidator validator;

    /** Helper to create a dummy {@link JsonSchema} implementation using a dynamic proxy. */
    private static JsonSchema proxyJsonSchema() {
        return (JsonSchema) Proxy.newProxyInstance(
                JsonSchema.class.getClassLoader(),
                new Class[]{JsonSchema.class},
                (proxy, method, args) -> null);
    }

    /** Simple subclass that overrides the protected {@code preloadJsonSchemas} method
     * so that the argument can be inspected or an exception can be forced. */
    static class TestablePrefixItemsValidator extends PrefixItemsValidator {
        List<JsonSchema> captured;
        RuntimeException toThrow;
        boolean invokeSuper = false; // for the modification‑exception test

        TestablePrefixItemsValidator(String path, JsonNode schemaArray) {
            // parentSchema and validationContext are not needed for the tests that use an empty array.
            super(path, schemaArray, null, null);
        }

        @Override
        protected void preloadJsonSchemas(List<JsonSchema> schemas) {
            if (toThrow != null) {
                throw toThrow;
            }
            if (invokeSuper) {
                // delegate to the real implementation (which does nothing in the library)
                super.preloadJsonSchemas(schemas);
                return;
            }
            this.captured = schemas;
        }

        List<JsonSchema> getCaptured() {
            return captured;
        }

        void setToThrow(RuntimeException e) {
            this.toThrow = e;
        }

        void setInvokeSuper(boolean b) {
            this.invokeSuper = b;
        }
    }

    /** Utility to set the private {@code tupleSchema} field via reflection. */
    private static void setTupleSchema(PrefixItemsValidator target, List<JsonSchema> value) throws Exception {
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Utility to read the private {@code tupleSchema} field via reflection. */
    private static List<JsonSchema> getTupleSchema(PrefixItemsValidator target) throws Exception {
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> v = (List<JsonSchema>) f.get(target);
        return v;
    }

    @BeforeEach
    void setup() {
        // Use an empty schema array – the constructor will not attempt to call the
        // (null) ValidationContext, avoiding NPEs.
        ArrayNode emptyArray = JsonNodeFactory.instance.arrayNode();
        validator = new TestablePrefixItemsValidator("testPath", emptyArray);
    }

    @Test
    void preloadJsonSchema_delegatesToPreloadJsonSchemas() throws Exception {
        List<JsonSchema> list = Arrays.asList(proxyJsonSchema(), proxyJsonSchema());
        setTupleSchema(validator, list);
        validator.preloadJsonSchema();
        assertSame(list, validator.getCaptured(),
                "preloadJsonSchema should forward the internal tupleSchema list");
    }

    @Test
    void preloadJsonSchema_withNullTupleSchema() throws Exception {
        setTupleSchema(validator, null);
        validator.preloadJsonSchema();
        assertNull(validator.getCaptured(), "When tupleSchema is null the captured argument must be null");
    }

    @Test
    void preloadJsonSchema_withEmptyTupleSchema() throws Exception {
        setTupleSchema(validator, Collections.emptyList());
        validator.preloadJsonSchema();
        List<JsonSchema> captured = validator.getCaptured();
        assertNotNull(captured, "Captured list must not be null");
        assertTrue(captured.isEmpty(), "Captured list should be empty");
    }

    @Test
    void preloadJsonSchema_withMultipleSchemas() throws Exception {
        List<JsonSchema> three = Arrays.asList(proxyJsonSchema(), proxyJsonSchema(), proxyJsonSchema());
        setTupleSchema(validator, three);
        validator.preloadJsonSchema();
        assertEquals(three, validator.getCaptured(),
                "The exact list of three schemas must be forwarded");
    }

    @Test
    void preloadJsonSchema_withNullElementInTupleSchema() throws Exception {
        List<JsonSchema> listWithNull = new ArrayList<>();
        listWithNull.add(proxyJsonSchema());
        listWithNull.add(null);
        listWithNull.add(proxyJsonSchema());
        setTupleSchema(validator, listWithNull);
        validator.preloadJsonSchema();
        assertEquals(listWithNull, validator.getCaptured(),
                "List containing a null entry must be forwarded unchanged");
    }

    @Test
    void preloadJsonSchema_withSingleSchema() throws Exception {
        JsonSchema single = proxyJsonSchema();
        setTupleSchema(validator, Collections.singletonList(single));
        validator.preloadJsonSchema();
        List<JsonSchema> captured = validator.getCaptured();
        assertEquals(1, captured.size(), "List size should be one");
        assertSame(single, captured.get(0), "The single schema reference must be preserved");
    }

    @Test
    void preloadJsonSchema_withDuplicateSchemas() throws Exception {
        JsonSchema dup = proxyJsonSchema();
        List<JsonSchema> dupList = Arrays.asList(dup, dup, dup);
        setTupleSchema(validator, dupList);
        validator.preloadJsonSchema();
        assertEquals(dupList, validator.getCaptured(),
                "Duplicate schemas must be forwarded in original order");
    }

    @Test
    void preloadJsonSchema_propagatesExceptionFromPreloadJsonSchemas() {
        RuntimeException boom = new RuntimeException("boom");
        validator.setToThrow(boom);
        RuntimeException thrown = assertThrows(RuntimeException.class,
                validator::preloadJsonSchema,
                "Exception from preloadJsonSchemas must be propagated");
        assertSame(boom, thrown, "Propagated exception should be the original one");
    }

    @Test
    void preloadJsonSchema_withUnmodifiableTupleSchema() throws Exception {
        List<JsonSchema> mutable = Arrays.asList(proxyJsonSchema(), proxyJsonSchema());
        List<JsonSchema> unmodifiable = Collections.unmodifiableList(mutable);
        setTupleSchema(validator, unmodifiable);
        validator.preloadJsonSchema();
        assertSame(unmodifiable, validator.getCaptured(),
                "The unmodifiable list instance must be forwarded as‑is");
    }

    @Test
    void preloadJsonSchema_preservesOrderOfSchemas() throws Exception {
        JsonSchema a = proxyJsonSchema();
        JsonSchema b = proxyJsonSchema();
        JsonSchema c = proxyJsonSchema();
        List<JsonSchema> ordered = Arrays.asList(a, b, c);
        setTupleSchema(validator, ordered);
        validator.preloadJsonSchema();
        assertEquals(ordered, validator.getCaptured(),
                "Schema order must be unchanged");
    }

    @Test
    void preloadJsonSchema_listThrowsOnModification_propagatesException() throws Exception {
        // List that throws UnsupportedOperationException on any mutating operation.
        List<JsonSchema> throwingList = new AbstractList<JsonSchema>() {
            private final List<JsonSchema> delegate = new ArrayList<>();

            @Override
            public JsonSchema get(int index) {
                return delegate.get(index);
            }

            @Override
            public int size() {
                return delegate.size();
            }

            @Override
            public JsonSchema set(int index, JsonSchema element) {
                throw new UnsupportedOperationException("set not supported");
            }

            @Override
            public void add(int index, JsonSchema element) {
                throw new UnsupportedOperationException("add not supported");
            }

            @Override
            public JsonSchema remove(int index) {
                throw new UnsupportedOperationException("remove not supported");
            }
        };

        // Populate the list via reflection to bypass the throwing add().
        Field delegateField = throwingList.getClass().getDeclaredField("delegate");
        delegateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> delegate = (List<JsonSchema>) delegateField.get(throwingList);
        delegate.add(proxyJsonSchema()); // now the list has one element

        setTupleSchema(validator, throwingList);

        // Subclass that attempts to modify the received list, causing the exception.
        TestablePrefixItemsValidator modSpy = new TestablePrefixItemsValidator("testPath", JsonNodeFactory.instance.arrayNode()) {
            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                // This add will trigger the UnsupportedOperationException from throwingList.
                schemas.add(null);
            }
        };
        // Copy the tupleSchema we prepared.
        setTupleSchema(modSpy, throwingList);

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                modSpy::preloadJsonSchema,
                "Modification attempt should cause exception to propagate");
        assertEquals("add not supported", thrown.getMessage());
    }
}
```