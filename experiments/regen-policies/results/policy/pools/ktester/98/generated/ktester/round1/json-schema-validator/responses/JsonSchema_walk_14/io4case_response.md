{
    "groups": [
        {
            "group": "Custom ExecutionContextCustomizer that throws",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> { ctx.setAttribute(\"before\", true); throw new RuntimeException(\"customizer failure\"); }"
                        }
                    ],
                    "expected": "RuntimeException with message \"customizer failure\" is propagated and no ValidationResult is returned",
                    "description": "The customizer throws an exception after mutating the ExecutionContext. walk should not swallow the exception; it must be propagated to the caller."
                }
            ]
        },
        {
            "group": "Schema with unresolved $ref produces validation errors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"$ref\": \"#/components/schemas/Unknown\"\n}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.isValid == false and ValidationResult.getMessages() contains a message about an unresolved $ref",
                    "description": "The schema contains a $ref that cannot be resolved. walk should still deserialize the JSON but validation (against the meta‑schema) must fail, reporting the missing reference."
                }
            ]
        },
        {
            "group": "Valid empty schema {} returns a successful ValidationResult",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.isValid == true and ValidationResult.getMessages().isEmpty()",
                    "description": "An empty JSON object is a valid schema according to the JSON‑Schema meta‑schema. walk should return a successful result."
                }
            ]
        },
        {
            "group": "Schema with invalid \"type\" value fails validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{\"type\":\"invalidType\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.isValid == false and ValidationResult.getMessages() contains a message about an unsupported type value",
                    "description": "The meta‑schema defines a limited set of allowed values for the \"type\" keyword. Supplying an unsupported value should cause validation to fail."
                }
            ]
        },
        {
            "group": "URL‑encoded JSON input is correctly decoded before validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "%7B%22type%22%3A%22object%22%2C%22properties%22%3A%7B%22age%22%3A%7B%22type%22%3A%22integer%22%7D%7D%7D"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.isValid == true and ValidationResult.getMessages().isEmpty()",
                    "description": "The input string is URL‑encoded. walk should decode it (via URLDecoder) before deserialization, resulting in a valid schema and a successful ValidationResult."
                }
            ]
        },
        {
            "group": "YAML schema using anchors and aliases parses correctly",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "type: object\nproperties:\n  person: &personSchema\n    type: object\n    properties:\n      name:\n        type: string\n  employee:\n    <<: *personSchema\n    properties:\n      employeeId:\n        type: integer"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.isValid == true and ValidationResult.getMessages().isEmpty()",
                    "description": "The YAML schema uses an anchor (&personSchema) and an alias (<<: *personSchema). walk must correctly resolve these YAML features and validate the resulting schema successfully."
                }
            ]
        },
        {
            "group": "Validate flag false with a schema that would otherwise be invalid",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "{\"type\":\"invalid\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": false
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": null
                        }
                    ],
                    "expected": "ValidationResult.isValid == true and ValidationResult.getMessages().isEmpty()",
                    "description": "Even though the schema contains an illegal \"type\" value, the validate flag is false, so walk should skip validation and return a successful result."
                }
            ]
        }
    ]
}