```java
package com.networknt.schema;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_equals_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this static utility method
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

    @Test
    void equals_whenExactlySameNumber_returnsTrue() {
        assertTrue(BaseJsonValidator.equals(1.23456789, 1.23456789),
                "Identical numbers should be considered equal");
    }

    @Test
    void equals_whenDifferenceWithinTolerance_returnsTrue() {
        double a = 1.000000000001;
        double b = 1.000000000002; // difference = 1e-12
        assertTrue(BaseJsonValidator.equals(a, b),
                "Numbers whose absolute difference is less than 1e-12 should be equal");
    }

    @Test
    void equals_whenDifferenceExactlyAtTolerance_returnsFalse() {
        double a = 1.0;
        double b = 1.0 + 1.0e-12; // difference = 1e-12 (not less than)
        assertFalse(BaseJsonValidator.equals(a, b),
                "Numbers whose absolute difference equals the tolerance should not be considered equal");
    }

    @Test
    void equals_whenDifferenceBeyondTolerance_returnsFalse() {
        double a = 12345.6789;
        double b = 12345.6789 + 1.0e-11; // difference larger than tolerance
        assertFalse(BaseJsonValidator.equals(a, b),
                "Numbers whose absolute difference exceeds the tolerance should not be equal");
    }

    @Test
    void equals_withNegativeZeroAndPositiveZero_returnsTrue() {
        assertTrue(BaseJsonValidator.equals(-0.0, 0.0),
                "Negative zero and positive zero should be considered equal");
    }

    @Test
    void equals_withNaNValues_returnsFalse() {
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN),
                "NaN compared to NaN should not be considered equal");
        assertFalse(BaseJsonValidator.equals(Double.NaN, 0.0),
                "NaN compared to a regular number should not be considered equal");
    }

    @Test
    void equals_withInfinityValues_returnsCorrectResult() {
        assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
                "Positive infinity should be equal to itself");
        assertTrue(BaseJsonValidator.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
                "Negative infinity should be equal to itself");
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                "Positive and negative infinity should not be equal");
        assertFalse(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, 1.0e308),
                "Infinity compared to a finite number should not be equal");
    }
}
```