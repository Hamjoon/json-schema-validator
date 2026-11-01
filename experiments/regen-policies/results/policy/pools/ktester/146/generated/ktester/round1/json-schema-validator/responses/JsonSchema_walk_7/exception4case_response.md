{
    "new_test_cases": [
        {
            "group": "UnsupportedEncodingException during deserialization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"name%3DJohn%26age%3D30\""
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
                    "expected": "throws java.io.UnsupportedEncodingException",
                    "description": "When the deserialization logic attempts to URL‑decode the input string using a charset that is not supported, an UnsupportedEncodingException should be propagated."
                }
            ]
        },
        {
            "group": "Empty input string – YAML format",
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
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (or ValidationResult containing error messages)",
                    "description": "An empty string is not valid YAML; the deserialize step should fail with a parsing exception."
                }
            ]
        },
        {
            "group": "Blank input string – YAML format (whitespace only)",
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
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws com.fasterxml.jackson.core.JsonParseException (or ValidationResult containing error messages)",
                    "description": "A string that contains only whitespace cannot be parsed as YAML; the method should raise a parsing exception."
                }
            ]
        },
        {
            "group": "Invalid enum constant for InputFormat",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "jsonSchema.createExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"name\\\":\\\"John\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.UNKNOWN"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "throws IllegalArgumentException",
                    "description": "Supplying an enum constant that is not recognised by the library should cause an IllegalArgumentException during deserialization."
                }
            ]
        }
    ]
}