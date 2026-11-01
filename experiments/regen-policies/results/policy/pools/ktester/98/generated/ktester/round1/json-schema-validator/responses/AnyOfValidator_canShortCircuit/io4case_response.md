{
    "group": "AnyOfValidator.canShortCircuit",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(getExecutionConfig() returns null)"
                }
            ],
            "expected": "NullPointerException",
            "description": "When ExecutionContext.getExecutionConfig() returns null the method dereferences it, causing a NullPointerException."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(isAnnotationCollectionEnabled() throws IllegalStateException)"
                }
            ],
            "expected": "IllegalStateException",
            "description": "If ExecutionConfig.isAnnotationCollectionEnabled() throws an unchecked exception, canShortCircuit should propagate that exception unchanged."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "spyExecutionContext(with ExecutionConfig returning true and a counter to verify single call)"
                }
            ],
            "expected": false,
            "description": "When annotation collection is enabled (true) the method must return false; the test also verifies that ExecutionContext.getExecutionConfig() and isAnnotationCollectionEnabled() are each invoked exactly once (no extra processing)."
        }
    ]
}