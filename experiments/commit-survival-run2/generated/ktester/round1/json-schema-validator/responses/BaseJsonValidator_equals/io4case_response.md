{
    "group": "BaseJsonValidator.equals(double, double) - additional cases",
    "cases": [
        {
            "input": [
                { "parameter": "n1", "value": 1e-13 },
                { "parameter": "n2", "value": -1e-13 }
            ],
            "expected": true,
            "description": "Positive and negative numbers with a tiny absolute difference (2e-13) that is still below the 1e‑12 tolerance; should return true."
        },
        {
            "input": [
                { "parameter": "n1", "value": 1.0 },
                { "parameter": "n2", "value": -1.0 }
            ],
            "expected": false,
            "description": "Numbers of opposite sign with a large absolute difference (2.0) exceeding the tolerance; should return false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Infinity" },
                { "parameter": "n2", "value": "-Infinity" }
            ],
            "expected": false,
            "description": "Positive infinity versus negative infinity; subtraction yields NaN, comparison fails, so false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "NaN" },
                { "parameter": "n2", "value": "NaN" }
            ],
            "expected": false,
            "description": "Both arguments are NaN; any arithmetic with NaN results in NaN, making the '<' comparison false."
        },
        {
            "input": [
                { "parameter": "n1", "value": 0.0 },
                { "parameter": "n2", "value": -0.0 }
            ],
            "expected": true,
            "description": "Positive zero versus negative zero; the absolute difference is 0.0, which is less than the tolerance."
        },
        {
            "input": [
                { "parameter": "n1", "value": 1.7976931348623157E308 },
                { "parameter": "n2", "value": 1.7976931348623157E308 }
            ],
            "expected": true,
            "description": "Two identical maximum double values; subtraction yields 0.0, therefore true."
        },
        {
            "input": [
                { "parameter": "n1", "value": 1.7976931348623157E308 },
                { "parameter": "n2", "value": 8.988465674311579E307 }
            ],
            "expected": false,
            "description": "Maximum double value versus half of it; absolute difference (~8.99e307) far exceeds the tolerance, so false."
        },
        {
            "input": [
                { "parameter": "n1", "value": 2.2250738585072014E-308 },
                { "parameter": "n2", "value": 4.9e-324 }
            ],
            "expected": true,
            "description": "Two subnormal positive numbers; their absolute difference (~2.225e-308) is far below 1e‑12, thus true."
        },
        {
            "input": [
                { "parameter": "n1", "value": 2.2250738585072014E-308 },
                { "parameter": "n2", "value": 0.0 }
            ],
            "expected": true,
            "description": "Smallest normal positive double versus zero; difference (~2.2e-308) < tolerance, so true."
        },
        {
            "input": [
                { "parameter": "n1", "value": 4.9e-324 },
                { "parameter": "n2", "value": 0.0 }
            ],
            "expected": true,
            "description": "Smallest positive subnormal double versus zero; difference (~4.9e-324) is well under the tolerance, returning true."
        }
    ]
}