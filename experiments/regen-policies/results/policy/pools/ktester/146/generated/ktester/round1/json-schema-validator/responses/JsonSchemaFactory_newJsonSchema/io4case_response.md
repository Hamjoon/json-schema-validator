{
    "group": "Both schemaUri and config null – default handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "null"
                },
                {
                    "parameter": "schemaNode",
                    "value": "mockJsonNode"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "returns a non‑null JsonSchema; a default SchemaValidatorsConfig is created internally; getSchemaLocation receives null and returns null (or a placeholder); doCreate is invoked with the default config, null schema location, a JsonNodePath built from the default path type, the supplied schemaNode, parentSchema = null and suppressSubSchemaRetrieval = false; preload is called with the created JsonSchema and the default config.",
            "description": "Verifies that when both the schema location and the configuration are null the factory falls back to its default configuration and still creates a JsonSchema without throwing an exception."
        }
    ]
}