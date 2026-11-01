{
    "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode) - delegation tests",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"http://example.com/schema.json\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"object\")"
                }
            ],
            "expected": "non‑null JsonSchema instance (the instance returned by overridden newJsonSchema)",
            "description": "Both arguments are non‑null. The method should delegate to newJsonSchema with the same SchemaLocation, the same JsonNode and a null config, and return the JsonSchema produced by newJsonSchema."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "null"
                },
                {
                    "parameter": "jsonNode",
                    "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")"
                }
            ],
            "expected": "NullPointerException",
            "description": "schemaUri is null while jsonNode is non‑null. newJsonSchema is expected to reject a null SchemaLocation, so getSchema should propagate the NullPointerException."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"http://example.com/another.json\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema instance (the instance returned by overridden newJsonSchema)",
            "description": "jsonNode is null but schemaUri is valid. The method should still delegate to newJsonSchema with a null JsonNode and null config, returning the JsonSchema created by newJsonSchema."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "null"
                },
                {
                    "parameter": "jsonNode",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException",
            "description": "Both parameters are null. The delegation should fail early with a NullPointerException because newJsonSchema cannot handle a null SchemaLocation."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new SchemaLocation(\"http://example.com/schema.yaml\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "JsonNodeFactory.instance.objectNode().put(\"$schema\", \"http://json-schema.org/draft-07/schema#\")"
                }
            ],
            "expected": "non‑null JsonSchema instance, and the captured config argument inside newJsonSchema is null",
            "description": "Verify that getSchema always passes a null config to newJsonSchema regardless of the input values."
        }
    ]
}