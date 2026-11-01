{
    "tests": [
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - malformed JSON",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing '{\"type\":\"object\"' (missing closing brace)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown with cause of JsonParseException",
                    "description": "The InputStream contains invalid JSON; the method should catch the IOException from the mapper and re‑throw it as a JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - null InputStream",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (NullPointerException as cause)",
                    "description": "Passing a null InputStream should cause the underlying ObjectMapper to throw a NullPointerException, which is wrapped in a JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - empty InputStream",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing '' (empty string)"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown (cause: EOFException or JsonParseException)",
                    "description": "An empty stream cannot be parsed into a JsonNode; the method should propagate the parsing error as a JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - IOException from stream",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "Custom InputStream that throws IOException on read()"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException is thrown with the original IOException as cause",
                    "description": "Simulate a failure while reading the stream (e.g., network error). The method must catch the IOException and wrap it in JsonSchemaException."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - custom configuration flags",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing '{\"type\":\"string\",\"format\":\"date-time\"}'"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig with typeLoose=true, failFast=true"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance; the schema respects the supplied config (e.g., validation will be type‑loose and fail‑fast)",
                    "description": "Provide a non‑null configuration that modifies validator behavior; the factory should still create a JsonSchema instance without error."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - large schema payload",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing a JSON schema with 10,000 properties (generated programmatically)"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance (schemaNode has 10,000 properties)",
                    "description": "Validate that the method can handle a large schema without running out of memory or throwing unexpected exceptions."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) - schema with $ref",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaStream",
                            "value": "ByteArrayInputStream containing '{\"$ref\":\"#/definitions/foo\",\"definitions\":{\"foo\":{\"type\":\"integer\"}}}'"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance; internal schemaNode contains a $ref node",
                    "description": "Ensure that schemas containing $ref are parsed correctly; the factory should not resolve the reference but must retain it in the JsonNode."
                }
            ]
        }
    ]
}