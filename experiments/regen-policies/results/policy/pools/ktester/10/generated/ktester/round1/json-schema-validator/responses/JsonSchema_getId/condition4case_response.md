{
    "group": "JsonSchema.getId()",
    "cases": [
        {
            "input": [],
            "expected": "urn:example:schema",
            "description": "Verify that getId() returns the identifier that was supplied to the JsonSchema instance during construction (non‑null id)."
        },
        {
            "input": [],
            "expected": null,
            "description": "Verify that getId() returns null when the JsonSchema instance was created without an identifier (id field is null)."
        }
    ]
}