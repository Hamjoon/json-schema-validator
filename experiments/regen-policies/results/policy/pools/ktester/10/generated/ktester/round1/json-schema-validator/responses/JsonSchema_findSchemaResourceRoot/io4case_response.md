{
    "group": "findSchemaResourceRoot - deep hierarchy",
    "cases": [
        {
            "input": [],
            "expected": "the top‑most ancestor (level 10) is returned",
            "description": "Create a chain of ten JsonSchema objects where only the level‑10 schema reports true for isSchemaResourceRoot(). The method should traverse nine parent links and return the level‑10 instance."
        },
        {
            "input": [],
            "expected": "the top‑most ancestor (level 50) is returned",
            "description": "Create a longer chain (e.g., 50 levels) with the root at the deepest level. This verifies that the while‑loop works correctly for deep hierarchies and does not overflow the stack."
        }
    ]
}