{
  "testCases": [
    {
      "group": "Empty schemas list",
      "cases": [
        {
          "input": [],
          "expected": "method completes without invoking any JsonSchema.initializeValidators() and calls canShortCircuit() exactly once",
          "description": "When the internal `schemas` list is empty, the loop body should never be executed. The test verifies that no `initializeValidators()` calls are made and that `canShortCircuit()` is still invoked to cache the short‑circuit flag."
        }
      ]
    },
    {
      "group": "Single schema",
      "cases": [
        {
          "input": [],
          "expected": "method completes and JsonSchema.initializeValidators() is invoked once, canShortCircuit() invoked once",
          "description": "The `schemas` list contains a single mock `JsonSchema`. The test asserts that `initializeValidators()` is called exactly once and that `canShortCircuit()` is called afterwards."
        }
      ]
    },
    {
      "group": "Multiple schemas",
      "cases": [
        {
          "input": [],
          "expected": "method completes and JsonSchema.initializeValidators() is invoked for each schema (e.g., three times), canShortCircuit() invoked once",
          "description": "The `schemas` list holds several mock `JsonSchema` objects. The test verifies that the loop iterates over all entries, calling `initializeValidators()` on each, and that `canShortCircuit()` is called a single time after the loop."
        }
      ]
    },
    {
      "group": "Schema initialization exception",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException propagated from JsonSchema.initializeValidators() and canShortCircuit() not called",
          "description": "One of the schemas in the list throws a `RuntimeException` when `initializeValidators()` is invoked. The test confirms that `preloadJsonSchema()` propagates the exception and aborts before calling `canShortCircuit()`."
        }
      ]
    }
  ]
}