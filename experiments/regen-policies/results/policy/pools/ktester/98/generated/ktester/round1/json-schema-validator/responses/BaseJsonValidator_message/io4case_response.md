{
    "group": "BaseJsonValidator.message() – additional scenarios",
    "cases": [
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode is the Jackson NullNode singleton (JsonNode instance representing JSON null)",
            "description": "Create a concrete subclass of BaseJsonValidator passing `JsonNode schemaNode = NullNode.getInstance()` to the constructor. Invoke `message()` and verify that the returned builder’s `schemaNode` property is exactly the same NullNode instance (i.e., `builder.schemaNode() == NullNode.getInstance()`). This tests that the method correctly propagates a non‑object, non‑array JsonNode that represents JSON null."
        },
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode is the same mutable ArrayNode reference supplied at construction time",
            "description": "Instantiate a concrete BaseJsonValidator with an `ArrayNode` containing a few numeric elements (e.g., `[1,2,3]`). Call `message()` to obtain a builder and assert that `builder.schemaNode()` returns the exact same `ArrayNode` object (reference equality). Then mutate the original `ArrayNode` (e.g., add another element) and verify that the builder’s `schemaNode` reflects the mutation, confirming that the builder stores the reference rather than a copy."
        },
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose MessageSource is the default implementation (`DefaultMessageSource`)",
            "description": "Construct a validator with any schemaNode (null or non‑null). After calling `message()`, retrieve the builder’s `messageSource` (or equivalent accessor) and assert that it is an instance of `DefaultMessageSource`. This ensures that the overridden `message()` method does not replace the default message source supplied by `super.message()`."
        }
    ]
}