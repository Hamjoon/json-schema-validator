{
    "group": "isSchemaResourceRoot - branch coverage",
    "cases": [
        {
            "input": [],
            "expected": true,
            "description": "Schema has a non‑null id. The first `if (getId() != null)` branch is taken and the method returns true without evaluating the other conditions."
        },
        {
            "input": [],
            "expected": true,
            "description": "Schema id is null and it has no parent schema (`getParentSchema() == null`). The second `if` branch is taken, returning true."
        },
        {
            "input": [],
            "expected": true,
            "description": "Schema id is null, a parent schema exists, but the schema location IRI differs from the parent schema location IRI. The third `if (!Objects.equals(...))` branch evaluates to true, so the method returns true."
        },
        {
            "input": [],
            "expected": false,
            "description": "Schema id is null, a parent schema exists, and both schema location IRI and parent schema location IRI are equal. None of the earlier conditions are satisfied, so the method falls through and returns false."
        }
    ]
}