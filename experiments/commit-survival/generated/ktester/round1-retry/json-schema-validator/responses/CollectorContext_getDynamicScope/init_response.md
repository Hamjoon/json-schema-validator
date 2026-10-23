```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CollectorContext#getDynamicScope()}.
 *
 * <p>The tests verify that the dynamic scope handling works as expected:
 *
 * <ul>
 *   <li>On construction a non‑null top scope is created.</li>
 *   <li>Calling {@code enterDynamicScope()} pushes a new scope and {@code getDynamicScope()}
 *       returns that newly pushed scope.</li>
 *   <li>Calling {@code exitDynamicScope()} pops the current scope and the previous scope becomes
 *       the current one again.</li>
 * </ul>
 *
 * <p>No private fields or methods are accessed; only the public API of {@link CollectorContext}
 * is used.
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_getDynamicScope_Test {

    private CollectorContext collectorContext;

    /** Create a fresh {@link CollectorContext} before each test. */
    @BeforeEach
    void setupBeforeEach() {
        // use the default constructor (which internally calls the two‑arg constructor)
        collectorContext = new CollectorContext();
    }

    /** No special cleanup is required after each test. */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the reference to help the GC in case of large test suites.
        collectorContext = null;
    }

    /** No global resources to initialise before all tests. */
    @BeforeAll
    static void setupBeforeAll() {
        // placeholder – kept for symmetry with the template
    }

    /** No global resources to clean up after all tests. */
    @AfterAll
    static void teardownAfterAll() {
        // placeholder – kept for symmetry with the template
    }

    /** The initial dynamic scope created by the constructor must be non‑null. */
    @Test
    void initialDynamicScopeIsNotNull() {
        Scope topScope = collectorContext.getDynamicScope();
        assertNotNull(topScope, "The top dynamic scope should be created by the constructor");
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
        assertNotSame(originalTop, afterEnter, "A new scope should be on top after enterDynamicScope()");

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
}
```