{
    "test_cases": [
        {
            "group": "Exception – null executionContext",
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
                    "expected": "NullPointerException",
                    "description": "validate() dereferences executionContext (e.g., to obtain the schema factory). Passing a null executionContext should cause a NullPointerException."
                }
            ]
        },
        {
            "group": "Exception – null instanceLocation",
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
                    "expected": "NullPointerException",
                    "description": "The validator builds ValidationMessage objects using the instanceLocation. Supplying a null instanceLocation should trigger a NullPointerException."
                }
            ]
        },
        {
            "group": "Exception – null rootNode with non‑null node",
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
                    "expected": "NullPointerException",
                    "description": "During $ref resolution the validator may need the root node. Providing a null rootNode while the instance node is non‑null should cause a NullPointerException."
                }
            ]
        },
        {
            "group": "Exception – validator constructed with null subschemas list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (validator built with null subschemas list)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {}"
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
                    "expected": "IllegalStateException",
                    "description": "OneOfValidator expects a non‑null collection of subschemas. Supplying a null list at construction time should cause an IllegalStateException when validate() is invoked."
                }
            ]
        },
        {
            "group": "Exception – validator contains null subschema entry",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (oneOf list contains a null element)"
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
                    "expected": "NullPointerException",
                    "description": "When iterating over the subschemas, a null entry should cause a NullPointerException because the validator attempts to invoke validate() on it."
                }
            ]
        },
        {
            "group": "Exception – oneOf contains non‑schema element",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (oneOf array includes a plain string instead of a JsonSchema object)"
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
                    "expected": "IllegalArgumentException",
                    "description": "The validator expects each element of the oneOf array to be a JsonSchema. Supplying a non‑schema element (e.g., a raw string) should raise an IllegalArgumentException during validation setup."
                }
            ]
        },
        {
            "group": "Exception – circular $ref leads to SchemaException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (schema contains circular $ref between subschemas)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"id\":1}"
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
                    "expected": "SchemaException",
                    "description": "A circular $ref resolution should be detected and reported as a SchemaException (or a subclass) rather than causing infinite recursion or a StackOverflowError."
                }
            ]
        },
        {
            "group": "Exception – executionContext with null validatorFactory",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (validatorFactory is null)"
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
                    "expected": "NullPointerException",
                    "description": "During validation the ExecutionContext is used to obtain a JsonSchemaFactory. If the factory field is null, a NullPointerException should be thrown."
                }
            ]
        }
    ]
}