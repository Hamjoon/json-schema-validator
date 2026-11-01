```java
package com.networknt.schema;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;

/**
 * Simple unit test for {@link BaseJsonValidator#toString()}.
 *
 * <p>This test avoids external mocking frameworks and instead provides a minimal
 * concrete subclass of {@link BaseJsonValidator} together with a tiny stub of
 * {@link JsonNodePath}. The stub returns a predefined name for the last path
 * segment, allowing us to verify that {@code BaseJsonValidator.toString()} returns
 * exactly that name.</p>
 */
class BaseJsonValidator_toString_Test {

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} required for
     * instantiation. All abstract validation methods are implemented with empty
     * behaviour because they are irrelevant for the {@code toString()} test.
     */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(JsonNodePath evaluationPath) {
            // The remaining constructor arguments are not needed for this test,
            // so we pass {@code null}.
            super(null, evaluationPath, null, null, null, null);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                JsonNodePath at) {
            // No validation logic required for the toString test.
            return Collections.emptySet();
        }
    }

    /**
     * Stub implementation of {@link JsonNodePath} that always returns the same
     * name for the last segment (index {@code -1} as used by {@code BaseJsonValidator}).
     */
    private static class StubJsonNodePath extends JsonNodePath {
        private final String lastSegmentName;

        StubJsonNodePath(String lastSegmentName) {
            this.lastSegmentName = lastSegmentName;
        }

        @Override
        public String getName(int index) {
            // The validator asks for the name of the last segment using index -1.
            // We simply ignore the index and return the configured name.
            return lastSegmentName;
        }
    }

    @Test
    void toString_returnsLastEvaluationPathName() {
        // Arrange: create a stub path that will return the expected segment name.
        String expected = "expectedPathSegment";
        JsonNodePath stubPath = new StubJsonNodePath(expected);

        // Act: create the validator (partial concrete subclass) and call toString().
        BaseJsonValidator validator = new TestValidator(stubPath);
        String result = validator.toString();

        // Assert: the result should be the name of the last path segment.
        assertEquals(expected, result,
                "BaseJsonValidator.toString() should return the name of the last segment of the evaluation path");
    }
}
```