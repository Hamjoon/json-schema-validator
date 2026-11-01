package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

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

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}.
     * The {@code validate} method required by {@link JsonValidator} simply
     * returns an empty set – validation itself is not relevant for these
     * tests.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
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

    /**
     * Creates a {@link ValidationContext} instance with all factories set to {@code null}.
     */
    private static ValidationContext createEmptyValidationContext() {
        // The constructor of ValidationContext accepts the configuration objects.
        // Supplying {@code null} for all of them is sufficient for the
        // {@code fetchSubSchemaNode} implementation used in these tests.
        return new ValidationContext(null, null, null, null, null);
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        // Arrange – validator with suppression enabled.
        TestValidator validator = new TestValidator("dummyPath", JsonNodeFactory.instance.objectNode(), null, null, createEmptyValidationContext(), true);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        // Assert
        assertNull(result, "When sub‑schema retrieval is suppressed the method must return null");
    }

    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed() {
        // Arrange – validator with suppression disabled.
        TestValidator validator = new TestValidator("dummyPath", JsonNodeFactory.instance.objectNode(), null, null, createEmptyValidationContext(), false);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        // Assert
        assertNotNull(result, "When sub‑schema retrieval is not suppressed a JsonSchema should be returned");
    }

    /* -------------------------------------------------------------
     * 1. suppressSubSchemaRetrieval = true – always returns null.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext() {
        TestValidator validator = new TestValidator("unusedPath", JsonNodeFactory.instance.objectNode(), null, null, createEmptyValidationContext(), true);
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        assertNull(result, "With suppressSubSchemaRetrieval=true the method must return null");
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNullContext() {
        TestValidator validator = new TestValidator("unusedPath", JsonNodeFactory.instance.objectNode(), null, null, null, true);
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Even with a null ValidationContext the method must return null");
    }

    /* -------------------------------------------------------------
     * 2. suppressSubSchemaRetrieval = false – normal delegation.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode() {
        // Arrange – validator that does NOT suppress retrieval and has a non‑null schema node.
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        // (optional) put a dummy property so the node is not completely empty
        schemaNode.put("type", "object");
        TestValidator validator = new TestValidator("unusedPath", schemaNode, null, null, createEmptyValidationContext(), false);
        // Act
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        // Assert
        assertNotNull(result, "When not suppressed a JsonSchema instance should be returned");
    }
}
