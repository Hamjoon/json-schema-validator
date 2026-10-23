```java
package com.networknt.schema;

import org.junit.jupiter.api.*;

import java.util.NoSuchElementException;

/**
 * Unit tests for {@link CollectorContext#exitDynamicScope()}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>Calling {@code exitDynamicScope()} returns the {@link Scope} that was most recently
 *       pushed onto the internal stack via {@code enterDynamicScope()}.</li>
 *   <li>The method correctly removes the scope from the internal {@code Deque}.</li>
 *   <li>Attempting to pop a scope when none are left results in a {@link NoSuchElementException},
 *       which is the behaviour of {@link java.util.Deque#pop()}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_exitDynamicScope_Test {

    private CollectorContext collectorContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The default constructor creates an initial top‑level scope.
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the instance will be garbage‑collected.
        collectorContext = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /**
     * Verify that a scope entered via {@code enterDynamicScope()} is the same instance
     * returned by the subsequent {@code exitDynamicScope()} call.
     */
    @Test
    void exitDynamicScopeReturnsPreviouslyEnteredScope() {
        // given: a fresh CollectorContext and a newly entered dynamic scope
        Scope enteredScope = collectorContext.enterDynamicScope();

        // when: we exit the dynamic scope
        Scope exitedScope = collectorContext.exitDynamicScope();

        // then: the exited scope must be the same instance that was entered
        Assertions.assertSame(enteredScope, exitedScope,
                "exitDynamicScope should return the scope that was most recently entered");
    }

    /**
     * Verify that calling {@code exitDynamicScope()} on a brand‑new CollectorContext
     * (which already contains the initial top scope) returns that initial scope.
     */
    @Test
    void exitDynamicScopeOnFreshContextPopsInitialScope() {
        // given: a fresh CollectorContext (its constructor already pushed a top scope)

        // when: we exit the dynamic scope without an explicit prior enter
        Scope poppedScope = collectorContext.exitDynamicScope();

        // then: a non‑null scope should be returned (the initial top scope)
        Assertions.assertNotNull(poppedScope, "The initial top scope should be popped and returned");

        // and: a second pop should fail because the internal deque is now empty
        Assertions.assertThrows(NoSuchElementException.class,
                () -> collectorContext.exitDynamicScope(),
                "Popping from an empty dynamicScopes deque should throw NoSuchElementException");
    }

    /**
     * Verify that after exiting a scope, the internal stack no longer contains that scope.
     * This is indirectly checked by ensuring a second pop throws an exception.
     */
    @Test
    void scopeIsRemovedFromStackAfterExit() {
        // given: enter two nested scopes
        Scope outer = collectorContext.enterDynamicScope();
        Scope inner = collectorContext.enterDynamicScope();

        // when: exit the inner scope
        Scope exitedInner = collectorContext.exitDynamicScope();

        // then: the returned scope must be the inner one
        Assertions.assertSame(inner, exitedInner, "The inner scope should be popped first");

        // when: exit the outer scope
        Scope exitedOuter = collectorContext.exitDynamicScope();

        // then: the returned scope must be the outer one
        Assertions.assertSame(outer, exitedOuter, "The outer scope should be popped after the inner one");

        // and: a further pop should fail
        Assertions.assertThrows(NoSuchElementException.class,
                () -> collectorContext.exitDynamicScope(),
                "All scopes have been popped; further pop should throw NoSuchElementException");
    }
}
```