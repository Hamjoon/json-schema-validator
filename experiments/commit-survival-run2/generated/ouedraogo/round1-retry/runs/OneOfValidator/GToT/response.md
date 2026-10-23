**Step 1 – Public API of `OneOfValidator`**

| Modifier | Return type | Method | Parameters | Description |
|----------|-------------|--------|------------|-------------|
| `public` | `OneOfValidator` | **constructor** | `(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)` | Builds a validator from an array‑node containing the *oneOf* subschemas. |
| `public` | `Set<ValidationMessage>` | **validate** | `(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at)` | Checks that **exactly one** subschema validates the supplied JSON node. |
| `public` | `Set<ValidationMessage>` | **walk** | `(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)` | Delegates to `validate` when `shouldValidateSchema` is `true`; otherwise walks each subschema without collecting messages. |
| `public` | `void` | **preloadJsonSchema** | `()` | Calls `initializeValidators()` on every subschema – used for eager loading. |

---

**Step 2 – Basic JUnit 4 test ideas (one per method)**  

| Method | Basic test case (what we verify) |
|--------|----------------------------------|
| **constructor** | After construction the internal `schemas` list contains the same number of elements as the supplied array‑node. |
| **validate** | When **exactly one** subschema returns an empty error set, `validate` returns an empty set and the `ValidatorState` is marked as matched. |
| **walk** | With `shouldValidateSchema = true` the method returns the same result as `validate`; with `false` it invokes `walk` on each subschema and returns an empty set. |
| **preloadJsonSchema** | Every subschema receives a call to `initializeValidators()`. |

---

**Step 3 – Edge‑cases & exception scenarios that must be covered**

| Scenario | Why it matters | Expected behaviour |
|----------|----------------|--------------------|
| **No subschemas** (`schemaNode` is empty array) | “oneOf” with zero alternatives should always fail. | `validate` returns a *OneOf* error (`numberOfValidSchema = 0`). |
| **More than one matching subschema** | Violates the “exactly one” rule. | `validate` returns a *OneOf* error (`numberOfValidSchema > 1`). |
| **All subschemas fail** | Normal failure path – child errors must be bubbled up. | `validate` returns a *OneOf* error plus all child errors. |
| **`failFast` flag set** | Library can be configured to throw on first error. | `validate` throws `JsonSchemaException` containing the *OneOf* message. |
| **`state.isWalkEnabled()` true** | Walk‑mode changes the way subschemas are invoked. | `validate` must call `schema.walk(...)` instead of `schema.validate(...)`. |
| **`walk(..., false)`** | Walk should *not* collect validation messages. | Returned set is empty, but each subschema’s `walk` is invoked. |
| **`preloadJsonSchema` when a subschema throws** | Loader errors must propagate. | Exception from `initializeValidators()` is thrown (test verifies propagation). |

---

