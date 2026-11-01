{
    "newTestCases": [
        {
            "group": "evaluationParentSchemaNullParentNull",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "When `evaluationParentSchema` is `null` **and** `getParentSchema()` also returns `null`, `getEvaluationParentSchema()` should return `null` without throwing."
                }
            ]
        },
        {
            "group": "evaluationParentSchemaNullParentThrows",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "When `evaluationParentSchema` is `null` and `getParentSchema()` throws a `RuntimeException`, the exception must propagate out of `getEvaluationParentSchema()`."
                }
            ]
        },
        {
            "group": "evaluationParentSchemaSetParentThrows",
            "cases": [
                {
                    "input": [],
                    "expected": "mockEvaluationParentSchema",
                    "description": "Even if `getParentSchema()` would throw an exception, when `evaluationParentSchema` is already set the method must return that instance and never invoke `getParentSchema()`."
                }
            ]
        },
        {
            "group": "evaluationParentSchemaSetSameAsParent",
            "cases": [
                {
                    "input": [],
                    "expected": "sameInstance",
                    "description": "When both `evaluationParentSchema` and the value returned by `getParentSchema()` refer to the **same** `JsonSchema` instance, `getEvaluationParentSchema()` should return that instance (demonstrating that the method does not create a new object or alter the reference)."
                }
            ]
        },
        {
            "group": "evaluationParentSchemaSetDifferentFromParent",
            "cases": [
                {
                    "input": [],
                    "expected": "mockEvaluationParentSchema",
                    "description": "When `evaluationParentSchema` is set to a different `JsonSchema` than the one returned by `getParentSchema()`, the method must return the explicitly set `evaluationParentSchema` and ignore the parent schema."
                }
            ]
        }
    ]
}