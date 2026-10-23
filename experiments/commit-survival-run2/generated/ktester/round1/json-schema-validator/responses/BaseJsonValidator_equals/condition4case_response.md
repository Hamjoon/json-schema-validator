{
  "group": "BaseJsonValidator.equals(double, double) - branch coverage",
  "cases": [
    {
      "input": [
        { "parameter": "n1", "value": 1.0 },
        { "parameter": "n2", "value": 1.0 }
      ],
      "expected": true,
      "description": "Identical numbers – difference is 0, which is less than 1e‑12, so the method should return true."
    },
    {
      "input": [
        { "parameter": "n1", "value": 1.0 },
        { "parameter": "n2", "value": 1.0E-13 }
      ],
      "expected": true,
      "description": "Numbers differ by 9.9e‑13 (< 1e‑12); the method should evaluate to true."
    },
    {
      "input": [
        { "parameter": "n1", "value": 1.0 },
        { "parameter": "n2", "value": 1.0E-12 }
      ],
      "expected": false,
      "description": "Numbers differ exactly by 1e‑12; the strict ‘<’ comparison fails, so false is expected."
    },
    {
      "input": [
        { "parameter": "n1", "value": 1.0 },
        { "parameter": "n2", "value": 1.0E-11 }
      ],
      "expected": false,
      "description": "Difference (9e‑12) exceeds the tolerance; method should return false."
    },
    {
      "input": [
        { "parameter": "n1", "value": -5.0 },
        { "parameter": "n2", "value": -5.0E-13 }
      ],
      "expected": true,
      "description": "Negative numbers with a tiny difference (< 1e‑12) – should return true."
    },
    {
      "input": [
        { "parameter": "n1", "value": -5.0 },
        { "parameter": "n2", "value": -5.0E-11 }
      ],
      "expected": false,
      "description": "Negative numbers whose absolute difference exceeds the tolerance – should return false."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 0.0 }
      ],
      "expected": true,
      "description": "Both zeros – difference is zero, therefore true."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 1.0E-13 }
      ],
      "expected": true,
      "description": "Zero vs a very small positive number (< 1e‑12) – true."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": 1.0E-12 }
      ],
      "expected": false,
      "description": "Zero vs a number exactly at the tolerance boundary – false."
    },
    {
      "input": [
        { "parameter": "n1", "value": "NaN" },
        { "parameter": "n2", "value": 0.0 }
      ],
      "expected": false,
      "description": "First argument is NaN – any arithmetic with NaN yields NaN, comparison fails, so false."
    },
    {
      "input": [
        { "parameter": "n1", "value": 0.0 },
        { "parameter": "n2", "value": "NaN" }
      ],
      "expected": false,
      "description": "Second argument is NaN – same reasoning as above, result false."
    },
    {
      "input": [
        { "parameter": "n1", "value": "Infinity" },
        { "parameter": "n2", "value": "Infinity" }
      ],
      "expected": false,
      "description": "Both arguments are positive infinity – subtraction yields NaN, comparison false."
    },
    {
      "input": [
        { "parameter": "n1", "value": "-Infinity" },
        { "parameter": "n2", "value": "-Infinity" }
      ],
      "expected": false,
      "description": "Both arguments are negative infinity – same behavior as positive infinity, result false."
    },
    {
      "input": [
        { "parameter": "n1", "value": "Infinity" },
        { "parameter": "n2", "value": 1.0 }
      ],
      "expected": false,
      "description": "One argument infinite, the other finite – difference is infinite, so false."
    }
  ]
}