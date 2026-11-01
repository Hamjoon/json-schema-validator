{
    "test_cases": [
        {
            "group": "BaseJsonValidator.message() exception scenarios",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Create a concrete subclass of BaseJsonValidator that overrides the protected `message()` method of the superclass (`ValidationMessageHandler`) to return `null`. Invoking the overridden `BaseJsonValidator.message()` should then attempt to call `.schemaNode(this.schemaNode)` on a null reference, causing a NullPointerException. This verifies the method does not guard against a null builder from the super‑class."
                },
                {
                    "input": [],
                    "expected": "java.lang.IllegalArgumentException",
                    "description": "Construct the validator with a `JsonNode` of a type that the `MessageSourceValidationMessage.Builder.schemaNode(JsonNode)` method does not accept (e.g., a `BinaryNode` or `MissingNode`). When `message()` calls `.schemaNode(this.schemaNode)`, the builder should reject the unsupported node type and throw an IllegalArgumentException. This tests type‑validation inside the builder."
                },
                {
                    "input": [],
                    "expected": "java.lang.IllegalStateException",
                    "description": "Create a subclass of `BaseJsonValidator` that, after calling `super.message()`, immediately calls `build()` on the returned builder (finalising it). Then, still within the same `message()` call, invoke `.schemaNode(this.schemaNode)`. The builder, being already built, should refuse further modification and throw an IllegalStateException. This validates that the method respects the builder’s lifecycle constraints."
                }
            ]
        }
    ]
}