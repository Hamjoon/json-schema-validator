package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

class CollectorContext_getEvaluatedItems_Test {

    private CollectorContext collectorContext;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh context for each test (default: tracking enabled).
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        collectorContext = null;
    }

    @Test
    void getEvaluatedItems_DefaultContext_ReturnsMutableCollection() {
        Collection<String> items = collectorContext.getEvaluatedItems();
        // The collection should be initially empty.
        assertTrue(items.isEmpty(), "Evaluated items collection should start empty");
        // Adding an element must succeed.
        assertTrue(items.add("item-1"), "Adding to the default collection should succeed");
        assertEquals(1, items.size(), "Collection size should reflect the added element");
        assertTrue(items.contains("item-1"), "Collection must contain the added element");
    }

    @Test
    void getEvaluatedItems_DisableUnevaluatedItems_ReturnsNoOpCollection() {
        // Create a context where unevaluated‑items tracking is disabled.
        CollectorContext disabledContext = new CollectorContext(true, false);
        Collection<String> items = disabledContext.getEvaluatedItems();
        // The collection should behave as a no‑op collection.
        assertTrue(items.isEmpty(), "No‑op collection should appear empty");
        assertFalse(items.add("should-not-be-added"), "Add operation must return false");
        assertEquals(0, items.size(), "Size must remain zero after a failed add");
    }

    @Test
    void getEvaluatedItems_SameInstanceReturnedOnRepeatedCalls() {
        Collection<String> firstCall = collectorContext.getEvaluatedItems();
        Collection<String> secondCall = collectorContext.getEvaluatedItems();
        // Both calls must return the exact same object reference.
        assertSame(firstCall, secondCall, "Repeated calls should return the same collection instance");
    }

    @Test
    void getEvaluatedItems_DynamicScopeIsolation() {
        // Top‑level scope – add an element.
        Collection<String> topScopeItems = collectorContext.getEvaluatedItems();
        topScopeItems.add("top-item");
        // Enter a new dynamic scope.
        CollectorContext.Scope innerScope = collectorContext.enterDynamicScope();
        Collection<String> innerScopeItems = collectorContext.getEvaluatedItems();
        // The inner scope should start empty and be independent of the top scope.
        assertTrue(innerScopeItems.isEmpty(), "New dynamic scope must start with an empty collection");
        assertNotSame(topScopeItems, innerScopeItems, "Inner scope collection must be a different instance");
        // Add an element to the inner scope.
        innerScopeItems.add("inner-item");
        assertEquals(1, innerScopeItems.size(), "Inner scope collection should contain its own element");
        // Exit the inner scope and verify the top‑level collection is unchanged.
        collectorContext.exitDynamicScope();
        Collection<String> afterExitItems = collectorContext.getEvaluatedItems();
        assertEquals(1, afterExitItems.size(), "Top‑level collection size should remain unchanged after exiting inner scope");
        assertTrue(afterExitItems.contains("top-item"), "Top‑level collection must still contain its original element");
        assertFalse(afterExitItems.contains("inner-item"), "Top‑level collection must not contain inner‑scope elements");
    }

    @Test
    void getEvaluatedItems_NullSafety_NonNullReturned() {
        assertNotNull(collectorContext.getEvaluatedItems(), "getEvaluatedItems() must never return null");
    }

    @Test
    void getEvaluatedItems_AddNullEnabledCollection() {
        Collection<String> items = collectorContext.getEvaluatedItems();
        assertTrue(items.add(null), "Adding null to an enabled collection should succeed");
        assertEquals(1, items.size(), "Size must be 1 after adding null");
        assertTrue(items.contains(null), "Collection should contain the null element");
    }

    @Test
    void getEvaluatedItems_AddNullDisabledCollection() {
        CollectorContext disabledContext = new CollectorContext(true, false);
        Collection<String> items = disabledContext.getEvaluatedItems();
        assertFalse(items.add(null), "Adding null to a disabled collection must return false");
        assertEquals(0, items.size(), "Size must stay 0 after a failed add");
        assertFalse(items.contains(null), "Collection must not contain null");
    }

    @Test
    void getEvaluatedItems_MergeScopes_ReflectsAllEntries() {
        // Two independent scopes with tracking enabled.
        CollectorContext.Scope first = new CollectorContext.Scope(false, false, null);
        CollectorContext.Scope second = new CollectorContext.Scope(false, false, null);
        first.getEvaluatedItems().addAll(Arrays.asList("a", "b"));
        second.getEvaluatedItems().add("c");
        // Merge second into first.
        first.mergeWith(second);
        // Context that returns the merged scope.
        CollectorContext context = new CollectorContext() {

            @Override
            public Scope getDynamicScope() {
                return first;
            }
        };
        Collection<String> merged = context.getEvaluatedItems();
        assertEquals(3, merged.size(), "Merged collection must contain three elements");
        assertTrue(merged.containsAll(Arrays.asList("a", "b", "c")), "Merged collection must contain a, b and c");
    }

    @Test
    void getEvaluatedItems_NullDynamicScope_ThrowsNPE() {
        CollectorContext ctx = new CollectorContext() {

            @Override
            public Scope getDynamicScope() {
                // simulate a broken internal state
                return null;
            }
        };
        assertThrows(NullPointerException.class, ctx::getEvaluatedItems, "Calling getEvaluatedItems() when dynamic scope is null must throw NPE");
    }

    @Test
    void getEvaluatedItems_UnmodifiableCollection_AddThrowsUnsupportedOperationException() throws Exception {
        // Create a normal scope first.
        CollectorContext.Scope mutableScope = new CollectorContext.Scope(false, false, null);
        // Replace its evaluatedItems with an unmodifiable collection via reflection.
        Collection<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        Field field = CollectorContext.Scope.class.getDeclaredField("evaluatedItems");
        field.setAccessible(true);
        field.set(mutableScope, unmodifiable);
        // Context that returns this specially‑crafted scope.
        CollectorContext ctx = new CollectorContext() {

            @Override
            public Scope getDynamicScope() {
                return mutableScope;
            }
        };
        Collection<String> items = ctx.getEvaluatedItems();
        assertThrows(UnsupportedOperationException.class, () -> items.add("x"), "Adding to an unmodifiable evaluated‑items collection must throw UnsupportedOperationException");
    }

    @Test
    void getEvaluatedItems_ExitBeyondRoot_ThrowsNPE() {
        // Start with a fresh context, then exit more times than we entered.
        // first exit – may already be at root
        collectorContext.exitDynamicScope();
        // second exit – pushes internal reference to null
        collectorContext.exitDynamicScope();
        assertThrows(NullPointerException.class, collectorContext::getEvaluatedItems, "After exiting beyond the root scope, getEvaluatedItems() should throw NPE");
    }

    @Test
    void getEvaluatedItems_EnteringNewDynamicScopeCreatesDistinctCollection() {
        Collection<String> outer = collectorContext.getEvaluatedItems();
        assertNotNull(outer, "Outer collection must not be null");
        assertEquals(0, outer.size(), "Outer collection should start empty");
        CollectorContext.Scope innerScope = collectorContext.enterDynamicScope();
        Collection<String> inner = collectorContext.getEvaluatedItems();
        assertNotNull(inner, "Inner collection must not be null");
        assertEquals(0, inner.size(), "Inner collection should start empty");
        assertNotSame(outer, inner, "Inner and outer collections must be different instances");
    }

    @Test
    void getEvaluatedItems_ExitingDynamicScopeRestoresOriginalCollection() {
        Collection<String> outer = collectorContext.getEvaluatedItems();
        CollectorContext.Scope innerScope = collectorContext.enterDynamicScope();
        Collection<String> inner = collectorContext.getEvaluatedItems();
        inner.add("x");
        assertTrue(inner.contains("x"), "Inner collection must contain the added element");
        collectorContext.exitDynamicScope();
        Collection<String> afterExit = collectorContext.getEvaluatedItems();
        assertSame(outer, afterExit, "After exiting, the returned collection must be the original outer instance");
        assertFalse(afterExit.contains("x"), "Outer collection must not contain elements added in the inner scope");
    }
}
