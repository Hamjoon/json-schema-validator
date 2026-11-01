{
    "reportChildErrorsExceptionPropagation": {
        "cases": [
            {
                "input": [
                    {
                        "parameter": "executionContext",
                        "value": "mock ExecutionContext whose getExecutionConfig() throws IllegalArgumentException(\"invalid config\")"
                    }
                ],
                "expected": "IllegalArgumentException with message \"invalid config\"",
                "description": "When getExecutionConfig() throws an IllegalArgumentException, the exception should propagate out of reportChildErrors without being caught."
            },
            {
                "input": [
                    {
                        "parameter": "executionContext",
                        "value": "mock ExecutionContext whose ExecutionConfig.isFailFast() throws AssertionError(\"assertion failed\")"
                    }
                ],
                "expected": "AssertionError with message \"assertion failed\"",
                "description": "If isFailFast() throws an AssertionError, reportChildErrors must not swallow it; the error should be propagated to the caller."
            }
        ]
    }
}