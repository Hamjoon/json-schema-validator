{
    "test_cases": [
        {
            "group": "JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer) - exception edge cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"John\",\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "customizerThrowsAssertionError"
                        }
                    ],
                    "expected": "AssertionError is propagated (the same error thrown by the customizer)",
                    "description": "ExecutionContextCustomizer throws an AssertionError (subclass of Error). The validate method must not catch Errors, so the AssertionError should bubble up unchanged."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"John\",\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "customizerThrowsIllegalStateException"
                        }
                    ],
                    "expected": "IllegalStateException is propagated (the same exception thrown by the customizer)",
                    "description": "ExecutionContextCustomizer throws a checked‑runtime exception (IllegalStateException). The method should let the exception propagate without wrapping."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "customizerThrowsRuntimeException"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown with message containing \"input must not be null\"",
                    "description": "Even though the customizer would throw a RuntimeException, argument validation occurs first. The method must detect the null input and throw IllegalArgumentException before invoking the customizer."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"John\",\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown with message containing \"inputFormat must not be null\"",
                    "description": "Argument validation should also reject a null InputFormat even when the input string itself is valid."
                }
            ]
        }
    ]
}