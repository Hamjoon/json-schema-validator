/**
 * JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests cover:
 * 1. Construction of the internal schema list.
 * 2. Normal validation where exactly one child schema matches.
 * 3. Validation where no child schema matches.
 * 4. Validation where more than one child schema matches.
 * 5. Fail‑fast behaviour (exception is thrown).
 * 6. The {@code walk} method in both “validate” and “skip‑validate” modes.
 * 7. {@code preloadJsonSchema} – verifies that each child schema is initialised.
 *
 * The test uses Mockito to mock the heavy‑weight collaborators
 * (ExecutionContext, CollectorContext, Scope, JsonSchema, ValidationContext, etc.).
 * A small reflective helper is used to inject the list of mocked child schemas
 * because the constructor builds the list internally from a JsonNode.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private Scope dummyScope;

    @Mock
    private JsonSchema schema0;

    @Mock
    private JsonSchema schema1;

    @Mock
    private JsonSchema schema2; // used for the “empty schema list” test

    private OneOfValidator validator;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // Common stubbing for CollectorContext used by every test case
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // A real ValidatorState instance is easier to work with than a mock,
        // because the validator manipulates its internal flags.
        ValidatorState validatorState = new ValidatorState();
        // By default we want the “walk” mode to be disabled.
        validatorState.setWalkEnabled(false);
        validatorState.setValidationEnabled(true);
        validatorState.setMatchedNode(true);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // -----------------------------------------------------------------
        // Build a OneOfValidator with an empty schema array – we will inject
        // the real list of mocked schemas via reflection.
        // -----------------------------------------------------------------
        ArrayNode emptyArray = MAPPER.createArrayNode(); // size = 0
        validator = new OneOfValidator("testPath", emptyArray, null, validationContext);

        // inject the mocked child schemas (schema0, schema1) into the private field
        List<JsonSchema> schemaList = new ArrayList<>(Arrays.asList(schema0, schema1));
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, schemaList);
    }

    /**
     * Helper that creates a ValidationMessage set containing a single message
     * with the supplied text.  The concrete message content is not important
     * for the logic of OneOfValidator – only the *presence* of a message.
     */
    private Set<ValidationMessage> singleMessage(String text) {
        ValidationMessage vm = ValidationMessage.builder()
                .code("anyCode")
                .path("$")
                .message(text)
                .build();
        return Collections.singleton(vm);
    }

    // -----------------------------------------------------------------
    // 1. Exactly one schema matches → no validation errors.
    // -----------------------------------------------------------------
    @Test
    public void testValidate_ExactlyOneSchemaMatches() {
        // schema0 matches (empty error set)
        when(schema0.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        // schema1 does NOT match (returns a non‑empty set)
        when(schema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(singleMessage("schema1 error"));

        Set<ValidationMessage> result = validator.validate(executionContext,
                MAPPER.createObjectNode(), // node – content is irrelevant for the mock
                MAPPER.createObjectNode(),
                "$");

        // Expect no errors because exactly one schema succeeded.
        assertTrue("Result should be empty when a single schema matches", result.isEmpty());

        // Verify that the state was marked as matched.
        ValidatorState state = (ValidatorState) collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY);
        assertTrue(state.isMatchedNode());
    }

    // -----------------------------------------------------------------
    // 2. No schema matches → OneOf error + child errors.
    // -----------------------------------------------------------------
    @Test
    public void testValidate_NoSchemaMatches() {
        when(schema0.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(singleMessage("error0"));
        when(schema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(singleMessage("error1"));

        Set<ValidationMessage> result = validator.validate(executionContext,
                MAPPER.createObjectNode(),
                MAPPER.createObjectNode(),
                "$");

        // There must be at least the OneOf message plus the two child messages.
        assertEquals("Expected 3 messages (OneOf + 2 child)", 3, result.size());

        // The OneOf message contains the number of valid schemas (0)
        Optional<ValidationMessage> oneOf = result.stream()
                .filter(m -> m.getCode().equals(ValidatorTypeCode.ONE_OF.getErrorCodeKey()))
                .findFirst();
        assertTrue("OneOf validation message should be present", oneOf.isPresent());
        assertTrue("OneOf message should contain '0' as the count",
                oneOf.get().getMessage().contains("0"));
    }

    // -----------------------------------------------------------------
    // 3. More than one schema matches → OneOf error (value 2) and no child errors.
    // -----------------------------------------------------------------
    @Test
    public void testValidate_MultipleSchemasMatch() {
        // Both schemas succeed (empty error sets)
        when(schema0.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext,
                MAPPER.createObjectNode(),
                MAPPER.createObjectNode(),
                "$");

        // OneOf error only (because childErrors stays empty)
        assertEquals("Only the OneOf error should be present", 1, result.size());

        ValidationMessage vm = result.iterator().next();
        assertEquals("Error code must be ONE_OF", ValidatorTypeCode.ONE_OF.getErrorCodeKey(), vm.getCode());
        assertTrue("Message should indicate two valid schemas", vm.getMessage().contains("2"));
    }

    // -----------------------------------------------------------------
    // 4. Fail‑fast mode – JsonSchemaException is thrown as soon as the
    //    validation result is known to be invalid.
    // -----------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testValidate_FailFastThrowsException() throws Exception {
        // make validator failFast = true via reflection
        Field failFastField = BaseJsonValidator.class.getDeclaredField("failFast");
        failFastField.setAccessible(true);
        failFastField.set(validator, true);

        // No schema matches → triggers the exception
        when(schema0.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(singleMessage("error0"));
        when(schema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(singleMessage("error1"));

        validator.validate(executionContext,
                MAPPER.createObjectNode(),
                MAPPER.createObjectNode(),
                "$");
        // exception expected – no further assertions
    }

    // -----------------------------------------------------------------
    // 5. walk(.., shouldValidateSchema = true) delegates to validate().
    // -----------------------------------------------------------------
    @Test
    public void testWalk_ValidateMode() {
        // Spy on the validator to verify that validate() is invoked.
        OneOfValidator spyValidator = Mockito.spy(validator);

        // make validate return a known set
        Set<ValidationMessage> dummySet = Collections.singleton(
                ValidationMessage.builder()
                        .code("dummy")
                        .path("$")
                        .message("dummy")
                        .build());
        doReturn(dummySet).when(spyValidator).validate(any(), any(JsonNode.class), any(JsonNode.class), anyString());

        Set<ValidationMessage> result = spyValidator.walk(executionContext,
                MAPPER.createObjectNode(),
                MAPPER.createObjectNode(),
                "$",
                true); // shouldValidateSchema

        verify(spyValidator, times(1)).validate(any(), any(JsonNode.class), any(JsonNode.class), anyString());
        assertEquals("walk should return exactly what validate returned", dummySet, result);
    }

    // -----------------------------------------------------------------
    // 6. walk(.., shouldValidateSchema = false) walks each child schema
    //    but does NOT collect any validation messages.
    // -----------------------------------------------------------------
    @Test
    public void testWalk_SkipValidateMode() {
        // make the underlying schemas return a non‑empty set when walk() is called
        when(schema0.walk(any(), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false)))
                .thenReturn(singleMessage("walk0"));
        when(schema1.walk(any(), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false)))
                .thenReturn(singleMessage("walk1"));

        Set<ValidationMessage> result = validator.walk(executionContext,
                MAPPER.createObjectNode(),
                MAPPER.createObjectNode(),
                "$",
                false); // shouldValidateSchema

        // verify that each schema's walk method was called exactly once
        verify(schema0, times(1)).walk(any(), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false));
        verify(schema1, times(1)).walk(any(), any(JsonNode.class), any(JsonNode.class), anyString(), eq(false));

        // walk should return an empty set when validation is skipped
        assertTrue("walk should return empty set when shouldValidateSchema = false", result.isEmpty());
    }

    // -----------------------------------------------------------------
    // 7. preloadJsonSchema() must invoke initializeValidators() on each child.
    // -----------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema() {
        validator.preloadJsonSchema();

        verify(schema0, times(1)).initializeValidators();
        verify(schema1, times(1)).initializeValidators();
    }

    // -----------------------------------------------------------------
    // 8. Edge case – validator built with an empty schema list.
    //    The result must be a ONE_OF error with a count of 0.
    // -----------------------------------------------------------------
    @Test
    public void testValidate_EmptySchemaList() throws Exception {
        // Build a brand‑new validator whose internal list is empty.
        ArrayNode emptyArray = MAPPER.createArrayNode();
        OneOfValidator emptyValidator = new OneOfValidator("emptyPath", emptyArray, null, validationContext);

        // inject the empty list via reflection
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(emptyValidator, Collections.emptyList());

        // stub collector context as in setUp()
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        ValidatorState state = new ValidatorState();
        state.setWalkEnabled(false);
        state.setValidationEnabled(true);
        state.setMatchedNode(true);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        Set<ValidationMessage> result = emptyValidator.validate(executionContext,
                MAPPER.createObjectNode(),
                MAPPER.createObjectNode(),
                "$");

        assertEquals("One error expected (ONE_OF)", 1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertEquals("Error code must be ONE_OF", ValidatorTypeCode.ONE_OF.getErrorCodeKey(), vm.getCode());
        assertTrue("Message should indicate 0 valid schemas", vm.getMessage().contains("0"));
    }
}
