{
  "testCases": [
    {
      "group": "Validate when shouldValidateSchema is true and node is non‑null",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNonNullNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Set containing the ValidationMessage(s) returned by the overridden validate method (e.g., [{\"type\":\"oneOf\",\"path\":\"$\",\"message\":\"oneOf validation failed\"}])",
          "description": "When shouldValidateSchema is true and the instance node is not null, walk should invoke the protected validate(...) method with walk=true and return the messages produced by that call."
        }
      ]
    },
    {
      "group": "Delegate to child schemas when shouldValidateSchema is false (node non‑null)",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNonNullNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "Empty Set (no ValidationMessage added by OneOfValidator) and verify that each child JsonSchema in the schemas list receives a walk call with shouldValidateSchema=false",
          "description": "When shouldValidateSchema is false, walk must skip its own validation and forward the call to every child schema with shouldValidateSchema set to false."
        }
      ]
    },
    {
      "group": "Delegate to child schemas when shouldValidateSchema is true but node is null",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": null },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Empty Set and verify that each child JsonSchema in the schemas list receives a walk call with shouldValidateSchema=false",
          "description": "Even though shouldValidateSchema is true, the node is null, so the condition fails and walk must delegate to child schemas, just like the else‑branch."
        }
      ]
    },
    {
      "group": "No child schemas – else branch returns empty set",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNonNullNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "Empty Set (no ValidationMessage and no child schema walk invoked)",
          "description": "When the schemas list is empty and the else‑branch is taken, walk should simply return an empty LinkedHashSet."
        }
      ]
    }
  ]
}