{
  "group": "JsonSchema.getValidationContext() - basic return value",
  "cases": [
    {
      "input": [],
      "expected": "the same ValidationContext instance that was supplied to the JsonSchema via the static from(...) factory method",
      "description": "Create a JsonSchema using JsonSchema.from(...) with a non‑null mock ValidationContext. Calling getValidationContext() should return that exact instance, proving the getter simply returns the field without alteration."
    },
    {
      "input": [],
      "expected": "null",
      "description": "Create a JsonSchema where the internal validationContext field is never set (e.g., by passing null to JsonSchema.from(...)). The getter should return null, confirming that no default value is injected."
    }
  ]
}