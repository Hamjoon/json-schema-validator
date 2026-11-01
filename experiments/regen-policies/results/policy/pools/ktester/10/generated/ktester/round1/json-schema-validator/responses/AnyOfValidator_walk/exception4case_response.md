{
    "test_cases": [
        {
            "group": "null executionContext – shouldValidateSchema true",
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
                    "description": "When executionContext is null and shouldValidateSchema is true, AnyOfValidator.walk should forward the null to validate(...). The validate method (or subsequent code) will dereference it, causing a NullPointerException which must propagate."
                }
            ]
        },
        {
            "group": "null executionContext – shouldValidateSchema false",
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
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When executionContext is null and shouldValidateSchema is false, the method iterates over internal schemas and passes the null executionContext to each schema.walk(...). A NullPointerException is expected to propagate."
                }
            ]
        },
        {
            "group": "null rootNode – shouldValidateSchema true",
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
                    "description": "If rootNode is null and shouldValidateSchema is true, the call to validate(...) will receive a null rootNode and should result in a NullPointerException."
                }
            ]
        },
        {
            "group": "null rootNode – shouldValidateSchema false",
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
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When rootNode is null and shouldValidateSchema is false, each inner schema.walk receives the null rootNode; the first invocation that dereferences it should throw a NullPointerException which propagates."
                }
            ]
        },
        {
            "group": "null instanceLocation – shouldValidateSchema true",
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
                    "description": "If instanceLocation is null and shouldValidateSchema is true, validate(...) receives a null JsonNodePath, leading to a NullPointerException that must propagate."
                }
            ]
        },
        {
            "group": "null instanceLocation – shouldValidateSchema false",
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
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When instanceLocation is null and shouldValidateSchema is false, each inner schema.walk is called with a null instanceLocation; the call should result in a NullPointerException that propagates."
                }
            ]
        },
        {
            "group": "schemas list contains null element – shouldValidateSchema false",
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
                    "description": "The AnyOfValidator instance is prepared with a schemas list that contains a null entry. During walk with shouldValidateSchema false, the loop attempts to invoke walk on the null entry, causing a NullPointerException which must propagate."
                }
            ]
        },
        {
            "group": "validate method throws RuntimeException – shouldValidateSchema true",
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
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "When shouldValidateSchema is true, AnyOfValidator delegates to its validate(...) method. If validate throws a RuntimeException (e.g., due to internal schema errors), walk must not catch it; the exception should propagate unchanged."
                }
            ]
        }
    ]
}