{
    "group": "JsonSchema.isSchemaResourceRoot - edge cases",
    "cases": [
        {
            "input": [],
            "expected": "True",
            "description": "Schema id is an empty string (non‑null). The first `if (getId() != null)` branch is taken, confirming that any non‑null id – even empty – causes the method to return true."
        },
        {
            "input": [],
            "expected": "True",
            "description": "Schema id is whitespace (non‑null). Verifies that the method treats any non‑null id as truthy, regardless of its content."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Schema id is null, a parent schema exists, but the parent’s `getSchemaLocation()` returns null while the child’s location is non‑null. Accessing `parent.getSchemaLocation().getAbsoluteIri()` should throw NPE, confirming that the method does not guard against a null parent location."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Schema id is null, a parent schema exists, the child’s `getSchemaLocation()` returns null while the parent’s location is non‑null. The call to `getSchemaLocation().getAbsoluteIri()` on the child triggers NPE, testing the method’s behavior when the child location is missing."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Both the schema and its parent have null `SchemaLocation`. The method attempts to call `getAbsoluteIri()` on a null reference, resulting in NPE. This case validates that the implementation assumes non‑null locations."
        }
    ]
}