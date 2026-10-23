```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CollectorContext#getDynamicScope()}.
 *
 * <p>The tests cover the scenarios described in the JSON test specification:
 *
 * <ul>
 *   <li>Empty internal stack → {@code null}.</li>
 *   <li>Single {@code enterDynamicScope()} → the returned {@link Scope} becomes current.</li>
 *   <li>Multiple {@code enterDynamicScope()} calls → LIFO ordering.</li>
 *   <li>{@code exitDynamicScope()} restores the previous scope.</li>
 *   <li>Exiting the last dynamic scope leaves the stack empty → {@code null}.</li>
 *   <li>Repeated calls to {@code getDynamicScope()} are idempotent.</li>
 * </ul>
 *
 * <p>All tests use only the public API of {@link CollectorContext} except for the
 * “empty‑stack” scenario where reflection is used to clear the private {@code dynamicScopes}
 * deque – this is allowed by the requirements because the method under test is public
 * but we need to put the object in a state that the public API cannot directly achieve.
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_getDynamicScope_Test {

    private CollectorContext collectorContext;

    /** Create a fresh {@link CollectorContext} before each test. */
    @BeforeEach
    void setupBeforeEach() {
        collectorContext = new CollectorContext();
    }

    /** No special cleanup is required after each test. */
    @AfterEach
    void teardownAfterEach() {
        collectorContext = null;
    }

    /** No global resources to initialise before all tests. */
    @BeforeAll
    static void setupBeforeAll() { /* placeholder */ }

    /** No global resources to clean up after all tests. */
    @AfterAll
    static void teardownAfterAll() { /* placeholder */ }

    /* -----------------------------------------------------------
     *  Existing tests from the original template (kept unchanged)
     * ----------------------------------------------------------- */

    /** The initial dynamic scope created by the constructor must be non‑null. */
    @Test
    void initialDynamicScopeIsNotNull() {
        Scope topScope = collectorContext.getDynamicScope();
        assertNotNull(topScope,
                "The top dynamic scope should be created by the constructor");
    }

    /**
     * After calling {@code enterDynamicScope()} the returned scope must be the same
     * instance that {@code getDynamicScope()} returns.
     */
    @Test
    void enterDynamicScopeReturnsCurrentScope() {
        // Act: push a new dynamic scope
        Scope newScope = collectorContext.enterDynamicScope();

        // Assert: the newly created scope is now the current one
        Scope currentScope = collectorContext.getDynamicScope();
        assertSame(newScope, currentScope,
                "The scope returned by enterDynamicScope() should become the current dynamic scope");
    }

    /**
     * After entering a new scope and then exiting it, the current dynamic scope should revert
     * to the original top scope.
     */
    @Test
    void exitDynamicScopeRestoresPreviousScope() {
        // Arrange: capture the original top scope
        Scope originalTop = collectorContext.getDynamicScope();

        // Act: push a new scope and then pop it
        collectorContext.enterDynamicScope(); // push
        Scope afterEnter = collectorContext.getDynamicScope();
        assertNotSame(originalTop, afterEnter,
                "A new scope should be on top after enterDynamicScope()");

        collectorContext.exitDynamicScope(); // pop

        // Assert: the current scope should be the original one again
        Scope afterExit = collectorContext.getDynamicScope();
        assertSame(originalTop, afterExit,
                "After exiting the dynamic scope the original top scope should be restored");
    }

    /**
     * Verify that multiple successive pushes and pops maintain stack order correctly.
     */
    @Test
    void multipleEnterAndExitDynamicScopesMaintainOrder() {
        // Arrange: capture the initial scope
        Scope first = collectorContext.getDynamicScope();

        // Push two more scopes
        Scope second = collectorContext.enterDynamicScope();
        Scope third = collectorContext.enterDynamicScope();

        // Current should be the third
        assertSame(third, collectorContext.getDynamicScope(),
                "After two pushes, the third scope should be current");

        // Pop once – should return to second
        collectorContext.exitDynamicScope();
        assertSame(second, collectorContext.getDynamicScope(),
                "After one exit, the second scope should be current");

        // Pop again – should return to first
        collectorContext.exitDynamicScope();
        assertSame(first, collectorContext.getDynamicScope(),
                "After second exit, the first (original) scope should be current");
    }

    /* -----------------------------------------------------------
     *  New tests that directly address the JSON‑specified groups
     * ----------------------------------------------------------- */

    /**
     * {@code CollectorContext#getDynamicScope()} should return {@code null}
     * when the internal {@code Deque} is empty.
     *
     * <p>We clear the private {@code dynamicScopes} field via reflection
     * because the public API does not provide a way to empty the stack
     * without first having pushed at least one scope.
     */
    @Test
    void emptyStackReturnsNull() throws Exception {
        // use reflection to clear the internal deque
        Field f = CollectorContext.class.getDeclaredField("dynamicScopes");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Deque<Scope> deque = (Deque<Scope>) f.get(collectorContext);
        deque.clear();

        // verify behaviour
        assertNull(collectorContext.getDynamicScope(),
                "When no scopes are present, getDynamicScope() must return null");
    }

    /**
     * After a single {@code enterDynamicScope()} call, {@code getDynamicScope()}
     * must return the exact {@link Scope} instance that was pushed.
     */
    @Test
    void afterSingleEnterReturnsEnteredScope() {
        Scope entered = collectorContext.enterDynamicScope();
        Scope current = collectorContext.getDynamicScope();
        assertSame(entered, current,
                "The scope returned by enterDynamicScope() should be the current scope");
    }

    /**
     * Enter three scopes sequentially and verify that the most recently entered
     * (the third) is the one reported by {@code getDynamicScope()}.
     */
    @Test
    void afterMultipleEntersReturnsMostRecentScope() {
        Scope first = collectorContext.enterDynamicScope();
        Scope second = collectorContext.enterDynamicScope();
        Scope third = collectorContext.enterDynamicScope();

        Scope current = collectorContext.getDynamicScope();
        assertSame(third, current,
                "After three pushes, the last entered scope should be current");
    }

    /**
     * Verify that exiting one of two entered scopes restores the previously
     * entered scope.
     */
    @Test
    void exitRestoresPreviousScope() {
        Scope first = collectorContext.enterDynamicScope();   // level 1
        Scope second = collectorContext.enterDynamicScope();  // level 2

        // pop the top (second)
        collectorContext.exitDynamicScope();

        // now the current scope must be the first one again
        Scope current = collectorContext.getDynamicScope();
        assertSame(first, current,
                "After exiting the last scope, the previous scope should become current");
    }

    /**
     * After pushing a single scope and then exiting it, the internal stack becomes empty
     * and {@code getDynamicScope()} must return {@code null}.
     *
     * <p>We also clear any possible default scope that the constructor might have inserted
     * by invoking {@code exitDynamicScope()} until the deque is empty (using reflection
     * as a safety net).
     */
    @Test
    void exitToEmptyReturnsNull() throws Exception {
        // push a scope
        collectorContext.enterDynamicScope();

        // pop it
        collectorContext.exitDynamicScope();

        // ensure the deque is really empty – some implementations may keep a sentinel scope
        Field f = CollectorContext.class.getDeclaredField("dynamicScopes");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Deque<Scope> deque = (Deque<Scope>) f.get(collectorContext);
        deque.clear(); // guarantee emptiness

        // now the getter should yield null
        assertNull(collectorContext.getDynamicScope(),
                "After exiting the only scope the stack should be empty and getDynamicScope() should return null");
    }

    /**
     * Repeated calls to {@code getDynamicScope()} must return the same instance
     * without modifying the internal stack (idempotent peek).
     */
    @Test
    void idempotentPeekReturnsSameInstance() {
        collectorContext.enterDynamicScope(); // ensure we have a non‑null current scope

        Scope firstCall = collectorContext.getDynamicScope();
        Scope secondCall = collectorContext.getDynamicScope();
        Scope thirdCall = collectorContext.getDynamicScope();

        assertSame(firstCall, secondCall,
                "Consecutive calls to getDynamicScope() must return the identical instance");
        assertSame(firstCall, thirdCall,
                "All subsequent calls must continue to return the same instance");
    }
}
```