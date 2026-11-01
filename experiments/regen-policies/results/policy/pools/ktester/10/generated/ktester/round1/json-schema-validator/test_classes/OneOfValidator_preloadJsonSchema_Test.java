package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode // the array node passed to the constructor
    schemaNode;

    @Mock
    JsonNode // individual child schema nodes
    childNode1;

    @Mock
    JsonNode childNode2;

    @Mock
    JsonSchema // parent schema – not used by the method under test
    parentSchema;

    @Mock
    JsonSchema // schemas that will be stored inside OneOfValidator
    childSchema1;

    @Mock
    JsonSchema childSchema2;

    /* --------------------------------------------------------------------- *
     *  Helper method to create a validator with a pre‑populated schemas list *
     * --------------------------------------------------------------------- */
    private OneOfValidator createValidatorWithSchemas(JsonSchema... schemas) {
        // Use a dummy schemaNode (size 0) so the constructor does not add any schemas.
        when(schemaNode.size()).thenReturn(0);
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        try {
            Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            f.set(validator, new ArrayList<>(Arrays.asList(schemas)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return validator;
    }

    @Test
    void preloadJsonSchema_callsInitializeValidatorsOnAllSchemas() {
        // ----- GIVEN ---------------------------------------------------------
        // The schemaNode represents an array with two child schemas
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(childNode1);
        when(schemaNode.get(1)).thenReturn(childNode2);
        // ValidationContext creates a JsonSchema for each child node
        when(validationContext.newSchema(any(), any(), eq(childNode1), any())).thenReturn(childSchema1);
        when(validationContext.newSchema(any(), any(), eq(childNode2), any())).thenReturn(childSchema2);
        // ----- WHEN ----------------------------------------------------------
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
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
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        validator.preloadJsonSchema();
        // ----- THEN ----------------------------------------------------------
        // No schema instances exist, so initializeValidators() must never be called.
        verify(validationContext, never()).newSchema(any(), any(), any(), any());
    }

    @Test
    void preloadJsonSchema_singleSchema_initializesOnce() {
        // ----- GIVEN ---------------------------------------------------------
        when(schemaNode.size()).thenReturn(1);
        when(schemaNode.get(0)).thenReturn(childNode1);
        when(validationContext.newSchema(any(), any(), eq(childNode1), any())).thenReturn(childSchema1);
        // ----- WHEN ----------------------------------------------------------
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        validator.preloadJsonSchema();
        // ----- THEN ----------------------------------------------------------
        verify(childSchema1, times(1)).initializeValidators();
        verifyNoMoreInteractions(childSchema1);
    }

    @Test
    void preloadJsonSchema_multipleSchemas_initializesAllInOrder() {
        // ----- GIVEN ---------------------------------------------------------
        when(schemaNode.size()).thenReturn(3);
        JsonNode childNode3 = mock(JsonNode.class);
        JsonSchema childSchema3 = mock(JsonSchema.class);
        when(schemaNode.get(0)).thenReturn(childNode1);
        when(schemaNode.get(1)).thenReturn(childNode2);
        when(schemaNode.get(2)).thenReturn(childNode3);
        when(validationContext.newSchema(any(), any(), eq(childNode1), any())).thenReturn(childSchema1);
        when(validationContext.newSchema(any(), any(), eq(childNode2), any())).thenReturn(childSchema2);
        when(validationContext.newSchema(any(), any(), eq(childNode3), any())).thenReturn(childSchema3);
        // ----- WHEN ----------------------------------------------------------
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        validator.preloadJsonSchema();
        // ----- THEN ----------------------------------------------------------
        InOrder inOrder = inOrder(childSchema1, childSchema2, childSchema3);
        inOrder.verify(childSchema1).initializeValidators();
        inOrder.verify(childSchema2).initializeValidators();
        inOrder.verify(childSchema3).initializeValidators();
    }

    @Test
    void preloadJsonSchema_exceptionFromSchema_propagatesRuntimeException() {
        // ----- GIVEN ---------------------------------------------------------
        JsonSchema explodingSchema = mock(JsonSchema.class);
        doThrow(new RuntimeException("boom")).when(explodingSchema).initializeValidators();
        OneOfValidator validator = createValidatorWithSchemas(explodingSchema);
        // ----- WHEN / THEN -------------------------------------------------
        RuntimeException ex = assertThrows(RuntimeException.class, validator::preloadJsonSchema, "RuntimeException should propagate out of preloadJsonSchema");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void preloadJsonSchema_nullSchemaElement_throwsNullPointerException() {
        // ----- GIVEN ---------------------------------------------------------
        // List contains a null entry
        OneOfValidator validator = createValidatorWithSchemas(childSchema1, null, childSchema2);
        // ----- WHEN / THEN -------------------------------------------------
        assertThrows(NullPointerException.class, validator::preloadJsonSchema, "NullPointerException expected when a null schema is present");
    }

    @Test
    void preloadJsonSchema_multipleInvocations_callsInitializeEachTime() {
        // ----- GIVEN ---------------------------------------------------------
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(childNode1);
        when(schemaNode.get(1)).thenReturn(childNode2);
        when(validationContext.newSchema(any(), any(), eq(childNode1), any())).thenReturn(childSchema1);
        when(validationContext.newSchema(any(), any(), eq(childNode2), any())).thenReturn(childSchema2);
        OneOfValidator validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        // ----- WHEN ----------------------------------------------------------
        // first call
        validator.preloadJsonSchema();
        // second call
        validator.preloadJsonSchema();
        // ----- THEN ----------------------------------------------------------
        verify(childSchema1, times(2)).initializeValidators();
        verify(childSchema2, times(2)).initializeValidators();
    }

    @Test
    void preloadJsonSchema_duplicateSchemaInstances_eachOccurrenceInvoked() {
        // ----- GIVEN ---------------------------------------------------------
        // Same mock instance appears twice
        OneOfValidator validator = createValidatorWithSchemas(childSchema1, childSchema1);
        // ----- WHEN ----------------------------------------------------------
        validator.preloadJsonSchema();
        // ----- THEN ----------------------------------------------------------
        verify(childSchema1, times(2)).initializeValidators();
    }

    @Test
    void preloadJsonSchema_orderPreservedWithSideEffects() {
        // ----- GIVEN ---------------------------------------------------------
        List<String> callOrder = new ArrayList<>();
        // Schemas record their invocation order
        doAnswer(invocation -> {
            callOrder.add("first");
            return null;
        }).when(childSchema1).initializeValidators();
        doAnswer(invocation -> {
            callOrder.add("second");
            return null;
        }).when(childSchema2).initializeValidators();
        OneOfValidator validator = createValidatorWithSchemas(childSchema1, childSchema2);
        // ----- WHEN ----------------------------------------------------------
        validator.preloadJsonSchema();
        // ----- THEN ----------------------------------------------------------
        assertEquals(List.of("first", "second"), callOrder, "initializeValidators should be called in list order");
    }

    @Test
    void preloadJsonSchema_errorFromSchema_propagatesErrorUnchanged() {
        // ----- GIVEN ---------------------------------------------------------
        JsonSchema errorSchema = mock(JsonSchema.class);
        doThrow(new Error("fatal")).when(errorSchema).initializeValidators();
        OneOfValidator validator = createValidatorWithSchemas(errorSchema);
        // ----- WHEN / THEN -------------------------------------------------
        Error err = assertThrows(Error.class, validator::preloadJsonSchema, "Error should propagate unchanged");
        assertEquals("fatal", err.getMessage());
    }

    @Test
    void preloadJsonSchema_concurrentModificationDuringIteration_throwsCME() throws Exception {
        // ----- GIVEN ---------------------------------------------------------
        // Prepare a validator with a single schema that will modify the list
        JsonSchema mutatingSchema = mock(JsonSchema.class);
        OneOfValidator validator = createValidatorWithSchemas(mutatingSchema);
        // Obtain the internal list via reflection so the mock can modify it
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> internalList = (List<JsonSchema>) schemasField.get(validator);
        // The mock adds a new element during initializeValidators()
        doAnswer(invocation -> {
            internalList.add(mock(JsonSchema.class));
            return null;
        }).when(mutatingSchema).initializeValidators();
        // ----- WHEN / THEN -------------------------------------------------
        assertThrows(ConcurrentModificationException.class, validator::preloadJsonSchema, "Modifying the schemas list during iteration should cause CME");
    }

    @Test
    void preloadJsonSchema_nullSchemasList_throwsNullPointerException() throws Exception {
        // ----- GIVEN ---------------------------------------------------------
        OneOfValidator validator = createValidatorWithSchemas(childSchema1);
        // Force the private field to null
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, null);
        // ----- WHEN / THEN -------------------------------------------------
        assertThrows(NullPointerException.class, validator::preloadJsonSchema, "NullPointerException expected when internal schemas list is null");
    }
}
