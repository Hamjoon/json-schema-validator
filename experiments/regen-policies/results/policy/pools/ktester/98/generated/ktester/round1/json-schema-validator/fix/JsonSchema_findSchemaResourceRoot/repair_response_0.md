```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#findSchemaResourceRoot()}.
 *
 * <p>The tests use a lightweight concrete subclass of {@link JsonSchema}
 * that overrides only the methods required for the algorithm:
 * {@link JsonSchema#isSchemaResourceRoot()} and {@link JsonSchema#getParentSchema()}.
 * This avoids the need for external mocking libraries.</p>
 */
class JsonSchema_findSchemaResourceRoot_Test {

    /** Simple concrete implementation used only for testing. */
    private static class TestJsonSchema extends JsonSchema {
        private JsonSchema parent;
        private final boolean root;

        TestJsonSchema(JsonSchema parent, boolean root) {
            this.parent = parent;
            this.root = root;
        }

        /** Allows changing the parent after construction (used for self‑reference test). */
        void setParent(JsonSchema parent) {
            this.parent = parent;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parent;
        }

        @Override
        public boolean isSchemaResourceRoot() {
            return root;
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Leaf should resolve to the root schema")
    void shouldReturnRootWhenCalledOnLeaf() {
        JsonSchema root = new TestJsonSchema(null, true);
        JsonSchema middle = new TestJsonSchema(root, false);
        JsonSchema leaf = new TestJsonSchema(middle, false);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(root, result,
                "The root schema should be returned when starting from the leaf");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Root should return itself")
    void shouldReturnSelfWhenAlreadyRoot() {
        JsonSchema root = new TestJsonSchema(null, true);

        JsonSchema result = root.findSchemaResourceRoot();
        assertSame(root, result,
                "When the schema itself is the resource root, it should be returned unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Null parent causes NullPointerException")
    void shouldThrowNPEWhenParentIsNullAndNoRoot() {
        JsonSchema orphan = new TestJsonSchema(null, false);
        assertThrows(NullPointerException.class,
                orphan::findSchemaResourceRoot,
                "A null parent while searching for a resource root should result in NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Stops at the nearest true ancestor even if higher ancestors are also true")
    void shouldReturnNearestTrueAncestor() {
        JsonSchema root = new TestJsonSchema(null, true);
        JsonSchema middle = new TestJsonSchema(root, true);   // also a root
        JsonSchema leaf = new TestJsonSchema(middle, false);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(middle, result,
                "The method should stop at the first true ancestor (the direct parent)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Finds root in a deep hierarchy (depth 5)")
    void shouldFindRootInDeepHierarchy() {
        JsonSchema deepRoot = new TestJsonSchema(null, true);
        JsonSchema l4 = new TestJsonSchema(deepRoot, false);
        JsonSchema l3 = new TestJsonSchema(l4, false);
        JsonSchema l2 = new TestJsonSchema(l3, false);
        JsonSchema l1 = new TestJsonSchema(l2, false);
        JsonSchema leaf = new TestJsonSchema(l1, false);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(deepRoot, result,
                "The method should correctly walk five levels and return the deep root");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Self‑referencing schema leads to StackOverflowError")
    void shouldOverflowWhenSelfReferencing() {
        TestJsonSchema self = new TestJsonSchema(null, false);
        // create a self‑reference
        self.setParent(self);

        assertThrows(StackOverflowError.class,
                self::findSchemaResourceRoot,
                "A self‑referencing parent without a true root should eventually cause StackOverflowError");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by isSchemaResourceRoot propagates")
    void shouldPropagateExceptionFromIsSchemaResourceRoot() {
        JsonSchema root = new TestJsonSchema(null, true);

        JsonSchema broken = new JsonSchema() {
            @Override
            public boolean isSchemaResourceRoot() {
                throw new IllegalStateException("boom");
            }

            @Override
            public JsonSchema getParentSchema() {
                return root;
            }
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                broken::findSchemaResourceRoot,
                "The IllegalStateException from isSchemaResourceRoot should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by getParentSchema propagates")
    void shouldPropagateExceptionFromGetParentSchema() {
        JsonSchema broken = new JsonSchema() {
            @Override
            public boolean isSchemaResourceRoot() {
                return false;
            }

            @Override
            public JsonSchema getParentSchema() {
                throw new UnsupportedOperationException("no parent");
            }
        };

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                broken::findSchemaResourceRoot,
                "The UnsupportedOperationException from getParentSchema should propagate");
        assertEquals("no parent", ex.getMessage());
    }
}
```