```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests cover all execution branches described in the JSON test‑case specification,
 * including edge cases such as null schemas, exceptions from delegated methods, and
 * variations of the {@code shouldValidateSchema} flag.
 *
 * <p>All heavy collaborators are mocked; the {@link OneOfValidator} itself is either
 * used directly, spied, or subclassed when we need to override protected methods.</p>
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /* --------------------------------------------------------------------- *
     *  Common mocks used by most test cases.
     * --------------------------------------------------------------------- */
    @Mock ValidationContext validationContext;
    @Mock ExecutionContext executionContext;
    @Mock JsonNode node;            // non‑null instance node
    @Mock JsonNode nullNode;        // will be used when a null node is required
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instanceLocation;

    @Mock JsonSchema childSchema1;
    @Mock JsonSchema childSchema2;
    @Mock JsonSchema childSchema3; // for tests needing three child schemas

    OneOfValidator validator; // will be (re)initialised in @BeforeEach

    /* --------------------------------------------------------------------- *
     *  Helper to inject a custom {@code schemas} list via reflection.
     * --------------------------------------------------------------------- */
    private static void setSchemas(OneOfValidator v, List<JsonSchema> list) {
        try {
            Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            f.set(v, list);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        /* -------------------------------------------------------------
         *  Build a dummy “oneOf” array node – its content is irrelevant,
         *  only the size matters for the constructor.
         * ------------------------------------------------------------- */
        JsonNode schemaArrayNode = mock(JsonNode.class);
        when(schemaArrayNode.isArray()).thenReturn(true);
        when(schemaArrayNode.size()).thenReturn(2);
        when(schemaArrayNode.get(0)).thenReturn(mock(JsonNode.class));
        when(schemaArrayNode.get(1)).thenReturn(mock(JsonNode.class));

        /* -------------------------------------------------------------
         *  ValidationContext#newSchema(..) is called once per array entry.
         * ------------------------------------------------------------- */
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(childSchema1, childSchema2);

        /* -------------------------------------------------------------
         *  Construct the validator with the two child schemas.
         * ------------------------------------------------------------- */
        SchemaLocation dummyLocation = mock(SchemaLocation.class);
        JsonNodePath dummyPath = mock(JsonNodePath.class);
        validator = new OneOfValidator(dummyLocation,
                dummyPath,
                schemaArrayNode,
                null,               // parentSchema – not needed here
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Mockito handles mock lifecycle automatically.
    }

    /* --------------------------------------------------------------------- *
     *  1. shouldValidateSchema == true && node != null  → delegate to validate
     * --------------------------------------------------------------------- */
    @Test
    void walk_ShouldValidateWhenFlagTrueAndNodeNotNull() {
        OneOfValidator spyValidator = Mockito.spy(validator);

        ValidationMessage dummyMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> dummySet = new HashSet<>(Collections.singleton(dummyMessage));

        // Stub the protected validate(..., true) method.
        doReturn(dummySet).when(spyValidator)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode,
                instanceLocation, true);

        assertSame(dummySet, result, "walk should return exactly the set from validate");
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        verifyNoInteractions(childSchema1, childSchema2);
    }

    /* --------------------------------------------------------------------- *
     *  2. shouldValidateSchema == false  → delegate to each child schema
     * --------------------------------------------------------------------- */
    @Test
    void walk_ShouldDelegateToChildSchemasWhenFlagFalse() {
        when(childSchema1.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                instanceLocation, false);

        assertTrue(result.isEmpty(), "walk should return an empty set when flag is false");
        verify(childSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false));
        verify(childSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  3. shouldValidateSchema == true && node == null  → else‑branch
     * --------------------------------------------------------------------- */
    @Test
    void walk_ShouldDelegateToChildSchemasWhenFlagTrueButNodeNull() {
        // Use the same child schemas as in the default setup.
        when(childSchema1.walk(eq(executionContext), eq(nullNode), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(eq(executionContext), eq(nullNode), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, nullNode, rootNode,
                instanceLocation, true);

        assertTrue(result.isEmpty(), "When node is null, walk should behave like the else‑branch");
        verify(childSchema1, times(1)).walk(eq(executionContext), eq(nullNode), eq(rootNode),
                eq(instanceLocation), eq(false));
        verify(childSchema2, times(1)).walk(eq(executionContext), eq(nullNode), eq(rootNode),
                eq(instanceLocation), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  4. No child schemas (empty list) → else‑branch returns empty set
     * --------------------------------------------------------------------- */
    @Test
    void walk_NoChildSchemas_ElseBranchReturnsEmpty() throws Exception {
        // Replace the internal schemas list with an empty list.
        setSchemas(validator, Collections.emptyList());

        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                instanceLocation, false);

        assertTrue(result.isEmpty(), "Empty schemas list should yield an empty result set");
        // No child schema interactions should happen.
        verifyNoInteractions(childSchema1, childSchema2);
    }

    /* --------------------------------------------------------------------- *
     *  5. Null schemas list → NullPointerException
     * --------------------------------------------------------------------- */
    @Test
    void walk_NullSchemasList_ThrowsNullPointerException() {
        setSchemas(validator, null); // force a null internal list

        assertThrows(NullPointerException.class,
                () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                "Iterating over a null schemas list must throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  6. Null child schema element → NullPointerException
     * --------------------------------------------------------------------- */
    @Test
    void walk_NullChildSchemaInList_ThrowsNullPointerException() {
        List<JsonSchema> listWithNull = new ArrayList<>();
        listWithNull.add(null);          // first element is null
        listWithNull.add(childSchema1);  // second element is a valid mock
        setSchemas(validator, listWithNull);

        assertThrows(NullPointerException.class,
                () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                "Calling walk on a null child schema should throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  7. validate method throws RuntimeException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void walk_ValidateMethodThrowsRuntimeException() {
        // Subclass that overrides the protected validate method to throw.
        class ThrowingValidator extends OneOfValidator {
            ThrowingValidator(SchemaLocation loc, JsonNodePath path,
                              JsonNode schemaNode, JsonSchema parent,
                              ValidationContext ctx) {
                super(loc, path, schemaNode, parent, ctx);
            }

            @Override
            protected Set<ValidationMessage> validate(ExecutionContext ec, JsonNode n,
                                                       JsonNode r, JsonNodePath i, boolean w) {
                throw new RuntimeException("validation failed");
            }
        }

        // Build a minimal validator instance (the constructor logic is unchanged).
        JsonNode schemaArrayNode = mock(JsonNode.class);
        when(schemaArrayNode.isArray()).thenReturn(true);
        when(schemaArrayNode.size()).thenReturn(0); // no child schemas needed
        SchemaLocation dummyLocation = mock(SchemaLocation.class);
        JsonNodePath dummyPath = mock(JsonNodePath.class);
        ThrowingValidator throwingValidator = new ThrowingValidator(dummyLocation,
                dummyPath, schemaArrayNode, null, validationContext);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> throwingValidator.walk(executionContext, node, rootNode,
                        instanceLocation, true),
                "Exception from validate should propagate");
        assertEquals("validation failed", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  8. Child schema walk throws RuntimeException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void walk_ChildSchemaWalkThrowsRuntimeException() {
        when(childSchema1.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false)))
                .thenThrow(new RuntimeException("child walk failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.walk(executionContext, node, rootNode,
                        instanceLocation, false),
                "Exception from child schema walk should propagate");
        assertEquals("child walk failed", ex.getMessage());

        // Verify that the second child was never invoked because the first already failed.
        verify(childSchema2, never()).walk(any(), any(), any(), any(), any());
    }

    /* --------------------------------------------------------------------- *
     *  9. Multiple child schemas (three) receive walk with flag false
     * --------------------------------------------------------------------- */
    @Test
    void walk_MultipleChildSchemas_ValidateFalse() {
        // Replace internal list with three mocks.
        List<JsonSchema> threeSchemas = Arrays.asList(childSchema1, childSchema2, childSchema3);
        setSchemas(validator, threeSchemas);

        // Stub each child to return an empty set.
        when(childSchema1.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema3.walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode,
                instanceLocation, false);

        assertTrue(result.isEmpty(), "Result should be empty when child walks return empty sets");
        verify(childSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false));
        verify(childSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false));
        verify(childSchema3, times(1)).walk(eq(executionContext), eq(node), eq(rootNode),
                eq(instanceLocation), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  10. validate returns empty set (still true branch) → empty result
     * --------------------------------------------------------------------- */
    @Test
    void walk_ValidateReturnsEmptySet() {
        OneOfValidator spyValidator = Mockito.spy(validator);
        Set<ValidationMessage> emptySet = Collections.emptySet();

        doReturn(emptySet).when(spyValidator)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode,
                instanceLocation, true);

        assertTrue(result.isEmpty(), "When validate returns empty, walk should also return empty");
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        verifyNoInteractions(childSchema1, childSchema2);
    }

    /* --------------------------------------------------------------------- *
     *  11. Ensure child schemas are NOT invoked when validation path is taken
     * --------------------------------------------------------------------- */
    @Test
    void walk_DoNotInvokeChildSchemasWhenValidatingItself() {
        OneOfValidator spyValidator = Mockito.spy(validator);
        Set<ValidationMessage> dummySet = Collections.singleton(mock(ValidationMessage.class));

        doReturn(dummySet).when(spyValidator)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode,
                instanceLocation, true);

        assertEquals(dummySet, result);
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // No walk on child schemas should have happened.
        verifyNoInteractions(childSchema1, childSchema2);
    }
}
```