{
    "testCases": [
        {
            "group": "BaseJsonValidator#getParentSchema - constructor initialization",
            "cases": [
                {
                    "input": [],
                    "expected": "instance of JsonSchema (constructorParentSchema)",
                    "description": "A concrete subclass of BaseJsonValidator receives a non‑null parentSchema via its constructor. getParentSchema() must return exactly the instance supplied at construction (identity check)."
                }
            ]
        },
        {
            "group": "BaseJsonValidator#getParentSchema - reset to null after being set",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "A subclass sets parentSchema to a mock JsonSchema during construction, then later (e.g., via a setter or reflection) resets the field to null. Subsequent call to getParentSchema() should return null."
                }
            ]
        },
        {
            "group": "BaseJsonValidator#getParentSchema - idempotent return",
            "cases": [
                {
                    "input": [],
                    "expected": "same instance on repeated calls",
                    "description": "When parentSchema is non‑null, multiple successive invocations of getParentSchema() must return the exact same object reference each time (no new objects are created)."
                }
            ]
        },
        {
            "group": "BaseJsonValidator#getParentSchema - subclass JsonSchema implementation",
            "cases": [
                {
                    "input": [],
                    "expected": "instance of CustomJsonSchema (subclassParentSchema)",
                    "description": "Assign a custom implementation of JsonSchema (e.g., class CustomJsonSchema extends JsonSchema) to the protected parentSchema field. getParentSchema() should return that subclass instance, confirming polymorphic behavior."
                }
            ]
        },
        {
            "group": "BaseJsonValidator#getParentSchema - safe with exception‑throwing mock",
            "cases": [
                {
                    "input": [],
                    "expected": "instance of JsonSchema (exceptionMock)",
                    "description": "Set parentSchema to a mock JsonSchema whose any method throws a RuntimeException. getParentSchema() must simply return the reference without invoking any method on the mock, therefore no exception should be propagated."
                }
            ]
        }
    ]
}