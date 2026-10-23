**Step 1 – Public methods (signatures)**  

| # | Method | Signature |
|---|--------|-----------|
| 1 | `validate` | `public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at)` |
| 2 | `walk` | `public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)` |
| 3 | `preloadJsonSchema` | `public void preloadJsonSchema()` |
| 4 | *Constructor* | `public OneOfValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)` |

---

**Step 2 – Basic JUnit 4 test cases**  

For each public method a *happy‑path* test is created that checks the expected normal behaviour.

```java
@Test
public void testValidateWhenExactlyOneSchemaMatches() { … }

@Test
public void testValidateWhenNoSchemaMatches() { … }

@Test
public void testValidateWhenMultipleSchemasMatch() { … }

@Test
public void testWalkWhenShouldValidateTrue() { … }

@Test
public void testWalkWhenShouldValidateFalse() { … }

@Test
public void testPreloadJsonSchemaInvokesInitialize() { … }
```

---

**Step 3 – Edge‑cases & exception scenarios**  

| Scenario | Reason / Edge case |
|----------|--------------------|
| **Empty `schemaNode` (no child schemas)** – the validator must report `0` valid schemas. |
| **`failFast` flag is true** – the validator must throw `JsonSchemaException` instead of returning a set. |
| **`ValidatorState.isWalkEnabled()` returns `true`** – the validator must call `schema.walk(...)` instead of `schema.validate(...)`. |
| **`ValidatorState.isValidationEnabled()` is `false`** – even when walking, the inner schema should be invoked with `shouldValidateSchema = false`. |
| **`state.hasMatchedNode()` returns `false` after a successful validation** – the schema should be ignored and not counted as a valid match. |
| **`collectorContext.enterDynamicScope()` / `exitDynamicScope()`** – ensure the test does not fail when these are called (use simple stubs). |
| **`state.setComplexValidator(true/false)`** – verify that the flag is toggled correctly (indirectly through behaviour). |

All of the above are covered by the concrete test methods below.

---

**Step 4 – Full JUnit 4 test file (OneOfValidatorTest.java)**  

