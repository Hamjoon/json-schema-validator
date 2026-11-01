```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#hasAdjacentKeywordInEvaluationPath(String)}.
 *
 * The tests verify that the method correctly walks the evaluation‑parent schema chain
 * and returns {@code true} when a validator with the supplied keyword is found,
 * otherwise {@code false}.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /** Minimal concrete subclass of {@link BaseJsonValidator} used for testing. */
    static class TestValidator extends BaseJsonValidator {

        /** The schema that is considered the evaluation‑parent of this validator. */
        private final JsonSchema evaluationParentSchema;

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext validationContext) {
            // All constructor arguments that are not used by the method under test are passed as null/false.
            super(null, null, null, null, null, null, validationContext, false);
            this.evaluationParentSchema = evaluationParentSchema;
        }

        /** Expose the evaluation‑parent schema so the method can traverse it. */
        @Override
        public JsonSchema getEvaluationParentSchema() {
            return evaluationParentSchema;
        }

        /* --------------------------------------------------------------------- */
        /* The following methods are required by the JsonValidator interface but
           are irrelevant for the tests of hasAdjacentKeywordInEvaluationPath.
           They are therefore stubbed with harmless defaults.                */
        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return null;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /* Test cases                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void returnsTrueWhenKeywordFoundInImmediateParent() {
        // Mock a validator whose keyword matches the searched one.
        JsonValidator matchingValidator = mock(JsonValidator.class);
        when(matchingValidator.getKeyword()).thenReturn("unevaluatedItems");

        // Mock the parent schema to return the matching validator.
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.getValidators()).thenReturn(List.of(matchingValidator));
        when(parentSchema.getEvaluationParentSchema()).thenReturn(null); // end of chain

        // Create the validator under test with the mocked parent schema.
        TestValidator validator = new TestValidator(parentSchema, mock(ValidationContext.class));

        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedItems"),
                "Keyword should be found in the immediate evaluation‑parent schema");
    }

    @Test
    void returnsTrueWhenKeywordFoundInAncestorSchema() {
        // Leaf schema – does NOT contain the keyword.
        JsonValidator otherValidator = mock(JsonValidator.class);
        when(otherValidator.getKeyword()).thenReturn("someOtherKeyword");
        JsonSchema leafSchema = mock(JsonSchema.class);
        when(leafSchema.getValidators()).thenReturn(List.of(otherValidator));

        // Ancestor (parent) schema – contains the keyword we are looking for.
        JsonValidator matchingValidator = mock(JsonValidator.class);
        when(matchingValidator.getKeyword()).thenReturn("unevaluatedProperties");
        JsonSchema ancestorSchema = mock(JsonSchema.class);
        when(ancestorSchema.getValidators()).thenReturn(List.of(matchingValidator));
        when(ancestorSchema.getEvaluationParentSchema()).thenReturn(null); // top of chain

        // Wire the chain: leaf → ancestor.
        when(leafSchema.getEvaluationParentSchema()).thenReturn(ancestorSchema);

        TestValidator validator = new TestValidator(leafSchema, mock(ValidationContext.class));

        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedProperties"),
                "Keyword should be found in an ancestor evaluation‑parent schema");
    }

    @Test
    void returnsFalseWhenKeywordNotPresentInAnySchema() {
        // Leaf schema – no matching keyword.
        JsonValidator leafValidator = mock(JsonValidator.class);
        when(leafValidator.getKeyword()).thenReturn("leafKeyword");
        JsonSchema leafSchema = mock(JsonSchema.class);
        when(leafSchema.getValidators()).thenReturn(List.of(leafValidator));

        // Parent schema – also no matching keyword.
        JsonValidator parentValidator = mock(JsonValidator.class);
        when(parentValidator.getKeyword()).thenReturn("parentKeyword");
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.getValidators()).thenReturn(List.of(parentValidator));
        when(parentSchema.getEvaluationParentSchema()).thenReturn(null); // end of chain

        // Chain leaf → parent.
        when(leafSchema.getEvaluationParentSchema()).thenReturn(parentSchema);

        TestValidator validator = new TestValidator(leafSchema, mock(ValidationContext.class));

        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("nonExistingKeyword"),
                "Method should return false when the keyword is absent in the whole chain");
    }

    @Test
    void returnsFalseWhenNoEvaluationParentSchema() {
        // The validator has no evaluation‑parent schema at all.
        TestValidator validator = new TestValidator(null, mock(ValidationContext.class));

        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"),
                "Method should return false when there is no evaluation‑parent schema");
    }
}
```