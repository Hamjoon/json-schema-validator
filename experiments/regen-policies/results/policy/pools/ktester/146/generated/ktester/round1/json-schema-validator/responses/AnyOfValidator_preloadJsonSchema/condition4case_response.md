{
  "testCases": [
    {
      "group": "Preload with null schemas",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchemas is called with null argument and canShortCircuit() is invoked exactly once",
          "description": "Verify that when the AnyOfValidator is constructed with a null schemas list, calling preloadJsonSchema() forwards the null list to preloadJsonSchemas() and still triggers a call to canShortCircuit() for caching."
        }
      ]
    },
    {
      "group": "Preload with empty schemas list",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchemas is called with an empty list and canShortCircuit() is invoked exactly once",
          "description": "Ensure that an empty schemas collection does not cause errors; the method should pass the empty list to preloadJsonSchemas() and still call canShortCircuit() to cache the short‑circuit flag."
        }
      ]
    },
    {
      "group": "Preload with multiple schemas",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchemas receives the exact list of schemas supplied to the validator and canShortCircuit() is invoked exactly once",
          "description": "Confirm that when the validator holds several JsonSchema objects, preloadJsonSchema() forwards the same list to preloadJsonSchemas() and triggers a single call to canShortCircuit() for caching."
        }
      ]
    },
    {
      "group": "Preload does not alter schemas list",
      "cases": [
        {
          "input": [],
          "expected": "the original schemas list remains unchanged after preloadJsonSchema() execution",
          "description": "Check that preloadJsonSchema() does not modify the internal schemas collection (e.g., by adding, removing, or reordering elements) while still invoking the helper methods."
        }
      ]
    },
    {
      "group": "Preload invokes canShortCircuit regardless of exception from preloadJsonSchemas",
      "cases": [
        {
          "input": [],
          "expected": "canShortCircuit() is still called even if preloadJsonSchemas throws a RuntimeException",
          "description": "Simulate a failure inside preloadJsonSchemas (e.g., by mocking it to throw) and verify that the validator still attempts to call canShortCircuit() before propagating the exception."
        }
      ]
    }
  ]
}