package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    // -----------------------------------------------------------------------
//     @Mock
    private SchemaLocation schemaLocation;

//     @Mock
    private JsonNodePath evaluationPath;

//     @Mock
    private JsonNode schemaNode;

//     @Mock
    private JsonSchema parentSchema;

//     @Mock
    private ValidatorTypeCode validatorType;

//     @Mock
    private ValidationContext validationContext;

    // -----------------------------------------------------------------------
    private BaseJsonValidator validator;

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
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            // No validation logic required for the toString test.
            return Collections.emptySet();
        }
    }

    /**
     * Stub implementation of {@link JsonNodePath} that can be configured to
     * return a specific name or to throw a supplied exception when {@code getName}
     * is invoked.
     */
    private static class StubJsonNodePath extends JsonNodePath {

        private final String nameToReturn;

        private final RuntimeException exceptionToThrow;

        StubJsonNodePath(String nameToReturn) {
            super(PathType.JSON_PATH);
            this.nameToReturn = nameToReturn;
            this.exceptionToThrow = null;
        }

        StubJsonNodePath(RuntimeException exceptionToThrow) {
            super(PathType.JSON_PATH);
            this.nameToReturn = null;
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public String getName(int index) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return nameToReturn;
        }
    }

    /**
     * Functional interface used to create a {@link BaseJsonValidator} instance
     * that is specific for a particular test case.
     */
    @FunctionalInterface
    private interface Setup {

        BaseJsonValidator create();
    }

//     @BeforeEach
    void setupBeforeEach() {
        // By default, return a harmless string; individual test cases will
        // override this behavior.
//         when(evaluationPath.getName(-1)).thenReturn("default");
        // Create a partial mock that calls real methods (including toString)
        // while using the mocked constructor arguments.
//         validator = mock(BaseJsonValidator.class, withSettings().useConstructor(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext).defaultAnswer(CALLS_REAL_METHODS));
    }

    /**
     * Supplies all test cases.
     */
    private static Stream<Arguments> provideToStringCases() {
        return Stream.of(// normal string cases
        Arguments.of("single element \"root\"", (Setup) () -> new TestValidator(new StubJsonNodePath("root")), "root", null), Arguments.of("multiple elements, last is \"grandchild\"", (Setup) () -> new TestValidator(new StubJsonNodePath("grandchild")), "grandchild", null), // edge / exception cases
        Arguments.of("empty path – IndexOutOfBoundsException", (Setup) () -> new TestValidator(new StubJsonNodePath(new IndexOutOfBoundsException())), null, IndexOutOfBoundsException.class), Arguments.of("last element explicitly null", (Setup) () -> new TestValidator(new StubJsonNodePath((String) null)), null, null), Arguments.of("single empty string element", (Setup) () -> new TestValidator(new StubJsonNodePath("")), "", null), Arguments.of("whitespace only string", (Setup) () -> new TestValidator(new StubJsonNodePath("   ")), "   ", null), Arguments.of("numeric string \"12345\"", (Setup) () -> new TestValidator(new StubJsonNodePath("12345")), "12345", null), Arguments.of("special characters \"node@#%&*\"", (Setup) () -> new TestValidator(new StubJsonNodePath("node@#%&*")), "node@#%&*", null), Arguments.of("getEvaluationPath() returns null – NullPointerException", (Setup) () -> new TestValidator(null), null, NullPointerException.class), Arguments.of("IllegalStateException from JsonNodePath", (Setup) () -> new TestValidator(new StubJsonNodePath(new IllegalStateException())), null, IllegalStateException.class), Arguments.of("large path (1000 elements) – last is \"leaf1000\"", (Setup) () -> new TestValidator(new StubJsonNodePath("leaf1000")), "leaf1000", null), Arguments.of("IllegalArgumentException from JsonNodePath", (Setup) () -> new TestValidator(new StubJsonNodePath(new IllegalArgumentException())), null, IllegalArgumentException.class), Arguments.of("UnsupportedOperationException from JsonNodePath", (Setup) () -> new TestValidator(new StubJsonNodePath(new UnsupportedOperationException())), null, UnsupportedOperationException.class), Arguments.of("generic RuntimeException from JsonNodePath", (Setup) () -> new TestValidator(new StubJsonNodePath(new RuntimeException("unexpected"))), null, RuntimeException.class));
    }

    @Test
    void toString_returnsLastEvaluationPathName() {
        // Arrange: create a stub path that will return the expected segment name.
        String expected = "expectedPathSegment";
        JsonNodePath stubPath = new StubJsonNodePath(expected);
        // Act: create the validator (concrete subclass) and call toString().
        BaseJsonValidator validator = new TestValidator(stubPath);
        String result = validator.toString();
        // Assert: the result should be the name of the last path segment.
        assertEquals(expected, result, "BaseJsonValidator.toString() should return the name of the last segment of the evaluation path");
    }

    // -----------------------------------------------------------------------
//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("provideToStringCases")
//     void toString_variousScenarios(String description, Setup stubber, String expectedValue, Class<? extends Throwable> expectedException) {
//         BaseJsonValidator validator = setup.create();
//         if (expectedException != null) {
//             // Verify that the exact exception type is propagated.
//             Throwable thrown = assertThrows(expectedException, validator::toString, () -> "Expected exception for case: " + description);
//             assertNotNull(thrown.getMessage(), "Exception thrown by toString() should have a message");
//         } else {
//             // No exception expected – verify the returned string (including null).
//             String result = validator.toString();
//             assertEquals(expectedValue, result, () -> "Incorrect result for case: " + description);
//         }
//     }
}