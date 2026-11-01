{
    "group": "BaseJsonValidator#getSchemaNode",
    "cases": [
        {
            "input": [],
            "expected": "the same non‑null JsonNode instance that was supplied to the validator’s constructor",
            "description": "Create a concrete subclass of BaseJsonValidator (e.g., TestValidator) whose constructor forwards the supplied JsonNode to the super‑class. Pass a non‑null ObjectNode (e.g., {\"type\":\"string\"}) as the schemaNode. After construction, invoke getSchemaNode() and verify that the returned reference is exactly the same object (identity equality) and that its content matches the original schema."
        },
        {
            "input": [],
            "expected": "null",
            "description": "Instantiate the concrete TestValidator with a null schemaNode. Call getSchemaNode() and assert that the method returns null. This verifies that the accessor does not perform any null‑guarding or transformation and simply returns the field value."
        }
    ]
}