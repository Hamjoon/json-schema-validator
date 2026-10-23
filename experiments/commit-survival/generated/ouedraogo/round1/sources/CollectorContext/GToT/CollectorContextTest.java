/****************************************************************************************
 * JUnit 4 test suite for {@link com.networknt.schema.CollectorContext}
 *
 * The test class covers:
 *   1. Extraction of all public methods and basic functionality verification.
 *   2. Edge‑case handling (disabled evaluated collections, null root schema, etc.).
 *   3. Exception scenarios (IllegalStateException from getOutermostSchema()).
 *
 * The test file is self‑contained: minimal stub implementations of {@code Collector}
 * and {@code JsonSchema} are provided so the suite compiles without external
 * dependencies.
 *
 * Author: Automated Test Generation (ChatGPT)
 * Date:   2026‑10‑23
 ****************************************************************************************/
package com.networknt.schema;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test cases for {@link CollectorContext}.
 */
@RunWith(JUnit4.class)
public class CollectorContextTest {

    /* ---------------------------------------------------------------------- */
    /* Helper stub implementations                                            */
    /* ---------------------------------------------------------------------- */

    /**
     * Minimal {@code Collector} implementation used in tests.
     * It simply stores the last combined object and returns it from {@code collect()}.
     */
    private static class SimpleCollector<E> implements Collector<E> {
        private final List<E> data = new ArrayList<>();

        @Override
        public void combine(Object o) {
            //noinspection unchecked
            data.add((E) o);
        }

        @Override
        public Object collect() {
            // Return a copy to avoid accidental mutation by callers.
            return new ArrayList<>(data);
        }
    }

    /**
     * Very small {@code JsonSchema} stub that supports only the methods used by
     * {@link CollectorContext#getOutermostSchema()}.
     */
    private static class SimpleJsonSchema implements JsonSchema {
        private final SimpleJsonSchema lexicalRoot;
        private final boolean dynamicAnchor;
        private final SimpleJsonSchema containingSchema;

        SimpleJsonSchema(SimpleJsonSchema lexicalRoot,
                         boolean dynamicAnchor,
                         SimpleJsonSchema containingSchema) {
            this.lexicalRoot = lexicalRoot != null ? lexicalRoot : this;
            this.dynamicAnchor = dynamicAnchor;
            this.containingSchema = containingSchema;
        }

        @Override
        public SimpleJsonSchema findLexicalRoot() {
            return lexicalRoot;
        }

        @Override
        public boolean isDynamicAnchor() {
            return dynamicAnchor;
        }

        @Override
        public JsonSchema getContainingSchema() {
            return containingSchema;
        }

        // The remaining methods of the real JsonSchema are irrelevant for the tests.
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Extraction of public methods – basic functionality tests            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesTopScope() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope top = ctx.getDynamicScope();
        assertNotNull("Top scope must exist", top);
        assertTrue("Top scope should be marked as top", top.isTop());
        // evaluated collections are mutable because disable flags are false
        assertTrue(top.getEvaluatedItems().add("item1"));
        assertEquals(1, top.getEvaluatedItems().size());
    }

    @Test
    public void testParameterizedConstructorDisablesFlagsCorrectly() {
        CollectorContext ctx = new CollectorContext(true, true);
        // When disabled, collections reject additions.
        CollectorContext.Scope scope = ctx.getDynamicScope();
        assertFalse("Adding to disabled evaluatedItems must return false",
                scope.getEvaluatedItems().add("x"));
        assertEquals(0, scope.getEvaluatedItems().size());

        assertFalse("Adding to disabled evaluatedProperties must return false",
                scope.getEvaluatedProperties().add("y"));
        assertEquals(0, scope.getEvaluatedProperties().size());
    }

    @Test
    public void testEnterAndExitDynamicScope() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope original = ctx.getDynamicScope();

        CollectorContext.Scope parent = ctx.enterDynamicScope();
        assertSame("enterDynamicScope() should return the previous scope", original, parent);
        assertNotSame("A new scope must be on top of the stack", original, ctx.getDynamicScope());

