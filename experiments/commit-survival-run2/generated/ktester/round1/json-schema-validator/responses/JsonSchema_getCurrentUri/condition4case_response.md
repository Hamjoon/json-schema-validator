{
    "group": "JsonSchema.getCurrentUri() - basic getter behavior",
    "cases": [
        {
            "input": [],
            "expected": "http://example.com/schema.json",
            "description": "When a JsonSchema instance is created via the static factory method `JsonSchema.from(...)` with a non‑null `currentUri`, the getter `getCurrentUri()` must return the exact same URI object. The test builds a minimal ValidationContext, supplies a valid URI (`http://example.com/schema.json`), invokes `from`, and asserts that `getCurrentUri().toString()` equals the supplied URI string."
        },
        {
            "input": [],
            "expected": null,
            "description": "When a JsonSchema instance has its internal `currentUri` field set to `null` (e.g., by calling the factory method with a null URI or by using reflection to clear the field), the getter should return `null`. The test creates a schema with a null URI, calls `getCurrentUri()`, and verifies that the result is `null`."
        }
    ]
}