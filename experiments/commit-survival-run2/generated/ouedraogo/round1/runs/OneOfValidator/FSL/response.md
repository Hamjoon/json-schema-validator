**###Test START##**  
```java
/****************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}
 *
 * The tests use Mockito to isolate the validator from the rest of the library.
 * Every public method of OneOfValidator is exercised:
 *
 *   • constructor – verifies that the correct number of child schemas are created;
 *   • validate   – scenarios with 0, 1 and >1 matching schemas and the fail‑fast mode;
 *   • walk       – both branches (shouldValidateSchema = true/false);
 *   • preloadJsonSchema – ensures that each child schema is asked to initialise.
 *
 * The test class lives in the same package as the class under test so that it can
 * access package‑private members (e.g. the internal {@code schemas} list) via
 * reflection when necessary.
 ****************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test suite for {@link OneOfValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    /* --------------------------------------------------------------------- *
     *  Mocks required for constructing a OneOfValidator instance
     * --------------------------------------------------------------------- */
    @Mock private ValidationContext validationContext;
    @Mock private JsonNode schemaNode;          // the "oneOf" array node
    @Mock private JsonNode childNode0;          // individual schema definitions
    @Mock private JsonNode childNode1;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope dynamicScope;           // returned by enter/exitDynamicScope()
    @Mock private ValidatorState validatorState; // stored inside CollectorContext
    @Mock private JsonSchema childSchema0;
    @Mock private JsonSchema childSchema1;

    /* --------------------------------------------------------------------- *
     *  Helper constants
     * --------------------------------------------------------------------- */
    private static final String SCHEMA_PATH = "/testSchema";

    /** Helper to build a validator with two child schemas. */
    private OneOfValidator buildValidator() {
        // ----- mock the "oneOf" array node (size = 2) --------------------
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(childNode0);
        when(schemaNode.get(1)).thenReturn(childNode1);

        // ----- mock ValidationContext so that it creates the two child schemas ----
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/0"), eq(childNode0), any(JsonSchema.class)))
                .thenReturn(childSchema0);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/1"), eq(childNode1), any(JsonSchema.class)))
                .thenReturn(childSchema1);

        // ----- build the validator (constructor will fill the internal list) -----
        return new OneOfValidator(SCHEMA_PATH, schemaNode, mock(JsonSchema.class), mock(ValidationContext.class));
    }

    /** Common stub set‑up for the execution/collector context used by all tests. */
    @Before
    public void setUpCommonStubs() {
        // collectorContext handling of dynamic scopes
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
        // the validator state stored inside the collector
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // default behaviour of the validator state
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // evaluated collections – they are cleared when validation fails
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        // setters do nothing (they are void)
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
    }

    /* --------------------------------------------------------------------- *
     *  1. Constructor – verify that the expected number of child schemas are
     *     created via ValidationContext#newSchema.
     * --------------------------------------------------------------------- */
    @Test
    public void constructorCreatesAllChildSchemas() {
        // arrange – the mocks are already prepared in setUpCommonStubs()
        // act
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext);

        // assert – newSchema must have been called twice with the expected paths
        verify(validationContext, times(1))
                .newSchema(eq(SCHEMA_PATH + "/0"), eq(childNode0), any(JsonSchema.class));
        verify(validationContext, times(1))
                .newSchema(eq(SCHEMA_PATH + "/1"), eq(childNode1), any(JsonSchema.class));

        // also verify that the internal list really contains two entries
        List<JsonSchema> internal = (List<JsonSchema>) TestUtils.getPrivateField(validator, "schemas");
        assertEquals(2, internal.size());
        assertTrue(internal.containsAll(Arrays.asList(childSchema0, childSchema1)));
    }

    /* --------------------------------------------------------------------- *
     *  2. validate – exactly ONE schema matches (empty error set)
     * --------------------------------------------------------------------- */
    @Test
    public void validateExactlyOneValidSchemaReturnsNoErrors() {
        // arrange
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext);
        // first schema fails, second succeeds
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // act
        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // assert – no validation messages because exactly one schema matched
        assertTrue("No errors expected when a single schema matches", result.isEmpty());
        // the validator state must have been marked as a match
        verify(validatorState).setMatchedNode(true);
    }

    /* --------------------------------------------------------------------- *
     *  3. validate – ZERO schemas match
     * --------------------------------------------------------------------- */
    @Test
    public void validateZeroValidSchemasReturnsOneOfErrorAndChildErrors() {
        // arrange
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext);

        ValidationMessage err0 = mock(ValidationMessage.class);
        ValidationMessage err1 = mock(ValidationMessage.class);
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(err0));
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(err1));

        // act
        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // assert – the result must contain the OneOf error plus the two child errors
        assertEquals("Expected three messages (one OneOf + two child errors)", 3, result.size());
        // one of the messages is the generic OneOf error (its content is not inspected)
        assertTrue("Result must contain the child error from schema 0", result.contains(err0));
        assertTrue("Result must contain the child error from schema 1", result.contains(err1));
    }

    /* --------------------------------------------------------------------- *
     *  4. validate – MORE THAN ONE schema matches (short‑circuit)
     * --------------------------------------------------------------------- */
    @Test
    public void validateMultipleValidSchemasTriggersOneOfError() {
        // arrange
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext);

        // both schemas succeed (empty error sets)
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // act
        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // assert – a OneOf error must be present; no child errors because both matched
        assertEquals("Only the OneOf validation message should be present", 1, result.size());
        ValidationMessage onlyMessage = result.iterator().next();
        assertNotNull("The OneOf validation message must not be null", onlyMessage);
    }

    /* --------------------------------------------------------------------- *
     *  5. validate – failFast mode (exception is thrown)
     * --------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void validateFailFastThrowsExceptionWhenNumberOfValidSchemasIsNotOne() throws Exception {
        // arrange – build a validator and force failFast = true via reflection
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext);
        // make both schemas succeed so that numberOfValidSchema = 2
        when(childSchema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // use reflection to set the protected field 'failFast' defined in BaseJsonValidator
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // act – this must throw JsonSchemaException
        validator.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), "$");
    }

    /* --------------------------------------------------------------------- *
     *  6. walk – shouldValidateSchema = true (delegates to validate)
     * --------------------------------------------------------------------- */
    @Test
    public void walkWhenShouldValidateSchemaTrueDelegatesToValidate() {
        // arrange
        OneOfValidator validator = spy(new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext));
        Set<ValidationMessage> fakeResult = Collections.singleton(mock(ValidationMessage.class));
        // force the spy to return a known set from validate()
        doReturn(fakeResult).when(validator).validate(any(), any(), any(), anyString());

        // act
        Set<ValidationMessage> walkResult = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", true);

        // assert
        assertSame("walk should return exactly what validate returned", fakeResult, walkResult);
        verify(validator, times(1)).validate(executionContext, mock(JsonNode.class),
                mock(JsonNode.class), "$");
    }

    /* --------------------------------------------------------------------- *
     *  7. walk – shouldValidateSchema = false (just walks child schemas)
     * --------------------------------------------------------------------- */
    @Test
    public void walkWhenShouldValidateSchemaFalseWalksChildrenOnly() {
        // arrange
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext);
        // child schemas are mocked – we only care that walk() is invoked on them
        when(childSchema0.walk(any(), any(), any(), anyString(), eq(false)))
                .thenReturn(Collections.emptySet());
        when(childSchema1.walk(any(), any(), any(), anyString(), eq(false)))
                .thenReturn(Collections.emptySet());

        // act
        Set<ValidationMessage> result = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", false);

        // assert – walk should return an empty set and both child walks must have been called
        assertTrue("walk with shouldValidateSchema=false must return an empty set", result.isEmpty());
        verify(childSchema0, times(1)).walk(executionContext, any(), any(), eq("$"), eq(false));
        verify(childSchema1, times(1)).walk(executionContext, any(), any(), eq("$"), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  8. preloadJsonSchema – each child schema must be initialised
     * --------------------------------------------------------------------- */
    @Test
    public void preloadJsonSchemaInitialisesAllChildSchemas() {
        // arrange
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, schemaNode,
                mock(JsonSchema.class), validationContext);

        // act
        validator.preloadJsonSchema();

        // assert – each child schema's initialiseValidators() must have been invoked once
        verify(childSchema0, times(1)).initializeValidators();
        verify(childSchema1, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- *
     *  Utility class – reflection helpers used by the test suite
     * --------------------------------------------------------------------- */
    private static class TestUtils {
        /** Retrieves a private field value via reflection (used only for test assertions). */
        static Object getPrivateField(Object target, String fieldName) {
            try {
                java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
```
**###Test END##**