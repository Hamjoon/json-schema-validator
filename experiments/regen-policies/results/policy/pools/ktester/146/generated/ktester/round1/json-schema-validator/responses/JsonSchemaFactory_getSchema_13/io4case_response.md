{
    "groups": [
        {
            "group": "Error handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "MockInputStream that throws IOException on any read"
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
                    "expected": "JsonSchemaException is thrown (the original IOException is wrapped)",
                    "description": "Simulates an IOException while reading the schema stream to verify that the method translates it into a JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream containing valid JSON schema (e.g., {\"type\":\"object\"})"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (null InputFormat leads to parsing failure)",
                    "description": "Ensures that a null InputFormat is not accepted and results in an exception rather than undefined behaviour."
                }
            ]
        },
        {
            "group": "Invalid inputs",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "null"
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
                    "expected": "NullPointerException or JsonSchemaException is thrown",
                    "description": "Verifies that passing a null InputStream is handled gracefully (the method should not succeed)."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream containing empty content"
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
                    "expected": "JsonSchemaException is thrown (empty content cannot be parsed into a JsonNode)",
                    "description": "Checks behaviour when the stream is syntactically empty – parsing should fail."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream containing valid JSON schema (e.g., {\"type\":\"object\"})"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (JSON content cannot be parsed as YAML)",
                    "description": "Ensures that a mismatch between the declared InputFormat and the actual content results in a parsing error."
                }
            ]
        },
        {
            "group": "Custom configurations",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream containing a valid JSON schema with $ref (e.g., {\"$ref\":\"#/definitions/foo\",\"definitions\":{\"foo\":{\"type\":\"string\"}}})"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig with failFast=true"
                        }
                    ],
                    "expected": "A non‑null JsonSchema instance is returned (configuration is applied)",
                    "description": "Verifies that a non‑default SchemaValidatorsConfig (failFast enabled) does not prevent schema creation."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "InputStream containing a very large JSON schema (e.g., 10 000 nested objects)"
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
                    "expected": "A non‑null JsonSchema instance is returned (factory handles large payloads)",
                    "description": "Tests the method's ability to process a large schema without running out of memory or timing out."
                }
            ]
        }
    ]
}