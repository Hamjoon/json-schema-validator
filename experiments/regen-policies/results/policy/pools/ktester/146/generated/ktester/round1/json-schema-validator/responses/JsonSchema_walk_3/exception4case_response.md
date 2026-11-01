{
    "groups": [
        {
            "group": "walk_invalidRefThrowsIOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "{\"refField\":\"any\"}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IOException (or JsonSchemaException wrapping IOException) when resolving a $ref that points to a non‑existent external schema file",
                    "description": "The JsonSchema instance contains a $ref to an external resource that cannot be read (e.g., file not found). Calling walk should attempt to resolve the reference and fail with an IOException."
                }
            ]
        },
        {
            "group": "walk_circularRefThrowsJsonSchemaException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "{\"a\":1}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalStateException) indicating a circular $ref detected",
                    "description": "The schema used by the JsonSchema instance contains a circular $ref (e.g., \"#/definitions/self\" referring back to itself). The walk method should detect the cycle and throw a JsonSchemaException rather than entering infinite recursion."
                }
            ]
        },
        {
            "group": "walk_unknownKeywordThrowsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "{\"name\":\"John\"}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException stating that an unsupported schema keyword was encountered",
                    "description": "The schema definition includes a non‑standard keyword (e.g., \"unknownKeyword\": true). When walk processes the schema it should reject the unknown keyword and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "walk_schemaNodeNotObjectThrowsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "defaultExecutionContext"
                        },
                        {
                            "parameter": "node",
                            "value": "\"just a string\""
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException because the root schema node is not a JSON object (e.g., schema is a string or array)",
                    "description": "The JsonSchema instance was created from a schema where the root node is not an object (e.g., the schema itself is a JSON string). The walk method expects an object schema and should throw IllegalArgumentException."
                }
            ]
        }
    ]
}