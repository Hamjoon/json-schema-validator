{
    "new_test_cases": [
        {
            "group": "OneOfValidator.validate - null executionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"type\":\"A\"}"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        }
                    ],
                    "expected": "NullPointerException (executionContext is required)",
                    "description": "Passing a null ExecutionContext should cause the validator to throw a NullPointerException before any schema processing occurs."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - null instanceLocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"type\":\"A\"}"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (instanceLocation must not be null)",
                    "description": "If the JsonNodePath argument is null, the validator cannot compute the location of validation messages and should throw a NullPointerException."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - null rootNode with non‑null node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"type\":\"A\"}"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        }
                    ],
                    "expected": "NullPointerException (rootNode is required for relative validation)",
                    "description": "When rootNode is null while the instance node is present, internal validation logic that resolves relative references will attempt to use rootNode and raise a NullPointerException."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - executionContext that throws during validator lookup",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContextThrowingOnGetValidator"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"type\":\"A\"}"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        }
                    ],
                    "expected": "RuntimeException (wrapped exception from ExecutionContext while obtaining validator factory)",
                    "description": "If the provided ExecutionContext throws an unexpected RuntimeException when the validator attempts to retrieve the JsonSchemaFactory, the validate method should propagate that exception."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - node of unsupported type causing ClassCastException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing a binary value (e.g., Base64 encoded string) that the validator treats as an ObjectNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        }
                    ],
                    "expected": "ClassCastException (validator expects an ObjectNode but receives a different JsonNode subtype)",
                    "description": "If the internal validation logic casts the supplied JsonNode to ObjectNode without checking its actual type, supplying a non‑object node (e.g., a binary node) will trigger a ClassCastException."
                }
            ]
        }
    ]
}