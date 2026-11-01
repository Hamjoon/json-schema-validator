```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#findSchemaResourceRoot()}.
 *
 * The tests build a small hierarchy of {@link JsonSchema} mocks:
 *
 * <pre>
 * leaf ──► middle ──► root
 * </pre>
 *
 * {@code root} reports {@code isSchemaResourceRoot()} as {@code true},
 * while the other two report {@code false}. The {@code getParentSchema()}
 * method is stubbed to return the next element in the chain.
 *
 * The expectations are:
 * <ul>
 *   <li>When called on {@code leaf} the method walks up the chain and returns {@code root}.</li>
 *   <li>When called on {@code root} it returns itself (no walking required).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findSchemaResourceRoot_Test {

    /** The root schema – the one that reports {@code isSchemaResourceRoot()} as true. */
    private JsonSchema root;

    /** An intermediate schema whose parent is {@code root}. */
    private JsonSchema middle;

    /** The leaf schema whose parent is {@code middle}. */
    private JsonSchema leaf;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real‑method mock for JsonSchema so that the actual
        // implementation of findSchemaResourceRoot() is executed,
        // while we can stub the methods it depends on.
        root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null); // root has no parent

        middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        when(middle.getParentSchema()).thenReturn(root);

        leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(middle);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        Mockito.reset(root, middle, leaf);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void shouldReturnRootWhenCalledOnLeaf() {
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(root, result, "The root schema should be returned when starting from the leaf");
    }

    @Test
    void shouldReturnSelfWhenAlreadyRoot() {
        JsonSchema result = root.findSchemaResourceRoot();
        assertSame(root, result, "When the schema itself is the resource root, it should be returned unchanged");
    }
}
```