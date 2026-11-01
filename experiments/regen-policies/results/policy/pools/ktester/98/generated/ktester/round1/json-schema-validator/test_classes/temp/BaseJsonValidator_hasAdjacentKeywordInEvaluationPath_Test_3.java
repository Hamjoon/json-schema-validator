package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.InvocationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import java.util.Set;
import java.util.Collections;

class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /* --------------------------------------------------------------------- */
    private static class SimpleJsonSchema extends JsonSchema {

        private final List<JsonValidator> validators;

        private final JsonSchema parent;

        SimpleJsonSchema(List<JsonValidator> validators, JsonSchema parent) {
            // The super‑constructor of {@link JsonSchema} has many arguments.
            // For the purpose of these tests we can safely pass {@code null}
            // (or {@code false}) for everything that is not used.
            super(null, null, null, null, null, false);
            this.validators = validators;
            this.parent = parent;
        }

        @Override
        public List<JsonValidator> getValidators() {
            return validators;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return parent;
        }
    }

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} used for testing.
     *
     * <p>It stores the evaluation‑parent schema and the keyword that should be
     * returned by {@link #getKeyword()}.</p>
     */
    static class TestValidator extends BaseJsonValidator {

        private final JsonSchema evaluationParentSchema;

        private final String keyword;

        /**
         * Helper that creates a non‑null {@link ValidationContext}.
         */
        private static ValidationContext dummyContext() {
            // The builder creates a fully initialised context with default values.
            return ValidationContext.builder().build();
        }

        TestValidator(JsonSchema evaluationParentSchema) {
            this(evaluationParentSchema, null);
        }

        TestValidator(JsonSchema evaluationParentSchema, String keyword) {
            this(evaluationParentSchema, keyword, dummyContext());
        }

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext ignored) {
            this(evaluationParentSchema, null, dummyContext());
        }

        TestValidator(JsonSchema evaluationParentSchema, String keyword, ValidationContext ignored) {
            // The super‑constructor arguments that are irrelevant for the
            // tests are passed as {@code null} / {@code false}.
            super(null, null, null, null, null, null, dummyContext(), false);
            this.evaluationParentSchema = evaluationParentSchema;
            this.keyword = keyword;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return evaluationParentSchema;
        }

        @Override
        public String getKeyword() {
            return keyword;
        }

        /* ----------------------------------------------------------------- */
        /* The following methods are required by the JsonValidator interface  */
        /* but are irrelevant for the tests of hasAdjacentKeywordInEvaluationPath. */
        /* ----------------------------------------------------------------- */
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
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath path) {
            return Collections.emptySet();
        }
    }

    /**
     * Helper that creates a {@link JsonSchema} proxy exposing only the two methods
     * needed by {@code BaseJsonValidator.hasAdjacentKeywordInEvaluationPath}:
     * {@code getValidators()} and {@code getEvaluationParentSchema()}.
     */
    private static JsonSchema schema(List<JsonValidator> validators, JsonSchema parent) {
        return new SimpleJsonSchema(validators, parent);
    }

    @Test
    void returnsTrueWhenKeywordFoundInImmediateParent() {
        TestValidator matchingValidator = new TestValidator(null, "unevaluatedItems");
        JsonSchema parentSchema = schema(List.of(matchingValidator), null);
        TestValidator validator = new TestValidator(parentSchema);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedItems"), "Keyword should be found in the immediate evaluation‑parent schema");
    }

    @Test
    void returnsTrueWhenKeywordFoundInAncestorSchema() {
        TestValidator otherValidator = new TestValidator(null, "someOtherKeyword");
        JsonSchema leafSchema = schema(List.of(otherValidator), null);
        TestValidator matchingValidator = new TestValidator(null, "unevaluatedProperties");
        JsonSchema ancestorSchema = schema(List.of(matchingValidator), null);
        JsonSchema leafWithParent = schema(List.of(otherValidator), ancestorSchema);
        TestValidator validator = new TestValidator(leafWithParent);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedProperties"), "Keyword should be found in an ancestor evaluation‑parent schema");
    }

    @Test
    void returnsFalseWhenKeywordNotPresentInAnySchema() {
        TestValidator leafValidator = new TestValidator(null, "leafKeyword");
        JsonSchema leafSchema = schema(List.of(leafValidator), null);
        TestValidator parentValidator = new TestValidator(null, "parentKeyword");
        JsonSchema parentSchema = schema(List.of(parentValidator), null);
        JsonSchema leafWithParent = schema(List.of(leafValidator), parentSchema);
        TestValidator validator = new TestValidator(leafWithParent);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("nonExistingKeyword"), "Method should return false when the keyword is absent in the whole chain");
    }

    @Test
    void returnsFalseWhenNoEvaluationParentSchema() {
        TestValidator validator = new TestValidator(null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Method should return false when there is no evaluation‑parent schema");
    }

    @Test
    @DisplayName("Parent schema without validators → returns false")
    void returnsFalseWhenParentSchemaHasNoValidators() {
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Empty validator list should lead to false.");
    }

    @Test
    @DisplayName("Multiple validators, early break → returns true")
    void returnsTrueWhenEarlyBreakOccurs() {
        TestValidator matching = new TestValidator(null, "exclusiveMaximum");
        TestValidator shouldNotBeVisited = new TestValidator(null, "shouldNotBeVisited") {

            @Override
            public String getKeyword() {
                throw new AssertionError("Should not be visited");
            }
        };
        JsonSchema parent = schema(List.of(matching, shouldNotBeVisited), null);
        TestValidator validator = new TestValidator(parent);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("exclusiveMaximum"), "Method should stop after finding the first matching validator.");
    }

    @Test
    @DisplayName("Validator with null keyword → returns false")
    void returnsFalseWhenValidatorKeywordIsNull() {
        TestValidator nullKeywordValidator = new TestValidator(null, (String) null);
        JsonSchema parent = schema(List.of(nullKeywordValidator), null);
        TestValidator validator = new TestValidator(parent);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("required"), "Null keyword from a validator must be ignored and result in false.");
    }

    @Test
    @DisplayName("Null input keyword → throws NullPointerException")
    void throwsNPEWhenKeywordIsNull() {
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath(null), "Passing null keyword should result in NullPointerException.");
    }

    @Test
    @DisplayName("No match across multiple ancestors → returns false")
    void returnsFalseWhenNoMatchAcrossAncestors() {
        TestValidator leafV = new TestValidator(null, "leafK");
        JsonSchema leaf = schema(List.of(leafV), null);
        TestValidator middleV = new TestValidator(null, "middleK");
        JsonSchema middle = schema(List.of(middleV), null);
        TestValidator topV = new TestValidator(null, "topK");
        JsonSchema top = schema(List.of(topV), null);
        JsonSchema leafWithParent = schema(List.of(leafV), middle);
        JsonSchema middleWithParent = schema(List.of(middleV), top);
        // The leaf schema already knows its parent (middleWithParent) via the chain.
        TestValidator validator = new TestValidator(leafWithParent);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Keyword absent in the whole chain should yield false.");
    }

    @Test
    @DisplayName("Keyword matches validator in current schema only → returns false")
    void returnsFalseWhenKeywordOnlyInCurrentSchema() {
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("type"), "Method should ignore validators belonging to the current schema.");
    }

    @Test
    @DisplayName("Validator list contains null element → throws NullPointerException")
    void throwsNPEWhenValidatorListContainsNull() {
        JsonSchema parent = schema(Collections.singletonList(null), null);
        TestValidator validator = new TestValidator(parent);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("maxItems"), "Null element in validators list should cause NPE.");
    }

    @Test
    @DisplayName("Schema validators collection is null → throws NullPointerException")
    void throwsNPEWhenValidatorsCollectionIsNull() {
        JsonSchema parent = schema(null, null);
        TestValidator validator = new TestValidator(parent);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("minLength"), "Null validators collection should cause NPE.");
    }

    @Test
    @DisplayName("Empty keyword matches empty validator keyword → returns true")
    void returnsTrueWhenEmptyKeywordMatchesEmptyValidatorKeyword() {
        TestValidator emptyKeywordValidator = new TestValidator(null, "");
        JsonSchema parent = schema(List.of(emptyKeywordValidator), null);
        TestValidator validator = new TestValidator(parent);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath(""), "Empty string keyword should match validator returning empty string.");
    }

    @Test
    @DisplayName("Case‑sensitivity mismatch → returns false")
    void returnsFalseWhenCaseMismatch() {
        TestValidator validatorWithKeyword = new TestValidator(null, "maxLength");
        JsonSchema parent = schema(List.of(validatorWithKeyword), null);
        TestValidator testValidator = new TestValidator(parent);
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath("MAXLENGTH"), "Keyword comparison is case‑sensitive; mismatch should return false.");
    }

    @Test
    @DisplayName("Keyword with surrounding whitespace does not match → returns false")
    void returnsFalseWhenKeywordHasWhitespace() {
        TestValidator validatorWithKeyword = new TestValidator(null, "pattern");
        JsonSchema parent = schema(List.of(validatorWithKeyword), null);
        TestValidator testValidator = new TestValidator(parent);
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath(" pattern "), "Whitespace around keyword should cause mismatch.");
    }

    @Test
    @DisplayName("Validator list contains non‑JsonValidator element → throws ClassCastException")
    void throwsClassCastExceptionWhenNonJsonValidatorInList() {
        @SuppressWarnings("unchecked")
        List rawList = new java.util.ArrayList();
        rawList.add("I am not a JsonValidator");
        JsonSchema parent = schema(rawList, null);
        TestValidator validator = new TestValidator(parent);
        assertThrows(ClassCastException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Non‑JsonValidator element should cause ClassCastException.");
    }
}
