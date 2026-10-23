/**
 * JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API extraction (constructor, {@code validate}, {@code walk},
 *       {@code preloadJsonSchema}).</li>
 *   <li>Basic functional behaviour for each method.</li>
 *   <li>Edge‑case handling:
 *       <ul>
 *         <li>zero, one and multiple matching subschemas.</li>
 *         <li>fail‑fast mode (exception is thrown).</li>
 *         <li>walk delegating vs. executing validation.</li>
 *         <li>preload invoking {@code initializeValidators()} on each subschema.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * The test uses Mockito to stub the complex collaborators
 * ({@link com.networknt.schema.JsonSchema},
 * {@link com.networknt.schema.ExecutionContext},
 * {@link com.networknt.schema.CollectorContext},
 * {@link com.networknt.schema.Scope},
 * {@link com.networknt.schema.ValidatorState},
 * {@link com.networknt.schema.ValidationContext}).
 *
 * The real {@code BaseJsonValidator} class contains a protected {@code failFast}
 * flag; we set it via reflection because the class does not expose a public
 * setter.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    /** Helper to create an {@link ArrayNode} with the given number of elements. */
    private static ArrayNode createSchemaArray(int size) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < size; i++) {
            array.add(JsonNodeFactory.instance.objectNode()); // empty child schema node
        }
        return array;
    }

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private Scope dynamicScope; // used for both parent & grand‑parent scopes

    @Mock
    private ValidatorState validatorState;

    @Mock
    private JsonSchema childSchema0;

    @Mock
    private JsonSchema childSchema1;

    @Mock
    private JsonSchema childSchema2;

    private OneOfValidator validator;

    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // Common mock wiring
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // -----------------------------------------------------------------
        // Build a validator with three subschemas
        // -----------------------------------------------------------------
        ArrayNode schemaArray = createSchemaArray(3);
        // make the ValidationContext return a distinct mock for each child schema
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema0, childSchema1, childSchema2);

        validator = new OneOfValidator("#/oneOf", schemaArray, mock(JsonSchema.class), validationContext);
        // set failFast = false (default) – we will flip it later via reflection when needed
        setFailFast(validator, false);
    }

    /** Utility to flip the protected {@code failFast} flag via reflection. */
    private static void setFailFast(OneOfValidator v, boolean value) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.setBoolean(v, value);
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: Constructor
     *  -----------------------------------------------------------------
     */
    @Test
    public void constructorShouldCreateChildSchemas() {
        // The constructor should have called ValidationContext#newSchema three times
        verify(validationContext, times(3)).newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class));
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: validate – exactly one schema matches
     *  -----------------------------------------------------------------
     */
    @Test
    public void validateWhenExactlyOneSchemaMatches_ShouldReturnNoErrors() {
        // childSchema0 fails, childSchema1 succeeds, childSchema2 fails
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error0")));
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error2")));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        assertTrue("No validation errors expected when exactly one subschema matches", result.isEmpty());
        // state should be marked as matched
        verify(validatorState).setMatchedNode(true);
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: validate – no schemas match
     *  -----------------------------------------------------------------
     */
    @Test
    public void validateWhenNoSchemaMatches_ShouldReturnOneOfErrorAndChildErrors() {
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error0")));
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error1")));
        when(childSchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error2")));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // Expect ONE "oneOf" error + all child errors (3)
        assertEquals("One 'oneOf' error + three child errors expected", 4, result.size());

        // Verify that the aggregated message contains the count "0"
        boolean containsZero = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("Aggregated message should contain the number of valid schemas (0)", containsZero);
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: validate – multiple schemas match, failFast = false
     *  -----------------------------------------------------------------
     */
    @Test
    public void validateWhenMultipleSchemasMatch_FailFastFalse_ShouldReturnOneOfError() {
        // Two schemas succeed (empty error set)
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        // third schema fails – its errors are still collected
        when(childSchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error2")));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // Since two schemas matched, we should still get a "oneOf" error (count = 2)
        // plus the child error from the failing schema (1) => total 2 messages
        assertEquals("One 'oneOf' error + one child error expected", 2, result.size());

        boolean containsTwo = result.stream()
                .anyMatch(m -> m.getMessage().contains("2"));
        assertTrue("Aggregated message should contain the number of valid schemas (2)", containsTwo);
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: validate – multiple schemas match, failFast = true
     *  -----------------------------------------------------------------
     */
    @Test(expected = JsonSchemaException.class)
    public void validateWhenMultipleSchemasMatch_FailFastTrue_ShouldThrowException() throws Exception {
        setFailFast(validator, true);

        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        validator.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), "$");
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: walk – shouldValidate = true
     *  -----------------------------------------------------------------
     */
    @Test
    public void walkWhenShouldValidateTrue_ShouldDelegateToValidate() {
        // Use the same stubbing as the "exactly one matches" scenario
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error0")));
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(validationMessage("error2")));

        Set<ValidationMessage> msgs = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", true);

        // walk(true) should return the same set as validate()
        assertTrue("No errors expected because exactly one subschema matches", msgs.isEmpty());
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: walk – shouldValidate = false
     *  -----------------------------------------------------------------
     */
    @Test
    public void walkWhenShouldValidateFalse_ShouldInvokeEachSchemaWalkOnly() {
        // Setup each child schema to receive a walk call
        when(childSchema0.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema1.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(Collections.emptySet());

        Set<ValidationMessage> msgs = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", false);

        // walk(false) never adds messages; the set should be empty
        assertTrue(msgs.isEmpty());

        // Verify that walk(false) was called on each child schema
        verify(childSchema0).walk(any(), any(), any(), anyString(), eq(false));
        verify(childSchema1).walk(any(), any(), any(), anyString(), eq(false));
        verify(childSchema2).walk(any(), any(), any(), anyString(), eq(false));
    }

    /** -----------------------------------------------------------------
     *  PUBLIC METHOD: preloadJsonSchema
     *  -----------------------------------------------------------------
     */
    @Test
    public void preloadJsonSchema_ShouldInitializeAllChildSchemas() {
        validator.preloadJsonSchema();

        verify(childSchema0).initializeValidators();
        verify(childSchema1).initializeValidators();
        verify(childSchema2).initializeValidators();
    }

    // -----------------------------------------------------------------
    // Helper to create a dummy ValidationMessage (the real class has a
    // static factory; we use the constructor via reflection for brevity)
    // -----------------------------------------------------------------
    private ValidationMessage validationMessage(String msg) {
        try {
            // ValidationMessage has a protected constructor: (String type, String path, String message)
            java.lang.reflect.Constructor<ValidationMessage> ctor =
                    ValidationMessage.class.getDeclaredConstructor(String.class, String.class, String.class);
            ctor.setAccessible(true);
            return ctor.newInstance("test", "$", msg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
