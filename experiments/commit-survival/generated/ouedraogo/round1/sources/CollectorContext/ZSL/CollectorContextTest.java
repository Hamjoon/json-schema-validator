/*
 * Unit tests for {@link CollectorContext}.
 *
 * These tests are written using JUnit 4 and Mockito and are placed in the same
 * package as the class under test so that package‑private members can be
 * accessed.
 */
package com.networknt.schema;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for {@link CollectorContext}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CollectorContextTest {

    private CollectorContext defaultContext;

    @Mock
    private JsonSchema mockSchemaRoot;

    @Mock
    private JsonSchema mockSchemaChild;

    @Mock
    private Collector<Object> mockCollector;

    @Before
    public void setUp() {
        // default context (no disabling of unevaluated items/properties)
        defaultContext = new CollectorContext();
    }

    /* -------------------------------------------------------------
     *  Constructor / Scope handling
     * ------------------------------------------------------------- */

    @Test
    public void defaultConstructorCreatesTopScope() {
        Scope top = defaultContext.getDynamicScope();
        assertTrue("Top scope should be marked as top", top.isTop());
        assertNull("Top scope should have null containing schema", top.getContainingSchema());

        // evaluated collections are enabled (add should succeed)
        assertTrue(top.getEvaluatedItems().add("item1"));
        assertTrue(top.getEvaluatedProperties().add("prop1"));
        assertEquals(1, top.getEvaluatedItems().size());
        assertEquals(1, top.getEvaluatedProperties().size());
    }

    @Test
    public void constructorWithDisableFlagsCreatesTopScopeWithDisabledCollections() {
        CollectorContext ctx = new CollectorContext(true, true);
        Scope top = ctx.getDynamicScope();

        // collections are disabled – add returns false and size stays zero
        assertFalse(top.getEvaluatedItems().add("x"));
        assertFalse(top.getEvaluatedProperties().add("y"));
        assertEquals(0, top.getEvaluatedItems().size());
        assertEquals(0, top.getEvaluatedProperties().size());
    }

    @Test
    public void enterAndExitDynamicScopeMaintainStackCorrectly() {
        // parent (top) scope
        Scope parent = defaultContext.getDynamicScope();

        // child scope with a mock schema
        Scope previous = defaultContext.enterDynamicScope(mockSchemaChild);
        assertSame("enterDynamicScope should return the previous (parent) scope", parent, previous);

        Scope current = defaultContext.getDynamicScope();
        assertNotSame("Current scope should be a new instance", parent, current);
        assertEquals("Containing schema of new scope should be the one supplied",
                mockSchemaChild, current.getContainingSchema());

        // exit back to parent
        Scope exited = defaultContext.exitDynamicScope();
        assertSame("exitDynamicScope should return the scope that is being popped", current, exited);
        assertSame("After exit, the active scope should be the original parent", parent,
                defaultContext.getDynamicScope());
    }

    @Test(expected = IllegalStateException.class)
    public void getOutermostSchemaThrowsWhenRootSchemaMissing() {
        // The default context has a top scope with null containingSchema.
        defaultContext.getOutermostSchema();
    }

    @Test
    public void getOutermostSchemaResolvesDynamicAnchorCorrectly() {
        // Setup the root schema to be a dynamic anchor and its lexical root.
        when(mockSchemaRoot.isDynamicAnchor()).thenReturn(true);
        when(mockSchemaRoot.findLexicalRoot()).thenReturn(mockSchemaRoot);
        // Child schema is also a dynamic anchor (simulating a nested $anchor)
        when(mockSchemaChild.isDynamicAnchor()).thenReturn(true);
        when(mockSchemaChild.findLexicalRoot()).thenReturn(mockSchemaRoot);

        // Create a context where the top scope has null schema, then push a child scope.
        CollectorContext ctx = new CollectorContext();
        ctx.enterDynamicScope(mockSchemaRoot); // first dynamic scope (root)
        ctx.enterDynamicScope(mockSchemaChild); // second dynamic scope (nested)

        // The method should walk the stack and return the nearest dynamic anchor,
        // which is mockSchemaChild in this case.
        JsonSchema result = ctx.getOutermostSchema();
        assertSame("Should return the nearest containing schema that is a dynamic anchor",
                mockSchemaChild, result);
    }

    /* -------------------------------------------------------------
     *  Evaluated items / properties collections
     * ------------------------------------------------------------- */

    @Test
    public void evaluatedCollectionsAreLiveAndNeverNull() {
        Collection<String> items = defaultContext.getEvaluatedItems();
        Collection<String> props = defaultContext.getEvaluatedProperties();

        assertNotNull(items);
        assertNotNull(props);

        items.add("0");
        props.add("name");

        assertTrue(items.contains("0"));
        assertTrue(props.contains("name"));
    }

    @Test
    public void disabledEvaluatedCollectionsRejectAdds() {
        CollectorContext ctx = new CollectorContext(true, true);
        Collection<String> items = ctx.getEvaluatedItems();
        Collection<String> props = ctx.getEvaluatedProperties();

        assertFalse("Add should be rejected when items are disabled", items.add("any"));
        assertFalse("Add should be rejected when properties are disabled", props.add("any"));
        assertEquals(0, items.size());
        assertEquals(0, props.size());
    }

    /* -------------------------------------------------------------
     *  Scope merging and toString()
     * ------------------------------------------------------------- */

    @Test
    public void scopeMergeCombinesEvaluatedSets() {
        Scope s1 = new CollectorContext().newTopScope();
        Scope s2 = new CollectorContext().newTopScope();

        s1.getEvaluatedItems().add("i1");
        s1.getEvaluatedProperties().add("p1");

        s2.getEvaluatedItems().add("i2");
        s2.getEvaluatedProperties().add("p2");

        s1.mergeWith(s2);

        assertTrue(s1.getEvaluatedItems().containsAll(Arrays.asList("i1", "i2")));
        assertTrue(s1.getEvaluatedProperties().containsAll(Arrays.asList("p1", "p2")));
    }

    @Test
    public void scopeToStringContainsBothCollections() {
        Scope scope = new CollectorContext().newTopScope();
        scope.getEvaluatedItems().add("item");
        scope.getEvaluatedProperties().add("prop");

        String str = scope.toString();
        assertTrue(str.contains("item"));
        assertTrue(str.contains("prop"));
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));
    }

    /* -------------------------------------------------------------
     *  Collector handling (add / get / combine / load)
     * ------------------------------------------------------------- */

    @Test
    public void addCollectorAndGetReturnsCollectorBeforeLoading() {
        CollectorContext ctx = new CollectorContext();
        ctx.add("myCollector", mockCollector);

        Object retrieved = ctx.get("myCollector");
        assertSame("Before loading, get should return the collector instance itself",
                mockCollector, retrieved);
    }

    @Test
    public void combineWithCollectorInvokesCollectorCombine() {
        CollectorContext ctx = new CollectorContext();
        ctx.add("c1", mockCollector);

        Object data = "some data";
        ctx.combineWithCollector("c1", data);

        verify(mockCollector, times(1)).combine(data);
    }

    @Test
    public void loadCollectorsPopulatesLoadMapAndGetReturnsCollectedData() {
        // Prepare collector to return a specific value when collect() is called.
        when(mockCollector.collect()).thenReturn("collectedResult");

        CollectorContext ctx = new CollectorContext();
        ctx.add("cKey", mockCollector);
        ctx.add("plainKey", "plainValue"); // non‑collector entry

        // Load all collectors – package‑private method is accessible because test
        // resides in the same package.
        ctx.loadCollectors();

        // After loading, the collector entry should resolve to the collected result.
        assertEquals("collectedResult", ctx.get("cKey"));
        // Non‑collector entry should stay unchanged.
        assertEquals("plainValue", ctx.get("plainKey"));
    }

    @Test
    public void getAllMergesCollectorMapAndLoadMap() {
        when(mockCollector.collect()).thenReturn(42);
        CollectorContext ctx = new CollectorContext();
        ctx.add("c", mockCollector);
        ctx.add("obj", "value");

        // Before loading, getAll should contain only the original map entries.
        Map<String, Object> before = ctx.getAll();
        assertTrue(before.containsKey("c"));
        assertTrue(before.containsKey("obj"));
        assertEquals(mockCollector, before.get("c"));

        // Load collectors.
        ctx.loadCollectors();

        // After loading, getAll should contain the collected data under the same key.
        Map<String, Object> after = ctx.getAll();
        assertEquals(42, after.get("c"));
        assertEquals("value", after.get("obj"));
    }

    /* -------------------------------------------------------------
     *  Edge cases for getOutermostSchema with non‑dynamic lexical root
     * ------------------------------------------------------------- */

    @Test
    public void getOutermostSchemaReturnsLexicalRootWhenNoDynamicAnchorFound() {
        // Root schema is NOT a dynamic anchor.
        when(mockSchemaRoot.isDynamicAnchor()).thenReturn(false);
        when(mockSchemaRoot.findLexicalRoot()).thenReturn(mockSchemaRoot);

        // Child schema also not a dynamic anchor.
        when(mockSchemaChild.isDynamicAnchor()).thenReturn(false);
        when(mockSchemaChild.findLexicalRoot()).thenReturn(mockSchemaRoot);

        CollectorContext ctx = new CollectorContext();
        ctx.enterDynamicScope(mockSchemaRoot);
        ctx.enterDynamicScope(mockSchemaChild);

        JsonSchema result = ctx.getOutermostSchema();
        assertSame("Should return the lexical root when no dynamic anchor is present",
                mockSchemaRoot, result);
    }
}
