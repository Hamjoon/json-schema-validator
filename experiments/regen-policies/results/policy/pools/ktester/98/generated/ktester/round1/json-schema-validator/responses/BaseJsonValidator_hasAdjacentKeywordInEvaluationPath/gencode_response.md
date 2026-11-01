```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#hasAdjacentKeywordInEvaluationPath(String)}.
 *
 * The tests cover a comprehensive set of scenarios described in the supplied JSON
 * test‑case specification, including normal matches, missing keywords, null handling,
 * early break optimisation and exceptional situations.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /** Minimal concrete subclass of {@link BaseJsonValidator} used for testing. */
    static class TestValidator extends BaseJsonValidator {

        /** The schema that is considered the evaluation‑parent of this validator. */
        private final JsonSchema evaluationParentSchema;

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext validationContext) {
            // Constructor arguments not needed for the method under test are passed as null/false.
            super(null, null, null, null, null, null, validationContext, false);
            this.evaluationParentSchema = evaluationParentSchema;
        }

        /** Expose the evaluation‑parent schema so the method can traverse it. */
        @Override
        public JsonSchema getEvaluationParentSchema() {
            return evaluationParentSchema;
        }

        /* --------------------------------------------------------------------- */
        /* Stub implementations required by the JsonValidator interface. */
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
    @DisplayName("No evaluation parent schema → returns false")
    void returnsFalseWhenNoEvaluationParentSchema() {
        TestValidator validator = new TestValidator(null, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("type"),
                "When getEvaluationParentSchema() returns null the method should return false.");
    }

    @Test
    @DisplayName("Parent schema without validators → returns false")
    void returnsFalseWhenParentSchemaHasNoValidators() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"),
                "Empty validator list should lead to false.");
    }

    @Test
    @DisplayName("Direct match in immediate parent → returns true")
    void returnsTrueWhenKeywordFoundInImmediateParent() {
        JsonValidator matching = mock(JsonValidator.class);
        when(matching.getKeyword()).thenReturn("maxLength");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(matching));
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("maxLength"),
                "Keyword should be found in the immediate evaluation‑parent schema.");
    }

    @Test
    @DisplayName("Match in ancestor schema (nested hierarchy) → returns true")
    void returnsTrueWhenKeywordFoundInAncestorSchema() {
        // leaf schema – no matching keyword
        JsonValidator leafValidator = mock(JsonValidator.class);
        when(leafValidator.getKeyword()).thenReturn("leafKeyword");
        JsonSchema leaf = mock(JsonSchema.class);
        when(leaf.getValidators()).thenReturn(List.of(leafValidator));

        // ancestor schema – contains the keyword we are looking for
        JsonValidator ancestorValidator = mock(JsonValidator.class);
        when(ancestorValidator.getKeyword()).thenReturn("pattern");
        JsonSchema ancestor = mock(JsonSchema.class);
        when(ancestor.getValidators()).thenReturn(List.of(ancestorValidator));
        when(ancestor.getEvaluationParentSchema()).thenReturn(null);

        // link leaf → ancestor
        when(leaf.getEvaluationParentSchema()).thenReturn(ancestor);

        TestValidator validator = new TestValidator(leaf, mock(ValidationContext.class));
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("pattern"),
                "Keyword should be found in an ancestor evaluation‑parent schema.");
    }

    @Test
    @DisplayName("Multiple validators, early break → returns true")
    void returnsTrueWhenEarlyBreakOccurs() {
        // matching validator (first in list)
        JsonValidator matching = mock(JsonValidator.class);
        when(matching.getKeyword()).thenReturn("exclusiveMaximum");

        // a second validator that would throw if inspected (ensures early break)
        JsonValidator shouldNotBeVisited = mock(JsonValidator.class);
        when(shouldNotBeVisited.getKeyword()).thenThrow(new AssertionError("Should not be visited"));

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(matching, shouldNotBeVisited));
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("exclusiveMaximum"),
                "Method should stop after finding the first matching validator.");
    }

    @Test
    @DisplayName("Validator with null keyword → returns false")
    void returnsFalseWhenValidatorKeywordIsNull() {
        JsonValidator nullKeywordValidator = mock(JsonValidator.class);
        when(nullKeywordValidator.getKeyword()).thenReturn(null);

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(nullKeywordValidator));
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("required"),
                "Null keyword from a validator must be ignored and result in false.");
    }

    @Test
    @DisplayName("Null input keyword → throws NullPointerException")
    void throwsNPEWhenKeywordIsNull() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertThrows(NullPointerException.class,
                () -> validator.hasAdjacentKeywordInEvaluationPath(null),
                "Passing null keyword should result in NullPointerException.");
    }

    @Test
    @DisplayName("No match across multiple ancestors → returns false")
    void returnsFalseWhenNoMatchAcrossAncestors() {
        // leaf schema
        JsonValidator leafV = mock(JsonValidator.class);
        when(leafV.getKeyword()).thenReturn("leafK");
        JsonSchema leaf = mock(JsonSchema.class);
        when(leaf.getValidators()).thenReturn(List.of(leafV));

        // middle schema
        JsonValidator middleV = mock(JsonValidator.class);
        when(middleV.getKeyword()).thenReturn("middleK");
        JsonSchema middle = mock(JsonSchema.class);
        when(middle.getValidators()).thenReturn(List.of(middleV));

        // top schema
        JsonValidator topV = mock(JsonValidator.class);
        when(topV.getKeyword()).thenReturn("topK");
        JsonSchema top = mock(JsonSchema.class);
        when(top.getValidators()).thenReturn(List.of(topV));
        when(top.getEvaluationParentSchema()).thenReturn(null);

        // chain leaf → middle → top
        when(leaf.getEvaluationParentSchema()).thenReturn(middle);
        when(middle.getEvaluationParentSchema()).thenReturn(top);

        TestValidator validator = new TestValidator(leaf, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"),
                "Keyword absent in the whole chain should yield false.");
    }

    @Test
    @DisplayName("Keyword matches validator in current schema only → returns false")
    void returnsFalseWhenKeywordOnlyInCurrentSchema() {
        // The evaluation‑parent schema has no validators.
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        // The validator's own schema (not part of the evaluation path) is irrelevant;
        // we do not need to model it.

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("type"),
                "Method should ignore validators belonging to the current schema.");
    }

    @Test
    @DisplayName("Validator list contains null element → throws NullPointerException")
    void throwsNPEWhenValidatorListContainsNull() {
        JsonSchema parent = mock(JsonSchema.class);
        // List with a single null entry
        when(parent.getValidators()).thenReturn(Collections.singletonList(null));
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertThrows(NullPointerException.class,
                () -> validator.hasAdjacentKeywordInEvaluationPath("maxItems"),
                "Null element in validators list should cause NPE.");
    }

    @Test
    @DisplayName("Schema validators collection is null → throws NullPointerException")
    void throwsNPEWhenValidatorsCollectionIsNull() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(null);
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertThrows(NullPointerException.class,
                () -> validator.hasAdjacentKeywordInEvaluationPath("minLength"),
                "Null validators collection should cause NPE.");
    }

    @Test
    @DisplayName("Empty keyword matches empty validator keyword → returns true")
    void returnsTrueWhenEmptyKeywordMatchesEmptyValidatorKeyword() {
        JsonValidator emptyKeywordValidator = mock(JsonValidator.class);
        when(emptyKeywordValidator.getKeyword()).thenReturn("");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(emptyKeywordValidator));
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath(""),
                "Empty string keyword should match validator returning empty string.");
    }

    @Test
    @DisplayName("Case‑sensitivity mismatch → returns false")
    void returnsFalseWhenCaseMismatch() {
        JsonValidator validatorWithKeyword = mock(JsonValidator.class);
        when(validatorWithKeyword.getKeyword()).thenReturn("maxLength");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(validatorWithKeyword));
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator testValidator = new TestValidator(parent, mock(ValidationContext.class));
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath("MAXLENGTH"),
                "Keyword comparison is case‑sensitive; mismatch should return false.");
    }

    @Test
    @DisplayName("Keyword with surrounding whitespace does not match → returns false")
    void returnsFalseWhenKeywordHasWhitespace() {
        JsonValidator validatorWithKeyword = mock(JsonValidator.class);
        when(validatorWithKeyword.getKeyword()).thenReturn("pattern");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(List.of(validatorWithKeyword));
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator testValidator = new TestValidator(parent, mock(ValidationContext.class));
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath(" pattern "),
                "Whitespace around keyword should cause mismatch.");
    }

    @Test
    @DisplayName("Validator list contains non‑JsonValidator element → throws ClassCastException")
    void throwsClassCastExceptionWhenNonJsonValidatorInList() {
        // Use a raw List to inject a non‑JsonValidator element (e.g., a String).
        @SuppressWarnings("unchecked")
        List rawList = new ArrayList();
        rawList.add("I am not a JsonValidator");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(rawList);
        when(parent.getEvaluationParentSchema()).thenReturn(null);

        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertThrows(ClassCastException.class,
                () -> validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"),
                "Non‑JsonValidator element should cause ClassCastException.");
    }
}
```