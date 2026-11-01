{
    "testCases": [
        {
            "group": "evaluationParentSchema null and getParentSchema throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException with message \"parent schema failure\"",
                    "description": "When the protected field `evaluationParentSchema` is null and the overridden `getParentSchema()` deliberately throws a RuntimeException, `getEvaluationParentSchema()` must propagate that exception unchanged."
                }
            ]
        },
        {
            "group": "evaluationParentSchema non‑null and getParentSchema throws RuntimeException (exception not propagated)",
            "cases": [
                {
                    "input": [],
                    "expected": "returns the JsonSchema instance set as evaluationParentSchema (MockSchemaX) without throwing",
                    "description": "Even if the overridden `getParentSchema()` throws a RuntimeException, the method should short‑circuit because `evaluationParentSchema` is non‑null, thus no exception is propagated and the stored schema is returned."
                }
            ]
        },
        {
            "group": "evaluationParentSchema null and getParentSchema throws NullPointerException",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When `evaluationParentSchema` is null and `getParentSchema()` is overridden to throw a NullPointerException, the call to `getEvaluationParentSchema()` must let that NullPointerException bubble up."
                }
            ]
        },
        {
            "group": "evaluationParentSchema null and getParentSchema throws custom checked exception wrapped in RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException caused by IllegalStateException",
                    "description": "If `getParentSchema()` throws a checked exception that is wrapped in a RuntimeException (e.g., IllegalStateException), the wrapper should be propagated unchanged by `getEvaluationParentSchema()`."
                }
            ]
        }
    ]
}