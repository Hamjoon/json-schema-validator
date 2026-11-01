{
    "group": "AnyOfValidator.canShortCircuit",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(getExecutionConfig() throws IllegalStateException)"
                }
            ],
            "expected": "IllegalStateException",
            "description": "When ExecutionContext.getExecutionConfig() throws an unchecked exception (IllegalStateException), canShortCircuit should propagate that exception without handling it."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext(getExecutionConfig() throws NullPointerException)"
                }
            ],
            "expected": "NullPointerException",
            "description": "When ExecutionContext.getExecutionConfig() itself throws a NullPointerException, the method should not catch it and the exception should propagate to the caller."
        }
    ]
}