{
  "testCases": [
    {
      "group": "initializeValidators - first successful load",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded == true and each JsonValidator.preloadJsonSchema() invoked exactly once",
          "description": "When validatorsLoaded is false and the schema has a non‑empty list of validators, initializeValidators() should iterate over the list, call preloadJsonSchema() on each validator, and finally set validatorsLoaded to true."
        }
      ]
    },
    {
      "group": "initializeValidators - subsequent call (already loaded)",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded remains true and no JsonValidator.preloadJsonSchema() is invoked",
          "description": "After a successful first call, a second call to initializeValidators() must skip the preload loop because validatorsLoaded is already true."
        }
      ]
    },
    {
      "group": "initializeValidators - empty validator list",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded == true and no preloadJsonSchema() calls",
          "description": "If getValidators() returns an empty list, the method should still set validatorsLoaded to true without throwing any exception."
        }
      ]
    },
    {
      "group": "initializeValidators - preload throws exception",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException propagated and validatorsLoaded stays false",
          "description": "When a validator's preloadJsonSchema() throws an exception, initializeValidators() must propagate the exception and must NOT set validatorsLoaded to true, allowing a retry on a later call."
        }
      ]
    }
  ]
}