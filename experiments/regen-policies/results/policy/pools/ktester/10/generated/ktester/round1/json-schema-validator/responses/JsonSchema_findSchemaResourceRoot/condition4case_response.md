{
    "group": "findSchemaResourceRoot - basic scenarios",
    "cases": [
        {
            "input": [],
            "expected": "the same JsonSchema instance (root) is returned",
            "description": "When the current schema is already a schema‑resource root (isSchemaResourceRoot() returns true), the method should return the instance itself without traversing to a parent."
        },
        {
            "input": [],
            "expected": "the direct parent JsonSchema instance is returned",
            "description": "When the current schema is not a root (isSchemaResourceRoot() returns false) but its immediate parent is a root, the loop should iterate once and return that parent."
        },
        {
            "input": [],
            "expected": "the top‑most ancestor that is a schema‑resource root is returned",
            "description": "With a chain of three schemas (child → intermediate → root) where only the topmost ancestor reports true for isSchemaResourceRoot(), the method must walk up two levels and return the root instance."
        },
        {
            "input": [],
            "expected": "NullPointerException is thrown",
            "description": "If the schema hierarchy is malformed such that a non‑root schema has a null parent (getParentSchema() returns null), the while loop will eventually call isSchemaResourceRoot() on null, resulting in a NullPointerException. This case verifies that the method does not silently succeed on an invalid hierarchy."
        }
    ]
}