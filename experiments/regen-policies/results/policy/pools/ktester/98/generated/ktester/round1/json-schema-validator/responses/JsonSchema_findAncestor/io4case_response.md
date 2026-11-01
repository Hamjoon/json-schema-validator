{
    "newTestCases": [
        {
            "group": "Self‑referential parent (immediate cycle)",
            "cases": [
                {
                    "input": [],
                    "expected": "StackOverflowError (due to infinite recursion when a schema's parent is itself)",
                    "description": "Create a JsonSchema instance whose getParentSchema() returns the same instance. Invoking findAncestor() should recurse without termination and result in a StackOverflowError, confirming that the current implementation lacks cycle detection for self‑references."
                }
            ]
        },
        {
            "group": "Idempotent root ancestor retrieval",
            "cases": [
                {
                    "input": [],
                    "expected": "the same JsonSchema instance on both calls",
                    "description": "Obtain a root schema (parent == null). Call findAncestor() twice on the same instance and verify that each call returns the original root object, demonstrating that the method is idempotent for root schemas."
                }
            ]
        },
        {
            "group": "Deep non‑cyclic parent chain within safe recursion depth",
            "cases": [
                {
                    "input": [],
                    "expected": "the top‑most parent JsonSchema instance (the root of the 50‑level chain)",
                    "description": "Construct a chain of 50 JsonSchema objects where each schema's parent points to the next one, ending with a root whose parent is null. findAncestor() should successfully recurse through all levels and return the root without throwing an error."
                }
            ]
        },
        {
            "group": "Deep chain exceeding typical stack depth",
            "cases": [
                {
                    "input": [],
                    "expected": "StackOverflowError (recursion depth too large for the JVM stack)",
                    "description": "Create a chain of 2000 JsonSchema objects (or a number that reliably exceeds the default JVM stack limit). Invoking findAncestor() is expected to overflow the stack, confirming that the method relies on recursion and has practical depth limits."
                }
            ]
        },
        {
            "group": "Parent schema throws exception on getParentSchema()",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException propagated from getParentSchema()",
                    "description": "Define a subclass of JsonSchema that overrides getParentSchema() to throw a RuntimeException. When findAncestor() is called on a child schema whose parent is this faulty instance, the exception should bubble up, verifying that findAncestor() does not swallow unexpected errors from the parent accessor."
                }
            ]
        }
    ]
}