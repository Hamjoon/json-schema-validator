package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CollectorContext_enterDynamicScope_Test {

    private CollectorContext collectorContext;

    @BeforeEach
    void setupBeforeEach() {
        // The default constructor creates an initial (root) dynamic scope.
        collectorContext = new CollectorContext();
    }

    /**
     * Verify that the no‑arg {@code enterDynamicScope()} creates a new scope,
     * returns it, and makes it the current dynamic scope.
     */
    @Test
    void shouldEnterDynamicScopeWithoutArgument() {
        // GIVEN: a fresh CollectorContext (already set up in @BeforeEach)
        // WHEN
        Scope newScope = collectorContext.enterDynamicScope();
        // THEN
        assertNotNull(newScope, "enterDynamicScope() should return a non‑null Scope");
        assertSame(newScope, collectorContext.getDynamicScope(), "The returned Scope must become the current dynamic scope");
    }

    /**
     * Verify that the overload accepting a {@link JsonSchema} behaves the same
     * as the no‑arg version (i.e., creates a new scope and makes it current).
     */
    @Test
    void shouldEnterDynamicScopeWithJsonSchema(@Mock JsonSchema mockSchema) {
        // GIVEN: a mock JsonSchema (the implementation does not use it for this test)
        // WHEN
        Scope newScope = collectorContext.enterDynamicScope(mockSchema);
        // THEN
        assertNotNull(newScope, "enterDynamicScope(JsonSchema) should return a non‑null Scope");
        assertSame(newScope, collectorContext.getDynamicScope(), "The returned Scope must become the current dynamic scope");
    }

    /**
     * Verify that nested calls push scopes onto the internal stack and that
     * {@code exitDynamicScope()} correctly restores the previous scope.
     */
    @Test
    void shouldMaintainScopeStackWhenEnteringAndExiting() {
        // GIVEN: a fresh CollectorContext
        // Capture the initial (root) scope
        Scope rootScope = collectorContext.getDynamicScope();
        assertNotNull(rootScope, "Root scope must be present after construction");
        // WHEN: enter first dynamic scope
        Scope firstScope = collectorContext.enterDynamicScope();
        assertNotNull(firstScope, "First entered scope must be non‑null");
        assertSame(firstScope, collectorContext.getDynamicScope(), "First entered scope should be current");
        // AND: enter second dynamic scope
        Scope secondScope = collectorContext.enterDynamicScope();
        assertNotNull(secondScope, "Second entered scope must be non‑null");
        assertSame(secondScope, collectorContext.getDynamicScope(), "Second entered scope should be current");
        // THEN: exiting should unwind the stack
        Scope afterFirstExit = collectorContext.exitDynamicScope();
        assertSame(firstScope, afterFirstExit, "exitDynamicScope() should return the scope that was just popped");
        assertSame(firstScope, collectorContext.getDynamicScope(), "After one exit, the current scope should be the first entered scope");
        // AND: exiting again should return to the root scope
        Scope afterSecondExit = collectorContext.exitDynamicScope();
        assertSame(rootScope, afterSecondExit, "Second exit should return the original root scope");
        assertSame(rootScope, collectorContext.getDynamicScope(), "After all exits, the current scope should be the original root scope");
    }

    /**
     * State Independence – the method must work whether or not {@code loadCollectors()}
     * has been called beforehand.
     */
    @Test
    void shouldCreateScopeEvenIfCollectorsNotLoaded() {
        // The CollectorContext constructor does not invoke loadCollectors().
        // We simply call enterDynamicScope() – the expectation is that it does not
        // depend on the collector map being populated.
        Scope scope = collectorContext.enterDynamicScope();
        assertNotNull(scope, "Scope should be created even when collectors are not loaded");
    }

    /**
     * Explicit null parameter – calling the overload with {@code null}
     * must behave like the no‑arg version.
     */
    @Test
    void shouldEnterDynamicScopeWithExplicitNull() {
        Scope scope = collectorContext.enterDynamicScope((JsonSchema) null);
        assertNotNull(scope, "Scope must be non‑null when entering with a null JsonSchema");
        assertSame(scope, collectorContext.getDynamicScope(), "Returned scope must become the current dynamic scope");
    }

    /**
     * Scope With Containing Schema – the created {@link Scope} should retain a
     * reference to the supplied {@link JsonSchema}.  The test accesses the
     * (presumed) private field {@code containingSchema} via reflection.
     */
    @Test
    void shouldRecordContainingSchemaInScope(@Mock JsonSchema mockSchema) throws Exception {
        Scope scope = collectorContext.enterDynamicScope(mockSchema);
        assertNotNull(scope, "Scope must be created");
        // Try to read a private field named 'containingSchema' inside Scope.
        Field schemaField = null;
        for (Field f : scope.getClass().getDeclaredFields()) {
            if (f.getType().equals(JsonSchema.class) || f.getName().toLowerCase().contains("schema")) {
                schemaField = f;
                break;
            }
        }
        assertNotNull(schemaField, "Scope should contain a field referencing the JsonSchema");
        schemaField.setAccessible(true);
        Object stored = schemaField.get(scope);
        assertSame(mockSchema, stored, "The Scope must store the supplied JsonSchema");
    }

    /**
     * Nested Dynamic Scopes – verify that the second scope has the first one as its parent.
     * The parent relationship is accessed via a presumed {@code getParent()} method
     * or a private field named {@code parent}.
     */
    @Test
    void shouldLinkNestedScopes() throws Exception {
        Scope first = collectorContext.enterDynamicScope();
        Scope second = collectorContext.enterDynamicScope();
        // Try a public getter first
        Scope parent = null;
        try {
            parent = (Scope) second.getClass().getMethod("getParent").invoke(second);
        } catch (NoSuchMethodException ignored) {
            // fall back to reflection on a private field called 'parent'
            for (Field f : second.getClass().getDeclaredFields()) {
                if (f.getType().equals(Scope.class) && f.getName().toLowerCase().contains("parent")) {
                    f.setAccessible(true);
                    parent = (Scope) f.get(second);
                    break;
                }
            }
        }
        assertNotNull(parent, "Second scope should have a reference to its parent");
        assertSame(first, parent, "Parent of the second scope must be the first entered scope");
    }

    /**
     * Exit Returns Previous Scope – after two enters, a single exit must return the first scope.
     */
    @Test
    void shouldReturnPreviousScopeOnExit() {
        Scope first = collectorContext.enterDynamicScope();
        Scope second = collectorContext.enterDynamicScope();
        Scope returned = collectorContext.exitDynamicScope();
        assertSame(first, returned, "exitDynamicScope() should return the first entered scope");
        assertSame(first, collectorContext.getDynamicScope(), "Current dynamic scope after exit must be the first scope");
    }

    /**
     * Exit Without Prior Enter – invoking {@code exitDynamicScope()} on a fresh context
     * must throw an {@link IllegalStateException}.
     */
    @Test
    void shouldThrowWhenExitingWithoutEnter() {
        // The constructor already created a root scope; according to the spec,
        // exiting *beyond* the root should be illegal.
        // exit the root, now we are at “no scope”
        collectorContext.exitDynamicScope();
        assertThrows(IllegalStateException.class, () -> collectorContext.exitDynamicScope(), "Exiting when no dynamic scope is present should throw IllegalStateException");
    }

    /**
     * Multiple Exits Return To Null – after exiting all entered scopes the
     * current dynamic scope should become {@code null}.
     */
    @Test
    void shouldBecomeNullAfterAllExits() {
        // Enter one extra scope on top of the root.
        collectorContext.enterDynamicScope();
        // First exit returns us to the root scope.
        collectorContext.exitDynamicScope();
        // Second exit removes the root scope; after this the context has no scope.
        collectorContext.exitDynamicScope();
        assertNull(collectorContext.getDynamicScope(), "After exiting all scopes the current dynamic scope should be null");
    }

    /**
     * Enter With Already Associated Schema – simulate a schema already bound to
     * another {@link CollectorContext}.  We inject a private field {@code collectorContext}
     * into the mock schema via reflection and expect {@link IllegalArgumentException}.
     */
    @Test
    void shouldRejectSchemaAlreadyAssociatedWithOtherContext(@Mock JsonSchema mockSchema) throws Exception {
        // Simulate an association by setting a private field named 'collectorContext' on the schema.
        // The exact field name is unknown; we search for a field of type CollectorContext.
        boolean fieldSet = false;
        for (Field f : mockSchema.getClass().getDeclaredFields()) {
            if (f.getType().equals(CollectorContext.class)) {
                f.setAccessible(true);
                // associate with a *different* context
                f.set(mockSchema, new CollectorContext());
                fieldSet = true;
                break;
            }
        }
        // If we could not find such a field, we simply skip the association – the method
        // under test may still throw IllegalArgumentException for other reasons.
        if (!fieldSet) {
            // nothing to do – the test will still verify that an IllegalArgumentException is thrown.
        }
        assertThrows(IllegalArgumentException.class, () -> collectorContext.enterDynamicScope(mockSchema), "A JsonSchema already linked to another CollectorContext should cause IllegalArgumentException");
    }

    /**
     * Enter When Internal Collector Map Is Null – force the private collector map
     * to {@code null} and verify that a {@link NullPointerException} is raised.
     */
    @Test
    void shouldThrowWhenCollectorMapIsNull() throws Exception {
        // Find the first field of type Map (the internal collector map) and null it.
        boolean nullified = false;
        for (Field f : collectorContext.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                f.set(collectorContext, null);
                nullified = true;
                break;
            }
        }
        // If the field cannot be found the test is inconclusive – guard with an assumption.
        Assumptions.assumeTrue(nullified, "Collector map field not found – test skipped");
        assertThrows(NullPointerException.class, () -> collectorContext.enterDynamicScope(), "When the internal collector map is null, enterDynamicScope() should throw NPE");
    }

    /**
     * Enter After Context Has Been Marked As Finished – simulate a finished context.
     * The test searches for a boolean field that likely represents the finished state.
     */
    @Test
    void shouldRejectEnterWhenContextIsFinished() throws Exception {
        // Locate a boolean field that plausibly indicates a finished / closed state.
        boolean finishedFlagSet = false;
        for (Field f : collectorContext.getClass().getDeclaredFields()) {
            if (f.getType().equals(boolean.class) && f.getName().toLowerCase().contains("finished")) {
                f.setAccessible(true);
                f.setBoolean(collectorContext, true);
                finishedFlagSet = true;
                break;
            }
        }
        // If we cannot locate such a flag, skip the test.
        Assumptions.assumeTrue(finishedFlagSet, "No boolean 'finished' flag found – test skipped");
        assertThrows(IllegalStateException.class, () -> collectorContext.enterDynamicScope(), "Entering a scope after the context is marked finished should throw IllegalStateException");
    }
}
