```java
package com.networknt.schema;

import org.junit.jupiter.api.*;

import java.util.*;

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