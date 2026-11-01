```java
package com.networknt.schema;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

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
}
```