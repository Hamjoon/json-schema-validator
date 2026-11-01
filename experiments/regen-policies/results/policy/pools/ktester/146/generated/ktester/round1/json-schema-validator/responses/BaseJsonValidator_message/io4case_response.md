{
    "group": "BaseJsonValidator.message() additional scenarios",
    "cases": [
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode is the same TextNode instance supplied to the validator (e.g., a JsonNode of type TextNode with value \"example\")",
            "description": "Validate that when the validator is constructed with a non‑ObjectNode JsonNode (a TextNode), the overridden message() method still propagates that exact node to the builder, confirming that the method does not require the node to be an ObjectNode."
        },
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode reference reflects mutations performed on the original ObjectNode after validator construction",
            "description": "Create an ObjectNode with an initial field, construct the validator with it, then add another field to the ObjectNode before invoking message(). Verify that the builder's schemaNode contains the newly added field, demonstrating that the builder holds a reference to the original node rather than a snapshot."
        },
        {
            "input": [],
            "expected": "Two distinct MessageSourceValidationMessage.Builder instances on successive calls to message() (i.e., builder1 != builder2)",
            "description": "Call the message() method twice on the same validator instance and confirm that each invocation returns a new builder object. This ensures that the method does not reuse a single builder instance across calls, which could lead to unintended state sharing."
        },
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode is an empty ObjectNode when the validator is constructed with an empty ObjectNode",
            "description": "Validate that providing an empty ObjectNode (no fields) as the schemaNode results in a builder whose schemaNode is that same empty ObjectNode, confirming correct handling of edge‑case but non‑null schema nodes."
        },
        {
            "input": [],
            "expected": "MessageSourceValidationMessage.Builder instance whose schemaNode is a complex nested ObjectNode (containing arrays and child objects) supplied to the validator",
            "description": "Construct the validator with a richly structured ObjectNode (including nested objects and arrays) and verify that the builder's schemaNode preserves the full structure, ensuring that deep JSON structures are passed through unchanged."
        }
    ]
}