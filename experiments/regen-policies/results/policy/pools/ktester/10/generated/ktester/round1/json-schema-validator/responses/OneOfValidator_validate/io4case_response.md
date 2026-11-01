{
    "newTestCases": [
        {
            "group": "No schemas registered – always OneOf error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true, schemas=[])"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(anyNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath(\"/path\")"
                        }
                    ],
                    "expected": "returns Set containing OneOf ValidationMessage (numberOfValidSchema == 0, childErrors empty)",
                    "description": "When the OneOfValidator has an empty schema list, the loop never increments numberOfValidSchema, so the validator must always produce a OneOf error regardless of the input node."
                }
            ]
        },
        {
            "group": "Walk enabled – schema returns errors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(walkEnabled=true, failFast=false, validationEnabled=true, schemas=[errorSchema])"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(invalidNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath(\"/path\")"
                        }
                    ],
                    "expected": "returns Set containing OneOf ValidationMessage and the child ValidationMessage(s) from errorSchema.walk",
                    "description": "With walk enabled, the validator calls schema.walk(). The mock walk returns a non‑empty error set, so numberOfValidSchema stays 0 and the final result contains the OneOf error plus the child errors."
                }
            ]
        },
        {
            "group": "Fail‑fast mode – exactly one matching schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(walkEnabled=false, failFast=true, validationEnabled=true, schemas=[validSchema])"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath(\"/path\")"
                        }
                    ],
                    "expected": "returns empty Set<ValidationMessage> (no exception, state.matchedNode true)",
                    "description": "Fail‑fast only throws when the OneOf condition fails. Here a single schema validates successfully, so the validator returns an empty error set even though failFast is true."
                }
            ]
        },
        {
            "group": "Walk enabled – validation disabled (state.isValidationEnabled = false)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(walkEnabled=true, failFast=false, validationEnabled=false, schemas=[validSchema])"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath(\"/path\")"
                        }
                    ],
                    "expected": "returns empty Set<ValidationMessage> (schema.walk called with validationEnabled=false, but still counts as valid, state.matchedNode true)",
                    "description": "When walk is enabled but the validator state reports validationEnabled = false, the schema.walk() is invoked with the flag false. The mock schema is set up to ignore validation when the flag is false and return an empty error set, leading to a successful OneOf validation."
                }
            ]
        },
        {
            "group": "Multiple schemas – first matches, second returns errors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true, schemas=[validSchema, errorSchema])"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath(\"/path\")"
                        }
                    ],
                    "expected": "returns Set containing OneOf ValidationMessage and child ValidationMessage(s) from errorSchema (numberOfValidSchema == 1, but childErrors from second schema are added)",
                    "description": "The first schema validates without errors, incrementing numberOfValidSchema to 1. The second schema produces errors; those errors are collected in childErrors and returned together with the OneOf error because exactly one schema must match, but additional errors from non‑matching schemas are also reported."
                }
            ]
        },
        {
            "group": "State.matchedNode reset between schemas – first matches but resets to false before second",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true, schemas=[validSchema, resetMatchedNodeSchema])"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockJsonNodePath(\"/path\")"
                        }
                    ],
                    "expected": "returns Set containing OneOf ValidationMessage (numberOfValidSchema == 0 after second schema resets matchedNode)",
                    "description": "The first schema validates successfully and leaves state.matchedNode true, but the second schema explicitly sets state.matchedNode to false (simulated via mock). Because the loop continues, the first successful match is discarded, resulting in zero valid schemas and a OneOf error."
                }
            ]
        }
    ]
}