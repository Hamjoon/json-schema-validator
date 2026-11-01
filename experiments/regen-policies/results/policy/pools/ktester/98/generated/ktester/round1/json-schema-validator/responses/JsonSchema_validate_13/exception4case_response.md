{
    "test_cases": [
        {
            "group": "validate – internal validate throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalStateException (or any RuntimeException) propagates out of validate; format.format is never invoked.",
                    "description": "Simulates a scenario where the internal validate(executionContext, node) method throws a generic RuntimeException (e.g., IllegalStateException). The validate method should not catch it, allowing the exception to bubble up, and should not call format.format."
                }
            ]
        },
        {
            "group": "validate – internal validate throws Error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "AssertionError (or any java.lang.Error) propagates out of validate; format.format is never invoked.",
                    "description": "Ensures that if the internal validation logic throws an Error (e.g., AssertionError), the validate method does not catch it and the error propagates to the caller."
                }
            ]
        },
        {
            "group": "validate – FailFastAssertionException with null messages",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "invalidJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "format.customize(executionContext, validationContext) is invoked; internal validate throws FailFastAssertionException whose getValidationMessages() returns null; catch block assigns validationMessages = null; format.format(this, null, executionContext, validationContext) is invoked and its return value is returned.",
                    "description": "Tests the edge case where FailFastAssertionException is thrown but its payload (validation messages) is null. The method should forward the null set to the OutputFormat without throwing."
                }
            ]
        }
    ]
}