{
    "testCases": [
        {
            "group": "BaseJsonValidator#getKeyword() - exception propagation scenarios",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When the internal `keyword` object is non‑null but its `getValue()` implementation throws a `NullPointerException`, `getKeyword()` should let that exception propagate."
                },
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "When the internal `keyword` object is non‑null and its `getValue()` throws an `IllegalArgumentException`, the exception must not be caught by `getKeyword()` and should propagate to the caller."
                },
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "When `keyword.getValue()` throws a generic unchecked `RuntimeException` (e.g., new RuntimeException(\"boom\")), `getKeyword()` must propagate the exception unchanged."
                },
                {
                    "input": [],
                    "expected": "AssertionError",
                    "description": "If `keyword.getValue()` throws an `Error` such as `AssertionError`, `getKeyword()` does not handle it and the error propagates outward."
                }
            ]
        }
    ]
}