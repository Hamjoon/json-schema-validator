{
    "group": "BaseJsonValidator#getSchemaNode",
    "cases": [
        {
            "input": [],
            "expected": "ObjectNode instance containing {\"type\":\"string\"}",
            "description": "Verify that getSchemaNode() returns the exact JsonNode that was supplied to the validator via the constructor. The validator is instantiated with a non‑null ObjectNode representing a simple schema (e.g., {\"type\":\"string\"). The returned node should be the same instance (identity equality) and contain the expected field."
        },
        {
            "input": [],
            "expected": "null",
            "description": "Verify that getSchemaNode() correctly returns null when the validator is constructed with a null schemaNode. This ensures the method does not perform any hidden conversion or defaulting."
        }
    ]
}