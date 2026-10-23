/*****************************************************************************************
 * JUnit 4 test suite for {@link com.networknt.schema.CollectorContext}
 *
 * The tests cover:
 *   • Construction and default top‑scope behaviour
 *   • Dynamic scope management (enter / exit)
 *   • Evaluated‑items / evaluated‑properties collections, including the
 *     “disable” flags that produce no‑op collections
 *   • Adding and retrieving plain objects and {@link Collector} instances
 *   • Loading collectors, combining data and the merged “getAll” view
 *   • Scope merging, toString() format and top‑flag handling
 *   • getOutermostSchema() logic with dynamic anchors (using Mockito)
 *
 * The test class is self‑contained and uses a simple stub implementation of
 * {@link Collector} and Mockito for {@link JsonSchema}.
 *
 * Author:  Automated Test Generator
 * Date:    2026‑10‑23
 *****************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test suite for {@link CollectorContext}.
 */
public class CollectorContextTest {

    // -------------------------------------------------------------------------
    // Helper classes / stubs
    // -------------------------------------------------------------------------

    /** Simple {@link Collector} stub that records combine() calls and returns a fixed value. */
    private static class SimpleCollector implements Collector<Object> {
        private Object combinedData;
        private final Object collectResult;

        SimpleCollector(Object collectResult) {
            this.collectResult = collectResult;
        }

        @Override
        public void combine(Object data) {
            this.combinedData = data;
        }

        @Override
        public Object collect() {
            return this.collectResult;
        }

        Object getCombinedData() {
            return this.combinedData;
        }
    }

    // -------------------------------------------------------------------------
    // Mocks for JsonSchema used in getOutermostSchema()
    // -------------------------------------------------------------------------

    @Mock
    private JsonSchema mockRootSchema;                     // the lexical root
    @Mock
    private JsonSchema mockDynamicAnchorSchema;           // a schema that is a dynamic anchor
    @Mock
    private JsonSchema mockChildSchema;                   // a child schema referencing the root

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private CollectorContext defaultContext;               // default flags (both false)
    private CollectorContext disabledItemsContext;        // disableUnevaluatedItems = true
    private CollectorContext disabledPropsContext;        // disableUnevaluatedProperties = true

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // default context – no disabling
        defaultContext = new CollectorContext();

        // contexts with disabled collections
        disabledItemsContext = new CollectorContext(true, false);
        disabledPropsContext = new CollectorContext(false, true);

        // -----------------------------------------------------------------
        // Configure JsonSchema mocks for getOutermostSchema() tests
        // -----------------------------------------------------------------
        // root schema is the lexical root of everything
        when(mockRootSchema.findLexicalRoot()).thenReturn(mockRootSchema);
        when(mockRootSchema.isDynamicAnchor()).thenReturn(false);

        // child schema delegates to the root for lexical root
        when(mockChildSchema.findLexicalRoot()).thenReturn(mockRootSchema);
        when(mockChildSchema.getContainingSchema()).thenReturn(mockRootSchema);
        when(mockChildSchema.isDynamicAnchor()).thenReturn(false);

