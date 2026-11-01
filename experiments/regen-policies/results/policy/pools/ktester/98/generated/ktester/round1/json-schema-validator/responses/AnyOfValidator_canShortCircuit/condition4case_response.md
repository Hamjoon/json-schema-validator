{
    "group": "AnyOfValidator.canShortCircuit",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(annotationCollectionEnabled = true)"
                }
            ],
            "expected": false,
            "description": "When the ExecutionConfig reports annotation collection is enabled, canShortCircuit should return false (no short‑circuiting)."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(annotationCollectionEnabled = false)"
                }
            ],
            "expected": true,
            "description": "When the ExecutionConfig reports annotation collection is disabled, canShortCircuit should return true (short‑circuiting is allowed)."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null ExecutionContext should result in a NullPointerException because the method dereferences the argument without null‑check."
        }
    ]
}