{
    "test_cases": [
        {
            "group": "Partial stream IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "MockInputStream that returns a few bytes then throws IOException on subsequent read"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (the underlying IOException from the stream is wrapped)",
                    "description": "Simulates a stream that fails part‑way through reading to verify that the method propagates the IOException as a JsonSchemaException."
                }
            ]
        },
        {
            "group": "Unsupported InputFormat",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream containing a valid JSON schema (e.g., {\"type\":\"object\"})"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.UNKNOWN (a non‑standard enum value not handled by readTree)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown (readTree does not recognise the supplied InputFormat)",
                    "description": "Passes an InputFormat value that the factory does not support, expecting an IllegalArgumentException before any IOException handling."
                }
            ]
        },
        {
            "group": "Null schema node handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream that readTree is stubbed to return null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException is thrown (newJsonSchema receives a null JsonNode)",
                    "description": "Mocks readTree to return null, causing newJsonSchema to be invoked with a null schemaNode and resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "Invalid schema content causing newJsonSchema failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream containing syntactically valid JSON but semantically invalid schema (e.g., {\"type\":\"unknown\"})"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (newJsonSchema validates the node and throws due to unsupported type)",
                    "description": "Provides a JSON document that parses correctly but fails schema validation inside newJsonSchema, ensuring the exception propagates unchanged."
                }
            ]
        }
    ]
}