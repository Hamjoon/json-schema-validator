package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    private JsonSchema parentSchema;

    private JsonSchema childSchema1;

    private JsonSchema childSchema2;

    private OneOfValidator validator;

    private ExecutionContext executionContext;

    private CollectorContext collectorContext;

    private Scope grandParentScope;

    private ValidatorState validatorState;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* ---------- Mocks for the construction of OneOfValidator ---------- */
        validationContext = mock(ValidationContext.class);
        parentSchema = mock(JsonSchema.class);
        childSchema1 = mock(JsonSchema.class);
        childSchema2 = mock(JsonSchema.class);
        // ValidationContext#newSchema is called twice (once per array element)
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(childSchema1, childSchema2);
        // Build a dummy schema node that looks like an array with two elements
        ArrayNode schemaArray = MAPPER.createArrayNode();
        // first child schema placeholder
        schemaArray.addObject();
        // second child schema placeholder
        schemaArray.addObject();
        // Create the validator under test
        validator = new OneOfValidator("testPath", schemaArray, parentSchema, validationContext);
        /* ---------- Mocks for the execution context used during validation ---------- */
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // Scopes – one for the grand‑parent and one per child iteration (2 children)
        grandParentScope = mock(Scope.class);
        Scope childScope1 = mock(Scope.class);
        Scope childScope2 = mock(Scope.class);
        when(collectorContext.enterDynamicScope()).thenReturn(// first call – grand parent
        grandParentScope).thenReturn(// first child iteration
        childScope1).thenReturn(// second child iteration
        childScope2);
        // exitDynamicScope – we simply return a fresh mock each time (its value is not used)
        when(collectorContext.exitDynamicScope()).thenReturn(mock(Scope.class));
        // ValidatorState stored inside the CollectorContext
        validatorState = mock(ValidatorState.class);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // Default behaviour for the state used by OneOfValidator
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // The collections cleared after a failure – return mutable sets
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
    }

    /**
     * Helper that creates a simple empty JSON node ({}).
     */
    private JsonNode emptyNode() {
        return MAPPER.createObjectNode();
    }

    /**
     * Utility to replace the internal {@code schemas} list with the provided mocks.
     */
    private void setInternalSchemas(JsonSchema... schemas) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, new ArrayList<>(Arrays.asList(schemas)));
    }

    /**
     * Utility to enable/disable the {@code failFast} flag via reflection.
     */
    private void setFailFast(boolean failFast) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.set(validator, failFast);
    }

    @Test
    void validate_WhenExactlyOneSchemaMatches_ReturnsNoErrors() {
        // childSchema1 validates successfully, childSchema2 returns an error
        when(childSchema1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(childSchema2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(mock(ValidationMessage.class)));
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        // Only the error from the second schema should be present (OneOf error not triggered)
        assertEquals(1, result.size(), "When exactly one schema matches, only the child errors should be returned.");
    }

    @Test
    void validate_WhenZeroOrMoreThanOneSchemaMatches_ReturnsOneOfError() {
        // ---- Scenario 1: zero matching schemas ----
        when(childSchema1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(mock(ValidationMessage.class)));
        when(childSchema2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(mock(ValidationMessage.class)));
        Set<ValidationMessage> zeroMatch = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(zeroMatch.isEmpty(), "Zero matching schemas must produce a OneOf error.");
        // The OneOf error message is the first element (buildValidationMessage) – we only check count
        assertTrue(zeroMatch.size() >= 1, "At least the OneOf error should be present.");
        // ---- Scenario 2: two matching schemas ----
        // Reset the mocks for a fresh call
        reset(childSchema1, childSchema2);
        when(childSchema1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(childSchema2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> twoMatch = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(twoMatch.isEmpty(), "More than one matching schema must produce a OneOf error.");
        // The set should contain the OneOf error plus any child errors (none in this case)
        assertEquals(1, twoMatch.size(), "When two schemas match, only the OneOf error should be reported.");
    }

    /* ---------------------------------------------------------
     *  1. Walk disabled – exactly one schema matches
     * --------------------------------------------------------- */
    @Test
    void validate_WalkDisabled_ExactlyOneSchemaMatches_ReturnsNoErrors() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        setInternalSchemas(s1, s2);
        // first schema validates → empty set, second → error set
        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(mock(ValidationMessage.class)));
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        // Only the error from the second (invalid) schema should be present.
        assertEquals(1, result.size(), "When exactly one schema matches, the OneOf validator must return the child errors only.");
        // matchedNode should be set to true by the validator (no explicit check – just ensure no exception)
    }

    /* ---------------------------------------------------------
     *  2. Walk disabled – zero matching schemas
     * --------------------------------------------------------- */
    @Test
    void validate_WalkDisabled_NoSchemaMatches_ReturnsOneOfError() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        setInternalSchemas(s1, s2);
        ValidationMessage childErr1 = mock(ValidationMessage.class);
        ValidationMessage childErr2 = mock(ValidationMessage.class);
        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(childErr1));
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(childErr2));
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(), "Zero matching schemas must produce a OneOf error.");
        // the set must contain the OneOf message plus both child errors
        assertTrue(result.contains(childErr1) && result.contains(childErr2), "Child errors must be part of the returned set.");
        // evaluated collections must be cleared
        assertTrue(collectorContext.getEvaluatedItems().isEmpty() && collectorContext.getEvaluatedProperties().isEmpty(), "Evaluated items/properties should be cleared after a failure.");
    }

    /* ---------------------------------------------------------
     *  3. Walk disabled – matchedNode flag false after validation
     * --------------------------------------------------------- */
    @Test
    void validate_WalkDisabled_MatchedNodeFalse_IgnoresEmptySchemaErrors() throws Exception {
        JsonSchema s = mock(JsonSchema.class);
        setInternalSchemas(s);
        // schema validates successfully (empty set)
        when(s.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        // but the state reports that no node was matched
        when(validatorState.hasMatchedNode()).thenReturn(false);
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(), "Because matchedNode is false, validator should treat schema as non‑matching.");
        // only the OneOf error should be present (no child errors)
        assertEquals(1, result.size(), "When matchedNode is false, exactly one OneOf error must be reported.");
    }

    /* ---------------------------------------------------------
     *  4. Walk enabled – exactly one schema matches
     * --------------------------------------------------------- */
    @Test
    void validate_WalkEnabled_ExactlyOneSchemaMatches_ReturnsNoErrors() throws Exception {
        // enable walk
        when(validatorState.isWalkEnabled()).thenReturn(true);
        JsonSchema s = mock(JsonSchema.class);
        setInternalSchemas(s);
        when(s.walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), anyBoolean())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertTrue(result.isEmpty(), "One matching schema with walk enabled must produce no errors.");
    }

    /* ---------------------------------------------------------
     *  5. Walk enabled – matchedNode false after walk
     * --------------------------------------------------------- */
    @Test
    void validate_WalkEnabled_MatchedNodeFalse_IgnoresEmptyWalkResult() throws Exception {
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        JsonSchema s = mock(JsonSchema.class);
        setInternalSchemas(s);
        when(s.walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), anyBoolean())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(), "Validator must treat the schema as non‑matching because matchedNode is false.");
        assertEquals(1, result.size(), "Only the OneOf error should be present when walk succeeds but matchedNode is false.");
    }

    /* ---------------------------------------------------------
     *  6. Walk enabled – validation disabled (state.isValidationEnabled == false)
     * --------------------------------------------------------- */
    @Test
    void validate_WalkEnabled_ValidationDisabled_SchemaWalkCalledWithFalseFlag() throws Exception {
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.isValidationEnabled()).thenReturn(false);
        JsonSchema s = mock(JsonSchema.class);
        setInternalSchemas(s);
        when(s.walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false))).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertTrue(result.isEmpty(), "With validation disabled, an empty walk result should still be considered a match.");
    }

    /* ---------------------------------------------------------
     *  7. Multiple valid schemas – failFast = false
     * --------------------------------------------------------- */
    @Test
    void validate_MultipleValidSchemas_FailFastFalse_ReturnsOneOfError() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        setInternalSchemas(s1, s2, s3);
        // all schemas succeed
        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(s3.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        // ensure failFast is false (default is false, explicitly set for clarity)
        setFailFast(false);
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(), "More than one matching schema must produce a OneOf error when failFast is false.");
        assertEquals(1, result.size(), "When multiple schemas match and failFast is false, only the OneOf message should be present.");
    }

    /* ---------------------------------------------------------
     *  8. Multiple valid schemas – failFast = true
     * --------------------------------------------------------- */
    @Test
    void validate_MultipleValidSchemas_FailFastTrue_ThrowsJsonSchemaException() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        setInternalSchemas(s1, s2);
        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        setFailFast(true);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> validator.validate(executionContext, emptyNode(), emptyNode(), "#"), "When failFast is true and multiple schemas match, an exception must be thrown.");
        assertNotNull(ex.getMessage(), "Exception should contain a message describing the OneOf error.");
    }
}
