{
    "tests": [
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - error handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{invalid json\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (caused by IOException from malformed JSON)",
                    "description": "The supplied string is not valid JSON; ObjectMapper.readTree throws an IOException which should be wrapped in a JsonSchemaException by the method."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException is propagated",
                    "description": "When the schema argument itself is null, ObjectMapper.readTree throws IllegalArgumentException which is not caught by the method, so the exception should bubble up."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"   \""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (caused by IOException due to empty/whitespace JSON)",
                    "description": "A string containing only whitespace is not valid JSON; parsing fails and the method must wrap the IOException in a JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (caused by IOException due to empty input)",
                    "description": "An empty string cannot be parsed into a JsonNode; the method should convert the resulting IOException into a JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - configuration variations",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"age\\\":{\\\"type\\\":\\\"integer\\\"}}}\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig().setFailFast(true)"
                        }
                    ],
                    "expected": "Returns a non‑null JsonSchema instance; the config is stored inside the schema for later validation",
                    "description": "Pass a non‑default configuration (e.g., fail‑fast enabled) and verify that the method still creates a JsonSchema without throwing, ensuring the config object is accepted."
                },
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{\\\"type\\\":\\\"array\\\",\\\"items\\\":{\\\"type\\\":\\\"string\\\"}}\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "Returns a non‑null JsonSchema instance (array schema) even when config is null",
                    "description": "Validate that a valid JSON schema representing an array is correctly parsed and returned when the configuration argument is null."
                }
            ]
        }
    ]
}