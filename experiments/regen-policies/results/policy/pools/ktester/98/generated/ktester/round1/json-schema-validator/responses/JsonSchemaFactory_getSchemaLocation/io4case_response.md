{
    "group": "JsonSchemaFactory.getSchemaLocation.additional",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.DOCUMENT"
                }
            ],
            "expected": "SchemaLocation.DOCUMENT",
            "description": "When the explicit default constant SchemaLocation.DOCUMENT is supplied, the method should return the same instance (true branch) rather than substituting another value."
        },
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.SCHEMA_DRAFT_04"
                }
            ],
            "expected": "SchemaLocation.SCHEMA_DRAFT_04",
            "description": "Verify that a non‑null enum constant representing Draft‑04 is returned unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.SCHEMA_DRAFT_06"
                }
            ],
            "expected": "SchemaLocation.SCHEMA_DRAFT_06",
            "description": "Verify that a non‑null enum constant representing Draft‑06 is returned unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.SCHEMA_DRAFT_07"
                }
            ],
            "expected": "SchemaLocation.SCHEMA_DRAFT_07",
            "description": "Verify that a non‑null enum constant representing Draft‑07 is returned unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemaLocation",
                    "value": "SchemaLocation.SCHEMA_DRAFT_2019_09"
                }
            ],
            "expected": "SchemaLocation.SCHEMA_DRAFT_2019_09",
            "description": "Verify that a non‑null enum constant representing Draft‑2019‑09 is returned unchanged."
        }
    ]
}