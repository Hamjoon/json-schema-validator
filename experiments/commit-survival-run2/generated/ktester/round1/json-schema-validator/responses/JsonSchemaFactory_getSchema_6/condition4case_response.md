{
    "group": "Successful schema loading",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "ByteArrayInputStream containing '{\"type\":\"object\"}'"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "JsonSchema instance (non‑null) whose underlying JsonNode equals the parsed JSON object",
            "description": "Valid JSON schema is supplied via a normal InputStream and a non‑null configuration. The method should parse the stream, create a JsonSchema via newJsonSchema and return it without throwing."
        },
        {
            "input": [
                {
                    "parameter": "schemaStream",
                    "value": "ByteArrayInputStream containing '{\"type\":\"array\",\"items\":{\"type\":\"string\"}}'"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "JsonSchema instance (non‑null) representing the array schema",
            "description": "Valid JSON schema is supplied but the configuration argument is null. The method must still succeed, passing the null config to newJsonSchema."
        }
    ]
}