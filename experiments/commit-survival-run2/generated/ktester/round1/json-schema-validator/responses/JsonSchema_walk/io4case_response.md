{
    "testCases": [
        {
            "group": "walk_successfulValidation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created via JsonSchema.createExecutionContext() for a simple object schema {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"name\":\"John\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.success == true and ValidationResult.getMessages() is empty",
                    "description": "Valid JSON node matches the schema; walk should return a successful ValidationResult with no error messages."
                }
            ]
        },
        {
            "group": "walk_failureDueToTypeMismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"age\":\"twenty\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.success == false and ValidationResult.getMessages() contains a message with keyword 'type'",
                    "description": "The node provides a string where the schema expects an integer; walk should report a type‑mismatch validation error."
                }
            ]
        },
        {
            "group": "walk_missingRequiredProperty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"string\"}}}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {} (empty object)"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.success == false and ValidationResult.getMessages() contains a message with keyword 'required'",
                    "description": "The required property 'id' is missing; walk should return a ValidationResult indicating the required‑property violation."
                }
            ]
        },
        {
            "group": "walk_noValidationWhenFlagFalse",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for the same schema as in walk_missingRequiredProperty"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {} (empty object)"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "false"
                        }
                    ],
                    "expected": "ValidationResult.success == true and ValidationResult.getMessages() is empty",
                    "description": "When shouldValidateSchema is false the walk should skip validation logic, therefore even an invalid node yields a successful result."
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
                            "value": "ExecutionContext created for any valid schema"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException is thrown",
                    "description": "Passing a null JsonNode should cause the method to throw a NullPointerException (guard clause validation)."
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
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"any\":\"value\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown",
                    "description": "A null ExecutionContext is not allowed; the method should reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "walk_schemaWithExternalRef",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"$ref\":\"#/definitions/person\" ,\"definitions\":{\"person\":{\"type\":\"object\",\"properties\":{\"firstName\":{\"type\":\"string\"}}}}}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"firstName\":\"Alice\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.success == true and no messages",
                    "description": "The schema contains a $ref that resolves to an internal definition; walk must resolve the reference and validate the node successfully."
                }
            ]
        },
        {
            "group": "walk_dynamicAnchorResolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "ExecutionContext created for schema {\"$dynamicAnchor\":\"myAnchor\",\"type\":\"object\",\"properties\":{\"code\":{\"type\":\"string\"}}}"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNode representing {\"code\":\"XYZ\"}"
                        },
                        {
                            "parameter": "shouldValidateSchema",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult.success == true and no messages",
                    "description": "Schema uses $dynamicAnchor; walk should treat it like a normal schema and validate the node without errors."
                }
            ]
        }
    ]
}