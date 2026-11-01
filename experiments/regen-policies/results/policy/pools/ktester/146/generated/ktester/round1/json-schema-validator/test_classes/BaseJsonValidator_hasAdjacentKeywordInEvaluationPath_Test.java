package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /**
     * Concrete subclass that exposes the protected method and allows us to
     * inject a mocked {@link JsonSchema} as the evaluation‑parent schema.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final JsonSchema evaluationParentSchema;

        TestValidator(JsonSchema evaluationParentSchema) {
            // Use the most permissive public constructor – the arguments are not
            // used by the method under test, so we can safely pass {@code null}.
            super(null, null, null, null, null, null, null, null, null, null, null);
            this.evaluationParentSchema = evaluationParentSchema;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return evaluationParentSchema;
        }

        /**
         * expose the protected method for testing
         */
        boolean callHasAdjacentKeywordInEvaluationPath(String keyword) {
            return super.hasAdjacentKeywordInEvaluationPath(keyword);
        }
    }

    /**
     * Helper to create a mock {@link JsonSchema} that returns the supplied values.
     */
    private JsonSchema mockSchema(List<JsonValidator> validators, JsonNodePath evaluationPath, JsonSchema parent) {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getValidators()).thenReturn(validators);
        when(schema.getEvaluationPath()).thenReturn(evaluationPath);
        when(schema.getEvaluationParentSchema()).thenReturn(parent);
        return schema;
    }

    /**
     * Helper to create a mock {@link JsonValidator} that returns the given keyword.
     */
    private JsonValidator mockValidator(String keyword) {
        JsonValidator validator = mock(JsonValidator.class);
        when(validator.getKeyword()).thenReturn(keyword);
        return validator;
    }

    @Test
    void shouldReturnTrueWhenKeywordExistsInCurrentSchema() {
        // arrange – a single validator with the target keyword
        JsonValidator validator = mockValidator("unevaluatedItems");
        JsonNodePath path = mock(JsonNodePath.class);
        // element -1 is something other than "properties"/"items"
        when(path.getElement(-1)).thenReturn("foo");
        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("unevaluatedItems");
        // assert
        assertTrue(result, "The method should return true when the keyword is present");
    }

    @Test
    void shouldReturnFalseWhenKeywordNotPresentInAnySchema() {
        // arrange – validator with a different keyword
        JsonValidator validator = mockValidator("someOtherKeyword");
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("foo");
        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("unevaluatedItems");
        // assert
        assertFalse(result, "The method should return false when the keyword is absent");
    }

    @Test
    void shouldReturnFalseWhenEncounteringPropertiesOrItemsElement() {
        // arrange – the evaluation path reports "properties"
        JsonValidator validator = mockValidator("unevaluatedItems");
        JsonNodePath path = mock(JsonNodePath.class);
        // triggers early exit
        when(path.getElement(-1)).thenReturn("properties");
        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("unevaluatedItems");
        // assert
        assertFalse(result, "The method should return false when the evaluation path element is 'properties'");
    }

    @Test
    void shouldTraverseParentSchemasUntilKeywordFound() {
        // arrange – first schema does not contain the keyword, second (parent) does
        JsonValidator childValidator = mockValidator("differentKeyword");
        JsonValidator parentValidator = mockValidator("unevaluatedProperties");
        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("foo");
        when(parentPath.getElement(-1)).thenReturn("foo");
        JsonSchema parentSchema = mockSchema(Collections.singletonList(parentValidator), parentPath, null);
        JsonSchema childSchema = mockSchema(Collections.singletonList(childValidator), childPath, parentSchema);
        TestValidator testValidator = new TestValidator(childSchema);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("unevaluatedProperties");
        // assert
        assertTrue(result, "The method should return true when the keyword is found in a parent schema");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldReturnFalseWhenEvaluationPathElementIsProperties() {
        // arrange – evaluation path reports "properties"
        JsonValidator validator = mockValidator("minimum");
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("properties");
        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("minimum");
        // assert
        assertFalse(result, "The method should return false when the evaluation path element is 'properties'");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldReturnFalseWhenEvaluationPathElementIsItems() {
        // arrange – evaluation path reports "items"
        JsonValidator validator = mockValidator("maxLength");
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("items");
        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("maxLength");
        // assert
        assertFalse(result, "The method should return false when the evaluation path element is 'items'");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldReturnFalseWhenNoParentAndKeywordAbsent() {
        // arrange – single schema without the keyword and no parent
        JsonValidator validator = mockValidator("someOtherKeyword");
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("foo");
        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("exclusiveMaximum");
        // assert
        assertFalse(result, "The method should return false when no ancestor contains the keyword");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldThrowNPEWhenKeywordIsNull() {
        // arrange – any schema; the NPE occurs before schema inspection
        JsonValidator validator = mockValidator("type");
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("foo");
        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act & assert
        assertThrows(NullPointerException.class, () -> testValidator.callHasAdjacentKeywordInEvaluationPath(null), "Passing a null keyword should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldFindKeywordInGrandparentAfterTwoLevels() {
        // arrange – child and parent have no match, grand‑parent does
        JsonValidator childValidator = mockValidator("foo");
        JsonValidator parentValidator = mockValidator("bar");
        JsonValidator grandParentValidator = mockValidator("minimum");
        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        JsonNodePath grandParentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("anyOf");
        when(parentPath.getElement(-1)).thenReturn("oneOf");
        when(grandParentPath.getElement(-1)).thenReturn("anyOf");
        JsonSchema grandParent = mockSchema(Collections.singletonList(grandParentValidator), grandParentPath, null);
        JsonSchema parent = mockSchema(Collections.singletonList(parentValidator), parentPath, grandParent);
        JsonSchema child = mockSchema(Collections.singletonList(childValidator), childPath, parent);
        TestValidator testValidator = new TestValidator(child);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("minimum");
        // assert
        assertTrue(result, "The method should climb two levels and return true when the keyword is in the grand‑parent");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldStopSearchWhenEncounteringPropertiesInParent() {
        // arrange – child has no match, parent evaluation path is "properties",
        // grand‑parent (which actually contains the keyword) must not be inspected
        JsonValidator childValidator = mockValidator("foo");
        // not the searched keyword
        JsonValidator parentValidator = mockValidator("maxItems");
        // would match if reached
        JsonValidator grandParentValidator = mockValidator("maxItems");
        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        JsonNodePath grandParentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("anyOf");
        // early stop
        when(parentPath.getElement(-1)).thenReturn("properties");
        when(grandParentPath.getElement(-1)).thenReturn("anyOf");
        JsonSchema grandParent = mockSchema(Collections.singletonList(grandParentValidator), grandParentPath, null);
        JsonSchema parent = mockSchema(Collections.singletonList(parentValidator), parentPath, grandParent);
        JsonSchema child = mockSchema(Collections.singletonList(childValidator), childPath, parent);
        TestValidator testValidator = new TestValidator(child);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("maxItems");
        // assert
        assertFalse(result, "Search should stop at 'properties' and not reach the grand‑parent where the keyword exists");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldReturnFalseWhenKeywordAbsentInAllAncestors() {
        // arrange – three levels, none contain the keyword, none have stop elements
        JsonValidator childValidator = mockValidator("foo");
        JsonValidator parentValidator = mockValidator("bar");
        JsonValidator grandParentValidator = mockValidator("baz");
        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        JsonNodePath grandParentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("anyOf");
        when(parentPath.getElement(-1)).thenReturn("oneOf");
        when(grandParentPath.getElement(-1)).thenReturn("anyOf");
        JsonSchema grandParent = mockSchema(Collections.singletonList(grandParentValidator), grandParentPath, null);
        JsonSchema parent = mockSchema(Collections.singletonList(parentValidator), parentPath, grandParent);
        JsonSchema child = mockSchema(Collections.singletonList(childValidator), childPath, parent);
        TestValidator testValidator = new TestValidator(child);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("exclusiveMinimum");
        // assert
        assertFalse(result, "The method should exhaust all ancestors and return false when the keyword never appears");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldFindKeywordInParentWhenCurrentHasNoValidators() {
        // arrange – child schema returns empty list, parent contains the keyword
        JsonValidator parentValidator = mockValidator("pattern");
        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("anyOf");
        when(parentPath.getElement(-1)).thenReturn("anyOf");
        JsonSchema parent = mockSchema(Collections.singletonList(parentValidator), parentPath, null);
        JsonSchema child = mockSchema(Collections.emptyList(), childPath, parent);
        TestValidator testValidator = new TestValidator(child);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("pattern");
        // assert
        assertTrue(result, "The method should continue to the parent schema when the current schema has no validators");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldContinueSearchWhenEvaluationPathElementIsUnrelated() {
        // arrange – child path element is "additionalProperties", parent has match
        JsonValidator childValidator = mockValidator("foo");
        JsonValidator parentValidator = mockValidator("format");
        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("additionalProperties");
        when(parentPath.getElement(-1)).thenReturn("anyOf");
        JsonSchema parent = mockSchema(Collections.singletonList(parentValidator), parentPath, null);
        JsonSchema child = mockSchema(Collections.singletonList(childValidator), childPath, parent);
        TestValidator testValidator = new TestValidator(child);
        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("format");
        // assert
        assertTrue(result, "Unrelated evaluation path elements should not stop the search");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldThrowNPEWhenValidatorsCollectionIsNull() {
        // arrange – schema returns null for getValidators()
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("foo");
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getValidators()).thenReturn(null);
        when(schema.getEvaluationPath()).thenReturn(path);
        when(schema.getEvaluationParentSchema()).thenReturn(null);
        TestValidator testValidator = new TestValidator(schema);
        // act & assert
        assertThrows(NullPointerException.class, () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"), "A null validators collection should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldThrowNPEWhenValidatorInCollectionIsNull() {
        // arrange – collection contains a null element
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("foo");
        List<JsonValidator> validators = new ArrayList<>();
        // the problematic element
        validators.add(null);
        JsonSchema schema = mockSchema(validators, path, null);
        TestValidator testValidator = new TestValidator(schema);
        // act & assert
        assertThrows(NullPointerException.class, () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"), "A null validator inside the collection should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldThrowNPEWhenEvaluationPathIsNull() {
        // arrange – getEvaluationPath() returns null
        JsonValidator validator = mockValidator("type");
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getValidators()).thenReturn(Collections.singletonList(validator));
        when(schema.getEvaluationPath()).thenReturn(null);
        when(schema.getEvaluationParentSchema()).thenReturn(null);
        TestValidator testValidator = new TestValidator(schema);
        // act & assert
        assertThrows(NullPointerException.class, () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"), "A null evaluation path should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldThrowIndexOutOfBoundsWhenEvaluationPathIsEmpty() {
        // arrange – mock path throws when getElement(-1) is called
        JsonValidator validator = mockValidator("type");
        JsonNodePath emptyPath = mock(JsonNodePath.class);
        when(emptyPath.getElement(-1)).thenThrow(new IndexOutOfBoundsException("No element at -1"));
        JsonSchema schema = mockSchema(Collections.singletonList(validator), emptyPath, null);
        TestValidator testValidator = new TestValidator(schema);
        // act & assert
        assertThrows(IndexOutOfBoundsException.class, () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"), "An empty evaluation path should cause an IndexOutOfBoundsException");
    }
}