        // dynamic anchor schema is itself the lexical root and reports true
        when(mockDynamicAnchorSchema.findLexicalRoot()).thenReturn(mockDynamicAnchorSchema);
        when(mockDynamicAnchorSchema.isDynamicAnchor()).thenReturn(true);
    }

    // -------------------------------------------------------------------------
    // 1. Construction & top‑scope basics
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesTopScope() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope top = ctx.getDynamicScope();
        assertTrue("Top scope must be marked as top", top.isTop());
        assertNull("Top scope should have no containing schema", top.getContainingSchema());
    }

    @Test
    public void testCustomConstructorSetsDisableFlags() {
        CollectorContext ctx = new CollectorContext(true, true);
        // The top scope is created with `top = true` and the *disable* flags passed
        // to the underlying Scope constructor.
        assertTrue(ctx.getDynamicScope().isTop());
        // Verify that collections are the no‑op versions by checking add() result.
        assertFalse("add on disabled items collection must return false",
                ctx.getEvaluatedItems().add("any"));
        assertFalse("add on disabled properties collection must return false",
                ctx.getEvaluatedProperties().add("any"));
    }

    // -------------------------------------------------------------------------
    // 2. Dynamic scope management (enter / exit)
    // -------------------------------------------------------------------------

    @Test
    public void testEnterAndExitDynamicScope() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope original = ctx.getDynamicScope();

        CollectorContext.Scope parent = ctx.enterDynamicScope();
        assertSame("enterDynamicScope should return previous (parent) scope", original, parent);
        assertNotSame("New dynamic scope must be a different instance", original, ctx.getDynamicScope());

        CollectorContext.Scope exited = ctx.exitDynamicScope();
        assertSame("exitDynamicScope should return the scope that was just popped", exited, ctx.getDynamicScope());
        assertSame("After exiting, the dynamic scope must be the original one", original, ctx.getDynamicScope());
    }

    @Test
    public void testEnterDynamicScopeWithContainingSchema() {
        CollectorContext ctx = new CollectorContext();
        // Simulate a schema object – we use the mockChildSchema
        CollectorContext.Scope parent = ctx.getDynamicScope();
        CollectorContext.Scope newScope = ctx.enterDynamicScope(mockChildSchema);
        assertSame("Containing schema of the new scope must be the one supplied",
                mockChildSchema, newScope.getContainingSchema());

        // If we pass null, the containing schema should be inherited from parent
        ctx.exitDynamicScope(); // back to parent
        CollectorContext.Scope inheritedScope = ctx.enterDynamicScope(null);
        assertSame("When null is supplied, containing schema is inherited from parent",
                parent.getContainingSchema(), inheritedScope.getContainingSchema());
    }

    // -------------------------------------------------------------------------
    // 3. Evaluated items / properties collections (including disabled variants)
    // -------------------------------------------------------------------------

    @Test
    public void testEvaluatedCollectionsAddAndMerge() {
        CollectorContext.Scope s1 = defaultContext.getDynamicScope();
        s1.getEvaluatedItems().add("0");
        s1.getEvaluatedProperties().add("propA");

        CollectorContext.Scope s2 = new CollectorContext.Scope(false, false, false, null);
        s2.getEvaluatedItems().add("1");
        s2.getEvaluatedProperties().add("propB");

        // merge s2 into s1
        s1.mergeWith(s2);
        assertTrue(s1.getEvaluatedItems().contains("0"));
        assertTrue(s1.getEvaluatedItems().contains("1"));
        assertTrue(s1.getEvaluatedProperties().contains("propA"));
        assertTrue(s1.getEvaluatedProperties().contains("propB"));
    }

    @Test
    public void testDisabledEvaluatedItemsCollectionIsNoOp() {
        CollectorContext.Scope disabledScope = disabledItemsContext.getDynamicScope();
        assertFalse("add should be ignored when items are disabled",
                disabledScope.getEvaluatedItems().add("any"));
        assertEquals("size must stay 0 for disabled items collection", 0,
                disabledScope.getEvaluatedItems().size());
    }

    @Test
    public void testDisabledEvaluatedPropertiesCollectionIsNoOp() {
        CollectorContext.Scope disabledScope = disabledPropsContext.getDynamicScope();
        assertFalse("add should be ignored when properties are disabled",
                disabledScope.getEvaluatedProperties().add("any"));
        assertEquals("size must stay 0 for disabled properties collection", 0,
                disabledScope.getEvaluatedProperties().size());
    }

    // -------------------------------------------------------------------------
    // 4. Collector handling (add / get / load / combine)
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndGetPlainObject() {
        CollectorContext ctx = new CollectorContext();
        ctx.add("plainKey", "plainValue");
        assertEquals("plainValue", ctx.get("plainKey"));
    }

    @Test
    public void testAddCollectorAndGetBeforeLoading() {
        CollectorContext ctx = new CollectorContext();
        SimpleCollector collector = new SimpleCollector("collectedData");
        ctx.add("myCollector", collector);

        // Before loadCollectors, get() must return the collector instance itself
        Object result = ctx.get("myCollector");
        assertSame("Should return the Collector instance before loading", collector, result);
    }

    @Test
    public void testLoadCollectorsReplacesWithCollectedData() {
        CollectorContext ctx = new CollectorContext();
        SimpleCollector collector = new SimpleCollector("finalData");
        ctx.add("c1", collector);
        ctx.loadCollectors(); // triggers collect()

        // After loading, get() must return the collect() result, not the collector object
        Object result = ctx.get("c1");
        assertEquals("finalData", result);
        // The internal map should now contain the loaded value
        assertTrue("collectorLoadMap must contain the key after loading",
                ctx.getAll().containsKey("c1"));
        assertEquals("finalData", ctx.getAll().get("c1"));
    }

    @Test
    public void testCombineWithCollectorInvokesCombine() {
        CollectorContext ctx = new CollectorContext();
        SimpleCollector collector = new SimpleCollector("ignored");
        ctx.add("combiner", collector);

        ctx.combineWithCollector("combiner", "combinedPayload");
        assertEquals("combinedPayload", collector.getCombinedData());
    }

    @Test
    public void testCombineWithCollectorWhenNoCollectorIsPresentDoesNothing() {
        CollectorContext ctx = new CollectorContext();
        ctx.add("plain", "value");
        // No exception should be thrown – method silently does nothing
        ctx.combineWithCollector("plain", "anything");
        // Verify that the plain entry is still unchanged
        assertEquals("value", ctx.get("plain"));
    }

    @Test
    public void testGetAllMergesBothMaps() {
        CollectorContext ctx = new CollectorContext();
        ctx.add("obj1", "value1");
        SimpleCollector collector = new SimpleCollector("collected");
        ctx.add("c2", collector);
        ctx.loadCollectors(); // now collectorLoadMap contains "c2"

        Map<String, Object> all = ctx.getAll();
        assertEquals("value1", all.get("obj1"));
        assertEquals("collected", all.get("c2"));
        // Ensure no extra keys are present
        assertEquals(2, all.size());
    }

    // -------------------------------------------------------------------------
    // 5. Scope toString() format
    // -------------------------------------------------------------------------

    @Test
    public void testScopeToStringContainsCollections() {
        CollectorContext.Scope scope = new CollectorContext.Scope(false, false, false, null);
        scope.getEvaluatedItems().add("item1");
        scope.getEvaluatedProperties().add("prop1");
        String txt = scope.toString();

        assertTrue(txt.contains("\"evaluatedItems\":"));
        assertTrue(txt.contains("\"evaluatedProperties\":"));
        assertTrue(txt.contains("item1"));
        assertTrue(txt.contains("prop1"));
        assertTrue(txt.startsWith("{ "));
        assertTrue(txt.endsWith(" }"));
    }

    // -------------------------------------------------------------------------
    // 6. getOutermostSchema() – dynamic anchor handling
    // -------------------------------------------------------------------------

    @Test
    public void testGetOutermostSchemaReturnsRootWhenNoDynamicAnchor() {
        // Arrange: context with a normal (non‑dynamic) schema chain
        CollectorContext ctx = new CollectorContext();
        // push a scope that contains mockChildSchema
        ctx.enterDynamicScope(mockChildSchema);

        JsonSchema result = ctx.getOutermostSchema();

        // The lexical root of mockChildSchema is mockRootSchema, which is not a dynamic anchor
        assertSame("Should return the lexical root (mockRootSchema)", mockRootSchema, result);
    }

    @Test
    public void testGetOutermostSchemaFindsNearestDynamicAnchor() {
        // Arrange a chain: top -> anchorScope (dynamic) -> innerScope (non‑dynamic)
        CollectorContext ctx = new CollectorContext();

        // First, push a scope whose containing schema is the dynamic anchor
        ctx.enterDynamicScope(mockDynamicAnchorSchema); // now top of stack is dynamic anchor scope

        // Then push another scope (inner) with a regular child schema
        ctx.enterDynamicScope(mockChildSchema);

        // Act
        JsonSchema outermost = ctx.getOutermostSchema();

        // The lexical root of mockChildSchema is mockRootSchema (non‑dynamic), but we have
        // a dynamic anchor earlier in the stack, so the method must return that anchor.
        assertSame("Should return the nearest dynamic‑anchor schema", mockDynamicAnchorSchema, outermost);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOutermostSchemaThrowsWhenRootMissing() {
        // Create a context and manually replace the top scope’s containing schema with null
        CollectorContext ctx = new CollectorContext();
        // The top scope created by the constructor has null containing schema.
        // Directly call getOutermostSchema – it should throw.
        ctx.getOutermostSchema();
    }

    // -------------------------------------------------------------------------
    // 7. Miscellaneous edge cases
    // -------------------------------------------------------------------------

    @Test
    public void testEnterDynamicScopeReturnsParentEvenWhenStackHasOneElement() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope parent = ctx.getDynamicScope(); // the only element
        CollectorContext.Scope returned = ctx.enterDynamicScope();
        assertSame("Parent scope should be returned even when there is only the top scope",
                parent, returned);
    }

    @Test
    public void testExitDynamicScopeOnSingleElementStackReturnsTop() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope top = ctx.getDynamicScope();
        CollectorContext.Scope popped = ctx.exitDynamicScope();
        assertSame("Popping the only element returns the top scope", top, popped);
    }

    @Test
    public void testScopeMergeWithEmptyScopeDoesNotChangeCollections() {
        CollectorContext.Scope s1 = defaultContext.getDynamicScope();
        s1.getEvaluatedItems().add("a");
        s1.getEvaluatedProperties().add("propX");

        CollectorContext.Scope empty = new CollectorContext.Scope(false, false, false, null);
        s1.mergeWith(empty);

        assertTrue(s1.getEvaluatedItems().contains("a"));
        assertTrue(s1.getEvaluatedProperties().contains("propX"));
        assertEquals(1, s1.getEvaluatedItems().size());
        assertEquals(1, s1.getEvaluatedProperties().size());
    }

    @Test
    public void testScopeIsTopFlagPropagation() {
        CollectorContext ctx = new CollectorContext();
        assertTrue("Top scope must report true", ctx.getDynamicScope().isTop());

        ctx.enterDynamicScope(); // second scope is not top
        assertFalse("Nested scope must report false for isTop()", ctx.getDynamicScope().isTop());
    }
}
