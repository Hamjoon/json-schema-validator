```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;          // the array node passed to the constructor

    @Mock
    JsonNode childNode1;          // individual child schema nodes

    @Mock
    JsonNode childNode2;

    @Mock
    JsonSchema parentSchema;      // parent schema – not used by the method under test

    @Mock
    JsonSchema childSchema1;      // schemas that will be stored inside OneOfValidator

    @Mock
    JsonSchema childSchema2;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises the @Mock fields, nothing else needed here
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void preloadJsonSchema_callsInitializeValidatorsOnAllSchemas() {
        // ----- GIVEN ---------------------------------------------------------
        // The schemaNode represents an array with two child schemas
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(childNode1);
        when(schemaNode.get(1)).thenReturn(childNode2);

        // ValidationContext creates a JsonSchema for each child node
        when(validationContext.newSchema(any(), any(), eq(childNode1), any()))
                .thenReturn(childSchema1);
        when(validationContext.newSchema(any(), any(), eq(childNode2), any()))
                .thenReturn(childSchema2);

        // ----- WHEN ----------------------------------------------------------
        OneOfValidator validator = new OneOfValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validationContext);

        // The method under test
        validator.preloadJsonSchema();

        // ----- THEN ----------------------------------------------------------
        // Each schema added in the constructor must have its initialiseValidators()
        // method invoked exactly once.
        verify(childSchema1, times(1)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();

        // No other unexpected interactions
        verifyNoMoreInteractions(childSchema1, childSchema2);
    }

    @Test
    void preloadJsonSchema_withNoSchemasDoesNotInvokeInitializeValidators() {
        // ----- GIVEN ---------------------------------------------------------
        // Empty array – size 0, therefore no child schemas are added
        when(schemaNode.size()).thenReturn(0);

        // ----- WHEN ----------------------------------------------------------
        OneOfValidator validator = new OneOfValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validationContext);

        validator.preloadJsonSchema();

        // ----- THEN ----------------------------------------------------------
        // No schema instances exist, so initializeValidators() must never be called.
        verify(validationContext, never()).newSchema(any(), any(), any(), any());
    }
}
```