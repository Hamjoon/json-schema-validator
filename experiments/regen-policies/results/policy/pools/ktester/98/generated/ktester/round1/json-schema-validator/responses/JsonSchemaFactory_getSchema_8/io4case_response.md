{
    "group": "JsonSchemaFactory.getSchema(JsonNode, SchemaValidatorsConfig)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-04/schema#\\\",\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"id\\\":{\\\"type\\\":\\\"integer\\\"}}}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig(){ { setTypeLoose(true); } }"
                }
            ],
            "expected": "Returns a non‑null JsonSchema whose meta‑schema corresponds to Draft‑04 (the $schema value) and whose SchemaValidatorsConfig has typeLoose set to true.",
            "description": "Schema node explicitly declares Draft‑04 meta‑schema and a custom config enables type‑loose mode; the factory must honour both the $schema URI and the supplied configuration."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "Returns a non‑null JsonSchema instance using the default meta‑schema (the factory’s default) even though the schema node is empty.",
            "description": "An empty JSON object is a valid (though useless) schema; the method should still create a JsonSchema object rather than throwing."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"42\")"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "Returns a non‑null JsonSchema instance (the factory does not validate the content of the JsonNode at creation time).",
            "description": "The supplied JsonNode is a primitive numeric node, not an object schema. The factory should still wrap it in a JsonSchema; validation errors would surface later, not during creation."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig(){ { setFailFast(true); } }"
                }
            ],
            "expected": "Returns a non‑null JsonSchema whose internal SchemaValidatorsConfig has failFast set to true.",
            "description": "A custom configuration that enables fail‑fast validation is supplied; the created JsonSchema must carry this configuration forward."
        }
    ]
}