{
  "group": "equals(double, double)",
  "cases": [
    {
      "input": [
        { "parameter": "n1", "value": 5.0 },
        { "parameter": "n2", "value": 5.0 }
      ],
      "expected": true,
      "description": "Identical numbers should be considered equal (difference = 0)."
    },
    {
      "input": [
        { "parameter": "n1", "value": 1.0 },
        { "parameter": "n2", "value": 1.0000000000005 }
      ],
      "expected": true,
      "description": "Numbers whose absolute difference (5e-13) is smaller than the tolerance 1e-12 should be equal."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 1.0e-12 }
      ],
      "expected": false,
      "description": "Difference exactly equal to the tolerance (1e-12) must evaluate to false because the condition uses '<' not '<='."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 1.5e-12 }
      ],
      "expected": false,
      "description": "Numbers whose absolute difference (1.5e-12) exceeds the tolerance should not be equal."
    },
    {
      "input": [
        { "parameter": "n1", "value": 1.0e20 },
        { "parameter": "n2", "value": 1.0000000000005e20 }
      ],
      "expected": true,
      "description": "Even with very large magnitudes, equality is based on absolute difference; here the difference is 5e-13, which is within tolerance."
    },
    {
      "input": [
        { "parameter": "n1", "value": "NaN" },
        { "parameter": "n2", "value": "NaN" }
      ],
      "expected": false,
      "description": "Any comparison involving NaN results in false because Math.abs(NaN) is NaN and the '<' comparison yields false."
    },
    {
      "input": [
        { "parameter": "n1", "value": "Infinity" },
        { "parameter": "n2", "value": "Infinity" }
      ],
      "expected": false,
      "description": "Positive infinity compared to itself produces NaN after subtraction, so the method returns false."
    },
    {
      "input": [
        { "parameter": "n1", "value": "Infinity" },
        { "parameter": "n2", "value": 1.0 }
      ],
      "expected": false,
      "description": "Positive infinity vs a finite number yields an infinite difference, which is not less than the tolerance."
    },
    {
      "input": [
        { "parameter": "n1", "value": "-Infinity" },
        { "parameter": "n2", "value": "-Infinity" }
      ],
      "expected": false,
      "description": "Negative infinity compared to itself also results in NaN after subtraction, leading to false."
    }
  ]
}