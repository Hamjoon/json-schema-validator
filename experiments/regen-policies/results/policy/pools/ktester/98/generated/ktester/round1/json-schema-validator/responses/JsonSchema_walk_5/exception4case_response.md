{
    "newTestCases": [
        {
            "group": "Malformed YAML input – validation enabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name: John\nage: 30:\n"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.failure with a parsing error message (e.g., \"while scanning for the next token\" or \"could not find expected ':'\")",
                    "description": "The YAML payload is syntactically incorrect (extra colon). The deserialize step should throw a YAML parsing exception that is captured and reported in the ValidationResult."
                }
            ]
        },
        {
            "group": "Invalid percent‑encoding in input string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "%GG%ZZ"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "IllegalArgumentException (or ValidationResult.failure wrapping it) caused by URLDecoder when encountering malformed percent‑encoding",
                    "description": "The input contains malformed percent‑encoded sequences. During deserialization the code calls URLDecoder.decode, which throws IllegalArgumentException for invalid hex digits. The test verifies that this exception is propagated as a failure."
                }
            ]
        },
        {
            "group": "Invalid JSON escape sequence",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"path\":\"C:\\\\temp\\\\file\\x\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.failure with a parsing error message indicating an illegal escape character (e.g., \"Invalid escape sequence: \\x\")",
                    "description": "The JSON string contains an illegal escape sequence (\\x). The Jackson parser throws a JsonParseException, which should be captured and reported as a validation failure."
                }
            ]
        }
    ]
}