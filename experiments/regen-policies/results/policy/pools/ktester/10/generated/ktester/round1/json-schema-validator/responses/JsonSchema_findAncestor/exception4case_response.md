{
    "groups": [
        {
            "group": "findAncestor_parentThrows",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException is propagated when getParentSchema() throws it.",
                    "description": "Create a subclass of JsonSchema that overrides getParentSchema() to throw IllegalStateException. Invoking findAncestor() should not catch the exception, allowing it to bubble up."
                }
            ]
        },
        {
            "group": "findAncestor_indirectCycle",
            "cases": [
                {
                    "input": [],
                    "expected": "StackOverflowError is thrown due to infinite recursion caused by an indirect parent cycle (e.g., A → B → C → A).",
                    "description": "Construct three JsonSchema instances A, B, and C where A.setParentSchema(B), B.setParentSchema(C), and C.setParentSchema(A). Calling findAncestor() on any of them should trigger unbounded recursion and result in a StackOverflowError."
                }
            ]
        }
    ]
}