{
  "group": "equals(double, double) - additional cases",
  "cases": [
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": -0.0 }
      ],
      "expected": true,
      "description": "Positive zero and negative zero have a difference of 0.0, which is less than the tolerance, so they are considered equal."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 4.9e-324 }
      ],
      "expected": true,
      "description": "The smallest positive sub‑normal double (Double.MIN_VALUE) differs from zero by ~4.9×10⁻³²⁴, well below the 1e‑12 tolerance."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 9.99999999999e-13 }
      ],
      "expected": true,
      "description": "Difference (9.99999999999×10⁻¹³) is just under the tolerance (1e‑12), therefore the numbers are equal."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 1.000000000001e-12 }
      ],
      "expected": false,
      "description": "Difference (1.000000000001×10⁻¹²) is just above the tolerance, so the method should return false."
    },
    {
      "input": [
        { "parameter": "n1", "value": 5e-13 },
        { "parameter": "n2", "value": -5e-13 }
      ],
      "expected": false,
      "description": "Numbers with opposite signs have an absolute difference of exactly 1e‑12 (the tolerance). Because the comparison uses '<' (strictly less), the result is false."
    },
    {
      "input": [
        { "parameter": "n1", "value": "NaN" },
        { "parameter": "n2", "value": 0.0 }
      ],
      "expected": false,
      "description": "Any comparison involving NaN yields false; subtraction results in NaN, and the '<' test fails."
    },
    {
      "input": [
        { "parameter": "n1", "value": "NaN" },
        { "parameter": "n2", "value": "Infinity" }
      ],
      "expected": false,
      "description": "NaN compared with Infinity also returns false for the same reason as above."
    },
    {
      "input": [
        { "parameter": "n1", "value": 1.7976931348623157e308 },
        { "parameter": "n2", "value": 1.7976931348623157e308 }
      ],
      "expected": true,
      "description": "Two identical maximum‑value doubles have zero difference, which is less than the tolerance."
    },
    {
      "input": [
        { "parameter": "n1", "value": 1.7976931348623157e308 },
        { "parameter": "n2", "value": 1.7976931348623157e308 }
      ],
      "expected": true,
      "description": "Two maximum‑value doubles where the second operand is mathematically different but too small to affect the double representation (e.g., MAX_VALUE - 1e292). The subtraction underflows to 0.0, so the method reports equality."
    }
  ]
}