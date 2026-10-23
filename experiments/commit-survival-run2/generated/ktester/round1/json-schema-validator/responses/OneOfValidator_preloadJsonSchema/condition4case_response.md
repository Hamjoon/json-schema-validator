{
  "testCases": [
    {
      "group": "Empty schemas list",
      "cases": [
        {
          "input": [],
          "expected": "No interaction with JsonSchema.initializeValidators; method completes without exception.",
          "description": "The OneOfValidator instance is created with an empty internal `schemas` list. Calling `preloadJsonSchema()` should iterate over an empty collection, resulting in no calls to `initializeValidators()` and no exceptions."
        }
      ]
    },
    {
      "group": "Single schema",
      "cases": [
        {
          "input": [],
          "expected": "JsonSchema.initializeValidators called exactly once; method completes without exception.",
          "description": "A mock JsonSchema is added to the validator's `schemas` list. After invoking `preloadJsonSchema()`, verify that the mock's `initializeValidators()` method is invoked exactly once."
        }
      ]
    },
    {
      "group": "Multiple schemas",
      "cases": [
        {
          "input": [],
          "expected": "JsonSchema.initializeValidators called once for each schema (e.g., three times); method completes without exception.",
          "description": "Three mock JsonSchema objects are added to the validator's `schemas` list. When `preloadJsonSchema()` runs, each mock must receive a single call to `initializeValidators()`. The test asserts the total call count equals the number of schemas."
        }
      ]
    },
    {
      "group": "Exception propagation",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException thrown by a schema's initializeValidators propagates out of preloadJsonSchema.",
          "description": "A mock JsonSchema is configured to throw a RuntimeException when `initializeValidators()` is invoked. After adding it to the validator's `schemas` list, calling `preloadJsonSchema()` should result in the same exception being thrown, confirming that the method does not swallow errors."
        }
      ]
    }
  ]
}