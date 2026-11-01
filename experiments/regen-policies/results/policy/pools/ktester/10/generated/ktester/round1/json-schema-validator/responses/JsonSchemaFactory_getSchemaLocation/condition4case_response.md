{
    "group": "JsonSchemaFactory.getSchemaLocation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaRetrievalUri",
                    "value": "null"
                },
                {
                    "parameter": "schemaNode",
                    "value": "any JsonNode (mocked)"
                },
                {
                    "parameter": "validationContext",
                    "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns \"http://example.com/schema\""
                }
            ],
            "expected": "SchemaLocation.of(\"http://example.com/schema\")",
            "description": "When the ValidationContext resolves a non‑null schema ID, the method should return a SchemaLocation created from that ID, ignoring the supplied schemaRetrievalUri (which is null in this case)."
        },
        {
            "input": [
                {
                    "parameter": "schemaRetrievalUri",
                    "value": "SchemaLocation.of(\"http://fallback.com/schema\")"
                },
                {
                    "parameter": "schemaNode",
                    "value": "any JsonNode (mocked)"
                },
                {
                    "parameter": "validationContext",
                    "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns null"
                }
            ],
            "expected": "SchemaLocation.of(\"http://fallback.com/schema\")",
            "description": "When the ValidationContext returns null and a non‑null schemaRetrievalUri is supplied, the method should fall back to the string representation of schemaRetrievalUri."
        },
        {
            "input": [
                {
                    "parameter": "schemaRetrievalUri",
                    "value": "null"
                },
                {
                    "parameter": "schemaNode",
                    "value": "any JsonNode (mocked)"
                },
                {
                    "parameter": "validationContext",
                    "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns null"
                }
            ],
            "expected": "SchemaLocation.DOCUMENT",
            "description": "When both the resolved schema ID and the supplied schemaRetrievalUri are null, the method should return the constant SchemaLocation.DOCUMENT."
        },
        {
            "input": [
                {
                    "parameter": "schemaRetrievalUri",
                    "value": "SchemaLocation.of(\"http://fallback.com/schema\")"
                },
                {
                    "parameter": "schemaNode",
                    "value": "any JsonNode (mocked)"
                },
                {
                    "parameter": "validationContext",
                    "value": "mock ValidationContext where resolveSchemaId(schemaNode) returns \"http://example.com/schema\""
                }
            ],
            "expected": "SchemaLocation.of(\"http://example.com/schema\")",
            "description": "Even if a schemaRetrievalUri is provided, a non‑null ID resolved from the ValidationContext takes precedence."
        }
    ]
}