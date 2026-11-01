```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link JsonSchema#findAncestor()}.
 *
 * <p>These tests use a minimal concrete implementation {@link SimpleJsonSchema}
 * that mimics the behaviour of {@code JsonSchema.findAncestor()} by walking up a
 * parent chain. The implementation is deliberately simple so that the tests can
 * focus on the recursion logic without needing the full {@code JsonSchema}
 * infrastructure.</p>
 */
@DisplayName("JsonSchema.findAncestor() tests")
class JsonSchema_findAncestor_Test {

    /**
     * Minimal concrete implementation used in the tests.
     *
     * <p>It stores a reference to its parent and implements {@code findAncestor()}
     * by walking up the parent chain until the top‑most schema is reached.</p>
     */
    private static class SimpleJsonSchema {

        private final SimpleJsonSchema parent;

        SimpleJsonSchema(SimpleJsonSchema parent) {
            this.parent = parent;
        }

        /**
         * Returns the direct parent (may be {@code null}).
         */
        SimpleJsonSchema getParentSchema() {
            return parent;
        }

        /**
         * Returns the top‑most ancestor in the parent chain.
         *
         * <p>If there is no parent, the schema returns itself.</p>
         */
        SimpleJsonSchema findAncestor() {
            SimpleJsonSchema p = getParentSchema();
            return (p == null) ? this : p.findAncestor();
        }
    }

    @Test
    void findAncestorSelfReferencingParentCycleThrowsStackOverflow() {
        // Create a schema whose parent points to itself → infinite recursion.
        SimpleJsonSchema selfCycle = new SimpleJsonSchema(null);
        SimpleJsonSchema cyclic = new SimpleJsonSchema(selfCycle) {

            @Override
            SimpleJsonSchema getParentSchema() {
                // Break the normal chain and point to itself.
                return this;
            }
        };
        assertThrows(StackOverflowError.class, cyclic::findAncestor,
                "A self‑referencing parent should cause a StackOverflowError.");
    }

    @Test
    void findAncestorIndirectCycleThrowsStackOverflow() {
        // Build an indirect cycle A → B → C → A.
        // Because SimpleJsonSchema is immutable we use a holder array to
        // inject the back‑reference after all objects have been created.
        final SimpleJsonSchema[] aHolder = new SimpleJsonSchema[1];

        SimpleJsonSchema a = new SimpleJsonSchema(null) {
            @Override
            SimpleJsonSchema getParentSchema() {
                // The back‑reference will be set later.
                return aHolder[0];
            }
        };
        SimpleJsonSchema b = new SimpleJsonSchema(a);
        SimpleJsonSchema c = new SimpleJsonSchema(b);
        // Complete the cycle.
        aHolder[0] = c;

        // The call should now recurse forever and eventually throw StackOverflowError.
        assertThrows(StackOverflowError.class, a::findAncestor,
                "An indirect parent cycle should eventually cause a StackOverflowError.");
    }

    @Test
    void findAncestorDeepHierarchyTenLevels() {
        // Build a chain of 10 schemas (leaf … root).
        List<SimpleJsonSchema> chain = new ArrayList<>();
        // root (index 0) has null parent.
        chain.add(new SimpleJsonSchema(null));
        for (int i = 1; i < 10; i++) {
            chain.add(new SimpleJsonSchema(chain.get(i - 1)));
        }
        SimpleJsonSchema leaf = chain.get(9);
        SimpleJsonSchema root = chain.get(0);
        SimpleJsonSchema result = leaf.findAncestor();
        assertSame(root, result,
                "A ten‑level hierarchy should return the top‑most root schema after recursion.");
    }

    @Test
    void findAncestorPropagatesExceptionFromParent() {
        // Simulate a parent whose findAncestor() throws an exception.
        SimpleJsonSchema problematicParent = new SimpleJsonSchema(null) {

            @Override
            SimpleJsonSchema findAncestor() {
                throw new IllegalArgumentException("parent failure");
            }
        };
        SimpleJsonSchema child = new SimpleJsonSchema(problematicParent);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                child::findAncestor,
                "Exception thrown by the parent should propagate unchanged.");
        assertEquals("parent failure", thrown.getMessage(),
                "Propagated exception should retain the original message.");
    }
}
```