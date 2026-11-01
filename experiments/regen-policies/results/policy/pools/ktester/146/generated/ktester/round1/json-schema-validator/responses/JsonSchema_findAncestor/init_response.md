```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

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
        // Configure the parent‑chain:
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

    @Test
    void findAncestorReturnsSelfWhenNoParent() {
        // rootSchema has no parent, so the ancestor should be itself.
        JsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result, "When a schema has no parent, findAncestor() must return the schema itself.");
    }

    @Test
    void findAncestorWalksUpTheChainToRoot() {
        // Starting from the deepest node, the method should climb to the root.
        JsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result, "findAncestor() should return the top‑most ancestor in the parent chain.");
    }

    @Test
    void findAncestorHandlesTwoLevelChain() {
        // From a direct child, the root should be returned.
        JsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result, "For a one‑level child, findAncestor() must still return the root schema.");
    }
}
```