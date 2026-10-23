```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.uri.DefaultURIFactory;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * <p>The tests use a minimal concrete implementation of {@link BaseJsonValidator}
 * because the abstract class cannot be instantiated directly.  The {@code validate}
 * method is irrelevant for these tests, so it simply returns an empty set.</p>
 */
class BaseJsonValidator_fetchSubSchemaNode_Test {

    // -----------------------------------------------------------------------
    // Minimal concrete implementation of the abstract BaseJsonValidator.
    // -----------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType,
                  validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            // Validation itself is not part of these tests.
            return Collections.emptySet();
        }
    }

    // -----------------------------------------------------------------------
    // Helper methods to manipulate protected fields via reflection.
    // -----------------------------------------------------------------------
    private static void setSchemaNode(BaseJsonValidator validator, JsonNode node) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("schemaNode");
            f.setAccessible(true);
            f.set(validator, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setSuppressFlag(BaseJsonValidator validator, boolean flag) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
            f.setAccessible(true);
            f.setBoolean(validator, flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------------
    // Creates a ValidationContext that satisfies the constructor requirements.
    // -----------------------------------------------------------------------
    private static ValidationContext createEmptyValidationContext() {
        // The ValidationContext constructor requires a non‑null URIFactory.
        // We provide a default JsonSchemaFactory and a DefaultURIFactory; the
        // remaining arguments are not needed for the fetchSubSchemaNode tests.
        return new ValidationContext(
                JsonSchemaFactory.getInstance(),
                new DefaultURIFactory(),
                null,
                null,
                null);
    }

    // -----------------------------------------------------------------------
    // Test cases.
    // -----------------------------------------------------------------------
    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        TestValidator validator = new TestValidator(
                "dummyPath",
                JsonNodeFactory.instance.objectNode(),
                null,
                null,
                createEmptyValidationContext(),
                true);                         // suppression enabled
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        assertNull(result, "When sub‑schema retrieval is suppressed the method must return null");
    }

    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed() {
        TestValidator validator = new TestValidator(
                "dummyPath",
                JsonNodeFactory.instance.objectNode(),
                null,
                null,
                createEmptyValidationContext(),
                false);                        // suppression disabled
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        assertNotNull(result, "When sub‑schema retrieval is not suppressed a JsonSchema should be returned");
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext() {
        TestValidator validator = new TestValidator(
                "unusedPath",
                JsonNodeFactory.instance.objectNode(),
                null,
                null,
                createEmptyValidationContext(),
                true);
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        assertNull(result, "With suppressSubSchemaRetrieval=true the method must return null");
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNullContext() {
        TestValidator validator = new TestValidator(
                "unusedPath",
                JsonNodeFactory.instance.objectNode(),
                null,
                null,
                null,
                true);
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Even with a null ValidationContext the method must return null");
    }

    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode() {
        // A non‑empty schema node (e.g., {"type":"object"}) allows the factory
        // to produce a concrete JsonSchema instance.
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "object");

        TestValidator validator = new TestValidator(
                "unusedPath",
                schemaNode,
                null,
                null,
                createEmptyValidationContext(),
                false);
        JsonSchema result = validator.fetchSubSchemaNode(createEmptyValidationContext());
        assertNotNull(result, "When not suppressed a JsonSchema instance should be returned");
    }
}
```