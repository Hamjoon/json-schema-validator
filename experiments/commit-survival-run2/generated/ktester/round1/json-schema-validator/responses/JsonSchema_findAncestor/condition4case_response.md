{
    "group": "findAncestor",
    "cases": [
        {
            "input": [],
            "expected": "returns the same JsonSchema instance because it has no parent",
            "description": "Root schema: a JsonSchema object whose getParentSchema() returns null. The method should detect that there is no parent and return the current instance (`this`). This covers the branch where the `if` condition evaluates to false."
        },
        {
            "input": [],
            "expected": "returns the direct parent JsonSchema instance because the parent has no further ancestor",
            "description": "Single‑level child: create a child JsonSchema whose getParentSchema() returns a parent JsonSchema whose getParentSchema() is null. The method should recurse once, obtain the parent, see that the parent has no further parent, and finally return that parent. This tests the true branch of the `if` and verifies that recursion works for one level."
        },
        {
            "input": [],
            "expected": "returns the top‑most ancestor JsonSchema instance in a multi‑level chain",
            "description": "Multi‑level chain: build a chain of three schemas (grandParent → parent → child) where each schema's getParentSchema() points to the next higher level. Invoking findAncestor() on the leaf child should recurse through parent and grandParent, ultimately returning the grandParent instance. This ensures the recursive call correctly propagates up the hierarchy and that the method always returns the ultimate ancestor regardless of chain length."
        }
    ]
}