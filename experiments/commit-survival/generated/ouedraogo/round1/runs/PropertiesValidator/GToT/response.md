###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.PropertiesValidator}.
 *
 * The tests focus on the public API of the class while using Mockito to isolate
 * all collaborators (ExecutionContext, ValidationContext, JsonSchema, etc.).
 *
 * The test cases cover:
 *   1. Construction – the schema map is built from the supplied schema node.
 *   2. Normal validation – a property that exists is delegated to the
 *      corresponding {@link JsonSchema#validate(...)} call and the returned
 *      {@link ValidationMessage}s are propagated.
 *   3. Missing property with a required validator – required‑property errors
 *      are added.
 *   4. Missing property when the validator is part of a complex validator
 *      (e.g. oneOf) – the method returns an empty set immediately.
 *   5. Edge cases – null node, empty schema, and behaviour when walk is
 *      disabled.
 *
 * The test class uses the Mockito‑JUnit runner so that all mocks are created
 * automatically and injected where needed.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesValidatorTest {

    /* --------------------------------------------------------------------- *
     *  Mocks for collaborators that are required by PropertiesValidator.
     * --------------------------------------------------------------------- */
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonSchema requiredValidator;
    @Mock private JsonSchema propertySchema1;
    @Mock private JsonSchema propertySchema2;

    private ObjectMapper mapper = new ObjectMapper();

    // a simple node that will be used as the JSON instance to validate
    private ObjectNode instanceNode;

    // helper to build a schema node that defines two properties: "p1" and "p2"
    private ObjectNode buildSchemaNode() {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.set("p1", mapper.createObjectNode().put("type", "string"));
        schemaNode.set("p2", mapper.createObjectNode().put("type", "integer"));
        return schemaNode;
    }

    @Before
    public void setUp() {
        // ----- generic ExecutionContext / CollectorContext set‑up -----------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // the collector context returns a mutable set for evaluated properties
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<String>());
        // store the validator state under the well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // ----- ValidationContext – it creates a JsonSchema for each property ---------
        // the validator will call validationContext.newSchema(schemaPath, schemaNode, parent)
        // We simply return the pre‑created mock schemas in the order they are requested.
        Answer<JsonSchema> newSchemaAnswer = invocation -> {
            String path = invocation.getArgument(0);
            if (path.endsWith("/p1")) {
                return propertySchema1;
            } else if (path.endsWith("/p2")) {
                return propertySchema2;
            }
            return mock(JsonSchema.class);
        };
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(newSchemaAnswer);

        // ----- parent schema – needed for required‑property handling -----------------
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        when(parentSchema.hasRequiredValidator()).thenReturn(true);

        // ----- a simple instance node ------------------------------------------------
        instanceNode = mapper.createObjectNode();
        instanceNode.put("p1", "hello");
        // p2 is deliberately omitted in some tests to trigger the “missing property”
        // path.
    }

    /* --------------------------------------------------------------------- *
     *  1. Constructor – verifies that the internal map is populated correctly.
     * --------------------------------------------------------------------- */
    @Test
    public void constructor_buildsSchemasMap() {
        ObjectNode schemaNode = buildSchemaNode();

        PropertiesValidator validator = new PropertiesValidator(
                "#/properties", schemaNode, parentSchema, validationContext);

        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertEquals(2, schemas.size());
        assertTrue(schemas.containsKey("p1"));
        assertTrue(schemas.containsKey("p2"));
        // the map must contain the exact mock instances we returned from
        // ValidationContext#newSchema
        assertSame(propertySchema1, schemas.get("p1"));
        assertSame(propertySchema2, schemas.get("p2"));
    }

    /* --------------------------------------------------------------------- *
     *  2. Normal validation – property present, walk disabled.
     * --------------------------------------------------------------------- */
    @Test
    public void validate_propertyPresent_delegatesToPropertySchema() {
        // arrange ---------------------------------------------------------------
        ObjectNode schemaNode = buildSchemaNode();
        PropertiesValidator validator = new PropertiesValidator(
                "#/properties", schemaNode, parentSchema, validationContext);

        // validator state: walk disabled, not a complex validator
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        // mock the property schema to return a single ValidationMessage
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> propertyErrors = Collections.singleton(msg);
        when(propertySchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(propertyErrors);
        // p2 is missing – we do not want it to interfere with this test
        when(propertySchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // act --------------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext, instanceNode, instanceNode, "#");

        // assert -----------------------------------------------------------------
        // the result must contain the message returned by propertySchema1
        assertEquals(1, result.size());
        assertTrue(result.contains(msg));

        // verify that the property schema was indeed invoked with the correct path
        verify(propertySchema1).validate(eq(executionContext),
                eq(instanceNode.get("p1")), eq(instanceNode), eq("#/p1"));
        verify(propertySchema2, never()).validate(any(), any(), any(), anyString());
    }

    /* --------------------------------------------------------------------- *
     *  3. Missing property – required validator returns errors.
     * --------------------------------------------------------------------- */
    @Test
    public void validate_missingPropertyWithRequiredValidator_returnsRequiredErrors() {
        // arrange ----------------------------------------------------------------
        ObjectNode schemaNode = buildSchemaNode();
        PropertiesValidator validator = new PropertiesValidator(
                "#/properties", schemaNode, parentSchema, validationContext);

        // state: not a complex validator, walk disabled
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        // required validator produces a single error for the missing "p2"
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        Set<ValidationMessage> requiredErrors = Collections.singleton(requiredMsg);
        when(requiredValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(requiredErrors);

        // act --------------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext, instanceNode, instanceNode, "#");

        // assert -----------------------------------------------------------------
        // the result must contain the required‑validator error
        assertEquals(1, result.size());
        assertTrue(result.contains(requiredMsg));

        // verify that the property schema for "p2" was never called (because the node is missing)
        verify(propertySchema2, never()).validate(any(), any(), any(), anyString());
    }

    /* --------------------------------------------------------------------- *
     *  4. Missing property while this validator participates in a complex
     *     validator (e.g. oneOf) – the method should return an empty set.
     * --------------------------------------------------------------------- */
    @Test
    public void validate_missingPropertyInComplexValidator_returnsEmptySet() {
        // arrange ----------------------------------------------------------------
        ObjectNode schemaNode = buildSchemaNode();
        PropertiesValidator validator = new PropertiesValidator(
                "#/properties", schemaNode, parentSchema, validationContext);

        // state: the validator is part of a complex validator
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isComplexValidator()).thenReturn(true);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        // required validator would normally return an error, but because we are in
        // a complex validator the validator must short‑circuit and return empty.
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        Set<ValidationMessage> requiredErrors = Collections.singleton(requiredMsg);
        when(requiredValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(requiredErrors);

        // act --------------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext, instanceNode, instanceNode, "#");

        // assert -----------------------------------------------------------------
        // An empty set signals that the complex validator decided to abort further
        // validation of this branch.
        assertTrue(result.isEmpty());

        // verify that the complex flag was queried correctly
        verify(validatorState, atLeastOnce()).isComplexValidator();
        // the required validator should have been called (the code checks it before
        // deciding to abort)
        verify(requiredValidator).validate(eq(executionContext), eq(instanceNode), eq(instanceNode), eq("#"));
    }

    /* --------------------------------------------------------------------- *
     *  5. Edge case – the node supplied to {@code validate} is null.
     * --------------------------------------------------------------------- */
    @Test
    public void validate_nullNode_returnsEmptySet() {
        // arrange ----------------------------------------------------------------
        ObjectNode schemaNode = buildSchemaNode();
        PropertiesValidator validator = new PropertiesValidator(
                "#/properties", schemaNode, parentSchema, validationContext);

        // state: walk disabled, not complex
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        // act --------------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(
                executionContext, null, null, "#");

        // assert -----------------------------------------------------------------
        // No property can be inspected, therefore no errors are produced.
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  6. walk(...) – when shouldValidateSchema == true the method must delegate
     *     to {@code validate(...)}.  We use a spy to ensure the delegation occurs.
     * --------------------------------------------------------------------- */
    @Test
    public void walk_withValidationDelegatesToValidate() {
        // arrange ----------------------------------------------------------------
        ObjectNode schemaNode = buildSchemaNode();
        PropertiesValidator realValidator = new PropertiesValidator(
                "#/properties", schemaNode, parentSchema, validationContext);
        PropertiesValidator spyValidator = spy(realValidator);

        // make the underlying validate() return a known set
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> fakeResult = Collections.singleton(msg);
        doReturn(fakeResult).when(spyValidator).validate(
                any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString());

        // act --------------------------------------------------------------------
        Set<ValidationMessage> walkResult = spyValidator.walk(
                executionContext, instanceNode, instanceNode, "#", true);

        // assert -----------------------------------------------------------------
        assertSame(fakeResult, walkResult);
        verify(spyValidator, times(1)).validate(eq(executionContext), eq(instanceNode), eq(instanceNode), eq("#"));
    }

    /* --------------------------------------------------------------------- *
     *  7. preloadJsonSchema() – verify that the protected helper in the base
     *     class is invoked for every stored schema.  We use a spy on the
     *     validator to capture the call.
     * --------------------------------------------------------------------- */
    @Test
    public void preloadJsonSchema_callsPreloadOnAllChildSchemas() {
        // arrange ----------------------------------------------------------------
        ObjectNode schemaNode = buildSchemaNode();
        PropertiesValidator validator = new PropertiesValidator(
                "#/properties", schemaNode, parentSchema, validationContext);

        // spy the validator so we can verify the protected method call
        PropertiesValidator spyValidator = spy(validator);
        // we do not want the real implementation of preloadJsonSchemas to run
        // (it would try to load actual schemas).  Instead we stub it.
        doNothing().when(spyValidator).preloadJsonSchemas(anyCollection());

        // act --------------------------------------------------------------------
        spyValidator.preloadJsonSchema();

        // assert -----------------------------------------------------------------
        // The protected method must have been called with a collection containing
        // exactly the two child schemas.
        ArgumentCaptor<Collection<JsonSchema>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(spyValidator, times(1)).preloadJsonSchemas(captor.capture());
        Collection<JsonSchema> passed = captor.getValue();
        assertEquals(2, passed.size());
        assertTrue(passed.contains(propertySchema1));
        assertTrue(passed.contains(propertySchema2));
    }
}
```
###Test END##