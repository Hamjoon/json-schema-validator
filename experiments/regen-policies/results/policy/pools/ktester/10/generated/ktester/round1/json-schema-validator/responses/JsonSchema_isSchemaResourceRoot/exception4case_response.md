{
    "test_cases": [
        {
            "group": "JsonSchema.isSchemaResourceRoot - getId throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "A subclass of JsonSchema overrides `getId()` to throw a RuntimeException. The method should not catch it, so the exception propagates."
                }
            ]
        },
        {
            "group": "JsonSchema.isSchemaResourceRoot - getSchemaLocation throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "`getSchemaLocation()` returns a mock object whose `getAbsoluteIri()` throws a RuntimeException. Since the method does not guard against this, the exception is expected to bubble up."
                }
            ]
        },
        {
            "group": "JsonSchema.isSchemaResourceRoot - parent getSchemaLocation throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "The parent schema's `getSchemaLocation()` returns a mock that throws a RuntimeException when `getAbsoluteIri()` is invoked. The call inside `Objects.equals` should propagate the exception."
                }
            ]
        }
    ]
}