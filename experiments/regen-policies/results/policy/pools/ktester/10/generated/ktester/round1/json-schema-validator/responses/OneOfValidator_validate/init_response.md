```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    }

    @Test
    void validate_whenExactlyOneSchemaMatches_returnsNoErrors() {
        // first subschema fails, second succeeds
        ValidationMessage childError = mock(ValidationMessage.class);
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Set.of(childError));
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),   // instance node
                mock(JsonNode.class),   // root node
                mock(JsonNodePath.class) // instance location
        );

        assertTrue(result.isEmpty(), "Expected no validation errors when exactly one schema matches");
    }

    @Test
    void validate_whenNoSchemaMatches_returnsOneOfErrorAndChildErrors() {
        // both subschemas produce errors
        ValidationMessage error0 = mock(ValidationMessage.class);
        ValidationMessage error1 = mock(ValidationMessage.class);
        when(childSchema0.validate(any(), any(), any(), any())).thenReturn(Set.of(error0));
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Set.of(error1));

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                mock(JsonNodePath.class)
        );

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

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                mock(JsonNode.class),
                mock(JsonNode.class),
                mock(JsonNodePath.class)
        );

        // only the OneOf error should be present
        assertEquals(1, result.size(), "Expected a single OneOf error when more than one schema matches");
    }
}
```