{
    "test_cases": [
        {
            "group": "Null schema element handling",
            "cases": [
                {
                    "input": [],
                    "expected": "A NullPointerException is thrown when the internal `schemas` list contains a null element and `preloadJsonSchema()` attempts to invoke `initializeValidators()` on it.",
                    "description": "Verify that a null entry inside the `schemas` list is not silently ignored and causes a NullPointerException, ensuring the method does not swallow nulls."
                }
            ]
        },
        {
            "group": "Repeated preload invocation",
            "cases": [
                {
                    "input": [],
                    "expected": "For each schema in the list, `initializeValidators()` is called once per invocation of `preloadJsonSchema()`. After two consecutive calls, the method on each schema is invoked exactly twice, and no exception is thrown.",
                    "description": "Ensure that `preloadJsonSchema()` does not cache a state that prevents re‑initialisation; calling it multiple times should re‑execute `initializeValidators()` on every schema."
                }
            ]
        },
        {
            "group": "Duplicate schema instances",
            "cases": [
                {
                    "input": [],
                    "expected": "`initializeValidators()` is called once for each occurrence of the duplicate schema object in the list (i.e., twice if the same instance appears twice). No exception is thrown.",
                    "description": "Confirm that the method treats each list entry independently, even when the same `JsonSchema` instance appears multiple times."
                }
            ]
        },
        {
            "group": "Schema initialization order with side effects",
            "cases": [
                {
                    "input": [],
                    "expected": "Side‑effect markers (e.g., appending to a shared list) appear in the exact order of the schemas in the `schemas` list, demonstrating that iteration order is preserved.",
                    "description": "Validate that `preloadJsonSchema()` iterates over the `schemas` list sequentially, not in parallel or random order, by using mock schemas that record the order of `initializeValidators()` calls."
                }
            ]
        },
        {
            "group": "Schema throws Error propagation",
            "cases": [
                {
                    "input": [],
                    "expected": "The `Error` thrown by a schema's `initializeValidators()` propagates out of `preloadJsonSchema()` unchanged.",
                    "description": "Check that the method does not catch or wrap `Error` types (e.g., `OutOfMemoryError`), allowing them to bubble up to the caller."
                }
            ]
        }
    ]
}