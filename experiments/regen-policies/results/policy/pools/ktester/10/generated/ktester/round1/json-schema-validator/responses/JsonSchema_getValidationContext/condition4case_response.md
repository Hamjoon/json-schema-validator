{
  "group": "getValidationContext() - basic behavior",
  "cases": [
    {
      "input": [],
      "expected": "the same non‑null ValidationContext instance that was injected into the JsonSchema object",
      "description": "Create a JsonSchema instance (via reflection) and set its private field `validationContext` to a newly constructed ValidationContext. Calling `getValidationContext()` should return that exact instance (reference equality)."
    },
    {
      "input": [],
      "expected": "null",
      "description": "Instantiate a JsonSchema object without setting the `validationContext` field (it remains the default null). The method should simply return null, confirming that no hidden initialization occurs."
    }
  ]
}