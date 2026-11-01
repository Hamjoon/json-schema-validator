{
    "group": "BaseJsonValidator.message()",
    "cases": [
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode equals the non‑null JsonNode supplied to the validator constructor",
            "description": "Verify that when the validator is constructed with a concrete JsonNode (e.g., an ObjectNode representing {\"type\":\"string\"}), the overridden message() method returns a builder that has its schemaNode property set to that same JsonNode."
        },
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode is null",
            "description": "Verify that when the validator is constructed with a null schemaNode, the overridden message() method still returns a builder and the builder's schemaNode property is null (i.e., the method forwards the null reference without throwing)."
        },
        {
            "input": [],
            "expected": "Two successive calls to message() return distinct Builder objects (different object references)",
            "description": "Ensure that each invocation of message() creates a fresh Builder rather than reusing a cached instance, confirming that super.message() is called on every call."
        }
    ]
}