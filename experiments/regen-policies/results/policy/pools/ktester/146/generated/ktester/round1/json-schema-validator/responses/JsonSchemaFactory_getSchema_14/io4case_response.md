{
    "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - edge cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "null"
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
            "expected": "throws NullPointerException (or IllegalArgumentException) because schemaUri is required",
            "description": "Verify that passing a null SchemaLocation results in an immediate exception – the factory cannot create a JsonSchema without a location identifier."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(URI.create(\"classpath:/valid-schema.json\"))"
                },
                {
                    "parameter": "jsonNode",
                    "value": "null"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "returns a non‑null JsonSchema instance; the factory creates a default SchemaValidatorsConfig and loads the schema from the classpath resource",
            "description": "Both jsonNode and config are null – the method should fall back to loading the schema from the supplied location and use a default configuration without throwing an exception."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(URI.create(\"classpath:/non‑existent-schema.json\"))"
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
            "expected": "throws IllegalArgumentException (or a runtime exception) indicating that the schema resource could not be found",
            "description": "The provided classpath URI does not point to an existing file; the factory must surface the error rather than returning a malformed JsonSchema."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(URI.create(\"ht!tp://bad-schema-uri\"))"
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
            "expected": "throws IllegalArgumentException (or URISyntaxException) because the URI scheme is malformed",
            "description": "Supply a syntactically invalid URI to ensure the factory validates the URI format before attempting to load a schema."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(URI.create(\"classpath:/valid-schema.json\"))"
                },
                {
                    "parameter": "jsonNode",
                    "value": "new ObjectMapper().readTree(\"{}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig().setFailFast(true)"
                }
            ],
            "expected": "returns a non‑null JsonSchema instance whose internal SchemaValidatorsConfig has failFast set to true",
            "description": "Pass a custom configuration (failFast=true) together with an empty schema node; the returned JsonSchema should retain the supplied configuration."
        }
    ]
}