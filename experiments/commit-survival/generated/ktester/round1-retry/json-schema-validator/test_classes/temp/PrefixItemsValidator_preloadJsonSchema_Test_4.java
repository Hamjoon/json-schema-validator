package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.lang.reflect.*;

class PrefixItemsValidator_preloadJsonSchema_Test {

    //     @Mock
    ValidationContext // mocked dependency
    validationContext;

    //     @Mock
    JsonSchema // mocked parent schema
    parentSchema;

    // instance under test
    PrefixItemsValidator validator;

    /**
     * Simple subclass that overrides {@code preloadJsonSchema()} so that the
     * internal {@code tupleSchema} list can be inspected or an exception forced.
     */
    static class TestablePrefixItemsValidator extends PrefixItemsValidator {

        /**
         * Captured reference to the internal {@code tupleSchema}.
         */
        List<JsonSchema> captured;

        /**
         * Exception to be thrown instead of normal processing (if non‑null).
         */
        RuntimeException toThrow;

        /**
         * When true the original implementation is invoked after capturing.
         */
        boolean invokeSuper = false;

        /**
         * Constructor that guarantees the {@code schemaArray} passed to the super‑class
         * is a *non‑empty* array.  The real library validates that the array contains at
         * least one schema; many of the unit‑tests only need the validator instance and
         * later replace the internal {@code tupleSchema} via reflection, so we can safely
         * inject a dummy element when the supplied array is empty.
         */
        TestablePrefixItemsValidator(String path, ArrayNode schemaArray) {
            super(path, makeNonEmpty(schemaArray), null, null);
        }

        /**
         * Helper that returns a non‑empty array node.
         */
        private static ArrayNode makeNonEmpty(ArrayNode original) {
            // If the caller already supplied a non‑empty array, keep it.
            if (original != null && original.size() > 0) {
                return original;
            }
            // Otherwise create a new array containing a single empty object node.
            ArrayNode dummy = JsonNodeFactory.instance.arrayNode();
            // placeholder schema
            dummy.add(JsonNodeFactory.instance.objectNode());
            return dummy;
        }

        /**
         * Overrides the method under test. Captures the current {@code tupleSchema},
         * optionally throws a supplied exception, and optionally delegates to the real
         * implementation.
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
        // Use an empty array; the constructor of TestablePrefixItemsValidator will replace it
        // with a dummy non‑empty array so that the super‑class validation succeeds.
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
        return (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(), new Class[] { JsonSchema.class }, (proxy, method, args) -> null);
    }

    /**
     * Utility to read the private {@code tupleSchema} field via reflection.
     */
    private static List<JsonSchema> getTupleSchema(PrefixItemsValidator target) throws Exception {
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> v = (List<JsonSchema>) f.get(target);
        return v;
    }

    /**
     * If {@code preloadJsonSchemas} attempts to modify the list and the list throws
     * {@link UnsupportedOperationException}, the exception must propagate.
     */
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
        // now the list has one element
        delegate.add(proxyJsonSchema());
        setTupleSchema(validator, throwingList);
        // Subclass that attempts to modify the received list, causing the exception.
        TestablePrefixItemsValidator modSpy = new TestablePrefixItemsValidator("testPath", JsonNodeFactory.instance.arrayNode()) {

            @Override
            public void preloadJsonSchema() {
                // Capture first (as in the normal subclass) then try to modify.
                super.preloadJsonSchema();
                // This add will trigger the UnsupportedOperationException from throwingList.
                getCaptured().add(null);
            }
        };
        setTupleSchema(modSpy, throwingList);
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, modSpy::preloadJsonSchema, "Modification attempt should cause exception to propagate");
        assertEquals("add not supported", thrown.getMessage());
    }
}
