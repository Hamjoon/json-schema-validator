{
    "group": "findSchemaResourceRoot_exceptionPropagation",
    "cases": [
        {
            "input": [],
            "expected": "java.lang.IllegalStateException",
            "description": "When isSchemaResourceRoot() is overridden (e.g., via a mock or subclass) to throw IllegalStateException, findSchemaResourceRoot() should not catch it and must propagate the exception to the caller."
        },
        {
            "input": [],
            "expected": "java.lang.IllegalArgumentException",
            "description": "When getParentSchema() is overridden (e.g., via a mock or subclass) to throw IllegalArgumentException during the upward traversal, findSchemaResourceRoot() should propagate the exception without handling it."
        }
    ]
}