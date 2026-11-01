{
    "group": "JsonSchemaFactory.getSchemaLocation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.SCHEMA_2020_12"
                }
            ],
            "expected": "SchemaLocation.SCHEMA_2020_12",
            "description": "When a non‑null SchemaLocation is supplied, the method should return the same instance (the true branch of the ternary operator)."
        },
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": null
                }
            ],
            "expected": "SchemaLocation.DOCUMENT",
            "description": "When a null SchemaLocation is supplied, the method should fall back to the default constant SchemaLocation.DOCUMENT (the false branch of the ternary operator)."
        }
    ]
}