**Step 4 – Full JUnit 4 test file**

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

    // -----------------------------------------------------------------------
    // Mocks for the surrounding validation infrastructure
    // -----------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock ValidatorState validatorState;
    @Mock Scope dynamicScope;
    @Mock JsonSchema parentSchema;          // not used directly in tests

    // Two dummy subschemas – we will program their behaviour per test
    @Mock JsonSchema schemaA;
    @Mock JsonSchema schemaB;

    // A dummy child validation message (used for “failed” subschemas)
    @Mock ValidationMessage childMessage;

    // -----------------------------------------------------------------------
    // Test data
    // -----------------------------------------------------------------------
    private OneOfValidator validator;
    private ObjectMapper mapper = new ObjectMapper();
    private ArrayNode oneOfArray;   // will be filled in @Before

    @Before
    public void setUp() throws Exception {
        // -------------------------------------------------------------------
        // Build a schemaNode that contains two empty subschemas: [{} , {}]
        // -------------------------------------------------------------------
        oneOfArray = mapper.createArrayNode();
        oneOfArray.add(mapper.createObjectNode());
        oneOfArray.add(mapper.createObjectNode());

        // -------------------------------------------------------------------
        // ValidationContext creates the two subschemas in order
        // -------------------------------------------------------------------
        // First call → schemaA, second call → schemaB
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        // -------------------------------------------------------------------
        // Create the validator under test
        // -------------------------------------------------------------------
        validator = new OneOfValidator("#/oneOf", oneOfArray, parentSchema, validationContext);

        // -------------------------------------------------------------------
        // Wire the ExecutionContext → CollectorContext → ValidatorState
        // -------------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // Default state: walk disabled, validation enabled, matchedNode true
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // The state setters are void – just let Mockito swallow them
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
    }

    // -----------------------------------------------------------------------
    // Helper to create a dummy JSON node that will be validated
    // -----------------------------------------------------------------------
    private JsonNode dummyNode() {
        return mapper.createObjectNode().put("dummy", "value");
    }

    // -----------------------------------------------------------------------
    // 1️⃣ Constructor basic test – number of subschemas equals array size
    // -----------------------------------------------------------------------
    @Test
    public void constructor_shouldCreateSameNumberOfSchemasAsArrayElements() throws Exception {
        // The validator already built in @Before; just verify internal list size
        // (We cannot access the private list directly, but we can verify that the
        // ValidationContext was asked twice – once per array element.)
        verify(validationContext, times(2)).newSchema(anyString(), any(JsonNode.class), eq(parentSchema));
    }

    // -----------------------------------------------------------------------
    // 2️⃣ validate – exactly one subschema matches → no errors
    // -----------------------------------------------------------------------
    @Test
    public void validate_whenExactlyOneSchemaMatches_returnsEmptyErrorSet() {
        // schemaA matches (empty error set), schemaB fails (non‑empty)
        when(schemaA.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(childMessage));

        Set<ValidationMessage> result = validator.validate(executionContext,
                dummyNode(), dummyNode(), "$");

        assertTrue("No validation messages expected when exactly one schema matches", result.isEmpty());
        // The validator must have marked the node as matched
        verify(validatorState).setMatchedNode(true);
    }

    // -----------------------------------------------------------------------
    // 3️⃣ validate – no subschema matches → OneOf error + child errors
    // -----------------------------------------------------------------------
    @Test
    public void validate_whenNoSchemaMatches_returnsOneOfErrorAndChildErrors() {
        // Both schemas return a single child error
        when(schemaA.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(childMessage));
        when(schemaB.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(childMessage));

        Set<ValidationMessage> result = validator.validate(executionContext,
                dummyNode(), dummyNode(), "$");

        // Expect at least the OneOf error (the validator builds a message with the count)
        assertEquals("Expected three messages: OneOf + two child errors", 3, result.size());

        // Verify that the OneOf message contains “0” (zero matching schemas)
        boolean containsZero = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("OneOf error should indicate 0 matching schemas", containsZero);
    }

    // -----------------------------------------------------------------------
    // 4️⃣ validate – more than one schema matches → OneOf error
    // -----------------------------------------------------------------------
    @Test
    public void validate_whenMultipleSchemasMatch_returnsOneOfError() {
        // Both schemas succeed (empty error sets)
        when(schemaA.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext,
                dummyNode(), dummyNode(), "$");

        // Only the OneOf error should be present (no child errors)
        assertEquals("Only the OneOf error expected when >1 schemas match", 1, result.size());

        ValidationMessage msg = result.iterator().next();
        assertTrue("OneOf message must contain the number of valid schemas (2)",
                msg.getMessage().contains("2"));
    }

    // -----------------------------------------------------------------------
    // 5️⃣ validate – failFast flag forces exception
    // -----------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void validate_whenFailFastEnabled_throwsJsonSchemaException() throws Exception {
        // Enable failFast via reflection (field lives in BaseJsonValidator)
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.set(validator, true);

        // Both schemas fail → OneOf error should be thrown
        when(schemaA.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(childMessage));
        when(schemaB.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(childMessage));

        validator.validate(executionContext, dummyNode(), dummyNode(), "$");
    }

    // -----------------------------------------------------------------------
    // 6️⃣ validate – walkEnabled true → schema.walk is used
    // -----------------------------------------------------------------------
    @Test
    public void validate_whenWalkEnabled_callsSchemaWalk() {
        // Switch the state to walk mode
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(schemaA.walk(executionContext, any(JsonNode.class), any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());
        when(schemaB.walk(executionContext, any(JsonNode.class), any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(childMessage));

        Set<ValidationMessage> result = validator.validate(executionContext,
                dummyNode(), dummyNode(), "$");

        // Verify that walk was called on both schemas
        verify(schemaA).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), eq(true));
        verify(schemaB).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), eq(true));

        // One schema matched → no OneOf error
        assertTrue("Result should be empty when exactly one schema matches in walk mode", result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 7️⃣ walk – shouldValidateSchema == true → delegates to validate()
    // -----------------------------------------------------------------------
    @Test
    public void walk_whenShouldValidateTrue_delegatesToValidate() {
        // Make validate return a known set
        Set<ValidationMessage> fakeSet = Collections.singleton(childMessage);
        // Spy the validator to intercept the call to validate()
        OneOfValidator spyValidator = Mockito.spy(validator);
        doReturn(fakeSet).when(spyValidator).validate(executionContext, dummyNode(),
                dummyNode(), "$");

        Set<ValidationMessage> result = spyValidator.walk(executionContext,
                dummyNode(), dummyNode(), "$", true);

        assertEquals(fakeSet, result);
        verify(spyValidator).validate(executionContext, dummyNode(), dummyNode(), "$");
    }

    // -----------------------------------------------------------------------
    // 8️⃣ walk – shouldValidateSchema == false → each subschema.walk is invoked
    // -----------------------------------------------------------------------
    @Test
    public void walk_whenShouldValidateFalse_callsEachSchemaWalk() {
        // No need to stub return values – walk returns void in the interface
        Set<ValidationMessage> result = validator.walk(executionContext,
                dummyNode(), dummyNode(), "$", false);

        // The method should return an empty set
        assertTrue(result.isEmpty());

        // Verify each subschema received a walk call
        verify(schemaA).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false));
        verify(schemaB).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false));
    }

    // -----------------------------------------------------------------------
    // 9️⃣ preloadJsonSchema – each subschema.initializeValidators() is called
    // -----------------------------------------------------------------------
    @Test
    public void preloadJsonSchema_callsInitializeValidatorsOnAllSchemas() {
        validator.preloadJsonSchema();

        verify(schemaA).initializeValidators();
        verify(schemaB).initializeValidators();
    }

    // -----------------------------------------------------------------------
    // 1️⃣0️⃣ Edge case – empty oneOf array → always fails
    // -----------------------------------------------------------------------
    @Test
    public void validate_withEmptyOneOfArray_returnsOneOfError() {
        // Build a validator with an empty array node
        ArrayNode emptyArray = mapper.createArrayNode(); // size == 0
        OneOfValidator emptyValidator = new OneOfValidator("#/empty", emptyArray,
                parentSchema, validationContext);

        // No subschemas exist → validate should directly produce an error
        Set<ValidationMessage> result = emptyValidator.validate(executionContext,
                dummyNode(), dummyNode(), "$");

        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("0"));
    }

    // -----------------------------------------------------------------------
    // 1️⃣1️⃣ Edge case – preloadJsonSchema propagates exception
    // -----------------------------------------------------------------------
    @Test(expected = RuntimeException.class)
    public void preloadJsonSchema_whenSchemaThrowsException_propagatesIt() {
        doThrow(new RuntimeException("init failure")).when(schemaA).initializeValidators();

        // The call should bubble the RuntimeException up
        validator.preloadJsonSchema();
    }
}
###Test END##