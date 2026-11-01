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
class AnyOfValidator_walk_Test {

    // ----- Mocks shared by all tests -------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock ExecutionContext executionContext;
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instanceLocation;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonSchema parentSchema;

    // ----- Test specific mocks -------------------------------------------------------
    @Mock JsonNode anyOfArrayNode;          // represents the schemaNode passed to the validator
    @Mock JsonNode subschemaNode0;          // individual subschema nodes inside the array
    @Mock JsonNode subschemaNode1;

    @Mock JsonSchema subschemaValidator0;    // the JsonSchema objects created by ValidationContext
    @Mock JsonSchema subschemaValidator1;

    AnyOfValidator anyOfValidator;        // object under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- Prepare the "anyOf" array node ---------------------------------------
        when(anyOfArrayNode.size()).thenReturn(2);
        when(anyOfArrayNode.get(0)).thenReturn(subschemaNode0);
        when(anyOfArrayNode.get(1)).thenReturn(subschemaNode1);

        // ----- Stub ValidationContext#newSchema to return our mock subschema validators
        // The real method signature is:
        //   JsonSchema newSchema(SchemaLocation location, JsonNodePath path, JsonNode node,
        //                       JsonSchema parent)
        // We only need to match the call arguments loosely.
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class),
                eq(subschemaNode0), any(JsonSchema.class))).thenReturn(subschemaValidator0);
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class),
                eq(subschemaNode1), any(JsonSchema.class))).thenReturn(subschemaValidator1);

        // ----- Build the validator under test ---------------------------------------
        anyOfValidator = new AnyOfValidator(
                schemaLocation,
                evaluationPath,
                anyOfArrayNode,
                parentSchema,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    @Test
    void walk_ShouldValidateSchema_True_InvokesValidateAndReturnsItsResult() {
        // Arrange: spy the validator so we can stub validate()
        AnyOfValidator spyValidator = spy(anyOfValidator);
        Set<ValidationMessage> expected = new LinkedHashSet<>();
        expected.add(mock(ValidationMessage.class));
        doReturn(expected).when(spyValidator).validate(executionContext, mock(JsonNode.class),
                rootNode, instanceLocation);

        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext,
                mock(JsonNode.class), rootNode, instanceLocation, true);

        // Assert
        assertSame(expected, actual, "When shouldValidateSchema is true, walk should return the result of validate()");
        verify(spyValidator, times(1)).validate(executionContext, any(JsonNode.class),
                eq(rootNode), eq(instanceLocation));
        // No schema.walk calls should happen when validation is performed directly
        verifyNoInteractions(subschemaValidator0, subschemaValidator1);
    }

    @Test
    void walk_ShouldValidateSchema_False_InvokesEachSubschemaWalkAndReturnsEmptySet() {
        // Arrange: stub each subschema's walk method (they return arbitrary non‑empty sets)
        Set<ValidationMessage> dummySet0 = new LinkedHashSet<>(Collections.singleton(mock(ValidationMessage.class)));
        Set<ValidationMessage> dummySet1 = new LinkedHashSet<>(Collections.singleton(mock(ValidationMessage.class)));
        when(subschemaValidator0.walk(executionContext, mock(JsonNode.class), rootNode,
                instanceLocation, false)).thenReturn(dummySet0);
        when(subschemaValidator1.walk(executionContext, mock(JsonNode.class), rootNode,
                instanceLocation, false)).thenReturn(dummySet1);

        JsonNode testNode = mock(JsonNode.class); // the node being validated

        // Act
        Set<ValidationMessage> result = anyOfValidator.walk(executionContext, testNode,
                rootNode, instanceLocation, false);

        // Assert
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "When shouldValidateSchema is false, AnyOfValidator should return an empty set");
        // Verify each subschema's walk was invoked exactly once with shouldValidateSchema = false
        verify(subschemaValidator0, times(1)).walk(executionContext, testNode,
                rootNode, instanceLocation, false);
        verify(subschemaValidator1, times(1)).walk(executionContext, testNode,
                rootNode, instanceLocation, false);
    }
}
```