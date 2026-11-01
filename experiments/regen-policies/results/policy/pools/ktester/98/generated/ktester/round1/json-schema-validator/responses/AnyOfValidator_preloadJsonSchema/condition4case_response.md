{
  "testCases": [
    {
      "group": "Null schemas handling",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException thrown because internal preloadJsonSchemas is invoked with a null schemas list",
          "description": "Verify that preloadJsonSchema() throws a NullPointerException when the AnyOfValidator instance is constructed with a null `schemas` field. This exercises the branch where the internal method receives a null argument."
        }
      ]
    },
    {
      "group": "Empty schemas list",
      "cases": [
        {
          "input": [],
          "expected": "No exception; preloadJsonSchemas called with an empty list and canShortCircuit() executed",
          "description": "Validate that when `schemas` is an empty list, preloadJsonSchema() completes without error, does not attempt to preload any child schema, and still invokes canShortCircuit() to cache the short‑circuit flag."
        }
      ]
    },
    {
      "group": "Single schema preload",
      "cases": [
        {
          "input": [],
          "expected": "No exception; preloadJsonSchemas called once with the single schema and canShortCircuit() executed",
          "description": "Confirm that with a single non‑null JsonSchema in the `schemas` list, preloadJsonSchema() delegates to preloadJsonSchemas() exactly once and then caches the short‑circuit flag via canShortCircuit()."
        }
      ]
    },
    {
      "group": "Multiple schemas preload",
      "cases": [
        {
          "input": [],
          "expected": "No exception; preloadJsonSchemas called for each schema in the list (e.g., three times) and canShortCircuit() executed",
          "description": "Ensure that when `schemas` contains multiple JsonSchema objects, preloadJsonSchema() iterates over all of them, invoking preloadJsonSchemas() for each, and finally calls canShortCircuit() to cache the flag."
        }
      ]
    },
    {
      "group": "Schema preload throws exception",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException propagated from preloadJsonSchemas when a child schema fails to preload",
          "description": "Test that if any child JsonSchema throws an exception during its preload step, preloadJsonSchema() does not swallow the exception but propagates it, exercising the error‑handling branch."
        }
      ]
    }
  ]
}