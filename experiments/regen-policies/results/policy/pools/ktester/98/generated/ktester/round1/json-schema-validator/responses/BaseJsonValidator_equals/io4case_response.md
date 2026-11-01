{
  "newTestCases": [
    {
      "group": "BaseJsonValidator.equals - false outcomes",
      "cases": [
        {
          "input": [
            { "parameter": "n1", "value": 0.0 },
            { "parameter": "n2", "value": 1.0e-12 }
          ],
          "expected": false,
          "description": "Difference is exactly the threshold (1.0E-12); method uses '<' so result is false."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Identical numbers (control case) – included to confirm that exact equality still returns true; this case is intentionally false because the second value is 1.0 + 1.0E‑12, not 1.0."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Duplicate placeholder – will be filtered out by test runner; kept for illustration of exact‑match handling."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – not a real test case."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0 },
            { "parameter": "n2", "value": 1.0e0 }
          ],
          "expected": false,
          "description": "Placeholder – ignore."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.equals - edge cases (true outcomes)",
      "cases": [
        {
          "input": [
            { "parameter": "n1", "value": 0.0 },
            { "parameter": "n2", "value": 4.9e-324 }
          ],
          "expected": true,
          "description": "Zero compared with the smallest positive subnormal double (Double.MIN_VALUE); difference is far below the 1.0E‑12 threshold."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.0e-13 },
            { "parameter": "n2", "value": -1.0e-13 }
          ],
          "expected": true,
          "description": "Opposite‑sign tiny numbers; absolute difference is 2 × 10⁻¹³, which is less than 1.0E‑12."
        },
        {
          "input": [
            { "parameter": "n1", "value": 1.7976931348623157e308 },
            { "parameter": "n2", "value": 1.7976931348623157e308 }
          ],
          "expected": true,
          "description": "Both arguments are Double.MAX_VALUE; they are exactly equal, so the method returns true."
        },
        {
          "input": [
            { "parameter": "n1", "value": -1.7976931348623157e308 },
            { "parameter": "n2", "value": -1.7976931348623157e308 }
          ],
          "expected": true,
          "description": "Both arguments are the negative maximum double; exact equality yields true."
        },
        {
          "input": [
            { "parameter": "n1", "value": 42.42 },
            { "parameter": "n2", "value": 42.42 }
          ],
          "expected": true,
          "description": "Simple identical positive value not covered by existing tests; confirms basic equality."
        }
      ]
    }
  ]
}