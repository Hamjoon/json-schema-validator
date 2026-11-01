{
    "newTestCases": [
        {
            "group": "Whitespace‑Only Input String",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "   \t  \n  "
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing a parsing‑error message indicating that the content is empty or cannot be parsed.",
                    "description": "The input consists only of whitespace characters. After trimming, there is no JSON content, so deserialization should fail and a validation message about empty content is expected."
                }
            ]
        },
        {
            "group": "Input with UTF‑8 Byte Order Mark (BOM)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\uFEFF{\"name\":\"Alice\",\"age\":25}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "A JSON payload prefixed with the UTF‑8 BOM should be correctly recognized and stripped by the deserializer, resulting in successful validation."
                }
            ]
        },
        {
            "group": "Malformed URL‑Encoded JSON (invalid percent sequence)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "%7B%22name%22%3A%22Bob%22%7"   ,
                            "value": "%7B%22name%22%3A%22Bob%22%7"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED_JSON"
                        }
                    ],
                    "expected": "IllegalArgumentException (or a ValidationMessage indicating a URL‑decoding error).",
                    "description": "The percent‑encoding is incomplete (trailing ‘%7’). URL decoding should throw an exception which is surfaced as a validation error."
                }
            ]
        },
        {
            "group": "Non‑Encoded JSON with URL_ENCODED_JSON format",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"city\":\"Paris\",\"population\":2148000}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED_JSON"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "When the format indicates URL‑encoded JSON but the payload is plain JSON, the decoder simply returns the original string, allowing normal deserialization and validation."
                }
            ]
        },
        {
            "group": "JSON payload with InputFormat.YAML",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"product\":\"Book\",\"price\":12.99}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "Jackson’s YAML parser can also read pure JSON. Supplying JSON with the YAML format should therefore succeed."
                }
            ]
        },
        {
            "group": "YAML payload with InputFormat.JSON",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "title: \"Effective Java\"\nauthor: \"Joshua Bloch\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing a parsing‑error message because the YAML syntax is not valid JSON.",
                    "description": "Providing a YAML document while the format is set to JSON should cause a parsing failure and a corresponding validation message."
                }
            ]
        },
        {
            "group": "Valid JSON with Leading/Trailing Whitespace",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "  \n\t { \"status\" : \"ok\" , \"code\" : 200 } \t\n "
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "Whitespace surrounding a valid JSON document must be ignored by the deserializer, resulting in successful validation."
                }
            ]
        },
        {
            "group": "Array Root JSON (type‑mismatch)",
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
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing at least one message indicating a type mismatch (e.g., expected object but found array).",
                    "description": "The schema used by JsonSchema expects an object at the root. Supplying a JSON array should deserialize correctly but later produce a validation message about the wrong type."
                }
            ]
        },
        {
            "group": "Literal JSON null value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing a type‑mismatch message because the root node is a null literal, not an object.",
                    "description": "The string \"null\" is a valid JSON literal but does not satisfy a schema that expects an object, leading to a validation error."
                }
            ]
        },
        {
            "group": "Unicode Characters in JSON String Values",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"greeting\":\"こんにちは\",\"language\":\"Japanese\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "JSON strings may contain Unicode characters. The deserializer should handle UTF‑8 correctly and validation should succeed."
                }
            ]
        }
    ]
}