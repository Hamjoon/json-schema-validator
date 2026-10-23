{
    "group": "createExecutionContext – null validationContext",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException is thrown because the JsonSchema's validationContext field is null and the method attempts to call getConfig() on it.",
            "description": "Create a JsonSchema instance (or a subclass/mock) where the internal validationContext is set to null. Invoke createExecutionContext() and verify that a NullPointerException is thrown, confirming that the method does not guard against a missing ValidationContext."
        }
    ]
}