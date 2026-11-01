```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        when(path.getElement(-1)).thenReturn("properties"); // triggers early exit

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
}
```