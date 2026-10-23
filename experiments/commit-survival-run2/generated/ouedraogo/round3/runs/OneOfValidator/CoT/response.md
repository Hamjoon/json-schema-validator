**1. Public methods of `OneOfValidator`**

| Visibility | Return type | Method name | Parameter list |
|------------|-------------|-------------|----------------|
| `public` | `OneOfValidator` (constructor) | `OneOfValidator` | `(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)` |
| `public` | `Set<ValidationMessage>` | `validate` | `(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at)` |
| `public` | `Set<ValidationMessage>` | `walk` | `(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)` |
| `public` | `void` | `preloadJsonSchema` | `()` |

---

**2. Identified edge‑case / exception scenarios**

| Scenario | Why it is an edge case | What should be verified |
|----------|-----------------------|------------------------|
| **Empty `schemaNode` array** – the validator is built with zero child schemas. | The loop that creates `schemas` will add none; validation must treat “zero valid schemas” as a failure. | `validate` returns a `ValidationMessage` indicating `0` valid schemas. |
| **Exactly one child schema validates** – the happy‑path. | The validator must return an empty error set and set `matchedNode` to `true`. | Result set is empty; `ValidatorState.setMatchedNode(true)` is called. |
| **No child schema validates** – all child schemas return errors. | `numberOfValidSchema == 0` → a “one‑of” error must be added together with the child errors. | Result contains the built “one‑of” message **and** the child errors. |
| **More than one child schema validates** – ambiguous data. | `numberOfValidSchema > 1` → a “one‑of” error must be added. | Result contains the built “one‑of” message; child error set may be empty. |
| **`failFast` flag is `true`** – the validator should throw `JsonSchemaException` as soon as the “one‑of” condition fails. | Guarantees early termination. | `validate` throws `JsonSchemaException`. |
| **`walk` with `shouldValidateSchema = true`** – should delegate to `validate`. | Guarantees that `walk` does not duplicate logic. | Returned set equals the set returned by `validate`. |
| **`walk` with `shouldValidateSchema = false`** – should invoke `walk` on each child schema. | Makes sure the validator respects the “walk‑only” mode. | Each child schema’s `walk` method is called exactly once. |
| **`preloadJsonSchema`** – must initialise every child schema. | Guarantees that the pre‑loading step is propagated. | `initializeValidators()` is invoked on every child schema. |

---

**3. Complete JUnit 4 test file (`OneOfValidatorTest.java`)**

