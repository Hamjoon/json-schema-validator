{
    "group": "findAncestor",
    "cases": [
        {
            "input": [],
            "expected": "The method returns the same JsonSchema instance because getParentSchema() returns null (no parent).",
            "description": "Verify that a root schema (no parent) returns itself as the ancestor."
        },
        {
            "input": [],
            "expected": "The method returns the direct parent JsonSchema instance because the parent’s getParentSchema() returns null.",
            "description": "Verify that a schema with a single parent returns that parent as the ancestor."
        },
        {
            "input": [],
            "expected": "The method returns the top‑most ancestor (grandparent) JsonSchema instance when the schema has a parent that itself has a parent.",
            "description": "Verify recursive traversal: a child schema with a parent and a grandparent returns the grandparent as the ancestor."
        }
    ]
}