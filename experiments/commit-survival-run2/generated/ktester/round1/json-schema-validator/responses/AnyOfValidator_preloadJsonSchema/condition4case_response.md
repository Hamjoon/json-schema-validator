{
  "testCases": [
    {
      "group": "Preload with empty schemas list",
      "cases": [
        {
          "input": [],
          "expected": "schemas list remains empty after preloadJsonSchema() is called",
          "description": "AnyOfValidator is instantiated with an empty internal schemas list. The preloadJsonSchema() method should invoke preloadJsonSchemas(emptyList) and, because the mocked BaseJsonValidator does not add any schemas, the list should stay empty. This verifies the method correctly forwards the empty list without altering it."
        }
      ]
    },
    {
      "group": "Preload with pre‑populated schemas list",
      "cases": [
        {
          "input": [],
          "expected": "schemas list size becomes 3 after preloadJsonSchema() is called",
          "description": "A test double of AnyOfValidator is created where the internal `schemas` list is pre‑filled with two mock JsonSchema instances. The overridden `preloadJsonSchemas(List<JsonSchema>)` method adds one additional mock schema to the supplied list. After invoking preloadJsonSchema(), the list should contain three schemas, confirming that the method forwards the existing list and that the delegate can modify it."
        }
      ]
    },
    {
      "group": "Exception propagation from preloadJsonSchemas",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException is thrown by preloadJsonSchema()",
          "description": "A subclass of AnyOfValidator overrides `preloadJsonSchemas` to throw a RuntimeException. When preloadJsonSchema() is executed, the exception should propagate to the caller. This test ensures that the method does not swallow exceptions from the delegated preload operation."
        }
      ]
    }
  ]
}