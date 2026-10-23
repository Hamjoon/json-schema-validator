###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Construction – ensuring the validator builds its internal schema list
 *       from the supplied {@link JsonNode} and {@link ValidationContext}.</li>
 *   <li>Successful validation when exactly one sub‑schema matches.</li>
 *   <li>Failure when none or more than one sub‑schema matches.</li>
 *   <li>Fail‑fast behaviour (exception is thrown).</li>
 *   <li>Walk delegation for both {@code shouldValidateSchema = true} and {@code false}.</li>
 *   <li>Pre‑loading of sub‑schemas via {@link #preloadJsonSchema()}.</li>
 * </ul>
 *
 * <p>All external collaborators are mocked with Mockito – this isolates the
 * {@code OneOfValidator} logic from the rest of the library.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private CollectorContext.Scope mockScope;

    @Mock
    private ValidatorState validatorState;

    /** Helper to build a validator with a given list of mocked {@link JsonSchema}s. */
    private OneOfValidator buildValidator(List<JsonSchema> schemaMocks, boolean failFast) throws Exception {
        // Build a dummy schema node (an array) – the actual content is irrelevant
        ArrayNode schemaNode = MAPPER.createArrayNode();
        for (int i = 0; i < schemaMocks.size(); i++) {
            schemaNode.add(MAPPER.createObjectNode()); // placeholder child node
        }

        // ValidationContext#newSchema(...) will be called sequentially for each child node.
        // Configure it to return the supplied mocks in order.
        OngoingStubbing<JsonSchema> stub = when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)));
        for (JsonSchema mock : schemaMocks) {
            stub = stub.thenReturn(mock);
        }

        OneOfValidator validator = new OneOfValidator("testPath", schemaNode, null, validationContext);

        // Set the failFast flag (protected in BaseJsonValidator) via reflection
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, failFast);

        // Mock the execution context to return our collector context
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext dynamic scope handling
        when(collectorContext.enterDynamicScope()).thenReturn(mockScope);
        when(collectorContext.exitDynamicScope()).thenReturn(mockScope);

        // The validator stores a ValidatorState object in the collector context.
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // Default behaviour for the state
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // The evaluated‑items/properties collections are cleared on error
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        return validator;
    }

    @Before
    public void setUp() {
        // Ensure every test starts with a clean mock state
        Mockito.reset(validationContext, executionContext, collectorContext, mockScope, validatorState);
    }

    /**
     * Exactly one sub‑schema validates – the validator should return an empty set
     * and leave the {@link ValidatorState} in a matched state.
     */
    @Test
    public void testValidateExactlyOneSchemaValid() throws Exception {
        // Two schemas: first fails, second succeeds
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.singleton(
                ValidationMessage.builder().code("error0").path("$").message("first fails").build()));
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1), false);

        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, "$");

        // No errors because exactly one schema matched
        assertTrue("Result should be empty when exactly one schema matches", result.isEmpty());

        // Verify state flag handling
        verify(validatorState).setComplexValidator(true);
        verify(validatorState).setMatchedNode(true);
        verify(validatorState, never()).setMatchedNode(false);
    }

    /**
     * No sub‑schema validates – the validator must return a ONE_OF error
     * together with the collected child errors.
     */
    @Test
    public void testValidateNoSchemaValid() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        ValidationMessage child0 = ValidationMessage.builder().code("c0").path("$").message("c0 error").build();
        ValidationMessage child1 = ValidationMessage.builder().code("c1").path("$").message("c1 error").build();

        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.singleton(child0));
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.singleton(child1));

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1), false);

        JsonNode instance = MAPPER.readTree("{\"b\":2}");
        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, "$");

        // Should contain the ONE_OF message plus both child errors
        assertEquals("There should be three messages (ONE_OF + two child errors)", 3, result.size());

        // The ONE_OF message's code must be the one defined for the validator type
        Optional<ValidationMessage> oneOfMsg = result.stream()
                .filter(m -> m.getCode().equals(ValidatorTypeCode.ONE_OF.getErrorCodeKey()))
                .findFirst();
        assertTrue("ONE_OF validation message must be present", oneOfMsg.isPresent());

        // Child errors must be present
        assertTrue(result.contains(child0));
        assertTrue(result.contains(child1));
    }

    /**
     * More than one sub‑schema validates – validation must short‑circuit and
     * report a ONE_OF error (the validator does not consider the instance valid).
     */
    @Test
    public void testValidateMultipleSchemasValid() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        // First two schemas succeed, third is not evaluated because of short‑circuit
        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        // schema2.validate should never be called
        when(schema2.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1, schema2), false);

        JsonNode instance = MAPPER.readTree("{\"c\":3}");
        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, "$");

        // ONE_OF error only (no child errors because validation stopped early)
        assertEquals("Only ONE_OF error expected when >1 schemas match", 1, result.size());

        ValidationMessage msg = result.iterator().next();
        assertEquals("Error code must be ONE_OF", ValidatorTypeCode.ONE_OF.getErrorCodeKey(), msg.getCode());
        assertEquals("Number of valid schemas should be reported as 2", "2", msg.getArguments()[0]);

        // Verify that the third schema was never invoked
        verify(schema2, never()).validate(any(), any(), any(), any());
    }

    /**
     * When {@code failFast} is true the validator must throw a {@link JsonSchemaException}
     * as soon as the ONE_OF rule is violated.
     */
    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFastThrowsException() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // Both schemas fail – numberOfValidSchema will be 0
        when(schema0.validate(any(), any(), any(), any())).thenReturn(
                Collections.singleton(ValidationMessage.builder().code("e0").path("$").message("e0").build()));
        when(schema1.validate(any(), any(), any(), any())).thenReturn(
                Collections.singleton(ValidationMessage.builder().code("e1").path("$").message("e1").build()));

        // Build validator with failFast = true
        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1), true);

        JsonNode instance = MAPPER.readTree("{\"d\":4}");
        // This call must raise JsonSchemaException
        validator.validate(executionContext, instance, instance, "$");
    }

    /**
     * {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)} with
     * {@code shouldValidateSchema = true} must delegate to {@link #validate(...)}.
     */
    @Test
    public void testWalkValidateTrueDelegatesToValidate() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Collections.singletonList(schema), false);

        // Spy the validator to verify that validate() is called from walk()
        OneOfValidator spyValidator = Mockito.spy(validator);

        JsonNode instance = MAPPER.readTree("{\"e\":5}");
        Set<ValidationMessage> result = spyValidator.walk(executionContext, instance, instance, "$", true);

        // The walk should return the same result as validate (empty set)
        assertTrue(result.isEmpty());
        verify(spyValidator, times(1)).validate(executionContext, instance, instance, "$");
    }

    /**
     * {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)} with
     * {@code shouldValidateSchema = false} must invoke {@code walk} on each child schema
     * but must not perform validation.
     */
    @Test
    public void testWalkValidateFalseCallsSchemaWalk() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // Walk on child schemas should be invoked
        doReturn(Collections.emptySet()).when(schema0).walk(any(), any(), any(), any(), eq(false));
        doReturn(Collections.emptySet()).when(schema1).walk(any(), any(), any(), any(), eq(false));

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1), false);

        JsonNode instance = MAPPER.readTree("{\"f\":6}");
        Set<ValidationMessage> result = validator.walk(executionContext, instance, instance, "$", false);

        // walk returns an empty set because we asked not to validate
        assertTrue(result.isEmpty());

        verify(schema0, times(1)).walk(executionContext, instance, instance, "$", false);
        verify(schema1, times(1)).walk(executionContext, instance, instance, "$", false);
    }

    /**
     * {@link OneOfValidator#preloadJsonSchema()} must call {@code initializeValidators()}
     * on each child schema.
     */
    @Test
    public void testPreloadJsonSchemaInitializesAllSchemas() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1, schema2), false);
        validator.preloadJsonSchema();

        verify(schema0, times(1)).initializeValidators();
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    /**
     * Edge case: validator constructed with an empty schema array.
     * Validation should always fail with ONE_OF error (0 valid schemas).
     */
    @Test
    public void testEmptySchemaArrayAlwaysFails() throws Exception {
        // Empty schema list – no need to mock any child schemas
        OneOfValidator validator = buildValidator(Collections.emptyList(), false);

        JsonNode instance = MAPPER.readTree("{\"g\":7}");
        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, "$");

        assertEquals("Exactly one ONE_OF error expected when no sub‑schemas are defined", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals("Error code must be ONE_OF", ValidatorTypeCode.ONE_OF.getErrorCodeKey(), msg.getCode());
        assertEquals("Number of valid schemas reported should be 0", "0", msg.getArguments()[0]);
    }
}
```
###Test END##