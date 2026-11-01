package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation schemaLocation;

    @Mock
    private JsonNodePath // will be stubbed to return a name
    evaluationPath;

    @Mock
    private JsonNode schemaNode;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidatorTypeCode validatorType;

    @Mock
    private ValidationContext validationContext;

    // -----------------------------------------------------------------------
    private BaseJsonValidator validator;

    /**
     * Functional interface used to apply case‑specific stubbing.
     */
    @FunctionalInterface
    private interface Setup {

        /**
         * Apply mock configuration for a particular test case.
         *
         * @param validator the {@link BaseJsonValidator} mock/partial‑mock under test
         * @param path      the {@link JsonNodePath} mock used by the validator
         */
        void apply(BaseJsonValidator validator, JsonNodePath path);
    }

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

    @BeforeEach
    void setupBeforeEach() {
        // By default, return a harmless string; individual test cases will
        // override this behavior.
        when(evaluationPath.getName(-1)).thenReturn("default");
        // Create a partial mock that calls real methods (including toString)
        // while using the mocked constructor arguments.
        validator = mock(BaseJsonValidator.class, withSettings().useConstructor(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext).defaultAnswer(CALLS_REAL_METHODS));
    }

    /**
     * Supplies all test cases.  Each {@link Arguments} entry contains:
     * <ul>
     *   <li>a description (used as the test name),</li>
     *   <li>a {@link Setup} lambda that configures the mocks,</li>
     *   <li>the expected string result (null if the method should return null),</li>
     *   <li>the expected exception class (null when no exception is expected).</li>
     * </ul>
     */
    private static Stream<Arguments> provideToStringCases() {
        return // ----- normal string cases -------------------------------------------------
        Stream.// ----- normal string cases -------------------------------------------------
        of(// ----- additional edge string cases -----------------------------------------
        Arguments.of("single element \"root\"", (Setup) (v, p) -> when(p.getName(-1)).thenReturn("root"), "root", null), // ----- additional edge string cases -----------------------------------------
        Arguments.of("multiple elements, last is \"grandchild\"", (Setup) (v, p) -> when(p.getName(-1)).thenReturn("grandchild"), "grandchild", null), // ----- additional edge string cases -----------------------------------------
        Arguments.of("empty path – IndexOutOfBoundsException", (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new IndexOutOfBoundsException()), null, IndexOutOfBoundsException.class), // ----- additional edge string cases -----------------------------------------
        Arguments.of("last element explicitly null", (Setup) (v, p) -> when(p.getName(-1)).thenReturn(null), null, null), Arguments.of("single empty string element", (Setup) (v, p) -> when(p.getName(-1)).thenReturn(""), "", null), Arguments.of("whitespace only string", (Setup) (v, p) -> when(p.getName(-1)).thenReturn("   "), "   ", null), Arguments.of("numeric string \"12345\"", (Setup) (v, p) -> when(p.getName(-1)).thenReturn("12345"), "12345", null), Arguments.of("special characters \"node@#%&*\"", (Setup) (v, p) -> when(p.getName(-1)).thenReturn("node@#%&*"), "node@#%&*", null), Arguments.of("getEvaluationPath() returns null – NullPointerException", (Setup) (v, p) -> {
            // Stub the validator itself to return null for the path.
            when(v.getEvaluationPath()).thenReturn(null);
        }, null, // ----- exception edge cases -------------------------------------------------
        NullPointerException.class), // ----- exception edge cases -------------------------------------------------
        Arguments.of("IllegalStateException from JsonNodePath", (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new IllegalStateException()), null, IllegalStateException.class), // ----- exception edge cases -------------------------------------------------
        Arguments.of("large path (1000 elements) – last is \"leaf1000\"", (Setup) (v, p) -> when(p.getName(-1)).thenReturn("leaf1000"), "leaf1000", null), Arguments.of("IllegalArgumentException from JsonNodePath", (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new IllegalArgumentException()), null, IllegalArgumentException.class), Arguments.of("UnsupportedOperationException from JsonNodePath", (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new UnsupportedOperationException()), null, UnsupportedOperationException.class), Arguments.of("generic RuntimeException from JsonNodePath", (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new RuntimeException("unexpected")), null, RuntimeException.class));
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
        assertEquals(expected, result, "BaseJsonValidator.toString() should return the name of the last segment of the evaluation path");
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideToStringCases")
    void toString_variousScenarios(String description, Setup stubber, String expectedValue, Class<? extends Throwable> expectedException) {
        // Apply the case‑specific stubbing.
        stubber.apply(validator, evaluationPath);
        if (expectedException != null) {
            // Verify that the exact exception type is propagated.
            Throwable thrown = assertThrows(expectedException, validator::toString, () -> "Expected exception for case: " + description);
            // Optional: ensure the message is not null (helps debugging).
            assertNotNull(thrown.getMessage(), "Exception thrown by toString() should have a message");
        } else {
            // No exception expected – verify the returned string (including null).
            String result = validator.toString();
            assertEquals(expectedValue, result, () -> "Incorrect result for case: " + description);
        }
    }
}
