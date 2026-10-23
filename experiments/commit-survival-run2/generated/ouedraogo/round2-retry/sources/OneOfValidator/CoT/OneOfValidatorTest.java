/**
 * JUnit 4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests cover:
 * 1. All public methods of the class.
 * 2. Normal behaviour when exactly one subschema validates.
 * 3. Error handling when no subschema validates or more than one validates.
 * 4. Fail‑fast mode (exception is thrown).
 * 5. Edge cases such as an empty list of subschemas.
 * 6. The {@code walk} and {@code preloadJsonSchema} helper methods.
 *
 * The test class heavily uses Mockito to isolate {@code OneOfValidator}
 * from the rest of the validation framework.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class OneOfValidatorTest {

    private static final String DUMMY_SCHEMA_PATH = "$.test";

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private CollectorContext collectorContext;
    private Scope mockScope;
    private ValidatorState validatorState;

    /** Two schema mocks that will be injected into the validator. */
    private JsonSchema schemaMock1;
    private JsonSchema schemaMock2;

    /** The validator under test. */
    private OneOfValidator validator;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();

        /* ---------- Mocks for the surrounding framework ---------- */
        validationContext = mock(ValidationContext.class);
        executionContext   = mock(ExecutionContext.class);
        collectorContext   = mock(CollectorContext.class);
        mockScope          = mock(Scope.class);
        validatorState     = mock(ValidatorState.class);

        // ExecutionContext → CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // CollectorContext → dynamic scopes
        when(collectorContext.enterDynamicScope()).thenReturn(mockScope);
        when(collectorContext.exitDynamicScope()).thenReturn(mockScope);
        // CollectorContext → stored validator state
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // Evaluated collections (cleared by the validator)
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // Default behaviour of the validator state
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // The validator will set these flags during execution – we let the mock
        // simply accept the calls.
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());

        /* ---------- Create two distinct schema mocks ---------- */
        schemaMock1 = mock(JsonSchema.class);
        schemaMock2 = mock(JsonSchema.class);

        // ValidationContext#newSchema must return the two mocks in order.
        // The validator will call it once per child node.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaMock1)
                .thenReturn(schemaMock2);

        /* ---------- Build a simple schema array node (two child schemas) ---------- */
        ArrayNode schemaArrayNode = mapper.createArrayNode();
        schemaArrayNode.add(mapper.createObjectNode()); // child 0
        schemaArrayNode.add(mapper.createObjectNode()); // child 1

        /* ---------- Instantiate the validator ---------- */
        validator = new OneOfValidator(DUMMY_SCHEMA_PATH, schemaArrayNode, null, validationContext);

        /* ---------- Replace the internally created schema list with our mocks ----------
         * The constructor already filled the list via ValidationContext#newSchema,
         * but we replace it to guarantee we have full control over each entry.
         */
        java.lang.reflect.Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        List<JsonSchema> schemaList = new ArrayList<>();
        schemaList.add(schemaMock1);
        schemaList.add(schemaMock2);
        schemasField.set(validator, schemaList);
    }

    /** --------------------------------------------------------------------
     *  PUBLIC METHOD LIST (extracted from the source)
     *
     *  1. OneOfValidator(String schemaPath,
     *                     JsonNode schemaNode,
     *                     JsonSchema parentSchema,
     *                     ValidationContext validationContext)
     *
     *  2. Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                    JsonNode node,
     *                                    JsonNode rootNode,
     *                                    String at)
     *
     *  3. Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                 JsonNode node,
     *                                 JsonNode rootNode,
     *                                 String at,
     *                                 boolean shouldValidateSchema)
     *
     *  4. void preloadJsonSchema()
     * --------------------------------------------------------------------*/

    /* ====================== BASIC FUNCTIONAL TESTS ====================== */

    @Test
    public void testValidateExactlyOneSchemaValidReturnsNoErrors() {
        // schemaMock1 -> returns a non‑empty error set (invalid)
        // schemaMock2 -> returns an empty set (valid)
        Set<ValidationMessage> errorsFromFirst = Collections.singleton(mock(ValidationMessage.class));
        when(schemaMock1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorsFromFirst);
        when(schemaMock2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode dummyNode = mapper.createObjectNode(); // the actual payload is irrelevant for the mocks
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, "$");

        // Exactly one schema matched → no errors should be reported
        assertTrue("Result should be empty when exactly one subschema validates", result.isEmpty());

        // Verify state flag changes
        verify(validatorState).setComplexValidator(true);
        verify(validatorState).setMatchedNode(true);
    }

    @Test
    public void testValidateNoSchemaValidReturnsOneOfError() {
        // Both subschemas return errors → numberOfValidSchema == 0
        Set<ValidationMessage> errSet = Collections.singleton(mock(ValidationMessage.class));
        when(schemaMock1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errSet);
        when(schemaMock2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errSet);

        JsonNode dummyNode = mapper.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, "$");

        // Expect a ONE_OF error plus the child errors
        assertFalse("Result must contain errors when no schema validates", result.isEmpty());

        // The first message should be the ONE_OF message and its text must contain the number of valid schemas (0)
        ValidationMessage first = result.iterator().next();
        assertTrue("ONE_OF message should contain the number of valid schemas",
                first.getMessage().contains("0"));

        // Child errors should also be present
        assertEquals("Exactly two child errors should be present", 2, result.size() - 1);
    }

    @Test
    public void testValidateMultipleSchemasValidReturnsOneOfError() {
        // Both subschemas validate successfully → numberOfValidSchema == 2
        when(schemaMock1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaMock2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode dummyNode = mapper.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, "$");

        // Expect ONE_OF error because more than one schema matched
        assertFalse("Result must contain errors when more than one schema validates", result.isEmpty());
        ValidationMessage first = result.iterator().next();
        assertTrue("ONE_OF message should contain the number of valid schemas",
                first.getMessage().contains("2"));
    }

    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFastThrowsException() throws Exception {
        // Enable fail‑fast mode via reflection (the field is defined in BaseJsonValidator)
        java.lang.reflect.Field failFastField = BaseJsonValidator.class.getDeclaredField("failFast");
        failFastField.setAccessible(true);
        failFastField.setBoolean(validator, true);

        // Both subschemas return errors → validation would normally return a set,
        // but with failFast enabled a JsonSchemaException must be thrown.
        Set<ValidationMessage> errSet = Collections.singleton(mock(ValidationMessage.class));
        when(schemaMock1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errSet);
        when(schemaMock2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errSet);

        JsonNode dummyNode = mapper.createObjectNode();
        validator.validate(executionContext, dummyNode, dummyNode, "$");
        // exception expected → no further assertions
    }

    @Test
    public void testValidateWithEmptySchemasArrayReturnsOneOfError() throws Exception {
        // Build a validator with an empty schema array node
        ArrayNode emptyArray = mapper.createArrayNode(); // size == 0
        OneOfValidator emptyValidator = new OneOfValidator(DUMMY_SCHEMA_PATH, emptyArray, null, validationContext);

        // No schemas → the internal list is empty; validation should still produce ONE_OF error
        JsonNode dummyNode = mapper.createObjectNode();
        Set<ValidationMessage> result = emptyValidator.validate(executionContext, dummyNode, dummyNode, "$");

        assertFalse("Result must contain a ONE_OF error when there are no subschemas", result.isEmpty());
        ValidationMessage message = result.iterator().next();
        assertTrue("ONE_OF message should indicate 0 valid schemas", message.getMessage().contains("0"));
    }

    /* ====================== WALK METHOD TESTS ====================== */

    @Test
    public void testWalkShouldValidateCallsValidate() {
        // Force the validator to walk with shouldValidateSchema = true.
        // The walk method delegates to validate() – we verify that the call happens.
        OneOfValidator spyValidator = spy(validator);
        JsonNode dummyNode = mapper.createObjectNode();

        // Stub validate to return a known set
        Set<ValidationMessage> fakeResult = Collections.singleton(mock(ValidationMessage.class));
        doReturn(fakeResult).when(spyValidator).validate(executionContext, dummyNode, dummyNode, "$");

        Set<ValidationMessage> result = spyValidator.walk(executionContext, dummyNode, dummyNode, "$", true);
        assertEquals("walk should return exactly what validate returns", fakeResult, result);
        verify(spyValidator, times(1)).validate(executionContext, dummyNode, dummyNode, "$");
    }

    @Test
    public void testWalkShouldNotValidateDelegatesToSchemas() {
        // When shouldValidateSchema == false the validator must call walk on each child schema.
        JsonNode dummyNode = mapper.createObjectNode();

        // Make the child schemas return empty sets (the return value of walk is ignored)
        when(schemaMock1.walk(executionContext, dummyNode, dummyNode, "$", false)).thenReturn(Collections.emptySet());
        when(schemaMock2.walk(executionContext, dummyNode, dummyNode, "$", false)).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode, dummyNode, "$", false);
        // The method should return an empty set because we asked it not to validate.
        assertTrue("walk with shouldValidateSchema=false must return an empty set", result.isEmpty());

        verify(schemaMock1, times(1)).walk(executionContext, dummyNode, dummyNode, "$", false);
        verify(schemaMock2, times(1)).walk(executionContext, dummyNode, dummyNode, "$", false);
    }

    /* ====================== PRELOAD TEST ====================== */

    @Test
    public void testPreloadJsonSchemaInitializesAllSchemas() {
        // preloadJsonSchema should call initializeValidators() on each child schema.
        validator.preloadJsonSchema();

        verify(schemaMock1, times(1)).initializeValidators();
        verify(schemaMock2, times(1)).initializeValidators();
    }
}
