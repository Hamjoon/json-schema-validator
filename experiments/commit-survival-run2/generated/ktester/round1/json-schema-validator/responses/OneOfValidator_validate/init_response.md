```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* ---------- Mocks for the construction of OneOfValidator ---------- */
        validationContext = mock(ValidationContext.class);
        parentSchema = mock(JsonSchema.class);
        childSchema1 = mock(JsonSchema.class);
        childSchema2 = mock(JsonSchema.class);

        // ValidationContext#newSchema is called twice (once per array element)
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1, childSchema2);

        // Build a dummy schema node that looks like an array with two elements
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject(); // first child schema placeholder
        schemaArray.addObject(); // second child schema placeholder

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
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // first call – grand parent
                .thenReturn(childScope1)        // first child iteration
                .thenReturn(childScope2);       // second child iteration

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

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /** Helper that creates a simple empty JSON node ({}). */
    private JsonNode emptyNode() {
        return MAPPER.createObjectNode();
    }

    @Test
    void validate_WhenExactlyOneSchemaMatches_ReturnsNoErrors() {
        // childSchema1 validates successfully, childSchema2 returns an error
        when(childSchema1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(mock(ValidationMessage.class)));

        Set<ValidationMessage> result = validator.validate(executionContext,
                emptyNode(), emptyNode(), "#");

        // Only the error from the second schema should be present (OneOf error not triggered)
        assertEquals(1, result.size(),
                "When exactly one schema matches, only the child errors should be returned.");
    }

    @Test
    void validate_WhenZeroOrMoreThanOneSchemaMatches_ReturnsOneOfError() {
        // ---- Scenario 1: zero matching schemas ----
        when(childSchema1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(mock(ValidationMessage.class)));
        when(childSchema2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Set.of(mock(ValidationMessage.class)));

        Set<ValidationMessage> zeroMatch = validator.validate(executionContext,
                emptyNode(), emptyNode(), "#");

        assertFalse(zeroMatch.isEmpty(), "Zero matching schemas must produce a OneOf error.");
        // The OneOf error message is the first element (buildValidationMessage) – we only check count
        assertTrue(zeroMatch.size() >= 1, "At least the OneOf error should be present.");

        // ---- Scenario 2: two matching schemas ----
        // Reset the mocks for a fresh call
        reset(childSchema1, childSchema2);
        when(childSchema1.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema2.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> twoMatch = validator.validate(executionContext,
                emptyNode(), emptyNode(), "#");

        assertFalse(twoMatch.isEmpty(), "More than one matching schema must produce a OneOf error.");
        // The set should contain the OneOf error plus any child errors (none in this case)
        assertEquals(1, twoMatch.size(),
                "When two schemas match, only the OneOf error should be reported.");
    }
}
```