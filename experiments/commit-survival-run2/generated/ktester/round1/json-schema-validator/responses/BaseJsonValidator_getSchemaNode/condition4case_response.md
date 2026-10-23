{
  "group": "BaseJsonValidator#getSchemaNode",
  "cases": [
    {
      "input": [],
      "expected": "null",
      "description": "When the validator's internal schemaNode field is never initialized (remains null), getSchemaNode() should return null."
    },
    {
      "input": [],
      "expected": "{ \"type\": \"string\" }",
      "description": "When schemaNode is set to a non‑null ObjectNode (e.g., {\"type\":\"string\"}), getSchemaNode() must return that exact JSON node."
    },
    {
      "input": [],
      "expected": "same instance as the original schemaNode",
      "description": "Validate that getSchemaNode() returns the same object reference that was stored in the validator. After calling getSchemaNode(), mutating the returned node should be reflected in the validator's internal state, confirming it is not a copy."
    },
    {
      "input": [],
      "expected": "identical node on repeated calls",
      "description": "Calling getSchemaNode() multiple times should consistently return the same node instance (no new objects are created on each call)."
    }
  ]
}