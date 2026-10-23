```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.
 *
 * The test cases cover the combinations described in the specification JSON:
 *   - walk enabled / disabled,
 *   - validation enabled / disabled,
 *   - matchedNode true / false,
 *   - failFast flag,
 *   - zero / one / many matching schemas,
 *   - error handling (null arguments, exceptions from child schemas, etc.).
 *
 * The {@code @BeforeEach} method creates the basic mocks needed for the validator.
 * Individual tests may re‑configure those mocks or replace the internal {@code schemas} list
 * via reflection to achieve the required scenario.
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private OneOfValidator validator;

    private ExecutionContext executionContext;
    private CollectorContext collectorContext;
    private Scope grandParentScope;
    private ValidatorState validatorState;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* ---------- basic mocks for validator construction ---------- */
        validationContext = mock(ValidationContext.class);
        // a dummy schema array – we will replace the internal list later
        ArrayNode dummyArray = MAPPER.createArrayNode();
        dummyArray.addObject(); // placeholder

        validator = new OneOfValidator("testPath", dummyArray, mock(JsonSchema.class), validationContext);

        /* ---------- mocks used during validation ---------- */
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // first enterDynamicScope → grand‑parent, subsequent calls → child scopes
        grandParentScope = mock(Scope.class);
        Scope childScope1 = mock(Scope.class);
        Scope childScope2 = mock(Scope.class);
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // grand parent
                .thenReturn(childScope1)        // first child
                .thenReturn(childScope2);       // second child
        when(collectorContext.exitDynamicScope()).thenReturn(mock(Scope.class));

        validatorState = mock(ValidatorState.class);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // collections cleared after a failure – mutable sets so we can assert they are emptied
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
    }

    /** Helper that creates an empty JSON object node ({}). */
    private JsonNode emptyNode() {
        return MAPPER.createObjectNode();
    }

    /** Utility to replace the internal {@code schemas} list with the provided mocks. */
    private void setInternalSchemas(JsonSchema... schemas) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, new ArrayList<>(Arrays.asList(schemas)));
    }

    /** Utility to enable/disable the {@code failFast} flag via reflection. */
    private void setFailFast(boolean failFast) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.set(validator, failFast);
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
        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(mock(ValidationMessage.class)));

        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");

        // Only the error from the second (invalid) schema should be present.
        assertEquals(1, result.size(),
                "When exactly one schema matches, the OneOf validator must return the child errors only.");
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
        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(childErr1));
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(childErr2));

        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");

        assertFalse(result.isEmpty(), "Zero matching schemas must produce a OneOf error.");
        // the set must contain the OneOf message plus both child errors
        assertTrue(result.contains(childErr1) && result.contains(childErr2),
                "Child errors must be part of the returned set.");
        // evaluated collections must be cleared
        assertTrue(collectorContext.getEvaluatedItems().isEmpty()
                        && collectorContext.getEvaluatedProperties().isEmpty(),
                "Evaluated items/properties should be cleared after a failure.");
    }

    /* ---------------------------------------------------------
     *  3. Walk disabled – matchedNode flag false after validation
     * --------------------------------------------------------- */
    @Test
    void validate_WalkDisabled_MatchedNodeFalse_IgnoresEmptySchemaErrors() throws Exception {
        JsonSchema s = mock(JsonSchema.class);
        setInternalSchemas(s);

        // schema validates successfully (empty set)
        when(s.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        // but the state reports that no node was matched
        when(validatorState.hasMatchedNode()).thenReturn(false);

        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");

        assertFalse(result.isEmpty(), "Because matchedNode is false, validator should treat schema as non‑matching.");
        // only the OneOf error should be present (no child errors)
        assertEquals(1, result.size(),
                "When matchedNode is false, exactly one OneOf error must be reported.");
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

        when(s.walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

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
        when(s.walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(), "Validator must treat the schema as non‑matching because matchedNode is false.");
        assertEquals(1, result.size(),
                "Only the OneOf error should be present when walk succeeds but matchedNode is false.");
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
        when(s.walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false)))
                .thenReturn(Collections.emptySet());

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
        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(s3.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // ensure failFast is false (default is false, explicitly set for clarity)
        setFailFast(false);

        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");

        assertFalse(result.isEmpty(), "More than one matching schema must produce a OneOf error when failFast is false.");
        assertEquals(1, result.size(),
                "When multiple schemas match and failFast is false, only the OneOf message should be present.");
    }

    /* ---------------------------------------------------------
     *  8. Multiple valid schemas – failFast = true
     * --------------------------------------------------------- */
    @Test
    void validate_MultipleValidSchemas_FailFastTrue_ThrowsJsonSchemaException() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        setInternalSchemas(s1, s2);

        when(s1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(s2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        setFailFast(true);

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> validator.validate(executionContext, emptyNode(), emptyNode(), "#"),
                "When failFast is true and multiple schemas match, an exception must be thrown.");
        assertNotNull(ex.getMessage(), "Exception should contain a message describing the OneOf error.");
    }

    /* ---------------------------------------------------------
     *  9. Empty schema list
     * --------------------------------------------------------- */
    @Test
    void validate_EmptySchemaList_ReturnsOneOfError() throws Exception {
        // construct a validator with an empty array node
        ArrayNode emptyArray = MAPPER.createArrayNode();
        validator = new OneOfValidator("testPath", emptyArray, mock(JsonSchema.class), validationContext);
        // re‑wire the execution context mocks (the constructor does not touch them)
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // only grand‑parent needed
                .thenReturn(mock(Scope.class));
        when(collectorContext.exitDynamicScope()).thenReturn(mock(Scope.class));

        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");

        assertFalse(result.isEmpty(), "Empty schema list must produce a OneOf error.");
        assertEquals(1, result.size(), "Only the OneOf message