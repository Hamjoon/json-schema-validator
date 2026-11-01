{
    "group": "isSchemaResourceRoot - NullPointerException scenarios",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Schema's getSchemaLocation() returns null while a parent schema exists with a non‑null schema location. The method attempts to call getAbsoluteIri() on a null reference, causing a NullPointerException."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Parent schema's getSchemaLocation() returns null while the schema's own location is non‑null. The method attempts to call getAbsoluteIri() on the parent's null location, causing a NullPointerException."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Both the schema and its parent have null schema locations. The first call to getAbsoluteIri() on a null reference triggers a NullPointerException."
        }
    ]
}