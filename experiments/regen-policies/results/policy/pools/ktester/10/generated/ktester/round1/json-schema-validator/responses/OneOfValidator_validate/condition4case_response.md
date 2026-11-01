{
  "groups": [
    {
      "group": "Walk disabled – exactly one matching schema",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(\"/path\")" }
          ],
          "expected": "returns empty Set<ValidationMessage> (no errors, state.matchedNode set to true)",
          "description": "state.isWalkEnabled() is false, the single registered schema validates without errors and state.hasMatchedNode() remains true, so numberOfValidSchema == 1 and the validator returns an empty error set."
        }
      ]
    },
    {
      "group": "Walk disabled – schema returns errors",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(invalidNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(\"/path\")" }
          ],
          "expected": "returns Set containing OneOf ValidationMessage and the child ValidationMessage(s) from the failing schema",
          "description": "state.isWalkEnabled() is false, the schema produces validation errors, therefore numberOfValidSchema stays 0, failFast is false, so a OneOf error is added together with the child errors."
        }
      ]
    },
    {
      "group": "Walk disabled – schema matches but state.hasMatchedNode is false",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true, matchedNodeAfterValidate=false)" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(\"/path\")" }
          ],
          "expected": "returns Set containing OneOf ValidationMessage (no valid schema counted)",
          "description": "Even though the schema returns no errors, state.hasMatchedNode() is false after validation, causing the `continue` path; numberOfValidSchema remains 0, leading to a OneOf error."
        }
      ]
    },
    {
      "group": "Walk disabled – more than one matching schema",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(walkEnabled=false, failFast=false, validationEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(\"/path\")" }
          ],
          "expected": "returns Set containing OneOf ValidationMessage (numberOfValidSchema == 2, short‑circuit after second schema)",
          "description": "Two schemas both validate without errors, causing numberOfValidSchema to become 2, which triggers the break and later the OneOf error because exactly one schema must match."
        }
      ]
    },
    {
      "group": "Fail‑fast mode – invalid number of matching schemas",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(walkEnabled=false, failFast=true, validationEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(invalidNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(\"/path\")" }
          ],
          "expected": "throws JsonSchemaException containing OneOf ValidationMessage",
          "description": "failFast is true and the number of valid schemas is not exactly one, so the validator throws a JsonSchemaException instead of returning an error set."
        }
      ]
    },
    {
      "group": "Walk enabled – schema validation performed via walk()",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(walkEnabled=true, failFast=false, validationEnabled=true)" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(\"/path\")" }
          ],
          "expected": "returns empty Set<ValidationMessage> (schema.walk returns empty, state.hasMatchedNode true)",
          "description": "When walk is enabled, the validator calls schema.walk(). The mock walk returns no errors and leaves state.hasMatchedNode true, resulting in a successful OneOf validation with no errors."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext(walkEnabled=true, failFast=false, validationEnabled=true, matchedNodeAfterWalk=false)" },
            { "parameter": "node", "value": "mockJsonNode(validNode)" },
            { "parameter": "rootNode", "value": "mockJsonNode(root)" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath(\"/path\")" }
          ],
          "expected": "returns Set containing OneOf ValidationMessage (schema.walk returns empty but state.hasMatchedNode false)",
          "description": "Even with walk enabled, if after schema.walk() the validator state reports no matched node, the schema is ignored (continue) and the final result is a OneOf error."
        }
      ]
    }
  ]
}