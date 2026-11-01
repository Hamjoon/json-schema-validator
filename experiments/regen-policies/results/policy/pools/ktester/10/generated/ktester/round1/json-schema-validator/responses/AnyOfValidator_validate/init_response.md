```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    /* --------------------------------------------------------------------- */
    /*  Shared test fixtures                                                 */
    /* --------------------------------------------------------------------- */
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock ValidatorState validatorState;
    @Mock Scope dynamicScope;               // used for both parent & child scopes
    @Mock ValidationContext validationContext;
    @Mock SchemaValidatorsConfig config;   // config inside ValidationContext
    @Mock JsonNode instanceNode;            // node being validated
    @Mock JsonNode rootNode;                // root node of the document
    @Mock JsonNodePath instanceLocation;   // location of the instance node

    AnyOfValidator validator;              // system‑under‑test

    @BeforeAll
    static void setupBeforeAll() {
        // No static resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* --------------------------------------------------------------- */
        /*  Mock the execution context hierarchy                           */
        /* --------------------------------------------------------------- */
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(executionContext.getValidatorState()).thenReturn(validatorState);
        when(executionContext.getExecutionConfig()).thenReturn(new ExecutionConfig());

        // every call to enterDynamicScope returns the same mock scope;
        // the validator only uses the returned Scope for mergeWith()
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
        // mergeWith is a void method – no behaviour needed
        doNothing().when(dynamicScope).mergeWith(any(Scope.class));

        // walk / validation are not required for the execution context itself
        doNothing().when(executionContext).enterDiscriminatorContext(any(), any());
        doNothing().when(executionContext).leaveDiscriminatorContextImmediately(any());

        /* --------------------------------------------------------------- */
        /*  Mock ValidationContext and its configuration                    */
        /* --------------------------------------------------------------- */
        when(validationContext.getConfig()).thenReturn(config);
        // keep the tests simple – we do NOT enable OpenAPI‑3 discriminators
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // the validator will ask the ValidationContext for new JsonSchema objects
        // we will provide a mock JsonSchema that we can control per test case
        // (the mock is created lazily inside each test method)

        /* --------------------------------------------------------------- */
        /*  Build a minimal schema node (an array with a single element)   */
        /* --------------------------------------------------------------- */
        JsonNode schemaArrayNode = mock(JsonNode.class);
        when(schemaArrayNode.size()).thenReturn(1);
        when(schemaArrayNode.get(0)).thenReturn(mock(JsonNode.class)); // dummy subschema node

        // create a dummy SchemaLocation – its internals are irrelevant for the test
        SchemaLocation schemaLocation = new SchemaLocation(new AbsoluteIri("http://example.com/schema"), JsonNodePath.JSON_POINTER);

        // instantiate the validator – it will call validationContext.newSchema(...)
        validator = new AnyOfValidator(schemaLocation,
                new JsonNodePath(PathType.JSON),   // evaluationPath (any value works)
                schemaArrayNode,
                null,                              // parentSchema – not needed for our tests
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        clearInvocations(executionContext, collectorContext, validatorState,
                dynamicScope, validationContext, config,
                instanceNode, rootNode, instanceLocation);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to clean up
    }

    /* --------------------------------------------------------------------- */
    /*  Test cases                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void testValidateSuccess_FirstSubschemaMatches() {
        /* --------------------------------------------------------------- */
        /*  Arrange – a single subschema that returns an empty error set   */
        /* --------------------------------------------------------------- */
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);

        // No type validator – the validator will skip type checks
        when(subSchema.getTypeValidator()).thenReturn(null);

        // The subschema validates without errors
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // state handling – we pretend the validator is not in “walk” mode
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true); // indicate a previous match

        /* --------------------------------------------------------------- */
        /*  Act                                                            */
        /* --------------------------------------------------------------- */
        Set<ValidationMessage> result = validator.validate(
                executionContext, instanceNode, rootNode, instanceLocation);

        /* --------------------------------------------------------------- */
        /*  Assert – no errors should be returned                         */
        /* --------------------------------------------------------------- */
        assertTrue(result.isEmpty(), "Expected no validation errors when a subschema matches");
        // verify that the subschema's validate method was invoked exactly once
        verify(subSchema, times(1)).validate(executionContext, instanceNode, rootNode, instanceLocation);
    }

    @Test
    void testValidateFailure_AllSubschemasError() {
        /* --------------------------------------------------------------- */
        /*  Arrange – a single subschema that returns a non‑empty error set */
        /* --------------------------------------------------------------- */
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema);

        when(subSchema.getTypeValidator()).thenReturn(null);

        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = Collections.singleton(mockMessage);
        when(subSchema.validate(any(), any(), any(), any()))
                .thenReturn(errorSet);

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false); // no prior match

        /* --------------------------------------------------------------- */
        /*  Act                                                            */
        /* --------------------------------------------------------------- */
        Set<ValidationMessage> result = validator.validate(
                executionContext, instanceNode, rootNode, instanceLocation);

        /* --------------------------------------------------------------- */
        /*  Assert – the returned set must contain the mocked error       */
        /* --------------------------------------------------------------- */
        assertEquals(1, result.size(), "Expected exactly one validation error");
        assertTrue(result.contains(mockMessage), "Returned errors should contain the mocked message");
        verify(subSchema, times(1)).validate(executionContext, instanceNode, rootNode, instanceLocation);
    }
}
```