{
    "new_cases": [
        {
            "group": "isSchemaResourceRoot throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "throws IllegalStateException",
                    "description": "Configure a JsonSchema instance whose isSchemaResourceRoot() method throws IllegalStateException (e.g., by subclassing JsonSchema). The while‑loop will invoke this method on the first iteration and the exception should propagate out of findSchemaResourceRoot()."
                }
            ]
        },
        {
            "group": "getParentSchema throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "throws UnsupportedOperationException",
                    "description": "Create a JsonSchema subclass where getParentSchema() is overridden to throw UnsupportedOperationException. The instance reports false for isSchemaResourceRoot(), causing the loop to call getParentSchema(), which then throws the exception. The test verifies that findSchemaResourceRoot() does not catch the exception and it propagates."
                }
            ]
        }
    ]
}