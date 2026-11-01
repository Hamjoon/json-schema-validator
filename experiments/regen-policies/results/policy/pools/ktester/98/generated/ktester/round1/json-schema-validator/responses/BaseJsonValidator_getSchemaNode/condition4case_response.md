{
    "group": "BaseJsonValidator#getSchemaNode",
    "cases": [
        {
            "input": [],
            "expected": "the same non‑null JsonNode instance that was supplied to the validator’s constructor",
            "description": "When the validator is constructed with a concrete JsonNode (e.g., an ObjectNode representing {\"type\":\"string\"}), getSchemaNode() must return that exact instance, confirming the getter simply returns the stored field."
        },
        {
            "input": [],
            "expected": "null",
            "description": "When the validator is constructed with a null schemaNode (allowed for some internal validators), getSchemaNode() should return null without throwing an exception."
        }
    ]
}