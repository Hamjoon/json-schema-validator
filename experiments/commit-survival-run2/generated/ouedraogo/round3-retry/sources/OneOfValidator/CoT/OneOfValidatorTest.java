package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    @Mock ExecutionContext execCtx;
    @Mock JsonNode node;
    @Mock JsonNode rootNode;
    @Mock CollectorContext collectorContext;
    @Mock ValidatorState state;
    @Mock Scope dummyScope;               // used for both enter/exit

    @Before
    public void setUp() {
        // deep‑stub the ExecutionContext → CollectorContext hierarchy
        when(execCtx.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);
        when(state.isWalkEnabled()).thenReturn(false);
        when(state.isValidationEnabled()).thenReturn(true);
        when(state.hasMatchedNode()).thenReturn(true);
        when(state.isMatchedNode()).thenReturn(true);
        when(state.isComplexValidator()).thenReturn(false);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
    }

    /**
     * Helper that builds a OneOfValidator with the supplied child schemas.
     */
    private OneOfValidator buildValidator(JsonSchema... childSchemas) {
        JsonNode schemaArray = mock(JsonNode.class);
        when(schemaArray.size()).thenReturn(childSchemas.length);
        for (int i = 0; i < childSchemas.length; i++) {
            JsonNode child = mock(JsonNode.class);
            when(schemaArray.get(i)).thenReturn(child);
        }

        ValidationContext vc = mock(ValidationContext.class);
        // each call to newSchema returns the corresponding child schema
        Answer<JsonSchema> answer = new Answer<JsonSchema>() {
            private int idx = 0;
            @Override
            public JsonSchema answer(InvocationOnMock invocation) {
                return childSchemas[idx++];
            }
        };
        when(vc.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(answer);

        return new OneOfValidator("path", schemaArray, mock(JsonSchema.class), vc);
    }

    /**
     * Utility to flip the protected `failFast` flag via reflection.
     */
    private void setFailFast(OneOfValidator validator, boolean value) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("failFast");
            f.setAccessible(true);
            f.setBoolean(validator, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility to read a private field via reflection (used in the constructor test).
     */
    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------------
    //  Basic public‑method tests
    // --------------------------------------------------------------------
    @Test
    public void constructorCreatesSchemasForEachChildNode() {
        JsonNode schemaArray = mock(JsonNode.class);
        when(schemaArray.size()).thenReturn(2);
        when(schemaArray.get(0)).thenReturn(mock(JsonNode.class));
        when(schemaArray.get(1)).thenReturn(mock(JsonNode.class));

        ValidationContext vc = mock(ValidationContext.class);
        when(vc.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(mock(JsonSchema.class));

        OneOfValidator validator = new OneOfValidator("path", schemaArray,
                mock(JsonSchema.class), vc);

        List<JsonSchema> internalSchemas = getField(validator, "schemas");
        assertEquals(2, internalSchemas.size());
    }

    @Test
    public void validateExactlyOneMatchingSchemaReturnsNoErrors() {
        JsonSchema matching = mock(JsonSchema.class);
        JsonSchema nonMatching = mock(JsonSchema.class);
        when(matching.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(nonMatching.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));

        OneOfValidator validator = buildValidator(matching, nonMatching);
        setFailFast(validator, false);

        Set<ValidationMessage> errors = validator.validate(execCtx, node, rootNode, "#");
        assertTrue(errors.isEmpty());
        verify(state, times(1)).setMatchedNode(true);
    }

    @Test
    public void walkDelegatesToValidateWhenShouldValidateTrue() {
        OneOfValidator validator = spy(buildValidator());
        Set<ValidationMessage> dummy = Collections.singleton(mock(ValidationMessage.class));
        doReturn(dummy).when(validator).validate(any(), any(), any(), any());

        Set<ValidationMessage> result = validator.walk(execCtx, node, rootNode, "#", true);
        assertSame(dummy, result);
        verify(validator, times(1)).validate(execCtx, node, rootNode, "#");
    }

    @Test
    public void preloadJsonSchemaInitialisesAllChildSchemas() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        OneOfValidator validator = buildValidator(s1, s2);

        validator.preloadJsonSchema();

        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    // --------------------------------------------------------------------
    //  Edge‑case / exception tests
    // --------------------------------------------------------------------
    @Test
    public void validateWithNoSchemasReturnsOneOfError() {
        JsonNode emptyArray = mock(JsonNode.class);
        when(emptyArray.size()).thenReturn(0);
        ValidationContext vc = mock(ValidationContext.class);
        OneOfValidator validator = new OneOfValidator("p", emptyArray,
                mock(JsonSchema.class), vc);
        setFailFast(validator, false);

        Set<ValidationMessage> errors = validator.validate(execCtx, node, rootNode, "#");
        assertEquals(1, errors.size());
        ValidationMessage msg = errors.iterator().next();
        // The exact message format depends on the library; we check the key part
        assertEquals("ONE_OF", msg.getType());
        assertEquals("0", msg.getMessage());
    }

    @Test
    public void validateMultipleMatchingSchemasReturnsOneOfErrorWithChildErrors() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        when(s1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(s2.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(s1, s2);
        setFailFast(validator, false);

        Set<ValidationMessage> errors = validator.validate(execCtx, node, rootNode, "#");
        // At least the OneOf error must be present
        assertTrue(errors.stream().anyMatch(m -> "ONE_OF".equals(m.getType())));
    }

    @Test(expected = JsonSchemaException.class)
    public void validateFailFastThrowsExceptionWhenNotExactlyOneMatch() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        when(s1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(s2.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(s1, s2);
        setFailFast(validator, true);

        validator.validate(execCtx, node, rootNode, "#");
    }

    @Test
    public void validateUsesWalkWhenStateWalkEnabled() {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.walk(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(schema);
        when(state.isWalkEnabled()).thenReturn(true);
        when(state.isValidationEnabled()).thenReturn(true);

        Set<ValidationMessage> errors = validator.validate(execCtx, node, rootNode, "#");
        assertTrue(errors.isEmpty());
        verify(schema, times(1)).walk(execCtx, node, rootNode, "#", true);
    }

    @Test
    public void walkWithoutValidationInvokesChildWalksOnly() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        OneOfValidator validator = buildValidator(s1, s2);

        Set<ValidationMessage> result = validator.walk(execCtx, node, rootNode, "#", false);
        assertTrue(result.isEmpty());

        verify(s1, times(1)).walk(execCtx, node, rootNode, "#", false);
        verify(s2, times(1)).walk(execCtx, node, rootNode, "#", false);
    }

    @Test
    public void preloadJsonSchemaWithNoChildrenDoesNotThrow() {
        JsonNode emptyArray = mock(JsonNode.class);
        when(emptyArray.size()).thenReturn(0);
        ValidationContext vc = mock(ValidationContext.class);
        OneOfValidator validator = new OneOfValidator("p", emptyArray,
                mock(JsonSchema.class), vc);
        // Should simply complete without NPE
        validator.preloadJsonSchema();
    }
}
