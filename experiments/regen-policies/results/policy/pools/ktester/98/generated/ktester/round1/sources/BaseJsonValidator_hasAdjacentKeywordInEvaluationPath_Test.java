package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.*;
import java.util.List;
import java.util.Set;
import java.util.Collections;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} used for testing.
     *
     * <p>It stores the evaluation‑parent schema and the keyword that should be
     * returned by {@link #getKeyword()}.</p>
     */
    static class TestValidator extends BaseJsonValidator {

        private final JsonSchema evaluationParentSchema;

        private final String keyword;

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext validationContext) {
            this(evaluationParentSchema, validationContext, null);
        }

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext validationContext, String keyword) {
            // The super‑constructor arguments that are not needed for the test are passed as null/false.
            super(null, null, null, null, null, null, validationContext, false);
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

        /* --------------------------------------------------------------------- */
        /* The following methods are required by the JsonValidator interface but
           are irrelevant for the tests of hasAdjacentKeywordInEvaluationPath.
           They are therefore stubbed with harmless defaults.                */
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
        InvocationHandler handler = (proxy, method, args) -> {
            switch(method.getName()) {
                case "getValidators":
                    return validators;
                case "getEvaluationParentSchema":
                    return parent;
                default:
                    // All other methods return null (or a sensible default) – they are never used in the tests.
                    return null;
            }
        };
        return (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(), new Class[] { JsonSchema.class }, handler);
    }

    @Test
    void returnsTrueWhenKeywordFoundInImmediateParent() {
        // validator that matches the searched keyword
        TestValidator matchingValidator = new TestValidator(null, null, "unevaluatedItems");
        // parent schema containing the matching validator
        JsonSchema parentSchema = schema(List.of(matchingValidator), null);
        // validator under test whose evaluation‑parent is the above schema
        TestValidator validator = new TestValidator(parentSchema, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedItems"), "Keyword should be found in the immediate evaluation‑parent schema");
    }

    @Test
    void returnsTrueWhenKeywordFoundInAncestorSchema() {
        // leaf schema – does NOT contain the keyword.
        TestValidator otherValidator = new TestValidator(null, null, "someOtherKeyword");
        JsonSchema leafSchema = schema(List.of(otherValidator), null);
        // ancestor (parent) schema – contains the keyword we are looking for.
        TestValidator matchingValidator = new TestValidator(null, null, "unevaluatedProperties");
        JsonSchema ancestorSchema = schema(List.of(matchingValidator), null);
        // wire the chain: leaf → ancestor
        JsonSchema leafWithParent = schema(List.of(otherValidator), ancestorSchema);
        TestValidator validator = new TestValidator(leafWithParent, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedProperties"), "Keyword should be found in an ancestor evaluation‑parent schema");
    }

    @Test
    void returnsFalseWhenKeywordNotPresentInAnySchema() {
        // leaf schema – no matching keyword.
        TestValidator leafValidator = new TestValidator(null, null, "leafKeyword");
        JsonSchema leafSchema = schema(List.of(leafValidator), null);
        // parent schema – also no matching keyword.
        TestValidator parentValidator = new TestValidator(null, null, "parentKeyword");
        JsonSchema parentSchema = schema(List.of(parentValidator), null);
        // chain leaf → parent.
        JsonSchema leafWithParent = schema(List.of(leafValidator), parentSchema);
        TestValidator validator = new TestValidator(leafWithParent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("nonExistingKeyword"), "Method should return false when the keyword is absent in the whole chain");
    }

    @Test
    void returnsFalseWhenNoEvaluationParentSchema() {
        // The validator has no evaluation‑parent schema at all.
        TestValidator validator = new TestValidator(null, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Method should return false when there is no evaluation‑parent schema");
    }

    @Test
    @DisplayName("Parent schema without validators → returns false")
    void returnsFalseWhenParentSchemaHasNoValidators() {
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Empty validator list should lead to false.");
    }

    @Test
    @DisplayName("Multiple validators, early break → returns true")
    void returnsTrueWhenEarlyBreakOccurs() {
        // matching validator (first in list)
        TestValidator matching = new TestValidator(null, null, "exclusiveMaximum");
        // a second validator that would throw if inspected (ensures early break)
        TestValidator shouldNotBeVisited = new TestValidator(null, null, "shouldNotBeVisited") {

            @Override
            public String getKeyword() {
                throw new AssertionError("Should not be visited");
            }
        };
        JsonSchema parent = schema(List.of(matching, shouldNotBeVisited), null);
        TestValidator validator = new TestValidator(parent, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("exclusiveMaximum"), "Method should stop after finding the first matching validator.");
    }

    @Test
    @DisplayName("Validator with null keyword → returns false")
    void returnsFalseWhenValidatorKeywordIsNull() {
        TestValidator nullKeywordValidator = new TestValidator(null, null, null);
        JsonSchema parent = schema(List.of(nullKeywordValidator), null);
        TestValidator validator = new TestValidator(parent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("required"), "Null keyword from a validator must be ignored and result in false.");
    }

    @Test
    @DisplayName("Null input keyword → throws NullPointerException")
    void throwsNPEWhenKeywordIsNull() {
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath(null), "Passing null keyword should result in NullPointerException.");
    }

    @Test
    @DisplayName("No match across multiple ancestors → returns false")
    void returnsFalseWhenNoMatchAcrossAncestors() {
        // leaf schema
        TestValidator leafV = new TestValidator(null, null, "leafK");
        JsonSchema leaf = schema(List.of(leafV), null);
        // middle schema
        TestValidator middleV = new TestValidator(null, null, "middleK");
        JsonSchema middle = schema(List.of(middleV), null);
        // top schema
        TestValidator topV = new TestValidator(null, null, "topK");
        JsonSchema top = schema(List.of(topV), null);
        // chain leaf → middle → top
        JsonSchema leafWithParent = schema(List.of(leafV), middle);
        JsonSchema middleWithParent = schema(List.of(middleV), top);
        // Use the leaf that has the proper parent chain
        TestValidator validator = new TestValidator(leafWithParent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Keyword absent in the whole chain should yield false.");
    }

    @Test
    @DisplayName("Keyword matches validator in current schema only → returns false")
    void returnsFalseWhenKeywordOnlyInCurrentSchema() {
        // The evaluation‑parent schema has no validators.
        JsonSchema parent = schema(Collections.emptyList(), null);
        TestValidator validator = new TestValidator(parent, null);
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("type"), "Method should ignore validators belonging to the current schema.");
    }

    @Test
    @DisplayName("Validator list contains null element → throws NullPointerException")
    void throwsNPEWhenValidatorListContainsNull() {
        JsonSchema parent = schema(Collections.singletonList(null), null);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("maxItems"), "Null element in validators list should cause NPE.");
    }

    @Test
    @DisplayName("Schema validators collection is null → throws NullPointerException")
    void throwsNPEWhenValidatorsCollectionIsNull() {
        // Use a proxy that returns null for getValidators()
        InvocationHandler handler = (proxy, method, args) -> {
            if ("getValidators".equals(method.getName())) {
                return null;
            }
            if ("getEvaluationParentSchema".equals(method.getName())) {
                return null;
            }
            return null;
        };
        JsonSchema parent = (JsonSchema) Proxy.newProxyInstance(JsonSchema.class.getClassLoader(), new Class[] { JsonSchema.class }, handler);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("minLength"), "Null validators collection should cause NPE.");
    }

    @Test
    @DisplayName("Empty keyword matches empty validator keyword → returns true")
    void returnsTrueWhenEmptyKeywordMatchesEmptyValidatorKeyword() {
        TestValidator emptyKeywordValidator = new TestValidator(null, null, "");
        JsonSchema parent = schema(List.of(emptyKeywordValidator), null);
        TestValidator validator = new TestValidator(parent, null);
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath(""), "Empty string keyword should match validator returning empty string.");
    }

    @Test
    @DisplayName("Case‑sensitivity mismatch → returns false")
    void returnsFalseWhenCaseMismatch() {
        TestValidator validatorWithKeyword = new TestValidator(null, null, "maxLength");
        JsonSchema parent = schema(List.of(validatorWithKeyword), null);
        TestValidator testValidator = new TestValidator(parent, null);
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath("MAXLENGTH"), "Keyword comparison is case‑sensitive; mismatch should return false.");
    }

    @Test
    @DisplayName("Keyword with surrounding whitespace does not match → returns false")
    void returnsFalseWhenKeywordHasWhitespace() {
        TestValidator validatorWithKeyword = new TestValidator(null, null, "pattern");
        JsonSchema parent = schema(List.of(validatorWithKeyword), null);
        TestValidator testValidator = new TestValidator(parent, null);
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath(" pattern "), "Whitespace around keyword should cause mismatch.");
    }

    @Test
    @DisplayName("Validator list contains non‑JsonValidator element → throws ClassCastException")
    void throwsClassCastExceptionWhenNonJsonValidatorInList() {
        // Use a raw List to inject a non‑JsonValidator element (e.g., a String).
        @SuppressWarnings("unchecked")
        List rawList = new java.util.ArrayList();
        rawList.add("I am not a JsonValidator");
        JsonSchema parent = schema(rawList, null);
        TestValidator validator = new TestValidator(parent, null);
        assertThrows(ClassCastException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Non‑JsonValidator element should cause ClassCastException.");
    }
}