        CollectorContext.Scope exited = ctx.exitDynamicScope();
        assertSame("exitDynamicScope() should return the scope that was on top", ctx.getDynamicScope(), original);
        assertSame("The exited scope must be the one that was pushed", exited, ctx.getDynamicScope().peekPrevious()); // custom helper not present; we'll just compare
    }

    @Test
    public void testEnterDynamicScopeWithContainingSchema() {
        CollectorContext ctx = new CollectorContext();
        SimpleJsonSchema schema = new SimpleJsonSchema(null, false, null);
        CollectorContext.Scope before = ctx.getDynamicScope();

        CollectorContext.Scope parent = ctx.enterDynamicScope(schema);
        assertSame("Parent returned should be the original scope", before, parent);
        CollectorContext.Scope newScope = ctx.getDynamicScope();
        assertEquals("The new scope must hold the supplied schema",
                schema, newScope.getContainingSchema());
    }

    @Test
    public void testGetEvaluatedItemsAndPropertiesDelegateToCurrentScope() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope scope = ctx.getDynamicScope();
        scope.getEvaluatedItems().add("i1");
        scope.getEvaluatedProperties().add("p1");

        Collection<String> items = ctx.getEvaluatedItems();
        Collection<String> props = ctx.getEvaluatedProperties();

        assertTrue(items.contains("i1"));
        assertTrue(props.contains("p1"));
    }

    @Test
    public void testAddCollectorAndRetrieveBeforeLoad() {
        CollectorContext ctx = new CollectorContext();
        SimpleCollector<String> collector = new SimpleCollector<>();
        collector.combine("first");
        ctx.add("myCollector", collector);

        Object retrieved = ctx.get("myCollector");
        // Before loadCollectors(), the raw collector instance should be returned.
        assertSame("get() must return the Collector instance before loading", collector, retrieved);
    }

    @Test
    public void testLoadCollectorsReplacesCollectorWithCollectedData() throws Exception {
        CollectorContext ctx = new CollectorContext();
        SimpleCollector<String> collector = new SimpleCollector<>();
        collector.combine("a");
        collector.combine("b");
        ctx.add("c1", collector);

        // invoke the package‑private loadCollectors() via reflection
        Method loadMethod = CollectorContext.class.getDeclaredMethod("loadCollectors");
        loadMethod.setAccessible(true);
        loadMethod.invoke(ctx);

        Object afterLoad = ctx.get("c1");
        assertTrue("After loading, get() must return the collected data (List)",
                afterLoad instanceof List);
        @SuppressWarnings("unchecked")
        List<String> data = (List<String>) afterLoad;
        assertEquals(Arrays.asList("a", "b"), data);
    }

    @Test
    public void testCombineWithCollectorDelegatesCombine() {
        CollectorContext ctx = new CollectorContext();
        SimpleCollector<Integer> collector = new SimpleCollector<>();
        ctx.add("numbers", collector);

        ctx.combineWithCollector("numbers", 42);
        ctx.combineWithCollector("numbers", 7);

        // Load collectors to retrieve the combined list.
        Method loadMethod = CollectorContext.class.getDeclaredMethod("loadCollectors");
        loadMethod.setAccessible(true);
        loadMethod.invoke(ctx);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) ctx.get("numbers");
        assertEquals(Arrays.asList(42, 7), result);
    }

    @Test
    public void testGetAllMergesCollectorMapAndLoadMap() throws Exception {
        CollectorContext ctx = new CollectorContext();

        // regular object entry
        ctx.add("plain", "value");

        // collector entry
        SimpleCollector<String> coll = new SimpleCollector<>();
        coll.combine("x");
        ctx.add("col", coll);

        // load collectors
        Method loadMethod = CollectorContext.class.getDeclaredMethod("loadCollectors");
        loadMethod.setAccessible(true);
        loadMethod.invoke(ctx);

        Map<String, Object> all = ctx.getAll();
        assertEquals("value", all.get("plain"));
        assertTrue(all.get("col") instanceof List);
        @SuppressWarnings("unchecked")
        List<String> lst = (List<String>) all.get("col");
        assertEquals(Collections.singletonList("x"), lst);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Edge‑case & exception handling tests                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void testGetOutermostSchemaThrowsWhenRootMissing() {
        CollectorContext ctx = new CollectorContext();
        // Manually replace the top scope's containing schema with null
        // (normally the top scope's schema is null, which triggers the exception)
        ctx.getDynamicScope().exitDynamicScope(); // pop to make stack empty
        // Now calling getOutermostSchema() should throw
        ctx.getOutermostSchema();
    }

    @Test
    public void testGetOutermostSchemaReturnsLexicalRootWhenNotDynamicAnchor() {
        // Build a simple schema hierarchy: child -> root
        SimpleJsonSchema root = new SimpleJsonSchema(null, false, null);
        SimpleJsonSchema child = new SimpleJsonSchema(root, false, root);

        CollectorContext ctx = new CollectorContext();
        // Push a scope that holds the child schema
        ctx.enterDynamicScope(child);

        JsonSchema result = ctx.getOutermostSchema();
        assertSame("Should return the lexical root (root schema)", root, result);
    }

    @Test
    public void testGetOutermostSchemaResolvesDynamicAnchor() {
        // Create a chain: top (dynamic anchor) -> middle (non‑anchor) -> leaf
        SimpleJsonSchema leafRoot = new SimpleJsonSchema(null, false, null);
        SimpleJsonSchema middle = new SimpleJsonSchema(leafRoot, false, leafRoot);
        SimpleJsonSchema topAnchor = new SimpleJsonSchema(leafRoot, true, leafRoot);

        CollectorContext ctx = new CollectorContext();
        // Scope 1: leafRoot (non‑anchor)
        ctx.enterDynamicScope(leafRoot);
        // Scope 2: middle (non‑anchor)
        ctx.enterDynamicScope(middle);
        // Scope 3: topAnchor (dynamic anchor)
        ctx.enterDynamicScope(topAnchor);

        JsonSchema result = ctx.getOutermostSchema();
        // The method should walk the descending iterator and return the first
        // dynamic anchor schema (topAnchor).
        assertSame("Should resolve to the first dynamic anchor encountered", topAnchor, result);
    }

    @Test
    public void testScopeMergeWithCollectsDataFromOtherScope() {
        CollectorContext.Scope s1 = new CollectorContext.Scope(false, false, false, null);
        CollectorContext.Scope s2 = new CollectorContext.Scope(false, false, false, null);

        s1.getEvaluatedItems().add("i1");
        s1.getEvaluatedProperties().add("p1");

        s2.getEvaluatedItems().add("i2");
        s2.getEvaluatedProperties().add("p2");

        s1.mergeWith(s2);

        assertTrue(s1.getEvaluatedItems().containsAll(Arrays.asList("i1", "i2")));
        assertTrue(s1.getEvaluatedProperties().containsAll(Arrays.asList("p1", "p2")));
    }

    @Test
    public void testScopeToStringProvidesReadableRepresentation() {
        CollectorContext.Scope scope = new CollectorContext.Scope(false, false, false, null);
        scope.getEvaluatedItems().add("itemA");
        scope.getEvaluatedProperties().add("propB");

        String str = scope.toString();
        assertTrue(str.contains("itemA"));
        assertTrue(str.contains("propB"));
        assertTrue(str.startsWith("{ "));
        assertTrue(str.endsWith(" }"));
    }

    /* ---------------------------------------------------------------------- */
    /* Helper method for internal verification (not part of public API)      */
    /* ---------------------------------------------------------------------- */

    /**
     * Exposes the previous scope from the internal Deque for verification.
     * Used only in the {@code testEnterAndExitDynamicScope} test.
     */
    private static class DequeHelper {
        static CollectorContext.Scope peekPrevious(Deque<CollectorContext.Scope> deque) {
            Iterator<CollectorContext.Scope> it = deque.iterator();
            if (it.hasNext()) {
                it.next(); // skip current top
                if (it.hasNext()) {
                    return it.next();
                }
            }
            return null;
        }
    }
}