```java
###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // ----- Mocks used by many tests -------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope mockScope;                // used for every dynamic scope
    @Mock private Scope mockParentScope;          // scope created for each child schema

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Before
    public void setUp() {
        // ExecutionContext → CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext dynamic scopes (enter/exit)
        when(collectorContext.enterDynamicScope()).thenReturn(mockScope);
        when(collectorContext.exitDynamicScope()).thenReturn(mockScope);
        when(collectorContext.enterDynamicScope()).thenReturn(mockParentScope); // second call (parent) – same mock works

        // CollectorContext stores ValidatorState under the well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // Default behaviour of the validator state – walk disabled, matchedNode true
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // The state is mutated in the code; we simply let the calls be no‑ops
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
        when(validatorState.hasMatchedNode()).thenReturn(true);
    }

    /** Helper to build an `ArrayNode` from a list of JsonNode objects. */
    private ArrayNode buildArrayNode(JsonNode... elements) {
        ArrayNode array = MAPPER.createArrayNode();
        for (JsonNode n : elements) {
            array.add(n);
        }
        return array;
    }

    /** Helper to create a `OneOfValidator` with a given array of child schemas. */
    private OneOfValidator createValidator(JsonSchema... childSchemas) {
        // Build a dummy schemaNode (an array with as many elements as childSchemas)
        ArrayNode schemaNode = MAPPER.createArrayNode();
        for (int i = 0; i < childSchemas.length; i++) {
            schemaNode.add(MAPPER.createObjectNode()); // content is irrelevant – ValidationContext will be mocked
        }

        // Mock ValidationContext#newSchema to return the supplied child schemas in order
        for (int i = 0; i < childSchemas.length; i++) {
            final int idx = i;
            when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                    .thenAnswer(invocation -> childSchemas[idx]);
        }

        return new OneOfValidator("root", schemaNode, null, validationContext);
    }

    // -------------------------------------------------------------------------
    // 1. Basic happy‑path: exactly one child schema validates (no errors)
    // -------------------------------------------------------------------------
    @Test
    public void testValidate_ExactlyOneValidSchema() {
        JsonSchema schemaValid = mock(JsonSchema.class);
        JsonSchema schemaInvalid = mock(JsonSchema.class);

        // schemaValid returns empty error set → valid
        when(schemaValid.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // schemaInvalid returns a non‑empty set → invalid
        ValidationMessage childError = ValidationMessage.builder()
                .code("error")
                .path("$")
                .message("invalid")
                .build();
        when(schemaInvalid.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(childError));

        OneOfValidator validator = createValidator(schemaValid, schemaInvalid);

        Set<ValidationMessage> result = validator.validate(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$");

        // No errors should be reported
        assertTrue("Result should be empty when exactly one schema matches", result.isEmpty());

        // The validator must have signalled a successful match
        verify(validatorState, atLeastOnce()).setMatchedNode(true);
    }

    // -------------------------------------------------------------------------
    // 2. No child schema validates
    // -------------------------------------------------------------------------
    @Test
    public void testValidate_NoValidSchemas() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        ValidationMessage err1 = ValidationMessage.builder().code("e1").path("$").message("err1").build();
        ValidationMessage err2 = ValidationMessage.builder().code("e2").path("$").message("err2").build();

        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(err1));
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(err2));

        OneOfValidator validator = createValidator(schema1, schema2);

        Set<ValidationMessage> result = validator.validate(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$");

        // Must contain the “oneOf” message (built by the validator) + both child errors
        assertEquals("Should contain three messages (oneOf + two child errors)", 3, result.size());

        // Verify that each child error is present
        assertTrue(result.contains(err1));
        assertTrue(result.contains(err2));
    }

    // -------------------------------------------------------------------------
    // 3. More than one child schema validates → ambiguous
    // -------------------------------------------------------------------------
    @Test
    public void testValidate_MultipleValidSchemas() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);

        when(schemaA.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = createValidator(schemaA, schemaB);

        Set<ValidationMessage> result = validator.validate(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$");

        // Because two schemas matched, we must get a oneOf error (no child errors)
        assertEquals("Only the oneOf validation message should be present", 1, result.size());
        ValidationMessage onlyMsg = result.iterator().next();
        assertTrue("Message should contain the number of valid schemas (2)",
                onlyMsg.getMessage().contains("2"));
    }

    // -------------------------------------------------------------------------
    // 4. failFast = true → JsonSchemaException is thrown as soon as the condition fails
    // -------------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testValidate_FailFastThrowsException() throws Exception {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);

        when(schemaA.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = createValidator(schemaA, schemaB);

        // Use reflection to flip the protected `failFast` flag defined in BaseJsonValidator
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // This call must throw JsonSchemaException
        validator.validate(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$");
    }

    // -------------------------------------------------------------------------
    // 5. walk(…, shouldValidateSchema = true) delegates to validate()
    // -------------------------------------------------------------------------
    @Test
    public void testWalk_ShouldValidateTrue() {
        JsonSchema child = mock(JsonSchema.class);
        OneOfValidator validator = createValidator(child);

        // Spy the validator to intercept the call to validate()
        OneOfValidator spy = Mockito.spy(validator);

        Set<ValidationMessage> fakeResult = Collections.singleton(
                ValidationMessage.builder().code("x").path("$").message("walked").build());

        doReturn(fakeResult).when(spy).validate(executionContext,
                any(JsonNode.class), any(JsonNode.class), anyString());

        Set<ValidationMessage> walkResult = spy.walk(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$", true);

        assertEquals(fakeResult, walkResult);
        verify(spy, times(1)).validate(executionContext,
                any(JsonNode.class), any(JsonNode.class), anyString());
    }

    // -------------------------------------------------------------------------
    // 6. walk(…, shouldValidateSchema = false) calls walk() on each child schema
    // -------------------------------------------------------------------------
    @Test
    public void testWalk_ShouldValidateFalse() {
        JsonSchema child1 = mock(JsonSchema.class);
        JsonSchema child2 = mock(JsonSchema.class);

        OneOfValidator validator = createValidator(child1, child2);

        // When shouldValidateSchema is false the result set must be empty
        Set<ValidationMessage> result = validator.walk(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$", false);

        assertTrue("Result must be empty when validation is disabled", result.isEmpty());

        // Verify each child schema's walk() method was invoked exactly once
        verify(child1, times(1)).walk(executionContext,
                any(JsonNode.class), any(JsonNode.class), anyString(), eq(false));
        verify(child2, times(1)).walk(executionContext,
                any(JsonNode.class), any(JsonNode.class), anyString(), eq(false));
    }

    // -------------------------------------------------------------------------
    // 7. preloadJsonSchema() must initialise every child schema
    // -------------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema_InitialisesAllChildren() {
        JsonSchema childA = mock(JsonSchema.class);
        JsonSchema childB = mock(JsonSchema.class);
        JsonSchema childC = mock(JsonSchema.class);

        OneOfValidator validator = createValidator(childA, childB, childC);

        validator.preloadJsonSchema();

        verify(childA, times(1)).initializeValidators();
        verify(childB, times(1)).initializeValidators();
        verify(childC, times(1)).initializeValidators();
    }

    // -------------------------------------------------------------------------
    // 8. Edge case: validator built with an empty schema array
    // -------------------------------------------------------------------------
    @Test
    public void testValidate_WithNoChildSchemas() {
        // Empty array → validator will have zero child schemas
        OneOfValidator validator = createValidator(); // no arguments

        Set<ValidationMessage> result = validator.validate(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), "$");

        // As per the logic, numberOfValidSchema == 0 → a oneOf error is produced
        assertEquals("Exactly one oneOf error should be reported", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("0"));
    }
}
###Test END##