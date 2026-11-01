{
    "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - normal flow",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(URI.create(\"classpath:/valid-schema.json\"))"
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
            "expected": "returns a non‑null JsonSchema instance whose getSchemaNode() equals the supplied jsonNode",
            "description": "Verify that a valid SchemaLocation, a well‑formed JsonNode and a non‑null config produce a JsonSchema object without throwing any exception."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(URI.create(\"http://example.com/schema.json\"))"
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
            "expected": "returns a non‑null JsonSchema instance; the internal schema node is loaded from the supplied URI (network or classpath) because jsonNode is null",
            "description": "When jsonNode is null the factory should load the schema from the provided SchemaLocation; ensure no NullPointerException is thrown."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(URI.create(\"classpath:/valid-schema.json\"))"
                },
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "returns a non‑null JsonSchema instance; the factory uses a default SchemaValidatorsConfig when config is null",
            "description": "Confirm that a null config is tolerated and the factory substitutes a default configuration."
        }
    ]
}