```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Stream;

/**
 * Comprehensive tests for {@link BaseJsonValidator#toString()}.
 *
 * The method simply delegates to {@code getEvaluationPath().getName(-1)}.
 * Therefore each test configures the {@link JsonNodePath} mock (or the
 * {@code getEvaluationPath()} call itself) to return a specific value or to
 * throw an exception, and then verifies that {@code toString()} behaves exactly
 * the same – returning the value or propagating the exception.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    // -----------------------------------------------------------------------
    // Mocks for the constructor parameters of BaseJsonValidator
    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation schemaLocation;

    @Mock
    private JsonNodePath evaluationPath;   // will be stubbed per test case

    @Mock
    private JsonNode schemaNode;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidatorTypeCode validatorType;

    @Mock
    private ValidationContext validationContext;

    // -----------------------------------------------------------------------
    // The object under test – a real BaseJsonValidator instance that calls real
    // methods (including toString) while its constructor arguments are mocked.
    // -----------------------------------------------------------------------
    private BaseJsonValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // By default, return a harmless string; individual test cases will
        // override this behavior.
        when(evaluationPath.getName(-1)).thenReturn("default");

        // Create a partial mock that calls real methods (including toString)
        // while using the mocked constructor arguments.
        validator = mock(BaseJsonValidator.class,
                withSettings()
                        .useConstructor(schemaLocation, evaluationPath, schemaNode,
                                parentSchema, validatorType, validationContext)
                        .defaultAnswer(CALLS_REAL_METHODS));
    }

    // -----------------------------------------------------------------------
    // Parameterized test covering all scenarios described in the JSON spec.
    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideToStringCases")
    void toString_variousScenarios(String description,
                                   Setup stubber,
                                   String expectedValue,
                                   Class<? extends Throwable> expectedException) {
        // Apply the case‑specific stubbing.
        stubber.apply(validator, evaluationPath);

        if (expectedException != null) {
            // Verify that the exact exception type is propagated.
            Throwable thrown = assertThrows(expectedException, validator::toString,
                    () -> "Expected exception for case: " + description);
            // Optional: ensure the message is not null (helps debugging).
            assertNotNull(thrown.getMessage(),
                    "Exception thrown by toString() should have a message");
        } else {
            // No exception expected – verify the returned string (including null).
            String result = validator.toString();
            assertEquals(expectedValue, result,
                    () -> "Incorrect result for case: " + description);
        }
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
        return Stream.of(
                // ----- normal string cases -------------------------------------------------
                Arguments.of("single element \"root\"",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn("root"),
                        "root", null),

                Arguments.of("multiple elements, last is \"grandchild\"",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn("grandchild"),
                        "grandchild", null),

                Arguments.of("empty path – IndexOutOfBoundsException",
                        (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new IndexOutOfBoundsException()),
                        null, IndexOutOfBoundsException.class),

                Arguments.of("last element explicitly null",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn(null),
                        null, null),

                // ----- additional edge string cases -----------------------------------------
                Arguments.of("single empty string element",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn(""),
                        "", null),

                Arguments.of("whitespace only string",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn("   "),
                        "   ", null),

                Arguments.of("numeric string \"12345\"",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn("12345"),
                        "12345", null),

                Arguments.of("special characters \"node@#%&*\"",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn("node@#%&*"),
                        "node@#%&*", null),

                Arguments.of("getEvaluationPath() returns null – NullPointerException",
                        (Setup) (v, p) -> {
                            // Stub the validator itself to return null for the path.
                            when(v.getEvaluationPath()).thenReturn(null);
                        },
                        null, NullPointerException.class),

                Arguments.of("IllegalStateException from JsonNodePath",
                        (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new IllegalStateException()),
                        null, IllegalStateException.class),

                Arguments.of("large path (1000 elements) – last is \"leaf1000\"",
                        (Setup) (v, p) -> when(p.getName(-1)).thenReturn("leaf1000"),
                        "leaf1000", null),

                // ----- exception edge cases -------------------------------------------------
                Arguments.of("IllegalArgumentException from JsonNodePath",
                        (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new IllegalArgumentException()),
                        null, IllegalArgumentException.class),

                Arguments.of("UnsupportedOperationException from JsonNodePath",
                        (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new UnsupportedOperationException()),
                        null, UnsupportedOperationException.class),

                Arguments.of("generic RuntimeException from JsonNodePath",
                        (Setup) (v, p) -> when(p.getName(-1)).thenThrow(new RuntimeException("unexpected")),
                        null, RuntimeException.class)
        );
    }

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
}
```