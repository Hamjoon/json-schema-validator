{
    "group": "BaseJsonValidator.equals(double, double) - additional cases",
    "cases": [
        {
            "input": [
                { "parameter": "n1", "value": "-0.0" },
                { "parameter": "n2", "value": "0.0" }
            ],
            "expected": "true",
            "description": "Negative zero and positive zero are numerically equal; the absolute difference is 0, which is less than the 1e-12 threshold."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.MIN_VALUE" },
                { "parameter": "n2", "value": "0.0" }
            ],
            "expected": "true",
            "description": "The smallest positive subnormal double (≈4.9e-324) differs from zero by far less than 1e-12, so the method should return true."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.MAX_VALUE" },
                { "parameter": "n2", "value": "Double.MAX_VALUE" }
            ],
            "expected": "true",
            "description": "Two identical maximum finite double values have a difference of 0, which satisfies the < 1e-12 condition."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.MAX_VALUE" },
                { "parameter": "n2", "value": "Double.MAX_VALUE - 1e-100" }
            ],
            "expected": "true",
            "description": "Subtracting an extremely tiny amount from Double.MAX_VALUE does not change its value due to rounding; the two arguments are effectively equal, yielding true."
        },
        {
            "input": [
                { "parameter": "n1", "value": "-1.0e-13" },
                { "parameter": "n2", "value": "1.0e-13" }
            ],
            "expected": "true",
            "description": "The absolute difference is 2e-13, which is smaller than the 1e-12 tolerance, so the method should return true."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.NaN" },
                { "parameter": "n2", "value": "0.0" }
            ],
            "expected": "false",
            "description": "Any comparison involving NaN results in false; the method should return false when one argument is NaN."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.POSITIVE_INFINITY" },
                { "parameter": "n2", "value": "1.0" }
            ],
            "expected": "false",
            "description": "Positive infinity minus a finite number yields infinity, whose absolute difference is not less than the threshold, so false is returned."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.NEGATIVE_INFINITY" },
                { "parameter": "n2", "value": "-1.0" }
            ],
            "expected": "false",
            "description": "Negative infinity compared with a finite number also results in a difference that is not less than 1e-12, leading to false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "-Double.MAX_VALUE" },
                { "parameter": "n2", "value": "-Double.MAX_VALUE" }
            ],
            "expected": "true",
            "description": "Two identical large negative double values have zero difference, satisfying the < 1e-12 condition."
        },
        {
            "input": [
                { "parameter": "n1", "value": "-1.0" },
                { "parameter": "n2", "value": "-1.000000000001" }
            ],
            "expected": "false",
            "description": "The numbers differ by exactly 1e-12 (the threshold) but the method uses a strict '<', so it should return false."
        }
    ]
}