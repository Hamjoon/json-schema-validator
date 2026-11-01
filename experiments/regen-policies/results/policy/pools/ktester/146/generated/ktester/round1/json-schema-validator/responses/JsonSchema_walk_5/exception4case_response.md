{
    "test_cases": [
        {
            "group": "Malformed URL‑encoded input string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "%E0%A4%A"
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
                    "expected": "throws IllegalArgumentException",
                    "description": "The walk method URL‑decodes the input before deserialization. An input with an incomplete or illegal percent‑encoding sequence causes URLDecoder to throw IllegalArgumentException, which should propagate out of walk."
                }
            ]
        },
        {
            "group": "Invalid $ref in schema leading to schema resolution failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"John\"}"
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
                    "expected": "throws JsonSchemaException",
                    "description": "When the JsonSchema instance contains a $ref that cannot be resolved (e.g., points to a non‑existent definition), the walk execution attempts to resolve the reference during validation and throws a JsonSchemaException. The test should construct such a schema and verify the exception is propagated."
                }
            ]
        },
        {
            "group": "Unsupported character set during URL decoding (simulated)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "%7B%22key%22%3A%22value%22%7D"
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
                    "expected": "throws UnsupportedEncodingException",
                    "description": "If the implementation of walk uses URLDecoder.decode(String, charsetName) with a charset that is not available on the platform, an UnsupportedEncodingException is thrown. This test forces that path (e.g., by mocking the decoder or configuring the library to use a non‑standard charset) and verifies the exception is propagated."
                }
            ]
        }
    ]
}