```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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
}
```