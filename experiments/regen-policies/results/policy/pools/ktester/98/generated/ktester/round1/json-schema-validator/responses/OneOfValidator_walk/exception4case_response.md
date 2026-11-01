{
    "testCases": [
        {
            "group": "walkExceptionWhenExecutionContextIsNull",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
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
                    "description": "When shouldValidateSchema is true and executionContext is null, the call to validate(...) should throw a NullPointerException."
                }
            ]
        },
        {
            "group": "walkExceptionWhenNodeIsNull",
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
                    "description": "When shouldValidateSchema is true and the node argument is null, validate(...) is expected to raise a NullPointerException."
                }
            ]
        },
        {
            "group": "walkExceptionWhenRootNodeIsNull",
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
                            "value": "null"
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
                    "description": "When shouldValidateSchema is true and rootNode is null, validate(...) should throw a NullPointerException."
                }
            ]
        },
        {
            "group": "walkExceptionWhenInstanceLocationIsNull",
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
                            "value": "null"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true and instanceLocation is null, the validate(...) call is expected to raise a NullPointerException."
                }
            ]
        },
        {
            "group": "walkExceptionWhenValidateReturnsNull",
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
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true and the overridden validate(..., true) method returns null, addAll(null) triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "walkExceptionWhenSchemasListIsNull",
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
                    "description": "When shouldValidateSchema is false and the internal `schemas` field is null, iterating over it throws a NullPointerException."
                }
            ]
        },
        {
            "group": "walkExceptionWhenSchemasContainNullElement",
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
                    "description": "When shouldValidateSchema is false and the `schemas` list contains a null entry, invoking `null.walk(...)` results in a NullPointerException."
                }
            ]
        },
        {
            "group": "walkExceptionWhenChildSchemaWalkThrowsRuntimeException",
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
                    "expected": "RuntimeException",
                    "description": "When shouldValidateSchema is false and a child schema's walk method throws a RuntimeException, OneOfValidator.walk should propagate that exception."
                }
            ]
        }
    ]
}