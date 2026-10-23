package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Iterator;

@ExtendWith(MockitoExtension.class)
class CollectorContext_getOutermostSchema_Test {

    private CollectorContext collectorContext;

    @Mock
    private JsonSchema // will act as lexical root
    rootSchema;

    @Mock
    private JsonSchema // a deeper dynamic scope
    childSchema;

    @BeforeEach
    void setupBeforeEach() {
        collectorContext = new CollectorContext();
    }

    /**
     * When the current dynamic scope does not contain a schema,
     * {@code getOutermostSchema()} must throw {@link IllegalStateException}.
     */
    @Test
    void shouldThrowWhenContainingSchemaIsNull() {
        // push a scope whose containing schema is null
        collectorContext.enterDynamicScope((JsonSchema) null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, collectorContext::getOutermostSchema);
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
        // top scope
        collectorContext.enterDynamicScope(rootSchema);
        // deeper scope
        collectorContext.enterDynamicScope(childSchema);
        JsonSchema result = collectorContext.getOutermostSchema();
        // According to the implementation, the first (most recent) dynamic‑anchor
        // containing schema should be returned – i.e., childSchema.
        assertSame(childSchema, result, "The returned schema should be the nearest dynamic anchor");
        // Verify interactions that are relevant for the logic
        // called to obtain lexicalRoot
        verify(childSchema).findLexicalRoot();
        // lexicalRoot.isDynamicAnchor()
        verify(rootSchema).isDynamicAnchor();
        // check during iteration
        verify(childSchema).isDynamicAnchor();
    }

