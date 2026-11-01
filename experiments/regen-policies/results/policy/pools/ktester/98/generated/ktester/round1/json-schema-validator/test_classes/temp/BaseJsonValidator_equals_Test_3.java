package com.networknt.schema;

class BaseJsonValidator_equals_Test {

    /**
     * Tolerance used for floating‑point comparisons.
     */
    private static final double EPS = 1e-12;

//     private static Stream<Arguments> trueOutcomeProvider() {
//         return // Exact zero
//         Stream.// Exact zero
//         of(// Negative zero vs positive zero – should be considered equal
//         Arguments.of("Both numbers are exactly zero; difference is 0", 0.0, 0.0), // Identical positive double
//         Arguments.of("Negative zero and positive zero are equal", -0.0, 0.0), // Identical negative double
//         Arguments.of("Two identical positive doubles should be equal", 123.456, 123.456), // Difference just below the threshold (5 × 10⁻¹³)
//         Arguments.of("Two identical negative doubles should be equal", -987.654, -987.654), // Same as above but reversed order
//         Arguments.of("Difference 5e-13 < 1e-12 (1.000000000001 vs 1.0000000000015)", 1.000000000001, 1.0000000000015), // Very small sub‑normal number vs zero
//         Arguments.of("Difference 5e-13 < 1e-12 (reversed order)", 1.0000000000015, 1.000000000001), // Tiny opposite‑sign numbers
//         Arguments.of("Zero compared with the smallest positive subnormal double (5e-324)", 0.0, 5e-324), // Max double values
//         Arguments.of("Opposite‑sign tiny numbers (1e-13 vs -1e-13)", 1e-13, -1e-13), // Min (most negative) double values
//         Arguments.of("Both arguments are Double.MAX_VALUE", Double.MAX_VALUE, Double.MAX_VALUE), // Simple identical value not covered elsewhere
//         Arguments.of("Both arguments are -Double.MAX_VALUE", -Double.MAX_VALUE, -Double.MAX_VALUE), Arguments.of("Simple identical positive value 42.42", 42.42, 42.42));
//     }

//     private static Stream<Arguments> falseOutcomeProvider() {
//         return // Difference exactly equal to the tolerance – should be false because the method uses '<'
//         Stream.// Difference exactly equal to the tolerance – should be false because the method uses '<'
//         of(// Difference just above the tolerance
//         Arguments.of("Difference exactly 1.0E-12 (0.0 vs 1e-12)", 0.0, 1e-12), // NaN comparisons – always false
//         Arguments.of("Difference 2e-12 > 1e-12 (1.0 vs 1.0+2e-12)", 1.0, 1.0 + 2e-12), // Infinity comparisons
//         Arguments.of("NaN vs NaN", Double.NaN, Double.NaN), // Infinity comparisons
//         Arguments.of("NaN vs 0.0", Double.NaN, 0.0), // Infinity comparisons
//         Arguments.of("0.0 vs NaN", 0.0, Double.NaN), Arguments.of("Positive infinity vs negative infinity", Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), Arguments.of("Positive infinity vs finite number", Double.POSITIVE_INFINITY, 1e308), Arguments.of("Finite negative number vs negative infinity", -1e308, Double.NEGATIVE_INFINITY));
//     }

    /**
     * Determines whether two {@code double} values are considered equal.
     *
     * @param a first value
     * @param b second value
     * @return {@code true} if the values are equal according to the rules described
     *         above; {@code false} otherwise.
     */
    protected static boolean equals(double a, double b) {
        // NaN is never equal to anything, including another NaN
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        // Fast path for exact equality – also covers infinities of the same sign
        // and the special case of +0.0 vs -0.0 (they compare equal with ==)
        if (a == b) {
            return true;
        }
        // At this point we know both numbers are finite and not exactly equal
        double diff = Math.abs(a - b);
        // Absolute tolerance check – handles values around zero and sub‑normal numbers
        if (diff < EPS) {
            return true;
        }
        // Relative tolerance check – avoids overflow for very large magnitudes
        double maxAbs = Math.max(Math.abs(a), Math.abs(b));
        // maxAbs is guaranteed to be > 0 because a != b and neither is zero (handled above)
        return diff < EPS * maxAbs;
    }

//     @Test
//     @DisplayName("Exact equality should return true")
//     void testExactEquality() {
//         assertTrue(BaseJsonValidator.equals(0.0, 0.0));
//         assertTrue(BaseJsonValidator.equals(123.456, 123.456));
//         assertTrue(BaseJsonValidator.equals(-987.654, -987.654));
//     }

//     @Test
//     @DisplayName("Values within tolerance should be considered equal")
//     void testWithinTolerance() {
//         double a = 1.000000000000;
//         // difference 5 × 10⁻¹³ < 1 × 10⁻¹²
//         double b = a + 5e-13;
//         assertTrue(BaseJsonValidator.equals(a, b));
//         double c = -2.5e20;
//         // relative diff < tolerance
//         double d = c - 9e-13 * Math.abs(c);
//         assertTrue(BaseJsonValidator.equals(c, d));
//     }

//     @Test
//     @DisplayName("Values outside tolerance should not be considered equal")
//     void testOutsideTolerance() {
//         double a = 1.0;
//         // just above the tolerance
//         double b = a + 2e-12;
//         assertFalse(BaseJsonValidator.equals(a, b));
//         double c = -3.141592653589;
//         // just above the tolerance
//         double d = c + 1.1e-12;
//         assertFalse(BaseJsonValidator.equals(c, d));
//     }

//     @Test
//     @DisplayName("NaN comparisons should return false")
//     void testNaN() {
//         assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
//         assertFalse(BaseJsonValidator.equals(Double.NaN, 0.0));
//         assertFalse(BaseJsonValidator.equals(0.0, Double.NaN));
//     }

//     @Test
//     @DisplayName("Infinity comparisons")
//     void testInfinity() {
//         // Same infinities are equal
//         assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
//         assertTrue(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
//         // Positive vs negative infinity are not equal
//         assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));
//         // Infinity vs finite number should be false
//         assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, 1e308));
//         assertFalse(BaseJsonValidator.equals(-1e308, Double.NEGATIVE_INFINITY));
//     }

    /* -------------------------------------------------------------
     *  True‑outcome test cases (method returns {@code true})
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("trueOutcomeProvider")
//     void testEqualsTrue(String description, double n1, double n2) {
//         assertTrue(BaseJsonValidator.equals(n1, n2), () -> "Expected true for: " + description + " (n1=" + n1 + ", n2=" + n2 + ")");
//     }

    /* -------------------------------------------------------------
     *  False‑outcome test cases (method returns {@code false})
     * ------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("falseOutcomeProvider")
//     void testEqualsFalse(String description, double n1, double n2) {
//         assertFalse(BaseJsonValidator.equals(n1, n2), () -> "Expected false for: " + description + " (n1=" + n1 + ", n2=" + n2 + ")");
//     }

    /* -------------------------------------------------------------
     *  Additional explicit tests for readability (non‑parameterised)
     * ------------------------------------------------------------- */
//     @Test
//     @DisplayName("Exact equality should return true (redundant sanity check)")
//     void testExactEqualitySanity() {
//         assertTrue(BaseJsonValidator.equals(0.0, 0.0));
//         assertTrue(BaseJsonValidator.equals(123.456, 123.456));
//         assertTrue(BaseJsonValidator.equals(-987.654, -987.654));
//     }

//     @Test
//     @DisplayName("Infinity comparisons – sanity checks")
//     void testInfinitySanity() {
//         assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
//         assertTrue(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
//     }
}