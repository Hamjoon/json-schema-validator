{
    "testCases": [
        {
            "group": "jsonNodeReader is null – edge cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown",
                    "description": "When jsonNodeReader is null and the InputStream is null, ObjectMapper.readTree(null) throws IllegalArgumentException; the method should propagate this exception."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing malformed JSON: {\"name\": \"John\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "JsonParseException (or IOException) is thrown",
                    "description": "When jsonNodeReader is null and the JSON text is malformed, the underlying ObjectMapper.readTree throws a parsing exception; the method must propagate the exception."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream that is empty (zero bytes)"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "JsonParseException (or EOFException) is thrown",
                    "description": "An empty InputStream does not contain a valid JSON document; ObjectMapper.readTree will raise a parsing exception which should be propagated."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing JSON text: {\"valid\":true}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown",
                    "description": "If inputFormat is null, getObjectMapper(null) is expected to throw NPE (or fallback) – the method should not swallow the exception."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing JSON text: {\"valid\":true}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "UNKNOWN"
                        }
                    ],
                    "expected": "IllegalArgumentException or custom exception indicating unsupported format",
                    "description": "When an unsupported InputFormat value is supplied while jsonNodeReader is null, the factory should reject it and propagate the resulting exception."
                }
            ]
        },
        {
            "group": "jsonNodeReader is not null – edge cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "YAML"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown",
                    "description": "Even when a custom JsonNodeReader is present, a null InputStream should cause the underlying reader to throw IllegalArgumentException, which must be propagated."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing malformed YAML: name: John: Doe"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "YAML"
                        }
                    ],
                    "expected": "IOException (or parsing exception) is thrown",
                    "description": "If JsonNodeReader attempts to parse malformed YAML, it will throw an IOException; the method must propagate it."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing JSON text: {\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown",
                    "description": "When jsonNodeReader is present but inputFormat is null, the JsonNodeReader implementation is expected to reject the null and throw NPE, which should be propagated."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing JSON text: {\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "UNSUPPORTED"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown",
                    "description": "If an unsupported InputFormat enum constant is passed to a non‑null JsonNodeReader, it should raise an exception that the method propagates."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing JSON text: {\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "null JsonNode is returned",
                    "description": "When JsonNodeReader.readTree returns null (e.g., custom implementation decides the content is empty), the method should simply return null without throwing."
                },
                {
                    "input": [
                        {
                            "parameter": "content",
                            "value": "InputStream containing JSON text: {\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "JSON"
                        }
                    ],
                    "expected": "RuntimeException is propagated",
                    "description": "If JsonNodeReader.readTree throws an unchecked exception (e.g., RuntimeException), the method must not catch it and should let it propagate."
                }
            ]
        }
    ]
}