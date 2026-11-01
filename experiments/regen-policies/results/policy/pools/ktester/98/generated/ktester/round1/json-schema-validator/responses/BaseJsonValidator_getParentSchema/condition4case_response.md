{
  "testCases": [
    {
      "group": "ParentSchema Retrieval - Null Scenario",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "When the BaseJsonValidator (via a concrete test subclass) is constructed without providing a parent schema, the internal `parentSchema` field remains `null`. Calling `getParentSchema()` should therefore return `null`."
        }
      ]
    },
    {
      "group": "ParentSchema Retrieval - Non‑Null Scenario",
      "cases": [
        {
          "input": [],
          "expected": "instance of MockJsonSchema",
          "description": "When the validator is constructed with a specific `JsonSchema` implementation (e.g., a mock or stub), `getParentSchema()` must return the exact same object that was supplied. This verifies that the method does not create a copy or alter the reference."
        }
      ]
    }
  ]
}