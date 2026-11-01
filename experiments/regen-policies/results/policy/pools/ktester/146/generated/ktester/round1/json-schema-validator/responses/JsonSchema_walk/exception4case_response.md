{
    "testCases": [
        {
            "group": "ExecutionContext throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContextThatThrowsRuntimeException"
                        },
                        {
                            "parameter": "node",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "RuntimeException propagated from ExecutionContext",
                    "description": "If the supplied `ExecutionContext` throws an unchecked exception during the walk (e.g., when the validator collection is accessed), the `walk` method must not catch it; the exception should propagate to the caller."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer creates illegal validation state",
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
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "executionCustomizerThatSetsInvalidContextState"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"Invalid validation state\"",
                    "description": "A customizer that mutates the `ExecutionContext` into an invalid state (e.g., clears the schema validators map) should cause the subsequent validation step to detect the inconsistency and throw an `IllegalArgumentException`."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer is null but validate flag true",
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
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages)",
                    "description": "When `executionCustomizer` is null the method should behave as if a no‑op customizer was supplied; with `validate` true it must still perform validation and return a successful result for a conforming node."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer is null and validate flag false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "invalidJsonNodeMissingRequiredProperty"
                        },
                        {
                            "parameter": "validate",
                            "value": false
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages) – validation is skipped even though the node is invalid",
                    "description": "When `validate` is false the method must skip validation entirely; a null customizer must not change this behaviour."
                }
            ]
        }
    ]
}