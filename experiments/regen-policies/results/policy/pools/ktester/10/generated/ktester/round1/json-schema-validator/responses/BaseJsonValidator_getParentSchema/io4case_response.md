{
    "group": "Parent Schema Retrieval Edge Cases",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "After a non‑null parent schema is assigned (e.g., a mock), the protected `parentSchema` field is reset to `null` via reflection; `getParentSchema()` must then return `null`."
        },
        {
            "input": [],
            "expected": "selfReference",
            "description": "When the validator's own `JsonSchema` instance (the schema being validated) is assigned as its `parentSchema`, `getParentSchema()` should return that same instance, confirming self‑reference handling."
        },
        {
            "input": [],
            "expected": "proxyInstance",
            "description": "A dynamic proxy that implements `JsonSchema` is set as `parentSchema`. `getParentSchema()` must return the proxy object, demonstrating that proxy instances are returned unchanged."
        },
        {
            "input": [],
            "expected": "customSubclass",
            "description": "A custom subclass of `JsonSchema` (e.g., `MyJsonSchema extends JsonSchema`) is assigned to `parentSchema`. The method should return this subclass instance, verifying polymorphic return values."
        },
        {
            "input": [],
            "expected": "differentThreadInstance",
            "description": "The `parentSchema` field is set in one thread and `getParentSchema()` is called from another thread without synchronization. The method should still return the assigned instance, confirming that no thread‑local caching interferes with the simple getter."
        }
    ]
}