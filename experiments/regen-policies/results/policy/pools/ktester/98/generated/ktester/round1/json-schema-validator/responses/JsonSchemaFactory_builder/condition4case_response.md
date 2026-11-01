{
  "group": "builder() method",
  "cases": [
    {
      "input": [],
      "expected": "non-null instance of JsonSchemaFactory.Builder",
      "description": "Verify that builder() returns a newly created Builder object and the result is not null."
    },
    {
      "input": [],
      "expected": "two calls to builder() return distinct Builder instances",
      "description": "Ensure that each invocation of builder() creates a separate Builder instance (no singleton or caching behavior)."
    }
  ]
}