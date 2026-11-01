{
  "testCases": [
    {
      "group": "getValidationContext - returns the context set during creation",
      "cases": [
        {
          "input": [],
          "expected": "the same ValidationContext instance that was supplied to JsonSchema.from(...)",
          "description": "Create a ValidationContext mock (or real instance) and use the static JsonSchema.from(...) factory method to construct a JsonSchema with that context. Calling getValidationContext() should return the exact same object (reference equality)."
        }
      ]
    },
    {
      "group": "getValidationContext - returns null when the internal field is null",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Instantiate a JsonSchema via reflection (or a subclass) without initializing the private field `validationContext`. After the field is forced to null, invoking getValidationContext() must return null."
        }
      ]
    },
    {
      "group": "getValidationContext - does not modify the returned context",
      "cases": [
        {
          "input": [],
          "expected": "the original ValidationContext instance unchanged",
          "description": "Obtain the ValidationContext via getValidationContext(), then mutate a mutable property of the returned object (if any). A subsequent call to getValidationContext() should still return the same instance reflecting the mutation, confirming that the method returns the stored reference without creating a copy."
        }
      ]
    }
  ]
}