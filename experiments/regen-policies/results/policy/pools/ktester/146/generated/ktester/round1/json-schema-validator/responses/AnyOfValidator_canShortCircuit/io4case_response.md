{
    "group": "AnyOfValidator.canShortCircuit(ExecutionContext) - edge cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null ExecutionContext should cause a NullPointerException because the method dereferences executionContext."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext_nullConfig"
                }
            ],
            "expected": "NullPointerException",
            "description": "When ExecutionContext.getExecutionConfig() returns null, the method attempts to call isAnnotationCollectionEnabled() on a null reference, resulting in a NullPointerException."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext_annotationEnabledThrows"
                }
            ],
            "expected": "RuntimeException",
            "description": "If ExecutionConfig.isAnnotationCollectionEnabled() throws a RuntimeException, the method propagates that exception without handling it."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext_annotationEnabledTrue_multipleCalls"
                }
            ],
            "expected": "false",
            "description": "Even if isAnnotationCollectionEnabled() is called multiple times (e.g., via a spy), the result remains false when the flag is true."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext_annotationEnabledFalse_multipleCalls"
                }
            ],
            "expected": "true",
            "description": "Even if isAnnotationCollectionEnabled() is called multiple times, the result remains true when the flag is false."
        }
    ]
}