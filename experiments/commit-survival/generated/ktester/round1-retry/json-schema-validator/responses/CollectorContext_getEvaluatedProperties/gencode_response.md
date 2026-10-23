```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_getEvaluatedProperties_Test {

    private CollectorContext collectorContext;

    /**
     * Creates a fresh {@link CollectorContext} before each test.
     * The default constructor enables evaluation of both items and properties.
     */
    @BeforeEach
    void setupBeforeEach() {
        collectorContext = new CollectorContext(); // disableUnevaluatedItems = false, disableUnevaluatedProperties = false
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up for this class.
        collectorContext = null;
    }

    /* -------------------------------------------------------------
     *  Existing tests from the original scaffold (kept untouched)
     * ------------------------------------------------------------- */

    /**
     * Verify that a newly created {@link CollectorContext} returns an empty,
     * mutable collection for evaluated properties.
     */
    @Test
    void getEvaluatedProperties_defaultScope_isMutableAndInitiallyEmpty() {
        Collection<String> evaluated = collectorContext.getEvaluatedProperties();

        // Initially empty
        assertTrue(evaluated.isEmpty(), "Evaluated properties should be empty on a fresh context");

        // Adding an element should succeed
        boolean added = evaluated.add("propA");
        assertTrue(added, "Collection should allow adding when unevaluated properties are enabled");
        assertEquals(1, evaluated.size());
        assertTrue(evaluated.contains("propA"));
    }

    /**
     * When unevaluated‑properties tracking is disabled, the collection returned
     * by {@code getEvaluatedProperties()} should be a no‑op collection: add()
     * returns false and size remains zero.
     */
    @Test
    void getEvaluatedProperties_whenDisabled_isNoOpCollection() {
        // Create a context with unevaluated‑properties disabled
        CollectorContext disabledContext = new CollectorContext(false, true);

        Collection<String> evaluated = disabledContext.getEvaluatedProperties();

        // The collection must never be null
        assertNotNull(evaluated, "Evaluated properties collection must never be null");

        // Adding should return false and not change the size
        boolean added = evaluated.add("propB");
        assertFalse(added, "Add should return false when collection is disabled");
        assertEquals(0, evaluated.size(), "Size must stay zero for disabled collection");
    }

    /**
     * {@code getEvaluatedProperties()} should always delegate to the current
     * dynamic scope. Adding a property via the scope must be visible through the
     * context and vice‑versa.
     */
    @Test
    void getEvaluatedProperties_delegatesToCurrentDynamicScope() {
        // Enter a new dynamic scope (top = false)
        CollectorContext.Scope innerScope = collectorContext.enterDynamicScope();

        // Add a property via the scope
        innerScope.getEvaluatedProperties().add("innerProp");

        // The context should expose the same collection
        Collection<String> fromContext = collectorContext.getEvaluatedProperties();
        assertTrue(fromContext.contains("innerProp"),
                "Property added via scope must be visible through CollectorContext");

        // Add another property via the context
        fromContext.add("anotherProp");
        assertTrue(innerScope.getEvaluatedProperties().contains("anotherProp"),
                "Property added via CollectorContext must be reflected in the current scope");
    }

    /**
     * After exiting a dynamic scope, {@code getEvaluatedProperties()} must refer
     * to the outer scope's collection again.
     */
    @Test
    void getEvaluatedProperties_afterExitingScope_returnsOuterScopeCollection() {
        // Outer scope collection reference
        Collection<String> outer = collectorContext.getEvaluatedProperties();

        // Enter inner scope and add a property there
        CollectorContext.Scope inner = collectorContext.enterDynamicScope();
        inner.getEvaluatedProperties().add("innerOnly");

        // Ensure inner collection is distinct from outer
        assertNotSame(outer, collectorContext.getEvaluatedProperties(),
                "While inside inner scope, the collection should differ from the outer one");

        // Exit inner scope
        collectorContext.exitDynamicScope();

        // Now the context must return the original outer collection
        Collection<String> afterExit = collectorContext.getEvaluatedProperties();
        assertSame(outer, afterExit, "After exiting, the context should return the original outer collection");
        assertFalse(afterExit.contains("innerOnly"),
                "Properties added in the inner scope should not leak to the outer scope");
    }

    /* -------------------------------------------------------------
     *  Additional tests derived from the JSON test specifications
     * ------------------------------------------------------------- */

    /**
     * Multiple calls to {@code getEvaluatedProperties()} inside the same dynamic
     * scope must return the identical collection instance (identity equality).
     */
    @Test
    void getEvaluatedProperties_multipleCallsReturnSameInstance() {
        collectorContext.enterDynamicScope(); // ensure we are in a dynamic scope
        Collection<String> first = collectorContext.getEvaluatedProperties();
        Collection<String> second = collectorContext.getEvaluatedProperties();
        assertSame(first, second,
                "Repeated calls should return the same underlying collection instance");
    }

    /**
     * When no explicit dynamic scope is entered, the context still provides a
     * mutable collection (the top scope). Adding to it must succeed.
     */
    @Test
    void getEvaluatedProperties_withoutExplicitScope_returnsMutableTopScope() {
        Collection<String> evaluated = collectorContext.getEvaluatedProperties();

        assertNotNull(evaluated, "Collection should never be null");
        assertTrue(evaluated.isEmpty(), "Top‑scope collection should start empty");

        // Mutability check
        assertTrue(evaluated.add("rootProp"));
        assertEquals(1, evaluated.size());
        assertTrue(evaluated.contains("rootProp"));
    }

    /**
     * Two nested dynamic scopes must each own distinct collections. After exiting
     * the inner scope the original outer collection instance is restored.
     */
    @Test
    void getEvaluatedProperties_nestedScopesDistinctCollections() {
        // First (outer) scope – the default top scope
        Collection<String> outer = collectorContext.getEvaluatedProperties();

        // Enter first dynamic scope
        collectorContext.enterDynamicScope();
        Collection<String> afterFirstEnter = collectorContext.getEvaluatedProperties();
        assertNotSame(outer, afterFirstEnter, "First dynamic scope should have a different collection");

        // Enter second (nested) dynamic scope
        collectorContext.enterDynamicScope();
        Collection<String> inner = collectorContext.getEvaluatedProperties();
        assertNotSame(afterFirstEnter, inner, "Nested scope must have its own collection");

        // Exit back to first scope
        collectorContext.exitDynamicScope();
        Collection<String> backToFirst = collectorContext.getEvaluatedProperties();
        assertSame(afterFirstEnter, backToFirst, "After exiting inner scope we should see the first scope's collection again");

        // Exit to top scope
        collectorContext.exitDynamicScope();
        Collection<String> backToTop = collectorContext.getEvaluatedProperties();
        assertSame(outer, backToTop, "After exiting all scopes we should be back to the original top collection");
    }

    /**
     * Verifies that {@link CollectorContext.Scope#mergeWith(CollectorContext.Scope)} correctly
     * merges evaluated‑property collections from a child scope into its parent.
     */
    @Test
    void mergeWith_childScope_propertiesAreCombined() {
        // Parent scope (top)
        CollectorContext.Scope parent = collectorContext.enterDynamicScope();
        parent.getEvaluatedProperties().add("parentProp");

        // Child scope
        CollectorContext.Scope child = collectorContext.enterDynamicScope();
        child.getEvaluatedProperties().add("childProp");

        // Merge child into parent
        parent.mergeWith(child);

        // After merge, parent collection must contain both entries
        Collection<String> parentProps = parent.getEvaluatedProperties();
        assertTrue(parentProps.contains("parentProp"));
        assertTrue(parentProps.contains("childProp"));
        assertEquals(2, parentProps.size(),
                "Parent collection should contain exactly two distinct properties after merge");
    }

    /**
     * Parameterized test that repeatedly checks identity equality of the collection
     * returned by {@code getEvaluatedProperties()} after entering a dynamic scope.
     * Demonstrates that the behavior is consistent across multiple invocations.
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void getEvaluatedProperties_identityEquality_multipleInvocations(int iteration) {
        collectorContext.enterDynamicScope();
        Collection<String> first = collectorContext.getEvaluatedProperties();
        Collection<String> second = collectorContext.getEvaluatedProperties();
        assertSame(first, second,
                "Iteration " + iteration + ": the same instance must be returned on repeated calls");
        collectorContext.exitDynamicScope();
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these unit tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }
}
```