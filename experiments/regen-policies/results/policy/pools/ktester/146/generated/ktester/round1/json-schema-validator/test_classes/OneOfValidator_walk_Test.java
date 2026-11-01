package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /**
     * Mocks required for constructing the {@link OneOfValidator}.
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    ExecutionContext executionContext;

    @Mock
    JsonNode node;

    @Mock
    JsonNode rootNode;

    @Mock
    JsonNodePath instanceLocation;

    /**
     * Two child schemas that will be returned by the mocked {@link ValidationContext}.
     */
    @Mock
    JsonSchema childSchema1;

    @Mock
    JsonSchema childSchema2;

    /**
     * The validator under test.
     */
    OneOfValidator validator;

    // will be used when a null node is required
    @Mock
    JsonNode nullNode;

    // for tests needing three child schemas
    @Mock
    JsonSchema childSchema3;

    class ThrowingValidator extends OneOfValidator {

        ThrowingValidator(SchemaLocation loc, JsonNodePath path, JsonNode schemaNode, JsonSchema parent, ValidationContext ctx) {
            super(loc, path, schemaNode, parent, ctx);
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext ec, JsonNode n, JsonNode r, JsonNodePath i, boolean w) {
            throw new RuntimeException("validation failed");
        }
    }

    class ThrowingValidator extends OneOfValidator {

        ThrowingValidator(SchemaLocation loc, JsonNodePath path, JsonNode schemaNode, JsonSchema parent, ValidationContext ctx) {
            super(loc, path, schemaNode, parent, ctx);
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext ec, JsonNode n, JsonNode r, JsonNodePath i, boolean w) {
            throw new RuntimeException("validation failed");
        }
    }

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
        when(validationContext.newSchema(any(), any(), any(), any())).thenReturn(childSchema1, childSchema2);
        /* -----------------------------------------------------------------
         * Construct the validator.  All constructor arguments except the
         * ValidationContext are either mocks or simple dummies.
         * ----------------------------------------------------------------- */
        SchemaLocation dummyLocation = mock(SchemaLocation.class);
        JsonNodePath dummyPath = mock(JsonNodePath.class);
        validator = new OneOfValidator(dummyLocation, dummyPath, schemaArrayNode, // parentSchema – not needed for these tests
        null, validationContext);
    }

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
        doReturn(dummySet).when(spyValidator).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // Invoke the method under test.
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode, instanceLocation, true);
        // Verify behaviour and result.
        assertSame(dummySet, result, "The walk method should return the set produced by validate");
        verify(spyValidator, times(1)).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
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
        when(childSchema1.walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        // Invoke the method under test with the flag set to false.
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, instanceLocation, false);
        // The validator itself returns an empty set in this path.
        assertTrue(result.isEmpty(), "walk should return an empty set when shouldValidateSchema is false");
        // Verify that each child schema was walked exactly once with the correct flag.
        verify(childSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(childSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        // No validation should have been performed on the parent validator.
        // (We cannot directly verify a protected method, but the absence of
        // interactions with the child schemas beyond the expected walk calls
        // confirms the intended flow.)
    }

    /* --------------------------------------------------------------------- *
     *  3. shouldValidateSchema == true && node == null  → else‑branch
     * --------------------------------------------------------------------- */
    @Test
    void walk_ShouldDelegateToChildSchemasWhenFlagTrueButNodeNull() {
        // Use the same child schemas as in the default setup.
        when(childSchema1.walk(eq(executionContext), eq(nullNode), eq(rootNode), eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(eq(executionContext), eq(nullNode), eq(rootNode), eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.walk(executionContext, nullNode, rootNode, instanceLocation, true);
        assertTrue(result.isEmpty(), "When node is null, walk should behave like the else‑branch");
        verify(childSchema1, times(1)).walk(eq(executionContext), eq(nullNode), eq(rootNode), eq(instanceLocation), eq(false));
        verify(childSchema2, times(1)).walk(eq(executionContext), eq(nullNode), eq(rootNode), eq(instanceLocation), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  4. No child schemas (empty list) → else‑branch returns empty set
     * --------------------------------------------------------------------- */
    @Test
    void walk_NoChildSchemas_ElseBranchReturnsEmpty() throws Exception {
        // Replace the internal schemas list with an empty list.
        setSchemas(validator, Collections.emptyList());
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, instanceLocation, false);
        assertTrue(result.isEmpty(), "Empty schemas list should yield an empty result set");
        // No child schema interactions should happen.
        verifyNoInteractions(childSchema1, childSchema2);
    }

    /* --------------------------------------------------------------------- *
     *  5. Null schemas list → NullPointerException
     * --------------------------------------------------------------------- */
    @Test
    void walk_NullSchemasList_ThrowsNullPointerException() {
        // force a null internal list
        setSchemas(validator, null);
        assertThrows(NullPointerException.class, () -> validator.walk(executionContext, node, rootNode, instanceLocation, false), "Iterating over a null schemas list must throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  6. Null child schema element → NullPointerException
     * --------------------------------------------------------------------- */
    @Test
    void walk_NullChildSchemaInList_ThrowsNullPointerException() {
        List<JsonSchema> listWithNull = new ArrayList<>();
        // first element is null
        listWithNull.add(null);
        // second element is a valid mock
        listWithNull.add(childSchema1);
        setSchemas(validator, listWithNull);
        assertThrows(NullPointerException.class, () -> validator.walk(executionContext, node, rootNode, instanceLocation, false), "Calling walk on a null child schema should throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  7. validate method throws RuntimeException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void walk_ValidateMethodThrowsRuntimeException() {
        // Subclass that overrides the protected validate method to throw.
        class ThrowingValidator extends OneOfValidator {

            ThrowingValidator(SchemaLocation loc, JsonNodePath path, JsonNode schemaNode, JsonSchema parent, ValidationContext ctx) {
                super(loc, path, schemaNode, parent, ctx);
            }

            @Override
            protected Set<ValidationMessage> validate(ExecutionContext ec, JsonNode n, JsonNode r, JsonNodePath i, boolean w) {
                throw new RuntimeException("validation failed");
            }
        }
        // Build a minimal validator instance (the constructor logic is unchanged).
        JsonNode schemaArrayNode = mock(JsonNode.class);
        when(schemaArrayNode.isArray()).thenReturn(true);
        // no child schemas needed
        when(schemaArrayNode.size()).thenReturn(0);
        SchemaLocation dummyLocation = mock(SchemaLocation.class);
        JsonNodePath dummyPath = mock(JsonNodePath.class);
        ThrowingValidator throwingValidator = new ThrowingValidator(dummyLocation, dummyPath, schemaArrayNode, null, validationContext);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> throwingValidator.walk(executionContext, node, rootNode, instanceLocation, true), "Exception from validate should propagate");
        assertEquals("validation failed", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  8. Child schema walk throws RuntimeException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void walk_ChildSchemaWalkThrowsRuntimeException() {
        when(childSchema1.walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false))).thenThrow(new RuntimeException("child walk failed"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.walk(executionContext, node, rootNode, instanceLocation, false), "Exception from child schema walk should propagate");
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
        when(childSchema1.walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema3.walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false))).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, instanceLocation, false);
        assertTrue(result.isEmpty(), "Result should be empty when child walks return empty sets");
        verify(childSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(childSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(childSchema3, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  10. validate returns empty set (still true branch) → empty result
     * --------------------------------------------------------------------- */
    @Test
    void walk_ValidateReturnsEmptySet() {
        OneOfValidator spyValidator = Mockito.spy(validator);
        Set<ValidationMessage> emptySet = Collections.emptySet();
        doReturn(emptySet).when(spyValidator).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode, instanceLocation, true);
        assertTrue(result.isEmpty(), "When validate returns empty, walk should also return empty");
        verify(spyValidator, times(1)).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        verifyNoInteractions(childSchema1, childSchema2);
    }

    /* --------------------------------------------------------------------- *
     *  11. Ensure child schemas are NOT invoked when validation path is taken
     * --------------------------------------------------------------------- */
    @Test
    void walk_DoNotInvokeChildSchemasWhenValidatingItself() {
        OneOfValidator spyValidator = Mockito.spy(validator);
        Set<ValidationMessage> dummySet = Collections.singleton(mock(ValidationMessage.class));
        doReturn(dummySet).when(spyValidator).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode, instanceLocation, true);
        assertEquals(dummySet, result);
        verify(spyValidator, times(1)).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // No walk on child schemas should have happened.
        verifyNoInteractions(childSchema1, childSchema2);
    }
}
