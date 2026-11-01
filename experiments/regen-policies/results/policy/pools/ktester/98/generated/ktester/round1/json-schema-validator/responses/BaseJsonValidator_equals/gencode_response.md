```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#equals(double, double)}.
 *
 * <p>This method determines equality of two {@code double} values using a
 * tolerance of {@code 1.0E-12}. The test suite covers:</p>
 *
 * <ul>
 *   <li>Exact equality (including {@code -0.0} vs {@code 0.0}).</li>
 *   <li>Values whose absolute difference is just below the tolerance.</li>
 *   <li>Values whose absolute difference is exactly or just above the tolerance.</li>
 *   <li>Special floating‑point values such as {@code NaN} and infinities.</li>
 *   <li>Edge cases with sub‑normal numbers and the maximum double values.</li>
 * </ul>
 *
 * <p>All tests are written as parameterised tests so that each case is
 * self‑documenting via the {@code description} field from the supplied JSON.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_equals_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these static utility tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* -------------------------------------------------------------
     *  True‑outcome test cases (method returns {@code true})
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("trueOutcomeProvider")
    void testEqualsTrue(String description, double n1, double n2) {
        assertTrue(BaseJsonValidator.equals(n1, n2),
                () -> "Expected true for: " + description + " (n1=" + n1 + ", n2=" + n2 + ")");
    }

    private static Stream<Arguments> trueOutcomeProvider() {
        return Stream.of(
                // Exact zero
                Arguments.of("Both numbers are exactly zero; difference is 0", 0.0, 0.0),

                // Negative zero vs positive zero – should be considered equal
                Arguments.of("Negative zero and positive zero are equal", -0.0, 0.0),

                // Identical positive double
                Arguments.of("Two identical positive doubles should be equal", 123.456, 123.456),

                // Identical negative double
                Arguments.of("Two identical negative doubles should be equal", -987.654, -987.654),

                // Difference just below the threshold (5 × 10⁻¹³)
                Arguments.of("Difference 5e-13 < 1e-12 (1.000000000001 vs 1.0000000000015)", 1.000000000001, 1.0000000000015),

                // Same as above but reversed order
                Arguments.of("Difference 5e-13 < 1e-12 (reversed order)", 1.0000000000015, 1.000000000001),

                // Very small sub‑normal number vs zero
                Arguments.of("Zero compared with the smallest positive subnormal double (5e-324)", 0.0, 5e-324),

                // Tiny opposite‑sign numbers
                Arguments.of("Opposite‑sign tiny numbers (1e-13 vs -1e-13)", 1e-13, -1e-13),

                // Max double values
                Arguments.of("Both arguments are Double.MAX_VALUE", Double.MAX_VALUE, Double.MAX_VALUE),

                // Min (most negative) double values
                Arguments.of("Both arguments are -Double.MAX_VALUE", -Double.MAX_VALUE, -Double.MAX_VALUE),

                // Simple identical value not covered elsewhere
                Arguments.of("Simple identical positive value 42.42", 42.42, 42.42)
        );
    }

    /* -------------------------------------------------------------
     *  False‑outcome test cases (method returns {@code false})
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("falseOutcomeProvider")
    void testEqualsFalse(String description, double n1, double n2) {
        assertFalse(BaseJsonValidator.equals(n1, n2),
                () -> "Expected false for: " + description + " (n1=" + n1 + ", n2=" + n2 + ")");
    }

    private static Stream<Arguments> falseOutcomeProvider() {
        return Stream.of(
                // Difference exactly equal to the tolerance – should be false because the method uses '<'
                Arguments.of("Difference exactly 1.0E-12 (0.0 vs 1e-12)", 0.0, 1e-12),

                // Difference just above the tolerance
                Arguments.of("Difference 2e-12 > 1e-12 (1.0 vs 1.0+2e-12)", 1.0, 1.0 + 2e-12),

                // NaN comparisons – always false
                Arguments.of("NaN vs NaN", Double.NaN, Double.NaN),
                Arguments.of("NaN vs 0.0", Double.NaN, 0.0),
                Arguments.of("0.0 vs NaN", 0.0, Double.NaN),

                // Infinity comparisons
                Arguments.of("Positive infinity vs negative infinity", Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                Arguments.of("Positive infinity vs finite number", Double.POSITIVE_INFINITY, 1e308),
                Arguments.of("Finite negative number vs negative infinity", -1e308, Double.NEGATIVE_INFINITY)
        );
    }

    /* -------------------------------------------------------------
     *  Additional explicit tests for readability (non‑parameterised)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("Exact equality should return true (redundant sanity check)")
    void testExactEqualitySanity() {
        assertTrue(BaseJsonValidator.equals(0.0, 0.0));
        assertTrue(BaseJsonValidator.equals(123.456, 123.456));
        assertTrue(BaseJsonValidator.equals(-987.654, -987.654));
    }

    @Test
    @DisplayName("Infinity comparisons – sanity checks")
    void testInfinitySanity() {
        assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertTrue(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    }
}
```