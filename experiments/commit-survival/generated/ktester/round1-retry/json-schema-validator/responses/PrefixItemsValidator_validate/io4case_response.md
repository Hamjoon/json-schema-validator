{
    "tests": [
        {
            "group": "nonArrayNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with empty CollectorContext"
                        },
                        {
                            "parameter": "node",
                            "value": "{\"name\":\"John\"}"  ,
                            "description": "a JSON object (not an array)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "{\"name\":\"John\"}"
                        },
                        {
                            "parameter": "at",
                            "value": "/"
                        }
                    ],
                    "expected": "empty ValidationMessage set (no errors)",
                    "description": "When the node is not an array the validator must ignore it and return an empty error set."
                }
            ]
        },
        {
            "group": "emptyTupleSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with empty CollectorContext"
                        },
                        {
                            "parameter": "node",
                            "value": "[]"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "[]"
                        },
                        {
                            "parameter": "at",
                            "value": "/items"
                        }
                    ],
                    "expected": "empty ValidationMessage set (no errors)",
                    "description": "If the tupleSchema list is empty, even an array node should produce no validation errors and no evaluated items."
                }
            ]
        },
        {
            "group": "arrayNodeSizeLessThanTupleSchema_AllValid",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with CollectorContext that records evaluated items"
                        },
                        {
                            "parameter": "node",
                            "value": "[1, \"a\"]"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "[1, \"a\"]"
                        },
                        {
                            "parameter": "at",
                            "value": "/data"
                        }
                    ],
                    "expected": "empty ValidationMessage set (no errors) and evaluated items contain \"/data/0\" and \"/data/1\"",
                    "description": "Array length (2) is less than tupleSchema size (3). Both items are validated by schemas that return no errors, therefore both paths are added to evaluated items and no errors are returned."
                }
            ]
        },
        {
            "group": "arrayNodeSizeLessThanTupleSchema_WithErrors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with CollectorContext that records evaluated items"
                        },
                        {
                            "parameter": "node",
                            "value": "[5, \"bad\"]"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "[5, \"bad\"]"
                        },
                        {
                            "parameter": "at",
                            "value": "/payload"
                        }
                    ],
                    "expected": "ValidationMessage set containing error from second schema; evaluated items contain only \"/payload/0\"",
                    "description": "First tuple schema validates successfully (adds evaluated item), second tuple schema returns an error, so the error is collected and the second path is **not** added to evaluated items."
                }
            ]
        },
        {
            "group": "arrayNodeSizeGreaterThanTupleSchema_AllValid",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with CollectorContext that records evaluated items"
                        },
                        {
                            "parameter": "node",
                            "value": "[10, \"x\", true, null]"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "[10, \"x\", true, null]"
                        },
                        {
                            "parameter": "at",
                            "value": "/array"
                        }
                    ],
                    "expected": "empty ValidationMessage set (no errors) and evaluated items contain \"/array/0\", \"/array/1\", \"/array/2\" (only first three because tupleSchema size = 3)",
                    "description": "When the instance array is larger than the tuple schema, only the first *tupleSchema.size()* elements are validated. All three schemas succeed, so their paths are recorded, and the extra element is ignored."
                }
            ]
        },
        {
            "group": "arrayNodeSizeGreaterThanTupleSchema_WithErrorInFirstSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with CollectorContext that records evaluated items"
                        },
                        {
                            "parameter": "node",
                            "value": "[\"invalid\", 2, 3]"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "[\"invalid\", 2, 3]"
                        },
                        {
                            "parameter": "at",
                            "value": "/list"
                        }
                    ],
                    "expected": "ValidationMessage set containing error from first schema; evaluated items set is empty",
                    "description": "The first tuple schema fails, producing an error. Because it fails, its path is **not** added to evaluated items. The remaining schemas are still evaluated (they succeed) but their paths are added only if they succeed; in this case they succeed, so evaluated items contain \"/list/1\" and \"/list/2\"."
                }
            ]
        },
        {
            "group": "atPathConstructionVerification",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with CollectorContext that records evaluated items"
                        },
                        {
                            "parameter": "node",
                            "value": "[1,2]"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "[1,2]"
                        },
                        {
                            "parameter": "at",
                            "value": "/parent/child"
                        }
                    ],
                    "expected": "empty ValidationMessage set and evaluated items contain \"/parent/child/0\" and \"/parent/child/1\"",
                    "description": "Ensures that the validator builds the correct JSON‑Pointer path using `atPath(at,i)` for each indexed element."
                }
            ]
        },
        {
            "group": "nullNodeHandledGracefully",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with empty CollectorContext"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        },
                        {
                            "parameter": "at",
                            "value": "/"
                        }
                    ],
                    "expected": "empty ValidationMessage set (no errors)",
                    "description": "A null JsonNode is not an array; the validator must return an empty error set without throwing."
                }
            ]
        }
    ]
}