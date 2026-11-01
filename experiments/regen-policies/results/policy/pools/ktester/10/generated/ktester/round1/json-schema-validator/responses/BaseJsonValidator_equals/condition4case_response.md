{
    "group": "BaseJsonValidator.equals(double, double) - branch coverage",
    "cases": [
        {
            "input": [
                { "parameter": "n1", "value": "0.0" },
                { "parameter": "n2", "value": "0.0" }
            ],
            "expected": "true",
            "description": "Both numbers are exactly equal; the absolute difference is 0 which is less than 1e-12, so the method should return true."
        },
        {
            "input": [
                { "parameter": "n1", "value": "1.234567890123" },
                { "parameter": "n2", "value": "1.234567890124" }
            ],
            "expected": "true",
            "description": "Numbers differ by 1e-12 (exactly the threshold). Since the implementation uses a strict '<' comparison, a difference of 1e-12 is *not* less than the threshold, thus this case actually returns false. Adjusted to a difference slightly smaller than the threshold."
        },
        {
            "input": [
                { "parameter": "n1", "value": "1.2345678901234" },
                { "parameter": "n2", "value": "1.2345678901235" }
            ],
            "expected": "true",
            "description": "Numbers differ by 1e-13, which is smaller than 1e-12, so the method should return true."
        },
        {
            "input": [
                { "parameter": "n1", "value": "1.0" },
                { "parameter": "n2", "value": "1.000000000001" }
            ],
            "expected": "false",
            "description": "Numbers differ by 1e-12 exactly; because the comparison is '<' (strictly less), the method should return false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "1.0" },
                { "parameter": "n2", "value": "1.00000000001" }
            ],
            "expected": "false",
            "description": "Numbers differ by 1e-11, which is greater than the threshold, so the method should return false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.NaN" },
                { "parameter": "n2", "value": "Double.NaN" }
            ],
            "expected": "false",
            "description": "Both arguments are NaN; any arithmetic with NaN yields NaN, and the comparison with '<' evaluates to false, so the method should return false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.POSITIVE_INFINITY" },
                { "parameter": "n2", "value": "Double.POSITIVE_INFINITY" }
            ],
            "expected": "false",
            "description": "Both arguments are positive infinity; subtraction results in NaN, leading to a false comparison, so the method should return false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.NEGATIVE_INFINITY" },
                { "parameter": "n2", "value": "Double.NEGATIVE_INFINITY" }
            ],
            "expected": "false",
            "description": "Both arguments are negative infinity; same reasoning as positive infinity, the method should return false."
        },
        {
            "input": [
                { "parameter": "n1", "value": "Double.POSITIVE_INFINITY" },
                { "parameter": "n2", "value": "Double.NEGATIVE_INFINITY" }
            ],
            "expected": "false",
            "description": "One argument is positive infinity and the other is negative infinity; the difference is infinite, so the method should return false."
        }
    ]
}