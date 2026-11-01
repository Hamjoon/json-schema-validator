{
    "group": "findAncestor - exception propagation from parent",
    "cases": [
        {
            "input": [],
            "expected": "RuntimeException thrown by parent schema is propagated",
            "description": "Create a parent JsonSchema subclass whose findAncestor() method throws an IllegalArgumentException. Set this object as the child schema's parent. Invoking findAncestor() on the child should result in the IllegalArgumentException being thrown, confirming that the method does not catch exceptions from the recursive call."
        }
    ]
}