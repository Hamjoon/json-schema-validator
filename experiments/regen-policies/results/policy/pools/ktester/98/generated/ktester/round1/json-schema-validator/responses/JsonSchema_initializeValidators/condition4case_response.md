{
  "testCases": [
    {
      "group": "initializeValidators - first invocation (validators not loaded)",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded flag becomes true; preloadJsonSchema() is invoked exactly once on each validator returned by getValidators()",
          "description": "When validatorsLoaded is false, initializeValidators() should iterate over the list returned by getValidators(), call preloadJsonSchema() on each validator, and finally set validatorsLoaded to true."
        }
      ]
    },
    {
      "group": "initializeValidators - subsequent invocation (already loaded)",
      "cases": [
        {
          "input": [],
          "expected": "validatorsLoaded flag remains true; preloadJsonSchema() is NOT invoked on any validator",
          "description": "If validatorsLoaded is already true, the method must skip the preload loop entirely, leaving the flag unchanged and not calling any validator."
        }
      ]
    },
    {
      "group": "initializeValidators - preload throws exception",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException is propagated; validatorsLoaded flag stays false; preloadJsonSchema() is called only on the validator that throws, subsequent validators are not processed",
          "description": "When a validator's preloadJsonSchema() throws, the exception should bubble up, the method should not set validatorsLoaded to true, and iteration should stop at the failing validator."
        }
      ]
    }
  ]
}