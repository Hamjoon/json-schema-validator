###Test START##
```java
/****************************************************************************************
 * Comprehensive JUnit‑4 test suite for {@link CollectorContext}.
 *
 * The tests are written by three “experts” that each contributed a set of scenarios
 * covering typical usage, edge cases and error conditions.  The suite lives in the
 * same package as the class under test so that package‑private methods (e.g.
 * {@code loadCollectors()}) can be invoked directly.
 *
 * The following helper types are defined inside the test class:
 *   • {@code DummyCollector<E>} – a minimal implementation of {@link Collector}
 *     that records combined values and returns them on {@code collect()}.
 *   • {@code DummySchema} – a lightweight stub of {@link JsonSchema} providing the
 *     methods required by {@code CollectorContext#getOutermostSchema()}.
 *
 * The test class validates:
 *   – construction and default flags,
 *   – dynamic scope management (enter/exit/get),
 *   – evaluation‑tracking collections (including disabled behaviour),
 *   – merging of scopes,
 *   – collector handling (add, combine, load, retrieval),
 *   – the logic of {@code getOutermostSchema()} for both static and dynamic anchors,
 *   – the {@code getAll()} map merging semantics,
 *   – string representation of a scope.
 *
 * All tests are written with JUnit‑4 annotations and use only standard JDK classes.
 ****************************************************************************************/
package com.networknt.schema;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CollectorContextTest {

    private CollectorContext context;

    @Before
    public void setUp() {
        // default constructor – both unevaluated features disabled = false
        context = new CollectorContext();
    }

    /* -------------------------------------------------------------------------
     * Helper implementations
     * ---------------------------------------------------------------------- */

    /** Minimal {@link Collector} implementation that stores combined objects. */
    private static class DummyCollector<E> implements Collector<E> {
        private final List<E> data = new ArrayList<>();

        @Override
        public void combine(Object obj) {
            //noinspection unchecked
            data.add((E) obj);
        }

        @Override
        public E collect() {
            //noinspection unchecked
            return (E) new ArrayList<>(data);
        }
    }

    /** Very small stub of {@link JsonSchema} used only for {@code getOutermostSchema}. */
    private static class DummySchema implements JsonSchema {
        private final DummySchema lexicalRoot;
        private final boolean dynamicAnchor;
        private final JsonSchema containingSchema; // could be null

        DummySchema(DummySchema lexicalRoot, boolean dynamicAnchor, JsonSchema containingSchema) {
            this.lexicalRoot = lexicalRoot != null ? lexicalRoot : this;
            this.dynamicAnchor = dynamicAnchor;
            this.containingSchema = containingSchema;
        }

        @Override
        public JsonSchema findLexicalRoot() {
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

        // The real JsonSchema has many other methods – they are not needed for the tests.
    }

    /* -------------------------------------------------------------------------
     * Basic construction and top‑scope behaviour
     * ---------------------------------------------------------------------- */

    @Test
    public void defaultConstructor_createsTopScopeWithDisabledFlags_false() {
        // By default both flags are false → collections are mutable
        CollectorContext.Scope top = context.getDynamicScope();
        assertTrue("Top scope should be marked as top", top.isTop());
        assertNotNull("Evaluated items collection must never be null", top.getEvaluatedItems());
        assertNotNull("Evaluated properties collection must never be null", top.getEvaluatedProperties());

        // Verify that we can actually add entries
        assertTrue(top.getEvaluatedItems().add("0"));
        assertTrue(top.getEvaluatedProperties().add("prop"));
        assertEquals(1, top.getEvaluatedItems().size());
        assertEquals(1, top.getEvaluatedProperties().size());
    }

    @Test
    public void constructorWithFlags_disablesCorrespondingCollections() {
        CollectorContext ctx = new CollectorContext(true, true);
        CollectorContext.Scope top = ctx.getDynamicScope();

        // Collections should reject any addition
        assertFalse("When disabled, evaluated items must refuse add()", top.getEvaluatedItems().add("0"));
        assertFalse("When disabled, evaluated properties must refuse add()", top.getEvaluatedProperties().add("prop"));
        assertEquals(0, top.getEvaluatedItems().size());
        assertEquals(0, top.getEvaluatedProperties().size());
    }

    /* -------------------------------------------------------------------------
     * Dynamic scope stack handling
     * ---------------------------------------------------------------------- */

    @Test
    public void enterDynamicScope_pushesNewScope_andReturnsParent() {
        CollectorContext.Scope parent = context.getDynamicScope();
        CollectorContext.Scope returned = context.enterDynamicScope();
        assertSame("enterDynamicScope should return the previous scope", parent, returned);
        assertNotSame("A new scope must be on top of the stack", parent, context.getDynamicScope());
    }

    @Test
    public void enterDynamicScope_withSchema_usesProvidedSchemaWhenNotNull() {
        DummySchema schema = new DummySchema(null, false, null);
        CollectorContext.Scope parent = context.getDynamicScope();
        CollectorContext.Scope newScope = context.enterDynamicScope(schema);
        assertSame("Parent scope must be returned", parent, newScope);
        assertSame("Containing schema of the newly created scope must be the supplied schema",
                schema, context.getDynamicScope().getContainingSchema());
    }

    @Test
    public void exitDynamicScope_restoresPreviousScope() {
        CollectorContext.Scope first = context.getDynamicScope();
        context.enterDynamicScope();
        CollectorContext.Scope second = context.getDynamicScope();

        CollectorContext.Scope exited = context.exitDynamicScope();
        assertSame("exitDynamicScope should return the scope being removed", second, exited);
        assertSame("After exit, the top of the stack must be the original scope", first, context.getDynamicScope());
    }

    @Test(expected = EmptyStackException.class)
    public void exitDynamicScope_onEmptyStack_throwsException() {
        // Pop the initial top scope
        context.exitDynamicScope();
        // Now the stack is empty – another pop should fail
        context.exitDynamicScope();
    }

    /* -------------------------------------------------------------------------
     * Evaluated items / properties accessors
     * ---------------------------------------------------------------------- */

    @Test
    public void getEvaluatedItems_returnsCurrentScopeCollection() {
        CollectorContext.Scope top = context.getDynamicScope();
        top.getEvaluatedItems().add("1");
        Collection<String> items = context.getEvaluatedItems();
        assertEquals(Collections.singleton("1"), new HashSet<>(items));
    }

    @Test
    public void getEvaluatedProperties_returnsCurrentScopeCollection() {
        CollectorContext.Scope top = context.getDynamicScope();
        top.getEvaluatedProperties().add("propA");
        Collection<String> props = context.getEvaluatedProperties();
        assertEquals(Collections.singleton("propA"), new HashSet<>(props));
    }

    /* -------------------------------------------------------------------------
     * Collector handling (add / get / combine / load / getAll)
     * ---------------------------------------------------------------------- */

    @Test
    public void addCollector_andGet_beforeLoad_returnsCollectorInstance() {
        DummyCollector<String> collector = new DummyCollector<>();
        collector.combine("first");
        context.add("myCollector", collector);

        Object result = context.get("myCollector");
        assertSame("Before loading, get() must return the original Collector instance",
                collector, result);
    }

    @Test
    public void addSimpleObject_andGet_returnsObject() {
        String payload = "simpleValue";
        context.add("plainKey", payload);
        assertEquals(payload, context.get("plainKey"));
    }

    @Test
    public void combineWithCollector_delegatesToCollector() {
        DummyCollector<Integer> collector = new DummyCollector<>();
        context.add("numbers", collector);
        context.combineWithCollector("numbers", 10);
        context.combineWithCollector("numbers", 20);

        // Load collectors to materialise the combined data
        context.loadCollectors();
        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) context.get("numbers");
        assertEquals(Arrays.asList(10, 20), result);
    }

    @Test
    public void loadCollectors_populatesCollectorLoadMap_and_getReturnsCollectedData() {
        DummyCollector<String> collector = new DummyCollector<>();
        collector.combine("a");
        collector.combine("b");
        context.add("c1", collector);

        // Ensure the collector is still present before loading
        assertSame(collector, context.get("c1"));

        // Trigger loading
        context.loadCollectors();

        // After loading, get() must return the collected list, not the collector instance
        @SuppressWarnings("unchecked")
        List<String> loaded = (List<String>) context.get("c1");
        assertEquals(Arrays.asList("a", "b"), loaded);
    }

    @Test
    public void getAll_mergesCollectorMap_andLoadMap_correctly() {
        DummyCollector<String> collector = new DummyCollector<>();
        collector.combine("x");
        context.add("c1", collector);
        context.add("plain", "value");

        // Load collectors so that the load map contains an entry for "c1"
        context.loadCollectors();

        Map<String, Object> all = context.getAll();
        assertTrue(all.containsKey("c1"));
        assertTrue(all.containsKey("plain"));
        // The value for "c1" must be the collected list, not the collector instance
        @SuppressWarnings("unchecked")
        List<String> collected = (List<String>) all.get("c1");
        assertEquals(Collections.singletonList("x"), collected);
        assertEquals("value", all.get("plain"));
    }

    /* -------------------------------------------------------------------------
     * Scope merging and toString()
     * ---------------------------------------------------------------------- */

    @Test
    public void mergeWith_combinesEvaluatedCollections() {
        CollectorContext.Scope s1 = new CollectorContext.Scope(false, false, null);
        CollectorContext.Scope s2 = new CollectorContext.Scope(false, false, null);

        s1.getEvaluatedItems().add("i1");
        s1.getEvaluatedProperties().add("p1");

        s2.getEvaluatedItems().add("i2");
        s2.getEvaluatedProperties().add("p2");

        s1.mergeWith(s2);

        assertEquals(new HashSet<>(Arrays.asList("i1", "i2")), new HashSet<>(s1.getEvaluatedItems()));
        assertEquals(new HashSet<>(Arrays.asList("p1", "p2")), new HashSet<>(s1.getEvaluatedProperties()));
    }

    @Test
    public void mergeWith_onDisabledCollections_leavesCollectionsEmpty() {
        CollectorContext.Scope disabled = new CollectorContext.Scope(true, true, null);
        CollectorContext.Scope normal = new CollectorContext.Scope(false, false, null);
        normal.getEvaluatedItems().add("item");
        normal.getEvaluatedProperties().add("prop");

        // merging a normal scope into a disabled one – the disabled collection must ignore adds
        disabled.mergeWith(normal);
        assertEquals(0, disabled.getEvaluatedItems().size());
        assertEquals(0, disabled.getEvaluatedProperties().size());
    }

    @Test
    public void scopeToString_includesBothCollections() {
        CollectorContext.Scope scope = new CollectorContext.Scope(false, false, null);
        scope.getEvaluatedItems().add("i");
        scope.getEvaluatedProperties().add("p");
        String txt = scope.toString();

        assertTrue(txt.contains("\"evaluatedItems\": [i]") || txt.contains("\"evaluatedItems\": [i]"));
        assertTrue(txt.contains("\"evaluatedProperties\": [p]") || txt.contains("\"evaluatedProperties\": [p]"));
    }

    /* -------------------------------------------------------------------------
     * getOutermostSchema() logic – static and dynamic anchor cases
     * ---------------------------------------------------------------------- */

    @Test
    public void getOutermostSchema_returnsRootWhenRootIsNotDynamicAnchor() {
        DummySchema root = new DummySchema(null, false, null);
        // Build a context whose top scope contains the root schema
        CollectorContext ctx = new CollectorContext(false, false);
        ctx.enterDynamicScope(root); // now current scope's containingSchema = root

        JsonSchema result = ctx.getOutermostSchema();
        assertSame("When root is not a dynamic anchor, the lexical root must be returned", root, result);
    }

    @Test
    public void getOutermostSchema_findsDynamicAnchorInStack() {
        // Build a hierarchy:
        //  - top lexical root is a dynamic anchor (root)
        //  - a deeper scope also contains a dynamic anchor (inner)
        DummySchema root = new DummySchema(null, true, null); // dynamic anchor
        DummySchema inner = new DummySchema(root, true, null); // also dynamic anchor

        CollectorContext ctx = new CollectorContext(false, false);
        // First scope (top) contains root
        ctx.enterDynamicScope(root);
        // Second scope contains inner
        ctx.enterDynamicScope(inner);

        JsonSchema result = ctx.getOutermostSchema();

        // According to the algorithm, the first dynamic anchor encountered when walking
        // from the innermost scope outward should be returned – that is 'inner'.
        assertSame("The innermost dynamic anchor should be selected", inner, result);
    }

    @Test(expected = IllegalStateException.class)
    public void getOutermostSchema_throwsWhenNoRootSchemaPresent() {
        // Create a context and then pop the initial scope – the stack becomes empty.
        CollectorContext emptyCtx = new CollectorContext(false, false);
        emptyCtx.exitDynamicScope(); // remove the only (top) scope

        // This call must raise an IllegalStateException because there is no containing schema.
        emptyCtx.getOutermostSchema();
    }
}
```
###Test END##