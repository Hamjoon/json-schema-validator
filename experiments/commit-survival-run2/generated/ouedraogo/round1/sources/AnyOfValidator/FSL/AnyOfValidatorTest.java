package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AnyOfValidator}. The tests use Mockito to
 * isolate the validator from the rest of the library and focus on the
 * control‑flow inside {@code validate()}, {@code walk()} and
 * {@code preloadJsonSchema()}.
 *
 * <p>Because {@code AnyOfValidator} depends on many internal classes
 * (e.g. {@link ValidationContext}, {@link ExecutionContext},
 * {@link CollectorContext}, {@link ValidatorState}), the test creates
 * comprehensive mock objects that mimic the required behaviour.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Successful validation when at least one subschema matches.</li>
 *   <li>Type‑mismatch handling (the validator adds a type error).</li>
 *   <li>Filtering of “required” errors when a valid subschema exists.</li>
 *   <li>Behaviour of {@code walk()} with both {@code shouldValidateSchema}
 *       values.</li>
 *   <li>That {@code preloadJsonSchema()} forwards the list of subschemas
 *       to {@code BaseJsonValidator#preloadJsonSchemas(List)}.</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // --------------------------------------------------------------------
    // Common mocks used by several test cases
    // --------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private JsonNode schemaNode;               // the “anyOf” node
    @Mock private JsonSchema parentSchema;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope dynamicScope;
    @Mock private Scope parentScope;
    @Mock private ValidatorState validatorState;

    @Before
    public void setUp() {
        // ValidationContext returns its config
        when(validationContext.getConfig()).thenReturn(config);
        // By default OpenAPI‑style discriminators are disabled
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // CollectorContext always returns a fresh dynamic scope
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope);
        // The validator stores a ValidatorState object under a known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // By default the validator is not in “walk” mode
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // The state reports that no node has been matched yet
        when(validatorState.hasMatchedNode()).thenReturn(false);
        // ExecutionContext simply returns the mocked CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /**
     * Helper that builds an {@link AnyOfValidator} containing the supplied
     * list of {@link JsonSchema} mocks as its subschemas.
     *
     * @param subschemas the subschemas that the validator must evaluate
     * @return a fully initialised {@code AnyOfValidator}
     */
    private AnyOfValidator buildValidator(List<JsonSchema> subschemas) {
        // the JsonNode representing the “anyOf” keyword must report its size
        when(schemaNode.size()).thenReturn(subschemas.size());

        // ValidationContext creates a new JsonSchema for each element of the array
        // (the real implementation would read the child node – here we simply
        // return the mocks that the test supplied)
        for (int i = 0; i < subschemas.size(); i++) {
            JsonSchema mockSchema = subschemas.get(i);
            when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(mockSchema);
        }

        // Build the validator – the constructor will populate its internal list
        return new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);
    }

    // --------------------------------------------------------------------
    // 1. Successful validation – at least one subschema matches, no
    //    discriminators are active.
    // --------------------------------------------------------------------
    @Test
    public void testValidate_AllValidSubschemas_NoDiscriminator_ReturnsEmptySet() {
        // ----- arrange ---------------------------------------------------
        // A single subschema that validates without producing errors
        JsonSchema happySchema = mock(JsonSchema.class);
        when(happySchema.hasTypeValidator()).thenReturn(false);
        when(happySchema.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), "#"))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(Collections.singletonList(happySchema));

        // ----- act -------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),   // node
                mock(JsonNode.class),   // rootNode
                "#");

        // ----- assert ----------------------------------------------------
        assertNotNull("Result must never be null", result);
        assertTrue("When a subschema validates, the result must be empty", result.isEmpty());

        // Verify that the validator asked the subschema to validate exactly once
        verify(happySchema, times(1)).validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("#"));
    }

    // --------------------------------------------------------------------
    // 2. Type‑mismatch handling – the subschema has a TypeValidator whose
    //    type does NOT match the instance node.
    // --------------------------------------------------------------------
    @Test
    public void testValidate_TypeMismatch_AddsTypeError() {
        // ----- arrange ---------------------------------------------------
        JsonSchema typeSchema = mock(JsonSchema.class);
        TypeValidator typeValidator = mock(TypeValidator.class);
        JsonType schemaType = JsonType.STRING; // any non‑UNION type

        when(typeSchema.hasTypeValidator()).thenReturn(true);
        when(typeSchema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(schemaType);
        // Force a mismatch
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);
        // The validator would normally add a type error and then continue
        // (no further validation of this subschema)
        when(typeValidator.getSchemaType()).thenReturn(schemaType);

        AnyOfValidator validator = buildValidator(Collections.singletonList(typeSchema));

        // ----- act -------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),   // node
                mock(JsonNode.class),   // rootNode
                "#");

        // ----- assert ----------------------------------------------------
        assertNotNull(result);
        // The validator adds exactly one error – the type‑mismatch message
        assertEquals("A type mismatch should produce a single ValidationMessage", 1, result.size());

        ValidationMessage msg = result.iterator().next();
        // The message type is the textual representation of the schema type
        assertEquals("The error type must be the schema type that did not match",
                schemaType.toString(), msg.getType());
    }

    // --------------------------------------------------------------------
    // 3. Required‑error filtering – one subschema fails with a “required”
    //    error, another subschema validates successfully. The final result
    //    must be empty because the only errors are of type REQUIRED.
    // --------------------------------------------------------------------
    @Test
    public void testValidate_FiltersRequiredErrorsWhenValidSubschemaExists() {
        // ----- arrange ---------------------------------------------------
        // Subschema #1 – produces a REQUIRED error
        JsonSchema requiredSchema = mock(JsonSchema.class);
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getType()).thenReturn(ValidatorTypeCode.REQUIRED.getValue());
        when(requiredSchema.hasTypeValidator()).thenReturn(false);
        when(requiredSchema.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), "#"))
                .thenReturn(Collections.singleton(requiredMsg));

        // Subschema #2 – validates without errors
        JsonSchema happySchema = mock(JsonSchema.class);
        when(happySchema.hasTypeValidator()).thenReturn(false);
        when(happySchema.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), "#"))
                .thenReturn(Collections.emptySet());

        // Force the ValidatorState to report that a node has been matched
        // (otherwise the validator would ignore the successful subschema)
        when(validatorState.hasMatchedNode()).thenReturn(true);

        AnyOfValidator validator = buildValidator(Arrays.asList(requiredSchema, happySchema));

        // ----- act -------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                "#");

        // ----- assert ----------------------------------------------------
        assertNotNull(result);
        // Because the only errors are “required” and a subschema succeeded,
        // the final error set must be empty.
        assertTrue("Required errors must be filtered out when a subschema succeeds", result.isEmpty());

        // Verify that both subschemas were invoked
        verify(requiredSchema, times(1)).validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("#"));
        verify(happySchema, times(1)).validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("#"));
    }

    // --------------------------------------------------------------------
    // 4. walk() – when shouldValidateSchema == true, the method must delegate
    //    to validate().
    // --------------------------------------------------------------------
    @Test
    public void testWalk_ValidateWhenShouldValidateIsTrue() {
        // ----- arrange ---------------------------------------------------
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.hasTypeValidator()).thenReturn(false);
        when(schema.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), "#"))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(Collections.singletonList(schema));

        // Spy on the validator to capture the call to validate()
        AnyOfValidator spy = Mockito.spy(validator);
        doReturn(Collections.singleton(mock(ValidationMessage.class)))
                .when(spy).validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("#"));

        // ----- act -------------------------------------------------------
        Set<ValidationMessage> result = spy.walk(executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                "#",
                true); // shouldValidateSchema = true

        // ----- assert ----------------------------------------------------
        assertNotNull(result);
        // The spy returns the set we programmed above
        assertEquals(1, result.size());
        // Verify that validate() was indeed invoked
        verify(spy, times(1)).validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("#"));
    }

    // --------------------------------------------------------------------
    // 5. walk() – when shouldValidateSchema == false, each subschema’s walk()
    //    method must be called and an empty set must be returned.
    // --------------------------------------------------------------------
    @Test
    public void testWalk_NoValidateCallsSubschemaWalk() {
        // ----- arrange ---------------------------------------------------
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        // The subschemas' walk() methods are expected to be called with
        // shouldValidate = false
        when(schemaA.walk(executionContext, mock(JsonNode.class), mock(JsonNode.class), "#", false))
                .thenReturn(Collections.emptySet());
        when(schemaB.walk(executionContext, mock(JsonNode.class), mock(JsonNode.class), "#", false))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(Arrays.asList(schemaA, schemaB));

        // ----- act -------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                "#",
                false); // shouldValidateSchema = false

        // ----- assert ----------------------------------------------------
        assertNotNull(result);
        assertTrue("When validation is disabled walk() must return an empty set", result.isEmpty());

        // Verify that each subschema's walk() method was called once
        verify(schemaA, times(1)).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("#"), eq(false));
        verify(schemaB, times(1)).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("#"), eq(false));
    }

    // --------------------------------------------------------------------
    // 6. preloadJsonSchema() – the validator must forward its internal list
    //    of subschemas to BaseJsonValidator#preloadJsonSchemas(List).
    // --------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema_DelegatesToBaseClass() {
        // ----- arrange ---------------------------------------------------
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        List<JsonSchema> subschemas = Arrays.asList(schema1, schema2);

        // Use a concrete subclass that overrides the protected helper
        // method so we can capture the argument.
        class TestableAnyOfValidator extends AnyOfValidator {
            List<JsonSchema> captured;

            TestableAnyOfValidator(String path, JsonNode node, JsonSchema parent, ValidationContext ctx) {
                super(path, node, parent, ctx);
            }

            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                this.captured = schemas;
            }
        }

        // Build the validator (the constructor will fill the internal list)
        TestableAnyOfValidator validator = new TestableAnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);
        // Manually inject the subschemas list (the constructor already added them,
        // but we want a deterministic list for the assertion)
        validator.schemas.addAll(subschemas);

        // ----- act -------------------------------------------------------
        validator.preloadJsonSchema();

        // ----- assert ----------------------------------------------------
        assertNotNull("The overridden method must have been called", validator.captured);
        assertEquals("Both subschemas must be passed to preloadJsonSchemas", subschemas, validator.captured);
    }
}
