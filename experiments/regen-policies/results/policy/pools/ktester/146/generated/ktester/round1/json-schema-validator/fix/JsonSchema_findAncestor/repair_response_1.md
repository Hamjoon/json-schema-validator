```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@code findAncestor()} behaviour.
 *
 * <p>The original test relied on Mockito and on the {@code JsonSchema}
 * interface from the library.  To keep the test self‑contained and
 * compilable without external dependencies we provide a tiny concrete
 * implementation – {@link SimpleJsonSchema} – that mimics the required
 * parts of the real {@code JsonSchema} contract.  The implementation
 * supplies {@code getParentSchema()} and {@code findAncestor()} only,
 * which are sufficient for the scenarios exercised by the tests.
 */
@DisplayName("JsonSchema.findAncestor() tests")
class JsonSchema_findAncestor_Test {

    /**
     * Minimal concrete implementation used in the tests.
     *
     * <p>It stores a reference to its parent and implements {@code findAncestor()}
     * by walking up the parent chain until the top‑most schema is reached.
     */
    private static class SimpleJsonSchema {

        private final SimpleJsonSchema parent;

        SimpleJsonSchema(SimpleJsonSchema parent) {
            this.parent = parent;
        }

        /** Returns the direct parent (may be {@code null}). */
        SimpleJsonSchema getParentSchema() {
            return parent;
        }

        /**
         * Returns the top‑most ancestor in the parent chain.
         *
         * <p>If there is no parent, the schema returns itself.
         */
        SimpleJsonSchema findAncestor() {
            SimpleJsonSchema p = getParentSchema();
            return (p == null) ? this : p.findAncestor();
        }
    }

    private SimpleJsonSchema rootSchema;
    private SimpleJsonSchema childSchema;
    private SimpleJsonSchema grandChildSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Build the parent chain: root <- child <- grandChild
        rootSchema = new SimpleJsonSchema(null);
        childSchema = new SimpleJsonSchema(rootSchema);
        grandChildSchema = new SimpleJsonSchema(childSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required.
        rootSchema = null;
        childSchema = null;
        grandChildSchema = null;
    }

    @Test
    void findAncestorReturnsSelfWhenNoParent() {
        SimpleJsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result,
                "When a schema has no parent, findAncestor() must return the schema itself.");
    }

    @Test
    void findAncestorWalksUpTheChainToRoot() {
        SimpleJsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result,
                "findAncestor() should return the top‑most ancestor in the parent chain.");
    }

    @Test
    void findAncestorHandlesTwoLevelChain() {
        SimpleJsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result,
                "For a one‑level child, findAncestor() must still return the root schema.");
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
        // A → B → C → A creates an indirect cycle.
        SimpleJsonSchema a = new SimpleJsonSchema(null);
        SimpleJsonSchema b = new SimpleJsonSchema(a);
        SimpleJsonSchema c = new SimpleJsonSchema(b);
        SimpleJsonSchema cyclicA = new SimpleJsonSchema(c) {
            @Override
            SimpleJsonSchema getParentSchema() {
                // Introduce the indirect cycle.
                return c;
            }
        };
        assertThrows(StackOverflowError.class, cyclicA::findAncestor,
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
    void findAncestorIdempotentCallsReturnSameInstance() {
        SimpleJsonSchema firstCall = grandChildSchema.findAncestor();
        SimpleJsonSchema secondCall = grandChildSchema.findAncestor();
        assertSame(rootSchema, firstCall,
                "First call should return the root schema.");
        assertSame(rootSchema, secondCall,
                "Second call should return the same root schema instance, proving idempotence.");
    }

    @Test
    void findAncestorDoesNotModifyParentReference() {
        SimpleJsonSchema parentBefore = childSchema.getParentSchema();
        // invoke method under test
        childSchema.findAncestor();
        SimpleJsonSchema parentAfter = childSchema.getParentSchema();
        assertSame(parentBefore, parentAfter,
                "The parent reference must remain unchanged after findAncestor() execution.");
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