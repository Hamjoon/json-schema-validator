/**
 * JUnit 4 test suite for {@link AnyOfValidator}.
 *
 * The tests use Mockito to mock the complex collaborators of {@code AnyOfValidator}
 * (e.g. {@link ValidationContext}, {@link ExecutionContext}, {@link CollectorContext},
 * {@link JsonSchema}, {@link TypeValidator}, etc.).  The goal is to exercise the
 * different branches inside {@code validate(...)} and {@code walk(...)} without
 * needing the full schema‑validation runtime.
 *
 * The test class deliberately covers:
 *
 * 1. Successful validation when a subschema matches and returns no errors.
 * 2. Type‑mismatch handling (the “type validator” branch).
 * 3. Interaction of required‑property errors with a successful subschema.
 * 4. OpenAPI 3 discriminator handling (both match‑found with errors and the
 *    “no alternative could be chosen” error).
 * 5. The {@code walk(...)} method when validation is disabled.
 *
 * The tests are written against the public API only; reflection is used only
 * to manipulate the private {@code discriminatorContext} when the OpenAPI 3
 * discriminator feature is enabled.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private Scope grandParentScope;

    @Mock
    private Scope parentScope;

    @Mock
    private Scope childScope;

    @Mock
    private ValidatorState validatorState;

    /**
     * Common stubbing for collector context and validator state.
     */
    @Before
    public void setUpCommon() {
        // collectorContext dynamic scopes
        when(collectorContext.enterDynamicScope()).thenReturn(parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope, childScope);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // the validator state stored under the well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // configuration
        when(validationContext.getConfig()).thenReturn(config);
    }

    /**
     * Helper that creates an {@link AnyOfValidator} instance whose internal
     * {@code schemas} list contains the supplied {@code JsonSchema} mocks.
     *
     * @param discriminatorEnabled whether the OpenAPI 3 discriminator feature is on
     * @param schemas               the subschemas that the validator must evaluate
     * @return a fully initialised {@code AnyOfValidator}
     */
    private AnyOfValidator createValidator(boolean discriminatorEnabled,
                                           JsonSchema... schemas) throws Exception {

        // the schema node that drives the constructor – an array node with one element per subschema
        JsonNode schemaNode = MAPPER.readTree("[{}, {}, {}]"); // size >= schemas.length, extra entries are ignored

        // stub newSchema(...) to return the mocks we supplied (in order)
        OngoingStubbing<JsonSchema> stub = when(validationContext.newSchema(anyString(),
                any(JsonNode.class), any(JsonSchema.class)));
        for (JsonSchema s : schemas) {
            stub = stub.thenReturn(s);
        }

        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(discriminatorEnabled);

        AnyOfValidator validator = new AnyOfValidator("anyOf", schemaNode,
                mock(JsonSchema.class), validationContext);

        // When discriminator is enabled we need a real DiscriminatorContext instance
        // (it is created inside the constructor).  The test will set its internal flags
        // via reflection when needed.
        return validator;
    }

    /**
     * Test case 1 – a single subschema validates without errors.
     */
    @Test
    public void testValidate_AllValid_NoErrors() throws Exception {
        // a subschema that simply returns an empty error set
        JsonSchema subschema = mock(JsonSchema.class);
        when(subschema.hasTypeValidator()).thenReturn(false);
        when(subschema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = createValidator(false, subschema);

        JsonNode node = MAPPER.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> result = validator.validate(executionContext, node,
                node, "$");

        // Expect an empty set because the subschema succeeded.
        assertTrue("Result must be empty when a subschema passes", result.isEmpty());

        // Verify that the validator called the subschema’s validate method exactly once.
        verify(subschema, times(1)).validate(any(), eq(node), eq(node), eq("$"));
    }

    /**
     * Test case 2 – the subschema has a type validator that does not match the node.
     * The validator should add a type‑mismatch error and continue.
     */
    @Test
    public void testValidate_TypeMismatch_AddsError() throws Exception {
        // stub a TypeValidator that expects a STRING but the node is an OBJECT
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(any())).thenReturn(false);

        JsonSchema subschema = mock(JsonSchema.class);
        when(subschema.hasTypeValidator()).thenReturn(true);
        when(subschema.getTypeValidator()).thenReturn(typeValidator);
        // No need to stub validate() because the type mismatch short‑circuits it.
        when(subschema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = createValidator(false, subschema);

        JsonNode node = MAPPER.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> result = validator.validate(executionContext, node,
                node, "$.anyOf[0]");

        // The result must contain exactly one validation message (the type‑mismatch).
        assertEquals("Exactly one error expected for type mismatch", 1, result.size());
        ValidationMessage message = result.iterator().next();
        assertTrue("Error message must contain the expected schema type",
                message.getMessage().contains(JsonType.STRING.toString()));
    }

    /**
     * Test case 3 – one subschema passes, another returns a REQUIRED‑type error.
     * The REQUIRED error must be filtered out because a valid subschema exists.
     */
    @Test
    public void testValidate_RequiredError_FilteredWhenValidSubschemaExists() throws Exception {
        // Subschema A – succeeds
        JsonSchema successSchema = mock(JsonSchema.class);
        when(successSchema.hasTypeValidator()).thenReturn(false);
        when(successSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // Subschema B – fails with a REQUIRED‑type error
        ValidationMessage requiredError = new ValidationMessage(
                ValidatorTypeCode.REQUIRED.getValue(),
                "$.anyOf[1]",
                "required property 'id' missing");
        JsonSchema failingSchema = mock(JsonSchema.class);
        when(failingSchema.hasTypeValidator()).thenReturn(false);
        when(failingSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(requiredError));

        AnyOfValidator validator = createValidator(false, successSchema, failingSchema);

        JsonNode node = MAPPER.readTree("{\"name\":\"test\"}");
        Set<ValidationMessage> result = validator.validate(executionContext, node,
                node, "$");

        // Because the first subschema succeeded, the REQUIRED error from the second
        // subschema must be removed – the final result is empty.
        assertTrue("Result must be empty when a valid subschema exists", result.isEmpty());
    }

    /**
     * Test case 4 – OpenAPI 3 discriminator is enabled, a match is found,
     * but the chosen subschema still produces errors. The validator must return
     * those errors plus the discriminator remark.
     */
    @Test
    public void testValidate_DiscriminatorMatchFound_WithErrors() throws Exception {
        // Enable discriminator handling
        JsonSchema subschema = mock(JsonSchema.class);
        when(subschema.hasTypeValidator()).thenReturn(false);
        ValidationMessage dummyError = new ValidationMessage("anyOf", "$", "dummy error");
        when(subschema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(dummyError));

        AnyOfValidator validator = createValidator(true, subschema);

        // Force the internal DiscriminatorContext to signal a match.
        setDiscriminatorContextFlags(validator, true, true);

        JsonNode node = MAPPER.readTree("{\"type\":\"A\"}");
        Set<ValidationMessage> result = validator.validate(executionContext, node,
                node, "$");

        // Expect the original error plus the discriminator remark.
        assertEquals("Two messages expected (original + discriminator remark)", 2, result.size());

        boolean hasRemark = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result must contain the discriminator remark", hasRemark);
    }

    /**
     * Test case 5 – OpenAPI 3 discriminator is enabled but no match is found.
     * The validator must return the generic discriminator‑failure message.
     */
    @Test
    public void testValidate_DiscriminatorActive_NoMatchFound() throws Exception {
        // No subschemas (empty anyOf) – the constructor still creates an empty list.
        AnyOfValidator validator = createValidator(true /* discriminator enabled */);

        // Activate the discriminator context without a match.
        setDiscriminatorContextFlags(validator, false, true);

        JsonNode node = MAPPER.readTree("{\"type\":\"unknown\"}");
        Set<ValidationMessage> result = validator.validate(executionContext, node,
                node, "$");

        // The validator should return exactly one message explaining that no alternative
        // could be chosen based on the discriminator.
        assertEquals("Exactly one discriminator‑failure message expected", 1, result.size());
        ValidationMessage message = result.iterator().next();
        assertTrue(message.getMessage().contains("based on the provided discriminator"));
    }

    /**
     * Test case 6 – the {@code walk(...)} method with {@code shouldValidateSchema}
     * set to {@code false} must invoke {@code walk(...)} on each subschema but
     * return an empty set.
     */
    @Test
    public void testWalk_SkipValidation() throws Exception {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        // The subschemas' walk method should be called with shouldValidate = false.
        when(schema1.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(new LinkedHashSet<>());
        when(schema2.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(new LinkedHashSet<>());

        AnyOfValidator validator = createValidator(false, schema1, schema2);

        JsonNode node = MAPPER.readTree("{\"x\":1}");
        Set<ValidationMessage> result = validator.walk(executionContext, node,
                node, "$", false);

        // walk should return an empty set.
        assertTrue("walk should return an empty set when validation is disabled", result.isEmpty());

        // Verify that each subschema's walk method was invoked exactly once.
        verify(schema1, times(1)).walk(any(), eq(node), eq(node), eq("$"), eq(false));
        verify(schema2, times(1)).walk(any(), eq(node), eq(node), eq("$"), eq(false));
    }

    /**
     * Utility method that uses reflection to set the internal flags of the
     * {@code DiscriminatorContext} held by the validator.
     *
     * @param validator the validator whose context is to be modified
     * @param matchFound true → {@code isDiscriminatorMatchFound()} returns true
     * @param active true → {@code isActive()} returns true
     */
    private void setDiscriminatorContextFlags(AnyOfValidator validator,
                                               boolean matchFound, boolean active) throws Exception {
        Field ctxField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        ctxField.setAccessible(true);
        Object discriminatorContext = ctxField.get(validator);

        // The concrete class is ValidationContext.DiscriminatorContext.
        // It has private boolean fields named "discriminatorMatchFound" and "active".
        Field matchField = discriminatorContext.getClass()
                .getDeclaredField("discriminatorMatchFound");
        matchField.setAccessible(true);
        matchField.setBoolean(discriminatorContext, matchFound);

        Field activeField = discriminatorContext.getClass()
                .getDeclaredField("active");
        activeField.setAccessible(true);
        activeField.setBoolean(discriminatorContext, active);
    }
}
