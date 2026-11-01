```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#findAncestor()}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>simple cases (no parent, one‑level, multi‑level)</li>
 *   <li>deep hierarchies (10 levels)</li>
 *   <li>idempotence of repeated calls</li>
 *   <li>absence of side‑effects on the parent reference</li>
 *   <li>behaviour when a parent throws an exception</li>
 *   <li>cycle detection (self‑cycle and indirect cycle) – expected to overflow the stack</li>
 * </ul>
 *
 * <p>All mocks are reset after each test to avoid cross‑test interference.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    /** Mock representing the top‑most schema (no parent). */
    @Mock
    JsonSchema rootSchema;

    /** Mock representing a child of {@link #rootSchema}. */
    @Mock
    JsonSchema childSchema;

    /** Mock representing a grand‑child of {@link #rootSchema}. */
    @Mock
    JsonSchema grandChildSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these unit tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the parent‑chain for the three basic mocks:
        // rootSchema.getParentSchema() → null (default behaviour of a mock)
        when(childSchema.getParentSchema()).thenReturn(rootSchema);
        when(grandChildSchema.getParentSchema()).thenReturn(childSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(rootSchema, childSchema, grandChildSchema);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /* --------------------------------------------------------------------- */
    /* Existing basic tests (kept unchanged)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void findAncestorReturnsSelfWhenNoParent() {
        // rootSchema has no parent, so the ancestor should be itself.
        JsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result,
                "When a schema has no parent, findAncestor() must return the schema itself.");
    }

    @Test
    void findAncestorWalksUpTheChainToRoot() {
        // Starting from the deepest node, the method should climb to the root.
        JsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result,
                "findAncestor() should return the top‑most ancestor in the parent chain.");
    }

    @Test
    void findAncestorHandlesTwoLevelChain() {
        // From a direct child, the root should be returned.
        JsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result,
                "For a one‑level child, findAncestor() must still return the root schema.");
    }

    /* --------------------------------------------------------------------- */
    /* Additional test cases derived from the JSON specification               */
    /* --------------------------------------------------------------------- */

    @Test
    void findAncestorSelfReferencingParentCycleThrowsStackOverflow() {
        // Create a schema whose parent points to itself → infinite recursion.
        JsonSchema selfCycle = mock(JsonSchema.class);
        when(selfCycle.getParentSchema()).thenReturn(selfCycle);

        assertThrows(StackOverflowError.class,
                selfCycle::findAncestor,
                "A self‑referencing parent should cause a StackOverflowError.");
    }

    @Test
    void findAncestorIndirectCycleThrowsStackOverflow() {
        // A → B → C → A creates an indirect cycle.
        JsonSchema a = mock(JsonSchema.class);
        JsonSchema b = mock(JsonSchema.class);
        JsonSchema c = mock(JsonSchema.class);

        when(a.getParentSchema()).thenReturn(b);
        when(b.getParentSchema()).thenReturn(c);
        when(c.getParentSchema()).thenReturn(a);

        assertThrows(StackOverflowError.class,
                a::findAncestor,
                "An indirect parent cycle should eventually cause a StackOverflowError.");
    }

    @Test
    void findAncestorDeepHierarchyTenLevels() {
        // Build a chain of 10 schemas (leaf … root).
        List<JsonSchema> chain = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            chain.add(mock(JsonSchema.class));
        }
        // Set parent references: chain[i] -> chain[i-1]; root (index 0) has null parent.
        for (int i = 1; i < chain.size(); i++) {
            when(chain.get(i).getParentSchema()).thenReturn(chain.get(i - 1));
        }

        JsonSchema leaf = chain.get(9);
        JsonSchema root = chain.get(0);
        JsonSchema result = leaf.findAncestor();

        assertSame(root, result,
                "A ten‑level hierarchy should return the top‑most root schema after recursion.");
    }

    @Test
    void findAncestorIdempotentCallsReturnSameInstance() {
        // Using the previously configured chain: leaf -> child -> root.
        JsonSchema firstCall = grandChildSchema.findAncestor();
        JsonSchema secondCall = grandChildSchema.findAncestor();

        assertSame(rootSchema, firstCall,
                "First call should return the root schema.");
        assertSame(rootSchema, secondCall,
                "Second call should return the same root schema instance, proving idempotence.");
    }

    @Test
    void findAncestorDoesNotModifyParentReference() {
        // Verify that the parent reference stays unchanged after invocation.
        JsonSchema parentBefore = childSchema.getParentSchema();
        childSchema.findAncestor(); // invoke method under test
        JsonSchema parentAfter = childSchema.getParentSchema();

        assertSame(parentBefore, parentAfter,
                "The parent reference must remain unchanged after findAncestor() execution.");
    }

    @Test
    void findAncestorPropagatesExceptionFromParent() {
        // Parent's findAncestor() is stubbed to throw an IllegalArgumentException.
        JsonSchema problematicParent = mock(JsonSchema.class);
        when(problematicParent.getParentSchema()).thenReturn(null); // root of its own chain
        when(problematicParent.findAncestor()).thenThrow(new IllegalArgumentException("parent failure"));

        // Child points to the problematic parent.
        JsonSchema child = mock(JsonSchema.class);
        when(child.getParentSchema()).thenReturn(problematicParent);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                child::findAncestor,
                "Exception thrown by the parent should propagate unchanged.");

        assertEquals("parent failure", thrown.getMessage(),
                "Propagated exception should retain the original message.");
    }
}
```