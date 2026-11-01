```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link BaseJsonValidator#equals(double, double)} method.
 *
 * <p>The tests are split into two groups that mirror the JSON test case description
 * provided in the task.  Each group is exercised by a {@code @ParameterizedTest}
 * that receives the two double operands, the expected result and a short
 * description.  The description is used only for readability – it appears in the
 * test name when the test is executed.</p>
 *
 * <p>All {@code @BeforeAll}, {@code @BeforeEach}, {@code @AfterEach} and
 * {@code @AfterAll} methods are left untouched as required by the framework.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_equals_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these static tests
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

    /* --------------------------------------------------------------------- */
    /*  Simple explicit tests (kept from the original scaffold)               */
    /* --------------------------------------------------------------------- */

    @Test
    void equals_ShouldReturnTrue_WhenNumbersAreExactlyEqual() {
        assertTrue(BaseJsonValidator.equals(5.0, 5.0));
        assertTrue(BaseJsonValidator.equals(-3.1415, -3.1415));
    }

    @Test
    void equals_ShouldReturnTrue_WhenDifferenceIsWithinTolerance() {
        double a = 1.0;
        double b = 1.0 + 0.5e-12; // difference = 5e-13 < 1e-12
        assertTrue(BaseJsonValidator.equals(a, b));

        double c = -2.0;
        double d = -2.0 - 0.9e-12; // difference = 9e-13 < 1e-12
        assertTrue(BaseJsonValidator.equals(c, d));
    }

    @Test
    void equals_ShouldReturnFalse_WhenDifferenceEqualsTolerance() {
        double a = 1.0;
        double b = 1.0 + 1.0e-12; // difference = 1e-12 (not less)
        assertFalse(BaseJsonValidator.equals(a, b));
    }

    @Test
    void equals_ShouldReturnFalse_WhenDifferenceExceedsTolerance() {
        double a = 1.0;
        double b = 1.0 + 2.0e-12; // difference = 2e-12 > 1e-12
        assertFalse(BaseJsonValidator.equals(a, b));
    }

    @Test
    void equals_ShouldReturnFalse_ForNaNValues() {
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
        assertFalse(BaseJsonValidator.equals(Double.NaN, 0.0));
        assertFalse(BaseJsonValidator.equals(0.0, Double.NaN));
    }

    @Test
    void equals_ShouldReturnFalse_ForInfinityValues() {
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertFalse(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, 1.0));
        assertFalse(BaseJsonValidator.equals(-1.0, Double.NEGATIVE_INFINITY));
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterized tests – group: "equals(double, double)"               */
    /* --------------------------------------------------------------------- */

    /**
     * Executes the core set of test cases described in the first JSON group.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideCoreEqualityCases")
    void equals_CoreCases(double n1, double n2, boolean expected, String description) {
        boolean actual = BaseJsonValidator.equals(n1, n2);
        assertEquals(expected, actual, description);
    }

    private static Stream<Arguments> provideCoreEqualityCases() {
        return Stream.of(
                // Identical numbers
                Arguments.of(5.0, 5.0, true,
                        "Identical numbers should be considered equal (difference = 0)."),

                // Within tolerance (5e-13)
                Arguments.of(1.0, 1.0000000000005, true,
                        "Numbers whose absolute difference (5e-13) is smaller than the tolerance 1e-12 should be equal."),

                // Exactly at tolerance – should be false
                Arguments.of(0.0, 1e-12, false,
                        "Difference exactly equal to the tolerance (1e-12) must evaluate to false because the condition uses '<' not '<='."),

                // Exceeds tolerance – should be false
                Arguments.of(0.0, 1.5e-12, false,
                        "Numbers whose absolute difference (1.5e-12) exceeds the tolerance should not be equal."),

                // Very large magnitude but still within tolerance
                Arguments.of(1e20, 1.0000000000005e20, true,
                        "Even with very large magnitudes, equality is based on absolute difference; here the difference is 5e-13, which is within tolerance."),

                // NaN vs NaN
                Arguments.of(Double.NaN, Double.NaN, false,
                        "Any comparison involving NaN results in false because Math.abs(NaN) is NaN and the '<' comparison yields false."),

                // Positive infinity vs positive infinity
                Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, false,
                        "Positive infinity compared to itself produces NaN after subtraction, so the method returns false."),

                // Positive infinity vs finite
                Arguments.of(Double.POSITIVE_INFINITY, 1.0, false,
                        "Positive infinity vs a finite number yields an infinite difference, which is not less than the tolerance."),

                // Negative infinity vs negative infinity
                Arguments.of(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, false,
                        "Negative infinity compared to itself also results in NaN after subtraction, leading to false.")
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterized tests – group: "equals(double, double) - additional cases" */
    /* --------------------------------------------------------------------- */

    /**
     * Executes the additional edge‑case scenarios from the second JSON group.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideAdditionalEqualityCases")
    void equals_AdditionalCases(double n1, double n2, boolean expected, String description) {
        boolean actual = BaseJsonValidator.equals(n1, n2);
        assertEquals(expected, actual, description);
    }

    private static Stream<Arguments> provideAdditionalEqualityCases() {
        return Stream.of(
                // Positive zero vs negative zero
                Arguments.of(0.0, -0.0, true,
                        "Positive zero and negative zero have a difference of 0.0, which is less than the tolerance, so they are considered equal."),

                // Smallest positive sub‑normal double (Double.MIN_VALUE) vs zero
                Arguments.of(0.0, Double.MIN_VALUE, true,
                        "The smallest positive sub‑normal double differs from zero by ~4.9×10⁻³²⁴, well below the 1e‑12 tolerance."),

                // Just under the tolerance
                Arguments.of(0.0, 9.99999999999e-13, true,
                        "Difference (9.99999999999×10⁻¹³) is just under the tolerance (1e‑12), therefore the numbers are equal."),

                // Just over the tolerance
                Arguments.of(0.0, 1.000000000001e-12, false,
                        "Difference (1.000000000001×10⁻¹²) is just above the tolerance, so the method should return false."),

                // Opposite signs, difference exactly 1e‑12
                Arguments.of(5e-13, -5e-13, false,
                        "Numbers with opposite signs have an absolute difference of exactly 1e‑12 (the tolerance). Because the comparison uses '<' (strictly less), the result is false."),

                // NaN vs zero
                Arguments.of(Double.NaN, 0.0, false,
                        "Any comparison involving NaN yields false; subtraction results in NaN, and the '<' test fails."),

                // NaN vs Infinity
                Arguments.of(Double.NaN, Double.POSITIVE_INFINITY, false,
                        "NaN compared with Infinity also returns false for the same reason as above."),

                // Two identical maximum‑value doubles
                Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE, true,
                        "Two identical maximum‑value doubles have zero difference, which is less than the tolerance."),

                // MAX_VALUE vs a value that cannot be represented (underflows to 0.0)
                Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE - 1e292, true,
                        "Two maximum‑value doubles where the second operand is mathematically different but too small to affect the double representation (e.g., MAX_VALUE - 1e292). The subtraction underflows to 0.0, so the method reports equality.")
        );
    }
}
```