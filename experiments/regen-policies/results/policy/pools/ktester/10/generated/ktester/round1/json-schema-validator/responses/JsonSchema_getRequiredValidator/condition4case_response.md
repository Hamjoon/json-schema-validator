{
  "group": "JsonSchema.getRequiredValidator() - default state",
  "cases": [
    {
      "input": [],
      "expected": "null",
      "description": "A newly created JsonSchema instance (constructed via the public static factory `JsonSchema.from` with an empty schema) should have its `requiredValidator` field unset. Calling `getRequiredValidator()` must return `null`."
    }
  ]
}