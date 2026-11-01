{
    "group": "JsonSchemaFactory.getSchemaLocation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.SCHEMA"
                }
            ],
            "expected": "SchemaLocation.SCHEMA",
            "description": "When a non‑null SchemaLocation is supplied, the method should return the same instance (branch where the ternary condition is true)."
        },
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "null"
                }
            ],
            "expected": "SchemaLocation.DOCUMENT",
            "description": "When the supplied SchemaLocation is null, the method should fall back to the default constant SchemaLocation.DOCUMENT (branch where the ternary condition is false)."
        }
    ]
}