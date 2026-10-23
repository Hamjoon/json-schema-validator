{
    "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - Normal Flow",
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
            "expected": "Returns a non‑null JsonSchema instance whose internal JsonNode matches the parsed schema",
            "description": "Valid JSON string is supplied together with a default configuration. The method should parse the string without error and delegate to newJsonSchema, returning a JsonSchema object."
        }
    ]
}