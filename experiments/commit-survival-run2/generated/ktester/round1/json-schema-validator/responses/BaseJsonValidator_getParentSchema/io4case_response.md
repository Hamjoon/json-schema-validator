{
    "group": "Parent Schema Retrieval - Reference Integrity",
    "cases": [
        {
            "input": [],
            "expected": "the exact same JsonSchema instance that was injected, even after the schema object's internal state is mutated",
            "description": "Inject a mutable JsonSchema implementation (e.g., a simple subclass with a mutable field). Mutate the field after construction and verify that getParentSchema() still returns the same object reference, confirming that the method does not create a copy."
        },
        {
            "input": [],
            "expected": "the exact same proxy JsonSchema instance that was injected",
            "description": "Inject a Mockito‑created proxy of JsonSchema that records method invocations. Verify that getParentSchema() returns the proxy itself, ensuring that the method works with dynamically generated subclasses."
        }
    ]
}