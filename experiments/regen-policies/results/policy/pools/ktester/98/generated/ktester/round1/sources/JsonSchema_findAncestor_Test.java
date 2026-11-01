package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    /**
     * Helper to create a very simple schema (empty object).
     */
    private static JsonSchema createSchema() {
        // The library provides a factory that can build a schema from a JSON string.
        // An empty object is sufficient for our purposes – it creates a schema with
        // no validation rules and, crucially, a {@code null} parent.
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema("{}");
    }

    /**
     * Helper that sets the {@code parentSchema} field via reflection.
     */
    private static void setParent(JsonSchema child, JsonSchema parent) {
        try {
            Field f = JsonSchema.class.getDeclaredField("parentSchema");
            f.setAccessible(true);
            f.set(child, parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A schema without a parent should return itself as the ancestor.
     */
    @Test
    void findAncestorWhenRoot() {
        // parent == null
        JsonSchema root = createSchema();
        JsonSchema result = root.findAncestor();
        assertSame(root, result, "Root schema should be its own ancestor");
    }

    /**
     * A child schema should resolve its ancestor to the root schema.
     */
    @Test
    void findAncestorWhenChild() {
        JsonSchema root = createSchema();
        JsonSchema child = createSchema();
        setParent(child, root);
        JsonSchema result = child.findAncestor();
        assertSame(root, result, "Ancestor of a direct child should be the root schema");
    }

    /**
     * A deeper hierarchy (grand‑child → child → root) should still resolve to the root.
     */
    @Test
    void findAncestorWhenGrandChild() {
        JsonSchema root = createSchema();
        JsonSchema child = createSchema();
        JsonSchema grandChild = createSchema();
        setParent(child, root);
        setParent(grandChild, child);
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
        JsonSchema a = createSchema();
        JsonSchema b = createSchema();
        setParent(a, b);
        setParent(b, a);
        assertThrows(StackOverflowError.class, a::findAncestor, "Cyclic parent chain should cause StackOverflowError");
    }

    /**
     * A schema whose parent is itself also creates an immediate cycle.
     * Expect a {@link StackOverflowError}.
     */
    @Test
    void findAncestorWithSelfReferentialParent() {
        JsonSchema self = createSchema();
        setParent(self, self);
        assertThrows(StackOverflowError.class, self::findAncestor, "Self‑referential parent should cause StackOverflowError");
    }

    /**
     * Idempotent root retrieval – calling {@code findAncestor()} multiple
     * times on a root schema should always return the same instance.
     */
    @Test
    void findAncestorIdempotentRoot() {
        JsonSchema root = createSchema();
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
        // create the schemas
        for (int i = 0; i < depth; i++) {
            chain.add(createSchema());
        }
        // wire the chain: element i's parent is element i‑1 (i > 0)
        for (int i = depth - 1; i > 0; i--) {
            setParent(chain.get(i), chain.get(i - 1));
        }
        JsonSchema deepest = chain.get(depth - 1);
        JsonSchema ancestor = deepest.findAncestor();
        assertSame(chain.get(0), ancestor, "Deep (50‑level) chain should resolve to the first element as ancestor");
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
            chain.add(createSchema());
        }
        for (int i = depth - 1; i > 0; i--) {
            setParent(chain.get(i), chain.get(i - 1));
        }
        JsonSchema deepest = chain.get(depth - 1);
        assertThrows(StackOverflowError.class, deepest::findAncestor, "A chain deeper than the JVM stack limit should cause StackOverflowError");
    }

    /**
     * Verify that exceptions thrown by {@code getParentSchema()} are
     * propagated unchanged by {@code findAncestor()}.
     */
//     @Test
//     void findAncestorParentThrowsException() {
//         JsonSchema faultyParent = mock(JsonSchema.class, CALLS_REAL_METHODS);
//         when(faultyParent.getParentSchema()).thenThrow(new RuntimeException("parent failure"));
//         JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
//         when(child.getParentSchema()).thenReturn(faultyParent);
//         RuntimeException ex = assertThrows(RuntimeException.class, child::findAncestor, "Exception from getParentSchema() should propagate");
//         assertEquals("parent failure", ex.getMessage(), "Propagated exception should retain original message");
//     }
}