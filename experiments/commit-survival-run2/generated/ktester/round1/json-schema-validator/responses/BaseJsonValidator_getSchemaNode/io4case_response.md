{
    "group": "BaseJsonValidator#getSchemaNode - diverse JsonNode types",
    "cases": [
        {
            "input": [],
            "expected": "[\"alpha\",\"beta\",\"gamma\"]",
            "description": "When schemaNode is set to an ArrayNode (e.g., [\"alpha\",\"beta\",\"gamma\"]), getSchemaNode() must return that exact array node instance."
        },
        {
            "input": [],
            "expected": "null",
            "description": "When schemaNode is set to Jackson's NullNode (the JSON literal null), getSchemaNode() should return a non‑null JsonNode representing JSON null, not the Java null reference."
        },
        {
            "input": [],
            "expected": "12345",
            "description": "When schemaNode is set to an IntNode with value 12345, getSchemaNode() must return the same numeric node."
        },
        {
            "input": [],
            "expected": "true",
            "description": "When schemaNode is set to a BooleanNode true, getSchemaNode() must return that BooleanNode."
        },
        {
            "input": [],
            "expected": "\"sample text\"",
            "description": "When schemaNode is set to a TextNode containing \"sample text\", getSchemaNode() must return that TextNode."
        },
        {
            "input": [],
            "expected": "same instance as the original schemaNode after child mutation",
            "description": "Set schemaNode to an ObjectNode {\"inner\": {\"value\": 1}}. After calling getSchemaNode() and mutating a nested child (e.g., change \"value\" to 2), the change must be visible through subsequent getSchemaNode() calls, confirming the returned node is not a deep copy."
        },
        {
            "input": [],
            "expected": "identical instance after unrelated validator method call",
            "description": "Assign any JsonNode to schemaNode, invoke another public method of the validator (e.g., getSchemaPath()), then call getSchemaNode() again. The returned node must be the same instance as before, proving that other operations do not replace the stored schemaNode."
        }
    ]
}