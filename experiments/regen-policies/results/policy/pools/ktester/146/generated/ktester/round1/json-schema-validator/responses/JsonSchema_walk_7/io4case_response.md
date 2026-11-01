{
    "newTestCases": [
        {
            "group": "Validate flag false with valid YAML",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"name: John\\nage: 30\""
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
                    "expected": "ValidationResult with no validation messages (isSuccess == true)",
                    "description": "Walk a well‑formed YAML string with validation disabled – deserialization should succeed and the result should be successful even though schema validation is skipped."
                }
            ]
        },
        {
            "group": "Null ExecutionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
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
                    "expected": "throws NullPointerException",
                    "description": "Passing a null ExecutionContext should cause a NullPointerException before deserialization or validation is performed."
                }
            ]
        },
        {
            "group": "Null InputFormat",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws NullPointerException or IllegalArgumentException",
                    "description": "When the InputFormat argument is null the method cannot decide how to deserialize; it should fail with a clear exception."
                }
            ]
        },
        {
            "group": "Empty input string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
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
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (or ValidationResult containing error messages)",
                    "description": "An empty string is not valid JSON; deserialization should throw a parsing exception."
                }
            ]
        },
        {
            "group": "Blank input string (whitespace only)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"   \\n\\t\""
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
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (or ValidationResult containing error messages)",
                    "description": "A string that contains only whitespace cannot be parsed as JSON; the method should raise a parsing exception."
                }
            ]
        },
        {
            "group": "Valid JSON array as root node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"[1,2,3]\""
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
                    "expected": "ValidationResult with no validation messages (isSuccess == true)",
                    "description": "Walk a valid JSON array; the schema may allow any type at root, so deserialization succeeds and validation passes."
                }
            ]
        },
        {
            "group": "Invalid YAML syntax",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"name: John\\nage 30\""
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
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (or ValidationResult containing error messages)",
                    "description": "Malformed YAML (missing colon) should cause the deserialization step to fail with a parsing exception."
                }
            ]
        },
        {
            "group": "Valid JSON with Unicode characters",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"text\\\":\\\"\\u00A9\\u20AC\\\"}\""
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
                    "expected": "ValidationResult with no validation messages (isSuccess == true)",
                    "description": "JSON containing Unicode escape sequences should be deserialized correctly and validated successfully."
                }
            ]
        }
    ]
}