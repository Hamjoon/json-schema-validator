{
    "test_cases": [
        {
            "group": "isSchemaResourceRoot_exceptions",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When the schema has a null id, a non‑null parent schema, but the schema's SchemaLocation is null, calling getSchemaLocation().getAbsoluteIri() throws NullPointerException."
                },
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When the schema has a null id, a non‑null parent schema whose SchemaLocation is null (while the schema's own SchemaLocation is non‑null), the call to getParentSchema().getSchemaLocation().getAbsoluteIri() throws NullPointerException."
                }
            ]
        }
    ]
}