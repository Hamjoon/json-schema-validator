###Test START##
```java
/*
 * OneOfValidatorTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests use Mockito to mock the heavy dependencies of the validator
 * (JsonSchema, ValidationContext, ExecutionContext, CollectorContext, etc.)
 * and focus on the behaviour of the public methods:
 *
 *  * {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}
 *  * {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}
 *  * {@link OneOfValidator#preloadJsonSchema()}
 *
 * The test cases cover:
 *
 * 1. Exactly one subschema matches – no error is reported.
 * 2. No subschema matches – a “oneOf” error plus child errors are reported.
 * 3. More than one subschema matches – a “oneOf” error is reported (failFast = false).
 * 4. More than one subschema matches with failFast = true – a JsonSchemaException is thrown.
 * 5. walk() when validation is required (delegates to validate()).
 * 6. walk() when validation is skipped (delegates to each subschema’s walk()).
 * 7. preloadJsonSchema() – each subschema’s initialiseValidators() is invoked.
 *
 * The test class is completely self‑contained and can be compiled and run
 * against the original library (JUnit‑4.13+, Mockito‑3+).
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    /** Helper constants **/
    private static final String SCHEMA_PATH = "$.oneOf";
    private static final String AT = "$.test";

    /** Mocks that are used in several tests **/
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope grandParentScope;
    @Mock private Scope parentScope;
    @Mock private Scope childScope;

    /** Two child schemas that will be returned by ValidationContext#newSchema **/
    @Mock private JsonSchema schema1;
    @Mock private JsonSchema schema2;

    /** A dummy parent schema – not used by the validator itself **/
    @Mock private JsonSchema parentSchema;

    /** ObjectMapper for building simple JsonNode structures **/
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The validator under test – created per test case so that its internal
     *  list of subschemas can be configured differently. */
    private OneOfValidator validator;

    /** Common set‑up for all tests **/
    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // 1) Build a minimal JSON array node that represents the "oneOf"
        //    keyword – the content of the child nodes is irrelevant because
        //    ValidationContext#newSchema is mocked.
        // -----------------------------------------------------------------
        JsonNode schemaNode = MAPPER.readTree("[{},{}]");

        // -----------------------------------------------------------------
        // 2) ValidationContext#newSchema must return the two mocked subschemas
        //    in the order they appear in the array.
        // -----------------------------------------------------------------
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/0"), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(schema1);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/1"), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(schema2);

        // -----------------------------------------------------------------
        // 3) ExecutionContext provides the CollectorContext.
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // -----------------------------------------------------------------
        // 4) CollectorContext returns a fresh ValidatorState when queried.
        // -----------------------------------------------------------------
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // default behaviour of the state – walk is disabled, validation is enabled
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        // after each schema the validator calls state.setMatchedNode(true);
        // we make hasMatchedNode() simply return true (the usual case for a match).
        when(validatorState.hasMatchedNode()).thenReturn(true);

        // -----------------------------------------------------------------
        // 5) Dynamic scopes – we only need them to be non‑null and to support
        //    mergeWith(...).  The real merging logic is not relevant for the
        //    unit tests.
        // -----------------------------------------------------------------
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope, parentScope);
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);

        // -----------------------------------------------------------------
        // 6) Evaluated‑items / evaluated‑properties containers – they must be
        //    mutable so that clear() can be verified.
        // -----------------------------------------------------------------
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // -----------------------------------------------------------------
        // 7) Finally, create the validator instance.
        // -----------------------------------------------------------------
        validator = new OneOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
    }

    /* --------------------------------------------------------------- *
     *  TEST 1 – exactly one subschema matches.
     * --------------------------------------------------------------- */
    @Test
    public void testValidateExactlyOneSchemaMatches() {
        // schema1 produces an error, schema2 validates successfully (empty set)
        ValidationMessage error1 = mock(ValidationMessage.class);
        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.singleton(error1));
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, MAPPER.createObjectNode(),
                MAPPER.createObjectNode(), AT);

        // No "oneOf" error because exactly one schema matched
        assertTrue("Result should be empty when one schema matches", result.isEmpty());

        // The validator must have marked the node as matched
        verify(validatorState).setMatchedNode(true);
        // No clearing of evaluated collections because we had a valid match
        verify(collectorContext.getEvaluatedItems(), never()).clear();
        verify(collectorContext.getEvaluatedProperties(), never()).clear();
    }

    /* --------------------------------------------------------------- *
     *  TEST 2 – no subschema matches.
     * --------------------------------------------------------------- */
    @Test
    public void testValidateNoSchemaMatches() {
        ValidationMessage err1 = mock(ValidationMessage.class);
        ValidationMessage err2 = mock(ValidationMessage.class);
        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.singleton(err1));
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.singleton(err2));

        Set<ValidationMessage> result = validator.validate(executionContext, MAPPER.createObjectNode(),
                MAPPER.createObjectNode(), AT);

        // Expect a "oneOf" error plus both child errors (total 3)
        assertEquals("Should contain one 'oneOf' error plus two child errors", 3, result.size());

        // Verify that the evaluated collections have been cleared
        assertTrue("Evaluated items should be cleared", collectorContext.getEvaluatedItems().isEmpty());
        assertTrue("Evaluated properties should be cleared", collectorContext.getEvaluatedProperties().isEmpty());
    }

    /* --------------------------------------------------------------- *
     *  TEST 3 – more than one subschema matches, failFast = false.
     * --------------------------------------------------------------- */
    @Test
    public void testValidateMultipleSchemasMatchFailFastFalse() {
        // Both schemas validate successfully (empty error sets)
        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, MAPPER.createObjectNode(),
                MAPPER.createObjectNode(), AT);

        // With two matching schemas we must receive the "oneOf" error
        assertEquals("Only the 'oneOf' error should be present when failFast=false", 1, result.size());

        // The child error set is empty because both subschemas succeeded
        // but the validator still clears the evaluated collections.
        assertTrue("Evaluated items should be cleared", collectorContext.getEvaluatedItems().isEmpty());
        assertTrue("Evaluated properties should be cleared", collectorContext.getEvaluatedProperties().isEmpty());
    }

    /* --------------------------------------------------------------- *
     *  TEST 4 – more than one subschema matches, failFast = true.
     * --------------------------------------------------------------- */
    @Test
    public void testValidateMultipleSchemasMatchFailFastTrue() throws Exception {
        // Enable fail‑fast via reflection (BaseJsonValidator defines a protected boolean)
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // Both subschemas succeed
        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());

        try {
            validator.validate(executionContext, MAPPER.createObjectNode(),
                    MAPPER.createObjectNode(), AT);
            fail("Expected JsonSchemaException because failFast is true and more than one schema matched");
        } catch (JsonSchemaException ex) {
            // The message should contain the number of valid schemas ("2")
            assertTrue("Exception message should contain the number of valid schemas",
                    ex.getMessage().contains("2"));
        }
    }

    /* --------------------------------------------------------------- *
     *  TEST 5 – walk() with shouldValidateSchema = true.
     * --------------------------------------------------------------- */
    @Test
    public void testWalkShouldValidateTrue() {
        // Re‑use the "exactly one matches" scenario
        ValidationMessage err1 = mock(ValidationMessage.class);
        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.singleton(err1));
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> walkResult = validator.walk(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), AT, true);

        // walk(true) should delegate to validate() – same result as testValidateExactlyOneSchemaMatches
        assertTrue("Walk with validation should produce empty result (one schema matches)", walkResult.isEmpty());
    }

    /* --------------------------------------------------------------- *
     *  TEST 6 – walk() with shouldValidateSchema = false.
     * --------------------------------------------------------------- */
    @Test
    public void testWalkShouldValidateFalse() {
        // When validation is disabled, walk() must invoke each subschema's walk()
        // but must not aggregate any results.
        when(schema1.walk(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT), eq(false)))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));
        when(schema2.walk(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT), eq(false)))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));

        Set<ValidationMessage> walkResult = validator.walk(executionContext,
                MAPPER.createObjectNode(), MAPPER.createObjectNode(), AT, false);

        // The validator does not return the child errors when shouldValidateSchema is false
        assertTrue("Walk without validation should return an empty set", walkResult.isEmpty());

        // Verify that each subschema's walk() method was indeed called
        verify(schema1).walk(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT), eq(false));
        verify(schema2).walk(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT), eq(false));
    }

    /* --------------------------------------------------------------- *
     *  TEST 7 – preloadJsonSchema() must initialise each subschema.
     * --------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchema() {
        // preloadJsonSchema should invoke initialiseValidators() on each child schema
        validator.preloadJsonSchema();

        verify(schema1).initializeValidators();
        verify(schema2).initializeValidators();
    }

    /* ---------------------------------------------------------------- *
     * Helper method to create a dummy JsonNode (empty object node).
     * ---------------------------------------------------------------- */
    private static JsonNode dummyNode() {
        return MAPPER.createObjectNode();
    }
}
```
###Test END##