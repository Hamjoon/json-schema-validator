{
    "group": "Valid inputs - happy path",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "SchemaLocation.of(\"classpath:/schemas/person-schema.json\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "ObjectMapper.readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"}}}\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "non‑null JsonSchema instance whose meta‑schema matches the supplied schema",
            "description": "All arguments are non‑null and represent a valid JSON schema. The method should delegate to newJsonSchema and return a properly constructed JsonSchema object."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "SchemaLocation.of(\"http://example.com/schema\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "ObjectMapper.readTree(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\"}\")"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema instance (default configuration applied)",
            "description": "Configuration is null – the factory should fall back to its default SchemaValidatorsConfig and still produce a JsonSchema."
        }
    ]
}