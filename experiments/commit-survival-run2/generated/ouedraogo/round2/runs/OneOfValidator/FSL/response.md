###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * <p>The tests use Mockito to mock the heavy‑weight collaborators
 * ({@link com.networknt.schema.ExecutionContext},
 * {@link com.networknt.schema.CollectorContext},
 * {@link com.networknt.schema.Scope},
 * {@link com.networknt.schema.JsonSchema},
 * {@link com.networknt.schema.ValidatorState},
 * etc.).  This allows us to focus on the logic that lives inside
 * {@code OneOfValidator} without having to spin up the whole
 * JSON‑Schema engine.</p>
 *
 * <p>Each test builds a {@code OneOfValidator} instance with a synthetic
 * {@code JsonNode} that represents an array of subschemas.  The
 * {@link com.networknt.schema.ValidationContext} is mocked so that the
 * {@code newSchema(..)} method returns a pre‑configured mock
 * {@link com.networknt.schema.JsonSchema} for each array element.</p>
 *
 * <p>The suite covers:</p>
 * <ul>
 *   <li>Exactly one subschema matches – validation succeeds.</li>
 *   <li>No subschemas match – an error message is produced and child
 *       errors are propagated.</li>
 *   <li>More than one subschema matches – validation fails with a
 *       “oneOf” error.</li>
 *   <li>Fail‑fast mode – the validator throws {@link com.networknt.schema.JsonSchemaException}
 *       instead of returning a set.</li>
 *   <li>The {@code walk(...)} method in both validation and non‑validation
 *       modes.</li>
 *   <li>Internal state reset after validation.</li>
 * </ul>
 *
 * <p>All tests are written for JUnit‑4 and use the standard {@code assert*}
 * methods together with Mockito verification.</p>
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope grandParentScope;
    @Mock private Scope parentScope;
    @Mock private Scope childScope;
    @Mock private ValidatorState validatorState;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Common stubbing performed before each test.
     */
    @Before
    public void setUp() {
        // ExecutionContext -> CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext dynamic scopes
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // first call – outermost
                .thenReturn(parentScope)        // second call – per‑schema
                .thenReturn(childScope);       // third call – inner loop (if any)

        // exitDynamicScope() – just return a dummy scope (the same as entered)
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);

        // ValidatorState stored inside CollectorContext
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY))
                .thenReturn(validatorState);

        // By default the walk is disabled (the validator will call {@code validate})
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);
        when(validatorState.isComplexValidator()).thenReturn(false);
    }

    /**
     * Helper that creates a {@code OneOfValidator} with a given array node.
     *
     * @param arrayJson a JSON array string, e.g. "[{},{}]"
     * @return a freshly constructed {@code OneOfValidator}
     * @throws Exception if Jackson fails to parse the JSON
     */
    private OneOfValidator createValidator(String arrayJson) throws Exception {
        JsonNode schemaNode = mapper.readTree(arrayJson);
        // The validator's constructor will ask the ValidationContext for a schema for each element.
        // We let the test method set up those expectations before calling this helper.
        return new OneOfValidator("testPath", schemaNode, mock(JsonSchema.class), validationContext);
    }

    // -------------------------------------------------------------------------
    // 1. Exactly one subschema validates – success case
    // -------------------------------------------------------------------------

    @Test
    public void whenExactlyOneSchemaMatches_thenValidationSucceeds() throws Exception {
        // ---- Arrange --------------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        // schemaA fails, schemaB succeeds
        when(schemaA.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));
        when(schemaB.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // ValidationContext returns the above mocks for the two array entries
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)   // first child
                .thenReturn(schemaB);  // second child

        OneOfValidator validator = createValidator("[{},{}]");

        // ---- Act ------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mapper.readTree("{\"dummy\":\"value\"}"),
                null,
                "$"
        );

        // ---- Assert ---------------------------------------------------------
        assertTrue("No validation errors expected when exactly one schema matches", result.isEmpty());

        // Verify that the internal state was reset (complex flag cleared, matchedNode true)
        verify(validatorState).setComplexValidator(false);
        verify(validatorState).setMatchedNode(true);
    }

    // -------------------------------------------------------------------------
    // 2. No subschema matches – error message + child errors
    // -------------------------------------------------------------------------

    @Test
    public void whenNoSchemaMatches_thenOneOfErrorAndChildErrorsReturned() throws Exception {
        // ---- Arrange --------------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        ValidationMessage errA = mock(ValidationMessage.class);
        ValidationMessage errB = mock(ValidationMessage.class);

        when(schemaA.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(errA));
        when(schemaB.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(errB));

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        OneOfValidator validator = createValidator("[{},{}]");

        // ---- Act ------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mapper.readTree("{\"dummy\":\"value\"}"),
                null,
                "$"
        );

        // ---- Assert ---------------------------------------------------------
        assertEquals("Exactly two validation messages expected (oneOf + child error)", 3, result.size());
        // The first message must be the OneOf error (its code key contains "oneOf")
        ValidationMessage oneOfMessage = result.iterator().next();
        assertTrue("First message should be a OneOf error",
                oneOfMessage.getMessage().contains("oneOf") ||
                oneOfMessage.getMessage().contains("OneOf"));
        // Child errors must be present
        assertTrue(result.contains(errA));
        assertTrue(result.contains(errB));
    }

    // -------------------------------------------------------------------------
    // 3. More than one subschema matches – validation fails with OneOf error
    // -------------------------------------------------------------------------

    @Test
    public void whenMoreThanOneSchemaMatches_thenOneOfErrorReturned() throws Exception {
        // ---- Arrange --------------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        // Both succeed (empty error set)
        when(schemaA.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        OneOfValidator validator = createValidator("[{},{}]");

        // ---- Act ------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mapper.readTree("{\"dummy\":\"value\"}"),
                null,
                "$"
        );

        // ---- Assert ---------------------------------------------------------
        // The validator should report a OneOf error because two schemas were valid.
        assertEquals("Only the OneOf error should be present (no child errors)", 1, result.size());
        ValidationMessage message = result.iterator().next();
        assertTrue("Message should indicate more than one schema matched",
                message.getMessage().contains("oneOf") || message.getMessage().contains("OneOf"));
    }

    // -------------------------------------------------------------------------
    // 4. Fail‑fast mode – JsonSchemaException is thrown
    // -------------------------------------------------------------------------

    @Test(expected = JsonSchemaException.class)
    public void whenFailFastEnabled_andValidationFails_thenExceptionIsThrown() throws Exception {
        // ---- Arrange --------------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        // Both fail – return a non‑empty set
        when(schemaA.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));
        when(schemaB.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        OneOfValidator validator = createValidator("[{},{}]");

        // Enable fail‑fast via reflection (the field is protected in BaseJsonValidator)
        java.lang.reflect.Field failFastField = BaseJsonValidator.class.getDeclaredField("failFast");
        failFastField.setAccessible(true);
        failFastField.setBoolean(validator, true);

        // ---- Act ------------------------------------------------------------
        validator.validate(
                executionContext,
                mapper.readTree("{\"dummy\":\"value\"}"),
                null,
                "$"
        );

        // ---- Assert ---------------------------------------------------------
        // Expect JsonSchemaException (declared in @Test)
    }

    // -------------------------------------------------------------------------
    // 5. walk(..) with shouldValidateSchema = false – delegates to child walks
    // -------------------------------------------------------------------------

    @Test
    public void walkWhenShouldValidateIsFalse_callsChildWalksAndReturnsEmptySet() throws Exception {
        // ---- Arrange --------------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);

        // Child walks should be invoked
        when(schemaA.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(schemaB.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        OneOfValidator validator = createValidator("[{},{}]");

        // ---- Act ------------------------------------------------------------
        Set<ValidationMessage> msgs = validator.walk(
                executionContext,
                mapper.readTree("{\"dummy\":\"value\"}"),
                null,
                "$",
                false
        );

        // ---- Assert ---------------------------------------------------------
        assertTrue("walk with validation disabled must return an empty set", msgs.isEmpty());
        verify(schemaA).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("$"), eq(false));
        verify(schemaB).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("$"), eq(false));
    }

    // -------------------------------------------------------------------------
    // 6. walk(..) with shouldValidateSchema = true – behaves like validate()
    // -------------------------------------------------------------------------

    @Test
    public void walkWhenShouldValidateIsTrue_behavesLikeValidate() throws Exception {
        // ---- Arrange --------------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        ValidationMessage err = mock(ValidationMessage.class);

        // Let the first schema succeed, second fail
        when(schemaA.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err));

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        OneOfValidator validator = createValidator("[{},{}]");

        // ---- Act ------------------------------------------------------------
        Set<ValidationMessage> msgs = validator.walk(
                executionContext,
                mapper.readTree("{\"dummy\":\"value\"}"),
                null,
                "$",
                true
        );

        // ---- Assert ---------------------------------------------------------
        // With one matching schema the result must be empty (valid)
        assertTrue("When exactly one schema matches, walk should return no errors", msgs.isEmpty());
    }

    // -------------------------------------------------------------------------
    // 7. preloadJsonSchema() – ensures each child schema is initialized
    // -------------------------------------------------------------------------

    @Test
    public void preloadJsonSchema_initializesAllChildSchemas() throws Exception {
        // ---- Arrange --------------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        OneOfValidator validator = createValidator("[{},{}]");

        // ---- Act ------------------------------------------------------------
        validator.preloadJsonSchema();

        // ---- Assert ---------------------------------------------------------
        verify(schemaA).initializeValidators();
        verify(schemaB).initializeValidators();
    }
}
```
###Test END##