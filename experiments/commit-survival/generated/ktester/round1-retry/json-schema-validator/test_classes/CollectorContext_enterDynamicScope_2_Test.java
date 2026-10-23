package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Deque;

@ExtendWith(MockitoExtension.class)
class CollectorContext_enterDynamicScope_2_Test {

    private CollectorContext collectorContext;

    @Mock
    private JsonSchema // mocked JsonSchema used as a containing schema
    mockJsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CollectorContext for each test
        collectorContext = new CollectorContext();
        // mockJsonSchema is injected by MockitoExtension
    }

    /* -------------------------------------------------
     * Helper utilities
     * ------------------------------------------------- */
    private int getDynamicScopeSize() {
        try {
            Field f = CollectorContext.class.getDeclaredField("dynamicScopes");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Deque<CollectorContext.Scope> dq = (Deque<CollectorContext.Scope>) f.get(collectorContext);
            return dq.size();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void pushScopeDirectly(CollectorContext.Scope scope) {
        try {
            Field f = CollectorContext.class.getDeclaredField("dynamicScopes");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Deque<CollectorContext.Scope> dq = (Deque<CollectorContext.Scope>) f.get(collectorContext);
            dq.push(scope);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that {@code enterDynamicScope(JsonSchema)} pushes a new {@link CollectorContext.Scope}
     * onto the internal stack and returns the previous (parent) scope.
     * The newly created scope must contain the supplied {@link JsonSchema}.
     */
    @Test
    void testEnterDynamicScopeWithProvidedSchema() {
        // Act – push a new dynamic scope using the mocked JsonSchema
        CollectorContext.Scope parentScope = collectorContext.enterDynamicScope(mockJsonSchema);
        // The method should return the previous (top) scope that existed before the push
        assertNotNull(parentScope, "Parent scope should never be null");
        // The current top scope after the push can be obtained via getDynamicScope()
        CollectorContext.Scope currentTop = collectorContext.getDynamicScope();
        assertNotNull(currentTop, "Current top scope should not be null");
        // The newly created scope must have the provided JsonSchema as its containing schema
        assertSame(mockJsonSchema, currentTop.getContainingSchema(), "The new dynamic scope should contain the supplied JsonSchema");
        // The parent scope returned should be the one that was on top before the push.
        // Its containing schema is whatever the CollectorContext created initially (likely null)
        // but we can verify that it is *different* from the new top scope.
        assertNotSame(currentTop, parentScope, "Parent scope returned should be different from the newly created top scope");
    }

    /**
     * Verify that {@code enterDynamicScope()} (no‑arg overload) delegates to the
     * {@code enterDynamicScope(JsonSchema)} implementation and uses the containing
     * schema of the current top scope when the argument is {@code null}.
     */
    @Test
    void testEnterDynamicScopeWithNullUsesParentContainingSchema() {
        // First, push a scope with an explicit containing schema (mockJsonSchema)
        collectorContext.enterDynamicScope(mockJsonSchema);
        // The current top now has mockJsonSchema as its containing schema
        CollectorContext.Scope firstTop = collectorContext.getDynamicScope();
        assertSame(mockJsonSchema, firstTop.getContainingSchema(), "First top scope should contain the mock JsonSchema");
        // Act – invoke the no‑arg version which should use the current top's schema
        // delegates to null argument
        CollectorContext.Scope parentScope = collectorContext.enterDynamicScope();
        // The method should return the scope that was current before this call
        assertSame(firstTop, parentScope, "Parent scope returned should be the scope that was on top before the second push");
        // The new top scope after the second push should also contain the same mockJsonSchema
        CollectorContext.Scope secondTop = collectorContext.getDynamicScope();
        assertNotNull(secondTop, "Second top scope should not be null");
        assertSame(mockJsonSchema, secondTop.getContainingSchema(), "Second top scope should inherit the containing schema from the previous top scope");
    }

    @Test
    @DisplayName("When a parent scope exists and argument is null -> inherit parent's schema")
    void testEnterDynamicScope_WithExistingParentAndNullArgument() {
        // arrange: push an initial scope with a known schema
        CollectorContext.Scope first = collectorContext.enterDynamicScope(mockJsonSchema);
        int sizeBefore = getDynamicScopeSize();
        // act: call the overload that takes a JsonSchema, passing null explicitly
        CollectorContext.Scope returnedParent = collectorContext.enterDynamicScope((JsonSchema) null);
        // assert: returned parent is the scope that was on top before the call
        assertSame(first, returnedParent, "Returned parent should be the original top scope");
        // new top scope should contain the same schema as the parent (mockJsonSchema)
        CollectorContext.Scope newTop = collectorContext.getDynamicScope();
        assertSame(mockJsonSchema, newTop.getContainingSchema(), "New top scope must inherit containingSchema from the parent when null is passed");
        // size must have increased by one
        assertEquals(sizeBefore + 1, getDynamicScopeSize(), "Dynamic scope deque size should increase by one");
    }

    @Test
    @DisplayName("When a parent scope exists and a non‑null schema is supplied -> new scope uses supplied schema")
    void testEnterDynamicScope_WithExistingParentAndNonNullArgument() {
        JsonSchema mockNewSchema = mock(JsonSchema.class);
        // arrange: push a first scope
        CollectorContext.Scope first = collectorContext.enterDynamicScope(mockJsonSchema);
        int sizeBefore = getDynamicScopeSize();
        // act
        CollectorContext.Scope returnedParent = collectorContext.enterDynamicScope(mockNewSchema);
        // assert
        assertSame(first, returnedParent, "Returned parent should be the previously pushed scope");
        CollectorContext.Scope newTop = collectorContext.getDynamicScope();
        assertSame(mockNewSchema, newTop.getContainingSchema(), "New top scope must contain the schema that was supplied as argument");
        assertEquals(sizeBefore + 1, getDynamicScopeSize(), "Deque size must increase by one");
    }

    @Test
    @DisplayName("No parent and null argument -> NullPointerException")
    void testEnterDynamicScope_NoParentAndNullArgument_throwsNPE() {
        // collectorContext starts empty
        NullPointerException npe = assertThrows(NullPointerException.class, () -> collectorContext.enterDynamicScope((JsonSchema) null), "Calling with null when there is no parent should throw NPE");
        // The exact message is not guaranteed by the implementation but we assert it's not empty
        assertNotNull(npe.getMessage());
    }

    @Test
    @DisplayName("No parent and non‑null schema -> returns null parent and creates new scope")
    void testEnterDynamicScope_NoParentAndNonNullArgument() {
        JsonSchema mockNewSchema = mock(JsonSchema.class);
        // pre‑condition: deque is empty
        assertEquals(0, getDynamicScopeSize(), "Deque should be empty before first push");
        // act
        CollectorContext.Scope returnedParent = collectorContext.enterDynamicScope(mockNewSchema);
        // assert: parent should be null
        assertNull(returnedParent, "When there is no existing scope the returned parent must be null");
        // new top scope must hold the supplied schema
        CollectorContext.Scope top = collectorContext.getDynamicScope();
        assertNotNull(top, "A new scope must have been created");
        assertSame(mockNewSchema, top.getContainingSchema(), "The newly created scope must contain the supplied schema");
        // size must be exactly 1
        assertEquals(1, getDynamicScopeSize(), "Deque size should be 1 after first push");
    }

    @Test
    @DisplayName("Parent exists with null containingSchema and null argument -> NullPointerException")
    void testEnterDynamicScope_ParentExistsWithNullContainingSchema_NullArgument() {
        // arrange: create a parent scope whose containingSchema is null
        CollectorContext.Scope nullParent = new CollectorContext.Scope(false, false, null);
        pushScopeDirectly(nullParent);
        assertEquals(1, getDynamicScopeSize(), "Parent scope should have been injected");
        // act & assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> collectorContext.enterDynamicScope((JsonSchema) null), "Calling with null when parent containingSchema is null should throw NPE");
        assertNotNull(npe.getMessage());
    }

    @Test
    @DisplayName("Parent exists with null containingSchema and non‑null argument -> overrides null")
    void testEnterDynamicScope_ParentExistsWithNullContainingSchema_NonNullArgument() {
        JsonSchema mockNewSchema = mock(JsonSchema.class);
        // arrange: parent with null containingSchema
        CollectorContext.Scope nullParent = new CollectorContext.Scope(false, false, null);
        pushScopeDirectly(nullParent);
        int sizeBefore = getDynamicScopeSize();
        // act
        CollectorContext.Scope returnedParent = collectorContext.enterDynamicScope(mockNewSchema);
        // assert
        assertSame(nullParent, returnedParent, "Returned parent must be the previously injected null‑schema scope");
        CollectorContext.Scope newTop = collectorContext.getDynamicScope();
        assertSame(mockNewSchema, newTop.getContainingSchema(), "New top scope must contain the supplied non‑null schema, overriding the parent's null");
        assertEquals(sizeBefore + 1, getDynamicScopeSize(), "Deque size must increase by one");
    }
}
