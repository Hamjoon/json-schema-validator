{
    "test_cases": [
        {
            "group": "BaseJsonValidator.validate - internal validation throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat (customize does nothing, format records invocation but is not called because exception occurs earlier)"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "RuntimeException with message \"validation-failure\" is propagated; OutputFormat.format is never invoked.",
                    "description": "The subclass of BaseJsonValidator overrides the protected validate(ExecutionContext, JsonNode) method to throw a RuntimeException. The test verifies that the exception bubbles up and that the OutputFormat.format method is not called."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.validate - internal validation returns null causing format.format NPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "mockOutputFormat (customize does nothing, format throws NullPointerException when received null ValidationMessage set)"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown by OutputFormat.format because the Set<ValidationMessage> argument is null.",
                    "description": "A subclass of BaseJsonValidator overrides validate(ExecutionContext, JsonNode) to return null instead of a Set. The mock OutputFormat is programmed to throw NPE when the validationMessages argument is null. The test ensures that the NPE propagates and no other side‑effects occur."
                }
            ]
        }
    ]
}