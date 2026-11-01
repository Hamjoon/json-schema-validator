{
    "group": "AnyOfValidator.canShortCircuit(ExecutionContext)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext_annotationEnabledTrue"
                }
            ],
            "expected": "false",
            "description": "When the ExecutionConfig reports annotationCollectionEnabled = true, the method should return false (no short‑circuit)."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext_annotationEnabledFalse"
                }
            ],
            "expected": "true",
            "description": "When the ExecutionConfig reports annotationCollectionEnabled = false, the method should return true (short‑circuit is allowed)."
        }
    ]
}