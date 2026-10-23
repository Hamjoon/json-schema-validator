{
  "testCases": [
    {
      "group": "initializeValidators - validator throws exception during preload",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException thrown from validator.preloadJsonSchema(); validatorsLoaded flag is set to true before the exception propagates.",
          "description": "When a validator's preloadJsonSchema() method throws a RuntimeException, initializeValidators() should propagate the exception. The flag validatorsLoaded is already true because it is set prior to iterating over validators."
        }
      ]
    },
    {
      "group": "initializeValidators - getValidators returns null",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException thrown when attempting to call getValidators().values().",
          "description": "If the internal getValidators() method returns null (an unexpected state), initializeValidators() will attempt to dereference it and throw a NullPointerException."
        }
      ]
    },
    {
      "group": "initializeValidators - validators map contains null entry",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException thrown when invoking preloadJsonSchema() on a null validator.",
          "description": "When the validators map includes a null value, the for‑each loop will try to call preloadJsonSchema() on null, resulting in a NullPointerException."
        }
      ]
    },
    {
      "group": "initializeValidators - validators map with duplicate validator instances",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded flag becomes true; preloadJsonSchema() is invoked once for each map entry (e.g., twice if the same validator appears twice).",
          "description": "If the validators map contains the same validator instance under different keys, initializeValidators() should still iterate over all entries and call preloadJsonSchema() for each entry, resulting in multiple calls on the same object."
        }
      ]
    }
  ]
}