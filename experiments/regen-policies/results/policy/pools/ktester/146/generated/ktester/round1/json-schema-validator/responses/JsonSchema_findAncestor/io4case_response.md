{
    "testCases": [
        {
            "group": "findAncestor - deep hierarchy (10 levels)",
            "cases": [
                {
                    "input": [],
                    "expected": "Returns the top‑most ancestor (root schema) instance after traversing ten levels",
                    "description": "Create a chain of ten JsonSchema objects where each schema's parent points to the previous one, and the first (root) schema has a null parent. Invoking findAncestor() on the deepest leaf should recursively climb ten levels and return the root schema, confirming the method works for deep recursion without stack overflow."
                }
            ]
        },
        {
            "group": "findAncestor - idempotent call",
            "cases": [
                {
                    "input": [],
                    "expected": "Subsequent calls return the same root instance",
                    "description": "Using a multi‑level schema chain (e.g., leaf → middle → root), call findAncestor() on the leaf twice. Both calls should return the identical root schema instance, demonstrating that the method is pure and does not alter internal state."
                }
            ]
        },
        {
            "group": "findAncestor - indirect cycle detection",
            "cases": [
                {
                    "input": [],
                    "expected": "Throws StackOverflowError",
                    "description": "Construct three schemas A, B, C where A.parent = B, B.parent = C, and C.parent = A, forming an indirect cycle. Invoking findAncestor() on any schema in the cycle should cause infinite recursion and eventually throw a StackOverflowError, confirming that the method does not guard against non‑self cycles."
                }
            ]
        },
        {
            "group": "findAncestor - no side‑effects on parent reference",
            "cases": [
                {
                    "input": [],
                    "expected": "Parent reference remains unchanged after findAncestor() execution",
                    "description": "Create a child schema with a known parent schema. After calling findAncestor() on the child, verify that child.getParentSchema() still returns the original parent instance (i.e., the method does not modify the parent link)."
                }
            ]
        }
    ]
}