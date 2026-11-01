{
    "group": "JsonSchemaFactory.getSchemaLocation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.DOCUMENT"
                }
            ],
            "expected": "SchemaLocation.DOCUMENT",
            "description": "When the supplied SchemaLocation is the default constant DOCUMENT (non‑null), the method should return the same instance, confirming that the true branch of the ternary operator works for the default value as well and that the returned reference is identical to the input."
        }
    ]
}