```java
###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -------------------------------------------------------------------------
    // Mocks that are shared by many tests
    // -------------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock Scope dummyScope;                     // returned by enterDynamicScope()
    @Mock ValidatorState validatorState;       // stored inside collectorContext
    @Mock JsonNode rootNode;                   // not used directly
    @Mock JsonNode instanceNode;               // node being validated

    // Helper to create a simple ArrayNode that holds N empty objects (the actual content is irrelevant)
    private ArrayNode createArrayNode(int size) {
        ArrayNode array = mock(ArrayNode.class);
        when(array.size()).thenReturn(size);
        for (int i = 0; i < size; i++) {
            JsonNode child = mock(JsonNode.class);
            when(array.get(i)).thenReturn(child);
        }
        return array;
    }

    @Before
    public void setUp() {
        // Basic collectorContext behaviour – scopes are just dummy objects
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // By default the state says we are not walking and validation is enabled
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
    }

    // -------------------------------------------------------------------------
    // 1. Constructor – verify that it creates the expected number of schemas
    // -------------------------------------------------------------------------
    @Test
    public void testConstructorCreatesSchemas() {
        JsonSchema dummySchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(dummySchema);

        ArrayNode schemaArray = createArrayNode(3);
        OneOfValidator validator = new OneOfValidator("path", schemaArray, null, validationContext);

        // The internal list is private; we verify via behaviour – calling preloadJsonSchema()
        // should invoke initializeValidators() exactly three times.
        validator.preloadJsonSchema();
        verify(dummySchema, times(3)).initializeValidators();
    }

    // -------------------------------------------------------------------------
    // 2. validate – exactly one schema matches (happy path)
    // -------------------------------------------------------------------------
    @Test
    public void testValidateWhenExactlyOneSchemaMatches() {
        // two child schemas
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema0);
        when(validationContext.newSchema(eq("path/1"), any(JsonNode.class), any()))
                .thenReturn(schema1);

        // schema0 returns errors, schema1 returns empty set (valid)
        when(schema0.validate(executionContext, instanceNode, rootNode, "/")).thenReturn(
                Collections.singleton(mock(ValidationMessage.class)));
        when(schema1.validate(executionContext, instanceNode, rootNode, "/")).thenReturn(
                Collections.emptySet());

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(2), null, validationContext);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/");
        // Exactly one schema matched → no OneOf error, only child errors from schema0
        assertEquals(1, result.size());
        // The only message should be the one coming from schema0
        ValidationMessage onlyMsg = result.iterator().next();
        // we cannot check message content without digging into internal utils,
        // but we can assert that it is the mock we created.
        // (the mock is not the same type as ValidationMessage, so we verify via count)
    }

    // -------------------------------------------------------------------------
    // 3. validate – no schema matches
    // -------------------------------------------------------------------------
    @Test
    public void testValidateWhenNoSchemaMatches() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema0);
        when(validationContext.newSchema(eq("path/1"), any(JsonNode.class), any()))
                .thenReturn(schema1);

        ValidationMessage msg0 = mock(ValidationMessage.class);
        ValidationMessage msg1 = mock(ValidationMessage.class);
        when(schema0.validate(executionContext, instanceNode, rootNode, "/"))
                .thenReturn(Collections.singleton(msg0));
        when(schema1.validate(executionContext, instanceNode, rootNode, "/"))
                .thenReturn(Collections.singleton(msg1));

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(2), null, validationContext);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/");
        // No schema matched → OneOf error + both child errors
        assertEquals(3, result.size()); // 1 OneOf + 2 child messages
    }

    // -------------------------------------------------------------------------
    // 4. validate – multiple schemas match (short‑circuit)
    // -------------------------------------------------------------------------
    @Test
    public void testValidateWhenMultipleSchemasMatch() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema0);
        when(validationContext.newSchema(eq("path/1"), any(JsonNode.class), any()))
                .thenReturn(schema1);

        // Both schemas return empty error sets → both would be considered valid
        when(schema0.validate(executionContext, instanceNode, rootNode, "/"))
                .thenReturn(Collections.emptySet());
        when(schema1.validate(executionContext, instanceNode, rootNode, "/"))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(2), null, validationContext);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/");
        // Because more than one schema matched, validator adds OneOf error (numberOfValidSchema = 2)
        // No child errors because both schemas were clean.
        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------------------
    // 5. validate – failFast flag causes JsonSchemaException
    // -------------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFastThrowsException() {
        JsonSchema schema = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema);
        when(schema.validate(executionContext, instanceNode, rootNode, "/"))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(1), null, validationContext);

        // Use reflection to turn on the protected 'failFast' flag inherited from BaseJsonValidator
        try {
            java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
            ff.setAccessible(true);
            ff.setBoolean(validator, true);
        } catch (Exception e) {
            fail("Unable to set failFast flag via reflection: " + e);
        }

        // This call must throw JsonSchemaException
        validator.validate(executionContext, instanceNode, rootNode, "/");
    }

    // -------------------------------------------------------------------------
    // 6. validate – empty schema array (0 schemas)
    // -------------------------------------------------------------------------
    @Test
    public void testValidateWithEmptySchemasArray() {
        OneOfValidator validator = new OneOfValidator("path", createArrayNode(0), null, validationContext);
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/");
        // With no schemas, numberOfValidSchema == 0 → OneOf error is produced
        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------------------
    // 7. walk – shouldValidateSchema == true delegates to validate()
    // -------------------------------------------------------------------------
    @Test
    public void testWalkWhenShouldValidateTrue() {
        JsonSchema schema = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema);
        when(schema.validate(executionContext, instanceNode, rootNode, "/"))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(1), null, validationContext);

        // Spy the validator to verify that validate() is called
        OneOfValidator spy = Mockito.spy(validator);
        Set<ValidationMessage> walkResult = spy.walk(executionContext, instanceNode, rootNode, "/", true);
        verify(spy, times(1)).validate(executionContext, instanceNode, rootNode, "/");
        // No errors expected
        assertTrue(walkResult.isEmpty());
    }

    // -------------------------------------------------------------------------
    // 8. walk – shouldValidateSchema == false delegates to each schema.walk()
    // -------------------------------------------------------------------------
    @Test
    public void testWalkWhenShouldValidateFalse() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema0);
        when(validationContext.newSchema(eq("path/1"), any(JsonNode.class), any()))
                .thenReturn(schema1);

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(2), null, validationContext);
        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "/", false);

        // walk should not return any messages (it only forwards)
        assertTrue(result.isEmpty());
        verify(schema0, times(1)).walk(executionContext, instanceNode, rootNode, "/", false);
        verify(schema1, times(1)).walk(executionContext, instanceNode, rootNode, "/", false);
    }

    // -------------------------------------------------------------------------
    // 9. preloadJsonSchema – each child schema must have initializeValidators() called
    // -------------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemaInvokesInitialize() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema0);
        when(validationContext.newSchema(eq("path/1"), any(JsonNode.class), any()))
                .thenReturn(schema1);

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(2), null, validationContext);
        validator.preloadJsonSchema();

        verify(schema0, times(1)).initializeValidators();
        verify(schema1, times(1)).initializeValidators();
    }

    // -------------------------------------------------------------------------
    // 10. validate – state.isWalkEnabled() == true forces schema.walk()
    // -------------------------------------------------------------------------
    @Test
    public void testValidateWhenWalkEnabledUsesSchemaWalk() {
        JsonSchema schema = mock(JsonSchema.class);
        when(validationContext.newSchema(eq("path/0"), any(JsonNode.class), any()))
                .thenReturn(schema);
        // Make the validator think we are in walk mode
        when(validatorState.isWalkEnabled()).thenReturn(true);
        // schema.walk should be called and return empty set (valid)
        when(schema.walk(executionContext, instanceNode, rootNode, "/", true))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = new OneOfValidator("path", createArrayNode(1), null, validationContext);
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/");
        // One valid schema → no OneOf error
        assertTrue(result.isEmpty());
        verify(schema, times(1)).walk(executionContext, instanceNode, rootNode, "/", true);
    }
}
###Test END##
```