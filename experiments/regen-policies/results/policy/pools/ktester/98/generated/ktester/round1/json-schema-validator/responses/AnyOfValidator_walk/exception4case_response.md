{
    "test_cases": [
        {
            "group": "AnyOfValidator.walk - shouldValidateSchema true with null executionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": null
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
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
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true the method delegates to validate(..., true). Passing a null ExecutionContext should cause a NullPointerException inside validate (or before)."
                }
            ]
        },
        {
            "group": "AnyOfValidator.walk - shouldValidateSchema true with null node",
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
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true a null JsonNode argument should trigger a NullPointerException during validation."
                }
            ]
        },
        {
            "group": "AnyOfValidator.walk - shouldValidateSchema true with null rootNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": null
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
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true a null rootNode should cause a NullPointerException inside validate."
                }
            ]
        },
        {
            "group": "AnyOfValidator.walk - shouldValidateSchema true with null instanceLocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": null
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true a null JsonNodePath should result in a NullPointerException during validation."
                }
            ]
        },
        {
            "group": "AnyOfValidator.walk - shouldValidateSchema false with null schemas list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
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
                    "description": "If the internal `schemas` field is null, the for‑each loop will throw a NullPointerException when shouldValidateSchema is false."
                }
            ]
        },
        {
            "group": "AnyOfValidator.walk - shouldValidateSchema false with null schema element in list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
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
                    "description": "When shouldValidateSchema is false and the `schemas` list contains a null entry, invoking `schema.walk(...)` on that entry results in a NullPointerException."
                }
            ]
        },
        {
            "group": "AnyOfValidator.walk - shouldValidateSchema false with schema.walk throwing exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode"
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
                    "expected": "RuntimeException (propagated from child schema)",
                    "description": "When shouldValidateSchema is false the method iterates over child schemas. If a child schema's `walk` method throws a RuntimeException, the exception should propagate out of AnyOfValidator.walk."
                }
            ]
        }
    ]
}