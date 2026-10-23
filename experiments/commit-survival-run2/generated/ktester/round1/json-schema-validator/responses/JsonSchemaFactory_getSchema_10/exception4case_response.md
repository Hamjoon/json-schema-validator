{
    "group": "Unsupported schema version – unknown $schema value",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "http://example.com/unknown-schema"
                },
                {
                    "parameter": "jsonNode",
                    "value": "{\"$schema\":\"http://example.com/unknown/schema#\",\"type\":\"object\"}"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The supplied JSON schema references an unknown meta‑schema via the $schema keyword; JsonSchemaFactory should reject it and throw an IllegalArgumentException."
        }
    ]
}