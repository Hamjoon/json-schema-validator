{
    "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - normal flow",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()" 
                }
            ],
            "expected": "Returns a non‑null JsonSchema instance whose internal schemaNode matches the supplied JSON structure.",
            "description": "Valid JSON string is parsed successfully; the method should create and return a JsonSchema object without throwing any exception."
        },
        {
            "input": [
                {
                    "parameter": "schema",
                    "value": "{}"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "Returns a non‑null JsonSchema instance (empty schema).",
            "description": "Even when the configuration object is null and the JSON string represents an empty object, the method should still parse the JSON and return a JsonSchema."
        }
    ]
}