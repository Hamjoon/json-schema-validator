{
    "group": "JsonSchemaFactory.builder()",
    "cases": [
        {
            "input": [],
            "expected": "non‑null instance of JsonSchemaFactory.Builder",
            "description": "Verify that the static builder() method returns a newly created Builder object (not null)."
        },
        {
            "input": [],
            "expected": "two distinct Builder instances on successive calls",
            "description": "Call builder() twice and assert that the returned Builder objects are different (different reference) to ensure a fresh builder is created each time."
        },
        {
            "input": [],
            "expected": "Builder instance with default configuration (no side‑effects from previous calls)",
            "description": "Obtain a Builder, modify a property on it (e.g., set a custom URI translator), then obtain a second Builder and verify that the second Builder does not inherit the modifications made to the first, confirming isolation between builder instances."
        }
    ]
}