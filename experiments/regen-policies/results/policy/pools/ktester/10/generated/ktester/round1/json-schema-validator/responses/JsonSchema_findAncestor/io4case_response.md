{
    "testCases": [
        {
            "group": "findAncestor_deepHierarchy",
            "cases": [
                {
                    "input": [],
                    "expected": "The method returns the top‑most ancestor (root) JsonSchema instance when the schema is part of a deep chain (e.g., 5 levels).",
                    "description": "Create a chain of five JsonSchema objects where each child’s parent points to the previous one. Calling findAncestor() on the deepest child should traverse recursively and return the root (first) schema."
                }
            ]
        },
        {
            "group": "findAncestor_selfLoop",
            "cases": [
                {
                    "input": [],
                    "expected": "StackOverflowError is thrown due to infinite recursion when a schema’s parent reference points to itself.",
                    "description": "Set a JsonSchema instance’s parent to itself (schema.setParentSchema(schema)). The recursive implementation has no cycle detection, so findAncestor() should cause a StackOverflowError."
                }
            ]
        },
        {
            "group": "findAncestor_idempotent",
            "cases": [
                {
                    "input": [],
                    "expected": "Calling findAncestor() on the result of a previous findAncestor() call returns the same instance (idempotent).",
                    "description": "For a schema with a parent chain, invoke findAncestor() to obtain the root, then invoke findAncestor() again on that root. The second call should return the same root instance, confirming that the method is idempotent."
                }
            ]
        },
        {
            "group": "findAncestor_noSideEffect",
            "cases": [
                {
                    "input": [],
                    "expected": "After invoking findAncestor(), the original schema’s parent reference remains unchanged.",
                    "description": "Create a child schema with a known parent. Call findAncestor() on the child and then verify that child.getParentSchema() still returns the original parent, ensuring the method does not mutate state."
                }
            ]
        }
    ]
}