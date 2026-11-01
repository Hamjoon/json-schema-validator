```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for {@link JsonSchema#findAncestor()} covering
 * edge‑cases such as cycles, deep recursion and exception propagation.
 *
 * The tests use {@code CALLS_REAL_METHODS} so that the real implementation of
 * {@code findAncestor()} is executed while {@code getParentSchema()} is stubbed
 * to build the required hierarchy.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * A schema without a parent should return itself as the ancestor.
     */
    @Test
    void findAncestorWhenRoot() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema result = root.findAncestor();

        assertSame(root, result, "Root schema should be its own ancestor");
    }

    /**
     * A child schema should resolve its ancestor to the root schema.
     */
    @Test
    void findAncestorWhenChild() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getParentSchema()).thenReturn(root);

        JsonSchema result = child.findAncestor();

        assertSame(root, result, "Ancestor of a direct child should be the root schema");
    }

    /**
     * A deeper hierarchy (grand‑child → child → root) should still resolve to the root.
     */
    @Test
    void findAncestorWhenGrandChild() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getParentSchema()).thenReturn(root);

        JsonSchema grandChild = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(grandChild.getParentSchema()).thenReturn(child);

        JsonSchema result = grandChild.findAncestor();

        assertSame(root, result, "Ancestor of a grand‑child should be the root schema");
    }

    /**
     * Two schemas that reference each other as parents create a cycle.
     * The current implementation does not guard against cycles, so a
     * {@link StackOverflowError} is expected.
     */
    @Test
    void findAncestorWithCyclicParentReference() {
        JsonSchema a = mock(JsonSchema.class, CALLS_REAL_METHODS);
        JsonSchema b = mock(JsonSchema.class, CALLS_REAL_METHODS);

        when(a.getParentSchema()).thenReturn(b);
        when(b.getParentSchema()).thenReturn(a);

        assertThrows(StackOverflowError.class,
                a::findAncestor,
                "Cyclic parent chain should cause StackOverflowError");
    }

    /**
     * A schema whose parent is itself also creates an immediate cycle.
     * Expect a {@link StackOverflowError}.
     */
    @Test
    void findAncestorWithSelfReferentialParent() {
        JsonSchema self = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(self.getParentSchema()).thenReturn(self);

        assertThrows(StackOverflowError.class,
                self::findAncestor,
                "Self‑referential parent should cause StackOverflowError");
    }

    /**
     * Idempotent root retrieval – calling {@code findAncestor()} multiple
     * times on a root schema should always return the same instance.
     */
    @Test
    void findAncestorIdempotentRoot() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getParentSchema()).thenReturn(null);

        JsonSchema first = root.findAncestor();
        JsonSchema second = root.findAncestor();

        assertSame(root, first, "First call should return the root itself");
        assertSame(root, second, "Second call should also return the root itself");
        assertSame(first, second, "Both calls should return the identical instance");
    }

    /**
     * Build a non‑cyclic chain of 50 schemas and verify that the deepest
     * schema resolves to the top‑most root without stack overflow.
     */
    @Test
    void findAncestorDeepNonCyclicChain() {
        final int depth = 50;
        List<JsonSchema> chain = new ArrayList<>(depth);

        // Create mocks for each level.
        for (int i = 0; i < depth; i++) {
            JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
            when(schema.getParentSchema()).thenReturn(null); // default, will be overridden
            chain.add(schema);
        }

        // Wire the chain: last element is the root (parent == null)
        for (int i = depth - 1; i > 0; i--) {
            when(chain.get(i).getParentSchema()).thenReturn(chain.get(i - 1));
        }
        // The root (index 0) already returns null.

        JsonSchema deepest = chain.get(depth - 1);
        JsonSchema ancestor = deepest.findAncestor();

        assertSame(chain.get(0), ancestor,
                "Deep (50‑level) chain should resolve to the first element as ancestor");
    }

    /**
     * Construct a very deep chain (e.g., 2000 levels) that exceeds typical
     * JVM stack depth. The recursive implementation is expected to throw
     * {@link StackOverflowError}.
     */
    @Test
    void findAncestorDeepChainExceedingStack() {
        final int depth = 2000;
        List<JsonSchema> chain = new ArrayList<>(depth);

        for (int i = 0; i < depth; i++) {
            JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
            when(schema.getParentSchema()).thenReturn(null);
            chain.add(schema);
        }

        for (int i = depth - 1; i > 0; i--) {
            when(chain.get(i).getParentSchema()).thenReturn(chain.get(i - 1));
        }

        JsonSchema deepest = chain.get(depth - 1);
        assertThrows(StackOverflowError.class,
                deepest::findAncestor,
                "A chain deeper than the JVM stack limit should cause StackOverflowError");
    }

    /**
     * Verify that exceptions thrown by {@code getParentSchema()} are
     * propagated unchanged by {@code findAncestor()}.
     */
    @Test
    void findAncestorParentThrowsException() {
        JsonSchema faultyParent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(faultyParent.getParentSchema()).thenThrow(new RuntimeException("parent failure"));

        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getParentSchema()).thenReturn(faultyParent);

        RuntimeException ex = assertThrows(RuntimeException.class,
                child::findAncestor,
                "Exception from getParentSchema() should propagate");
        assertEquals("parent failure", ex.getMessage(),
                "Propagated exception should retain original message");
    }
}
```