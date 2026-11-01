{
    "group": "reportChildErrors",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextGetConfigThrows"
                }
            ],
            "expected": "IllegalStateException",
            "description": "ExecutionContext.getExecutionConfig() throws IllegalStateException; the exception should propagate out of reportChildErrors."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContextConfigError"
                }
            ],
            "expected": "AssertionError",
            "description": "ExecutionConfig.isFailFast() throws an AssertionError (an Error); the method should not catch it and the AssertionError should propagate."
        }
    ]
}