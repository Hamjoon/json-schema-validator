{
    "tests": [
        {
            "group": "OneOfValidator.validate - array node (no subschema matches)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing [\"A\"]"
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
                    "expected": "Set containing a ValidationMessage with code \"oneOf\" indicating that no subschema matches",
                    "description": "The instance is an array, while all subschemas expect an object. The validator must report a oneOf error because none of the subschemas validate the array."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - empty object (none matches)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
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
                    "expected": "Set containing a ValidationMessage with code \"oneOf\" indicating that no subschema matches",
                    "description": "An empty object does not satisfy any of the defined subschemas (e.g., each subschema requires a specific property). Validation should fail with a oneOf error."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - exactly one match despite extra unrelated property",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"type\":\"A\",\"foo\":\"bar\"}"
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
                    "expected": "empty Set (validation succeeds because only the first subschema matches; the extra property does not trigger another subschema)",
                    "description": "The instance matches the subschema that requires \"type\":\"A\". Another subschema in the oneOf list only matches objects that contain an \"extra\" field, which this instance lacks, so only one subschema matches and validation succeeds."
                }
            ]
        },
        {
            "group": "OneOfValidator.walk - short‑circuit enabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContextShortCircuitTrue"
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
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "empty Set (validation succeeds and walk short‑circuits after the first matching subschema)",
                    "description": "Calling the public `walk` method with `shouldValidateSchema=true` should delegate to the internal validation logic with `walk=true`. With `canShortCircuit()` returning true, the validator stops after the first matching subschema and returns success."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - nested instance location",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"inner\":{\"type\":\"A\"}}"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"inner\")"
                        }
                    ],
                    "expected": "empty Set (validation succeeds because exactly one subschema matches the nested object)",
                    "description": "The validator is invoked on a nested node (`inner`). The instance location should be reflected in any generated ValidationMessage. Since the nested object matches exactly one subschema, validation succeeds with no messages."
                }
            ]
        }
    ]
}