{
    "testCases": [
        {
            "group": "testGetSchemaWithValidNodeAndConfig",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance whose schemaNode equals the supplied JsonNode and whose meta‑schema is the default (draft‑07)",
                    "description": "A typical valid JSON‑Schema node is supplied together with an explicit, default‑configured SchemaValidatorsConfig. The factory should return a usable JsonSchema object without throwing."
                }
            ]
        },
        {
            "group": "testGetSchemaWithValidNodeAndNullConfig",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance (default configuration is applied internally)",
                    "description": "When the configuration argument is null the factory must fall back to its built‑in default configuration and still produce a JsonSchema."
                }
            ]
        },
        {
            "group": "testGetSchemaWithEmptyObjectNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ }"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance representing an empty schema (accepts any JSON value)",
                    "description": "An empty JSON object is a valid schema (equivalent to {}); the factory should create a JsonSchema that validates everything."
                }
            ]
        },
        {
            "group": "testGetSchemaWithNullJsonNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null JsonNode is illegal; the method is expected to throw a NullPointerException rather than returning a schema."
                }
            ]
        },
        {
            "group": "testGetSchemaWithInvalidSchemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"type\": \"unknownType\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "JsonSchemaException (or a subclass) indicating an unsupported type keyword",
                    "description": "If the supplied JsonNode contains an invalid or unknown keyword/value combination, the factory should raise a JsonSchemaException during schema creation."
                }
            ]
        },
        {
            "group": "testGetSchemaWithMetaSchemaReference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"string\" }"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "non‑null JsonSchema instance whose meta‑schema is draft‑04",
                    "description": "When the schema node explicitly declares a $schema URI, the factory must resolve and use the referenced meta‑schema (draft‑04) instead of the default."
                }
            ]
        }
    ]
}