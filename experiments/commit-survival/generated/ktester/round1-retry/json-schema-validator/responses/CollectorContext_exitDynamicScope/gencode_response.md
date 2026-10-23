```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;

/**
 * Unit tests for {@link CollectorContext#exitDynamicScope()}.
 *
 * <p>The tests verify that:
 *
 * <ul>
 *   <li>Calling {@code exitDynamicScope()} returns the {@link Scope} that was most recently
 *       pushed onto the internal stack via {@code enterDynamicScope()} (including the
 *       overload that accepts a {@link JsonSchema}).</li>
 *   <li>The method correctly removes the scope from the internal {@code Deque}.</li>
 *   <li>The internal state after an exit reflects the previous scope.</li>
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
     * Verify that after exiting a scope, a subsequent call to {@code getDynamicScope()}
     * returns the previously entered (now current) scope.
     */
    @Test
    void dynamicScopeStackStateAfterExit() {
        // given: two nested scopes
        Scope scope1 = collectorContext.enterDynamicScope(); // outer
        Scope scope2 = collectorContext.enterDynamicScope(); // inner

        // when: we exit the inner scope
        collectorContext.exitDynamicScope();

        // then: getDynamicScope() should return the outer scope (scope1)
        Scope current = collectorContext.getDynamicScope();
        Assertions.assertSame(scope1, current,
                "After exiting the most recent scope, the current dynamic scope should be the previous one");
    }

    /**
     * Verify LIFO ordering when multiple scopes are entered.
     */
    @Test
    void multipleDynamicScopesLifoBehavior() {
        // given: enter two scopes sequentially
        Scope first = collectorContext.enterDynamicScope();   // Scope1
        Scope second = collectorContext.enterDynamicScope();  // Scope2

        // when: exit once
        Scope poppedFirst = collectorContext.exitDynamicScope();

        // then: the popped scope must be the last entered (second)
        Assertions.assertSame(second, poppedFirst,
                "exitDynamicScope should pop the most recently entered scope (LIFO)");

        // when: exit again
        Scope poppedSecond = collectorContext.exitDynamicScope();

        // then: the remaining scope must be the first one
        Assertions.assertSame(first, poppedSecond,
                "After popping the inner scope, the next pop should return the outer scope");
    }

    /**
     * Verify that repeated calls to {@code exitDynamicScope()} eventually throw
     * {@link NoSuchElementException} once the internal stack is exhausted.
     */
    @Test
    void repeatedExitsUntilEmptyFinalException() {
        // given: push two scopes
        collectorContext.enterDynamicScope();
        collectorContext.enterDynamicScope();

        // when: pop both scopes – should succeed
        collectorContext.exitDynamicScope();
        collectorContext.exitDynamicScope();

        // then: a third pop must fail
        Assertions.assertThrows(NoSuchElementException.class,
                () -> collectorContext.exitDynamicScope(),
                "Third exit after two scopes have been popped should throw NoSuchElementException");
    }

    /**
     * Verify that a scope created via the overloaded {@code enterDynamicScope(JsonSchema)}
     * method is handled identically by {@code exitDynamicScope()}.
     */
    @Test
    void enterDynamicScopeWithContainingSchemaAndExit(@Mock JsonSchema mockSchema) {
        // given: a scope created with a JsonSchema argument
        Scope entered = collectorContext.enterDynamicScope(mockSchema);

        // when: we exit the dynamic scope
        Scope exited = collectorContext.exitDynamicScope();

        // then: the popped scope must be the one created with the schema
        Assertions.assertSame(entered, exited,
                "Scope created via enterDynamicScope(JsonSchema) should be returned by exitDynamicScope");
    }

    /**
     * Verify that after exiting the most recent scope, the internal deque no longer
     * contains that scope. This is indirectly validated by ensuring a second pop
     * throws an exception when only one scope existed.
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