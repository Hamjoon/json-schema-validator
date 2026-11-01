{
    "group": "Successful schema loading",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "InputStream containing a valid JSON schema (e.g., {\"type\":\"object\"})"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "A non‑null JsonSchema instance is returned (the schema node should be parsed without errors).",
            "description": "Verify that a well‑formed JSON schema supplied as an InputStream is read correctly and a JsonSchema object is created."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "InputStream containing a valid YAML schema (e.g., \"type: object\")"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.YAML"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "A non‑null JsonSchema instance is returned (the YAML content is parsed into a JsonNode and wrapped).",
            "description": "Verify that a well‑formed YAML schema is correctly parsed when the inputFormat is YAML and a JsonSchema object is returned."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "InputStream containing a valid JSON schema"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "A non‑null JsonSchema instance is returned (null config is accepted and defaults are used).",
            "description": "Confirm that passing a null SchemaValidatorsConfig does not cause a failure; the factory should fall back to default configuration."
        }
    ]
}