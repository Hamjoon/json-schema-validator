{
    "testCases": [
        {
            "group": "evaluationParentSchemaNullParentThrowsIllegalArgument",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "When `evaluationParentSchema` is null and the overridden `getParentSchema()` throws an `IllegalArgumentException`, the exception must propagate unchanged out of `getEvaluationParentSchema()`."
                }
            ]
        },
        {
            "group": "evaluationParentSchemaNullParentThrowsError",
            "cases": [
                {
                    "input": [],
                    "expected": "AssertionError",
                    "description": "When `evaluationParentSchema` is null and the overridden `getParentSchema()` throws an `AssertionError` (a subclass of `Error`), the method should not catch it and must let the `Error` propagate to the caller."
                }
            ]
        }
    ]
}