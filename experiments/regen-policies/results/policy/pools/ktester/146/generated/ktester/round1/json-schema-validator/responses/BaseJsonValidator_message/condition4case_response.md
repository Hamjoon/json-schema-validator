{
    "group": "BaseJsonValidator.message()",
    "cases": [
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode equals the non‑null ObjectNode supplied to the validator",
            "description": "Validate that when the validator is constructed with a concrete (non‑null) schemaNode, the overridden message() method returns a builder that carries the same schemaNode. This exercises the normal execution path of the method."
        },
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode is null",
            "description": "Validate that when the validator is constructed with a null schemaNode, the overridden message() method still returns a builder without throwing an exception and the builder’s schemaNode property is null. This covers the edge case where the field is null."
        }
    ]
}