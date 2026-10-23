{
    "testCases": [
        {
            "group": "Exception propagation from a schema during preload",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas propagates the RuntimeException thrown by the offending JsonSchema",
                    "description": "Insert a mock JsonSchema into the internal `schemas` map whose `preloadJsonSchema` implementation throws a RuntimeException. When `PropertiesValidator.preloadJsonSchema()` is invoked, the exception should bubble up unchanged, confirming that the method does not swallow unexpected errors from individual schemas."
                }
            ]
        },
        {
            "group": "Repeated invocation does not alter internal state",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas receives the same collection on each call and the internal `schemas` map remains unchanged after each invocation",
                    "description": "Populate the `schemas` map with two distinct JsonSchema instances. Call `preloadJsonSchema()` twice in succession. Verify that both calls forward identical collections (same size, same order) and that the internal map still contains the original two entries after the second call."
                }
            ]
        },
        {
            "group": "Order preservation with mixed insertion patterns",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas receives schemas in the exact insertion order, even after intermediate removals and re‑insertions",
                    "description": "Add three schemas A, B, C to the map in that order, then remove B and re‑add a new schema D under the same key as B. The final insertion order should be A, C, D. After invoking `preloadJsonSchema()`, assert that the collection passed to `preloadJsonSchemas` follows this order, demonstrating that the LinkedHashMap ordering semantics are respected."
                }
            ]
        },
        {
            "group": "Circular reference schema handling",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas throws a ValidationException (or StackOverflowError) due to circular reference, which propagates from preloadJsonSchema",
                    "description": "Create two mock JsonSchema objects where each references the other (e.g., schema1's `$ref` points to schema2 and vice‑versa). Insert both into the `schemas` map. When `preloadJsonSchema()` is called, the underlying preload logic will encounter a circular reference and raise an exception. The test verifies that this exception is not caught inside `preloadJsonSchema`."
                }
            ]
        },
        {
            "group": "Null collection handling after map mutation",
            "cases": [
                {
                    "input": [],
                    "expected": "preloadJsonSchemas receives a collection that reflects the map state at call time; subsequent mutations to the map do not affect the already‑passed collection",
                    "description": "Add a JsonSchema instance to the map, invoke `preloadJsonSchema()`, then immediately clear the map. Using a spy on `preloadJsonSchemas`, confirm that the collection received during the call still contains the original schema, proving that the method passes a snapshot rather than a live view."
                }
            ]
        }
    ]
}