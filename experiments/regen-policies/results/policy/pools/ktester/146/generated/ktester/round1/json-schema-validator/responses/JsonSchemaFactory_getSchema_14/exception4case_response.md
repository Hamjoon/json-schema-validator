{
    "groups": [
        {
            "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - null internal URI in SchemaLocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(null)"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws NullPointerException because the underlying URI inside SchemaLocation is null",
                    "description": "A SchemaLocation constructed with a null URI should cause an immediate NPE when getSchema attempts to resolve the location."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - unsupported URI scheme",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(URI.create(\"ftp://example.com/schema.json\"))"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws IllegalArgumentException (or UnsupportedOperationException) because the factory does not support the ftp scheme",
                    "description": "Supply a SchemaLocation with a scheme that the factory cannot handle; it should reject the URI before attempting to load the schema."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - jsonNode is not an object node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(URI.create(\"classpath:/valid-schema.json\"))"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"[1,2,3]\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws IllegalArgumentException because a JSON Schema must be a JSON object node",
                    "description": "Pass a JsonNode representing a JSON array; the factory should detect that the root node is not an object and reject it."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - schema resource contains malformed JSON",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(URI.create(\"classpath:/malformed-schema.json\"))"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws IOException (or JsonProcessingException) when the factory attempts to parse the malformed content",
                    "description": "The classpath resource exists but its content is not valid JSON/YAML; loading it should raise a parsing exception."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - invalid fragment identifier",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(URI.create(\"classpath:/valid-schema.json#/nonexistent\"))"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws IllegalArgumentException (or JsonPointerException) because the fragment points to a non‑existent sub‑schema",
                    "description": "When a fragment is supplied, the factory resolves it against the loaded schema; an invalid pointer should cause an exception."
                }
            ]
        }
    ]
}