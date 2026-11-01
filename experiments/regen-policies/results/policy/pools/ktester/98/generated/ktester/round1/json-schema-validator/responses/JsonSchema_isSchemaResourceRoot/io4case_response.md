{
    "group": "isSchemaResourceRoot - null IRI handling",
    "cases": [
        {
            "input": [],
            "expected": false,
            "description": "Both the schema and its parent have a non‑null id (null) and a parent schema, but their `SchemaLocation.getAbsoluteIri()` methods return `null`. `Objects.equals(null, null)` evaluates to true, so the method falls through and returns false."
        },
        {
            "input": [],
            "expected": true,
            "description": "Schema id is null, a parent schema exists, the schema's `getAbsoluteIri()` returns `null` while the parent's returns a non‑null IRI. `Objects.equals(null, \"http://example.com/parent\")` is false, so the method returns true."
        },
        {
            "input": [],
            "expected": true,
            "description": "Schema id is null, a parent schema exists, the schema's IRI is non‑null while the parent's IRI is `null`. `Objects.equals(\"http://example.com/schema\", null)` is false, leading to a true result."
        }
    ]
}