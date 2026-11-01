{
    "group": "JsonSchema.isSchemaResourceRoot - branch coverage",
    "cases": [
        {
            "input": [],
            "expected": true,
            "description": "Schema has a non‑null id. The first `if (getId() != null)` branch is taken, method returns true regardless of parent or location."
        },
        {
            "input": [],
            "expected": true,
            "description": "Schema id is null and there is no parent schema (`getParentSchema() == null`). The second `if` branch is taken, method returns true."
        },
        {
            "input": [],
            "expected": true,
            "description": "Schema id is null, a parent schema exists, but the absolute IRI of this schema differs from the parent’s absolute IRI. The third `if (!Objects.equals(...))` branch is taken, method returns true."
        },
        {
            "input": [],
            "expected": false,
            "description": "Schema id is null, a parent schema exists, and both schemas share the same absolute IRI. None of the early `if` conditions match, so the method falls through and returns false."
        }
    ]
}