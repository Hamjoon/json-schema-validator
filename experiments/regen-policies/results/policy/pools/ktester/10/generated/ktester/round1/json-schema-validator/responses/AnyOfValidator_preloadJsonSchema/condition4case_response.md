{
  "testCases": [
    {
      "group": "preloadJsonSchema - empty schema list",
      "cases": [
        {
          "input": [],
          "expected": "No exception is thrown and the internal `schemas` list remains empty after the call.",
          "description": "A freshly instantiated `AnyOfValidator` has an empty `schemas` list. Invoking `preloadJsonSchema()` should simply delegate to `preloadJsonSchemas` with an empty list, resulting in no change and no errors."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - non‑empty schema list",
      "cases": [
        {
          "input": [],
          "expected": "The method `preloadJsonSchemas` is invoked exactly once with the internal `schemas` list containing the two mock `JsonSchema` objects; after the call the list still contains those two objects (pre‑loading may have side‑effects on the mocks, which can be verified with Mockito).",
          "description": "Populate the private `schemas` field with two mocked `JsonSchema` instances before calling `preloadJsonSchema()`. The test verifies that the validator forwards the list to `preloadJsonSchemas` (using a spy or Mockito `verify`) and that the list content is unchanged."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - exception propagation",
      "cases": [
        {
          "input": [],
          "expected": "A `RuntimeException` thrown from `preloadJsonSchemas` propagates out of `preloadJsonSchema()` unchanged.",
          "description": "Create a subclass or spy of `AnyOfValidator` where `preloadJsonSchemas` is overridden to throw a `RuntimeException`. Calling `preloadJsonSchema()` should result in that exception being thrown, confirming that the method does not swallow exceptions."
        }
      ]
    }
  ]
}