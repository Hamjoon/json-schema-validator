{
    "newTestCases": [
        {
            "group": "Validate when shouldValidateSchema is true and schemas list is non‑empty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "nonNullJsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "nonNullJsonNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"/field\")"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "validate is invoked and its returned Set<ValidationMessage> (e.g., [{\"type\":\"anyOf\",\"message\":\"failed\"}]) is returned; none of the inner schemas' walk methods are called despite the list containing two mock schemas",
                    "description": "Ensures that the presence of inner schemas does not affect the short‑circuit path when validation is required; the validator must delegate to its own validate method and must not walk the contained schemas."
                }
            ]
        },
        {
            "group": "Skip validation when shouldValidateSchema is false and schemas list is non‑empty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "nonNullJsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "nonNullJsonNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"/field\")"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "validate is NOT invoked; each schema in the non‑empty schemas list (e.g., mockSchemaA and mockSchemaB) has its walk(executionContext, node, rootNode, instanceLocation, false) called exactly once; method returns Collections.emptySet()",
                    "description": "Verifies that when validation is disabled, the validator walks every inner schema with shouldValidateSchema set to false and ignores any messages those schemas might produce."
                }
            ]
        },
        {
            "group": "Node is null, shouldValidateSchema true, schemas list non‑empty",
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
                            "value": null
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"/field\")"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "validate is NOT invoked because node is null; each inner schema (e.g., mockSchemaX) has its walk called with shouldValidateSchema = false; method returns an empty Set",
                    "description": "Confirms that the guard `(shouldValidateSchema && node != null)` prevents validation when the node is null, even if validation was requested, and that the fallback walking of inner schemas still occurs."
                }
            ]
        },
        {
            "group": "Node is null, shouldValidateSchema false, schemas list non‑empty",
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
                            "value": null
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"/field\")"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": false
                        }
                    ],
                    "expected": "validate is NOT invoked; each inner schema (e.g., mockSchemaY) has its walk called with shouldValidateSchema = false; method returns Collections.emptySet()",
                    "description": "Ensures that when validation is disabled and the node is null, the validator still walks each inner schema with the flag false and returns an empty result set."
                }
            ]
        },
        {
            "group": "Validate returns non‑empty set while schemas list is non‑empty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "nonNullJsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "nonNullJsonNode"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.of(\"/field\")"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": true
                        }
                    ],
                    "expected": "validate returns a non‑empty Set<ValidationMessage> (e.g., [{\"type\":\"anyOf\",\"message\":\"first schema failed\"},{\"type\":\"anyOf\",\"message\":\"second schema failed\"}]); walk returns exactly that same Set; no inner schema.walk calls occur",
                    "description": "Checks that any messages produced by the validator's own validate method are propagated unchanged, regardless of the presence of inner schemas."
                }
            ]
        }
    ]
}