    /**
     * Lexical root is a dynamic anchor but **none** of the dynamic scopes contain
     * a dynamic‑anchor schema (some are null, others return false). The method
     * must fall back to returning the lexical root.
     */
    @Test
    void shouldFallbackToLexicalRootWhenNoDynamicAnchorInStack() {
        // lexical root is a dynamic anchor
        when(rootSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(rootSchema.isDynamicAnchor()).thenReturn(true);
        // a non‑dynamic‑anchor schema
        when(childSchema.isDynamicAnchor()).thenReturn(false);
        when(childSchema.findLexicalRoot()).thenReturn(rootSchema);
        // push a null containing schema and a non‑dynamic‑anchor schema
        collectorContext.enterDynamicScope((JsonSchema) null);
        collectorContext.enterDynamicScope(childSchema);
        JsonSchema result = collectorContext.getOutermostSchema();
        assertSame(rootSchema, result, "When no dynamic anchor is found in the stack, the lexical root should be returned");
        verify(rootSchema).findLexicalRoot();
        verify(rootSchema).isDynamicAnchor();
    }

    /**
     * Lexical root is **not** a dynamic anchor, while a deeper scope *is* a dynamic
     * anchor. The method must ignore the deeper dynamic anchor and still return the
     * lexical root.
     */
    @Test
    void shouldIgnoreDynamicAnchorWhenLexicalRootIsNotDynamicAnchor() {
        // lexical root – not a dynamic anchor
        when(rootSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(rootSchema.isDynamicAnchor()).thenReturn(false);
        // deeper schema that *is* a dynamic anchor
        when(childSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(childSchema.isDynamicAnchor()).thenReturn(true);
        // root scope
        collectorContext.enterDynamicScope(rootSchema);
        // deeper dynamic‑anchor scope
        collectorContext.enterDynamicScope(childSchema);
        JsonSchema result = collectorContext.getOutermostSchema();
        // lexical root wins because it is not a dynamic anchor
        assertSame(rootSchema, result, "Lexical root should be returned even if a deeper scope is a dynamic anchor");
        verify(rootSchema).findLexicalRoot();
        verify(rootSchema).isDynamicAnchor();
        // childSchema.isDynamicAnchor() should never be called because lexicalRoot.isDynamicAnchor() is false
        verify(childSchema, never()).isDynamicAnchor();
    }

    /**
     * Lexical root is a dynamic anchor. The dynamic‑scope stack (newest → oldest)
     * contains: {@code null}, a non‑dynamic‑anchor schema, and finally a
     * dynamic‑anchor schema. The method must return the *most recent* dynamic
     * anchor (the last one pushed).
     */
    @Test
    void shouldReturnMostRecentDynamicAnchorWhenMixedStack() {
        // lexical root – dynamic anchor
        when(rootSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(rootSchema.isDynamicAnchor()).thenReturn(true);
        // a non‑dynamic‑anchor schema
        when(childSchema.isDynamicAnchor()).thenReturn(false);
        when(childSchema.findLexicalRoot()).thenReturn(rootSchema);
        // a second schema that *is* a dynamic anchor
        JsonSchema anotherDynamicAnchor = mock(JsonSchema.class);
        when(anotherDynamicAnchor.isDynamicAnchor()).thenReturn(true);
        when(anotherDynamicAnchor.findLexicalRoot()).thenReturn(rootSchema);
        // push: null → non‑anchor → dynamic‑anchor (newest)
        collectorContext.enterDynamicScope((JsonSchema) null);
        collectorContext.enterDynamicScope(childSchema);
        collectorContext.enterDynamicScope(anotherDynamicAnchor);
        JsonSchema result = collectorContext.getOutermostSchema();
        assertSame(anotherDynamicAnchor, result, "The most recent dynamic‑anchor schema should be returned");
        verify(anotherDynamicAnchor).isDynamicAnchor();
    }

    /**
     * Lexical root is a dynamic anchor and the stack contains **multiple**
     * dynamic‑anchor schemas. The newest (most recent) dynamic anchor must be
     * returned, not an older one.
     */
    @Test
    void shouldReturnNewestDynamicAnchorWhenSeveralPresent() {
        // lexical root – dynamic anchor
        when(rootSchema.findLexicalRoot()).thenReturn(rootSchema);
        when(rootSchema.isDynamicAnchor()).thenReturn(true);
        // older dynamic anchor
        JsonSchema olderAnchor = mock(JsonSchema.class);
        when(olderAnchor.isDynamicAnchor()).thenReturn(true);
        when(olderAnchor.findLexicalRoot()).thenReturn(rootSchema);
        // newest dynamic anchor
        JsonSchema newestAnchor = mock(JsonSchema.class);
        when(newestAnchor.isDynamicAnchor()).thenReturn(true);
        when(newestAnchor.findLexicalRoot()).thenReturn(rootSchema);
        // push older first, then newest
        collectorContext.enterDynamicScope(olderAnchor);
        collectorContext.enterDynamicScope(newestAnchor);
        JsonSchema result = collectorContext.getOutermostSchema();
        assertSame(newestAnchor, result, "The newest dynamic‑anchor schema should be returned");
        verify(newestAnchor).isDynamicAnchor();
        // olderAnchor.isDynamicAnchor() may be called but should not affect the result
    }

    /**
     * If the internal {@code getDynamicScope()} call returns {@code null}
     * (e.g., the dynamic‑scope stack is empty), a {@link NullPointerException}
     * is thrown when the method attempts to call {@code getContainingSchema()}
     * on the null reference.
     */
    @Test
    void shouldThrowNPEWhenDynamicScopeIsNull() {
        // Do **not** push any scope – the internal deque is empty
        NullPointerException ex = assertThrows(NullPointerException.class, collectorContext::getOutermostSchema);
        // implementation does not set a message
        assertNull(ex.getMessage());
    }

    /**
     * When {@code findLexicalRoot()} returns {@code null}, a subsequent call to
     * {@code isDynamicAnchor()} triggers a {@link NullPointerException}.
     */
    @Test
    void shouldThrowNPEWhenLexicalRootIsNull() {
        // mock schema whose lexical root is null
        JsonSchema schemaWithNullRoot = mock(JsonSchema.class);
        when(schemaWithNullRoot.findLexicalRoot()).thenReturn(null);
        // containing schema itself is non‑null, so the first null‑check passes
        collectorContext.enterDynamicScope(schemaWithNullRoot);
        NullPointerException ex = assertThrows(NullPointerException.class, collectorContext::getOutermostSchema);
        // The NPE originates from lexicalRoot.isDynamicAnchor()
        assertNull(ex.getMessage());
    }
}
