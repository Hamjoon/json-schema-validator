```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Answers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link JsonSchema#findSchemaResourceRoot()}.
 *
 * <p>The method walks up the parent chain until it finds a schema whose
 * {@code isSchemaResourceRoot()} returns {@code true}.  The tests use
 * Mockito spies (real‑method calls) so that the actual implementation of
 * {@code findSchemaResourceRoot()} is exercised while the dependent methods
 * {@code isSchemaResourceRoot()} and {@code getParentSchema()} are stubbed.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findSchemaResourceRoot_Test {

    /** A mock representing the root schema (the one that is a resource root). */
    private JsonSchema rootSchema;

    /** A mock representing a child schema whose parent is {@code rootSchema}. */
    private JsonSchema childSchema;

    /** A mock representing a grand‑child schema whose parent is {@code childSchema}. */
    private JsonSchema grandChildSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources are required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create spies that call real methods unless explicitly stubbed.
        rootSchema = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
        childSchema = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
        grandChildSchema = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);

        // ----- Stubbing the behaviour required for the traversal -----
        // rootSchema is the resource root.
        when(rootSchema.isSchemaResourceRoot()).thenReturn(true);
        // Its parent is irrelevant – the algorithm stops before calling getParentSchema().
        when(rootSchema.getParentSchema()).thenReturn(null);

        // childSchema is NOT a resource root and its parent is rootSchema.
        when(childSchema.isSchemaResourceRoot()).thenReturn(false);
        when(childSchema.getParentSchema()).thenReturn(rootSchema);

        // grandChildSchema is also NOT a resource root and its parent is childSchema.
        when(grandChildSchema.isSchemaResourceRoot()).thenReturn(false);
        when(grandChildSchema.getParentSchema()).thenReturn(childSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(rootSchema, childSchema, grandChildSchema);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    @DisplayName("findSchemaResourceRoot returns the same instance when it is already the root")
    void whenCurrentSchemaIsRoot_thenReturnedInstanceIsSelf() {
        // rootSchema.isSchemaResourceRoot() == true, so the method should return itself.
        JsonSchema result = rootSchema.findSchemaResourceRoot();

        assertSame(rootSchema, result,
                "When the current schema is a resource root, findSchemaResourceRoot should return the same instance.");
        // Verify that no parent lookup was performed.
        verify(rootSchema, never()).getParentSchema();
    }

    @Test
    @DisplayName("findSchemaResourceRoot walks up one level when immediate parent is the root")
    void whenImmediateParentIsRoot_thenRootIsReturned() {
        // childSchema's parent is rootSchema.
        JsonSchema result = childSchema.findSchemaResourceRoot();

        assertSame(rootSchema, result,
                "The method should return the immediate parent that is a resource root.");
        // Verify traversal order.
        InOrder inOrder = inOrder(childSchema, rootSchema);
        inOrder.verify(childSchema).isSchemaResourceRoot();
        inOrder.verify(childSchema).getParentSchema();
        inOrder.verify(rootSchema).isSchemaResourceRoot();
        verify(rootSchema, never()).getParentSchema();
    }

    @Test
    @DisplayName("findSchemaResourceRoot walks up the parent chain until a root is found")
    void whenSchemaHasAncestors_thenRootIsReturned() {
        // Starting from grandChildSchema, the algorithm should walk:
        // grandChildSchema -> childSchema -> rootSchema.
        JsonSchema result = grandChildSchema.findSchemaResourceRoot();

        assertSame(rootSchema, result,
                "The method should return the nearest ancestor whose isSchemaResourceRoot() is true.");
        // Verify the traversal order.
        InOrder inOrder = inOrder(grandChildSchema, childSchema, rootSchema);
        inOrder.verify(grandChildSchema).isSchemaResourceRoot();
        inOrder.verify(grandChildSchema).getParentSchema();
        inOrder.verify(childSchema).isSchemaResourceRoot();
        inOrder.verify(childSchema).getParentSchema();
        inOrder.verify(rootSchema).isSchemaResourceRoot();
        // No further parent look‑ups after the root is found.
        verify(rootSchema, never()).getParentSchema();
    }

    @Test
    @DisplayName("findSchemaResourceRoot works with a deep hierarchy (10 levels)")
    void whenDeepHierarchyTenLevels_thenTopMostRootReturned() {
        JsonSchema topRoot = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
        when(topRoot.isSchemaResourceRoot()).thenReturn(true);
        when(topRoot.getParentSchema()).thenReturn(null);

        // Build a chain of 9 intermediate schemas whose parent points to the previous one.
        JsonSchema previous = topRoot;
        for (int i = 0; i < 9; i++) {
            JsonSchema current = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
            when(current.isSchemaResourceRoot()).thenReturn(false);
            when(current.getParentSchema()).thenReturn(previous);
            previous = current;
        }
        JsonSchema leaf = previous; // the 10th element (leaf)

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(topRoot, result, "The method should return the level‑10 root schema.");
    }

    @Test
    @DisplayName("findSchemaResourceRoot works with a deep hierarchy (50 levels)")
    void whenDeepHierarchyFiftyLevels_thenTopMostRootReturned() {
        JsonSchema topRoot = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
        when(topRoot.isSchemaResourceRoot()).thenReturn(true);
        when(topRoot.getParentSchema()).thenReturn(null);

        JsonSchema previous = topRoot;
        for (int i = 0; i < 49; i++) {
            JsonSchema current = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
            when(current.isSchemaResourceRoot()).thenReturn(false);
            when(current.getParentSchema()).thenReturn(previous);
            previous = current;
        }
        JsonSchema leaf = previous; // the 50th element

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(topRoot, result, "The method should correctly traverse 49 parent links and return the root.");
    }

    @Test
    @DisplayName("findSchemaResourceRoot propagates IllegalStateException from isSchemaResourceRoot")
    void whenIsSchemaResourceRootThrowsException_thenPropagated() {
        JsonSchema faulty = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
        when(faulty.isSchemaResourceRoot()).thenThrow(new IllegalStateException("Corrupted state"));
        // parent is irrelevant because exception occurs before getParentSchema()
        when(faulty.getParentSchema()).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                faulty::findSchemaResourceRoot,
                "IllegalStateException should propagate unchanged.");
        assertEquals("Corrupted state", ex.getMessage());
    }

    @Test
    @DisplayName("findSchemaResourceRoot propagates UnsupportedOperationException from getParentSchema")
    void whenGetParentSchemaThrowsException_thenPropagated() {
        JsonSchema faulty = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
        when(faulty.isSchemaResourceRoot()).thenReturn(false);
        when(faulty.getParentSchema()).thenThrow(new UnsupportedOperationException("Immutable hierarchy"));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                faulty::findSchemaResourceRoot,
                "UnsupportedOperationException should propagate unchanged.");
        assertEquals("Immutable hierarchy", ex.getMessage());
    }

    @Test
    @DisplayName("findSchemaResourceRoot enters infinite loop when circular reference exists")
    void whenCircularReference_thenMethodTimesOut() {
        // Create two schemas that reference each other as parents.
        JsonSchema a = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);
        JsonSchema b = mock(JsonSchema.class, Answers.CALLS_REAL_METHODS);

        when(a.isSchemaResourceRoot()).thenReturn(false);
        when(b.isSchemaResourceRoot()).thenReturn(false);

        when(a.getParentSchema()).thenReturn(b);
        when(b.getParentSchema()).thenReturn(a);

        // The call should never return; we enforce a timeout and assert that it indeed times out.
        Executable call = () -> a.findSchemaResourceRoot();

        // assertTimeoutPreemptively throws AssertionFailedError (subclass of AssertionError) when the timeout expires.
        assertThrows(AssertionError.class,
                () -> assertTimeoutPreemptively(Duration.ofMillis(200), call),
                "The method should not terminate for a circular parent chain, causing a timeout.");
    }
}
```