{
    "test_cases": [
        {
            "group": "Invalid JSON input – deserialization failure with validation disabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"bad\\\": [1,2,}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "false"
                        }
                    ],
                    "expected": "throws com.fasterxml.jackson.core.JsonProcessingException (or a subclass) from deserialize",
                    "description": "Even when validation is disabled, the method must first deserialize the payload. An ill‑formed JSON string should cause a JsonProcessingException to be propagated."
                }
            ]
        },
        {
            "group": "Invalid YAML input – deserialization failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"name: John\\nage: [1, 2\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws com.fasterxml.jackson.core.JsonProcessingException (or a subclass) from deserialize",
                    "description": "A malformed YAML document should trigger a deserialization error regardless of the validate flag."
                }
            ]
        },
        {
            "group": "Empty string input – deserialization failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws com.fasterxml.jackson.core.JsonProcessingException (or a subclass) because an empty string is not valid JSON",
                    "description": "An empty payload cannot be parsed into a JsonNode; the method should surface the parsing exception."
                }
            ]
        },
        {
            "group": "Whitespace‑only input – deserialization failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"   \\n\\t  \""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws com.fasterxml.jackson.core.JsonProcessingException (or a subclass) because whitespace alone is not a valid JSON document",
                    "description": "Even though the string contains only whitespace, Jackson treats it as invalid JSON and throws an exception."
                }
            ]
        },
        {
            "group": "Null ExecutionContext – argument validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"key\\\":\\\"value\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws NullPointerException (or IllegalArgumentException) because executionContext is required",
                    "description": "The method expects a non‑null ExecutionContext; passing null should result in an immediate exception before deserialization."
                }
            ]
        },
        {
            "group": "Valid YAML input without validation (validate = false)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"name: Alice\\nage: 30\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "validate",
                            "value": "false"
                        }
                    ],
                    "expected": "ValidationResult with isSuccess() == true (no ValidationMessage entries) – validation is skipped but walk completes",
                    "description": "When validation is disabled, walk should still deserialize the YAML and traverse the schema tree without producing validation messages."
                }
            ]
        },
        {
            "group": "Valid JSON literal null input – walk succeeds",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"null\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult with isSuccess() == true (or no ValidationMessage entries) – the JSON literal null is deserialized to a NullNode and walked without errors",
                    "description": "A JSON document that is the literal `null` should be handled gracefully; deserialization yields a NullNode which the walk algorithm can process."
                }
            ]
        },
        {
            "group": "Valid JSON array input – walk succeeds",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"[\\\"a\\\", \\\"b\\\", \\\"c\\\"]\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult with isSuccess() == true (no ValidationMessage entries) – array nodes are supported and should not cause errors in a permissive schema",
                    "description": "Ensures that the walk method correctly handles a root JSON array after successful deserialization."
                }
            ]
        },
        {
            "group": "Valid JSON number input – walk succeeds",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"12345\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult with isSuccess() == true (no ValidationMessage entries) – numeric root nodes are deserialized to NumericNode and walked without issues",
                    "description": "Verifies that a simple numeric JSON document is processed correctly by walk."
                }
            ]
        },
        {
            "group": "Valid JSON empty object – walk succeeds",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "JsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "ValidationResult with isSuccess() == true (no ValidationMessage entries) – an empty object should be accepted by a schema that does not require properties",
                    "description": "Confirms that a minimal valid JSON object does not cause unexpected validation failures."
                }
            ]
        }
    ]
}