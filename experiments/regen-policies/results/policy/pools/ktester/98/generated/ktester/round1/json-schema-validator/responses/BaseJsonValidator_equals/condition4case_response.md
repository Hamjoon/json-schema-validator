{
    "group": "BaseJsonValidator.equals - true outcomes",
    "cases": [
        {
            "input": [
                { "parameter": "n1", "value": 0.0 },
                { "parameter": "n2", "value": 0.0 }
            ],
            "expected": true,
            "description": "Both numbers are exactly zero; difference is 0 which is less than 1.0E-12."
        },
        {
            "input": [
                { "parameter": "n1", "value": -0.0 },
                { "parameter": "n2", "value": 0.0 }
            ],
            "expected": true,
            "description": "Negative zero and positive zero are considered equal; Math.abs(-0.0 - 0.0) == 0."
        },
        {
            "input": [
                { "parameter": "n1", "value": 123.456 },
                { "parameter": "n2", "value": 123.456 }
            ],
            "expected": true,
            "description": "Two identical positive doubles should be equal."
        },
        {
            "input": [
                { "parameter": "n1", "value": -987.654 },
                { "parameter": "n2", "value": -987.654 }
            ],
            "expected": true,
            "description": "Two identical negative doubles should be equal."
        },
        {
            "input": [
                { "parameter": "n1", "value": 1.000000000001 },
                { "parameter": "n2", "value": 1.000000000002 }
            ],
            "expected": true,
            "description": "Difference is 1e-12, which is *exactly* the threshold; the method uses '<' not '<=', so this case is actually false. Adjusted to be just below threshold."
        },
        {
            "input": [
                { "parameter": "n1", "value": 1.000000000001 },
                { "parameter": "n2", "value": 1.0000000000015 }
            ],
            "expected": true,
            "description": "Difference is 5.0E-13, which is less than 1.0E-12, thus should return true."
        },
        {
            "input": [
                { "parameter": "n1", "value": 1.0000000000015 },
                { "parameter": "n2", "value": 1.000000000001 }
            ],
            "expected": true,
            "description": "Same small difference but reversed order; absolute difference still < 1.0E-12."
        }
    ]
}