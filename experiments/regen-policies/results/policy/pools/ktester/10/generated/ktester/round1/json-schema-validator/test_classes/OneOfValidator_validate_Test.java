package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_Test {

    // Mocks shared by all tests
    ExecutionContext executionContext;

    CollectorContext collectorContext;

    ExecutionConfig executionConfig;

    ValidatorState validatorState;

    ValidationContext validationContext;

    SchemaLocation schemaLocation;

    JsonNodePath evaluationPath;

    JsonSchema parentSchema;

    JsonNode schemaNode;

    JsonSchema childSchema0;

    JsonSchema childSchema1;

    OneOfValidator validator;

    JsonNodePath instanceLocation;

    private static class Scenario {

        final String description;

        final boolean walkEnabled;

        final boolean failFast;

        final boolean validationEnabled;

        final List<JsonSchema> schemas;

        final List<Set<ValidationMessage>> schemaValidateReturns;

        final List<Set<ValidationMessage>> schemaWalkReturns;

        final Boolean hasMatchedNodeAfterValidate;

        final Boolean hasMatchedNodeAfterWalk;

        final Class<? extends Throwable> expectedException;

        final int expectedErrorCount;

        Scenario(String description, boolean walkEnabled, boolean failFast, boolean validationEnabled, List<JsonSchema> schemas, List<Set<ValidationMessage>> schemaValidateReturns, List<Set<ValidationMessage>> schemaWalkReturns, Boolean hasMatchedNodeAfterValidate, Boolean hasMatchedNodeAfterWalk, Class<? extends Throwable> expectedException, int expectedErrorCount) {
            this.description = description;
            this.walkEnabled = walkEnabled;
            this.failFast = failFast;
            this.validationEnabled = validationEnabled;
            this.schemas = schemas;
            this.schemaValidateReturns = schemaValidateReturns;
            this.schemaWalkReturns = schemaWalkReturns;
            this.hasMatchedNodeAfterValidate = hasMatchedNodeAfterValidate;
            this.hasMatchedNodeAfterWalk = hasMatchedNodeAfterWalk;
            this.expectedException = expectedException;
            this.expectedErrorCount = expectedErrorCount;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- execution context & related collaborators -----
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        executionConfig = mock(ExecutionConfig.class);
        validatorState = mock(ValidatorState.class);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionContext.getValidatorState()).thenReturn(validatorState);
        // default state behaviour
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(executionConfig.getLocale()).thenReturn(Locale.ENGLISH);
        // ----- dynamic scopes (enter/exit) -----
        when(collectorContext.enterDynamicScope()).thenAnswer(invocation -> mock(Scope.class));
        when(collectorContext.exitDynamicScope()).thenAnswer(invocation -> mock(Scope.class));
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        // ----- validation context & schema construction -----
        validationContext = mock(ValidationContext.class);
        schemaLocation = mock(SchemaLocation.class);
        evaluationPath = mock(JsonNodePath.class);
        parentSchema = mock(JsonSchema.class);
        schemaNode = mock(JsonNode.class);
        // the schemaNode represents an array with two subschemas
        when(schemaNode.size()).thenReturn(2);
        JsonNode childNode0 = mock(JsonNode.class);
        JsonNode childNode1 = mock(JsonNode.class);
        when(schemaNode.get(0)).thenReturn(childNode0);
        when(schemaNode.get(1)).thenReturn(childNode1);
        // child schemas returned by ValidationContext.newSchema(...)
        childSchema0 = mock(JsonSchema.class);
        childSchema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), eq(childNode0), any())).thenReturn(childSchema0);
        when(validationContext.newSchema(any(), any(), eq(childNode1), any())).thenReturn(childSchema1);
        // ----- create the validator under test -----
        validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        executionConfig = mock(ExecutionConfig.class);
        validatorState = mock(ValidatorState.class);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionContext.getValidatorState()).thenReturn(validatorState);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(executionConfig.getLocale()).thenReturn(Locale.ENGLISH);
        when(collectorContext.enterDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(collectorContext.exitDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        validationContext = mock(ValidationContext.class);
        schemaLocation = mock(SchemaLocation.class);
        instanceLocation = mock(JsonNodePath.class);
        parentSchema = mock(JsonSchema.class);
        schemaNode = mock(JsonNode.class);
        when(schemaNode.size()).thenReturn(2);
        JsonNode childNode0 = mock(JsonNode.class);
        JsonNode childNode1 = mock(JsonNode.class);
        when(schemaNode.get(0)).thenReturn(childNode0);
        when(schemaNode.get(1)).thenReturn(childNode1);
        childSchema0 = mock(JsonSchema.class);
        childSchema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), eq(childNode0), any())).thenReturn(childSchema0);
        when(validationContext.newSchema(any(), any(), eq(childNode1), any())).thenReturn(childSchema1);
        validator = new OneOfValidator(schemaLocation, instanceLocation, schemaNode, parentSchema, validationContext);
    }

    private void setValidatorSchemas(List<JsonSchema> schemas) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, schemas);
    }

    private void setFailFast(boolean failFast) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.set(validator, failFast);
    }

    private ExecutionContext mockExecutionContext(boolean walkEnabled, boolean failFast, boolean validationEnabled, List<JsonSchema> schemas) throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        CollectorContext cc = mock(CollectorContext.class);
        ExecutionConfig ec = mock(ExecutionConfig.class);
        ValidatorState vs = mock(ValidatorState.class);
        when(ctx.getCollectorContext()).thenReturn(cc);
        when(ctx.getExecutionConfig()).thenReturn(ec);
        when(ctx.getValidatorState()).thenReturn(vs);
        when(vs.isWalkEnabled()).thenReturn(walkEnabled);
        when(vs.isValidationEnabled()).thenReturn(validationEnabled);
        when(vs.hasMatchedNode()).thenReturn(true);
        when(ec.getLocale()).thenReturn(Locale.ENGLISH);
        when(cc.enterDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(cc.exitDynamicScope()).thenAnswer(i -> mock(Scope.class));
        when(cc.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(cc.getEvaluatedProperties()).thenReturn(new HashSet<>());
        OneOfValidator v = new OneOfValidator(schemaLocation, instanceLocation, schemaNode, parentSchema, validationContext);
        setValidatorSchemas(schemas);
        setFailFast(failFast);
        validator = v;
        return ctx;
    }

    private static Stream<Scenario> scenarios() {
        ValidationMessage vm0 = mock(ValidationMessage.class);
        ValidationMessage vm1 = mock(ValidationMessage.class);
        ValidationMessage vm2 = mock(ValidationMessage.class);
        JsonSchema validSchema = mock(JsonSchema.class);
        JsonSchema errorSchema = mock(JsonSchema.class);
        JsonSchema resetMatchedNodeSchema = mock(JsonSchema.class);
        JsonSchema nullReturningValidateSchema = mock(JsonSchema.class);
        JsonSchema nullReturningWalkSchema = mock(JsonSchema.class);
        ???;
    }

    @Test
    void validate_whenExactlyOneSchemaMatches_returnsNoErrors() {
        // first subschema fails, second succeeds
        ValidationMessage childError = mock(ValidationMessage.class);
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Set.of(childError));
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, // instance node
        mock(JsonNode.class), // root node
        mock(JsonNode.class), // instance location
        mock(JsonNodePath.class));
        assertTrue(result.isEmpty(), "Expected no validation errors when exactly one schema matches");
    }

    @Test
    void validate_whenNoSchemaMatches_returnsOneOfErrorAndChildErrors() {
        // both subschemas produce errors
        ValidationMessage error0 = mock(ValidationMessage.class);
        ValidationMessage error1 = mock(ValidationMessage.class);
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Set.of(error0));
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Set.of(error1));
        Set<ValidationMessage> result = validator.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), mock(JsonNodePath.class));
        // expect the OneOf error + the two child errors
        assertEquals(3, result.size(), "Expected OneOf error plus two child errors");
        assertTrue(result.contains(error0), "Result should contain first child error");
        assertTrue(result.contains(error1), "Result should contain second child error");
    }

    @Test
    void validate_whenMultipleSchemasMatch_returnsOneOfErrorOnly() {
        // both subschemas succeed (empty error sets)
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), mock(JsonNodePath.class));
        // only the OneOf error should be present
        assertEquals(1, result.size(), "Expected a single OneOf error when more than one schema matches");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void validate_scenarios(Scenario sc) throws Exception {
        setValidatorSchemas(sc.schemas);
        setFailFast(sc.failFast);
        when(validatorState.isWalkEnabled()).thenReturn(sc.walkEnabled);
        when(validatorState.isValidationEnabled()).thenReturn(sc.validationEnabled);
        if (sc.hasMatchedNodeAfterValidate != null) {
            when(validatorState.hasMatchedNode()).thenReturn(sc.hasMatchedNodeAfterValidate);
        }
        if (sc.hasMatchedNodeAfterWalk != null) {
            when(validatorState.hasMatchedNode()).thenReturn(sc.hasMatchedNodeAfterWalk);
        }
        for (int i = 0; i < sc.schemas.size(); i++) {
            JsonSchema schema = sc.schemas.get(i);
            if (schema == null) {
                continue;
            }
            if (sc.walkEnabled) {
                Set<ValidationMessage> walkResult = (sc.schemaWalkReturns != null && i < sc.schemaWalkReturns.size()) ? sc.schemaWalkReturns.get(i) : Collections.emptySet();
                when(schema.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(walkResult);
            } else {
                Set<ValidationMessage> validateResult = (sc.schemaValidateReturns != null && i < sc.schemaValidateReturns.size()) ? sc.schemaValidateReturns.get(i) : Collections.emptySet();
                when(schema.validate(any(), any(), any(), any())).thenReturn(validateResult);
            }
        }
        ExecutionContext ctxToUse = executionContext;
        if (sc.expectedException == NullPointerException.class && sc.description.contains("Null executionContext")) {
            ctxToUse = null;
        } else if (sc.expectedException == NullPointerException.class && sc.description.contains("Null collectorContext")) {
            when(executionContext.getCollectorContext()).thenReturn(null);
        }
        if (sc.expectedException != null) {
            assertThrows(sc.expectedException, () -> validator.validate(ctxToUse, mock(JsonNode.class), mock(JsonNode.class), mock(JsonNodePath.class)), "Expected exception for scenario: " + sc.description);
            return;
        }
        Set<ValidationMessage> result = validator.validate(ctxToUse, mock(JsonNode.class), mock(JsonNode.class), mock(JsonNodePath.class));
        assertEquals(sc.expectedErrorCount, result.size(), "Unexpected number of errors for scenario: " + sc.description);
    }
}
