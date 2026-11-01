{
    "test_cases": [
        {
            "group": "Null schemas list triggers NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": null
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The OneOfValidator instance is constructed with its internal `schemas` field set to null. When `shouldValidateSchema` is false (or true with a null node) the method takes the else‑branch and attempts to iterate over `schemas`, causing a NullPointerException."
                }
            ]
        },
        {
            "group": "Null child schema in schemas list triggers NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNonNullNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The validator's `schemas` list contains a null element (e.g., `[null, mockSchema]`). During the else‑branch iteration the code calls `schema.walk(...)` on the null entry, which throws a NullPointerException."
                }
            ]
        },
        {
            "group": "validate method propagates RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNonNullNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "A subclass of OneOfValidator overrides the protected `validate(...)` method to throw a RuntimeException (e.g., new RuntimeException(\"validation failed\")). When `shouldValidateSchema` is true and `node` is non‑null, `walk` calls `validate(..., true)`. The exception should propagate out of `walk` unchanged."
                }
            ]
        },
        {
            "group": "child schema walk propagates RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNonNullNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "One of the child `JsonSchema` objects in the validator's `schemas` list is a mock that throws a RuntimeException when its `walk` method is invoked. Because `shouldValidateSchema` is false, `OneOfValidator.walk` delegates to each child schema. The exception from the child should bubble up and be the result of the call."
                }
            ]
        }
    ]
}