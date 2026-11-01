{
    "groups": [
        {
            "group": "Valid YAML, validation enabled, no customizer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name: John\nage: 30"
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
                    "expected": "ValidationResult with isValid=true and no ValidationMessage; executionCustomizer not invoked",
                    "description": "Ensures that a well‑formed YAML payload is correctly deserialized, validated against the schema, and that the method works when no Consumer is supplied."
                }
            ]
        },
        {
            "group": "Valid JSON, validation disabled, customizer provided",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"city\":\"Paris\"}"
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
                            "value": "ctx -> ctx.setAttribute(\"skip\", true)"
                        }
                    ],
                    "expected": "ValidationResult with isValid=true (no validation performed) and empty messages; executionCustomizer must be invoked (attribute \"skip\" set to true)",
                    "description": "When validation is turned off the Consumer should still be executed, allowing callers to modify the ExecutionContext even though the schema is not checked."
                }
            ]
        },
        {
            "group": "Empty JSON string triggers deserialization exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": ""
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
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (or wrapped ValidationException) and returns ValidationResult with isValid=false and a parsing error message",
                    "description": "An empty string is not valid JSON; the method must propagate the parsing failure from deserialize()."
                }
            ]
        },
        {
            "group": "Whitespace‑only JSON triggers deserialization exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "   \t\n"
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
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (or wrapped ValidationException) and returns ValidationResult with isValid=false and a parsing error message",
                    "description": "A string containing only whitespace should be treated as invalid JSON and cause a parsing exception."
                }
            ]
        },
        {
            "group": "Valid URL_ENCODED input, validation enabled, no customizer",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name%3DJohn%26age%3D30"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
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
                    "expected": "ValidationResult with isValid=true and no ValidationMessage; the URL‑encoded string is correctly decoded to a JSON object {\"name\":\"John\",\"age\":30}",
                    "description": "Confirms that the URL_ENCODED format is decoded, transformed into a JsonNode, and validated when the payload is well‑formed."
                }
            ]
        },
        {
            "group": "JSON array input fails schema validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "[1, 2, 3]"
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
                    "expected": "ValidationResult with isValid=false and at least one ValidationMessage indicating type mismatch (schema expects an object)",
                    "description": "The schema associated with the JsonSchema instance expects a JSON object; providing a JSON array should cause validation to fail."
                }
            ]
        },
        {
            "group": "Consumer sets custom attribute, validation enabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"id\":10}"
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
                            "value": "ctx -> ctx.setAttribute(\"requestId\", \"abc-123\")"
                        }
                    ],
                    "expected": "ValidationResult with isValid=true and no ValidationMessage; executionCustomizer must be invoked and the attribute \"requestId\" set to \"abc-123\" in the ExecutionContext",
                    "description": "Verifies that a Consumer can enrich the ExecutionContext before validation runs and that the enrichment is observable after the walk call."
                }
            ]
        },
        {
            "group": "No‑op Consumer, validation enabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"status\":\"ok\"}"
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
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "ValidationResult with isValid=true and no ValidationMessage; executionCustomizer is called but makes no changes",
                    "description": "Ensures that a Consumer that performs no operation does not interfere with normal validation flow."
                }
            ]
        }
    ]
}