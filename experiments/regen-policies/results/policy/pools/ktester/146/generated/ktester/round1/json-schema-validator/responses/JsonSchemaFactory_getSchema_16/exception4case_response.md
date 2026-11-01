{
    "test_cases": [
        {
            "group": "Invalid URI handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"relative/path/schema.json\")"
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
                    "expected": "IllegalArgumentException",
                    "description": "A relative URI (no scheme) should be rejected by SchemaLocation.of, causing an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Non‑object schema node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new URI(\"http://example.com/schema.json\")"
                        },
                        {
                            "parameter": "jsonNode",
                            "value": "new ObjectMapper().readTree(\"\\\"just a string\\\"\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Providing a JsonNode that is not an object (e.g., a JSON string) should cause newJsonSchema to reject the schema node and throw IllegalArgumentException."
                }
            ]
        }
    ]
}