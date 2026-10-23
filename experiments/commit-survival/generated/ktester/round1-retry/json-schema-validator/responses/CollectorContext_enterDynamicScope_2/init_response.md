```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_enterDynamicScope_2_Test {

    private CollectorContext collectorContext;

    @Mock
    private JsonSchema mockJsonSchema;   // mocked JsonSchema used as a containing schema

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CollectorContext for each test
        collectorContext = new CollectorContext();
        // mockJsonSchema is injected by MockitoExtension
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required
    }

    /**
     * Verify that {@code enterDynamicScope(JsonSchema)} pushes a new {@link CollectorContext.Scope}
     * onto the internal stack and returns the previous (parent) scope.
     * The newly created scope must contain the supplied {@link JsonSchema}.
     */
    @Test
    void testEnterDynamicScopeWithProvidedSchema() {
        // Act – push a new dynamic scope using the mocked JsonSchema
        CollectorContext.Scope parentScope = collectorContext.enterDynamicScope(mockJsonSchema);

        // The method should return the previous (top) scope that existed before the push
        assertNotNull(parentScope, "Parent scope should never be null");

        // The current top scope after the push can be obtained via getDynamicScope()
        CollectorContext.Scope currentTop = collectorContext.getDynamicScope();
        assertNotNull(currentTop, "Current top scope should not be null");

        // The newly created scope must have the provided JsonSchema as its containing schema
        assertSame(mockJsonSchema, currentTop.getContainingSchema(),
                "The new dynamic scope should contain the supplied JsonSchema");

        // The parent scope returned should be the one that was on top before the push.
        // Its containing schema is whatever the CollectorContext created initially (likely null)
        // but we can verify that it is *different* from the new top scope.
        assertNotSame(currentTop, parentScope,
                "Parent scope returned should be different from the newly created top scope");
    }

    /**
     * Verify that {@code enterDynamicScope()} (no‑arg overload) delegates to the
     * {@code enterDynamicScope(JsonSchema)} implementation and uses the containing
     * schema of the current top scope when the argument is {@code null}.
     */
    @Test
    void testEnterDynamicScopeWithNullUsesParentContainingSchema() {
        // First, push a scope with an explicit containing schema (mockJsonSchema)
        collectorContext.enterDynamicScope(mockJsonSchema);

        // The current top now has mockJsonSchema as its containing schema
        CollectorContext.Scope firstTop = collectorContext.getDynamicScope();
        assertSame(mockJsonSchema, firstTop.getContainingSchema(),
                "First top scope should contain the mock JsonSchema");

        // Act – invoke the no‑arg version which should use the current top's schema
        CollectorContext.Scope parentScope = collectorContext.enterDynamicScope(); // delegates to null argument

        // The method should return the scope that was current before this call
        assertSame(firstTop, parentScope,
                "Parent scope returned should be the scope that was on top before the second push");

        // The new top scope after the second push should also contain the same mockJsonSchema
        CollectorContext.Scope secondTop = collectorContext.getDynamicScope();
        assertNotNull(secondTop, "Second top scope should not be null");
        assertSame(mockJsonSchema, secondTop.getContainingSchema(),
                "Second top scope should inherit the containing schema from the previous top scope");
    }
}
```