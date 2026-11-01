```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#hasAdjacentKeywordInEvaluationPath(String)}.
 *
 * <p>The protected method is exercised via a concrete subclass that exposes it.
 * Each test method corresponds to a logical group from the specification JSON.
 * Mockito is used to mock the {@link JsonSchema}, {@link JsonValidator} and
 * {@link JsonNodePath} collaborators.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_hasAdjacentKeywordInEvaluationPath_Test {

    /**
     * Concrete subclass that exposes the protected method and allows us to
     * inject a mocked {@link JsonSchema} as the evaluation‑parent schema.
     */
    private static class TestValidator extends BaseJsonValidator {
        private final JsonSchema evaluationParentSchema;

        TestValidator(JsonSchema evaluationParentSchema) {
            // The super‑constructor arguments are irrelevant for the method under test.
            super(null, null, null, null, null, null, null, null, null, null, null);
            this.evaluationParentSchema = evaluationParentSchema;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return evaluationParentSchema;
        }

        /** expose the protected method for testing */
        boolean callHasAdjacentKeywordInEvaluationPath(String keyword) {
            return super.hasAdjacentKeywordInEvaluationPath(keyword);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /** Helper to create a mock {@link JsonSchema} that returns the supplied values. */
    private JsonSchema mockSchema(List<JsonValidator> validators,
                                 JsonNodePath evaluationPath,
                                 JsonSchema parent) {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getValidators()).thenReturn(validators);
        when(schema.getEvaluationPath()).thenReturn(evaluationPath);
        when(schema.getEvaluationParentSchema()).thenReturn(parent);
        return schema;
    }

    /** Helper to create a mock {@link JsonValidator} that returns the given keyword. */
    private JsonValidator mockValidator(String keyword) {
        JsonValidator validator = mock(JsonValidator.class);
        when(validator.getKeyword()).thenReturn(keyword);
        return validator;
    }

    /* --------------------------------------------------------------------- */
    /*  1. Keyword found in current schema                                    */
    /* --------------------------------------------------------------------- */
    @Test
    void shouldReturnTrueWhenKeywordExistsInCurrentSchema() {
        // arrange – a single validator with the target keyword
        JsonValidator validator = mockValidator("type");
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("foo"); // not "properties"/"items"

        JsonSchema schema = mockSchema(Collections.singletonList(validator), path, null);
        TestValidator testValidator = new TestValidator(schema);

        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("type");

        // assert
        assertTrue(result, "The method should return true when the keyword is present in the current schema");
    }

    /* --------------------------------------------------------------------- */
    /*  2. Keyword not found, evaluation path element is "properties"          */
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
    /*  3. Keyword not found, evaluation path element is "items"              */
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
    /*  4. Keyword not found, evaluation path element is other value,        */
    /*     continue to parent and find match                                   */
    /* --------------------------------------------------------------------- */
    @Test
    void shouldTraverseParentSchemasUntilKeywordFound() {
        // arrange – child schema has no matching keyword, parent does
        JsonValidator childValidator = mockValidator("differentKeyword");
        JsonValidator parentValidator = mockValidator("pattern");

        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("anyOf");
        when(parentPath.getElement(-1)).thenReturn("anyOf");

        JsonSchema parentSchema = mockSchema(Collections.singletonList(parentValidator), parentPath, null);
        JsonSchema childSchema = mockSchema(Collections.singletonList(childValidator), childPath, parentSchema);

        TestValidator testValidator = new TestValidator(childSchema);

        // act
        boolean result = testValidator.callHasAdjacentKeywordInEvaluationPath("pattern");

        // assert
        assertTrue(result, "The method should return true when the keyword is found in a parent schema");
    }

    /* --------------------------------------------------------------------- */
    /*  5. Keyword not found, no parent schema                                 */
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
    /*  6. Keyword null input leads to NullPointerException                    */
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
        assertThrows(NullPointerException.class,
                () -> testValidator.callHasAdjacentKeywordInEvaluationPath(null),
                "Passing a null keyword should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /*  7. Keyword found in grandparent schema after traversing multiple ancestors */
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
    /*  8. Keyword found in ancestor but early "properties" element stops search */
    /* --------------------------------------------------------------------- */
    @Test
    void shouldStopSearchWhenEncounteringPropertiesInParent() {
        // arrange – child has no match, parent evaluation path is "properties",
        // grand‑parent (which actually contains the keyword) must not be inspected
        JsonValidator childValidator = mockValidator("foo");
        JsonValidator parentValidator = mockValidator("maxItems"); // not the searched keyword
        JsonValidator grandParentValidator = mockValidator("maxItems"); // would match if reached

        JsonNodePath childPath = mock(JsonNodePath.class);
        JsonNodePath parentPath = mock(JsonNodePath.class);
        JsonNodePath grandParentPath = mock(JsonNodePath.class);
        when(childPath.getElement(-1)).thenReturn("anyOf");
        when(parentPath.getElement(-1)).thenReturn("properties"); // early stop
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
    /*  9. Keyword not found, multiple ancestors, no matching keyword          */
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
    /* 10. Current schema has no validators, keyword found in parent           */
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
    /* 11. Evaluation path element is unrelated (e.g., "additionalProperties") */
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
    /* 12. Validators collection is null leads to NullPointerException        */
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
        assertThrows(NullPointerException.class,
                () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"),
                "A null validators collection should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 13. Validator in collection is null leads to NullPointerException       */
    /* --------------------------------------------------------------------- */
    @Test
    void shouldThrowNPEWhenValidatorInCollectionIsNull() {
        // arrange – collection contains a null element
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.getElement(-1)).thenReturn("foo");
        List<JsonValidator> validators = new ArrayList<>();
        validators.add(null); // the problematic element

        JsonSchema schema = mockSchema(validators, path, null);
        TestValidator testValidator = new TestValidator(schema);

        // act & assert
        assertThrows(NullPointerException.class,
                () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"),
                "A null validator inside the collection should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 14. EvaluationPath is null leads to NullPointerException                */
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
        assertThrows(NullPointerException.class,
                () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"),
                "A null evaluation path should cause a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 15. Empty EvaluationPath causes IndexOutOfBoundsException                */
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
        assertThrows(IndexOutOfBoundsException.class,
                () -> testValidator.callHasAdjacentKeywordInEvaluationPath("type"),
                "An empty evaluation path should cause an IndexOutOfBoundsException");
    }
}
```