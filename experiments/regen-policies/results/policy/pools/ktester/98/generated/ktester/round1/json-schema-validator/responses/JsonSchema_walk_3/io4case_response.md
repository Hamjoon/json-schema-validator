{
    "testCases": [
        {
            "group": "walk_validNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing a valid instance according to the schema (e.g., {\"name\":\"John\",\"age\":30})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true and no ValidationMessage in result",
                    "description": "When a non‑null ExecutionContext and a JSON node that conforms to the schema are supplied with validate=true, walk should return a successful ValidationResult with no error messages."
                }
            ]
        },
        {
            "group": "walk_invalidNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing an invalid instance (e.g., {\"name\":123,\"age\":\"thirty\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and at least one ValidationMessage describing the schema violations",
                    "description": "When the instance does not satisfy the schema constraints, walk should return a ValidationResult marked as invalid and contain appropriate validation messages."
                }
            ]
        },
        {
            "group": "walk_validateFlagFalse",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing any JSON (valid or invalid) – e.g., {\"any\":\"value\"}"
                        },
                        {
                            "parameter": "validate",
                            "value": false
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true and no ValidationMessage, regardless of node content",
                    "description": "When validate flag is false, walk should skip schema validation and always return a successful ValidationResult."
                }
            ]
        },
        {
            "group": "walk_nullNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": null
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and a ValidationMessage indicating that the instance node is missing or null",
                    "description": "Passing a null JsonNode should cause walk to treat it as an invalid instance and return an appropriate error message."
                }
            ]
        },
        {
            "group": "walk_nullExecutionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": null
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing a valid instance (e.g., {\"name\":\"Alice\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "Throws NullPointerException (or IllegalArgumentException) because ExecutionContext is required",
                    "description": "The method expects a non‑null ExecutionContext; providing null should result in an exception rather than a ValidationResult."
                }
            ]
        },
        {
            "group": "walk_recursiveAnchor_true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext with recursive anchor enabled (e.g., schema.isRecursiveAnchor() == true)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode that references itself via $recursiveRef (e.g., {\"$recursiveRef\":\"#\"})"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true and no ValidationMessage, confirming that recursive anchors are processed without infinite recursion",
                    "description": "When the schema defines a recursive anchor, walk must correctly resolve $recursiveRef without stack overflow and validate the node successfully."
                }
            ]
        },
        {
            "group": "walk_schemaResourceRoot",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext() for a schema that is a resource root (schema.isSchemaResourceRoot() == true)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing a valid instance for the resource‑root schema"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true, confirming that walk works when the schema is marked as a resource root",
                    "description": "Ensures that walk correctly handles schemas flagged as resource roots, which may affect resolution of relative $ref URIs."
                }
            ]
        },
        {
            "group": "walk_withCustomExecutionContextCustomizer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing a valid instance"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true; the customizer (if any) is invoked without affecting validation outcome",
                    "description": "Although the focal overload does not accept a customizer directly, the internal walkAtNodeInternal may receive a null customizer; this test confirms that the default path works and that providing a customizer via the overload that accepts it does not change the result."
                }
            ]
        }
    ]
}