{
    "test_cases": [
        {
            "group": "MalformedYamlThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name: John\nage: : thirty"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "com.fasterxml.jackson.core.JsonParseException",
                    "description": "A syntactically invalid YAML document should cause the deserialization step to throw a JsonParseException which propagates out of validate."
                }
            ]
        },
        {
            "group": "InvalidUrlEncodingThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "%GG"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the input string contains malformed percent‑encoding, URLDecoder.decode (used inside deserialize) throws IllegalArgumentException which should be propagated by validate."
                }
            ]
        },
        {
            "group": "CustomOutputFormatThrowsRuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"valid\":true}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "ExceptionThrowingOutputFormat"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "When a user‑supplied OutputFormat throws a RuntimeException during its apply/format method, validate must let the exception bubble up unchanged."
                }
            ]
        }
    ]
}