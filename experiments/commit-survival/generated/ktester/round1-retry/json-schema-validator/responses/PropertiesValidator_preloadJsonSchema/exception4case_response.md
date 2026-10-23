{
  "test_cases": [
    {
      "group": "Concurrent modification during preload",
      "cases": [
        {
          "input": [],
          "expected": "ConcurrentModificationException propagates",
          "description": "Insert a mock JsonSchema whose preloadJsonSchema implementation mutates the validator's internal `schemas` map (e.g., adds a new entry) while `preloadJsonSchemas` is iterating over `schemas.values()`. When `PropertiesValidator.preloadJsonSchema()` is called, the concurrent modification should cause a `ConcurrentModificationException` which must propagate unchanged."
        }
      ]
    },
    {
      "group": "Error propagation from schema",
      "cases": [
        {
          "input": [],
          "expected": "AssertionError propagates",
          "description": "Add a mock JsonSchema to the validator where the schema's `preloadJsonSchema` method throws an `AssertionError`. Invoke `preloadJsonSchema()` and verify that the `AssertionError` is not caught by the method and bubbles up to the caller."
        }
      ]
    },
    {
      "group": "Null internal schemas map",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException when accessing schemas.values()",
          "description": "Using reflection, set the private `schemas` field of a `PropertiesValidator` instance to `null`. Call `preloadJsonSchema()` and expect a `NullPointerException` because the method attempts to invoke `values()` on a null map."
        }
      ]
    }
  ]
}