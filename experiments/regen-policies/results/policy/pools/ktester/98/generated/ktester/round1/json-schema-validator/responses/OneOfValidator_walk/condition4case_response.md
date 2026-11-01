{
  "testCases": [
    {
      "group": "validateWhenShouldValidateTrue",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Set containing two ValidationMessage objects with messages \"error A\" and \"error B\"",
          "description": "When shouldValidateSchema is true the validator should delegate to validate(..., true) and return the messages produced by that call."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Empty Set",
          "description": "When shouldValidateSchema is true but validate returns an empty set, walk should also return an empty set."
        }
      ]
    },
    {
      "group": "walkWhenShouldValidateFalse",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "Empty Set and schema.walk called zero times",
          "description": "When shouldValidateSchema is false and the internal schemas list is empty, walk should iterate over no schemas and return an empty set."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockInstanceLocation" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "Empty Set and schema.walk called three times with the same parameters",
          "description": "When shouldValidateSchema is false and the validator holds three schemas, walk must invoke each schema's walk method with the original arguments but must not collect any ValidationMessage, therefore it returns an empty set."
        }
      ]
    }
  ]
}