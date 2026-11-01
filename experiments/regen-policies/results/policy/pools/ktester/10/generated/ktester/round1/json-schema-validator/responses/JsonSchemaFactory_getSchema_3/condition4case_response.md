{
    "group": "JsonSchemaFactory.getSchema(JsonNode) - basic functionality",
    "cases": [
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "ObjectMapper.readTree(\"{}\")"
                }
            ],
            "expected": "non‑null JsonSchema instance (schema representing an empty JSON object)",
            "description": "Verify that a simple, valid JSON object node results in a JsonSchema object being created without errors."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "ObjectMapper.readTree('{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}')"
                }
            ],
            "expected": "non‑null JsonSchema instance whose meta‑schema matches the default version",
            "description": "Pass a typical JSON‑Schema definition and ensure the factory creates a JsonSchema that can later be used for validation."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "The method should reject a null JsonNode argument and throw a NullPointerException (as per the implementation of newJsonSchema)."
        }
    ]
}