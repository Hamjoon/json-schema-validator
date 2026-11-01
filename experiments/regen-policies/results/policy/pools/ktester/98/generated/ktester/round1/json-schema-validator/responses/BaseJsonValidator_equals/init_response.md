```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code BaseJsonValidator.equals(double, double)} utility method.
 *
 * <p>This method determines equality of two {@code double} values using a
 * tolerance of {@code 1.0E-12}. The tests cover typical values, values that are
 * just inside or outside the tolerance, as well as special floating‑point
 * values such as {@code NaN} and infinities.</p>
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

    @Test
    @DisplayName("Exact equality should return true")
    void testExactEquality() {
        assertTrue(BaseJsonValidator.equals(0.0, 0.0));
        assertTrue(BaseJsonValidator.equals(123.456, 123.456));
        assertTrue(BaseJsonValidator.equals(-987.654, -987.654));
    }

    @Test
    @DisplayName("Values within tolerance should be considered equal")
    void testWithinTolerance() {
        double a = 1.000000000000;
        double b = a + 5e-13; // difference 5 × 10⁻¹³ < 1 × 10⁻¹²
        assertTrue(BaseJsonValidator.equals(a, b));

        double c = -2.5e20;
        double d = c - 9e-13 * Math.abs(c); // relative diff < tolerance
        assertTrue(BaseJsonValidator.equals(c, d));
    }

    @Test
    @DisplayName("Values outside tolerance should not be considered equal")
    void testOutsideTolerance() {
        double a = 1.0;
        double b = a + 2e-12; // just above the tolerance
        assertFalse(BaseJsonValidator.equals(a, b));

        double c = -3.141592653589;
        double d = c + 1.1e-12; // just above the tolerance
        assertFalse(BaseJsonValidator.equals(c, d));
    }

    @Test
    @DisplayName("NaN comparisons should return false")
    void testNaN() {
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
        assertFalse(BaseJsonValidator.equals(Double.NaN, 0.0));
        assertFalse(BaseJsonValidator.equals(0.0, Double.NaN));
    }

    @Test
    @DisplayName("Infinity comparisons")
    void testInfinity() {
        // Same infinities are equal
        assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertTrue(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));

        // Positive vs negative infinity are not equal
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));

        // Infinity vs finite number should be false
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, 1e308));
        assertFalse(BaseJsonValidator.equals(-1e308, Double.NEGATIVE_INFINITY));
    }
}
```