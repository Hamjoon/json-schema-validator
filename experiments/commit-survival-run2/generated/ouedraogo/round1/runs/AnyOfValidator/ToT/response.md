###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal validation flow (valid subschema, invalid subschema)</li>
 *   <li>required‑error filtering when at least one subschema is valid</li>
 *   <li>OpenAPI‑3 style discriminator handling (match found / not found, with and without errors)</li>
 *   <li>walk() delegation logic</li>
 *   <li>preloadJsonSchema() behaviour</li>
 * </ul>
 *
 * <p>All external collaborators are mocked with Mockito; the real {@link AnyOfValidator}
 * logic is exercised only.  The test class is deliberately self‑contained and does not
 * depend on any external JSON schema files.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    private static final String SCHEMA_PATH = "$.anyOf";

    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private JsonSchema parentSchema;

    @Before
    public void setUp() {
        // default behaviour for the execution context hierarchy
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(mock(CollectorContext.Scope.class));
        when(collectorContext.exitDynamicScope()).thenReturn(mock(CollectorContext.Scope.class));

        // ValidationContext provides a Config object
        when(validationContext.getConfig()).thenReturn(config);
        // by default discriminators are disabled – individual tests override as needed
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
    }

    /**
     * Helper that builds a minimal {@code schemaNode} representing an {@code anyOf}
     * with two empty subschemas and creates an {@link AnyOfValidator} instance.
     */
    private AnyOfValidator createValidator() {
        // create a JSON array node: [ {}, {} ]
        ArrayNode schemaNode = mapper.createArrayNode();
        schemaNode.add(mapper.createObjectNode());
        schemaNode.add(mapper.createObjectNode());

        // the ValidationContext must be able to create JsonSchema objects for each subschema
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/0"), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(subSchema1);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/1"), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(subSchema2);

        return new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
    }

    /**
     * Test that when a subschema validates without errors and discriminators are disabled,
     * {@link AnyOfValidator#validate} returns an empty {@link Set}.
     */
    @Test
    public void testValidate_ValidSubschema_NoDiscriminator_ReturnsEmpty() {
        AnyOfValidator validator = createValidator();

        // mock the two subschemas
        JsonSchema subSchema1 = validator.schemas.get(0);
        JsonSchema subSchema2 = validator.schemas.get(1);

        // first subschema returns errors (will be ignored later)
        when(subSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(mock(ValidationMessage.class)));

        // second subschema validates cleanly
        when(subSchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // validator state handling
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        JsonNode documentNode = mapper.createObjectNode(); // any node – content irrelevant

        Set<ValidationMessage> result = validator.validate(executionContext, documentNode,
                documentNode, "$");

        assertTrue("Result must be empty when a subschema validates successfully", result.isEmpty());

        // verify that both subschemas were invoked
        verify(subSchema1).validate(any(), any(), any(), eq("$"));
        verify(subSchema2).validate(any(), any(), any(), eq("$"));
    }

    /**
     * Test that when at least one subschema validates successfully,
     * any “required” errors from the other subschemas are filtered out.
     */
    @Test
    public void testValidate_FiltersRequiredErrors_WhenOneSubschemaValid() {
        AnyOfValidator validator = createValidator();

        JsonSchema validSchema = validator.schemas.get(0);
        JsonSchema requiredErrorSchema = validator.schemas.get(1);

        // valid schema – no errors
        when(validSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // schema that fails with a “required” error
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getType()).thenReturn(ValidatorTypeCode.REQUIRED.getValue());
        Set<ValidationMessage> requiredErrorSet = new HashSet<>();
        requiredErrorSet.add(requiredMsg);
        when(requiredErrorSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(requiredErrorSet);

        // state handling
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        JsonNode documentNode = mapper.createObjectNode();

        Set<ValidationMessage> result = validator.validate(executionContext, documentNode,
                documentNode, "$");

        // The required‑error must be filtered out → empty result
        assertTrue("Result should be empty after filtering required errors", result.isEmpty());

        verify(validSchema).validate(any(), any(), any(), eq("$"));
        verify(requiredErrorSchema).validate(any(), any(), any(), eq("$"));
    }

    /**
     * Test OpenAPI‑3 style discriminator handling where a discriminator match is found
     * and the selected subschema returns validation errors.
     * The returned set must contain the original errors plus the discriminator remark.
     */
    @Test
    public void testValidate_DiscriminatorMatchFound_WithErrors() throws Exception {
        // enable discriminator handling
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        AnyOfValidator validator = createValidator();

        // replace the real discriminator context with a Mockito mock so we can control its state
        ValidationContext.DiscriminatorContext discContext = mock(ValidationContext.DiscriminatorContext.class);
        java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        discField.set(validator, discContext);

        // simulate that a discriminator match has been found
        when(discContext.isDiscriminatorMatchFound()).thenReturn(true);
        when(discContext.isActive()).thenReturn(true);

        // mock the subschema – it will be the one selected by the discriminator
        JsonSchema selectedSchema = validator.schemas.get(0);
        ValidationMessage errorMsg = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = new HashSet<>();
        errorSet.add(errorMsg);
        when(selectedSchema.validate(any(), any(), any(), anyString())).thenReturn(errorSet);

        // state handling
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        JsonNode documentNode = mapper.createObjectNode();

        Set<ValidationMessage> result = validator.validate(executionContext, documentNode,
                documentNode, "$");

        // Expect original error + discriminator remark => size == 2
        assertEquals("Result must contain original error plus discriminator remark", 2, result.size());
        assertTrue("Result must contain the original error", result.contains(errorMsg));

        // Verify that discriminator context entry/exit methods are called
        verify(validationContext).enterDiscriminatorContext(eq(discContext), eq("$"));
        verify(validationContext).leaveDiscriminatorContextImmediately(eq("$"));
    }

    /**
     * Test OpenAPI‑3 discriminator handling where a discriminator match is found
     * and the selected subschema validates cleanly – result must be empty.
     */
    @Test
    public void testValidate_DiscriminatorMatchFound_NoErrors() throws Exception {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        AnyOfValidator validator = createValidator();

        // inject mock discriminator context
        ValidationContext.DiscriminatorContext discContext = mock(ValidationContext.DiscriminatorContext.class);
        java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        discField.set(validator, discContext);
        when(discContext.isDiscriminatorMatchFound()).thenReturn(true);
        when(discContext.isActive()).thenReturn(true);

        JsonSchema selectedSchema = validator.schemas.get(0);
        when(selectedSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        JsonNode documentNode = mapper.createObjectNode();

        Set<ValidationMessage> result = validator.validate(executionContext, documentNode,
                documentNode, "$");

        assertTrue("Result must be empty when discriminator‑selected schema validates", result.isEmpty());

        verify(validationContext).enterDiscriminatorContext(eq(discContext), eq("$"));
        verify(validationContext).leaveDiscriminatorContextImmediately(eq("$"));
    }

    /**
     * walk() with {@code shouldValidateSchema == true} must delegate to {@link #validate}.
     */
    @Test
    public void testWalk_DelegatesToValidate_WhenShouldValidateTrue() {
        AnyOfValidator validator = createValidator();

        // spy the validator so we can verify the internal call
        AnyOfValidator spyValidator = spy(validator);
        JsonNode node = mapper.createObjectNode();

        // stub validate() to return a known result
        Set<ValidationMessage> stubResult = Collections.singleton(mock(ValidationMessage.class));
        doReturn(stubResult).when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, node, "$", true);

        assertSame("walk should return the same set as validate", stubResult, result);
        verify(spyValidator).validate(executionContext, node, node, "$");
        verifyNoMoreInteractions(spyValidator);
    }

    /**
     * walk() with {@code shouldValidateSchema == false} must invoke {@code walk}
     * on each subschema with {@code shouldValidateSchema = false} and return an empty set.
     */
    @Test
    public void testWalk_SkipsValidation_WhenShouldValidateFalse() {
        AnyOfValidator validator = createValidator();

        JsonNode node = mapper.createObjectNode();

        // each subschema's walk should be called once with shouldValidate = false
        for (JsonSchema schema : validator.schemas) {
            when(schema.walk(any(), any(), any(), anyString(), eq(false)))
                    .thenReturn(Collections.emptySet());
        }

        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", false);

        assertTrue("Result must be empty when validation is skipped", result.isEmpty());

        // verify walk called on each subschema
        for (JsonSchema schema : validator.schemas) {
            verify(schema).walk(executionContext, node, node, "$", false);
        }
    }

    /**
     * preloadJsonSchema() must invoke {@code preloadJsonSchema()} on every subschema.
     */
    @Test
    public void testPreloadJsonSchema_InvokesAllSubschemas() {
        AnyOfValidator validator = createValidator();

        // spy each subschema to verify the preload call
        for (int i = 0; i < validator.schemas.size(); i++) {
            JsonSchema original = validator.schemas.get(i);
            JsonSchema spySchema = spy(original);
            validator.schemas.set(i, spySchema);
        }

        validator.preloadJsonSchema();

        for (JsonSchema schema : validator.schemas) {
            verify(schema).preloadJsonSchema();
        }
    }
}
```
###Test END##