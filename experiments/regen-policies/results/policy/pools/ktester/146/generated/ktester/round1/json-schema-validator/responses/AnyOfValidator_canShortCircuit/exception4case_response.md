{
    "new_test_cases": [
        {
            "group": "AnyOfValidator.canShortCircuit(ExecutionContext) - getExecutionConfig exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_getConfigThrowsRuntime"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "When ExecutionContext.getExecutionConfig() throws a RuntimeException, the method should propagate that RuntimeException."
                },
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_getConfigThrowsNPE"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When ExecutionContext.getExecutionConfig() throws a NullPointerException, the method should propagate that NullPointerException."
                }
            ]
        }
    ]
}