{
    "test_cases": [
        {
            "group": "walk_nullExecutionContext_shouldValidateTrue",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": null
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true and executionContext is null, the call to validate(...) is expected to throw a NullPointerException (validate implementation assumes a non‑null context)."
                }
            ]
        },
        {
            "group": "walk_nullNode_shouldValidateTrue",
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
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true and node is null, validate(...) will receive a null JsonNode and typically throws NullPointerException."
                }
            ]
        },
        {
            "group": "walk_nullRootNode_shouldValidateTrue",
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
                            "parameter": "rootNode",
                            "value": null
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is true and rootNode is null, validate(...) is expected to throw NullPointerException because the validator expects a non‑null root node."
                }
            ]
        },
        {
            "group": "walk_nullInstanceLocation_shouldValidateTrue",
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
                    "description": "When shouldValidateSchema is true and instanceLocation is null, validate(...) will receive a null JsonNodePath and typically throws NullPointerException."
                }
            ]
        },
        {
            "group": "walk_validateReturnsNull_shouldValidateTrue",
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
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If a subclass overrides validate() and returns null, walk() will call validationMessages.addAll(null) which throws NullPointerException."
                }
            ]
        },
        {
            "group": "walk_nullSchemaInList_shouldValidateFalse",
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
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When shouldValidateSchema is false and the internal schemas list contains a null element, the loop attempts to invoke schema.walk(...), resulting in NullPointerException."
                }
            ]
        },
        {
            "group": "walk_schemaWalkThrowsRuntimeException_shouldValidateFalse",
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
                            "parameter": "rootNode",
                            "value": "mockRootNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "When shouldValidateSchema is false and a schema in the list throws a RuntimeException during its walk() call, the exception propagates out of OneOfValidator.walk()."
                }
            ]
        }
    ]
}