{
    "testCases": [
        {
            "group": "findSchemaResourceRoot - exceptional internal failures",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException is propagated",
                    "description": "When isSchemaResourceRoot() throws an IllegalStateException (e.g., due to corrupted internal state), the method should not catch it and the exception must propagate to the caller."
                },
                {
                    "input": [],
                    "expected": "UnsupportedOperationException is propagated",
                    "description": "When getParentSchema() throws an UnsupportedOperationException (e.g., the schema hierarchy is immutable), the method should propagate the exception without swallowing it."
                },
                {
                    "input": [],
                    "expected": "Method does not terminate (infinite loop) leading to test timeout",
                    "description": "If a circular parent reference is introduced (schema A's parent is schema B and schema B's parent is schema A) and neither reports true for isSchemaResourceRoot(), the while‑loop never exits. The test should enforce a timeout and verify that the method fails to return, indicating a potential infinite‑loop bug."
                }
            ]
        }
    ]
}