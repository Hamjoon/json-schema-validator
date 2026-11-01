{
    "testCases": [
        {
            "group": "Empty schemas list",
            "cases": [
                {
                    "input": [],
                    "expected": "No exception is thrown and initializeValidators() is never called on any JsonSchema.",
                    "description": "When the internal `schemas` list is empty, `preloadJsonSchema()` should complete without invoking any `initializeValidators()` calls."
                }
            ]
        },
        {
            "group": "Single schema initialization",
            "cases": [
                {
                    "input": [],
                    "expected": "initializeValidators() is called exactly once on the sole JsonSchema instance and no exception is thrown.",
                    "description": "Verify that a single schema present in the `schemas` list is initialized exactly once."
                }
            ]
        },
        {
            "group": "Multiple schemas initialization",
            "cases": [
                {
                    "input": [],
                    "expected": "initializeValidators() is called once for each JsonSchema in the list (order preserved) and no exception is thrown.",
                    "description": "When the list contains several schemas, `preloadJsonSchema()` must iterate over the whole collection and invoke `initializeValidators()` on each element."
                }
            ]
        },
        {
            "group": "Exception propagation from a schema",
            "cases": [
                {
                    "input": [],
                    "expected": "The RuntimeException thrown by a schema's initializeValidators() propagates out of preloadJsonSchema().",
                    "description": "If any schema throws an unchecked exception during initialization, `preloadJsonSchema()` should not swallow it; the exception must bubble up to the caller."
                }
            ]
        }
    ]
}