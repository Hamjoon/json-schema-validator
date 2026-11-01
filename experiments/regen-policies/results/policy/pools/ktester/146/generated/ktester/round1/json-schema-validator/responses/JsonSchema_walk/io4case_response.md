{
    "new_test_cases": [
        {
            "group": "Validate flag false with null executionCustomizer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "anyJsonNode"
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
                    "expected": "ValidationResult.success (no validation messages) – walk performed without validation even when customizer is null",
                    "description": "When `validate` is false the method must ignore validation regardless of whether the `executionCustomizer` is null; the returned ValidationResult should be successful."
                }
            ]
        },
        {
            "group": "Null executionContext with validate false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": null
                        },
                        {
                            "parameter": "node",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "validate",
                            "value": false
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "mockExecutionCustomizer"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"ExecutionContext must not be null\"",
                    "description": "The method must validate its first argument before considering the `validate` flag; a null `ExecutionContext` should always cause an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer that throws RuntimeException",
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
                            "value": "executionCustomizerThatThrows"
                        }
                    ],
                    "expected": "RuntimeException propagated from ExecutionContextCustomizer",
                    "description": "If the supplied `ExecutionContextCustomizer` throws an unchecked exception, `walk` must not catch it silently; the exception should propagate to the caller."
                }
            ]
        },
        {
            "group": "No‑op ExecutionCustomizer (empty lambda)",
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
                            "value": "noOpExecutionCustomizer"
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages)",
                    "description": "A customizer that does nothing should behave identically to passing `null` – the walk proceeds normally and validation succeeds for a conforming node."
                }
            ]
        },
        {
            "group": "Customizer influences validation outcome",
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
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "customizerAddsDefaultValues"
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages) – the customizer injected default values that satisfy the schema",
                    "description": "The customizer can mutate the `ExecutionContext` (e.g., by adding a map of default values) that downstream validators use. After the customizer runs, a node that would otherwise be invalid becomes valid, resulting in a successful ValidationResult."
                }
            ]
        },
        {
            "group": "Validate true with type‑mismatch node (array vs object schema)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "arrayJsonNode"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "mockExecutionCustomizer"
                        }
                    ],
                    "expected": "ValidationResult.failure containing a type‑mismatch ValidationMessage",
                    "description": "When the schema expects an object but the supplied node is an array, validation must fail and the ValidationResult should contain a message indicating the type error."
                }
            ]
        },
        {
            "group": "Validate false with node that would be invalid if validated",
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
                            "value": "mockExecutionCustomizer"
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages) – validation is skipped",
                    "description": "Even though the node violates the schema, the `validate` flag is false, so the method must return a successful ValidationResult without reporting errors."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer that records invocation",
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
                            "value": "recordingExecutionCustomizer"
                        }
                    ],
                    "expected": "ValidationResult.success (no validation messages) and the customizer's invocation flag set to true",
                    "description": "A customizer that only records that it has been called (e.g., toggles a boolean in a test‑side holder) should be invoked exactly once before validation; the walk result must still be successful."
                }
            ]
        }
    ]
}