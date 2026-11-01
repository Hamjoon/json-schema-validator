{
    "tests": [
        {
            "group": "validate – null ExecutionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
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
                    "expected": "NullPointerException is thrown when format.customize(null, validationContext) is invoked.",
                    "description": "Ensures that passing a null ExecutionContext causes an NPE because the method does not guard against a null executionContext before delegating to the OutputFormat."
                }
            ]
        },
        {
            "group": "validate – null OutputFormat",
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
                            "value": "null"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown when attempting to invoke format.customize(...).",
                    "description": "Verifies that a null OutputFormat leads to an NPE because the method immediately calls format.customize without null‑checking."
                }
            ]
        },
        {
            "group": "validate – OutputFormat.customize throws RuntimeException",
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
                            "value": "outputFormatThrowingOnCustomize"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "RuntimeException (custom message) propagates out of validate method; format.format is never invoked.",
                    "description": "Checks that if OutputFormat.customize throws an unchecked exception, the validate method does not catch it and the exception bubbles up."
                }
            ]
        },
        {
            "group": "validate – OutputFormat.format throws RuntimeException",
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
                            "value": "outputFormatThrowingOnFormat"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "RuntimeException (custom message) propagates out of validate method after successful validation; format.customize is invoked, internal validation runs, but format.format throws.",
                    "description": "Ensures that an exception thrown by OutputFormat.format is not swallowed by validate and is propagated to the caller."
                }
            ]
        },
        {
            "group": "validate – ExecutionContextCustomizer throws RuntimeException",
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
                            "value": "customizerThrowingOnCustomize"
                        }
                    ],
                    "expected": "RuntimeException (custom message) propagates out of validate method after format.customize has been invoked; format.format is never called.",
                    "description": "Validates that a RuntimeException thrown by the supplied ExecutionContextCustomizer is not caught by validate and aborts processing."
                }
            ]
        },
        {
            "group": "validate – internal validate returns null",
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
                    "expected": "format.format(this, null, executionContext, validationContext) is invoked and its return value is returned.",
                    "description": "Simulates a scenario where the underlying validate(executionContext, node) method (perhaps overridden in a test subclass) returns null; the method should forward the null set to the OutputFormat without throwing."
                }
            ]
        },
        {
            "group": "validate – both customizer and format.customize throw",
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
                            "value": "outputFormatThrowingOnCustomize"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "customizerThrowingOnCustomize"
                        }
                    ],
                    "expected": "RuntimeException from format.customize propagates; executionCustomizer.customize is never invoked because the method aborts after the first exception.",
                    "description": "Confirms the order of operations: format.customize is called first; if it throws, the customizer is not executed."
                }
            ]
        },
        {
            "group": "validate – null JsonNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
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
                    "expected": "NullPointerException is thrown when internal validate(executionContext, null) is invoked (or earlier when format.customize is called, depending on implementation).",
                    "description": "Tests the behaviour when the JSON node to validate is null; the method does not perform a null‑check before delegating to the internal validation logic."
                }
            ]
        }
    ]
}