```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests cover the two distinct execution paths:
 * <ul>
 *   <li>When {@code shouldValidateSchema} is {@code true} and the node is non‑null the validator
 *       must delegate to its {@code validate(..., true)} method and return the messages produced.</li>
 *   <li>When {@code shouldValidateSchema} is {@code false} (or the node is {@code null}) the validator
 *       must walk each child schema with {@code shouldValidateSchema = false} and return an empty set.</li>
 * </ul>
 *
 * All heavy dependencies are mocked – no private fields or implementation details are accessed.
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /** Mocks required for constructing the {@link OneOfValidator}. */
    @Mock ValidationContext validationContext;
    @Mock ExecutionContext executionContext;
    @Mock JsonNode node;
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instanceLocation;

    /** Two child schemas that will be returned by the mocked {@link ValidationContext}. */
    @Mock JsonSchema childSchema1;
    @Mock JsonSchema childSchema2;

    /** The validator under test. */
    OneOfValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        /* -----------------------------------------------------------------
         * Build a minimal JSON array node that represents the “oneOf” schema.
         * The actual content of the child schemas is irrelevant because the
         * ValidationContext is mocked to return our pre‑created childSchema
         * mocks.
         * ----------------------------------------------------------------- */
        JsonNode schemaArrayNode = mock(JsonNode.class);
        when(schemaArrayNode.isArray()).thenReturn(true);
        when(schemaArrayNode.size()).thenReturn(2);
        when(schemaArrayNode.get(0)).thenReturn(mock(JsonNode.class));
        when(schemaArrayNode.get(1)).thenReturn(mock(JsonNode.class));

        /* -----------------------------------------------------------------
         * Mock ValidationContext#newSchema(...) to return the two child schemas.
         * The method is called twice (once per array element) during the
         * OneOfValidator construction.
         * ----------------------------------------------------------------- */
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(childSchema1, childSchema2);

        /* -----------------------------------------------------------------
         * Construct the validator.  All constructor arguments except the
         * ValidationContext are either mocks or simple dummies.
         * ----------------------------------------------------------------- */
        SchemaLocation dummyLocation = mock(SchemaLocation.class);
        JsonNodePath dummyPath = mock(JsonNodePath.class);
        validator = new OneOfValidator(dummyLocation,
                dummyPath,
                schemaArrayNode,
                null,                     // parentSchema – not needed for these tests
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release – Mockito handles mock lifecycle.
    }

    /* --------------------------------------------------------------
     * Test case 1 – the validator should delegate to its protected
     * validate(..., true) method when the flag is true and a node is
     * supplied.  The returned set must be exactly the set produced by
     * that method.
     * -------------------------------------------------------------- */
    @Test
    void walk_ShouldValidateWhenFlagTrueAndNodeNotNull() {
        // Spy the validator so we can stub the protected validate method.
        OneOfValidator spyValidator = Mockito.spy(validator);

        // Prepare a dummy ValidationMessage and a set containing it.
        ValidationMessage dummyMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> dummySet = new HashSet<>(Collections.singleton(dummyMessage));

        // Stub the protected validate method that includes the “walk” flag.
        doReturn(dummySet).when(spyValidator)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        // Invoke the method under test.
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode,
                instanceLocation, true);

        // Verify behaviour and result.
        assertSame(dummySet, result, "The walk method should return the set produced by validate");
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        verifyNoInteractions(childSchema1, childSchema2);
    }

    /* --------------------------------------------------------------
     * Test case 2 – when the flag is false the validator must walk each
     * child schema with shouldValidateSchema = false and must return an
     * empty set (the validator itself does not collect messages in this
     * branch).
     * -------------------------------------------------------------- */
    @Test
    void walk_ShouldDelegateToChildSchemasWhenFlagFalse() {
        // Stub the child schemas to return empty sets when walked.
        when(childSchema1.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());

        // Invoke the method under test with the flag set to false.
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                instanceLocation, false);

        // The validator itself returns an empty set in this path.
        assertTrue(result.isEmpty(), "walk should return an empty set when shouldValidateSchema is false");

        // Verify that each child schema was walked exactly once with the correct flag.
        verify(childSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false));
        verify(childSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false));

        // No validation should have been performed on the parent validator.
        // (We cannot directly verify a protected method, but the absence of
        // interactions with the child schemas beyond the expected walk calls
        // confirms the intended flow.)
    }
}
```