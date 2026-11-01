{
  "group": "walk_shouldValidateSchema_true",
  "cases": [
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContext" },
        { "parameter": "node", "value": "mockJsonNode" },
        { "parameter": "rootNode", "value": "mockRootNode" },
        { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
        { "parameter": "shouldValidateSchema", "value": true }
      ],
      "expected": "Set containing the ValidationMessage(s) returned by the overridden validate() method (e.g., [{\"type\":\"oneOf\",\"message\":\"validation failed\"}])",
      "description": "When shouldValidateSchema is true, walk() must invoke validate() and return its messages. The internal schemas list is empty, so no schema.walk() calls should occur."
    },
    {
      "input": [
        { "parameter": "executionContext", "value": "mockExecutionContext" },
        { "parameter": "node", "value": "mockJsonNode" },
        { "parameter": "rootNode", "value": "mockRootNode" },
        { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
        { "parameter": "shouldValidateSchema", "value": true }
      ],
      "expected": "Set containing the ValidationMessage(s) returned by the overridden validate() method (e.g., [{\"type\":\"oneOf\",\"message\":\"validation failed\"}])",
      "description": "When shouldValidateSchema is true and the validator holds pre‑loaded schemas, walk() still only calls validate() and must ignore the schemas list. The returned set must be exactly the one produced by validate()."
    }
  ]
}