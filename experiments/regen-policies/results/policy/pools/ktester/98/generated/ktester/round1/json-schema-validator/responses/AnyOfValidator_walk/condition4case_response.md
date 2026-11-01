{
  "group": "AnyOfValidator.walk - shouldValidateSchema true",
  "cases": [
    {
      "input": [
        {
          "parameter": "executionContext",
          "value": "mockExecutionContext"
        },
        {
          "parameter": "node",
          "value": "mockNode"
        },
        {
          "parameter": "rootNode",
          "value": "mockRootNode"
        },
        {
          "parameter": "instanceLocation",
          "value": "mockInstanceLocation"
        },
        {
          "parameter": "shouldValidateSchema",
          "value": true
        }
      ],
      "expected": "Set containing the ValidationMessage objects returned by the mocked validate method (e.g., [\"msg1\", \"msg2\"]).",
      "description": "When shouldValidateSchema is true the method must delegate to validate(..., true) and return its result. The test verifies that validate is invoked with walk=true and that the returned Set is exactly the one produced by the mock."
    }
  ]
}