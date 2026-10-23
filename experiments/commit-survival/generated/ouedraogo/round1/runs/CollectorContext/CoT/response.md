###Test START##
```java
package com.networknt.schema;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CollectorContext}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (including the nested {@code Scope} class).</li>
 *   <li>Typical usage scenarios.</li>
 *   <li>Edge‑cases and exception handling (null schemas, disabled collections, etc.).</li>
 * </ul>
 */
public class CollectorContextTest {

    /** Simple stub implementation of {@link Collector}. */
    private static class StubCollector<E> implements Collector<E> {
        private final List<E> received = new ArrayList<>();
        private final E result;

        StubCollector(E result) {
            this.result = result;
        }

        @Override
        public void combine(Object data) {
            //noinspection unchecked
            received.add((E) data);
        }

        @Override
        public E collect() {
            return result;
        }

        List<E> getReceived() {
            return received;
        }
    }

    /** Minimal stub for {@link JsonSchema}. */
    private static class StubJsonSchema implements JsonSchema {
        private final boolean dynamicAnchor;
        private final StubJsonSchema lexicalRoot;
        private final JsonSchema containingSchema; // may be null

        StubJsonSchema(boolean dynamicAnchor, StubJsonSchema lexicalRoot, JsonSchema containingSchema) {
            this.dynamicAnchor = dynamicAnchor;
            this.lexicalRoot = lexicalRoot == null ? this : lexicalRoot;
            this.containingSchema = containingSchema;
        }

        /** Factory for a simple non‑dynamic root schema. */
        static StubJsonSchema root() {
            return new StubJsonSchema(false, null, null);
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

        // The rest of the JsonSchema API is irrelevant for the tests, so we
        // provide empty/default implementations.
        @Override public String getId() { return null; }
        @Override public String getRef() { return null; }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public JsonSchema getSchemaNode() { return null; }
        @Override public JsonSchema getSchemaPath() { return null; }
        @Override public JsonSchema getSchemaNode(String s) { return null; }
        @Override public JsonSchema getSchemaNode(String s, boolean b) { return null; }
        @Override public boolean isBooleanSchema() { return false; }
        @Override public boolean isTrueSchema() { return false; }
        @Override public boolean isFalseSchema() { return false; }
        @Override public boolean isReferencedSchema() { return false; }
        @Override public boolean isRecursiveReference() { return false; }
        @Override public boolean isDynamicReference() { return false; }
        @Override public JsonSchema getRecursiveReference() { return null; }
        @Override public JsonSchema getDynamicReference() { return null; }
        @Override public JsonSchema get$ref() { return null; }
        @Override public JsonSchema get$dynamicRef() { return null; }
        @Override public JsonSchema get$anchor() { return null; }
        @Override public JsonSchema get$dynamicAnchor() { return null; }
        @Override public JsonSchema get$defs() { return null; }
        @Override public JsonNode getSchemaNode(JsonNode jsonNode) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode jsonNode, boolean b) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode jsonNode, String s) { return null; }
        @Override public JsonNode getSchemaNode(JsonNode jsonNode, String s, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, JsonNode jsonNode1) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, JsonNode jsonNode1, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s, JsonNode jsonNode1) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s, JsonNode jsonNode1, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, JsonNode jsonNode1, JsonNode jsonNode2) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, JsonNode jsonNode1, JsonNode jsonNode2, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s, JsonNode jsonNode1, JsonNode jsonNode2) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s, JsonNode jsonNode1, JsonNode jsonNode2, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, JsonNode jsonNode1, JsonNode jsonNode2, JsonNode jsonNode3) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, JsonNode jsonNode1, JsonNode jsonNode2, JsonNode jsonNode3, boolean b) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s, JsonNode jsonNode1, JsonNode jsonNode2, JsonNode jsonNode3) { return null; }
        @Override public JsonNode findJsonNode(JsonNode jsonNode, String s, JsonNode jsonNode1, JsonNode jsonNode2, JsonNode jsonNode3, boolean b) { return null; }
        @Override public boolean isSchemaNode() { return false; }
    }

    private CollectorContext context;

    @Before
    public void setUp() {
        context = new CollectorContext(); // default: both disables = false
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  Public constructors and basic dynamic‑scope handling               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesTopScope() {
        CollectorContext ctx = new CollectorContext();
        CollectorContext.Scope top = ctx.getDynamicScope();
        assertTrue("Top scope should be marked as top", top.isTop());
        // The top scope must be the only element initially.
        assertEquals("Only one scope should be present", top, ctx.exitDynamicScope());
    }

    @Test
    public void testParameterizedConstructorSetsFlags() {
        CollectorContext ctx = new CollectorContext(true, true);
        // The flags affect the collections inside scopes – we verify via a new scope.
        CollectorContext.Scope newScope = ctx.enterDynamicScope();
        // When disabled, the collection is an unmodifiable AbstractCollection (size == 0, add returns false)
        assertEquals(0, newScope.getEvaluatedItems().size());
        assertFalse(newScope.getEvaluatedItems().add("any"));
        assertEquals(0, newScope.getEvaluatedProperties().size());
        assertFalse(newScope.getEvaluatedProperties().add("anyProp"));
        // Clean up
        ctx.exitDynamicScope();
    }

    @Test
    public void testEnterDynamicScopeWithoutSchema() {
        CollectorContext.Scope parent = context.getDynamicScope();
        CollectorContext.Scope child = context.enterDynamicScope();
        assertNotSame("A new scope must be pushed", parent, child);
        assertEquals("Parent returned by enterDynamicScope should be the previous scope",
                parent, child);
        assertSame("Current dynamic scope must be the child", child, context.getDynamicScope());
        // exit restores parent
        context.exitDynamicScope();
        assertSame("After exit, dynamic scope must be the original parent", parent, context.getDynamicScope());
    }

    @Test
    public void testEnterDynamicScopeWithSchema() {
        StubJsonSchema schema = new StubJsonSchema(false, null, null);
        CollectorContext.Scope parent = context.getDynamicScope();
        CollectorContext.Scope child = context.enterDynamicScope(schema);
        assertNotSame(parent, child);
        assertSame(schema, child.getContainingSchema());
        // exit restores parent
        context.exitDynamicScope();
        assertSame(parent, context.getDynamicScope());
    }

    @Test
    public void testExitDynamicScopeReturnsPoppedScope() {
        CollectorContext.Scope original = context.getDynamicScope();
        CollectorContext.Scope pushed = context.enterDynamicScope();
        CollectorContext.Scope popped = context.exitDynamicScope();
        assertSame("exitDynamicScope must return the scope that was on top", pushed, popped);
        assertSame("After exit, the current scope must be the original one", original, context.getDynamicScope());
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  getOutermostSchema – normal and edge cases                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetOutermostSchemaSimpleRoot() {
        // Create a root schema and bind it to a new dynamic scope.
        StubJsonSchema root = StubJsonSchema.root();
        context.enterDynamicScope(root);
        JsonSchema result = context.getOutermostSchema();
        assertSame("Outermost schema should be the lexical root (the root schema)", root, result);
        // clean up
        context.exitDynamicScope();
    }

    @Test
    public void testGetOutermostSchemaWithDynamicAnchorResolution() {
        // Build a chain: top (null) -> scopeA (dynamic anchor) -> scopeB (normal) -> scopeC (dynamic anchor)
        StubJsonSchema leafRoot = new StubJsonSchema(false, null, null);
        StubJsonSchema dynamicAnchorA = new StubJsonSchema(true, leafRoot, null);
        StubJsonSchema dynamicAnchorC = new StubJsonSchema(true, leafRoot, null);

        // Scope A (dynamic anchor)
        context.enterDynamicScope(dynamicAnchorA);
        // Scope B (normal)
        context.enterDynamicScope(leafRoot);
        // Scope C (dynamic anchor)
        context.enterDynamicScope(dynamicAnchorC);

        // According to the algorithm, when the lexical root is a dynamic anchor,
        // the first dynamic anchor encountered while walking the stack from top
        // (most recent) to bottom should be returned.
        JsonSchema result = context.getOutermostSchema();
        assertSame("Should return the most recent dynamic anchor (C)", dynamicAnchorC, result);

        // Clean up all added scopes
        context.exitDynamicScope(); // C
        context.exitDynamicScope(); // B
        context.exitDynamicScope(); // A
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOutermostSchemaThrowsWhenNoRootSchema() {
        // The default top scope has a null containing schema, so calling
        // getOutermostSchema() directly must throw.
        context.getOutermostSchema();
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  Evaluated items / properties delegation                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetEvaluatedItemsAndPropertiesDelegateToCurrentScope() {
        CollectorContext.Scope scope = context.getDynamicScope();
        scope.getEvaluatedItems().add("0");
        scope.getEvaluatedProperties().add("name");

        Collection<String> items = context.getEvaluatedItems();
        Collection<String> props = context.getEvaluatedProperties();

        assertTrue(items.contains("0"));
        assertTrue(props.contains("name"));
    }

    @Test
    public void testEvaluatedCollectionsAreImmutableWhenDisabled() {
        CollectorContext disabledCtx = new CollectorContext(true, true);
        CollectorContext.Scope scope = disabledCtx.getDynamicScope();

        // The collection implementation returns false on add().
        assertFalse("add should be rejected when disabled", scope.getEvaluatedItems().add("any"));
        assertFalse("add should be rejected when disabled", scope.getEvaluatedProperties().add("anyProp"));
        assertEquals(0, scope.getEvaluatedItems().size());
        assertEquals(0, scope.getEvaluatedProperties().size());
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  Adding collectors / objects and retrieving them                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddCollectorAndGetBeforeLoadReturnsCollectorInstance() {
        StubCollector<String> collector = new StubCollector<>("collected");
        context.add("myCollector", collector);
        Object retrieved = context.get("myCollector");
        assertSame("Before loadCollectors, get() should return the collector itself",
                collector, retrieved);
    }

    @Test
    public void testLoadCollectorsReplacesCollectorWithCollectedData() {
        StubCollector<Integer> collector = new StubCollector<>(42);
        context.add("num", collector);
        // Trigger loading
        // loadCollectors is package‑private; we can invoke via reflection.
        try {
            java.lang.reflect.Method m = CollectorContext.class.getDeclaredMethod("loadCollectors");
            m.setAccessible(true);
            m.invoke(context);
        } catch (Exception e) {
            fail("Reflection failure while invoking loadCollectors: " + e);
        }

        Object afterLoad = context.get("num");
        assertEquals("After loading, get() must return collector.collect()", 42, afterLoad);
    }

    @Test
    public void testAddObjectAndRetrieve() {
        String value = "simpleString";
        context.add("key", value);
        assertEquals("Should retrieve the exact object that was added", value, context.get("key"));
    }

    @Test
    public void testGetReturnsNullForUnknownKey() {
        assertNull("Retrieving a non‑existent key must return null", context.get("unknown"));
    }

    @Test
    public void testCombineWithCollectorInvokesCollectorCombine() {
        StubCollector<String> collector = new StubCollector<>("ignored");
        context.add("c", collector);
        context.combineWithCollector("c", "payload");
        assertEquals("Collector should have received the combined payload",
                Collections.singletonList("payload"), collector.getReceived());
    }

    @Test
    public void testCombineWithCollectorDoesNothingWhenKeyIsNotACollector() {
        context.add("plain", "value");
        // Should not throw any exception.
        context.combineWithCollector("plain", "ignored");
        // Verify that the stored value is unchanged.
        assertEquals("value", context.get("plain"));
    }

    /* ---------------------------------------------------------------------- */
    /*  5.  getAll – merging of collectorMap and collectorLoadMap              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetAllMergesMapsCorrectly() throws Exception {
        // Add a plain object and a collector.
        context.add("obj", "original");
        StubCollector<String> collector = new StubCollector<>("collected");
        context.add("col", collector);

        // Load collectors via reflection.
        java.lang.reflect.Method load = CollectorContext.class.getDeclaredMethod("loadCollectors");
        load.setAccessible(true);
        load.invoke(context);

        Map<String, Object> all = context.getAll();

        // The plain object should be present unchanged.
        assertEquals("original", all.get("obj"));
        // The collector key should now map to the collected value.
        assertEquals("collected", all.get("col"));
    }

    /* ---------------------------------------------------------------------- */
    /*  6.  Scope specific behaviour                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testScopeMergeWithCombinesEvaluatedCollections() {
        CollectorContext.Scope s1 = context.enterDynamicScope();
        s1.getEvaluatedItems().add("0");
        s1.getEvaluatedProperties().add("prop1");

        CollectorContext.Scope s2 = context.enterDynamicScope();
        s2.getEvaluatedItems().add("1");
        s2.getEvaluatedProperties().add("prop2");

        // Merge s2 into s1
        s1.mergeWith(s2);

        assertTrue(s1.getEvaluatedItems().containsAll(Arrays.asList("0", "1")));
        assertTrue(s1.getEvaluatedProperties().containsAll(Arrays.asList("prop1", "prop2")));

        // Clean up scopes
        context.exitDynamicScope(); // s2
        context.exitDynamicScope(); // s1
    }

    @Test
    public void testScopeToStringContainsEvaluatedCollections() {
        CollectorContext.Scope scope = context.getDynamicScope();
        scope.getEvaluatedItems().add("item");
        scope.getEvaluatedProperties().add("prop");

        String str = scope.toString();
        assertTrue("toString should contain evaluatedItems", str.contains("item"));
        assertTrue("toString should contain evaluatedProperties", str.contains("prop"));
        assertTrue("toString should be a JSON‑like representation", str.startsWith("{"));
        assertTrue("toString should end with }", str.endsWith(" }"));
    }

    /* ---------------------------------------------------------------------- */
    /*  7.  Edge case: calling loadCollectors when no collectors are present  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLoadCollectorsDoesNothingWhenNoCollectors() throws Exception {
        // No collectors added.
        java.lang.reflect.Method load = CollectorContext.class.getDeclaredMethod("loadCollectors");
        load.setAccessible(true);
        load.invoke(context);
        // The internal load map should stay empty.
        assertTrue("collectorLoadMap must be empty", context.getAll().isEmpty());
    }
}
```
###Test END##