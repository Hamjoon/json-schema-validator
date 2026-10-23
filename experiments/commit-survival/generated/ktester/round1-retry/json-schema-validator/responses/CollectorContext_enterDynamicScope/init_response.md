```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CollectorContext#enterDynamicScope()} and the overload
 * that accepts a {@link JsonSchema}. The tests verify that a new {@link Scope}
 * is created and becomes the current dynamic scope, and that nested calls are
 * correctly stacked and can be unwound with {@link CollectorContext#exitDynamicScope()}.
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_enterDynamicScope_Test {

    private CollectorContext collectorContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The default constructor creates an initial (root) dynamic scope.
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
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
        assertSame(newScope, collectorContext.getDynamicScope(),
                "The returned Scope must become the current dynamic scope");
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
        assertSame(newScope, collectorContext.getDynamicScope(),
                "The returned Scope must become the current dynamic scope");
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
        assertSame(firstScope, collectorContext.getDynamicScope(),
                "First entered scope should be current");

        // AND: enter second dynamic scope
        Scope secondScope = collectorContext.enterDynamicScope();
        assertNotNull(secondScope, "Second entered scope must be non‑null");
        assertSame(secondScope, collectorContext.getDynamicScope(),
                "Second entered scope should be current");

        // THEN: exiting should unwind the stack
        Scope afterFirstExit = collectorContext.exitDynamicScope();
        assertSame(firstScope, afterFirstExit,
                "exitDynamicScope() should return the scope that was just popped");
        assertSame(firstScope, collectorContext.getDynamicScope(),
                "After one exit, the current scope should be the first entered scope");

        // AND: exiting again should return to the root scope
        Scope afterSecondExit = collectorContext.exitDynamicScope();
        assertSame(rootScope, afterSecondExit,
                "Second exit should return the original root scope");
        assertSame(rootScope, collectorContext.getDynamicScope(),
                "After all exits, the current scope should be the original root scope");
    }
}
```