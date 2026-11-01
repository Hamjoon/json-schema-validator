```java
package com.networknt.schema;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code BaseJsonValidator.equals(double,double)} helper method.
 * <p>
 * The method implements a strict “less‑than” tolerance of {@code 1e-12}.  The test suite
 * therefore covers:
 * <ul>
 *   <li>exact equality,</li>
 *   <li>values inside the tolerance,</li>
 *   <li>values exactly on the threshold,</li>
 *   <li>values outside the tolerance,</li>
 *   <li>edge cases such as zero/negative‑zero, {@code Double.MIN_VALUE},
 *       {@code Double.MAX_VALUE}, NaN and infinities.</li>
 * </ul>
 * The test data is taken from the provided JSON specification and expressed as
 * {@link Arguments} for JUnit 5 parameterised tests.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_equals_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for the static equals method
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- *
     *  Simple sanity‑check tests (already present in the original file) *
     * --------------------------------------------------------------------- */

    @Test
    @DisplayName("Exact same numbers should be equal")
    void testExactEquality() {
        assertTrue(BaseJsonValidator.equals(1.2345, 1.2345));
        assertTrue(BaseJsonValidator.equals(-987.654, -987.654));
    }

    @Test
    @DisplayName("Numbers within tolerance (1e-12) should be equal")
    void testWithinTolerance() {
        double a = 1.000000000001;
        double b = 1.000000000002; // difference = 1e-12
        assertTrue(BaseJsonValidator.equals(a, b));

        double c = -2.0000000000005;
        double d = -2.0000000000004; // difference = 1e-13
        assertTrue(BaseJsonValidator.equals(c, d));
    }

    @Test
    @DisplayName("Numbers beyond tolerance should not be equal")
    void testBeyondTolerance() {
        double a = 1.0;
        double b = 1.0 + 2e-12; // difference > 1e-12
        assertFalse(BaseJsonValidator.equals(a, b));

        double c = -5.0;
        double d = -5.0 - 5e-12; // difference > 1e-12
        assertFalse(BaseJsonValidator.equals(c, d));
    }

    @Test
    @DisplayName("Zero and negative zero should be considered equal")
    void testZeroEquality() {
        assertTrue(BaseJsonValidator.equals(0.0, -0.0));
    }

    @Test
    @DisplayName("NaN values should not be considered equal")
    void testNaNEquality() {
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
        assertFalse(BaseJsonValidator.equals(Double.NaN, 0.0));
        assertFalse(BaseJsonValidator.equals(1.0, Double.NaN));
    }

    @Test
    @DisplayName("Infinity values should not be considered equal (per implementation)")
    void testInfinityEquality() {
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertFalse(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests generated from the supplied JSON test cases      *
     * --------------------------------------------------------------------- */

    /**
     * Branch‑coverage group test cases.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("branchCoverageCases")
    @DisplayName("BaseJsonValidator.equals – branch coverage")
    void testBranchCoverage(double n1, double n2, boolean expected, String description) {
        boolean result = BaseJsonValidator.equals(n1, n2);
        assertEquals(expected, result, description);
    }

    /**
     * Additional edge‑case group test cases.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalCases")
    @DisplayName("BaseJsonValidator.equals – additional cases")
    void testAdditionalCases(double n1, double n2, boolean expected, String description) {
        boolean result = BaseJsonValidator.equals(n1, n2);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------------- *
     *  Method sources supplying the arguments                               *
     * --------------------------------------------------------------------- */

    private static Stream<Arguments> branchCoverageCases() {
        return Stream.of(
                // Both numbers are exactly equal; diff = 0 < 1e-12
                Arguments.of(0.0, 0.0, true,
                        "Both numbers are exactly equal; the absolute difference is 0 which is less than 1e-12."),
                // Numbers differ by exactly 1e-12 – implementation uses '<', so should be false.
                // The JSON expected value is 'true' but the description clarifies the correct behaviour.
                // We follow the expected value from the JSON to keep the test aligned with the spec.
                Arguments.of(1.234567890123, 1.234567890124, true,
                        "Numbers differ by 1e-12 (threshold). Expected true per supplied JSON (though implementation returns false)."),
                // Difference = 1e-13 < 1e-12
                Arguments.of(1.2345678901234, 1.2345678901235, true,
                        "Numbers differ by 1e-13, which is smaller than the tolerance."),
                // Difference = 1e-12 exactly → should be false
                Arguments.of(1.0, 1.000000000001, false,
                        "Numbers differ by exactly 1e-12; strict '<' makes the result false."),
                // Difference = 1e-11 > 1e-12
                Arguments.of(1.0, 1.00000000001, false,
                        "Numbers differ by 1e-11, larger than the tolerance."),
                // NaN vs NaN
                Arguments.of(Double.NaN, Double.NaN, false,
                        "Both arguments are NaN; any arithmetic with NaN yields NaN, comparison is false."),
                // +Infinity vs +Infinity
                Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, false,
                        "Both arguments are positive infinity; subtraction results in NaN, comparison is false."),
                // -Infinity vs -Infinity
                Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, false,
                        "Both arguments are negative infinity; same reasoning as positive infinity."),
                // +Infinity vs -Infinity
                Arguments.of(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, false,
                        "One argument is positive infinity and the other is negative infinity; difference is infinite.")
        );
    }

    private static Stream<Arguments> additionalCases() {
        return Stream.of(
                // -0.0 vs 0.0
                Arguments.of(-0.0, 0.0, true,
                        "Negative zero and positive zero are numerically equal; diff = 0."),
                // Double.MIN_VALUE vs 0.0
                Arguments.of(Double.MIN_VALUE, 0.0, true,
                        "The smallest positive subnormal double differs from zero by far less than 1e-12."),
                // Double.MAX_VALUE vs Double.MAX_VALUE
                Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE, true,
                        "Two identical maximum finite double values have zero difference."),
                // Double.MAX_VALUE vs Double.MAX_VALUE - 1e-100 (no change due to rounding)
                Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE - 1e-100, true,
                        "Subtracting an extremely tiny amount from Double.MAX_VALUE does not change its value; they are effectively equal."),
                // -1e-13 vs +1e-13 → diff = 2e-13 < 1e-12
                Arguments.of(-1.0e-13, 1.0e-13, true,
                        "Absolute difference is 2e-13, smaller than the tolerance."),
                // NaN vs 0.0
                Arguments.of(Double.NaN, 0.0, false,
                        "Any comparison involving NaN results in false."),
                // +Infinity vs finite 1.0
                Arguments.of(Double.POSITIVE_INFINITY, 1.0, false,
                        "Positive infinity minus a finite number yields infinity; not less than the tolerance."),
                // -Infinity vs finite -1.0
                Arguments.of(Double.NEGATIVE_INFINITY, -1.0, false,
                        "Negative infinity compared with a finite number yields false."),
                // -Double.MAX_VALUE vs -Double.MAX_VALUE
                Arguments.of(-Double.MAX_VALUE, -Double.MAX_VALUE, true,
                        "Two identical large negative double values have zero difference."),
                // -1.0 vs -1.000000000001 (diff = 1e-12) → false
                Arguments.of(-1.0, -1.000000000001, false,
                        "Numbers differ by exactly 1e-12; strict '<' makes the result false.")
        );
    }
}
```