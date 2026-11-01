
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} used for testing.
     */
    static class TestValidator extends BaseJsonValidator {

        /**
         * The schema that is considered the evaluation‑parent of this validator.
         */
        private final JsonSchema evaluationParentSchema;

        TestValidator(JsonSchema evaluationParentSchema, ValidationContext validationContext) {
            // All constructor arguments that are not used by the method under test are passed as null/false.
            super(null, null, null, null, null, null, validationContext, false);
            this.evaluationParentSchema = evaluationParentSchema;
        }

        /**
         * Expose the evaluation‑parent schema so the method can traverse it.
         */
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

    @Test
    void returnsTrueWhenKeywordFoundInImmediateParent() {
        // Mock a validator whose keyword matches the searched one.
        JsonValidator matchingValidator = mock(JsonValidator.class);
        when(matchingValidator.getKeyword()).thenReturn("unevaluatedItems");
        // Mock the parent schema to return the matching validator.
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.getValidators()).thenReturn(List.of(matchingValidator));
        // end of chain
        when(parentSchema.getEvaluationParentSchema()).thenReturn(null);
        // Create the validator under test with the mocked parent schema.
        TestValidator validator = new TestValidator(parentSchema, mock(ValidationContext.class));
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedItems"), "Keyword should be found in the immediate evaluation‑parent schema");
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
        // top of chain
        when(ancestorSchema.getEvaluationParentSchema()).thenReturn(null);
        // Wire the chain: leaf → ancestor.
        when(leafSchema.getEvaluationParentSchema()).thenReturn(ancestorSchema);
        TestValidator validator = new TestValidator(leafSchema, mock(ValidationContext.class));
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("unevaluatedProperties"), "Keyword should be found in an ancestor evaluation‑parent schema");
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
        // end of chain
        when(parentSchema.getEvaluationParentSchema()).thenReturn(null);
        // Chain leaf → parent.
        when(leafSchema.getEvaluationParentSchema()).thenReturn(parentSchema);
        TestValidator validator = new TestValidator(leafSchema, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("nonExistingKeyword"), "Method should return false when the keyword is absent in the whole chain");
    }

    @Test
    void returnsFalseWhenNoEvaluationParentSchema() {
        // The validator has no evaluation‑parent schema at all.
        TestValidator validator = new TestValidator(null, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Method should return false when there is no evaluation‑parent schema");
    }

    @Test
    @DisplayName("Parent schema without validators → returns false")
    void returnsFalseWhenParentSchemaHasNoValidators() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Empty validator list should lead to false.");
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
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("exclusiveMaximum"), "Method should stop after finding the first matching validator.");
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
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("required"), "Null keyword from a validator must be ignored and result in false.");
    }

    @Test
    @DisplayName("Null input keyword → throws NullPointerException")
    void throwsNPEWhenKeywordIsNull() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath(null), "Passing null keyword should result in NullPointerException.");
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
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("minimum"), "Keyword absent in the whole chain should yield false.");
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
        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("type"), "Method should ignore validators belonging to the current schema.");
    }

    @Test
    @DisplayName("Validator list contains null element → throws NullPointerException")
    void throwsNPEWhenValidatorListContainsNull() {
        JsonSchema parent = mock(JsonSchema.class);
        // List with a single null entry
        when(parent.getValidators()).thenReturn(Collections.singletonList(null));
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("maxItems"), "Null element in validators list should cause NPE.");
    }

    @Test
    @DisplayName("Schema validators collection is null → throws NullPointerException")
    void throwsNPEWhenValidatorsCollectionIsNull() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(null);
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
        assertThrows(NullPointerException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("minLength"), "Null validators collection should cause NPE.");
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
        assertTrue(validator.hasAdjacentKeywordInEvaluationPath(""), "Empty string keyword should match validator returning empty string.");
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
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath("MAXLENGTH"), "Keyword comparison is case‑sensitive; mismatch should return false.");
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
        assertFalse(testValidator.hasAdjacentKeywordInEvaluationPath(" pattern "), "Whitespace around keyword should cause mismatch.");
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
        assertThrows(ClassCastException.class, () -> validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"), "Non‑JsonValidator element should cause ClassCastException.");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:6: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:7: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:9: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:19: error: TestValidator is not abstract and does not override abstract method validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath) in JsonValidator
    static class TestValidator extends BaseJsonValidator {
           ^
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:68: error: cannot find symbol
        JsonValidator matchingValidator = mock(JsonValidator.class);
                                          ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:69: error: cannot find symbol
        when(matchingValidator.getKeyword()).thenReturn("unevaluatedItems");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:71: error: cannot find symbol
        JsonSchema parentSchema = mock(JsonSchema.class);
                                  ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:72: error: cannot find symbol
        when(parentSchema.getValidators()).thenReturn(List.of(matchingValidator));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:74: error: cannot find symbol
        when(parentSchema.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:76: error: cannot find symbol
        TestValidator validator = new TestValidator(parentSchema, mock(ValidationContext.class));
                                                                  ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:83: error: cannot find symbol
        JsonValidator otherValidator = mock(JsonValidator.class);
                                       ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:84: error: cannot find symbol
        when(otherValidator.getKeyword()).thenReturn("someOtherKeyword");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:85: error: cannot find symbol
        JsonSchema leafSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:86: error: cannot find symbol
        when(leafSchema.getValidators()).thenReturn(List.of(otherValidator));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:88: error: cannot find symbol
        JsonValidator matchingValidator = mock(JsonValidator.class);
                                          ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:89: error: cannot find symbol
        when(matchingValidator.getKeyword()).thenReturn("unevaluatedProperties");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:90: error: cannot find symbol
        JsonSchema ancestorSchema = mock(JsonSchema.class);
                                    ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:91: error: cannot find symbol
        when(ancestorSchema.getValidators()).thenReturn(List.of(matchingValidator));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:93: error: cannot find symbol
        when(ancestorSchema.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:95: error: cannot find symbol
        when(leafSchema.getEvaluationParentSchema()).thenReturn(ancestorSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:96: error: cannot find symbol
        TestValidator validator = new TestValidator(leafSchema, mock(ValidationContext.class));
                                                                ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:103: error: cannot find symbol
        JsonValidator leafValidator = mock(JsonValidator.class);
                                      ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:104: error: cannot find symbol
        when(leafValidator.getKeyword()).thenReturn("leafKeyword");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:105: error: cannot find symbol
        JsonSchema leafSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:106: error: cannot find symbol
        when(leafSchema.getValidators()).thenReturn(List.of(leafValidator));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:108: error: cannot find symbol
        JsonValidator parentValidator = mock(JsonValidator.class);
                                        ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:109: error: cannot find symbol
        when(parentValidator.getKeyword()).thenReturn("parentKeyword");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:110: error: cannot find symbol
        JsonSchema parentSchema = mock(JsonSchema.class);
                                  ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:111: error: cannot find symbol
        when(parentSchema.getValidators()).thenReturn(List.of(parentValidator));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:113: error: cannot find symbol
        when(parentSchema.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:115: error: cannot find symbol
        when(leafSchema.getEvaluationParentSchema()).thenReturn(parentSchema);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:116: error: cannot find symbol
        TestValidator validator = new TestValidator(leafSchema, mock(ValidationContext.class));
                                                                ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:123: error: cannot find symbol
        TestValidator validator = new TestValidator(null, mock(ValidationContext.class));
                                                          ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:130: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:131: error: cannot find symbol
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:132: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:133: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:141: error: cannot find symbol
        JsonValidator matching = mock(JsonValidator.class);
                                 ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:142: error: cannot find symbol
        when(matching.getKeyword()).thenReturn("exclusiveMaximum");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:144: error: cannot find symbol
        JsonValidator shouldNotBeVisited = mock(JsonValidator.class);
                                           ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:145: error: cannot find symbol
        when(shouldNotBeVisited.getKeyword()).thenThrow(new AssertionError("Should not be visited"));
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:146: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:147: error: cannot find symbol
        when(parent.getValidators()).thenReturn(List.of(matching, shouldNotBeVisited));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:148: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:149: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:156: error: cannot find symbol
        JsonValidator nullKeywordValidator = mock(JsonValidator.class);
                                             ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:157: error: cannot find symbol
        when(nullKeywordValidator.getKeyword()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:158: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:159: error: cannot find symbol
        when(parent.getValidators()).thenReturn(List.of(nullKeywordValidator));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:160: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:161: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:168: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:169: error: cannot find symbol
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:170: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:171: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:179: error: cannot find symbol
        JsonValidator leafV = mock(JsonValidator.class);
                              ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:180: error: cannot find symbol
        when(leafV.getKeyword()).thenReturn("leafK");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:181: error: cannot find symbol
        JsonSchema leaf = mock(JsonSchema.class);
                          ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:182: error: cannot find symbol
        when(leaf.getValidators()).thenReturn(List.of(leafV));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:184: error: cannot find symbol
        JsonValidator middleV = mock(JsonValidator.class);
                                ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:185: error: cannot find symbol
        when(middleV.getKeyword()).thenReturn("middleK");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:186: error: cannot find symbol
        JsonSchema middle = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:187: error: cannot find symbol
        when(middle.getValidators()).thenReturn(List.of(middleV));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:189: error: cannot find symbol
        JsonValidator topV = mock(JsonValidator.class);
                             ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:190: error: cannot find symbol
        when(topV.getKeyword()).thenReturn("topK");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:191: error: cannot find symbol
        JsonSchema top = mock(JsonSchema.class);
                         ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:192: error: cannot find symbol
        when(top.getValidators()).thenReturn(List.of(topV));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:193: error: cannot find symbol
        when(top.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:195: error: cannot find symbol
        when(leaf.getEvaluationParentSchema()).thenReturn(middle);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:196: error: cannot find symbol
        when(middle.getEvaluationParentSchema()).thenReturn(top);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:197: error: cannot find symbol
        TestValidator validator = new TestValidator(leaf, mock(ValidationContext.class));
                                                          ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:205: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:206: error: cannot find symbol
        when(parent.getValidators()).thenReturn(Collections.emptyList());
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:207: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:210: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:217: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:219: error: cannot find symbol
        when(parent.getValidators()).thenReturn(Collections.singletonList(null));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:220: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:221: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:228: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:229: error: cannot find symbol
        when(parent.getValidators()).thenReturn(null);
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:230: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:231: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:238: error: cannot find symbol
        JsonValidator emptyKeywordValidator = mock(JsonValidator.class);
                                              ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:239: error: cannot find symbol
        when(emptyKeywordValidator.getKeyword()).thenReturn("");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:240: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:241: error: cannot find symbol
        when(parent.getValidators()).thenReturn(List.of(emptyKeywordValidator));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:242: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:243: error: cannot find symbol
        TestValidator validator = new TestValidator(parent, mock(ValidationContext.class));
                                                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:250: error: cannot find symbol
        JsonValidator validatorWithKeyword = mock(JsonValidator.class);
                                             ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:251: error: cannot find symbol
        when(validatorWithKeyword.getKeyword()).thenReturn("maxLength");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:252: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:253: error: cannot find symbol
        when(parent.getValidators()).thenReturn(List.of(validatorWithKeyword));
        ^
  symbol:   method when(List<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:254: error: cannot find symbol
        when(parent.getEvaluationParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:255: error: cannot find symbol
        TestValidator testValidator = new TestValidator(parent, mock(ValidationContext.class));
                                                                ^
  symbol:   method mock(Class<ValidationContext>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:262: error: cannot find symbol
        JsonValidator validatorWithKeyword = mock(JsonValidator.class);
                                             ^
  symbol:   method mock(Class<JsonValidator>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:263: error: cannot find symbol
        when(validatorWithKeyword.getKeyword()).thenReturn("pattern");
        ^
  symbol:   method when(String)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java:264: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test
Note: src/test/java/com/networknt/schema/BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
100 errors
only showing the first 100 errors, of 107 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method hasAdjacentKeywordInEvaluationPath(String)': '\n     * Determines if the keyword exists adjacent in the evaluation path.\n     * <p>\n     * This does not check if the keyword exists in the current meta schema as this\n     * can be a cross-draft case where the properties keyword is in a Draft 7 schema\n     * and the unevaluatedProperties keyword is in an outer Draft 2020-12 schema.\n     * <p>\n     * The fact that the validator exists in the evaluation path implies that the\n     * keyword was valid in whatever meta schema for that schema it was created for.\n     * \n     * @param keyword the keyword to check\n     * @return true if found\n     ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected boolean canShortCircuit()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;\nclass `com.networknt.schema.utils.StringChecker`:\n\trelated functions:\n\t\tmethod `public static boolean isBoolean(String)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;\nclass `com.networknt.org.apache.commons.validator.routines.DomainValidator.IDNBUGHOLDER`:\n\trelated functions:\n\t\tmethod `private static boolean keepsTrailingDot()` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.hasAdjacentKeywordInEvaluationPath(String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


