package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    /* -------------------------------------------------------------
     * Minimal concrete implementation of the abstract BaseJsonValidator.
     * ------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Validation is out of scope for these tests.
            return Collections.emptySet();
        }
    }

    /* -------------------------------------------------------------
     * Helper to set the protected field {@code schemaNode} via reflection.
     * ------------------------------------------------------------- */
    private static void setSchemaNode(BaseJsonValidator validator, JsonNode node) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("schemaNode");
            f.setAccessible(true);
            f.set(validator, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     * Helper to set the protected field {@code suppressSubSchemaRetrieval}
     * via reflection (used only for sanity‑checking that the constructor
     * really stores the value we pass).
     * ------------------------------------------------------------- */
    private static void setSuppressFlag(BaseJsonValidator validator, boolean flag) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
            f.setAccessible(true);
            f.setBoolean(validator, flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        // Arrange: create a validator with suppressSubSchemaRetrieval = true
        TestValidator validator = new TestValidator("dummyPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // suppressSubSchemaRetrieval
        true);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        // Assert
        assertNull(result, "When sub‑schema retrieval is suppressed the method must return null");
    }

    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed() {
        // Arrange: validator with suppressSubSchemaRetrieval = false.
        // The private static method will be exercised; we only verify that
        // the result is *not* null for a well‑formed (mocked) schema node.
        TestValidator validator = new TestValidator("dummyPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // do NOT suppress
        false);
        // The private method creates a new JsonSchema instance; to keep the test
        // lightweight we let it run with the mocked arguments. The constructor
        // of JsonSchema expects a non‑null ValidationContext, so we stub the
        // minimal behaviour needed.
        when(mockValidationContext.getConfig()).thenReturn(null);
        when(mockValidationContext.getMetaSchema()).thenReturn(null);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(null);
        when(mockValidationContext.getUriFactory()).thenReturn(null);
        when(mockValidationContext.getUrnFactory()).thenReturn(null);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        // Assert
        assertNotNull(result, "When sub‑schema retrieval is not suppressed a JsonSchema should be returned");
    }

    /* -------------------------------------------------------------
     * 1. suppressSubSchemaRetrieval = true – always returns null.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext() {
        TestValidator validator = new TestValidator("unusedPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // suppress flag = true
        true);
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result, "With suppressSubSchemaRetrieval=true the method must return null");
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNullContext() {
        TestValidator validator = new TestValidator("unusedPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // suppress flag = true
        true);
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Even with a null ValidationContext the method must return null");
    }

    /* -------------------------------------------------------------
     * 2. suppressSubSchemaRetrieval = false – normal delegation.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode() {
        // Arrange – a validator that does NOT suppress retrieval and has a
        // non‑null schema node.  The real obtainSubSchemaNode will create a
        // JsonSchema instance; we only assert that the result is non‑null.
        TestValidator validator = new TestValidator("unusedPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // suppress flag = false
        false);
        // Minimal stubbing of ValidationContext – the private helper only needs
        // the factories to be non‑null.  Returning null works for the current
        // implementation because the helper guards against NPEs.
        when(mockValidationContext.getConfig()).thenReturn(null);
        when(mockValidationContext.getMetaSchema()).thenReturn(null);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(null);
        when(mockValidationContext.getUriFactory()).thenReturn(null);
        when(mockValidationContext.getUrnFactory()).thenReturn(null);
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNotNull(result, "When not suppressed a JsonSchema instance should be returned");
    }
}
