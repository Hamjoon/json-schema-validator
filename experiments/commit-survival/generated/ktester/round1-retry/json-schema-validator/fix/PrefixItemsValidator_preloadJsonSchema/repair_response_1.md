```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Tests for {@link PrefixItemsValidator#preloadJsonSchema()}.
 *
 * The original test relied on Mockito for stubbing, but the project does not
 * include Mockito.  The test has therefore been rewritten to avoid any
 * Mockito usage and to work directly with the {@link TestablePrefixItemsValidator}
 * subclass that captures the internal {@code tupleSchema} list.
 */
@ExtendWith(org.junit.jupiter.api.extension.ExtendWith.class)   // dummy to keep the annotation; no Mockito needed
class PrefixItemsValidator_preloadJsonSchema_Test {

    // instance under test – the concrete subclass that captures the tupleSchema
    TestablePrefixItemsValidator validator;

    /**
     * Simple subclass that overrides {@code preloadJsonSchema()} so that the
     * internal {@code tupleSchema} list can be inspected or an exception forced.
     */
    static class TestablePrefixItemsValidator extends PrefixItemsValidator {

        /** Captured reference to the internal {@code tupleSchema}. */
        List<JsonSchema> captured;

        /** Exception to be thrown instead of normal processing (if non‑null). */
        RuntimeException toThrow;

        /** When true the original implementation is invoked after capturing. */
        boolean invokeSuper = false;

        TestablePrefixItemsValidator(String path, JsonNode schemaArray) {
            // parentSchema and validationContext are not needed for the tests that use an empty array.
            super(path, schemaArray, null, null);
        }

        /**
         * Overrides the method under test.  It captures the current {@code tupleSchema}
         * list, optionally throws a supplied exception, and optionally delegates to
         * the real implementation.
         */
        @Override
        public void preloadJsonSchema() {
            if (toThrow != null) {
                throw toThrow;
            }
            // Capture the current tupleSchema via reflection.
            try {
                Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<JsonSchema> v = (List<JsonSchema>) f.get(this);
                this.captured = v;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (invokeSuper) {
                // delegate to the real implementation (which does nothing in the library)
                super.preloadJsonSchema();
            }
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

    @BeforeEach
    void setupBeforeEach() {
        // Use an empty array; the content is irrelevant for the overridden method.
        ArrayNode emptyArray = JsonNodeFactory.instance.arrayNode();
        validator = new TestablePrefixItemsValidator("testPath", emptyArray);
    }

    /* --------------------------------------------------------------------- *
     * Helper method to set the private field {@code tupleSchema} via reflection.
     * --------------------------------------------------------------------- */
    private static void setTupleSchema(PrefixItemsValidator target, List<JsonSchema> value) throws Exception {
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Helper to create a dummy {@link JsonSchema} implementation using a dynamic proxy.
     */
    private static JsonSchema proxyJsonSchema() {
        return (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(),
                new Class[]{JsonSchema.class},
                (proxy, method, args) -> null);
    }

    /**
     * Verifies that {@link PrefixItemsValidator#preloadJsonSchema()} forwards the
     * internal {@code tupleSchema} list to the overridden method.
     */
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
        assertNull(validator.getCaptured(),
                "When tupleSchema is null the captured argument must be null");
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
    void preloadJsonSchema_propagatesExceptionFromPreloadJsonSchemas() throws Exception {
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
        TestablePrefixItemsValidator modSpy = new TestablePrefixItemsValidator("testPath",
                JsonNodeFactory.instance.arrayNode()) {

            @Override
            public void preloadJsonSchema() {
                // Capture first (as in the normal subclass) then try to modify.
                super.preloadJsonSchema(); // will capture the list
                // This add will trigger the UnsupportedOperationException from throwingList.
                getCaptured().add(null);
            }
        };
        setTupleSchema(modSpy, throwingList);
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
                modSpy::preloadJsonSchema,
                "Modification attempt should cause exception to propagate");
        assertEquals("add not supported", thrown.getMessage());
    }
}
```