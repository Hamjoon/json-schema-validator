{
  "testCases": [
    {
      "group": "Empty schemas map",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchemas is invoked with an empty collection and no exception is thrown",
          "description": "When the internal `schemas` map is empty, `preloadJsonSchema()` should call `preloadJsonSchemas` with an empty iterable. The method must complete normally without throwing any exception."
        }
      ]
    },
    {
      "group": "Single schema entry",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchemas is invoked with a collection containing exactly the single added JsonSchema instance",
          "description": "Add one `JsonSchema` object to the private `schemas` map, then call `preloadJsonSchema()`. The method should forward a collection that contains exactly that schema to `preloadJsonSchemas`."
        }
      ]
    },
    {
      "group": "Multiple schema entries",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchemas is invoked with a collection containing all added JsonSchema instances in insertion order",
          "description": "Populate the `schemas` map with several `JsonSchema` objects (e.g., three). After invoking `preloadJsonSchema()`, verify that `preloadJsonSchemas` receives a collection that includes all those schemas preserving the map's insertion order."
        }
      ]
    },
    {
      "group": "Null schema value in map",
      "cases": [
        {
          "input": [],
          "expected": "preloadJsonSchemas is invoked with a collection that contains a null element, leading to a NullPointerException inside `preloadJsonSchemas` (if it does not tolerate nulls)",
          "description": "Insert a key with a `null` value into the `schemas` map. When `preloadJsonSchema()` is executed, it forwards the map values (including the null). The expected behavior is that the underlying `preloadJsonSchemas` method will throw a `NullPointerException` because a null schema cannot be pre‑loaded. The test asserts that the exception propagates."
        }
      ]
    }
  ]
}