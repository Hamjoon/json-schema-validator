```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Answers;

/**
 * Unit tests for {@link JsonSchema#findSchemaResourceRoot()}.
 *
 * The method walks up the parent chain until it finds a schema whose
 * {@code isSchemaResourceRoot()} returns {@code true}.  The tests use
 * Mockito spies (real‑method calls) so that the actual implementation of
 * {@code findSchemaResourceRoot()} is exercised while the dependent methods
 * {@code isSchemaResourceRoot()} and {@code getParentSchema()} are stubbed.
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
}
```