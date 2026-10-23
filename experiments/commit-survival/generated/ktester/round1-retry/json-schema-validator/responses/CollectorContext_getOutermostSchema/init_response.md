```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_getOutermostSchema_Test {

    private CollectorContext collectorContext;

    @Mock
    private JsonSchema rootSchema;   // will act as lexical root
    @Mock
    private JsonSchema childSchema;  // a deeper dynamic scope

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * When the current dynamic scope does not contain a schema,
     * {@code getOutermostSchema()} must throw {@link IllegalStateException}.
     */
    @Test
    void shouldThrowWhenContainingSchemaIsNull() {
        // push a scope whose containing schema is null
        collectorContext.enterDynamicScope((JsonSchema) null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                collectorContext::getOutermostSchema);
        assertEquals("Missing a root schema in the dynamic scope.", ex.getMessage());
    }

    /**
     * When the lexical root is not a dynamic anchor, the method should simply
     * return that lexical root.
     */
    @Test
    void shouldReturnLexicalRootWhenNotDynamicAnchor() {
        // root schema is the lexical root and is *not* a dynamic anchor
        when(rootSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(rootSchema.isDynamicAnchor()).thenReturn(false);

        // create a dynamic scope that holds the root schema
        collectorContext.enterDynamicScope(rootSchema);

        JsonSchema result = collectorContext.getOutermostSchema();

        assertSame(rootSchema, result, "The returned schema should be the lexical root");
        verify(rootSchema).findLexicalRoot();
        verify(rootSchema).isDynamicAnchor();
    }

    /**
     * When the lexical root is a dynamic anchor, the method must walk the
     * dynamic‑scope stack and return the first containing schema that is also a
     * dynamic anchor.
     */
    @Test
    void shouldReturnFirstDynamicAnchorInStack() {
        // ----- lexical root (topmost scope) -----
        when(rootSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(rootSchema.isDynamicAnchor()).thenReturn(true);

        // ----- child scope (deeper) -----
        // child schema also reports itself as a dynamic anchor and its lexical root is the same rootSchema
        when(childSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(childSchema.isDynamicAnchor()).thenReturn(true);

        // push the root scope first, then the child scope
        collectorContext.enterDynamicScope(rootSchema);   // top scope
        collectorContext.enterDynamicScope(childSchema); // deeper scope

        JsonSchema result = collectorContext.getOutermostSchema();

        // According to the implementation, the first (most recent) dynamic‑anchor
        // containing schema should be returned – i.e., childSchema.
        assertSame(childSchema, result, "The returned schema should be the nearest dynamic anchor");
        // Verify interactions that are relevant for the logic
        verify(childSchema).findLexicalRoot(); // called to obtain lexicalRoot
        verify(rootSchema).isDynamicAnchor(); // lexicalRoot.isDynamicAnchor()
        verify(childSchema).isDynamicAnchor(); // check during iteration
    }
}
```